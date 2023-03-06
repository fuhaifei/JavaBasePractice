package design_parttern.adaptor;

import org.junit.Test;

public class TestAdaptor {

    @Test
    public void testAdapter(){
        Target target = new Target();
        Adaptee adaptee = new Adaptee();

        //客户调用普通接口
        target.request();
        //客户调用特殊接口
        //adaptee.specialRequest();
        new Adapter(adaptee).request();
    }
}
