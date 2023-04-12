/**
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
 *              * Commit stage: 释放lock_sync mutex锁，获取Lock_commit mutex，按照队列顺序提交队列中的事务，唤醒对应线程
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
 * 3. BASE理论
 *      * 对于CAP理论的弱化，通过妥协一定程度的一致性，提供一定程度的可用性
 *      * 主要概念
 *          （1）Basically Available：提供一定程度的可用性
 *          （2）Soft State:软状态指允许系统中的数据存在中间状态，并认为该中间状态的存在不会影响系统的整体可用性，
 *              即允许系统在不同节点的数据副本之间进行数据同步的过程存在延时
 *           (3)Eventually Consistency:经过一段时间同步后，所有副本都达到一致性的
 * */
public class TempBasic {
}
