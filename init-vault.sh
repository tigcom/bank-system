
export VAULT_TOKEN="myroot"
export VAULT_ADDR="http://host.docker.internal:8200"

vault kv put secret/customer-service \
  db_username="root" \
  db_password="123456" \
  idp_realm="myrealm" \
  idp_client_id="customer-service" \
  idp_client_secret="vF8VYOn3m3g63csOanjpBqG9AxQNUEQX" \
  redis_password="123"
