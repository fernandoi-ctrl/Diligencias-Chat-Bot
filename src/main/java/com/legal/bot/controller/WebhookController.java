package com.legal.bot.controller;

import com.legal.bot.config.WhatsAppProperties;
import com.legal.bot.handler.WhatsAppMessageHandler;
import com.legal.bot.model.dto.WhatsAppWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook de la WhatsApp Cloud API.
 *
 * <p>Meta usa el mismo path para dos cosas distintas:</p>
 * <ul>
 *   <li><b>GET</b>: verificacion. Al dar de alta la URL en el panel de Meta, llega un GET
 *       con {@code hub.verify_token}; si coincide con el nuestro hay que devolver
 *       {@code hub.challenge} en texto plano.</li>
 *   <li><b>POST</b>: mensajes y acuses de entrega.</li>
 * </ul>
 *
 * <p>El POST responde <b>200 de inmediato</b> y delega el trabajo real en
 * {@link WhatsAppMessageHandler}, que corre en otro hilo. Es un requisito de Meta: si no
 * contestamos rapido, reintenta el envio y el cliente recibiria respuestas repetidas.
 * Tambien se devuelve 200 ante un cuerpo inesperado, porque un error hace que Meta
 * reintente y, si se repite, que desactive la suscripcion del webhook.</p>
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

	private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

	private final WhatsAppProperties properties;

	private final WhatsAppMessageHandler handler;

	public WebhookController(WhatsAppProperties properties, WhatsAppMessageHandler handler) {
		this.properties = properties;
		this.handler = handler;
	}
	// produces=text/plain explicito: Meta espera el challenge tal cual, sin comillas de
	// JSON. Con ResponseEntity<String> Spring ya suele elegir text/plain, pero dejarlo
	// fijado evita que un Accept raro haga que se negocie otro tipo.
	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> verificar(@RequestParam(name = "hub.mode", required = false) String modo,
			@RequestParam(name = "hub.verify_token", required = false) String token,
			@RequestParam(name = "hub.challenge", required = false) String challenge) {

		// Comparacion exacta y sensible a mayusculas, que es lo que hace Meta.
		if ("subscribe".equals(modo) && this.properties.getVerifyToken() != null
				&& this.properties.getVerifyToken().equals(token)) {
			log.info("Webhook de WhatsApp verificado correctamente por Meta.");
			return ResponseEntity.ok(challenge);
		}

		log.warn("Verificacion de webhook rechazada (mode={}). Revisa WHATSAPP_VERIFY_TOKEN.", modo);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token de verificacion invalido");
	}

	/**
	 * Recibe los eventos de WhatsApp. Siempre 200, y el procesamiento va aparte.
	 *
	 * <p>El {@code try/catch} envuelve solo la entrega al handler: si algo revienta al
	 * aplanar el sobre, se registra pero se sigue devolviendo 200, porque un 5xx
	 * repetido hace que Meta desactive la suscripcion del webhook.</p>
	 */
	@PostMapping
	public ResponseEntity<Void> recibir(@RequestBody(required = false) WhatsAppWebhookPayload payload) {
		// Log a INFO y no a DEBUG: es la primera senal de que Meta esta llegando hasta
		// aqui, y es justo lo que hay que ver en la consola al probar el bot.
		log.info("Webhook recibido de WhatsApp (object={}).", payload != null ? payload.object() : null);

		if (payload == null) {
			log.warn("Webhook con cuerpo vacio o no deserializable; se responde 200 igualmente.");
			return ResponseEntity.ok().build();
		}

		var mensajes = payload.mensajes();
		if (mensajes.isEmpty()) {
			// Caso normalisimo: son los acuses de entrega (sent/delivered/read).
			log.info("Webhook sin mensajes entrantes (probablemente un status); nada que hacer.");
			return ResponseEntity.ok().build();
		}

		log.info("Webhook con {} mensaje(s) entrante(s); se delega en el handler.", mensajes.size());
		try {
			this.handler.procesar(payload);
		}
		catch (Exception ex) {
			log.error("No se pudo encolar el procesamiento del webhook: {}", ex.getMessage(), ex);
		}
		return ResponseEntity.ok().build();
	}
}
