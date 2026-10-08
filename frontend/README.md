# Account Creation Demo — Frontend

A React + Vite (TypeScript) frontend for the KYC onboarding demo. Three screens:

- **Sign Up** (`/`, default landing page) — a 5-step KYC wizard (Personal, Contact, Identification,
  Financial Profile, Account Security). On submit it **creates the account immediately** via the
  Accounts API, then separately kicks off a **background risk-assessment workflow** via the
  Submission API and polls its status live. The risk outcome is informational only; it does not
  gate account creation.
- **Sign In** (`/signin`) — email + password; stores the returned bearer token in `AuthContext`
  (persisted to `localStorage`).
- **Accounts Dashboard** (`/dashboard`, protected) — lists accounts from the Accounts API using the
  bearer token. A `401` signs the user out and redirects to `/signin`.

## Running locally

```bash
npm install
npm run dev
```

The dev server runs on **http://localhost:5173**.

### Local environment file

The app reads its two backend base URLs from Vite env vars at build/dev time (with a runtime
override available in the built/Docker version — see below). Create a `.env` file in this
directory (not committed) with:

```
VITE_SUBMISSION_API=http://localhost:8082
VITE_ACCOUNTS_API=http://localhost:8083
```

If you skip this step, the app falls back to those same two defaults in code (see
`src/api/client.ts`), so `npm run dev` works out of the box against backends on their default
ports — the `.env` file is only needed if you want to point at different URLs locally.

### CORS

Since both API base URLs are absolute (not proxied through the Vite dev server), the backend
services must allow cross-origin requests from `http://localhost:5173` (and from wherever the
built/Docker image is served from). No CORS configuration is needed on the frontend side, but the
backends need to send the appropriate `Access-Control-Allow-Origin` headers.

## Backend contracts

**Accounts API** (`VITE_ACCOUNTS_API`, default `http://localhost:8083`):
- `POST /api/accounts` — KYC signup body `{ fullName, email, password, dateOfBirth, nationality, phone,
  addressLine1, addressLine2, city, state, postalCode, country, idType, idNumber, occupation,
  annualIncome, sourceOfFunds }` → `201` account, or `409 { code: "DUPLICATE_EMAIL", message }`
- `POST /api/accounts/signin` — `{ email, password }` → `200 { token, account }`, or `401 { code: "INVALID_CREDENTIALS", message }`
- `GET /api/accounts` — header `Authorization: Bearer <token>` → array of
  `{ id, fullName, email, createdAt, nationality, occupation, annualIncome, idNumberMasked }`, or `401`

**Submission API** (`VITE_SUBMISSION_API`, default `http://localhost:8082`) — background risk assessment:
- `POST /api/account-submissions` — same KYC body as signup **without** `password` → `202 { submissionId, status: "RUNNING" }`
- `GET /api/account-submissions/{submissionId}` → `{ submissionId, status: "RUNNING"|"COMPLETED"|"FAILED",
  result?: { outcome: "APPROVED"|"REJECTED", riskLevel: "LOW"|"HIGH", score, reasons[] }, errorMessage? }`

## Building for production

```bash
npm run build
```

Outputs static assets to `dist/`.

## Docker: runtime configuration

This is deployed as a static build served by nginx, but the two API base URLs must be resolvable
at **container start time**, not baked in at build time — so the same image works unchanged
across environments. This uses the common "runtime config" pattern:

- `index.html` loads `/env.js` via a `<script>` tag **before** the bundled app script.
- `public/env.js` is a placeholder that sets `window.__ENV__ = { VITE_SUBMISSION_API, VITE_ACCOUNTS_API }`.
- The API client (`src/api/client.ts`) reads config in this order of precedence:
  1. `window.__ENV__?.VITE_SUBMISSION_API` / `window.__ENV__?.VITE_ACCOUNTS_API` (set at container start)
  2. `import.meta.env.VITE_SUBMISSION_API` / `import.meta.env.VITE_ACCOUNTS_API` (Vite build-time env, from `.env` during local dev)
  3. Hardcoded defaults `http://localhost:8082` / `http://localhost:8083`
- `docker-entrypoint.sh` runs when the container starts, regenerates
  `/usr/share/nginx/html/env.js` from two environment variables, then execs nginx.

### Docker env vars

| Env var              | Purpose                                             | Default                   |
|-----------------------|------------------------------------------------------|----------------------------|
| `SUBMISSION_API_URL`  | Base URL for the background risk-assessment workflow   | `http://localhost:8082` |
| `ACCOUNTS_API_URL`    | Base URL for signup, sign-in and the accounts list     | `http://localhost:8083` |

### Build and run

```bash
docker build -t account-creation-frontend .
docker run -p 8080:80 \
  -e SUBMISSION_API_URL=http://submission-service:8082 \
  -e ACCOUNTS_API_URL=http://accounts-service:8083 \
  account-creation-frontend
```

Then open `http://localhost:8080`.

## File structure

```
frontend/
├── Dockerfile                  # multi-stage build: node:20-alpine build -> nginx:alpine serve
├── docker-entrypoint.sh        # writes env.js from env vars at container start, then execs nginx
├── index.html                  # loads /env.js before the bundled app script
├── public/
│   └── env.js                  # runtime-config placeholder (overwritten in Docker)
├── src/
│   ├── api/
│   │   └── client.ts           # fetch wrapper; resolves API base URLs (runtime > build-time > default)
│   ├── auth/
│   │   ├── AuthContext.tsx     # AuthProvider + useAuth(); session persisted to localStorage
│   │   └── ProtectedRoute.tsx  # redirects to /signin when not authenticated
│   ├── components/
│   │   ├── Field.tsx           # label + control + hint/error wrapper
│   │   ├── NavBar.tsx
│   │   ├── StatusBadge.tsx
│   │   └── StepProgress.tsx    # wizard step indicator
│   ├── constants/
│   │   └── kyc.ts              # countries, ID types, income/source-of-funds options + label helpers
│   ├── pages/
│   │   ├── SignUp.tsx          # 5-step KYC wizard + account-created / risk-assessment status
│   │   ├── SignIn.tsx
│   │   └── Dashboard.tsx       # accounts table (authenticated)
│   ├── App.tsx                 # routes
│   ├── main.tsx                # entrypoint
│   ├── index.css               # single stylesheet / design system
│   ├── types.ts                # shared API/domain types
│   └── vite-env.d.ts
├── vite.config.ts               # dev server on port 5173
├── tsconfig.json / tsconfig.node.json
└── package.json
```
