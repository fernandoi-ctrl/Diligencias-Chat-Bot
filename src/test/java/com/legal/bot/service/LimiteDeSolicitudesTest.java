package com.legal.bot.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteDeSolicitudesTest {

	/** Reloj que se puede adelantar a mano. */
	static class RelojManual extends Clock {
		Instant ahora = Instant.parse("2026-11-13T12:00:00Z");

		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return ahora; }
	}

	@Test
	void bloqueaAlSuperarElMaximoYSeLiberaAlPasarLaVentana() {
		RelojManual reloj = new RelojManual();
		LimiteDeSolicitudes limite = new LimiteDeSolicitudes(reloj);

		for (int i = 0; i < LimiteDeSolicitudes.MAXIMO; i++) {
			assertThat(limite.permitir("1.1.1.1")).isTrue();
		}
		assertThat(limite.permitir("1.1.1.1")).isFalse();
		assertThat(limite.permitir("2.2.2.2")).as("otra IP no se ve afectada").isTrue();

		reloj.ahora = reloj.ahora.plus(LimiteDeSolicitudes.VENTANA).plusSeconds(1);
		assertThat(limite.permitir("1.1.1.1")).isTrue();
	}
}
