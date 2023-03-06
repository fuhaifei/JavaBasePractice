package concurrent;

import org.junit.Test;
/**
 * 线程相关知识点
 * 1. 实现线程的两种方式
 *  * 继承Thread类，并重写对应的run()方法
 *  * 实现Runnable街口的run方法
 *
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


    @Test
    public void TestBasicUse(){
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
    }
}
