package example;

import com.caucho.util.L10N;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

/**
 *
 */
public class AskCommand extends AbstractCommand {
  static protected final Logger log = 
    Logger.getLogger(AskCommand.class.getName());
  static final L10N L = new L10N(AskCommand.class);

  public void init()
  {
    super.init();
  }

  void parse(Parser p)
    throws IOException
  {
  }

  String act(Magic8Ball magic8ball)
  {
    return magic8ball.getProphet() + " says \"" + magic8ball.getProphecy() + '"';
  }

}

