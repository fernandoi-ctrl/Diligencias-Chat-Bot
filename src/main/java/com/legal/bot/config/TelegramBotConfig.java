package com.legal.bot.config;

import com.legal.bot.handler.LegalMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registro manual del bot en modo LONG POLLING.
 *
 * <p>Se hace a mano (sin starter de Spring) a proposito: el unico starter oficial,
 * {@code org.telegram:telegrambots-spring-boot-starter}, quedo congelado en la version
 * 6.9.7.1 (Bot API 6.9) y su autoconfiguracion no funciona en Spring Boot 4.x. Usando
 * directamente {@code telegrambots-longpolling} el proyecto queda desacoplado de la
 * version de Spring Boot: aqui solo declaramos dos beans.</p>
 */
@Configuration
public class TelegramBotConfig {

	private static final Logger log = LoggerFactory.getLogger(TelegramBotConfig.class);

	/**
	 * Cliente usado para enviar mensajes (sendMessage, sendChatAction, etc.).
	 */
//	@Bean
//	public TelegramClient telegramClient(TelegramBotProperties properties) {
//		return new OkHttpTelegramClient(properties.token());
//	}

	/**
	 * Arranca la sesion de long polling y la cierra limpiamente al apagar la app.
	 *
	 * <p>Telegram solo admite un consumidor de updates por token: si el bot ya corre en
	 * otra terminal, veras errores 409 Conflict en el log.</p>
	 */
//	@Bean(destroyMethod = "close")
//	@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = true)
//	public TelegramBotsLongPollingApplication telegramBotsApplication(TelegramBotProperties properties,
//			LegalMessageHandler handler) throws Exception {
//
//		if (properties.token() == null || properties.token().isBlank()) {
//			throw new IllegalStateException("""
//					Falta el token de Telegram. Exporta la variable de entorno TELEGRAM_BOT_TOKEN \
//					con el token que te dio BotFather, o arranca con -Dtelegram.bot.enabled=false \
//					para levantar la aplicacion sin el bot.""");
//		}
//
//		TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
//		botsApplication.registerBot(properties.token(), handler);
//		log.info("Bot legal registrado en modo LONG POLLING. Escribele desde Telegram.");
//		return botsApplication;
//	}

}
