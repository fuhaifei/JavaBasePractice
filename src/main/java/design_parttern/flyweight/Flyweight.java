package design_parttern.flyweight;

public class Flyweight implements FlyweightInterface{
    private String info;

    public Flyweight(String info) {
        this.info = info;
    }
    public Flyweight() {
    }

    @Override
    public void showMyInfo() {
        System.out.println("i am b");
    }

    @Override
    public void setInfo() {
        this.info = info;
    }


}

