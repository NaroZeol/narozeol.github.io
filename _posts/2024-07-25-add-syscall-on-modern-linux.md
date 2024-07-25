---
date: 2024-07-25
title: "为现代Linux内核添加系统调用"
category: 学习经验
tags: [linux, WSL2]
---

# 为现代Linux内核添加系统调用

## 前言

学校的操作系统实验课虽然很糟糕（就没有不糟糕的实验课），但是这个实验还是很有意思的。

如果只是简单的跟着2016年堂堂更新的PPT来完成的话，这个实验还是很简单的。毕竟目标内核版本是2.6.32😁。

所以我决定给自己上点强度，在真正的现代Linux内核（5.15.153，虽然还是有点旧）上增加系统调用。

## 工具

不过话说回来，真正在一个裸机编译和加载一个Linux内核还是挺复杂的，再加上本人是一个WSL享受者。所以这次的实验就借助巨硬提供的好工具来做。

### 编译并更换WSL2的内核

巨硬为WSL2提供了非常好的内核支持，只需前往[microsoft/WSL2-Linux-Kernel: The source for the Linux kernel used in Windows Subsystem for Linux 2 (WSL2)](https://github.com/microsoft/WSL2-Linux-Kernel)下载最新的release就可以获得到微软为WSL2特制的Linux Kernel。

在准备好编译后，在内核文件路径下输入

	sudo make KCONFIG_CONFIG=Microsoft/config-wsl -j8

即可进行编译，其中`-j8`选项指示最多使用8个`cc1`进程进行编译，可以自行更改。

编译过程需要很多额外的库，如果遇到文件缺失尝试google一下是否要装某个库。

巨硬同时为WSL提供了非常方便的内核更换方式，编译后能在在目录的`arch/x86/boot`下找到编译后的`bzImage`。将其下载到windows中，在windows中的User目录下添加一个名为`.wslconfig`的文件，在其中添加以下内容配置WSL2的内核使用哪一个。其中字符串填入下载的`bzImage`的windows路径，注意使用双反斜杠。
```
[wsl2]
	kernel="C:\\XXX\\XXX\\XXX\\bzImage"
```

接着在powershell或者cmd使用`wsl --shutdown`关闭wsl实例，然后重启wsl即可以新的内核启动。

## 添加函数

最核心的工作之一，在`kernel/sys.c`的末尾添加一个函数，来实现我们需要的系统调用。

这里有一个需要注意的点。自2.6时代以后，随着Linux的规模越来越大，为了便于维护，所有的通用的函数定义模版都使用宏进行了包装，以便进行类型检查等操作。

使用`SYSCALL_DEFINE1`来进行函数定义，其中末尾的1表示该函数接受一个参数,`_user`宏指示编译器该指针来自用户空间不应该在内核空间直接解引用。

如果不使用该宏进行定义会导致`copy_to_user`函数总是返回非零值。

```C
SYSCALL_DEFINE1(pedagogictime, struct timespec64 __user *, tv)
{
	if(likely(tv)) {
		struct timespec64 ktv;
		ktime_get_real_ts64(&ktv);

		if (copy_to_user(tv, &ktv, sizeof(ktv)))
			return -EFAULT;
		else
			return 0;
	}
	return -1;
}
```

该服务函数基本功能与`gettimeofday`基本一致。原本的实验内容就是直接使用`do_gettimeofday`来获取当前的Unix时间。但该函数在内核中已被弃用（因为[y2038问题](https://en.wikipedia.org/wiki/Year_2038_problem))，需要使用更加现代的`ktime_get_real_ts64`。

## 添加用户系统调用

在`include/uapi/asm-generic/unistd.h`中模仿其他实现加入我们自己的系统调用编号以及服务函数，同时修改最大系统调用号个数，让其增加一（在5.15.153上默认系统调用的个数是449个）

```
#define __NR_pedagogictime	449
__SYSCALL(__NR_pedagogictime, sys_pedagogictime)

#undef __NR_syscalls
#define __NR_syscalls 450
```

## 添加架构相关的Entry

Linux内核仓库维护系统调用表来自动生成于系统调用相关的汇编代码。两个表分别为`arch/x86/entry/syscalls/syscall_32.tbl` `arch/x86/entry/syscalls/syscall_64.tbl`

考虑到我自己的处理器架构是x86-64，所以我只修改了`syscall_64.tbl`。根据该文件里注释的提示，在448后面加上一条，其中64代表该条目的abi为64位

	449 64      pedagogictime        sys_pedagogictime

## 测试内核

加载完新内核后，输入`uname -a`来确认是否真的加载了新内核。

	Linux Amadeus 5.15.153.1-microsoft-standard-WSL2 #17 SMP Mon Jun 3 23:44:57 CST 2024 x86_64 x86_64 x86_64 GNU/Linux

编译时间显示确实是刚编译好的内核。

接着编写一个简单的函数来测试内核的修改是否生效。

```C
#define _GNU_SOURCE
#include <sys/syscall.h>
#include <unistd.h>
#include <stdio.h>

struct timespec64
{
    long long int tv_sec;
    long tv_nsec;
};

int main ()
{
    struct timespec64 tv;
    long tmp = syscall(449, &tv);

    if (tmp == -1)
    {
        printf("Syscall return wrong\n");
        return 1;
    }

    printf("First, user get tv_sec:%lld\n", tv.tv_sec);
    return 0;
}
```

编译运行符合预期，确实输出了Unix时间。

# 结语

抛开无聊的实验课程和实验报告来说，这个实验本身还是很有意思的。让人亲手编译一次Linux内核，看着不断滚动的编译语句总是给人一种Super Hacker的快乐呢。

虽然给内核添加系统调用在XV6实验中已经做过很多次了。但实际到一个真实的系统还是很困难的，毕竟真实的Linux还是过于庞大了，如果没有网络上良好的指导和博客，还是很难做到的。

感谢巨硬提供这么好用的WSL，赞美WSL！！！

# 参考
[Linux 内核 | 内核的时间函数 - 一丁点儿 (dingmos.com)](https://www.dingmos.com/index.php/archives/38/)

[Linux内核之 printk 打印 - 皓然123 - 博客园 (cnblogs.com)](https://www.cnblogs.com/haoran123/p/17468657.html)

[WSL2（Ubuntu）下添加新的Linux（5.7.9）系统调用_wsl2 系统调用-CSDN博客](https://blog.csdn.net/m0_46161993/article/details/109738662)

[linux - How can i print current time in kernel? - Stack Overflow](https://stackoverflow.com/questions/55566038/how-can-i-print-current-time-in-kernel)

[microsoft/WSL2-Linux-Kernel: The source for the Linux kernel used in Windows Subsystem for Linux 2 (WSL2) (github.com)](https://github.com/microsoft/WSL2-Linux-Kernel)