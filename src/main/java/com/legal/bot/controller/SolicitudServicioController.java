package com.legal.bot.controller;

import com.legal.bot.model.dto.SolicitudServicioRequest;
import com.legal.bot.service.LimiteDeSolicitudes;
import com.legal.bot.service.SolicitudServicioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API publica de la landing. Contrato documentado en docs/api.md.
 * La respuesta nunca confirma el servicio: solo que la solicitud se recibio.
 */
@RestController
@RequestMapping("/api/v1/service-requests")
public class SolicitudServicioController {

    private static final Logger log = LoggerFactory.getLogger(SolicitudServicioController.class);
    private static final Map<String, String> RECIBIDA = Map.of("estado", "RECIBIDA");

    private final SolicitudServicioService service;
    private final LimiteDeSolicitudes limite;

    public SolicitudServicioController(SolicitudServicioService service, LimiteDeSolicitudes limite) {
        this.service = service;
        this.limite = limite;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> crear(@Valid @RequestBody SolicitudServicioRequest req,
            HttpServletRequest http) {
        if (!limite.permitir(ipCliente(http))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Demasiadas solicitudes. Intente de nuevo en unos minutos."));
        }
        // Honeypot lleno = bot. Se responde igual que un exito para no darle pistas.
        if (req.website() != null && !req.website().isBlank()) {
            log.info("Solicitud descartada por el campo trampa.");
            return ResponseEntity.status(HttpStatus.CREATED).body(RECIBIDA);
        }
        service.registrar(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(RECIBIDA);
    }

    // Render pone la IP real del visitante en X-Forwarded-For.
    private static String ipCliente(HttpServletRequest http) {
        String reenviada = http.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
