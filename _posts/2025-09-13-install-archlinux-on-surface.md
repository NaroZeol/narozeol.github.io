---
date: 2025-05-03
title: "在surface pro4上安装arch linux"
category: 学习经验
tags: [linux, surface, arch]
excerpt: "没事做，就爱作死"
---

人闲着没事就想着作死

最近总觉得debian + gnome用起来不怎么炫酷，有这样那样的限制，尝试了下debian + KDE发现还是不尽人意。于是萌生了尝试安装arch linux的念头，毕竟那群用arch的人的桌面看着确实炫酷。

同时也算跳出舒适圈吧，试试脱离自从接触linux以来就使用的debian系，尝试下新的环境，也算是一种不错的学习经验。

反正最近确实挺闲的，本来以为秋招能乱杀，结果归隐田园了一周一个面试都没有收到，心灰意冷之下，做点事情麻痹下自己也不赖。

实习中学到在搭建环境时尽可能多地记录下操作记录，能够增强对整个系统的理解，同时也能快速回退，是一个看起来低效实则十分高效的做法

因此就写下这么一个文档，记录下过程中做出的更改。

当然肯定不是自己头铁啃官方的英文文档，总体上参考这些文章：

[Arch Linux 详细安装教程，萌新再也不怕了！「2023.10」](https://zhuanlan.zhihu.com/p/596227524)

[Archlinux安装(超详细)](https://www.cnblogs.com/vconlln/p/17065420.html)

# 获取安装镜像

使用我最喜欢的中科大镜像源，无论是物理时延还是稳定性都比其他源都好

https://mirrors.ustc.edu.cn/archlinux/iso/2025.09.01/

# 制作启动盘

使用之前使用过的rufus-4.5来制作启动U盘

没有修改任何参数

![](/assets/images/2025-09-13-install-archlinux-on-surface/Figure1.png)


# 以U盘启动

因为之前就是linux，所以所有surface-linux需要的前置要求都已经满足了（比如关闭secure boot这些）

直接插入U盘，然后在grub中选择回到surface的UEFI界面，选择以U盘启动即可。

# 更换字体

选择一个适合当前屏幕的字体

随便先选一个

```sh
setfont ter-132b
```

# 配置无线连接

## 检查无线网卡

使用

```sh
ip link
```

检查无线网卡`wlan0`是否处于UP状态

要检查的是尖括号内的最后一个元素是否为UP状态

如果没有处于UP状态则使用

```sh
ip link set wlan0 up
```

将其拉起

## 配置无线连接

使用iwctl连接互联网

```
[iwd]# device list # 查看设备名称，此处即为wlan0

[iwd]# station wlan0 scan  # 扫描网络
[iwd]# station wlan0 get-networks  # 输出扫描结果

[iwd]# station wlan0 connect "xxx" # 连接到网络，即常规的无线连接方式，输入无线名称，然后输入无线密码
```

然后就能连接到互联网了，pretty easy

# 磁盘分区

使用cfdisk进行分区管理，比使用fdisk要直观很多

```sh
cfdisk /dev/nvme0n1
```

因为之前就已经安装过debian，自动划分好了分区，所以其实不用做太多调整。

不过我想把swap在分区时就划分出8G（之前是debian默认的1G左右），这样就不用之后再调整了。

最后调整后的分区如下

| Device | Start | End | Sectors | Size  | Type
| - | - | - | - | - | - |
| /dev/nvme0n1p1 | 2048 | 1050623 | 1048576 | 512M | EFI System |
| /dev/nvme0n1p2 | 1050624 | 483395583 | 482344960 | 230G | Linux filesystem | 
| /dev/nvme0n1p3 | 483395584 | 500117503 | 16721920 | 8G | Linux swap |

# 格式化文件系统

将所有的原来的数据都格式化了

```sh
mkfs.fat -F 32 /dev/nvme0n1p1
mkfs.ext4 /dev/nvme0n1p2
mkswap /dev/nvme0n1p3 # 输出 UUID=e2789593-896f-4ab3-a5d5-d116e8f475ec
```

# 临时挂载

临时挂载刚分区并初始化好的分区，便于后续进行arch的初始化

```bash
mount /dev/nvme0n1p2 /mnt
mkdir /mnt/boot
mount /dev/nvme0n1p1 /mnt/boot
swapon /dev/nvme0n1p3
```

# Pacman换源

身处国内，还是要换个源尊重下GFW的

还是用我最喜欢的中科大源

编辑/etc/pacman.d/mirrorlist，在最前面加上

```
Server = https://mirrors.ustc.edu.cn/archlinux/$repo/os/$arch
```

执行更新
```bash
pacman -Syy
```

# 安装系统

执行，安装必要的软件包，其中bash我就用zsh代替了

```bash
pacstrap /mnt base base-devel linux linux-firmware vim git dhcpcd e2fsprogs iwd sudo zsh zsh-completions ntfs-3g gvfs-mtp
```

**注意对于surface-linux来说，需要安装软件包`linux-firmware-marvell`**，否则无法使用wifi，参考 https://www.reddit.com/r/archlinux/comments/txwxq9/surface_pro_3_and_kde_no_wifi_networks/


# 生成文件系统表

自动化挂载信息

```bash
# efi + gpt，选择这个
genfstab -U /mnt >> /mnt/etc/fstab
# biso + mbr
genfstab -p /mnt >> /mnt/etc/fstabgenfstab -U /mnt >> /mnt/etc/fstab
```

# chroot

最伟大的命令，将根目录迁移到磁盘上的系统中，彻底摆脱U盘上的系统

```bash
arch-chroot /mnt
```

# 设置时区

```bash
ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
# 同步硬件时钟
hwclock --systohc
```

# 更改主机名，设置root密码

```bash
echo "Salieri" >> /etc/hostname
hostnamectl hostname Salieri
```

我的主设备的名称是Amadeus，:)

```bash
passwd root
```

# 安装引导程序 

wintel联盟钦定Intel CPU

尊贵的Intel(R) Core(TM) i5-6330U CPU @2.40GHZ

捡破烂的都不想要这十几年前的CPU

```bash
pacman -S intel-ucode
```

配置grub
```bash
# efi + gpt，使用该种
pacman -S grub efibootmgr efivar os-prober

# biso + mbr
pacman -S grub efivar os-prober
```

```bash
# --bootloader-id=name  其中 name 可以更改为自己想要的名称，建议简短明确
# efi + gpt

grub-install --target=x86_64-efi --efi-directory=/boot --bootloader-id=Arch --recheck

# biso + mbr

grub-install --target=i386-pc /dev/nvme0n1
```

修改/etc/default/grub中的内核启动参数

移除quiet，loglevel提升至5，便于日后排错

修改完成后使用`grub-mkconfig -o /boot/grub/grub.cfg`保存

# 配置开机自启项

下载需要的依赖软件，有些其实之前安装过

```bash
pacman -S dhcpcd iwd networkmanager
pacman -S vim sudo bash-completion  iproute2 zsh
```

启用服务

```bash
systemctl enable dhcpcd
systemctl enable iwd
systemctl enable NetworkManager
```

# 创建新用户

```bash
useradd -m -G wheel -s /bin/zsh naro
```

移除`/etc/sudoers`中组`wheel`权限前的注释，开启该组对系统全局资源的访问许可

# Reboot

回到U盘系统

```bash
umount -R /mnt
```


```bash
reboot
```

移除U盘

# 安装图形界面

## 准备xorg

```bash
sudo pacman -S xorg-server xorg-apps xorg-xinit xorg-xclock xterm
```

一路回车

# 安装代理软件

没想到这一步居然花了我一大堆时间，结果居然是因为http代理的格式写错了

最后的做法是：

1. 手机开启热点，surface连接

2. 手机开启Clash Meta

3. 设置->覆写，修改端口为8910，允许来自局域网的连接，监听地址0.0.0.0，

4. 在终端设置`export http_proxy=xxx.xxx.xxx.xxx:8910` `export https_proxy=xxx.xxx.xxx.xxx:8910`，其中xxx为手机的IP地址

5. 这时在终端中就可通过手机的代理访问外部网络了

6. 再参考：https://www.clashverge.dev/install.html#__tabbed_3_2 使用aur安装clash

7. 导入订阅链接，大功告成

# 输入法

第二个坑爹的地方

按照[Archlinux安装(超详细)](https://www.cnblogs.com/vconlln/p/17065420.html)这篇文章的来安装输入法，一切都很顺利，直到我安装了microsoft-edge（别问我为什么linux还用这个，问就是同步方便），发现输入法在这上面不工作。

翻看了fcitx5的issues，发现是一个chromium系列软件的常见问题。

总之修复办法参考官方文档：

https://fcitx-im.org/wiki/Using_Fcitx_5_on_Wayland#TL.3BDR_Do_we_still_need_XMODIFIERS.2C_GTK_IM_MODULE_and_QT_IM_MODULE.3F

其中的Support in Wayland Compositor -> KDE Plasma中的最佳实践，不要设置太多的环境变量，按需设置即可

# 同步配置，设置工作环境

克隆我的dotfiles,执行其中的安装脚本一键部署，非常方便，已经从这个dotfiles仓库中节省了好多时间了

https://github.com/NaroZeol/dotfiles

然后安装些缺失的软件和插件，starship、tmux、zsh-xxx插件等

最后安装vscode，至此一个能够使用的工作环境就彻底建立好了

# surface kernel

使用surface kernel代替原来的kernel，提供触屏支持

参考：

https://github.com/linux-surface/linux-surface/wiki/Installation-and-Setup#arch

跳过和secure boot有关的部分

# 蓝牙

```sh
sudo pacman -S bluez bluez-utils
sudo systemctl enable bluetooth.service
sudo systemctl start bluetooth.service
```

# 启用休眠功能

为了避免睡眠（sleep）维持内存电容需要的耗电，在surface上我选择使用休眠（hibernate）。

休眠功能需要额外进行一些设置才能启用

1. 移除swap partion
    
    本来是不希望使用swap file的，但是好像swap partion针对休眠场景有些问题

2. 扩展根分区，扩展根文件系统

    使用cfdisk扩展根分区，回收移除了的swap partion。

    随后使用resize2fs扩展ext4

3. 创建swapfile

    参考https://www.cnblogs.com/shuimoyun/p/18805669，创建`/swapfile`

4. 更新fstab，移除原有的partion，加入新创建的swapfile

5. 更新grub设置，修改内核启动参数

    参考https://medium.com/@kamalguptawork/configure-swap-space-and-hibernation-in-linux-8efabfa641c4

    使用作者提供的两个命令（本质上就是查询，然后修改grub中的内核参数），再保存grub设置

    ```bash
    RESUME_PARAMS="resume=UUID=$(findmnt / -o UUID -n) resume_offset=$(sudo filefrag -v /swapfile|awk 'NR==4{gsub(/\./,"");print $4;}') "

    if grep resume /etc/default/grub>/dev/null; then echo -e "\nERROR: Hibernation already configured. Remove the existing configuration from /etc/default/grub and add these parameters instead:\n$RESUME_PARAMS";else sudo sed -i "s/GRUB_CMDLINE_LINUX_DEFAULT=\"/GRUB_CMDLINE_LINUX_DEFAULT=\"$RESUME_PARAMS/" /etc/default/grub;fi

    sudo grub-mkconfig -o /boot/grub/grub.cfg
    ```

6. 修改initramfs设置，使其能够处理恢复逻辑
    
    修改/etc/mkinitcpio.conf，在HOOKS行中添加**resume**，注意添加位置，要求在udev之后

    HOOKS=(base udev **resume** autodetect microcode modconf kms keyboard keymap consolefont block filesystems fsck)

7. 重新创建initramfs

    ```bash
    sudo mkinitcpio -P

    ```

reboot后，使用systemctl hibernate就能执行休眠，再在KDE的设置中，将长时间无活动的行为修改为休眠即可。

# 美化

这方面的修改其实不多，主要随心挑选几个主题看看。

最重要的是把终端konsole弄成无边框的

哎我去无边框确实炫酷

# 总结

总结下整个过程吧

感觉arch的安装绝对没有网上吹的那么复杂，本质上就是这么几步，只要按照网上的教程来，真的没有多复杂

1. 从U盘启动便携版，其中带了绝大多数的驱动，开箱即可用

2. 通过便携版进行磁盘分区，文件系统格式化

3. chroot到新系统的根目录，安装必要依赖

4. reboot，此时就能得到一个arch发行版

5. 安装图形化界面

完成图形化界面的安装之后其实大部分的后续操作都和其他发行版没有区别了。

arch确实满足了部分人从零开始搭建linux的幻想，但是实际上大部分操作都是可自动化的，无非就是要自己安安驱动罢了。

另外一方面，arch确实提供了一个功能十分强大的包管理器，同时活跃的用户发行包社区也确实比其他发行版更时尚一些，能够立刻用上别的用户编译的最新版本。

中间最麻烦的反而还是国内特有的网络环境问题，虽然后面发现是我自己脑残忘了用命令行代理要用export。

不过KDE确实比gnome这个老古董现代很多，各种动画做的也很酷炫，能配置的地方也挺多，对surface的触控支持也十分优秀。

还是要感谢surface-linux社区，能够提供这么不错的开源项目，让一个现在只值400块的十多年前的老古董surface pro4也能用上现代的操作系统。
