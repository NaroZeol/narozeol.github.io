#!/usr/bin/env bash
set -euo pipefail
data_dir=/home/naro/.local/share/naro-thoughts
if [[ ! -s "$data_dir/gist-token" ]]; then
    python3 "$data_dir/deploy/configure-gist.py"
fi
sudo bash "$data_dir/deploy/install-system.sh"
cd "$data_dir/app"
PYTHONPATH="$data_dir/python" python3 publisher.py
echo 'Initial Gist publication completed.'
