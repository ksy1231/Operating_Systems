/**
 * BoundedBuffer.java
 *
 * Figure 6.9
 *
 * This program implements the bounded buffer with semaphores.
 * Note that the use of count only serves to output whether
 * the buffer is empty of full.
 *
 * @author Gagne, Galvin, Silberschatz
 * Operating System Concepts with Java - Eighth Edition
 * Copyright John Wiley & Sons - 2010.
 */


import java.util.*;

@SuppressWarnings("unchecked")

public class BoundedBuffer<E> implements Buffer<E>
{
	
	private static final int   BUFFER_SIZE = 5;
	
	private Semaphore mutex;
	private Semaphore empty;
	private Semaphore full;
	
	private int count;
	private int in, out;
	private E[] buffer;
	
	public BoundedBuffer()
	{
		// buffer is initially empty
		count = 0;
		in = 0;
		out = 0;
		
		buffer = (E[]) new Object[BUFFER_SIZE];
		
		mutex = new Semaphore(1);
		empty = new Semaphore(BUFFER_SIZE);
		full = new Semaphore(0);
	}
	
	// producer calls this method
	public void insert(E item) {
		empty.acquire();
		mutex.acquire();
		
		// add an item to the buffer
		++count;
		buffer[in] = item;
		in = (in + 1) % BUFFER_SIZE;
		
		if (count == BUFFER_SIZE)
			System.out.println("Producer Entered " + item + " Buffer FULL");
        else
			System.out.println("Producer Entered " + item + " Buffer Size = " +  count);
		
		mutex.release();
		full.release();
	}
	
	// consumer calls this method
	public E remove() {
		full.acquire();
		mutex.acquire();
		
		// remove an item from the buffer
		--count;
		E item = buffer[out];
		out = (out + 1) % BUFFER_SIZE;
		
		if (count == 0)
			System.out.println("Consumer Consumed " + item + " Buffer EMPTY");
        else
			System.out.println("Consumer Consumed " + item + " Buffer Size = " + count);
		
		mutex.release();
		empty.release();
		
		return item;
	}
	
}
