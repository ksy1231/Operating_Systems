import java.util.Random;
import java.util.concurrent.Semaphore;

public class Worker implements Runnable {
    private Semaphore sem;
    private String name;
    private final int CS_BURST_TIME = 100;
    private final int REMAINDER_TIME = 500;

    public Worker(Semaphore s, String n) {
        sem = s;
        name = n;
    }

    public void run() {
        // wait 1 sec for all threads to get initialized
        SleepUtilities.delay(1000);

        // run forever loop
        while(true) {
            System.out.printf("Attempting Critical Section [%s]\n", name);
            try {sem.acquire();} catch (InterruptedException e) {}
            criticalSection();
            sem.release();
            System.out.printf("Exiting CS and Executing Remainder Section [%s]\n", name);
            remainderSection();
        }
    }

    private void criticalSection () {
        System.out.printf("--Critical Section [%s]\n", name);
        SleepUtilities.nap( CS_BURST_TIME );
    }

    private void remainderSection() {
        SleepUtilities.nap( REMAINDER_TIME  );
    }
}

