//package IO;
//
//import io.netty.bootstrap.Bootstrap;
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ByteBufAllocator;
//import io.netty.buffer.PooledByteBufAllocator;
//import io.netty.buffer.Unpooled;
//import io.netty.buffer.UnpooledByteBufAllocator;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelHandler;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelOption;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.util.CharsetUtil;
//import org.junit.Test;
//
//import java.util.Objects;
//import java.util.Scanner;
//
///**
// * @author 79351
// * ***Netty学习笔记***
// * 1. Netty是什么?
// *  * 异步网络通信框架，是对JavaNIO的封装，具备高性能，API学习成本低等优点，用来简化网络编程
// *  * 用来作为底层通信组件开发高性能服务器应用（Dubbo,RocketMQ等）
// *  * 基于“主从Reactor模型”实现，主Reactor负责处理连接请求()，从Reactor负责处理读写请求
// * 2. Netty中的主要概念
// *  * EventLoop：线程+异步任务队列,处理IO（selectionKey就绪）和非IO任务（注册绑定等）和 EventLoopGroup:多个Eventloop
// *      * 例如：NioEventLoop 和 NioEventLoopGroup
// *      * 实现EventLoopGroup接口，默认线程数量为处理器的2倍数
// *  * Channel：等同于NIO中的Channel概念，包括异步（NioSocketChannel+NioServerSocketChannel）
// *    和同步（OioSocketChannel+OioServerSocketChannel）
// *  * option()/childOption()：设置主Reactor和从Reactor的连接设置、
// *      * opetion()包括：SO_BACKLOG接受的最大连接数
// *      * childOption()包括：SO_RCVBUF TCP连接缓冲区，TCP_NODELAY, SO_KEEPALIVE 连接保活
// *  * Future/ChannelFuture：Netty中所有连接/IO操作均异步，基于Future实现对于异步对象的监控（查看状态/addListeners）
// *  * ChannelHandler：类似于NIO中的Handler概念，用来处理丽连接/读写相关操作
// *  * ChannelHandlerContext：在ChannelHandler中获取相关全局信息（Channel,pipeline）
// *  * ChannelPipline：多个Handler组装处理的流水线
// *  * Bootstrap、ServerBootstrap：Netty应用程序从Bootstrap开始，负责配置Netty，串联
// *    各个组件
// * 3. Netty基本架构理解
// *  * Channel -> DefaultChannelPipeline -> HandlerContext(Excutor + Handler) -> EventLoop
// *  * 一个Channel对应一个pipiline，pipeline为一系列Handler组成处理链，Hanlder提交到EventLoop上进行执行
// *  * 以Reactor视角描述Netty执行流程
// *      * 客户端发起连接，由主Reactor对应线程池处理（bossGroup）-> 交给从Reactor
// *      * 从Reactor将连接与一个执行线程绑定（EventLoop）,由EventLoop进行select操作
// *      * 当Channel出现出入站读写事件时，经由过滤器链逐个处理，每次处理逻辑为：将对应Handler逻辑封装为Task提交到对应的EventLoop中执行
// *      * EventLoop可以理解为增加了select的单线程池；EventLoopGroup理解为多个EventLoop，负责将Channel分配给对应的EventLoop
// * 4. ByteBuf理解
// *  * 一个数组+两个index(readIndex + writeIndex), 0-readIndex：已读可丢弃；readIndex-writeIndex：可读；writeIndex-capacity：可写
// *  * 三个重要属性：readIndex，writeIndex，MaxCapacity
// *  * 缓冲区类型：
// *      * 三个维度：是否池化/堆或直接内存/unsafe直接操作，包含八种类型的ByteBuf
// *      * 两种类型分配器：PooledByteBufAllocator/UnpooledByteBufAllocator/Unpooled， 其中ByteBufAllocator在非安卓平台下默认选择Pooled
// *  * 常用API：
// *      * 读写（read/writeBytes,read/writeTypes）
// *      * 设置获取（set/getType）
// *      * 记录index(markWriter/ReaderIndex)
// *      * 浅层复制（slice/dupicate）：与源ByteBuf共享一个区域的Bytebuf，但是不会改变引用计数，一般通过retain保证原ByteBuf不被释放
// *          * slice由于限制了maxCapacity = readableBytes，不可写
// *          * dupicate可写可读
// *  * 自动扩容：
// *      * 小于一个阈值（CALCULATE_THRESHOLD = 4MB）时，从64开始扩容到大于当前容量的最小二的幂次
// *      * 大于一个阈值，每次增加一个阈值的大小
// *  * 内存回收策略：
// *      * 基于引用计数方法实现byteBuffer内存回收，定义在ReferenceCounted接口中
// *      * 相关API: retain() release() refCnt()
// *      * 当计数器为0时，ByteBuf无法使用无法使用，根据其所处的内存空间垃圾回收方法等待回收
// *  * Netty中ByteBuffer如何回收
// *      * 入站：在TailContext调用release方法进行释放
// *      * 出站：在HeadContext调用release方法进行释放
// *      * 自定义Handler需要传递msg/手动释放/继承SimpleChannelInboundHandler自动释放
// *  * Netty零拷贝：应用程序级别的零拷贝，减少堆与直接内存，堆内部，直接内存之间的无意义拷贝
// *      * CompositeByteBuf：组合不同ByteBuf，避免无效拷贝
// *      * Unpooled：提供了各种wrap包装方法，将byte[]/NIO ByteBuffer等包装成BygeBuf,避免无效拷贝
// */
//public class NettyLearn {
//    private static class NettyEchoServer{
//
//        public void startServer(){
//            //主Reactor和从Reactor线程池
//            EventLoopGroup bossGroup = new NioEventLoopGroup();
//            EventLoopGroup workerGroup = new NioEventLoopGroup();
//            try {
//                // 创建服务器端的启动对象
//                ServerBootstrap bootstrap = new ServerBootstrap();
//                // 使用链式编程来配置参数
//                bootstrap.group(bossGroup, workerGroup) //设置两个线程组
//                        // 使用NioServerSocketChannel作为服务器的通道实现
//                        .channel(NioServerSocketChannel.class)
//                        // 初始化服务器连接队列大小，服务端处理客户端连接请求是顺序处理的,所以同一时间只能处理一个客户端连接。
//                        // 多个客户端同时来的时候,服务端将不能处理的客户端连接请求放在队列中等待处理
//                        .option(ChannelOption.SO_BACKLOG, 1024)
//                        .childHandler(new ChannelInitializer<SocketChannel>() {//创建通道初始化对象，设置初始化参数，在 SocketChannel 建立起来之前执行
//
//                            @Override
//                            protected void initChannel(SocketChannel ch) throws Exception {
//                                //对workerGroup的SocketChannel设置处理器
//                                ch.pipeline().addLast(new NettyServerHandler());
//                            }
//                        });
//                System.out.println("netty server start。。");
//                // 绑定一个端口并且同步, 生成了一个ChannelFuture异步对象，通过isDone()等方法可以判断异步事件的执行情况
//                // 启动服务器(并绑定端口)，bind是异步操作，sync方法是等待异步操作执行完毕
//                ChannelFuture cf = bootstrap.bind(9000).sync();
//                // 给cf注册监听器，监听我们关心的事件
//            /*cf.addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    if (cf.isSuccess()) {
//                        System.out.println("监听端口9000成功");
//                    } else {
//                        System.out.println("监听端口9000失败");
//                    }
//                }
//            });*/
//                // 等待服务端监听端口关闭，closeFuture是异步操作
//                // 通过sync方法同步等待通道关闭处理完毕，这里会阻塞等待通道关闭完成，内部调用的是Object的wait()方法
//                cf.channel().closeFuture().sync();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } finally {
//                bossGroup.shutdownGracefully();
//                workerGroup.shutdownGracefully();
//            }
//        }
//        }
//
//
//    @ChannelHandler.Sharable
//    private static class NettyServerHandler extends ChannelInboundHandlerAdapter {
//
//        /**
//         * 当客户端连接服务器完成就会触发该方法
//         *
//         * @param ctx
//         * @throws Exception
//         */
//        @Override
//        public void channelActive(ChannelHandlerContext ctx) {
//            System.out.println("客户端连接通道建立完成");
//        }
//
//        /**
//         * 读取客户端发送的数据
//         *
//         * @param ctx 上下文对象, 含有通道channel，管道pipeline
//         * @param msg 就是客户端发送的数据
//         * @throws Exception
//         */
//        @Override
//        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//            //Channel channel = ctx.channel();
//            //ChannelPipeline pipeline = ctx.pipeline(); //本质是一个双向链接, 出站入站
//            //将 msg 转成一个 ByteBuf，类似NIO 的 ByteBuffer
//            ByteBuf buf = (ByteBuf) msg;
//            //回显输入内容
//            ctx.writeAndFlush(msg);
//        }
//
//        /**
//         * 数据读取完毕处理方法
//         *
//         * @param ctx
//         * @throws Exception
//         */
//        @Override
//        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//            super.channelReadComplete(ctx);
//        }
//
//        /**
//         * 处理异常, 一般是需要关闭通道
//         *
//         * @param ctx
//         * @param cause
//         * @throws Exception
//         */
//        @Override
//        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//            ctx.close();
//        }
//    }
//
//    private static class NettyClient {
//        public void startClient() {
//            //客户端需要一个事件循环组
//            EventLoopGroup group = new NioEventLoopGroup();
//            try {
//                //创建客户端启动对象
//                //注意客户端使用的不是ServerBootstrap而是Bootstrap
//                Bootstrap bootstrap = new Bootstrap();
//                //设置相关参数
//                bootstrap.group(group) //设置线程组
//                        .channel(NioSocketChannel.class) // 使用NioSocketChannel作为客户端的通道实现
//                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT) //设置池化poll
//                        .handler(new ChannelInitializer<SocketChannel>() {
//                            @Override
//                            protected void initChannel(SocketChannel ch) throws Exception {
//                                //加入处理器
//                                ch.pipeline().addLast(new NettyClientHandler());
//                            }
//                        });
//
//                //启动客户端去连接服务器端
//                ChannelFuture cf = bootstrap.connect("127.0.0.1", 9000).sync();
//                //对通道关闭进行监听
//                Channel channel = cf.channel();
//                //读取输入并不断发送
//                Scanner scanner = new Scanner(System.in);
//                System.out.println("send msg to server(e to exit):");
//                String nextLine = scanner.nextLine();
//                while(!Objects.equals(nextLine, "e")){
//                    ByteBuf buf = channel.alloc().buffer();
//                    buf.writeBytes(nextLine.getBytes());
//                    channel.writeAndFlush(buf);
//                    System.out.println("send msg to server(e to exit):");
//                    nextLine = scanner.nextLine();
//                }
//            } catch (InterruptedException e){
//                e.printStackTrace();
//            }finally {
//                group.shutdownGracefully();
//            }
//        }
//    }
//
//    private static class NettyClientHandler extends ChannelInboundHandlerAdapter {
//
//        /**
//         * 当客户端连接服务器完成就会触发该方法
//         *
//         * @param ctx
//         * @throws Exception
//         */
//        @Override
//        public void channelActive(ChannelHandlerContext ctx) {
//            ByteBuf buf = Unpooled.copiedBuffer("HelloServer".getBytes(CharsetUtil.UTF_8));
//            ctx.writeAndFlush(buf);
//        }
//
//        //当通道有读取事件时会触发，即服务端发送数据给客户端
//        @Override
//        public void channelRead(ChannelHandlerContext ctx, Object msg) {
//            ByteBuf buf = (ByteBuf) msg;
//            System.out.println("收到服务端的消息:" + buf.toString(CharsetUtil.UTF_8));
//        }
//
//        @Override
//        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//            cause.printStackTrace();
//            ctx.close();
//        }
//    }
//
//    @Test
//    public void buffType(){
//        ByteBuf directBuf = PooledByteBufAllocator.DEFAULT.directBuffer(10);
//        ByteBuf heapBuf =  UnpooledByteBufAllocator.DEFAULT.buffer(10);
//        //UnpooledByteBufAllocator.DEFAULT.compositeDirectBuffer()
//        heapBuf.writeBytes("HAHAH".getBytes());
//        directBuf.writeBytes("HAHAH".getBytes());
//        System.out.println(directBuf.hasArray());
//        System.out.println(heapBuf.hasArray());
//    }
//
//    public static void main(String[] args) {
//        new Thread(()->{
//                new NettyEchoServer().startServer();
//        }).start();
//        new NettyClient().startClient();
//
////        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(10);
////
////        System.out.println(buf.capacity());
////        System.out.println(buf.maxCapacity());
////
////        buf.writeBytes(new byte[]{1,2,3,4});
////        buf.writeBytes(new byte[]{1,2,3,4});
////        buf.writeBytes(new byte[]{1,2,3,4});
////        System.out.println(buf.capacity());
////        ByteBuf duplicate = buf.duplicate();
////        System.out.println(buf.writerIndex());
////        System.out.println(duplicate.writerIndex());
////        duplicate.writeBytes("haha".getBytes());
////        System.out.println(buf.writerIndex());
////        System.out.println(duplicate.writerIndex());
////        buf.writerIndex();
////
////        //System.out.println(buf.retain());
//    }
//}
