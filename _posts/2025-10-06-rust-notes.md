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

    其中签名中的`&self`等价于`self: &Rectangle`以及`self: &Self`，是一个用于简化签名的语法糖，总之**结构体方法**必须要有一个名为self的变量。

    注意此处使用的是`&self`，这样就不会剥夺调用者对对象的所有权。rust也支持使用`self`，但这种做法很少见，只有在需要将某个对象转化为对象，希望剥夺调用者的所有权时才会使用。

23. rust支持实现和字段名同名的方法来作为getter。但遗憾的是，rust不支持自动实现getter和setter，多半是出于零成本抽象的目的，不像C#一样可以忽略这个代价。

24. rust 有一个叫 自动引用和解引用（automatic referencing and dereferencing）的功能。方法调用是 Rust 中少数几个拥有这种行为的地方。

    它是这样工作的：当使用 object.something() 调用方法时，Rust 会自动为 object 添加 &、&mut 或 \* 以便使 object 与方法签名匹配。也就是说，这些代码是等价的：

    ```rust
    p1.distance(&p2);
    (&p1).distance(&p2);
    ```

    也就是说，rust将对类型的限制交给了方法的提供者。

    显然rust不支持函数重载，任何一个方法，都必须显式指定期望的调用者对象类型

25. 所有在`impl`块中定义的函数被成为关联函数，其与`impl`指定的类型有关。可以定义不以`self`为第一参数的关联函数，此时就不支持以`.`运算符调用，而是通过作用域`::`符调用，如String类型中的`String::from`

    这就是rust实现构造函数的方法，同时其也更灵活，可以为构造函数起各种名字，不像C++限制为类型名称

    ```rust
    // 创建一个正方形
    impl Rectangle {
        fn square(size: u32) -> Self {
            Self {
                width: size,
                height: size,
            }
        }
    }
    ```

26. rust的枚举相比其他语言复杂很多。

    - rust允许为一个枚举中的不同枚举项绑定到不同的数据类型，这扩展了传统的枚举的语义，该语法最大的用户就是下面将要提到的Option

    ```rust
    enum IpAddr {
        V4(u8, u8, u8, u8),
        V6(String),
    }

    let home = IpAddr::V4(127, 0, 0, 1);

    let loopback = IpAddr::V6(String::from("::1"));
    ```

    - rust提供强大的`match`语法来支持可以绑定数据的enum，通过在`match`的绑定语法，可以将enum绑定的数据赋予到变量上，然后在对应的处理代码中使用该变量

    > Rust 的 match 不仅仅是控制流语法，而是 类型系统的一部分。

    ```rust
    // 其中Coin是一个枚举类型，定义了其中的一个枚举：Quarter(UsState)，Quarter是枚举名，UsState是实际类型
    fn value_in_cents(coin: Coin) -> u8 {
        match coin {
            Coin::Penny => 1, // 按顺序匹配
            Coin::Nickel => 5,
            Coin::Dime => 10,
            Coin::Quarter(state) => {
                println!("State quarter from {state:?}!");
                25
            }
        }
    }
    ```

27. rust没有null值，它推崇现代的基于option的编程范式，使用一个特定的枚举值来表示“没有”或“空”。

    ```rust
    enum Option<T> {
        None,
        Some(T),
    }
    ```

    标准库的Option过于常用，因此rust将其直接内置于全局空间中，可以直接定义与调用`Some`和`None`，例如

    ```rust
    let some_number = Some(5);
    let some_char = Some('e');

    let absent_number: Option<i32> = None;
    ```

    基于Option的编程范式规定在使用一个Option值之前，必须主动将其从Option中取出，同时完成一些校验来保证不是空置。即在使用一个值之前，一定能保证使用的值是存在的，以此避免过去常见的解引用空指针这一非常难以避免的运行时错误。

    **强制程序员接受显式处理“空值”，不要信赖任何函数的返回值，从根本上避免运行时解引用空指针的问题。**

    > 不存在的值不应该与存在的值共享同一个类型空间。
    >
    > 使“不存在”成为类型系统的一部分，而非运行时的一个潜在 bug。

28. `match`语法让处理Option变得稍微不那么复杂：

    ```rust
    fn plus_one(x: Option<i32>) -> Option<i32> {
        match x {
            None => None,
            Some(i) => Some(i + 1), // 如果不为None，则将Some中绑定的数据赋予i
        }
    }

    let five = Some(5);
    let six = plus_one(five);
    let none = plus_one(None);
    ```

    `match`语法要求“穷尽”所有情况，因此必须给出所有枚举值的处理方式（或者提供通配），对于一个Option，必须要显式处理None的情况。

    `match`的匹配规则是从上至下，当遇到了一个不属于该枚举值或该类型可能值的名称时，认为这是一个“通配”。此时rust将match的变量传递给通配制定的变量，加入通配处理的上下文中，由通配的处理逻辑进行处理。

    ```rust
    let dice_roll = 9;
    match dice_roll {
        3 => add_fancy_hat(),
        7 => remove_fancy_hat(),
        other => move_player(other),
    }
    ```

    其中`other`能是任意字符，只要不满足任何匹配规则。同时也可以使用`_`表示舍弃通配的变量。

29. 当我们只关心某些枚举值时，`match`要求的穷尽规则显得有些冗余。所以rust提供了`if let和let else`语法糖来简化代码

    下面两个代码是等价的

    ```rust
    let mut count = 0;
    match coin {
        Coin::Quarter(state) => println!("State quarter from {state:?}!"),
        _ => count += 1,
    }
    ```

    ```rust
    let mut count = 0;
    if let Coin::Quarter(state) = coin {
        println!("State quarter from {state:?}!");
    } else {
        count += 1;
    }
    ```

    有一说一，好像没有简化多少，总之rust允许我们这么做，显式地选择某些我们关心的枚举值进行处理。还有个`let else`语法，说实话感觉没啥用处，徒增了复杂性，有兴趣自己看看吧。（https://kaisery.github.io/trpl-zh-cn/ch06-03-if-let.html）

30. crate 是 Rust 在编译时最小的代码单位。crate的直译是“箱子、板条箱”，cargo的直译是“货物”，所以cargo就是将这些“箱子”打包成“货物”的命令。

31. crate分为二进制crate和库crate，区分方式在于是否提供了main函数作为可执行文件的入口。大部分场景下，crate指的是库crate，也就是别的语言中的library概念。

    包（package）是一个或多个crate的集合，包拥有`Cargo.toml`文件，阐述如何去构建这些crate。

    **包最多只能包含一个库crate，可以包含任意个二进制crate，至少要包含一个crate**

32. 每个crate都有一个`crate root`，编译器以它为起点编译出crate根模块。按照约定，`src/main.rs`作为**与包同名的二进制crate**的crate root。`src/lib.rs`作为**与包名同名的库crate**的crate root。

    前面说过，包能够包含多个二进制crate，通过将文件放在 src/bin 目录下，一个包可以拥有多个二进制 crate：每个 src/bin 下的文件都会被编译成一个独立的二进制 crate。

33. rust的包管理要求包的创建者主动设计并提供各个依赖的层级关系。

    具体来说，rust使用树状结构维护模块关系，并要求父模块主动申明包含了子模块。从结构上看rust还是使用了以文件系统为基础的包划分关系。

    假设一个场景，学校。按照树状规则，rust的依赖组织可以按照这两种方式进行

    ```
    +---------------------------+       +---------------------------+
    |                           |       |                           |
    | .                         |       | .                         |
    | ├── Cargo.lock            |       | ├── Cargo.lock            |
    | ├── Cargo.toml            |       | ├── Cargo.toml            |
    | └── src                   |       | └── src                   |
    |     ├── main.rs           |       |     ├── main.rs           |
    |     ├── school            |       |     └── school            |
    |     │   └── people.rs     |       |         ├── mod.rs        |
    |     └── school.rs         |       |         └── people.rs     |
    |                           |       |                           |
    +---------------------------+       +---------------------------+
    ```

    `main.rs`的内容为

    ```rust
    mod school; // 声明使用school子模块，对应目录中的school内容

    use crate::school::people::Student;

    fn main() {
        let p = Student {
            name: String::from("naro"),
            age: 21,
        };

        p.whoami();
    }
    ```

    `school.rs`（同`mod.rs`)的内容为

    ```rust
    pub mod people; // 声明包含子模块 people，并且该子模块是允许外部访问的
    ```

    `people.rs`的内容为

    ```rust
    pub struct Student {
        pub name: String,
        pub age: u32,
    }

    impl Student {
        pub fn whoami(&self) {
            println!("name: {}, age: {}, goodbye!", self.name, self.age);
        }
    }
    ```

    总结下来，子模块**文件夹**和父模块的**源文件**在文件系统中同级。

    同时要求 在模块文件夹的同级有和模块同名的rs文件 或 在模块的文件夹中存在mod.rs 来描述该模块对外可见的情况

    按照这个层级关系，就可以在`main.rs`中合法使用`crate::school::people::Student`来使用`Student`类型。

34. 库crate要求在`src/lib.rs`中主动定义库对外的模块和相关函数

    ```rust
    mod front_of_house {
        mod hosting {
            fn add_to_waitlist() {}

            fn seat_at_table() {}
        }

        mod serving {
            fn take_order() {}

            fn serve_order() {}

            fn take_payment() {}
        }
    }
    ```

35. 子模块可以使用`super`作为路径的前缀在依赖关系中代表父模块的路径

    比如，下面中，`back_of_house`作为当前上下文的一个子模块，在其中可以使用`super`来代表父模块即当前上下文，以此引用到当前上下文中的`deliver_order`
    
    ```rust
    fn deliver_order() {}

    mod back_of_house {
        fn fix_incorrect_order() {
            cook_order();
            super::deliver_order();
        }

        fn cook_order() {}
    }
    ```

    这个需求在同级模块之间互相调用的情况下还是挺常见的

36. 结构体类型的成员默认为私有的，而枚举类型的成员默认为公有。

37. 使用`use`创建一个名称的别名，默认情况下使用最后一个名称作为`use`创造的缩写，也可以用`use ... as`的方法来自定义名称

    ```rust
    use std::fmt::Result;
    use std::io::Result as IoResult;

    fn function1() -> Result {
        // --snip--
    }

    fn function2() -> IoResult<()> {
        // --snip--
    }
    ```

38. 使用绑定数据的枚举，就可以将任意类型的数据存放到一个vector中，感觉有些黑科技。这是否说明绑定数据的枚举的结构实际上是{类型标识, 数据指针}这样？

    感觉具体看情况吧，大部分情况下应该是类似union那样向最大的项对齐，在某些情况才是{类型表示，数据指针}

    ```rust
    enum SpreadsheetCell {
        Int(i32),
        Float(f64),
        Text(String),
    }

    let row = vec![
        SpreadsheetCell::Int(3),
        SpreadsheetCell::Text(String::from("blue")),
        SpreadsheetCell::Float(10.12),
    ];
    ```

39. rust内置的vector和C++一样提供下标和函数两种风格的成员访问，但在rust中，两者行为不等价

    - nums[index]：
        
        和常见的下标访问一致，当越界时rust自动panic

    - nums.get(index):
        
        返回一个Option值，当越界时返回None。
    
    可以根据情况选择

40. 
