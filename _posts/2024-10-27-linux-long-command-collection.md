---
date: 2024-10-27
title: "linux长命令记录"
category: 记录
tags: [linux, 记录]
excerpt: "记录一些写过的长命令，方便下次查询"
---

在linux中经常需要使用一些组合命令来处理文本base的数据或者通过调用一些二进制服务来实现某些功能

这些组合命令往往很长，为了避免写完就忘，这里做一些记录，方便以后查阅

为了便于通过目录快速查找，每个条目都使用占有一个标题

# 统计ssh失败登陆的用户名

来自[missing-semester](https://missing.csail.mit.edu/2020/data-wrangling/)的例子，用于统计ssh登陆失败的用户名，做了一些自己的修改

```bash
cat auth.log* | grep sshd | grep Disconnected | sed -E "s/.*Disconnected from//" | grep user | grep "invalid\|authenticating" | sed -E "s/.* user (.*) [0-9.]+ port [0-9]+ \[preauth\]/\1/" | sort | uniq -c | sort -n
```

# 改变窗口透明度 

这是我实现gnome-terminal透明化用的代码，写在zshrc里。核心是调用x11的服务让窗口变成透明，同时排除掉vscode，因为在vscode中也会启动终端并运行zshrc，而我不希望它变成透明的

实际上是由ChatGPT-4o编写的脚本，不过挺实用的就顺便记录学习一下

```bash
# transparency
TRANSPARENCY_HEX=$(printf 0x%x $((0xffffffff * 90 / 100)))
if [ -n "$WINDOWID" ]; then
    # 检查窗口名称是否包含”vscode“，如果包含则跳过
    if ! xprop -id "$WINDOWID" | grep -iq "Visual Studio Code"; then
        xprop -id "$WINDOWID" -f _NET_WM_WINDOW_OPACITY 32c -set _NET_WM_WINDOW_OPACITY "$TRANSPARENCY_HEX"
    fi
else
    windowid=$(xprop -root | grep "_NET_ACTIVE_WINDOW(WINDOW)" | tail -n1 | cut -d ' ' -f 5)
    # 检查窗口名称是否包含”vscode“，如果包含则跳过
    if ! xprop -id "$windowid" | grep -iq "Visual Studio Code"; then
        xprop -id "$windowid" -f _NET_WM_WINDOW_OPACITY 32c -set _NET_WM_WINDOW_OPACITY "$TRANSPARENCY_HEX"
    fi
fi
```

# 关闭所有的qq进程

至少在写下这段话的时候qq-nt的linux版本居然不支持退出qq，所以只能自己写一个命令来关闭所有的qq进程

编写思路和ssh非法登陆用户查询是一样的

```bash
ps -aux | grep /opt/QQ | sed -E "s/naro       ([0-9]+).*/\1/" | xargs kill
```

# 统计zsh的命令使用频率

ls频率遥遥领先，哈哈

其实不是很准确的查询， 像组合命令这样的就直接忽略了，仅供娱乐

```bash
cat ~/.zsh_history  |  sed -E "s/sudo //"| cut -d " " -f 2  | sed -E "s/[0-9:;]+//" | sed -E "/^$/d" | sort | uniq -c | sort -n
```
