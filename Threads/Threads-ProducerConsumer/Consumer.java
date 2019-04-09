import java.util.Date;

class Consumer implements Runnable {
   private Channel<Date> queue;
   public Consumer(Channel<Date> queueInit) {
      queue = queueInit;
   }

   public void run() {
      Date message;

      while (true) {
         // nap for a while
         SleepUtilities.nap();

         // consume an item from the buffer
         message = queue.receive();

         if (message != null)
            System.out.println("Consumer consumed " + message);
      }
   }
}