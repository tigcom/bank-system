#!/bin/sh

export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=myroot

sleep 10

# Enable kv-v2 (only once)
vault secrets enable -path=secret kv-v2 || true

# Put secrets
vault kv put secret/myapp \
  username_transaction_DB=root \
  password_transaction_DB=0986341885dai \
  masterAccount=97045201999010381 \
  provider.api.electricity.url=http://localhost:8089/mock-api/electricity \
  provider.api.telephone.url=http://localhost:8089/mock-api/telephone \
  core-banking.api.url=http://localhost:8083/corebanking/api/core-bank \
  mock-provider-api=http://localhost:8089/mock-api \
  mock-napas-api=http://localhost:8089/mock-napas
