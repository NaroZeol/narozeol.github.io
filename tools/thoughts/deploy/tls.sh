#!/usr/bin/env bash
# Private app CA: keys stay on the server; only ca.crt is bundled in Android.
set -euo pipefail
umask 077
data_dir=${THOUGHTS_DATA_DIR:-/home/naro/.local/share/naro-thoughts}
tls_dir="$data_dir/tls"
mkdir -p "$tls_dir"
exec 9>"$tls_dir/renew.lock"
flock 9
if [[ ! -f "$tls_dir/ca.crt" ]]; then
  if [[ -e "$tls_dir/ca.key" || -e "$tls_dir/server.crt" ]]; then
    echo 'Incomplete CA files; restore from backup instead of replacing app trust.' >&2
    exit 1
  fi
  openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 -nodes \
    -keyout "$tls_dir/ca.key" -out "$tls_dir/ca.crt" -days 3650 -sha256 \
    -subj '/CN=Naro Thoughts App CA' \
    -addext 'basicConstraints=critical,CA:TRUE,pathlen:0' \
    -addext 'keyUsage=critical,keyCertSign,cRLSign'
fi
[[ -s "$tls_dir/ca.key" ]] || { echo 'CA signing key is missing.' >&2; exit 1; }
# Never silently rotate the root: installed apps must keep trusting it.
openssl x509 -in "$tls_dir/ca.crt" -checkend 34560000 -noout || {
  echo 'CA expires within 400 days; ship an App update with the replacement CA first.' >&2
  exit 1
}
if [[ -s "$tls_dir/server.crt" && -s "$tls_dir/server.key" ]] && \
   openssl verify -CAfile "$tls_dir/ca.crt" -verify_hostname narozeol.top "$tls_dir/server.crt" >/dev/null 2>&1 && \
   openssl x509 -in "$tls_dir/server.crt" -checkend 2592000 -noout >/dev/null; then
  exit 0
fi
work_dir=$(mktemp -d "$tls_dir/.renew-XXXXXX")
trap 'rm -rf "$work_dir"' EXIT
if [[ ! -s "$tls_dir/server.key" ]]; then
  openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:prime256v1 -out "$tls_dir/server.key"
fi
openssl req -new -key "$tls_dir/server.key" -out "$work_dir/server.csr" -subj '/CN=narozeol.top'
cat > "$work_dir/extensions" <<'EOF'
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature
extendedKeyUsage=serverAuth
subjectAltName=DNS:narozeol.top
EOF
openssl x509 -req -in "$work_dir/server.csr" -CA "$tls_dir/ca.crt" -CAkey "$tls_dir/ca.key" \
  -set_serial "0x$(openssl rand -hex 16)" -days 397 -sha256 \
  -extfile "$work_dir/extensions" -out "$work_dir/server.crt"
openssl verify -CAfile "$tls_dir/ca.crt" -verify_hostname narozeol.top "$work_dir/server.crt"
mv "$work_dir/server.crt" "$tls_dir/server.crt"
echo 'Server certificate renewed; App trust is unchanged.'
