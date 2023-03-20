package jvm;

import org.junit.Test;

/**
 * 运行时数据区
 * 1. 基本组成
 *      * 本地方法栈（Native Method Stacks）
 *      * 程序计数器（Program Counter Register）
 *      * 虚拟机栈（jvm.JVM Stacks）：运行时单位
 *      * 堆区（Heap）：存储单位
 *      * 元数据区（Metaspace）
 * 2. 程序计数器（Program Counter Register）
 *      * 对于普通CP寄存器的一种抽象模拟
 *      * 每个线程都有自己的程序计数器，线程私有，与线程的生命周期保持一致
 *      * 程序计数器存储当前线程只在执行JAVA方法的JVM指令地址/native方法/位置定制
 *      * 字节码解释器通过改变程序计数器来依次读取指令
 * 3. 本地方法栈（Native Method Stacks） native关键字
 *      * 一个Native Method是一个Java调用非Java代码的接囗
 *      * Java应用需要与Java外面的硬件环境交互，这是本地方法存在的主要原因，例如：java.lang.Thread的setPriority()方法
 *        是用Java实现的，但是它实现调用的是该类里的本地方法setPriority0()
 *      * Java虚拟机栈于管理Java方法的调用，而本地方法栈用于管理本地方法的调用，在Execution Engine 执行时加载本地方法库
 *      * 当某个线程调用一个本地方法时，它就进入了一个全新的并且不再受虚拟机限制的世界，它和虚拟机拥有同样的权限
 * 4. 虚拟机栈（jvm.JVM Stacks）
 *      * 数据以栈帧（Stack Frame）形式存在,对应着一次次的Java方法调用
 *      * 固定大小（StackoverflowError）或者动态扩展（OutOfMemoryError）
 *      * 栈帧的内部结构：
 *          * 局部变量表：存储方法内的局部变量，大小在编译器确定，每个局部变量对应表中的一个slot，this存储slot=0的位置
 *          * 操作数栈：保存计算过程中的中间结果，作为计算过程中变量临时的存储空间
 *              * 栈顶缓存（Top-of-Stack Cashing）：将操作数栈栈顶元素缓存在CPU的寄存器中，降低对内存的读写次数
 *          * 动态链接：一个指向运行时常量池中该栈帧所属方法的引用
 *              * 静态/动态链接：是否在编译器将符号引用转化为直接引用
 *              * 早期/晚期绑定：编译时替换/运行时替换
 *              * 动态/静态语言：对类型的检查实在编译器还是运行期,动态->变量没有类型，变量值有类型
 *              * 动态分派：重写基于动态分派,虚拟机首先找到操作数栈顶第一个元素所指向的对象的实际类型，在类的元数据中寻找对应的方法
 *                  * 为了解决动态分派频繁方法的问题，，JVM在类的方法区建立一个虚方法表（virtual method table）
 *                  * 每个类都有虚方法表，存储各个方法的实际入口，在类加载链接阶段初始化创建
 *          * 方法返回地址：方法正常退出时，调用者的pc计数器的值作为返回地址，即调用该方法的指令的下一条指令的地址；异常根据异常处理表跳转指令
 *          * 其他附加信息
 *      * 垃圾回收并不会涉及到虚拟机栈
 * 5. 堆区（Heap）
 *      * 所有的对象实例以及数组都应当在运行时分配在堆上
 *      * 堆空间又细分为:
 *          * Java7及之前：新生区（Young Generation Space,又被划分为Eden区和Survivor区）+养老区(Old generation space)+永久区
 *          * Java8:新生区（Young Generation Space）+养老区(Old generation space)+元空间(Meta Space)
 *              * 新生区 = Eden（8） + Survivor 0（1） + Survivor 0（1）, 存储生命周期较短的瞬时对象
 *              * 老年区，存储生命周期较长的对象
 *              * 新生区：老年区 = 1：2
 *      * 设置堆内存：-Xms用于表示堆区的起始内存，-Xmx 则用于表示堆区的最大内存
 *          * 初始内存大小为：电脑内存/64，最大内存大小为：电脑内存/4
 *      * 堆内存不够会抛出java.lang.OutOfMemoryError: Java heap space 异常
 *      * 堆内存空间分配过程（对象创建）
 *          1. 首先在eden区创建
 *          2. 若eden区空间满，则由JVM对elden+Survivor 0区进行垃圾回收（MinorGC），存活的对象移动到Survivor 1区，对象年龄加1
 *          3. 下次进行GC时S1区往S0区移动（即SO和S1身份不断互换）
 *          4. 随着不断的进行垃圾回收，当Survivor区中对象年龄达到15时，触发一次Promotion操作，晋升到老年区中
 *          * 分配整体思路：eden放不下->MinorGC，还是放不下->老年区放不下(Major GC)->FullGC,还是放不下(整个堆和方法的垃圾回收)->OOM
 *      * 涉及到的几种GC类型
 *          * Young/Minor GC:当Eden满时，触发MinorGC,触发频繁，回收速度较快，但是导致其他线程暂停
 *          * Major GC:发生在老年区的GC，比Minor GC满10倍以上
 *          * Full GC: 老年代空间不足/方法区空间不足/调用System.gc()时,出发Full GC执行
 *     * TLAB(Thread Local Allocation Buffer)
 *          * JVM为每个线程分配了一个私有缓存区域，它包含在Eden空间内,多线程同时分配内存时，使用TLAB可以避免一系列的非线程安全问题
 *          * 每个线程都有一个TLAB空间,当一个线程的TLAB存满时，可以使用公共区域
 *          * 哪个线程要分配内存，就在哪个线程的本地缓冲区中分配，只有本地缓冲区用完,分配新的缓存区时才需要同步锁定
 *      * 常用参数
 *          * -XX:+PrintFlagsInitial : 查看所有的参数的默认初始值
 *          * -XX:+PrintFlagsFinal  ：查看所有的参数的最终值（可能会存在修改，不再是初始值）
 *          * -Xms：初始堆空间内存 （默认为物理内存的1/64）
 *          * -Xmx：最大堆空间内存（默认为物理内存的1/4）
 *          * -Xmn：设置新生代的大小。(初始值及最大值)
 *          * -XX:NewRatio：配置新生代与老年代在堆结构的占比
 *          * -XX:SurvivorRatio：设置新生代中Eden和S0/S1空间的比例
 *          * -XX:MaxTenuringThreshold：设置新生代垃圾的最大年龄
 *          * -XX:+PrintGCDetails：输出详细的GC处理日志
 *          * 打印gc简要信息：① -XX:+PrintGC   ② -verbose:gc
 *          * -XX:HandlePromotionFailure：是否设置空间分配担保
 *      * 逃逸分析：分析对象引用的使用范围，决定是否将该对象分配到堆上（分析对象作用域）
 *          * 一个对象在方法中被定义后，只在方法内部使用，认为没有逃逸，它被外部方法所引用，则认为发生逃逸（返回值，其他函数调用的参数）
 *          * 在JDK 1.7 版本之后，HotSpot中默认就已经开启了逃逸分析
 *          * 确定对象为非逃逸对象后，编译器可以对代码进行优化：
 *              1. 堆上分配->栈上分配：避免垃圾回收
 *              2. 同步省略（锁消除）：确定对象只能由一个线程访问后，省略并发线程访问的同步操作（synchronized的单个访问）
 *              3. 分离对象或标量替换：将对象拆解为若干个成员变量(原始数据类型)替代（point(x,y)->(point.x + point。y)）
 *          * 存在的问题：逃逸分析的消耗可能大于避免堆内存开辟节省的消耗
 * 6. 方法区
 *      * 逻辑上属于堆的一部分，JDK7之前堆空间中的永生带，JDK8之后堆空间的元数据区，可以看作独立于JAVA堆的内存空间（没有GC）
 *      * 用于存储已被虚拟机记载的:类型信息（类型修饰符，接口列表，全类名）、域信息（成员变量）、方法信息、静态变量、全局常量（static final）
 *      * 常量池：各种字面量和对类型、域和方法的符号引用（不同类型），例如字符串常量池
 *      * JDK1.7 存储在堆中永生代；JDK1.8,无永久代，存储在本地内存的原空间中（降低受JVM限制OOM的问题；直接内存IO性能更高）
 *      * 方法区的垃圾回收：常量池中废弃的常量 + 不再使用的类型
 *          * 常量回收类似于JAVA堆中对象的回收
 *          * 类卸载：判断类是否废弃（实例被回收，加载器被回收，.class对象不被引用）-> 可以回收
 *          * 基于类卸载解决大量使用反射、动态代理、CGLib等字节码框架对于方法带来的内存压力
 * 2. JVM线程
 *      * 每个线程与操作系统本地线程直接映射
 *      * 系统线程：虚拟机线程，周期任务线程，GC线程，编译线程，信号调度线程
 * */

public class MemoryStructure {

    /**
     * 获取系统中的堆内存大小
     * */
    @Test
    public void getHeapSize(){
        long initialMemory = Runtime.getRuntime().totalMemory()/ 1024 /1024;
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 /1024;
        System.out.println("initial memory:"+ initialMemory);
        System.out.println("initial memory:"+ maxMemory);
        System.out.println("initial memory:"+ maxMemory / 16);
    }
}
