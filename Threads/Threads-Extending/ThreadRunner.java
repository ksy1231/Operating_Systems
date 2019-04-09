public class ThreadRunner
{
   ThreadRunner( ) {
      Worker runner = new Worker();
      runner.start();
      while ( true ) {
         System.out.println("I am the main thread");
         SleepUtilities.nap();         
      }
   }

   public static void main(String args[]) {
      ThreadRunner thread = new ThreadRunner( );
   }
}