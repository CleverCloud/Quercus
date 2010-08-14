package example;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PeriodicTask
 implements PeriodicTaskMBean, javax.resource.spi.work.Work
{
  static protected final Logger log
    = Logger.getLogger(PeriodicTask.class.getName());

  private long _estimatedAverageTime = 5000;

  private boolean _isActive = false;
  private long _lastActiveTime = -1;
  private long _totalActiveCount = 0;
  private long _totalActiveTime = 0;


  public PeriodicTask()
  {
  }

  /**
   * {@inheritDoc}
   */
  public void setEstimatedAverageTime(long estimatedAverageTime)
  {
    _estimatedAverageTime = estimatedAverageTime;
  }

  /**
   * {@inheritDoc}
   */
  public long getEstimatedAverageTime()
  {
    return _estimatedAverageTime;
  }

  public void init()
    throws Exception
  {
  }

  /**
   * {@inheritDoc}
   */
  public boolean isActive()
  {
    synchronized (this) {
      return _isActive == true;
    }
  }

  /**
   * {@inheritDoc}
   */
  public long getEstimatedTimeRemaining()
  {
    synchronized (this) {
      if (_isActive) {
        long now = System.currentTimeMillis();
        long activeTime = now - _lastActiveTime;
        long estimate = getAverageActiveTime() - activeTime;
        if (estimate < 0)
          return 1000;
        else
          return estimate;
      }
      else
        return 0;
    }
  }

  /**
   * {@inheritDoc}
   */
  public long getLastActiveTime()
  {
    return _lastActiveTime;
  }

  /**
   * {@inheritDoc}
   */
  public long getTotalActiveCount()
  {
    return _totalActiveCount;
  }

  /**
   * {@inheritDoc}
   */
  public long getTotalActiveTime()
  {
    return _totalActiveTime;
  }

  /**
   * {@inheritDoc}
   */
  public long getAverageActiveTime()
  {
    synchronized (this) {
      long count = _isActive ? _totalActiveCount -1 : _totalActiveCount;
      if (count < 1)
        return _estimatedAverageTime;
      else
        return _totalActiveTime / count;
    }
  }

  /**
   * {@inheritDoc}
   *
   * Various statistical information is collected and maintained by this method,
   * the actual task is performed by the performTask() method. 
   */
  public void run()
  {
    synchronized (this) {
      if (_isActive == true)
        return;
      _isActive = true;

      _lastActiveTime = System.currentTimeMillis();
      _totalActiveCount++;
    }

    try {
      log.fine("performing task");

      performTask();

      log.fine("done performing task");
    }
    catch (Exception ex) {
      log.log(Level.WARNING,"task failed",ex);
    }
    finally {
      synchronized (this) {
        _totalActiveTime += (System.currentTimeMillis() - _lastActiveTime);
        _isActive = false;
      }
    }
  }

  protected void performTask()
    throws Exception
  {
    // for the purposes of this tutorial, sleep for 10 seconds
    // to imitate a task that takes 10 seconds to perform
    Thread.sleep(10 * 1000L); 
  }

  /**
   * Required implementation of javax.resource.spi.work.Work.release()
   */
  public void release()
  {
  }
}

