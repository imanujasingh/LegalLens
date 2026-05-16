# Revision simulation

## Scenario

Use these two files:

- `test-documents/high-risk-service-agreement.txt`
- `test-documents/high-risk-service-agreement-v2.txt`

The revised file intentionally changes the contract:

- Clause 1 modified: subcontractors now require written notice.
- Clause 4 removed: service levels clause is gone.
- Clause 9 modified: Customer liability is now capped for some obligations.
- Other clauses remain unchanged.

## Step 1: Upload original contract

```bash
curl -X POST "http://localhost:8080/api/contracts/upload" \
  -F "file=@test-documents/high-risk-service-agreement.txt" \
  -F "partyName=Atlas Retail Private Limited" \
  -F "partyRole=CUSTOMER" \
  -F "jurisdiction=India"
```

Expected upload response:

```json
{
  "contractUuid": "<v1-uuid>",
  "parentContractUuid": null,
  "version": 1,
  "isLatestVersion": true,
  "analysisStatus": "UPLOADING",
  "message": "Contract uploaded successfully. Analysis in progress."
}
```

Poll until completed:

```bash
curl "http://localhost:8080/api/contracts/<v1-uuid>/status"
```

Then get result:

```bash
curl "http://localhost:8080/api/contracts/<v1-uuid>/result"
```

Expected v1 result highlights:

```json
{
  "parentContractUuid": null,
  "version": 1,
  "isLatestVersion": true,
  "changeSummary": "Initial contract analysis",
  "comparison": null
}
```

`clauseAnalyses` should contain extracted clauses from the original document.

## Step 2: Upload revised contract as version 2

Use the original `contractUuid` as `parentContractUuid`.

```bash
curl -X POST "http://localhost:8080/api/contracts/upload" \
  -F "file=@test-documents/high-risk-service-agreement-v2.txt" \
  -F "partyName=Atlas Retail Private Limited" \
  -F "partyRole=CUSTOMER" \
  -F "jurisdiction=India" \
  -F "parentContractUuid=<v1-uuid>"
```

Expected upload response:

```json
{
  "contractUuid": "<v2-uuid>",
  "parentContractUuid": "<v1-uuid>",
  "version": 2,
  "isLatestVersion": true,
  "analysisStatus": "UPLOADING",
  "message": "Contract uploaded successfully. Analysis in progress."
}
```

Poll until completed:

```bash
curl "http://localhost:8080/api/contracts/<v2-uuid>/status"
```

Then get result:

```bash
curl "http://localhost:8080/api/contracts/<v2-uuid>/result"
```

## Expected v2 comparison shape

The exact AI text may vary, but the structural fields should look like this:

```json
{
  "contractUuid": "<v2-uuid>",
  "parentContractUuid": "<v1-uuid>",
  "version": 2,
  "isLatestVersion": true,
  "changeSummary": "2 added or modified clauses require fresh LLM analysis; unchanged clauses reuse prior analysis.",
  "comparison": {
    "riskImprovement": null,
    "clausesChanged": 3,
    "suggestionsAdopted": 0,
    "improvementSummary": "2 added or modified clauses require fresh LLM analysis; unchanged clauses reuse prior analysis.",
    "changes": [
      {
        "clauseNumber": 1,
        "changeType": "MODIFIED"
      },
      {
        "clauseNumber": 4,
        "changeType": "REMOVED"
      },
      {
        "clauseNumber": 9,
        "changeType": "MODIFIED"
      }
    ]
  }
}
```

## Expected LLM optimization

For v2, the worker should send only changed clauses to the LLM:

- Clause 1
- Clause 9

Clause 4 is removed, so it is recorded as `REMOVED` without an LLM call.

Unchanged clauses reuse previous clause analysis:

- Clause 2
- Clause 3
- Clause 5
- Clause 6
- Clause 7
- Clause 8
- Clause 10
- Clause 11
- Clause 12
- Clause 13

## Important caveat

The current comparison uses exact normalized clause hashes and clause-number matching. It does not yet use semantic similarity. If clause numbering changes heavily, the comparison can become less accurate.
