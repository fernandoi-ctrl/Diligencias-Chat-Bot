package com.legal.bot.handler;

import java.util.List;
import java.util.Optional;

import tools.jackson.databind.json.JsonMapper;
import com.legal.bot.config.WhatsAppProperties;
import com.legal.bot.model.dto.WhatsAppMessage;
import com.legal.bot.model.dto.WhatsAppWebhookPayload;
import com.legal.bot.model.entity.Cliente;
import com.legal.bot.model.entity.Conversacion;
import com.legal.bot.service.ConversationService;
import com.legal.bot.service.KnowledgeService;
import com.legal.bot.service.LegalAIService;
import com.legal.bot.service.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre la parte critica de la fase 1: cuando el bot responde y cuando NO.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WhatsAppMessageHandlerTest {

	private static final String TELEFONO = "5214491234567";

	@Mock
	private ConversationService conversationService;

	@Mock
	private LegalAIService legalAIService;

	@Mock
	private WhatsAppService whatsAppService;

	private WhatsAppMessageHandler handler;

	private Cliente cliente;

	private Conversacion conversacion;

	@BeforeEach
	void setUp() {
		this.cliente = Cliente.builder().telefono(TELEFONO).nombre("Fernando").modoIaActivo(true).build();
		this.conversacion = Conversacion.builder().cliente(this.cliente).activa(true).build();

		WhatsAppProperties properties = new WhatsAppProperties();
		properties.setNumerosPermitidos(List.of(TELEFONO));

		when(this.conversationService.obtenerOCrearCliente(anyString(), any())).thenReturn(this.cliente);
		when(this.conversationService.obtenerOCrearConversacion(any())).thenReturn(this.conversacion);
		when(this.whatsAppService.enviarTexto(anyString(), anyString())).thenReturn(Optional.of("wamid.OUT"));
		when(this.legalAIService.responder(any(Conversacion.class), anyString()))
				.thenReturn("Respuesta del modelo.");

		this.handler = new WhatsAppMessageHandler(this.conversationService, this.legalAIService,
				this.whatsAppService, new KnowledgeService(JsonMapper.builder().build()), properties);
	}

	@Test
	void respondeConLaIaAUnaConsultaLegal() {
		this.handler.procesar(payload("wamid.1", "text", "Llevan divorcios?"));

		verify(this.legalAIService).responder(this.conversacion, "Llevan divorcios?");
		verify(this.whatsAppService).enviarTexto(TELEFONO, "Respuesta del modelo.");
		verify(this.conversationService).guardarSaliente(any(), anyString(), eq(true), anyString());
	}

	/** El numero fuera de la whitelist se descarta antes de tocar BD o modelo. */
	@Test
	void ignoraUnNumeroQueNoEstaEnLaWhitelist() {
		WhatsAppProperties soloOtro = new WhatsAppProperties();
		soloOtro.setNumerosPermitidos(List.of("5219999999999"));
		WhatsAppMessageHandler restringido = new WhatsAppMessageHandler(this.conversationService,
				this.legalAIService, this.whatsAppService,
				new KnowledgeService(JsonMapper.builder().build()), soloOtro);

		restringido.procesar(payload("wamid.fuera", "text", "Hola"));

		verify(this.whatsAppService, never()).enviarTexto(anyString(), anyString());
		verify(this.conversationService, never()).guardarEntrante(any(), anyString(), anyString(), anyString());
	}

	/**
	 * Mexico: Meta manda el telefono unas veces con el '1' de movil y otras sin el.
	 * Las dos formas tienen que pasar la whitelist.
	 */
	@Test
	void aceptaElNumeroMexicanoSinElUnoDeMovil() {
		WhatsAppProperties props = new WhatsAppProperties();
		props.setNumerosPermitidos(List.of("5214491234567"));

		assertThat(props.numeroEstaPermitido("524491234567")).isTrue();
		assertThat(props.numeroEstaPermitido("5214491234567")).isTrue();
		assertThat(props.numeroEstaPermitido("5214490000000")).isFalse();
	}

	@Test
	void noRespondeDosVecesAlMismoMensaje() {
		when(this.conversationService.yaProcesado("wamid.repetido")).thenReturn(true);

		this.handler.procesar(payload("wamid.repetido", "text", "Hola"));

		verify(this.whatsAppService, never()).enviarTexto(anyString(), anyString());
		verify(this.legalAIService, never()).responder(any(Conversacion.class), anyString());
		verify(this.conversationService, never()).guardarEntrante(any(), anyString(), anyString(), anyString());
	}

	@Test
	void noRespondeSiElClienteTieneElModoIaApagado() {
		this.cliente.setModoIaActivo(false);

		this.handler.procesar(payload("wamid.2", "text", "Necesito ayuda con un despido"));

		// El mensaje si se guarda: el abogado tiene que poder leerlo.
		verify(this.conversationService).guardarEntrante(any(), eq("wamid.2"), eq("text"),
				eq("Necesito ayuda con un despido"));
		verify(this.whatsAppService, never()).enviarTexto(anyString(), anyString());
		verify(this.legalAIService, never()).responder(any(Conversacion.class), anyString());
	}

	@Test
	void contestaElMenuAnteUnSaludoSinLlamarAlModelo() {
		this.handler.procesar(payload("wamid.3", "text", "Hola"));

		verify(this.legalAIService, never()).responder(any(Conversacion.class), anyString());

		ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
		verify(this.whatsAppService).enviarTexto(eq(TELEFONO), enviado.capture());
		assertThat(enviado.getValue()).contains("Despacho Legal Aguascalientes").contains("1.");
	}

	@Test
	void laOpcionCuatroAvisaQuePasaraUnAbogado() {
		this.handler.procesar(payload("wamid.4", "text", "4"));

		verify(this.legalAIService, never()).responder(any(Conversacion.class), anyString());

		ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
		verify(this.whatsAppService).enviarTexto(eq(TELEFONO), enviado.capture());
		assertThat(enviado.getValue()).contains("abogado");
	}

	@Test
	void avisaQueSoloLeeTextoAnteUnAudio() {
		this.handler.procesar(payload("wamid.5", "audio", null));

		verify(this.legalAIService, never()).responder(any(Conversacion.class), anyString());
		verify(this.conversationService).guardarEntrante(any(), eq("wamid.5"), eq("audio"), anyString());

		ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
		verify(this.whatsAppService).enviarTexto(eq(TELEFONO), enviado.capture());
		assertThat(enviado.getValue()).contains("texto");
	}

	/** Un fallo al enviar no debe tumbar el procesamiento del resto del lote. */
	@Test
	void guardaLaRespuestaAunqueMetaRechaceElEnvio() {
		when(this.whatsAppService.enviarTexto(anyString(), anyString())).thenReturn(Optional.empty());

		this.handler.procesar(payload("wamid.6", "text", "Cuanto cuesta una diligencia?"));

		verify(this.conversationService).guardarSaliente(any(), anyString(), anyBoolean(), eq(null));
	}

	/** Construye el sobre del webhook con un solo mensaje dentro. */
	private static WhatsAppWebhookPayload payload(String wamid, String tipo, String texto) {
		WhatsAppMessage mensaje = new WhatsAppMessage(TELEFONO, wamid, "1726000000", tipo,
				texto != null ? new com.legal.bot.model.dto.WhatsAppText(texto) : null);

		WhatsAppWebhookPayload.Contact contacto = new WhatsAppWebhookPayload.Contact(TELEFONO,
				new WhatsAppWebhookPayload.Profile("Fernando"));

		WhatsAppWebhookPayload.Value value = new WhatsAppWebhookPayload.Value("whatsapp",
				new WhatsAppWebhookPayload.Metadata("5214490000000", "123"), List.of(contacto),
				List.of(mensaje));

		return new WhatsAppWebhookPayload("whatsapp_business_account",
				List.of(new WhatsAppWebhookPayload.Entry("1",
						List.of(new WhatsAppWebhookPayload.Change("messages", value)))));
	}

}
