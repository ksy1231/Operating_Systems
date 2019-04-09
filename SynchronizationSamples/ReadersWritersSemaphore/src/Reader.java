/**
 * Created by Steve on 4/28/14.
 */
public class Reader implements Runnable {
    private SemaphoreDB db;
    String name;
    int nap_time;

    public Reader(SemaphoreDB db, String name,int nap_time) {
        this.db = db;
        this.name = name;
        this.nap_time = nap_time;
    }

    public void run() {
        while(true) {
            // take a nap
            SleepUtilities.nap(nap_time);

            // lock against other readers/writers
            db.acquireReadLock();

            // do read from database
            System.out.println("Reader" +name +" Reading:" + db.read());
            SleepUtilities.nap(nap_time);

            db.releaseReadLock();
        }
    }
}
