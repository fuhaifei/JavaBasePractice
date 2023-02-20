package annotation;

import org.junit.Test;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 注解（annotation）: 一类提供给编译器使用的特殊注释、说明，起到为类，方法提供补充说明的作用。{@link java.lang.annotation}
 * 属性包括：{@link java.lang.annotation.ElementType} 指定注解的适用范围（类注解，方法注解，参数注解等）,一对多的关系
 *          {@link java.lang.annotation.Retention}  注解保留的范围，只在编译使用或者在运行时访问
 * 通用注解定义方式： @Documented
 *                 @Target(ElementType.TYPE) 指定使用范围
 *                 @Retention(RetentionPolicy.RUNTIME) 指定保留范围
 *                 public @interface MyAnnotation1 {
 *                 }
 * 自带注解包括：@Deprecated  -- @Deprecated 所标注内容，不再被建议使用。
 *             @Override    -- @Override 只能标注方法，表示该方法覆盖父类中的方法。
 *             @Documented  -- @Documented 所标注内容，可以出现在javadoc中。
 *             @Inherited   -- @Inherited只能被用来标注“Annotation类型”，它所标注的Annotation具有继承性。
 *             @Retention   -- @Retention只能被用来标注“Annotation类型”，而且它被用来指定Annotation的RetentionPolicy属性。
 *             @Target      -- @Target只能被用来标注“Annotation类型”，而且它被用来指定Annotation的ElementType属性。
 *             @SuppressWarnings -- @SuppressWarnings 所标注内容产生的警告，编译器会对这些警告保持静默。
 * 注解的使用：1. 编译检查：例如@Override，@Deprecated等，在编译时按照注解规则对代码进行检查
 *           2. 在反射中使用注解：框架中的
 * */
public class BasicUse {
    public BasicUse(){

    }

    private class TestUseOne{
        @Deprecated
        public void deprecateOne(){
            System.out.println("this method is deprecated");
        }

        @Override
        public String toString(){
            System.out.println("override parent method");
            return "";
        }
    }

    @Test
    public void TestUseOne(){
        TestUseOne testUseOne = new TestUseOne();
        testUseOne.deprecateOne();
        testUseOne.toString();
    }


    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestController{
        String controllerName() default "myController";
        String urlPattern() default "/";
        int noDefault();
    }


    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PrintAnno{
    }


    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MyRequestMapping{
        String urlPattern() default "/";
    }
    @PrintAnno
    @TestController(noDefault = 1)
    public class MyControllerTest{
        public MyControllerTest() {
        }

        public void MyController(){

        }
        @MyRequestMapping(urlPattern = "/getName")
        public String getName(){
            System.out.println("调用/getName");
            return "/getName";
        }
        @MyRequestMapping(urlPattern = "/getPassWord")
        public String getPassword(){
            System.out.println("调用/getPassWord");
            return "/getPassWord";
        }
    }

    public String notFound(){

        System.out.println("404 not found");
        return "404 not found";
    }

    @Test
    public void TestUseTwo() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        //获取所有方法注解
        Class<MyControllerTest> myControllerClass = MyControllerTest.class;
        //首先获取类级别注解
        Annotation[] annotations = myControllerClass.getAnnotations();
        System.out.println(annotations.length);
        for(Annotation annotation:annotations){
            if(annotation.annotationType().equals(PrintAnno.class)){
                System.out.println("输出注解要求输出xxx");
            }else if(annotation.annotationType().equals(TestController.class)){
                System.out.println("当前类为controller");
            }
        }

        //获取所有方法注解
        Map<String, Method> callMap = new HashMap<>();
        Method[] methods = myControllerClass.getMethods();

        for(Method method:methods){
            if(method.isAnnotationPresent(MyRequestMapping.class)){
                callMap.put(method.getAnnotation(MyRequestMapping.class).urlPattern(), method);
            }
        }

        //最后随便定义一个空方法
        Method notFoundMethod = this.getClass().getMethod("notFound");

        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        while(!input.isEmpty()){
            Method callMethod = callMap.getOrDefault(input, notFoundMethod);
            //调用方法并输出结果
            Object o;
            if(callMethod.getDeclaringClass().equals(MyControllerTest.class)){
                o = callMethod.getDeclaringClass().getConstructors()[0].newInstance(new BasicUse());
            }else{
                o = callMethod.getDeclaringClass().getConstructor().newInstance();
            }
            callMethod.invoke(o);
            input = scanner.next();
        }
    }

}
