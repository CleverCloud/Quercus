package example;

import javax.management.NotificationBroadcasterSupport;
import javax.management.Notification;

/**
 * Implements an MBean which sends notifications.
 */
public class Emitter extends NotificationBroadcasterSupport
  implements EmitterMBean {
  private long _sequence;
  
  /**
   * Sends a notification.
   */
  public void send()
  {
    Notification notif;

    notif = new Notification("example.send", this, _sequence++);

    sendNotification(notif);
  }
}
