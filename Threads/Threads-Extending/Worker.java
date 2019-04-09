class  Worker extends Thread
{
   public void run() {
      while ( true ) {
        System.out.println("I am a Worker Thread");
		SleepUtilities.nap();
      }
   }
}