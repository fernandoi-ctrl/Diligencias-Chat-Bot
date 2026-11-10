package com.legal.bot.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telefono_cliente", nullable = false, length = 20)
    private String telefonoCliente;

    @Column(name = "nombre_cliente", nullable = false, length = 200)
    private String nombreCliente;

    @Column(name = "fecha_hora_solicitada", nullable = false, length = 100)
    private String fechaHoraSolicitada;

    @Column(name = "tipo_diligencia", nullable = false, length = 200)
    private String tipoDiligencia;

    @Column(name = "ubicacion", nullable = false, length = 200)
    private String ubicacion;

    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Constructor vacío requerido por JPA
    public Cita() {
    }

    // Constructor completo
    public Cita(Long id, String telefonoCliente, String nombreCliente, String fechaHoraSolicitada, 
                String tipoDiligencia, String ubicacion, String estado, LocalDateTime fechaCreacion) {
        this.id = id;
        this.telefonoCliente = telefonoCliente;
        this.nombreCliente = nombreCliente;
        this.fechaHoraSolicitada = fechaHoraSolicitada;
        this.tipoDiligencia = tipoDiligencia;
        this.ubicacion = ubicacion;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTelefonoCliente() { return telefonoCliente; }
    public void setTelefonoCliente(String telefonoCliente) { this.telefonoCliente = telefonoCliente; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getFechaHoraSolicitada() { return fechaHoraSolicitada; }
    public void setFechaHoraSolicitada(String fechaHoraSolicitada) { this.fechaHoraSolicitada = fechaHoraSolicitada; }

    public String getTipoDiligencia() { return tipoDiligencia; }
    public void setTipoDiligencia(String tipoDiligencia) { this.tipoDiligencia = tipoDiligencia; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    // --- PATRÓN BUILDER MANUAL ---
    public static CitaBuilder builder() {
        return new CitaBuilder();
    }

    public static class CitaBuilder {
        private String telefonoCliente;
        private String nombreCliente;
        private String fechaHoraSolicitada;
        private String tipoDiligencia;
        private String ubicacion;
        private String estado = "PENDIENTE";
        private LocalDateTime fechaCreacion = LocalDateTime.now();

        public CitaBuilder telefonoCliente(String telefonoCliente) {
            this.telefonoCliente = telefonoCliente;
            return this;
        }

        public CitaBuilder nombreCliente(String nombreCliente) {
            this.nombreCliente = nombreCliente;
            return this;
        }

        public CitaBuilder fechaHoraSolicitada(String fechaHoraSolicitada) {
            this.fechaHoraSolicitada = fechaHoraSolicitada;
            return this;
        }

        public CitaBuilder tipoDiligencia(String tipoDiligencia) {
            this.tipoDiligencia = tipoDiligencia;
            return this;
        }

        public CitaBuilder ubicacion(String ubicacion) {
            this.ubicacion = ubicacion;
            return this;
        }

        public CitaBuilder estado(String estado) {
            this.estado = estado;
            return this;
        }

        public CitaBuilder fechaCreacion(LocalDateTime fechaCreacion) {
            this.fechaCreacion = fechaCreacion;
            return this;
        }

        public Cita build() {
            return new Cita(null, telefonoCliente, nombreCliente, fechaHoraSolicitada, 
                            tipoDiligencia, ubicacion, estado, fechaCreacion);
        }
    }
}