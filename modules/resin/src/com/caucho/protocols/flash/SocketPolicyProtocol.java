package com.caucho.protocols.flash;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.http.AbstractHttpProtocol;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Simple protocol that sends the contents of a specified file as soon
 * as it is contacted.  It is intended for sending flash policy files
 * for flash.net.Sockets when the target port of the socket is < 1024.
 *
 **/
public class SocketPolicyProtocol extends AbstractHttpProtocol
{
  private final static L10N L = new L10N(SocketPolicyRequest.class);
  
  private Path _policy;
  
  public SocketPolicyProtocol()
  {
    setProtocolName("http");
  }

  public void setSocketPolicyFile(Path path)
  {
    setPolicyFile(path);
  }

  /**
   * Sets the flash socket policy file.
   */
  public void setPolicyFile(Path path)
  {
    _policy = path;
  }

  @PostConstruct
  public void init()
  {
    if (_policy == null)
      throw new ConfigException(L.l("flash requires a policy-file"));
  }

  public ProtocolConnection createConnection(SocketLink conn)
  {
    return new SocketPolicyRequest(getServer(), conn, _policy);
  }
}
