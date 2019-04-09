/**
 * Created by Steve on 4/28/14.
 */
public class Writer implements Runnable {
    private SemaphoreDB db;
    int nap_time;

    public Writer(SemaphoreDB db, int nap_time) {
        this.db = db;
        this.nap_time = nap_time;
    }

    public void run() {
        int count = 0;
        while(true) {
            SleepUtilities.nap(nap_time);
            db.acquireWriteLock();

            // do write to database

            System.out.println("Writer has database lock ...writing:" + ++count);
            db.write(count);

            SleepUtilities.nap(nap_time);

            db.releaseWriteLock();
        }
    }
}
