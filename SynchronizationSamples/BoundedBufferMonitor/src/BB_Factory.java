/**
 * Created by Steve on 4/27/14.
 */
import java.util.Date;

public class BB_Factory {

        public static void main (String args[]){

        // must declare the buffer type before passing it to threads
        Buffer<Date> buffer = new BoundedBuffer<Date>();

        // now create the producer and consumer threads
        Thread producer = new Thread(new Producer(buffer));
        Thread consumer = new Thread(new Consumer(buffer));
        producer.start();
        consumer.start();
    }
}