---
date: 2024-08-11
title: "MIT-6.5840-lab-mapreudece"
category: 学习经验
tags: [MIT-6.5840,分布式系统]
---

# MapReduce

## 论文解读

MIT-6.5840(原6.824)这门课程以多篇论文为核心，所以想要做好lab要扎实地读完原论文

### MapReduce简介

MapReduce是Google于2004年提出的利用分布式系统解决处理大规模数据的方案

Google意识到随着磁盘逐渐变得廉价，使用一个大型系统来索引整个互联网是可能的，因此一个用于实行大规模分布式计算的系统被设计出来，作为Google的“三架马车”之一开启了大数据时代。其历史意义非凡

MapReduce旨在利用网络通信的方式来利用廉价的消费级主机集群来处理一台单机无法处理的任务，比如一个涉及到TB级的数据的运算

### MapReduce模型

MapReduce是一种编程模型。来自于Jeff Dean和他的同事们总结的编写的分布式计算任务经验，他们注意到大部分的任务都可以简单地划分为Map阶段和Reduce阶段，因此他们提出将分布式计算任务抽象成设计Map函数以及Reduce函数。这两个概念来自于函数式编程，表示映射和归约。

Map函数，接受键值对为输入，然后输出中间产物（也是键值对）。然后MapReduce框架会将中间产物中的具有相同键值I的键值对发送给对应的Reduce函数

Reduce函数，接受由键值I和列表组成的键值对，列表包含map阶段所有的键值相同且为I的中间产物。随后Reduce函数根据这个列表产生最终的输出，也是一个键值对

当然，以上的都是逻辑上应该会这么运作，但实际实现时只要保证这个逻辑性即可。

#### 例子

原始论文中给出了很多有用的例子：

- Word Count

    MapReduce最经典的示例。Map函数接受<文件名, 文件内容>的键值对，然后按词遍历文件内容，产生<word, “1”>这样的中间产物。Reduce函数接受中间产物中所有键值相同的<word, [ ]values>，遍历values统计得到word的个数

- Distributed Grep

    分布式的Grep。Map函数接受<文件名，文件内容>（不一定是这样），然后根据文件中的每一行是否匹配，如果匹配则产生<yes, 行内容>的中间产物，不匹配则产生<no, 行内容>的中间产物。Reduce函数只需要简单的收集键值为yes的中间产物即可。


- Count of URL Access Frequency
    
    原理和word count一样，Map函数产生中间产物<URL, "1">，Reduce函数根据中间产物计数。

- Reverse Web-Link Graph

    建立反向链接图谱，这个应用主要就是搜索引擎分析一个链接受欢迎的程度。这项任务通过扫描互联网上所有的Web页面(称为source)，解析其中包含的URL(称为target)，建立<target, list(source)>的索引。原理上还是和word count一样，只不过中间产物变成了<target, source>，Reduce函数仍然是收集键值相同的，产生<targetm list(source)>

- Distributed Sort

    其他的例子都是大同小异的，但这个比较特殊。Map函数接受一个键值对，然后原封不动地输出这个键值对，Reduce函数也是收集但原封不动地输出。按键值排序的任务会自动在框架中完成。

### 具体实现

MapReduce可以运行在一个有消费级主机组成的集群上。整个集群运行一个分布式文件系统，在论文中即“三架马车”之一的GFS，因此可以简单的共享数据。

分区函数： 在运行MapReduce时，最终要启用的Reduce工作是确定的。分区函数的意义就在于将最终的结果按规律写入nReduce个文件中。最常用的方法就是使用hash(key)%nReduce作为分区函数

#### 执行流程

1. 框架自动将输入的文件分割为M块，单块的大小通常是16MB到64MB之间，然后正式启动程序。（这个分块也是有讲究的，应该要保证分割不会破坏完整的行）

2. 在所有启动的程序中，有一个机器上的程序是最特殊的，被称为master（后面因为一些政治正确的原因，也被称为coordinator），其余的都是worker。master负责分配map或reduce工作给worker

3. 接收到map工作的worker从指定的地方获取数据，然后使用用户自定义的map函数处理这些数据。将中间数据缓存在内存中

4. 片刻后，在worker完成map工作后，将中间数据写入本地的文件系统（不是GFS）中。根据分区函数的设置，将中间产物写入对应的分区文件中，以便接下来的Reduce函数的使用。比如说可以以`mr-nMap-nReduce`命名，`nMap`是map工作的序号，`nReduce`是区号。假设在word count中，键值apple经过分区函数计算后得到2，那么在本次map工作中，有关apple的数据都会写入`mr-nMap-2`中

5. 当master通知了一个reduce worker需要的数据所在的位置时，reduce worker通过网络通信的方式获取map阶段产生的数据。假设该reduce worker的编号为R,因为该reduce worker获取的数据是从多个类似于`mr-nMap-R`的文件中读取的，尽管每一个`mr-nMap-R`文件内部都是按键值排序好的，但reduce worker要使用的是它们的数据的组合，所以进行一个归并是有必要的

6. 归并完成后，所有的`mr-nMap-R`文件合并成了一个大的有序的中间数据文件。接着reduce worker从该文件中读取键值相同的数据，将其传给用户自定义的reduce函数，将结果记录在最终的输出文件的一个分区文件上（比如`mr-out-R`），reduce工作的结果会存储在分布式的文件系统中

7. 当所有的map和reduce工作完成后，master向他们发送终止信号，退出MapReduce

#### 故障恢复

因为mapreduce设计在成百上千台计算机上工作，小概率事件乘上大基数就是必然发生。所以一个完整的分布式系统必须要有自动的故障恢复能力。

