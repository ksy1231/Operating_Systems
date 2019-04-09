import java.lang.Thread;

public class TaskCreator {
   public static void main( String[] args ) {
      System.out.println("Creating threads");

      // create each thread
      Thread t1 = new Thread(new PrintTask("Task 1"));
      Thread t2 = new Thread(new PrintTask("Task 2"));
      Thread t3 = new Thread(new PrintTask("Task 3"));

      System.out.println("Threads created, starting tasks");

      // start threads
      t1.start();
      t2.start();
      t3.start();

      System.out.println("Tasks started, main ends");
   }
}
