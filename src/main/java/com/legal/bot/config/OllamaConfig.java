package com.legal.bot.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Comprobacion de arranque del modelo local.
 *
 * <p>La conexion con Ollama la autoconfigura Spring AI (cliente compatible OpenAI apuntado
 * a {@code /v1}); aqui no se redefine nada de eso. Lo unico que hace esta clase es
 * consultar {@code /api/tags} una vez que la app esta lista, para que un Ollama apagado o
 * un modelo sin descargar se vea en el log al arrancar y no como un fallo raro en la
 * primera consulta de un cliente.</p>
 */
@Configuration
public class OllamaConfig {

	private static final Logger log = LoggerFactory.getLogger(OllamaConfig.class);

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private final String baseUrl;

	private final String modelo;

	public OllamaConfig(@Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
			@Value("${spring.ai.openai.chat.options.model:}") String modelo) {
		this.baseUrl = baseUrl;
		this.modelo = modelo;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void verificarOllama() {
		// HttpClient no es AutoCloseable en Java 17, asi que no va en try-with-resources.
		HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(this.baseUrl + "/api/tags"))
					.timeout(TIMEOUT)
					.GET()
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				log.warn("Ollama respondio {} en {}/api/tags.", response.statusCode(), this.baseUrl);
				return;
			}

			// Busqueda por texto: basta para saber si el tag esta descargado y evita
			// parsear la respuesta completa de /api/tags.
			if (!this.modelo.isBlank() && !response.body().contains("\"" + this.modelo + "\"")) {
				log.warn("Ollama esta arriba pero el modelo '{}' no aparece descargado. Ejecuta: ollama pull {}",
						this.modelo, this.modelo);
				return;
			}
			log.info("Ollama disponible en {} con el modelo '{}'.", this.baseUrl, this.modelo);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (Exception ex) {
			log.warn("""
					No se pudo contactar a Ollama en {}. Arrancalo con 'ollama serve'; \
					mientras tanto el bot contestara con el mensaje de fallback. Detalle: {}""",
					this.baseUrl, ex.getMessage());
		}
	}

}
