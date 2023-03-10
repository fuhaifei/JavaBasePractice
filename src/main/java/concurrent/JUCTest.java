package concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * JUC中工具以及原理总结
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
 * */

public class JUCTest {
    /**
     * 案例一：自定义线程池
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
        Executors.newCachedThreadPool();

    }

}
