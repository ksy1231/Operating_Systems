public class MyThread {
   public static void main( String args[] ) {
      String arg = args[0];
      ThreadFunc child = new ThreadFunc( arg );
      child.start( );
      for ( int i = 0; i < 5; i++ ) {
         try {
            Thread.sleep( 1000 );
         } catch ( InterruptedException e ) { };
         System.out.println( "Master["+ i +"]: " + arg );
      }
      try {
         child.join( );
      } catch ( InterruptedException e ) { };
      System.out.println( "Master synched with slave" );
   }
}
