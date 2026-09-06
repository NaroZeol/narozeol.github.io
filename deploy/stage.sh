#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f _site/index.html && -f android/build/thoughts.apk ]] || { echo 'Build the blog and Android APK first.' >&2; exit 1; }
ssh aliyun 'mkdir -p ~/.local/share/naro-thoughts/{app,deploy,blog/downloads,backups}; chmod 700 ~/.local/share/naro-thoughts'
tar -C thoughts-app -cf - app.py publisher.py requirements.txt static | ssh aliyun 'tar -xf - -C ~/.local/share/naro-thoughts/app'
tar -C deploy -cf - Caddyfile thoughts.service thoughts-backup.service thoughts-backup.timer thoughts-publish.service thoughts-publish.timer backup.sh install-system.sh configure-gist.py activate.sh | ssh aliyun 'tar -xf - -C ~/.local/share/naro-thoughts/deploy'
tar -C _site -cf - . | ssh aliyun 'tar -xf - -C ~/.local/share/naro-thoughts/blog'
scp android/build/thoughts.apk aliyun:.local/share/naro-thoughts/blog/downloads/thoughts.apk
echo 'Staged. Install system services with:'
echo 'ssh -t aliyun sudo bash /home/naro/.local/share/naro-thoughts/deploy/install-system.sh'
