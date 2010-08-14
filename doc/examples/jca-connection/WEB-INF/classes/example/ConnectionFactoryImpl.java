package example;

import java.io.*;
import java.util.*;

import javax.resource.ResourceException;

import javax.resource.spi.ConnectionManager;

import javax.security.auth.*;

/**
 * Application's view of the connection factory.  This
 * class may have any API the connector wants.
 *
 * Often, it will just create connections.
 */
public class ConnectionFactoryImpl {
  // Reference to Resin's ConnectionManager API, used to
  // allocate a new connection
  private ConnectionManager _connManager;

  // Reference to the underlying SPI ManagedConnectionFactory
  private ManagedConnectionFactoryImpl _mcFactory;

  private volatile boolean _isClosed;

  /**
   * Create the connection factory, with a reference to
   * Resin's ConnectionManager interface.
   *
   * @param mcFactory the connector's SPI ManagedConnectionFactory
   * @param connManager Resin's ConnectionManager for allocating connections
   */
  ConnectionFactoryImpl(ManagedConnectionFactoryImpl mcFactory,
			ConnectionManager connManager)
  {
    _mcFactory = mcFactory;
    _connManager = connManager;
  }

  /**
   * Gets an available connection.  Something like
   * <code>getConnection</code>
   * is the main method most connections factories will implement.
   * It returns a class specific to the connector.
   *
   * @return the user connection facade.
   */
  public ConnectionImpl getConnection()
    throws ResourceException
  {
    // Asks Resin to return a new user connection.  The second argument
    // (ConnectionRequestInfo) is null since we have no specific
    // configuration information to pass along.
    return (ConnectionImpl) _connManager.allocateConnection(_mcFactory, null);
  }

  public String toString()
  {
    return "ConnectionFactoryImpl[" + _mcFactory + "]";
  }
}
