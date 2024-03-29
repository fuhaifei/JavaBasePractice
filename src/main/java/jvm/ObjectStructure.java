package jvm;

import org.junit.Test;

/**
 * 1. JAVA对象实例化和内存布局
 *      * 对象创建步骤
 *          （1）对象对应类的加载（加载->链接->初始化）
 *              * 首先判断能够在Metadata区域找到对应类文件，找不到则执行类加载步骤
 *          （2）为对象分配内存
 *              * 规整（指针碰撞）,不规整（基于空闲列表）
 *          （3）处理并发问题：在Eden区为每个线程分配一块区域
 *          （4）初始化空间：书幸福默认值
 *           (5)设置对象的对象头：将对象的所属类（即类的元数据信息）、对象的HashCode和对象的GC信息、锁信息等数据存储在对象的对象头中
 *          （6）执行init方法进行初始化（代码块初始化->构造器初始化），并把堆内对象的首地址赋值给引用变量
 *      * 对象内存布局
 *          * 对象头：运行时元数据（Mark Word） + 类型指针 + 数组长度（只有数组类型由）
 *              * 运行时元数据： 哈希值（HashCode）、GC 分代年龄、锁状态标志、线程持有的锁、偏向线程 ID、翩向时间戳
 *              * 类型指针：    指向Metadata中类信息的指针
 *          * 实例数据：对象真正存储的有效信息，包括程序代码中定义的各种类型的字段
 *          * 对齐填充
 *      * 对象访问
 *          * 句柄访问：在堆中开辟句柄池，放置对于对象的引用，栈中引用变量引用句柄池句柄（二次定位）
 *          * 直接访问：栈中引用变量直接指向堆中地址
 *      * 运行常量池指的是jvm运行中，将编译后的类放在metaspace区，
 *              具体包括class文件辕信息描述、编译后的代码数据、引用类型数据、类文件常量池等。
 * 2. 执行引擎
 *      * 将字节码指令解释/编译为对应平台上的本地机器指令，充当了将高级语言翻译为机器语言的译者
 *      * 分为两种执行模式：解释器 和 JIT编译器（HotSpot VM 采用解释器与即时编译器并存的架构）
 *          * 解释器：逐条将字节码翻译成机器指令执行
 *              * 字节码解释器 vs 模板解释器
 *              * 由Interpreter模块（解释器核心功能）和Code模块（管理本地机器指令）组成
 *          * JIT（just in time）编译器：整体编译，一次执行
 *      * 为什么要采取解释器和及时编译的双重架构
 *          * 即时编译会导致程序启动时需要花费更多的时间启动，解释器执行缓慢
 *          * 取得一个即使和逐条的折衷
 *          * 解释器可以首先发挥作用，而不必等待即时编译器全部编译完成后再执行，这样可以省去许多不必要的编译时间。
 *              随着时间的推移，编译器发挥作用，把越来越多的代码编译成本地代码，获得更高的执行效率
 *      * JIT编译器分为两部分
 *          * Client Compiler（C1编译器）：运行在Client模式下JVM调用
 *              * C1编译器会对字节码进行简单和可靠的优化，耗时短，以达到更快的编译速度(方法内联，去虚拟化，元余消除)
 *          * Server Compiler（C2编译器）：运行在Server模式下JVM调用
 *              * C2进行耗时较长的优化，以及激进优化，但优化的代码执行效率更高(标量替换，栈上分配，同步消除）
 *      * 三个编译概念
 *          * 前端编译器：.java->.class,例如Sun的javac
 *          * JIT编译器：.class->机器码，HotSpot VM的C1、C2编译器。
 *          * 静态提前编译器（AOT 编译器）：.java->机器码。GNU Compiler for the Java（GCJ
 *      * 分层编译：程序解释执行（不开启性能监控）可以触发C1编译，将字节码编译成机器码，可以进行简单优化，
 *                  也可以加上性能监控，C2编译会根据性能监控信息进行激进优化
 *          * Java7版本之后，一旦开发人员在程序中显式指定命令“-server"时，
 *              默认将会开启分层编译策略，由C1编译器和C2编译器相互协作共同来执行编译任务。
 *      * Graal编译器：JDK10起，HotSpot加入的一个全新的即时编译器
 *      * AOT编译器：jdk9引入了AoT编译器
 *          * 直接将.java编译为二进制库，直接执行
 *          * 破坏了 java “ 一次编译，到处运行”，降低了Java链接过程的动态性目前只支持Linux X64 java base
 * 3. String
 *      * 底层存储结构
 *          * JDK8及以上版本：private final char[] value;(char两个字节)
 *          * JDK9及以后版本：private final byte[] value(byte一个字节) + private final byte coder（标记编码Latin-1/UTF-16）;
 *              * 在只用英文常见字母时使用latin-1（1byte）节省存储空间；出现latin-1无法容纳字符时，使用UTF-16（2/4）
 *              * 之所以不使用utf8的原因在于utf8不定长（1-4字节），虽然长度短但是不适合连续存储
 *          * 为了解决字符串占用大量堆空间的问题，从应用收集到的数据表明大多数字符为Latin-1,只占用一个字符
 *          * 解释：<a href="https://www.zhihu.com/question/447224628">...</a>
 *      * 字符串常量池(StringTable)
 *          * StringTable是一个哈希表，key为字符串，value为字符串在堆中的地址
 *      * 字符串常量池的位置
 *          * JDK6及之前，字符串常量池存放在永久代
 *          * JDK7将字符串常量池调整到了堆中（永生代空间较小，且垃圾回收频率较低）
 *      * 字符串拼接与常量池
 *          * 常量与常量的拼接在常量池中（编译器优化）
 *          * 只要参与拼接的有一个是变量，结果就在堆中
 *          * 对于不可编译优化的字符串拼接，底层基于StringBuilder实现（在jdk5.0之前使用的是StringBuffer）
 *      * intern()方法：返回一个常量池中指定字符串的地址，没有则创建后返回
 *      * JDK7以后常量池会指向堆中的字符串对象（s.intern():有则返回，无则指向当前堆中字符串对象）
 *      * new String("xxx"): 首先在堆中创建String对象，再在字符串常量池中创建对应的String对象
 *      * 常量池 vs 运行时常量池 vs 字符串常量池
 *          * 常量池：字节码文件中的一个列表，存储字面量和堆类型、领和方法的符号引用（降低空间占用）
 *              * 数量值，字符串值，类引用，字段引用，方法引用
 *          * 运行时常量池：字节码文件加载运行后常量池->运行时常量池
 *              * 不再是常量池中的符号地址了，这里换为真实地址
 * */
public class ObjectStructure {


    @Test
    public void testString(){
        String s1 = "na";
        String s3 = new String("nanb");
        System.out.println(s3 == s3.intern());

        String s4 = s1 + "nb";
        System.out.println(s4 == s3.intern());

        String s = new String("a") + new String("b");
        String c = "ab";
        String s5 = s.intern();
        System.out.println("ab" == s);
        System.out.println("ab" == s5);

        String str1 = new String("aa");

        str1.intern();

        String str2 = "aa";

        System.out.println(str1 == str2);

        StringBuilder s6 = new StringBuilder();
        s6.toString();
    }
}
