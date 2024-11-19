---
date: 2024-07-31
title: "搭建一个内网穿透服务器"
category: 学习经验
tags: [服务器]
excerpt: "记录搭建内网穿透服务器的过程"
---

# 前言

暑假比较闲，本来是打算学点东西的。

先是想着学下计网，结果读完自顶向下的应用层部分就不想接着看了，这本书感觉写的一般，全是字，看不下去。

接着试着学下MIT 6.5850（原6.824），看完了第一节课，想试试lab。结果要求要先读完MapReduce的原论文。磨磨蹭蹭几天才读完了第三部分。。。

看来这个暑假终究是要一事无成。。。

言归正传，也是突然想起来阿里云的学生认证的300块优惠券还没有领过。于是想弄台服务器来玩玩。

# 服务器购买

阿里愿意拿出这么大的优惠力度来支持学生真是好事

登录https://university.aliyun.com 这个网址，完成学生认证即可领取300元抵用券，可以全站任意消费。

接着就可以创建实例了

我购买的配置如下：

![配置](/assets/images/2024-07-31-my-frp-server/配置.jpg)

因为只有300元，如果想要租一年的服务器的话只能选择2核2G的低配版本。而且使用的也是按流量收费的宽带计费方式

买到手时发现公网ip要另外买。选择弹性公网ip，购买一个就行了。不过这个公网ip的保有费也好贵啊，居然要0.02元一个**小时**，一个月就算不用也要15块。

不过免费的一年服务器，付点公网ip费也无所谓。

# 服务器配置

## 登录服务器

创建后的服务器可以使用ssh登录

阿里云要求用私钥登录

在阿里云的控制台创建一个密钥对，一份密钥会自动下载到本地。接着使用

    ssh -i <本地密钥文件路径> root@<公网ip地址>

登录服务器

## 创建新用户

我个人不太喜欢直接使用root登录的方法，所以我创建了一个新的普通用户。

在服务器的终端中输入

    sudo passwd <新用户名>

即可创建一个新用户

新创建的用户是无法使用sudo的，需要为其赋予权限

在root用户下修改`/etc/sudoers`

    sudo vi /etc/sudoers

然后找到root的那一行，在其模拟该格式添加一行

    <用户名> ALL=(ALL:ALL) ALL

接着保存退出，使用

    su <用户名>

切换到新创建的用户

如果觉得使用ssh密钥登录的方式麻烦也可以修改ssh配置文件来允许使用密码登录。

# 使用frp构建内网穿透

## frp

[frp](https://github.com/fatedier/frp) 是一个专注于内网穿透的高性能的反向代理应用，支持 TCP、UDP、HTTP、HTTPS 等多种协议，且支持 P2P 通信。可以将内网服务以安全、便捷的方式通过具有公网 IP 节点的中转暴露到公网。

选择使用frp的原因主要是它是由国人开发，有详细的中文文档，而且使用起来也足够轮椅。

## frp服务器配置

用于中转流量的服务器被称作**服务器**，也就是在阿里云等云服务平台买的有公网ip的服务器。

下载Release中的压缩包，在服务器上解压。甚至都不需要配置就可以直接运行可执行文件监听默认的7000端口。

出于安全考虑，可以在`frps.toml`中添加一行

    auth.token = "xxx"

启用口令验证，字符串可以自定义。

开启服务后不要忘记在阿里云的控制台中将7000端口打开，让frp的客户端可以访问7000端口

## frp客户端配置

需要进行内网穿透的机器就是所谓的**客户端**。

也是下载Release的压缩包，在客户端解压。

修改其中的配置文件`frpc.toml`，不过话说为什么toml这种配置文件会火，感觉不如json。。。

填入以下内容

```toml
serverAddr = "x.x.x.x"
serverPort = 7000
auth.token = "xxx"

[[proxies]]
name = "RDP"
type = "tcp"
localIP = "127.0.0.1"
localPort = 3389
remotePort = 6000

[[proxies]]
name = "SSH"
type = "tcp"
localIP = "127.0.0.1"
localPort = 22
remotePort = 6022
```
我个人主要需要转发的两个端口是22（SSH），和3389（远程桌面）

然后不要忘了把服务器上对应的remotePort启用

启动frpc之后既可以通过服务器的6022和6000端口访问到我本机电脑的ssh和远程桌面

# 设置自动启动

## 服务器的自动启动

服务器的自动启动参考[官网的教程](https://gofrp.org/zh-cn/docs/setup/systemd/)，通过配置systemd来实现开机自启

## 客户端的自动启动

我个人的客户端是一台Windows 10的PC，要实现自动启动有些复杂。

最开始尝试写一个bat脚本来自启，脚本内容就是启动命令，但是这样在启动后会一直有一个终端窗口，不怎么美观。

在搜索了一下之后我选择使用battoexeconverter工具来讲bat转化成exe文件，然后设置窗口为隐藏（也许写一个Windows窗口应用会更好，不过这是很简单的任务，没必要那么做）

~~接着生成一个快捷方式，将快捷方式放到`C:\Users\%USERNAME%\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup`中，即可实现开机自动运行任务。~~

**2024/9/24 Update**:

使用将快捷方式放入启动文件夹的方式只会在**用户登录**时启动`frpc.exe`而不是**系统启动**时启动。

> tips: **用户登录**指用户输入了正确的密码进入桌面，而**系统启动**才是真正的开机自启

因此真正的做法是使用**任务计划程序**来实现自动启动，`win + S`搜索即可找到该程序，新创建一个任务选择`不管用户是否登录都要运行`，设置触发器为`在系统启动时`，然后将操作设置为对应的bat脚本或者程序加参数

# 保护措施

**更新于2024/11/19**

把一个内网的机器暴露在公网上是很危险的，一些学校甚至会因为这个原因禁止学生使用内网穿透工具。不过我们学校好像不怎么管这个，但是还是要做好保护措施以免被请去喝茶

## IP白名单

原本是想用一些公共服务来屏蔽掉恶意的IP地址，也就是根据公共的恶意IP数据库来设置黑名单，但是文档没看懂。。。所以还是省事一点直接用白名单吧，反正我平时也不怎么移动位置，几个固定的网段足够了

注意到学校网络的出口总是前16位固定，所以添加规则时使用16位掩码，这样就算是动态IP也可以使用了。

添加方法为：打开阿里云控制面板，选择ECS实例，选择安全组，添加安全组

## 日志监护

白名单机制其实已经足够好用了，在启用之后世界立马清净了，而且我不太认为会有人喜欢用大陆运营商的网络来做网络攻击。

不过保险起见我还启用了一项措施，通过检测日志行数来关闭frp服务。

因为那些攻击脚本通过暴力尝试密码的方式来攻击，所以在被攻击时frp日志会暴涨到几千条，而普通正常使用一天的日志几乎不会超过100条。根据这个思路就有了日志监护的保险方法。

首先要开启frp的文本化日志，在服务端的配置文件`frps.toml`添加：

```toml
log.to = '/path/to/log/frps.log'
```

然后重启frp服务就可以看到它将日志写入了这个文件中

接着就是写一个小脚本定期检查这个日志文件的行数，将其加入到systemd的自启项即可

```bash
#!/bin/bash
# Writen by ChatGPT-4o

# 定义日志文件路径和服务名称
LOG_FILE="/path/to/log/frps.log"  # 这里替换成你的日志文件路径
SERVICE_NAME="frps.service"      # 这里替换成你要停止的 systemd 服务的名称

# 定义行数阈值
LINE_THRESHOLD=300

# 定义检查的间隔时间 (单位: 秒，300秒即5分钟)
INTERVAL=300

while true; do
    # 获取日志文件的当前行数
    current_line_count=$(wc -l < "$LOG_FILE")

    # 检查日志文件的行数是否超过阈值
    if [ "$current_line_count" -gt "$LINE_THRESHOLD" ]; then
        echo "日志文件超过了 $LINE_THRESHOLD 行，停止服务: $SERVICE_NAME"
        # 停止 systemd 服务
        systemctl stop "$SERVICE_NAME"
    else
        echo "日志文件行数为 $current_line_count，未超过阈值。"
    fi

    # 等待 INTERVAL 秒后再次执行
    sleep "$INTERVAL"
done
```

systemd的service文件如下：

```toml
[Unit]
Description=Check log file and stop frps service if too many lines

[Service]
ExecStart=bash /path/to/script/frps-log-watch.sh
Restart=always

[Install]
WantedBy=multi-user.target
```

然后启用该服务即可，做法和前面的[官网教程](https://gofrp.org/zh-cn/docs/setup/systemd/)中的一致