package com.legal.bot.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite simple en memoria por IP para la API publica (anti-spam). Al ser por
 * instancia, se reinicia con cada despliegue y no se comparte entre instancias;
 * suficiente para un solo servicio en Render.
 */
@Component
public class LimiteDeSolicitudes {

    static final int MAXIMO = 5;
    static final Duration VENTANA = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();
    private final Clock clock;

    public LimiteDeSolicitudes() {
        this(Clock.systemUTC());
    }

    LimiteDeSolicitudes(Clock clock) {
        this.clock = clock;
    }

    /** Registra el intento y devuelve false si la IP ya agoto su cupo en la ventana. */
    public boolean permitir(String ip) {
        Instant ahora = clock.instant();
        Instant limite = ahora.minus(VENTANA);
        boolean[] permitido = {false};
        intentos.compute(ip, (k, cola) -> {
            Deque<Instant> c = cola != null ? cola : new ArrayDeque<>();
            while (!c.isEmpty() && c.peekFirst().isBefore(limite)) c.pollFirst();
            if (c.size() < MAXIMO) {
                c.addLast(ahora);
                permitido[0] = true;
            }
            return c;
        });
        // Limpieza perezosa para que el mapa no crezca sin limite.
        intentos.values().removeIf(c -> c.isEmpty() || c.peekLast().isBefore(limite));
        return permitido[0];
    }
}
