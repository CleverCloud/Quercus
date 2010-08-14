package example;

import java.io.*;
import java.util.*;

import javax.resource.spi.ResourceAdapter;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ConnectionManager;

import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ConnectionRequestInfo;

import javax.security.auth.Subject;

/**
 * Main interface between Resin and the connector.  It's the
 * top-level SPI class for creating the SPI ManagedConnections.
 *
 * The resource configuration in Resin's web.xml will use bean-style
 * configuration to configure the ManagecConnectionFactory.
 */
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory {
  private String _name;

  // A counter for the example to keep track of the underlying connections.
  // Each ManagedConnectionImpl will get its own id.
  private int _mcCount;
  
  // A counter for the example to keep track of the user connections.
  // Each ConnectionImpl will get its own id.
  private int _cCount;

  /**
   * Sets the name for the connector
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Creates the application's view of the connection factory.
   *
   * The ConnectionFactory is the equivalent of the JDBC DataSource.
   * Applications will use the ConnectionFactory to create connections.
   *
   * The connector can use any API that makes sense for it.  JDBC
   * connectors will return a DataSource.  JMS connectors will return
   * SessionFactory, etc.
   *
   * @param manager ConnectionManager provided by Resin gives access to
   *   some application server resources.
   */
  public Object createConnectionFactory(ConnectionManager manager)
  {
    return new ConnectionFactoryImpl(this, manager);
  }

  /**
   * Creates the SPI-side of a connection, like the <code>XAConnection</code> of
   * JDBC.  Resin will use the returned <code>ManagedConnection</code>
   * to create the application connections, manage transactions,
   * and manage the pool.
   *
   * The <code>ConnectionRequestInfo</code> is not used in this example.
   * When needed, the <code>ConnectionFactoryImpl</code> will create a
   * <code>ConnectionRequestInfo</code> and pass it to Resin with
   * the <code>allocateConnection</code> call.
   *
   * @param subject security identifier of the application requesting
   *   the connection.
   * @param reqInfo connection-specific configuration information
   */
  public ManagedConnection
    createManagedConnection(Subject subject, ConnectionRequestInfo reqInfo)
  {
    return new ManagedConnectionImpl(_name + "-" + _mcCount++, this);
  }

  /**
   * A connection pool method which lets the connector choose which
   * idle connection are allowed to be reused for a request.
   * It returns a connection from the set matching the subject and request
   * info.
   *
   * Many connectors can just return the first connection, if it doesn't
   * matter which connection is used.  However, the pool might contain
   * connections with different configurations and subjects.  This method
   * lets the connector return a connection that properly matches the
   * request.
   *
   * @param set Resin's current pool of idle connections
   * @param subject the application id asking for a connection
   * @param reqInfo connector-specific information used to configure
   *   the connection
   *
   * @return a connection matching the subject and reqInfo requirements or
   *   null if none match
   */
  public ManagedConnection
    matchManagedConnections(Set set,
			    Subject subject,
			    ConnectionRequestInfo reqInfo)
  {
    Iterator iter = set.iterator();

    while (iter.hasNext()) {
      ManagedConnectionImpl mConn = (ManagedConnectionImpl) iter.next();

      // In this example, all connections are equivalent
      return mConn;
    }

    return null;
  }

  /**
   * This connection factory does not have a separate resource adapter.
   *
   * More complicated connection factories will have a
   * separate ResourceAdapter object to share state among multiple
   * connection factories and to manage threads, etc, using
   * the application server.
   */
  public void setResourceAdapter(ResourceAdapter ra)
  {
  }

  /**
   * This connection factory does not have a separate resource adapter.
   *
   * More complicated connection factories will have a
   * separate ResourceAdapter object to share state among multiple
   * connection factories and to manage threads, etc, using
   * the application server.
   */
  public ResourceAdapter getResourceAdapter()
  {
    return null;
  }

  /**
   * createConnectionFactory with no arguments is for a connection factory
   * outside of an application server.  Although most connection factories
   * will implement it, Resin never uses it.
   */
  public Object createConnectionFactory()
  {
    throw new UnsupportedOperationException();
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
  public void setLogWriter(PrintWriter out)
  {
  }

  /**
   * Returns the connection name.
   */
  public String generateConnectionName()
  {
    return _name + "-" + _cCount++ + "-conn";
  }

  public String toString()
  {
    return "ManagedConnectionFactoryImpl[" + _name + "]";
  }
}
