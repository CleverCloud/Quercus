package example;

import javax.servlet.*;
import java.io.*;
import java.util.logging.*;

import com.caucho.servlet.*;

public class WebSocketServlet extends GenericServlet {
  private static final Logger log
    = Logger.getLogger(WebSocketServlet.class.getName());

  public void service(ServletRequest request,
                      ServletResponse response)
    throws IOException, ServletException
  {
    WebSocketServletRequest wsRequest = (WebSocketServletRequest) request;

    wsRequest.startWebSocket(new WebSocketHandler());
  }

  static class WebSocketHandler implements WebSocketListener {
    private InputStream _is;
    private OutputStream _os;

    public void onStart(WebSocketContext context)
      throws IOException
    {
      // sets the connection timeout to 120s
      context.setTimeout(120000);

      _is = context.getInputStream();
      _os = context.getOutputStream();
    }

    public void onRead(WebSocketContext context)
      throws IOException
    {
      StringBuilder sb = new StringBuilder();

      // The syntax of a websocket string is
      //  0x00 utf8-encoded-data 0xff
      int ch = _is.read();

      if (ch != 0x00) {
        log.warning("WebSocket unexpected initial byte: "
                    + " 0x" + Integer.toHexString(ch));
        return;
      }

      while ((ch = _is.read()) >= 0 && ch != 0xff) {
        sb.append((char) ch);
      }

      String message = sb.toString();
      String result = "unknown message";

      if ("hello".equals(message))
        result = "world";
      else if ("server".equals(message))
        result = "Resin";

      // Encode the response with the websocket packet
      //  0x00 utf8-encoded-data 0xff
      _os.write(0x00);
      _os.write(result.getBytes("utf-8"));
      _os.write(0xff);
      _os.flush();
    }

    public void onComplete(WebSocketContext context)
      throws IOException
    {
    }

    public void onTimeout(WebSocketContext context)
      throws IOException
    {
    }
  }
}