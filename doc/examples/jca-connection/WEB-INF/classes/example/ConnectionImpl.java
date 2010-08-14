package example;

import java.io.*;
import java.util.*;

import javax.resource.*;
import javax.resource.spi.*;
import javax.security.auth.*;

/**
 * Implementation of the user's view of the connection.  This
 * class is entirely customizable.
 *
 * Normally, it will just be a facade to the underlying managed
 * connection.
 */
public class ConnectionImpl {
  private String _name;
  
  // Reference to the underlying connection
  private ManagedConnectionImpl _mConn;

  private volatile boolean _isClosed;

  /**
   * Create the connection, with a reference to the underlying
   * connection.
   */
  ConnectionImpl(String name, ManagedConnectionImpl mConn)
  {
    _name = name;
    _mConn = mConn;
  }

  /**
   * Adding a <code>close()</code> method is very important for any
   * connection API.  It lets Resin know that the user has
   * closed the connection and Resin can mark the managed connection
   * as idle and reuse it.
   *
   * It is also important that the connection let the user call
   * <code>close()</code>, but only the allow the  first <code>close</code>
   * to have any effect.
   *
   * In particular, it would be a mistake to call <code>_mConn.close</code>
   * twice since that would tell Resin that the connection had closed
   * twice, which might confuse Resin's pool.
   */
  public void close()
  {
    synchronized (this) {
      if (_isClosed)
	return;
      _isClosed = true;
    }

    ManagedConnectionImpl mConn = _mConn;
    _mConn = null;
    
    mConn.close(this);
  }

  public String toString()
  {
    return "ConnectionImpl[" + _name + "," + _mConn + "]";
  }

  public void finalize()
  {
    close();
  }
}
