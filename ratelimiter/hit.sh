#!/usr/bin/env bash
#
# Fire N requests at the rate-limited endpoint and log status + rate-limit headers.
#
# Usage:
#   ./hit.sh [count] [url] [user]
#
# Examples:
#   ./hit.sh                      # 110 requests to http://localhost:8080/users as user "alice"
#   ./hit.sh 50                   # 50 requests
#   ./hit.sh 200 http://localhost:8080/users bob
#
set -euo pipefail

COUNT="${1:-110}"
URL="${2:-http://localhost:8080/users}"
# Default to a fresh user each run (PID + epoch) so every run starts with an empty window.
USER="${3:-user-$$-$(date +%s)}"

echo "Firing $COUNT requests at $URL  (X-User-Id: $USER)"
echo "------------------------------------------------------------------"
printf "%-5s %-6s %-8s %-10s %s\n" "#" "STATUS" "REMAIN" "LIMIT" "BODY"
echo "------------------------------------------------------------------"

allowed=0
blocked=0

for i in $(seq 1 "$COUNT"); do
    # -D - dumps response headers to stdout; -w appends status + body marker.
    resp="$(curl -s -D - -H "X-User-Id: $USER" -o /tmp/hit_body \
                 -w 'HTTP_STATUS:%{http_code}' "$URL")"

    status="$(printf '%s' "$resp" | sed -n 's/.*HTTP_STATUS:\([0-9]*\).*/\1/p')"
    remain="$(printf '%s' "$resp" | grep -i '^X-RateLimit-Remaining:' | tr -d '\r' | awk '{print $2}')"
    limit="$(printf '%s'  "$resp" | grep -i '^X-RateLimit-Limit:'     | tr -d '\r' | awk '{print $2}')"
    body="$(cat /tmp/hit_body)"

    if [ "$status" = "429" ]; then
        blocked=$((blocked + 1))
    else
        allowed=$((allowed + 1))
    fi

    printf "%-5s %-6s %-8s %-10s %s\n" "$i" "${status:-?}" "${remain:-?}" "${limit:-?}" "$body"
done

echo "------------------------------------------------------------------"
echo "Allowed: $allowed   Blocked (429): $blocked"
rm -f /tmp/hit_body
