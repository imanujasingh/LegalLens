# LegalLens

> **AI-powered contract risk analysis backend** — upload a legal contract, get structured risk assessment, clause-by-clause breakdown, and negotiation insights from your party's perspective.

Built with **Java 17 · Spring Boot 3 · Spring AI · PostgreSQL · Redis · Docker**

---

## What it does

LegalLens accepts a contract upload (PDF, DOCX, or TXT), parses it, sends the content to GPT-4o via Spring AI, and returns a structured risk analysis with:

- Overall risk level (NONE → LOW → MEDIUM → HIGH → CRITICAL) and numeric score
- Clause-by-clause risk breakdown with red/green flags
- Party-aware analysis — the same contract reads differently for a Buyer vs a Vendor
- Contract versioning — upload a revised contract and only changed clauses are re-analysed, reusing cached analysis for unchanged ones (cost-optimised LLM usage)
- SHA-256 duplicate detection — same file uploaded twice returns the existing contract UUID

---

## Architecture

```
Client
  └─▶ POST /api/contracts/upload
        └─▶ ContractService
              ├─▶ SHA-256 duplicate check
              ├─▶ PartyProfileEntity (find or create)
              ├─▶ ContractEntity (persist, status = UPLOADING)
              └─▶ ContractAnalysisWorker (async, post-commit)
                    ├─▶ DocumentParserService   (PDF / DOCX / TXT)
                    ├─▶ ClauseExtractionService (regex + hash-based)
                    ├─▶ AIService               (Spring AI → GPT-4o)
                    ├─▶ RiskEvaluationService   (keyword scoring → RiskLevel)
                    └─▶ ContractStatusService   (@CacheEvict on each transition)

Client
  └─▶ GET /api/contracts/{uuid}/status   (Redis-cached, TTL 30 min)
  └─▶ GET /api/contracts/{uuid}/result   (Redis-cached on COMPLETED only)
```

**Key design decisions:**
- Async processing via `@Async` + `ThreadPoolTaskExecutor` — upload returns 202 immediately
- Background worker triggered **after transaction commit** via `TransactionSynchronization.afterCommit()` — prevents race conditions where the worker reloads a contract row before it's visible
- Revised contract uploads reuse prior clause analyses via SHA-256 text hash comparison — only changed clauses incur LLM cost
- Three-tier ChatClient configuration (fast/standard/complex) maps task complexity to model cost

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5, Spring MVC |
| AI | Spring AI 1.1, OpenAI GPT-4o |
| Persistence | PostgreSQL 15, Spring Data JPA |
| Caching | Redis 7, Spring Cache (`@Cacheable` / `@CacheEvict`) |
| Security | Spring Security 6, JWT (jjwt 0.12) |
| Document parsing | Apache PDFBox 3, Apache POI 5, Apache Tika 3 |
| Async | `ThreadPoolTaskExecutor` (core 5, max 10, queue 25) |
| API docs | SpringDoc OpenAPI 2.5 (Swagger UI) |
| Containerisation | Docker, Docker Compose (Postgres + pgAdmin + Redis) |
| Build | Maven 3.9, Maven Wrapper |
| Testing | JUnit 5, Spring Boot Test, H2 (test profile) |

---

## Prerequisites

- Java 17 JDK on PATH
- Docker & Docker Compose (for Postgres + Redis)
- OpenAI API key

---

## Quick start

**1. Start infrastructure**

```bash
docker compose up -d
```

This starts Postgres 15 on `:5432`, pgAdmin on `:5050`, and Redis 7 on `:6379`.

**2. Set environment variables**

```bash
export OPENAI_API_KEY=sk-...
# Optional overrides (defaults shown):
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

**3. Run the application**

```bash
./mvnw spring-boot:run
```

Application starts on `http://localhost:8080`. All API paths are prefixed with `/api`.

**4. Open Swagger UI**

```
http://localhost:8080/api/swagger-ui/index.html
```

---

## API reference

Base path: `/api`

### Upload a contract

```bash
curl -X POST "http://localhost:8080/api/contracts/upload" \
  -F "file=@/path/to/contract.pdf" \
  -F "partyName=Acme Corp" \
  -F "partyRole=CUSTOMER" \
  -F "jurisdiction=India"
```

Returns `202 Accepted`:

```json
{
  "contractUuid": "a1b2c3d4-...",
  "analysisStatus": "UPLOADING",
  "version": 1,
  "isLatestVersion": true,
  "message": "Contract uploaded successfully. Analysis in progress."
}
```

### Upload a revised contract (version 2+)

```bash
curl -X POST "http://localhost:8080/api/contracts/upload" \
  -F "file=@/path/to/contract-v2.pdf" \
  -F "partyName=Acme Corp" \
  -F "partyRole=CUSTOMER" \
  -F "jurisdiction=India" \
  -F "parentContractUuid=a1b2c3d4-..."
```

Only added or modified clauses are sent to the LLM. Unchanged clauses reuse prior analysis.

### Poll status

```bash
curl "http://localhost:8080/api/contracts/{uuid}/status"
```

```json
{
  "contractUuid": "a1b2c3d4-...",
  "analysisStatus": "ANALYZING",
  "progressPercentage": 80,
  "failureReason": null
}
```

Status progression: `UPLOADING → PARSING → ANALYZING → COMPLETED` (or `FAILED`)

### Get result

```bash
curl "http://localhost:8080/api/contracts/{uuid}/result"
```

Returns `200 OK` when `COMPLETED`, `409 Conflict` while still processing.

```json
{
  "contractUuid": "a1b2c3d4-...",
  "analysisStatus": "COMPLETED",
  "overallRisk": "HIGH",
  "riskScore": 7.5,
  "totalClauses": 12,
  "analyzedClauses": 12,
  "version": 1,
  "isLatestVersion": true,
  "redFlags": ["Uncapped liability exposure", "Broad indemnification obligation"],
  "summary": "...",
  "riskDistribution": { "HIGH": 1 }
}
```

### Supported party roles

`BUYER` `SELLER` `EMPLOYER` `EMPLOYEE` `LICENSOR` `LICENSEE`
`LANDLORD` `TENANT` `CLIENT` `VENDOR` `CUSTOMER` `SUPPLIER`

### Error responses

| Status | Scenario |
|---|---|
| `400 Bad Request` | Missing required field, empty file |
| `404 Not Found` | Contract UUID does not exist |
| `409 Conflict` | Duplicate file already uploaded / analysis not yet complete |
| `413 Payload Too Large` | File exceeds 20MB limit |
| `415 Unsupported Media Type` | File type other than PDF, DOCX, TXT |
| `503 Service Unavailable` | OpenAI API unreachable |

---

## Configuration reference

| Property | Default | Description |
|---|---|---|
| `OPENAI_API_KEY` | *(required)* | OpenAI API key |
| `DB_USERNAME` | `postgres` | Postgres username |
| `DB_PASSWORD` | `postgres` | Postgres password |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/postgres` | DB URL |
| `spring.data.redis.host` | `localhost` | Redis host |
| `legal-lens.jwt.secret` | `default-dev-secret-change-in-production` | JWT signing key — **override in production** |
| `spring.servlet.multipart.max-file-size` | `20MB` | Max upload size |

---

## Running tests

```bash
./mvnw test
```

Tests use H2 in-memory database (PostgreSQL-compatibility mode) and a mock OpenAI key. No external services required.

---

## Project structure

```
src/main/java/com/contractGuard/LegalLens/
├── config/          # Security, async, Redis cache, AI clients, OpenAPI
├── controller/      # ContractController (upload, status, result)
├── exception/       # GlobalExceptionHandler + domain exceptions
├── model/
│   ├── dto/         # Request/response DTOs
│   ├── entity/      # JPA entities (Contract, Clause, Party, etc.)
│   └── enums/       # AnalysisStatus, RiskLevel, ClauseType, etc.
├── repository/      # Spring Data JPA repositories
└── service/
    ├── ai/          # AIService, PromptBuilder
    ├── parser/      # DocumentParserService (PDF, DOCX, TXT)
    ├── ContractAnalysisWorker.java   # Async background processor
    ├── ContractService.java          # Upload orchestration
    ├── ContractStatusService.java    # Status transitions + cache eviction
    ├── ClauseExtractionService.java  # Regex clause segmentation
    └── RiskEvaluationService.java    # Keyword-based risk scoring
```

---

## Roadmap

- [ ] JWT-protected endpoints (UserDetailsService wired, filter already in chain)
- [ ] Flyway database migrations (replace `ddl-auto: update`)
- [ ] Durable job queue (Kafka or Redis Streams) for retry-safe async processing
- [ ] Structured JSON AI output (replace free-text summary with validated schema)
- [ ] Object storage for uploaded files (AWS S3 / GCS)
- [ ] Prometheus metrics + Grafana dashboard
- [ ] Multi-tenant support (org isolation, per-tenant rate limits)

---

## License

MIT — see [LICENSE](LICENSE)
