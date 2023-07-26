package concurrent;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DataTruncation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.*;

/**
 * 一、线程相关知识点
 * 1. 实现线程的3种方式
 *  * 继承Thread类，并重写对应的run()方法
 *  * 实现Runnable接口的run方法，作为参数传入Thread对象的初始化方法
 *     public void run() {
 *         if (target != null) {
 *             target.run();
 *         }
 *     }
 *   * 实现Callable接口，作为FutureTask的参数，FutureTask作为Thread的参数实现多线程
 *   * 基于Runnable接口的方法由于传入的是对象引用，当重复使用一个对象创建线程时，需要注意互斥问题
 * 2. 线程初始化问题 Thread(ThreadGroup g, Runnable target, String name,
 *                    long stackSize, AccessControlContext acc,
 *                    boolean inheritThreadLocals)
 * * 只有run方法重载或者传入了继承Runnable接口的对象，才会指定run方法，否则相当于不执行
 * * ThreadGroup:所有线程以组为单位进行操作，如设置优先级、守护线程，若初始化时不传入线程组，则默认和父线程同一个组
 *    if (g == null) {
 *                 g = parent.getThreadGroup();
 *    }
 *  * stackSize:指定当前线程占用stack大小，默认为0表示忽略该参数（JVM一旦启动，虚拟机栈的大小已经确定了）
 * 3. 线程只允许被启动一次，多次启动抛出异常
 *      if (threadStatus != 0)
 *             throw new IllegalThreadStateException();
 * 4. run() 和 start()区别
 *  * run（）只是一个普通方法，直接调用相当于在主线程中调用了一个普通方法
 *  * start()底层会调用start0()方法，在该方法中对run()方法进行调用
 *  * Java Native Interface (JNI)，它允许Java代码和其他语言写的代码进行交互
 * 5. 线程状态：new runnable blocked waiting(调用wait方法) time_waiting（sleep） terminated
 * 6. sleep() 和 yield方法
 *  * sleep()方法：runnable-> time_waiting， 其他线程interrupt能够中断休眠状态，抛出InterruptException
 *  * yield()方法：running -> runnable(让出CPU时间片)
 * 7. interrupted() 中断线程状态
 *  * 当线程处于wait(),sleep()，join()导致的waiting状态时，interrupted() 中断线程抛出{@link InterruptedException}
 *   并重置interrupted状态
 *      public static boolean interrupted() {
 *         Thread t = currentThread();
 *         boolean interrupted = t.interrupted;
 *         if (interrupted) {
 *             t.interrupted = false;
 *             clearInterruptEvent();
 *         }
 *         return interrupted;
 *     }
 *  * 运行时调用interrupted状态，会导致interrupted = true,即通过interrupted线程可判断自身是否被中断
 * 8. 不可变对象
 *  * 当对象为有状态的，且线程操作会影响对象的状态，状态又会影响对象行为，在并发情况下就可能出现race condition
 *  * 例如：SimpleDateFormat（线程不安全）->  DateTimeFormatter
 *  * final修饰对象为不可变对象，方法不可重写
 *
 * 二、线程池相关
 * 1. 线程池 ThreadPoolExecutor 实现原理(一个工作线程数组+一个满足生产者消费者模式的工作队列)
 *  * 使用int高3位表示线程池状态，低29位表示线程数量
 *      * RUNNIING, SHUTDOWN(不接收任务，处理阻塞对象中的任务), STOP（完全停止）, TIDYING（即将进入终结）, TERMINATED（终结状态）
 *      * 使用一个int存储状态+线程数量，是为了通过cas操作保证状态修改的原子性
 *  * 线程池执行流程
 *      * 随着任务的不断增加，线程池不断创建线程处理任务
 *      * 当线程数达到corePoolSize且无线程空闲时，任务加入到阻塞队列
 *      * 若设置为有界队列，线程池最多创建 maximunPoolSize - corePoolSize 个线程救急
 *      * 当线程数到达maximunPoolSize后，还有无法处理的任务，执行拒绝策略
 *          * 默认AbortPolicy 让调用者抛出 RejectedExecutionException 异常
 *          * CallerRunsPolicy 让调用者运行任务
 *          * DiscardPolicy 放弃本次任务
 *          * DiscardOldestPolicy 放弃队列中最早的任务，本任务取而代之
 *      * 当任务需求下降，根据keepAliveTime停止线程资源
 *  * 关闭线程池方法
 *      * shutdown() ->    SHUTDOWN
 *      * shutdownNow() -> STOP
 *  * 获取固定配置线程池
 *     * 延迟线程线程池：Executors.newScheduledThreadPool(1);  DelayedWorkQueue：
 *     * 单线程线程：Executors.newSingleThreadExecutor();     LinkedBlockingQueue：无限长队列
 *     * 固定线程池：Executors.newFixedThreadPool(10);        LinkedBlockingQueue：无限长队列
 *     * 临时线程线程池：Executors.newCachedThreadPool();      SynchronousQueue：放进队列元素被消费后才能返回
 *  * 任务调度线程池
 *     * {@link java.util.Timer} jdk5.0之前用来实现任务调度功能
 *          * 多生产者，单消费者（执行任务线程只有一个）的调度模型
 *          * 消费线程不断读取优先队列（开始执行时间为优先级）中的任务，根据开始时间休眠到对应时间点，执行任务，重复进行循环
 *    * Executors.newScheduledThreadPool(2) 调度线程池
 *          * 多生产者，多消费者的调度模型, 相较于Timer可以设置多个消费线程
 *          * 一个延迟调度方法：.schedule()
 *          * 两个周期调度方法：.scheduleAtFixedRate()/.scheduleWithFixedDelay(),两者区别在于后者在
 *            上一个任务完成的时间基础上进行延迟。
 *   * ExecutorService:在线程池的基础上增加了对于异步任务的支持
 *
 * * ThreadLocal 和 InheritableThreadLocal
 *     * ThreadLocal: 保存线程的变量，线程之间相互隔离
 *          * 原理：每个线程将自身持有的所有ThreadLocal放在自身的ThreadLocalMap中
 *                 其中Key为对象哈希值，value为ThreadLocal存储的值
 *          * ThreadLocal.ThreadLocalMap threadLocals
 *     * InheritableThreadLocal：父子线程之间实现线程参数传递
 *          * getMap()获得ThreadLocalMap对象从threadLocals->inheritableThreadLocals，
 *            inheritableThreadLocals在创建时会复制父线程中的ThreadLocal对象
 *
 *          * ThreadLocal.ThreadLocalMap inheritableThreadLocals
 * * Fork/Join：JDK1.7加入的新的线程池实现，分治实现cpu密集型运算
 *     * 分治任务继承 RecursiveTask（有返回值）或者 RecursiveAction(无法返回值)，实现compute方法（）
 *     * 通过调用fork()/join()实现多线程递归
 protected Integer compute() {
 if(begin == end){
 return aimArray[begin];
 }
 int middle = (begin + end) / 2;

 AddTask leftTask = new AddTask(begin, middle, aimArray);
 AddTask rightTask = new AddTask(middle + 1, end, aimArray);
 leftTask.fork();
 rightTask.fork();
 return leftTask.join() + rightTask.join();
 }
 *
 * */


public class ThreadTest {


    Logger logger;
    @Before
    public void initLog(){
        logger = LoggerFactory.getLogger(ThreadTest.class);
    }

    /**
     * 1.*********************************************************************************************************
     *  线程基本使用
     * */
    class MyThread extends Thread{
        @Override
        public void run(){
            for(int i = 0; i < 100; i++) {
                if(i % 2 == 0){
                    System.out.println(Thread.currentThread().getName() + ":" + i);
                }
            }
        }
    }

    class MyThreadTwo implements Runnable{

        @Override
        public void run() {
            for(int i = 0; i < 100; i++) {
                if(i % 2 == 0){
                    System.out.println(Thread.currentThread().getName() + ":" + i);
                }
            }
        }
    }

    class MyThreadThree implements Callable<String>{
        @Override
        public String call() throws Exception {
            for (int x = 0; x < 10; x++) {
                System.out.println("*******线程执行，x=" + x + "********");
            }
            return "线程执行完毕";
        }
    }


    @Test
    public void TestBasicUse() throws ExecutionException, InterruptedException {
        MyThread t1 = new MyThread();
        MyThread t2 = new MyThread();
        t1.start();
        t2.start();

        //如下操作仍然是在main线程中执行的。
        for (int i = 0; i < 100; i++) {
            if(i % 2 == 0){
                System.out.println(Thread.currentThread().getName() + ":" + i + "***********main()************");
            }
        }

        //测试Runnable线程的使用方法
        Thread t3 = new Thread(new MyThreadTwo());
        t3.setName("runnable");
        t3.start();

        //测试Callable线程使用方法
        FutureTask<String> task = new FutureTask<>(new MyThreadThree());
        new Thread(task).start();
        System.out.println("线程返回数据" + task.get());

    }

    @Test
    public void TestJoin() throws InterruptedException {
        MyThread t1 = new MyThread();
        t1.start();
        t1.join();
        Optional.of("All of tasks finish done.").ifPresent(System.out::println);
    }

    @Test
    public void testInterrupt() throws InterruptedException {
        Thread t1 = new Thread(() ->{
            while(true){
                //不断检查自身状态
                Thread current = Thread.currentThread();
                if(current.isInterrupted()){
                    System.out.println("当前线程被打断");
                    break;
                }
            }
        });
        long l = System.currentTimeMillis();
        t1.start();
        t1.join(1000);
        t1.interrupt();
        System.out.println(System.currentTimeMillis() - l);
    }

    /**
     * 基于Interrupt实现主线程通知从线程推出
     * */

    @Test
    public void testInform() throws InterruptedException {
        Thread t1 = new Thread(() ->{
            while(true){
                //不断检查自身状态
                Thread current = Thread.currentThread();
                if(current.isInterrupted()){
                    System.out.println("清理线程，准备退出");
                    break;
                }
                try {
                    Thread.sleep(2000);
                    System.out.println("正在处理工作");
                } catch (InterruptedException e) {
                    System.out.println("被中断");
                    current.interrupt();
                }
            }
        });

        t1.start();
        Thread.sleep(1000);
        t1.interrupt();
        System.out.println("主线程退出");
    }

    @Test
    public void testVariableObject() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse("1998-08-19");
        System.out.println(parse);
        for (int i = 0; i < 10; i++) {
            new Thread(() ->{
                try {
                    sdf.parse("1998-08-19");
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    @Test
    public void testConstObject(){
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate parse = sdf.parse("1998-08-19", LocalDate::from);
        System.out.println(parse);
        for (int i = 0; i < 10; i++) {
            new Thread(() ->{
                sdf.parse("1998-08-19", LocalDate::from);
            }).start();
        }
    }


    /**
     * 2.*********************************************************************************************************
     * 案例一：线程池使用
     * */
    public void threadExecutor() throws InterruptedException, ExecutionException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10,15,
                1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        //常见接口
        //1.执行单个任务
        executor.execute(()->{

        });

        //2.回调任务
        Future<?> submit = executor.submit(() -> {
        });

        //3.批量提交任务
        List<Future<Object>> futures = executor.invokeAll(new ArrayList<>());

        //4.提交任务，第一个返回的正常执行，其他取消
        Object o = executor.invokeAny(new ArrayList<>());
        Executors.newScheduledThreadPool(1);
        Executors.newSingleThreadExecutor();
        Executors.newFixedThreadPool(10) ;
        ExecutorService executorService = Executors.newCachedThreadPool();
    }

    /**
     * 测试Timer
     * */
    @Test
    public void testTimer() throws InterruptedException {
        Timer timer = new Timer();
        //两个TimerTask
        TimerTask t1 = new TimerTask() {
            @Override
            public void run() {
                logger.info(Thread.currentThread().getName() + " finish task 1， sleep 1s");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        TimerTask t2 = new TimerTask() {
            @Override
            public void run() {
                logger.info(Thread.currentThread().getName() + " finish task 2");
            }
        };

        timer.schedule(t1, 1000);
        timer.schedule(t2, 1000);
        Thread.sleep(10000);
        System.out.println(Thread.currentThread().getName());
    }

    /**
     * 任务调度线程池
     * */
    @Test
    public void testScheduleTP() throws InterruptedException {
        ScheduledExecutorService se = Executors.newScheduledThreadPool(2);

        se.schedule(() ->{
            try {
                logger.info(Thread.currentThread().getName() + " finish Work 1");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 0, TimeUnit.SECONDS);

        se.schedule(() ->{
            try {
                logger.info(Thread.currentThread().getName() + "start to Work 2");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 0, TimeUnit.SECONDS);

        ScheduledFuture<?> schedule = se.schedule(() -> {
            logger.info(Thread.currentThread().getName() + "start to Work 3");
        }, 0, TimeUnit.SECONDS);
        //schedule.cancel(true);

        Thread.sleep(10000);
        //每周四下午六点执行
        //1.获取周四下午6点的时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime aimTime = now.with(DayOfWeek.THURSDAY).withHour(18).
                withMinute(0).withSecond(0).withNano(0);
        if(now.compareTo(aimTime) >=0){
            aimTime = aimTime.plusWeeks(1);
        }
        long initialDelay = Duration.between(now, aimTime).toMillis();
        se.scheduleAtFixedRate(() ->{logger.info("happy thursday");}, initialDelay,
                7 * 24 * 60 * 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Fork/join
     * */

    public int multiThreadAdd(InheritableThreadLocal<int[]> rangeLocal,
                              int[] aimArray, ExecutorService executorService) throws ExecutionException, InterruptedException {
        int[] range = rangeLocal.get();
        if(range[0] == range[1]){
            return aimArray[range[0]];
        }
        int middle = (range[0] + range[1]) / 2;

        //求左边区间
        rangeLocal.set(new int[]{range[0], middle});
        Future<Integer> left = executorService.submit(() -> {
            try {
                return multiThreadAdd(rangeLocal, aimArray, executorService);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        //右区间
        rangeLocal.set(new int[]{middle + 1, range[1]});
        Future<Integer> right = executorService.submit(() -> {
            try {
                return multiThreadAdd(rangeLocal, aimArray, executorService);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return left.get() + right.get();
    }

    public int add(int[] aimArray){
        int result = 0;
        for (int i = 0; i < aimArray.length; i++) {
            result += aimArray[i];
        }
        return result;
    }

    @Test
    public void testMyMultiThread() throws ExecutionException, InterruptedException {
        int[] aimArray = {1,2,3,4,5,6,7,8,9,10};
        InheritableThreadLocal<int[]> range = new InheritableThreadLocal<>();
        ExecutorService executorService = Executors.newCachedThreadPool();
        range.set(new int[]{0, aimArray.length - 1});
        logger.info("call multiThreadAdd:");
        logger.info("result:" + multiThreadAdd(range, aimArray, executorService));
        logger.info("call normalAdd:");
        logger.info("result:" + add(aimArray));
        ForkJoinPool forkJoinPool = new ForkJoinPool(5);
        logger.info("call forJoinAdd:");
        logger.info("result:" + forkJoinPool.invoke(new AddTask(0, aimArray.length - 1, aimArray)));
    }

    /**
     * Fork/joinTest
     * */
    class AddTask extends RecursiveTask<Integer>{
        int begin;
        int end;
        int[] aimArray;

        public AddTask(int begin, int end, int[] aimArray) {
            this.begin = begin;
            this.end = end;
            this.aimArray = aimArray;
        }

        @Override
        protected Integer compute() {
            if(begin == end){
                return aimArray[begin];
            }
            int middle = (begin + end) / 2;

            AddTask leftTask = new AddTask(begin, middle, aimArray);
            AddTask rightTask = new AddTask(middle + 1, end, aimArray);
            leftTask.fork();
            rightTask.fork();
            return leftTask.join() + rightTask.join();
        }
    }
}
