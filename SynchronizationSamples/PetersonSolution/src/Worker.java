/**
 * Created by Steve on 4/23/14.
 */
public class Worker extends Thread {
    private String name;
    private int id;
    private MutualExclusion shared;

    public Worker(String n, int i, MutualExclusion s) {
        name = n;	  // my name
        id = i;	  // my thread id
        shared = s; // a share object including a critical section
    }
    public void run() {
        while (true) {
            shared.enteringCriticalSection(id);
            System.out.println("CS: "+name);
            SleepUtilities.nap( 100 );
//            try {Thread.sleep(500);} catch (InterruptedException e) {};
            shared.leavingCriticalSection(id);
            // out of critical section code
        }
    }
}
