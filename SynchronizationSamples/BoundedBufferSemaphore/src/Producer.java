/**
 * Created by Steve on 4/27/14.
 */
import java.util.Date;
public class Producer implements Runnable {
    private Buffer buffer;

    public Producer(Buffer buffer) {
        this.buffer = buffer;
    }

    public void run() {
        Date message;
        int count=0;

        // pump a date object into the buffer at random sleep times
        while (true) {
            ++count;
            // nap for awhile
            SleepUtilities.nap();
            // produce an item & enter it into the buffer
            message = new Date();
            System.out.println("PRODUCER["+ count +"]: Time-" + message);
            try {buffer.insert(message);} catch (InterruptedException e) {};
        }
    }
}

