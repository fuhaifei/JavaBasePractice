package jvm;

/**
 * JVM调优相关知识总结
 * * JAVA提供的查看JVM状态的相关工具
 *      1. jps(JVM Process Status Tool):列出正在运行的虚拟机进程，以及进程本地唯一ID(本地vmid对于进程ID)/主类名称
 *          * jps [options] [hostid]
 *      2. jstat（JVM Statistics Monitoring Tool）：监视虚拟机各种运行状态的命令行工具
 *          * jstat [option vmid [interval[s|ms]] [count]]
 *              * jstat -gc 2764 250 20 (每隔250毫秒查询一次进程2764垃圾回收的状况，共20次)
 *              * 远程虚拟机进程vmid格式为：[protpcol:][//]lvmid[@hostname[:port]/servername]
 *          * option选项包括
 *              * -class：类加载卸载 空间时间信息
 *              * -gc: Java堆状态，容量和垃圾回收
 *      3. jinfo(Configuration Info for Java)：实时查看和调整虚拟机各项参数
 *          * jinfo [option] pid
 *              * jinfo -flag [+|-]name / jinfo -flag name=value
 *      4. jmap(Memory Map for Java)：用于生成堆转储快照
 *          * 获取堆转储快照，查询finalize执行队列、Java堆和方法去的详细信息，如空间使用率部和当前垃圾收集器等
 *          * jmap [ option ] vimid
 *              * jmap -dump:format=b,file=xxx.xxx vmid
 *          * option包括
 *              * -dump：生成Java堆转储快照
 *              * -finalizerinfo:显示F-Queue中等待Finalizer线程执行finalize方法的对象
 *              * -heap:显示堆的详细信息
 *              * -histo:堆中对象的统计信息
 *     5. jhat(JVM Heap Analysis Tool)：分析jmap生成的堆转储快照
 *     6. jstack（Stack Trace for Java）：生成虚拟机当前时刻的线程快照
 *          * jstack [option] vmid
 *          * option包括
 *              * -F 强制输出线程堆栈
 *              * -l 额外显示锁的附加信息
 *              * -m 显示本地方法调用的c/c++方法栈
 * * 可视化故障处理工具
 *      * JConsole（监视管理工具）
 *          * JDK中自基于JMV的可视化监视管理工具
 *          * 监控内存变化/线程状态等
 *      * JHSDB（调试管理工具）
 *          * 基于服务性代理实现的进程外调试工具，集成了 jmap/jstack/jinfo
 *          * 更细粒度的内存地址/指针查找/对象查找等功能
 *      * VisualVM
 *          * 功能最强大的运行监视和故障处理程序之一（运行监视、故障处理、性能分析）
 *      * JMC(Java Mission Control):可持续在线监控工具
 *
 *
 * */
public class JVMOpi {
    private static class ObjectHolder{};

    static class Test{
        static ObjectHolder staticObj = new ObjectHolder();
        void foo(){
            ObjectHolder localObj = new ObjectHolder();
            System.out.println("done");
        }
    }

    public static void main(String[] args) {
        Test test = new JVMOpi.Test();
        test.foo();
        while(true){

        }
    }
}
