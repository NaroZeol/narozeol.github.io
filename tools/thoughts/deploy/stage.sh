#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
[[ -f tools/thoughts/android/build/thoughts.apk ]] || { echo 'Build the Android APK first.' >&2; exit 1; }
ssh aliyun 'mkdir -p ~/.local/share/naro-thoughts/{app,deploy,artifacts,backups,devices}; chmod 700 ~/.local/share/naro-thoughts'
tar -C tools/thoughts/server -cf - app.py ssh_gateway.py publisher.py requirements.txt static | ssh aliyun 'tar -xf - -C ~/.local/share/naro-thoughts/app'
tar -C tools/thoughts/deploy -cf - thoughts.service thoughts-backup.service thoughts-backup.timer thoughts-publish.service thoughts-publish.timer backup.sh install-system.sh configure-gist.py activate.sh ssh-gateway.sh register-device.py | ssh aliyun 'tar -xf - -C ~/.local/share/naro-thoughts/deploy'
ssh aliyun 'chmod 700 ~/.local/share/naro-thoughts/deploy/ssh-gateway.sh'
scp tools/thoughts/android/build/thoughts.apk aliyun:.local/share/naro-thoughts/artifacts/thoughts.apk
echo 'Staged. Register the phone public key using register-device.py; no new network ports are needed.'
