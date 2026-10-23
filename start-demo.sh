#!/bin/zsh
cd "$(dirname "$0")" || exit 1

echo "==> Docker"
open -a Docker
until docker info >/dev/null 2>&1; do sleep 3; done

echo "==> Neo4j"
brew services start neo4j
until nc -z localhost 7687 >/dev/null 2>&1; do sleep 2; done

echo "==> Stack (no rebuild, no internet needed)"
docker compose up -d

echo "==> Waiting for dashboard"
until curl -sf localhost:6969/api/health >/dev/null 2>&1; do sleep 3; done

echo "==> Services"
curl -s localhost:6969/api/health
echo
echo "==> Graph"
curl -s localhost:6969/api/graph | head -c 200
echo

echo "==> Keeping Mac awake"
caffeinate -dims &
CAFE=$!
trap 'kill $CAFE 2>/dev/null; echo "stopped"' EXIT INT TERM

echo "==> PUBLIC LINK: look for https://something.trycloudflare.com below"
cloudflared tunnel --url http://localhost:6969
