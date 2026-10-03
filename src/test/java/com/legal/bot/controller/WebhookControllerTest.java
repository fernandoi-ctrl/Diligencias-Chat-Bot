package com.legal.bot.controller;

import com.legal.bot.config.WhatsAppProperties;
import com.legal.bot.handler.WhatsAppMessageHandler;
import com.legal.bot.model.dto.WhatsAppWebhookPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del webhook sin levantar la app completa (ni BD, ni Ollama, ni Meta).
 */
// WhatsAppProperties es un @ConfigurationProperties: el valor se fija por propiedad y no
// con setters en un @Bean. Un @Bean construido a mano tambien pasa por el binder, que le
// sobreescribiria verify-token con el de src/test/resources/application.yaml.
@WebMvcTest(value = WebhookController.class,
		properties = "whatsapp.verify-token=" + WebhookControllerTest.VERIFY_TOKEN)
@Import(WhatsAppProperties.class)
class WebhookControllerTest {

	static final String VERIFY_TOKEN = "token-secreto-de-prueba";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WhatsAppMessageHandler handler;

	@Test
	void devuelveElChallengeCuandoElTokenCoincide() throws Exception {
		this.mockMvc.perform(get("/webhook")
						.param("hub.mode", "subscribe")
						.param("hub.verify_token", VERIFY_TOKEN)
						.param("hub.challenge", "1234567890"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
				.andExpect(content().string("1234567890"));
	}

	@Test
	void rechazaLaVerificacionConTokenIncorrecto() throws Exception {
		this.mockMvc.perform(get("/webhook")
						.param("hub.mode", "subscribe")
						.param("hub.verify_token", "token-equivocado")
						.param("hub.challenge", "1234567890"))
				.andExpect(status().isForbidden());
	}

	/** El test que faltaba: el POST tiene que llegar al handler, no solo devolver 200. */
	@Test
	void aceptaUnMensajeDeTextoYLoDelegaAlHandler() throws Exception {
		String cuerpo = """
				{
				  "object": "whatsapp_business_account",
				  "entry": [{
				    "id": "1",
				    "changes": [{
				      "field": "messages",
				      "value": {
				        "messaging_product": "whatsapp",
				        "metadata": {"display_phone_number": "5214490000000", "phone_number_id": "123"},
				        "contacts": [{"wa_id": "5214491234567", "profile": {"name": "Fernando"}}],
				        "messages": [{
				          "from": "5214491234567",
				          "id": "wamid.TEST1",
				          "timestamp": "1726000000",
				          "type": "text",
				          "text": {"body": "Cuanto cuesta una diligencia?"}
				        }]
				      }
				    }]
				  }]
				}""";

		this.mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isOk());

		verify(this.handler, times(1)).procesar(any(WhatsAppWebhookPayload.class));
	}

	/** Los acuses de entrega llegan al mismo endpoint y no deben procesarse. */
	@Test
	void ignoraLosAcusesDeEntrega() throws Exception {
		String cuerpo = """
				{
				  "object": "whatsapp_business_account",
				  "entry": [{
				    "id": "1",
				    "changes": [{
				      "field": "messages",
				      "value": {
				        "messaging_product": "whatsapp",
				        "metadata": {"display_phone_number": "5214490000000", "phone_number_id": "123"},
				        "statuses": [{"id": "wamid.X", "status": "delivered"}]
				      }
				    }]
				  }]
				}""";

		this.mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isOk());

		verify(this.handler, never()).procesar(any());
	}

	/** Un cuerpo raro no debe devolver error: Meta desactiva webhooks que fallan. */
	@Test
	void respondeOkAnteUnCuerpoVacio() throws Exception {
		this.mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isOk());

		verify(this.handler, never()).procesar(any());
	}

}
