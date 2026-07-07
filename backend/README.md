# Praja Disha AI — Backend

Spring Boot (Java 21) + MongoDB backend implementing the **Citizen App**, **Org-Admin Dashboard**,
and **AI Copilot** API contracts described in `api_contracts.md`.

## Running

All configuration lives in [`src/main/resources/application.properties`](src/main/resources/application.properties).

### Against your own MongoDB (default)

Put your connection string in `spring.data.mongodb.uri` (or set the `MONGODB_URI` env var), then:

```bash
mvn spring-boot:run
```

The API is served at `http://localhost:8080`. On first boot it seeds a demo organization (BBMP),
departments, officers, a citizen (`aisha_patel`), and a sample ticket (`PD-8821`).

PowerShell, overriding the URL without editing the file:

```powershell
$env:MONGODB_URI = "mongodb+srv://USER:PASS@cluster.mongodb.net/praja_disha"
mvn spring-boot:run
```

### Zero-setup with embedded MongoDB (`dev` profile)

No database installed? Run with the `dev` profile — it downloads and runs an embedded MongoDB:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

A local Docker Mongo is also provided: `docker compose up -d mongo`
(then use `mongodb://localhost:27017/praja_disha`).

## Authentication note

The contract does not specify an auth scheme, so tenancy is resolved pragmatically:

- **Citizen endpoints** identify the caller via the `X-Citizen-Username` header (returned by
  login/register). If absent it falls back to the seeded demo citizen `aisha_patel`.
- **Org-Admin endpoints** operate on the single "active" organization (the first org on record).

Swap `OrganizationService#getActive` and the header resolution in `CitizenController` for real
session/JWT handling when auth is introduced.

## AI endpoints (Ollama Cloud)

The AI is powered by **Ollama Cloud** through [`OllamaClient`](src/main/java/gov/prajadisha/backend/ai/service/OllamaClient.java):

- **`AiTriageService`** — sends each new ticket plus the org's categories/priorities/departments to
  the model (constrained to a JSON schema) to pick a category, priority and routing department.
- **`AiChatService`** — feeds a live civic-data snapshot (ticket counts by status/priority/category,
  overdue counts, departments) to the model and returns an analytical answer with follow-up suggestions.
- **Embeddings** — on triage, `descriptionEmbedding` is populated via Ollama's `/api/embed` for
  duplicate detection (best-effort).

Every AI call has a deterministic fallback, so the app stays fully functional even with no key or network.

### Configuration

Set your key via the `OLLAMA_API_KEY` env var (preferred) or in `application.properties`:

```powershell
$env:OLLAMA_API_KEY = "your-ollama-cloud-key"
mvn spring-boot:run
```

| Property | Env var | Default |
|----------|---------|---------|
| `ollama.api-key` | `OLLAMA_API_KEY` | _(empty → fallback mode)_ |
| `ollama.base-url` | `OLLAMA_BASE_URL` | `https://ollama.com` |
| `ollama.chat-model` | `OLLAMA_CHAT_MODEL` | `gpt-oss:120b` |
| `ollama.embed-model` | `OLLAMA_EMBED_MODEL` | `embeddinggemma` |
| `ollama.enabled` | `OLLAMA_ENABLED` | `true` |

## File storage (photos / videos / voice)

Media is stored on the **server's local disk** (not in MongoDB) under `app.upload-dir` (default
`./uploads`) and served back from `/files/...`.

- `POST /api/files/upload` (multipart field `file`) → `{ "url": "/files/images/<id>.png", ... }`
- `POST /api/files/upload-multiple` (multipart field `files`) → list of the above

Flow: the client uploads the photo/video/voice first, then submits the ticket with the returned URL as
`imageUrl` / `voiceUrl`. Max sizes are configurable (`spring.servlet.multipart.max-file-size`,
default 100MB) to accommodate video.

## Endpoint map

### Citizen App (`/api/citizen`)
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/auth/login` | Login by phone/email/username |
| POST | `/auth/register` | Create a citizen profile |
| POST | `/tickets` | Submit a civic issue (triggers async triage) |
| POST | `/tickets/{id}/feedback` | Rate a resolved ticket, award points |
| POST | `/tickets/{id}/reopen` | Reopen a ticket |
| PUT | `/profile/language` | Update preferred language |
| POST | `/wallet/redeem` | Redeem points for a transit pass |

### Org-Admin Dashboard
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/organizations/active` | Active organization details |
| GET | `/api/dashboard/tasks` | Triage table rows (`statusType`, `priority`, `page`, `pageSize`) |
| GET | `/api/dashboard/stats` | Live metric counters |
| GET | `/api/tasks/{id}/details` | Unified ticket detail payload |
| POST | `/api/tasks/{id}/comments` | Add a public comment |
| POST | `/api/tasks/{id}/notes` | Add an internal note |
| GET | `/api/officers` | List officers |
| POST | `/api/officers` | Create an officer |
| PUT | `/api/officers/{id}` | Update an officer |
| GET | `/api/departments` | List (nested) departments |
| POST | `/api/departments` | Create a department |
| PUT | `/api/departments/{id}` | Update a department |

### AI Copilot
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/ai/chat` | Ask the AI Command Copilot (Ollama-backed) |

### Files
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/files/upload` | Upload one photo/video/voice file, returns a local URL |
| POST | `/api/files/upload-multiple` | Upload several files at once |
| GET | `/files/**` | Serve a stored media file |

## Quick smoke test

```bash
curl http://localhost:8080/api/organizations/active
curl http://localhost:8080/api/dashboard/tasks
curl -X POST http://localhost:8080/api/citizen/auth/login \
  -H 'Content-Type: application/json' -d '{"identifier":"9876543210"}'
curl -X POST http://localhost:8080/api/citizen/tickets \
  -H 'Content-Type: application/json' -H 'X-Citizen-Username: aisha_patel' \
  -d '{"title":"Broken Streetlight","description":"Flickering at 4th Main","location":"4th Main"}'
```

## Project layout

```
gov.prajadisha.backend
├── citizen   # profiles, wallet, tickets submission (Citizen App)
├── org       # organizations, departments, officers, hierarchy (Org-Admin)
├── task      # tickets, dashboard, detail payload, comments/notes
├── ai        # triage + copilot chat
├── common    # GeoJSON, ids, formatting, error handling
└── config    # CORS, data seeding
```
