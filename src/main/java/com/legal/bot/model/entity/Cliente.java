package com.legal.bot.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// OJO: esta clase NO usa Lombok. Antes tenia @Getter/@Setter/@NoArgsConstructor/
// @AllArgsConstructor/@Builder ademas de los getters, setters, constructor vacio y
// builder escritos a mano de mas abajo. Lombok generaba los mismos miembros, asi que
// el compilador fallaba con "constructor Cliente() is already defined" y el proyecto
// entero no compilaba. Se conserva la version a mano y se quitan las anotaciones.
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "telefono", nullable = false, unique = true, length = 20)
    private String telefono;

    @Column(name = "nombre", length = 200)
    private String nombre;

    @Column(name = "modo_ia_activo")
    private Boolean modoIaActivo = true;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_ultima_interaccion")
    private LocalDateTime fechaUltimaInteraccion;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Conversacion> conversaciones = new ArrayList<>();

    // Constructor vacío requerido por JPA
    public Cliente() {}

    // Constructor privado para el Builder
    private Cliente(ClienteBuilder builder) {
        this.id = builder.id;
        this.telefono = builder.telefono;
        this.nombre = builder.nombre;
        this.modoIaActivo = builder.modoIaActivo;
        this.notas = builder.notas;
        this.fechaCreacion = builder.fechaCreacion;
        this.fechaUltimaInteraccion = builder.fechaUltimaInteraccion;
        this.conversaciones = builder.conversaciones;
    }

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (modoIaActivo == null) modoIaActivo = true;
    }

    // Método estático para inicializar el builder
    public static ClienteBuilder builder() {
        return new ClienteBuilder();
    }

    // Clase Builder Estática
    public static class ClienteBuilder {
        private Long id;
        private String telefono;
        private String nombre;
        private Boolean modoIaActivo = true; // Valor por defecto reflejado del @Builder.Default
        private String notas;
        private LocalDateTime fechaCreacion = LocalDateTime.now(); // Valor por defecto reflejado del @Builder.Default
        private LocalDateTime fechaUltimaInteraccion;
        private List<Conversacion> conversaciones = new ArrayList<>(); // Lista vacía por defecto de @Builder.Default

        public ClienteBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ClienteBuilder telefono(String telefono) {
            this.telefono = telefono;
            return this;
        }

        public ClienteBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public ClienteBuilder modoIaActivo(Boolean modoIaActivo) {
            this.modoIaActivo = modoIaActivo;
            return this;
        }

        public ClienteBuilder notas(String notas) {
            this.notas = notas;
            return this;
        }

        public ClienteBuilder fechaCreacion(LocalDateTime fechaCreacion) {
            this.fechaCreacion = fechaCreacion;
            return this;
        }

        public ClienteBuilder fechaUltimaInteraccion(LocalDateTime fechaUltimaInteraccion) {
            this.fechaUltimaInteraccion = fechaUltimaInteraccion;
            return this;
        }

        public ClienteBuilder conversaciones(List<Conversacion> conversaciones) {
            this.conversaciones = conversaciones;
            return this;
        }

        public Cliente build() {
            return new Cliente(this);
        }
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getModoIaActivo() { return modoIaActivo; }
    public void setModoIaActivo(Boolean modoIaActivo) { this.modoIaActivo = modoIaActivo; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaUltimaInteraccion() { return fechaUltimaInteraccion; }
    public void setFechaUltimaInteraccion(LocalDateTime fechaUltimaInteraccion) { this.fechaUltimaInteraccion = fechaUltimaInteraccion; }

    public List<Conversacion> getConversaciones() { return conversaciones; }
    public void setConversaciones(List<Conversacion> conversaciones) { this.conversaciones = conversaciones; }
}