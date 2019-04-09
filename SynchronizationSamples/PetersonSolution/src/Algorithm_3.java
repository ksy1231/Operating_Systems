/**
 * Created by Steve on 4/23/14.
 */
public class Algorithm_3 extends MutualExclusion {
    private volatile int turn;
    private volatile boolean[] flag = new boolean[2];

    public Algorithm_3() {
        flag[0] = false;
        flag[1] = false;
        turn = TURN_0;
    }
    public void enteringCriticalSection(int t) {
        int other = 1 - t; // If I am thread 0, the other is 1
        flag[t] = true;    // I'm declaring I will enter CS
        turn = other;      // Yield to other
        // If the other declared and turn is in the other, wait!
        while ((flag[other] == true) && (turn == other)) {
            Thread.yield();
        }
    }
    public void leavingCriticalSection(int t) {
        flag[t] = false;
    }
}
