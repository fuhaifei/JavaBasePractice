package concurrent;

import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * 并发基本概念总结：
 * 1. 进程和线程的区别
 *
 * 2. 临界区（Critical Section）和 竞态条件（Race Condition）
 *
 * 3. String/Integer等不可变对象是线程安全的
 *
 * 4. 死锁/活锁/饥饿
 *  * 死锁条件：互斥，占有且等待，不存在抢占，环状等待
 *  * 活锁：任务或者执行者没有被阻塞，由于某些条件没有满足，导致一直重复尝试，失败，尝试，失败
 *  * 饥饿：由于任务优先级较低，一直无法获取不到锁，导致程序始终无法执行的情况
 * 5. 伪共享问题
 *  * cpu读取数据以块(cpu line，一般为64byte)的形式，当分布在不同核的线程操作位于一个缓存块的数据时，任意一个线程的修改都会导致另外一个核心加载缓存失效
 *  * 解决方案：通过将添加填充，使得两个相邻的数据分布于不同的缓存块中
 * 6. 不同任务类型应该使用不同的线程池，这样能够避免饥饿，并能提升效率
 *
 * 7. JMM：Java Memory Model
 *  * 对底层CPU寄存器、缓存、硬件内存、CPU指令优化的抽象，定义了主存、工作内存等抽象概念
 *  * 包含三个性质： 1. 原子性-指令不会受到线程上下文切换的影响
 *               2. 可见性-保证指令不会受到CPU缓存的影响
 *               3. 有序性-保证指令不会受到CPU指令并行优化的影响
 *  可见性（volatile）
 *      * 为了降低线程访问主存的频率，线程会将访问变量复制到自身工作内存，导致不同线程见数据不可见
 *      * 通过volatile关键字修饰（成员变量和静态变量），限制线程只能操作主存中的变量
 *      * volatile 无法解决i++的race condition
 *      * 底层原理：读屏障（读主存，后面的指令不会到前面），写屏障（同步到主存，前面的指令不会到后面）
 *  有序性
 *      * JVM会在不影响正确性的前提下，调整语句的执行顺序
 *      * 进行指令重排的原因：不冲突的指令并行，实现更高效的执行
 *  8. AQS原理(AbstractQueueSynchronizer): 抽象队列同步框架
 *      * 定义中包括：
 *          * state（volatile标记基于CAS操作的int变量）：标示资源被占用或者释放的状态，通过getState, setState, compareAndSetState操作
 *          * FIFO队列：将等待获取资源的线程封装为队列节点，由AQS框架实现队列的进出，即线程状态的变化
 *      * 我们只需要实现 tryAcquire，tryRelease，tryAcquireShared，tryReleaseShared，isHeldExclusively方法，
 *        定义自己的获取/释放资源的操作逻辑，其他的线程状态维护，队列维护由AQS实现
 *      * CLH队列：多个线程竞争锁时，获取不到锁的线程排队进入队列尾部，每个线程自旋锁监控前一个节点的状态，但前一个节点释放锁时，当前节点获取锁
 *          * 通过空间换时间，通过队列将众多线程竞争一个锁->每个线程排队获取锁，避免了羊群效应
 *      * AQS中的CLH队列变体：CLH队列基于自旋锁，AQS中的队列基于LockSupport.park()实现阻塞和唤醒
 *      * 默认加锁流程：线程获取锁->state=1，获取失败->若当前节点为头节点，根据spin次数重试获取锁,否则修改状态为Waiting
 *                      ->调用LockSupport.park() 进入Waiting状态,等待唤醒
 *      * 默认解锁流程：上个一个线程释放锁->signalNext(head)—> LockSupport.unpark(s.waiter);
 * 9. ReentrantLock锁原理
 *      * 非公平锁实现原理：在获取锁前不判断队列中是否存在线程，先尝试获取锁后，获取失败再加入队列
 *          * 上锁前首先调用initialTryLock()方法
 *              final void lock() {
 *                   if (!initialTryLock())
 *                      acquire(1);
 *              }
 *          * 初始化根据参数，初始化为不同的sync对象
 *             public ReentrantLock(boolean fair) {
 *                  sync = fair ? new FairSync() : new NonfairSync();
 *             }
 *          * 两者区别在判断条件：!hasQueuedThreads()
                 if (!hasQueuedThreads() && compareAndSetState(0, 1)) {
                     setExclusiveOwnerThread(current);
                     return true;
                 }
 *      * 可重入原理：将state状态为表示当前获取锁的次数
 *          * 当占有锁线程=当前线程时，state++
             else if (getExclusiveOwnerThread() == current) {
                 if (++c < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                 setState(c);
                 return true;
             }
            * 释放时，只有state==0,才真正的释放锁
                 boolean free = (c == 0);
                 if (free)
                    setExclusiveOwnerThread(null);
 *      * 可打断原理：在对队列中等待获取锁,当获取到锁后检查interrupted状态，若为True直接执行释放锁逻辑
 *          * acquire()主循环中的逻辑
 *              if ((interrupted |= Thread.interrupted()) && interruptible)
 *                      break;
 *              xxxx
 *              return cancelAcquire(node, interrupted, interruptible);
 *          * 释放锁
 *              if (interrupted) {
 *                  if (interruptible)
 *                      return CANCELLED;
 *                  else
 *                      Thread.currentThread().interrupt();
 *              }
 *  10.条件变量（Condition）->ConditionObject
 *      * 独立锁之外的FIFO队列（ConditionNode）
 *      * await()：将线程添加到条件变量队列中，并调用LockSupport.park()暂停线程
 *      * signal()/signalAll()：唤醒条件队列中的线程，添加到锁队列中
 * 11. 信号量（Semaphore）
 *      * 底层就是state>1的AQS锁
 *      * 获取锁：state - require >= 0
 *      * 释放锁：state + release
 * 12. 读写锁（ReentrantReadWriteLock）
 *      * 底层基于一个Sync对象，其中state高16位记录读线程，低16位记录写线程
 *      * 获取读写锁的线程通过是否为ShareNode区分（读线程在队列中为ShareNode,写线程在队列中为ExclusiveNode）
 *      * 当写线程获取锁时，会判断队列中下一个节点是否为ShareNode,若为ShareNode同时唤醒
 *        if (shared)
 *          signalNextIfShared(node);
 *      * 写线程独占锁，导致其他并发线程进入队列
 * 13. CountDownLatch:主线程等待工作线程执行完毕后，继续执行（加强版join）
 *      * 基于AQS实现，类似于一个单次使用的Semaphore
 *      * API:CountDownLatch(int count),countDown(),await()
 * 14. CyclicBarrier: 控制多个线程的执行进度，所有线程执行到CyclicBarrier位置后，才能继续执行
 *      * 底层基于ReentrantLock+Condition实现：最后一个线程执行完最后操作后，直接替换新一代屏障（nextGeneration()
 *      * 生成新屏障的同时，唤醒其他等待的线程，其他线程发现屏障换了(g != generation)，返回到达的次数（index）,退出await()
 *         private void nextGeneration() {
 *              // signal completion of last generation
 *              trip.signalAll();
 *              // set up next generation
 *              count = parties;
 *              generation = new Generation();
 *          }
 *      * API:CyclicBarrier(int parties)，await()，await(long timeout, TimeUnit unit)
 * 15. Phaser: 提供类似于CyclicBarrier的功能，可重用,灵活性更高，支持同步线程的动态注册和取消
 *      * API: register()，bulkRegister(int parties)，arrive()，arriveAndDeregister()，arriveAndAwaitAdvance()
 * 16. Exchanger：线程间进行数据交换的工具，阻塞双向通信
 *      * API：exchange(V x)
 * */
public class BasicUse {
    /**
     * 测试变量的可见性
     * */
    public static boolean run = true;

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(()->{
            int i = 0;
            while (run){
                i++;
            }
            System.out.println("rs:"+i);
        });
        thread.start();
        Thread.sleep(1000);
        run = false;
        System.out.println("主线程修改run");
        Thread.sleep(1000);
    }
    @Test
    public void visibleTest() throws InterruptedException {
        new Thread(() -> {
            while(run){
                System.out.println("线程运行中....");
            }
            System.out.println("线程停止运行");
        }).start();
        Thread.sleep(1);
        run = false;
        System.out.println("主线程给修改完毕");
        Thread.sleep(100000);
    }

    /**
     * 基于AQS框架实现锁
     * */
    class MyLock implements Lock, java.io.Serializable{
        private final Sync sync = new Sync();
        private static class Sync extends AbstractQueuedSynchronizer{

            @Override
            protected boolean tryAcquire(int arg) {
                if(compareAndSetState(0, 1)){
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            }

            @Override
            protected boolean tryRelease(int arg) {
                if (!isHeldExclusively()){
                    throw new IllegalMonitorStateException();
                }
                setExclusiveOwnerThread(null);
                setState(0);
                return true;
            }
            @Override
            protected boolean isHeldExclusively() {
                return getExclusiveOwnerThread() == Thread.currentThread();
            }

            private Condition getCondition(){
                return new ConditionObject();
            }


        }
        @Override
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(time));
        }

        @Override
        public void unlock() {
            sync.release(1);
        }

        @Override
        public Condition newCondition() {
            return sync.getCondition();
        }

    }

    @Test
    public void testMyLock() throws InterruptedException {
        MyLock myLock = new MyLock();
        new Thread(() ->{
           myLock.lock();
           System.out.println(Thread.currentThread().getName() + "：工作中");
           try {
               Thread.sleep(5000);
               System.out.println(Thread.currentThread().getName() + "：下班");
           } catch (InterruptedException e) {
                throw new RuntimeException(e);
           }finally {
               myLock.unlock();
           }
        }).start();
        new Thread(() ->{
            myLock.lock();
            System.out.println(Thread.currentThread().getName() + "：继续工作中");
            System.out.println(Thread.currentThread().getName() + "：下班");
            myLock.unlock();
        }).start();
        Thread.sleep(10000);
    }

    public void testCondition() throws InterruptedException {
        ReentrantLock reentrantLock = new ReentrantLock();
        var condition = reentrantLock.newCondition();
        condition.await();

        Semaphore semaphore = new Semaphore(10);
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        readWriteLock.readLock().lockInterruptibly();
    }
}
