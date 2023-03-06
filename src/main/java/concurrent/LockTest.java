package concurrent;


import org.junit.Test;

/**
 * 同步相关
 * 1. synchronized 对象锁解决临界区的Race Condition
 *  * 非静态方法上的synchronized等价于synchronized(this)          对象锁
 *  * 静态方法上的synchronized等价于synchronized(this.getClass()) 类对象锁
 *  * 每个Java对象关联一个Monitor(管程)，包含三个结构
 *      * Owner：当前通过synchronized得到对象锁的线程
 *      * EntryList：等待获取对象锁的线程列表，阻塞在synchronized代码块的线程（blocked）
 *      * WaitSet: 获得过锁，但天骄不满足进入WAITING状态的线程
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

}
