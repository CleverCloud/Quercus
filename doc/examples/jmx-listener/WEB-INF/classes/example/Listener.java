package example;

import java.util.logging.Logger;

import javax.management.NotificationListener;
import javax.management.Notification;

/**
 * Implements an MBean event listener.
 */
public class Listener implements NotificationListener, ListenerMBean {
  private static final Logger log =
    Logger.getLogger(Listener.class.getName());
  /**
   * Count of the notifications received.
   */
  private int _notificationCount;

  /**
   * Returns the count of notifications received.
   */
  public int getNotificationCount()
  {
    return _notificationCount;
  }

  /**
   * Handles the notification.
   *
   * @param notif the notification sent by the event's MBean
   * @param handback an opaque object configured when the listener
   *   was configured.
   */
  public void handleNotification(Notification notif, Object handback)
  {
    _notificationCount++;
    
    if (handback != null)
      log.info("notification(type=" + notif.getType() + ",handback=" + handback + ")");
    else
      log.info("notification(type=" + notif.getType() + ")");
  }
  

  /**
   * Returns a printable version of the resource.
   */
  public String toString()
  {
    return "Listener[" + _notificationCount + "]";
  }
}
