/**
 * MutualExclusionUtilities.java
 *
 * Utilities for simulating critical and non-critical sections.
 */

public class MutualExclusionUtilities {
    /**
     * critical and non-critical sections are simulated by sleeping
     * for a random amount of time between 0 and 3 seconds.
     */
    public static void criticalSection( String name ) {
        System.out.println("--Critical Section:"+name);
        SleepUtilities.nap( 100 );
    }

    public static void remainderSection( String name ) {
        SleepUtilities.nap( 2000 );
    }
}