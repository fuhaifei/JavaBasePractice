package design_parttern.adaptor;

public class Adapter {

    private Adaptee adaptee;

    public Adapter(Adaptee adaptee){
        this.adaptee = adaptee;
    }

    public void request(){
        adaptee.specialRequest();
    }
}
