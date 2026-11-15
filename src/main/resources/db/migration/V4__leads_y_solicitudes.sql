-- Prospectos y solicitudes de diligencia que llegan desde la landing (POST /api/v1/service-requests).
-- Solo tablas nuevas: no toca clientes/conversaciones/mensajes/citas del bot.
-- Los UUID los genera Hibernate; no hace falta ninguna extension de PostgreSQL.

CREATE TABLE leads (
    id            UUID         PRIMARY KEY,
    full_name     VARCHAR(160) NOT NULL,
    firm_name     VARCHAR(160),
    email         VARCHAR(254),
    phone         VARCHAR(20),
    source        VARCHAR(40)  NOT NULL,
    utm_source    VARCHAR(100),
    utm_campaign  VARCHAR(100),
    consent_at    TIMESTAMPTZ  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT leads_requiere_contacto CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE INDEX idx_leads_created_at ON leads (created_at DESC);

CREATE TABLE service_requests (
    id              UUID          PRIMARY KEY,
    lead_id         UUID          NOT NULL REFERENCES leads (id) ON DELETE CASCADE,
    service_code    VARCHAR(40)   NOT NULL,
    legal_area      VARCHAR(80),
    court_name      VARCHAR(160),
    municipality    VARCHAR(80),
    requested_date  DATE,
    description     VARCHAR(1000),
    status          VARCHAR(20)   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_service_requests_lead ON service_requests (lead_id);
CREATE INDEX idx_service_requests_status ON service_requests (status, created_at DESC);
