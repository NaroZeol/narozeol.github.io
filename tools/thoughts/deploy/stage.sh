#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
[[ -f tools/thoughts/android/build/thoughts.apk ]] || { echo 'Build the Android APK first.' >&2; exit 1; }
ssh aliyun 'mkdir -p ~/.local/share/naro-thoughts/{app,deploy,artifacts,backups}; chmod 700 ~/.local/share/naro-thoughts'
tar -C tools/thoughts/server -cf - app.py publisher.py requirements.txt static | ssh aliyun 'tar -xf - -C ~/.local/share/naro-thoughts/app'
tar -C tools/thoughts/deploy -cf - thoughts.service thoughts-backup.service thoughts-backup.timer thoughts-publish.service thoughts-publish.timer thoughts-tls.service thoughts-tls.timer tls.sh backup.sh install-system.sh configure-gist.py activate.sh | ssh aliyun 'tar -xf - -C ~/.local/share/naro-thoughts/deploy'
scp tools/thoughts/android/res/raw/thoughts_ca.crt aliyun:.local/share/naro-thoughts/deploy/thoughts_ca.crt
scp tools/thoughts/android/build/thoughts.apk aliyun:.local/share/naro-thoughts/artifacts/thoughts.apk
echo 'Staged. Install system services with:'
echo 'ssh -t aliyun sudo bash /home/naro/.local/share/naro-thoughts/deploy/install-system.sh'
