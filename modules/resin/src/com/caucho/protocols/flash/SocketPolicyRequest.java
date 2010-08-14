package com.caucho.protocols.flash;

import java.io.*;

import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.http.HttpRequest;
import com.caucho.vfs.*;
import com.caucho.util.*;

public class SocketPolicyRequest extends HttpRequest
{
  private final static L10N L = new L10N(SocketPolicyRequest.class);
  
  private final Path _policy;
  private final SocketLink _connection;

  public SocketPolicyRequest(Server server,
                             SocketLink connection,
                             Path policy)
  {
    super(server, connection);
    
    _policy = policy;
    _connection = connection;
  }

  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init()
  {
    super.init();
  }
  
  /**
   * Handles a new connection.  The controlling TcpServer may call
   * handleConnection again after the connection completes, so 
   * the implementation must initialize any variables for each connection.
   */
  public boolean handleRequest() 
    throws IOException
  {
    ReadStream is = _connection.getReadStream();

    int ch = is.read();

    if (ch == '<') {
      OutputStream out = _connection.getWriteStream();
      _policy.writeToStream(out);
      out.write(0); // null byte required
      out.flush();

      return false;
    }
    else {
      is.unread();

      return super.handleRequest();
    }
  }
}
