package example;

import java.util.logging.Logger;
import java.util.concurrent.BlockingQueue;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

import javax.inject.Inject;
import javax.inject.Named;

public class MessageServlet extends GenericServlet {
  private static final Logger log =
    Logger.getLogger(MessageServlet.class.getName());

  @Inject @Named("my_queue")
  private BlockingQueue _sender;
  private int _count;
  
  /**
   * Sends the message.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    try {
      sendMessage(out);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private void sendMessage(PrintWriter out)
    throws IOException, InterruptedException
  {
    String message = "sample message: " + _count++;

    out.println("message: " + message + "<br>");
    
    log.info("sending: " + message);

    _sender.put(message);
    out.println("last message (0ms): " + MyListener.getLastMessage() + "<br>");
    
    log.info("complete send");

    Thread.sleep(100);

    out.println("last message (100ms): " + MyListener.getLastMessage() + "<br>");
  }
}
