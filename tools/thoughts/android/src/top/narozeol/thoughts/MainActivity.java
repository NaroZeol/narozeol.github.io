package top.narozeol.thoughts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

public final class MainActivity extends Activity {

  private static final int INK = Color.rgb(32, 37, 44),
    MUTED = Color.rgb(99, 109, 122),
    BLUE = Color.rgb(54, 89, 189),
    PAPER = Color.rgb(248, 249, 251),
    LINE = Color.rgb(220, 225, 231);
  private static final ExecutorService IO = Executors.newSingleThreadExecutor();
  private static final AtomicBoolean SYNCING = new AtomicBoolean(false);
  private Store store;
  private Account account;
  private SharedPreferences drafts;
  private LinearLayout root, surface, feed;
  private TextView statusView;
  private EditText content, tags, search;
  private String editingId = null,
    currentTab = "capture",
    filter = "all",
    message = "想法先保存在手机，联网后打开同步";
  private int editingVersion = 0;
  private boolean restoring = false,
    registered = false;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable persistDraft = () -> saveDraft();
  private final ConnectivityManager.NetworkCallback networkCallback =
    new ConnectivityManager.NetworkCallback() {
      public void onAvailable(Network network) {
        runOnUiThread(() -> sync());
      }
    };

  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    store = new Store(this);
    account = new Account(this);
    drafts = getSharedPreferences("draft", MODE_PRIVATE);
    build();
    receiveShare(getIntent());
  }

  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    receiveShare(intent);
  }

  private void receiveShare(Intent intent) {
    if (Intent.ACTION_SEND.equals(intent.getAction())) {
      String value = intent.getStringExtra(Intent.EXTRA_TEXT);
      if (value != null) {
        if (!currentTab.equals("capture")) tab("capture");
        String previous = content.getText().toString();
        content.setText(previous.isEmpty() ? value : previous + "\n\n" + value);
        saveDraft();
      }
      intent.setAction(null);
    }
  }

  protected void onResume() {
    super.onResume();
    try {
      (
        (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)
      ).registerDefaultNetworkCallback(networkCallback);
      registered = true;
    } catch (Exception ignored) {}
    sync();
  }

  protected void onPause() {
    saveDraft();
    if (registered) {
      try {
        (
          (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)
        ).unregisterNetworkCallback(networkCallback);
      } catch (Exception ignored) {}
      registered = false;
    }
    super.onPause();
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private LinearLayout column() {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    return layout;
  }

  private TextView text(String value, int size, int color) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(color);
    view.setLineSpacing(dp(3), 1);
    return view;
  }

  private GradientDrawable background(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    d.setStroke(dp(1), LINE);
    return d;
  }

  private void space(LinearLayout parent, int size) {
    View gap = new View(this);
    parent.addView(gap, new LinearLayout.LayoutParams(1, dp(size)));
  }

  private Button button(String label, Runnable action, boolean primary) {
    Button button = new Button(this);
    button.setText(label);
    button.setTextSize(14);
    button.setAllCaps(false);
    button.setMinHeight(dp(48));
    button.setTextColor(primary ? Color.WHITE : INK);
    button.setBackground(background(primary ? INK : PAPER, 8));
    button.setPadding(dp(12), dp(5), dp(12), dp(5));
    button.setOnClickListener(v -> action.run());
    return button;
  }

  private EditText input(String hint, boolean multiline) {
    EditText view = new EditText(this);
    view.setTextSize(16);
    view.setTextColor(INK);
    view.setHintTextColor(MUTED);
    view.setHint(hint);
    view.setPadding(dp(14), dp(14), dp(14), dp(14));
    view.setBackground(background(Color.WHITE, 8));
    view.setInputType(
      InputType.TYPE_CLASS_TEXT |
        (multiline
          ? InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
          : 0)
    );
    return view;
  }

  private TextWatcher watcher(Runnable fn) {
    return new TextWatcher() {
      public void beforeTextChanged(
        CharSequence s,
        int start,
        int count,
        int after
      ) {}

      public void onTextChanged(
        CharSequence s,
        int start,
        int before,
        int count
      ) {
        fn.run();
      }

      public void afterTextChanged(Editable value) {}
    };
  }

  private void build() {
    root = column();
    root.setBackgroundColor(PAPER);
    root.setPadding(dp(20), dp(12), dp(20), dp(12));
    root.setOnApplyWindowInsetsListener((v, insets) -> {
      v.setPadding(
        dp(20),
        insets.getSystemWindowInsetTop() + dp(12),
        dp(20),
        insets.getSystemWindowInsetBottom() + dp(12)
      );
      return insets;
    });
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = text("想法", 30, INK);
    title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    header.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1));
    header.addView(button("同步 ↻", () -> sync(), false));
    root.addView(header);
    statusView = text(message, 12, MUTED);
    statusView.setPadding(0, dp(10), 0, dp(16));
    statusView.setAccessibilityLiveRegion(
      View.ACCESSIBILITY_LIVE_REGION_POLITE
    );
    root.addView(statusView);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    surface = column();
    scroll.addView(surface);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout nav = new LinearLayout(this);
    nav.setPadding(0, dp(12), 0, 0);
    String[][] tabs = {
      { "capture", "＋ 记录" },
      { "notes", "想法" },
      { "settings", "设置" },
    };
    for (String[] item : tabs) {
      Button b = button(item[1], () -> tab(item[0]), false);
      b.setTextColor(currentTab.equals(item[0]) ? BLUE : MUTED);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
      p.setMargins(dp(3), 0, dp(3), 0);
      nav.addView(b, p);
    }
    root.addView(nav);
    setContentView(root);
    if (currentTab.equals("capture")) capture();
    else if (currentTab.equals("notes")) notes();
    else settings();
  }

  private void tab(String name) {
    saveDraft();
    currentTab = name;
    content = null;
    tags = null;
    build();
  }

  private void capture() {
    restoring = true;
    editingId = drafts.getString("id", null);
    editingVersion = drafts.getInt("version", 0);
    TextView heading = text(
      editingId == null ? "记下这一刻。" : "编辑想法",
      28,
      INK
    );
    heading.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    surface.addView(heading);
    space(surface, 20);
    content = input("有什么想法？", true);
    content.setGravity(Gravity.TOP);
    content.setMinLines(7);
    content.setMaxLines(14);
    content.setFilters(new android.text.InputFilter[] {
      new android.text.InputFilter.LengthFilter(20000),
    });
    content.setText(drafts.getString("content", ""));
    content.setContentDescription("想法内容");
    surface.addView(content, new LinearLayout.LayoutParams(-1, -2));
    space(surface, 16);
    tags = input("标签，用逗号分隔", false);
    tags.setContentDescription("标签，用逗号分隔");
    tags.setText(drafts.getString("tags", ""));
    surface.addView(tags);
    space(surface, 10);
    surface.addView(button("保存并发布  ↗", () -> saveNote(), true));
    if (editingId != null) {
      space(surface, 10);
      surface.addView(
        button(
          "取消编辑",
          () ->
            new AlertDialog.Builder(this)
              .setMessage("丢弃当前编辑草稿？")
              .setNegativeButton("保留", null)
              .setPositiveButton("丢弃", (d, w) -> {
                drafts.edit().clear().commit();
                content = null;
                currentTab = "capture";
                build();
              })
              .show(),
          false
        )
      );
    }
    space(surface, 16);
    surface.addView(
      text("全部想法公开 · 离线时先保存，联网同步后发布", 12, MUTED)
    );
    content.addTextChangedListener(watcher(() -> scheduleDraft()));
    tags.addTextChangedListener(watcher(() -> scheduleDraft()));
    restoring = false;
  }

  private void scheduleDraft() {
    if (!restoring) {
      handler.removeCallbacks(persistDraft);
      handler.postDelayed(persistDraft, 250);
    }
  }

  private void saveDraft() {
    handler.removeCallbacks(persistDraft);
    if (content != null && tags != null && !restoring) {
      boolean ok = drafts
        .edit()
        .putString("content", content.getText().toString())
        .putString("tags", tags.getText().toString())
        .putString("id", editingId)
        .putInt("version", editingVersion)
        .commit();
      if (!ok) status("草稿写入失败，请复制内容备份");
    }
  }

  private void saveNote() {
    try {
      String body = content.getText().toString().trim();
      if (body.isEmpty()) throw new Exception("先写下一点想法吧");
      JSONArray list = new JSONArray();
      java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
      for (String tag : tags.getText().toString().split("[,，]")) {
        String t = tag.trim();
        if (t.length() > 30) throw new Exception("每个标签最多 30 字");
        if (!t.isEmpty()) unique.add(t);
      }
      if (unique.size() > 12) throw new Exception("最多添加 12 个标签");
      for (String tag : unique) list.put(tag);
      store.save(editingId, body, list, editingVersion);
      handler.removeCallbacks(persistDraft);
      drafts.edit().clear().commit();
      content = null;
      tags = null;
      editingId = null;
      editingVersion = 0;
      build();
      status("已保存到手机 · 等待同步");
      sync();
    } catch (Exception e) {
      saveDraft();
      status(e.getMessage());
    }
  }

  private void notes() {
    TextView title = text("我的想法", 24, INK);
    surface.addView(title);
    space(surface, 18);
    search = input("搜索内容或标签", false);
    search.setContentDescription("搜索想法");
    surface.addView(search);
    space(surface, 12);
    LinearLayout filters = new LinearLayout(this);
    String[][] choices = { { "all", "全部" }, { "trash", "回收站" } };
    for (String[] item : choices) {
      Button b = button(
        item[1],
        () -> {
          filter = item[0];
          notesRebuild();
        },
        false
      );
      b.setTextColor(filter.equals(item[0]) ? BLUE : MUTED);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1);
      p.setMargins(dp(2), 0, dp(2), 0);
      filters.addView(b, p);
    }
    surface.addView(filters);
    space(surface, 10);
    feed = column();
    surface.addView(feed);
    search.addTextChangedListener(watcher(() -> renderFeed()));
    renderFeed();
  }

  private void notesRebuild() {
    String query = search.getText().toString();
    surface.removeAllViews();
    notes();
    search.setText(query);
  }

  private void renderFeed() {
    if (feed == null || !currentTab.equals("notes")) return;
    feed.removeAllViews();
    try {
      String query = search
        .getText()
        .toString()
        .toLowerCase(java.util.Locale.ROOT);
      int count = 0;
      for (Store.Entry entry : store.entries()) {
        JSONObject note = entry.note;
        boolean deleted = !note.isNull("deleted_at");
        if (filter.equals("trash") ? !deleted : deleted) continue;
        if (
          !(note.optString("content") + note.optString("tags"))
            .toLowerCase(java.util.Locale.ROOT)
            .contains(query)
        ) continue;
        count++;
        LinearLayout card = column();
        card.setPadding(dp(16), dp(16), dp(16), dp(12));
        card.setBackground(background(Color.WHITE, 8));
        String state =
          entry.error != null
            ? "需要处理冲突"
            : entry.pending != null
              ? "待同步"
              : deleted
                ? "回收站"
                : "已提交服务器";
        card.addView(
          text(
            formatDate(note.optString("created_at")) + "  ·  " + state,
            12,
            entry.error != null ? Color.rgb(160, 54, 44) : MUTED
          )
        );
        space(card, 12);
        TextView body = text(note.optString("content"), 16, INK);
        body.setMaxLines(7);
        body.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(body);
        JSONArray tags = note.optJSONArray("tags");
        if (tags != null && tags.length() > 0) {
          space(card, 10);
          StringBuilder line = new StringBuilder();
          for (int i = 0; i < tags.length(); i++) line
            .append("#")
            .append(tags.optString(i))
            .append("  ");
          card.addView(text(line.toString(), 12, BLUE));
        }
        space(card, 12);
        card.addView(button("查看 / 管理", () -> openNote(entry), false));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(12));
        feed.addView(card, p);
      }
      if (count == 0) {
        space(feed, 28);
        feed.addView(
          text(
            query.isEmpty() ? "这里还没有想法。" : "没有找到相关想法。",
            14,
            MUTED
          )
        );
      }
    } catch (Exception e) {
      status("读取本机记录失败，请先导出备份");
    }
  }

  private String formatDate(String value) {
    try {
      return java.time.OffsetDateTime.parse(value)
        .atZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(
          java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        );
    } catch (Exception e) {
      return value;
    }
  }

  private void openNote(Store.Entry entry) {
    ScrollView scroll = new ScrollView(this);
    TextView body = text(entry.note.optString("content"), 16, INK);
    body.setTextIsSelectable(true);
    body.setPadding(dp(24), dp(16), dp(24), dp(16));
    scroll.addView(body);
    new AlertDialog.Builder(this)
      .setTitle(entry.error != null ? "本地内容已保留" : "想法")
      .setView(scroll)
      .setNegativeButton("关闭", null)
      .setNeutralButton("复制", (d, w) -> copy(entry.note.optString("content")))
      .setPositiveButton("管理", (d, w) -> manage(entry))
      .show();
  }

  private void manage(Store.Entry entry) {
    if (entry.error != null) {
      new AlertDialog.Builder(this)
        .setTitle("同步冲突")
        .setMessage(
          entry.error +
            "\n\n保留为新想法后将会发布，同时重新拉取服务器上的原记录。"
        )
        .setNegativeButton("稍后处理", null)
        .setPositiveButton("保留为新想法并发布", (d, w) -> {
          try {
            store.keepConflictAsCopy(entry);
            renderFeed();
            sync();
          } catch (Exception e) {
            status(e.getMessage());
          }
        })
        .show();
      return;
    }
    boolean deleted = !entry.note.isNull("deleted_at");
    String[] actions = deleted
      ? new String[] { "恢复并发布", "编辑历史" }
      : new String[] { "编辑", "移到回收站", "编辑历史" };
    new AlertDialog.Builder(this)
      .setTitle("管理想法")
      .setItems(actions, (d, index) -> {
        if ((deleted && index == 1) || (!deleted && index == 2)) {
          history(entry);
          return;
        }
        if (!deleted && index == 0) {
          if (
            entry.pending != null &&
            !entry.pending.equals("create") &&
            !entry.pending.equals("update")
          ) {
            status("请先完成同步");
            return;
          }
          Runnable edit = () -> {
            drafts
              .edit()
              .putString("id", entry.note.optString("id"))
              .putInt("version", entry.note.optInt("version"))
              .putString("content", entry.note.optString("content"))
              .putString("tags", joinTags(entry.note.optJSONArray("tags")))
              .commit();
            currentTab = "capture";
            build();
          };
          if (
            !drafts.getString("content", "").trim().isEmpty()
          ) new AlertDialog.Builder(this)
            .setMessage("当前有未保存的草稿，要用这条想法替换吗？")
            .setNegativeButton("保留草稿", null)
            .setPositiveButton("替换", (a, b) -> edit.run())
            .show();
          else edit.run();
          return;
        }
        new AlertDialog.Builder(this)
          .setMessage(
            deleted ? "恢复后会重新发布到博客。" : "移到回收站？之后仍可恢复。"
          )
          .setNegativeButton("取消", null)
          .setPositiveButton("确定", (a, b) -> {
            try {
              store.removeOrRestore(entry, deleted);
              renderFeed();
              sync();
            } catch (Exception e) {
              status(e.getMessage());
            }
          })
          .show();
      })
      .show();
  }

  private String joinTags(JSONArray tags) {
    StringBuilder result = new StringBuilder();
    if (tags != null) for (int i = 0; i < tags.length(); i++) {
      if (i > 0) result.append(", ");
      result.append(tags.optString(i));
    }
    return result.toString();
  }

  private void history(Store.Entry entry) {
    status("正在读取编辑历史…");
    IO.execute(() -> {
      try {
        JSONArray items = Api.request(
          "/thoughts/" + entry.note.getString("id") + "/history",
          "GET",
          null,
          account.token()
        ).getJSONArray("items");
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < items.length(); i++) {
          JSONObject row = items.getJSONObject(i),
            note = row.getJSONObject("item");
          body
            .append("版本 ")
            .append(note.getInt("version"))
            .append(" · ")
            .append(formatDate(row.getString("saved_at")))
            .append("\n\n")
            .append(note.getString("content"))
            .append("\n\n────────\n\n");
        }
        runOnUiThread(() -> {
          TextView text = text(
            items.length() == 0 ? "还没有编辑历史。" : body.toString(),
            15,
            INK
          );
          text.setPadding(dp(20), dp(12), dp(20), dp(12));
          text.setTextIsSelectable(true);
          ScrollView scroll = new ScrollView(this);
          scroll.addView(text);
          new AlertDialog.Builder(this)
            .setTitle("编辑历史")
            .setView(scroll)
            .setPositiveButton("关闭", null)
            .show();
          status("历史已加载");
        });
      } catch (Exception e) {
        runOnUiThread(() -> status(errorMessage(e)));
      }
    });
  }

  private void settings() {
    surface.addView(text("设置", 26, INK));
    space(surface, 24);
    surface.addView(text("服务器", 12, MUTED));
    space(surface, 8);
    surface.addView(text(Api.ORIGIN, 16, INK));
    space(surface, 24);
    surface.addView(button("登录 / 重新登录", () -> login(), true));
    space(surface, 12);
    surface.addView(
      button(
        "导出本机记录",
        () -> {
          Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
          intent.setType("application/json");
          intent.addCategory(Intent.CATEGORY_OPENABLE);
          intent.putExtra(
            Intent.EXTRA_TITLE,
            "thoughts-local-" + java.time.LocalDate.now() + ".json"
          );
          startActivityForResult(intent, 41);
        },
        false
      )
    );
    space(surface, 12);
    surface.addView(
      button(
        "网页管理与完整历史导出 ↗",
        () ->
          startActivity(
            new Intent(
              Intent.ACTION_VIEW,
              android.net.Uri.parse(Api.ORIGIN + "/app/")
            )
          ),
        false
      )
    );
    space(surface, 12);
    surface.addView(button("退出并清除本机记录", () -> logout(), false));
    space(surface, 30);
    surface.addView(
      text(
        "想法 1.0\n\n无广告、无统计、无后台常驻。\n打开应用、保存记录或点同步时联网。\n\n卸载会清除本机数据，卸载前请先同步或导出。",
        14,
        MUTED
      )
    );
  }

  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == 41 && result == RESULT_OK && data != null) {
      try {
        JSONArray records = new JSONArray();
        for (Store.Entry entry : store.entries())
          records.put(
            new JSONObject()
              .put("thought", entry.note)
              .put(
                "pending",
                entry.pending == null ? JSONObject.NULL : entry.pending
              )
              .put("error", entry.error == null ? JSONObject.NULL : entry.error)
          );
        JSONObject output = new JSONObject()
          .put("schema_version", 1)
          .put("records", records)
          .put(
            "draft",
            new JSONObject()
              .put("content", drafts.getString("content", ""))
              .put("tags", drafts.getString("tags", ""))
              .put("id", drafts.getString("id", null))
              .put("version", drafts.getInt("version", 0))
          );
        try (
          java.io.OutputStream out = getContentResolver().openOutputStream(
            data.getData()
          )
        ) {
          out.write(output.toString(2).getBytes("UTF-8"));
        }
        status("本机记录和未同步草稿已导出");
      } catch (Exception e) {
        status("导出失败，请重试");
      }
    }
  }

  private void login() {
    EditText password = input("管理密码", false);
    password.setInputType(
      InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
    );
    password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
    LinearLayout box = column();
    box.setPadding(dp(24), dp(8), dp(24), dp(8));
    box.addView(password);
    AlertDialog dialog = new AlertDialog.Builder(this)
      .setTitle("登录想法")
      .setView(box)
      .setNegativeButton("取消", null)
      .setPositiveButton("登录", null)
      .create();
    dialog.setOnShowListener(d ->
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
        String value = password.getText().toString();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        IO.execute(() -> {
          try {
            String token = Api.request(
              "/login",
              "POST",
              new JSONObject().put("password", value),
              ""
            ).getString("token");
            account.save(token);
            runOnUiThread(() -> {
              password.setText("");
              dialog.dismiss();
              status("已登录");
              sync();
            });
          } catch (Exception e) {
            runOnUiThread(() -> {
              dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
              password.setError(errorMessage(e));
            });
          }
        });
      })
    );
    dialog.show();
  }

  private void logout() {
    try {
      for (Store.Entry entry : store.entries())
        if (entry.pending != null) {
          status("还有未同步记录，请先同步或导出");
          return;
        }
      if (!drafts.getString("content", "").isEmpty()) {
        status("还有草稿，请先保存或导出");
        return;
      }
    } catch (Exception e) {
      status(e.getMessage());
      return;
    }
    new AlertDialog.Builder(this)
      .setTitle("退出登录？")
      .setMessage("会清除手机上的已同步记录，服务器中的记录不受影响。")
      .setNegativeButton("取消", null)
      .setPositiveButton("退出", (d, w) -> {
        AlertDialog progress = new AlertDialog.Builder(this)
          .setMessage("正在退出…")
          .setCancelable(false)
          .show();
        IO.execute(() -> {
          try {
            String token = account.token();
            if (!token.isEmpty()) {
              try {
                Api.request("/logout", "POST", new JSONObject(), token);
              } catch (Api.Failure e) {
                if (e.code != 401) throw e;
              }
            }
            account.clear();
            store.clear();
            drafts.edit().clear().commit();
            runOnUiThread(() -> {
              progress.dismiss();
              status("已退出并清除本机记录");
            });
          } catch (Exception e) {
            runOnUiThread(() -> {
              progress.dismiss();
              status(errorMessage(e));
            });
          }
        });
      })
      .show();
  }

  private void copy(String value) {
    ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
      ClipData.newPlainText("Thought", value)
    );
    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
  }

  private void status(String value) {
    message = value == null ? "操作失败，请重试" : value;
    if (statusView != null) statusView.setText(message);
  }

  private String errorMessage(Exception e) {
    if (e instanceof Api.Failure) return e.getMessage();
    return "暂时无法同步，本机记录已保留。请检查网络后重试。";
  }

  private void sync() {
    if (!SYNCING.compareAndSet(false, true)) return;
    status("正在同步 · 本机记录已保留");
    IO.execute(() -> {
      String result;
      try {
        result = Sync.run(store, account);
      } catch (Exception e) {
        result = errorMessage(e);
      } finally {
        SYNCING.set(false);
      }
      String message = result;
      runOnUiThread(() -> {
        if (!isDestroyed()) {
          status(message);
          renderFeed();
        }
      });
    });
  }
}
