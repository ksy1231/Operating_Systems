import java.util.Date;

class Producer implements Runnable {
   private Channel<Date> queue;
   public Producer(Channel<Date> queueInit) {
      queue = queueInit; 
   }

   public void run() {
      Date message;

      while (true) {
         // nap for a while
         SleepUtilities.nap();

         // produce an item and stick it in the buffer
         message = new Date();
         System.out.println("Producer produced " + message);
         queue.send(message);
      }
   }
}