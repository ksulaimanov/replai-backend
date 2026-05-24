# replai-backend

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-jjwt_0.12-000000?logo=jsonwebtokens&logoColor=white)](https://github.com/jwtk/jjwt)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Brevo](https://img.shields.io/badge/Brevo_REST_API-v3-0092FF)](https://developers.brevo.com/)

> REST API-ядро платформы replAI. Управляет аутентификацией, мультитенантными ботами, вебхуками Telegram, базой знаний и аналитикой лидов.

---

## Архитектурные принципы

### Stateless File Processing
Загружаемые файлы баз знаний (PDF, TXT, MD) никогда не сохраняются на диск. Бинарный контент считывается в heap (`byte[]`), и напрямую форвардируется в AI-сервис через `RestTemplate` с `ByteArrayResource` — без `java.io.File`, без временных директорий, без утечки данных клиентов на сервере.

```
MultipartFile → byte[] → ByteArrayResource → HTTP POST /knowledge/upload → ChromaDB
```

### Tenant Isolation
Каждый арендатор (компания) владеет ровно одним ботом. Все запросы проходят через `SecurityUtils.getCurrentUserEmail()` → `botRepository.findByOwner_Email()` — данные чужих компаний физически недостижимы без изменения токена.

### Telegram Webhook Pipeline

Асинхронный трёхфазный пайплайн обработки входящего сообщения Telegram:

```
WebhookController.handleTelegramWebhook()
  │
  ├─ [Phase 1 — @Transactional] WebhookPersistenceService.persistIncoming()
  │    ├─ Валидация X-Telegram-Bot-Api-Secret-Token (→ 403 при несовпадении)
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
       └─ split by ||| → отправить каждую часть
```

AI-запрос выполняется **вне транзакции** — соединение с БД не висит 30 секунд.

### 200 OK Guard (Защита от ретраев Telegram)
Telegram повторяет вебхук до тех пор, пока не получит 2xx. При `IllegalStateException` (незарегистрированный токен) контроллер пишет `WARN` в лог и возвращает `200 OK` — Telegram перестаёт слать повторы.

### SpEL Secret Injection
`MAIL_PASSWORD` инжектируется через SpEL-выражение с `?.trim()` прямо на этапе инициализации бина:
```java
@Value("#{environment['MAIL_PASSWORD']?.trim()}")
private String brevoApiKey;
```
Лишние `\r`, `\n` и пробелы удаляются **до** первого использования.

---

## API контракты

### Аутентификация

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/auth/register` | Регистрация + отправка email-кода |
| `POST` | `/api/auth/login` | Вход → JWT |
| `POST` | `/api/auth/verify` | Подтверждение email |
| `POST` | `/api/auth/resend-code` | Повторная отправка кода |

### Бот и файлы (требует Bearer JWT)

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/bot/config` | Конфигурация бота |
| `PUT` | `/api/bot/config` | Обновление имени/промпта |
| `GET` | `/api/bot/files` | Список файлов базы знаний |
| `POST` | `/api/bot/files` | Загрузка файла (max 5 MB, .txt/.md/.pdf) |
| `DELETE` | `/api/bot/files/{id}` | Удаление файла |
| `GET` | `/api/bot/leads` | Список лидов с историей чата |
| `GET` | `/api/bot/analytics` | Аналитика конверсии |

### Вебхук Telegram (публичный)

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/webhook/telegram/{token}` | Входящее обновление Telegram |

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

## Локальная разработка

### Зависимости

- Java 17+
- Maven 3.9+
- PostgreSQL 15 (или Docker)

### Запуск PostgreSQL через Docker

```bash
docker run -d --name replai-pg \
  -e POSTGRES_DB=replai-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  -p 5433:5432 postgres:15
```

### Переменные окружения (`.env` или системные)

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

### Сборка и запуск

```bash
# Запуск в dev-режиме (с mock AI)
mvn spring-boot:run

# Запуск тестов
mvn test

# Сборка JAR
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## Docker

```bash
# Сборка образа
docker build -t replai-backend:latest .

# Запуск (требует переменных окружения)
docker run -d --name replai-backend \
  --env-file .env \
  -p 8080:8080 \
  replai-backend:latest
```

Для продакшена используйте `docker-compose.yml` из корневого репозитория.

---

## Структура проекта

```
src/main/java/com/replai/backend/
├── controller/         REST-эндпоинты
├── service/            Бизнес-логика
├── entity/             JPA-сущности
├── repository/         Spring Data репозитории
├── dto/                Request/Response DTO
├── security/           JWT-фильтр, SecurityConfig, SecurityUtils
└── exception/          Кастомные исключения
```

---

## Контакты

Email: [ksulaimanov.dev@gmail.com](mailto:ksulaimanov.dev@gmail.com) · Telegram: [@ksulaimanov](https://t.me/ksulaimanov)
