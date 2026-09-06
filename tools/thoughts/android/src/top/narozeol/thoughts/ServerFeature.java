package top.narozeol.thoughts;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import org.json.JSONObject;

/** Server overview is a separate capability; it never receives a shell or private key. */
final class ServerFeature extends Ui implements Feature {

  private JSONObject snapshot;
  private boolean loading;
  private String problem = "";
  private long checkedAt;

  ServerFeature(Feature.Host host) {
    super(host);
    try {
      SharedPreferences p = activity.getSharedPreferences("server_status", 0);
      String value = p.getString("snapshot", "");
      if (!value.isEmpty()) snapshot = new JSONObject(value);
      checkedAt = p.getLong("checked_at", 0);
    } catch (Exception ignored) {}
  }

  public String id() {
    return "server";
  }

  public String label() {
    return "服务";
  }

  public void render(LinearLayout surface) {
    heading(
      surface,
      "SERVER",
      "我的服务器",
      "连接设备，查看服务与数据的状态。"
    );
    LinearLayout connection = card(
      surface,
      !account.isVerified() ? "连接这台设备" : "SSH 加密连接",
      "narozeol.top  ·  端口 22"
    );
    space(connection, 14);
    connection.addView(
      button(
        loading
          ? "正在验证…"
          : !account.isVerified()
            ? "验证连接"
            : "刷新服务状态",
        () -> verify(),
        true
      )
    );
    space(connection, 10);
    connection.addView(button("设备登记与公钥", () -> enrollment(), false));
    if (!problem.isEmpty()) {
      space(connection, 14);
      connection.addView(text(problem, 13, ALERT));
    }
    if (!account.isVerified()) {
      LinearLayout intro = card(
        surface,
        "只需登记一次",
        "1. 复制这台手机的公钥\n2. 在服务器运行设备登记命令\n3. 回到这里验证连接"
      );
      space(intro, 12);
      intro.addView(
        text(
          "私钥保存在手机内。换机或卸载后需要重新登记，未同步内容请先导出。",
          12,
          MUTED
        )
      );
      return;
    }
    if (snapshot == null) {
      card(
        surface,
        "服务状态尚未读取",
        "连接成功后会显示发布队列、最近备份和存储情况。"
      );
      return;
    }
    try {
      LinearLayout notes = card(surface, "想法服务", "");
      row(notes, "服务版本", snapshot.optString("version", "—"));
      JSONObject counts = snapshot.getJSONObject("records");
      row(notes, "公开想法", counts.optInt("active") + " 条");
      row(notes, "回收站", counts.optInt("trash") + " 条");
      JSONObject publication = snapshot.getJSONObject("publication");
      boolean pending =
        publication.optInt("generation") >
        publication.optInt("published_generation");
      LinearLayout publishing = card(
        surface,
        "博客发布",
        pending
          ? "记录已在服务器，正在等待 Gist 更新。"
          : "服务器记录已发布到 Gist。"
      );
      row(publishing, "发布队列", pending ? "待发布" : "已同步");
      if (pending) {
        space(publishing, 12);
        publishing.addView(button("重试发布", () -> host.sync(), false));
      }
      JSONObject backup = snapshot.getJSONObject("backup"),
        storage = snapshot.getJSONObject("storage");
      LinearLayout data = card(surface, "备份与存储", "");
      row(data, "最近备份", date(backup.optString("latest_at", "")));
      row(data, "保留备份", backup.optInt("count") + " 份");
      row(data, "数据库", size(storage.optLong("database_bytes")));
      row(data, "磁盘可用", size(storage.optLong("free_bytes")));
      surface.addView(
        text(
          "状态读取于 " +
            java.time.Instant.ofEpochMilli(checkedAt)
              .atZone(java.time.ZoneId.systemDefault())
              .format(
                java.time.format.DateTimeFormatter.ofPattern("MM.dd HH:mm")
              ),
          12,
          MUTED
        )
      );
    } catch (Exception e) {
      card(surface, "状态暂时无法显示", "请刷新后重试，已有内容不受影响。");
    }
  }

  private String date(String value) {
    try {
      return java.time.OffsetDateTime.parse(value)
        .atZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("MM.dd HH:mm"));
    } catch (Exception e) {
      return "暂无备份";
    }
  }

  void verify() {
    if (loading) return;
    loading = true;
    problem = "";
    host.redraw();
    status("正在验证设备与服务器身份…");
    IO.execute(() -> {
      try {
        JSONObject session = Api.request("/session", "GET", null);
        account.verified(session);
        if (account.can("system.read")) {
          snapshot = Api.request("/system", "GET", null);
          checkedAt = System.currentTimeMillis();
          activity
            .getSharedPreferences("server_status", 0)
            .edit()
            .putString("snapshot", snapshot.toString())
            .putLong("checked_at", checkedAt)
            .apply();
        }
        runOnUiThread(() -> {
          status("设备验证通过，可以同步想法");
          host.sync();
        });
      } catch (Exception e) {
        problem = errorMessage(e);
        runOnUiThread(() -> status(problem));
      } finally {
        loading = false;
        runOnUiThread(() -> {
          if (host.activeFeature().equals(id())) host.redraw();
        });
      }
    });
  }

  void enrollment() {
    status("正在准备设备公钥…");
    IO.execute(() -> {
      try {
        DeviceKey key = new DeviceKey();
        String publicKey = key.publicKey(),
          fingerprint = key.fingerprint();
        runOnUiThread(() ->
          new AlertDialog.Builder(activity)
            .setTitle("登记这台手机")
            .setMessage(
              "先复制公钥，再在服务器执行：\n\npython3 ~/.local/share/naro-thoughts/deploy/register-device.py\n\n按提示粘贴公钥，完成后点击验证连接。\n\n设备指纹\n" +
                fingerprint
            )
            .setPositiveButton("复制公钥", (d, w) -> copy(publicKey))
            .setNeutralButton("复制登记命令", (d, w) ->
              copy(
                "python3 ~/.local/share/naro-thoughts/deploy/register-device.py"
              )
            )
            .setNegativeButton("关闭", null)
            .show()
        );
      } catch (Exception e) {
        runOnUiThread(() -> status("无法创建设备密钥，请检查手机安全存储。"));
      }
    });
  }
}
