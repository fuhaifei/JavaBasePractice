import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public String reverseStr(String str){
        char[] charOfStr = str.toCharArray();

        for(int i = 0;i < charOfStr.length / 2;i++){
            char mid = charOfStr[i];
            charOfStr[i] = charOfStr[str.length() - i - 1];
            charOfStr[str.length() - i - 1] = mid;
        }
        return new String(charOfStr);
    }

    public void mergeSort(int left,int right, int[] aimArray){
        if(left == right){
            return;
        }
        int middle = (left + right) / 2;
        mergeSort(left, middle, aimArray);
        mergeSort(middle + 1, right, aimArray);

        //合并
        int[] tempArray = new int[right - left + 1];
        int curIndex = 0;
        int leftIndex = left;
        int rightIndex = right;
        while(leftIndex <= middle || rightIndex <= right){
            if(leftIndex > middle) {
                tempArray[curIndex++] = aimArray[rightIndex++];
            }else if(rightIndex > right){
                tempArray[curIndex++] = aimArray[leftIndex++];
            }else if(aimArray[leftIndex] > aimArray[rightIndex]){
                tempArray[curIndex++] = aimArray[leftIndex++];
            }else{
                tempArray[curIndex++] = aimArray[rightIndex++];
            }
        }
        //复制合并数字到原来数组
        for(int i = 0;i < tempArray.length;i++){
            aimArray[left + i] = tempArray[i];
        }
    }

    public static void quickSort(int left,int right, int[] aimArray) {

        if(left >= right) {
            return;
        }
        int pivot = aimArray[left];
        int i = left;
        int j = right;
        while(i < j) {
            while(aimArray[j] >= pivot && i < j) {
                j--;
            }
            while(aimArray[i] <= pivot && i < j) {
                i++;
            }
            //交换左右数字
            int temp = aimArray[j];
            aimArray[j] = aimArray[i];
            aimArray[i] = temp;
        }
        aimArray[left] = aimArray[i];
        aimArray[i] = pivot;
        quickSort(left,j-1, aimArray);
        quickSort(j+1,right, aimArray);
    }

    public long getTotalSize(String filePath) throws InterruptedException {
        File dir = new File(filePath);
        AtomicLong result = new AtomicLong(0);
        if(dir.isDirectory()){
            File[] files = dir.listFiles();
            CountDownLatch countDownLatch = new CountDownLatch(files.length);

            for (File file : files) {
                Thread thread = new Thread(() -> {
                    //线程执行
                    result.compareAndSet(result.get(), result.get() + file.length());
                    countDownLatch.countDown();
                });
                thread.start();
            }
            countDownLatch.await();
        }
        return result.get();
    }

    static class ProducerAndConsumer{
        private static int MAX_SIZE = 100;
        public static final Object lock = new Object();
        private static List<Object> queue = new LinkedList<>();

        private static class Producer extends Thread{
            @Override
            public void run(){
                while(true){
                    synchronized (lock){
                        if(queue.size() == MAX_SIZE){
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if(queue.size() < MAX_SIZE){
                            //生产
                            queue.add(new Object());
                            //唤醒正在等待的线程
                            notifyAll();
                        }
                    }
                }
            }
        }

        private static class Consumer extends Thread{
            @Override
            public void run(){
                while(true){
                    synchronized (lock){
                        if(queue.size() == 0){
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if(queue.size() > 0){
                            //生产
                            queue.remove(0);
                            //唤醒正在等待的线程
                            lock.notifyAll();
                        }
                    }
                }
            }

        }

    }

    static class ProducerAndConsumerLock{
        private ReentrantLock lock = new ReentrantLock();
        private Condition writeCondition = lock.newCondition();
        private Condition readCondition = lock.newCondition();
        //资源 + 资源数量上限
        private static Integer count = 0;
        private static final Integer FULL = 10;

        private class Consumer implements Runnable{
            @Override
            public void run() {
                while(true){
                    try {
                        lock.lock();
                        if(count == 0){
                            readCondition.await();
                        }
                        count--;
                        writeCondition.notifyAll();
                    } catch (InterruptedException e) {
                        System.out.println("线程被中断");
                    }finally {
                        lock.unlock();
                    }
                }
            }
        }

        private class Producer implements Runnable{
            @Override
            public void run() {
                while(true){
                    try {
                        lock.lock();
                        if(count == FULL){
                            writeCondition.await();
                        }
                        count++;
                        readCondition.notifyAll();
                    } catch (InterruptedException e) {
                        System.out.println("线程被中断");
                    }finally {
                        lock.unlock();
                    }
                }
            }
        }
    }


    static class SequenceExecute{
        static final Object lockObject = new Object();
        static volatile int flag = 0;
        static final int max = 10;

        public static class myThead implements Runnable{
            private  int myFlag;

            public myThead(int myFlag){
                this.myFlag = myFlag;
            }
            @Override
            public void run() {
                while (true){
                    synchronized (lockObject){
                        try {
                            if(flag != myFlag){
                                lockObject.wait();
                            }
                            flag = (flag + 1) % max;
                            lockObject.notifyAll();
                        }catch (InterruptedException e){
                        }
                    }
                }
            }
        }
        public static void executeDemo(){
            for(int i = 0;i < max;i++){
                new Thread(new myThead(i)).start();
            }
        }
    }

    class MultiThreadSum{
        private int[] sums;

        private class ComputeThread implements Runnable {
            int segIndex;
            int left;
            int right;

            public ComputeThread(int segIndex, int left, int right){
                this.segIndex = segIndex;
                this.left = left;
                this.right = right;
            }
            @Override
            public void run() {
                int result = 0;
                for(int i = left;i < right;i++){
                    result += i;
                }
                sums[segIndex] = result;
            }
        }
        public MultiThreadSum(int left, int right, int n) throws InterruptedException {
            sums = new int[n];
            int len = (right - left + 1) / n;
            int remind = (right - left + 1) % n;
            int preRight = -1;

            Thread[] allThread = new Thread[n];
            for(int i = 0;i < n;i++){
                int curLeft = preRight + 1;
                int curRight = curLeft + len - 1;
                if(i < remind){
                    curRight += 1;
                }

                allThread[i] = new Thread(new ComputeThread(i, curLeft, curRight));
                allThread[i].start();
            }
            for(int i = 0;i < n;i++){
                allThread[i].join();
            }
            int totalSum = 0;
            for(int i = 0;i < n;i++){
                totalSum += sums[i];
            }
            System.out.println(totalSum);
        }

        public void
    }
}
