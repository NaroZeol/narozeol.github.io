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

  private Bundle arguments;

  public void onCreate(Bundle args) {
    super.onCreate(args);
    arguments = args;
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
    if ("key".equals(arguments.getString("mode"))) {
      try {
        result.putString(
          "stream",
          "DEVICE_PUBLIC_KEY: " + new DeviceKey().publicKey() + "\n"
        );
        finish(-1, result);
      } catch (Exception e) {
        result.putString("stream", "FAIL: " + e);
        finish(1, result);
      }
      return;
    }
    try {
      checkDeviceSignature();
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
      screenshot("capture");
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
      screenshot("notes");
      runOnMainSync(() ->
        findButton(screen.getWindow().getDecorView(), "记录").performClick()
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
      screenshot("settings");
      runOnMainSync(() ->
        findButton(screen.getWindow().getDecorView(), "服务").performClick()
      );
      waitForIdleSync();
      screenshot("server");
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
      if (arguments.containsKey("ssh_host_key")) checkSsh();
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

  private void screenshot(String name) throws Exception {
    waitForIdleSync();
    android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot();
    java.io.File dir = new java.io.File(
      getTargetContext().getExternalFilesDir(null),
      "screenshots"
    );
    dir.mkdirs();
    try (
      java.io.OutputStream out = new java.io.FileOutputStream(
        new java.io.File(dir, name + ".png")
      )
    ) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    bitmap.recycle();
  }

  private byte[] field(java.io.DataInputStream in) throws Exception {
    byte[] value = new byte[in.readInt()];
    in.readFully(value);
    return value;
  }

  private void checkDeviceSignature() throws Exception {
    DeviceKey key = new DeviceKey();
    java.io.DataInputStream pub = new java.io.DataInputStream(
      new java.io.ByteArrayInputStream(key.getPublicKeyBlob())
    );
    field(pub);
    java.math.BigInteger exponent = new java.math.BigInteger(field(pub)),
      modulus = new java.math.BigInteger(field(pub));
    java.security.PublicKey publicKey = java.security.KeyFactory.getInstance(
      "RSA"
    ).generatePublic(
      new java.security.spec.RSAPublicKeySpec(modulus, exponent)
    );
    byte[] payload = "device-signature-proof".getBytes("UTF-8");
    java.io.DataInputStream encoded = new java.io.DataInputStream(
      new java.io.ByteArrayInputStream(
        key.getSignature(payload, "rsa-sha2-256")
      )
    );
    check(
      new String(field(encoded), "UTF-8").equals("rsa-sha2-256"),
      "SSH must use RSA SHA-2"
    );
    java.security.Signature signature = java.security.Signature.getInstance(
      "SHA256withRSA"
    );
    signature.initVerify(publicKey);
    signature.update(payload);
    check(
      signature.verify(field(encoded)),
      "Android Keystore signature must verify with exported public key"
    );
    check(
      java.util.Arrays.equals(
        key.getPublicKeyBlob(),
        new DeviceKey().getPublicKeyBlob()
      ),
      "Device key must survive recreation"
    );
    check(
      key.getSignature(payload, "ssh-rsa") == null,
      "Reject legacy SHA-1 signatures"
    );
  }

  private void checkSsh() throws Exception {
    ServerProfile profile = new ServerProfile(
      "ci",
      "CI fixture",
      "10.0.2.2",
      2222,
      arguments.getString("ssh_user"),
      "ecdsa-sha2-nistp256 " + arguments.getString("ssh_host_key")
    );
    SshTransport transport = new SshTransport(profile);
    org.json.JSONObject session = transport.request("/session", "GET", null);
    check(
      session.getString("transport").equals("ssh"),
      "Actual SSH authentication must succeed"
    );
    String id = java.util.UUID.randomUUID().toString();
    org.json.JSONObject note = transport.request(
      "/thoughts",
      "POST",
      new org.json.JSONObject()
        .put("id", id)
        .put("content", "CI SSH record")
        .put("tags", new JSONArray())
    );
    check(note.getInt("version") == 1, "SSH create must persist");
    note = transport.request(
      "/thoughts/" + id,
      "PATCH",
      new org.json.JSONObject()
        .put("version", 1)
        .put("content", "CI SSH edited")
    );
    check(note.getInt("version") == 2, "SSH edit must use version check");
    check(
      transport
        .request("/thoughts/" + id + "/history", "GET", null)
        .getJSONArray("items")
        .length() == 1,
      "SSH edit history must persist"
    );
    transport.request(
      "/thoughts/" + id,
      "DELETE",
      new org.json.JSONObject().put("version", 2)
    );
    note = transport.request(
      "/thoughts/" + id,
      "PATCH",
      new org.json.JSONObject().put("version", 3).put("restore", true)
    );
    check(note.isNull("deleted_at"), "SSH restore must succeed");
    check(
      transport
        .request("/system", "GET", null)
        .getString("service")
        .equals("想法"),
      "Read-only server capability must work"
    );
    boolean rejected = false;
    try {
      transport.request("/login", "POST", new org.json.JSONObject());
    } catch (Api.Failure e) {
      rejected = e.code == 403;
    }
    check(rejected, "Restricted SSH key must not access other API routes");
    rejected = false;
    try {
      new SshTransport(
        new ServerProfile(
          "wrong",
          "CI",
          "10.0.2.2",
          2222,
          arguments.getString("ssh_user"),
          "ecdsa-sha2-nistp256 " + arguments.getString("ssh_wrong_host_key")
        )
      ).request("/session", "GET", null);
    } catch (Api.Failure e) {
      rejected = e.code == 495;
    }
    check(rejected, "Server host key mismatch must be rejected");
  }
}
