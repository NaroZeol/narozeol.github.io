package top.narozeol.thoughts;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.ByteArrayInputStream;

/** The same pinned server identity applies to enrollment and routine device access. */
final class SshConnection {

  static Session open(ServerProfile profile, byte[] password) throws Exception {
    JSch ssh = new JSch();
    ssh.setKnownHosts(
      new ByteArrayInputStream(
        (profile.id + " " + profile.knownHost + "\n").getBytes("UTF-8")
      )
    );
    if (password == null) ssh.addIdentity(new DeviceKey(), null);
    Session session = ssh.getSession(profile.user, profile.host, profile.port);
    session.setHostKeyAlias(profile.id);
    session.setConfig("StrictHostKeyChecking", "yes");
    session.setConfig(
      "PreferredAuthentications",
      password == null ? "publickey" : "password"
    );
    session.setConfig("server_host_key", "ecdsa-sha2-nistp256");
    session.setConfig("kex", "ecdh-sha2-nistp256");
    session.setConfig("cipher.c2s", "aes128-ctr");
    session.setConfig("cipher.s2c", "aes128-ctr");
    session.setConfig("mac.c2s", "hmac-sha2-256");
    session.setConfig("mac.s2c", "hmac-sha2-256");
    session.setConfig("PubkeyAcceptedAlgorithms", "rsa-sha2-512,rsa-sha2-256");
    session.setConfig("compression.c2s", "none");
    session.setConfig("compression.s2c", "none");
    session.setTimeout(35000);
    if (password != null) session.setPassword(password);
    try {
      session.connect(12000);
      return session;
    } catch (Exception e) {
      session.disconnect();
      throw e;
    }
  }

  static Api.Failure failure(JSchException error, boolean password) {
    String message = String.valueOf(error.getMessage()).toLowerCase(
      java.util.Locale.ROOT
    );
    if (
      message.contains("hostkey") || message.contains("host key")
    ) return new Api.Failure(
      495,
      "服务器身份校验失败，已停止连接。请核对服务器公钥。"
    );
    if (
      message.contains("auth fail") || message.contains("auth cancel")
    ) return new Api.Failure(
      401,
      password
        ? "密码不正确，或服务器未开启密码登录。"
        : "设备尚未登记或已被撤销，请重新连接服务器。"
    );
    return new Api.Failure(
      503,
      "SSH 连接失败，请检查网络和服务器状态。本机记录已保留。"
    );
  }
}
