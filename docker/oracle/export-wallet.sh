
#!/bin/sh

set -eu

wallet_dir="${1:-docker/oracle/wallet}"
container_id="$(docker-compose ps -q oracle)"

if [ -z "$container_id" ]; then
  echo "The Oracle container is not running. Start it first with: docker-compose --profile oracle up -d oracle" >&2
  exit 1
fi

if ! docker exec "$container_id" test -s /opt/oracle/oradata/clientWallet/FREE/cwallet.sso; then
  echo "The Oracle wallet is not ready yet. Wait for the database healthcheck and try again." >&2
  exit 1
fi

mkdir -p "$wallet_dir"
docker cp "$container_id:/opt/oracle/oradata/clientWallet/FREE/." "$wallet_dir/"

echo "Oracle wallet exported to $wallet_dir"
echo "Set ORACLE_WALLET_DIR to $(cd "$wallet_dir" && pwd) in your IDE run configuration."
