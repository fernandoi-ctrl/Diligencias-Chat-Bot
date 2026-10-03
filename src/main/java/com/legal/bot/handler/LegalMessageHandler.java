package com.legal.bot.handler;

import com.legal.bot.service.LegalAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Recibe los updates de Telegram (long polling) y contesta usando {@link LegalAIService}.
 *
 * <p>Implementa {@code LongPollingSingleThreadUpdateConsumer}, que procesa cada update en
 * un hilo del pool de la libreria: una consulta lenta al modelo local no bloquea a los
 * demas usuarios.</p>
 */
@Component
public class LegalMessageHandler implements LongPollingSingleThreadUpdateConsumer {

	private static final Logger log = LoggerFactory.getLogger(LegalMessageHandler.class);

	@Override
	public void consume(Update update) {
		// TODO Auto-generated method stub
		
	}

	/** Limite duro de la API de Telegram por mensaje. */
//	private static final int MAX_CARACTERES_TELEGRAM = 4096;
//
//	private static final String MENSAJE_BIENVENIDA = """
//			Bienvenido al Despacho Legal Aguascalientes.
//
//			Puedo orientarte sobre:
//			- Costos de diligencias
//			- Materias que llevamos (civil, familiar, laboral, mercantil, penal y administrativa)
//			- Horario de atencion
//
//			Escribe tu duda legal y te respondo.""";
//
//	private final TelegramClient telegramClient;
//
//	private final LegalAIService legalAIService;
//
//	public LegalMessageHandler(TelegramClient telegramClient, LegalAIService legalAIService) {
//		this.telegramClient = telegramClient;
//		this.legalAIService = legalAIService;
//	}
//
//	@Override
//	public void consume(Update update) {
//		// Ignoramos todo lo que no sea un mensaje de texto (fotos, stickers, ediciones...).
//		if (!update.hasMessage() || !update.getMessage().hasText()) {
//			return;
//		}
//
//		Message mensaje = update.getMessage();
//		String chatId = String.valueOf(mensaje.getChatId());
//		String texto = mensaje.getText().trim();
//
//		log.info("Consulta recibida del chat {}: {}", chatId, texto);
//
//		// Los comandos se contestan localmente, sin gastar una llamada al modelo.
//		if (texto.startsWith("/start") || texto.startsWith("/help") || texto.startsWith("/ayuda")) {
//			enviarTexto(chatId, MENSAJE_BIENVENIDA);
//			return;
//		}
//
//		// "Escribiendo..." en Telegram: el modelo local puede tardar varios segundos.
//		mostrarEscribiendo(chatId);
//
//		String respuesta = this.legalAIService.responder(texto);
//		enviarTexto(chatId, respuesta);
//	}
//
//	/** Envia la respuesta, partiendola si excede el limite de Telegram. */
//	private void enviarTexto(String chatId, String texto) {
//		for (int inicio = 0; inicio < texto.length(); inicio += MAX_CARACTERES_TELEGRAM) {
//			int fin = Math.min(inicio + MAX_CARACTERES_TELEGRAM, texto.length());
//			SendMessage envio = SendMessage.builder()
//					.chatId(chatId)
//					.text(texto.substring(inicio, fin))
//					.build();
//			try {
//				this.telegramClient.execute(envio);
//			}
//			catch (TelegramApiException ex) {
//				log.error("No se pudo enviar el mensaje al chat {}: {}", chatId, ex.getMessage(), ex);
//				return;
//			}
//		}
//	}
//
//	/** Indicador de "escribiendo"; si falla no interrumpe la respuesta. */
//	private void mostrarEscribiendo(String chatId) {
//		try {
//			this.telegramClient.execute(SendChatAction.builder()
//					.chatId(chatId)
//					.action("typing")
//					.build());
//		}
//		catch (TelegramApiException ex) {
//			log.debug("No se pudo enviar la accion 'typing' al chat {}: {}", chatId, ex.getMessage());
//		}
//	}

}
