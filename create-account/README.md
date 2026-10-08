# create-account

Owns the `accounts` Postgres table, which is the single source of truth for customer accounts.
It is a KYC signup and sign-in service:

- **Signup** (`POST /api/accounts`) takes a **plaintext** password straight from the frontend
  and hashes it with **BCrypt**. The plaintext is never stored, and it never passes through any
  workflow-history-persisting system.
- **Sign-in** (`POST /api/accounts/signin`) checks credentials and issues a signed **JWT**.
- **Reads** (`GET /api/accounts`, `GET /api/accounts/{id}`) require `Authorization: Bearer <jwt>`.

This service has zero knowledge of Temporal. Other services in the account-creation workflow
POC call it over plain HTTP. For example, the orchestration engine calls the public
`GET /api/accounts/exists` endpoint.

## Stack

Java 17, Spring Boot 3.3.4, Maven, Spring Web, Spring Data JPA, Spring Security (stateless,
BCrypt), JJWT 0.13.0, PostgreSQL JDBC driver, Bean Validation.

## Configuration

| Env var                      | Default                                              | Purpose |
|------------------------------|------------------------------------------------------|---------|
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://localhost:5434/accounts`          | Postgres JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `accounts_user`                                      | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `accounts_pass`                                      | DB password |
| `JWT_SECRET`                 | `dev-only-insecure-secret-change-me-please-...` (81 bytes) | HMAC key used to sign/verify JWTs. **Must be at least 32 bytes** or startup fails (`WeakKeyException`). **Always override outside local dev.** |
| `JWT_EXPIRATION_HOURS`       | `4`                                                  | Token lifetime in hours |

The browser origins allowed by CORS come from the `app.cors.allowed-origins` property. It
defaults to `http://localhost:5173,http://localhost:5174`.

JJWT's `signWith(key)` picks the HMAC algorithm from the key length: 32-47 bytes gives
HS256, 48-63 gives HS384, and 64 or more gives HS512. The 81-byte default dev secret
therefore yields **HS512** tokens. This is transparent to clients, because the token is
opaque to them.

## Running locally

Requires a local PostgreSQL instance reachable on port 5434 with a database named `accounts`
and a user matching the credentials below (adjust the env vars if yours differ).

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/accounts
export SPRING_DATASOURCE_USERNAME=accounts_user
export SPRING_DATASOURCE_PASSWORD=accounts_pass
export JWT_SECRET='replace-with-a-long-random-secret-of-at-least-32-bytes'
export JWT_EXPIRATION_HOURS=4

mvn spring-boot:run
```

The service starts on port **8083**. With `spring.jpa.hibernate.ddl-auto: update`, the
`accounts` table is created or updated automatically on startup, including the new nullable KYC
columns. That is fine for this POC. A real deployment would use Flyway or Liquibase migrations
instead.

### Running the tests

```bash
mvn test
```

### Building the Docker image

```bash
mvn -DskipTests package
docker build -t create-account .
docker run -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5434/accounts \
  -e SPRING_DATASOURCE_USERNAME=accounts_user \
  -e SPRING_DATASOURCE_PASSWORD=accounts_pass \
  -e JWT_SECRET='replace-with-a-long-random-secret-of-at-least-32-bytes' \
  create-account
```

## Endpoints

| Method | Path                                 | Auth        | Description |
|--------|--------------------------------------|-------------|-------------|
| POST   | `/api/accounts`                      | public      | KYC signup. `201` + account DTO, `409` duplicate email, `400` validation failure |
| POST   | `/api/accounts/signin`               | public      | Sign in. `200 { token, account }`, `401` invalid credentials, `400` validation failure |
| GET    | `/api/accounts/exists?email={email}` | public      | `200 { "exists": true \| false }` (called service-to-service) |
| GET    | `/api/accounts`                      | Bearer JWT  | `200` array of all accounts (no pagination, POC only) |
| GET    | `/api/accounts/{id}`                 | Bearer JWT  | `200` account DTO, or `404` if not found |

`/actuator/**` stays unauthenticated so Prometheus can keep scraping `/actuator/prometheus`.
The set of exposed actuator endpoints is restricted in `application.yml`.

### `POST /api/accounts` (signup)

Request body. Only `fullName`, `email` (valid format) and `password` (8 or more characters)
are required. All KYC fields are optional and may be `null` or omitted.

```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "password": "plaintext-password-min-8-chars",
  "dateOfBirth": "1990-05-17",
  "nationality": "US",
  "phone": "+1-555-0100",
  "addressLine1": "1 Main St",
  "addressLine2": "Apt 2",
  "city": "Springfield",
  "state": "IL",
  "postalCode": "62701",
  "country": "US",
  "idType": "PASSPORT",
  "idNumber": "A1234567",
  "occupation": "Software Engineer",
  "annualIncome": "50K_TO_100K",
  "sourceOfFunds": "EMPLOYMENT"
}
```

Field notes:

- `dateOfBirth`: ISO-8601 `YYYY-MM-DD`. Any other format returns `400 VALIDATION_ERROR`.
- `nationality` and `country`: ISO 3166-1 alpha-2, for example `"US"`.
- `idType`: `PASSPORT` \| `NATIONAL_ID` \| `DRIVERS_LICENSE`.
- `annualIncome`: `UNDER_25K` \| `25K_TO_50K` \| `50K_TO_100K` \| `100K_TO_250K` \| `OVER_250K`.
- `sourceOfFunds`: `EMPLOYMENT` \| `BUSINESS` \| `INVESTMENTS` \| `INHERITANCE` \| `OTHER`.

The enum-like values are stored as plain strings and are not validated server-side (demo).

### Account DTO

POST `/api/accounts`, the GET endpoints, and the `account` field of the sign-in response all
return this shape:

```json
{
  "id": "b3f1c2e4-....",
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "createdAt": "2026-09-26T12:00:00Z",
  "nationality": "US",
  "occupation": "Software Engineer",
  "annualIncome": "50K_TO_100K",
  "idNumberMasked": "****4567"
}
```

The DTO is deliberately minimal. It never includes `passwordHash`, the full `idNumber`,
the date of birth, or any address or contact fields. `idNumberMasked` is `****` followed by the
last 4 characters of `idNumber`. It is just `****` if `idNumber` is missing or shorter than 4
characters.

### `POST /api/accounts/signin`

Request:

```json
{ "email": "jane@example.com", "password": "plaintext-password" }
```

`200` response:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9....",
  "account": { "id": "...", "fullName": "...", "email": "...", "createdAt": "...",
               "nationality": "US", "occupation": "Software Engineer",
               "annualIncome": "50K_TO_100K", "idNumberMasked": "****4567" }
}
```

The JWT subject (`sub`) is the account email. It also carries `iat` and `exp`.

An unknown email and a wrong password return the **same** generic `401`, so the response does
not reveal whether an email is registered:

```json
{ "code": "INVALID_CREDENTIALS", "message": "Invalid email or password" }
```

Both paths also run one BCrypt comparison, so response timing does not reveal it either.

### Calling protected endpoints

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/accounts
```

A missing, invalid, tampered, or expired token gets a JSON `401`. An invalid token is treated
exactly like no token:

```json
{ "code": "UNAUTHORIZED", "message": "Authentication required: provide a valid 'Authorization: Bearer <token>' header" }
```

### Error body shape

```json
{
  "code": "DUPLICATE_EMAIL",
  "message": "An account with email 'jane@example.com' already exists"
}
```

Error codes used:

| Code                  | Status | Meaning |
|-----------------------|--------|---------|
| `DUPLICATE_EMAIL`     | 409    | Signup with an email that is already registered |
| `ACCOUNT_NOT_FOUND`   | 404    | No account with the requested id |
| `VALIDATION_ERROR`    | 400    | Missing or invalid field, or malformed JSON |
| `INVALID_CREDENTIALS` | 401    | Sign-in failed |
| `UNAUTHORIZED`        | 401    | Protected endpoint called without a valid JWT |

## CORS

`config/CorsConfig` exposes one `CorsConfigurationSource` bean for `/api/**`, built from
`app.cors.allowed-origins`. Spring Security's filter chain uses this bean through
`.cors(Customizer.withDefaults())`. Preflight `OPTIONS` requests never carry the
`Authorization` header, so they are answered inside the security chain before any
authentication check. There is no separate MVC-level copy of the CORS config.
