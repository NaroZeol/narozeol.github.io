"""Run interactively on aliyun. The token is never echoed or sent to a client."""
import getpass
import json
import os
from pathlib import Path
from urllib.request import Request, urlopen

token = getpass.getpass("GitHub token (only Gist write permission required): ").strip()
if not token:
    raise SystemExit("No token supplied; nothing changed.")
request = Request("https://api.github.com/gists/a783c548bd3c7b22578a4bd3748bc9d5", headers={"Authorization": "Bearer " + token, "User-Agent": "NaroZeol-Thoughts"})
try:
    with urlopen(request, timeout=20) as response:
        metadata = json.load(response)
        scopes = response.headers.get("X-OAuth-Scopes", "")
except Exception:
    raise SystemExit("Could not verify access to the configured Gist; token was not saved.")
if metadata.get("owner", {}).get("login", "").lower() != "narozeol":
    raise SystemExit("Unexpected Gist owner; token was not saved.")
if scopes and set(part.strip() for part in scopes.split(",")) != {"gist"}:
    raise SystemExit("Use a dedicated token with only the gist scope; token was not saved.")
path = Path.home() / ".local/share/naro-thoughts/gist-token"
fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
with os.fdopen(fd, "w") as output:
    output.write(token)
os.chmod(path, 0o600)
print("Gist token stored privately. The next publish attempt will use it.")
