package IO;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

/**
 * @author fuhaifei
 * */

public class ReactorServerDemo {
    private static Logger logger = LoggerFactory.getLogger(ReactorServerDemo.class.getName());
    
    //单线程版本ReactorServer
    public class SingleReactor implements Runnable{
        Selector selector;
        
        SingleReactor(){
            try {
                //创建socket
                selector = Selector.open();
                ServerSocketChannel socketChannel = ServerSocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.bind(new InetSocketAddress(80));
                //注册到selector
                SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_ACCEPT);
                selectionKey.attach(new AcceptorHandler());
                logger.info(Thread.currentThread().getName() + "init server success");
            }catch (IOException e){
                logger.error(Thread.currentThread().getName() + "init server error:"+e);
            }
            
        }
        
        @Override
        public void run() {
            while(!Thread.interrupted()){
                try {
                    selector.select(1000);
                    Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
                    while(selectionKeys.hasNext()){
                        SelectionKey curKey = selectionKeys.next();
                        AbstractHandler handler = (AbstractHandler)curKey.attachment();
                        handler.handle(curKey);
                        selectionKeys.remove();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            logger.info(Thread.currentThread().getName() + "Reactor server exit");
            try {
                selector.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        }
        
        private interface  AbstractHandler{
            public void handle(SelectionKey selectionKey);
        }
        
        private class AcceptorHandler implements AbstractHandler{
            
            public AcceptorHandler(){
            }
            
            @Override
            public void handle(SelectionKey selectionKey) {
                Selector selector = selectionKey.selector();
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectionKey.channel();
                SocketChannel socketChannel;
                try {
                    socketChannel = serverSocketChannel.accept();
                    if(socketChannel != null){
                        logger.info(Thread.currentThread().getName() + "accept socketChannel:" );
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ, new RequestHandler(socketChannel));
                    }else{
                        logger.info(Thread.currentThread().getName() + "accept socketChannel is null:" );
                    }
                } catch (IOException e) {
                    logger.error(Thread.currentThread().getName() + "accept connection failed:" + e);
                }
                selector.wakeup();
            }
        }
        
        private class RequestHandler implements AbstractHandler{
            ByteBuffer writeBuffer;
            public RequestHandler(SocketChannel socketChannel){
                writeBuffer = ByteBuffer.allocate(10240);
            }
            
            
            @Override
            public void handle(SelectionKey selectionKey) {
                selectionKey.cancel();
                SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
                if(selectionKey.isWritable()){
                    try {
                        socketChannel.write(writeBuffer);
                        
                        //删除写事件
                        if(writeBuffer.remaining() == 0){
                            selectionKey.interestOps(selectionKey.interestOps() ^ SelectionKey.OP_WRITE);
                            //转化为写状态
                            writeBuffer.clear();
                        }
                    }catch (IOException e){
                        logger.error(Thread.currentThread().getName() + "write into socket failed");
                    }
                }else{
                    ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
                    try{
                        socketChannel.read(receiveBuffer);
                        receiveBuffer.flip();
                        //写入write buffer
                        String returnMsg = "we receive your"+ socketChannel.getRemoteAddress() +" message:"+
                                new String(receiveBuffer.array(), 0, receiveBuffer.limit())
                                + " , thanks for you call.";
                        System.out.println("server:"+returnMsg);
                        //向writeBuffer中写入
                        writeBuffer.put(returnMsg.getBytes());
                        //转化为读事件
                        writeBuffer.flip();
                        //注册事件
                        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                    } catch (IOException e){
                        logger.error(Thread.currentThread().getName() + "read from socket failed");
                    }
                }
            }
        }
    }
    
    public void testSingleReactor() throws IOException {
        Thread serverThread = new Thread(new SingleReactor(), "serverClient");
        serverThread.start();
        
        //启动两个线程进行交互
        Thread heartbeatClient = new Thread(() ->{
            SocketChannel socketChannel = null;
            try {
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(true);
                socketChannel.bind(new InetSocketAddress(50));
                socketChannel.connect(new InetSocketAddress(80));
                ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
                logger.info("heartbeat client connection established");
                while(!Thread.interrupted()){
                    sendBuffer.put("heartbeat client heart beat message".getBytes());
                    sendBuffer.flip();
                    socketChannel.write(sendBuffer);
                    sendBuffer.clear();
                    socketChannel.read(sendBuffer);
                    sendBuffer.flip();
                    System.out.println("heart beat return message:"+
                            new String(sendBuffer.array(), 0, sendBuffer.limit()));
                    Thread.sleep(30000);
                }
                logger.info("heartbeat client exit");
                socketChannel.close();
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
        }, "heartbeatClient");
        heartbeatClient.start();
        //主线程交互
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.bind(new InetSocketAddress(51));
            socketChannel.connect(new InetSocketAddress(80));
            ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
            logger.info("main client client connection established");
            Scanner scanner = new Scanner(System.in);
            String nextLine;
            while(!Objects.equals(nextLine = scanner.nextLine(), "exit")){
                logger.info("input:" + nextLine);
                sendBuffer.put(("main Client" + nextLine).getBytes());
                sendBuffer.flip();
                socketChannel.write(sendBuffer);
                sendBuffer.clear();
                logger.info("send over" + nextLine);
            }
            logger.info("main client");
            serverThread.interrupt();
            heartbeatClient.interrupt();
            socketChannel.close();
        } catch (IOException e) {
            serverThread.interrupt();
            heartbeatClient.interrupt();
            socketChannel.close();
        }
    }
    
    public static void main(String[] args) throws IOException {
        new ReactorServerDemo().testSingleReactor();
    }
}
