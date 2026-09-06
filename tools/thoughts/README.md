# 想法：开发与运维

## 已确认的产品约束

- 想法全部公开，没有私密状态或可见范围切换。
- Android 原生 App → 阿里云 → Gist；博客只从 Gist 获取内容，没有阿里云 API 回退。
- 记录、修改、删除、恢复、历史和导出需要登录。GitHub 写入凭据只留在服务器。
- 手机离线保存草稿和待同步记录，联网打开应用或手动同步后发布。没有后台常驻服务。
- 保留博客原有文章、About 等正文。首页不突出想法，仅作为普通导航标签。

## 结构

- `tools/thoughts/android/`：Android 8.0+ 原生 Java 界面，系统 SQLite、网络栈和 Keystore；零第三方运行时依赖。
- `tools/thoughts/server/`：Flask + Gunicorn，SQLite 保存记录、历史、会话和发布队列。
- `tools/thoughts/server/publisher.py`：把有效记录写入原 Gist 的 `thoughts.json`，不创建新的 Gist。
- `assets/js/thoughts.js`：只读原 Gist raw URL，搜索与分页在浏览器完成。
- `tools/thoughts/deploy/`：Gunicorn 直接 TLS、API 服务、证书续期、每分钟发布重试、每天备份。
- `.github/workflows/check.yml`：博客构建、API 测试、APK 构建及 Android 模拟器烟雾测试。

Gist 地址沿用原配置。该 Gist 是 unlisted，通过公开链接可读取；它不是登录保护的存储。

## 同步与持久化

手机先写本地 SQLite，再请求服务器。新记录用固定 UUID 重试，避免重复创建；修改使用版本号检查。同步期间产生的新本地编辑不会被响应覆盖。冲突保留本地内容，可另存为新想法后发布。

服务端修改与发布队列递增在同一个事务里完成。发布器串行执行，读取一致快照后释放数据库锁，再写 Gist；成功只确认这次快照对应的版本。上传过程中新增的记录仍然排队，失败也不会丢失。

App 和网页区分「已保存到服务器」与「已发布到 Gist」。移入回收站后，下一次发布从当前 Gist 内容中移除；恢复会重新发布。数据库和 Gist 自身的版本历史仍保留先前内容。

单次发布上限 900 KB，超出会显示错误并保留待发布队列，不截断内容。需要增长到这一规模时再做分卷。

## 首次启用

首次部署时先在服务器运行 `deploy/tls.sh`（以 naro 用户运行），把生成的 `~/.local/share/naro-thoughts/tls/ca.crt` 复制到 `android/res/raw/thoughts_ca.crt`，再构建 Android APK。仓库里的 CA 公共证书已对应当前服务器，正常构建不需重新生成。执行 `bash tools/thoughts/deploy/stage.sh`。它只上传构建和代码，不上传凭据。

在自己的终端运行：

```sh
ssh -t aliyun bash /home/naro/.local/share/naro-thoughts/deploy/activate.sh
```

依次输入仅有 `gist` scope 的 GitHub classic PAT 和服务器的 sudo 密码。Token 使用不回显的交互输入，并保存为服务器上 `0600` 的 `~/.local/share/naro-thoughts/gist-token`。不要把 token 写进仓库、App、网页或聊天。

此脚本不安装 Web 服务器，启用 Gunicorn 直接 TLS 和定时器，最后执行首次 Gist 发布。仅需放行 TCP 8443；不需要 80/443、DNS API 或公网证书。安全组由服务器 `/home/naro/aliyun-security-group-mgr/sgmgr_rules.conf` 管理。变更前先核对云端规则，避免该服务全量同步时顺带修改无关规则。

从旧版本升级时，会在新 API 健康检查成功后停用本项目的 Caddy 服务；不会卸载软件或停用其他项目的 Caddy。更新 APK 至 1.0.1（versionCode 2），它使用新的端口和专用证书信任。博客继续由 GitHub Pages 构建发布，API 服务不托管博客。

- App API：`https://narozeol.top:8443/api/`。
- 辅助网页管理：`https://narozeol.top:8443/app/`。浏览器需要明确导入并信任专用 CA；不要关闭证书校验或忽略警告输入密码。App 自带信任，无需在手机系统中安装 CA。
- APK：本地 `android/build/thoughts.apk` 或 GitHub Actions 的 APK artifact；服务端不再提供 APK 下载页面。
- 管理登录信息：服务器 `~/.local/share/naro-thoughts/login.txt`，首次初始化时生成。
- 服务器数据：`~/.local/share/naro-thoughts/thoughts.sqlite`。
- 原始 Gist 备份：`~/.local/share/naro-thoughts/backups/legacy-original.json`。

初次迁移只修复原 Gist JSON 的一处缺失逗号，12 条内容按原文导入；幂等导入不会重复创建记录。

## 本地开发与检查

```sh
bundle install
bundle exec jekyll serve
python3 -m venv .venv
.venv/bin/pip install -r tools/thoughts/server/requirements.txt pytest==8.4.2
.venv/bin/python -m pytest tools/thoughts/server/tests -q
THOUGHTS_DEV=1 THOUGHTS_ORIGIN=http://127.0.0.1:8765 \
  .venv/bin/python -m gunicorn --chdir tools/thoughts/server --bind 127.0.0.1:8765 'app:create_app()'
```

开发数据库可用 `THOUGHTS_DATABASE` 指向临时位置。不要把开发实例指向生产数据库。

## Android 构建与签名

构建使用 JDK 17、Android platform 35、build-tools 35.0.0；不需要 Gradle 或 Android Studio。

```sh
export ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android.jar"
export ANDROID_BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"
export THOUGHTS_KEYSTORE=/path/to/private/android-signing.jks
export THOUGHTS_KEYSTORE_PASSWORD_FILE=/path/to/private/signing-password
bash tools/thoughts/android/build.sh
```

签名别名固定为 `thoughts`。构建输出 `tools/thoughts/android/build/thoughts.apk`，密钥和构建目录均不纳入 Git。当前正式签名文件由工作机保存在 `~/.local/share/naro-thoughts/`，应单独备份，后续升级必须使用同一密钥并递增 manifest 的 versionCode。

GitHub Actions 可配置 `THOUGHTS_KEYSTORE_BASE64` 和 `THOUGHTS_KEYSTORE_PASSWORD` 两个 repository secrets，生成同签名的正式包；没有 secrets 时生成包名独立的「想法·预览」，可与正式版共存，不可用于更新正式版。预览签名每次变化，只适合 CI 验证。

CI 的模拟器测试验证原生启动、离线保存、列表展示、草稿保留和同步响应不覆盖新编辑。签名检查与实际设备测试是不同层次；新版本仍建议在自己的手机上试用。

## 运维

```sh
sudo systemctl status thoughts thoughts-tls.timer thoughts-publish.timer thoughts-backup.timer
sudo journalctl -u thoughts -u thoughts-publish --since today
sudo systemctl start thoughts-publish
sudo systemctl start thoughts-backup
```

每日备份使用 SQLite online backup 保证 WAL 一致性，保留 30 天。备份目前在同一台服务器，重要数据还应复制到自己的其他设备。网页提供含历史的 JSON 导出；App 提供含本机待同步内容和草稿的导出。

修改管理密码会撤销所有已登录会话：

```sh
ssh -t aliyun 'cd ~/.local/share/naro-thoughts/app && PYTHONPATH=$HOME/.local/share/naro-thoughts/python python3 app.py password'
```

更换 Gist token：在服务器执行 `python3 ~/.local/share/naro-thoughts/deploy/configure-gist.py`。

恢复备份时，先停止 API 与发布定时器并另存当前数据库及 WAL，再替换数据库、恢复 `naro:naro` 所有权和 `0600` 权限，启动服务。避免直接覆盖运行中的 SQLite 文件。

## TLS 与证书维护

Gunicorn 使用 1 个 worker、4 个线程直接提供 TLS，移除反向代理信任。Android 用系统 Network Security Configuration，仅对 `narozeol.top` 信任内置 CA，保持域名校验和禁止明文连接；不使用跳过校验的 TrustManager。

CA 有效期 10 年，服务器证书有效期 397 天。每日定时检查，剩余不足 30 天时签发新服务器证书，并通过 HUP 平滑重载 Gunicorn。续期不会改变 App 的 CA 信任，不需要更新 APK。根 CA 到期前至少 400 天脚本会报错，届时需要先发布带新旧 CA 的 App 过渡版本，再轮换根 CA，不能直接删除重建。

`tls/ca.key` 和 `tls/server.key` 均以 0600 保存在服务器上，绝不进入仓库、APK 或 CI。请单独安全备份整个 `tls/` 目录（数据库定时备份不含私钥）。`android/res/raw/thoughts_ca.crt` 只有公开证书，可以提交。

命令行检查：

```sh
curl --cacert tools/thoughts/android/res/raw/thoughts_ca.crt https://narozeol.top:8443/api/health
```
