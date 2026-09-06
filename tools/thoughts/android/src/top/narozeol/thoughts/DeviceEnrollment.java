package top.narozeol.thoughts;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import org.json.JSONObject;

/** Password is used once to run the fixed registration helper; no general shell is exposed. */
final class DeviceEnrollment {

  static JSONObject register(ServerProfile profile, byte[] password)
    throws Exception {
    Session session = null;
    ChannelExec channel = null;
    try {
      session = SshConnection.open(profile, password);
      Arrays.fill(password, (byte) 0);
      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand(
        "python3 \"$HOME/.local/share/naro-thoughts/deploy/register-device.py\" --name Android --key-file /dev/stdin"
      );
      channel.setPty(false);
      channel.setAgentForwarding(false);
      channel.setInputStream(
        new ByteArrayInputStream(
          (new DeviceKey().publicKey() + "\n").getBytes("UTF-8")
        )
      );
      InputStream input = channel.getInputStream();
      channel.connect(10000);
      byte[] buffer = new byte[1024];
      int count,
        total = 0;
      while ((count = input.read(buffer)) != -1) {
        total += count;
        if (total > 16384) throw new Api.Failure(
          503,
          "服务器登记响应异常，请改用手动登记。"
        );
      }
      long deadline = System.nanoTime() + 5_000_000_000L;
      while (!channel.isClosed() && System.nanoTime() < deadline)
        Thread.sleep(20);
      if (channel.getExitStatus() != 0) throw new Api.Failure(
        503,
        "登录成功，但设备登记未完成。请检查服务器登记脚本或使用手动登记。"
      );
    } catch (JSchException e) {
      throw SshConnection.failure(e, true);
    } finally {
      Arrays.fill(password, (byte) 0);
      if (channel != null) channel.disconnect();
      if (session != null) session.disconnect();
    }
    // A successful password login is insufficient: prove the restricted device key now works.
    return new SshTransport(profile).request("/session", "GET", null);
  }
}
