package com.legal.bot.model.dto;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sobre completo del webhook de WhatsApp Cloud API.
 *
 * <p>Meta anida el contenido en {@code entry[].changes[].value.messages[]} y en el mismo
 * endpoint envia tambien acuses de entrega ({@code value.statuses}), que aqui se ignoran.
 * Los records anidados existen solo para desenvolver esa estructura; el mensaje util es
 * {@link WhatsAppMessage}.</p>
 *
 * <p>Los campos que Meta manda en snake_case llevan {@link JsonProperty} explicito, porque
 * la estrategia de nombres por defecto de Jackson es camelCase.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayload(String object, List<Entry> entry) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Entry(String id, List<Change> changes) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Change(String field, Value value) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Value(@JsonProperty("messaging_product") String messagingProduct, Metadata metadata,
			List<Contact> contacts, List<WhatsAppMessage> messages) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Metadata(@JsonProperty("display_phone_number") String displayPhoneNumber,
			@JsonProperty("phone_number_id") String phoneNumberId) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Contact(@JsonProperty("wa_id") String waId, Profile profile) {

		/** Nombre que el cliente tiene puesto en su perfil de WhatsApp, si lo comparte. */
		public String nombre() {
			return this.profile != null ? this.profile.name() : null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Profile(String name) {
	}

	/**
	 * Aplana el sobre y devuelve solo los mensajes entrantes.
	 *
	 * <p>Un mismo POST puede traer varios {@code entry}/{@code changes}, aunque en la
	 * practica casi siempre llega uno.</p>
	 */
	public List<WhatsAppMessage> mensajes() {
		return lista(this.entry).stream()
				.flatMap(e -> lista(e.changes()).stream())
				.map(Change::value)
				.filter(v -> v != null)
				.flatMap(v -> lista(v.messages()).stream())
				.filter(m -> m != null && m.id() != null)
				.toList();
	}

	/**
	 * Nombre de perfil asociado a un telefono, si viene en {@code contacts[]}.
	 */
	public Optional<String> nombreDe(String telefono) {
		return lista(this.entry).stream()
				.flatMap(e -> lista(e.changes()).stream())
				.map(Change::value)
				.filter(v -> v != null)
				.flatMap(v -> lista(v.contacts()).stream())
				.filter(c -> c != null && telefono.equals(c.waId()))
				.map(Contact::nombre)
				.filter(n -> n != null && !n.isBlank())
				.findFirst();
	}

	private static <T> List<T> lista(List<T> valor) {
		return valor != null ? valor : List.of();
	}
}
