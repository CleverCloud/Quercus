package example;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.MessageListener;

public class MyListener implements MessageListener {
  private static final Logger log
    = Logger.getLogger(MyListener.class.getName());
  private static String _lastMessage;

  /**
   * Returns the last received message.
   */
  public static String getLastMessage()
  {
    return _lastMessage;
  }
  
  /**
   * Receives the message.
   */
  public void onMessage(Message message)
  {
    try {
      TextMessage objMessage = (TextMessage) message;

      log.info("received: " + objMessage.getText());

      _lastMessage = (String) objMessage.getText();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}
