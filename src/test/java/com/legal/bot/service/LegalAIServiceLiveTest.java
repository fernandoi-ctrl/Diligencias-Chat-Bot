package com.legal.bot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de extremo a extremo contra un Ollama real.
 *
 * <p>Esta desactivada por defecto para que {@code mvn test} no dependa de Ollama.
 * Para ejecutarla (con Ollama corriendo y el modelo descargado):</p>
 *
 * <pre>mvnw test -Dollama.live=true -Dtest=LegalAIServiceLiveTest</pre>
 */
@SpringBootTest(properties = "telegram.bot.enabled=false")
@EnabledIfSystemProperty(named = "ollama.live", matches = "true")
class LegalAIServiceLiveTest {

	@Autowired
	private LegalAIService legalAIService;

	@Test
	void respondeElCostoDeDiligenciaEnLaCiudad() {
		String respuesta = this.legalAIService.responder("¿Cuánto cuesta una diligencia en la ciudad de Aguascalientes?");

		System.out.println("[RESPUESTA DEL MODELO] " + respuesta);
		// Debe citar la cifra exacta del system prompt, no inventarla.
		assertThat(respuesta).containsAnyOf("1,000", "1000");
	}

	@Test
	void confirmaLasMateriasQueSeLlevan() {
		String respuesta = this.legalAIService.responder("¿Llevan casos de pensión alimenticia?");

		System.out.println("[RESPUESTA DEL MODELO] " + respuesta);
		assertThat(respuesta).isNotBlank();
	}

}
