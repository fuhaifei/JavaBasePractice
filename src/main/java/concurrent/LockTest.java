package concurrent;


import org.junit.Test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 同步相关
 * 1. synchronized 对象锁解决临界区的Race Condition
 *  * 非静态方法上的synchronized等价于synchronized(this)          对象锁
 *  * 静态方法上的synchronized等价于synchronized(this.getClass()) 类对象锁
 *  * 每个Java对象关联一个Monitor(管程)，包含三个结构
 *      * Owner：当前通过synchronized得到对象锁的线程
 *      * EntryList：等待获取对象锁的线程列表，阻塞在synchronized代码块的线程（blocked）
 *      * WaitSet: 调用wait()方法的线程，释放锁进入waiting状态，进入WaitSet
 *      * 锁外调用wait()会抛出java.lang.IllegalMonitorStateException异常
 *      * wait()被唤醒后继续从当前位置继续执行
 * 1.1 synchronized原理
 *  * Java对象布局：对象头（Mark word + 类型指针，指向类元数据）+实例数据+对齐填充
 *      * Mark word：哈希码（HashCode）、GC分代年龄、锁状态标志、 线程持有的锁、偏向线程ID、偏向时间戳
 *      * 偏向锁->线程ID;轻量级锁->线程栈Lock Record指针;重量级锁->堆中的monitor指针
 *  * Lock Record:执行同步块之前，在每个现成的栈帧中创建一个Lock Record，包括一个用来存储对象头的mark word和指向对象的指针
 *  * 分为四种状态：无锁状态、偏向锁状态、轻量级锁状态、重量级锁状态
 *      * 偏向锁
 *          * 线程通过CAS操作将MarkWord中的threadId从0改为自己的ID，成功继续执行，否则升级为轻量级锁
 *          * 偏向锁对于支持低成本可重入：判断threadId为当前线程，在线程栈中创建空mark word的Lock Record
 *          * 偏向锁只有在出现竞争时才会被动释放或者升级->回到未锁定（执行完）/轻量级锁状态（未执行完）
 *      * 轻量级锁
 *          * 线程栈创建一个LockRecord，obj指向锁对象，锁对象maskword存储到LockRecord中
 *          * 通过尝试CAS将LockRecord地址存储到对象头mark word中，成功则获取锁成功
 *          * 释放：逐个删除指向锁对象的lockRecord（可重入），直到最后一个包含mark word的LockRecord，基于CAS操作将mask word写回对象头
 *      * 重量级锁
 *          * 轻量级锁获取失败或者轻量级锁释放失败（唤醒被挂起的线程）
 *          * mark word`为指向一个堆中monitor对象的指针
 *      * 偏向锁就是一个标记，每次访问只需要比较一下标记即可，无需CAS
 *        轻量级锁是一个轮流使用的锁，一个人使用时贴上标记，离开时撕下标记
 *        重量级锁是真的出现并发使用的情况
 * 2. object.notify() 和 object.notifyAll()
 *  * 持有锁的线程通过调用notify()方法唤醒WaitSet中等待的线程
 *  * 被唤醒的线程等待调用notify()线程释放锁后继续执行
 *  * interrupted() 同样能够唤醒waiting线程，只是会抛出InterruptedException
 *  * join 的实现、Future 的实现，基于同一个锁对象的wait和notify
 *         while (isAlive()) {
 *            wait(0);
 *         }
 * 3. LockSupport: park() 和 unPark 控制线程的执行和暂停
 *  * 每个线程绑定一个Parker对象包括_counter， _cond和 _mutex
 *      * park() 判断_counter > 0， 若等于0则等待，否则执行并_counter  - 1
 *      * unpark() 方法给 Math.min(_counter + 1,1)
 *  * 由park()导致的暂停为waiting/time_waiting状态
 *  * interrupted()方法会打断暂停（底层调用unPark()）,并将interrupt标志设置为true,park()方法在interrupt为true的情况下不会赞同
 * 4. ReentrantLock 支持可重入/超时/打断/公平锁
 *  * tryLock(long timeout, TimeUnit unit):获取锁，等待timeout时间自动退出
 *  * lockInterruptibly():可中断锁
 *  * new ReentrantLock(true): 公平锁，会降低并发度
 * 5. 信号量/条件变量
 * * 信号量和锁的区别：锁是服务于共享资源的；而semaphore是服务于多个线程间的执行的逻辑顺序的
 * * 条件变量(Condition)：唤醒所有等待条件的线程，与锁绑定使用(接口从wait/notify变味了await/signal)
 *      * await()
 *      * signal()
 *
 * */

public class LockTest {
    final String lockObject = "haha";
    int num = 0;


    @Test
    public void testSynchronized() throws InterruptedException {

        Thread t1 = new Thread(() ->{
           for(int i = 0;i < 5000;i++){
               synchronized (lockObject){
                   num++;
               }
           }
        });
        Thread t2 = new Thread(() -> {
            for(int i = 0;i < 5000;i++){
                synchronized (lockObject){
                    num--;
                }
            }

        });

        t1.join();
        t2.join();
        System.out.println(num);
    }

    class Number{
        public synchronized void a() throws InterruptedException {
            Thread.sleep(1000);
            System.out.println("a");
        }
        public synchronized void b() {
            System.out.println("b");
        }
        public synchronized void c() {
            System.out.println("c");
        }
    }

    @Test
    public void testSync() throws InterruptedException {
        Number n1 = new Number();
        new Thread(() ->{
            try {
                n1.a();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(n1::b).start();
        new Thread(n1::c).start();

        Thread.sleep(10000);
    }

    /**
     * 测试notify和wait
     * */
    @Test
    public void testNotify() throws InterruptedException {
        Thread t1 = new Thread(() -> {
           synchronized (lockObject){

               System.out.println("从线程：执行业务代码，后进入休眠状态：" + num);
               try {
                   lockObject.wait();
               } catch (InterruptedException e) {
                   System.out.println(Thread.currentThread().isInterrupted());
                   throw new RuntimeException(e);
               }
               System.out.println("从线程：休眠结束，继续执行业务代码:" + num);
           }
        });
        t1.start();
        Thread.sleep(1000);
//        synchronized (lockObject){
//            lockObject.notify();
//            System.out.println("主线程：发送叫醒信号后，休眠 " + num);
//            num++;
//            Thread.sleep(10000);
//            System.out.println("主线程：休眠结束" + num);
//        }
        t1.interrupt();
    }

    /**
     * 测试park和unpark
     * */

    @Test
    public void testPark() throws InterruptedException {
        Thread t1 = new Thread(() ->{
            synchronized (lockObject){
                while(num == 0){
                    LockSupport.park();
                }
                System.out.println(num);
            }
        });
        t1.start();
        Thread.sleep(1000);
        synchronized (lockObject){
            num = 100;
        }
        System.out.println("执行完毕，获得执行结果");
        LockSupport.unpark(t1);
        Thread.sleep(1000);
    }
    /***
     *  测试lockSupport被中断
     */

    @Test
    public void testPark2() throws InterruptedException {
        Thread t1 = new Thread(() ->{
            LockSupport.park();
            System.out.println(Thread.currentThread().isInterrupted());
            LockSupport.park();
            System.out.println(Thread.currentThread().isInterrupted());
        });
        t1.start();
        Thread.sleep(1000);
        synchronized (lockObject){
            num = 100;
        }
        System.out.println("执行完毕，获得执行结果");
        t1.interrupt();
        Thread.sleep(1000);
    }

    /**
     * 测试ReentrantLock
     * */
    @Test
    public void testReentrantLock(){
        ReentrantLock reentrantLock = new ReentrantLock();
        new Thread(()->{
            if(reentrantLock.tryLock()){
                System.out.println(Thread.currentThread().getName()+ ":获取到锁，开始执行操作");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                reentrantLock.unlock();
            }else{
                System.out.println(Thread.currentThread().getName()+ "获取锁失败");
            }
        }).start();

        new Thread(()->{
            if(reentrantLock.tryLock()){
                System.out.println(Thread.currentThread().getName()+ ":获取到锁，开始执行操作");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                reentrantLock.unlock();
            }else{
                System.out.println(Thread.currentThread().getName()+ "获取锁失败");
            }
        }).start();

    }
    
    /**
     * 测试信号量
     * */
    @Test
    public void testSemaphore(){
        ReentrantLock reentrantLock = new ReentrantLock();
        Condition condition = reentrantLock.newCondition();
    }


    /**
     * 案例1：一个线程等待另一个线程的结果
     * */

    class GuardianObject{
        private Object result;
        private final Object lock = new Object();

        public Object get() {
            synchronized (lock){
                while(result == null){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        System.out.println("interrupted");
                    }
                }
                return result;
            }
        }

        public void finishWork(Object result){
            synchronized (lock){
                this.result = result;
                lock.notify();
            }
        }
    }
    @Test
    public void waitResult() throws InterruptedException {
        GuardianObject guardianObject = new GuardianObject();

        new Thread(() ->{
            //进行操作
            System.out.println("开始生成结果");
            guardianObject.finishWork("today is a good day");
        }).start();

        Object o = guardianObject.get();
        System.out.println(o);
    }

    /**
     * 案例2：固定轮流执行
     * */

    private boolean runFlag = false;
    @Test
    public void testRound() throws InterruptedException {
        new Thread(() ->{
            while(true){
                if(runFlag){
                    synchronized (lockObject){
                        if(runFlag){
                            //运行
                            System.out.println(Thread.currentThread().getName() + ":正在运行");
                            runFlag = false;
                        }
                    }
                }else{
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, "小明").start();

        new Thread(() ->{
            while(true){
                if(!runFlag){
                    synchronized (lockObject){
                        if(!runFlag){
                            //运行
                            System.out.println(Thread.currentThread().getName() + ":正在运行");
                            runFlag = true;
                        }
                    }
                }else{
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, "小王").start();

        Thread.sleep(10000);
    }

    /**
     * 案例3；生产者消费者
     * */
    public class ConsumerAndProducer<E>{
        Object[] buffer;
        int head;
        int tail;

        public ConsumerAndProducer(int bufferSize){
            buffer = new Object[bufferSize];
            head = buffer.length - 1;
            tail = 0;
        }

        private void offer(E element){
            buffer[tail] = element;
            tail = (tail + 1) % buffer.length;
        }

        private E poll(){
            head = (head + 1) % buffer.length;
            return (E) buffer[head];
        }

        private boolean isFull(){
            return head == tail;
        }

        private boolean isEmpty(){
            return (head + 1) % buffer.length == tail;
        }

        public void produce(E element) throws InterruptedException {
            synchronized (this){
                //等待非空
                while(isFull()){
                    wait();
                }
                System.out.println(head + " " + tail);
                offer(element);
                notifyAll();
            }
        }

        public E consume() throws InterruptedException {
            synchronized (this){
                while (isEmpty()){
                    wait();
                }
                System.out.println(head + " " + tail);
                E curElement = poll();
                notifyAll();
                return curElement;
            }
        }

    }
    @Test
    public void testConsumer() throws InterruptedException {
        ConsumerAndProducer<String> testCase = new ConsumerAndProducer<>(10);

        //三个生产者
        for(int i = 0;i < 3;i++){
            new Thread(() ->{
               for(int j = 0;j < 10;j++){
                   try {
                       testCase.produce("" + j);
                   } catch (InterruptedException e) {
                       throw new RuntimeException(e);
                   }
               }
            }).start();
        }

        //两个消费者
        for(int i = 0;i < 2;i++){
            new Thread(() ->{
                while(true){
                    try {
                        System.out.println("consumer "+ " :" + testCase.consume());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

        }

        Thread.sleep(1000000);
    }
}
