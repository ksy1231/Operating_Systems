/**
 * Created by Steve on 4/28/14.
 */
public interface SemaphoreDB {
    public void acquireReadLock();
    public void acquireWriteLock();
    public void releaseReadLock();
    public void releaseWriteLock();

    public int read();
    public void write(int data);
}
