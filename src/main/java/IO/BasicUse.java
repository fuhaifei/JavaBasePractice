package IO;

import java.io.*;

/**
 * Java流输入输出
 * 1. Java主要的输入输出流
 *      * 按照数据单位分为:字节流（8bit Stream结尾）和字符流(16bit Reader/Writer结尾)
 *      * 按照流的类型划分为：节点流（直接从数据源或者目的地读写数据） 和 处理流（连接已经存在的流）
 * 2. 常用实现类
 *      * Reader和Writer抽象类，定义了字符流输入输出的基本方法
 *          * write(char/char[])/read(char/char[])/flush()/open()/close()
 *          * FileReader/FileWriter：以字符的形式操作文件的输入输出
 *          * 字符缓冲流：BufferedReader，BufferedWriter（8KB缓冲区，每次程序操作缓冲区，缓冲区空/满再与文件系统交互）
 *              * 可以按行读取写入：readLine()/newLine()
 *      * InputStream和OutputStream抽象类，定义了字节流输出输出的基本方式
 *          * write(byte/byte[])/byte/byte[])/flush()/open()/close()
 *          * 文件字节流：FileInputStream\FileOutputStream
 *          * 字节缓冲流： BufferedInputStream，BufferedOutputStream
 *      * 处理流(在其他流的基础上进行操作和处理)
 *          * InputStreamReader(InputStream inn, String charsetName):/InputStreamWriter()
 *      * 数据输出输入流：DataOutputStream、DataInputStream（基础数据类型+字符串类型）
 *      * 对象输入输出流：ObjectOutputStream、ObjectInputStream（）
 *          * writeXXX()(各种数据类型), writeObject(Object obj)
 * 3. 字符集
 *      * ASCII字符集（0+7bit）表示共128个字符
 *      * ISO-8859-1: LATIN-1，单字节编码兼容ASCII
 *      * GBXXX字符集：为了显示中文设计的字符集:GB2312(单字节兼容ASCII，双字节表示中文)/GBK(双字节编码)/GB18030
 *      * Unicode: 2字节统一编码，问题：无法判断两个字节为两个ASCII字符/还是一个Unicode字符
 *      * UTF-8(1-4字节)/UTF-16(2/4字节)/UTF-32
 * 4. 序列化机制
 *      * 一个对象可以被表示为一个字节序列，该字节序列包括该对象的数据、有关对象的类型的信息和存储在对象中数据的类型。
 *      * 序列化条件：类实现Serializable接口，引用类型属性必须也为可序列化的，或者标注为transient。
 *      * 静态（static）变量的值不会序列化
 *      * SerializableID 号是根据类的特征和类的签名算出来的。为什么 ID 号那么长，是因为为了避免重复。
 *        所以 Serializable 是给类加上 id 用的。用于判断类和对象是否是同一个版本。
 * 5. Java基础IO概念
 *      * IO的两个阶段
 *          * 阶段1：操作系统等待数据准备好
 *              * 网络IO：等待接收到足够的网络分组，等待数据被拷贝到操作系统内核缓冲区
 *              * 普通文件IO：等待文件准备好
 *          * 阶段2：复制到应用进程缓冲区
 *      * UNIX I/O模型
 *          1. 阻塞式IO
 *              * 两个阶段均阻塞：等待数据准备好并复制到用户进程空间缓冲区
 *          2. 非阻塞式IO
 *              * 第一阶段非阻塞，第二阶段阻塞
 *              * 进程轮询调用recvfrom,若数据为准备好则报错（EWOULDBLOCK），直到数据准备好；进程阻塞直到数据复制到用户进程空间
 *          3. I/O复用模型
 *              * 用户态：调用select方法，阻塞到IO事件发生，轮询所有流得到对应的文件描述符并拷贝到内核态
 *              * 内核态：检查每个fd标志位判断是否需要进行处理
 *              * 相较于阻塞式IO，可同时监控多个文件的读写
 *              * 三种实现
 *                  * select: 打开的文件描述符数量限制为1024个；fd从用户态->内核态；轮询效率低
 *                  * poll: 使用链表改进，不存在数量限制
 *                  * epoll: 基于红黑树+链表+回调机制，避免了轮询的代价，fd集合维护在内核态
 *         4. 信号驱动式IO模型
 *              * 信号+信号处理程序，当内核准备好数据后，发送SIGIO信号给用户进程，用户进程调用recfrom读取数据
 *              * 第一阶段执行完成发送信号，用户金册灰姑娘接收到信号进入第二阶段
 *         5. 异步IO模型
 *              * 告知内核启动某个操作，并让内核在整个操作完全进行完成之后通知进程。
 *              * 与信号的区别在于前者第一阶段结束发送信号，后者第二阶段结束才发送信号
 *       * JAVA IO模型
 *          1. BIO（Bloking I/O）：同步阻塞IO
 *              * 一个Accpet不断地调用socket.accpet(),一旦建立连接，开辟一个线程进行IO操作（socket.read(), socket.write()）
 *              * 一个请求一个线程：线程开辟和上下文切换成本较高
 *              * 改进：伪异步IO。基于线程池控制线程开辟的成本问题
 *          2. NIO（New I/O）:支持面向缓冲的，基于通道的I/O操作方法
 *              * jdk 1.4引入的NIO框架，对应java.nio包，包括Channel, Selector, Buffer
 *              * 三个核心组件：Selector（遍历） Channel（双向连接） Buffer（读取写入缓冲），底层基于epoll实现
 *              * 与普通IO流的区别：非阻塞，面向缓冲区，基于Channel概念可读可写
 *          3. AIO（Asynchronous I/O）
 *              * Java 7 中引入了 NIO 的改进版 NIO 2,它是异步非阻塞的IO模型
 *              * 异步 IO 是基于事件和回调机制实现的，也就是应用操作之后会直接返回，不会堵塞在那里，
 *                当后台处理完成，操作系统会通知相应的线程进行后续的操作
 * */
public class BasicUse {

}
