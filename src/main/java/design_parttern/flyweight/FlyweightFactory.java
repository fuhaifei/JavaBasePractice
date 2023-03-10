package design_parttern.flyweight;

import org.junit.Test;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class FlyweightFactory {
    Map<String, FlyweightInterface> map;

    public FlyweightFactory(){
        map = new HashMap<>();
    }

    public FlyweightInterface getFlyweight(String flyKey){
        if(!map.containsKey(flyKey)){
            map.put(flyKey, new Flyweight(flyKey));
        }
        return map.get(flyKey);
    }

    @Test
    public void testFlyweight(){
        FlyweightFactory flyweightFactory = new FlyweightFactory();
        System.out.println(flyweightFactory.getFlyweight("a"));
        System.out.println(flyweightFactory.getFlyweight("a"));
    }

//    Byte, Short, Long 缓存的范围都是 -128~127
//    Character 缓存的范围是 0~127
//    Integer的默认范围是 -128~127
//    最小值不能变
//    但最大值可以通过调整虚拟机参数 `
//            -Djava.lang.Integer.IntegerCache.high` 来改变
//    Boolean 缓存了 TRUE 和 FALSE
    @Test
    public void testInteger(){
        Integer a = Integer.valueOf(100);
        Integer b = 100;

        Integer c = Integer.valueOf(1000);
        Integer d = 1000;
        Integer e = 10;
        Integer f = 10;

        System.out.println("a==b:" + (a==b));
        System.out.println("c==d:" + (c==d));
        System.out.println("e==f:" + (e==f));

        Long l = Long.valueOf(10);
    }
    //自定义链接库
    public class MyConnection{
        private String url;
        private String username;
        private String password;

        public MyConnection(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        public void close(){
            System.out.println("连接已关闭");
        }


    }

    public class ConnectionPool{
        //存储链接和对象状态
        private int poolSize;
        private MyConnection[] connections;
        private AtomicIntegerArray status;

        //链接初始化属性

        private String url;
        private String username;
        private String password;

        public ConnectionPool(int poolSize, String url, String username, String password) {
            this.poolSize = poolSize;
            connections = new MyConnection[poolSize];
            status = new AtomicIntegerArray(poolSize);
            this.url = url;
            this.username = username;
            this.password = password;
        }

        public MyConnection getConnection() throws InterruptedException {
            synchronized (this){
                while(true){
                    for(int i = 0;i < poolSize;i++){
                        if(connections[i] == null){
                            connections[i] = new MyConnection(url, username, password);

                        }
                        if(status.compareAndSet(i, 0, 1)){
                            return connections[i];
                        }
                    }
                    //不存在可用链接，就等待链接释放
                    wait();
                }
            }
        }

        public void close(Connection connection){
            for(int i = 0;i < poolSize;i++){
                if(connections[i] == connection){
                    status.set(i, 0);
                    synchronized (this) {
                        this.notifyAll();
                    }
                }
            }
        }
    }
}
