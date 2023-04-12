import org.junit.Test;

/**
 * Java基础概念
 * 1. Java权限修饰符
 *      * public:    能被所有类访问
 *      * protected: 只能被同一个包中的类/其他包中的子类访问
 *      * null:      默认权限，只能被同一包中的类访问（即使是不同包内的子类也无法访问）
 *      * private:   私有方法，只有当前类本身可访问
 * 2. Java final关键字
 *      * final关键字来修饰类、方法和变量（包括成员变量和局部变量）,限制被修饰对象不可变
 *      * 修饰变量
 *          * 被修饰的成员变量必须在声明时/构造方法初始化(意味着不能存在无参构造方法)
 *          * 在编译时能够确定其final变量值会进行变量替换
 *      * 修饰方法：限制方法无法被子类重写，类的private方法会隐式地被指定为final方法
 *      * 修饰类：当用final修饰一个类时，表明这个类不能被继承，final类中的所有成员方法都会被隐式地指定为final方法，成员变量不受影响
 * 3. Java 内部类
 *      * 成员内部类：内部类作为外部类的成员
 *          * 内部类可访问外部类的所有成员变量/属性方法,外部类访问内部类的属性和方法必须通过创建内部类对象
 *          * 成员内部类能够访问外部类的原因为：编译器会默认为成员内部类添加了一个指向外部类对象
 *          * 静态类对象不依赖于外部类对象，可以独立于外部类对象创建（非static内部类无法实现该效果）
 *      * 局部内部类：类似于局部变量
 *      * 匿名内部类：常用在传参等
 *      * 为什么局部内部类和匿名内部类使用的外部参数必须为final
 *          * 内部类和外部类包含在两个class文件中，内部类对于局部变量的引用实际上参数复制：若编译期能够确定则直接替换，否则经构造方法初始化
 *          * 所以内外并不是同一个对象，如果对该变量修改会导致内外变量不一致
 * 3. 面向过程 vs 面向对象 vs 面向切面
 *      * 三者的定义
 *          * 面向对象思想以对象为中心，关注任务完成过程中存在哪些对象以及对象之间的交互，强调的是“谁来做”
 *          * 面向过程思想以过程为中心，关注的执行的任务过程中的操作和数据，强调的是“怎么做”
 *          * 面向切面编程(AOP)是一种基于面向对象编程(OOP)的编程思想，它是OOP的一种补充和完善。它所面对的是处理过程中的某个步骤或阶段,
 *            强调的是在“哪里做”和“何时做”
 *              * AOP的四个概念：Aspect/Joint Point/Point Cut/Advice
 * 4. 抽象类和接口
 *      * 抽象类=抽象方法（可有可无）+普通方法（可有可无），抽象类无法创建实例，其子类只有实现所有抽象方法后才能成为普通类，否则仍为抽象类
 *          * 一部分功能是确定实现的，另一部分功能需要根据子类的逻辑进行实现
 *          * abstract 无法修饰私有方法
 *      * 接口 = jdk8.0之前 静态常量（static final） + 抽象方法 ->jdk8.0: default修饰的公共默认方法+ 公共静态方法 -> jdk9.0：增加了私有方法
 *          * 接口无成员变量，无构造函数
 *          * 类实现接口：必须重写接口中的抽象方法+可选重写接口中的默认方法，静态方法不能被继承也不能被重写
 *          * 接口与抽象类的最大区别就是接口支持多继承（接口可继承多个接口，类可实现多个类）
 *      * 接口多继承导致的冲突问题：
 *          * 方法冲突：1.类优先原则 2.重名方法若返回值不同，则直接报错 3.多个接口default方法冲突，则强制实现类重写该方法
 *          * 属性冲突：调用时必须指明父接口名称/父类名称
 * Redis八股补充
 * 1. Redis 热Key问题和大Key问题
 *      * 热点Key: 在某些业务场景中，大量的请求访问同一个key，导致流量过于集中，例如热门的商品信息、热门话题等
 *          * 造成的问题：热点key所在服务器需要处理大量请求，cpu负载高，导致服务器瘫痪，可能出现缓存击穿问题
 *          * 如果抓取热点key
 *              * redis端收集: monitor命令,redis-cli时加上–hotkeys
 *              * 客户端收集：在客户端统计key的请求次数
 *              * Proxy层收集：在reids和业务服务器之间添加一个中间层（codis），由该中间层负责实现
 *           * 解决方案：
 *              * 使用本地缓存缓存热点key,避免访问redis
 *              * 将热点key分散到不同的服务器中（对热点key随机拼接后缀，映射到对应的服务器上），类似于备份热点key
 *      * 大key问题：有些key访问量不搞，但是由于value较大，造成网络负载较大
 *           * 大key的标准：String类型的Key值大于10kb，list、set、zset、hash元素数量过多（1000-5000个）大于100MB
 *           * 大key场景：热点排行榜（上万个）
 *           * 大key定位： --bigkeys
 *           * 造成的问题：阻塞redis，造成redis服务器相应其他请求的响应时间增加；网络阻塞；集群分片存储的条件下可能造成数据倾斜
 *           * 解决方案：大key拆分；value序列化/压缩算法压缩;
 *           * 大key删除：分批次删除；异步删除（unlink命令以非阻塞的方式删除大key）；被动删除（由redis在适当的实际自动删除）：
 *              * 其他命令带有隐式删除命令
 *              * 主从同步时主主动清理数据
 *              * 内存达到最大空间时删除
 * 2. Redis 内存淘汰策略
 *      * 默认策略（noeviction）：不删除键，只能读不能写
 *      * 按照回收策略：lfu和lru 和 random 和ttl（删除最快过期的键）
 *      * 按照回收范围：volatile（设置了过期时间的key）,allkeys（所有key）
 * 3. Redis过期键回收策略
 *      * 定时回收  : 为每个key设计定时器，当到达对应时间后删除过期key
 *      * 周期性回收 : 设置一个周期性回收线程，周期性回收过期key
 *      * 惰性回收  : 当请求过期key时，删除该key
 *      * redis采用：周期性回收+惰性回收
 * */
public class Basic {

    public Basic() {
    }

    public class BasicInner{
        public void print(){
            testFinal();
        }
    }

    public void testFinal(){
        final String name = "hahah";
        System.out.println(name);
    }
    public interface testInterface{
        public String getName();
        private  String getAge(){
            return "nb";
        }
    }

    public interface Foo {
        int x = 1;
        int y = 1;
        default void doThat() {
            System.out.println("nFoo");
        }
    }
    public class Bar {
        int x = 2;
        public void doThat() {
            System.out.println("nBar");
        }
    }

    public class FooBar extends Bar implements Foo {

        public void doThat() {
            Foo.super.doThat();
            System.out.println(y);
        }
    }

    enum TestEnum {
        Num1, Num2, Num3, Num4;

    }

    @Test
    public void TestEnum(){
        TestEnum num1 = TestEnum.Num1;
        TestEnum num11 = TestEnum.valueOf("Num1");
        System.out.println(num1);
        System.out.println(num11);
    }
}
