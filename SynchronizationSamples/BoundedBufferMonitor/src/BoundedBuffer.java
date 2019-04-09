/**
 * Created by Steve on 4/27/14.
 */

public class BoundedBuffer<E> implements Buffer<E> {
    private static final int   BUFFER_SIZE = 5;
    private int in, out, count;
    private E[] buffer;

    // constructor method
    public BoundedBuffer( ) {
        // buffer is initially empty
        in = 0;  out = 0; count = 0;
        buffer = (E[]) new Object[BUFFER_SIZE];
    }

    // insert an object into the buffer
    public synchronized void insert(E item) {
        // go into wait state if the buffer is full
        while (count == BUFFER_SIZE) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }

        // add an item to the buffer, this is a CS!
        buffer[in] = item;
        in = (in + 1) % BUFFER_SIZE;
        ++count;
        notify();   // notify the "remover"
    }

    // remove an object from the buffer
    public  synchronized E remove( ) {
        // go into wait state if the buffer is empty
        while (count == 0) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        // remove an item from the buffer, also a CS!
        E item = buffer[out];
        out = (out + 1) % BUFFER_SIZE;
        --count;

        notify();   // notify the "inserter"
        return item;
    }
}

