package example;

/**
 * Implements a resource which is a plain-old bean, which exposes
 * the <code>getData()</code> method as a JMX-managed attribute.
 */
public class Test implements TestAdmin {
  /**
   * The sample data param.
   */
  private String _data = "default";

  /**
   * Sets the value.
   */
  public void setData(String data)
  {
    _data = data;
  }

  /**
   * Gets the value.
   */
  public String getData()
  {
    return _data;
  }

  /**
   * Returns a printable version of the resource.
   */
  public String toString()
  {
    return "Test[" + _data + "]";
  }
}
