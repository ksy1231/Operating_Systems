import java.io.*;
import java.net.*;
import java.util.*;

public class TimeServer {

   public static void main(String[] args) {
      try {
         final DatagramSocket so = new DatagramSocket(9999);
         new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  while (true) {
                     try {
                        InetAddress group = InetAddress.getByName("224.0.0.255");
                        byte[] buf = new byte[256];
                        Calendar cal = Calendar.getInstance();
                        buf = cal.toString().getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length,
                                group, 9998);
                        so.send(packet);
                     } catch (IOException ex) {
                        ex.printStackTrace();
                     }
                     Thread.sleep((int)(Math.random() * 5));
                  }
               } catch (InterruptedException ex) {
                  ex.printStackTrace();
               }
            }
         }).start();
      } catch (IOException ex) {
         ex.printStackTrace();
      }
   }
}