public class Worker2 implements Runnable {

    private String name;      // the name of this thread
    private static HardwareData lock;

    public Worker2( String name, HardwareData lock ) {
        this.name = name;
        this.lock = lock;
    }
    /**
     * This run() method tests the swap() instruction
     */
    public void run() {
        HardwareData key = new HardwareData( true );
        while ( true ) {
            System.out.println( name + " wants to enter CS" );
            key.set( true );

            do {
                lock.swap( key );
            } while( key.get() == true );

            MutualExclusionUtilities.criticalSection( name );
            System.out.println( name + " is exiting critical section" );
            lock.set( false );
            MutualExclusionUtilities.remainderSection( name );
        }
    }
}