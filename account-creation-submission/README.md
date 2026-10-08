# account-creation-submission

The single base URL the frontend calls to trigger a background **risk assessment** for an
already-submitted KYC application and poll its status. **This service does not create an
account and never handles a password** - signup happens synchronously, directly against
`create-account`, before this is ever called (see the top-level architecture note in the
repo). It forwards the KYC payload to `temporal-orchestration-workflow-engine` (which runs
the actual Temporal workflow), then exposes a matching status-polling endpoint that passes
through to that engine. This service has **zero** Temporal dependency - it is a thin REST
façade only.

## Stack

Java 17, Spring Boot 3.3.4, Maven, Spring Web (`RestClient` for the outbound call), Bean
Validation.

## Running locally

Requires the `temporal-orchestration-workflow-engine` service to be reachable (default
`http://localhost:8081`; override with the env var below).

```bash
export ORCHESTRATION_ENGINE_URL=http://localhost:8081

mvn spring-boot:run
```

The service starts on port **8082**.

### Running the tests

```bash
mvn test
```

### Building the Docker image

```bash
docker build -t account-creation-submission .
docker run -p 8082:8082 \
  -e ORCHESTRATION_ENGINE_URL=http://host.docker.internal:8081 \
  account-creation-submission
```

## Endpoints

| Method | Path                                  | Description |
|--------|----------------------------------------|--------------|
| POST   | `/api/account-submissions`             | Trigger a risk-assessment run for a KYC application. `202` with `{ submissionId, status }`, `400` on validation failure, `502` if the orchestration engine can't be reached |
| GET    | `/api/account-submissions/{submissionId}` | Poll status. `200` passthrough of the engine's status, `404` if unknown, `502` if the orchestration engine can't be reached |

### Request/response shapes

`POST /api/account-submissions` request body (only `fullName`/`email` are required - the
rest is lenient, since this only feeds a risk score, it doesn't persist anything):

```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "dateOfBirth": "1990-01-01",
  "nationality": "US",
  "phone": "555-0100",
  "addressLine1": "1 Main St", "addressLine2": null,
  "city": "Springfield", "state": "IL", "postalCode": "62701", "country": "US",
  "idType": "PASSPORT", "idNumber": "A1234567",
  "occupation": "Engineer", "annualIncome": "50K_TO_100K", "sourceOfFunds": "EMPLOYMENT"
}
```

`202 Accepted` response:

```json
{ "submissionId": "account-creation-<uuid>", "status": "RUNNING" }
```

`GET /api/account-submissions/{submissionId}` response (passthrough of the engine's status,
with `workflowId` renamed to `submissionId`; `result` and `errorMessage` are omitted when
not applicable):

```json
{ "submissionId": "...", "status": "RUNNING" }
{ "submissionId": "...", "status": "COMPLETED",
  "result": { "outcome": "APPROVED", "riskLevel": "LOW", "score": 0, "reasons": [] } }
{ "submissionId": "...", "status": "COMPLETED",
  "result": { "outcome": "REJECTED", "riskLevel": "HIGH", "score": 100, "reasons": ["..."] } }
{ "submissionId": "...", "status": "FAILED", "errorMessage": "..." }
```

`404` when the engine doesn't recognize the submission id.

Error body shape (`400` / `404` / `502` / `500`):

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request body is invalid",
  "fieldErrors": { "email": "email must be a valid email address" }
}
```

Error codes used: `VALIDATION_ERROR` (400), `SUBMISSION_NOT_FOUND` (404),
`ORCHESTRATION_ENGINE_UNAVAILABLE` (502), `INTERNAL_ERROR` (500).

## Configuration

| Property                                  | Env var                   | Default                 |
|--------------------------------------------|----------------------------|--------------------------|
| `services.orchestration-engine.base-url`    | `ORCHESTRATION_ENGINE_URL`  | `http://localhost:8081`  |
| `server.port`                               | (`SERVER_PORT`)             | `8082`                   |

## Layout

```
com.example.submission
├── controller  AccountSubmissionController                                     (HTTP only, delegates to SubmissionService)
├── service     SubmissionService(+Impl),
│               OrchestrationEngineClient/RestClientOrchestrationEngineClient    (outbound HTTP call to the engine)
├── dto         request/response records for both this API and the engine's API
├── exception   SubmissionNotFoundException, OrchestrationEngineException,
│               ErrorResponse, GlobalExceptionHandler                           (maps exceptions to HTTP responses)
└── config      RestClientConfig                                                (RestClient bean, engine base URL)
```
