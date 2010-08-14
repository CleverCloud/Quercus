package example;

/**
 * Perform a task and keep track of statistics.
 */ 
public interface PeriodicTaskMBean {
  /**
   * The estimated amount of milliseconds that the task will take,
   * used when the PeriodicTask has not been run yet so no historical data
   * is available.  Default is 5000.
   */
  public void setEstimatedAverageTime(long estimatedAverageTime);

  /**
   * The estimated amount of milliseconds that the task will take,
   * used when the PeriodicTask has not been run yet so no historical data
   * is available.
   */
  public long getEstimatedAverageTime();

  /**
   * True if the task is currently active.
   */
  public boolean isActive();

  /**
   * If currently active, how much longer will the task take?
   *
   * @return the estimate in milliseconds, 0 if not currently active 
   */
  public long getEstimatedTimeRemaining();

  /**
   * The last time the task was started.
   *
   * @return the last active time, in milliseconds since the epoch 
   */
  public long getLastActiveTime();

  /**
   * The number of times the task has been performed.
   */
  public long getTotalActiveCount();

  /**
   * The total amount of time the tasks have taken.
   *
   * @return the total active time, in milliseconds
   */
  public long getTotalActiveTime();

  /**
   * The average amount of time the task has taken.
   * If the task has never been run, then the estimatedAverageTime is returned.
   *
   * @return the average active time, in milliseconds
   */
  public long getAverageActiveTime();

  /**
   * Execute the task.  Only one execution of the task can take place at a
   * time, if the task is currently active, this method returns immediately.
   */
  public void run();
}

