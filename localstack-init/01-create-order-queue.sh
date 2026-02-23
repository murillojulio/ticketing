#!/bin/sh
set -e

awslocal sqs create-queue \
  --queue-name ticketing-order-processing \
  --attributes VisibilityTimeout=45,MessageRetentionPeriod=1209600
