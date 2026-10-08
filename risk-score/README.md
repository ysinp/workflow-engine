# risk-score

A stateless KYC risk-scoring rule engine for the account-creation workflow POC. Given an
applicant's KYC data, it returns a binary risk verdict (`LOW`/`HIGH`) used to gate the
approval/rejection branch of a Temporal workflow (owned by another service). This service
holds no state, no database, and has **zero** Temporal dependency - it is a pure rule
evaluator called over plain HTTP.

The actual rules are evaluated by [Drools](https://drools.org) (Apache Drools 10), not
hand-written Java: `src/main/resources/com/example/riskscore/rules/risk-rules.drl` is the
single source of truth for scoring logic, loaded into a `KieContainer` at startup (see
`config.DroolsConfig`). Changing a threshold or adding a rule means editing that `.drl` file,
not recompiling a Java strategy class.

## Stack

Java 17, Spring Boot 3.3.4, Maven, Spring Web, Bean Validation, Drools (Apache Drools) 10.2.0.

## Running locally

```bash
mvn spring-boot:run
```

The service starts on port **8084**. No external dependencies (database, Temporal, or
otherwise) are required.

### Running the tests

```bash
mvn test
```

### Building the Docker image

```bash
mvn -DskipTests package
docker build -t risk-score .
docker run -p 8084:8084 risk-score
```

## Endpoints

| Method | Path                        | Description                                              |
|--------|-----------------------------|------------------------------------------------------------|
| POST   | `/api/risk-score/evaluate`  | Evaluate a KYC applicant. `200` with the risk verdict, `400` on validation failure |

### Request/response shapes

`POST /api/risk-score/evaluate` request body:

```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "dateOfBirth": "2000-01-01",
  "nationality": "IR",
  "idType": "PASSPORT",
  "idNumber": "string",
  "occupation": "string",
  "annualIncome": "UNDER_25K",
  "sourceOfFunds": "EMPLOYMENT",
  "address": {
    "line1": "string", "line2": "string", "city": "string",
    "state": "string", "postalCode": "string", "country": "US"
  }
}
```

Only `email` and `idNumber` are required (`@NotBlank`) - everything else may be blank or
null, since this is a stateless scoring call, not a form validator. `nationality`/`country`
are ISO 3166-1 alpha-2 codes. `idType`, `annualIncome`, and `sourceOfFunds` are treated as
plain strings server-side; an unrecognized value simply doesn't match any rule rather than
being rejected.

`200 OK` response:

```json
{ "riskLevel": "LOW", "score": 0, "reasons": [] }
```

`riskLevel` is `LOW` or `HIGH` only - binary, matching the workflow's two-branch decision.
`score` is an integer (clamped to a minimum of 0). `reasons` lists a short human-readable
string for each rule that fired (empty when nothing fired).

Error body shape (`400`):

```json
{
  "code": "VALIDATION_ERROR",
  "message": "email must not be blank"
}
```

## Risk rules

Three rules, deliberately - this is a demo rule engine, not a real scoring model. They live
in `risk-rules.drl` as real Drools rules, not Java classes. For each incoming request,
`RiskScoringServiceImpl` builds a `RiskFact` (the two fields any rule reads: `nationality`,
`idNumber`), opens a fresh `KieSession`, inserts the fact, and calls `fireAllRules()`. A rule
that matches inserts a `RuleOutcome(points, reason)` fact; `RiskScoringServiceImpl` collects
every `RuleOutcome` left in working memory afterward, sums the points, clamps the total to a
minimum of 0, and flags `HIGH` at a score of 50 or more. A rule that doesn't match simply
inserts nothing.

| Rule (DRL rule name) | Fires when | Points | Reason text |
|------|-----------|--------|-------------|
| `High-risk jurisdiction` | `nationality` is one of `IR`, `KP`, `SY`, `CU`, `SD` | +100 | `Nationality {code} is on the high-risk jurisdiction list` |
| `Demo marker - force high risk` (demo-only) | `idNumber` contains `RISK-HIGH` (case-insensitive) | +100 | `ID number contains the RISK-HIGH test marker` |
| `Demo marker - force low risk` (demo-only) | `idNumber` contains `RISK-LOW` (case-insensitive) | -100 | `ID number contains the RISK-LOW test marker` |

### Demo markers

To reliably trigger a branch during the leadership demo, without needing a genuinely
high-risk profile, set one of these on the request:

| Want to trigger | Set | Example |
|---|---|---|
| **HIGH** via jurisdiction | `nationality` to `IR`, `KP`, `SY`, `CU`, or `SD` | `"nationality": "IR"` |
| **HIGH** via ID marker | `idNumber` containing `RISK-HIGH` anywhere | `"idNumber": "P-RISK-HIGH-0001"` |
| **LOW**, even overriding a high-risk nationality | `idNumber` containing `RISK-LOW` anywhere | `"idNumber": "P-RISK-LOW-0001"` |
| **LOW** (no marker fires) | anything else | `"idNumber": "P1234567"` |

The `RISK-LOW` marker is a guaranteed override: a high-risk nationality alone scores 100
(`HIGH`), but combined with a `RISK-LOW` marker the rule points sum to `100 + (-100) = 0`,
which is below the 50-point `HIGH` threshold - so the applicant still comes back `LOW`,
with both reasons listed. This is intentional (see `RiskScoringServiceImplTest`) and lets a
demo operator force a LOW result on demand even against an otherwise-risky profile.

## Configuration

| Property      | Env var       | Default |
|---------------|---------------|---------|
| `server.port` | (`SERVER_PORT`) | `8084` |

Actuator is exposed identically to the sibling services: `health`, `info`, `prometheus`,
`metrics` under `/actuator`, tagged with `application: risk-score`.

## Layout

```
com.example.riskscore
├── controller  RiskScoreController                                          (HTTP only, delegates to RiskScoringService)
├── config      DroolsConfig                                                 (compiles risk-rules.drl into a KieContainer bean)
├── service     RiskScoringService(+Impl)                                    (fires the Drools rule base, sums/clamps score)
│   └── rule    RiskFact (input fact), RuleOutcome (fact a rule inserts when it fires)
├── dto         RiskEvaluationRequest(+Address), RiskEvaluationResponse
└── exception   ErrorResponse, GlobalExceptionHandler                        (maps validation failures to 400)

src/main/resources
├── META-INF/kmodule.xml                           (declares the kbase/ksession Drools loads at startup)
└── com/example/riskscore/rules/risk-rules.drl     (the actual rules - see "Risk rules" above)
```
