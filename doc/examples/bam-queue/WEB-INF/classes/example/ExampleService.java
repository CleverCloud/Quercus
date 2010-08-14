package example;

import com.caucho.bam.SimpleActor;
import com.caucho.bam.Message;

import java.util.ArrayList;
import java.util.Date;

import javax.inject.Inject;

public class ExampleService extends SimpleActor
{
  @Inject
  private ExampleMessages _messages;
  
  @Message
  public void message(String to, String from, ExampleMessage msg)
  {
    String body = msg.getBody();
    String text = body + " [from=" + from + " at " + new Date() + "]";
    
    _messages.addMessage(text);
  }
  
  @Message
  public void message(String to, String from, String body)
  {
    String text = body + " [from=" + from + " at " + new Date() + "]";
    
    _messages.addMessage(text);
  }
}
