import java.util.Date;

public class Test
{
   public static void main(String[] args) {
      Channel<Date> mailBox = new MessageQueue<Date>();
      mailBox.send(new Date());

      Date rightNow = mailBox.receive();
      System.out.println(rightNow);
   }
}
