/**
 * Worker.java
 *
 * This is a thread that is used to demonstrate solutions
 * to the critical section problem using the test-and-set instruction.
 *
 */

public class Worker implements Runnable {

    private String name;       // the name of this thread
    private HardwareData mutex;      // shared mutex

    public Worker( String name, HardwareData mutex ) {
        this.name = name;
        this.mutex = mutex;
    }

    /**
     * This run() method tests the getAndSet() instruction
     */
    public void run() {
        while ( true ) {
            System.out.println( name + " wants to enter CS" );
            while ( mutex.TestAndSet( true ) ) ;
             //   Thread.yield(); // let someone else get some work done
//            System.out.println( name + " is in critical section" );
            MutualExclusionUtilities.criticalSection( name );
            System.out.println( name + " is out of critical section" );
            mutex.set( false );
            MutualExclusionUtilities.remainderSection( name );
        }
    }
}
