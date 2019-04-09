/**
 * Created by Steve on 4/27/14.
 */
import java.util.concurrent.Semaphore;

public class BoundedBuffer<E> implements Buffer<E> {
    private static final int   BUFFER_SIZE = 5;
    private Semaphore mutex, empty, full;
    private int in, out;
    private E[] buffer;

    // constructor method
    public BoundedBuffer( ) {
        // buffer is initially empty
        in = 0;  out = 0;
        buffer = (E[]) new Object[BUFFER_SIZE];   // Shared buffer can store five objects.
        mutex = new Semaphore( 1 );            // Semaphore mutex initialized to 1
        empty = new Semaphore(BUFFER_SIZE);    // Semaphore empty initialized to BUFFER_SIZE
        full = new Semaphore( 0 );             // Semaphore full initialized to 0
    }

    // Semaphore P function ('proberen') - block on the empty semaphore, lock the buffer
    private void P_empty() {
        try { empty.acquire(); } catch (InterruptedException e) {}; // blocked if empty
        try { mutex.acquire(); } catch (InterruptedException e) {}; // blocked while someone is using buffer
    }

    // Semaphore P function ('proberen') - block on the full semaphore, lock the buffer
    private void P_full() {
        try { full.acquire();  } catch (InterruptedException e) {}; // blocked if full
        try { mutex.acquire(); } catch (InterruptedException e) {}; // blocked while someone is using buffer
    }

    // Semaphore V function ('verhogen') - block on the full semaphore, lock the buffer
    private void V_empty() {
        mutex.release();
        empty.release();
    }

    // Semaphore V function ('verhogan') - release the full semaphore, lock the buffer
    private void V_full() {
        mutex.release();
        full.release();
    }

    // insert an object into the buffer
    public void insert(E item) {
        P_empty();
        // add an item to the buffer, this is a CS!
        buffer[in] = item;
        in = (in + 1) % BUFFER_SIZE;
        V_full();
    }

    // remove an object from the buffer
    public E remove( ) {
        P_full();
        // remove an item from the buffer, also a CS!
        E item = buffer[out];
        out = (out + 1) % BUFFER_SIZE;
        V_empty();
        return item;
    }
}

