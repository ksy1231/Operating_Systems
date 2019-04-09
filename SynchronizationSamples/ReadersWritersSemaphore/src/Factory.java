import java.util.Date;

/**
 * Created by Steve on 4/28/14.
 */
public class Factory {
    public static void main (String args[]){

        // must declare the buffer type before passing it to threads
        Database db = new Database();

        // now create the producer and consumer threads
        Thread reader1 = new Thread(new Reader(db,"R1",1000));
        Thread reader2 = new Thread(new Reader(db,"R2",2000));
        Thread reader3 = new Thread(new Reader(db,"R3",3000));
        Thread writer = new Thread(new Writer(db,500));
        reader1.start();
        reader2.start();
        reader3.start();
        writer.start();
    }

}
