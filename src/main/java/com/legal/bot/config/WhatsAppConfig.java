package com.legal.bot.config;

import java.time.Duration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Cliente HTTP para la WhatsApp Cloud API (Graph API de Meta).
 *
 * <p>Se registra un {@link WebClient} dedicado y no el generico: lleva ya la base-url del
 * numero emisor y la cabecera {@code Authorization}, para que {@code WhatsAppService} solo
 * tenga que construir el cuerpo JSON.</p>
 */
@Configuration
public class WhatsAppConfig {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppConfig.class);

	/** Graph API responde rapido; si tarda mas de esto es que algo va mal. */
	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	@Bean
	public WebClient whatsAppWebClient(WhatsAppProperties properties) {
		if (!properties.estaConfigurado()) {
			// No se lanza excepcion a proposito: el bot de Telegram debe poder seguir
			// funcionando aunque WhatsApp no este dado de alta todavia.
			log.warn("""
					WhatsApp sin configurar: faltan WHATSAPP_ACCESS_TOKEN y/o \
					WHATSAPP_PHONE_NUMBER_ID. El webhook seguira aceptando mensajes pero \
					las respuestas no se enviaran.""");
		}

		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TIMEOUT.toMillis())
				.responseTimeout(TIMEOUT)
				.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler((int) TIMEOUT.toSeconds())));

		// Si falta configuracion, la base-url se deja en un valor inocuo: WhatsAppService
		// comprueba estaConfigurado() antes de usar el cliente, asi que nunca se llama.
		String baseUrl = properties.estaConfigurado() ? properties.messagesUrl() : "https://graph.facebook.com/v21.0/sin-configurar/messages";

		return WebClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (properties.accessToken() != null ? properties.accessToken() : ""))
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
				.build();
	}

}
