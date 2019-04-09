package edu.uwb.css;

import java.util.Random;

public class Main {

    //First, we have several methods that are useful for debugging
    // Each of these will print <whatever>, and include the name
    // of the thread that's doing the printing.
    // It's an easy way to generate a log of which thread is doing what

    private static boolean DEBUG = false;
    // Print the message and the thread that's executing this code
    // This is used for debugging purposes
    public static void TPrint(String msg) {
        if( DEBUG )
            System.out.println(Thread.currentThread().getName()+": " +msg);
    }
    public static void TPrint(DiningState[] state) {
        if (DEBUG)
            for( int i = 0; i < state.length;i++)
                TPrint("Slot " + i + " is: " + state[i]);
    }
    public static void TPrint(DiningPhilosophers dining) {
        if (DEBUG)
            for( int i = 0; i < dining.numPhilosophers();i++)
                TPrint("Slot " + i + " is: " + dining.getDiningState(i));
    }

    // Occasionally a test thread will want to exit before it gets
    // to the 'passed/failed' part of the code.  We call this
    // mostly so that the compiler only complains once about us
    // using the deprecated 'stop()' method :)
    public static void TExit() { Thread.currentThread().stop(); }

    public static void main(String[] args) {

        Main m = new Main();
        m.RunTests();
    }

    // In retrospect it probably would have been faster to just write this all using JUnit
    //      upside: it's probably easier for students to understand with JUnit ( Hi, Students! :) )
    //
    // The basic plan is this:
    // You (the student) starts a thread to execute main, which ends up here
    // because these tests might deadlock (because of bugs) we need to run the tests on separate
    //  threads.  Thus the 'main' thread launches a 'test' thread, one for each test
    public void RunTests() {
        for( int iTest = 0; iTest < TestThread.NUM_TESTS; iTest++) {

            System.out.println("======== Starting Test " + iTest + " ========");

            TestThread runnable = new TestThread(iTest, new StateMachine(0));
            Thread t = new Thread(runnable);
            t.start(); // run the test here, on it's own thread
            try {
                t.join(10000); // give it 5 seconds, then move on :)
                //t.join(); // this one has no time limit.  Use this if you're running the tests under the debugger :)
                if( t.getState() != Thread.State.TERMINATED )
                    System.out.println("Test " + iTest + " ran out of time (test timed out)");
            }
            catch( InterruptedException ie) {
                System.out.println("Test " + iTest + " failed with InterruptedException: " + ie.getMessage());
            }
            finally {
                Main.TPrint( " === Test " + iTest + " has finished === " );
            }
        }
    }

    /**
     * If you need to, now is a great time to search the Web for 'state machine diagram'
     *  (and/or 'state machine')
     *
     * We'll represent a State with an int
     * This has a 'spin lock' method that spins until a state is reached.
     * We use this for simple coordination amongst threads that need to take turns.
     */
    private class StateMachine {

        public StateMachine(int startingState) {
            stateMachineCurrentState = startingState;
        }

        // Set to public so a thread can just set the state to whatever it wants
        volatile public int stateMachineCurrentState;

        // This will wait until a state is set, then Thread.sleep() for 1 more second,and then finally return.
        // Waiting for a second is a hack, in an attempt to let the other thread set the state (and then block)
        // and then have this thread react to it.
        // This approach isn't bulletproof, but since it's test code only we (the developers) should see
        // it.  So if we get a false negative on a test we can investigate it without the
        // customer ever seeing it.
        private void SpinLock(int untilThisState) {

            Main.TPrint("About to spinlock for state " + untilThisState);
            while( stateMachineCurrentState != untilThisState)
                ; // empty while loop ON PURPOSE!!!  Haha, multithreading is fun!

            try {
                Thread.sleep(1000); // this is a terrible, terrible attempt to avoid a race condition :)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /** Both student-visible test code and instructor-visible test code derive from this class
     * This exists mostly to define instance variables that both classes can use
    */
    private class TestThreadBase  {
        protected StateMachine spinner;

        protected DiningPhilosophers dining;

        protected int whichTest;
        protected int whichThread; // some tests have multiple threads
        public boolean testPassed = false;

        public TestThreadBase(int whichTest, StateMachine spinLock) {
            this(whichTest, 0, spinLock, null);
        }

        public TestThreadBase(int whichTest, int whichThread, StateMachine spinLock, DiningPhilosophers diners) {
            this.whichTest = whichTest;
            this.whichThread = whichThread;
            this.spinner = spinLock;
            if (diners != null)
                this.dining = diners;
            else
                this.dining = new DiningPhilosophers(5);
        }
    }

    private class TestThread extends TestThreadBase implements Runnable {
        // Put this here temporarily since it's near where I add the tests
        // if it's still here it's because I forgot to move it to the top of the class :)
        public static final int NUM_TESTS = 9;

        public TestThread(int whichTest, StateMachine spinLock) {
            this(whichTest, 0, spinLock, null);
        }

        public TestThread(int whichTest, int whichThread, StateMachine spinLock, DiningPhilosophers diners)
        {
            super(whichTest, whichThread, spinLock, diners);
        }

        /** Given an array of DiningState values this will double-check that
         *      1) the length of that array is the same as the number of philosopers, and
         *      2) the contents of the array are the same as the states of each of the philosophers.
         * @param correctState
         * @return
         */
        public boolean checkState(DiningState[]correctState )  {
            if( correctState.length != dining.numPhilosophers() ) {
                System.out.println("checkState given " + correctState.length + " element array, but there are" +
                        ", in fact, " + dining.numPhilosophers() + " philosophers at the table");
                System.exit(-1);
            }

            boolean result = true;
            for(int i = 0 ; i < correctState.length; i++) {
                if( correctState[i] != dining.getDiningState(i)) {
                    Main.TPrint("checkState: ERROR: Philosopher " + i + " should be in state " + correctState[i] +
                            " but is actually in state: " + dining.getDiningState(i) );
                    result = false;
                }
            }
            if( !result ) {
                Main.TPrint("checkState: Correct state:");
                Main.TPrint(correctState);
                Main.TPrint("checkState: actual state:");
                Main.TPrint(dining);
            }

            return result;
        }

        @Override
        public void run()  {
            DiningState[] correctState = null;
            boolean thisPartPassed;
            final int THREAD_EXIT_STATE = 10000;

            switch(whichTest) {
                // first, some single-threaded tests:

                // If any of these hang then we'll know that there's a problem with the solution

                // If they don't hang then it means that either things are looking good
                // or else the solution doesn't wait/block even when it should :)

                ///////////// THESE TESTS ARE SINGLE-THREADED TESTS ///////////////////
                // Single takeForks
                case 0:
                    dining.takeForks(0);
                    testPassed = checkState(new DiningState[]{
                            DiningState.EATING,
                            DiningState.THINKING, DiningState.THINKING, DiningState.THINKING, DiningState.THINKING});
                    break;

                // 4 philosophers, takeForks on opposite sides
                case 1:
                    dining = new DiningPhilosophers(4);
                    dining.takeForks(0);
                    dining.takeForks(2);
                    testPassed = checkState(new DiningState[]{
                            DiningState.EATING,
                            DiningState.THINKING,
                            DiningState.EATING,
                            DiningState.THINKING});
                    break;

                // 6 philosophers, takeForks every other
                case 2:
                    dining = new DiningPhilosophers(6);
                    dining.takeForks(0);
                    dining.takeForks(2);
                    dining.takeForks(4);
                    testPassed = checkState(new DiningState[]{
                            DiningState.EATING,
                            DiningState.THINKING,
                            DiningState.EATING,
                            DiningState.THINKING,
                            DiningState.EATING,
                            DiningState.THINKING});
                    break;
                case 3: // alternates on the other side:
                    dining = new DiningPhilosophers(6);
                    dining.takeForks(1);
                    dining.takeForks(3);
                    dining.takeForks(5);
                    testPassed = checkState(new DiningState[]{
                            DiningState.THINKING,
                            DiningState.EATING,
                            DiningState.THINKING,
                            DiningState.EATING,
                            DiningState.THINKING,
                            DiningState.EATING});
                    break;

                // 3 phil, call takeForks then returnForks on #0
                case 4:
                    dining.takeForks(1);
                    thisPartPassed = checkState(new DiningState[]{
                            DiningState.THINKING,
                            DiningState.EATING,
                            DiningState.THINKING,
                            DiningState.THINKING,
                            DiningState.THINKING});
                    if (!thisPartPassed) break;

                    dining.returnForks(1);
                    testPassed = checkState(new DiningState[]{
                            DiningState.THINKING,
                            DiningState.THINKING,
                            DiningState.THINKING,
                            DiningState.THINKING,
                            DiningState.THINKING});
                    break;

                // call takeForks then returnForks on each one
                case 5:
                    for (int i = 0; i < dining.numPhilosophers(); i++) {
                        correctState = new DiningState[]{
                                DiningState.THINKING,
                                DiningState.THINKING,
                                DiningState.THINKING,
                                DiningState.THINKING,
                                DiningState.THINKING};

                        thisPartPassed = checkState(correctState);
                        if (!thisPartPassed) break;

                        dining.takeForks(i);
                        correctState[i] = DiningState.EATING;
                        thisPartPassed = checkState(correctState);
                        if (!thisPartPassed) break;

                        dining.returnForks(i);
                        correctState[i] = DiningState.THINKING;
                        testPassed = checkState(correctState);
                        if (!testPassed) break;
                    }
                    break;

                // call takeForks then returnForks on each one several times
                case 6:
                    LOuterLoop:
                    for (int i = 0; i < dining.numPhilosophers(); i++) {
                        correctState = new DiningState[]{
                                DiningState.THINKING,
                                DiningState.THINKING,
                                DiningState.THINKING,
                                DiningState.THINKING,
                                DiningState.THINKING};

                        for (int numTimes = 0; numTimes < 3; numTimes++) {
                            thisPartPassed = checkState(correctState);
                            if (!thisPartPassed) break LOuterLoop;

                            dining.takeForks(i);
                            correctState[i] = DiningState.EATING;
                            thisPartPassed = checkState(correctState);
                            if (!thisPartPassed) break LOuterLoop;

                            dining.returnForks(i);
                            correctState[i] = DiningState.THINKING;
                            thisPartPassed = checkState(correctState);
                            if (!thisPartPassed) break LOuterLoop;
                        }
                        testPassed = true;
                    }
                    break;

                ///////////// THESE TESTS ARE MULTI-THREADED TESTS ///////////////////
                // Note that for these tests, the first test thread will start
                //  several other test threads (using the 'whichThread' parameter to the constructor)
                //
                // Note that all the test threads should share a single StateMachine object :)
                //
                // The join() back in RunTests will wait for the
                //  first test thread to exit and when it does join() will return.
                //  Thus the first test thread may need to wait for all of it's test "sub-threads" to
                //  finish first.
                //
                //
                // Use the 'Main.TPrint' so that any messages will clearly identify which thread printed it
                //
                // First test:
                // Check that the other thread will (correctly) block when it's neighbor is EATING
                case 7:
                    switch (whichThread) {
                        case 0:
                            // main thread: start the other thread
                            dining.takeForks(0); // grab a fork before there's any contention :)

                            TestThread otherThreadsRunnable = new TestThread(whichTest, 1, this.spinner, this.dining);
                            Thread otherThread = new Thread(otherThreadsRunnable);
                            otherThread.start();

                            this.spinner.SpinLock(1); // wait till the other thread is up and running
                            // we're using it since it typically works (even if it's not guaranteed)
                            // "it's only test code" is the rationale - the 'customer' will never see it
                            correctState = new DiningState[]{
                                    DiningState.EATING,
                                    DiningState.HUNGRY,
                                    DiningState.THINKING,
                                    DiningState.THINKING,
                                    DiningState.THINKING};
                            thisPartPassed = checkState(correctState);
                            if (!thisPartPassed) {
                                System.out.println("ERROR: Test " + whichTest + ": Incorrect state after state 1");
                            }
                            Main.TPrint("thread 0 is EATING, thread 1 is HUNGRY : ");

                            dining.returnForks(0); // this should cause philsopher 1 to resume
                            this.spinner.stateMachineCurrentState = 2;
                            Main.TPrint("finished return forks, about to exit: ");

                            this.spinner.SpinLock(THREAD_EXIT_STATE); // if this main thread exits the test will be listed as failing
                            // so wait for the designated 'ending state' before exiting

                            Main.TPrint("finished exit state spinlock: ");

                            // At this point the other thread will determine if the test has passed or not
                            this.testPassed = otherThreadsRunnable.testPassed;
                            break;

                        case 1:
                            // second thread
                            this.spinner.stateMachineCurrentState = 1; // tell the other thread that we're running
                            dining.takeForks(1); // this should block

                            Main.TPrint("thread 1 - returned from takeForks: ");

                            // wait for the other thread to check stuff
                            this.spinner.SpinLock(2);

                            Main.TPrint("thread 1 - finished spinlock: ");

                            // first thread should have gone back to thinking, we're now eating
                            correctState = new DiningState[]{
                                    DiningState.THINKING,
                                    DiningState.EATING,
                                    DiningState.THINKING,
                                    DiningState.THINKING,
                                    DiningState.THINKING};
                            testPassed = checkState(correctState);
                            if (!testPassed) {
                                Main.TPrint("ERROR: Test " + whichTest + ": Incorrect state after state 2");
                            }
                            Main.TPrint("thread 1 is EATING, all other threads THINKING : ");

                            this.spinner.stateMachineCurrentState = THREAD_EXIT_STATE;
                            Main.TExit(); // we don't want to finish this function
                            // (so we don't accidentally print a test passed/failed message, below
                            // This is a hack, but it's probably ok
                            break;

                    }
                    break;
                // Create several threads - have them randomly eat, wait, then eat again
                case 8:
                    int NUM_PHILOSOPHER_THREADS = 5;
                    int NUM_TIMES_TO_EAT = 10;
                    dining = new DiningPhilosophers(NUM_PHILOSOPHER_THREADS);
                    TestThread[]finished = new TestThread[NUM_PHILOSOPHER_THREADS+1];
                    switch (whichThread) {
                        case 0:
                            // for this test the main thread will do nothing except kick off the other
                            // test threads
                            for (int iTestThread = 1; iTestThread <= NUM_PHILOSOPHER_THREADS; iTestThread++) {
                                Main.TPrint("Starting test sub-thread " + iTestThread);

                                finished[iTestThread] = new TestThread(whichTest, iTestThread, this.spinner, this.dining);
                                Thread t = new Thread(finished[iTestThread]);
                                t.start();
                            }
                            try {
                                Thread.sleep(7000);
                            } catch (InterruptedException ie) {
                                Main.TPrint("ERROR: InterruptedException in test " + whichTest);
                                Main.TExit();
                            }

                            Main.TPrint("IF all " + NUM_PHILOSOPHER_THREADS + " philosopher threads printed out a " +
                                    "\"LOOKING GOOD\" message then this test has passed.");

                            boolean allFinished = true;
                            for(int i = 1; i < NUM_PHILOSOPHER_THREADS+1; i++) {
                                // slot zero is empty (null), so ignore it.
                                if( finished[i].testPassed == false) {
                                    allFinished = false;
                                    break;
                                }
                            }
                            this.testPassed = allFinished;
                            break;

                        // all the test threads:
                        default:
                            Random r = new Random();
                            int forkNum = whichThread - 1;
                            for (int i = 0; i < NUM_TIMES_TO_EAT; i++) {
                                Main.TPrint("About to take fork " + whichThread);
                                dining.takeForks(forkNum);
                                try {
                                    Main.TPrint("Got fork" + forkNum + " for thread " + whichThread + "!  About to delay! ");
                                    Thread.sleep(r.nextInt(500)); // random delay, less than half a second
                                    Main.TPrint("Still got fork" + forkNum + "!  Delay finished! ");
                                } catch (InterruptedException ie) {
                                    Main.TPrint("ERROR: InterruptedException in test " + whichTest);
                                    Main.TExit();
                                }
                                dining.returnForks(forkNum);
                                Main.TPrint("Returned fork " + forkNum);
                            }

                            this.testPassed = true;
                            Main.TPrint(" LOOKING GOOD: This thread executed all it's takeForks/returnForks " +
                                    "without deadlocking\nIt's possible that other threads have run into trouble, " +
                                    "but this thread seems fine");
                            Main.TExit();
                            break;
                    }
                    break;
            }

            if( testPassed )
                System.out.println("TEST " + whichTest + "  PASSED");
            else
                System.out.println("TEST " + whichTest + "  FAILED");
        }
    }
}
