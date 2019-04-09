public class TestThread3b extends Thread {
    private byte[] data;

    public TestThread3b() {
    }

    public void run() {
        data = new byte[512];
        for (int i = 0; i < 1000; i++) {
            SysLib.rawwrite(i, data);
            SysLib.rawread(i, data);
        }
        SysLib.cout("done disk\n");
        SysLib.exit();
    }
}