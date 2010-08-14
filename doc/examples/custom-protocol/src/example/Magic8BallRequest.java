package example;

import com.caucho.util.L10N;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.vfs.*;
import com.caucho.server.port.ServerRequest;
import com.caucho.server.connection.Connection;
import java.io.IOException;

/**
 * Protocol specific information for each request.  An instance of this
 * object may be reused to reduce memory allocations.
 */
public class Magic8BallRequest implements ServerRequest {
  static protected final Logger log = 
    Logger.getLogger(Magic8BallRequest.class.getName());
  static final L10N L = new L10N(Magic8BallRequest.class);

  Connection _conn;
  Magic8BallProtocol _protocol;


  // the parser is reset for each request
  Parser _parser = new Parser();

  Magic8Ball _magic8ball = new Magic8Ball();

  /**
   *
   */
  public Magic8BallRequest(Magic8BallProtocol protocol, Connection conn)
  {
    _protocol = protocol;
    _conn = conn;
  }

  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init()
  {
  }

  /**
   * Handle a new connection.  The controlling Server may call
   * handleRequest again after the connection completes, so the
   * implementation must initialize any variables for each connection.
   */
  public boolean handleRequest() throws IOException
  {
    ReadStream readStream = _conn.getReadStream();
    WriteStream writeStream = _conn.getWriteStream();

    try {
      _parser.init(readStream);


      AbstractCommand cmd = null;
      do {
        String result = null;
        String error = null;

        cmd = _parser.nextCommand();
        if (_parser.isError()) {
          error = _parser.getError();
        }
        else if (cmd != null) {
          result = cmd.act(_magic8ball);
          if (cmd.isError())
            error = cmd.getError();
        }

        if (error != null) {
          writeStream.print("ERROR: ");
          writeStream.println(_parser.getError());
          break;
        }
        else if (result != null) {
          writeStream.print("RESULT: ");
          writeStream.println(result);
        }
      } while (cmd != null);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return false;
  }
}

