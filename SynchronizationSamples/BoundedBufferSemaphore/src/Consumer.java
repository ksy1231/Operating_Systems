/**
 * Created by Steve on 4/27/14.
 */
import java.util.Date;
public class Consumer implements Runnable {
    private Buffer buffer;
    public Consumer(Buffer buffer) {
        this.buffer = buffer;
    }
    public void run() {
        Date message = new Date();
        int count=0;

        // fetch one object from the buffer at a time and print
        while (true) {
            ++count;
            // nap for awhile
            SleepUtilities.nap();
            // consume an item from the buffer
            try {message = (Date)buffer.remove();} catch (InterruptedException e) {};
            System.out.println("-------------->CONSUMER["+ count +"]: Time-" + message);
        }
    }
}

