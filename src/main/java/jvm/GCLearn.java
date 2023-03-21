package jvm;

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
 *      * 当程序创建一个新的对象或者基本类型的数据，内存空间不足时，会触发GC的执行
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
 *          * JDK8的默认垃圾收集器组合：Parallel Scavenge + Parallelo Old
 *      * CMS （-XX:+UseConcMarkSweepGC） 是JDK1.4后期推出的GC收集器，它是一款并发低停顿的收集器
 *          * 采用标记-清除算法的老年代垃圾回收器
 *          * 垃圾收集步骤
 *              * 1.初始标记：需要STW，只标记与GC Roots直接关联的对象
 *              * 2.并发标记：不需要STW，遍历标记整个引用链，可以与用户线程并发执行
 *              * 3.重新标记：需要STW，标记之前并发标记时用户线程增加的垃圾
 *              * 4.并发清理：不需要STW，遍历老年代空间，清理可回收对象，重置CMS收集器的数据结构，等待下一次回收
 *          * 问题：采用标记清理算法导致大量内存碎片；GC线程并发降低吞吐量；浮动垃圾，并发清理过程中伴随着垃圾的增加只能等到下一次垃圾回收
 *              (jdk5 68%, jdk6 92%)
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
 *
 * 7. 八股问题
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
}
