#!/usr/bin/env bash
case "$1" in
  on)  curl -s -X POST localhost:8085/api/payments/chaos/fail ;;
  off) curl -s -X POST localhost:8085/api/payments/chaos/reset ;;
  *)   echo "usage: chaos.sh on|off"; exit 1 ;;
esac
echo
