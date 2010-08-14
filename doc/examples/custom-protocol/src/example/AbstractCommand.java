package example;

import com.caucho.util.L10N;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

/**
 * Abstract base class for commands.
 */
abstract public class AbstractCommand {
  static protected final Logger log = 
    Logger.getLogger(AbstractCommand.class.getName());
  static final L10N L = new L10N(AbstractCommand.class);

  private String _error = null;

  public void init()
  {
    _error = null;
  }

  /**
   * If a parse error is encountered then the implementing class calls
   * setError().
   */
  abstract void parse(Parser p) throws IOException;

  /**
   * Perform the command.
   *
   * @return a String result to return to the client, or null if the 
   * command does not produce a result.
   * If an error occurs then the implementing class calls setError().
   */ 
  abstract String act(Magic8Ball magic8ball);


  public boolean isError()
  {
    return _error != null;
  }

  public String getError()
  {
    return _error;
  }

  protected void setError(String error)
  {
    _error = error;
  }
}

