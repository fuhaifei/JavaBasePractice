package concurrent;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
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
 * 5.
 * 6. 不同任务类型应该使用不同的线程池，这样能够避免饥饿，并能提升效率
 *
 * 7. JMM：Java Memory Model：是Java虚拟机规范中所定义的一种内存模型，Java内存模型是标准化的，屏蔽掉了底层不同计算机的区别
 *  对底层CPU寄存器、缓存、硬件内存、CPU指令优化的抽象，定义了主存、工作内存等抽象概念，即计算机内存模型（CPU->寄存器->l1->l2->l3缓存->主存）
 *     -> JMM(主存存储共享变量，每个线程有自己的工作内存（CPU寄存器/缓存）存储共享变量副本)
 *  定义了八个操作：lock&unlock(用于锁定主内存变量)，read&load(从主存加载到工作内存)，store&write(从工作内存写入到主存)；
 *                  use&assign(从执行引擎接收值到工作内存)
 *               * unlock操作之前，必须将变量同步到主存中
 *
 *  包含三个性质： 1. 原子性-指令不会受到线程上下文切换的影响
 *               2. 可见性-保证指令不会受到CPU缓存的影响
 *               3. 有序性-保证指令不会受到CPU指令并行优化的影响
 *  happens-before:解决强内存模型约束，和编译器、处理器希望约束更少方便优化的冲突
 *      * 操作1 happens-before 操作2,无论实际执行顺序如何，最终结构必须和 操作1先执行，操作2后执行一致
 *      * 8个原则（记不住，简单看看）
 *          * 程序次数原则：一个线程中，书写在前面的操作先于书写在后面的操作
 *          * 管程锁定原则：unlock操作先行发生于后面对同一个锁的lock操作
 *          * volatile变量原则：volatile写操作先发生于后面对于变量的读操作
 *          * 线程终止原则：线程中的所有操作先于现成的终止检测操作
 *          * 线程中中断原则：线程interrupt()调用先于中断线程检测到中断事件的发生
 *          * 对象终结原则：对象的初始化先行发生于他的finalize()方法的开始
 *          * 传递性原则
 *
 *  可见性
 *      * 为了降低线程访问主存的频率，线程会将访问变量复制到自身工作内存，导致不同线程见数据不可见
 *  volatile
 *      * 可见性：写volatile变量限制修改刷新到主存中，读volatile变量，强制从主存中读。
 *      * 有序性：volatile读后操作不会重排序到volatile之前，volatile写前读写操作不会重排序到volatile之后
 *      * 从线程通信的角度理解：写相当于发消息，发消息之前必须把材料准备好；读相当于收消息，收到消息后才能开始处理
 *      * 通过volatile关键字修饰（成员变量和静态变量），限制线程只能操作主存中的变量
 *      * volatile 无法解决i++的race condition
 *      * 底层原理：读屏障（读主存，后面的指令不会到前面），写屏障（同步到主存，前面的指令不会到后面）
 *  MESI(Modified Exclusive Shared or Invalid) 广泛应用的缓存一致性协议
 *  * 定义了缓存行的四个状态：Modified(已修改),Exclusive(独占的),Shared(共享的)，Invalid（无效的）
 *  *  * 当一个线程从主存中加载数据到缓存中，此时缓存行状态为Exclusive(独占的)，当有其他线程加载该缓存行，缓存状态均变为Shared
 *  *  * 当其中一个线程修改缓存数据后，该线程缓存状态变为Modified，其他线程对应缓存行变为Invalid（无效的），其他线程必须等待修改线程将修改写入到
 *  *    主存后才能读取继续操作
 *  伪共享问题（时间和空间的局部性）
 *  * cpu读取数据以块(cpu line，一般为64byte)的形式，当分布在不同核的线程操作位于一个缓存块的数据时，
 *    任意一个线程的修改都会导致另外一个核心加载缓存失效
 *  * 解决方案：通过将添加填充，使得两个相邻的数据分布于不同的缓存块中
 *  有序性
 *      * JVM会在不影响正确性的前提下，调整语句的执行顺序
 *      * 进行指令重排的原因：不冲突的指令并行，实现更高效的执行
 *  指令重排
 *      * 每个指令包含不同的步骤（取指令->指令译码->指令执行->访存取数->结果写回），可能访问不同的硬件，计算机为了提升指令执行速度，引入了流水线
 *      技术，在同一个时钟周期执行不同指定的不同步骤，从而实现指令加速执行（A=B+C,D=E-F），指令重拍将指令按照尽可能满足流水线并行执行的重新排序，
 *      实现指令执行的加速
 *      * 指令重排包括三个阶段：1.编译器优化重排。2.指令并行重排。3.内存系统重排，
 *      * 不管编译器和CPU如何重排序，指令重排保证在单线程情况下程序的结果是正确的（as-if-serial）：读后写，写后读，写后写（数据依赖关系）
 *
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
 *      * 写线程重入基于额外的计数属性实现
 *          * firstReader(第一次获取读锁的线程ID)+firstReaderHoldCount(重入次数)：第一个获取读锁的重入记录
 *          * 以ThreadLocal形式与线程绑定的ReaderHolders: t_id(线程ID) + count（Count）
 *          * cachedHolderCounter: 为了减少调用ThreadLocal.get(),缓存上次调用的ReaderHolders
 *          * 重入流程：判断是否为firstReader->判断是否在cachedHolderCounter->ReaderHolders
 *          * ReaderHolders中存储线程ID是为了避免循环引用，降低GC垃圾回收成本
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
 * 17. 阻塞队列：当队列中不存在元素进行消费/队列满写入队列时，会阻塞当前读写线程。
 *      * 实现BlockingQueue接口，该接口定义方法包括
 *          * 当队列空或者满抛出异常：add(), remove(), element()
 *          * 返回消费/生产成功失败的boolean(可传入等待时间):offer(),poll(), peek()
 *          * 阻塞方法：put(),take()
 *      * 主要的实现类：
 *          * 两种最普通的阻塞队列：ArrayBlockingQueue，LinkedBlockingQueue
 *          * 特殊阻塞队列：PriorityBlockingQueue，DelayQueue，SynchronousQueue(不存储队列数据，当出现消费者和生产者时，配对直接消费)，
 *                      LinkedTransferQueue（生产者阻塞等待消费者消费），LinkedBlockingDeque
 * 18. 带有回调的多线程方法：
 *      * Feature API(FutureTask): get()/get(TimeOut), cancel()/isCancelled(),isDone()
 *          * 中断问题：cancel()通过调用interrupted()方法实现任务取消，interrupted()效果有两个
 *              * 使得基于wait(),sleep(),join()等进入wait状态的线程抛出InterruptedException
 *              * 其他情况只会修改线程的interrupted = true
 *              * 因此cancel无法真正的取消运行中任务，必须在任务中条件Thread.interrupted()判断是否中断
 *          * 为了避免异步线程无法取消的问题，可将执行异步任务的线程设置为守护线程，在主线程退出时主动退出
 *          * 其他问题：正在执行的线程cancel无法中断，isCancelled() = True，无法获取异步操作结果
 *                      已经执行完的异步任务无法中断，isDone() = true
 *          * 无法解决的问题：当多个线程执行耗时不同的任务时，主线程按顺序获取结果导致耗时较长任务阻塞耗时较短任务获取输出
 *      * CompletionService：对于线程池的增强，先完成任务的结果可以优先获得到(线程池+阻塞队列)
 *          * API:take() 阻塞直到队列中出现异步返回结果，poll(),poll(timeout) 直接返回当前异步结果或者null
 *      * CompletableFuture：真正的异步非阻塞调用(Future（异步非阻塞调用）+CompletionStage（多线程间配合和结果的传递）)
 *          * 为了解决Future对象需要线程主动调用，获得返回结果的情况，通过传入会调用想，当异步任务完成/发生异常时，自动调用对象的回调方法
 *          * 推荐使用静态构造方法创建CompletableFuture对象
 *          * 相关API：allOf(CompletableFuture<?>... cfs),anyOf(CompletableFuture<?>... cfs)
 *                  * 不带返回值：runAsync(Runnable runnable,Executor executor)，runAsync(Runnable runnable)
 *                  * 带返回值的：supplyAsync(Supplier<U> supplier)，supplyAsync(Supplier<U> supplier, Executor executor)
 *                  * 执行完的回调方法：whenComplete(BiConsumer<? super T,? super Throwable> action) （返回结果和抛出异常）
 *                                  whenCompleteAsync(BiConsumer<? super T,? super Throwable> action)
 *                                  whenCompleteAsync(BiConsumer<? super T,? super Throwable> action, Executor executor)
 *                  * 线程间的配合方法：thenAcceptBoth(), runAfterBoth(),runAfterEither(),thenCombine(),thenCompose()\
 *                  * 获取结果/处理异常：exceptionally(),getNow(T valueIfAbsent),complete(T value)
 * */
public class BasicUse {
    /**
     * 测试变量的可见性
     * */
    public static boolean run = true;

//    public static void main(String[] args) throws InterruptedException {
//        Thread thread = new Thread(()->{
//            int i = 0;
//            while (run){
//                i++;
//            }
//            System.out.println("rs:"+i);
//        });
//        thread.start();
//        Thread.sleep(1000);
//        run = false;
//        System.out.println("主线程修改run");
//        Thread.sleep(1000);
//    }
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

    /**
     * Feature对象测试
     * */
    @Test
    public void testFeature() throws InterruptedException, ExecutionException {
        FutureTask<Integer> future = new FutureTask<>(() ->{
            while(true){
            }
        });

        Thread t = new Thread(future);
        t.setDaemon(true);
        t.start();
        Thread.sleep(1000);
        System.out.println(future.cancel(true));
        System.out.println(future.isCancelled());
        System.out.println(future.get());
    }

    /**
     * 测试CompletionService对象
     * */
    @Test
    public void testCompletionService() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        List<Callable<Integer>> allTask = Arrays.asList(
                () -> {
                    TimeUnit.MILLISECONDS.sleep(200);
                    System.out.println("Thread 10 finished!");
                    return 10;
                },
                () -> {
                    TimeUnit.MILLISECONDS.sleep(400);
                    System.out.println("Thread 20 finished!");
                    return 20;
                }
        );
        ExecutorCompletionService<Integer> service = new ExecutorCompletionService<>(executor);
        allTask.forEach(service::submit);

        for(int i = 0;i < allTask.size();i++){
            Future<Integer> result = service.take();
            System.out.println(result.get());
        }
    }

    /**
     * 测试CompletableFuture
     * */
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main....start....");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getName());
            int i = 10 / 0;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).whenComplete((res, excption) -> { //虽然能得到异常信息，但是没法修改返回数据
            System.out.println("异步任务成功完成了...结果是:" + res + "；异常信息是" + excption);
        }).exceptionally(throwable -> {  //可以感知异常，同时返回默认值
            return 10;
        }); //成功以后干啥事

        System.out.println("future获取结果：" + future.get());


        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getName());
            int i = 10 / 4;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).handle((res, thr) -> {
            if (res != null) {
                return res * 2;
            }
            if (thr != null) {  //异常不等于空了，就返回0
                return 0;
            }
            return 0;
        });

        System.out.println("future1获取结果：" + future1.get());


        executor.shutdown();
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        readWriteLock.readLock().lock();
    }


    @Test
    public void testWait() throws InterruptedException {
        String str = "hahah";
        Thread t = new Thread(() ->{
            synchronized (str){
                System.out.println("休眠中");
                try {
                    str.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("休眠结束，继续执行");
            }
        });
        Thread t2 = new Thread(() ->{
            System.out.println("等待获取锁");
            synchronized (str){
                System.out.println("获取到锁了");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        t.start();
        Thread.sleep(10);
        t2.start();
        Thread.sleep(1000);
        synchronized (str){
            str.notify();
        }
        Thread.sleep(1000);
        wait();
        System.out.println("exit");
        ReentrantLock reentrantLock = new ReentrantLock();
        Condition condition = reentrantLock.newCondition();
        condition.await();
        condition.signal();
    }

}
