#!/usr/bin/env bash
# Start the backend against the DB configured in backend/.env (Azure by default).
# Usage:  ./run.sh
set -e
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "backend/.env not found — copy .env.example and fill it in." >&2
  exit 1
fi

# load env (SPRING_DATASOURCE_*, OPENAI_*, SELLER_MODE, ...)
set -a; . ./.env; set +a

# free port 8080 if a previous run is still holding it
for p in $(lsof -ti :8080 2>/dev/null); do kill "$p" 2>/dev/null || true; done
sleep 1

echo "DB: ${SPRING_DATASOURCE_URL%%;*}"
exec ./mvnw -B spring-boot:run
