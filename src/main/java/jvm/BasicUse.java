package jvm;

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
 *      * Micorsoft JVM/Taobao JVM/Dalvik VM/Graal VM
 *  4. JVM/JDK/JRE/OpenJDK之间的区别
 *      * JVM：用来运行编译后的Java字节码的虚拟机（.class）
 *      * JRE：运行基于Java编写程序的运行环境，用于解释执行Java的字节码文件（JVM+运行类库）
 *      * JDK：运行环境（JDK）+开发环境（compiler和debugger）
 *      * OpenJDK: 开源的JDK版本，也是OracleJDK的基础，两者之间没有显著的区别（deployment code）
 *      * From Java 11 forward, therefore, Oracle JDK builds and OpenJDK builds will be essentially identical.

 * */

public class BasicUse {
}
