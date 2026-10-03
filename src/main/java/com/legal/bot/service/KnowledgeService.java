package com.legal.bot.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Carga {@code conocimiento-despacho.json} al arrancar y lo deja disponible en memoria.
 *
 * <p>En esta fase sirve para armar el mensaje de bienvenida con el menu, de forma que los
 * datos del despacho (costos, materias, horario) se editen en un solo archivo y no
 * repartidos por el codigo. En la fase de RAG este mismo servicio sera el que alimente el
 * contexto del modelo.</p>
 */
@Service
public class KnowledgeService {

	private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

	private static final String ARCHIVO = "conocimiento-despacho.json";

	private final Map<String, Object> conocimiento;

	/**
	 * Spring Boot 4 usa Jackson 3, cuyo mapper vive en {@code tools.jackson.databind}.
	 * El bean autoconfigurado es un {@code JsonMapper}, que extiende {@link ObjectMapper}.
	 */
	public KnowledgeService(ObjectMapper objectMapper) {
		this.conocimiento = cargar(objectMapper);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> cargar(ObjectMapper objectMapper) {
		try (InputStream in = new ClassPathResource(ARCHIVO).getInputStream()) {
			Map<String, Object> datos = objectMapper.readValue(in, Map.class);
			log.info("Conocimiento del despacho cargado desde {} ({} secciones).", ARCHIVO, datos.size());
			return datos;
		}
		catch (Exception ex) {
			// No se aborta el arranque: el SYSTEM_PROMPT de LegalAIService ya trae los
			// datos, asi que el bot puede seguir atendiendo sin el menu.
			log.error("No se pudo leer {}: {}. Se usara el menu por defecto.", ARCHIVO, ex.getMessage());
			return Map.of();
		}
	}

	/** Copia de solo lectura del JSON completo. */
	public Map<String, Object> datos() {
		return this.conocimiento;
	}

	/**
	 * Mensaje de bienvenida con el menu numerado que viene del JSON.
	 */
	public String mensajeBienvenida() {
		StringBuilder texto = new StringBuilder("Bienvenido a ")
				.append(valor("despacho", "nombre", "nuestro despacho"))
				.append(".\n\n¿En que te podemos ayudar?\n");

		List<Map<String, Object>> menu = menuOpciones();
		if (menu.isEmpty()) {
			texto.append("\nEscribe tu duda legal y te oriento.");
			return texto.toString();
		}

		for (Map<String, Object> opcion : menu) {
			texto.append("\n").append(opcion.get("opcion")).append(". ").append(opcion.get("texto"));
		}
		texto.append("\n\nResponde con el numero o escribe tu duda legal directamente.");
		return texto.toString();
	}

	/** Texto de la opcion elegida, o {@code null} si ese numero no esta en el menu. */
	public String textoDeOpcion(String numero) {
		return menuOpciones().stream()
				.filter(o -> numero.equals(String.valueOf(o.get("opcion"))))
				.map(o -> String.valueOf(o.get("texto")))
				.findFirst()
				.orElse(null);
	}

	/** Horario de atencion, para respuestas rapidas sin pasar por el modelo. */
	public String horario() {
		return valor("despacho", "horario", "Lunes a Viernes 9:00 AM - 7:00 PM");
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> menuOpciones() {
		Object menu = this.conocimiento.get("menu_opciones");
		return menu instanceof List<?> lista ? (List<Map<String, Object>>) lista : List.of();
	}

	@SuppressWarnings("unchecked")
	private String valor(String seccion, String clave, String porDefecto) {
		Object bloque = this.conocimiento.get(seccion);
		if (bloque instanceof Map<?, ?> mapa) {
			Object valor = ((Map<String, Object>) mapa).get(clave);
			if (valor != null) {
				return String.valueOf(valor);
			}
		}
		return porDefecto;
	}
}
