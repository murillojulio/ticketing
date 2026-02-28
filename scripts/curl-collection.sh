#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "1) Creating event..."
EVENT_RESPONSE=$(curl -sS -X POST "${BASE_URL}/api/events" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rock Festival",
    "date": "2026-12-01T20:00:00Z",
    "venue": "National Stadium",
    "totalCapacity": 100
  }')
echo "${EVENT_RESPONSE}"

EVENT_ID=$(echo "${EVENT_RESPONSE}" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
if [ -z "${EVENT_ID}" ]; then
  echo "Unable to extract event id from response"
  exit 1
fi
echo "Event ID: ${EVENT_ID}"

echo
echo "2) Listing events..."
curl -sS "${BASE_URL}/api/events"
echo

echo
echo "3) Checking availability..."
curl -sS "${BASE_URL}/api/events/${EVENT_ID}/availability"
echo

echo
echo "4) Creating order..."
ORDER_RESPONSE=$(curl -sS -X POST "${BASE_URL}/api/orders" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventId\": \"${EVENT_ID}\",
    \"customerId\": \"customer-1\",
    \"quantity\": 2
  }")
echo "${ORDER_RESPONSE}"

ORDER_ID=$(echo "${ORDER_RESPONSE}" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
if [ -z "${ORDER_ID}" ]; then
  echo "Unable to extract order id from response"
  exit 1
fi
echo "Order ID: ${ORDER_ID}"

echo
echo "5) Querying order state..."
curl -sS "${BASE_URL}/api/orders/${ORDER_ID}"
echo

echo
echo "6) Sending payment confirmation webhook..."
curl -sS -X POST "${BASE_URL}/api/payments/webhook" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": \"${ORDER_ID}\",
    \"paymentId\": \"payment-123\",
    \"status\": \"CONFIRMED\",
    \"occurredAt\": \"2026-12-01T20:05:00Z\"
  }"
echo

echo
echo "7) Querying order state after payment event..."
curl -sS "${BASE_URL}/api/orders/${ORDER_ID}"
echo
