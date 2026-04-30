# LegalLens High-Level Design

## 1. Project Summary

LegalLens is a Spring Boot backend that accepts legal contract uploads, parses document text, sends the content to an AI model for risk analysis, evaluates the risk score, and exposes asynchronous APIs to track and retrieve the analysis result.

The project is positioned as an AI-assisted contract risk analysis backend. The architecture demonstrates document ingestion, asynchronous processing, AI integration, persistence, and a domain model designed for future clause-level analysis and contract version tracking.

## 2. Current System Capabilities

### Implemented Today

- Upload legal documents through a multipart REST API.
- Support TXT, PDF, and DOCX parsing.
- Store contract metadata in PostgreSQL using Spring Data JPA.
- Detect exact duplicate uploads using SHA-256 file hash.
- Create or reuse party profiles based on party name.
- Process uploaded contracts asynchronously through a dedicated worker.
- Call OpenAI through Spring AI for legal risk analysis.
- Evaluate AI output into a numeric risk score.
- Track analysis status using `UPLOADING`, `PARSING`, `ANALYZING`, `COMPLETED`, and `FAILED`.
- Expose separate APIs for upload, status polling, and result retrieval.
- Maintain base entities for contract versioning, clauses, clause analysis, negotiation tracking, and suggestion adoption.

### Current API Flow

```text
Client
  -> POST /api/contracts/upload
      -> ContractService
          -> saves ContractEntity
          -> triggers ContractAnalysisWorker asynchronously
          -> returns contractUuid

Client
  -> GET /api/contracts/{uuid}/status
      -> returns progress and analysis status

Client
  -> GET /api/contracts/{uuid}/result
      -> returns completed AI summary and risk score
```

## 3. Current Component Boundaries

### Controller Layer

`ContractController`

Responsibilities:

- Accept HTTP requests.
- Validate required request parameters.
- Delegate business logic to service layer.
- Return upload, status, and result DTOs.

It should not contain document parsing, AI logic, persistence decisions, or risk-scoring logic.

### Application Service Layer

`ContractService`

Responsibilities:

- Validate upload request.
- Compute file hash.
- Detect exact duplicate uploads.
- Create or reuse `PartyProfileEntity`.
- Persist initial `ContractEntity`.
- Trigger background analysis.
- Serve status/result read operations.

This service owns the upload orchestration boundary.

### Background Processing Layer

`ContractAnalysisWorker`

Responsibilities:

- Run analysis outside the upload request path.
- Reload contract and party profile from the database using IDs.
- Parse document content.
- Call AI service.
- Run risk evaluation.
- Persist parsed text, AI summary, score, and final status.

This worker owns the long-running processing boundary.

### Status Management Layer

`ContractStatusService`

Responsibilities:

- Update contract status and progress.
- Centralize status mutation.
- Provide a future integration point for Redis cache eviction and status history.

### Parser Layer

`DocumentParserService`

Responsibilities:

- Convert supported file formats into plain text.
- Keep file-type-specific parsing isolated from business logic.

Supported formats:

- TXT
- PDF
- DOCX

### AI Integration Layer

`AIService`

Responsibilities:

- Build and send prompts through Spring AI.
- Return AI-generated contract analysis text.
- Translate AI/network failures into domain exceptions.

### Risk Evaluation Layer

`RiskEvaluationService`

Responsibilities:

- Convert AI analysis text into a risk score.
- Derive risk level using keyword-based scoring.
- Identify basic red/green risk signals internally.

Current limitation: red/green flags are calculated but not persisted or exposed in the final response.

### Persistence Layer

Repositories:

- `ContractRepository`
- `PartyProfileRepository`
- `ClauseRepository`
- `ClauseAnalysisRepository`
- Negotiation and comparison repositories

Current primary persistence path uses:

- `ContractEntity`
- `PartyProfileEntity`

Several richer entities already exist but are not fully wired into the runtime flow yet.

## 4. Current Data Model

### Active Runtime Entities

`ContractEntity`

- Stores uploaded file metadata.
- Stores parsed text.
- Stores AI summary.
- Stores risk score.
- Tracks status and progress.
- Contains fields for versioning.

`PartyProfileEntity`

- Stores party name, role, jurisdiction, and negotiation preferences.
- Used to analyze contracts from a specific party's perspective.

### Designed But Not Fully Integrated Yet

`ClauseEntity`

- Intended to store individual clauses extracted from the contract.

`ClauseAnalysisEntity`

- Intended to store clause-level risks, benefits, suggestions, flags, and model metadata.

`ClauseChange`

- Intended to track added, removed, or modified clauses between versions.

`NegotiationTracking`

- Intended to track negotiation rounds and accepted/rejected changes.

`SuggestionAdoption`

- Intended to track whether AI suggestions were adopted in revised versions.

## 5. Why Upload, Status, and Result Are Separate APIs

AI analysis can take time. A single synchronous upload-and-result request can timeout or create a poor user experience.

The current design uses an asynchronous job-style pattern:

```text
Upload API:
  accepts file and starts analysis

Status API:
  allows polling progress

Result API:
  returns completed analysis only when ready
```

This is a good backend pattern for long-running document and AI workflows.

## 6. Current Architecture Strengths

- Clear separation between controller, service, parser, AI, worker, and repository layers.
- Async processing boundary is now separated into `ContractAnalysisWorker`.
- Safer async design by passing IDs instead of JPA entities.
- Duplicate detection using SHA-256 hash.
- Domain model already anticipates contract versions, clauses, analysis, and negotiation workflows.
- PostgreSQL is a good choice for structured metadata and JSONB-based AI results.
- API flow is suitable for frontend polling and long-running AI jobs.

## 7. Current Limitations

### Security

All endpoints are currently public. Legal documents are sensitive, so authentication and authorization are required before production use.

Missing:

- User authentication.
- Owner-based access control.
- Admin/user roles.
- Audit logs.
- Secure document storage rules.

### Structured Analysis

The current result stores AI analysis as plain text in `aiSummary`.

Limitations:

- Clause-level analysis is not persisted.
- `redFlags` and `greenFlags` return empty arrays.
- `clauseAnalyses` returns an empty array.
- Results are harder to query, compare, and visualize.

### Versioning

The entity model supports parent-child versions, but upload flow does not yet support revised contract uploads.

Current behavior:

- Same file hash: duplicate.
- Changed file: treated as a new contract.

Needed behavior:

- Changed file uploaded as revision: linked to parent contract and marked as version 2, 3, etc.

### Caching

Redis is not fully integrated yet.

Good Redis use cases:

- Cache completed contract results.
- Cache frequently-polled status responses with short TTL.
- Cache AI prompt/result metadata if needed.
- Later, support queue-backed background processing.

### Reliability

Current async processing is in-process.

Limitations:

- If the application crashes during analysis, in-progress work may be lost.
- No retry policy for failed AI calls.
- No durable job table or queue.

### Database Migrations

The application currently uses Hibernate `ddl-auto: update`.

This is acceptable for local development but not for production-quality schema evolution.

Needed:

- Flyway or Liquibase migrations.
- `ddl-auto: validate` outside local development.

## 8. Completion Scope For Resume-Ready Version

These items should be completed to present the project strongly as a backend engineering project.

### 1. Redis Caching

Add Redis for read optimization.

Scope:

- Add Redis dependency.
- Add Redis service to Docker Compose.
- Configure Spring Cache with Redis.
- Cache completed analysis result.
- Optionally cache status with very short TTL.
- Evict status cache whenever status changes.

Suggested cache policy:

```text
contractResult:
  TTL: 30 minutes to 2 hours
  cache only completed results

contractStatus:
  TTL: 5 to 15 seconds
  useful only during polling
```

### 2. Revised Contract Versioning

Add optional revision support.

Recommended API:

```text
POST /api/contracts/upload
  file
  partyName
  partyRole
  jurisdiction
  parentContractUuid optional
```

Rules:

```text
Same hash:
  duplicate upload

Different hash + parentContractUuid present:
  revised version

Different hash + no parentContractUuid:
  new contract
```

Expected output fields:

```json
{
  "contractUuid": "new-version-uuid",
  "parentContractUuid": "original-contract-uuid",
  "version": 2,
  "isLatestVersion": true,
  "changeSummary": "Clause 5 removed"
}
```

### 3. Clause Extraction

Add a clause extraction step after parsing.

Flow:

```text
parsedText
  -> ClauseExtractionService
  -> List<ClauseEntity>
  -> save clauses
  -> AI clause analysis
```

This unlocks:

- Accurate `totalClauses`.
- Clause-level risk summaries.
- Clause diffing between versions.
- Better frontend display.

### 4. Structured AI Output

Move from free-form AI text to validated JSON.

Target shape:

```json
{
  "overallRisk": "HIGH",
  "riskScore": 7.5,
  "summary": "The contract is high risk...",
  "redFlags": [],
  "greenFlags": [],
  "clauses": []
}
```

Benefits:

- Easier testing.
- Easier persistence.
- Better UI.
- Better comparison between versions.

### 5. Exception Handling

Add handlers for:

- Duplicate upload: `409 Conflict`
- Contract not found: `404 Not Found`
- Analysis not ready: `202 Accepted` or `409 Conflict`
- Invalid upload: `400 Bad Request`
- Unsupported file type: `415 Unsupported Media Type`
- AI unavailable: `503 Service Unavailable`

### 6. Basic Security

Minimum resume-ready security:

- Keep health endpoint public.
- Protect contract APIs.
- Store authenticated user as `uploadedBy`.
- Ensure users can only access their own contracts.

### 7. Integration Tests

Add tests for:

- Upload valid TXT contract.
- Duplicate upload.
- Unsupported file type.
- Status flow.
- Result before completion.
- AI failure marks contract as failed.
- Revised version upload.
- Redis-cached result.

## 9. Future Roadmap

These are good future items but do not need to be completed immediately.

### Durable Job Queue

Move from in-process async to a durable queue.

Options:

- Redis queue
- RabbitMQ
- Kafka
- Database-backed job table

Recommended future design:

```text
Upload API
  -> saves contract
  -> creates AnalysisJob
  -> queue publishes job

Worker
  -> consumes job
  -> retries on failure
  -> updates status
```

### Advanced Contract Diffing

Compare revised contracts at clause level.

Track:

- Added clauses.
- Removed clauses.
- Modified clauses.
- Risk score improvement or regression.
- Suggestions adopted or rejected.

### Object Storage

Move raw document storage out of the database/app server.

Options:

- AWS S3
- Azure Blob Storage
- Local MinIO for development

### Audit Trail

Track:

- Who uploaded contract.
- Who viewed result.
- When analysis was run.
- Which model was used.
- Prompt version.
- Token usage and cost.

### Observability

Add:

- Request correlation IDs.
- Job IDs in logs.
- AI latency metrics.
- Token usage metrics.
- Failure dashboards.

### Multi-Tenant Readiness

Add tenant boundaries:

- Organization ID.
- User roles.
- Per-tenant contract isolation.
- Per-tenant rate limits.

## 10. Suggested Final Architecture

```text
Client
  -> ContractController
      -> ContractService
          -> ContractRepository
          -> PartyProfileRepository
          -> ContractAnalysisWorker

ContractAnalysisWorker
  -> DocumentParserService
  -> ClauseExtractionService
  -> AIService
  -> RiskEvaluationService
  -> ContractStatusService
  -> ClauseRepository
  -> ClauseAnalysisRepository

Read APIs
  -> Redis Cache
  -> PostgreSQL fallback

PostgreSQL
  -> contracts
  -> party_profiles
  -> clauses
  -> clause_analyses
  -> clause_changes
  -> negotiation_tracking
```

## 11. Resume Positioning

Strong resume description:

```text
Built LegalLens, an AI-assisted contract risk analysis backend using Spring Boot, PostgreSQL, Spring AI, asynchronous processing, and Redis caching. Designed upload/status/result APIs for long-running document analysis workflows, implemented document parsing for PDF/DOCX/TXT, SHA-256 duplicate detection, party-aware risk analysis, and a domain model for contract versioning, clause-level analysis, and negotiation tracking.
```

Architecture keywords to mention:

- Asynchronous processing
- Clear service boundaries
- AI integration layer
- Document parsing pipeline
- Domain-driven entity model
- PostgreSQL persistence
- Redis caching
- Contract versioning
- Clause-level risk analysis
- Secure API roadmap
- Failure handling and observability roadmap

## 12. Completion Checklist

### Complete Now

- Redis cache integration.
- Cache eviction through `ContractStatusService`.
- Revised version upload using optional `parentContractUuid`.
- Clause extraction and persistence.
- Structured AI JSON response.
- Better exception handling.
- Basic authentication and ownership checks.
- Integration tests.

### Future Enhancements

- Durable queue-based workers.
- Object storage for uploaded files.
- Advanced semantic clause comparison.
- Prompt versioning and AI cost tracking.
- Audit trail.
- Multi-tenant support.
- Admin dashboard and metrics.
