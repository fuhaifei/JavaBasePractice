package reflect;


import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 *  Java Reflection基本使用
 *  1. 什么是java反射
 *      在运行期间获得任何类的内部信息，操作类对象的属性和方法的一套API
 *  2. 解决什么问题
 *      运行时判断对象所属的类，构造任意类的对象，操作对象的属性和方法
 *  3. 相关API
 *      （1）{@link Class} 该类对象代表运行中java中的类和接口，该对象随着类的加载由JVM自动创建，对应一个.class文件
 *      （2）{@link Method} 类的方法
 *      （3）{@link Field} 类的s属性
 *      （4）{@link java.lang.reflect.Constructor} 类的构造方法
 * */
public class BasicUse {

    public static class Person{
        public String name;
        public String location;
        private int height;

        public Person(String name, String location, int height){
            this.name = name;
            this.location = location;
            this.height = height;
        }

        public Person(){

        }

        public void learn(int type){
            if(type == 1){
                System.out.println(name + " learn english");
            }else{
                System.out.println(name + " learn math");
            }
        }

        private void playGame(){
            System.out.println("");
        }
        @Override
        public String toString(){
            return "object info:"+name + " " + location + " " + height + " cm";
        }
    }
    
    @Test
    public void getRunClass() throws ClassNotFoundException {
        //1. 通过类的class属性获得
        Class<Integer> clazzOne = Integer.class;
        //2. 通过对象的.getClass()方法获得
        Integer i = 5;
        Class<? extends Integer> clazzTwo = i.getClass();
        //3. 通过Class.forName(全类名)静态方法启动
        Class<?> clazzThree = Class.forName("java.lang.Integer");
        //4. 通过AppClassLoader加载类
        Class<?> clazzFour = this.getClass().getClassLoader().loadClass("java.lang.Integer");

        System.out.println(clazzOne);
        System.out.println(clazzTwo);
        System.out.println(clazzThree);
        System.out.println(clazzFour);
        System.out.println(clazzOne.equals(clazzFour));
    }

    @Test
    public void testReflectionInterface() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        Class<?> clazz = Class.forName("reflect.BasicUse$Person");

        //调用有参构造方法
        Object o1 = clazz.getDeclaredConstructor(String.class, String.class, int.class).newInstance("小王", "北京", 178);
        System.out.println(o1);
        //由Class对象创建类对象
        Object o2 = clazz.getDeclaredConstructor().newInstance();
        System.out.println(o2);
        //逐个设置参数(getField/getDeclaredField)
        Field name = clazz.getDeclaredField("name");
        Field location = clazz.getDeclaredField("location");
        Field height = clazz.getDeclaredField("height");
        name.set(o2, "小张");
        location.set(o2, "日本");
        height.set(o2, 180);
        System.out.println(o2);
        //调用方法
        Method study = clazz.getDeclaredMethod("learn", int.class);
        study.invoke(o1, 1);
        study.invoke(o2, 1);
    }

    /**
     * 类加载机制（classloader）：将Class字节码文件加载到内存中，将静态数据转化为方法去的运行数据结构后，在堆生成一个对应java.lang.Class对象
     * 1. 从高到底分为三个类加载器：Bootstrap ClassLoader->Extension ClassLoader ->Application ClassLoader
     *      * 启动类加载器（Bootstrap ClassLoader）：加载java核心库（jre/lib）
     *      * 扩展加载器（Extension ClassLoader）：加载插件jar包（jre/lib/ext）
     *      * 应用程序加载器（Application ClassLoader）加载classpath下的类库,即用户编写的类（默认的系统加载器）
     *      * 用户自定义类加载器：实现自定义类库加载
     * 2. 基于双亲委派机制实现类库加载，即当加载器需要加载类时，首先交给父类加载器进行加载，父类加载器无法完成加载时，子类再就行加载（保证安全性）
     * 3. 加载时通过锁机制以及判断是否已经加载，避免重复加载同一个类
     * 4. 要打破双亲机制，必须通过自定义加载器重载loadClass（）的方法实现
     * 5. JDK9中将Extension ClassLoader -> platformLoader
     * 6. 加载器之间的父子关系通过组合实现
     * 7. .getClassLoader（) 获得加载当前类的类加载器
     * */
    @Test
    public void testClassLoader() throws ClassNotFoundException, IOException {
        System.out.println("1.展示加载器层层级结构");
        System.out.println(Class.forName("reflect.BasicUse$Person").getClassLoader());
        System.out.println(Class.forName("reflect.BasicUse$Person").getClassLoader().getParent());
        System.out.println(Class.forName("reflect.BasicUse$Person").getClassLoader().getParent().getParent());
        System.out.println(ClassLoader.getSystemClassLoader());
        System.out.println(ClassLoader.getPlatformClassLoader());


        System.out.println("2.使用类加载器读取文件流");
        //Application ClassLoader的扫名路径为classpath(未打包时的src,打包后的target/classes)
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("src/main/java/reflect/BasicUse.java"))));
        String line = bufferedReader.readLine();
        while(line != null){
            System.out.println(line);
            line = bufferedReader.readLine();
        }
    }
}
