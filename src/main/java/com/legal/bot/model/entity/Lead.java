package com.legal.bot.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

// Prospecto que llega desde la landing. Distinto de Cliente, que es quien escribe por
// WhatsApp y se identifica por su telefono.
@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "firm_name", length = 160)
    private String firmName;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "source", nullable = false, length = 40)
    private String source;

    @Column(name = "utm_source", length = 100)
    private String utmSource;

    @Column(name = "utm_campaign", length = 100)
    private String utmCampaign;

    @Column(name = "consent_at", nullable = false)
    private Instant consentAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "NUEVO";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Lead() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getFirmName() { return firmName; }
    public void setFirmName(String firmName) { this.firmName = firmName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getUtmSource() { return utmSource; }
    public void setUtmSource(String utmSource) { this.utmSource = utmSource; }

    public String getUtmCampaign() { return utmCampaign; }
    public void setUtmCampaign(String utmCampaign) { this.utmCampaign = utmCampaign; }

    public Instant getConsentAt() { return consentAt; }
    public void setConsentAt(Instant consentAt) { this.consentAt = consentAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
}
