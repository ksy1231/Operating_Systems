/**
 * This creates the buffer and the producer and consumer threads.
 *
 * Figure 6.14
 *
 * @author Gagne, Galvin, Silberschatz
 * Operating System Concepts with Java - Eighth Edition
 * Copyright John Wiley & Sons - 2010.
 */

import java.util.Date;

public class Factory
{
	public static void main(String args[]) {
		Buffer<Date> server = new BoundedBuffer<Date>();
		
		// now create the producer and consumer threads
		Thread producerThread = new Thread(new Producer(server));
		Thread consumerThread = new Thread(new Consumer(server));
		
		producerThread.start();
		consumerThread.start();               
	}
}
