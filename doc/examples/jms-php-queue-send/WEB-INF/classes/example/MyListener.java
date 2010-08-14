package example;

import javax.jms.*;
import javax.inject.Inject;

public class MyListener implements MessageListener {
  @Inject
  private MessageStore _messageStore;

  public void onMessage(Message message)
  {
    try {
      if (message instanceof TextMessage) {
	String text = ((TextMessage) message).getText();
	
	_messageStore.addMessage(text);
      }
      else if (message instanceof ObjectMessage) {
	Object value = ((ObjectMessage) message).getObject();
	
	_messageStore.addMessage(String.valueOf(value));
      }
      else
	_messageStore.addMessage(String.valueOf(message));
    } catch (JMSException e) {
      throw new RuntimeException(e);
    }
  }
}

