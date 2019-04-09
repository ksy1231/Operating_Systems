import java.io.*;
import java.net.*;
import java.util.*;

public class QuoteClient {
   public static void main(String[] args) throws IOException {

      DatagramPacket packet;
      InetAddress address;

      if (args.length != 1) {
         System.out.println("Usage: java QuoteClient <hostname>");
         return;
      }

      // get a datagram socket
      DatagramSocket socket = new DatagramSocket();

      // send request
      byte[] buf = new byte[256];
      try {
         address = InetAddress.getByName(args[0]);
         packet = new DatagramPacket(buf, buf.length, address, 4445);
         socket.send(packet);
         System.out.println("Send Packet Request to: " + address);
      } catch (FileNotFoundException e) {
         System.err.println("Error in sending packet request");
      }

      // get response
      packet = new DatagramPacket(buf, buf.length);
      socket.receive(packet);

      // display response
      String received = new String(packet.getData(), 0, packet.getLength());
      System.out.println("Quote of the Moment: " + received);

      socket.close();
   }
}
