# replai-backend

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-jjwt_0.12-000000?logo=jsonwebtokens&logoColor=white)](https://github.com/jwtk/jjwt)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Brevo](https://img.shields.io/badge/Brevo_REST_API-v3-0092FF)](https://developers.brevo.com/)

> The REST API core of the replAI platform. Manages authentication, multi-tenant bots, Telegram webhooks, Knowledge Base (KB), and lead analytics.

---

## Architectural Principles

### Stateless File Processing
Knowledge Base files (PDF, TXT, MD) are never persisted to disk. Binary content is read directly into the heap (`byte[]`) without writing to `java.io.File`, eliminating local disk I/O vulnerabilities — and forwarded to the AI service via `RestTemplate` with `ByteArrayResource`. Zero-disk in-memory streaming via `RestTemplate` and `ByteArrayResource` ensures no temporary directories are created and no cross-tenant data leaking occurs on the server.

```
MultipartFile → byte[] → ByteArrayResource → HTTP POST /knowledge/upload → ChromaDB
```

### Tenant Isolation
Multi-tenant isolation: Each tenant (company) owns exactly one bot. All requests pass through `SecurityUtils.getCurrentUserEmail()` → `botRepository.findByOwner_Email()` — another company's data is physically unreachable without tampering with the JWT token.

### Telegram Webhook Pipeline

Asynchronous Three-Phase Pipeline for processing an incoming Telegram message:

```
WebhookController.handleTelegramWebhook()
  │
  ├─ [Phase 1 — @Transactional] WebhookPersistenceService.persistIncoming()
  │    ├─ Validate X-Telegram-Bot-Api-Secret-Token (→ 403 on mismatch)
  │    ├─ find/create Chat
  │    ├─ save incoming Message
  │    └─ extractLead (regex → Lead entity)
  │
  ├─ [Phase 2 — NO TRANSACTION] AiService.generateReply()
  │    └─ HTTP POST → FastAPI → AiChatResponseDTO(reply, is_lead, lead_summary)
  │
  ├─ [Phase 3 — @Transactional] WebhookPersistenceService.persistReply()
  │    ├─ save bot Message
  │    └─ if is_lead → Chat.status=HOT_LEAD, Chat.leadSummary=...
  │
  └─ [Phase 4 — NO TRANSACTION] TelegramService.sendMessage()
       └─ split by ||| → send each part as a separate message
```

The AI request is executed **outside any transaction** — the database connection is not held open for the duration of the LLM call.

### 200 OK Guard — Request Flooding Protection

Telegram retries a webhook delivery until it receives a 2xx response. On `IllegalStateException` (unregistered token), the controller logs a `WARN` and returns `200 OK` — preventing Telegram from endlessly re-delivering the same update. This is the '200 OK Guard' against request flooding via retry storms.

### SpEL Secret Injection
`MAIL_PASSWORD` is injected via a SpEL expression with `?.trim()` at bean initialization time:
```java
@Value("#{environment['MAIL_PASSWORD']?.trim()}")
private String brevoApiKey;
```
Trailing `\r`, `\n`, and whitespace characters are stripped **before** the value is first used.

---

## API Contracts

### Authentication

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register account + send email verification code |
| `POST` | `/api/auth/login` | Authenticate → JWT |
| `POST` | `/api/auth/verify` | Confirm email address |
| `POST` | `/api/auth/resend-code` | Resend verification code |

### Bot & Files (requires Bearer JWT)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/bot/config` | Retrieve bot configuration |
| `PUT` | `/api/bot/config` | Update bot name / system prompt |
| `GET` | `/api/bot/files` | List Knowledge Base (KB) files |
| `POST` | `/api/bot/files` | Upload KB file (max 5 MB, .txt/.md/.pdf) |
| `DELETE` | `/api/bot/files/{id}` | Delete KB file |
| `GET` | `/api/bot/leads` | List leads with full chat history |
| `GET` | `/api/bot/analytics` | Conversion analytics |

### Telegram Webhook (public)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/webhook/telegram/{token}` | Incoming Telegram update |

---

## Entity Model

```
User (1) ──── (1) Bot
                   │
         ┌─────────┼─────────┐
         ▼         ▼         ▼
      Channel    Chat    KnowledgeBase
     (Telegram)   │
                  ├─ Message[]
                  ├─ Lead
                  └─ leadSummary
```

---

## Local Development

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 15 (or Docker)

### Start PostgreSQL via Docker

```bash
docker run -d --name replai-pg \
  -e POSTGRES_DB=replai-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  -p 5433:5432 postgres:15
```

### Environment Variables (`.env` or system env)

```env
DATABASE_URL=jdbc:postgresql://localhost:5433/replai-postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=root
MAIL_FROM=no-reply@replai.app
MAIL_PASSWORD=<brevo_api_key>
AI_SERVICE_URL=http://localhost:8000
AI_SERVICE_INTERNAL_KEY=secret
AI_SERVICE_MOCK=true
```

### Build & Run

```bash
# Run in dev mode (with mock AI)
mvn spring-boot:run

# Run tests
mvn test

# Build JAR
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## Docker

```bash
# Build image
docker build -t replai-backend:latest .

# Run (requires environment variables)
docker run -d --name replai-backend \
  --env-file .env \
  -p 8080:8080 \
  replai-backend:latest
```

For production use the `docker-compose.yml` from the root repository.

---

## Project Structure

```
src/main/java/com/replai/backend/
├── controller/         REST endpoints
├── service/            Business logic
├── entity/             JPA entities
├── repository/         Spring Data repositories
├── dto/                Request/Response DTOs
├── security/           JWT filter, SecurityConfig, SecurityUtils
└── exception/          Custom exceptions
```

---

## Contact

Email: [ksulaimanov.dev@gmail.com](mailto:ksulaimanov.dev@gmail.com) · Telegram: [@ksulaimanov](https://t.me/ksulaimanov)
