-- Esquema inicial del bot legal.
--
-- NO se ejecuta solo: el proyecto no usa Flyway todavia. Copia este archivo en
-- el SQL Editor de Supabase (Dashboard > SQL Editor > New query) y ejecutalo una
-- sola vez. La app arranca con spring.jpa.hibernate.ddl-auto=validate, asi que
-- si las tablas no existen falla al inicio con un error de "missing table".
--
-- El nombre V1__... sigue la convencion de Flyway a proposito, para que cuando
-- se añada la dependencia estos scripts ya esten en su sitio.

-- Personas que escriben al bot. El telefono viene de Meta en formato
-- internacional sin '+' (p. ej. 5214491234567) y es la clave natural.
CREATE TABLE IF NOT EXISTS cliente (
    id                      BIGSERIAL PRIMARY KEY,
    telefono                VARCHAR(32)  NOT NULL UNIQUE,
    nombre                  VARCHAR(160),
    fecha_registro          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_ultima_actividad  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Hilos de conversacion. 'estado' es lo que decide si el bot responde:
-- ponlo en ATENDIDA_POR_HUMANO para que el bot calle y contesta tu.
CREATE TABLE IF NOT EXISTS conversacion (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_id            BIGINT       NOT NULL REFERENCES cliente (id) ON DELETE CASCADE,
    canal                 VARCHAR(20)  NOT NULL CHECK (canal IN ('WHATSAPP', 'TELEGRAM')),
    estado                VARCHAR(32)  NOT NULL CHECK (estado IN ('ACTIVA_BOT', 'ATENDIDA_POR_HUMANO', 'CERRADA')),
    fecha_inicio          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_ultimo_mensaje  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Indice que sirve a la consulta del ultimo hilo de un cliente por canal.
CREATE INDEX IF NOT EXISTS idx_conversacion_cliente_canal
    ON conversacion (cliente_id, canal, fecha_ultimo_mensaje DESC);

-- Mensajes entrantes y salientes.
-- wa_message_id es UNIQUE: es la defensa contra los reintentos del webhook de
-- Meta, que sin esto harian que el cliente reciba la respuesta varias veces.
CREATE TABLE IF NOT EXISTS mensaje (
    id               BIGSERIAL PRIMARY KEY,
    conversacion_id  BIGINT       NOT NULL REFERENCES conversacion (id) ON DELETE CASCADE,
    wa_message_id    VARCHAR(128) UNIQUE,
    direccion        VARCHAR(10)  NOT NULL CHECK (direccion IN ('ENTRANTE', 'SALIENTE')),
    tipo             VARCHAR(32)  NOT NULL,
    contenido        TEXT         NOT NULL,
    generado_por_ia  BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mensaje_conversacion
    ON mensaje (conversacion_id, fecha_creacion DESC);
