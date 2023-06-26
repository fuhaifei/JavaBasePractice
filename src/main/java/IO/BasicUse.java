package IO;

import org.junit.Test;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

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
 *              * 第一阶段执行完成发送信号，用户进程接收到信号进入第二阶段
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
 *      * DMA（Direct Memory Access）：代理CPU的读取功能，负责将磁盘中的数据传输到内存中
 *          * 从磁盘->内核空间这一步由DMA代理
 *      * 零拷贝概念
 *          * 一次传统IO涉及到两次内核与用户空间之间的缓存拷贝
 *              * 读：进程读取数据->陷入内核态，数据从磁盘到内核缓冲区/socket缓冲区->CPU将内核数据复制到用户空间->用户处理
 *              * 写：用户写数据,写入到用户缓存空间->陷入内核态，数据从用户缓存孔教待内核缓冲->写入磁盘或者socket发送
 *              * 均涉及到两次拷贝：用户缓存+内核缓存+硬件缓存（磁盘/网卡）
 *          * 零拷贝：减少数据传输中不同缓冲区之间的复制操作
 *          * 实现方案：
 *              1. mmap：将内核空间映射到用户进程虚拟空间，用户进程直接操作内核空间，避免了内核空间到用户空间的复制
 *                  * 多个进程可共享相同文件的物理映射
 *                  * 基于DMA可避免CPU的参与，由DMA负责读取以及脏页刷新
 *              2. sendfile：优化了读取文件发送到网络的逻辑。直接从内核缓冲区拷贝到网卡中进行发送，不需要再涉及到用户空间和socket缓冲
 *                  * 内核空间内传递文件描述符，不再写入socket缓冲区，而是直接使用文件描述符向网卡写入
 *                  * 文件描述符->socket的通信
 *              3. splice：sendfile的升级，实现了任意两个文件描述符之间互相通信
 *
 *
 * 6. Java NIO深入学习
 *      * Buffer：面向缓冲读写
 *          * 包含八个主要实现类：7种数据类型+MappedByteBuffer(内存映射的ByteBuffer),主要使用ByteBuffer
 *          * 四个关键属性：capacity(容量)+position(读写位置)+limit(读取写入数据的最大上限)+mark(读写位置临时备份)
 *          * Buffer创建时为写模式，通过flip()可以转化为读模式，clear()/compact()方法可以转化回写模式
 *          * buffer接口：rewind()/mark()/reset()/clear() 子类中接口:get()/put()/compact()[保留未消费元素的状态转换]
 *          * 所有xxxBuffer均为抽象类，调用acllocate方法创建的为其HeapxxxBuffer子类类型，即默认存储在堆上
 *      * Channel：文件句柄
 *          * 定义了两个接口：isOpen()/close()
 *          * 包含四个主要实现类：
 *              * FileChannel：文件读写
 *              * SocketChannel：套接字读写
 *              * ServerSocketChannel：服务监听通道
 *              * DatagramChannel： 数据报通道，用于UDP的数据读写
 *          * 阻塞模式不需要使用selector,下使用与Socket基本相同
 *              * SocketChannel/DatagramChannel基于ByteBuffer输入输出
 *                  * DataGramChannel无连接，调用connect方法锁定一个发送方
 *              * Socket基于InputStream/OutputStream输入输出
 *              * DatagramSocket基于DatagramPacket实现输入输出
 *     * Selector:监控多个通信链路的IO事件
 *          * Selector -> SelectionKey -> Channel
 *              * Selector监控所有的SelectionKey（interestOps + readyOps）
 *              * 一个Channel对应一个SelectionKey
 *          * IO事件类型包括（SelectionKey）
 *              * 可读：   OP_READ     (第0位)
 *              * 可写：   OP_WRITE   （第1位）
 *              * 连接建立：OP_CONNECT （第2位）
 *              * 接受连接：OP_ACCEPT  （第3位）
 *          * 基本的使用流程:
 *              1. 获得selector对象
 *                  Selector selector = Selector.open()
 *              2. 注册Channel到selector对象上
 *                  serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
 *              3. 获取处理IO事件
 *                 while(selector.select() > 0){
 *                     Set selectedKeys = selector.selectedKeys();
 *                     Iterator keyIterator = selectedKeys.iterator();
 *                     while(keyIterator.hashNext(){
 *                         SelectionKey key = keyIterator.next();
 *                         根据key的类型进行处理
 *                     }
 *                 }
 *          * 使用注意事项
 *              * 每次处理完相关事件后，需要手动清空就绪key列表
 *              *
 *          * 补充：
 *              * 注册之后，通过select()通过JNI调用底层系统调用，更新readyOps
 *              * 只有实现SelectableChannel接口的Channel类型才能注册到Selector进行监听（FileChannel未实现）
 *              * 向Selector注册的Channel对象必须为非阻塞状态
 *              * selector.wakeup() 会唤醒阻塞在select.select()的线程，禁止没有channel准备就绪
 *
 *
 * */
public class BasicUse {
    
    private ByteBuffer byteBuffer;
    
    public static void main(String[] args) throws IOException, InterruptedException {
        testSocketUdpChannel();
    }
    
    @Test
    public void testBufferUse(){
        IntBuffer intBuffer = IntBuffer.allocate(100);
        System.out.println(intBuffer.capacity());
        System.out.println(intBuffer.position());
        System.out.println(intBuffer.limit());
        System.out.println(intBuffer.mark());
        intBuffer.put(10);
        intBuffer.put(100);
        intBuffer.flip();
        System.out.println(intBuffer.position());
        System.out.println(intBuffer.get());
        System.out.println(intBuffer.get(1));
        System.out.println(intBuffer.get(0));
        System.out.println(intBuffer.position());
    }
    
    @Test
    public void testCopyFile() throws IOException {
        FileChannel inChannel = new
                FileInputStream("D:\\CodeProject\\Javabase\\src\\main\\resources\\BasicUse.java").getChannel();
        FileChannel outChannel = new
                FileOutputStream("D:\\CodeProject\\Javabase\\src\\main\\resources\\BasicUse2.java").getChannel();
        
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while(inChannel.read(buffer) != -1){
            buffer.flip();
            outChannel.write(buffer);
            buffer.clear();
        }
        inChannel.close();
        outChannel.close();
    }
    
    public static void testSocketTcp(){
        //服务器线程
        Thread serverThread = new Thread(() ->{
            try {
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress(80));
                
                Socket clientSocket;
                while((clientSocket = serverSocket.accept()) != null){
                    System.out.println("client " + clientSocket.getInetAddress() + " connected, start to receive msg");
                    InputStream in = clientSocket.getInputStream();
                    byte[] buf = new byte[1024 * 1024];
                    int len;
                    while((len = in.read(buf)) != -1){
                        System.out.println(new String(buf, 0, len));
                    }
                    in.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        });
        serverThread.setDaemon(true);
        serverThread.start();
        try {
            Socket socket1 = new Socket();
            socket1.connect(new InetSocketAddress(80));
            OutputStream outputStream = socket1.getOutputStream();
            Scanner scanner = new Scanner(System.in);
            String line;
            while(!Objects.equals(line = scanner.nextLine(), "")){
                outputStream.write(line.getBytes());
            }
            outputStream.close();
            socket1.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }
    public static void testSocketTcpChannel(){
        //服务器线程
        Thread serverThread = new Thread(() ->{
            try {
                ServerSocketChannel socketChannel = ServerSocketChannel.open();
                
                socketChannel.bind(new InetSocketAddress("127.0.0.1", 80));
                System.out.println("socket server started.");
                SocketChannel clientSocket;
                while((clientSocket = socketChannel.accept()) != null){
                    System.out.println("get connection:");
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    while(clientSocket.read(byteBuffer) != -1){
                        byteBuffer.flip();
                        System.out.println(new String(byteBuffer.array(), 0, byteBuffer.limit(), StandardCharsets.UTF_8));
                        byteBuffer.clear();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        });
        
        serverThread.start();
        //客户端线程
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            
            //是否设置异步模式
            socketChannel.configureBlocking(true);
            
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 80));
            
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            
            //开始写入
            Scanner scanner = new Scanner(System.in);
            String next;
            while(!Objects.equals(next = scanner.nextLine(), "")){
                byteBuffer.put(next.getBytes(StandardCharsets.UTF_8));
                byteBuffer.flip();
                socketChannel.write(byteBuffer);
                byteBuffer.clear();
            }
            //停止输入
            socketChannel.shutdownInput();
            socketChannel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void testSocketUdp() throws InterruptedException {
        //服务器线程
        Thread serverThread = new Thread(() ->{
            try {
                DatagramSocket serverSocket = new DatagramSocket(80);
                
                //不断的接收报文
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, 0, buf.length);
                serverSocket.receive(packet);
                System.out.println("receive udp package form " + packet.getAddress() + ":" + packet.getPort());
                System.out.println("data is:" + new String(packet.getData(), 0, packet.getLength()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        });
        serverThread.setDaemon(true);
        serverThread.start();
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            byte[] data = "udp 报文测试".getBytes();
            clientSocket.send(new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 80));
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        Thread.sleep(10000);
    }
    public static void testSocketUdpChannel(){
        //服务器线程
        Thread serverThread = new Thread(() ->{
            try {
                DatagramChannel socketChannel = DatagramChannel.open();
                
                socketChannel.bind(new InetSocketAddress("127.0.0.1", 80));
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                SocketAddress receive = socketChannel.receive(buffer);
                buffer.flip();
                System.out.println("receive form " + receive + " :" + new String(buffer.array(), 0, buffer.limit()));
                socketChannel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        });
        
        serverThread.start();
        //客户端线程
        DatagramChannel socketChannel = null;
        try {
            socketChannel = DatagramChannel.open();
            ByteBuffer outBuffer = ByteBuffer.allocate(1024);
            outBuffer.put("test udp channel".getBytes());
            outBuffer.flip();
            System.out.println(outBuffer.limit());
            int send = socketChannel.send(outBuffer, new InetSocketAddress("127.0.0.1", 80));
            System.out.println("send byte:"+send);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    
}
