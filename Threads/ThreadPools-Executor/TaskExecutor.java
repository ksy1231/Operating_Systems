import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class TaskExecutor {
   public static void main(String[] args) {
      PrintTask t1 = new PrintTask("Task1");
      PrintTask t2 = new PrintTask("Task2");
      PrintTask t3 = new PrintTask("Task3");

      System.out.println("Starting Executor");

      ExecutorService threadExecutor = Executors.newCachedThreadPool();

      threadExecutor.execute(t1);
      threadExecutor.execute(t2);
      threadExecutor.execute(t3);
      System.out.println("Threads Executed");
      SleepUtilities.nap();
      threadExecutor.shutdown();
      SleepUtilities.nap();
      System.out.println("Tasks started, main ends");
   }
}
