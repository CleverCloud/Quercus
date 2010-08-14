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

  private WorkResource _resource;

  // main lifecycle variable
  private volatile boolean _isActive = true;

  /**
   * Creates the work task.
   */
  WorkTask(WorkResource resource)
  {
    _resource = resource;
  }

  /**
   * The method called to execute the task, like Runnable
   */
  public void run()
  {
    log.fine("work starting");
      
    while (_isActive) {
      log.fine("work adding count");
      
      _resource.addCount();

      try {
	synchronized (this) {
	  wait(_resource.getSleepTime());
	}
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
    
    log.fine("work complete");
  }

  /**
   * Resin will call the release() method when the server shuts down
   * to tell the task to close.
   */
  public void release()
  {
    _isActive = false;
    
    synchronized (this) {
      notifyAll();
    }
  }
}
