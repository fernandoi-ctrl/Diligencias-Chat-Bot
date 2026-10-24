package com.legal.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.legal.bot.config.WhatsAppProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Envio de mensajes a traves de la Cloud API de WhatsApp (Graph API de Meta).
 *
 * <p>Usa el {@code WebClient} que declara {@code WhatsAppConfig}, que ya trae los timeouts
 * y la cabecera {@code Authorization}. Antes se inyectaba un {@code WebClient.Builder}
 * generico, con lo que el bean configurado quedaba sin usar y se perdian los timeouts.</p>
 */
@Service
public class WhatsAppService{

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final WebClient webClient;
    private final WhatsAppProperties props;

    public WhatsAppService(@Qualifier("whatsAppWebClient") WebClient webClient, WhatsAppProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    /** Envía un texto y devuelve el wamid asignado por Meta, si lo hubo. */
    public Optional<String> enviarTexto(String telefono, String texto) {
        if (!this.props.estaConfigurado()) {
            log.error("""
                    No se envia el mensaje a {}: falta WHATSAPP_ACCESS_TOKEN y/o \
                    WHATSAPP_PHONE_NUMBER_ID en el entorno.""", telefono);
            return Optional.empty();
        }

        String url = this.props.messagesUrl();
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", telefono,
                "type", "text",
                "text", Map.of("body", texto)
        );

        log.info("Enviando texto a {} via {} ({} caracteres).", telefono, url, texto.length());

        try {
            Map<?, ?> response = this.webClient.post()
                    .uri(url)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Meta acepto el envio a {}. Respuesta: {}", telefono, response);

            if (response != null && response.get("messages") instanceof List<?> lista
                    && !lista.isEmpty() && lista.get(0) instanceof Map<?, ?> primero) {
                Object id = primero.get("id");
                if (id != null) {
                    return Optional.of(id.toString());
                }
            }
            log.warn("Meta respondio 2xx pero sin wamid para {}. Cuerpo: {}", telefono, response);
            return Optional.empty();
        }
        catch (WebClientResponseException ex) {
            // El cuerpo del error de Meta es lo unico que dice la causa real:
            // token caducado, numero fuera de la lista de prueba, ventana de 24h cerrada...
            log.error("Meta rechazo el envio a {}: HTTP {} {}. Cuerpo: {}",
                    telefono, ex.getStatusCode().value(), ex.getStatusText(),
                    ex.getResponseBodyAsString(), ex);
            return Optional.empty();
        }
        catch (Exception ex) {
            log.error("Fallo de red al enviar el mensaje a {}: {}", telefono, ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    /** Marca un mensaje entrante como leído (doble check azul). No es critico si falla. */
    public void marcarComoLeido(String waMessageId) {
        if (!this.props.estaConfigurado()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "status", "read",
                "message_id", waMessageId
        );

        try {
            this.webClient.post()
                    .uri(this.props.messagesUrl())
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }
        catch (WebClientResponseException ex) {
            log.warn("No se pudo marcar {} como leido: HTTP {}. Cuerpo: {}",
                    waMessageId, ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
        catch (Exception ex) {
            log.warn("No se pudo marcar {} como leido: {}", waMessageId, ex.getMessage());
        }
    }
}
