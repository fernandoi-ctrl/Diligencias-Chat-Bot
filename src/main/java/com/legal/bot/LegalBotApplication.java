package com.legal.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del bot legal.
 *
 * <p>El bot NO usa webhook: al arrancar, {@code TelegramBotConfig} abre una sesion de
 * long polling contra la API de Telegram, por lo que no se necesita URL publica.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LegalBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegalBotApplication.class, args);
	}

}
