package jvm;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * 1.  JVM基于栈指令集
 *      * 基于栈的指令集架构：    以零地址指令集为主，操作过程只依赖于操作栈，指令集小，编译器容易实现
 *      * 基于寄存器的指令集架构： 以带地址的指令为主，与硬件的耦合度较高
 * 2. 编译器 vs 解释器
 *      * 编译器：将代码转化为可运行的机器码形式
 *      * 解释器：逐行解释代码并运行
 *      * 两者是运行代码的不同方法，只是逐条和整体的区别
 *      * Java先经过编辑器转化为字节码，再由JVM解释执行
 * 3. 目前已有的虚拟机
 *      * HotSpot: Sun/Oracle JDK和openJDK的默认虚拟机
 *      * JRockit: 不包含解析器实现，即时编译器编译后执行（速度快，启动慢）
 *      * Micorsoft jvm.JVM/Taobao jvm.JVM/Dalvik VM/Graal VM
 *  4. jvm.JVM/JDK/JRE/OpenJDK 之间的区别
 *      * jvm.JVM：用来运行编译后的Java字节码的虚拟机（.class）
 *      * JRE：运行基于Java编写程序的运行环境，用于解释执行Java的字节码文件（jvm.JVM+运行类库）
 *      * JDK：运行环境（JDK）+开发环境（compiler和debugger）
 *      * OpenJDK: 开源的JDK版本，也是OracleJDK的基础，两者之间没有显著的区别（deployment code）
 *      * From Java 11 forward, therefore, Oracle JDK builds and OpenJDK builds will be essentially identical.
 *  5. Java SPI机制
 *      * Service Provider interface，由JDK定义服务的接口，服务提供厂商提供服务的实现
 *      * 核心类为ServiceLoader,具体实现加载的逻辑代码如下:
 *         ServiceLoader<Service> load = ServiceLoader.load(Service.class);
           Iterator<Service> iterator = load.iterator();
           while (iterator.hasNext()){
                Registry Service = iterator.next();
           }
        * 第一行传入线程上下文加载器（默认为AppClassLoader），构造serviceloader对象
        * serviceloader的内部serviceloader，扫描"META-INF/services/"文件，确定需要加载的实现类
 *      * next（）方法不断通过反射加载对应实现类，通过反射机制创建对象
 *      * SPI案例：
 *          * JDBC Driver的加载，不再使用Class.forname()手动加载，而是通过getConnection()基于SPI机制自动加载
 *          * springboot自动配置从CLASSPATH下的每个Jar包中搜寻所有META-INF/spring.factories配置文件
 *
 *
 *

 * */

public class BasicUse {
    ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
    Iterator<Driver> driversIterator = loadedDrivers.iterator();
}
