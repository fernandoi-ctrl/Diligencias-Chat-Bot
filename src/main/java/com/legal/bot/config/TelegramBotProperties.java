package com.legal.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del bot, enlazadas desde el prefijo {@code telegram.bot}.
 *
 * @param token   token entregado por BotFather (se inyecta desde TELEGRAM_BOT_TOKEN)
 * @param enabled permite desactivar el long polling (util en tests o desarrollo local)
 */
//@ConfigurationProperties(prefix = "telegram.bot")
//public record TelegramBotProperties(String token, boolean enabled) {
//}
