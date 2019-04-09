/**
 * Database.java
 *
 * This class contains the methods the readers and writers will use
 * to coordinate access to the database. Access is coordinated using Java synchronization.
 *
 * Figure 6.33
 *
 * @author Gagne, Galvin, Silberschatz
 * Operating System Concepts with Java - Eighth Edition
 * Copyright John Wiley & Sons - 2010. 
 */

public class Database implements ReadWriteLock
{
   // the number of active readers
   private int readerCount;

   // flag to indicate whether the database is in use
   private boolean dbWriting;

   public Database()
   {
      readerCount = 0;
      dbWriting = false;
   }

   // reader will call this when they start reading
   public synchronized void acquireReadLock(int readerNum)
   {
      while (dbWriting == true)
      {
         try { wait(); }
         catch(InterruptedException e) { }
      }

      ++readerCount;

      System.out.println("Reader " + readerNum + " is reading. Reader count = " + readerCount);
   }

   // reader will call this when they finish reading
   public synchronized void releaseReadLock(int readerNum)
   {
      --readerCount;

      // if I am the last reader tell all others
      // that the database is no longer being read
      if (readerCount == 0)
	 notify();

      System.out.println("Reader " + readerNum + " is done reading. Reader count = " + readerCount);
   }

   // writer will call this when they start writing
    public synchronized void acquireWriteLock(int writerNum) {
      while (readerCount > 0 || dbWriting == true) {
         try { wait(); }
         catch(InterruptedException e) {}
      }

      // once there are either no readers or writers
      // indicate that the database is being written
      dbWriting = true;

      System.out.println("writer " + writerNum + " is writing.");
   }

   // writer will call this when they start writing
   public synchronized void releaseWriteLock(int writerNum)
   {
      dbWriting = false;

      System.out.println("writer " + writerNum + " is done writing.");

	/**
	 * This must be notifyAll()  as there may be more than
	 * one waiting reader to read the database and we must
	 * notify ALL of them.
	 */
      notifyAll();
   }
}
