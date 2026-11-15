-- Tabla de la entidad Cita (solicitudes de cita que registra el bot con agendarCita).
--
-- En Supabase ya existe (se creo a mano), por eso IF NOT EXISTS: alli este script no
-- cambia nada y solo deja la tabla documentada para BDs nuevas. Columnas y tipos
-- iguales a la entidad, que se valida con ddl-auto=validate.

CREATE TABLE IF NOT EXISTS citas (
    id                     BIGSERIAL PRIMARY KEY,
    telefono_cliente       VARCHAR(20)  NOT NULL,
    nombre_cliente         VARCHAR(200) NOT NULL,
    fecha_hora_solicitada  VARCHAR(100) NOT NULL,
    tipo_diligencia        VARCHAR(200) NOT NULL,
    ubicacion              VARCHAR(200) NOT NULL,
    estado                 VARCHAR(50),
    fecha_creacion         TIMESTAMP
);
