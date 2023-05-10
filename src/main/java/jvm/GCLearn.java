package jvm;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * JVM垃圾回收相关知识总结
 * 1. 基础总结
 *      * 什么是垃圾：在运行程序中没有任何指针指向的对象
 *      * 垃圾回收：回收内存中的空指向对象，整理内存碎片
 *      * C++中的手动垃圾回收：使用new关键字进行内存申请，delete关键字进行内存释放
 *      * 手动回收的问题：容易导致内存泄漏
 *      * java自动垃回收：重点关注堆区的回收
 *      * 垃圾回收频率：Young区>Old区>元数据区
 * 2. 标记阶段-死亡对象标记
 *      * 引用计数算法：对每个对象保存一个整型的引用计数器属性，用于记录对象被引用的情况，当记录为0时，标记为对象死亡。
 *          * 优点：实现简单，垃圾对象容易辨识；判定效率高，回收没有延迟性
 *          * 缺点：额外计数器属性增加了存储空间的开销；更新计数器，增加了时间开销；无法处理循环引用（导致无法回收）
 *      * 可达性分析：从跟对象集合（GCRoots）促发，按照从上至下的方式搜素目标对象是否可达，只有与根对象集合直接或简介相连的对象才是存活对象
 *          * GC Roots:虚拟机栈引用的对象，本地方法引用的对象（JNI），方法区常量引用的对象，虚拟机内部引用（指针指向堆内存中的对象，但是本身
 *                     不在堆中）（全局性的引用（例如常量或类静态属性）与执行上下文（例如栈帧中的本地变量表））
 *          * 可达性分析时导致GC停顿的主要原因（Stop the world）
 *      * 对象的finalization机制：标记死亡对象在被垃圾回收前，总是会先调用该对象的finalize()方法
 *          * finalize() 方法允许在子类中被重写，用于在对象被回收时进行资源释放（关闭文件/释放连接等）
 *          * 永远不要主动调用finalize()方法，因为对象回收是由垃圾回收器负责的
 *          * finalize()可能导致对象复活，因此虚拟机将对象分为可触及的、可复活的、不可触及的三种状态，垃圾回收值只回收不可触及状态的对象
 *       * 具体过程-二次标记
 *          * 对象没有引用链，进行第一次标记
 *          * 进行筛选判断是否需要执行finalize()方法：
 *              * 若未重写finalize方法，直接判定为不可触及；
 *              * 若重写了finalize方法，且还未执行，则将对象插入到F-Queue中，等待触发器finalize()方法
 *          * 若finalize()使得对象与其他对象建立联系，移动出队列，直到第二次没有引用链存在的情况，直接标记为不可触及，不再调用finalize()方法
 * 3. 清除阶段
 *      * 标记-清除算法(Mark-Sweep)
 *          * 当可用内存空间被耗尽时，暂停整个程序进行垃圾祸首
 *          * 标记：从引用根节点开始遍历，标记所有被引用的对象
 *          * 清除：对堆内存进行从头到尾遍历，若发现某个对象没有可被标记未可达对象，则将其回收
 *          缺点：遍历效率差；垃圾回收需要暂停整个应用程序；清理出的内存是不连续的，碎片化的
 *      * 复制算法（Copying）
 *          * 将内存空间分为两块，每次使用一块，垃圾回收时将一块中的存活对象复制到另一块，对当前块进行整体清除
 *          * 优点：保证空间的连续性，不会出现碎片问题
 *          * 缺点：需要两倍的内存空间
 *          * 应用场景：系统中垃圾对象较多的场景，此时复制的存活对象较少，效率较高；新生代的垃圾回收
 *      * 标记压缩/整理算法（Mark-Compact）
 *          * 第一阶段：标记被引用对象；第二阶段：将存活对象压缩到内存的一段，清理边界外的所有空间
 *          * 相当于标记清除算法+内存整理
 *          * 优点：解决了内存碎片的问题，消除了复制算法中内存减半的问题
 *          * 缺点：移动对象还需要移动引用地址；移动过程在暂停整个应用程序
 *       * 三个回收算法总结
 *          * 速度：复制>标记清除>标记整理
 *          * 空间开销：复制>标记清除>标记整理
 *       * 分代收集算法
 *          * 针对不同生命周期的对象，采用不同的垃圾回收策略（新生代和老年代）
 *                  * 年轻代：区域较小、生命周期短、存活率低、回收频繁。->基于复制的垃圾回收算法
 *                  * 老年代：生命周期长、区域大->标记清除/标记压缩/混合
 *          * Java中的实现
 *              * 新生代GC：大多数对象存放在Eden区（大对象直接进入老年区），每次GC将Eden+Survivor区存活对象移动到另一个Survivor区中，当
 *                         对象年龄超过年龄阈值，移动到老年代（Minor GC）
 *              * 老年代：Marjor GC/Full Gc,回收频率较低，采用标记-整理算法
 *              * 分配担保机制：当极端情况下可能会出现超过10%的对象存活,导致MinorGC复制空间不够，此时直接进入老年代
 *       * 增量收集算法：实时垃圾回收
 *              * 每次垃圾收集线程只收集一小片区域的内存空间，接着切换到应用程序线程。依次反复，直到垃圾收集完成
 *              * 基础还是标记清除/复制算法
 *       * 分区算法：将整个堆空间划分成连续的不同小区间。每一个小区间都独立使用，独立回收
 * 4. JVM GC执行的时机
 *      * 创建对象时首先分配到Elden区中，若Elden区空间不足，则执行一次young GC（Elden+s0存活对象移动到s1中）
 *      * young GC发现Survivor中无法存放移动来的对象，通过分配担保机制提前移动到老年代中
 *      * 分配担保机制：若 老年代连续空间大小>新生代对象总大小/历次晋升平均大小，则正常进行Young GC。否则直接进行full GC
 *      * full GC后发现空间仍然无法存储全部对象，抛出：java.lang.OutOfMemoryError: Java heap space
 *      * 其他时机：大对象直接存储在老年代；年龄超过上限的对象在GC过程中移动到老年代
 * 5. 跨代引用的问题
 *      * 新生代对象可能会被老年代对象所引用，新生代触发GC时，只扫描牺牲带区域不够
 *      * Java定义了名为记忆集的抽象数据结构，用于记录存在跨区域引用的对象指针集合
 *      * 卡表（Card Table）实现记忆集：卡表由一个数组构成，每一个元素都对应着一块特定大小的内存区域，这块内存区域被称之为卡页（Card Page），
 *                                  每一个卡页，可能会包含N个存在跨区域引用的对象，只要存在跨区域引用的对象，这个卡页就会被标识为1
 *      * 在GC扫描时将标记为1的卡页加入扫描范围
 * 6. 常用的垃圾回收期
 *      * Serial(-xx:+UserSerialGC):Serial 是Java虚拟机初代收集器，单线程工作的收集器。在进行垃圾回收的时候，
 *                                  需要暂停所有的用户线程，直到回收结束
 *          * 使用复制标记-复制算法
 *          * Hotspot运行在客户端模式下的默认新生代收集器
 *          * JDK1.3之前新生代收集器的唯一选择
 *      * Serial Old（-XX:+UseSerialOldGC）：Serial收集器的老年代版本
 *          * 使用标记整理算法
 *          * Hotspot在客户端模式下的默认老年代收集器
 *          * Hotspot在客户端模式下：作为CMS收集器在出现并发模式故障（Concurrent Mode Failure） 时的后备收集器
 *      * ParNew（-XX:+UseParNewGC）：Serial的多线程版本
 *          * 采用标记-复制算法的新生代垃圾回收器
 *          * JDK9之后，ParNew只能和CMS搭配使用了
 *      * Parallel Scavenge（-XX:+UseParallelGC）：可控制吞吐量的类似ParNew回收器
 *          * 采用标记-复制算法的新生代垃圾回收器
 *          * Parallel Scavenge 收集器提供了一些参数，给用户按自身需求控制吞吐量
 *      * Parallel Old（-XX:+UseParallelOldGC）：Parallel Scavenge的老年代版本
 *          * 采用标记-整理算法的老年代垃圾回收器
 *          * JDK8的默认垃圾收集器组合：Parallel Scavenge + Parallel Old
 *      * CMS （-XX:+UseConcMarkSweepGC） 是JDK1.4后期推出的GC收集器，它是一款并发低停顿的收集器
 *          * 采用标记-清除算法的老年代垃圾回收器
 *          * 垃圾收集步骤
 *              * 1.初始标记：需要STW，只标记与GC Roots直接关联的对象
 *              * 2.并发标记：不需要STW，遍历标记整个引用链，可以与用户线程并发执行
 *              * 3.重新标记：需要STW，标记之前并发标记时用户线程增加的垃圾
 *              * 4.并发清理：不需要STW，遍历老年代空间，清理可回收对象，重置CMS收集器的数据结构，等待下一次回收
 *          * 问题：采用标记清理算法导致大量内存碎片；GC线程并发降低吞吐量；浮动垃圾，并发清理过程中伴随着垃圾的增加只能等到下一次垃圾回收
 *              (jdk5 68%, jdk6 92%)
 *          * CMS维护一个老年代到新生代的cardtable, 将所有新生代对象加入CG Roots解决跨代引用问题
 *          * CMS的逻辑
 *              * 当老年代达到预先设定的存储空间阈值(jdk5 68%, jdk6 92%)是，触发Major GC
 *              * 当出现 concurrent promotion failed（空间分配担保机制下空间不足） 或者 concurrent mode failure（并发标记空间）
 *                触发Full GC(Serial Old GC)
 *       * Garbage First(G1):停顿时间可控的低延迟垃圾收集器,JDK9 的时候成为了服务端模式下的默认垃圾收集器
 *          * 将堆区域花费为多个大小相同的区域（Region）,每一个Region都可以根据运行情况的需要，扮演Eden、Survivor、老年代区域、或者Humongous区域
 *          * 大对象会存放到多个连续的Humongous区域，G1大多数情况下会把这个区域当作老年代来看待
 *          * 垃圾收集步骤
 *              * 1.年轻代GC：STW,GC创建回收集合（需要被回收内存分段的集合）
 *              * 2. 并发标记过程：
 *                  * 1.初始标记（Initial Marking）需要STW，只标记与GC Roots直接关联的对象
 *                  * 2.并发标记（Concurrent Marking）：不需要STW，基于引用链遍历整个堆，找出存活的对象（并发）
 *                  * 3.最终标记（Final Marking）：需要STW，标记之前并发标记时用户线程增加的垃圾
 *                  * 4.独占清理（Live Data Counting and Evacuation）：需要STW，计算各个区域的存辉对象和GC回收比例，排序
 *                  * 5.并发清理：识别并清理完全空闲的区域
 *              * 3. 混合回收过程（Mixed GC）:混合回收的回收集（Collection Set）包括八分之一的老年代内存分段，Eden区内存分段，Survivor区内存分段。
 *              * 4. Full GC(可选)：如果上述方式不能正常工作，G1会停止应用程序的执行（Stop-The-World），使用单线程的内存回收算法进行垃圾回收
 *                  计算出各个Region的回收价值和成本，再根据用户期望的停顿时间来决定要回收多少个Region
 *              * 回收采用标记复制算法
 *        * G1的逻辑
 *              * Elden区满了触发young GC
 *              * VM已使用内存/总内存”的比例超过设定阈值IHOP（InitiatingHeapOccupancyPercent）后，
 *                  G1会执行一次Concurrent Marking Cycle,阶段2并发标记阶段并发标记
 *              * 之后执行多轮的Mixed GC(年轻代和老年代Region回收)：为了满足所谓的暂停时间可控
 *              * Concurrent Mode Failure时通过Serial Old实现Full GC
 * 7. 安全点（Safepoint）和安全区域(SafeRegion)
 *      * 程序执行时并非在所有地方都能停顿下来开始GC，只有在特定的位置才能停顿下来开始GC，这些位置称为"安全点"
 *      * 如果太少可能导致GC等待的时间太长，如果太频繁可能导致运行时的性能问题,大部分指令的执行时间都非常短暂，
 *        通常会根据“是否具有让程序长时间执行的特征”为标准,如方法调用、循环跳转和异常跳转等
 *      * 抢先式中断（中断所有线程，没跑打safepoint的线程继续跑）/主动式中断（线程到达safepoint,检查中断标志是否需要暂停）
 *      * 安全区域是指在一段代码片段中，对象的引用关系不会发生变化，在这个区域中的任何位置开始GC都是安全的（解决程序不执行时的GC）
 * 8. 强引用/软引用/弱引用/虚引用
 *      * 强引用（Strong Reference）:要强引用关系还存在，垃圾收集器就永远不会回收掉被引用的对象(new 创建对象)
 *      * 软引用（Soft Reference）: 在系统将要发生内存溢出之前，将会把这些对象列入回收范围之中进行第二次回收。
 *                                如果这次回收后还没有足够的内存，才会抛出内存溢出异常（高速缓存）
 *            * SoftReference<Object> sf = new SoftReference<>(obj);
 *      * 弱引用（WeakReference）：被弱引用关联的对象只能生存到下一次垃圾收集之前。
 *                               当垃圾收集器工作时，无论内存空间是否足够，都会回收掉被弱引用关联的对象。
 *            * WeakReference<Object> sf = new WeakReference<>(obj);
 *      * 虚引用（Phantom Reference）：一个对象是否有虚引用的存在，完全不会对其生存时间构成影响，
 *            * 无法通过虚引用来获得一个对象的实例。
 *            * 为一个对象设置虚引用关联的唯一目的就是能在这个对象被收集器回收时收到一个系统通知。
 *            * ReferenceQueue phantomQueue = new ReferenceQueue();
 *            * PhantomReference<Object> sf = new PhantomReference<>(obj, phantomQueue);
 * 9. CGRoots：可达性分析中的根节点
 *      * 由系统类加载器加载的对象
 *      * 线程相关变量：线程/栈中的对象
 *      * 本地方法调用相关对象：JNI栈中对象和全局对象
 *      * 用于同步的锁对象
 *      * JVM本身持有的对象：类加载器，重要的异常类，处理异常的与分配对象
 * 10. JAVA程序内存泄露分析
 *      * 使用到的相关工具
 *          * pmap：显示进程的内存映像，linux命令
 *          * NMT（Native Memory Tracking）用来跟踪 JVM 本地内存分配情况
 *              * 通过命令开启：-XX:NativeMemoryTracking=off|summary|detail
 *              * jmcd命令：查看当前JMV各个内存区域占用空间大小（堆/线程/GC/代码缓存/）
 *              * baseline summary.differ 查看一段时间的内存变化
 *          * jps：查看当前服务器运行的java程序（pid，jar包名称等）
 *          * jmap（Java Virtual Machine Memory Map）JDK提供的一个可以生成Java虚拟机的堆转储快照dump文件的命令行工具
 *              * jmap [optins] pid包括：
 *                  -heap 查看当前堆信息（垃圾回收/配置/内存空间使用）
 *                  -histo[:live] 显示堆中对象的统计信息
 *              * -dump:[live,]format=b,file= 生成Java虚拟机的堆转储快照dump文件
 *          * jstat：对Heap size和垃圾回收状况的监控
 *          * jstack:jvm中当前所有线程的运行情况和线程当前状态
 *       * 基本思路为
 *          * top命令查看当前占用内存空间较高的应用程序(pid)
 *          * jmap 获得当前堆内存使用情况，如果堆内存
 * 11. JVM调优基本思路
 *      * 主要目标减少GC次数和GC停顿时间
 *      * 具体的步骤包括
 *          1. 定位问题
 *              * 查看GC日志判断Young GC和Full GC频率
 *              * 查看GC停顿时间
 *          2. 调整参数/垃圾回收器
 *      * 不同问题的解决方案
 *          1. Young GC次数过于频繁：年轻代空间过小
 *          2. Young GC停顿时间过长：是否对象不符合“照生夕死”的特征，降低转移到老年代的年龄限制
 *          3. Full GC次数过于频繁：老年代空间过小
 *          4. Full GC停顿时间过长：老年代空间过大/垃圾回收器不合适（看CPU负载，如果较低，可改为低延迟回收期）
 * 12. 永久代被替代的原因
 *     * JVM加载类的方法的大小很难确定-永久代启动时即确定大小，导致永久代容易出现OOM问题
 *     * 元空间是存储在本地内存里面，内存上限比较大，可以很好的避免这个问题
 *     * 永久代对象通过FullGC和老年代同时进行垃圾回收，转移到元空间后，简化了full GCd的逻辑
 *     * Oracle合并Hotspot和JRockit的代码，JRockit没有永久代
 *     * HotSpot的内部类型也是Java对象不应该对用户透明
 * 13. 可达性分析：三色标记法
 *     * 定义：标记的过程就是节点颜色从白色->灰色->黑色的遍历过程
 *          * 白色：未被垃圾回收器访问过的对象。
 *          * 黑色：已经被访问过且所有引用均被扫描。
 *          * 灰色：已被访问但还未扫描全部引用
 *     * 并发情况下可能出现的问题
 *          * 消亡对象背标记为存活-可接受
 *          * 存活对象被标记为消亡-不可接受
 *     * 导致出现问题二的两个充分必要条件：
 *          * 增加黑色->白色的引用 对应解决方案：CMS采用增量更新，记录增加的引用在二次扫描中重新扫描
 *          * 删除灰色->白色的引用 对应解决方案：G1则采用原始快照（SATB），记录原始快照和删除的引用，再次扫描中处理
 * 补充：八股问题
 *    * 设置停顿时间的参数是什么？
 *          * -XX:MaxGCPauseMillis 默认值是 200 毫秒
 *    * G1收集器是怎么保证停顿时间可控的？
 *          * 首先G1把内存区域分成了若干个相同大小的 Region 区，在执行回收的时候，
 *              根据标记阶段统计到的数据，计算出各个 Region 区的回收价值和成本，
 *              有了这些数据之后，就可以计算出回收哪几个Region价值最高，且符合用户预期的停顿时间。
 *
 *
 * */

public class GCLearn {

    private static class TestClass{

    }

    public static void main(String[] args) {
        testPromotion();
    }

    /*
     * 测试新生代minorGC:对象优先在Elden区分配，若Elden区空间不足，则执行一次young GC
     * 测试参数:-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:SurvivorRatio=8
     * //jdk9及以后 -verbose:gc -Xms20M -Xmx20M -Xmn10M -Xlog:gc+heap=debug -XX:SurvivorRatio=8
     * 测试结果：发生了一次young gc，将Eden regions内存放到Survivor中
     * */
    public static void testMinorGc(){
        byte[] allocOne = new byte[2 * 1024 * 1024];
        byte[] allocTwo = new byte[2 * 1024 * 1024];
        byte[] allocThree = new byte[2 * 1024 * 1024];
        byte[] allocFour = new byte[4 * 1024 * 1024];
    }

    /*
    * 大对象会直接进入老年代
    * 测试参数：-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:PretenureSizeThreshold=3145728
    * 测试结果：看不明白GC日志
    * */
    public static void testLargeObject(){
        byte[] allocFour = new byte[11 * 1024 * 1024];
    }

    /*
    * 长期存储对象会进入老年代
    * 测试参数：-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1
    * */
    public static void testAgeGc(){
        byte[] allocOne = new byte[512 * 1024];
        byte[] allocTwo = new byte[4 * 1024 * 1024];
        byte[] allocThree = new byte[4 * 1024 * 1024];
        allocThree = null;
        byte[] allocFour = new byte[4 * 1024 * 1024];
    }

    /*
     * 动态年龄判断:当Survivor空间中低于或者等于某年龄的所有对象大小综合大于Survivor空间的一半，大于该年龄的对象可以直接进入老年代
     * 测试参数：-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=15
     * */
    public static void testDynamicGc(){
        byte[] allocOne = new byte[256 * 1024];
        byte[] allocTwo = new byte[256 * 1024];
        byte[] allocThree = new byte[4 * 1024 * 1024];
        byte[] allocFour = new byte[4 * 1024 * 1024];
        allocThree = null;
        byte[] allocFive = new byte[4 * 1024 * 1024];
    }

    /*
     * 空间分配担保机制：老年代连续空间大小>新生代对象总大小/历次晋升平均大小，即进行minor GC否则进行full GC
     * 测试参数：-verbose:gc -Xms10M -Xmx10M -Xmn5M -XX:+PrintGCDetails -XX:SurvivorRatio=8
     * */
    public static void testPromotion(){
        byte[] allocOne = new byte[1 * 1024 * 1024];
        byte[] allocTwo = new byte[1 * 1024 * 1024];
        byte[] allocThree = new byte[1 * 1024 * 1024];
        allocOne = null;
        byte[] allocFour = new byte[1 * 1024 * 1024];
        byte[] allocFive = new byte[1 * 1024 * 1024];
        byte[] allocSix = new byte[1 * 1024 * 1024];
        allocFive = null;
        allocFour = null;
        allocSix = null;
        byte[] allocSeven = new byte[1 * 1024 * 1024];
    }

}
