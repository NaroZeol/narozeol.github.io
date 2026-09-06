#!/usr/bin/env bash
set -euo pipefail
if [[ "$EUID" -ne 0 ]]; then
  echo 'Run this script with sudo to configure HTTPS and system services.' >&2
  exit 1
fi
deploy_dir=/home/naro/.local/share/naro-thoughts/deploy
data_dir=/home/naro/.local/share/naro-thoughts
if [[ ! -f "$data_dir/app/app.py" || ! -f "$data_dir/blog/index.html" ]]; then
  echo 'Stage the validated application and blog before running this script.' >&2
  exit 1
fi
if [[ -f /etc/caddy/Caddyfile ]] && ! head -1 /etc/caddy/Caddyfile | grep -q 'Managed by naro-thoughts'; then
  echo 'An existing Caddy configuration was found. Review and merge the staged Caddyfile manually.' >&2
  exit 1
fi
if [[ -d /var/www/naro-blog && ! -f /var/www/naro-blog/.naro-thoughts-managed ]]; then
  echo '/var/www/naro-blog already exists and is not managed by this app.' >&2
  exit 1
fi
apt-get update
apt-get install -y caddy
install -d -m 755 /var/www/naro-blog
cp -a "$data_dir/blog/." /var/www/naro-blog/
touch /var/www/naro-blog/.naro-thoughts-managed
chmod -R a+rX /var/www/naro-blog
install -m 644 "$deploy_dir/Caddyfile" /etc/caddy/Caddyfile
caddy validate --config /etc/caddy/Caddyfile
# Stop the temporary per-user verification service before starting the durable service.
runuser -u naro -- env XDG_RUNTIME_DIR=/run/user/1000 systemctl --user disable --now thoughts-preview.service || true
install -m 644 "$deploy_dir/thoughts.service" /etc/systemd/system/
install -m 644 "$deploy_dir/thoughts-backup.service" /etc/systemd/system/
install -m 644 "$deploy_dir/thoughts-backup.timer" /etc/systemd/system/
install -m 644 "$deploy_dir/thoughts-publish.service" /etc/systemd/system/
install -m 644 "$deploy_dir/thoughts-publish.timer" /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now thoughts.service thoughts-backup.timer thoughts-publish.timer caddy.service
systemctl restart thoughts.service
systemctl reload caddy
systemctl start thoughts-backup.service
curl --fail --silent --retry 5 --retry-connrefused --max-time 10 http://127.0.0.1:8765/api/health
echo
echo 'Services configured. Ensure the Aliyun security group allows inbound TCP 80 and 443.'
echo 'The management UI will be available at https://narozeol.top/app/ once HTTPS is issued.'
