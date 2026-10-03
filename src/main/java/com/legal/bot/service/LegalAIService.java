package com.legal.bot.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import com.legal.bot.model.entity.Conversacion;

/**
 * Procesa las consultas legales con el modelo local de Ollama a traves de Spring AI.
 *
 * <p>Todo el conocimiento del despacho vive en el SYSTEM PROMPT, de modo que el modelo
 * responde con datos fijos (costos, materias, horario) sin inventarlos.</p>
 */
@Service
public class LegalAIService {

	private static final Logger log = LoggerFactory.getLogger(LegalAIService.class);

	/** Respuesta de cortesia cuando Ollama no esta disponible o falla la generacion. */
	private static final String RESPUESTA_FALLBACK = """
			En este momento no puedo procesar tu consulta. Por favor intenta de nuevo en \
			unos minutos o agenda una consulta con uno de nuestros abogados.""";

	private static final String SYSTEM_PROMPT = """
			Eres el asistente virtual del Despacho Legal Aguascalientes.

			DATOS DEL DESPACHO
			- Nombre: Despacho Legal Aguascalientes
			- Ubicacion: Aguascalientes, Mexico
			- Horario de atencion: Lunes a Viernes, 9:00 AM a 7:00 PM

			COSTOS DE DILIGENCIAS (son cifras exactas, nunca las modifiques)
			- Dentro de la ciudad de Aguascalientes: $1,000 MXN
			- En municipios del estado de Aguascalientes: $1,300 MXN
			Si el usuario pregunta por el costo y no aclara la ubicacion, menciona ambas
			cifras indicando a que caso corresponde cada una.

			MATERIAS DE JUICIO QUE SI SE LLEVAN
			- Civil: contratos, arrendamientos, cobros, danos y perjuicios
			- Familiar: divorcios, pension alimenticia, custodia, patria potestad
			- Laboral: despidos injustificados, liquidaciones, conflictos laborales
			- Mercantil: titulos de credito, pagares, cheques, conflictos entre socios
			- Penal: defensa penal, querellas, denuncias
			- Administrativa: amparos, recursos administrativos

			REGLAS DE RESPUESTA
			1. Responde SIEMPRE en espanol.
			2. Habla solo de temas legales y de los servicios del despacho. Si te preguntan
			   otra cosa, aclara amablemente que solo puedes orientar en asuntos legales.
			3. Da cantidades exactas de las diligencias segun la ubicacion.
			4. Si te preguntan si se lleva una materia, confirma usando la lista de arriba.
			5. NO inventes informacion: no des precios, plazos, nombres, direcciones,
			   telefonos ni articulos de ley que no aparezcan en este prompt.
			6. Si no sabes algo o el caso requiere analisis, sugiere agendar una consulta
			   con un abogado del despacho.
			7. Se conciso: maximo 300 caracteres siempre que sea posible.
			8. Tono profesional pero accesible, sin tecnicismos innecesarios.
			9. No des una asesoria legal definitiva ni garantices resultados de un juicio.
			""";

	private final ChatClient chatClient;

	private final ConversationService conversationService;

	/**
	 * Spring AI autoconfigura el {@code ChatClient.Builder}; aqui le fijamos el system
	 * prompt para que aplique a todas las consultas.
	 */
	public LegalAIService(ChatClient.Builder chatClientBuilder, ConversationService conversationService) {
		this.chatClient = chatClientBuilder
				.defaultSystem(SYSTEM_PROMPT)
				.build();
		this.conversationService = conversationService;
	}

	/**
	 * Envia la consulta del usuario al modelo y devuelve la respuesta generada.
	 * Nunca lanza excepcion: ante un fallo devuelve {@link #RESPUESTA_FALLBACK}.
	 */
	public String responder(String consulta) {
		if (consulta == null || consulta.isBlank()) {
			return "Cuentame tu duda legal y con gusto te oriento.";
		}

		try {
			String respuesta = this.chatClient.prompt()
					.user(consulta)
					.call()
					.content();

			if (respuesta == null || respuesta.isBlank()) {
				log.warn("El modelo devolvio una respuesta vacia para la consulta recibida.");
				return RESPUESTA_FALLBACK;
			}
			return respuesta.trim();
		}
		catch (Exception ex) {
			// Causa mas comun: Ollama apagado en localhost:11434 o modelo no descargado.
			log.error("Error al consultar el modelo de IA: {}", ex.getMessage(), ex);
			return RESPUESTA_FALLBACK;
		}
	}
	/**
	 * Firma principal, la que usa el handler de WhatsApp: responde teniendo en cuenta los
	 * ultimos mensajes de la conversacion.
	 *
	 * <p>Antes era un stub que devolvia cadena vacia. El handler interpreta la cadena
	 * vacia como "el modelo no supo que contestar" y no envia nada, asi que el bot
	 * guardaba el mensaje del cliente y se quedaba callado en toda consulta abierta.</p>
	 */
	public String responder(Conversacion conversacion, String texto) {
		if (texto == null || texto.isBlank()) {
			return "Cuentame tu duda legal y con gusto te oriento.";
		}

		List<Message> historial = historialDe(conversacion);
		if (historial.isEmpty()) {
			// Sin contexto previo: es exactamente la consulta suelta de responder(String).
			return responder(texto);
		}

		try {
			String respuesta = this.chatClient.prompt()
					.messages(historial)
					.user(texto)
					.call()
					.content();

			if (respuesta == null || respuesta.isBlank()) {
				log.warn("El modelo devolvio una respuesta vacia para la conversacion {}.",
						conversacion != null ? conversacion.getId() : null);
				return RESPUESTA_FALLBACK;
			}
			return respuesta.trim();
		}
		catch (Exception ex) {
			log.error("Error al consultar el modelo de IA con historial: {}", ex.getMessage(), ex);
			return RESPUESTA_FALLBACK;
		}
	}

	/**
	 * Version para cuando el tema viene del menu numerado y no hay una pregunta escrita
	 * por el cliente. Se convierte la etiqueta de la opcion en una consulta normal.
	 */
	public String responderPorTema(String tema) {
		if (tema == null || tema.isBlank()) {
			return RESPUESTA_FALLBACK;
		}
		return responder("El cliente eligio del menu la opcion: \"" + tema
				+ "\". Explicale ese punto de forma breve y concreta.");
	}

	/**
	 * Traduce los mensajes guardados en BD a mensajes de Spring AI. Si la lectura falla
	 * (BD caida, por ejemplo) se devuelve lista vacia: preferimos contestar sin memoria
	 * antes que no contestar.
	 */
	private List<Message> historialDe(Conversacion conversacion) {
		if (conversacion == null || conversacion.getId() == null) {
			return List.of();
		}
		try {
			return this.conversationService.obtenerHistorialReciente(conversacion).stream()
					.filter(m -> m.getContenido() != null && !m.getContenido().isBlank())
					.<Message>map(m -> "assistant".equalsIgnoreCase(m.getRol())
							? new AssistantMessage(m.getContenido())
							: new UserMessage(m.getContenido()))
					.toList();
		}
		catch (Exception ex) {
			log.warn("No se pudo leer el historial de la conversacion {}: {}",
					conversacion.getId(), ex.getMessage());
			return List.of();
		}
	}
}
