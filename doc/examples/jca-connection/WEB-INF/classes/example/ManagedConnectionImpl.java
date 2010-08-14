package example;

import java.io.*;
import java.util.*;

import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionEvent;

import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.LocalTransaction;

import javax.transaction.xa.XAResource;

import javax.security.auth.Subject;

/**
 * ManagedConnectionImpl represents the underlying (SPI) connection to the
 * resource.  Resin will manage this ManagedConnection in its pool.
 *
 * The user will see the ConnectionImpl facade and may not even know
 * that the ManagedConnectionImpl exists.
 */
public class ManagedConnectionImpl implements ManagedConnection {
  private ManagedConnectionFactoryImpl _factory;
  
  // Identifier for the ManagedConnectionImpl
  private String _name;
  
  // Resin needs to listen for close events
  private ArrayList _listeners = new ArrayList();

  /**
   * Creates a new ManagedConnection with its id.
   * ManagedConnectionFactoryImpl will create the ManagedConnectionImpl
   * in response to a Resin request.
   */
  ManagedConnectionImpl(String name, ManagedConnectionFactoryImpl factory)
  {
    _name = name;
    _factory = factory;
  }
  
  /**
   * Creates a new application connection.  The application connection
   * will be in use until its <code>close()</code> method is called.
   * It's <code>close</code> method will call the ConnectionEventListener
   * registered with this ManagedConnectionImpl instance.
   *
   * It is important for the connection's close to be called and for the
   * connection to call the close listeners, because Resin needs to
   * know when the application is done with the connection and Resin
   * can return the ManagedConnection to the pool.
   *
   * @param subject the subject for the connection
   * @param info the connection information for the connection
   *
   * @return the application-view of the connection
   */
  public Object getConnection(Subject subject, ConnectionRequestInfo info)
  {
    return new ConnectionImpl(_factory.generateConnectionName(), this);
  }

  /**
   * XXX:???
   *
   * In some cases, Resin can associate an old application connection
   * with the ManagedConnection.  (I'm not sure when this can happen.)
   *
   * @param conn the application view of the connection
   */
  public void associateConnection(Object conn)
  {
  }

  /**
   * Resin will register a listener with the ManagedConnection so it
   * will know when a connection closes or has a fatal error.
   *
   * @param listener Resin's listener to receive notice of a close.
   */
  public void addConnectionEventListener(ConnectionEventListener listener)
  {
    _listeners.add(listener);
  }


  /**
   * Resin can remove it's listener when it removes the managed connection
   * from the pool.
   *
   * @param listener Resin's listener to receive notice of a close.
   */
  public void removeConnectionEventListener(ConnectionEventListener listener)
  {
    _listeners.remove(listener);
  }

  /**
   * Implementation method to allow the <code>ConnectionImpl</code> to
   * trigger Resin's listeners when the connection closes.
   *
   * @param conn the user connection which is closing.
   */
  void close(ConnectionImpl conn)
  {
    ConnectionEvent evt;
    evt = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);

    for (int i = 0; i < _listeners.size(); i++) {
      ConnectionEventListener listener;
      listener = (ConnectionEventListener) _listeners.get(i);

      listener.connectionClosed(evt);
    }
  }

  /**
   * This example isn't returning any meta data.  In general,
   * providing the meta data is nice for the applications.
   */
  public ManagedConnectionMetaData getMetaData()
  {
    return null;
  }

  /**
   * Transaction-aware resources will return the XAResource for the
   * managed connection.
   */
  public XAResource getXAResource()
  {
    return null;
  }

  /**
   * Transaction-aware resources will return the LocalTransaction for the
   * managed connection.  LocalTransaction is a lightweight interface
   * for transactions that don't need the full XA transactions.
   */
  public LocalTransaction getLocalTransaction()
  {
    return null;
  }

  /**
   * Called when Resin returns a connection to the idle pool.
   */
  public void cleanup()
  {
  }

  /**
   * Called when Resin is closing the connection.  Any sockets, etc,
   * would be closed here.
   */
  public void destroy()
  {
  }

  /**
   * Logging should use JDK 1.4 java.util.logging.
   */
  public PrintWriter getLogWriter()
  {
    return null;
  }

  /**
   * Logging should use JDK 1.4 java.util.logging.
   */
  public void setLogWriter(PrintWriter log)
  {
  }

  public String toString()
  {
    return "ManagedConnectionImpl[" + _name + "]";
  }
}
