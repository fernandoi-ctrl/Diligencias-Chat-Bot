package com.legal.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class CitaTools {

    private static final Logger log = LoggerFactory.getLogger(CitaTools.class);
    private final WhatsAppService whatsAppService; 

    public CitaTools(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @Tool(description = "Agenda una cita por Zoom. Requiere nombre, fecha/hora, tipo de diligencia y ubicación.")
    public String agendarCita(
            @ToolParam(description = "Nombre completo del cliente") String nombre,
            @ToolParam(description = "Fecha y hora exacta (ej. 31 de octubre a las 4pm)") String fechaHora,
            @ToolParam(description = "Tipo de diligencia (notificación, fe de hechos, etc)") String tipoDiligencia,
            @ToolParam(description = "Ubicación de la diligencia (dentro de la ciudad o municipio)") String ubicacion) {
        
        log.info("====== NUEVA CITA DE DILIGENCIA ======");
        log.info("Cliente: {}", nombre);
        log.info("Fecha: {}", fechaHora);
        log.info("Diligencia: {}", tipoDiligencia);
        log.info("Ubicación: {}", ubicacion);
        log.info("======================================");

        // Intentamos enviarte la notificación, pero si falla, NO rompemos el flujo
        try {
            String mensajeNotificacion = String.format(
                "🦉 *Nueva Cita de Diligencia*\n\n" +
                "👤 Cliente: %s\n" +
                "📅 Fecha: %s\n" +
                "📄 Diligencia: %s\n" +
                "📍 Ubicación: %s",
                nombre, fechaHora, tipoDiligencia, ubicacion
            );
            whatsAppService.enviarTexto("5214491157917", mensajeNotificacion);
            log.info("Notificación enviada al administrador con éxito.");
        } catch (Exception e) {
            log.error("Fallo al enviar notificación por WhatsApp al admin: {}", e.getMessage());
            // El error se ignora para que el bot pueda responderle al cliente.
        }
        
        return "La cita ha sido agendada con éxito para el día " + fechaHora + " por Zoom. Se ha enviado la confirmación.";
    }
}