package IO;

import java.io.*;

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
 *
 * */
public class BasicUse {

}
