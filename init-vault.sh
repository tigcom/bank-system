export VAULT_ADDR="http://host.docker.internal:8200"
export VAULT_TOKEN='myroot'

vault kv put secret/customer-service \
  db-username=root \
  db-password=123456 \
  idp-realm=myrealm \
  idp-client-id=customer-service \
  idp-client-secret=vF8VYOn3m3g63csOanjpBqG9AxQNUEQX \
  redis-password=123