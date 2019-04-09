import java.util.concurrent.Semaphore;

public class SemaphoreFactory {
    public static void main(String args[]) {
        Semaphore sem = new Semaphore(1);
        Thread[] bees = new Thread[5];

        for(int i=0; i < 5; i++) {
            bees[i] = new Thread(new Worker(sem, "BEE"+i));
        }
        System.out.println("Threads Initialized");
        for(int i=0; i < 5; i++) {
            bees[i].start();
        }
        System.out.println("Threads Started");
    }
}