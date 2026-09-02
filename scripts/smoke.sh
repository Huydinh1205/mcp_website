#!/usr/bin/env bash
# End-to-end API smoke test. Backend must be running on :8080 with a seeded DB.
#   ./scripts/smoke.sh
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
j() { python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get(sys.argv[1],""))' "$1"; }

echo "1. login (mai@example.com)"
LOGIN=$(curl -s "$BASE/api/auth/login" -H 'content-type: application/json' \
  -d '{"email":"mai@example.com","password":"password"}')
TOKEN=$(echo "$LOGIN" | j token)
[ -n "$TOKEN" ] || { echo "  ✗ no token: $LOGIN"; exit 1; }
echo "  ✓ token ${TOKEN:0:16}…"
AUTH=(-H "Authorization: Bearer $TOKEN" -H 'content-type: application/json')

echo "2. GET /api/products (public)"
curl -s "$BASE/api/products?q=keyboard&sort=sold" | head -c 160; echo " …"

echo "3. search_products via /api/mcp"
PID=$(curl -s "$BASE/api/mcp" "${AUTH[@]}" \
  -d '{"tool":"search_products","args":{"query":"65% Mechanical Keyboard"}}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)[0]["product_id"])')
echo "  ✓ product_id=$PID"

echo "4. submit_offer (price 45, free shipping) — server seller should respond"
STATE=$(curl -s "$BASE/api/mcp" "${AUTH[@]}" \
  -d "{\"tool\":\"submit_offer\",\"args\":{\"product_id\":\"$PID\",\"price\":45,\"free_shipping\":true}}")
echo "  $STATE" | head -c 240; echo " …"
NEG=$(echo "$STATE" | j negotiation_id)
echo "  ✓ negotiation_id=$NEG  status=$(echo "$STATE" | j status)"

echo "5. /api/agent/turn — checks OPENAI_API_KEY + proxy"
TURN=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/agent/turn" "${AUTH[@]}" \
  -d '{"messages":[{"role":"user","content":"say hi"}],"tools":[]}')
if [ "$TURN" = "200" ]; then echo "  ✓ agent/turn 200 (OpenAI key OK)"
elif [ "$TURN" = "503" ]; then echo "  ✗ agent/turn 503 — OPENAI_API_KEY missing or out of credit"
else echo "  ? agent/turn HTTP $TURN"; fi

echo "6. GET /api/negotiations"
curl -s "$BASE/api/negotiations?since=" "${AUTH[@]}" | head -c 200; echo " …"
echo "done."
