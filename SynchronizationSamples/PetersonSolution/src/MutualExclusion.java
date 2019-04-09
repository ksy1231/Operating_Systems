/**
 * Created by Steve on 4/23/14.
 */
public abstract class MutualExclusion {
    // turn_0 = 0 allows thread 0 to enter
    // turn_1 = 1 allows thread 1 to enter
    public static final int TURN_0 = 0;
    public static final int TURN_1 = 1;

    // guarantee a mutual execution
    public abstract void enteringCriticalSection(int t);
    // picks up another thread to enter
    public abstract void leavingCriticalSection(int t);
    public static void criticalSection() {
        // simulate the critical section
    }

    public static void nonCriticalSection() {
        // simulate the non-critical section
    }
}
