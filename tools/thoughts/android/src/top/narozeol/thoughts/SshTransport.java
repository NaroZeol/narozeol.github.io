package top.narozeol.thoughts;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.json.JSONObject;

final class SshTransport implements Transport {

  private final ServerProfile profile;

  SshTransport(ServerProfile profile) {
    this.profile = profile;
  }

  public JSONObject request(String path, String method, JSONObject body)
    throws Exception {
    Session session = null;
    ChannelExec channel = null;
    try {
      JSch ssh = new JSch();
      ssh.setKnownHosts(
        new ByteArrayInputStream(
          (profile.id + " " + profile.knownHost + "\n").getBytes("UTF-8")
        )
      );
      ssh.addIdentity(new DeviceKey(), null);
      session = ssh.getSession(profile.user, profile.host, profile.port);
      session.setHostKeyAlias(profile.id);
      session.setConfig("StrictHostKeyChecking", "yes");
      session.setConfig("PreferredAuthentications", "publickey");
      session.setConfig("server_host_key", "ecdsa-sha2-nistp256");
      session.setConfig("kex", "ecdh-sha2-nistp256");
      session.setConfig("cipher.c2s", "aes128-ctr");
      session.setConfig("cipher.s2c", "aes128-ctr");
      session.setConfig("mac.c2s", "hmac-sha2-256");
      session.setConfig("mac.s2c", "hmac-sha2-256");
      session.setConfig(
        "PubkeyAcceptedAlgorithms",
        "rsa-sha2-512,rsa-sha2-256"
      );
      session.setConfig("compression.c2s", "none");
      session.setConfig("compression.s2c", "none");
      session.setTimeout(35000);
      session.connect(12000);
      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand("thoughts-rpc-v1");
      channel.setPty(false);
      channel.setAgentForwarding(false);
      byte[] payload = (
        new JSONObject()
          .put("path", path)
          .put("method", method)
          .put("body", body == null ? JSONObject.NULL : body)
          .toString() + "\n"
      ).getBytes("UTF-8");
      if (payload.length > 140 * 1024) throw new Api.Failure(
        400,
        "内容过长，请分成多条想法"
      );
      channel.setInputStream(new ByteArrayInputStream(payload));
      InputStream input = channel.getInputStream();
      channel.connect(10000);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int count;
      while ((count = input.read(buffer)) != -1) {
        output.write(buffer, 0, count);
        if (output.size() > 16 * 1024 * 1024) throw new Api.Failure(
          413,
          "响应过大，请在服务器导出备份"
        );
      }
      JSONObject response;
      try {
        response = new JSONObject(output.toString("UTF-8"));
      } catch (Exception e) {
        throw new Api.Failure(
          503,
          "服务器连接已建立，但想法服务未就绪，请检查设备登记"
        );
      }
      int status = response.getInt("status");
      JSONObject result = response.getJSONObject("body");
      if (status >= 400) throw new Api.Failure(
        status,
        result.optString("error", "请求失败")
      );
      return result;
    } catch (JSchException e) {
      String message = String.valueOf(e.getMessage()).toLowerCase(
        java.util.Locale.ROOT
      );
      if (
        message.contains("hostkey") || message.contains("host key")
      ) throw new Api.Failure(
        495,
        "服务器身份校验失败，已停止连接。请核对服务器公钥。"
      );
      if (
        message.contains("auth fail") || message.contains("auth cancel")
      ) throw new Api.Failure(
        401,
        "设备尚未登记或已被撤销。请在服务器登记这台手机的公钥。"
      );
      throw new Api.Failure(
        503,
        "SSH 连接失败，请检查网络和服务器状态。本地记录已保留。"
      );
    } finally {
      if (channel != null) channel.disconnect();
      if (session != null) session.disconnect();
    }
  }
}
