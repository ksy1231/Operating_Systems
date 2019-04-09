public class HardwareData {

    private boolean value = false;

    public HardwareData( boolean initialValue ) {
        this.value = initialValue;
    }

    public boolean get() {
        return value;
    }

    public void set( boolean newValue ) {
        this.value = newValue;
    }

    public boolean TestAndSet( boolean newValue ) {
        boolean oldValue = this.get();
//        if(!oldValue) this.set( newValue );
        this.set( newValue );
        return oldValue;
    }

    public void swap(HardwareData other) {
        boolean temp = this.get();
//        if(!temp) this.set( other.get() );
        this.set( other.get() );
        other.set( temp );
    }
}