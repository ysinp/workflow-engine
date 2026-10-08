#!/bin/sh
set -eu

# Writes the runtime config file that index.html loads before the bundled
# app script. This lets one built image be pointed at different backend
# URLs per environment (dev/test/prod) purely through container env vars,
# with no rebuild required.
ACCOUNTS_API_URL="${ACCOUNTS_API_URL:-http://localhost:8083}"
ORCHESTRATION_API_URL="${ORCHESTRATION_API_URL:-http://localhost:8081}"
PRODUCT_API_URL="${PRODUCT_API_URL:-http://localhost:8087}"

cat > /usr/share/nginx/html/env.js <<EOF
window.__ENV__ = {
  VITE_ACCOUNTS_API: "${ACCOUNTS_API_URL}",
  VITE_ORCHESTRATION_API: "${ORCHESTRATION_API_URL}",
  VITE_PRODUCT_API: "${PRODUCT_API_URL}"
};
EOF

exec nginx -g "daemon off;"
