package com.legal.bot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.legal.bot.handler.LegalMessageHandler;
import com.legal.bot.service.LegalAIService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el contexto se levanta y que los beans clave existen.
 *
 * <p>El long polling se desactiva para no contactar a Telegram durante los tests; no se
 * invoca al modelo, asi que tampoco hace falta que Ollama este corriendo.</p>
 */
@SpringBootTest(properties = {
		"telegram.bot.enabled=false",
		"telegram.bot.token=token-de-prueba"
})
class LegalBotApplicationTests {

	@Autowired
	private LegalAIService legalAIService;

	@Autowired
	private LegalMessageHandler legalMessageHandler;

	@Test
	void contextLoads() {
		assertThat(this.legalAIService).isNotNull();
		assertThat(this.legalMessageHandler).isNotNull();
	}

	@Test
	void consultaVaciaNoLlamaAlModelo() {
		assertThat(this.legalAIService.responder("   ")).contains("duda legal");
	}

}
