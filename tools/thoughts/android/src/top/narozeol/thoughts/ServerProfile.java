package top.narozeol.thoughts;

/** Server identity is kept separate from feature routes and device credentials. */
final class ServerProfile {

  final String id, name, host, user, knownHost;
  final int port;

  ServerProfile(
    String id,
    String name,
    String host,
    int port,
    String user,
    String knownHost
  ) {
    if (!id.matches("[a-z0-9_-]{1,40}")) throw new IllegalArgumentException(
      "Invalid server profile ID"
    );
    this.id = id;
    this.name = name;
    this.host = host;
    this.port = port;
    this.user = user;
    this.knownHost = knownHost;
  }

  static final ServerProfile ALIYUN = new ServerProfile(
    "aliyun",
    "我的服务器",
    "narozeol.top",
    22,
    "naro",
    "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBEfHiGpKcBGoHoPlWc343Ge5JHkJA23wAKsC2STXMJQy/XbDcl77Oe2i4wDzygCQurjIpqCuvwV4EK4YJWKg+xc="
  );
}
