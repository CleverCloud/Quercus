package example;

/**
 * Management interface for the basic MBean.
 */
public interface BasicMBean {
  /**
   * Gets the data value.
   */
  public String getData();
  
  /**
   * Sets the value.
   */
  public void setData(String data);
}
