package top.narozeol.thoughts;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;

public final class SmokeTest extends Instrumentation {

  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  private View find(View view, String text) {
    if (
      text.equals(view.getContentDescription()) ||
      (view instanceof TextView &&
        text.contentEquals(((TextView) view).getText()))
    ) return view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        View found = find(group.getChildAt(i), text);
        if (found != null) return found;
      }
    }
    return null;
  }

  private void check(boolean pass, String message) {
    if (!pass) throw new AssertionError(message);
  }

  private Button findButton(View view, String label) {
    if (
      view instanceof Button && label.contentEquals(((Button) view).getText())
    ) return (Button) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        Button result = findButton(group.getChildAt(i), label);
        if (result != null) return result;
      }
    }
    return null;
  }

  public void onStart() {
    Bundle result = new Bundle();
    MainActivity activity = null;
    try {
      checkAppTrust();
      Store store = new Store(getTargetContext());
      store.clear();
      new Account(getTargetContext()).clear();
      getTargetContext()
        .getSharedPreferences("draft", 0)
        .edit()
        .clear()
        .commit();
      activity = (MainActivity) startActivitySync(
        new Intent(getTargetContext(), MainActivity.class).addFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK
        )
      );
      final MainActivity screen = activity;
      waitForIdleSync();
      runOnMainSync(() -> {
        EditText editor = (EditText) find(
          screen.getWindow().getDecorView(),
          "想法内容"
        );
        check(editor != null, "Native editor did not render");
        editor.setText("Offline capture smoke test");
        findButton(
          screen.getWindow().getDecorView(),
          "保存并发布  ↗"
        ).performClick();
      });
      waitForIdleSync();
      check(
        store.entries().size() == 1,
        "Offline capture must persist exactly one record"
      );
      Store.Entry saved = store.entries().get(0);
      check(
        "create".equals(saved.pending),
        "Offline record must remain queued"
      );
      check(
        !saved.note.has("visibility"),
        "Records must have no visibility state"
      );
      runOnMainSync(() ->
        findButton(screen.getWindow().getDecorView(), "想法").performClick()
      );
      waitForIdleSync();
      check(
        find(screen.getWindow().getDecorView(), "Offline capture smoke test") !=
          null,
        "Saved record did not render in native list"
      );
      runOnMainSync(() ->
        findButton(screen.getWindow().getDecorView(), "＋ 记录").performClick()
      );
      waitForIdleSync();
      runOnMainSync(() ->
        (
          (EditText) find(screen.getWindow().getDecorView(), "想法内容")
        ).setText("Unsent draft")
      );
      runOnMainSync(() ->
        findButton(screen.getWindow().getDecorView(), "设置").performClick()
      );
      waitForIdleSync();
      check(
        "Unsent draft".equals(
          getTargetContext()
            .getSharedPreferences("draft", 0)
            .getString("content", "")
        ),
        "Navigation lost the draft"
      );
      // A local edit while the first request is in flight must survive acknowledgement.
      store.save(
        saved.note.getString("id"),
        "Newer local edit",
        new JSONArray(),
        0
      );
      org.json.JSONObject response = new org.json.JSONObject(
        saved.note.toString()
      ).put("version", 1);
      store.acknowledge(saved, response);
      Store.Entry newer = store.entries().get(0);
      check(
        "Newer local edit".equals(newer.note.getString("content")),
        "Acknowledgement overwrote newer local content"
      );
      check(
        "update".equals(newer.pending),
        "Newer edit must remain queued as an update"
      );
      result.putString(
        "stream",
        "PASS: native launch, offline capture, list rendering, draft persistence, in-flight edit preservation\n"
      );
      finish(-1, result);
    } catch (Throwable error) {
      result.putString(
        "stream",
        "FAIL: " + android.util.Log.getStackTraceString(error)
      );
      finish(1, result);
    } finally {
      if (activity != null) {
        MainActivity screen = activity;
        runOnMainSync(() -> screen.finish());
      }
    }
  }

  private void checkAppTrust() throws Exception {
    int resource = getTargetContext().getResources().getIdentifier(
      "thoughts_ca", "raw", getTargetContext().getPackageName()
    );
    java.security.cert.X509Certificate ca;
    try (java.io.InputStream in = getTargetContext().getResources().openRawResource(resource)) {
      ca = (java.security.cert.X509Certificate) java.security.cert.CertificateFactory
        .getInstance("X.509").generateCertificate(in);
    }
    ca.checkValidity();
    javax.net.ssl.TrustManagerFactory factory = javax.net.ssl.TrustManagerFactory.getInstance(
      javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
    );
    factory.init((java.security.KeyStore) null);
    android.net.http.X509TrustManagerExtensions trust = new android.net.http.X509TrustManagerExtensions(
      (javax.net.ssl.X509TrustManager) factory.getTrustManagers()[0]
    );
    java.security.cert.X509Certificate[] chain = {ca};
    trust.checkServerTrusted(chain, "ECDHE_ECDSA", "narozeol.top");
    boolean rejected = false;
    try {
      trust.checkServerTrusted(chain, "ECDHE_ECDSA", "unrelated.example");
    } catch (java.security.cert.CertificateException expected) {
      rejected = true;
    }
    check(rejected, "App CA trust must be restricted to narozeol.top");
    check(!android.security.NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("narozeol.top"),
      "App must reject unencrypted traffic");
  }
}
