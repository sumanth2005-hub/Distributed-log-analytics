#!/usr/bin/env bash
# Hits all 5 services every INTERVAL sec (default 1.5). Ctrl+C stops.
INTERVAL="${1:-1.5}"
hit() { # name method url
  code=$(curl -s -o /dev/null -m 3 -w '%{http_code}' -X "$2" "$3")
  printf '%s=%s ' "$1" "$code"
}
while true; do
  hit auth    POST "localhost:8081/api/auth/login?userId=$((RANDOM%5+1))&password=x"
  hit user    GET  "localhost:8082/api/users/$((RANDOM%5+1))"
  hit movies  GET  "localhost:8083/api/movies/$((RANDOM%5+1))"
  hit series  GET  "localhost:8084/api/webseries/watch?userId=$((RANDOM%5+1))&seriesId=$((RANDOM%5+1))"
  hit payment GET  "localhost:8085/api/payments/status/$((RANDOM%5+1))"
  echo
  sleep "$INTERVAL"
done
