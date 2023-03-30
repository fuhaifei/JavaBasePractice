package collections;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Java集合框架总结
 * 1. 集合接口层次
 *      * Collection：add()/addAll() clear() remove()/removeAll() isEmpty() contains()/containsAll()
 *                    size() toArray() iterator()
 *          * List(可通过Index访问的集合)：add(index, element)/get(index)/remove(index)/set(index, element)
 *              * Vector：    古老集合实现，类似于线程安全版的ArrayList,
 *              * ArrayList： 变长数组
 *              * LinkedList：双向链表
 *          * Set（不重复元素集合）:of() 构建集合
 *              * HashSet（无序，非线程安全，元素可为null,基于hashcode+equals()，底层基于hashmap）
 *                  -> LinkedHashSet(底层基于LinkedHashMap)
 *              * SortedSet: first()/last()/headSet()/tailSet()
 *                  -> NavigableSet: lower, floor, ceiling, and higher
 *                      -> TreeSet(底层基于TreeMap)
 *          * Queue
 *              * Deque:双端队列
 *                  * ArrayDeque: Resizable-array implementation of the Deque interface.
 *                  * LinkedList: 链表双端队列
 *              * BlockingQueue:阻塞队列
 *                  * 并发部分总结过了
 *      * Map：get()/put()/of()/remove()/keySet()/values()/entrySet()
 *          * Hashtable：线程安全的古老Map实现类，Hashtable 不允许使用 null 作为 key 或 value。
 *              -> Properties：
 *          * Hashmap：Key相同两个标准hashcode/equals()
 *              -> LinkedHashMap：将插入元素通过链表连接
 *          * SortedMap：firstKey()/lastKey()/headMap()/tailMap()
 *              -> NavigableMap：lowerEntry, floorEntry, ceilingEntry, and higherEntry
 *                  -> TreeMap：自然排序和定制排序
 *      * Iterable接口->Iterator对象 + foreach->next()/hasNext()
 * 2. 工具类
 *      * Collections
 *      * Arrays
 * 3. 集合框架底层原理
 *      * Vector 和 ArrayList
 *          * 底层存储：Object[] elementData
 *          * 初始化：Vector 默认初始化长度为10；
 *                  ArrayList 在JDK6.0及之前为10，JDK8版本后只设置一个空数组引用，添加第一个元素时再真正开辟
 *                             Object[] EMPTY_ELEMENTDATA = {};
 *          * 扩容机制：Vector 创建时未指定capacityIncrement，则长度翻倍；指定了capacityIncrement，长度增加capacityIncrement
 *                    ArrayList 长度扩大为 oldCapacity + oldCapacity >> 1，即原来空间大小的1.5倍
 *          * 线程安全：Vector在方法上添加了synchronized关键字，保证并发安全性
 *                    ArrayList 线程不安全
 *          * modCount: modCount记录造成数组长度变化的操作，用在iterator遍历过程中判断是否存在对于集合的并发修改
 *                      If the value of this field changes unexpectedly, the iterator (or list iterator) will throw a
 *                      ConcurrentModificationException in response to the next, remove, previous, set or add operations.
 *                      final void checkForComodification() {
 *                          if (modCount != expectedModCount)
 *                              throw new ConcurrentModificationException();
 *                      }
 *       * LinkedList：
 *          * 底层存储：transient Node<E> first; transient Node<E> last; 指向第一个和最后一个元素，链表为空时均为null
 *       * HashMap:
 *          * 初始化：需要传入initialCapacity, loadFactor两个参数
 *                  默认DEFAULT_INITIAL_CAPACITY = 16，DEFAULT_LOAD_FACTOR = 0.75f，
 *                  threshold = 大于 DEFAULT_INITIAL_CAPACITY 的最小2^n
 *          * 添加流程
 *              * 计算hashcode: (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16));
 *                  * 低16位与高16位取异或，利用高位信息减少碰撞
 *              * 计算桶位置：(n - 1) & hash
 *              * 放置元素：桶位置为空，直接放置；
 *                        * 存在相同key（hashcode相同，且equals()相同）,修改key对应value
 *                        * 不为空插入到链表尾部，若链表长度>TREEIFY_THRESHOLD(8),转化为红黑树（数组长度超过64）
 *          * resize()：数组长度变为原来的两倍，并进行rehash,红黑树需要进行拆分，若长度<=UNTREEIFY_THRESHOLD,退化为链表
 *              * 时机：当前元素个数/load_factor>= threshold时
 *              * 扩容大小：新大小为大于 元素个数/load_factor 的最小2^n
 *          * JDK8和之前的区别：将<K,V>存储在内部Node/TreeNode类中，Node类继承自Entry
 *              * 1.8之前采用头插法，1.8采用尾插法
 *      * LinkedHashMap: 基于链表维护元素加入Map的顺序
 *          * 继承Node类，添加了 Entry<K,V> before, after属性形成链表
 *          * 重写了newNode方法，创建Node时会将节点添加到链表尾部；删除节点后调用afterNodeRemoval（）方法，将节点从链表删除
 *          * 继承关系为：Map.Entry<K,V> -> HashMap.Node<K,V> -> LinkedHashMap.Entry<K,V> -> HashMap.TreeNode<K,V>
 *          * 另外提供了LRU的实现机制，accessOrder + afterNodeAccess 可实现按照访问顺序排序
 *      * TreeMap: 基于红黑树实现的排序HashMap
 *          * 继承 Entry<K,V> implements Map.Entry<K,V>，实现了红黑树节点
 *          * 不再基于key的hashcode放置元素，根据comparator的顺序关系放置，如果两个Key大小相同，则认为位于同一个位置
 *      * HashMap/LinkedHashMap均支持key = null, 取0作为null的哈希值，TreeMap不支持key=null,Objects.requireNonNull(key);
 *      * HashSet/LinkedHashSet支持key=null, TreeSet不支持
 * 4. 线程安全集合
 *      * 安全集合：Hashtable,Vector
 *      * 基于Collections装饰的线程安全集合：Collections.synchronizedList
 *      * concurrent下的线程安全集合类
 *          * Blockingxxx
 *          * CopyOnWriteXXX
 *          * cONCURRENTxxx
 *      * ConcurrentHashMap：
 *          * JDK1.7实现：ConcurrentHashMap 把哈希桶数组切分成小数组（Segment ），每个小数组有 n 个 HashEntry 组成。
 *              * 创建默认创建16个Segment，以Segment为并发控制的最小单位（并发度），Segment初始化为大于设置数量的最小2^n长度
 *              * put首先获取到对应的segment,获取对应segment锁后(ReentrantLock)，再进行哈希表操作
 *              * get 不需要加锁，直接获取对应segment的对应值(volatile)
 *              * rehash（） 只在segment内，所以put时已经获取锁，不需要额外操作
 *              * 锁的segment，其他segment可以并发操作
 *          * JDK1.8实现：基本与HashMap相同：数组+链表/红黑树
 *              * 懒惰初始化，基于cas操作初始化表（CAS设置SIZECTL，只有操作成功的线程能够进行初始化）
 *              * put() 基于cas操作初始化表和每个桶位置对应的链表头,当需要执行链表插入时，只需要基于synchronized锁住链表头
 *              * get() 无锁操作，若get获得的是ForwardingNode，则在新表中进行搜索(volatile)
 *              * size() 元素个数统计类似于ADDER基于cell的实现，无竞争向baseCount添加，有竞争向cell累加
 *              * treeify()/untreeify() 基于synchronized锁住链表头
 *          * JDK1.7 -> JDK1.8 本质上就是并发度从多个桶到单个桶的变化
 *          * ConcurrentHashMap的value不能为null,因为并发情况下无法判断是由于Key不存在导致的null还是value本身就是null
 *
 *
 * */
public class BasicUse {

    /**
     * 测试集合框架接口
     * */
    @Test
    public void testCollection(){
        Collection<Integer> collection = new ArrayList<>();
        List<Integer> list = new ArrayList<>();
        List<Integer> list1 = new Vector<>();
        List<Integer> list2 = new LinkedList<>();
        Set<Integer> set1 = new HashSet<>();
        Set<Integer> set2 = new LinkedHashSet<>();
        SortedSet<Integer> set3 = new TreeSet<>();
        Map<Integer, Integer> map = new HashMap();
        Map<Integer, Integer> linkedHashMap = new LinkedHashMap<>();
        SortedMap<Integer, Integer> treeMap = new TreeMap<>();
        Properties table = System.getProperties();
        Object o = table.get("file.encoding");
        System.out.println(o);
    }

    /**
     * 测试工具类方法
     * */
    @Test
    public void testTools(){
        List<Integer> testList = new ArrayList<>();
        //1.排序操作
        Collections.sort(testList, new Comparator<>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return 0;
            }
        });
        Collections.reverse(testList);
        Collections.shuffle(testList);
        //必须是按照自然排序升序排列的列表，保证返回多个元素中的其中一个，若不存在返回(-(insertion point) - 1)
        testList.add(100);
        testList.add(101);
        Collections.sort(testList);

        //2.查找操作
        System.out.println(Collections.binarySearch(testList, 200));
        Collections.max(testList);
        Collections.min(testList);
        Collections.frequency(testList, 10);

        //3.复制替换
        //Collections.copy(new ArrayList<>(), testList);
        Collections.replaceAll(testList, 100, 1000);
        //4. sychronizedxxx(),返回一个同步集合
        Collections.synchronizedList(testList);

        Deque<Integer> deque = new ArrayDeque<>();
        deque.offer(1);
        System.out.println(deque.poll());
    }
}
