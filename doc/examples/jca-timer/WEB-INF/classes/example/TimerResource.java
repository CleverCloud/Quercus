package example;

import java.util.Timer;
import java.util.TimerTask;

import java.util.logging.Logger;

import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.BootstrapContext;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkException;

import com.caucho.jca.AbstractResourceAdapter;

import com.caucho.config.types.Period;

/**
 * Implements a resource which uses uses Work management for
 * separate threading.
 */
public class TimerResource extends AbstractResourceAdapter {
  private static final Logger log =
    Logger.getLogger(TimerResource.class.getName());

  // The initial delay of the task.
  private long _initialDelay = 0;
  
  // The period of the task
  private long _period = 10000L;
  
  // The count of times the server has looped
  private int _count;

  private Timer _timer;

  /**
   * Sets the initial delay using the Resin-specific period notation:
   * 10s, 10m, etc.
   */
  public void setInitialDelay(Period initialDelay)
  {
    _initialDelay = initialDelay.getPeriod();
  }

  /**
   * Sets the period using the Resin-specific period notation:
   * 10s, 10m, etc.
   */
  public void setPeriod(Period period)
  {
    _period = period.getPeriod();
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
    try {
      log.info("WorkResource[] starting");

      WorkManager workManager = ctx.getWorkManager();
    
      Work work = new WorkTask(this);

      TimerTask timerTask = new WorkScheduleTimerTask(workManager, work);

      _timer = ctx.createTimer();

      _timer.schedule(timerTask, _initialDelay, _period);
    } catch (Exception e) {
      throw new ResourceAdapterInternalException(e);
    }
  }
  
  /**
   * Called when the resource adapter is stopped, i.e. when the
   * web-app or host closes down.
   */
  public void stop()
  {
    log.info("Resource[" + _count + "] stopping");

    _timer.cancel();
  }

  /**
   * Returns a printable version of the resource.
   */
  public String toString()
  {
    return "WorkResource[" + _count + "]";
  }
}
