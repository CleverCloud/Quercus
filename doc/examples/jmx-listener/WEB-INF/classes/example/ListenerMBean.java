package example;

/**
 * Interface for the ListenerMBean.
 */
public interface ListenerMBean {
  /**
   * Returns the count of notifications received.
   */
  public int getNotificationCount();
}
