/**
 * Created by Steve Dame on 4/28/14.
 */
import java.util.concurrent.Semaphore;

public class Database implements SemaphoreDB {
    private int readerCount;
    private Semaphore mutex;
    private Semaphore db;
    int  TheData;

    public Database() {
        readerCount = 0;
        mutex = new Semaphore(1);
        db = new Semaphore(1);
        TheData = 0;
    }

    public int read() { return TheData; }

    public void write (int data) { TheData = data;}

    public void acquireReadLock() {
        try {mutex.acquire(); } catch (InterruptedException e) {}

        ++readerCount;
        if(readerCount == 1)
            try {db.acquire();} catch (InterruptedException e) {}
        mutex.release();
    }

    public void releaseReadLock() {
        try {mutex.acquire(); } catch (InterruptedException e) {}
        --readerCount;
        if(readerCount <= 0)
            db.release();
        mutex.release();
    }

    public void acquireWriteLock() {
        try {db.acquire();} catch (InterruptedException e) {}
    }

    public void releaseWriteLock() {
        db.release();
    }
}


