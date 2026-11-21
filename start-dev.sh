#!/bin/bash
# ============================================================
# start-dev.sh — Launch the full dev environment
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Personal AI Assistant — Dev Startup"
echo "========================================"

# ── 1. Load environment ──────────────────────────────────────
BACKEND_ENV="$SCRIPT_DIR/backend/.env"
if [ ! -f "$BACKEND_ENV" ]; then
    echo "❌ Missing backend/.env — copy .env.example and fill it in:"
    echo "   cp backend/.env.example backend/.env"
    exit 1
fi
set -a; source "$BACKEND_ENV"; set +a

# ── 2. Start PostgreSQL ───────────────────────────────────────
echo "📦 Starting PostgreSQL..."
cd "$SCRIPT_DIR/backend"
docker compose up -d postgres
echo "   Waiting for PostgreSQL to be ready..."
until docker compose exec -T postgres pg_isready -U assistant -d personal_assistant &>/dev/null; do
    sleep 1
done
echo "   ✅ PostgreSQL ready"

# ── 3. Check Ollama ──────────────────────────────────────────
echo "🤖 Checking Ollama..."
if ! curl -sf "${OLLAMA_BASE_URL:-http://localhost:11434}/api/tags" > /dev/null; then
    echo "   ⚠️  Ollama is not running."
    echo "   Start it with: ollama serve"
    echo "   Then pull the model: ollama pull ${OLLAMA_MODEL:-qwen2.5:7b}"
else
    echo "   ✅ Ollama reachable"
fi

# ── 4. Start Spring Boot backend ─────────────────────────────
echo "☕ Starting Spring Boot backend..."
cd "$SCRIPT_DIR/backend"
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev &
BACKEND_PID=$!
echo "   Backend PID: $BACKEND_PID"

# ── 5. Start Next.js frontend ────────────────────────────────
echo "⚡ Starting Next.js frontend..."
cd "$SCRIPT_DIR/frontend"
npm run dev &
FRONTEND_PID=$!
echo "   Frontend PID: $FRONTEND_PID"

echo ""
echo "✅ All services started!"
echo "   Backend:  http://localhost:8080"
echo "   Frontend: http://localhost:3000"
echo ""
echo "Press Ctrl+C to stop all services."

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; echo 'Stopped.'" INT TERM
wait
