/**
 * Created by Steve on 4/23/14.
 */
public class Algorithm_2 extends MutualExclusion  {
    private volatile boolean[] flag = new boolean[2];

    public Algorithm_2() {
        flag[0] = false;
        flag[1] = false;
    }
    public void enteringCriticalSection(int t) {
        int other = 1 - t; // If I am thread 0, the other is 1
        flag[t] = true;    // I’m declaring I will enter CS
        while (flag[other] == true)   // If the other is in CS
            Thread.yield(); // I'll relinquish CPU.
    }
    public void leavingCriticalSection(int t) {
        flag[t] = false;    // I declared I'm exiting from CS
    }
}