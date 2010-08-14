package example;

import com.caucho.util.L10N;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.server.connection.Connection;
import com.caucho.server.port.Protocol;
import com.caucho.server.port.ServerRequest;

/**
 *
 */
public class Magic8BallProtocol extends Protocol {
  static protected final Logger log = 
    Logger.getLogger(Magic8BallProtocol.class.getName());
  static final L10N L = new L10N(Magic8BallProtocol.class);

  private String _protocolName = "magic8ball";

  /**
   * Construct.
   */
  public Magic8BallProtocol()
  {
  }

  /**
   * Returns the protocol name.
   */
  public String getProtocolName()
  {
    return _protocolName;
  }
  
  /**
   * Sets the protocol name.
   */
  public void setProtocolName(String name)
  {
    _protocolName = name;
  }

  /**
   * Create a Magic8BallRequest object for the new thread.
   */
  public ServerRequest createRequest(Connection conn)
  {
    return new Magic8BallRequest(this, conn);
  }
}

