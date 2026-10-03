package com.legal.bot.handler;

import com.legal.bot.config.AsyncConfig;
import com.legal.bot.config.WhatsAppProperties;
import com.legal.bot.model.dto.WhatsAppMessage;
import com.legal.bot.model.dto.WhatsAppWebhookPayload;
import com.legal.bot.model.entity.Cliente;
import com.legal.bot.model.entity.Conversacion;
import com.legal.bot.service.ConversationService;
import com.legal.bot.service.KnowledgeService;
import com.legal.bot.service.LegalAIService;
import com.legal.bot.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class WhatsAppMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageHandler.class);

    private static final Set<String> SALUDOS = Set.of(
            "hola", "buenas", "menu", "menú", "inicio", "start",
            "buenos dias", "buenos días", "buenas tardes", "buenas noches",
            "ayuda", "hey", "ola"
    );

    private static final String AVISO_SOLO_TEXTO = """
            Por ahora solo puedo leer mensajes de texto. Escríbeme tu duda legal por \
            escrito y con gusto te oriento.""";

    private static final String PASO_A_ABOGADO = """
            Con gusto. Un abogado del despacho retomará esta conversación en horario de \
            atención (%s). Puedes dejar aquí los detalles de tu caso.""";

    /** Respuesta que NO se envió: significa que el LLM no supo qué contestar. */
    private static final String MARCADOR_NO_RESPONDER = "NO_RESPONDER";

    private final ConversationService conversationService;
    private final LegalAIService legalAIService;
    private final WhatsAppService whatsAppService;
    private final KnowledgeService knowledgeService;

    private final WhatsAppProperties whatsappProperties;

    public WhatsAppMessageHandler(ConversationService conversationService,
                                  LegalAIService legalAIService,
                                  WhatsAppService whatsAppService,
                                  KnowledgeService knowledgeService,
                                  WhatsAppProperties whatsappProperties) {
        this.conversationService = conversationService;
        this.legalAIService = legalAIService;
        this.whatsAppService = whatsAppService;
        this.knowledgeService = knowledgeService;
        this.whatsappProperties = whatsappProperties;
    }

    @Async(AsyncConfig.WHATSAPP_EXECUTOR)
    public void procesar(WhatsAppWebhookPayload payload) {
        // Log en el hilo del executor: si esto no aparece, el @Async no esta aplicando
        // (proxy ausente o nombre de executor equivocado) y no es culpa del webhook.
        log.info("Procesando {} mensaje(s) en el hilo {}.",
                payload.mensajes().size(), Thread.currentThread().getName());

        for (WhatsAppMessage mensaje : payload.mensajes()) {
            try {
                atender(mensaje, payload.nombreDe(mensaje.from()).orElse(null));
            } catch (Exception ex) {
                log.error("Error procesando el mensaje {} de {}: {}",
                        mensaje.id(), mensaje.from(), ex.getMessage(), ex);
            }
        }
    }

    private void atender(WhatsAppMessage mensaje, String nombrePerfil) {
        String telefono = mensaje.from();

        
        if (!whatsappProperties.numeroEstaPermitido(telefono)) {
            // Se registra la lista para poder comparar el formato a simple vista: Meta
            // manda el telefono en internacional sin '+'.
            log.warn("Número {} no está en la whitelist de prueba {}; se ignora.",
                    telefono, whatsappProperties.getNumerosPermitidos());
            return;
        }
        
        // 1. Duplicado: Meta reintenta si nuestro 200 tardó.
        if (conversationService.yaProcesado(mensaje.id())) {
            log.debug("Mensaje {} ya procesado; se ignora el reintento.", mensaje.id());
            return;
        }

        Cliente cliente = conversationService.obtenerOCrearCliente(telefono, nombrePerfil);
        Conversacion conversacion = conversationService.obtenerOCrearConversacion(cliente);

        // 2. Modo IA apagado para este cliente: solo guardamos, no respondemos.
        if (!Boolean.TRUE.equals(cliente.getModoIaActivo())) {
            log.info("Cliente {} tiene modo IA apagado; se guarda sin responder.", telefono);
            String contenido = mensaje.esTexto() ? mensaje.contenido()
                    : "[" + mensaje.type() + " no procesado]";
            conversationService.guardarEntrante(conversacion, mensaje.id(), mensaje.type(), contenido);
            return;
        }

        // 3. Tipos no-texto en esta fase: se guardan y avisamos fijo.
        if (!mensaje.esTexto()) {
            log.info("Mensaje tipo '{}' de {}: se guarda y se responde aviso fijo.",
                    mensaje.type(), telefono);
            conversationService.guardarEntrante(conversacion, mensaje.id(),
                    mensaje.type(), "[" + mensaje.type() + " no procesado]");
            responder(conversacion, telefono, AVISO_SOLO_TEXTO, false);
            return;
        }

        // 4. Texto normal: se guarda.
        String texto = mensaje.contenido();
        log.info("Consulta de WhatsApp de {}: {}", telefono, texto);
        conversationService.guardarEntrante(conversacion, mensaje.id(), "text", texto);

        // Marcar como leído (doble check azul) mientras pensamos.
        whatsAppService.marcarComoLeido(mensaje.id());

        // 5. Respuestas fijas (saludo / menú / opción numérica).
        Optional<Respuesta> fija = respuestaFija(texto);
        if (fija.isPresent()) {
            responder(conversacion, telefono, fija.get().texto(), fija.get().generadoPorIa());
            return;
        }

        // 6. Consulta abierta: la responde la IA local.
        String respuestaIA = legalAIService.responder(conversacion, texto);

        // Si el LLM no supo qué contestar, no enviamos nada.
        if (respuestaIA == null || respuestaIA.isBlank()
                || MARCADOR_NO_RESPONDER.equalsIgnoreCase(respuestaIA.trim())) {
            log.info("El LLM no supo responder a '{}'; se omite respuesta.", texto);
            return;
        }

        responder(conversacion, telefono, respuestaIA, true);
    }

    private Optional<Respuesta> respuestaFija(String texto) {
        String normalizado = texto.toLowerCase(Locale.ROOT).trim();

        if (SALUDOS.contains(normalizado)) {
            return Optional.of(Respuesta.fija(knowledgeService.mensajeBienvenida()));
        }

        if (normalizado.length() == 1 && Character.isDigit(normalizado.charAt(0))) {
            String opcion = knowledgeService.textoDeOpcion(normalizado);
            if (opcion == null) {
                return Optional.of(Respuesta.fija(knowledgeService.mensajeBienvenida()));
            }
            if ("4".equals(normalizado)) {
                // Opción "Hablar con un abogado": solo aviso, sin silenciar el bot.
                return Optional.of(Respuesta.fija(
                        PASO_A_ABOGADO.formatted(knowledgeService.horario())));
            }
            // Las demás opciones se resuelven con la IA, usando el tema como prompt.
            String respuesta = legalAIService.responderPorTema(opcion);
            if (respuesta == null || respuesta.isBlank()
                    || MARCADOR_NO_RESPONDER.equalsIgnoreCase(respuesta.trim())) {
                return Optional.empty();
            }
            return Optional.of(Respuesta.deIa(respuesta));
        }

        return Optional.empty();
    }
    private record Respuesta(String texto, boolean generadoPorIa) {
        static Respuesta fija(String texto) { return new Respuesta(texto, false); }
        static Respuesta deIa(String texto) { return new Respuesta(texto, true); }
    }

    private void responder(Conversacion conversacion, String telefono,
                           String texto, boolean generadoPorIa) {
        String wamid = whatsAppService.enviarTexto(telefono, texto).orElse(null);
        conversationService.guardarSaliente(conversacion, texto, generadoPorIa, wamid);
    }
}