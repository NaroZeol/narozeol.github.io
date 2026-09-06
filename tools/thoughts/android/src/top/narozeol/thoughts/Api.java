package top.narozeol.thoughts;

import org.json.JSONObject;

final class Api {

  static final String ORIGIN = "ssh://narozeol.top:22";
  static final Transport TRANSPORT = new SshTransport(ServerProfile.ALIYUN);

  static final class Failure extends Exception {

    final int code;

    Failure(int code, String message) {
      super(message);
      this.code = code;
    }
  }

  static JSONObject request(String path, String method, JSONObject body)
    throws Exception {
    return TRANSPORT.request(path, method, body);
  }
}
