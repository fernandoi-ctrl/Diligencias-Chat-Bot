package com.legal.bot.service;

import com.legal.bot.model.entity.Cliente;
import com.legal.bot.model.entity.Conversacion;
import com.legal.bot.model.entity.Mensaje;
import com.legal.bot.repository.ClienteRepository;
import com.legal.bot.repository.ConversacionRepository;
import com.legal.bot.repository.MensajeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ClienteRepository clienteRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;

    /** Horas de inactividad tras las cuales se cierra la conversación y se abre una nueva. */
    @Value("${chat.conversacion.horas-expiracion:24}")
    private long horasExpiracion;

    /** Cuántos mensajes recientes se le pasan al LLM como contexto. */
    @Value("${chat.conversacion.mensajes-contexto:20}")
    private int mensajesContexto;

    public ConversationService(ClienteRepository clienteRepository,
                               ConversacionRepository conversacionRepository,
                               MensajeRepository mensajeRepository) {
        this.clienteRepository = clienteRepository;
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
    }

    /** true si ese wamid ya se guardó antes. Evita responder 2 veces si Meta reintenta. */
    @Transactional(readOnly = true)
    public boolean yaProcesado(String waMessageId) {
        return waMessageId != null && mensajeRepository.existsByWaMessageId(waMessageId);
    }

    /**
     * Recupera el cliente por teléfono o lo crea.
     * Sin @Transactional a propósito: cada repo abre su propia transacción, así la
     * carrera del INSERT duplicado no deja la sesión inservible.
     */
    public Cliente obtenerOCrearCliente(String telefono, String nombre) {
        Optional<Cliente> existente = clienteRepository.findByTelefono(telefono);
        if (existente.isPresent()) {
            Cliente cliente = existente.get();
            if (nombre != null && !nombre.isBlank() && !nombre.equals(cliente.getNombre())) {
                cliente.setNombre(nombre);
                return clienteRepository.save(cliente);
            }
            return cliente;
        }

        try {
            Cliente nuevo = Cliente.builder()
                    .telefono(telefono)
                    .nombre(nombre)
                    .modoIaActivo(true)
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            return clienteRepository.save(nuevo);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Carrera al crear el cliente {}; releyendo fila existente.", telefono);
            return clienteRepository.findByTelefono(telefono).orElseThrow(() -> ex);
        }
    }

    /**
     * Devuelve la conversación activa del cliente. Si la última supera las
     * horas de expiración configuradas, la cierra y abre una nueva.
     */
    @Transactional
    public Conversacion obtenerOCrearConversacion(Cliente cliente) {
        Optional<Conversacion> ultima = conversacionRepository
                .findFirstByClienteIdAndActivaTrueOrderByFechaInicioDesc(cliente.getId());

        if (ultima.isPresent()) {
            Conversacion c = ultima.get();
            LocalDateTime referencia = c.getFechaUltimoMensaje() != null
                    ? c.getFechaUltimoMensaje()
                    : c.getFechaInicio();

            boolean expirada = referencia != null
                    && referencia.isBefore(LocalDateTime.now().minusHours(horasExpiracion));

            if (expirada) {
                c.setActiva(false);
                c.setFechaFin(LocalDateTime.now());
                conversacionRepository.save(c);
                log.info("Conversación {} cerrada por inactividad (>{}h).", c.getId(), horasExpiracion);
            } else {
                return c;
            }
        }

        Conversacion nueva = Conversacion.builder()
                .cliente(cliente)
                .activa(true)
                .fechaInicio(LocalDateTime.now())
                .fechaUltimoMensaje(LocalDateTime.now())
                .build();
        return conversacionRepository.save(nueva);
    }

    /** Guarda el mensaje entrante del cliente. */
    @Transactional
    public Mensaje guardarEntrante(Conversacion conversacion, String waMessageId,
                                   String tipo, String contenido) {
        Mensaje mensaje = Mensaje.builder()
                .conversacion(conversacion)
                .waMessageId(waMessageId)
                .rol("user")
                .tipo(tipo != null ? tipo : "text")
                .contenido(contenido)
                .generadoPorIa(false)
                .fechaCreacion(LocalDateTime.now())
                .build();
        Mensaje guardado = mensajeRepository.save(mensaje);
        tocar(conversacion);
        tocarCliente(conversacion.getCliente());
        return guardado;
    }

    /** Guarda la respuesta saliente (IA o humano). */
    @Transactional
    public Mensaje guardarSaliente(Conversacion conversacion, String contenido,
                                   boolean generadoPorIa, String waMessageId) {
        Mensaje mensaje = Mensaje.builder()
                .conversacion(conversacion)
                .waMessageId(waMessageId)
                .rol("assistant")
                .tipo("text")
                .contenido(contenido)
                .generadoPorIa(generadoPorIa)
                .fechaCreacion(LocalDateTime.now())
                .build();
        Mensaje guardado = mensajeRepository.save(mensaje);
        tocar(conversacion);
        return guardado;
    }

    /** Últimos N mensajes de la conversación, en orden cronológico ascendente. */
    @Transactional(readOnly = true)
    public List<Mensaje> obtenerHistorialReciente(Conversacion conversacion, int limite) {
        List<Mensaje> desc = mensajeRepository.findUltimosPorConversacion(
                conversacion.getId(), PageRequest.of(0, limite));
        // Vienen DESC; los invertimos para pasárselos al LLM en orden cronológico.
        java.util.Collections.reverse(desc);
        return desc;
    }

    public List<Mensaje> obtenerHistorialReciente(Conversacion conversacion) {
        return obtenerHistorialReciente(conversacion, mensajesContexto);
    }

    private void tocar(Conversacion conversacion) {
        conversacion.setFechaUltimoMensaje(LocalDateTime.now());
        conversacionRepository.save(conversacion);
    }

    private void tocarCliente(Cliente cliente) {
        // getId() es lo unico que se le puede pedir a un proxy LAZY sin inicializarlo.
        if (cliente == null || cliente.getId() == null) {
            return;
        }
        clienteRepository.actualizarUltimaInteraccion(cliente.getId(), LocalDateTime.now());
    }
}