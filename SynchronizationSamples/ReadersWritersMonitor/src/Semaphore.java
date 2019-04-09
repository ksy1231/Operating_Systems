/**
 * Semaphore.java
 *
 * A basic counting semaphore using Java synchronization.
 */

public class Semaphore
{
   private int value;

   public Semaphore(int value) {
      this.value = value;
   }

   public synchronized void acquire() {
      while (value <= 0) {
         try {
            wait();
         }
         catch (InterruptedException e) { }
      }

      value--;
   }

   public synchronized void release() {
      ++value;

      notify();
   }
}

