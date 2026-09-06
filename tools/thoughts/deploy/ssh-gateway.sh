#!/usr/bin/env bash
set -euo pipefail
exec /usr/bin/python3 "$HOME/.local/share/naro-thoughts/app/ssh_gateway.py" "$@"
