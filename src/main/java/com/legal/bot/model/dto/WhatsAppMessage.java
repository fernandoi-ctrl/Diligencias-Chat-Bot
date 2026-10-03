package com.legal.bot.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Un mensaje entrante tal como lo manda la Cloud API dentro de {@code value.messages[]}.
 *
 * <p>Solo se mapean los campos que usa esta fase. {@code type} es clave para la logica de
 * "responder o no responder": en la fase 1 unicamente se contesta a {@code text}, y los
 * audios, imagenes o documentos se reconocen pero no se procesan.</p>
 *
 * @param from      telefono del cliente en formato internacional sin '+', p.ej. 5214491234567
 * @param id        identificador unico del mensaje (wamid...), usado para no duplicar
 * @param timestamp epoch en segundos, como cadena
 * @param type      text, audio, image, document, sticker, interactive...
 * @param text      presente solo cuando {@code type} es {@code text}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppMessage(String from, String id, String timestamp, String type, WhatsAppText text) {

	public boolean esTexto() {
		return "text".equalsIgnoreCase(this.type) && this.text != null && this.text.body() != null
				&& !this.text.body().isBlank();
	}

	/** Texto sin espacios sobrantes, o cadena vacia si el mensaje no es de texto. */
	public String contenido() {
		return esTexto() ? this.text.body().trim() : "";
	}

}
