/**
 * Created by Steve on 4/23/14.
 */
public class Algorithm_1 extends MutualExclusion {
    private volatile int turn;    // turn placed in a register

    public Algorithm_1() {
        turn = TURN_0;
    }
    public void enteringCriticalSection(int t) {
        while (turn != t)    // If it is not my turn,
            Thread.yield();   // I will relinquish CPU
    }
    public void leavingCriticalSection(int t) {
        turn = 1 - t; // If I'm thread 0, turn will be 1, otherwise 0
    }
}
