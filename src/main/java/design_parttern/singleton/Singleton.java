package design_parttern.singleton;

/**
 * 单例模式
 * * 懒汉式/饿汉式
 * * 反序列化破坏单例模式
 *   * 重写readResolve()方法，返回当前实例
 *      private Object readResolve() throws ObjectStreamException{
 *          return instance;
 *      }
 * * 反射破坏单例模式
 *  * 在构造方法中判断是否为空，不为空抛出异常
 *    private HungrySingleton() {
 *         if(HUNGRY_SINGLETON  != null){
 *             throw new RuntimeException("此类被设计者设计成单例模式，不允许重复创建对象，请使用静态方法getInstance()获取实例对象。");
 *         }
 *     }
 * */

public class Singleton {

    //普通单例模式实现(懒汉式)
    public class NormalLazySingleton{

        private NormalLazySingleton singleton;

        /**
         * 私有构造方法
         * */
        private NormalLazySingleton(){

        }

        public NormalLazySingleton getSingleton(){
            if(singleton == null){
                singleton = new NormalLazySingleton();
            }
            return singleton;
        }
    }

    //线程安全单例（懒汉式）
    //缺点：获取锁成本较高，大部分情况下不存在并发问题
    //解决：双重校验锁
    public class SafeLazySingleton{
        private static SafeLazySingleton singleton;

        private SafeLazySingleton(){

        }

        public synchronized SafeLazySingleton getSingleton(){
            if(singleton == null){
                singleton = new SafeLazySingleton();
            }
            return singleton;
        }
    }

    //线程安全单例（懒汉式）-静态内部类
    //延迟加载同时避免并发：静态内部类的加载是在程序中调用静态内部类的时候加载的
//    public class SafeStaticLazySingleton{
//        private static class SingletonHolder{
//            public static final SafeStaticLazySingleton singleton = new SafeStaticLazySingleton();
//        }
//
//        private SafeStaticLazySingleton(){}
//
//        public static final SafeStaticLazySingleton getSingle(){
//            return SingletonHolder.singleton;
//        }
//    }

    //普通单例模式实现（饿汉式）
    // 线程安全
    // 缺点：类初始化时加载，占用内存
    public class NormalHungrySingleton{
        private final NormalHungrySingleton singleton = new NormalHungrySingleton();

        private NormalHungrySingleton(){

        }

        public NormalHungrySingleton getSingleton(){
            return singleton;
        }
    }

    //
}
