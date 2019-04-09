import java.io.*;
import java.net.*;
import java.util.*;

public class MulticastServerThread extends QuoteServerThread {

    private long FIVE_SECONDS = 5000;
    String strHostIP;

    public MulticastServerThread(String host_ip_addr) throws IOException {
        super("com.css430.MulticastServerThread");
        strHostIP = host_ip_addr;
        System.out.println("Starting Multicast Server Host IP:" + strHostIP);
    }

    public void run() {
        while (moreQuotes) {
            try {
                byte[] buf = new byte[256];

                // construct quote
                String dString = null;
                if (in == null)
                    dString = new Date().toString();
                else
                    dString = getNextQuote();
                buf = dString.getBytes();

                // send it
                System.out.println("Trying: " + strHostIP);
                InetAddress group = InetAddress.getByName(strHostIP);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 9999);
                socket.send(packet);

                // sleep for a while
                try {
                    sleep((long)(Math.random() * FIVE_SECONDS));
                } catch (InterruptedException e) { }
            } catch (IOException e) {
                e.printStackTrace();
                moreQuotes = false;
            }
        }
        socket.close();
    }
}