---
date: 2025-10-15
title: "《unix环境高级编程》笔记"
category: 学习经验
tags: [笔记]
excerpt: "一些简单的备忘录"
---

# 前言

《unix环境高级编程》这本书是我在去年三四月份买的，当时看《操作系统导论》，作者一直在推荐这本书，说每一位系统程序员都需要阅读这本书。

在去年买的时候，这本书的第三版还没有中译，于是我做了一个非常傻逼的决定--买英文原版。

拜辞所赐，这本书的读书进度一直非常缓慢，中途也因为做项目和实习也耽搁了很久。最近从结束了一段实习，有了一段相对空闲的时间，就把这本书读了一些。

大概读了三四百页，但总感觉自己最近记忆力下降很明显，看过的东西一下就忘了。虽然有在纸质书上做一些笔记，但是看完之后基本上应该不会再翻书了，所以还是打算做一个精炼版的笔记记录一下。

所以接下来的内容大部分都是将书上的文字版笔记搬上来，应该是只有我能看懂的记录方式。。。

大致就按照大章节的方式来排列

# System Calls and Library Functions

1. 当我们谈及计时时，总是有三种时间概念

    1. Clock time

    有时也被成为"wall clock time"，即你墙上的挂钟的时间，所以Clock Time代表着现实世界的物理时间

    2. User CPU time

    在进程被调度后，在用户空间执行用户代码的实际运行时间

    3. System CPU time

    在内核空间执行内核代码的实际运行时间


2. 虽然对于一般用户来说，区分库函数和syscall是没有太大必要的。但是从这些功能的实现者的角度来看，区分两者是有必要的。

# UNIX Standardization and Implementations

这章主要讲各种UNIX标准和实现，不过从我的角度来看，我只关心Linux，这些标准和其他实现就当个故事听听

1. 总之本书提及的标准有ISO C，POSIX，SUS，FIPS，从我的理解上看，ISO C定义了C语言的工作方式，POSIX定义了C语言与内核交互的方式。SUS（Single UNIX Specification）是POSIX的超集，只有实现了SUS的发行版可以被成为UNIX系统。SUS是一个来自于工程实践的标准，最初开始没有标准时，所有的工程师需要手动使用syscall和自己实现各种工具函数，SUS总结了这些实践，形成了一个超大型的标准。至于FIPS(Federal Information Processing Standard)是由美国联邦政府发布的一个标准，不过已经被废弃了。

2. 现有的UNIX系统基本源自于第六版和第七版的UNIX，这两个版本是最早被大规模发行在贝尔实验室之外的版本，所以后续的其他实现都会参考这两个版本。（顺便一提，xv6的6就是指Version 6的UNIX）

3. 各个分支，当个故事听听（有些不是书上的内容，是我自己写的）：
    
    - UNIX System V Release 4

    即SVR4，AT&T融合了SVR3.2、SunOS、4.3BSD、Xenix而成的新版本。发布该版本时，AT&T也发布了System V Interface Definition（SVID），即System V手册。该手册影响深远，即使现在也能看到（比如以init+inittab的方式作为启动进程）。是现代UNIX除BSD系以外的所有发行版的祖先。

    - 4.4BSD

    由加州大学伯克利分校的计算机研究小组Computer Systems Research Group(CSRG)开发的系统，由美国军方提供赞助，因为当时的AT&T受到反垄断制裁，不允许进入计算机市场，也不允许参与军方计算机项目，只能出于研究目的在高校间共享源代码。

    BSD的主要使命是完成美国军方的课题--实现System V没有实现的网络栈，以此推进早期互联网的发展(ARPANET)。因此socket的实际设计是由BSD完成，而不是由贝尔实验室设计，这也是socket和UNIX的万物皆文件的概念有一种割裂感的原因。尽管后续贝尔实验室想要重新设计网络接口，但因为socket已经成为事实标准从而只能放弃。

    在出色地完成美国军方的课题后，CSRG的使命结束，因此4.4BSD就成为了CSRG发布的最后一个UNIX系统

    - FreeBSD

    由公益组织接受的BSD

    - Linux

    真神不必多解释，有教学系统MINIX发展而来，一位自大狂妄的少年发起的伟大开源项目。

    "A grass-roots effort then sparang up, whereby many developers across the world volunteered their time to use and enhance it"

    - Mac OS X

    源自于FreeBSD，核心名为"Darwin"

    - Solaris

    Sun（现被Oracle收购）的商业发行版，专业的工程师维护的发行版，不过现在随着Sun的落寞，也逐渐失去了过去的地位

    SUN(Stanford University Network)，核心理念为"The Network is the Computer"，一个领先时代的巨人，可惜缺乏商业化手段在竞争中落败。

4. 之前谈论的各个标准是这些实现分支的子集，每个发行版保证了标准的内容，但又往往会提供自己的一些功能。

5. 各个标准定义了一大堆的limits，书中对这段的描述也十分复杂难懂。总之当要编写一个能够跨平台运行的程序，需要注意这些定义的限制。

6. 尽管POSIX带来了统一的标准，但这并不代表在POSIX标准下，所有的行为都是一致的（世界就是这么不美好）。比如`signal`库函数，非常经典的一个例子。因为System V和BSD的分裂，两者实现了不同的语义和行为（是否需要在触发后重新添加handle，是否会打断阻塞性syscall），这种分裂的出现时间早于POSIX，因此POSIX选择尊重这种分裂，POSIX承认signal库函数在所有符合标准的系统中存在，但是不定义它的语义。额外提供了能够支持多种语义和行为的新库函数`sigaction`

# File I/O

1. 绝大多数的I/O任务在UNIX系统中可以通过五个系统调用实现：`open`,`read`,`write`,`lseek`,`close`。

2. 这些基础的I/O操作被称为`unbuffered I/O`，对应由ISO C定义的诸如`printf`的`buffer I/O`。

3. 将终端的输入输出绑定到文件描述符0,1,2不是内核的行为，而是用户空间应用的行为，这点在后续第九章Process Relationships中会讨论（其实是`getty`实现的）

4. 在现代的UNIX系统中，同时能打开的最多fd的值是一个动态值，取决于系统的内存。（在xv6上我记得是固定的上限32个）

5. `openat`接受一个目录fd作为第一个参数，然后在指定的目录fd下打开路径名的文件，这一方面提供了一些便捷，但更重要的是稍微缓解了time-of-check-to-time-of-use(TOCTTOU)问题

    所谓TOCTTOU问题是由于UNIX文件系统API的细分引入的，攻击者可以在检查文件再打开文件的间隙，更改路径文件的指向，以此诱导拥有特权的进程输出了不应该输出的文件内容


    ```c
    // 假设进程当前处于特权模式，此时访问任何文件都不会受阻
    if (access("/tmp/file", xxx...)) { // 所以会在访问文件前检查这个文件的权限是否符合预期
        // 在这个间隙，攻击者更改/tmp/file的指向，指向一个隐私文件
        int fd = open("/tmp/file", xxx...);
        // oh no，隐私文件被访问了
        read(fd, buf, xxx...);
        write(1, buf, xxx...)
    }
    ```

    使用openat，在一个确定的目录上进行访问文件，可以缓解攻击者将整个`/tmp`目录替换的case，但是遗憾的是如果攻击者能够修改/tmp/file的指向，TOCTTOU的问题还是存在的。

6. 一个经典UNIX笑话：有人问Ken Thompson设计UNIX时最大的遗憾是什么，他回答到，我忘记给`creat`加上e了。

    不过还好自从后续版本的open支持制定参数创建文件后，`creat`这个创建文件的方式几乎就没有人使用了。

7. `lseek`的偏移范围可以超过当前文件的大小。当这件事发生时，文件的会自动扩展，将新区域设置为**hole**。**hole**是文件的一块特殊区域，其不占据实际的磁盘空间，保证在读取这片空间时返回0。（可以使用`ls -ls`来观察）

8. 文件表的指向方向为：

    ```
    process table entry
        | (fd)
      file pointer -> file table entry
                        |
                      v-node pointer -> v-node table 
                                          |
                                        v_data-> i-node
    ```

    这种文件表的模式从早期的UNIX开始就不曾改变，引入v-node是为了接入各种形式的文件系统（NFS，tmpfs，procfs等）

    每次open的调用都会在内核空间产生一个file table entry，其中一个文件的offset也是由file table entry管理

9. 因为多进程和多线程的存在，一个file table entry中的offset随时都有可能被更改，使用`lseek`+`read`和`lseek`+`write`的方式会造成很多困扰。因此SUS定义了两个函数来原子化这个两个过程

    `pwrite(xxx..., off_t offset)`和 `pread(xxx..., off_t offset)`，这两个函数更像一个定点的读写，它们都不会导致文件的offset被修改。

    GFS论文的第七节就提到他们使用pread来替换mmap以解决某些问题
    
    > Since we are mainly limited by the networkinterface rather
    > than by memory copy bandwidth, we worked around this by
    > replacing mmap() with **pread()** at the cost of an extra copy.

10. `ioctl`是UNIX为了解决多种多样的设备的交互模式无法使用通用文件流模式表达的问题引入的一个大箩筐。

    之所以导致该问题，是因为现实世界中，不是所有设备都遵循文件流模式

    比如一个磁带机，当它映射为一个文件时，要如何表示倒带？

    虽然可以使用特殊的转义字符替代，但是这样反而带来的不规范。

    因此这样的一个`ioctl`定义如下

    ```c
    int ioctl(int fd, int request, ...);
    ```

    其中的request根据设备类型的不同，选择不同的命令，通过可变长参数传递需要的信息。ioctl做的只是向设备驱动传递用户的request。

11. `/dev/fd/n`提供了一个便捷的方式表示已经打开的文件

    当使用open打开这些fd，实际上是在执行了一次`dup`操作。

    这个功能最大的使用场景就是shell，使用文件`/dev/fd/{0/1/2}`可以快速完成输出的重定向，例如strace将stderr的输出合并到stdout，这样就能管道给下一个程序进行处理，不用去写shell的重定向语法。

    ```bash
    strace ls -o /dev/fd/1 | vim - # vim 会将 `-` 处理为/dev/stdin，注意这是大部分gnu工具链的行为，而不是shell的转义
    ```

