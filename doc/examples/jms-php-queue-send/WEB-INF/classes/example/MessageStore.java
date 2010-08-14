package example;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.enterprise.inject.Default;

/**
 * Custom singleton bean to store messages
 */
@Named  
@Default
@Singleton
public class MessageStore {
  private LinkedList<String> _messageLog = new LinkedList<String>();

  public void addMessage(String message)
  {
    synchronized (_messageLog) {
      if (_messageLog.size() > 10)
	_messageLog.remove(0);

      _messageLog.add(message);
    }
  }

  public ArrayList<String> getMessages()
  {
    synchronized (_messageLog) {
      return new ArrayList<String>(_messageLog);
    }
  }
}

