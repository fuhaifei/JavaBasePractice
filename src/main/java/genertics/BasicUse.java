package genertics;


import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * 泛型（Generics）：在定义类、接口、方法时候，通过一个标识符标示类的属性或者方法的参数/返回值等，在使用时确定实际的类型（类型的占位符）
 * 1. JDK1.5之前基于Object实现容器的泛型接纳，存在类型强制转换、类型不安全等问题，JDK1.5之后通过泛型，实现隐式的自动类型转化，并根据分型类型
 *    在编译时检查类型安全
 * 2. 若使用泛型时不指定泛型，则自动将泛型填充为Object类型或者对于有限定的泛型例如< T exnteds XClass >，转化为XClass类型
 * 3. 泛型擦除：当编译器对带有泛型的java代码进行编译时，它会去执行类型检查和类型推断，然后生成普通的不带泛型的字节码，实际上同一个的类的不同泛型最终指只加载一个Class类对象。
 *    例如List<String> 和 List<Integer>->List//编译不通过，已经存在形同方法签名
 *        void m(List<String> numbers){}
 *        void m(List<Integer> Strings){}
 *
 * 4. ?:类型通配符，包括三种形式
 *    (1)<?> 无限制通配符
 *    (2)<? extends E> 上界通配符,E或者E的子类
 *    (3)<? super E> 下届通配符，E或者E的父类
 * 5. 协变/逆变/不可变
 * */
public class BasicUse {

    private class TestGeneric<T1, T2, T3>{
        private T1 param1;
        public T1 getParam1(){
            return param1;
        }
        public void setParam1(T1 param1){
            this.param1 = param1;
        }
        //方法中使用泛型
        public <T4> void swap(T4[] arr, int a, int b){
            T4 temp = arr[a];
            arr[a] = arr[b];
            arr[b] = temp;
        }
    }

    //继承时指定父类的泛型
    private class TestSon<T1> extends TestGeneric<String, String, T1>{
    }


    //测试泛型擦除
    @Test
    public void testGenericErase(){
        List<String> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();
        System.out.println(list1.getClass());
        System.out.println(list2.getClass());
    }
    private class TestErase{
        public List<Integer> myList;
        public TestErase(){
            myList = new ArrayList<>();
        }
    }
    //测试泛型擦除导致的运行时类型约束消失
    @Test
    public void typeLost() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        TestErase testErase = new TestErase();
        Field add = testErase.getClass().getField("myList");
        List list = (List)add.get(testErase);
        list.add("to day is a good day");
        testErase.myList.add(10);
        var iterator = list.iterator();
        while(iterator.hasNext()){
            System.out.println(iterator.next().getClass());
        }

        List testList = new ArrayList();
        testList.add("String");
        testList.add(10);
        iterator = testList.iterator();
        while(iterator.hasNext()){
            System.out.println(iterator.next().getClass());
        }
    }

    //一个结合使用的案例:
    public  <E extends Comparable<? super E>> E getMax(List<? extends E> input){
        E max = null;
        Iterator<? extends E> iter = input.listIterator();
        while(iter.hasNext()){
            E current = iter.next();
            if(max == null || current.compareTo(max) > 0){
                max = current;
            }
        }
        return max;
    }

    @Test
    public void testGetMax(){
        List<Integer> arrayList = new ArrayList<>();
        for(int i = 0;i < 10;i++){
            arrayList.add(new Random().nextInt());
        }
        System.out.println(getMax(arrayList));
    }


    @Test
    public static void testArray(String[] args) {
        //1.java数组是协变的
        Object[] father = new String[5];
        //2.泛型是不可变的:List<Object> fatherType = new ArrayList<String>();
        //使用原始类型
        List fatherList = new ArrayList<String>();
        //使用类型通配符
        List<String>[] fatherList2 = (List<String>[]) new ArrayList<?>[10];
    }

}
