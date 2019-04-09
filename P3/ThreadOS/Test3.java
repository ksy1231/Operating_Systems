import java.util.Date;

public class Test3 extends Thread {
    private final static String cpuTest = "TestThread3a";
    private final static String ioTest  = "TestThread3b";
    private int pairs;

    public Test3() {
        pairs = 1;
    }

    public Test3(String[] args) {
        pairs = Integer.parseInt(args[0]);
    }

    @Override
    public void run() {
        // start timer
        Date stop, start = new Date();
        int  tid;           // returning thread ID

        // create pairs of test threads
        for (int i = 0; i < pairs; ++i) {
            SysLib.exec(SysLib.stringToArgs(cpuTest));
            SysLib.exec(SysLib.stringToArgs(ioTest));
        }

        // wait on individual threads
        for (int i = 0; i < pairs * 2; ++i) {
            tid  = SysLib.join();
            stop = new Date();
        }

        // stop timer
        stop = new Date();

        SysLib.cout("\nelapsed time = " +
                (stop.getTime() - start.getTime()) + " msec\n");
        SysLib.exit();
    }
}
