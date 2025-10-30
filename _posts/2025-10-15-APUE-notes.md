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

拜此所赐，这本书的读书进度一直非常缓慢，中途也因为做项目和实习也耽搁了很久。最近结束了一段实习，有了一段相对空闲的时间，就把这本书读了一些。

大概读了三四百页，但总感觉自己最近记忆力下降很明显，看过的东西一下就忘了。虽然有在纸质书上做一些笔记，但是看完之后基本上应该不会再翻书了，所以还是打算做一个精炼版的笔记记录一下。

所以接下来的内容大部分都是将书上的文字版笔记搬上来，应该是只有我能看懂的记录方式。。。

大致就按照大章节的方式来排列

# System Calls and Library Functions

1. 当我们谈及计时时，总是有三种时间概念

    - Clock time

    有时也被称为"wall clock time"，即你墙上的挂钟的时间，所以Clock Time代表着现实世界的物理时间

    - User CPU time

    在进程被调度后，在用户空间执行用户代码的实际运行时间

    - System CPU time

    在内核空间执行内核代码的实际运行时间


2. 虽然对于一般用户来说，区分库函数和syscall是没有太大必要的。但是从这些功能的实现者的角度来看，区分两者是有必要的。

# UNIX Standardization and Implementations

这章主要讲各种UNIX标准和实现，不过从我的角度来看，我只关心Linux，这些标准和其他实现就当个故事听听

1. 总之本书提及的标准有ISO C，POSIX，SUS，FIPS，从我的理解上看，ISO C定义了C语言的工作方式，POSIX定义了C语言与内核交互的方式。SUS（Single UNIX Specification）是POSIX的超集。只有实现了SUS的发行版可以被称为UNIX系统。SUS是一个来自于工程实践的标准，最初开始没有标准时，所有的工程师需要手动使用syscall和自己实现各种工具函数，SUS总结了这些实践，形成了一个超大型的标准。

    至于FIPS(Federal Information Processing Standard)是由美国联邦政府发布的一个标准，不过已经被废弃了。

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

    真神不必多解释，由教学系统MINIX发展而来，一位自大狂妄的少年发起的伟大开源项目。

    "A grass-roots effort then sparang up, whereby many developers across the world volunteered their time to use and enhance it"

    - Mac OS X

    源自于FreeBSD，核心名为"Darwin"

    - Solaris

    Sun（现被Oracle收购）的商业发行版，专业的工程师维护的发行版，不过现在随着Sun的落寞，也逐渐失去了过去的地位

    Stanford University Network（SUN），核心理念为"The Network is the Computer"，一个领先时代的巨人，可惜缺乏商业化手段在竞争中落败。

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

    `pwrite(xxx..., off_t offset)`和 `pread(xxx..., off_t offset)`，这两个函数更像定点的读写，它们都不会导致文件的offset被修改。

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
    strace ls -o /dev/fd/1 | vim - # vim 会将 `-` 处理为/dev/stdin，注意这是大部分gnu工具链的默认行为，而不是shell的转义
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

4. 大部分系统数据文件都设置了类似于目录的API进行访问（其实目录本身就可以视为一种系统数据文件）

    1. 一个get函数获取一项数据，然后将游标移动移动到下一项

    2. 一个set函数将游标移动到最开始一项

    3. 一个end函数关闭该文件

5. 与获取时间相关的几个API

    `time`是最开始的一个，但是存在溢出风险同时精度不高。

    因此后继者为`gettimeofday`，但该API相对于现代时钟来说精度还是不足（现代时钟支持纳秒级别，而`gettimeofday`只支持微妙级），同时它只能使用系统实时时钟，这是一个可以被调整的值，系统管理员的手动调整、时钟校准等操作都可能导致时钟前后记录的时间数值上不单调，不适合计时任务。

    而`clock_gettime`提供了强大的功能，能够支持纳秒级别，同时可以选择时钟源，如`CLOCK_MONOTONIC`提供一个逻辑时钟，单次获取的值不能和现实的时间对应（即起点没有定义），但是保证两次调用获取的值一定能够反映两次调用之间的精准时间差，适用于计时任务。而`CLOCK_REALTIME`语义保持和`gettimeofday`一致，用来获取当前的时间。

# Process Environment

1. 使用`atexit`注册的退出函数，执行的顺序和注册顺序相反，遵循栈的模式。

2. 一个C程序，在执行时：
    
    1. 内核将命令行参数、环境变量等安排到合适的内存空间中

    2. 执行libc的C runtime的_start（由链接器在链接时引入）

    3. 初始化各种libc需要的设置（如初始化stdio维护的stdin，stdout，stderr FILE结构体）

    4. 执行用户定义的main函数，传递`argc`,`argv`,`envp`参数

    5. 正常退出后调用libc的清理函数和用户程序注册的atexit函数

3. 在一个程序的内存布局中，存在一段未初始化数据段（bss），之前一直不知道为什么要区分初始化和未初始化的数据段

    bss设计上是为了减小可执行文件的体积。

    比如有一个程序设置了一个巨大的全局数组`int nums[1000000]`，因为这个全局数组实际上没有任何数据，所以无需在可执行文件中完整表示，等到执行程序时，自动全部初始化为0。

    因为已初始化的全局数据可以和text一起读入，而bss要后面再分配，所以在概念上就进行了一个分隔，实际上bss和初始化的数据还是连在一起的。

4. Linux采用over-commit的内存管理策略，当一个进程申请了一大片的内存空间，但是还没有实际发生写入时，这些虚拟空间不会对应到真实的物理空间。只有在写入时，触发缺页中断进行修复之后才会占用实际的物理空间。

5. 绝大多数的malloc实现不会主动归还使用sbrk获取的堆空间，因为sbrk只能归还break指针附近的空间，如果有用户应用恰好占用了这些空间，归还是基本不可能的。

    但是这其实不会造成太多问题，malloc在实现上只有在分配小块时才会使用sbrk分配的堆空间构成的内存池。对于大块，malloc通常选择使用mmap来分配，所以只要合理使用malloc，不要过度分配小块，break指针的增长其实并不多。

6. 内存管理是不容易的，经常会造成问题。Linux允许使用打桩技术在程序运行前设置需要链接的实际对象，因此可以使用该技术在测试环境将malloc库替换为带有安全检查的特定实现。

7. 环境变量的内存结构和命令行参数类似

    ```
                  -->*penv0 --> "http_proxy=xxx"
                /     |
    *environ --      *penv1 --> "SHELL=/bin/bash"
                      |
                     *penv2 --> "TERM=xxx"
                      |
                     NULL
                     ...
    ```
    其中penv是一个char*指针数组，以NULL结尾，初始化时由内核放置栈顶

    可以使用`putenv(char *str)`和`setenv(const char *name, const char *value, int rewrite)`来修改环境变量，两者的区别在于，`putenv`在大多数实现中直接将参数中的指针作为指针数组penv项的值，这意味这一旦给予给`putenv`的指针指向一个会被释放的空间，可能导致读取环境变量时出现空解引用的问题。而`setenv`会根据参数的内容构造一个环境变量字符串，不使用参数的字符串空间。

    不同于命令行参数，环境变量有修改需求，所以会有很多特殊的地方。

    1. 如果我们在修改一个已存在的环境变量
        
        1. 如果新的value的长度小于等于原有value的长度，这时可以复用原有值的空间，不需要额外分配空间

        2. 如果新的value的长度大于原有value的长度，这时需要分配一块新的空间用于存储（或者putenv直接使用传递进来的指针）

    2. 如果我们在添加一个新的环境变量

        1. 当我们第一次添加一个新的环境变量时，因为最初时指针数组penv是位于内存空间中栈空间的上面的固定空间，无法进行扩展，所以我们需要malloc一个新的空间用来存放新产生的penv数组，将penv数组完整拷贝一份，添加上新值，然后改动*environ的指向到新空间。

        2. 当我们再次进行环境变量的添加时，我们能发现这不是我们第一次扩展环境变量，因此只须简单调用realloc来扩展penv数组。

8. `setjump`和`longjump`是一组很有意思的API，完美诠释了操作系统的本质--状态机。但是要注意使用它们意味着跳过了中间的资源释放步骤，有很大的风险。

    而且如果没有对变量类型的精准认识，也会导致这组API产生未知的行为。因为`setjump`的行为取决于实现，不一定会保存寄存器的值，因此如果重新使用一个被编译成寄存器的变量，有可能会产生未知的结果，所以对于`longjump`后还希望再使用的变量，尽量使用`vilatile`标记。

# Process Control

1. 进程的PID在进程结束后会被新的进程重用，现代UNIX实现会尽可能不使用刚结束的进程的PID，所以大部分情况下不用在意重用问题。

2. 0号进程通常是名为swapper的调度器进程。它作为内核的一部分存在，没有在文件系统中与之对应的二进制文件存在。

3. 1号进程在传统上通常是`/bin/init`，根据配置文件`/etc/inittab`、`/etc/rc*`等文件完成诸如设备挂载、执行自启任务等初始化任务。1号进程永远不能挂，它在完成初始化任务后还会执行一系列定时任务（如回收孤儿进程）

4. 不过现代Linux发行版都转向了systemd。systemd提供更强大的初始化配置能力和服务管理能力，更适合生产环境和桌面环境的实际管理需求。system V的init + inittab模式在轻量环境中还是很受欢迎（比如一些轻量的docker容器还是使用init）

5. 每个UNIX系统实现有一些自己系统后台进程，比如Linux中会有管理RCU的进程。这些进程可以使用`ps aux`查看，其中COMMAND行的内容为`[xxx]`的以中括号包裹的一般就是kernel进程。

6. 虽然现代UNIX系统都会用COW来优化fork，但是遍历页表将所有内存标记为COW也是一个十分耗时的步骤。一个更好的选择是使用`vfork`，`vfork`允许父子进程共用内存空间，父进程会挂起直到子进程调用了exec，子进程应该尽快调用exec，在这之前子进程修改内存空间的后果是未定义的（Linux下就是单纯的共享空间）

7. UNIX从诞生之初就确定了fork + exec的创建新进程的方式。但是SUS标准还另外定义了`spawn`的单一调用来创建一个新进程，这个设置主要是为了一些实时操作系统或者那些非常不方便实现fork的系统（比如没有采用虚拟内存方案的嵌入式系统），大多数的实现会提供这样的接口，但在底层还是fork + exec的方式加上一些优化（如使用vfork代替fork）

8. exit保证清理stdio，但是不会清理其他打开的文件流。（清理只是将还在缓冲区的数据进行fflush，无论是否清理，最后内核都会执行相同的代码，将打开的文件关闭）

9. 区分**exit status**和**termination status**的概念，前者是传递给`exit`族函数的参数或者main函数的返回值，后者是由kernel生成的进程具体的结束原因。也就是后者是前者的超集，当子进程是通过exit正常退出时，后者能够包含前者（如果是因为signal导致的终结，显然不会有exit status的存在）

10. 在32位系统上，**exit status**是一个8位的状态码，**termination status**是一个使用特殊定义分割的32位整形（**当子进程正常使用exit退出时**，其中有8位记录了**exit status**，使用宏`WEXITSTATUS`便可获取），即`wait(int *statloc)`的参数。可以使用一系列宏`WIFEXITED`、`WIFSIGNALED`等来得知子进程退出的详细原因。

11. 当子进程的父进程提前结束，内核自动将一号进程init作为其父进程，这保证了每个用户进程一定会有父进程存在。init周期性地调用`wait`系列函数，完成孤儿僵尸进程的清理。

    在使用systemd的系统上，systemd会将这个任务委托给systemd负责回收任务的进程`systemd --user`，这个进程并不是一号进程，这样做的目的是实现systemd对daemon进程的监控能力

    这个功能依赖于自Linux 3.4引入的[子进程收割者机制](https://unix.stackexchange.com/questions/250153/what-is-a-subreaper-process)

    > Generated by Google Gemini
    >
    > Linux 3.4+ 的改变： Linux 内核引入了 PR_SET_CHILD_SUBREAPER 机制（通过 prctl() 系统调用）。任何进程都可以将自己标记为 Subreaper。
    >
    > Subreaper 的作用： 当一个进程变成孤儿进程时，内核会向上追溯其祖先，直到找到最近的那个被标记为 Subreaper 的进程，然后将孤儿进程的 PPID 设置为这个 Subreaper 的 PID，而不是默认的 PID 1。

12. UNIX下创建一个守护进程的常用方式是`double fork`，通过两次fork调用，创建出孙进程，随后再将子进程退出。此时孙进程就会成为孤儿进程，被挂到init或者后台任务管理进程上，成为一个守护进程

13. 总是使用`waitpid`代替`wait`，waitpid提供了丰富的功能，实际超出了wait和pid的范畴。首先它支持无阻塞的等待，允许父进程无阻塞地调用自己，同时可以用pid的不同值范围，如-1指代所有子进程，pid>0指代特定pid，pid==0指代同组子进程，pid<-1指代特定组子进程，然后还支持STOP和CONTINUE这些和作业管理有关的概念。

14. 除了`wait`和`waitpid`，SUS标准还提供了一个设计更符合直觉的`waitpid`-->`waitid`，以更人性化的接口提供`waitpid`的功能。绝大多数的实现还会提供从BSD继承来的`wait3`和`wait4`，提供子进程的运行报告，包括CPU使用时间，signal接收情况，缺页数量等和性能相关的报告。

15. exec 系列函数有很多变种，代表不同的参数传递方式

    | Function | pathname | filename | fd | arg list | argv[] | environ | envp[] |
    |---------:|:--------:|:--------:|:--:|:--------:|:------:|:-------:|:------:|
    | execl    |    ✓     |          |    |    ✓     |        |    ✓    |        |
    | execlp   |          |     ✓    |    |    ✓     |        |    ✓    |        |
    | execle   |    ✓     |          |    |    ✓     |        |         |   ✓    |
    | execv    |    ✓     |          |    |          |   ✓    |    ✓    |        |
    | execvp   |          |     ✓    |    |          |   ✓    |    ✓    |        |
    | execve   |    ✓     |          |    |          |   ✓    |         |   ✓    |
    | fexecve  |          |          | ✓  |          |   ✓    |         |   ✓    |
    | (letter) |          |     p    | f  |    l     |   v    |         |   e    |

    其中个人最常使用的是execve
    
    值得一提的是`fexecve`和`openat`一样，因为参数对象是一个锁定的fd，所以可以缓解TOCTTOU问题。

16. 出于安全原因，永远不要把当前目录`.`，加入到PATH的项中

17. 内核支持无限长的命令行参数列表，但是shell往往不允许这么做，一个权宜之计是使用xargs来支持过长的命令行参数列表。

18. 默认情况下打开的fd会被exec继承，除非在打开该文件时指定了`FD_CLOEXEC`。

    注意目录对象的fd总是被设置了`FD_CLOEXEC`，这是由`opendir`做的。

19. argv[0]通常是由shell填入的，通常是可执行文件的名称。

    一些程序会利用argv[0]来执行一些特殊的逻辑

    比如busybox会根据argv[0]来选择要执行的applet，这允许用户直接将busybox链接为一个常用可执行程序，替代它的功能。比如`ln -s /bin/busybox /bin/ls`，此时执行ls，实际执行的是busyboxy，shell传入`/bin/ls`的名字，busybox根据这个名字调用ls的applet，非常好玩的技巧。

    再比如login在执行用户设置的登陆shell时，argv[0]总是shell名前加上一个`-`，如`-zsh`，这告诉shell这次是在进行登陆，这时shell就会执行存储在`~/.zprofile`或者`~/.bash_profile`内希望只在登陆时执行一次的脚本。而tmux因为本质是在模拟终端，因此创建出来的shell进程也总是名为`-zsh`等等。

20. UNIX模型下，一个进程的用户id被分为三种（real user ID，effective user ID，saved set-user ID）

    一次`setuid`调用根据用户具有的权限和设置的目标ID，展现出不同的行为

    1. 进程拥有最高权限，直接将三种ID都设置为目标ID

    2. 进程不拥有特权，只有在目标ID等于real ID或者saved set-user ID时，将ID设置为目标ID

    对于ID的模型定义，总体上满足如下限制

    1. real user ID只能由特权进程设置，正常情况下，该ID是由`login`进行设置

    2. effective ID是kernel鉴权的实际ID。该ID可以**在任意时刻通过`setuid`调用变为real user ID或者saved set-user ID中的一个**。当exec执行了一个set-user-id的可执行文件，该ID会变为对应的ID（甚至能是root），否则就保持不变。

    3. saved set-user ID也是由exec设置，在执行完exec，在进入新程序代码之前，该ID和effective ID相同。

    前两者的设置不奇怪，奇怪的是第三个saved set-user ID。

    设计这个ID的初衷是为了让应用程序使用最小权限的同时，又有能力恢复到高权限。

    如何理解？一个可执行文件可以设置为set-user-id，让任何用户在执行时都拥有所有者的权限。这个设计的哲学是让所有者自己为set-user-id可执行文件的逻辑负责，比如`passwd`能够修改用户的密码，其被设置为set-user-id到root，kernel要求root用户管理好`passwd`的逻辑，当一个非特权用户使用了`passwd`，需要root用户在`passwd`这个二进制文件中管理好鉴权等行为，无论非特权用户如何折腾，都保证`passwd`只做修改当前用户密码的事。

    为此，`passwd`的设计必须遵循最小权限原则，大部分时间运行在低权限下，只在需要进行特权操作时，以特权用户的身份操作。

    因此存在

    ```
    以passwd为例，简化其逻辑

    程序执行 -> 处理命令行参数 -> 等待用户输入密码 -> 打开特权文件shadow校验密码 -> 等待用户输入新密码 -> 更改密码
    （特权）    （非特权）        （非特权）           （特权）                     （非特权）            （特权） 
    ```

    这样**不断地从特权身份降级到非特权身份，同时在需要时从非特权身份升格到特权身份**的需求，所以**内核需要记住“这个进程在启动时其实是以特权身份启动的，但是中途出于最小权限原则主动放弃了特权，现在它想要重新获取特权”**这样的事。

    by ChatGPT 5（哎，感觉再怎么总结都比不过AI总结的好，道心破碎）

    > 在早期 UNIX 中，如果一个程序一旦放弃了 root 权限，那它就再也无法恢复，除非重新 exec 程序。
    > 
    > 这造成了安全与便利的两难：
    >
    > 1. 如果一直保持 root 权限 → 危险。
    >
    > 2. 如果放弃 root 权限 → 无法再执行特权操作。
    >
    > 所以在 System V 和 BSD 的后续实现中，引入了 “saved set-user-ID” 机制。
    它让进程可以：
    >
    > 1. 降权运行大部分逻辑；
    >
    > 2. 仅在必要时通过 saved UID 重新获得临时特权；
    >
    > 3. 且整个机制是内核管理的，不可被非特权进程伪造。

21. 一个进程的nice值越高，受到调度的优先级越低。越nice的进程对其他进程来说就越nice

# Process Relationships

1. tty的全称是Teletypewriter，即电传打字机。可以看一些介绍的视频，还是很有意思的。可以说这就是计算机世界的马屁股大小决定铁轨宽度的故事，tty决定了终端的以行输入输出为主的工作方式。

2. 在常规的UNIX系统（使用init作为一号进程的系统）登陆时发生的流程大致如下：

    1. 系统管理员预先在系统中设置好`/etc/ttys`文件（在systemV系统中，是inittab)，里面记载了哪些终端设备类型以及可以作为登陆终端的设备

    2. init查询`/etc/ttys`文件，为每个终端设备名为getty的进程，执行终端的初始化（如设置波特率等基础设置），打开一个终端文件，将0，1，2号fd绑定到该文件上，即将标准IO绑定到终端。最终getty打印出Login，等待终端的输入（是的，登陆时的Login和Password实际上是由不同进程打印）

    3. 当终端输入了用户名，getty执行exec，转变为`/bin/login`。login打印出`Password`，关闭终端的回显，检查输入的密码是否和命令行参数中的用户名对应的shadow密码一致。

    4. 检查完毕，login进行home目录的设置、终端权限的设置（分配给登陆的user）、初始化环境变量等操作，最终执行exec转变为用户的登陆shell

    5. 登陆shell因为被设置了特殊的argv[0]，所以执行profile的登陆脚本。接着执行自定义脚本。

    实际上登陆流程远比上面描述的复杂。像有图形界面的发行版，login的部分可能会被替换为图形界面的登陆。

    而像使用了systemd作为一号进程的实现，getty的任务被systemd的内置模块替代了，所以在现代以systemd为基础的实现中是找不到`/etc/ttys`或者`/etc/inittab`这些的。

3. 为了支持图形化界面的虚拟终端或者通过网络登陆使用的终端这些不与现实物理终端对应的场景，UNIX引入了伪终端的概念，基本思想就是添加兼容层，将物理终端，网络终端，虚拟终端通过不同逻辑进行兼容。`pseudo terminal`即pts，也就是`/dev/pts/`目录下的设备。

4. 一个进程组中，pid==gid的进程被认为是该组的leader。一个进程组可以没有leader，只要该组中还有一个进程存在，这个进程组就是有效的。

5. `setpgid`调用可以修改一个进程自己或者子进程的组ID。对子进程组ID的修改只能在调用exec之前完成（看起来很奇怪的限制）

6. session是由一系列绑定到相同控制终端的进程组组成的集合。一个进程可以通过调用`pid_t setsid(void)`来创建一个session，**调用的进程不能是一个组的leader**，随后会发生以下三件事

    1. 这个进程会成为session的leader

    2. 这个进程会从原来的组中分离出来，将进程组ID设置为进程PID，即自己作为本组的leader

    3. 这个进程暂时没有控制终端

7. session和进程组的一些特性如下：

    - 一个session只能拥有一个控制终端，通常就是用户的登陆终端

    - 创建了控制终端连接的session leader也被成为**控制进程（controlling process）**

    - session中的进程组可以分为一个前台进程组和一个或多个后台进程组

    - 当终端接收到了控制字符（比如DELETE，Control-C，Control-\），对应的控制signal**只会**被传递给前台进程

    - 当终端断连（比如ssh断开连接），hang-up signal（SIGHUP）只会被发送给session leader

8. 上述规则中最重要的就是最后两条，后台进程不会受到controlling terminal的控制

9. 一个进程访问`/dev/tty`时，永远被导向本进程所属session对应的控制终端。（当一不小心关闭了stdio时，可以通过该方式恢复）

10. 当一个后台进程尝试向控制终端发起读请求，终端驱动会检测到这个行为，向该进程发送SIGTTIN，该signal默认情况下**暂停（STOPPED）**该进程。

    后台进程是否允许向控制终端发起写请求取决于用户的设置，可以通过`stty`这个进行设置，可以选择允许或者和读请求一致处理（此时发送SIGTTOU）

11. 现代的shell默认会将一条命令中的进程都打包成一个进程组执行，而旧shell的实现不会这么做。（不过现在基本上获取不到这样的shell了）

12. 所谓的孤儿进程组的定义

    > An orphaned process group is one in which the parent of every menber is either itself a member of the group or is not a member of the group's session.

    反过来解释，一个进程组不是孤儿进程组就是

    > The group is not orphaned as long as a process in the group has a parnet in a different process group but in the same session.

    当一个进程组不是孤儿的，假设该进程组内所有进程都被暂停，因为存在一个不属于该进程组但是属于同一个会话的某个父进程，所以总是存在将所有进程恢复的**可能性**。（属于同会话意味着这个父进程不是被分配的，是关心子进程的实现）

    比如说使用shell执行的一个程序，因为该程序总是将shell作为其父进程，同时两者又不属于同组但属于同一个会话，就算该程序所属的组内的所有进程都被暂停，因为其存在一个外部的shell控制源，所以存在被恢复的可能性，故不认为这个程序所属的组是孤儿进程组

    反之，如果此时shell被kill，程序的父进程自动继承到init上。一个不属于本会话的外部程序，没有义务恢复被暂停的进程，所以如果不采用某些机制，就会导致该程序组内所有的进程暂停，导致资源的泄漏

    为了解决这一问题，POSIX标准要求在进程组转变为孤儿进程组的瞬间，发送两个signal（SIGHUP和SIGCONT）。

    发送SIGHUP是通知该孤儿进程组的进程，当前进程组已经失去了控制源，内核希望结束该进程组，一个进程可以注册SIGHUP handler来进行一些处理，保存重要的数据或者日志。

    而SIGCONT是为了将处于STOPPED状态的进程唤醒，使其能够处理SIGHUP的逻辑。

    反映到实际，就是一个ssh连接的shell退出，该shell创建的所有进程组都失去了（除了nohup的进程组）**不属于该进程组但是属于同一个会话**的父进程，所以就转变为了孤儿进程组，进而受到SIGHUP信号。

# Signals

1. Signal是一个纯软件级别的中断，没有硬件支持，这意味着signal不是瞬时响应的。一般来说一个进程处理signal的时机是从内核态返回用户态之前。

2. 虽然早期的UNIX就引入了signal，但是它并不可靠，同时调用方式麻烦（需要重复注册signal）。后来BSD又独立实现了一套与旧UNIX不同的signal方案，其在语义上和原有的UNIX相差很多。POSIX选择尊重两者的分裂，不主动定义`signal`调用的行为，提供一个`sigaction`进行详细的设置，并要求后续的实现需要能够通过`sigaction`实现多种语义。

3. signal产生的方式无外乎以下几种：

    1. 终端在收到特定的输入时，由终端驱动发送相应的signal到该控制终端对应会话的前台进程组中

    2. 硬件异常，比如除以0、内存访问越界。kernel将会收到对应的错误，同时产生相应的中断。常见的这类中断就是SIGSEGV

    3. 由kill系统调用发送的中断

    4. 特定事件也会产生signal，比如向一个没有读者的pipe写入数据会触发SIGPIPE，因定时任务而触发的SIGALARM等

4. signal的触发对于一个进程来说可以在任何时刻发生。进程可以选择忽略signal、捕获并采用默认行为、捕获并采用自定义行为中的一个。其中SIGKILL和SIGSTOP是不可忽略的也不可以捕获的

5. 大部分的signal的默认行为是终止进程，有一些signal在终止进程的同时，会生成一份coredump文件，其中记载了进程在被这个signal终止前一瞬间的状态，可以使用gdb+进程二进制+coredump文件重现触发signal的一瞬间。（状态机yes）

6. coredump文件的产生要求在生成的一瞬间，进程对应的可执行文件的所有权归real user所有，同时real user在当前工作目录下有写下coredunmp文件的权限

7. 在signal列表中，有很多signal是由SUN的Solaris引入的，比如SIGJVM1、SIGJVM2、SIGLOST等，足以见得当时的SUN是多么的强大。

8. 和电量相关的通知实现使用SIGPWR，该signal用于通知低电量行为，随后根据管理员的配置决定一个进程要终止还是降低功率的。

9. Linux对于标准中signal默认行为定义为"or"的signal，总是使用终止的语义。它的设计原则就是如果一个signal是发送给特定程序的，那么非该专用用途的程序就不该收到该信号。

10. 命令行的可执行文件的kill默认发送的实际上是SIGTERM而不是SIGKILL，不过大家一般都是用`kill -9`

11. 前面说过，一个后台进程组如果希望向控制终端写入或读出，写入操作根据用户的设置可以正常进行或者受到一个SIGTTIN，读取操作总是受到一个SIGTTOU。默认情况下这两个信号会导致后台进程进入STOP状态，当进程组恢复到前台后能继续读写。但如果收到这两个信号的进程属于一个孤儿进程组，则读写操作总是会返回错误。

12. POSIX定义的信号预留给用户进程自定义使用的只有SIGUSR1和SIGUSR2，但也有一些扩展的做法，比如一些守护进程收到SIGHUP时会执行一些行为（比如重新加载配置文件），因为守护进程没有控制终端，所以理论上它们不会收到来自终端的SIGHUP，所以它们放心地使用这个SIGHUP来是实现一些自定义功能。

13. 当终端的窗口大小发生变化时，对应的前台进程组会收到一个SIGWINCH，被提醒需要做出一些调整来适应新的窗口大小。

14. 出于可移植的目的，永远不要使用signal来注册一个signal handler。前面说过很多次，POSIX只要求所有的实现要提供统一的signal调用，但是没有要求signal调用要求相同的语义。所以总是使用sigaction来注册一个signal handler

15. 在调用exec后，原有设置的signal handler会失效（当然会失效，地址都变得无效了），但是被设置为忽略的signal会被继承，被exec的进程后续依旧会忽略原先被设置为忽略的signal

16. 旧UNIX signal设计失败的主要原因是：

    - 存在重新设置signal handler的空窗期

    旧UNIX signal**一次注册handler只会生效一次**，类似于alarm的机制，需要在handler内再次注册一次signal。这导致存在signal handler空缺的窗口，如果这个时候又触发了一次signal，则会执行该signal的默认行为。

    ```c
    // 注册为SIGINT的handler一次
    sig_int() {
        // ...
        // 在进入该handler后，SIGINT的handler被清除
        // 此时又出发了一次SIGINT，因为handler为空，则触发默认行为：终止进程
        // ...
        signal(SIGINT, sig_int);
        // ...
    }

    ```

    现代UNIX在进入signal handler后会主动阻塞该signal，只有在返回后才解除，期间的阻塞会被记录，在解除阻塞时再次触发signal

    - 无法主动阻塞一个signal并记录阻塞的发生

    旧UNIX接口没有提供阻塞一个signal的方式，这导致用户进程没有办法在不希望接收到该signal时进行阻断。

    ```c
    int main () {
        signal(SIGINT, sig_int);
        // ...
        while (sig_int_flag == 0) {
            pause();
        }
        // ...
    }

    void *sig_int() {
        signal(SIGINT, sig_int);
        sig_int_flag = 1;
    }
    ```

    上面这个程序的本意是希望在进入while循环后，在pause状态停止，当收到一个signal时，进程会从pause返回，然后再检查flag是否被置1，如果置1就跳出循环。问题在于如果在检查完条件之后，进入pause之前，进程的控制者就已经发送了SIGINT，此时会处理这个signal，但如果控制着只想发送一次signal，在这之后就没有新的signal了，那么这个进程将一直在pause中阻塞。

    现代UNIX提供了阻塞一个signal的方式，阻塞signal意味着进程暂时不处理signal handler，但是依旧接收signal（记录signal在阻塞期间曾经到来过，但是不计数），随后在解除阻塞后，系统能够知道阻塞期间接收到的但是还没有处理的signal，随后执行相应的handler，这样就会更灵活的多。

17. 真正导致systemV和BSD的signal机制分歧的特性是signal对于阻塞性系统调用的影响

    一些syscall会让进程进入阻塞状态，比如read，write等，它们被称为"slow system call"。

    分歧点就在于，当程序正处于阻塞性syscall的阻塞状态中，这时触发signal并完成处理之后，系统是恢复原有的系统调用还是让这次系统调用失败？

    从现在的程序员的角度，我们肯定希望syscall能够自动恢复，不然当调用一个`read`的时候，还要写一大堆处理来防止`read`被打断。

    systemV选择了不恢复阻塞性syscall，总是让被打断的syscall返回错误，同时设置errno为EINTR。站在当时的角度来看问题，systemV选择了严谨的语义，将syscall发生的异常如实上报。“程序应当显式地知道自己被打断，而不是内核“悄悄”替它恢复”。

    从实现的角度考虑，选择不恢复syscall的内核实现也会更加简单，如果需要恢复syscall，则内核需要保存打断一瞬间的进程状态，而在signal处理程序中，也可能调用syscall，引入非常多的复杂性。

    而BSD站在了用户的一方，自动恢复阻塞性syscall使得应用程序不需要在每次调用时都写上一大堆的if else。当然这样的代价就是内核实现会变得非常复杂。

    POSIX选择了尊重两者，将signal的语义设置为未定义，通过sigaction的SA_RESTART选项来设置是否要恢复阻塞性syscall

18. 大多数信号不安全的函数都是因为使用了全局唯一数据（静态变量，全局变量等），比如exit使用了类似链表的全局结构来保存atexit注册的函数，所以它不是信号安全的。

19. 
