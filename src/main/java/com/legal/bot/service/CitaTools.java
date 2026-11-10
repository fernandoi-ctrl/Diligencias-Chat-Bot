package com.legal.bot.service;

import com.legal.bot.model.entity.Cita;
import com.legal.bot.repository.CitaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class CitaTools {

    private static final Logger log = LoggerFactory.getLogger(CitaTools.class);
    private final WhatsAppService whatsAppService;
    private final CitaRepository citaRepository;

    public CitaTools(WhatsAppService whatsAppService, CitaRepository citaRepository) {
        this.whatsAppService = whatsAppService;
        this.citaRepository = citaRepository;
    }

    @Tool(description = "Agenda una cita por Zoom. Requiere nombre, fecha/hora, tipo de diligencia y ubicación.")
    public String agendarCita(
            @ToolParam(description = "Nombre completo del cliente") String nombre,
            @ToolParam(description = "Fecha y hora exacta (ej. 31 de octubre a las 4pm)") String fechaHora,
            @ToolParam(description = "Tipo de diligencia (notificación, fe de hechos, etc)") String tipoDiligencia,
            @ToolParam(description = "Ubicación de la diligencia (dentro de la ciudad o municipio)") String ubicacion,
            ToolContext toolContext) { // <-- Inyectamos el contexto para obtener el teléfono
        
        // Extraemos el teléfono del contexto que enviaremos desde LegalAIService
        String telefono = toolContext.getContext().getOrDefault("telefono", "Desconocido").toString();

        log.info("====== NUEVA CITA DE DILIGENCIA ======");
        log.info("Cliente: {} ({})", nombre, telefono);
        log.info("Fecha: {}", fechaHora);
        log.info("Diligencia: {}", tipoDiligencia);
        log.info("Ubicación: {}", ubicacion);
        log.info("======================================");

        // 1. GUARDAR EN LA BASE DE DATOS
        try {
            Cita nuevaCita = Cita.builder()
                    .telefonoCliente(telefono)
                    .nombreCliente(nombre)
                    .fechaHoraSolicitada(fechaHora)
                    .tipoDiligencia(tipoDiligencia)
                    .ubicacion(ubicacion)
                    .estado("PENDIENTE")
                    .build();
            citaRepository.save(nuevaCita);
            log.info("Cita guardada en la base de datos exitosamente.");
        } catch (Exception e) {
            log.error("Error guardando la cita en BD: {}", e.getMessage());
        }

        // 2. NOTIFICAR AL ADMINISTRADOR POR WHATSAPP
        try {
            String mensajeNotificacion = String.format(
                "🦉 *Nueva Cita de Diligencia*\n\n" +
                "👤 Cliente: %s\n" +
                "📱 Tel: %s\n" +
                "📅 Fecha: %s\n" +
                "📄 Diligencia: %s\n" +
                "📍 Ubicación: %s",
                nombre, telefono, fechaHora, tipoDiligencia, ubicacion
            );
            whatsAppService.enviarTexto("5214491157917", mensajeNotificacion);
            log.info("Notificación enviada al administrador con éxito.");
        } catch (Exception e) {   
            log.error("Fallo al enviar notificación por WhatsApp: {}", e.getMessage());
        }
        
        return "La cita ha sido agendada con éxito en el sistema para el día " + fechaHora + " por Zoom.";
    }
}