"""Exercise real certificate creation and renewal without production keys."""
import os
import subprocess
from pathlib import Path


def test_private_tls_renewal_keeps_app_trust(tmp_path):
    script = Path(__file__).parents[2] / "deploy/tls.sh"
    env = {**os.environ, "THOUGHTS_DATA_DIR": str(tmp_path)}

    def renew():
        subprocess.run(["bash", str(script)], env=env, check=True, capture_output=True)

    renew()
    tls = tmp_path / "tls"
    ca = (tls / "ca.crt").read_bytes()
    leaf = (tls / "server.crt").read_bytes()
    key = (tls / "server.key").read_bytes()
    renew()
    assert (tls / "server.crt").read_bytes() == leaf
    # Force renewal, preserving the signing CA and leaf key.
    (tls / "server.crt").unlink()
    renew()
    assert (tls / "server.crt").read_bytes() != leaf
    assert (tls / "ca.crt").read_bytes() == ca
    assert (tls / "server.key").read_bytes() == key
    for name in ("ca.key", "server.key"):
        assert (tls / name).stat().st_mode & 0o077 == 0
    base = ["openssl", "verify", "-CAfile", str(tls / "ca.crt"), "-verify_hostname"]
    subprocess.run(base + ["narozeol.top", str(tls / "server.crt")], check=True, capture_output=True)
    assert subprocess.run(base + ["attacker.test", str(tls / "server.crt")], capture_output=True).returncode != 0
    assert subprocess.run(["openssl", "verify", str(tls / "server.crt")], capture_output=True).returncode != 0
    # A missing private root must fail, never regenerate incompatible app trust.
    (tls / "ca.key").unlink()
    assert subprocess.run(["bash", str(script)], env=env, capture_output=True).returncode != 0
    assert (tls / "ca.crt").read_bytes() == ca
