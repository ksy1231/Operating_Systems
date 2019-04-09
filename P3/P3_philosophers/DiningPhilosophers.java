package edu.uwb.css;

/**
 *
 * Created by mike on 7/11/2016.
 */
public class DiningPhilosophers {
    DiningState[] state;

    // This will let all the tests run (and fail)
    // You'll want to remove it once you actually create an array :)
    int nPhil;
    public DiningPhilosophers(int nPhilosophers) {
        nPhil =  nPhilosophers;
    }

    public void takeForks(int i) {
        Main.TPrint( "TakeForks:   i=" + i);
    }

    public void returnForks(int i) {
        Main.TPrint( "returnForks:   i=" + i );
    }

    public int numPhilosophers() {
        return nPhil;
    }

    public DiningState getDiningState(int i) {
        return DiningState.THINKING;
    }
}
