package design_parttern.singleton;

public class SafeStaticLazySingleton{
    private static class SingletonHolder{
        public static final SafeStaticLazySingleton singleton = new SafeStaticLazySingleton();
    }

    private SafeStaticLazySingleton(){}

    public static final SafeStaticLazySingleton getSingle(){
        return SingletonHolder.singleton;
    }
}