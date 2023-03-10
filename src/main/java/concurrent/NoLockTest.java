package concurrent;

import org.junit.Test;
import org.openjdk.jol.vm.VM;

import java.util.concurrent.atomic.*;

/**
 * 乐观并发控制(Atomic包)
 * CAS(Compare and Set)  VarHandle.compareAndSe
 *  * 比较修改封装为一个原子操作，比较（Compare）判断是否由并发操作，修改（Set）执行对应操作
 *  * 底层基于lock cmpxchg指定保证CAS操作的原子性
 *  * 依赖于volatile关键字，实现变量修改的线程可见性
 *      private volatile int value;
 * 原子整数
 *  * AtomicBoolean
 *  * AtomicInteger
 *  * AtomicLong
 * 原子引用
 *  * AtomicReference：类似于原子整数，只是保证引用值（地址）的取存原子性
 *  * AtomicMarkableReference：只标记是否修改过，不记录每一次修改的版本号（compareSet的stamp为boolean类型）
 *  * AtomicStampedReference：使用版本号记录修改（乐观并发控制终极版本）（compareSet的stamp为int类型）
 * 原子数组：：方法接口与对应原子类型一致，只是额外传入index属性
 *  * AtomicIntegerArray/AtomicLongArray
 *  * AtomicReferenceArray
 * 对象字段更新器：接口一致，需要对应方法属性标注为volatile
 *  * AtomicReferenceUpdater
 *  * AtomicLongReferenceUpdater
 *  * AtomicIIntgerReferenceUpdater
 * 原子累加器：
 *  * 原理：多个cell,并发情况下更新不同的cell，无并发更新base
 *      transient volatile Cell[] cells; (懒惰初始化)
 *      transient volatile long base;
 *  * LongAdder
 *  * LongAccumulator：类似于LongAdder，只是累加规则由初始化的累加函数确定（left为累加和，right为传入的新累加值）
 *     public interface LongBinaryOperator {
 *          long applyAsLong(long left,long right);
 *     }
 * Unsafe:该类对于普通程序员来说是“危险”的，一般开发者不应该也不会用到此类。因为Unsafe类功能过于强大，
 *  提供了一些可以绕开JVM的更底层的功能。它让JAVA拥有了想C语言的指针一样操作内存空间的能力，能够提升效率
 *  ，但是也带来了指针的复杂性等问题，所以官方并不建议使用，并且没提供文档支持，甚至计划在高版本去除该类。
 * */

public class NoLockTest {


    /**
     * 测试原子整数理性
     * */
    @Test
    public void testAtomicNumber(){
        AtomicInteger i = new AtomicInteger(0);
        //1. i++, ++i, --i, i--
        System.out.println(i.getAndIncrement());
        //2. 加减
        System.out.println(i.getAndAdd(10));
        //2. 复杂计算公式
        System.out.println(i.getAndUpdate(p -> 100 * p));
        //3.含参公式
        i.getAndAccumulate(100, Integer::sum);
    }

    /**
     * 测试字符串地址问题
     *
     * */
    @Test
    public void testStringPos(){
        String s1 = "abc";
        System.out.println("s1的内存地址："+VM.current().addressOf(s1));
        s1 = "abc";
        System.out.println("s1的内存地址："+VM.current().addressOf(s1));
    }

    /**
     * 测试原子引用
     * */
    @Test
    public void testAtomicReference(){
        AtomicStampedReference<String> stampedReference = new AtomicStampedReference<>("ha", 1);

        System.out.println(stampedReference.getStamp());

        //修改并操作
        stampedReference.set("张三", 2);

        new Thread(() -> {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
           while(true){
               int stamp = stampedReference.getStamp();
               String ref = stampedReference.getReference();
               if(stampedReference.compareAndSet(ref, "new value", stamp, stamp + 1)){
                   System.out.println("从线程：修改成功，版本号从 " + stamp + "->" + (stamp + 1));
                   break;
               }
           }
        }).start();

        for(int i = 0;i < 10;i++){
            while(true){
                int stamp = stampedReference.getStamp();
                String ref = stampedReference.getReference();
                if(stampedReference.compareAndSet(ref, "new value", stamp, stamp + 1)){
                    System.out.println("主线程：修改成功，版本号从 " + stamp + "->" + (stamp + 1));
                    break;
                }
            }
        }

//        AtomicMarkableReference<String> markableReference = new AtomicMarkableReference<>("haha", true);
//
//        markableReference.compareAndSet("haha", "haha", true, false);
    }


    /**
     * 测试原子数组
     * */
    @Test
    public void TestAtomicArray(){
        AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(5);
        atomicIntegerArray.incrementAndGet(1);

        AtomicReferenceArray<String> stringAtomicReferenceArray = new AtomicReferenceArray<>(5);
        stringAtomicReferenceArray.compareAndSet(1, null, " hahah");
    }

    /**
     * 测试原子累加器
     * */
    @Test
    public void testAdder() throws InterruptedException {
        AtomicLong atomicLong = new AtomicLong();
        //首先计算普通累加的时间
        Long curTime = System.currentTimeMillis() ;
        Thread[] threads = new Thread[100];
        for(int i = 0;i < 100;i++){
            Thread t = new Thread(() ->{
                for(int j = 0;j < 100;j++){
                    while(true){
                        long pre = atomicLong.get();
                        if(atomicLong.compareAndSet(pre, pre + 1)){
                            break;
                        }
                    }
                }
            });
            threads[i] = t;
            t.start();
        }

        for(Thread thread:threads){
            thread.join();
        }
        System.out.println("[atomiclong]经历时间："+ (System.currentTimeMillis() - curTime) + " 结果为：" + atomicLong.get());

        //使用Adder
        LongAdder longAdder = new LongAdder();
        curTime = System.currentTimeMillis() ;
        threads = new Thread[100];
        for(int i = 0;i < 100;i++){
            Thread t = new Thread(() ->{
                for(int j = 0;j < 100;j++){
                    longAdder.add(1);
                }
            });
            threads[i] = t;
            t.start();
        }

        for(Thread thread:threads){
            thread.join();
        }
        System.out.println("[adder]经历时间："+ (System.currentTimeMillis() - curTime) + " 结果为：" + longAdder.sum());
        LongAccumulator longAccumulator = new LongAccumulator((left, right) -> left / 2 + right, 0);
        longAccumulator.accumulate(10);
        longAccumulator.accumulate(3);
        System.out.println(longAccumulator.get());
    }


}
