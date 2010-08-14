package example;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.resource.spi.work.Work;

/**
 * Implements the work task.  This task just loops until the resource stops.
 */
public class WorkTask implements Work {
  private static final Logger log =
    Logger.getLogger(WorkTask.class.getName());

  private TimerResource _resource;

  /**
   * Creates the work task.
   */
  WorkTask(TimerResource resource)
  {
    _resource = resource;
  }

  /**
   * The method called to execute the task, like Runnable
   */
  public void run()
  {
    log.fine("work starting");
      
    _resource.addCount();
  }

  /**
   * Resin will call the release() method when the server shuts down
   * to tell the task to close.
   */
  public void release()
  {
  }
}
