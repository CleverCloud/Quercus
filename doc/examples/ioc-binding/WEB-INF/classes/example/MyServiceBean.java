package example;

import javax.inject.Singleton;
import javax.inject.Named;
import javax.enterprise.inject.Default;

@Singleton
@Named("myService")
@Default  
public class MyServiceBean implements MyService {
  private String _message = "initial message";
  
  public void setMessage(String message)
  {
    _message = message;
  }
  
  public String getMessage()
  {
    return _message;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _message + "]";
  }
}
