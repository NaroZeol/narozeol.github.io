---
date: 2024-07-25
title: "CSAPP-Malloclab"
category: 学习经验
tags: [lab, CSAPP]
---

## 总览

### 要求

该lab要求我们完成四个函数，分别是`mm_init`、`mm_malloc`、`mm_free`和`mm_realloc`。即模拟标准malloc库的行为。

### 建议
该lab鼓励我们实现一个用于检查堆的工具函数`mm_check`，在每次进行malloc、free、realloc操作后调用该函数，检查堆的一致性。具体的检查可以检查以下几点：

- Is every block in the free list marked as free?
- Are there any contiguous free blocks that somehow escaped coalescing?
- Is every free block actually in the free list?
- Do the pointers in the free list point to valid free blocks?
- Do any allocated blocks overlap?
- Do the pointers in a heap block point to valid heap addresses?

当然这样的工具不是必须的，但是可以帮助我们更好的理解和调试我们的代码。这样的工具会占用大量的运行开销，所以在最终提交的时候需要将调用检查的代码删除。

### 提供的工具
在`memlib.c`文件中提供了一组用于模拟系统调用的函数，我们可以使用这些函数来模拟系统调用。

- **void *mem_sbrk(int incr)**: Expands the heap by incr bytes, where incr is a positive
non-zero integer and returns a generic pointer to the first byte of the newly allocated heap area. The
semantics are identical to the Unix sbrk function, except that mem sbrk accepts only a positive
non-zero integer argument.
- **void *mem_heap_lo(void)**: Returns a generic pointer to the first byte in the heap.
- **void *mem_heap_hi(void)**: Returns a generic pointer to the last byte in the heap.
- **size_t mem_heapsize(void)**: Returns the current size of the heap in bytes.
- **size_t mem_pagesize(void)**: Returns the system’s page size in bytes (4K on Linux systems).

### 规则
- 不应该改变`mm.c`中的任何接口
- 不应该使用任何与内存相关的系统调用或库函数，应该使用`memlib.c`中提供的函数
- 不应该在`mm.c`中定义任何全局数据结构（例如数组，链表，树），但是简单的内置类型是允许的（在堆上构建的复杂数据结构也是允许的）
- 应该要和库函数的行为一致，总是返回8字节对齐的块

## 结构设计
我决定使用显式空闲链表来实现这个lab的要求。

### 显式空闲链表
显式空闲链表是一种双向链表，每个节点包含一个空闲块的地址和大小。这样的链表可以很方便的找到合适大小的空闲块。

为了提高分配器的效率，我们引入两个很有意思的机制：**分离适配**和**立即合并**。

- **分离适配**：我们将空闲块按照大小进行分组。每一组固定一个最小大小，当我们需要一个大小为n的空闲块时，我们只需现在最小大小小于n和最大大小大于n的组中找到一个满足条件的块即可。**然后如果这个块在分配给用户后还有大量的空余，我们选择将其进行分割，将剩余的部分放入对应的组中。**
- **立即合并**：当我们释放一个块时，我们**立即检查**其相邻的块是否是空闲的，如果是，我们就立即合并这两个块。这样可以避免出现连续的空闲块，为了得知相邻块的状态，我们需要在块的头部和尾部都存储一些信息。

### 显式空闲链表节点定义

每一个节点在**未分配**时，同时存储前一节点和后一节点的地址，便于进行取出和插入。同时也要存储该节点的大小，而且应该要在头尾都进行存储，便于进行合并节点的操作。

经过测试，该lab在编译时会将目标平台设置为32位，即**每个指针的大小为4字节。**

所以我决定将节点的**最小大小设置为16字节**，其中，8字节存储该节点的大小信息以及是否被分配，另外8字节存储前一节点和后一节点的地址，节点的大小应该是8的倍数。

空闲节点的具体结构如下：
```
+-------------------+-------------------+-------------------+..........+-------------------+
|       header      |       prev        |        next       |  bubble  |       footer      |
+-------------------+-------------------+-------------------+``````````+-------------------+
0(byte)             4                   8                   12     node_size - 4      node_size
```

在这样的结构中，`header`和`footer`都存储了该节点的大小信息和是否被分配，`prev`和`next`分别存储了前一节点和后一节点的地址。

`header`和`footer`的具体结构如下：
```
+----------------+---+
|   node_size    | A |
+----------------+---+
0                29  31(bit)
```
在4字节即32位的数据中，只有3位用于存储是否被分配（不止包括当前块是否被分别，甚至还应该包括前一个块是否被分配，**为了实现立即合并**），其余29位用于存储该节点的大小，这足够对应我们最大的堆大小（在`config.h`中定义为20mb）。

这样复杂的结构导致我们需要在需要取出这些值时进行一些指针操作（一个更简单的做法是使用结构体的位域定义，但是在规则下似乎不太适合这么做）。为此，我设置了一些宏用于取出这些值。

```c
// 操作节点的宏
// p 为一个已知地址的节点， 类型为 char *

#define GET_SIZE(p) (*(unsigned int *)(p) & 0x1fffffff) 
#define GET_ALLOC(p) ((*(unsigned int *)(p) & 0x80000000) >> 31) // 0 为未分配，1 为已分配
#define GET_PREV(p) (*(char * *)((p) + 4))
#define GET_NEXT(p) ((*(char * *)((p) + 8)))
#define GET_FOOTER(p) (*(unsigned int *)((p) + GET_SIZE((p)) - 4))
#define GET_SIZE_FROM_FOOTER(footer) (*(unsigned int *)(footer) & 0x1fffffff) // 从footer中取出size
#define GET_ALLOC_FROM_FOOTER(footer) ((*(unsigned int *)(footer) & 0x80000000) >> 31) // 从footer中取出alloc

// 从一个已知地址的节点中设置各种信息
#define SET_SIZE(p, size) (*(unsigned int *)(p) = ((size) | (0xe0000000 & *(unsigned int *)(p)))) // size 不应该超过 0x1fffffff
#define SET_ALLOC(p, alloc) (*(unsigned int *)(p) = ((*(unsigned int *)(p) & 0x7fffffff) | ((alloc) << 31))) // alloc 只能为 0 或 1
#define SET_PREV(p, prev) (*(char * *)((p) + 4) = (prev))
#define SET_NEXT(p, next) (*(char * *)((p) + 8) = (next))
#define SET_FOOTER(p) (*(unsigned int *)((p) + GET_SIZE((p)) - 4) = *(unsigned int *)(p)) // 将header的信息复制到footer
```

### 空闲链表节点的有效载荷

一个节点的有效载荷和该节点的大小是两个不同的概念。有效载荷指的是我们分配给用户的空间，而节点的大小指的是整个节点的大小。在我们的设计中，**`有效载荷` = `节点的大小` - 8**。

下面解释为什么是这样的关系：

当我们分配了一个节点后，我们从空闲链表中取出该节点。然后设置一些东西，返回给用户一个指针指向有效载荷。

分配后的节点结构如下：
```
+-------------------+-------------------+-------------------+..........+-------------------+
|       header      |       bubble      |                   payload                        |
+-------------------+-------------------+-------------------+``````````+-------------------+
0                   4                   8                   12                        node_size
                                        ↑
                                        p
```

其中p代表我们返回给用户的指针，指向有效载荷。

为什么要将p设置为`node + 8`呢？首先我们肯定要保留`header`的信息，而为什么指向`+8`而不是`+4`呢？这是因为我们需要保证返回给用户的指针是8字节对齐的。

### 如何正确的合并节点

在释放一个节点后，我们需要检查其相邻的节点是否是空闲的，如果是，我们就立即合并这两个节点。

但是**我们需要一个方式来确定一个节点的左右是否是空闲的**。

（为了便于**和链表中的前后节点的概念区分**，我们将物理空间与当前节点相邻且地址低于当前节点的节点称为当前节点的左节点，反之称为右节点）

对于右侧节点，这个问题很简单，我们只需要将当前节点的起始地址加上当前节点的大小，就可以得到右侧节点的起始地址，通过前面的宏，我们就可以得到后一个节点的信息。

但对于左侧节点，这个问题就有点复杂了。注意我们在将节点分配时为了让返回给用户的有效载荷最大化，有效载荷是包括了`footer`部分的。也就是说用户可以任意地修改有效载荷的内容，包括`footer`部分。所以我们不能直接通过`footer`部分来得到左侧节点的信息。

还好书中为我们提供了一个方案：
> 幸运的是，有一种非常聪明的边界标记的优化方法，能够使得在已分配块中不再需要脚部。回想一下，当我们试图在内存中合并当前块以及前面的块和后面的块时，只有在前面的块是空闲时，才会需要用到它的脚部。**如果我们把前面块的已分配/空闲位存放在当前块中多出来的低位中**，那么已分配的块就不需要脚部了，这样我们就可以将这个多出来的空间用作有效载荷了。不过请注意，空闲块仍然需要脚部。

书中提到如果将当前块是否已分配的信息存放到右侧块的低位中，那么我们就可以不需要脚部了。这样我们就可以将脚部的空间用作有效载荷了。

这就是为什么前面我们拿出三个bit用于存放是否分配的信息。

对于header我们作如下定义

```
+----------------+---+
|   node_size    | A |
+----------------+---+
0                29  31(bit)
                 ↑   ↑
             左侧块  当前块 
```

如果从一段连续的内存来看
```
                                            此处被置1表示左侧块被分配
                                                            ↓
+-------------------+~~~~~~~~~~~~~~~~~~~~~~~~+-------------------+~~~~~~~~~~~~~~~~~~~~~~~~+
|       header      |       other data       |      header       |       other data       |
+-------------------+~~~~~~~~~~~~~~~~~~~~~~~~+-------------------+~~~~~~~~~~~~~~~~~~~~~~~~+
0                   4                       16                 ↑ 20
块1（总大小16字节，有效载荷8字节，已分配）       块2           此处被置0表示当前块未被分配                 
```

为了实现检查，我设计了一组宏用于设置和检查这些标志位
```c
// p 为一个已知地址的节点， 类型为 char *
// 为了和prev以及next区分，我们将前一个节点称为left，后一个节点称为right

#define SET_RIGHT_NODE_WHEN_ALLOC(p) (*(unsigned int *)((p) + GET_SIZE((p))) = (*(unsigned int *)((p) + GET_SIZE((p))) | 0x20000000)) // 当一个节点被分配时，调用该宏将下一个节点的上一节点标志位设置为已分配
#define SET_RIGHT_NODE_WHEN_FREE(p) (*(unsigned int *)((p) + GET_SIZE((p))) = (*(unsigned int *)((p) + GET_SIZE(p)) & 0xdfffffff)) // 当一个节点被释放时，调用该宏将下一个节点的上一节点标志位设置为未分配

#define IS_LEFT_ALLOC(p) ((*(unsigned int *)((p)) & 0x20000000) >> 29)
#define IS_RIGHT_ALLOC(p) ((*(unsigned int *)((p) + GET_SIZE((p))) & 0x80000000) >> 31) // 检查下一节点是否被分配
```

注意检查左右节点是否被分配的宏都**不检查是否左右节点是否为NULL以及是否到达当前堆的边界，这需要我们在代码中自行判断**。

所以我们需要一个方式来知道合并操作是否到达了堆的边界。我们也可以提供一系列的宏来进行判断。

```c
// p 为一个已知地址的节点， 类型为 char *

// 判断是否为有效节点的最左边
#define IS_LEFTEST(p) ((p) == mem_heap_lo() + 112) // 112来自于我们在初始化时分配给空闲链表数组的大小

// 判断是否为堆的最右边
#define IS_RIGHTEST(p) ((p) + GET_SIZE((p)) == (mem_heap_hi() + 1))
```

### 链表数组的结构

我们使用一个数组来存储空闲链表，数组的每一个元素要么指向NULL，要么指向对应链表的第一个节点（即**没有头结点的双向链表**）

同时，因为不建议定义全局数据结构（包括数组），所以我们要手动使用`mem_sbrk`来分配一个数组来存储这些链表。

因为最大堆的大小为20mb（lab 的默认配置，做成动态分配会更好，这里就硬编码了），同时空闲链表的最小节点大小为16字节，所以我们设置从16到20mb的每个2的幂的空闲链表的头结点。

20mb = 20 * 1024 * 1024 = 20971520

以0为数组的最小下标，每一个下标对应的空闲链表的最小大小为2<sup>(i+4)</sup>，所以我们需要的数组大小为 4 + log<sub>2</sub>(20971520) = 4 + 24 = 28。

如下表所示 （由Copilot生成，仅保证前几个数据正确）

| 下标 | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 |
| - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | -| - | - | - | 
| 最小大小 | 16 | 32 | 64 | 128 | 256 | 512 | 1024 | 2048 | 4096 | 8192 | 16384 | 32768 | 65536 | 131072 | 262144 | 524288 | 1048576 | 2097152 | 4194304 | 8388608 | 16777216 | 33554432 | 67108864 | 134217728 | 268435456 | 536870912 | 1073741824 | 2147483648 |
| 最小有效载荷 | 8 | 24 | 56 | 120 | 248 | 504 | 1016 | 2040 | 4088 | 8184 | 16376 | 32760 | 65528 | 131064 | 262136 | 524280 | 1048568 | 2097144 | 4194296 | 8388600 | 16777208 | 33554324 | 67108736 | 134217720 | 268435448 | 536870904 | 1073741816 | 2147483640 |

我们需要的数组大小为28，所以我们需要分配 28 * 4 = 112 字节的空间，然后将首地址赋值给一个全局变量。

```c
// 初始化空闲链表的头结点
int mm_init(void)
{
    free_lists = mem_sbrk(sizeof(char *) * group_size);
    for (int i = 0; i < group_size; i++) {
        free_lists[i] = NULL;
    }
    return 0;
}
```

一个大致的链表结构如下所示（**注意为了节约内存我没有使用带头结点的方案**，如果使用那个方案，为了保证操作一致性，至少要用8字节，而不使用头结点只需4字节）：

```

编号        0          1          2
最小大小    16         32         64
            +----------+----------+----------+........
            ↓          ↓          ↓
            ○(20字节)  ○(38字节)   NULL
            ↓
            ○(28字节)
```

## 具体的实现细节


### 如何分配节点

这个是我们最关心的问题。

在显式空闲链表中，一个块的大小是按照当前块的实际大小进行分组的。而用户能够拿到的有效载荷等于当前块的大小减去8字节。

同时为了保证后续从堆中分配的块的地址也是8的倍数，我们需要将用户希望分配的大小向8的倍数取整（此即有效载荷）

所以当给定一个用户希望分配的size时，我们先将其向8的倍数取整然后加上8字节。
```c
int new_size = ALIGN(size) + 8;
```

接着我们用**二分查找**，找到一个最小大小小于`new_size`中的最大的组。
```c
// 一个通用的二分查找，从所有组中找到一个最小大小小于size中的最大的组，后续的所有寻找对应的组都使用该函数

static int binary_search(int size) {
    int left = 0;
    int right = group_size - 1;
    while (left <= right) {
        int mid = (left + right) / 2;
        if (size <= (1 << (mid + 4))) {
            right = mid - 1;
        } else {
            left = mid + 1;
        }
    }
    right = (right < 0) ? 0 : right; // 修正right为-1的情况
    return right;
}
```

我们从这个组开始遍历，寻找一个满足需求的块
```c
int start = binary_search(new_size);

for (int i = start; i < group_size; i++) {
    if (free_lists[i] != NULL) {
        char *p = free_lists[i];
        while (p != NULL) {
            if (GET_SIZE(p) >= new_size) {
                res = p;
                res_index = i;
                is_find = TRUE;
                break;
            }
            p = GET_NEXT(p);
        }
    }
    if (is_find) {
        break;
    }
}
```

如果找到了一个满足要求的块，那么将它从链表中取出，取出操作由一个通用函数实现。
```c
static void delete_node(char *p, int group_index) {
    char *prev =  GET_PREV(p);
    char *next = GET_NEXT(p);

    // 前节点指向后节点
    if (prev == NULL) { //prev为空意味着这是链表的第一个节点
        free_lists[group_index] = next; // 将链表数组中的对应项设为next
    }
    else { // 不是第一项
        SET_NEXT(prev, next); // 将前节点指向后节点
    }

    // 后节点指向前节点
    if (next != NULL) {
        SET_PREV(next, prev); 
    }
}
```

然后我们比较这个块的大小和将要分配的大小的差距，**如果差距大于等于16字节**（意味着至少能分割出一个最小的节点），我们就将这个块进行分割，再次使用二分查找找到一个适合放置剩余块的组，然后将剩余块加入到对应的链表中。

```c
// 如果得到的节点的大小比要求的大小大16字节（这表示我们至少还能分割出一个节点），则将其分割为两个节点
// 而且我们保证newsize是向8对齐的，所以不用在意是否会出现不对齐的问题
    if (GET_SIZE(res) - new_size >= 16){
        int old_node_size = GET_SIZE(res);
        char *new_node;
        int new_node_size = old_node_size - new_size;

        new_node = res + new_size; // 在满足原有节点的情况下的新节点地址
        SET_SIZE(res, new_size);

        SET_SIZE(new_node, new_node_size); // 这两条设置头部
        SET_ALLOC(new_node, FALSE);      //
        SET_FOOTER(new_node);            // 将头部的内容复制到脚部
        SET_PREV(new_node, NULL);
        SET_NEXT(new_node, NULL);

        // 二分查找找到要插入的组
        int inseart_index = binary_search(new_node_size);
        insert_node(new_node, inseart_index);
    }// 分割节点操作结束
```

接着我们修改节点的已分配状态以及告知右侧节点左侧节点已分配（注意无论是否被分割都要进行这个操作）。
```c
SET_ALLOC(res, TRUE);
SET_RIGHT_NODE_WHEN_ALLOC(res);
```

随后返回给用户一个指向有效载荷的指针。

如果没有找到满足要求的块，我们就需要调用`mem_sbrk`来分配一些新的空间。并进行相应的初始化，然后将有效载荷返回给用户。

### 如何从有效载荷中得到节点

为了从一个已分配的有效载荷中找到对应的节点的各种信息，我们需要通过指针运算来得到节点的头部地址。
```c
#define GET_NODE_FROM_PAYLOAD(p) (p - 8)
```

这样我们就获取到了节点的头部地址。

接着我们进行一系列设置，将节点的状态恢复至未分配状态。
```c
SET_ALLOC(node, FALSE);
SET_FOOTER(node);
SET_RIGHT_NODE_WHEN_FREE(node);
```

### 如何释放节点

当`mm_free`函数接受到需要释放的有效载荷时，我们将其转换为节点的头部地址。
```c
#define GET_NODE_FROM_PAYLOAD(p) ((p) - 8) // 从有效载荷的首地址得到节点的首地址
```
接着我们尝试进行合并。随后将最终的结果重新插入到对应的链表中。

### 合并操作

为了最大的利用空间，我们需要在释放一个节点后检查它是否能和周围的节点进行合并（**立即合并**策略）。

首先我们需要检查它的前一个节点和后一个节点是否是空闲的，如果是，我们就进行合并操作，这样的操作是**可递归的**，所以我们使用一个函数来完成这个操作。

正如书中所说，检查是否能合并会出现四种情况
- 左右节点都是空闲的
- 左节点是空闲的，右节点是已分配的
- 左节点是已分配的，右节点是空闲的
- 左右节点都是已分配的

我们只需要将空闲的节点从对应的链表中删除，然后通过指针运算来得到合并后的节点的头部地址，然后进行一系列的设置，将节点的状态设置为未分配状态。

一次合并操作完成后，我们立刻使用合并产生的新节点继续尝试合并操作。

将插入链表的责任交给`mm_free`函数，可以保证通用性，即使没有合并操作，也可以将节点插入到对应的链表中。
```c
// mm_free的一个片段
// 尝试合并节点
node = try_merge_free_node(node);
SET_NEXT(node, NULL);
SET_PREV(node, NULL);
SET_RIGHT_NODE_WHEN_FREE(node);

// 将节点插入到链表中
int node_size = GET_SIZE(node);
int index = binary_search(node_size);
insert_node(node, index);
```

## realloc的实现

realloc最简单最符合直觉的实现就是直接重新分配一个新的节点，然后将原节点的有效载荷拷贝到新节点中，然后释放原节点。但这样没办法利用我们现有的结构。

首先我们需要将size转换为需要的节点大小的形式
```c
int new_size = ALIGN(size) + 8;
```

如果条件是一下几种情况之一，我们可以尝试不重新分配节点和重新拷贝数据：

1. **new_size小于等于原节点的大小**

    这种情况下，我们只需要将原节点的大小设置为`new_size`，然后返回原节点的有效载荷即可。

    但是为了最大化地利用空间，我们**需要检查一下是否能够进行一次分割**。

    如果在满足了new_size后，剩余的空间大于等于16字节，意味着我们可以进行一次分割，将剩余的空间插入到对应的链表中。
2. **需要`realloc`的节点是位于最右侧的节点**

    如果是最右侧的节点，我们可以尝试调整堆指针的方式来获取不足的空间，需要上调的空间大小为`new_size` - `node_size`，然后更新当前节点的大小以及各种信息。
3. **`realloc`的节点的右侧节点是空闲的**

    如果右侧的节点是空闲的，我们可以尝试对右侧的节点进行合并。为了所谓的响应速度，我们在合并得到的大小超过需要的差额时停止合并，然后对节点信息进行一些调整之后返回即可。

    还注意到一个特殊情况，如果合并最后的节点是最右侧的节点，我们可以尝试进行第2种情况的操作，上调堆指针来补足差额。

如果在尝试了以上几个情况后都无法补足差额，那么我们就只好重新分配一个新的节点，然后将原节点的有效载荷拷贝到新节点中，然后释放原节点。

## 更进一步

### 更快的搜索速度

在`mm_malloc`中，我们通过遍历链表的形式来寻找符合要求的节点，这样的操作是线性的，这在链表很长时的表现会很糟糕。

可以考虑**将数据结构组织成红黑树**来加速搜索速度。

注意我们的header的结构中正好还有一个bit没有被使用。那么它正好能够用来存储红黑树的颜色信息。

同时prev和next指针也可以改为存储left和right指针。

但是手搓一个红黑树对我来说还是太困难了，就不选择实现了。

### 校验值

刚好在做这个lab的时候在阅读[OSTEP](https://pages.cs.wisc.edu/~remzi/OSTEP/)这本书。作者在第17章讲解内存管理时提到了一个很有意思的方案--**校验值**。

注意看我们分发出去的节点的结构
```
+-------------------+-------------------+-------------------+..........+-------------------+
|       header      |       bubble      |                   payload                        |
+-------------------+-------------------+-------------------+``````````+-------------------+
0                   4                   8                   12                        node_size
                                        ↑
                                        p
```
为了保证有效载荷的地址向8字节对齐，我们不得不在header的后面多加了4字节的气泡。这显然是一种浪费。

效验值的思想就是在多出来的四个字节中存储一个magic number，比如说1234567。然后在我们回收节点的时候，我们就可以**检查magic number是否被修改**了，如果被修改了，那么就说明用户写越界了，就可以立刻报错。

当然这样的保护措施只是一种辅助手段，不能完全保证用户不会写越界。但是这样的检查是很有意义的。

比如这样的伪代码
```c
if (check_magic_number(node) != 1234567) {
    printf("error: user write out of range\n");
    exit(1);
}
```

```
+-------------------+-------------------+-------------------+..........+-------------------+
|       header      |    magic number   |                   payload                        |
+-------------------+-------------------+-------------------+``````````+-------------------+
```

## 最终实现

该路径下放有最终实现的代码

[code](./code)

## 总结

最终lab的得分为93/100。听说实现了红黑树的版本可以得到满分。

lab在hint部分指出这可能是学生目前为止的职业生涯中写过的最复杂的代码，我认为这句话一点都不为过。

该lab从正式开始到题解写作结束大概花了五天的时间，尽管代码量只有简单的几百行，但是我从中收获到的远远超过之前写了几千行的学生管理系统，感觉到好学校的课程是真能学到真东西的。

通过这个lab，我对计算机内存的管理有了更深的理解，同时也尝试了如何在一个复杂的系统中进行调试（在这个lab中，我使用了大量的`printf`来进行调试，因为gdb在这个lab中的效果是远不如检查运行log来的方便的）。

拜托，自己实现一个标准库真的很帅好吧！