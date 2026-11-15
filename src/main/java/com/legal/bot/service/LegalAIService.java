package com.legal.bot.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import com.legal.bot.model.entity.Conversacion;

@Service
public class LegalAIService {

	private static final Logger log = LoggerFactory.getLogger(LegalAIService.class);

	private static final String RESPUESTA_FALLBACK = """
			En este momento no puedo procesar tu consulta. Por favor intenta de nuevo en \
			unos minutos o agenda una consulta con uno de nuestros abogados.""";

	private static final String SYSTEM_PROMPT = """
			Eres el asistente virtual de la Gestoría de Diligencias Aguascalientes.
			Atendemos únicamente servicios de diligencias y trámites, NO llevamos juicios ni casos legales completos.

			DATOS DEL SERVICIO
			- Nombre: Gestoría de Diligencias Aguascalientes
			- Ubicación: Aguascalientes, México
			- Horario de atención: Lunes a Viernes, 9:00 AM a 7:00 PM
			- Modalidad: Las citas son por videollamada (Zoom) para coordinar el servicio.

			SERVICIOS QUE SÍ REALIZAMOS (DILIGENCIAS)
			- Notificaciones, citatorios y emplazamientos.
			- Requerimientos de documentos.
			- Fe de hechos y trámites administrativos.

			COSTOS DE DILIGENCIAS (cifras exactas, nunca las modifiques)
			- Dentro de la ciudad de Aguascalientes: $1,000 MXN
			- En municipios del estado de Aguascalientes: $1,300 MXN
			Si el usuario pregunta por el costo y no aclara la ubicación, menciona ambas cifras.

			REGLAS DE RESPUESTA ESTRICTAS
			1. Responde SIEMPRE en español, de forma breve y amable.
			2. Si el usuario pide ayuda con un juicio, divorcio, pensión, demanda penal o cualquier caso legal completo, \
			aclara amablemente que SOLO realizan diligencias y ofrécele agendar una cita por Zoom.
			3. Da cantidades exactas de las diligencias según la ubicación.
			4. NO inventes información ni des asesoría legal.
			5. Si el usuario quiere contratar una diligencia o agendar una cita, debes usar la herramienta 'agendarCita'.

			INSTRUCCIONES PARA AGENDAR CITAS (ZOOM)
			1. Si el cliente pide agendar una cita o contratar una diligencia, DEBES usar la herramienta 'agendarCita'.
			2. Antes de usar la herramienta, recopila: nombre del cliente, fecha/hora deseada, tipo de diligencia y ubicación.
			3. Si faltan datos, pregúntalos uno por uno amablemente.
			4. Cuando tengas todos los datos, ejecuta la herramienta.
			5. La herramienta solo REGISTRA la solicitud. Nunca digas que la cita está agendada o confirmada: \
			explica que la abogada la confirmará y transmite el mensaje que devuelva la herramienta.
			""";

	private final ChatClient chatClient;
	private final ConversationService conversationService;

	public LegalAIService(ChatModel chatModel, CitaTools citaTools, ConversationService conversationService) {
		this.conversationService = conversationService;
		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(SYSTEM_PROMPT)
				.defaultTools(citaTools) 
				.build();
	}

	// En LegalAIService.java

		public String responder(String consulta, String telefono) {
			if (consulta == null || consulta.isBlank()) {
				return "Cuéntame tu duda legal y con gusto te oriento.";
			}
			try {
				String respuesta = this.chatClient.prompt()
						.user(consulta)
						.toolContext(java.util.Map.of("telefono", telefono)) // <-- AÑADIR ESTO
						.call()
						.content();

				if (respuesta == null || respuesta.isBlank()) {
					return RESPUESTA_FALLBACK;
				}
				return respuesta.trim();
			} catch (Exception ex) {
				log.error("Error al consultar el modelo de IA: {}", ex.getMessage(), ex);
				return RESPUESTA_FALLBACK;
			}
		}

		// Agregamos el parámetro String telefono
		public String responder(Conversacion conversacion, String texto, String telefono) {
			if (texto == null || texto.isBlank()) {
				return "Cuéntame tu duda legal y con gusto te oriento.";
			}

			// ELIMINA ESTA LÍNEA:
			// String telefono = conversacion.getCliente().getTelefono();

			List<Message> historial = historialDe(conversacion);
			
			if (historial.isEmpty()) {
				return responder(texto, telefono);
			}

			try {
				String respuesta = this.chatClient.prompt()
						.messages(historial)
						.user(texto)
						.toolContext(java.util.Map.of("telefono", telefono)) 
						.call()
						.content();

				if (respuesta == null || respuesta.isBlank()) {
					return RESPUESTA_FALLBACK;
				}
				return respuesta.trim();
			} catch (Exception ex) {
				log.error("Error al consultar el modelo de IA con historial: {}", ex.getMessage(), ex);
				return RESPUESTA_FALLBACK;
			}
		}

//	public String responderPorTema(String tema) {
//		if (tema == null || tema.isBlank()) {
//			return RESPUESTA_FALLBACK;
//		}
//		return responder("El cliente eligió del menú la opción: \"" + tema
//				+ "\". Explícale ese punto de forma breve y concreta.");
//	}
		public String responderPorTema(String tema, String telefono) { // <-- Agregamos el teléfono
			if (tema == null || tema.isBlank()) {
				return RESPUESTA_FALLBACK;
			}
			return responder("El cliente eligió del menú la opción: \"" + tema
					+ "\". Explícale ese punto de forma breve y concreta.", telefono); // <-- Pasamos el teléfono
		}

	private List<Message> historialDe(Conversacion conversacion) {
		if (conversacion == null || conversacion.getId() == null) {
			return List.of();
		}
		try {
			// CORRECCIÓN AQUÍ: Diferenciar User de Assistant
			return this.conversationService.obtenerHistorialReciente(conversacion).stream()
					.filter(m -> m.getContenido() != null && !m.getContenido().isBlank())
					.<Message>map(m -> "assistant".equalsIgnoreCase(m.getRol())
							? new AssistantMessage(m.getContenido())
							: new UserMessage(m.getContenido()))
					.toList();
		} catch (Exception ex) {
			log.warn("No se pudo leer el historial de la conversación {}: {}", conversacion.getId(), ex.getMessage());
			return List.of();
		}
	}
}