package top.narozeol.thoughts;

import android.app.Activity;
import android.widget.LinearLayout;
import java.util.concurrent.ExecutorService;

/** Each feature owns its view and lifecycle; the Activity owns navigation and shared services. */
interface Feature {
  String id();
  String label();
  void render(LinearLayout surface);

  default String headerAction() {
    return "";
  }

  default void performHeaderAction() {}

  default void renderFooter(LinearLayout footer) {}

  default void leave() {}

  default void refresh() {}

  interface Host {
    Activity activity();
    Store store();
    Account account();
    ExecutorService executor();
    String activeFeature();
    void navigate(String id);
    void redraw();
    void status(String message);
    void sync();
    void export(boolean remote);
    void disconnect();
  }
}
