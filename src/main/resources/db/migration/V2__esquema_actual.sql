-- Esquema que corresponde a las entidades JPA ACTUALES del proyecto.
--
-- Por que existe este archivo:
-- V1__esquema_inicial.sql quedo desfasado. Crea las tablas 'cliente', 'conversacion'
-- y 'mensaje' (en singular) con columnas de un diseño anterior (canal, estado,
-- direccion, fecha_registro...). Las entidades de hoy apuntan a 'clientes',
-- 'conversaciones' y 'mensajes' (en plural) con otras columnas. Como la app arranca
-- con spring.jpa.hibernate.ddl-auto=validate, Hibernate abortaba el arranque al no
-- encontrar esas tablas: la app nunca llegaba a escuchar en el puerto 8080 y por eso
-- no se veia ni un log del webhook.
--
-- No se borra nada: los nombres en plural no chocan con los del V1, asi que puedes
-- ejecutar este script tal cual en Supabase (Dashboard > SQL Editor > New query).
-- Cuando confirmes que todo va, las tablas en singular del V1 se pueden tirar a mano.
--
-- Los TIMESTAMP son 'without time zone' a proposito: las entidades usan
-- java.time.LocalDateTime, y con TIMESTAMPTZ la validacion de tipos de Hibernate
-- puede quejarse.

CREATE TABLE IF NOT EXISTS clientes (
    id                        BIGSERIAL PRIMARY KEY,
    telefono                  VARCHAR(20)  NOT NULL UNIQUE,
    nombre                    VARCHAR(200),
    modo_ia_activo            BOOLEAN      DEFAULT TRUE,
    notas                     TEXT,
    fecha_creacion            TIMESTAMP    DEFAULT now(),
    fecha_ultima_interaccion  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversaciones (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_id            BIGINT    NOT NULL REFERENCES clientes (id) ON DELETE CASCADE,
    activa                BOOLEAN   DEFAULT TRUE,
    fecha_inicio          TIMESTAMP DEFAULT now(),
    fecha_fin             TIMESTAMP,
    fecha_ultimo_mensaje  TIMESTAMP
);

-- Sirve a findFirstByClienteIdAndActivaTrueOrderByFechaInicioDesc.
CREATE INDEX IF NOT EXISTS idx_conversaciones_cliente_activa
    ON conversaciones (cliente_id, activa, fecha_inicio DESC);

-- wa_message_id es UNIQUE: es la defensa contra los reintentos del webhook de Meta,
-- que sin esto harian que el cliente reciba la respuesta varias veces.
CREATE TABLE IF NOT EXISTS mensajes (
    id               BIGSERIAL PRIMARY KEY,
    conversacion_id  BIGINT       NOT NULL REFERENCES conversaciones (id) ON DELETE CASCADE,
    rol              VARCHAR(20)  NOT NULL,
    contenido        TEXT         NOT NULL,
    tipo             VARCHAR(20)  DEFAULT 'text',
    media_url        TEXT,
    wa_message_id    VARCHAR(100) UNIQUE,
    generado_por_ia  BOOLEAN      DEFAULT FALSE,
    fecha_creacion   TIMESTAMP    DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mensajes_conversacion
    ON mensajes (conversacion_id, fecha_creacion DESC);
