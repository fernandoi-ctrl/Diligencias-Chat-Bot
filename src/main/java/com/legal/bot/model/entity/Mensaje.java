package com.legal.bot.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Sin Lombok a proposito: ver la nota en Cliente. Las anotaciones generaban los
// mismos constructores que ya estaban escritos a mano aqui abajo.
@Entity
@Table(name = "mensajes")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    /** "user" | "assistant" */
    @Column(name = "rol", nullable = false, length = 20)
    private String rol;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    /** text, audio, image, document */
    @Column(name = "tipo", length = 20)
    private String tipo = "text";

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    /** ID del mensaje en WhatsApp (wamid...). Sirve para deduplicar reintentos de Meta. */
    @Column(name = "wa_message_id", length = 100, unique = true)
    private String waMessageId;

    /** true si la respuesta la generó la IA, false si la escribió un humano desde el panel. */
    @Column(name = "generado_por_ia")
    private Boolean generadoPorIa = false;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (tipo == null) tipo = "text";
        if (generadoPorIa == null) generadoPorIa = false;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Conversacion getConversacion() {
		return conversacion;
	}

	public void setConversacion(Conversacion conversacion) {
		this.conversacion = conversacion;
	}

	public String getRol() {
		return rol;
	}

	public void setRol(String rol) {
		this.rol = rol;
	}

	public String getContenido() {
		return contenido;
	}

	public void setContenido(String contenido) {
		this.contenido = contenido;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getMediaUrl() {
		return mediaUrl;
	}

	public void setMediaUrl(String mediaUrl) {
		this.mediaUrl = mediaUrl;
	}

	public String getWaMessageId() {
		return waMessageId;
	}

	public void setWaMessageId(String waMessageId) {
		this.waMessageId = waMessageId;
	}

	public Boolean getGeneradoPorIa() {
		return generadoPorIa;
	}

	public void setGeneradoPorIa(Boolean generadoPorIa) {
		this.generadoPorIa = generadoPorIa;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public Mensaje(Conversacion conversacion, String rol, String contenido, String tipo, String mediaUrl,
			String waMessageId, Boolean generadoPorIa, LocalDateTime fechaCreacion) {
		this.conversacion = conversacion;
		this.rol = rol;
		this.contenido = contenido;
		this.tipo = tipo;
		this.mediaUrl = mediaUrl;
		this.waMessageId = waMessageId;
		this.generadoPorIa = generadoPorIa;
		this.fechaCreacion = fechaCreacion;
	}
	// Constructor vacío requerido por JPA
    public Mensaje() {}

    // Constructor privado utilizado por el Builder
    private Mensaje(MensajeBuilder builder) {
        this.id = builder.id;
        this.conversacion = builder.conversacion;
        this.rol = builder.rol;
        this.contenido = builder.contenido;
        this.tipo = builder.tipo;
        this.mediaUrl = builder.mediaUrl;
        this.waMessageId = builder.waMessageId;
        this.generadoPorIa = builder.generadoPorIa;
        this.fechaCreacion = builder.fechaCreacion;
    }

//    @PrePersist
//    public void prePersist() {
//        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
//        if (tipo == null) tipo = "text";
//        if (generadoPorIa == null) generadoPorIa = false;
//    }

    // Método estático para inicializar el builder
    public static MensajeBuilder builder() {
        return new MensajeBuilder();
    }

    // Clase Builder Estática
    public static class MensajeBuilder {
        private Long id;
        private Conversacion conversacion;
        private String rol;
        private String contenido;
        private String tipo = "text"; // Valor por defecto reflejado del @Builder.Default
        private String mediaUrl;
        private String waMessageId;
        private Boolean generadoPorIa = false; // Valor por defecto reflejado del @Builder.Default
        private LocalDateTime fechaCreacion = LocalDateTime.now(); // Valor por defecto reflejado del @Builder.Default

        public MensajeBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MensajeBuilder conversacion(Conversacion conversacion) {
            this.conversacion = conversacion;
            return this;
        }

        public MensajeBuilder rol(String rol) {
            this.rol = rol;
            return this;
        }

        public MensajeBuilder contenido(String contenido) {
            this.contenido = contenido;
            return this;
        }

        public MensajeBuilder tipo(String tipo) {
            this.tipo = tipo;
            return this;
        }

        public MensajeBuilder mediaUrl(String mediaUrl) {
            this.mediaUrl = mediaUrl;
            return this;
        }

        public MensajeBuilder waMessageId(String waMessageId) {
            this.waMessageId = waMessageId;
            return this;
        }

        public MensajeBuilder generadoPorIa(Boolean generadoPorIa) {
            this.generadoPorIa = generadoPorIa;
            return this;
        }

        public MensajeBuilder fechaCreacion(LocalDateTime fechaCreacion) {
            this.fechaCreacion = fechaCreacion;
            return this;
        }

        public Mensaje build() {
            return new Mensaje(this);
        }
    }

}