# API pública v1 (landing)

Base: `https://<servicio-render>/api/v1`. CORS limitado a `ALLOWED_ORIGINS` (lista separada por comas).
Los endpoints existentes del bot (`/webhook`, `/health`, `/api/admin/**`) no cambian.

## POST /api/v1/service-requests

Registra un prospecto (`leads`) y su solicitud (`service_requests`) en una sola transacción.
La respuesta solo confirma la **recepción**; el servicio, el precio y la fecha los confirma la abogada.

```json
{
  "nombre": "string, requerido, ≤160",
  "despacho": "string, ≤160",
  "email": "email, ≤254",
  "telefono": "10-20 caracteres: dígitos, espacios, ()-+",
  "servicio": "PROMOCIONES | REVISION_EXPEDIENTE | EXHORTOS | ACTUARIOS | AUDIENCIAS",
  "materia": "string, ≤80",
  "juzgado": "string, ≤160",
  "municipio": "string, ≤80",
  "fechaTentativa": "YYYY-MM-DD, hoy o futura",
  "descripcion": "string, ≤1000, sin datos sensibles del expediente",
  "aceptaPrivacidad": true,
  "utmSource": "string, ≤100",
  "utmCampaign": "string, ≤100",
  "website": "honeypot: siempre vacío"
}
```
`email` o `telefono`: al menos uno.

| Código | Cuerpo | Cuándo |
|---|---|---|
| 201 | `{"estado":"RECIBIDA"}` | Guardada (o descartada en silencio por el honeypot) |
| 400 | `{"error":"…","campos":{"campo":"mensaje"}}` | Validación; `campos` falta si el JSON es inválido o el servicio no existe |
| 429 | `{"error":"…"}` | Más de 5 solicitudes por IP en 10 minutos |

## Migraciones (Flyway)

- `V1`, `V2`: esquema del bot, ya aplicado a mano en Supabase.
- `V3__citas.sql`: tabla `citas` con `IF NOT EXISTS` (en Supabase no cambia nada).
- `V4__leads_y_solicitudes.sql`: tablas nuevas `leads` y `service_requests`.

En el primer arranque con Flyway sobre Supabase (tablas existentes, sin `flyway_schema_history`)
se registra un baseline en V2 y solo se ejecutan V3 y V4. Verificado en `MigracionesPostgresTest`
con PostgreSQL embebido.
