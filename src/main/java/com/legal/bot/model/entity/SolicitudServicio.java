package com.legal.bot.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Solicitud de diligencia/audiencia hecha desde la landing. Queda RECIBIDA hasta que la
// abogada la revise; nunca se da por aceptada automaticamente.
@Entity
@Table(name = "service_requests")
public class SolicitudServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(name = "service_code", nullable = false, length = 40)
    private String serviceCode;

    @Column(name = "legal_area", length = 80)
    private String legalArea;

    @Column(name = "court_name", length = 160)
    private String courtName;

    @Column(name = "municipality", length = 80)
    private String municipality;

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "RECIBIDA";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SolicitudServicio() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }

    public Lead getLead() { return lead; }
    public void setLead(Lead lead) { this.lead = lead; }

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public String getLegalArea() { return legalArea; }
    public void setLegalArea(String legalArea) { this.legalArea = legalArea; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
}
