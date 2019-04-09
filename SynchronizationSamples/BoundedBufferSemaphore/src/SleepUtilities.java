/**
 * utilities for causing a thread to sleep.
 */
public class SleepUtilities
{
    private static final int NAP_TIME = 5000;
    /**
     * Nap between zero and NAP_TIME msec.
     */
    public static void nap() {
        nap( NAP_TIME );
    }

    /**
     * Nap between zero and duration milliseconds.
     */
    public static void nap( int duration ) {
        int sleeptime = (int) ( duration * Math.random() );
        try {
            Thread.sleep( sleeptime );
        } catch (InterruptedException e) {}
    }

    public static void delay( int duration_msec ) {
        try {
            Thread.sleep( duration_msec );
        } catch (InterruptedException e) {}
    }

}
