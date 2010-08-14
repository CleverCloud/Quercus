/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.PropertyValueProgram;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.JndiBuilder;
import com.caucho.config.types.PathPatternType;
import com.caucho.config.types.Period;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.manager.EjbEnvironmentListener;
import com.caucho.env.jpa.ListenerPersistenceEnvironment;
import com.caucho.env.jpa.PersistenceManager;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class EJBServer
  implements EnvironmentBean
{
  static final L10N L = new L10N(EJBServer.class);
  protected static final Logger log
    = Logger.getLogger(EJBServer.class.getName());

  private static EnvironmentLocal<EJBServer> _localServer
    = new EnvironmentLocal<EJBServer>("caucho.ejb-server");

  protected static EnvironmentLocal<String> _localURL =
    new EnvironmentLocal<String>("caucho.url");
  
  private EjbManager _ejbContainer;
  private AmberContainer _amberContainer;

  private String _entityManagerJndiName = "java:comp/EntityManager";
  private ArrayList<Path> _ejbJars = new ArrayList<Path>();

  private MergePath _mergePath;

  private String _urlPrefix;

  private ArrayList<FileSetType> _configFileSetList =
    new ArrayList<FileSetType>();

  private ContainerProgram _jpaProgram = new ContainerProgram();
  
  // private DataSource _dataSource;
  private boolean _validateDatabaseSchema = true;

  private String _resinIsolation;
  private String _jdbcIsolation;

  private ConnectionFactory _jmsConnectionFactory;
  
  private Path _configDirectory;

  private boolean _forbidJVMCall;
  private boolean _autoCompile = true;
  private boolean _isAllowPOJO = false;

  private String _startupMode;

  private long _transactionTimeout = 0;

  /**
   * Create a server with the given prefix name.
   */
  public EJBServer()
    throws ConfigException
  {
    _ejbContainer = EjbManager.create();
    // _amberContainer = AmberContainer.create();
    
    _urlPrefix = _localURL.get();

    _mergePath = new MergePath();
    _mergePath.addMergePath(Vfs.lookup());
    _mergePath.addClassPath();
  }

  public void addJarUrls(EnvironmentClassLoader loader, Path root)
    throws java.io.IOException
  {
    Iterator<String> it = root.iterator();

    while (it.hasNext()) {

      String s = it.next();

      Path path = root.lookup(s);

      if (path.isDirectory()) {
        addJarUrls(loader, path);
      }
      else if (s.endsWith(".jar")) {
        JarPath jarPath = JarPath.create(path);

        loader.addURL(jarPath);
      }
    }
  }

  /**
   * Returns the local EJB server.
   */
  /*
    public static EnvServerManager getLocalManager()
    {
    return EnvServerManager.getLocal();
    }
  */

  /**
   * Gets the environment class loader.
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _ejbContainer.getClassLoader();
  }

  /**
   * Sets the environment class loader.
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader env)
  {
  }

  /**
   * Sets the JNDI name.
   */
  public void setName(String name)
  {
    setJndiName(name);
  }

  /**
   * Sets the JNDI name.
   */
  public void setJndiName(String name)
  {
    setJndiPrefix(name);
  }

  /**
   * Sets the JNDI name.
   */
  public void setJndiPrefix(String name)
  {
    _ejbContainer.getProtocolManager().setJndiPrefix(name);
  }

  /**
   * Gets the JNDI name.
   */
  public void setJndiLocalPrefix(String name)
  {
    _ejbContainer.getProtocolManager().setLocalJndiPrefix(name);
  }

  /**
   * Gets the remote JNDI name.
   */
  public void setJndiRemotePrefix(String name)
  {
    _ejbContainer.getProtocolManager().setRemoteJndiPrefix(name);
  }

  /**
   * Sets the EntityManager JNDI name.
   */
  public void setEntityManagerJndiName(String name)
  {
    _entityManagerJndiName = name;
  }

  /**
   * Gets the EntityManager JNDI name.
   */
  public String getEntityManagerJndiName()
  {
    return _entityManagerJndiName;
  }

  /**
   * Sets the URL-prefix for all external beans.
   */
  public void setURLPrefix(String urlPrefix)
  {
    _urlPrefix = urlPrefix;
  }

  /**
   * Gets the URL-prefix for all external beans.
   */
  public String getURLPrefix()
  {
    return _urlPrefix;
  }

  public void setConfigDirectory(Path path)
  {
    
  }
  
  /**
   * Adds an ejb jar.
   */
  public void addEJBJar(Path ejbJar)
    throws ConfigException
  {
    if (! ejbJar.canRead() || ! ejbJar.isFile())
      throw new ConfigException(L.l("<ejb-jar> {0} must refer to a valid jar file.",
                                    ejbJar.getURL()));

    // tck: sanity check
    if (_ejbJars.contains(ejbJar)) {
      log.fine("EJBServer.addEJBJar already added: " + ejbJar);
      return;
    }

    _ejbJars.add(ejbJar);
  }

  /**
   * Sets the data-source
   */
  public void setDataSource(DataSource dataSource)
    throws ConfigException
  {
    if (dataSource == null)
      throw new ConfigException(L.l("<ejb-server> data-source must be a valid DataSource."));

    _jpaProgram.addProgram(new PropertyValueProgram("jta-data-source-value", dataSource));

    // _amberContainer.setDataSource(_dataSource);
  }

  /**
   * Sets the data-source
   */
  public void setReadDataSource(DataSource dataSource)
    throws ConfigException
  {
    if (dataSource == null)
      throw new ConfigException(L.l("<ejb-server> data-source must be a valid DataSource."));

    _jpaProgram.addProgram(new PropertyValueProgram("non-jta-data-source-value", dataSource));

    // _amberContainer.setReadDataSource(dataSource);
  }

  /**
   * Sets the xa data-source
   */
  public void setXADataSource(DataSource dataSource)
    throws ConfigException
  {
    setDataSource(dataSource); // _amberContainer.setXADataSource(dataSource);
  }

  /**
   * Sets true if database schema should be generated automatically.
   */
  public void setCreateDatabaseSchema(boolean create)
  {
    // _amberContainer.setCreateDatabaseTables(create);
  }

  /**
   * True if database schema should be generated automatically.
   */
  public boolean getCreateDatabaseSchema()
  {
    //
    // return _amberContainer.getCreateDatabaseTables();
    return false;
  }

  /**
   * Sets true if database schema should be validated automatically.
   */
  public void setValidateDatabaseSchema(boolean validate)
  {
    log.config("validate-database-schema is no longer valid");
  }

  /**
   * True if database schema should be validated automatically.
   */
  public boolean getValidateDatabaseSchema()
  {
    log.config("validate-database-schema is no longer valid");
    
    return true;
  }

  /**
   * Sets true if database schema should be validated automatically.
   */
  public void setLoadLazyOnTransaction(boolean isLazy)
  {
    // _ejbContainer.setEntityLoadLazyOnTransaction(isLazy);
  }

  /**
   * Sets the jndi name of the jmsConnectionFactory
   */
  public void setJMSConnectionFactory(JndiBuilder factory)
    throws ConfigException, NamingException
  {
    Object obj = factory.getObject();

    if (! (obj instanceof ConnectionFactory))
      throw new ConfigException(L.l("'{0}' must be a JMS ConnectionFactory.", obj));

    _ejbContainer.setJmsConnectionFactory((ConnectionFactory) obj);
  }

  /**
   * Gets the jndi name of the jmsQueueConnectionFactory
   */
  public ConnectionFactory getConnectionFactory()
  {
    return _jmsConnectionFactory;
  }

  /**
   * Sets consumer max
   */
  public void setMessageConsumerMax(int consumerMax)
    throws ConfigException, NamingException
  {
    _ejbContainer.setMessageConsumerMax(consumerMax);
  }

  /**
   * Gets transaction timeout.
   */
  public long getTransactionTimeout()
  {
    return _transactionTimeout;
  }

  /**
   * Sets the transaction timeout.
   */
  public void setTransactionTimeout(Period timeout)
  {
    _transactionTimeout = timeout.getPeriod();
  }

  /**
   * Gets the Resin isolation.
   */
  public String getResinIsolation()
  {
    return _resinIsolation;
  }

  /**
   * Sets the Resin isolation.
   */
  public void setResinIsolation(String resinIsolation)
  {
    _resinIsolation = resinIsolation;
  }

  /**
   * Gets the JDBC isolation.
   */
  public String getJdbcIsolation()
  {
    return _jdbcIsolation;
  }

  /**
   * Sets the JDBC isolation.
   */
  public void setJdbcIsolation(String jdbcIsolation)
  {
    _jdbcIsolation = jdbcIsolation;
  }

  /**
   * If true, JVM calls are forbidden.
   */
  public void setForbidJvmCall(boolean forbid)
  {
    _forbidJVMCall = forbid;
  }

  /**
   * If true, automatically compile old EJBs.
   */
  public boolean isAutoCompile()
  {
    return _autoCompile;
  }

  /**
   * Set true to automatically compile old EJBs.
   */
  public void setAutoCompile(boolean autoCompile)
  {
    _autoCompile = autoCompile;
  }

  /**
   * If true, allow POJO beans
   */
  public boolean isAllowPOJO()
  {
    return _isAllowPOJO;
  }

  /**
   * Set true to allow POJO beans
   */
  public void setAllowPOJO(boolean allowPOJO)
  {
    _isAllowPOJO = allowPOJO;
  }

  /**
   * Sets the EJB server startup mode.
   */
  public void setStartupMode(String startupMode)
  {
    _startupMode = startupMode;
  }

  public static EJBServer getLocal()
  {
    return _localServer.get();
  }

  /**
   * Initialize the container.
   */
  @PostConstruct
  public void init()
  {
    try {
      Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
    
      // _ejbContainer.start();
      
      PersistenceManager persistenceManager = PersistenceManager.create();
      
      if (persistenceManager != null)
        persistenceManager.addPersistenceUnitDefault(_jpaProgram);

      if ("manual".equals(_startupMode))
        return;

      manualInit();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Initialize the container.
   */
  public void manualInit()
    throws Exception
  {
    try {
      log.fine("Initializing ejb-server : local-jndi="
               + _ejbContainer.getProtocolManager().getLocalJndiPrefix()
               + " remote-jndi="
               + _ejbContainer.getProtocolManager().getRemoteJndiPrefix());

      Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
      Environment.addChildLoaderListener(new EjbEnvironmentListener());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw e;
    }
  }

  /**
   * Initialize all EJBs for any *.ejb or ejb-jar.xml in the WEB-INF or
   * in a META-INF in the classpath.
   */
  public void initEJBs()
    throws Exception
  {
    manualInit();
  }
}
