# 想法：开发与运维

## 产品与数据流

Android 原生 App → SSH 受限通道 → 阿里云本机 API → 原 Gist。博客读者只读取 Gist，没有服务器回退。想法全部公开；手机离线保存草稿和待同步记录，联网打开应用或手动同步后发布，没有后台常驻服务。博客原有正文保持不变。

App 分为记录、想法、服务、设置四个入口：快速输入与草稿、搜索与回收站、发布与备份状态、设备登记与导出。界面采用已确认的「纸页」方向：无边框编辑区、页首发布操作、细线文字列表和分组设置；取消大标题、重复说明与卡片堆叠。宽屏内容限宽。

设备首次运行生成 Android Keystore RSA 3072 密钥，私钥不离开系统密钥库。首次连接可输入一次服务器密码，App 在校验固定服务器主机公钥后，通过 SSH 执行固定登记脚本，再验证受限设备密钥登录。密码不写入磁盘或偏好设置，使用后清空字节缓冲；仍可由管理员手动登记公钥。固定服务器主机公钥防止连接到冒充的服务器；密钥变化时拒绝连接，需要核验并更新配置。卸载或换机后需重新登记。

仅沿用 SSH 22 端口，不需要公网 HTTP、HTTPS、证书或新增安全组规则。API 只监听 `127.0.0.1:8765`。GitHub 的 Gist 写入凭据仅保存在服务器。

## 结构与扩展约定

- `android/`：Android 8.0+ 原生 Java、SQLite、Keystore。唯一运行时依赖为校验 SHA256 的 JSch；第三方许可证包含在 APK assets 中。
- `android/src/top/narozeol/thoughts/Feature.java`：模块接口。MainActivity 负责导航与生命周期；ThoughtsModule、ServerFeature、SettingsFeature 各自负责界面。
- `Transport.java` / `SshTransport.java`：传输接口与 SSH 实现；业务模块不直接接触密钥和 SSH 会话。
- `ServerProfile.java`：服务器身份与固定主机公钥；Store 按 profile 分库。目前界面只配置阿里云，多服务器选择尚未实现。
- `server/`：Flask + Gunicorn；SQLite 保存记录、历史、会话与发布队列；`ssh_gateway.py` 实现版本化受限 RPC。
- `deploy/`：设备登记与撤销、systemd 服务、每分钟发布重试、每天备份。
- 仓库 `assets/js/thoughts.js`：只读 Gist raw URL，搜索与分页在浏览器完成。
- `.github/workflows/{blog,thoughts-server,thoughts-android}.yml`：按目录触发各自检查。文章或博客样式改动不构建 APK；Android 代码、构建配置和共用 SSH 协议实现变化才构建并运行模拟器。内部 PR 使用对应 push 检查，避免重复构建；外部 PR 单独验证。所有工作流保留手动入口。

增加服务器管理功能时，新增独立 Feature 和明确的服务端路由；需要新权限时加入设备 capability 白名单，并由管理员显式授权。现有权限为 `thoughts` 和只读的 `system.read`。不能通过功能模块执行任意 shell、SFTP 或端口转发。设备只发送固定 `thoughts-rpc-v1` 命令及 JSON 请求；网关验证登记、路由、方法和权限后转发到固定本机 API。短期内部会话在请求结束时删除，手机不持有 API bearer token。

## 同步与持久化

手机先写本地 SQLite，再请求服务器。新记录用固定 UUID 重试，避免重复创建；修改使用版本号检查。同步期间产生的新本地编辑不会被响应覆盖。冲突保留本地内容，可另存为新想法后发布。

服务端修改与发布队列递增在同一个事务里完成。发布器串行执行，读取一致快照后释放数据库锁，再写 Gist；成功只确认这次快照的版本。上传期间的新记录仍然排队，失败不会丢失。

「已保存到服务器」与「已发布到 Gist」分别反馈。移入回收站后，下一次发布会从当前 Gist 内容中移除；恢复会重新发布。数据库和 Gist 自身的历史仍保留先前内容。单次发布上限 900 KB，超出保留队列并报告错误，不截断内容。

## 部署与设备登记

构建 APK 后执行 `bash tools/thoughts/deploy/stage.sh`，上传应用和部署脚本，不上传凭据。已有服务器的数据库、Gist token 和原有 SSH 公钥保持不变。

首次安装或更新系统服务，在自己的终端运行：

```sh
ssh -t aliyun bash /home/naro/.local/share/naro-thoughts/deploy/activate.sh
```

如未配置 Gist token，脚本会以不回显方式提示输入仅有 `gist` scope 的 classic PAT；随后 sudo 安装本机 API 和定时器。只停止带本项目标记的旧 Caddy 服务，不安装代理或修改安全组。Token 保存为 `0600` 的 `~/.local/share/naro-thoughts/gist-token`。

安装 APK，在「服务 → 连接服务器」输入服务器密码即可自动登记。密码只用于这一次连接，之后使用手机密钥。需要手动登记时，在「服务 → 手动登记公钥」复制公钥，然后执行：

```sh
ssh -t aliyun python3 /home/naro/.local/share/naro-thoughts/deploy/register-device.py
```

按提示粘贴公钥，回到 App 点击「验证连接」。也可用 `--name 我的手机 --key-file phone.pub` 登记文件中的公钥。脚本只增加带 `restrict,command=...` 的设备条目，保留已有管理员公钥。不要把手机公钥手动作为不受限登录密钥加入 authorized_keys。

```sh
# 查看设备（在服务器运行）
python3 ~/.local/share/naro-thoughts/deploy/register-device.py list
# 撤销指定设备，下一次请求立即拒绝
python3 ~/.local/share/naro-thoughts/deploy/register-device.py revoke --id DEVICE_ID
```

当前 Gist 沿用 `a783c548bd3c7b22578a4bd3748bc9d5`。它是 unlisted，通过公开链接即可读取。初次迁移只修复原 JSON 一处缺失逗号，12 条内容按原文导入；备份位于 `backups/legacy-original.json`。

## 构建与检查

API 测试：

```sh
python3 -m venv .venv
.venv/bin/pip install -r tools/thoughts/server/requirements.txt pytest==8.4.2
.venv/bin/python -m pytest tools/thoughts/server/tests -q
```

Android 使用 JDK 17、platform 35、build-tools 35.0.0，不需要 Gradle：

```sh
export ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android.jar"
export ANDROID_BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"
export THOUGHTS_KEYSTORE=/path/to/private/android-signing.jks
export THOUGHTS_KEYSTORE_PASSWORD_FILE=/path/to/private/signing-password
bash tools/thoughts/android/build.sh
```

签名别名为 `thoughts`，产物 `android/build/thoughts.apk`，当前版本 1.3.0（versionCode 6）。签名文件需单独备份；同签名覆盖安装保留数据，后续升级需递增 versionCode。

GitHub Actions 支持 repository secrets `THOUGHTS_KEYSTORE_BASE64` 和 `THOUGHTS_KEYSTORE_PASSWORD`；没有 secrets 时构建独立包名的「想法·预览」，可与正式版共存，不能用于更新正式版。CI 使用临时签名，仅供验证。

Android 10 / 15 模拟器验证离线保存、列表与草稿、同步响应不覆盖新编辑，以及 Android Keystore 签名、一次性密码登记与错误密码拒绝、密码缓冲清理、受限 SSH 登录、增删改恢复、历史、主机公钥拒绝和权限检查。SSH 测试在临时 CI 服务上运行，不连接生产服务器、不写 Gist；页面截图和日志作为 verification artifact 留存。

## 运维

```sh
sudo systemctl status thoughts thoughts-publish.timer thoughts-backup.timer
sudo journalctl -u thoughts -u thoughts-publish --since today
sudo systemctl start thoughts-publish
sudo systemctl start thoughts-backup
```

数据目录为 `~/.local/share/naro-thoughts/`；每日 SQLite online backup 保留 30 天。App 支持本机草稿及队列导出、服务端完整历史导出；备份目前在同一台服务器，可另存到自己的设备。

更换 Gist token：`python3 ~/.local/share/naro-thoughts/deploy/configure-gist.py`。恢复备份前停止 API 与发布定时器，另存当前数据库及 WAL，再替换数据库、恢复 `naro:naro` 所有权和 `0600` 权限后启动服务，不直接覆盖运行中的 SQLite。

本地开发可用 `THOUGHTS_DEV=1 THOUGHTS_ORIGIN=http://127.0.0.1:8765` 启动 Gunicorn；将 `THOUGHTS_DATABASE` 指向测试路径，避免开发写入生产数据。旧网页管理仍可在本机开发或管理员 SSH 转发中使用，原生 App 不依赖网页密码或 OAuth。
