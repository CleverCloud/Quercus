package example;

import javax.management.ObjectName;

/**
 * Management interface for the basic MBean.
 */
public interface TestMBean {
  /**
   * Gets the object name.
   */
  public ObjectName getObjectName();
}
