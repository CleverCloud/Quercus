package example;

import com.caucho.util.L10N;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.vfs.ReadStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Parse a request made using the magic8ball protocol, returning
 * appropriate objects that are instances of AbstractCommand.
 *
 * This class is both a parser and a command factory.
 */
public class Parser {
  static protected final Logger log = 
    Logger.getLogger(Parser.class.getName());
  static final L10N L = new L10N(Parser.class);

  // set in init() for each new parse
  private ReadStream _readStream;
  String _error;

  // parsing buffer
  private StringBuffer _buffer = new StringBuffer();

  // commands
  private HashMap _commands = new HashMap();

  public Parser()
  {
    _commands.put("set-prophet",new SetProphetCommand());
    _commands.put("ask",new AskCommand());
  }

  public void init(ReadStream readStream)
  {
    _readStream = readStream;
    _error = null;
  }


  /**
   * Parse one command out of the ReadStream.
   * Once the last command is encountered, null is returned. 
   *
   * @return null if there were no more commands to parse, or there is a parse
   * error.
   */ 
  public AbstractCommand nextCommand()
    throws IOException
  {
    String cmd = parseToken();
    if (cmd == null) {
      return null;
    }
    AbstractCommand command = (AbstractCommand) _commands.get(cmd.toLowerCase());

    if (command == null) {
      _error = "Unknown command `" + cmd + "'";
    } else {
      command.parse(this);
      if (command.isError()) {
        _error = command.getError();
        command = null;
      }
    }

    return command;
  }

  public boolean isError()
  {
    return _error != null;
  }

  public String getError()
  {
    return _error;
  }


  /**
   * @return true if ch is an indication that the end-of-stream was reached
   */
  boolean isEOS(int ch)
  {
    return ch < 0;
  }

  /**
   * @return true if ch is a whitespace character or end-of-stream indicator
   */
  boolean isWhitespace(int ch)
  {
    return isEOS(ch) || Character.isWhitespace((char) ch);
  }

  /**
   * Eat whitespace characters out of readStream
   *
   * @return the first non-whitespace character (which
   * is still in the _readStream), or -1 if end of stream encountered.
   */
  int eatWhitespace()
    throws IOException
  {
    int ch;
    while (!isEOS(ch = _readStream.read())) {
      if (!isWhitespace(ch)) {
        _readStream.unread();
        break;
      }
    }
    return ch;
  }

  /**
   * Parse optional whitespace then a token containing no whitespace.
   *
   * @return the token, or null if there are no tokens left on the stream
   */

  String parseToken()
    throws IOException
  {
    String token = null;

    int ch = eatWhitespace();

    if (ch >= 0) {
      _buffer.setLength(0);

      while (!isEOS(ch = _readStream.read())) {
        if (isWhitespace(ch)) {
          _readStream.unread();
          break;
        }
        _buffer.append((char)ch);
      }
      token = _buffer.toString();
    }

    return token;
  }
}

