import java.io.*;

public class MulticastServer {
    public static void main(String[] args) throws java.io.IOException {
        new MulticastServerThread(args[0]).start();
    }
}