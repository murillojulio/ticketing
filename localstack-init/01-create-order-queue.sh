#!/bin/sh
set -eu

QUEUE_NAME="${QUEUE_NAME:-ticketing-order-processing}"
PAYMENT_QUEUE_NAME="${PAYMENT_QUEUE_NAME:-ticketing-payment-events}"

awslocal sqs create-queue \
  --queue-name "${QUEUE_NAME}" \
  --attributes VisibilityTimeout=45,MessageRetentionPeriod=1209600

awslocal sqs create-queue \
  --queue-name "${PAYMENT_QUEUE_NAME}" \
  --attributes VisibilityTimeout=45,MessageRetentionPeriod=1209600
