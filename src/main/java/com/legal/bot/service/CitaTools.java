//package com.legal.bot.service;
//
//import org.springframework.ai.tool.annotation.Tool;
//import org.springframework.ai.tool.annotation.ToolParam;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//
//@Service
//public class CitaTools {
//
//    // Nota: En un caso real, aquí inyectarías tu CitaRepository para guardar en BD.
//    private final WhatsAppService whatsAppService;
//    public CitaTools(WhatsAppService w) {
//    	this.whatsAppService=w;
//    }
//	
//    @Tool(description = "Agenda una cita legal en el despacho. Úsala cuando el cliente pida una cita o consulta presencial/telefónica.")
//    public String agendarCita(
//            @ToolParam(description = "Nombre completo del cliente") String nombre,
//            @ToolParam(description = "Número de teléfono del cliente") String telefono,
//            @ToolParam(description = "Fecha y hora exacta de la cita (formato texto)") String fechaHora,
//            @ToolParam(description = "Motivo de la consulta o materia legal") String motivo,
//            @ToolParam(description = "Resumen detallado de lo que el cliente necesita") String resumen) {
//        
//        // 1. Aquí puedes guardar en tu base de datos (ej. crear un registro en la tabla 'citas')
//    	
//        // 2. Aquí puedes enviarte un WhatsApp a ti mismo o un correo con la info.
//        
//    	// Dentro de agendarCita:
//    	whatsAppService.enviarTexto("5214491157917", "NUEVA CITA: " + nombre + " - " + motivo + " - " + fechaHora);
//    	
//        System.out.println("====== NUEVA CITA AGENDADA ======");
//        System.out.println("Nombre: " + nombre);
//        System.out.println("Teléfono: " + telefono);
//        System.out.println("Fecha: " + fechaHora);
//        System.out.println("Motivo: " + motivo);
//        System.out.println("Resumen: " + resumen);
//        System.out.println("=================================");
//
//        // 3. Le devolvemos un texto al LLM para que sepa que la cita se guardó con éxito.
//        // El LLM usará esta información para responderle al usuario en WhatsApp.
//        return "La cita ha sido agendada con éxito en el sistema para el día " + fechaHora + 
//               ". El motivo es: " + motivo + ". El despacho ha recibido el resumen.";
//    }
//}
package com.legal.bot.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class CitaTools {

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
        
        System.out.println("====== NUEVA CITA DE DILIGENCIA ======");
        System.out.println("Cliente: " + nombre);
        System.out.println("Fecha: " + fechaHora);
        System.out.println("Diligencia: " + tipoDiligencia);
        System.out.println("Ubicación: " + ubicacion);
        System.out.println("======================================");

        // Aquí puedes notificarte a ti mismo (Cambia el número por el tuyo)
        // whatsAppService.enviarTexto("5214490000000", "Nueva Cita de " + nombre + ". Diligencia: " + tipoDiligencia + " el " + fechaHora);
     // Dentro de agendarCita:
    	whatsAppService.enviarTexto("5214491157917", "Nueva Cita de " + nombre + ". Diligencia: " + tipoDiligencia + " el " + fechaHora);
   
        
        return "La cita ha sido agendada con éxito para el día " + fechaHora + " por Zoom. Se ha enviado la confirmación.";
    }
}