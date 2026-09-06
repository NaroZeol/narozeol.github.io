package top.narozeol.thoughts;

import android.app.AlertDialog;
import android.widget.LinearLayout;

final class SettingsFeature extends Ui implements Feature {

  SettingsFeature(Feature.Host host) {
    super(host);
  }

  public String id() {
    return "settings";
  }

  public String label() {
    return "设置";
  }

  public void render(LinearLayout surface) {
    heading(
      surface,
      "PREFERENCES",
      "留给自己的空间",
      "管理设备、备份数据，了解同步方式。"
    );
    LinearLayout device = card(
      surface,
      "设备与连接",
      !account.isVerified()
        ? "这台设备尚未完成验证。"
        : "使用手机专属密钥认证，私钥不离开设备。"
    );
    space(device, 14);
    device.addView(
      button("管理服务器连接", () -> host.navigate("server"), false)
    );
    LinearLayout data = card(
      surface,
      "数据与导出",
      "离线草稿和待同步记录都保存在手机，卸载前请先导出。"
    );
    space(data, 14);
    data.addView(button("导出本机记录与草稿", () -> host.export(false), true));
    space(data, 10);
    data.addView(button("导出服务器完整历史", () -> host.export(true), false));
    LinearLayout sync = card(surface, "同步状态", account.lastSync());
    space(sync, 14);
    sync.addView(button("查看待同步记录", () -> host.navigate("notes"), false));
    LinearLayout about = card(
      surface,
      "想法 1.2",
      "记录随时发生的念头，让整理成为日常。"
    );
    space(about, 10);
    about.addView(
      text(
        "全部想法公开。手机通过 SSH 提交服务器，再发布到 Gist；博客读者只读取 Gist。\n\n打开 App、保存或手动同步时联网，无广告、无统计、无后台常驻。",
        13,
        MUTED
      )
    );
    space(about, 12);
    about.addView(
      button(
        "开源组件与许可",
        () ->
          new AlertDialog.Builder(activity)
            .setTitle("开源组件")
            .setMessage(
              "SSH：mwiede/JSch 2.28.7\nBSD 3-Clause / ISC 许可\n\n完整许可随 APK 提供于 assets/THIRD_PARTY_NOTICES.txt。"
            )
            .setPositiveButton("关闭", null)
            .show(),
        false
      )
    );
    space(surface, 8);
    surface.addView(
      button("断开并清除本机记录", () -> host.disconnect(), false)
    );
  }
}
