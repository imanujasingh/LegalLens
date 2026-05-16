# LegalLens

LegalLens is a Spring Boot application that analyzes uploaded contracts using a document parser, simple AI integration, and a risk evaluation pipeline. It is a backend prototype that demonstrates how a contract upload can be parsed, analyzed (via an AI service), and returned as a structured analysis response.

This README explains what the project is, how to configure and run it, the main API endpoints, example requests, common runtime problems, and quick troubleshooting steps.

---

## Tech stack
- Java 17
- Spring Boot (Spring Framework 6.x / Spring Boot 3.x series)
- Maven
- Optional integrations: OpenAI (via Spring AI), Redis (for caching/health checks), a relational database (JPA repositories are present)

## What this project does
- Accepts a contract file upload (multipart/form-data) and optional parameters (party name / partyId, contractName).
- Parses the document (via `DocumentParserService`).
- Sends parsed text to an AI analysis service (`AIService`) to get clause summaries and risk-relevant text.
- Runs a risk evaluation (`RiskEvaluationService`) and builds a `ContractAnalysisResponse` with:
  - `contractUuid`, `filename`, `analysisDate`, `analysisStatus`
  - counts for clauses, analyzed clauses
  - `overallRisk`, `riskScore`, `redFlags`, `greenFlags`
  - a concise `summary` and a `riskDistribution` map

(See `src/main/java/com/contractGuard/LegalLens/service/ContractService.java` for the response-building logic.)

## Prerequisites
- Java 17 JDK installed and available on PATH.
- Maven (or use the bundled `mvnw` wrapper).
- If you plan to use the real OpenAI integration, an OpenAI API key.
- If you configure a relational database, a running DB (Postgres/MySQL) and JDBC URL. Alternatively, add H2 to the classpath for an embedded DB.
- Redis is optional; if the app cannot reach Redis a health check warning will appear (it does not necessarily stop the app unless you rely on Redis-based beans).

## Configuration
Application configuration is read from `src/main/resources/application.yaml` and environment variables. The following properties are commonly required in development:

- Database (if you have a DB):
  - `spring.datasource.url` (e.g. `jdbc:postgresql://localhost:5432/legal_lens`)
  - `spring.datasource.username`
  - `spring.datasource.password`
  - Or add H2 dependency for an embedded DB.

- OpenAI (Spring AI):
  - `spring.ai.openai.api-key` or `SPRING_AI_OPENAI_API_KEY` environment variable
  - If not provided, OpenAI-related auto-configuration will fail with an error like: "OpenAI API key must be set..."

- Redis (optional):
  - `spring.redis.host` (default `localhost`) and `spring.redis.port` (default `6379`) if you enable Redis features.
  - Redis connection failures appear as warnings in logs: "Unable to connect to Redis".

Setting environment variables (PowerShell example):

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://dbhost:5432/legal_lens"
$env:SPRING_DATASOURCE_USERNAME = "dbuser"
$env:SPRING_DATASOURCE_PASSWORD = "dbpass"
$env:SPRING_AI_OPENAI_API_KEY = "sk-xxxxx"
```

You can also add these values to `application.yaml` or a profile-specific YAML file (e.g., `application-dev.yaml`).

## Run (development)
From the project root:

```powershell
# Run with the wrapper
./mvnw spring-boot:run
# Or build and run the jar
./mvnw -DskipTests package
java -jar target/legal-lens-backend-1.0.0.jar
```

If you see an application startup failure that mentions a missing DataSource URL, either provide a database configuration or add an embedded DB dependency (H2) to `pom.xml` for testing.

## API
The main endpoint exposed by the sample controller accepts contract uploads and returns an analysis response.

### POST /upload
- Content-Type: multipart/form-data
- Form fields:
  - `file` (required) — the contract file to upload (handled as `MultipartFile` in the controller)
  - `partyId` or `partyName` (optional) — identifies the party for which the analysis is performed
  - `contractName` (optional) — a friendly name to use for the contract; if omitted the original filename is used

Example curl (multipart/form-data):

```bash
curl -v -X POST "http://localhost:8080/upload" \
  -F "file=@/path/to/contract.pdf" \
  -F "partyName=Acme Corp" \
  -F "contractName=Acme Service Agreement"
```

Notes about the controller signature
- The controller expects a multipart file via `@RequestParam("file") MultipartFile file`. Do not send the file as JSON or `@RequestBody` unless you change the controller to accept base64-encoded payloads.
- The optional parameters (`partyId`, `partyName`, `contractName`) are sent as additional form fields and are exposed as `@RequestParam` parameters in the controller.

## Response example (simplified)
The response object is of type `ContractAnalysisResponse`. Example (JSON-like):

{
  "contractUuid": "...",
  "filename": "contract.pdf",
  "analysisDate": "2026-03-18T...",
  "analysisStatus": "COMPLETED",
  "totalClauses": 12,
  "analyzedClauses": 12,
  "overallRisk": "MEDIUM",
  "riskScore": 45,
  "redFlags": ["Clause 5: ambiguous liability"],
  "greenFlags": ["Clause 2: clear payment terms"],
  "summary": "Clause-level risk summary: ...",
  "riskDistribution": { "MEDIUM": 12 }
}

(Fields and exact structure are defined in `src/main/java/com/contractGuard/LegalLens/model/dto/ContractAnalysisResponse.java`.)

## Troubleshooting
- Error: "Failed to configure a DataSource: 'url' attribute is not specified" — set `spring.datasource.url` or add an embedded DB (H2) dependency.
- Error: "OpenAI API key must be set" — set `spring.ai.openai.api-key` or the `SPRING_AI_OPENAI_API_KEY` environment variable.
- Redis warnings: "Unable to connect to Redis" — either start a Redis instance at the configured host/port or disable Redis health checks / configuration if you don't use it.

## Testing
- Unit tests are under `src/test/java`. Run them with:

```powershell
./mvnw test
```

## Contributing and next steps
- Remove runtime logs from the repository and add `/logs/` to `.gitignore` to avoid committing generated artifacts.
- Add profile-specific configuration (e.g., `application-dev.yaml`) to make local development easier.
- Add a CURL / Postman collection for the most common flows.
