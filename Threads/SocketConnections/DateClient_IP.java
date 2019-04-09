import java.net.*;
import java.io.*;
public class DateClient_IP
{
   public static void main(String[] args) {
      try {
         //make connection to server socket
         System.out.println("Host IP:" + args[0]);
         Socket sock = new Socket(args[0],6013);
         InputStream in = sock.getInputStream();
         BufferedReader bin = new
            BufferedReader(new InputStreamReader(in));
         // read the date from the socket
         String line;
         while ( (line = bin.readLine()) != null)
            System.out.println(line);
         // close the socket connection
         sock.close();
      }
      catch (IOException ioe) {
         System.err.println(ioe);
      }
   }
}
