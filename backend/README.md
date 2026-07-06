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

## AI endpoints

`AiTriageService` (ticket auto-routing) and `AiChatService` (copilot) are deterministic stand-ins.
Each has a documented single method to replace with a real LLM call (e.g. the Anthropic Messages
API, `claude-opus-4-8`) without touching the controllers.

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
| POST | `/api/ai/chat` | Ask the AI Command Copilot |

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
