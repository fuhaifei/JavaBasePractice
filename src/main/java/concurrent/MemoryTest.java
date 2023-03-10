package concurrent;

import org.junit.Test;
/**
 * JMM：Java Memory Model
 *  * 对底层CPU寄存器、缓存、硬件内存、CPU指令优化的抽象，定义了主存、工作内存等抽象概念
 *  * 包含三个性质： 1. 原子性-指令不会受到线程上下文切换的影响
 *               2. 可见性-保证指令不会受到CPU缓存的影响
 *               3. 有序性-保证指令不会受到CPU指令并行优化的影响
 * 可见性（volatile）
 *  * 为了降低线程访问主存的频率，线程会将访问变量复制到自身工作内存，导致不同线程见数据不可见
 *  * 通过volatile关键字修饰（成员变量和静态变量），限制线程只能操作主存中的变量
 *  * volatile 无法解决i++的race condition
 *  * 底层原理：读屏障（读主存，后面的指令不会到前面），写屏障（同步到主存，前面的指令不会到后面）
 * 有序性
 *  * JVM会在不影响正确性的前提下，调整语句的执行顺序
 *  * 进行指令重排的原因：不冲突的指令并行，实现更高效的执行
 * */
public class MemoryTest {


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
}
