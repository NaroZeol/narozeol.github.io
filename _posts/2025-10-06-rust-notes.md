---
date: 2025-10-06
title: "Rust学习笔记"
category: 学习经验
tags: [rust]
excerpt: "原神，启动！"
---

通过《Rust 程序设计语言》来学习，看的是[中文译本](https://kaisery.github.io/trpl-zh-cn/)

只是些快速记录，便于后续查阅

记录一些自己感觉有意思的东西

1. rust调用宏的方式是`宏名+!`

    例如`println!("Hello World")`就是在调用名为println的宏而不是println函数

2. `Cargo.lock`文件记录了当前目录下的target生成的二进制实际依赖的版本

3. 使用`cargo check`来快速检查当前代码是否能正常编译，比直接执行编译更快速且方便

4. rust中变量默认不可变，需要可变的变量需要使用`mut`关键字

5. rust使用&表示引用而不是指针，需要在函数调用时显式指出，并且引用默认也是不可变，如需可变还是需要`mut`关键字

    如`io::stdin().read_line(&mut guess)`

6. rust使用语义化版本控制，cargo使用的包保证至少不低于指定版本，同时不会超过下一个兼容版本

7. cargo中实际控制编译时使用依赖版本的是`Cargo.lock`文件，因此其总是和代码一起进行版本管理。`Cargo.toml`实际上管理的是`Cargo.lock`最终会使用的最低兼容版本。

    比如设置dependencies中`rand=0.8.5`，可以使用`cargo update`来更新到`rand=0.8.6`，但不会更新至`rand=0.9.0`，即使这个版本是可用的

8.  rust允许`遮蔽 （Shadowing）`作用域中存在的变量，让不同类型的变量和之前相同的变量名。非常现代的一个特性。

    比如当前作用域有个叫`guess`的String变量，依旧允许使用
    ```rust
    let guess: u32 = guess.trim().parse().expect("Please type a number!");`
    ```
    来覆盖之前的guess变量

    遮蔽只会影响当前作用域，当离开当前作用域之后，内层作用域创建的遮蔽不会影响外层作用域

9. match关键字提供了类似其他语言switch的功能，语义和功能有点接近C#的switch

10. 使用loop关键字来创建无限循环

11. rust依旧提供语义更强的`const`来显式声明常量，常量根据约定，总是以全大写加下划线的形式存在（类似环境变量）

12. `char`类型表示一个Unicode字符，占4个字节。现代语言通常区分char和byte，不再沿用C语言的1字节模式。

13. 一行没有加分号的代码被认为是一个表达式，其作为代码块的返回值，这个设计有点意思。

    ```rust
    fn main() {
        let y = {
            let x = 3;
            x + 1
        };

        println!("The value of y is: {y}");
    }
    ```
    ```rust
    fn five() -> i32 {
        5
    }

    fn main() {
        let x = five();

        println!("The value of x is: {x}");
    }
    ```
    rust将控制流语句（if / match / loop）都设计成表达式，所以下面这样的代码也是合法的
    ```rust
    fn num(switch: bool) -> i32 {
        let val = if switch { 5 } else { 10 };
        val + 1
    }
    ```

14. 当一个非引用变量被赋值或者作用域销毁时，如果有实现`drop trait`，则会立刻调用类型实现的`drop`函数。本质上还是RAII那一套。

15. rust中，复杂类型（使用了堆空间）的默认复制方式总是被处理成移动。其在实现上还是类似于golang的做法，默认只复制结构体本身，也就是默认只做浅拷贝。但是rust设计巧妙的地方就在于会剥夺旧变量对于堆空间的所有权，在编译阶段就不允许两个变量同时持有堆空间的访问权限。

    s2是s1栈区数据的拷贝，不拷贝堆区，同时s1不允许再次使用。
    ```rust
    let s1 = String::from("hello");
    let s2 = s1;
    ```
    ![](/assets/images/2025-10-06-rust-notes/trpl04-04.svg)

    但这样的设计也导致**如果不使用引用**，在调用函数时需要多次转移所有权，在函数中主动归还调用者转移给函数的所有权。
    ```rust
    fn main() {
        let s1 = gives_ownership();        // gives_ownership 将它的返回值传递给 s1

        let s2 = String::from("hello");    // s2 进入作用域

        let s3 = takes_and_gives_back(s2); // s2 被传入 takes_and_gives_back, 
                                        // 它的返回值又传递给 s3
    } // 此处，s3 移出作用域并被丢弃。s2 被 move，所以无事发生
      // s1 移出作用域并被丢弃

    fn gives_ownership() -> String {       // gives_ownership 将会把返回值传入调用它的函数

        let some_string = String::from("yours"); // some_string 进入作用域

        some_string                        // 返回 some_string 并将其移至调用函数
    }

    // 该函数将传入字符串并返回该值
    fn takes_and_gives_back(a_string: String) -> String {
        // a_string 进入作用域

        a_string  // 返回 a_string 并移出给调用的函数
    }
    ```

16. 引用也分为不可变引用和可变引用。如果引用的对象本身就是不可变的，那么不允许创建可变引用。

    在所有权系统中，类似于读写锁关注的读写问题，要么只存在一个可变引用，要么在没有可变引用的前提下存在多个不可变引用
    
    -   ```rust
        // 错误的代码
        let mut s = String::from("hello");

        let r1 = &mut s;
        let r2 = &mut s; // 不允许存在多个可变引用

        println!("{}, {}", r1, r2);
        ```
    -  ```rust
        // 错误的代码
        let mut s = String::from("hello");

        let r1 = &mut s;
        let r2 = &s; // 在存在可变引用的情况下创建不可变引用

        println!("{}, {}", r1, r2);let mut s
       ```

17. 引用的作用域是从它创建的开始到它最后一次被使用的位置
    
    r1的作用域持续到第一个println结束，所以之后再创建一个可变引用不会违反不可同时存在多个可变引用的规则

    ```rust
    fn main() {
        let mut s = String::from("hello");

        let r1 = &mut s;
        println!("{}", r1);
        let r2 = &mut s;
        println!("{}", r2);
    }
    ```

    将顺序调换，此时r1的作用域持续到第二个可变引用创建之后，此时就会触发编译错误

    ```rust
    // 错误的代码
    fn main() {
        let mut s = String::from("hello");

        let r1 = &mut s;
        let r2 = &mut s; // 编译错误
        println!("{}", r1);
        println!("{}", r2);
    }
    ```

    总之rust设计的期望就是从语法上强制要求同一时间只有存在一个写端口，或者在没有写端口的情况下存在多个读端口

18. rust的字符串字面值是一个字符串slice，类型为`&str`，属于不可变slice,其中指针指向一块特定的内存空间。

19. rust允许当上下文中变量名与结构体的字段名一致时，可以免去字段名

    下面的代码是合法的（不必和原结构体的顺序一致）
    ```rust
    struct User {
        active: bool,
        username: String,
        email: String,
        sign_in_count: u64,
    }

    fn build_user(email: String, username: String) -> User {
        User {
            active: true,
            username,
            email,
            sign_in_count: 1,
        }
    }
    ```

20. rust提供了一个方便的从一个结构体更新出新的结构体的语法

    需要注意的是，这里的更新等同于赋值的`=`，对于部分类型的字段（比如`String`）会触发移动，导致原来的结构体不可用。总之永远不要重用使用了更新结构体语法的原结构体
    ```rust
    fn main() {
        // --snip--

        let user2 = User {
            email: String::from("another@example.com"),
            ..user1 // 除指定了的字段`email`以外，其他所有字段复用user1
        };
    }
    ```

21. 当结构体实现特定特性就可以和内置的宏交互，比如`dbg!()`宏（Debug）、`println!()`宏（Display），如果结构体实现了`Debug`，就可以方便地打印出表达式的值和结果。在结构体的定义出加上注解`#[derive(Debug)]`能够自动实现`Debug`，打印出对应的字段值

    如下：

    ```rust
    #[derive(Debug)]
    struct Rectangle {
        width: u32,
        height: u32,
    }

    fn main() {
        let scale = 2;
        let rect1 = Rectangle {
            width: dbg!(30 * scale),
            height: 50,
        };

        dbg!(&rect1);
    }
    ```
    输出：
    ```text
    [src/main.rs:10:16] 30 * scale = 60
    [src/main.rs:14:5] &rect1 = Rectangle {
        width: 60,
        height: 50,
    ```

22. rust实现结构体方法的编写方式是额外使用`impl`关键词划分一块代码块。说实话这个方式感觉还是没有golang自由，感觉目前声明结构体方法的语法中，golang的是最优的，其他的都差点意思。

    ```rust
    struct Rectangle {
        width: u32,
        height: u32,
    }

    impl Rectangle {
       fn area(&self) -> u32 {
           self.width * self.height
       }
       
    }
    ```
    
