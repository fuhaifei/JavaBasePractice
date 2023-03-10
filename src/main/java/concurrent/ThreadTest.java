package concurrent;

import org.junit.Test;

import java.sql.DataTruncation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * 线程相关知识点
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
 * */

public class ThreadTest {

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
}
