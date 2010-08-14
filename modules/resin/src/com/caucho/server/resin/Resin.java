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

package com.caucho.server.resin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.management.ObjectName;

import com.caucho.VersionFactory;
import com.caucho.bam.Broker;
import com.caucho.cloud.bam.BamService;
import com.caucho.cloud.loadbalance.LoadBalanceFactory;
import com.caucho.cloud.loadbalance.LoadBalanceService;
import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.network.NetworkClusterService;
import com.caucho.cloud.network.NetworkListenService;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.cloud.topology.CloudSystem;
import com.caucho.cloud.topology.TopologyService;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.functions.FmtFunctions;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.WebBeansAddLoaderListener;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.config.program.ConfigProgram;
import com.caucho.ejb.manager.EjbEnvironmentListener;
import com.caucho.env.jpa.ListenerPersistenceEnvironment;
import com.caucho.env.repository.LocalRepositoryService;
import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositoryService;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.service.RootDirectoryService;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownService;
import com.caucho.java.WorkDir;
import com.caucho.license.LicenseCheck;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ResinMXBean;
import com.caucho.management.server.ThreadPoolMXBean;
import com.caucho.naming.Jndi;
import com.caucho.server.admin.Management;
import com.caucho.server.cluster.ClusterPod;
import com.caucho.server.cluster.Server;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.resin.ResinArgs.BoundPort;
import com.caucho.server.webbeans.ResinCdiProducer;
import com.caucho.util.Alarm;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class Resin
{
  private static Logger _log;
  private static L10N _L;

  public static final int EXIT_OK = 0;

  private static final EnvironmentLocal<Resin> _resinLocal
    = new EnvironmentLocal<Resin>();

  private final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  private boolean _isEmbedded;

  private String _serverId = "";
  private final boolean _isWatchdog;
  
  private ResinArgs _args;

  private Path _resinHome;
  private Path _rootDirectory;

  private Path _resinDataDirectory;
  
  private ResinSystem _resinSystem;
  
  private long _shutdownWaitMax = 60000L;

  private Lifecycle _lifecycle;

  private BootResinConfig _bootResinConfig;
  private Server _server;

  private long _initialStartTime;
  private long _startTime;

  private String _licenseErrorMessage;

  private Path _resinConf;

  private ClassLoader _systemClassLoader;

  private Thread _mainThread;

  protected Management _management;

  private ThreadPoolAdmin _threadPoolAdmin;
  private ObjectName _objectName;
  private ResinAdmin _resinAdmin;

  private InputStream _waitIn;

  private Socket _pingSocket;
  
  private ResinWaitForExitService _waitForExitService;

  private String _stage = null;

  /**
   * Creates a new resin server.
   */
  protected Resin(ClassLoader loader, boolean isWatchdog)
  {
    this(loader, isWatchdog, null);
  }

  /**
   * Creates a new resin server.
   */
  protected Resin(ClassLoader loader,
                  boolean isWatchdog,
                  String licenseErrorMessage)
  {
    _startTime = Alarm.getCurrentTime();

    _isWatchdog = isWatchdog;
    _licenseErrorMessage = licenseErrorMessage;

    // DynamicClassLoader.setJarCacheEnabled(true);
    Environment.init();

    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();

    initEnvironment();
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin create()
  {
    return create(Thread.currentThread().getContextClassLoader(), false);
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createWatchdog()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    Resin resin = create(loader, true);

    return resin;
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin create(ClassLoader loader, boolean isWatchdog)
  {
    String licenseErrorMessage = null;

    Resin resin = null;
    
    if (loader == null)
      loader = Thread.currentThread().getContextClassLoader();

    try {
      Class<?> cl = Class.forName("com.caucho.server.resin.ProResin");
      Constructor<?> ctor = cl.getConstructor(new Class[] { ClassLoader.class, boolean.class });

      resin = (Resin) ctor.newInstance(loader, isWatchdog);
    } catch (ConfigException e) {
      log().log(Level.FINER, e.toString(), e);

      licenseErrorMessage = e.getMessage();
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();

      log().log(Level.FINER, cause.toString(), cause);

      if (cause instanceof ConfigException) {
        licenseErrorMessage = cause.getMessage();
      }
      else {
        licenseErrorMessage= L().l("  Using Resin(R) Open Source under the GNU Public License (GPL).\n"
                                   + "\n"
                                   + "  See http://www.caucho.com for information on Resin Professional,\n"
                                   + "  including caching, clustering, JNI acceleration, and OpenSSL integration.\n"
                                   + "\n  Exception=" + cause + "\n");
      }
    } catch (Throwable e) {
      log().log(Level.FINER, e.toString(), e);

      String causeMsg = "";
      if (! (e instanceof ClassNotFoundException)) {
        causeMsg = "\n  Exception=" + e + "\n";
      }


      String msg = L().l("  Using Resin(R) Open Source under the GNU Public License (GPL).\n"
                         + "\n"
                         + "  See http://www.caucho.com for information on Resin Professional,\n"
                         + "  including caching, clustering, JNI acceleration, and OpenSSL integration.\n"
                         + causeMsg);

      licenseErrorMessage = msg;
    }

    if (resin == null) {
      try {
        Class<?> cl = Class.forName("com.caucho.license.LicenseCheckImpl");
        LicenseCheck license = (LicenseCheck) cl.newInstance();

        license.requirePersonal(1);

        licenseErrorMessage = license.doLogging();
      } catch (ConfigException e) {
        licenseErrorMessage = e.getMessage();
      } catch (Throwable e) {
        // message should already be set above
      }

      resin = new Resin(loader, isWatchdog, licenseErrorMessage);
    }

    _resinLocal.set(resin, loader);

    // resin.initEnvironment();

    return resin;
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createOpenSource()
  {
    return createOpenSource(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a new Resin instance
   */
  public static Resin createOpenSource(ClassLoader loader)
  {
    return new Resin(loader, false, null);
  }

  /**
   * Returns the resin server.
   */
  public static Resin getLocal()
  {
    return _resinLocal.get();
  }

  /**
   * Returns the resin server.
   */
  public static Resin getCurrent()
  {
    return getLocal();
  }

  public ResinSystem getResinSystem()
  {
    return _resinSystem;
  }
  
  public CloudSystem getCloudSystem()
  {
    ResinSystem resinSystem = _resinSystem;
    
    if (resinSystem != null)
      return resinSystem.getService(TopologyService.class).getSystem();
    else
      return null;
  }
  
  public void setRootDirectory(Path path)
  {
    _rootDirectory = path;
  }
  
  public void setPingSocket(Socket socket)
  {
    _pingSocket = socket;
  }
  
  public void setStage(String stage)
  {
    _stage = stage; 
  }
  
  public void setEmbedded(boolean isEmbedded)
  {
    _isEmbedded = isEmbedded;
  }
  
  private void initEnvironment()
  {
    String resinHome = System.getProperty("resin.home");

    if (resinHome != null)
      _resinHome = Vfs.lookup(resinHome);
    else
      _resinHome = Vfs.getPwd();

    _rootDirectory = _resinHome;

    // server.root backwards compat
    String resinRoot = System.getProperty("server.root");

    if (resinRoot != null)
      _rootDirectory = Vfs.lookup(resinRoot);

    // resin.root backwards compat
    resinRoot = System.getProperty("resin.root");

    if (resinRoot != null)
      _rootDirectory = Vfs.lookup(resinRoot);
  }
  
  /**
   * Must be called after the Resin.create()
   */
  public void preConfigureInit()
  {
    if (_lifecycle != null)
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      String serverName = getServerId();
      if (serverName == null || "".equals(serverName))
        serverName = "default";
      
      _resinSystem = new ResinSystem(serverName);
      
      thread.setContextClassLoader(getClassLoader());

      _resinLocal.set(this, getClassLoader());

      _lifecycle = new Lifecycle(log(), "Resin[]");

      // default server id
      if (_args != null) {
        setServerId(_args.getServerId());

        if (_rootDirectory == null)
          setRootDirectory(_args.getRootDirectory());
      }
      
      if (getRootDirectory() == null)
        throw new NullPointerException();
      
      addServices();
      
      _bootResinConfig = new BootResinConfig(this);

      // watchdog/0212
      // else
      //  setRootDirectory(Vfs.getPwd());

      Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
      Environment.addChildLoaderListener(new WebBeansAddLoaderListener());
      Environment.addChildLoaderListener(new EjbEnvironmentListener());
      InjectManager cdiManager = InjectManager.create();

      Config.setProperty("resinHome", getResinHome());
      Config.setProperty("resin", new Var());
      Config.setProperty("server", new Var());
      Config.setProperty("java", new JavaVar());
      Config.setProperty("system", System.getProperties());
      Config.setProperty("getenv", System.getenv());

      // _management = createResinManagement();

      if (cdiManager.getBeans(ResinCdiProducer.class).size() == 0) {
        Config.setProperty("fmt", new FmtFunctions());

        ResinConfigLibrary.configure(cdiManager);

        try {
          Method method = Jndi.class.getMethod("lookup", new Class[] { String.class });
          Config.setProperty("jndi", method);
          Config.setProperty("jndi:lookup", method);
        } catch (Exception e) {
          throw ConfigException.create(e);
        }

        cdiManager.addManagedBean(cdiManager.createManagedBean(ResinCdiProducer.class));
        Class<?> resinValidatorClass = ResinCdiProducer.createResinValidatorProducer();
        
        if (resinValidatorClass != null)
          cdiManager.addManagedBean(cdiManager.createManagedBean(resinValidatorClass));

        cdiManager.update();
      }

      _threadPoolAdmin = ThreadPoolAdmin.create();
      _resinAdmin = new ResinAdmin(this);

      _threadPoolAdmin.register();

      MemoryAdmin.create();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  protected void addServices()
  {
    ShutdownService shutdown = new ShutdownService(_resinSystem, _isEmbedded);
    _resinSystem.addService(shutdown);
  
    TopologyService topology = new TopologyService(_resinSystem.getId());
    _resinSystem.addService(topology);
    topology.getSystem();
  }
  
  private void setArgs(ResinArgs args)
  {
    _args = args;
  }

  /**
   * Returns the classLoader
   */
  public ClassLoader getClassLoader()
  {
    return _resinSystem.getClassLoader();
  }

  public ObjectName getObjectName()
  {
    return _objectName;
  }

  public ResinMXBean getAdmin()
  {
    return _resinAdmin;
  }

  /**
   * Returns the admin broker
   */
  public Broker getAdminBroker()
  {
    return _management.getAdminBroker();
  }

  public ThreadPoolMXBean getThreadPoolAdmin()
  {
    return _threadPoolAdmin;
  }

  protected String getLicenseMessage()
  {
    return null;
  }

  protected String getLicenseErrorMessage()
  {
    return _licenseErrorMessage;
  }

  /**
   * Sets the server id.
   */
  public void setServerId(String serverId)
  {
    Config.setProperty("serverId", serverId);

    _serverId = serverId;
    _serverIdLocal.set(serverId);
  }

  /**
   * Returns the server id.
   */
  public String getServerId()
  {
    return _serverId;
  }

  /**
   * Returns true for a Resin server, false for a watchdog.
   */
  public boolean isResinServer()
  {
    return ! _isWatchdog;
  }

  public boolean isWatchdog()
  {
    return _isWatchdog;
  }

  public String getUniqueServerName()
  {
    String name;

    if (_isWatchdog)
      name = _serverId + "_watchdog";
    else
      name = _serverId;

    name = name.replace('-', '_');

    return name;
  }

  public static String getCurrentServerId()
  {
    Resin resin = getCurrent();

    if (resin != null)
      return resin.getServerId();
    else
      return "";
  }

  /**
   * Sets the server id.
   */
  public void setDynamicServer(String clusterId, String address, int port)
  {
    String id = address + ":" + port;

    /*
    _dynCluster = clusterId;
    _dynAddress = address;
    _dynPort = port;
*/
    if (_serverId == null)
      setServerId(id);
  }

  /**
   * Returns the server id.
   */
  public String getDisplayServerId()
  {
    if ("".equals(_serverId))
      return "default";
    else
      return _serverId;
  }

  /**
   * Sets the config file.
   */
  public void setConfigFile(String configFile)
  {
  }

  /**
   * Sets resin.home
   */
  public void setResinHome(Path home)
  {
    _resinHome = home;
  }

  /**
   * Returns resin.home.
   */
  public Path getResinHome()
  {
    return _resinHome;
  }

  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Returns the resin-data directory
   */
  public Path getResinDataDirectory()
  {
    Path path;

    if (_resinDataDirectory != null)
      path = _resinDataDirectory;
    else if (_isWatchdog)
      path = getRootDirectory().lookup("watchdog-data");
    else
      path = getRootDirectory().lookup("resin-data");

    if (path instanceof MemoryPath) { // QA
      path = WorkDir.getTmpWorkDir().lookup("qa/resin-data");
    }

    return path;
  }

  /**
   * Sets the admin directory
   */
  public void setAdminPath(Path path)
  {
    // setResinDataDirectory(path);
  }

  /**
   * The configuration file used to start the server.
   */
  public Path getResinConf()
  {
    return _resinConf;
  }

  protected String getResinName()
  {
    return "Resin";
  }

  /**
   * Set true for Resin pro.
   */
  public boolean isProfessional()
  {
    return false;
  }
  
  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }
  
  public void setShutdownWaitTime(long shutdownTime)
  {
    _shutdownWaitMax = shutdownTime;
  }
  
  /**
   * Returns the cluster names.
   */
  public ClusterMXBean []getClusters()
  {
    /*
    ClusterMXBean []clusters = new ClusterMXBean[_clusters.size()];

    for (int i = 0; i < _clusters.size(); i++)
      clusters[i] = _clusters.get(i).getAdmin();

    return clusters;
    */
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the initial start time.
   */
  void setInitialStartTime(long now)
  {
    _initialStartTime = now;
  }

  /**
   * Returns the initial start time.
   */
  public Date getInitialStartTime()
  {
    return new Date(_initialStartTime);
  }

  /**
   * Returns the start time.
   */
  public Date getStartTime()
  {
    return new Date(_startTime);
  }

  /**
   * Returns the current lifecycle state.
   */
  public LifecycleState getLifecycleState()
  {
    return _lifecycle;
  }

  /**
   * Initialize the server.
   */
  @PostConstruct
  public void init()
  {
    preConfigureInit();

    _lifecycle.toInit();
  }

  /**
   * Returns the active server.
   */
  public Server getServer()
  {
    return _server;
  }

  /**
   * Returns the management api.
   */
  public Management getManagement()
  {
    return _management;
  }
  
  public double getCpuLoad()
  {
    return 0;
  }

  public Server createServer()
  {
    if (_server == null) {
      configure();

      // _server.start();
    }

    return _server;
  }

  protected ClusterServer loadDynamicServer(ClusterPod pod,
                                            String dynId,
                                            String dynAddress,
                                            int dynPort)
  {
    throw new ConfigException(L().l("dynamic-server requires Resin Professional"));
  }

  /**
   * Starts the server.
   */
  public void start()
    throws Exception
  {
    preConfigureInit();

    if (! _lifecycle.toActive())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_resinSystem.getClassLoader());

      // force a GC on start
      System.gc();
      
      initRepository();

      _server = createServer();

      if (_args != null) {
        for (BoundPort port : _args.getBoundPortList()) {
          _server.bind(port.getAddress(),
                       port.getPort(),
                       port.getServerSocket());
        }
      }

      _resinSystem.start();

      /*
        if (! hasListeningPort()) {
        log().warning(L().l("-server \"{0}\" has no matching http or srun ports.  Check the resin.xml and -server values.",
        _serverId));
        }
      */

      log().severe(this + " started in " + (Alarm.getExactTime() - _startTime) + "ms");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private void initRepository()
  {
    LocalRepositoryService localRepositoryService
      = new LocalRepositoryService();

    Repository localRepository = localRepositoryService.getRepository();

    _resinSystem.addService(localRepositoryService);

    Repository repository = createRepository(localRepository);
    
    _resinSystem.addService(new RepositoryService(repository));
  }
  
  protected Repository createRepository(Repository localRepository)
  {
    return localRepository;
  }

  /**
   * Starts the server.
   */
  public void stop()
  {
    _resinSystem.stop();
  }

  /**
   * Dump threads for debugging
   */
  public void dumpThreads()
  {
  }

  /**
   * Returns true if active.
   */
  public boolean isActive()
  {
    return _resinSystem.isActive();
  }

  /**
   * Returns true if the server is closing.
   */
  public boolean isClosing()
  {
    return _lifecycle.isDestroying();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isClosed()
  {
    return _lifecycle.isDestroyed();
  }

  public void destroy()
  {
    _resinSystem.destroy();
  }

  /**
   * Initialize the server, binding to TCP and starting the threads.
   */
  public void initMain()
    throws Throwable
  {
    _mainThread = Thread.currentThread();
    _mainThread.setContextClassLoader(_systemClassLoader);

    preConfigureInit();

    addRandom();

    System.out.println(VersionFactory.getFullVersion());
    System.out.println(VersionFactory.getCopyright());
    System.out.println();

    String licenseMessage = getLicenseMessage();

    if (licenseMessage != null) {
      log().warning(licenseMessage);
      System.out.println(licenseMessage);
    }

    String licenseErrorMessage = getLicenseErrorMessage();

    if (licenseErrorMessage != null) {
      log().warning(licenseErrorMessage);
      System.err.println(licenseErrorMessage);
    }

    System.out.println("Starting " + getResinName()
                       + " on " + QDate.formatLocal(_startTime));
    System.out.println();

    Environment.init();

    // buildResinClassLoader();

    // validateEnvironment();

    Thread thread = Thread.currentThread();

    thread.setContextClassLoader(_systemClassLoader);

    if (_rootDirectory == null)
      _rootDirectory = _resinHome;

    configure();

    start();
  }
  
  private void configure()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_resinSystem.getClassLoader());
      
      if (_server == null) {
        BootResinConfig bootResin = configureBoot();
  
        _rootDirectory = bootResin.getRootDirectory();
  
        configureRoot(bootResin);
  
        configureServer();
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Configures the boot structure, which parses the clusters and servers
   * for the system topology.
   */
  private BootResinConfig configureBoot()
  {
    Vfs.setPwd(_rootDirectory);
    
    Path resinConf = null;
    
    if (_args != null)
      resinConf = _args.getResinConfPath();

    _resinConf = resinConf;

    // server.setServerRoot(_serverRoot);

    Vfs.setPwd(getRootDirectory());

    if (resinConf != null)
      configureFile(resinConf);

    return _bootResinConfig;
  }
  
  public void configureFile(Path resinConf)
  {
    BootResinConfig bootResin = _bootResinConfig;
    
    Config config = new Config();
    // server/10hc
    // config.setResinInclude(true);

    config.configure(bootResin, resinConf, bootResin.getSchema());
  }
  
  public void configureProgram(ConfigProgram program)
  {
    program.configure(_bootResinConfig);
  }
  
  /**
   * Configures the root directory and dataDirectory.
   */
  private void configureRoot(BootResinConfig bootConfig)
    throws IOException
  {
    Path dataDirectory;
  
    if (isWatchdog())
      dataDirectory = _rootDirectory.lookup("watchdog-data");
    else
      dataDirectory = _rootDirectory.lookup("resin-data");
  
    String serverName = _serverId;
  
    if ("".equals(serverName))
      serverName = "default";
  
    dataDirectory = dataDirectory.lookup(serverName);
  
    RootDirectoryService rootService
      = new RootDirectoryService(_rootDirectory, dataDirectory);
    
    _resinSystem.addService(rootService);
  }
  
  /**
   * Configures the selected server from the boot config.
   */
  private void configureServer()
    throws IOException
  {
    if (_server != null)
      return;
    
    BootResinConfig bootResin = _bootResinConfig;
    
    bootResin.configureServers();
    
    BootServerConfig bootServer = bootResin.findServer(_serverId);
    
    if (bootServer == null) {
      BootClusterConfig clusterConfig = bootResin.findCluster("");
      
      if (clusterConfig != null) {
      }
      else if (bootResin.getClusterList().size() == 0) {
        clusterConfig = bootResin.createCluster();
        clusterConfig.setId("");
        clusterConfig.init();
      }
      else {
          throw new ConfigException(L().l("'{0}' is an unknown server in the configuration file.",
                                          _serverId));
      }
        
      if (clusterConfig.getServerList().size() > 0) {
        throw new ConfigException(L().l("'{0}' is an unknown server in the configuration file.",
                                        _serverId));
      }
      
      bootServer = clusterConfig.createServer();
      bootServer.setId("");
      bootServer.init();
      // bootServer.configureServer();
    }
    
    CloudServer cloudServer = bootServer.getCloudServer();
    
    NetworkClusterService networkService = new NetworkClusterService(cloudServer);
    _resinSystem.addService(networkService);
    
    ClusterServer server = cloudServer.getData(ClusterServer.class);
    
    LoadBalanceService loadBalanceService;
    loadBalanceService = new LoadBalanceService(createLoadBalanceFactory());
    
    _resinSystem.addService(loadBalanceService);
    
    BamService bamService = new BamService(server.getBamAdminName());
    _resinSystem.addService(bamService);
    
    _server = createServer(networkService);

    if (_stage != null)
      _server.setStage(_stage);
    
    NetworkListenService listenService = new NetworkListenService(cloudServer);
    
    _resinSystem.addService(listenService);
    
    ServletService servletService = new ServletService(_server);
    _resinSystem.addService(servletService);
    
    ResinConfig resinConfig = new ResinConfig(this);
    
    bootResin.getProgram().configure(resinConfig);

    bootServer.getCluster().getProgram().configure(_server);
    
    _server.init();
  }
  
  protected LoadBalanceFactory createLoadBalanceFactory()
  {
    return new LoadBalanceFactory();
  }
  
  protected Server createServer(NetworkClusterService clusterService)
  {
    return new Server(_resinSystem, clusterService);
  }

  public Management createResinManagement()
  {
    return new Management(this);
  }
  
  private void addRandom()
  {
  }

  public void dumpHeapOnExit()
  {

  }

  /**
   * Thread to wait until Resin should be stopped.
   */
  public void waitForExit()
    throws IOException
  {
    _waitForExitService = new ResinWaitForExitService(this, _resinSystem,
                                                      _waitIn, _pingSocket);
    
    _waitForExitService.startResinActor();
    
    _waitForExitService.waitForExit();
  }
  
  /**
   * Called from the embedded server
   */
  public void close()
  {
    log().info("Resin closed from the embedded server");
    
    _resinSystem.destroy();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + _serverId + "]";
  }

  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.xml   : alternate configuration file
   * -port port        : set the server's portt
   * <pre>
   */
  public static void main(String []argv)
  {
    try {
      Environment.init();

      validateEnvironment();
      
      final Resin resin = Resin.create();

      ResinArgs args = new ResinArgs(resin, argv);

      resin.setArgs(args);

      resin.initMain();

      resin.getServer();

      resin.waitForExit();

      ShutdownService.shutdownActive(ExitCode.OK,
                                     "Resin shutdown from watchdog exit");
    } catch (Throwable e) {
      Throwable cause;

      for (cause = e;
           cause != null && cause.getCause() != null;
           cause = cause.getCause()) {
        if (cause instanceof CompileException) {
          break;
        }
      }

      if (cause instanceof BindException) {
        System.err.println(e.getMessage());

        log().severe(e.toString());

        log().log(Level.FINE, e.toString(), e);

        System.exit(ExitCode.BIND.ordinal());
      }
      else if (e instanceof CompileException) {
        System.err.println(e.getMessage());

        log().log(Level.CONFIG, e.toString(), e);
      }
      else {
        e.printStackTrace(System.err);
      }
    } finally {
      System.exit(ExitCode.BAD_CONFIG.ordinal());
    }
  }

  /**
   * Validates the environment.
   */
  private static void validateEnvironment()
    throws ConfigException
  {
    String loggingManager = System.getProperty("java.util.logging.manager");

    if (loggingManager == null
        || ! loggingManager.equals("com.caucho.log.LogManagerImpl")) {
      log().warning(L().l("The following system property must be set:\n  -Djava.util.logging.manager=com.caucho.log.LogManagerImpl\nThe JDK 1.4 Logging manager must be set to Resin's log manager."));
    }

    /*
    validatePackage("javax.servlet.Servlet", new String[] {"2.5", "1.5"});
    validatePackage("javax.servlet.jsp.jstl.core.Config", new String[] {"1.1"});
    validatePackage("javax.management.MBeanServer", new String[] { "1.2", "1.5" });
    validatePackage("javax.resource.spi.ResourceAdapter", new String[] {"1.5", "1.4"});
    */
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(Resin.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(Resin.class.getName());

    return _log;
  }

  /**
   * EL variables
   */
  public class Var {
    /**
     * Returns the resin.id
     */
    public String getId()
    {
      return _serverId;
    }

    /**
     * Returns the local address
     *
     * @return IP address
     */
    public String getAddress()
    {
      try {
        Server server = _server;

        if (server != null) {
          ClusterServer clusterServer = server.getSelfServer();

          return clusterServer.getAddress();
        }
        else
          return InetAddress.getLocalHost().getHostAddress();
      } catch (Exception e) {
        log().log(Level.FINE, e.toString(), e);

        return "localhost";
      }
    }

    /**
     * Returns the port (backward compat)
     */
    public int getPort()
    {
      Server server = _server;

      if (server != null) {
        ClusterServer clusterServer = server.getSelfServer();

        return clusterServer.getPort();
      }
      else
        return 0;
    }

    /**
     * Returns the port (backward compat)
     */
    public String getHttpAddress()
    {
      return getAddress();
    }

    /**
     * Returns the port (backward compat)
     */
    public String getHttpsAddress()
    {
      return getAddress();
    }

    /**
     * Returns the port (backward compat)
     */
    public int getHttpPort()
    {
      return 0;
    }

    /**
     * Returns the port (backward compat)
     */
    public int getHttpsPort()
    {
      return 0;
    }

    /**
     * Returns the resin config.
     */
    public Path getConf()
    {
      if (Alarm.isTest())
        return Vfs.lookup("file:/home/resin/conf/resin.xml");
      else
        return getResinConf();
    }

    /**
     * Returns the resin home.
     */
    public Path getHome()
    {
      if (Alarm.isTest())
        return Vfs.lookup("file:/home/resin");
      else
        return Resin.this.getResinHome();
    }

    /**
     * Returns the root directory.
     *
     * @return the root directory
     */
    public Path getRoot()
    {
      /*
      if (Alarm.isTest())
        return Vfs.lookup("file:/var/www");
      else
        return Resin.this.getRootDirectory();
        */
      return Resin.this.getRootDirectory();
    }

    public String getUserName()
    {
      return System.getProperty("user.name");
    }

    /**
     * Returns the version
     *
     * @return version
     */
    public String getVersion()
    {
      if (Alarm.isTest())
        return "3.1.test";
      else
        return VersionFactory.getVersion();
    }

    /**
     * Returns the version date
     *
     * @return version
     */
    public String getVersionDate()
    {
      if (Alarm.isTest())
        return "19980508T0251";
      else
        return VersionFactory.getVersionDate();
    }

    /**
     * Returns the local hostname
     *
     * @return version
     */
    public String getHostName()
    {
      try {
        if (Alarm.isTest())
          return "localhost";
        else
          return InetAddress.getLocalHost().getHostName();
      } catch (Exception e) {
        log().log(Level.FINE, e.toString(), e);

        return "localhost";
      }
    }

    /**
     * Returns the root directory.
     *
     * @return resin.home
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * Returns the root directory.
     *
     * @return resin.home
     */
    public Path getRootDirectory()
    {
      return getRoot();
    }

    /**
     * Returns true for Resin professional.
     */
    public boolean isProfessional()
    {
      return Resin.this.isProfessional();
    }

    /**
     * Returns the -server id
     */
    public String getServerId()
    {
      return _serverId;
    }
  }

  /**
   * Java variables
   */
  public class JavaVar {
    /**
     * Returns true for JDK 5
     */
    public boolean isJava5()
    {
      return true;
    }

    /**
     * Returns the JDK properties
     */
    public Properties getProperties()
    {
      return System.getProperties();
    }

    /**
     * Returns the user name
     */
    public String getUserName()
    {
      return System.getProperty("user.name");
    }

    /**
     * Returns the JDK version
     */
    public String getVersion()
    {
      return System.getProperty("java.version");
    }

    /**
     * Returns the JDK home
     */
    public Path getHome()
    {
      return Vfs.lookup(System.getProperty("java.home"));
    }
  }
}
