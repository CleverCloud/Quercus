package example;

import java.util.Date;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.naming.InitialContext;

import javax.resource.spi.work.Work;

/**
 * Implements the work task.  This task just loops until the resource stops.
 */
public class WorkTask implements Work {
  private static final Logger log = Logger.getLogger(WorkTask.class.getName());

  private String _value = "default";
  private String _jndi = "java:comp/env/test";
  
  /**
   * Creates the work task.
   */
  public WorkTask()
  {
  }

  /**
   * Sets the JNDI name to store the value.
   */
  public void setJNDI(String jndi)
  {
    _jndi = jndi;
  }

  /**
   * Sets the name of the work task (as an example bean-style parameter.)
   */
  public void setValue(String value)
  {
    _value = value;
  }

  /**
   * Resin calls <code>init()</code> immediately after the parameters are
   * set during configuration.  Resources can use <code>init()</code>
   * to validate parameters.
   */
  public void init()
  {
    try {
      new InitialContext().rebind(_jndi, _value + ": init");
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * The method called to execute the task, like Runnable
   */
  public void run()
  {
    log.fine("work starting");

    try {
      new InitialContext().rebind(_jndi, _value + ": " + new Date());
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Resin will call the release() method when the server shuts down
   * to tell the task to close.
   */
  public void release()
  {
  }
}
