# LegalLens — Architecture & Design

## 1. Project Summary

LegalLens is a Spring Boot backend that accepts legal contract uploads, parses document text, sends content to GPT-4o via Spring AI for risk analysis, evaluates a risk score, and exposes asynchronous APIs to track and retrieve analysis results.

---

## 2. Current System Capabilities

### Implemented and Operational

- Upload contracts via multipart REST API (`POST /api/contracts/upload`)
- Parse PDF, DOCX, and TXT formats (Apache PDFBox, POI, Tika)
- SHA-256 duplicate detection — rejects exact re-uploads immediately
- Party-aware analysis — same contract analysed differently for Buyer vs Vendor
- Async background processing via `@Async` + `ThreadPoolTaskExecutor`
- Background worker triggered **after transaction commit** (prevents worker/DB race conditions)
- Clause extraction via regex-based segmentation with SHA-256 text hashing
- Full contract analysis via OpenAI GPT-4o through Spring AI
- Revised contract versioning — only added/modified clauses incur LLM cost; unchanged clauses reuse prior analysis
- Keyword-based risk scoring → `RiskLevel` enum (NONE / LOW / MEDIUM / HIGH / CRITICAL)
- Status tracking: `UPLOADING → PARSING → ANALYZING → COMPLETED / FAILED`
- Redis caching for both status polling and completed results (`@Cacheable`)
- Cache eviction on every status transition (`@CacheEvict` in `ContractStatusService`)
- Global exception handling for all domain error cases (400 / 404 / 409 / 413 / 415 / 503)
- JWT authentication filter wired into Spring Security filter chain
- SpringDoc OpenAPI / Swagger UI at `/api/swagger-ui/index.html`
- Docker Compose setup for local Postgres + pgAdmin + Redis

### Domain Entities Defined (partially integrated)

| Entity | Status |
|---|---|
| `ContractEntity` | Fully integrated — stores metadata, parsed text, AI summary, risk score, status |
| `PartyProfileEntity` | Fully integrated — drives party-aware LLM prompting |
| `ClauseEntity` | Integrated — persisted after extraction, linked to analyses |
| `ClauseAnalysisEntity` | Integrated — persisted per clause with risk score and raw AI output |
| `ClauseChange` | Integrated — tracks ADDED / MODIFIED / REMOVED / UNCHANGED per revision |
| `NegotiationTracking` | Defined, not yet wired into API flow |
| `SuggestionAdoption` | Defined, not yet wired into API flow |
| `ContractStatusHistory` | Defined, appended on status transitions |

---

## 3. Component Boundaries

### ContractController
- Accepts HTTP requests
- Validates required params
- Delegates to `ContractService`
- Returns upload, status, and result DTOs

### ContractService
- Validates upload
- Computes SHA-256 file hash for duplicate detection
- Creates or reuses `PartyProfileEntity`
- Persists initial `ContractEntity` with `UPLOADING` status
- Registers `TransactionSynchronization.afterCommit()` to trigger async worker
- Serves status and result reads

### ContractAnalysisWorker
- Runs asynchronously on `contractAnalysisExecutor` thread pool (core 5, max 10, queue 25)
- Reloads contract and party profile by ID (not by JPA entity reference)
- Parses document bytes
- Extracts clauses, computes SHA-256 hashes
- Compares against parent version clauses (if revision) — skips unchanged, re-analyses changed
- Calls AI service only for new or modified content
- Evaluates risk score
- Persists clause analyses (new or cloned from prior version)
- Updates contract status at each stage

### ContractStatusService
- Centralises status mutation
- Issues `@CacheEvict` for both `contractStatus` and `contractResult` on every status change
- Uses `REQUIRES_NEW` transaction to ensure status persists even if outer transaction rolls back

### ClauseExtractionService
- Regex pattern: numbered clauses (`1. Title: body text`)
- Assigns `ClauseType` via keyword matching on title + body
- Computes SHA-256 text hash for each clause (used for revision diffing)
- Falls back to single-clause extraction if no numbered structure found

### AIService
- Builds prompt via `PromptBuilder` using party profile context
- Calls `standardChatClient` (GPT-4o, temperature 0.1)
- Returns plain-text analysis
- Wraps network and API failures as `AiUnavailableException` → 503

### RiskEvaluationService
- Keyword-based scoring of AI output text
- Produces `RiskEvaluationResult` with `RiskLevel`, `riskScore`, `redFlags`, `greenFlags`

### DocumentParserService
- PDF: Apache PDFBox (position-sorted extraction)
- DOCX: Apache POI XWPF
- TXT: UTF-8, normalised whitespace

---

## 4. Async Processing Design

```
Upload request (HTTP thread)
  └─▶ ContractService.uploadContract()
        ├─▶ persist ContractEntity (status = UPLOADING)
        ├─▶ register afterCommit hook
        └─▶ return 202 to client

Transaction commits
  └─▶ afterCommit fires
        └─▶ contractAnalysisWorker.analyzeContractAsync(contractId, fileBytes, partyProfileId)
              [runs on analysis-N thread, outside HTTP request thread]
```

The `afterCommit` pattern ensures the worker never reads a contract row before it's committed and visible. Without this, the worker could start on a Postgres row that doesn't exist yet in a READ COMMITTED isolation level.

---

## 5. Caching Design

```
contractStatus cache:
  Key: contractUuid
  TTL: 30 minutes (per RedisCacheConfig default)
  Populated: ContractService.getStatus()
  Evicted: ContractStatusService.updateStatus() on every status transition

contractResult cache:
  Key: contractUuid
  TTL: 30 minutes
  Populated: ContractService.getResult() — only when COMPLETED
  Condition: unless = "analysisStatus != 'COMPLETED'"
  Evicted: ContractStatusService.updateStatus() on every status transition
```

Status is evicted on every transition so polling clients always see fresh progress. The result cache only populates on `COMPLETED` to avoid caching an incomplete response.

---

## 6. Security Design

```
Request
  └─▶ JwtAuthenticationFilter (OncePerRequestFilter)
        ├─▶ Reads Authorization: Bearer <token>
        ├─▶ Validates HMAC-SHA256 signature + expiry
        ├─▶ Extracts subject + roles from Claims
        └─▶ Sets UsernamePasswordAuthenticationToken in SecurityContext

SecurityConfig:
  PUBLIC:    /actuator/health, /v3/api-docs/**, /swagger-ui/**
  CONTRACTS: /contracts/** (permitAll for dev; replace with .authenticated() for prod)
  DEFAULT:   .authenticated()
```

JWT filter is active and validates tokens. Contract endpoints are `permitAll` for development convenience. Switching to authenticated requires wiring a `UserDetailsService` — the filter chain is already in place.

---

## 7. Revision / Versioning Flow

```
Upload V2 (parentContractUuid = V1.uuid)
  └─▶ ContractService
        ├─▶ mark V1.isLatestVersion = false
        ├─▶ persist V2 with parentContract = V1, version = 2, isLatestVersion = true
        └─▶ trigger worker

Worker
  ├─▶ load V1 clauses → Map<hash, ClauseEntity>, Map<clauseNumber, ClauseEntity>
  ├─▶ extract V2 clauses
  ├─▶ for each V2 clause:
  │     same hash → UNCHANGED → clone prior analysis, save ClauseChange(UNCHANGED)
  │     same number, different hash → MODIFIED → re-analyse, save ClauseChange(MODIFIED)
  │     new number → ADDED → analyse fresh, save ClauseChange(ADDED)
  └─▶ V1 clauses not matched → save ClauseChange(REMOVED)
```

Only MODIFIED and ADDED clauses are sent to the LLM. This is the key cost-efficiency design.

---

## 8. Identified Limitations (Honest Accounting)

| Area | Current State | Next Step |
|---|---|---|
| Auth | JWT filter active, endpoints permitAll | Wire UserDetailsService, switch to .authenticated() |
| DB migrations | `ddl-auto: update` | Replace with Flyway |
| AI output | Free-text plain English | Move to structured JSON response |
| redFlags / greenFlags | Computed in RiskEvaluationResult but not copied to ContractAnalysisResponse | Wire through from(contract) |
| Job durability | In-process async — lost on JVM crash | Move to Kafka or Redis Streams job queue |
| File storage | File bytes held in memory during analysis | Move to S3 / GCS object storage |
| Observability | SLF4J logs only | Add Prometheus metrics, correlation IDs |

---

## 9. Technology Decisions

**Why Spring AI instead of raw OpenAI HTTP client?**
Spring AI provides a clean abstraction over model providers. Switching from OpenAI to another provider (Anthropic, Azure OpenAI, local Ollama) requires only a config change, not application code changes. It also handles retry, streaming, and structured output out of the box.

**Why three ChatClient beans?**
Fast tasks (clause keyword extraction) don't need GPT-4o. `gpt-4o-mini` gives ~10x cost reduction. Deterministic tasks (structured scoring) use temperature 0.0. The bean separation enforces this at injection time so individual services can't accidentally use the wrong tier.

**Why afterCommit for async trigger?**
Triggering `@Async` inside a `@Transactional` method means the async thread may try to load the persisted entity before the transaction commits. `TransactionSynchronization.afterCommit()` guarantees the entity is visible before the worker reads it.

**Why Redis cache with eviction on status update?**
Status polling can be high-frequency for large contracts. Caching prevents repeated DB reads. Evicting on every status change means the cache is always consistent — there's no stale-status window where a client sees an old status.

---

## 10. Roadmap

- Flyway migrations
- UserDetailsService + enforced JWT auth on contract endpoints
- Structured JSON AI output with validated schema
- Durable job queue (Kafka or Redis Streams)
- Clause-level red/green flags wired into API response
- AWS S3 / GCS for uploaded file storage
- Prometheus metrics (analysis latency, LLM token usage, error rates)
- Multi-tenant isolation (org ID on all entities)
