package jvm;

import org.junit.Test;

import java.io.FileNotFoundException;

/**
 * 类加载相关知识总结
 * 1. 类加载子系统
 *      * 将经过编译的类的字节码文件（.class）从文件系统中加载到方法区中
 *      * 加载的阶段包括：加载->链接->初始化
 *      * 字节码文件经过加载成为Class对象，存放在运行数据区中的方法区中
 *  2. 类加载过程
 *      * 加载阶段：基于类的全限定名获取类的二进制流->转化为方法区运行时数据结构->在堆中生成Class对象
 *      * 链接阶段：验证（校验字节码文件格式）->准备（静态变量默认初始化）->解析（符号引用->直接引用）
 *      * 初始化阶段：只有在类被主动调用时才会进行类的初始化，即执行执行类构造器方法`<clinit>()`的过程（父类先执行）
 *          * <clinit> = 类变量赋值+静态代码块中的语句 按顺序合并
 *          * <clinit> 保证在多线程同步加锁
 *      * Illegal forward reference: 在生命前将静态变量作为右值可能会出现未初始化就调用的问题，编译禁止
 * 3. 类加载器
 *      * Bootstrap ClassLoader -> Extension ClassLoader -> System ClassLoader-> User Defined ClassLoader
 *      * Bootstrap ClassLoader：c/c++语言实现
 *          * 加载Java核心库（JAVA_HOME/jre/lib/rt.jar、resources.jar）
 *          * 并不继承ClassLoader,没有父类
 *      * Extension ClassLoader(PlatformClassLoader): Java语言实现，派生于ClassLoader类，继承于BootStrap ClassLoader
 *          * 从jre/lib/ext下加载类库（用户的jar放在此目录下同样会加载）
 *      * AppClassLoader: 应用程序类加载器
 *          * on the application class path, module path, and JDK-specific tools.
 *          * classLoader.getSystemclassLoader() 获取的就是AppClassLoader
 * 4. 自定义类加载器（打破双亲委派机制）
 *      * 为什么要自定义加载器
 *          * 隔离加载类：不同的框架出现出现全路径名完全相同的类, 通过自定义类加载器实现隔离
 *          * 修改类加载的方式
 *          * 扩展加载源（从别的地方加载）+ 防止源码泄露
 *      * 如何自定义类加载器： 继承ClassLoader类，重写其findClass方法
 *      * 打破双亲委派机制
 *          * 实现自定义类加载器，重写其findClass和loadClass方法
 *          * 案例：Tomcat自定义了WebAppClassLoader加载器，首先加载Web应用自定义的类，如果加载不到在按照
 *                双亲委派机制交给父类加载
 *      * 获取ClassLoader的主要方法
 *          * clazz.getClassLoader(): 每个class对象提供加载其的类加载器
 *          * Thread.currentThread().getContextClassLoader(): 获得当前线程上下文的ClassLoader
 *          * ClassLoader.getSystemClassLoader(): 获得系统ClassLoader
 *      * SPI+线程上下文加载器破坏了双亲委派机制：由于SPI核心接口由JDK提供，所以类加载需要在交给boostarp classloader加载，然而加载完成
 *        对应接口后由于实现类不由jdk提供，因此需要引入线程上下文加载器，父加载器调用线程上下文加载器加载
 *        （线程上下文加载器往往为applicationClassLoader）
 *      * 线程上线文加载器：本质上就是一个随着线程传递的类加载器，由于线程开始于应用程序，所以其中携带的类加载器实际上就是应用程序层次的类加载，
 *        当父类需要加载自己访问不到但是子类能够访问的类时，通过改类加载器完成类加载，这就是SPI的核心
 *      * 上层加载类 依赖 下层加载类，需要线程上下文加载器
 * 5. 双亲委派原理
 *      * 类加载首先向上委托直到最上父类，能加载就加载，不能加载向下传
 *      * 优点：避免类的重复加载；保护程序安全，避免核心API被篡改
 *      * 两个Class对象相同的充分必要条件为：类的全路径名相同+加载类的类加载器对象相同
 * 6. 类加载案例
 *      1. Tomcat
 *          * 需求：
 *              * 同一服务器上的不同web服务类加载保证隔离的同时可共享
 *              * web服务器本身与web应用隔离
 *              * 支持热部署，即加载类的热替换
 *          * 不同组类资源目录，采用不同的加载策略（Tomcat为例）
 *              * /common：类库提供给tomcat和所有的web服务使用
 *              * /server：tomcat自身使用，对所有web服务不可见
 *              * /shared：对tomcat不可见，所有web服务共享
 *              * /WebApp/WEB-INF：仅当前web服务可用
 *          * 对应加载器设计策略：
 *              * CommonClassLoader(加载/common)
 *                  * CatalinaClassLoader(加载/server)
 *                  * SharedClassLoader(加载/shared) -> WebappClassLoader(加载/WEB-INF)  -> JasperLoader
 *
 * */

public class ClassLoadLearn {
    /**
     * 类加载初始化阶段案例
     * */
//    private static int num = 1;
//
//    static{
//        num = 2;
//        //System.out.println(number);
//        number = 20;
//    }
//
//    /**
//     * 1、linking之prepare: number = 0 --> initial: 20 --> 10
//     * 2、这里因为静态代码块出现在声明变量语句前面，所以之前被准备阶段为0的number变量会
//     * 首先被初始化为20，再接着被初始化成10（这也是面试时常考的问题哦）
//     *
//     */
//    private static int number = 10;
//
//    public static void main(String[] args) {
//        System.out.println(ClassLoadLearn.num);//2
//        System.out.println(ClassLoadLearn.number);//10
//    }

    @Test
    public void testClassLoader(){
        ClassLoader classLoader = ClassLoadLearn.class.getClassLoader();
        System.out.println(classLoader.getParent());
        System.out.println(classLoader.getParent().getParent());
        System.out.println(classLoader);
    }
    /**
     * 自定义类加载器
     * */

    public class MyClassLoader extends ClassLoader{
        @Override
        protected Class<?> findClass(String moduleName, String name) {
            try {
                byte[] result = getClassFromCustomPath(name);
                if (result == null) {
                    throw new FileNotFoundException();
                } else {
                    //defineClass和findClass搭配使用
                    return defineClass(name, result, 0, result.length);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                throw new ClassNotFoundException(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //自定义流的获取方式
    private byte[] getClassFromCustomPath(String name) {
        //从自定义路径中加载指定类:细节略
        //如果指定路径的字节码文件进行了加密，则需要在此方法中进行解密操作。
        return null;
    }

    /**
     * 测试不同获取类加载器的方法
     * */
    @Test
    public void getMyClassLoader(){
        //1.基于class对象
        ClassLoader classLoader = ClassLoadLearn.class.getClassLoader();
        System.out.println(String.class.getClassLoader());
        System.out.println(classLoader.getParent());
        //2.线程上下文
        System.out.println(Thread.currentThread().getContextClassLoader());
        //3.系统ClassLoader
        System.out.println(ClassLoader.getSystemClassLoader());

        Class<String> c = String.class;
    }

}
