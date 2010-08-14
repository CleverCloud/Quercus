package example;

import java.util.TimerTask;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkException;

/**
 * Implements the timer task.  This task just launches the work task.
 */
public class WorkScheduleTimerTask extends TimerTask {
  private static final Logger log =
    Logger.getLogger(TimerTask.class.getName());

  private WorkManager _workManager;
  private Work _work;

  /**
   * Creates the timer task.
   */
  WorkScheduleTimerTask(WorkManager workManager, Work work)
  {
    _workManager = workManager;
    _work = work;
  }

  /**
   * The method called to execute the task, like Runnable
   */
  public void run()
  {
    log.fine("timer starting");

    try {
      _workManager.scheduleWork(_work);
    } catch (WorkException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}
