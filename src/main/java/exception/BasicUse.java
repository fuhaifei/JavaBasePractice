package exception;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;

/**
 * Java异常体系
 * 1. 异常接口体系
 *      * Throwable(顶层接口)
 *          * Error（Java虚拟机解决的问题）:导致程序处于非正常状态，无法被捕获处理
 *              * VirtualMachineError（StackOverFlowError, OutOfMemoryError）
 *              * ThreadDeath
 *          * Exception（编程错误或者偶然的外在因素导致的一般性问题）
 *              * RuntimeException（运行时异常）unchecked
 *                  * Arithmetic Exception/Illegal Argument Exception/IndexOutOfBoundsException
 *              * 非运行时异常（编译时异常，由编译器检测的异常）checked
 *                  * IOException/ClassNotFoundException/CloneNotSupportedException
 * 2. 处理异常的方式
 *      * try-catch-finally（捕获并处理）
 *          * e.getMessage()
 *          * e.printStackTrace()
 *          * finally()先于return执行，但是并不会改变返回值，除非return语句在finally块内
 *      * try-catch-finally（捕获并处理）
 *          * e.getMessage()
 *          * e.printStackTrace()
 *          * finally()先于return执行，但是并不会改变返回值，除非return语句在finally块内
 *      * throw（向上抛出）
 *          * 带有抛出异常的方法重写时：子类方法异常范围<=父类方法中抛出的异常范围
 *          * Controller层实现统一异常处理
 * 3. 自定义异常类
 *      * 继承异常类型：Exception/RuntimeException
 *      * 提供默认构造器和message构造器
 *      * 提供serialVersionUID
 * */

public class BasicUse {
}
