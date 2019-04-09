public class ThreadFunc extends Thread {
   private String param;
   public ThreadFunc( String strInit ) {
      param = strInit;
   }
   public void run( ) {
      for ( int i = 0; i < 6; i++ ) {
         try {
            Thread.sleep( 2000 );
         } catch ( InterruptedException e ) { };
         System.out.println( " Slave["+ i + "]: " + param );
      }
   }
}
