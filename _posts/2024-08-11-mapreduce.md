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

MapReduce来自于Jeff Dean和他的Google同事们在编写有关处理互联网的海量数据的程序时的思考。

Google为了完成各种处理互联网海量数据的任务，编写过几百种不同的实现方案，比如计算反向指数，解析HTML文档等概念上很直接，但是都因为海量的数据而难以在以往的单机系统中实现。

因此Jeff Dean和他的同事们总结了在编写分布式应用的经验，提出了一种解决分布式系统问题的一种通用解法，即MapReduce。

Map，Reduce来源于函数式编程。作者们发现大部分的并行计算都涉及

- **map**操作将所有的输入转化为临时的键值对组合
- **reduce**操作将具有相同键的值组合起来计算

这篇论文的工作将构建分布式应用的工作简化为编写特定的Map函数和Reduce函数，同时保障具有足够的Scalability。

>hides the messy details of parallelization, fault-tolerance, data distribution and load balancing in a **library**

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

1. **Worker Failure**

    master定期检查worker是否存活。如果有worker死亡，根据该worker所工作的内容，要有以下工作：

    - 重做该worker的所有map工作，因为map输出的中间产物是存放在本地的磁盘而不是GFS中。在重做完之后要通知对应的reduce工作从新的位置获取中间产物。

    - 不需要重做reduce工作，因为reduce的产物是直接存放到GFS中。

2. **Master Failure**

    作者在论文中提到Master可以用定期存储checkpoint的方式来实现恢复

    但是因为只有一个Master，所以Master出现错误的概率很小。在原始论文中作者提到他们根本没有实现Master Failure。

    这是合理的，小概率乘上小基数约等于不发生，如果真的碰到了大不了重做就行了。

3. **故障恢复中的语义问题**

    因为在故障恢复中涉及到重做，所以会产生一些语义问题

    如果被执行的map或reduce函数是**不确定的**（non-deterministic），重做就会带来一些问题

    所谓的**不确定的**指一个函数的运算结果依赖于除了输入数据以外的数据，比如说一个map函数可能会依赖于执行map函数时的Unix时间戳，这时我们就称这个map函数是不确定的，因为在不同时刻以相同的输入调用会带来不同的输出

    在重做时，如果函数是**确定的**（deterministic），就可以保证前后重做的结果是一致的

    反之，如果map函数是**不确定的**的，重做后通知reduce使用新的版本，但是因为外部数据的变化，reduce一部分接收的中间产物是由外部数据变化前的map函数产生的，一部分是由变化后的map函数产生的。这时就无法保障在分布式系统中计算的结果和顺序执行的语义一致

    不过好在大部分的MapReduce工作都是不涉及外部数据的，使用者应该要有意避免使用可能会使用外部数据的函数。

### Backup Tasks

分布式系统一个常见的问题就是整体效率受到某个单机的效率而下降，即所谓木桶效应

单机效率低下会有很多原因，很多时候这种问题难以被察觉。因为从宏观上看，这台机器还是完好无损的。一个常见的例子就是磁盘的**Fail-slow问题**，一个磁盘看上去完好无损，但是读取速度却显著下降。其原因在于消费级磁盘为了维护数据完整性，尽管磁盘的部分发生了损坏，为了不影响读写功能，磁盘内部通过了某些备用机制修复了这些损坏，但代价就是读取速度从可能的30Mb/s变为1Mb/s

对于这种“straggler”（掉队者）问题，论文介绍了一种解决方案

> When a MapReduce operation is close to completion, the master schedules backup executions of the remaining in-progress tasks.

当MapReduce任务接近要完成时（仅剩下一小部分工作未完成），这时master会将正在运行的工作分配到空闲出来的机器上。接着master只需要等待第一个完成信号，然后将对应的工作标记为完成，接下来忽视该工作后续的完成信号即可。

通过这种机制补足了潜在的木桶效应带来的问题，确实很巧妙。

### 优化方案

接下来论文介绍了针对特殊任务的优化方案

#### 分区函数

前面提过，框架依赖于分区函数来确定不同键值的数据对要存放到哪里

通常默认的分区函数是**hash(key) mod R**，但是对于一些特殊的任务，MapReduce框架允许用户自定义分区函数

比如在有关URL的处理时，用户可能更希望分区函数能够将具有相同hostname的URL放的结果输出到同一文件而不是根据URL的哈希值

这时就可以**hash(Hostname(urlkey)) mod R**来对URL键值来分区，这样具有相同hostname的URL的结果会被输出到同一文件中

#### 组合函数

组合函数（Combiner Function）允许执行map工作的机器承担一部分的reduce工作。

比如说word count的例子中，因为英文单词the是高频词汇，中间产物中会有大量的<"the", "1">。组合函数允许在map工作完成之后，将这些键值对先进行一部分的预处理。比如说在word count中可以执行的预处理就是将10000个<"the", "1">组合成单个键值对<"the", "10000">这样。这时就能减轻reduce工作的压力。

#### Skipping Bad Records

跳过部分会导致错误的输入数据，避免整个系统因为某些数据而崩溃

因为map和reduce函数都是由用户编写的，难免会有一些问题。比如说有可能map函数在使用某条数据作为输入时会导致segment fault，而且是无论怎么调用都会出错

这时如果简单的让master重新分配该记录给其他worker，就会导致整个系统都因为一条数据而崩溃

因为MapReduce要面对的是以TB为量级的数据，所以就算忽视掉一小部分也是无所谓的

论文提到了一种解决这种问题的方案，在每一个worker中做好异常处理，当这个worker在使用某些数据导致segment fault时，发送一个UDP包告诉master该数据可能会导致异常。当master接收到多个UDP包时，就可以判断确实该数据会造成异常，接下来重新分配该工作时就可以告诉worker要避开这个可能导致异常的数据。

不得不说用UDP来实现这个机制确实聪明。

### 总结

后面跟性能分析的部分没怎么仔细看，就不写了。

不得不说不愧是来自Jeff Dean的论文。整篇文章行文流畅，读起来一点压力都没有，理解起来也不难。虽然很多实现细节没有完全揭示（不然Google怎么赚钱😂），但是整个机制解释的很清楚，基本能理解这个系统是怎么工作的了。

初次看MapReduce的概念真的很难理解为什么能开启大数据的黄金年代，在读完这篇论文之后总算有一点理解了。

其实我觉得构建这样的系统最难的反而是完成另外一辆马车GFS😂

## Lab 1: MapReduce

接下来是lab1的个人思路

根据[MIT学术诚信规范](https://integrity.mit.edu/)，不再公开完整代码（xv6 lab暂时先不管，既往不咎嗷），只写一些个人在完成lab时的思路

lab1相对来说还是比较简单的，毕竟评级是moderate/hard。在阅读完论文后大概前后花了三天时间通过所有测试，其中实际编码时间可能也只有十几个小时。

### 总体思路

在刚接触这个lab的时候，还是感受到十分的吃力。

不过好在和出自同一实验室的xv6 lab一样，该lab也有详细的需求分析和指导。

在阅读完所有的要求后，我走出家门出去河边转了几圈，一边散步一边想怎么做

这是我当时想到的几点可能比较重要的设计：（事实上后面也是跟着这个设计来的）

1. 由coordinator维护一个worker列表，其中记录worker的状态：空闲，工作，死亡。分配工作时就更改相关的状态

2. worker定期向coordinator查询自己的状态是否发生变化，发生变化则请求要工作的内容，然后完成工作

3. worker完成工作之后向coordinator发送改变状态的请求

4. coordinator维护要完成的task列表，在分配工作时启用一个计时器，如果对应的工作没有在10s被完成则重新分配工作，并标记对应的worker死亡，不再分配工作给它（实现时没有用协程计时器，用了更简单的时间戳和轮询）

5. 根据hash(key) % nReduce来决定要怎么分区，map在工作时生成文件map-m-r，其中m为该map工作的编号，r为对应的reduce工作的编号。采用论文提到的trick来提供避免数据竞争

6. 要有一个worker的注册机制

7. 在所有的工作都完成后，由coordinator发送终止信号

整个设计和论文中提到的设计有很多简化的地方（能跑就行，[KISS](https://en.wikipedia.org/wiki/KISS_principle)嘛）

1. 因为测试在本地运行，本地的文件系统即模拟分布式的文件系统。所以map产生的临时文件并不是像论文中提到的存放在对应map节点的本地，而是简单的存放在所谓的“全局文件系统”中。这给我们的实现带来很多好处

2. 论文中的设计是由coordinator主动通过网络通信的方式来告知worker有新的工作，worker是被动接收工作的。在我的设计中通过类似心跳机制的方式，worker是主动通过RPC访问coordinator维护的数据结构来查看自己当前是否应该处于工作状态

3. 在理想的设计中，部分的reduce工作应该是能够和map工作并行的。在我的设计中，reduce必须要在所有的map完成之后再开始

4. 在我的设计中，在一个工作被分配后，通过修改coordinator维护的worker列表中的字符串来标识工作的内容。比如一个worker被分配到map 1的工作，那么这个字符串就是`Map 1 {filename}`，如果分配的是reduce 1的工作，那么该字符串是`Reduce 1 1`。worker通过解析这些有规律的字符串来得知自己要完成的是什么任务

### 数据结构

Coordinator: 

```go

type worker struct {
	state workerState               // 枚举类型
	work  string                    // 该worker当前需要完成的工作，有规律的字符串
}

type workInfo struct {
	state     int                   // -1->done 0->ready or id of worker
	timestamp time.Time             // Only valid when state is not 0 or -1
}

type Coordinator struct {
	workerMap     map[int]worker    // 维护worker的map，以worker的id为键值
	workerMapLock sync.RWMutex
	workInfo      []workInfo        // 维护当前work的列表，根据阶段的不同，可以为map的列表也可以为reduce的列表
	workInfoLock  sync.RWMutex
	state         string            // Working, Starting, Exiting, Death 这是对于Coordinator自身而言的状态
	stateLock     sync.Mutex
	logger        Logger
	nReduce       int
}

```

worker:

```go

// 主要存放该worker的元数据，都是在一次初始化时完成，后续不再更改

type WorkerType struct {
	mapf    func(string, string) []KeyValue
	reducef func(string, []string) string
	logger  Logger
	nReduce int
	id      int
}

```

### 工作流程

1. Coordinator启动，根据输入文件的个数创建对应的map列表

2. Worker启动，通过RPC调用Coordinator的**注册函数**，在Coordinator维护Worker的表中添加一项，初始时状态被设置为`WS_Free`

3. Coordinator发现有尚未分配的map工作和空闲的Worker。修改Worker列表，设置该空闲的Worker**状态**为`WS_Ready`，**工作内容字符串**为`map {mNum} {filename}`，其中`mNum`为该map工作的id，`filename`为要处理的文件名，同时记录该工作发布的时间戳

4. 空闲中的Worker通过RPC来获取在Coordinator中自己的状态，如果发现状态被设置为了`WS_Ready`，则解析其中的工作内容字符串，根据其中的内容来决定要完成的工作

5. 执行map的Worker将中间产物以`mr-nMap-nReduce`的命名按json编码存放，该过程通过文件系统的重命名来保障原子性。

6. 当Worker在完成了一个工作后，通过RPC调用Coordinator的工作完成函数，该函数根据RPC的参数，将对应的工作标识为完成，同时修改对应worker的状态为`WS_Free`

7. 待所有的map工作都已完成，则Coordinator创建一个Reduce工作列表，开始进行Reduce工作的分配，其流程与map基本一致

8. 执行reduce的worker根据reduce的编号，从文件系统中获取对应的`mr-*-nReduce`，然后将结果输出到`mr-out-nReduce`中

9. Coordinator会检查已分配工作的时间戳，对比当前时间是否超过了10s。如果超过了则认定对应的worker已死亡，将其的状态置为`WS_Death`，重新分配该工作。

10. 待所有的工作都完成后，Coordinator遍历worker列表，向其中存活的worker发送一个**假工作**，设置**状态为**`WS_Exiting`，**工作内容字符串为**`Exit`。当worker接收到了`Exit`工作，则再通过RPC调用对应的死亡函数将Coordinator的列表中的状态设置为`WS_Death`。接着退出程序。

11. Coordinator持续检查是否所有的Worker都退出了，如果所有的Worker都已退出则自身也退出程序

### 缺点

相比于正常的方案，我的方案个人认为有以下缺点：

1. 使用类似心跳机制的方式让Worker获取工作，可能会在某些情况下造成效率的严重损失

2. 默认所有的中间产物都存储在分布式的文件系统而不是本地文件系统中，在真实的系统中会造成大的开销

3. Reduce工作只有在Map工作全部完成后才会发布，理论上应该要允许一部分并行

4. 没有解决潜在的木桶效应

5. 死亡的worker没有类似复活的机制，理论上可以实现一个重新注册功能

6. 使用有规律的字符串而不是一个特定的编码方式会造成可维护性差

7. 在并发控制时大量使用了锁来保护，可能会造成扩展性非常差
