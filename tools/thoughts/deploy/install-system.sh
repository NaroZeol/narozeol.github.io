#!/usr/bin/env bash
set -euo pipefail
if [[ "$EUID" -ne 0 ]]; then echo 'Run with sudo to configure system services.' >&2; exit 1; fi
deploy_dir=/home/naro/.local/share/naro-thoughts/deploy
data_dir=/home/naro/.local/share/naro-thoughts
[[ -f "$data_dir/app/app.py" && -f "$data_dir/app/ssh_gateway.py" ]] || { echo 'Stage the application first.' >&2; exit 1; }
for unit in thoughts.service thoughts-backup.service thoughts-backup.timer thoughts-publish.service thoughts-publish.timer; do
  install -m 644 "$deploy_dir/$unit" /etc/systemd/system/
done
systemctl daemon-reload
systemctl enable --now thoughts.service thoughts-backup.timer thoughts-publish.timer
systemctl restart thoughts.service
curl --fail --silent --retry 5 --retry-connrefused --max-time 10 http://127.0.0.1:8765/api/health
# Only disable the Caddy instance previously installed by this project.
if [[ -f /etc/caddy/Caddyfile ]] && head -1 /etc/caddy/Caddyfile | grep -q 'Managed by naro-thoughts'; then
  systemctl disable --now caddy.service
fi
systemctl start thoughts-backup.service
echo
echo 'Thoughts uses SSH on existing port 22. API is loopback-only; no TLS certificate or web port is required.'
