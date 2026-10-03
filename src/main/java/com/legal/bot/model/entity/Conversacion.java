package com.legal.bot.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Sin Lombok a proposito: ver la nota en Cliente. Las anotaciones generaban los
// mismos constructores y builder que ya estaban escritos a mano aqui abajo.
@Entity
@Table(name = "conversaciones")
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "activa")
    private Boolean activa = true;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    /** Se actualiza cada vez que entra o sale un mensaje en esta conversación. */
    @Column(name = "fecha_ultimo_mensaje")
    private LocalDateTime fechaUltimoMensaje;

    @OneToMany(mappedBy = "conversacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mensaje> mensajes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
        if (activa == null) activa = true;
        if (fechaUltimoMensaje == null) fechaUltimoMensaje = fechaInicio;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public Boolean getActiva() {
		return activa;
	}

	public void setActiva(Boolean activa) {
		this.activa = activa;
	}

	public LocalDateTime getFechaInicio() {
		return fechaInicio;
	}

	public void setFechaInicio(LocalDateTime fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	public LocalDateTime getFechaFin() {
		return fechaFin;
	}

	public void setFechaFin(LocalDateTime fechaFin) {
		this.fechaFin = fechaFin;
	}

	public LocalDateTime getFechaUltimoMensaje() {
		return fechaUltimoMensaje;
	}

	public void setFechaUltimoMensaje(LocalDateTime fechaUltimoMensaje) {
		this.fechaUltimoMensaje = fechaUltimoMensaje;
	}

	public List<Mensaje> getMensajes() {
		return mensajes;
	}

	public void setMensajes(List<Mensaje> mensajes) {
		this.mensajes = mensajes;
	}

	public Conversacion(Cliente cliente, Boolean activa, LocalDateTime fechaInicio, LocalDateTime fechaFin,
			LocalDateTime fechaUltimoMensaje, List<Mensaje> mensajes) {
		this.cliente = cliente;
		this.activa = activa;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.fechaUltimoMensaje = fechaUltimoMensaje;
		this.mensajes = mensajes;
	}
	 // Constructor vacío requerido por JPA
    public Conversacion() {}

    // Constructor privado para el Builder
    private Conversacion(ConversacionBuilder builder) {
        this.id = builder.id;
        this.cliente = builder.cliente;
        this.activa = builder.activa;
        this.fechaInicio = builder.fechaInicio;
        this.fechaFin = builder.fechaFin;
        this.fechaUltimoMensaje = builder.fechaUltimoMensaje;
        this.mensajes = builder.mensajes;
    }

    // Método estático para inicializar el builder
    public static ConversacionBuilder builder() {
        return new ConversacionBuilder();
    }

    // Clase Builder Estática
    public static class ConversacionBuilder {
        private Long id;
        private Cliente cliente;
        private Boolean activa = true; // Valor por defecto reflejado del @Builder.Default
        private LocalDateTime fechaInicio = LocalDateTime.now(); // Valor por defecto reflejado del @Builder.Default
        private LocalDateTime fechaFin;
        private LocalDateTime fechaUltimoMensaje;
        private List<Mensaje> mensajes = new ArrayList<>(); // Lista vacía por defecto de @Builder.Default

        public ConversacionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ConversacionBuilder cliente(Cliente cliente) {
            this.cliente = cliente;
            return this;
        }

        public ConversacionBuilder activa(Boolean activa) {
            this.activa = activa;
            return this;
        }

        public ConversacionBuilder fechaInicio(LocalDateTime fechaInicio) {
            this.fechaInicio = fechaInicio;
            return this;
        }

        public ConversacionBuilder fechaFin(LocalDateTime fechaFin) {
            this.fechaFin = fechaFin;
            return this;
        }

        public ConversacionBuilder fechaUltimoMensaje(LocalDateTime fechaUltimoMensaje) {
            this.fechaUltimoMensaje = fechaUltimoMensaje;
            return this;
        }

        public ConversacionBuilder mensajes(List<Mensaje> mensajes) {
            this.mensajes = mensajes;
            return this;
        }

        public Conversacion build() {
            return new Conversacion(this);
        }
    }
}