import java.util.*;

public class Scheduler extends Thread {
    private static final int DEFAULT_TIME_SLICE = 1000;
    private static final int DEFAULT_MAX_THREADS = 10000;
    private Vector queue0, queue1, queue2;
    private int timeSlice;
    // New data added to p161
    private boolean[] tids; // Indicate which ids have been used
    // A new feature added to p161
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;

    public Scheduler() {
        timeSlice = DEFAULT_TIME_SLICE;
        queue0 = new Vector();
        queue1 = new Vector();
        queue2 = new Vector();
        initTid(DEFAULT_MAX_THREADS);
    }

    public Scheduler(int quantum) {
        timeSlice = quantum;
        queue0 = new Vector();
        queue1 = new Vector();
        queue2 = new Vector();
        initTid(DEFAULT_MAX_THREADS);
    }

    // A new feature added to p161
    // A constructor to receive the max number of threads to be spawned
    public Scheduler(int quantum, int maxThreads) {
        timeSlice = quantum;
        queue0 = new Vector();
        queue1 = new Vector();
        queue2 = new Vector();
        initTid(maxThreads);
    }

    private void initTid(int maxThreads) {
        tids = new boolean[maxThreads];
        for (int i = 0; i < maxThreads; i++)
            tids[i] = false;
    }

    // A new feature added to p161
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid() {
        for (int i = 0; i < tids.length; i++) {
            int tentative = (nextId + i) % tids.length;
            if (tids[tentative] == false) {
                tids[tentative] = true;
                nextId = (tentative + 1) % tids.length;
                return tentative;
            }
        }
        return -1;
    }

    // A new feature added to p161
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid(int tid) {
        if (tid >= 0 && tid < tids.length && tids[tid] == true) {
            tids[tid] = false;
            return true;
        }
        return false;
    }

    // A new feature added to p161
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb() {
        Thread myThread = Thread.currentThread(); // Get my thread object
        synchronized (queue0) {                        //Look for TCB in queue0
            for (int i = 0; i < queue0.size(); i++) {
                TCB tcb = (TCB) queue0.elementAt(i); //Grab a TCB object
                Thread thread = tcb.getThread();       //Grab the TCB's thread
                if (thread == myThread) return tcb;   //Check if its the one
            }
        }
        synchronized (queue1) {
            for (int i = 0; i < queue1.size(); i++) {
                TCB tcb = (TCB) queue1.elementAt(i);
                Thread thread = tcb.getThread();
                if (thread == myThread) return tcb;
            }
        }
        synchronized (queue2) {
            for (int i = 0; i < queue2.size(); i++) {
                TCB tcb = (TCB) queue2.elementAt(i);
                Thread thread = tcb.getThread();
                if (thread == myThread) return tcb;
            }
        }
        return null;
    }

    // A new feature added to p161
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads() {
        return tids.length;
    }

    private void schedulerSleep() {
        try {
            Thread.sleep(timeSlice);
        } catch (InterruptedException e) {
        }
    }

    // A modified addThread of p161 example
    public TCB addThread(Thread t) {
        //t.setPriority( 2 ); //remove part 1
        TCB parentTcb = getMyTcb(); // get my TCB and find my TID
        int pid = (parentTcb != null) ? parentTcb.getTid() : -1;
        int tid = getNewTid(); // get a new TID
        if (tid == -1)
            return null;
        TCB tcb = new TCB(t, tid, pid); // create a new TCB
        queue0.add(tcb);
        return tcb;
    }

    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread() {
        TCB tcb = getMyTcb();
        if (tcb != null)
            return tcb.setTerminated();
        else
            return false;
    }

    public void sleepThread(int milliseconds) {
        try {
            sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }

    public boolean queue0IsEmpty() {
        return (queue0.size() == 0);
    }

    public boolean queue1IsEmpty() {
        return (queue1.size() == 0);
    }

    public boolean queue2IsEmpty() {
        return (queue2.size() == 0);
    }

    public boolean queue0HasThreads() {
        return (queue0.size() > 0);
    }

    public boolean queue1HasThreads() {
        return (queue1.size() > 0);
    }

    public boolean queue2HasThreads() {
        return (queue2.size() > 0);
    }

    public void processQueue0(Thread current) {
        TCB currentTCB = (TCB) queue0.firstElement();
        if (checkTerminated(currentTCB, queue0)) return;

        current = currentTCB.getThread();
        runThread(current);

        sleepThread(timeSlice / 2);

        moveThread(current, currentTCB, queue0, queue1);
    }

    public void processQueue1(Thread current) {
        TCB currentTCB = (TCB) queue1.firstElement();
        if (checkTerminated(currentTCB, queue1)) return;

        current = currentTCB.getThread();
        runThread(current);

        sleepThread(timeSlice / 2);
        checkQueue0(current);
        sleepThread(timeSlice / 2);

        moveThread(current, currentTCB, queue1, queue2);
    }

    public void processQueue2(Thread current) {
        TCB currentTCB = (TCB) queue2.firstElement();
        if (checkTerminated(currentTCB, queue2)) return;

        current = currentTCB.getThread();
        runThread(current);

        sleepThread(timeSlice / 2);
        checkQueue0(current);
        checkQueue1(current);
        sleepThread(timeSlice / 2);
        checkQueue0(current);
        checkQueue1(current);
        sleepThread(timeSlice / 2);
        checkQueue0(current);
        checkQueue1(current);
        sleepThread(timeSlice / 2);

        moveThread(current, currentTCB, queue2, queue2);
    }

    public void checkQueue0(Thread current) {
        current.suspend();
        while (queue0HasThreads()) {
            Thread newThread = null;
            processQueue0(newThread);
        }
        current.resume();
    }

    public void checkQueue1(Thread current) {
        current.suspend();
        while (queue1HasThreads()) {
            Thread newThread = null;
            processQueue1(newThread);
        }
        current.resume();
    }

    public boolean checkTerminated(TCB currentTCB, Vector queue) {
        if (currentTCB.getTerminated() == true) {
            queue.remove(currentTCB);
            returnTid(currentTCB.getTid());
            return true;
        }
        return false;
    }

    public void runThread(Thread current) {
        if (current == null) {
            return;
        }
        if (current.isAlive()) {
            current.resume();
        } else {
            current.start();

        }
    }

    public void moveThread(Thread current, TCB currentTCB, Vector from,
                           Vector to) {
        synchronized (from) {
            current.suspend();
            from.remove(currentTCB); // rotate this TCB to the end
            to.add(currentTCB);
        }
    }

    public void run() {
        Thread current = null;

        while (true) {
            try {
                if (queue0IsEmpty() && queue1IsEmpty() && queue2IsEmpty()) {
                    continue;
                }
                if (queue0HasThreads()) {
                    processQueue0(current);
                    continue;
                }
                if (queue0IsEmpty() && queue1HasThreads()) {
                    processQueue1(current);
                    continue;
                }
                if (queue0IsEmpty() && queue1IsEmpty() && queue2HasThreads()) {
                    processQueue2(current);
                    continue;
                }
            } catch (NullPointerException e) {
                System.err.println("ERROR");
            };
        }
    }
}
