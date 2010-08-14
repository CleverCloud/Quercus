package example;

import java.util.*;
import java.util.concurrent.*;

import javax.inject.Singleton;
import javax.inject.Inject;

import com.caucho.servlet.comet.*;

/**
 * The TimerService is an injectible service used to provide example events
 * for the comet application.  The ScheduledExecutorService schedules
 * a timer every 2 seconds, causing the TimerService to wake up the
 * comet threads so they can process the next data.
 *
 * @ApplicationScoped is a Java Injection marker telling Resin to create
 * a singleton instance of TimerService in the webApp to be used by all
 * clients.
 *
 * @Initializer selects the constructor for Java Injection and tells
 * it to inject Resin's own ScheduledExecutorService, which Resin
 * automatically registers with Java Injection under the @Current binding.
 *
 * Using Resin's ScheduledExecutorService is a good idea because it
 * uses Resin's own thread management and configuration for the applications.
 */
@Singleton
public class TimerService implements Runnable {
  private ScheduledExecutorService _timer;

  private Future _timerFuture;

  private ArrayList<CometState> _stateList
    = new ArrayList<CometState>();

  /**
   * Creates the TimerService for Java Injection, when the comet servlet
   * asks for it.  Java Injection passes the correct ScheduledExecutorService
   * automatically.
   */
  @Inject
  public TimerService(ScheduledExecutorService timer)
  {
    _timer = timer;

    _timerFuture = _timer.scheduleAtFixedRate(this, 0, 2, TimeUnit.SECONDS);
  }

  public void addCometState(CometState state)
  {
    synchronized (_stateList) {
      _stateList.add(state);
    }
  }

  /**
   * The timer task wakes up every active comet state.
   *
   * A more sophisticated application would notify the comet states
   * as part of an event-based system.
   */
  public void run()
  {
    synchronized (_stateList) {
      for (int i = _stateList.size() - 1; i >= 0; i--) {
        CometState state = _stateList.get(i);

        try {
          if (! state.wake()) {
            _stateList.remove(i);
          }
        } catch (Exception e) {
          e.printStackTrace();
          _stateList.remove(i);
        }
      }
    }
  }

  /**
   * Close the timer.
   */
  public void close()
  {
    _timerFuture.cancel(false);
  }
}
