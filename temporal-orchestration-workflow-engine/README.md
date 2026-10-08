# temporal-orchestration-workflow-engine

The **only** service in this POC that talks to Temporal. It hosts:

- the `AccountCreationWorkflow` (a normal, hand-written Temporal workflow),
- the Temporal **worker** that executes it (task queue `account-creation-tq`),
- a small REST API to start executions and check their status.

**Important:** despite the class/file names (kept as-is to control churn), this workflow does **not** create an account. Account creation now happens synchronously, directly against `create-account`, before this workflow is ever started. This workflow is a **risk-assessment-and-notify pipeline**: given a submitted KYC application, it checks whether it's a resubmission (logged only), scores its risk via the new `risk-score` service, and sends an approval/rejection notification. Its outcome never blocks account creation or sign-in.

`account-creation-submission` calls this service over HTTP; this service calls `create-account` (submission check only) and `risk-score` (the actual gate) over HTTP. None of those services has any Temporal dependency.

## Diagram to code

`src/main/resources/workflows/create-account-workflow.json` is a **design-reference artifact only**, kept in sync with what was actually drawn in the Workflow Builder editor. Nothing reads it at runtime. Each node maps to one piece of hand-written code:

| Diagram node (`id`) | Label | Code |
|---|---|---|
| `trigger-1` | Account Signup Submitted | `POST /api/orchestration/workflows/account-creation/executions` in `api/WorkflowExecutionController`, via `service/WorkflowExecutionService.startAccountCreation` |
| `action-check-duplicate` | Verify Account Submission | `AccountActivities.checkSubmission`, implemented in `activity/AccountActivitiesImpl` as `GET {create-account}/api/accounts/exists?email=` — **logged for audit only; the workflow does not branch on it**, matching the diagram (there is no decision node after this action) |
| `9dd29ab8-...` | Calculate Risk Score | `AccountActivities.evaluateRisk`, implemented as `POST {risk-score}/api/risk-score/evaluate` |
| `b0963d42-...` | Validate Risk Score | The `if (RiskAssessment.HIGH.equals(risk.riskLevel()))` / else in `workflow/AccountCreationWorkflowImpl.execute` |
| `d82afc2e-...` | Notify Account Creation Rejection | `AccountActivities.notifyRejected` (simulated: logged at INFO) |
| `b2188b30-...` | Notify Account Creation | `AccountActivities.notifyApproved` (simulated: logged at INFO) |

The reference JSON also fixes an editor quirk seen consistently in this tool: decision-node edges exported with a bare `sourceHandle: "source"` instead of the branch-specific `source:inner:<branchId>` — corrected here purely for the reference file's own clarity, since nothing executes it.

## REST API (port 8081)

**Start an execution**

```
POST /api/orchestration/workflows/account-creation/executions
{
  "fullName": "...", "email": "...", "dateOfBirth": "1990-01-01", "nationality": "US",
  "phone": "...", "addressLine1": "...", "addressLine2": "...", "city": "...", "state": "...",
  "postalCode": "...", "country": "US", "idType": "PASSPORT", "idNumber": "...",
  "occupation": "...", "annualIncome": "50K_TO_100K", "sourceOfFunds": "EMPLOYMENT"
}

202 Accepted
{ "workflowId": "account-creation-<uuid>", "runId": "<uuid>", "status": "RUNNING" }
```

Only `fullName`/`email` are required (`@NotBlank`); the KYC fields are lenient (this endpoint only feeds a risk assessment, it doesn't persist anything).

**Get execution status**

```
GET /api/orchestration/executions/{workflowId}

200 { "workflowId": "...", "status": "RUNNING" }
200 { "workflowId": "...", "status": "COMPLETED",
      "result": { "outcome": "APPROVED", "riskLevel": "LOW", "score": 0, "reasons": [] } }
200 { "workflowId": "...", "status": "COMPLETED",
      "result": { "outcome": "REJECTED", "riskLevel": "HIGH", "score": 100, "reasons": ["..."] } }
200 { "workflowId": "...", "status": "FAILED", "errorMessage": "..." }
404 (unknown workflowId)
```

`status` is `RUNNING`, `COMPLETED`, `FAILED`, or `UNKNOWN`. Errors come back as RFC 7807 `application/problem+json`. If Temporal is unreachable, the response is `503`.

## Configuration

| Property | Env var | Default |
|---|---|---|
| `temporal.target` | `TEMPORAL_TARGET` | `127.0.0.1:7233` |
| `services.create-account.base-url` | `CREATE_ACCOUNT_SERVICE_URL` | `http://localhost:8083` |
| `services.risk-score.base-url` | `RISK_SCORE_SERVICE_URL` | `http://localhost:8084` |
| `server.port` | (`SERVER_PORT`) | `8081` |

> **Startup requires Temporal.** Temporal SDK 1.30.1's `WorkerFactory.start()` checks the server connection on purpose, to fail fast. If Temporal isn't reachable, the app exits at boot. In docker-compose, do one of these:
> - use `depends_on: { temporal: { condition: service_healthy } }`
> - add `restart: on-failure`

## Build, test, run

```bash
mvn verify                      # unit tests use Temporal's in-memory test server; no Temporal/create-account/risk-score needed
java -jar target/app.jar

docker build -t temporal-orchestration-workflow-engine .
docker run -p 8081:8081 \
  -e TEMPORAL_TARGET=host.docker.internal:7233 \
  -e CREATE_ACCOUNT_SERVICE_URL=http://host.docker.internal:8083 \
  -e RISK_SCORE_SERVICE_URL=http://host.docker.internal:8084 \
  temporal-orchestration-workflow-engine
```

## Layout

```
com.example.orchestration
├── workflow   AccountCreationWorkflow(+Impl), AccountCreationInput, AccountCreationResult  (orchestration only, no I/O)
├── activity   AccountActivities(+Impl), RiskAssessment                                     (all HTTP / side effects)
├── config     TemporalConfig, TemporalWorkerLifecycle, TemporalConstants, HttpClientConfig
├── service    WorkflowExecutionService (WorkflowClient facade), ExecutionNotFoundException
└── api        WorkflowExecutionController, request/response records, ApiExceptionHandler
```
