package cache;

import com.google.common.base.Optional;
import com.google.common.cache.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BasicUse {

    public static class MockDB{
        public static List<String> getData(String key){
            System.out.println("getting from DB,key:" + key + " please wait...");
            List<String> returnList = null;
            // 模仿从数据库中取数据
            try {
                switch (key) {
                    case "CN" -> returnList = ImmutableList.of("上海", "北京", "广州", "深圳");
                    case "UK" -> returnList = ImmutableList.of("stanford", "london", "Birmingham", "Glasgow");
                    case "USA" -> returnList = ImmutableList.of("Birmingham", "Montgomery", "Mobile", "Miami");
                    case "EU" -> returnList = ImmutableList.of("Austria", "Belgium", "Bulgaria", "Croatia");
                }
            } catch (Exception e) {
                // 记日志
            }
            return Optional.fromNullable(returnList).or(Collections.EMPTY_LIST);
        }
    }

    @Test
    public void testGuava(){
        LoadingCache<String, List<String>> cityCache = CacheBuilder.newBuilder()
                //缓存最大容量
                .maximumSize(2)
                //缓存超时时间
                .expireAfterWrite(3, TimeUnit.SECONDS)
                //添加缓存清除监听器
                .removalListener(new RemovalListener<String, List<String>>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, List<String>> notification) {
                        System.out.println("key {" + notification.getKey() + "} value {" +
                                notification.getValue() + "} removed for:" + notification.getCause());

                    }
                })
                //设置自动加载缓存机制
                .build(
                        new CacheLoader<String, List<String>>() {
                            @Override
                            public List<String> load(String key) throws Exception {
                                return MockDB.getData(key);
                            }
                        }
                );

        String[] keys = new String[]{"CN","UK","USA","EU"};
        try {

            System.out.println("load from cache once : " + cityCache.get("USA"));
            System.out.println("load from cache once : " + cityCache.get("CN"));
            System.out.println("load from cache once : " + cityCache.get("UK"));
            Thread.sleep(2000);
            System.out.println("load from cache two : " + cityCache.get("USA"));
            Thread.sleep(2000);
            System.out.println("load from cache three : " + cityCache.get("USA"));
            Thread.sleep(2000);
            System.out.println("load not exist key from cache : " + cityCache.get("C"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
