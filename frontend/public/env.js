// Runtime configuration placeholder.
//
// In local development this file is served as-is (untouched) and the app
// falls back to the Vite build-time env vars (see .env / import.meta.env).
//
// In the Docker image, docker-entrypoint.sh OVERWRITES this exact file at
// container start from the SUBMISSION_API_URL / ACCOUNTS_API_URL environment
// variables, so the same built image can point at different backends in
// different environments without a rebuild.
window.__ENV__ = {
  VITE_SUBMISSION_API: "http://localhost:8082",
  VITE_ACCOUNTS_API: "http://localhost:8083",
};
