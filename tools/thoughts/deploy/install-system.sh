#!/usr/bin/env bash
set -euo pipefail
if [[ "$EUID" -ne 0 ]]; then
  echo 'Run this script with sudo to configure system services.' >&2
  exit 1
fi
deploy_dir=/home/naro/.local/share/naro-thoughts/deploy
data_dir=/home/naro/.local/share/naro-thoughts
[[ -f "$data_dir/app/app.py" ]] || { echo 'Stage the application first.' >&2; exit 1; }
runuser -u naro -- bash "$deploy_dir/tls.sh"
# Verify the staged App trust and TLS key pair before replacing any service.
cmp "$data_dir/tls/ca.crt" "$deploy_dir/thoughts_ca.crt"
python3 - "$data_dir/tls" <<'TLSCHECK'
import ssl, sys
context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
context.load_cert_chain(sys.argv[1] + '/server.crt', sys.argv[1] + '/server.key')
TLSCHECK
# Stop only temporary verification services belonging to this application.
for preview in thoughts-preview.service thoughts-direct-preview.service; do
  runuser -u naro -- env XDG_RUNTIME_DIR=/run/user/1000 systemctl --user disable --now "$preview" || true
done
for unit in thoughts.service thoughts-backup.service thoughts-backup.timer thoughts-publish.service thoughts-publish.timer thoughts-tls.service thoughts-tls.timer; do
  install -m 644 "$deploy_dir/$unit" /etc/systemd/system/
done
systemctl daemon-reload
systemctl enable --now thoughts.service thoughts-backup.timer thoughts-publish.timer thoughts-tls.timer
systemctl restart thoughts.service
curl --fail --silent --retry 5 --retry-connrefused --max-time 10 \
  --noproxy '*' --cacert "$data_dir/tls/ca.crt" --resolve narozeol.top:8443:127.0.0.1 \
  https://narozeol.top:8443/api/health
# Leave unrelated Caddy installations alone; disable only our previous config.
if [[ -f /etc/caddy/Caddyfile ]] && head -1 /etc/caddy/Caddyfile | grep -q 'Managed by naro-thoughts'; then
  systemctl disable --now caddy.service
fi
systemctl start thoughts-backup.service
systemctl start thoughts-tls.service
echo
echo 'Thoughts is served directly over TLS on TCP 8443. No Caddy, DNS challenge or ports 80/443 required.'
echo 'Install the updated APK with the bundled private CA. Browsers need this CA explicitly trusted.'
