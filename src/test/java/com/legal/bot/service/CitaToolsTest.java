package com.legal.bot.service;

import com.legal.bot.repository.CitaRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CitaToolsTest {

	private final WhatsAppService whatsAppService = mock(WhatsAppService.class);
	private final CitaRepository citaRepository = mock(CitaRepository.class);

	@Test
	void elMensajeNuncaDiceQueLaCitaEstaConfirmada() {
		CitaTools tools = new CitaTools(whatsAppService, citaRepository, "");

		String mensaje = tools.mensajeParaCliente("31 de octubre a las 4pm");

		assertThat(mensaje).contains("Solicitud recibida").contains("NO está confirmada");
		assertThat(mensaje).doesNotContain("agendada con éxito").doesNotContain("http");
	}

	@Test
	void incluyeElEnlaceDeReservasCuandoEstaConfigurado() {
		CitaTools tools = new CitaTools(whatsAppService, citaRepository, "https://calendar.example/reservas");

		assertThat(tools.mensajeParaCliente("mañana a las 10am"))
				.contains("https://calendar.example/reservas");
	}
}
