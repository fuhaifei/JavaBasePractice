
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
 *
 *  Mysql八股补充
 *  1. Mysql主从复制延迟问题
 *      * 出现该问题的场景：硬件性能差异/从库数量过多/SQL语句过多/执行大事务/网络延迟
 *      * Mysql三种同步模式
 *          （1）异步复制：主库再执行完事务后立即将结果返回给客户端，不关心从库是否执行完毕
 *          （2）全同步复制：主库和所有从库执行完成事务后，才将结果返回给用户
 *          （3）半同步复制：至少等待一个从库，主库只需要等待至少一个从库接收到并写到 Relay Log 并完成事务返回ACK
 *               * relay log就是一个存放在从服务器的中继日志
 *               * 存在数据不同步的问题：主库事务成功提交，从库未提交，主库宕机，导致主从不一致
 *          （4）增强半同步复制：MySQL 5.7 引入主库事务等待从库事务提交后，主库事务提交（AFTER_SYNC/AFTER_COMMIT）
 *              * 原有的丢数据问题->变为了多数据问题
 *      * 并行复制
 *           * 主从复制中主要的线程
 *               * Binlog Dump 线程：主库中发送Binlog的线程
 *               * IO线程：从库中读取Binlog写入relay log的线程
 *               * SQL线程：读取relay log修改数据库状态的线程
 *           * 主从复制延迟的主要原因
 *               * SQL 线程忙不过来（主要原因）
 *                   * 单线程再主库并发高，TPS高时会出西安严重的主备延迟问题
 *               * 网络抖动导致 IO 线程复制延迟（次要原因）；
 *           * 并行复制指的是SQL线程从单线程->多线程
 *               * SQL线程将任务分发给多个Worker线程，由Woker线程并发执行任务
 *               * 5.6：按库的并行/5.7 基于组提交
 * 2. 对于一个慢查询你将如何入手
 *     * Explain查看Sql执行计划
 *          * type: 是否使用了索引：index/all
 *              * 索引失效：sql编写是否存在问题（最左匹配原则，计算属性，索引覆盖）
 *              * 优化器优化：次级索引区分度问题,覆盖索引（若查询问题，不如删除索引；若区分度问题，增加索引长度）
 *          * 查看有排序操作 extra 中是否由file sort
 *              * 无索引+索引,顺序不一致则判断是否能够修改查询顺序
 *          * 查看是否由复杂表连接操作（ref或者eq_ref）
 *              * 判断join_buffer_size是否过小导致了多次表扫描
 *              * 建立索引加快连接过程
 *          * gruop by/order by （Using temporary Using filesort）
 *              * sort_buffer_size过小，导致磁盘临时文件排序
 *              * tmp_table_size过小，导致临时表需要写入磁盘
 *     * 首先通过show profiling查看详细执行时间
 *          * sending data
 *          * Creating tmp table/Copying to tmp table on disk
 *          * locked
 * 3. Mysql不同隔离级别如何实现
 *    * 读未提交：当前读+行锁
 *    * 读已提交：MVVC读（每个读一个readview）+ 行锁
 *    * 可重复读：MVVC读（一个事务一个readview） + 行锁
 *    * 顺序一致性：表级共享锁读+表级排他锁
 * 分布式理论补充
 * 1. Binlog 和 redo log 一致性问题
 *      * redo log持久化成功，binlog失败，会导致主从状态不一致，当主节点失效，从节点成为新的主时会出现幻读问题
 *      * Mysql通过两阶段提交实现 binlog和redo log的一致性持久化，保证了事务同时存在于存储引擎和binlog中
 *          * prepare阶段：获取prepare_commit_mutex锁，生成xid以及redo持久化和undo日志，将事务状态设置为TRX_PREPARED
 *          * 记录binlog日志，并将binlog持久化（sync_binlog）
 *          * 调用引擎提交事务 commit
 *      * 事务回滚还是提交有binlog日志中的xid_log_event决定
 * 2. binlog 与 redo log 顺序一致性的问题
 *      * 为了保证并发情况下，bin log 和 redo log刷盘顺序相同，Mysql5.6之前使用prepare_commit_mutex，只有上一个事务释放锁之后
 *        下一个事务才能够继续宁prepare操作
 *      * 为了解决每个事务获取一次锁/刷盘两次的操作，MySQL 5.6引入了BLGC（Binary Log Group Commit）组提交技术
 *          * 将事务按照顺序存放到队列中，其中队列第一个事务(线程)为leader,其余事务为follower
 *          * 提交过程分为三个阶段
 *              * flush stage: 获取lock_log mutex, 将队列中所有事务的binlog按顺序写入内存
 *              * sync stage: 释放lock_log mutex,获取lock_sync mutex, 落盘
 *              * commit stage: 释放lock_sync mutex锁，获取Lock_commit mutex，按照队列顺序提交队列中的事务，唤醒对应线程
 *           * 控制参数为：binlog_max_flush_queue_time：在flush阶段等待的时间，默认为0，时间越长意味着队列越长
 *                      等待binlog_group_commit_sync_delay毫秒直到达到binlog_group_commit_sync_no_delay_count
 * 3. follower read 如何保证一致性
 *      1. read index:
 *          * leader在响应读请求前需要首先确定自己是否认为leader(通过多数节点心跳)
 *          * follower向leader查询commitIndex, 若commitIndex > applyIndex，则副本等待日志应用到对应index后再响应用户请求（TiKV）
 *      2. lease read: 解决read index在确认主身份时heartbeat消息的通信成本
 *          * leader在选举成功后维护一个lease,在当前lease内认为不会有其他的leader出现（即自己不会被取代）
 *          * lease < election_timeout
 *          * 当从节点请求read index时，若当前在lease期限内，则不需要再通过心跳消息确认自身为leader
 * 5. BASE理论
 *      * 对于CAP理论的弱化，通过妥协一定程度的一致性，提供一定程度的可用性
 *      * 主要概念
 *          （1）Basically Available：提供一定程度的可用性
 *          （2）Soft State:软状态指允许系统中的数据存在中间状态，并认为该中间状态的存在不会影响系统的整体可用性，
 *              即允许系统在不同节点的数据副本之间进行数据同步的过程存在延时
 *           (3)Eventually Consistency:经过一段时间同步后，所有副本都达到一致性的
 * 6. 慢查询优化
 *      * 开启慢查询日志记录慢查询+EXPLAIN查看慢查询执行计划，主要关注type+extra字段
 *      * 首先看没有走索引，没有走索引是什么原因
 *          * 没有索引：能不能建立对应索引
 *          * 有索引但是没走：索引失效场景，索引区分度不够导致没有选择走索引
 *      * join的问题看看是否小表驱动大表，jion次数过多，有没有临时表排序
 *      * exists与in的区别在于：
 *          * exists外部表为驱动表，先执行外部表查询，再执行内部表查询->适合主表记录较少，子查询表较大且有索引
 *          * in 内部查询一次即返回结果，与外部表做笛卡尔积后通过条件筛选->适合主表大又有索引的场景
 *      * 先知慢查询优化案例
 *          * 查询归属某个人的风险记录时，人名模糊查询导致风险事件全表扫描，优化策略：子查询查询用户名+外部查询插风险事件
 *          * 查询某个组织的待处理和处理中风险：拆分sql，将组织查询独立出来，并通过caffeine本地缓存
 * */
public class Basic {
    public final String name;
    public final int number;

    public Basic(String name, int number) {
        this.name = name;
        this.number = number;
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

}
