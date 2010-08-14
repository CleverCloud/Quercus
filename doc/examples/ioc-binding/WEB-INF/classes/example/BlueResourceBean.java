package example;

public class BlueResourceBean implements MyResource {
  private String _data = "initial";
  
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