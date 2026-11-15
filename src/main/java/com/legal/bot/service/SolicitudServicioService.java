package com.legal.bot.service;

import com.legal.bot.model.dto.SolicitudServicioRequest;
import com.legal.bot.model.entity.Lead;
import com.legal.bot.model.entity.SolicitudServicio;
import com.legal.bot.repository.LeadRepository;
import com.legal.bot.repository.SolicitudServicioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SolicitudServicioService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudServicioService.class);

    private final LeadRepository leadRepository;
    private final SolicitudServicioRepository solicitudRepository;

    public SolicitudServicioService(LeadRepository leadRepository, SolicitudServicioRepository solicitudRepository) {
        this.leadRepository = leadRepository;
        this.solicitudRepository = solicitudRepository;
    }

    /** Guarda el prospecto y su solicitud juntos. Devuelve la solicitud creada. */
    @Transactional
    public SolicitudServicio registrar(SolicitudServicioRequest req) {
        Lead lead = new Lead();
        lead.setFullName(req.nombre().trim());
        lead.setFirmName(limpiar(req.despacho()));
        lead.setEmail(limpiar(req.email()));
        lead.setPhone(soloDigitos(req.telefono()));
        lead.setSource("web");
        lead.setUtmSource(limpiar(req.utmSource()));
        lead.setUtmCampaign(limpiar(req.utmCampaign()));
        lead.setConsentAt(Instant.now());
        leadRepository.save(lead);

        SolicitudServicio solicitud = new SolicitudServicio();
        solicitud.setLead(lead);
        solicitud.setServiceCode(req.servicio().name());
        solicitud.setLegalArea(limpiar(req.materia()));
        solicitud.setCourtName(limpiar(req.juzgado()));
        solicitud.setMunicipality(limpiar(req.municipio()));
        solicitud.setRequestedDate(req.fechaTentativa());
        solicitud.setDescription(limpiar(req.descripcion()));
        solicitudRepository.save(solicitud);

        // Sin datos personales en el log: solo ids y el servicio.
        log.info("Nueva solicitud {} (lead {}) de servicio {}", solicitud.getId(), lead.getId(), solicitud.getServiceCode());
        return solicitud;
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String soloDigitos(String telefono) {
        String limpio = limpiar(telefono);
        return limpio == null ? null : limpio.replaceAll("\\D", "");
    }
}
