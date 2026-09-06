package top.narozeol.thoughts;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.ExecutorService;

/** Shared spacing, typography and touch targets used by every feature. */
class Ui {

  static final int INK = Color.rgb(30, 42, 43),
    MUTED = Color.rgb(111, 122, 121),
    BLUE = Color.rgb(49, 104, 93),
    PAPER = Color.rgb(246, 247, 243),
    LINE = Color.rgb(224, 229, 222),
    WHITE = Color.WHITE,
    ALERT = Color.rgb(159, 78, 50);
  final Feature.Host host;
  final Activity activity;
  final Store store;
  final Account account;
  final ExecutorService IO;

  Ui(Feature.Host host) {
    this.host = host;
    activity = host.activity();
    store = host.store();
    account = host.account();
    IO = host.executor();
  }

  int dp(int value) {
    return Math.round(
      value * activity.getResources().getDisplayMetrics().density
    );
  }

  LinearLayout column() {
    LinearLayout v = new LinearLayout(activity);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  TextView text(String value, int size, int color) {
    TextView v = new TextView(activity);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setLineSpacing(dp(3), 1);
    v.setIncludeFontPadding(false);
    return v;
  }

  GradientDrawable background(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    d.setStroke(dp(1), LINE);
    return d;
  }

  void space(LinearLayout parent, int size) {
    parent.addView(
      new View(activity),
      new LinearLayout.LayoutParams(1, dp(size))
    );
  }

  Button button(String label, Runnable action, boolean primary) {
    Button b = new Button(activity);
    b.setText(label);
    b.setTextSize(14);
    b.setAllCaps(false);
    b.setMinHeight(dp(48));
    b.setTextColor(primary ? WHITE : INK);
    b.setPadding(dp(16), dp(8), dp(16), dp(8));
    b.setBackground(
      new RippleDrawable(
        ColorStateList.valueOf(0x1831685d),
        background(primary ? BLUE : WHITE, 12),
        null
      )
    );
    b.setOnClickListener(v -> action.run());
    return b;
  }

  EditText input(String hint, boolean multiline) {
    EditText v = new EditText(activity);
    v.setTextSize(16);
    v.setTextColor(INK);
    v.setHintTextColor(MUTED);
    v.setHint(hint);
    v.setPadding(dp(16), dp(16), dp(16), dp(16));
    v.setBackground(background(WHITE, 14));
    v.setInputType(
      InputType.TYPE_CLASS_TEXT |
        (multiline
          ? InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
          : 0)
    );
    return v;
  }

  TextWatcher watcher(Runnable fn) {
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

  void heading(
    LinearLayout parent,
    String eyebrow,
    String title,
    String description
  ) {
    TextView kicker = text(eyebrow, 11, BLUE);
    kicker.setLetterSpacing(.14f);
    parent.addView(kicker);
    space(parent, 12);
    TextView name = text(title, 28, INK);
    name.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    parent.addView(name);
    if (!description.isEmpty()) {
      space(parent, 10);
      parent.addView(text(description, 14, MUTED));
    }
    space(parent, 24);
  }

  LinearLayout card(LinearLayout parent, String title, String description) {
    LinearLayout card = column();
    card.setPadding(dp(20), dp(20), dp(20), dp(20));
    card.setBackground(background(WHITE, 16));
    if (!title.isEmpty()) {
      TextView t = text(title, 17, INK);
      t.setTypeface(null, Typeface.BOLD);
      card.addView(t);
    }
    if (!description.isEmpty()) {
      space(card, 10);
      card.addView(text(description, 14, MUTED));
    }
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.setMargins(0, 0, 0, dp(14));
    parent.addView(card, params);
    return card;
  }

  void row(LinearLayout parent, String label, String value) {
    LinearLayout row = new LinearLayout(activity);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(10), 0, dp(10));
    row.addView(
      text(label, 14, MUTED),
      new LinearLayout.LayoutParams(0, -2, 1)
    );
    TextView detail = text(value, 14, INK);
    detail.setGravity(Gravity.END);
    row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
    parent.addView(row);
  }

  void status(String message) {
    host.status(message);
  }

  void sync() {
    host.sync();
  }

  void runOnUiThread(Runnable fn) {
    activity.runOnUiThread(() -> {
      if (!activity.isDestroyed()) fn.run();
    });
  }

  String errorMessage(Exception e) {
    return e instanceof Api.Failure
      ? e.getMessage()
      : "操作未完成，本地内容已保留。请检查连接后重试。";
  }

  void copy(String value) {
    (
      (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE)
    ).setPrimaryClip(ClipData.newPlainText("想法", value));
    Toast.makeText(activity, "已复制", Toast.LENGTH_SHORT).show();
  }

  static String size(long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1048576) return String.format(
      java.util.Locale.ROOT,
      "%.1f KB",
      bytes / 1024.0
    );
    if (bytes < 1073741824) return String.format(
      java.util.Locale.ROOT,
      "%.1f MB",
      bytes / 1048576.0
    );
    return String.format(
      java.util.Locale.ROOT,
      "%.1f GB",
      bytes / 1073741824.0
    );
  }

  Drawable icon(String id, int color) {
    return new Drawable() {
      final Paint p = new Paint(3);

      public void draw(Canvas c) {
        c.save();
        c.translate(getBounds().left, getBounds().top);
        c.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        p.setColor(color);
        p.setStrokeWidth(1.7f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        if (id.equals("capture")) {
          c.drawRoundRect(4, 4, 20, 20, 4, 4, p);
          c.drawLine(8, 12, 16, 12, p);
          c.drawLine(12, 8, 12, 16, p);
        } else if (id.equals("notes")) {
          c.drawRoundRect(5, 3, 19, 21, 2, 2, p);
          for (int y = 8; y < 18; y += 4) c.drawLine(9, y, 15, y, p);
        } else if (id.equals("server")) {
          c.drawRoundRect(3, 4, 21, 11, 2, 2, p);
          c.drawRoundRect(3, 14, 21, 21, 2, 2, p);
          c.drawPoint(7, 7.5f, p);
          c.drawPoint(7, 17.5f, p);
          c.drawLine(12, 7.5f, 17, 7.5f, p);
          c.drawLine(12, 17.5f, 17, 17.5f, p);
        } else {
          for (int y = 6; y <= 18; y += 6) c.drawLine(4, y, 20, y, p);
          c.drawCircle(9, 6, 2, p);
          c.drawCircle(16, 12, 2, p);
          c.drawCircle(8, 18, 2, p);
        }
        c.restore();
      }

      public void setAlpha(int a) {
        p.setAlpha(a);
      }

      public void setColorFilter(ColorFilter f) {
        p.setColorFilter(f);
      }

      public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
      }
    };
  }
}
