# Bot Legal Aguascalientes

Bot para un despacho legal de Aguascalientes que responde consultas usando IA **local**
(Ollama) a traves de Spring AI. Atiende **dos canales** con el mismo cerebro:

- **Telegram**, por long polling: no necesita webhook, URL publica, dominio ni ngrok.
- **WhatsApp**, por la Cloud API oficial de Meta: este **si** necesita webhook publico, y
  guarda clientes, conversaciones y mensajes en Supabase. Ver
  [Canal de WhatsApp](#canal-de-whatsapp-fase-1).

## Stack

| Componente | Version | Nota |
|---|---|---|
| Java | 17 (target actual) | Spring Boot 4.1 pide 17 como minimo; ver [Java 21](#usar-java-21) |
| Spring Boot | 4.1.1 | |
| Spring AI | 2.0.1 | La 2.0.1 es la ultima GA (la especificacion pedia 2.0.0) |
| TelegramBots | 10.3.0 | `telegrambots-longpolling` + `telegrambots-client` |
| WhatsApp Cloud API | v21.0 | Graph API de Meta, llamada con `WebClient` |
| PostgreSQL | Supabase | Driver JDBC; esquema creado a mano con SQL |
| Ollama | 0.34.0+ | Modelo local, en `http://localhost:11434` |

### Por que no se usa un starter de Telegram

La especificacion sugeria `io.github.ksilisk:telegram-bot-spring-boot-starter:0.7.0` o
`org.telegram:telegrambots-spring-boot-starter:6.9.7.1`. Ninguno se usa:

- `org.telegram:telegrambots-spring-boot-starter` **quedo congelado en 6.9.7.1** (Bot API
  6.9, de 2023). Su autoconfiguracion no es compatible con Spring Boot 4.x, y la clase
  `TelegramLongPollingBot` que pedia la especificacion **ya no existe** en la libreria
  moderna.
- La linea mantenida de rubenlagus va en la **10.3.0** pero no publica starter de Spring.

Asi que el bot se registra a mano en
[`TelegramBotConfig`](src/main/java/com/legal/bot/config/TelegramBotConfig.java): son dos
beans, y el proyecto queda desacoplado de la version de Spring Boot. La API moderna es
`LongPollingSingleThreadUpdateConsumer` + `TelegramBotsLongPollingApplication`.

## Estructura

```
com.legal.bot
├── LegalBotApplication.java          clase principal
├── config
│   ├── TelegramBotConfig.java        registra el bot de Telegram en LONG_POLLING
│   ├── TelegramBotProperties.java    enlaza telegram.bot.*
│   ├── WhatsAppConfig.java           WebClient hacia la Cloud API (token + base-url)
│   ├── WhatsAppProperties.java       enlaza whatsapp.*
│   ├── OllamaConfig.java             chequeo de arranque: /api/tags
│   └── AsyncConfig.java              pool de hilos del webhook
├── controller
│   └── WebhookController.java        GET (verificacion) y POST (mensajes) de Meta
├── handler
│   ├── LegalMessageHandler.java      updates de Telegram
│   └── WhatsAppMessageHandler.java   decide si el bot responde o no
├── model
│   ├── dto
│   │   ├── WhatsAppMessage.java      un mensaje entrante
│   │   ├── WhatsAppText.java         cuerpo {"body": "..."}
│   │   └── WhatsAppWebhookPayload.java  sobre entry[].changes[].value
│   └── entity
│       ├── Cliente.java              telefono + nombre de perfil
│       ├── Conversacion.java         hilo + estado (ACTIVA_BOT / ATENDIDA_POR_HUMANO)
│       └── Mensaje.java              entrantes y salientes, con wamid unico
├── repository
│   ├── ClienteRepository.java
│   ├── ConversacionRepository.java
│   └── MensajeRepository.java
└── service
    ├── LegalAIService.java           SYSTEM_PROMPT + llamada a Ollama via Spring AI
    ├── WhatsAppService.java          envio de mensajes por la Cloud API
    ├── ConversationService.java      persistencia en Supabase
    └── KnowledgeService.java         carga conocimiento-despacho.json

src/main/resources
├── application.yaml
├── conocimiento-despacho.json
└── db/migration/V1__esquema_inicial.sql   ejecutalo A MANO en Supabase
```

---

## 1. Instalar Ollama

**Windows / macOS:** descarga el instalador de <https://ollama.com/download> y ejecutalo.

**Linux:**

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

Comprueba que el servicio esta arriba (deberia contestar `Ollama is running`):

```bash
curl http://localhost:11434
```

En Windows, Ollama arranca solo y queda en la bandeja del sistema. Si no, abre una
terminal y ejecuta `ollama serve`.

## 2. Descargar el modelo

```bash
ollama pull llama3.1:8b
```

Son ~4.9 GB. Verifica que quedo descargado:

```bash
ollama list
```

> **Nota sobre el tag.** `llama3.1:latest` y `llama3.1:8b` son el mismo modelo (8B,
> cuantizacion Q4_K_M). Si ya tienes `llama3.1:latest`, **no descargues nada**: es el
> valor por defecto de este proyecto. Para usar otro modelo, exporta `OLLAMA_MODEL`
> (por ejemplo `mistral:7b`) o cambia `spring.ai.openai.chat.options.model` en
> `application.yaml`.

Prueba rapida del modelo:

```bash
ollama run llama3.1:latest "Di hola en una linea"
```

## 3. Crear el bot en BotFather y obtener el token

1. Abre Telegram y busca **@BotFather** (el verificado, con el check azul).
2. Envia `/newbot`.
3. Te pedira un **nombre** visible, por ejemplo `Despacho Legal Aguascalientes`.
4. Luego un **username**, que debe ser unico y terminar en `bot`, por ejemplo
   `despacho_legal_ags_bot`.
5. BotFather responde con el token, con esta forma:

   ```
   8123456789:AAFxyzABCDEFghijklMNOPQRstuvWXYZ1234
   ```

6. Copialo y **no lo compartas ni lo subas a git**. Si se filtra, usa `/revoke` en
   BotFather para generar uno nuevo.

Opcional, para que el bot se vea mejor en Telegram:

- `/setdescription` — descripcion del bot
- `/setcommands` — pega esto:

  ```
  start - Ver que puedo consultar
  ayuda - Ver que puedo consultar
  ```

## 4. Configurar variables de entorno

El token se lee de `TELEGRAM_BOT_TOKEN`. No lo escribas en `application.yaml`.

**Windows (PowerShell)** — solo para la sesion actual:

```powershell
$env:TELEGRAM_BOT_TOKEN = "8123456789:AAFxyzABCDEFghijklMNOPQRstuvWXYZ1234"
```

Para dejarlo permanente (abre una terminal nueva despues):

```powershell
setx TELEGRAM_BOT_TOKEN "8123456789:AAFxyzABCDEFghijklMNOPQRstuvWXYZ1234"
```

**Linux / macOS:**

```bash
export TELEGRAM_BOT_TOKEN="8123456789:AAFxyzABCDEFghijklMNOPQRstuvWXYZ1234"
```

Variables opcionales:

| Variable | Default | Para que sirve |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | — | Token de BotFather (obligatorio) |
| `OLLAMA_MODEL` | `llama3.1:latest` | Modelo de Ollama a usar |

## 5. Ejecutar el proyecto

> Desde que se agrego el canal de WhatsApp, la app necesita **tambien** las tres variables
> `SUPABASE_*` para arrancar (JPA abre el pool al iniciar). Estan en la
> [tabla completa de variables](#10-variables-de-entorno).

Con Ollama corriendo y las variables exportadas:

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
./mvnw spring-boot:run
```

Sabras que arranco bien cuando veas en el log:

```
Bot legal registrado en modo LONG POLLING. Escribele desde Telegram.
```

Para generar el jar y ejecutarlo suelto:

```bash
./mvnw clean package
java -jar target/diligencias-0.0.1-SNAPSHOT.jar
```

Si quieres levantar la app **sin** conectarte a Telegram (por ejemplo para probar solo la
parte de IA):

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--telegram.bot.enabled=false
```

## 6. Probar el bot desde Telegram

1. En Telegram, busca el username que registraste (`@despacho_legal_ags_bot`).
2. Pulsa **Start** o envia `/start`. Debe contestar el mensaje de bienvenida al instante.
3. Prueba consultas reales:

   - `¿Cuánto cuesta una diligencia en la ciudad de Aguascalientes?` → $1,000 MXN
   - `¿Y en un municipio del estado?` → $1,300 MXN
   - `¿Llevan divorcios?` → confirma materia familiar
   - `¿Cuál es su horario?` → Lunes a Viernes, 9:00 AM a 7:00 PM
   - `¿Quién ganó el mundial?` → debe negarse amablemente

La primera respuesta tarda mas (el modelo se carga en memoria). Mientras el modelo
piensa, el bot muestra el indicador *escribiendo...*.

---

# Canal de WhatsApp (fase 1)

Ademas de Telegram, el bot atiende WhatsApp por la **Cloud API oficial de Meta**. A
diferencia de Telegram, WhatsApp **si necesita webhook**: una URL publica con HTTPS a la
que Meta manda los mensajes.

Esta fase incluye solo lo minimo: recibir, decidir si se responde, responder y guardar en
Supabase. El panel web, el RAG y las notas de voz llegan en fases siguientes.

## 7. Crear la app de WhatsApp en Meta

1. Entra a <https://developers.facebook.com/> y crea una app de tipo **Business**.
2. Anade el producto **WhatsApp**.
3. En **WhatsApp > API Setup** apunta dos datos:
   - **Phone number ID** del numero de prueba, para `WHATSAPP_PHONE_NUMBER_ID`
   - **Temporary access token**, que sirve para probar pero **caduca en 24 horas**
4. Para un token que no caduque, crea un **usuario del sistema** en
   *Business Settings > System Users*, dale el rol sobre la app y genera un token
   permanente con los permisos `whatsapp_business_messaging` y
   `whatsapp_business_management`. Ese va en `WHATSAPP_ACCESS_TOKEN`.
5. En **API Setup > To**, registra tu numero personal en la lista de destinatarios de
   prueba. Con el numero de prueba de Meta **solo puedes escribir a numeros dados de alta
   ahi**.

## 8. Crear la base de datos en Supabase

1. Crea un proyecto en <https://supabase.com/>.
2. Abre **SQL Editor > New query**, pega el contenido de
   [`src/main/resources/db/migration/V1__esquema_inicial.sql`](src/main/resources/db/migration/V1__esquema_inicial.sql)
   y ejecutalo. Crea tres tablas: `cliente`, `conversacion` y `mensaje`.
3. Copia la cadena de conexion en **Project Settings > Database > Connection string >
   JDBC**. Usa la del **Session pooler** (puerto 5432), no la del Transaction pooler:
   esa ultima no admite prepared statements y Hibernate los usa.

   ```
   jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require&prepareThreshold=0
   ```

> **Ojo:** la app arranca con `spring.jpa.hibernate.ddl-auto=validate`. Si no corriste el
> SQL, falla al inicio con un error de tabla inexistente. Eso es intencional: preferimos
> que avise al arrancar antes que crear tablas por su cuenta en produccion.

## 9. Exponer el webhook y darlo de alta

Meta necesita alcanzar tu maquina por HTTPS. En desarrollo, lo mas rapido es ngrok:

```bash
ngrok http 8080
```

Copia la URL `https://xxxx.ngrok-free.app` y en **Meta for Developers > WhatsApp >
Configuration > Webhook** pulsa *Edit*:

- **Callback URL:** `https://xxxx.ngrok-free.app/webhook`
- **Verify token:** exactamente el valor de `WHATSAPP_VERIFY_TOKEN` (lo inventas tu)

Al pulsar *Verify and save*, Meta manda un `GET /webhook` y la app devuelve el challenge.
En el log veras:

```
Webhook de WhatsApp verificado correctamente por Meta.
```

Despues, en **Webhook fields**, suscribete al campo **messages**. Sin esa suscripcion Meta
verifica la URL pero nunca manda nada.

> La URL de ngrok cambia en cada reinicio, y hay que volver a darla de alta. Para
> produccion, usa un dominio fijo.

## 10. Variables de entorno

| Variable | Obligatoria | Para que sirve |
|---|---|---|
| `SUPABASE_DB_URL` | **si** | Cadena JDBC del Session pooler de Supabase |
| `SUPABASE_DB_USER` | **si** | Usuario de la BD (`postgres` o `postgres.<ref>`) |
| `SUPABASE_DB_PASSWORD` | **si** | Password de la BD |
| `WHATSAPP_ACCESS_TOKEN` | para responder | Token permanente del usuario del sistema |
| `WHATSAPP_PHONE_NUMBER_ID` | para responder | Id del numero emisor |
| `WHATSAPP_VERIFY_TOKEN` | para el alta | Cadena que inventas tu; debe coincidir con Meta |
| `TELEGRAM_BOT_TOKEN` | solo Telegram | Token de BotFather |
| `OLLAMA_MODEL` | no | Modelo de Ollama (`llama3.1:latest` por defecto) |

Las tres de Supabase son obligatorias **para que la app arranque**, tambien si solo te
interesa Telegram: JPA abre el pool al iniciar. Las de WhatsApp no: sin ellas el webhook
sigue recibiendo y guardando, pero no envia respuestas y lo avisa en el log.

**Windows (PowerShell):**

```powershell
$env:SUPABASE_DB_URL = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require&prepareThreshold=0"
$env:SUPABASE_DB_USER = "postgres.abcdefghijklm"
$env:SUPABASE_DB_PASSWORD = "tu-password"
$env:WHATSAPP_ACCESS_TOKEN = "EAAG..."
$env:WHATSAPP_PHONE_NUMBER_ID = "123456789012345"
$env:WHATSAPP_VERIFY_TOKEN = "mi-token-inventado-123"
```

**Linux / macOS:** lo mismo con `export VAR="valor"`.

## 11. Probar el canal de WhatsApp

Con la app corriendo, ngrok arriba y el webhook suscrito, escribe al numero de prueba
desde tu WhatsApp:

| Escribes | Deberia pasar |
|---|---|
| `Hola` | Menu de bienvenida, **sin** llamar al modelo |
| `3` | Opcion 3 del menu, respondida por la IA |
| `4` | Avisa que un abogado retomara, y **el bot deja de responder en ese hilo** |
| Cuanto cuesta una diligencia en un municipio | $1,300 MXN, via Ollama |
| Una nota de voz | "Por ahora solo puedo leer mensajes de texto" |

Sin salir de la terminal tambien puedes simular a Meta. Guarda el cuerpo en un archivo:

```bash
cat > mensaje-prueba.json <<JSON
{
  "object": "whatsapp_business_account",
  "entry": [{
    "id": "1",
    "changes": [{
      "field": "messages",
      "value": {
        "messaging_product": "whatsapp",
        "metadata": {"phone_number_id": "123"},
        "contacts": [{"wa_id": "5214491234567", "profile": {"name": "Prueba"}}],
        "messages": [{
          "from": "5214491234567",
          "id": "wamid.PRUEBA1",
          "timestamp": "1726000000",
          "type": "text",
          "text": {"body": "Llevan divorcios?"}
        }]
      }
    }]
  }]
}
JSON

curl -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  --data @mensaje-prueba.json
```

Devuelve 200 al instante; el trabajo se ve en el log y en la tabla `mensaje`. Repite el
`curl` con el mismo `wamid` y comprueba que la segunda vez **no** responde: es la defensa
contra los reintentos de Meta.

## Como decide el bot si responde

Toda la logica esta en
[`WhatsAppMessageHandler`](src/main/java/com/legal/bot/handler/WhatsAppMessageHandler.java),
en este orden:

1. **`wamid` repetido**: no responde. Meta reintenta el webhook si el 200 tarda; sin esto
   el cliente recibiria la misma respuesta varias veces.
2. **No es texto** (audio, imagen, documento): lo guarda y contesta un aviso fijo, sin
   gastar una llamada al modelo.
3. **Conversacion en `ATENDIDA_POR_HUMANO`**: guarda el mensaje y **calla**, para que el
   abogado atienda sin que la IA se meta en medio. Se activa con la opcion `4` del menu, o
   a mano desde Supabase:

   ```sql
   UPDATE conversacion SET estado = 'ATENDIDA_POR_HUMANO' WHERE id = 1;
   -- y para devolverle el hilo al bot:
   UPDATE conversacion SET estado = 'ACTIVA_BOT' WHERE id = 1;
   ```

4. **Saludo u opcion numerica**: respuesta armada con `conocimiento-despacho.json`.
5. **Cualquier otra cosa**: la responde Ollama.

El POST del webhook devuelve **200 de inmediato** y el procesamiento real corre en el pool
de [`AsyncConfig`](src/main/java/com/legal/bot/config/AsyncConfig.java). Es obligatorio:
una consulta al modelo local tarda mas de lo que Meta espera, y un webhook que responde
tarde o con error acaba desactivado.

## Pendiente para las siguientes fases

- **Validar la firma `X-Hub-Signature-256`.** Hoy el endpoint `POST /webhook` acepta
  cualquier cuerpo bien formado, asi que quien conozca la URL puede inyectar mensajes
  falsos. Antes de dejarlo publico de forma permanente hay que comprobar el HMAC-SHA256
  del cuerpo crudo contra el *App Secret* de Meta.
- Panel web para que los abogados lean y contesten.
- RAG sobre documentos del despacho (hoy el conocimiento va en el system prompt).
- Transcripcion de notas de voz.
- Historial de la conversacion como contexto del modelo: ya se guarda en `mensaje`, y
  `MensajeRepository.findTop20ByConversacionOrderByFechaCreacionDesc` esta listo.

## Tests

```bash
./mvnw test
```

16 tests: contexto, webhook de WhatsApp (verificacion, mensaje, acuses) y la logica de
responder-o-no-responder del handler. No necesita Telegram, ni Ollama, ni Supabase: los
tests usan H2 en memoria y la configuracion de `src/test/resources/application.yaml`.

Para probar de verdad contra Ollama (debe estar corriendo):

```bash
./mvnw test -Dollama.live=true -Dtest=LegalAIServiceLiveTest
```

## Usar Java 21

El proyecto compila hoy con **Java 17** porque ese es el JDK instalado en este equipo
(`OpenLogic 17.0.12`). Spring Boot 4.1 soporta 17, 21 y 25. Para pasar a 21:

1. Instala un JDK 21 (por ejemplo [Adoptium Temurin 21](https://adoptium.net/)).
2. Apunta `JAVA_HOME` a ese JDK.
3. En `pom.xml`, cambia una sola linea:

   ```xml
   <java.version>21</java.version>
   ```

## Problemas comunes

| Sintoma | Causa y solucion |
|---|---|
| `Falta el token de Telegram...` al arrancar | No se exporto `TELEGRAM_BOT_TOKEN`, o lo exportaste en otra terminal. |
| `409 Conflict` en el log | El mismo bot ya corre en otro proceso. Telegram solo admite un consumidor por token: cierra la otra instancia. |
| El bot contesta "En este momento no puedo procesar tu consulta" | Ollama esta apagado, o el modelo configurado no esta descargado. Revisa `ollama list` y `curl http://localhost:11434`. |
| `404 model not found` en el log | El tag de `OLLAMA_MODEL` no existe localmente. Usa uno de `ollama list`. |
| El bot no responde nada | Revisa que el token sea del bot correcto y que no tengas un webhook activo: `curl "https://api.telegram.org/bot<TOKEN>/deleteWebhook"`. |
| Respuestas muy lentas | Normal en CPU. Un modelo mas pequeno ayuda: `OLLAMA_MODEL=llama3.2:3b`. |
| El GET de verificacion devuelve 403 | `WHATSAPP_VERIFY_TOKEN` no coincide con el que pusiste en el panel de Meta. Debe ser identico. |
| Meta verifica el webhook pero no llega ningun mensaje | Falta suscribirse al campo **messages** en *Webhook fields*, o la URL de ngrok cambio. |
| `Table "cliente" not found` / `missing table` al arrancar | No corriste `db/migration/V1__esquema_inicial.sql` en el SQL Editor de Supabase. |
| `Failed to configure a DataSource` al arrancar | Faltan `SUPABASE_DB_URL`, `SUPABASE_DB_USER` o `SUPABASE_DB_PASSWORD`. |
| `PreparedStatement already exists` con Supabase | Estas usando el Transaction pooler (puerto 6543). Cambia al Session pooler (5432) o agrega `prepareThreshold=0`. |
| `WhatsApp sin configurar` en el log | Faltan `WHATSAPP_ACCESS_TOKEN` o `WHATSAPP_PHONE_NUMBER_ID`: recibe y guarda, pero no envia. |
| Graph API responde 401 / `Error validating access token` | El token temporal de Meta caduco (dura 24 h). Genera uno permanente con un usuario del sistema. |
| Graph API responde 131030 `recipient not in allowed list` | Con el numero de prueba solo se puede escribir a numeros dados de alta en *API Setup > To*. |
| El bot dejo de contestar a un cliente en WhatsApp | Su conversacion quedo en `ATENDIDA_POR_HUMANO` (opcion 4 del menu). Devuelvela con `UPDATE conversacion SET estado = 'ACTIVA_BOT' WHERE id = ...`. |
| El cliente recibe la respuesta dos veces | No deberia: el `wamid` es UNIQUE en `mensaje`. Revisa que corriste el SQL con esa restriccion. |

## Nota legal

El bot da orientacion general y datos del despacho. No sustituye la asesoria de un
abogado ni garantiza resultados; para cualquier caso concreto, agenda una consulta.
