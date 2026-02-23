#!/bin/sh
set -eu

QUEUE_NAME="${QUEUE_NAME:-ticketing-order-processing}"

awslocal sqs create-queue \
  --queue-name "${QUEUE_NAME}" \
  --attributes VisibilityTimeout=45,MessageRetentionPeriod=1209600
