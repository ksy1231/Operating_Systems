class  Worker implements Runnable {
   public void run() {
      while ( true )
         System.out.println("I am a Worker Thread");
   }
}
