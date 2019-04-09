import java.io.*;
import java.net.*;
import java.util.*;

public class TimeClient {

   public static void main(String[] args) {
      try {
         MulticastSocket so = new MulticastSocket(9999);
         InetAddress group = InetAddress.getByName("224.0.0.255");
         so.joinGroup(group);

         // Try 5 packets
         for (int a=0; a<5; a++) {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            so.receive(packet);

            String recv = new String(packet.getData());
            System.out.println("RECEIVED: " + recv);
         }

         so.leaveGroup(group);
         so.close();
      } catch (IOException ex) {
         ex.printStackTrace();
      } 
   }

}