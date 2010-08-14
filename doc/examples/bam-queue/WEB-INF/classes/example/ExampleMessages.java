package example;

import java.util.ArrayList;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.enterprise.inject.Default;

@Named
@Default  
@Singleton
public class ExampleMessages
{
  private ArrayList<String> _messages = new ArrayList<String>();
  
  public void addMessage(String text)
  {
    if (_messages.size() > 10)
      _messages.remove(0);
    
    _messages.add(text);
  }

  public ArrayList<String> getMessages()
  {
    return _messages;
  }
}
