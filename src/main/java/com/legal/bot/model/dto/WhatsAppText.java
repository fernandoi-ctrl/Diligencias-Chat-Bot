package com.legal.bot.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Cuerpo de un mensaje de texto de WhatsApp: {@code {"body": "hola"}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppText(String body) {
}
