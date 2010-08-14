package example;

import javax.inject.Singleton;

@Singleton
public class StartupResourceBean implements MyResource {
  private String _data = "default";
  
  /**
   * Configures the resource with a data string.
   */
  public void setData(String data)
  {
    _data = data;
  }
  
  public String getMessage()
  {
    return getClass().getSimpleName() + ": " + _data;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _data + "]";
  }
}
