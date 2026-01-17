---
date: 2026-01-14
title: "Linux Samba 服务器配置笔记"
category: 记录
tags: [linux, 记录]
excerpt: "记录Linux下Samba服务器的配置过程和注意事项"
---

# 前言

毕设季，又到了读论文的时候，读论文还是要记一些笔记啥的。

我一般都用我最喜欢的开源PDF阅读软件Okular进行阅读和记笔记，问题在于Okular本身不提供云同步笔记的功能。所以我需要一个能在设备间共享笔记的方案。

当然使用一个商业软件肯定是最好的选择，但是谁让我喜欢折腾一下。

Linux + Samba共享文件这套方案之前其实也折腾过，当时是在刷了linux的surface pro4上安装samba，以供windows访问。虽然最后我发现这样做还不如直接python一行命令开一个http server来传文件。

现在我想在任何设备上，都能以挂载远端存储的方式来读取文件，所以我打算把存储地点放到我的ECS上。刚好最近也给ECS买了个域名，这样访问起来也很方便


教程方面先看看能不能在ChatGPT老师的指导下完成，不行的话再看看别人的教程吧

# 安装组件

安装组件，顺便一提，这种系统级服务，最好不要挂载在某个具体的用户下

下面没有加sudo是默认sudo su到了root下

```bash
# Debian / Ubuntu
apt install -y samba samba-common-bin wsdd
```

说明：

- samba：核心服务（smbd）

- samba-common-bin：工具（smbpasswd, testparm）

- wsdd：Windows 10/11 “网络”发现支持（适用于局域网进行设备发现）

设置开机自启

```bash
systemctl enable smbd
systemctl start smbd

# ECS场景下不用启用
# systemctl enable wsdd
# systemctl start wsdd
```

一切正常

![Figure1](/assets/images/2026-01-14-samba-server/Figure1.png)

# 配置samba

修改samba配置文件`/etc/samba/smb.conf`

1. **禁用SMB1**
    ```ini
    [global]
    server min protocol = SMB2
    server max protocol = SMB3
    ```
    ![Figure2](/assets/images/2026-01-14-samba-server/Figure2.png)
    SMB1 已废弃

    - 存在严重安全漏洞（如 WannaCry）
    - Windows 新版本默认禁用

2. **禁用匿名访问（除非只读）**
    ```ini
    map to guest = Never
    ```

    - 防止“误连即写”
    - 避免审计与权限失控

3. **明确服务器角色**
    ```ini
    server role = standalone server
    security = user
    ```
    - 明确这是独立文件服务器
    - 避免 Samba 自动尝试域控制逻辑

# 权限控制

1. **创建 Linux 组**

```bash
groupadd share_users
```

2. **创建 Linux 用户并加入组**

```bash
useradd -m samba
usermod -aG share_users samba
```

3. **为 Samba 启用用户**

同时设置密码，该密码独立于linux密码体系
```bash
smbpasswd -a samba
```

- Samba 用户 ≠ Linux 密码

- 这是一层“访问门槛”

4. **创建共享目录**

```bash
mkdir -p /data/share
chown root:share_users /data/share
chmod 2770 /data/share
```
解释：

- 2：setgid，确保新文件继承组

- 770：只有组内成员可访问

- Linux 层只做“粗粒度控制”

5. **启用 Windows ACL 支持**
```ini
vfs objects = acl_xattr
map acl inherit = yes
store dos attributes = yes
```
- `acl_xattr` 使用扩展属性存储 Windows ACL
- `map acl inherit = yes` 允许 Windows 权限继承语义生效
- `store dos attributes = yes` 支持“只读 / 隐藏 / 系统”等属性

6. **配置share行为**
```ini
[share]
path = /data/share
browseable = yes
writable = yes

valid users = @share_users
force group = share_users

create mask = 0660
directory mask = 2770
```
- `browseable` = yes 是否出现在“网络”
- `valid users = @share_users` 只允许该组访问
- `force group = share_users` 防止文件组漂移
- `create mask / directory mask` 控制新文件 / 目录的默认权限

# 防火墙设置

放行

- TCP 445 （samba服务端口）
- UDP 3702（windows设备服务发现端口，实际上不用设置）

注意配置白名单，我使用自己写的一个[小工具](https://github.com/NaroZeol/aliyun-security-group-mgr)在服务器上手动为本机IP添加白名单

# Windows配置

1. **文件管理器->此电脑->右键添加一个网络设备**

![Figure3](/assets/images/2026-01-14-samba-server/Figure3.png "没错，我是Window 10爱好者")

2. **按照`\\server-addr\share`的方式填入**

![Figure4](/assets/images/2026-01-14-samba-server/Figure4.png)

3. **给定一个名称，该名称和`C://` `D://`同级**

![Figure5](/assets/images/2026-01-14-samba-server/Figure5.png)

4. **测试写入共享**

![Figure6](/assets/images/2026-01-14-samba-server/Figure6.png)
![Figure7](/assets/images/2026-01-14-samba-server/Figure7.png)

Bingo ！！！！！

# Linux配置

## 临时挂载
我使用的是KDE，自带的文件管理器有类似的配置，但是此处的配置只会作为临时配置存在，只能在文件管理器中临时访问。

![Figure8](/assets/images/2026-01-14-samba-server/Figure8.png)

如果需要持久化访问，需要直接mount

1. **创建挂载点**

我选择在用户态进行挂载，挂载在home目录下

```bash
# 具体名字自行决定
mkdir -p ~/mnt/FG204
```

2. **挂载cifs**
```bash
# 具体服务器地址自行更改
mount.cifs //narozeol.top/share ～/mnt/FG204\
    -o vers=3.1.1,username=samba
```
这样做也只能做到临时挂载，如果需要在用户态自动挂载，需要使用systemd用户级automount功能

## 用户态自动挂载

1. **准备权限文件（注意权限）**

    写入文件`./smbcred`，内容为

    ```text
    username=xxx
    password=xxx
    ```

    ```bash
    chmod 600 ~/.smbcred
    ```

2. **在用户态创建可供用户访问的挂载点**
    以用户身份

    ```bash
    # 具体路径自定义
    mkdir ~/mnt/FG204
    ```

3. **修改`/etc/fstab`**
    启动时自动挂载

    在`/etc/fstab`最后追加

    ```text
    //narozeol.top/share  /home/naro/mnt/FG204  cifs  _netdev,x-systemd.automount,noatime,credentials=/home/naro/.smbcred,vers=3.1.1,uid=1000,gid=1000  0  0
    ```

    具体参数自定义

