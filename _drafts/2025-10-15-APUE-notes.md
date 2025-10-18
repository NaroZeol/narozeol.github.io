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

# Files and Directories

1. 所有拥有读取权限的进程可以查看一个目录的内容，但只有kernel有权修改目录的内容

2. 虽然POSIX标准允许将IPC对象（消息队列，信号量等）表示为文件，但是大多数实现不将它们实现为文件

3. 推荐使用`lstat`替代`stat`可以不follow符号链接

4. 文件拥有的`set-user-ID`和`set-group-ID`属性允许程序以某个身份运行，这是个很有意思的特性，常见于如passwd这样需要root权限修改某些特权文件，但是又希望普通用户能够执行的程序。后面的章节会详细讨论这个特性。

5. 需要区分目录的读权限和执行权限。当一个程序想要通过路径访问一个文件时，它必须要拥有从根目录开始的所有目录的执行权限。目录的读权限用于控制程序能否查看一个目录下的内容，而执行权限用于控制程序是否能够使用目录中的文件。

6. 修改一个目录需要拥有一个目录的写权限和执行权限。删除一个文件实际上与将要被删除的文件的权限无关，只与其涉及的目录的权限有关。（除非目录设置了`sticky bit`，比如`/tmp`就不允许用户移除别人创建的tmp文件)

7. `access`检查权限时总是使用real user ID和real group ID，这样的设置允许一个set-user-ID的程序即使运行时权限被上升至高权限，也可以在运行时检查某个文件当前的使用者是否有权访问

    比如下面的代码编译出来的二进制文件，如果owner设置为root，同时设置了`chmod u+s`，则会出现access不成功，但是open正常的情况。
    ```c
    #include <stdio.h>
    #include <stdlib.h>
    #include <fcntl.h>
    #include <unistd.h>

    int main (int argc, char *argv[]) {
        if (argc != 2) {
            printf("usage"": %s <pathname>\n", argv[0]);
            exit(1);
        }

        if (access(argv[1], R_OK) < 0) {
            printf("access error for %s\n", argv[1]);
        } else {
            printf("read access OK\n");
        }

        if (open(argv[1], O_RDONLY) < 0) {
            printf("open error fot %s\n", argv[1]);
        } else {
            printf("open for reading OK\n");
        }
        return 0;
    }
    ```

8. `umask`系统调用设置了一个进程在创建文件时能够使用的flags（权限设置）

    `mode_t umask(mode_t cmask)`，其中cmask指定了要关闭的创建文件时能够使用的flags

    该系统调用通常由shell使用，初始化一次终端下能够设置的文件的权限，作为一个兜底设置防止私密文件外泄。

    比如我的zsh的默认的设置是022(000-010-010)，即`rwx-rwx-rwx`关闭了group和others的w权限，因此我使用touch创建的文件只会拥有权限位`rw--r--r--`，这样我创建的文件默认情况下其他人都无法写入。

    在不修改umask的情况下，即使使用了`creat("xxx", S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH)`，也只能创建出`rw--r--r--`的文件。

9. inode内容的变化和文件实际内容的变化是两回事，`ls -l`默认只打印出文件实际内容变化的时间，所以在使用chmod时，不会修改`ls -l`的输出

10. 已打开的文件就算链接数归零也不会被清除，只有在链接数归零并且文件被关闭时才会。也就是存在机会恢复已打开但是误删除的文件。

    这个特性同时也可能造成一些问题，比如创建了一个很大的临时文件，然后在没有关闭文件的前提下删除该文件，会导致存储空间继续被占用。

11. 一个文件的内容访问时间，内容修改时间（除了元数据修改时间）都可以通过`futimens`来修改。这个函数的主要使用者是类似tar、zip、git这样的存档程序，通过修改文件时间来保证每台机器上的文件有相同的时间。

12. 目录的访问类似于文件流，每次只读一个，可以通过seek来调整位置。这样设计应该也 可以避免一个目录非常长，一瞬间消耗过多的内存。

13. 虽然传统意义上chdir不会影响到父进程的工作目录，但是linux特殊的进程结构设计和clone语义，允许父子进程共用一个文件表，这种情况下子进程对工作目录的修改是可以影响到父进程的。

14. `st_dev`和`st_rdev`用于标识一个文件，1.逻辑上的设备编号和在设备上的编号，2.物理上的设备编号和设备上的编号

    简单来说就可以通过这两个属性来区分文件从属的文件系统是否一致，具体的用法不用太关心。

# Standard I/O Library

1. libc提供的带缓冲的I/O（如printf等），给于用户一个十分易用的高性能I/O方式，但相对的，这样的封装也让这些缓冲I/O表现出一些奇怪的行为(比如经典的printf + fork)

2. 对多字节字符集的兼容一直是C/C++最头疼的地方，虽然libc提供了对宽字符集的支持，但是针对Unicode这样的字符集又显得不怎么好用。（所以现代语言基本上都把Unicode作为统一的string最小单位）

3. 当一次标准I/O会阻塞等待用户输入时（比如scanf stdin），libc保证将所有的行缓冲刷出。

    这样设计主要是为了一个常见场景：程序输出一个提示信息，然后要求用户进行信息的输入，这时如果不主动触发行缓冲的刷新，则用户就看不到相应的提示信息。

    ```c
    printf("Enter your name: "); // 没有换行，内容进入缓冲
    scanf("%s", str); // 在这之前，刷新stdin
    ```

4. 针对标准IO流属性的修改，必须在执行实际的IO之前进行，否则之后的所有修改请求都会被忽略

5. ISO C定义的fopen函数标志位的`b`选项在UNIX下无意义，因为UNIX在打开一个文件后不区分是否是字符文件还是二进制文件。该选项主要是为windows设计，当`b`标志位没有被设置时，处于文本模式，输入时`\r\n`会自动转化成`\n`，输出时`\n`会自动转化为`\r\n`，同时也会处理EOF符。设置了`b`标志位后，行为和UNIX一致，此时不会完成`\r\n`和`\n`的自动转化

6. `getc`在libc中可以以宏的形式存在，直接修改FILE结构体完成读取，效率更高。但这样带来了一些奇怪的现象，因此libc还提供对应的`fgetc`保证提供函数版的调用。

7. `clearerr`是一个很有意思的函数，它能够清除一个文件流的所有错误状态，使流重新变为可用。（甚至能恢复读取到EOF的stdin流）

8. 当stdin和stdout定向到一个终端设备时，默认情况下是行缓冲。当定向为一个普通文件时，默认情况为全缓冲。

    下面这个示例代码，直接执行`./a.out`和定向到文件`./a.out > file && cat file`得到的输出是不一样的

    ```c
    int main () {
        printf("AAA\n");
        if (fork() == 0) {
            printf("BBB\n");
        }
        return 0;
    }
    ```

9. 创建临时文件是有专门的libc函数`tmpnam`和`tmpfile`的，前者返回一个合法的不重复的临时文件名，后者直接创建一个临时文件并且在程序正常退出时自动删除临时文件。

    `tmpfile`的自动删除功能用到了一个有意思的小技巧：

    使用`tmpnam`获取一个独特的临时文件名，然后在`/tmp`下创建临时文件，再调用unlink将该文件从`/tmp`中删除。因为进程仍然拥有文件的引用，所以对于进程而言，在退出之前还可以正常使用该临时文件。

    SUS标准还定义了`mkdtemp`和`mkstemp`来创建临时目录和模版命名的文件

# System Data Files and Information

1. `/etc/passwd`存储着用户的部分数据，包括uid、gid、登录shell等信息

    一条记录的格式如下

    ```
    NaroZeol:x:1000:1000:,,,:/home/NaroZeol:/usr/bin/zsh
    ```

    这并不是完整的用户信息，详细的信息需要通过`getpwuid`或者`getpwnam`来获取。

    其中第二项的x，过去存放了加密后的用户密码，以便各种程序进行比对，但出于安全问题，现在`/etc/passwd`中不再显式提供加密后的密码。加密后的值被存储在shadow中，只能通过系统调用获取。

    一些软件设置的用户（比如mysql）或者发行版管理用的用户，会设置登录shell为`/bin/false`或`/usr/sbin/nologin`来禁止这些用户的登录，以此防止恶意攻击者对这些用户发起攻击性登录。

2. `getpwuid`和`getpwnam`都返回一个`struct passwd *`类型的指针，指向函数内部的静态变量空间。这个技巧在libc中很常见。

    总体上libc提供两种风格的调用方式：

    1. 代表的函数是和时间相关的API。提供一个空的结构体，传入其地址给函数，随后函数填充这个结构体

    2. 代表函数就是比如`getpwuid`和`getpwnam`。不需要提供任何额外空间，函数返回内部的静态变量的地址

    两种方式各有千秋，前者虽然调用起来麻烦，但是相比于后者可能存在的竞争条件和缺失的幂等性，还是可以接受的代价。

3. 为了降低用户的密码被碰撞获取的可能，前面说过，用户加密后的密码被存放在名为shadow的“文件”中，只能通过系统调用（`getspnam`）获取。

    当以非root身份调用`getspnam`时，返回的结构体中加密后的密码总是被置为null，也就是非root用户是无法得知其他用户的加密后的密码字符的，这样就无法通过碰撞的方式获取。

    常规应用中只有`login`，`passwd`，`sudo`这些有访问shadow的需求，它们都是set-user-ID的二进制文件，在执行时自动获取root权限来获取shadow的值。同时内置了延迟机制（每次失败都会sleep一两秒）来增加暴力破解的难度。

4. 
