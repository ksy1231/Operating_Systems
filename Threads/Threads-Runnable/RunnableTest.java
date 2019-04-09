public class RunnableTest {
   public static void main(String args[]) {
      RunnableTest main = new RunnableTest( );
   }
   RunnableTest( ) {
      Runnable runner = new Worker();
      Thread myThread = new Thread(runner);
      myThread.start();
      while ( true )
         System.out.println("I am the main thread");
   }
}
