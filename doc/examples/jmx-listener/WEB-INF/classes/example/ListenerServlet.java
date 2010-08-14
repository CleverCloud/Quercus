package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;

/**
 * A client listener servlet to show the MBean listener pattern.
 *
 * The ListenerServlet follows the Dependency Injection pattern,
 * letting Resin's web.xml configure the emitter and listener.
 */
public class ListenerServlet extends GenericServlet {
  private EmitterMBean _emitter;
  private ListenerMBean _listener;

  /**
   * The web.xml will configure the emitter.
   */
  public void setEmitter(EmitterMBean emitter)
  {
    _emitter = emitter;
  }

  /**
   * The web.xml will configure the listener.
   */
  public void setListener(ListenerMBean listener)
  {
    _listener = listener;
  }

  public void service(ServletRequest request,
		      ServletResponse response)
    throws ServletException, IOException
  {
    PrintWriter out = response.getWriter();
    
    _emitter.send();

    out.println("listener count: " + _listener.getNotificationCount());
  }
}
