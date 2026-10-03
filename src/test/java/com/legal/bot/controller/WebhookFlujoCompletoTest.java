package com.legal.bot.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.legal.bot.service.LegalAIService;
import com.legal.bot.service.WhatsAppService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Recorre el camino completo con la app levantada de verdad: HTTP POST en /webhook ->
 * WebhookController -> @Async del handler -> BD (H2) -> WhatsAppService.
 *
 * <p>Es el equivalente automatizado del {@code curl} de prueba: si esto pasa, el flujo
 * esta entero y lo que falte es configuracion de Meta o del tunel, no codigo.</p>
 *
 * <p>Solo se sustituye {@link WhatsAppService} (para no llamar a Graph API) y
 * {@link LegalAIService} (para no necesitar Ollama corriendo).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"telegram.bot.enabled=false",
				"whatsapp.numeros-permitidos[0]=5214499876543"
		})
class WebhookFlujoCompletoTest {

	private static final String CUERPO_DE_META = """
			{
			  "object": "whatsapp_business_account",
			  "entry": [{
			    "id": "123",
			    "changes": [{
			      "field": "messages",
			      "value": {
			        "messaging_product": "whatsapp",
			        "metadata": {"display_phone_number": "5214491234567", "phone_number_id": "1434921093874"},
			        "contacts": [{"wa_id": "5214499876543", "profile": {"name": "Fernando"}}],
			        "messages": [{
			          "from": "5214499876543",
			          "id": "wamid.TEST",
			          "timestamp": "1726000000",
			          "text": {"body": "Llevan casos de pension alimenticia?"},
			          "type": "text"
			        }]
			      }
			    }]
			  }]
			}""";

	@LocalServerPort
	private int puerto;

	@MockitoBean
	private WhatsAppService whatsAppService;

	@MockitoBean
	private LegalAIService legalAIService;

	@Test
	void unMensajeDeTextoLlegaHastaElEnvioDeLaRespuesta() throws Exception {
		when(this.legalAIService.responder(any(), anyString()))
				.thenReturn("Si, llevamos pension alimenticia.");
		when(this.whatsAppService.enviarTexto(anyString(), anyString()))
				.thenReturn(Optional.of("wamid.RESPUESTA"));

		HttpResponse<String> respuesta = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + this.puerto + "/webhook"))
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(CUERPO_DE_META))
						.build(),
				HttpResponse.BodyHandlers.ofString());

		// Meta exige 200 inmediato, antes de que el procesamiento termine.
		assertThat(respuesta.statusCode()).isEqualTo(200);

		// El handler corre en el executor 'whatsappExecutor', asi que se espera.
		Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
				verify(this.whatsAppService).enviarTexto(eq("5214499876543"),
						eq("Si, llevamos pension alimenticia.")));
	}

	@Test
	void elGetDeVerificacionDevuelveElChallengeEnTextoPlano() throws Exception {
		HttpResponse<String> respuesta = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder()
						.uri(URI.create("http://localhost:" + this.puerto
								+ "/webhook?hub.mode=subscribe&hub.verify_token=verify-de-prueba"
								+ "&hub.challenge=987654"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(respuesta.statusCode()).isEqualTo(200);
		assertThat(respuesta.body()).isEqualTo("987654");
		assertThat(respuesta.headers().firstValue("Content-Type").orElse(""))
				.startsWith("text/plain");
	}

}
