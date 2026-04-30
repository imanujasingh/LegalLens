# Expected output for high-risk-service-agreement.txt

Upload endpoint:

```bash
curl -X POST "http://localhost:8080/contracts/upload" \
  -F "file=@test-documents/high-risk-service-agreement.txt" \
  -F "partyName=Atlas Retail Private Limited" \
  -F "partyRole=CUSTOMER" \
  -F "jurisdiction=India"
```

Expected upload response shape:

```json
{
  "contractUuid": "<generated-uuid>",
  "analysisStatus": "UPLOADING",
  "message": "Contract uploaded successfully. Analysis in progress."
}
```

Poll:

```bash
curl "http://localhost:8080/contracts/<generated-uuid>/status"
```

Expected status progression:

```text
analysisStatus=UPLOADING, progressPercentage=0
analysisStatus=PARSING, progressPercentage=10
analysisStatus=ANALYZING, progressPercentage=40 or 80
analysisStatus=COMPLETED, progressPercentage=100
```

If the AI call or configuration fails, status will become `FAILED` and `failureReason` will contain the AI/config/network error.

Result:

```bash
curl "http://localhost:8080/contracts/<generated-uuid>/result"
```

Expected result shape:

```json
{
  "contractId": 1,
  "contractUuid": "<generated-uuid>",
  "filename": "high-risk-service-agreement.txt",
  "analysisDate": "<timestamp>",
  "analysisStatus": "COMPLETED",
  "overallRisk": "CRITICAL",
  "riskScore": 10.0,
  "totalClauses": 0,
  "analyzedClauses": 0,
  "version": 1,
  "isLatestVersion": true,
  "riskDistribution": {
    "CRITICAL": 1
  },
  "clauseAnalyses": [],
  "redFlags": [],
  "greenFlags": [],
  "negotiationStrategy": null,
  "summary": "<plain-English AI analysis text>",
  "improvementScore": null,
  "comparison": null,
  "suggestionsAdopted": []
}
```

Why the expected risk is `CRITICAL`:

The document intentionally includes these risk terms and scenarios:

- High-risk phrasing expected from AI: `high risk due to ...`
- Payment penalty: `penalty of 5 percent per month`
- Broad indemnity: `indemnify, defend, and hold harmless`
- Liability exposure: `uncapped` and `unlimited liability`
- Immediate termination: `immediately terminate ... without notice or cure period`
- Waiver: `Customer waives any right`
- Jurisdiction mismatch: India party, Singapore law and Singapore arbitration
- One-sided payment during disputes
- Vendor-owned deliverables with only an internal-use license

The current `RiskEvaluationService` scores the AI response by keyword:

- `critical risk`: +4
- `high risk`: +3
- `uncapped` or `unlimited liability`: +2
- `penalty` or `liquidated damages`: +1
- `indemnify` or `hold harmless`: +1.5
- `waiver`: +1
- `immediate termination`: +1
- Score is capped at 10

The prompt asks the AI to use `high risk` for serious exposure. If the AI also says `critical risk`, or repeats enough detected terms, the expected `riskScore` is `10.0` and `overallRisk` is `CRITICAL`. If the AI is less explicit, acceptable fallback outcomes are:

- `HIGH` with score from `6.0` to `7.9`
- `MEDIUM` with score from `4.0` to `5.9`

For this specific document, `LOW` or `NONE` should be treated as incorrect unless the AI response omitted most of the risk keywords.

Expected AI summary content:

The `summary` field should be plain English, not JSON. It should identify serious risk for Atlas Retail Private Limited as Customer, especially:

- One-sided payment and dispute terms
- Vendor-favorable termination rights
- Uncapped Customer liability and limited Vendor liability
- Broad Customer indemnity
- Waiver of Customer remedies
- IP ownership favoring Vendor
- Singapore governing law/arbitration despite India jurisdiction

The AI summary should end with a section titled exactly:

```text
Clause-level risk summary:
```

Expected clause-level bullets in the AI summary:

```text
- Clause 1 (Services): Vendor may use subcontractors without prior approval, creating operational and accountability risk for Customer.
- Clause 2 (Fees and Payment): Customer must pay disputed invoices and faces a 5 percent monthly penalty, creating high financial exposure.
- Clause 3 (Term): Customer is locked in for two years without convenience termination, limiting exit flexibility.
- Clause 4 (Service Levels): Vendor has weak service-level accountability because missed service levels do not automatically create credits.
- Clause 5 (Confidentiality): Vendor may retain confidential information for seven years, extending post-termination exposure.
- Clause 6 (Intellectual Property): Vendor owns project deliverables and gives Customer only an internal-use license, limiting Customer control.
- Clause 7 (Customer Data): Vendor can use aggregated data for its own purposes, which may create data governance concerns.
- Clause 8 (Indemnity): Customer carries broad indemnity and hold harmless obligations covering many third-party claims.
- Clause 9 (Limitation of Liability): Customer has uncapped and unlimited liability while Vendor liability is tightly capped.
- Clause 10 (Termination): Vendor has immediate termination rights without notice or cure period for broad triggers.
- Clause 11 (Waiver and Remedies): Customer waives key remedies and loses leverage during disputes.
- Clause 12 (Governing Law and Dispute Resolution): Singapore law and arbitration may increase cost and complexity for an India-based Customer.
- Clause 13 (Mutual Cooperation): Mutual reasonable cooperation is a positive operational term.
```

Known current-app behavior to remember:

- `redFlags` and `greenFlags` in `/result` are currently always empty because `ContractAnalysisResponse.from(...)` does not copy them from `RiskEvaluationResult`.
- `totalClauses` is currently `0` because uploaded text is not being split into persisted `ClauseEntity` records.
- `clauseAnalyses` is currently always an empty array for the same reason.
- Duplicate upload of the exact same file should fail with a duplicate-contract error that includes the previous contract UUID.
