package example;

import com.caucho.util.L10N;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

/**
 *
 */
public class SetProphetCommand extends AbstractCommand {
  static protected final Logger log = 
    Logger.getLogger(SetProphetCommand.class.getName());
  static final L10N L = new L10N(SetProphetCommand.class);

  String _prophet = null;

  public void init()
  {
    super.init();
    _prophet = null;
  }

  void parse(Parser p)
    throws IOException
  {
    _prophet = p.parseToken();
    if (_prophet == null)
      setError("Expecting prophet name");
  }

  String act(Magic8Ball magic8ball)
  {
    magic8ball.setProphet(_prophet);
    return "prophet set to `" + magic8ball.getProphet() + "'";
  }

}

