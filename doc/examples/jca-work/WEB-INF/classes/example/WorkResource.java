package example;

import java.util.logging.Logger;

import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.BootstrapContext;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkException;

import com.caucho.jca.AbstractResourceAdapter;

/**
 * Implements a resource which uses uses Work management for
 * separate threading.
 */
public class WorkResource extends AbstractResourceAdapter {
  private static final Logger log =
    Logger.getLogger(WorkResource.class.getName());

  // The time in milliseconds the resource should sleep
  private long _sleepTime = 10000L;

  // The count of times the server has looped
  private int _count;

  /**
   * Returns the sleep time.
   */
  public long getSleepTime()
  {
    return _sleepTime;
  }
  
  /**
   * Adds to the count.
   */
  public void addCount()
  {
    _count++;
  }
  
  /**
   * The start method is called when the resource adapter starts, i.e.
   * when the web-app or host initializes.
   */
  public void start(BootstrapContext ctx)
    throws ResourceAdapterInternalException
  {
    log.info("WorkResource[] starting");

    WorkManager workManager = ctx.getWorkManager();
    
    Work work = new WorkTask(this);

    try {
      // Submits the work, but does not wait for the result.
      // In other words, it spawns a new thread
      workManager.startWork(work);
    } catch (WorkException e) {
      throw new ResourceAdapterInternalException(e);
    }
  }
  
  /**
   * Called when the resource adapter is stopped, i.e. when the
   * web-app or host closes down.
   */
  public void stop()
    throws ResourceAdapterInternalException
  {
    log.info("Resource[" + _count + "] stopping");
  }

  /**
   * Returns a printable version of the resource.
   */
  public String toString()
  {
    return "WorkResource[" + _count + "]";
  }
}
