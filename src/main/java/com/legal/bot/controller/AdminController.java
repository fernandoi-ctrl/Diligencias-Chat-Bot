package com.legal.bot.controller;

import com.legal.bot.model.entity.Cliente;
import com.legal.bot.model.entity.Conversacion;
import com.legal.bot.model.entity.Mensaje;
import com.legal.bot.repository.ClienteRepository;
import com.legal.bot.repository.ConversacionRepository;
import com.legal.bot.repository.MensajeRepository;
import com.legal.bot.service.ConversationService;
import com.legal.bot.service.WhatsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ClienteRepository clienteRepo;
    private final ConversacionRepository conversacionRepo;
    private final MensajeRepository mensajeRepo;
    private final ConversationService conversationService;
    private final WhatsAppService whatsAppService;

    public AdminController(ClienteRepository clienteRepo, ConversacionRepository conversacionRepo, 
                           MensajeRepository mensajeRepo, ConversationService conversationService, 
                           WhatsAppService whatsAppService) {
        this.clienteRepo = clienteRepo;
        this.conversacionRepo = conversacionRepo;
        this.mensajeRepo = mensajeRepo;
        this.conversationService = conversationService;
        this.whatsAppService = whatsAppService;
    }

    // 2. Obtener los mensajes de un cliente
//    @GetMapping("/clientes/{telefono}/mensajes")
//    public ResponseEntity<List<Mensaje>> obtenerMensajes(@PathVariable String telefono) {
//        Optional<Cliente> cliente = clienteRepo.findByTelefono(telefono);
//        if (cliente.isEmpty()) return ResponseEntity.notFound().build();
//
//        // Buscamos la conversación activa o la última
//        Optional<Conversacion> conv = conversacionRepo.findFirstByClienteIdAndActivaTrueOrderByFechaInicioDesc(cliente.get().getId());
//        
//        if (conv.isEmpty()) return ResponseEntity.ok(List.of()); // Sin conversaciones
//
//        List<Mensaje> mensajes = mensajeRepo.findByConversacionIdOrderByFechaCreacionAsc(conv.get().getId());
//        return ResponseEntity.ok(mensajes);
//    }

    // 3. Encender / Apagar Modo IA
    @PatchMapping("/clientes/{telefono}/modo-ia")
    public ResponseEntity<?> cambiarModoIA(@PathVariable String telefono, @RequestBody Map<String, Boolean> body) {
        Optional<Cliente> clienteOpt = clienteRepo.findByTelefono(telefono);
        if (clienteOpt.isEmpty()) return ResponseEntity.notFound().build();

        Cliente cliente = clienteOpt.get();
        cliente.setModoIaActivo(body.getOrDefault("activo", true));
        clienteRepo.save(cliente);

        return ResponseEntity.ok(Map.of("modoIaActivo", cliente.getModoIaActivo()));
    }

    // 4. Enviar mensaje manual (desde la UI)
    @PostMapping("/clientes/{telefono}/mensajes")
    public ResponseEntity<?> enviarMensajeManual(@PathVariable String telefono, @RequestBody Map<String, String> body) {
        String texto = body.get("texto");
        if (texto == null || texto.isBlank()) return ResponseEntity.badRequest().body("Texto vacío");

        Optional<Cliente> clienteOpt = clienteRepo.findByTelefono(telefono);
        if (clienteOpt.isEmpty()) return ResponseEntity.notFound().build();

        Cliente cliente = clienteOpt.get();
        
        // Enviar por WhatsApp
        whatsAppService.enviarTexto(telefono, texto);

        // Guardar en base de datos (como 'assistant' pero generado por IA = false, o agregar un rol 'human')
        Conversacion conv = conversacionRepo.findFirstByClienteIdAndActivaTrueOrderByFechaInicioDesc(cliente.getId())
                .orElseGet(() -> conversationService.obtenerOCrearConversacion(cliente));
        
        conversationService.guardarSaliente(conv, texto, false, null); // generadoPorIa = false

        return ResponseEntity.ok(Map.of("status", "enviado"));
    }
    // 2. Obtener los mensajes de un cliente
    @GetMapping("/clientes/{telefono}/mensajes")
    public ResponseEntity<List<Map<String, Object>>> obtenerMensajes(@PathVariable String telefono) {
        Optional<Cliente> cliente = clienteRepo.findByTelefono(telefono);
        if (cliente.isEmpty()) return ResponseEntity.ok(List.of());

        Optional<Conversacion> conv = conversacionRepo.findFirstByClienteIdAndActivaTrueOrderByFechaInicioDesc(cliente.get().getId());
        
        if (conv.isEmpty()) return ResponseEntity.ok(List.of());

        List<Mensaje> mensajes = mensajeRepo.findByConversacionIdOrderByFechaCreacionAsc(conv.get().getId());
        
        // Convertimos a un Map para evitar el bucle infinito y datos sensibles
        List<Map<String, Object>> response = mensajes.stream().map(m -> {
            Map<String,Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("rol", m.getRol());
                map.put("contenido", m.getContenido());
                map.put("fecha", m.getFechaCreacion() != null ? m.getFechaCreacion().toString() : "");
        return map;    
        }
        ).toList();

        return ResponseEntity.ok(response);
    }
    // 1. Obtener todos los clientes (para la lista de chats)
    @GetMapping("/clientes")
    public List<Map<String, Object>> obtenerClientes() {
        return clienteRepo.findAll().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("telefono", c.getTelefono());
            map.put("nombre", c.getNombre() != null ? c.getNombre() : "Desconocido");
            map.put("modoIaActivo", c.getModoIaActivo());
            return map;
        }).toList();
    }

}