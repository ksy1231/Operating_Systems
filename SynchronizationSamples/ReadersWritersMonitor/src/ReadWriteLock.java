/**
 * An interface for reader-writer locks.
 *
 * Figure 6.17
 * In the text wedo not have readers and writers
 * pass their number into each method. However we do so
 * here to aid in output messages.
 *
 * @author Gagne, Galvin, Silberschatz
 * Operating System Concepts with Java - Eighth Edition
 * Copyright John Wiley & Sons - 2010. 
 */

public interface ReadWriteLock
{
	public abstract void acquireReadLock(int readerNum);
	public abstract void acquireWriteLock(int writerNum);
	public abstract void releaseReadLock(int readerNum);
	public abstract void releaseWriteLock(int writerNum);
}
