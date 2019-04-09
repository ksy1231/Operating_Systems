/**
 * Created by Steve on 4/27/14.
 */
import java.util.Date;
// Buffer interface specifies methods called by Producer and Consumer
public interface Buffer<E> {

    // place object into buffer
    public void insert(E item) throws InterruptedException;

    // remove an object from the buffer
    public E remove() throws InterruptedException;
}
