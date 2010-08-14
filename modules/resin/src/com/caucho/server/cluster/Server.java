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

package com.caucho.server.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import com.caucho.VersionFactory;
import com.caucho.bam.ActorClient;
import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.bam.SimpleActorClient;
import com.caucho.cloud.bam.BamService;
import com.caucho.cloud.deploy.DeployNetworkService;
import com.caucho.cloud.loadbalance.CustomLoadBalanceManager;
import com.caucho.cloud.loadbalance.LoadBalanceManager;
import com.caucho.cloud.loadbalance.SingleLoadBalanceManager;
import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.network.NetworkClusterService;
import com.caucho.cloud.network.NetworkListenService;
import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.distcache.ClusterCache;
import com.caucho.distcache.GlobalCache;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownService;
import com.caucho.env.thread.ThreadPool;
import com.caucho.hemp.broker.HempBrokerManager;
import com.caucho.hemp.servlet.ServerAuthManager;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.AlwaysModified;
import com.caucho.management.server.CacheItem;
import com.caucho.management.server.EnvironmentMXBean;
import com.caucho.management.server.ServerMXBean;
import com.caucho.network.listen.AbstractSelectManager;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.security.AdminAuthenticator;
import com.caucho.security.PermissionManager;
import com.caucho.server.admin.Management;
import com.caucho.server.cache.AbstractCache;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationMatcher;
import com.caucho.server.dispatch.ProtocolDispatchServer;
import com.caucho.server.distcache.DistributedCacheManager;
import com.caucho.server.distcache.FileCacheManager;
import com.caucho.server.distcache.PersistentStoreConfig;
import com.caucho.server.distlock.AbstractLockManager;
import com.caucho.server.distlock.AbstractVoteManager;
import com.caucho.server.distlock.SingleLockManager;
import com.caucho.server.distlock.SingleVoteManager;
import com.caucho.server.e_app.EarConfig;
import com.caucho.server.host.Host;
import com.caucho.server.host.HostConfig;
import com.caucho.server.host.HostContainer;
import com.caucho.server.host.HostController;
import com.caucho.server.host.HostExpandDeployGenerator;
import com.caucho.server.log.AccessLog;
import com.caucho.server.resin.Resin;
import com.caucho.server.rewrite.RewriteDispatch;
import com.caucho.server.webapp.ErrorPage;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.Vfs;

public class Server extends ProtocolDispatchServer
  implements EnvironmentBean, SchemaBean, AlarmListener,
             ClassLoaderListener
{
  private static final L10N L = new L10N(Server.class);
  private static final Logger log
    = Logger.getLogger(Server.class.getName());

  private static final long ALARM_INTERVAL = 60000;

  private static final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  private static final EnvironmentLocal<Server> _serverLocal
    = new EnvironmentLocal<Server>();

  private final Resin _resin;
  private final ResinSystem _resinSystem;
  private final NetworkClusterService _clusterService;
  private final ClusterServer _selfServer;

  private NetworkListenService _listenService;

  private Throwable _configException;

  private ServerAuthManager _authManager;
  private AdminAuthenticator _adminAuth;

  private InjectManager _cdiManager;

  private BamService _bamService;
  private PersistentStoreConfig _persistentStoreConfig;

  private DistributedCacheManager _distributedCacheManager;
  private AbstractLockManager _distributedLockManager;
  private AbstractVoteManager _distributedVoteManager;

  private HostContainer _hostContainer;

  private String _stage = "default";
  private boolean _isPreview;

  private String _serverHeader;

  private int _urlLengthMax = 8192;

  private long _waitForActiveTime = 10000L;

  private boolean _isDevelopmentModeErrorPage;

  private Management _management;

  private long _memoryFreeMin = 1024 * 1024;
  private long _permGenFreeMin = 1024 * 1024;

  private long _shutdownWaitMax = 60 * 1000;

  private int _threadMax = 4096;
  private int _threadExecutorTaskMax = -1;
  private int _threadIdleMin = -1;
  private int _threadIdleMax = -1;

  // <cluster> configuration

  private String _connectionErrorPage;

  private ServerAdmin _admin;

  private Alarm _alarm;
  private AbstractCache _cache;

  //
  // internal databases
  //

  // reliable system store
  private ClusterCache _systemStore;
  private GlobalCache _globalStore;

  // stats

  private long _startTime;

  private final Lifecycle _lifecycle;

  /**
   * Creates a new servlet server.
   */
  public Server(ResinSystem resinSystem,
                NetworkClusterService clusterService)
  {
    if (resinSystem == null)
      throw new NullPointerException();
    
    if (clusterService == null)
      throw new NullPointerException();
    
    _resinSystem = resinSystem;
    _clusterService = clusterService;
    
    _selfServer = _clusterService.getSelfServer().getData(ClusterServer.class);

    _resin = Resin.getCurrent();

    // pod id can't include the server since it's used as part of
    // cache ids
    //String podId
    //  = (cluster.getId() + ":" + _selfServer.getClusterPod().getId());

    String id = _selfServer.getId();
    
    if ("".equals(id))
      id = "default";
    
    // cannot set the based on server-id because of distributed cache
    // _classLoader.setId("server:" + id);

    _serverLocal.set(this, getClassLoader());

    if (! Alarm.isTest())
      _serverHeader = "Resin/" + VersionFactory.getVersion();
    else
      _serverHeader = "Resin/1.1";

    try {
      Thread thread = Thread.currentThread();

      Environment.addClassLoaderListener(this, getClassLoader());

      PermissionManager permissionManager = new PermissionManager();
      PermissionManager.setPermissionManager(permissionManager);

      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(getClassLoader());

        _serverIdLocal.set(_selfServer.getId());
        
        _lifecycle = new Lifecycle(log, toString(), Level.INFO);
        
        preInit();
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      // exceptions here must throw to the top because they're non-recoverable
      throw ConfigException.create(e);
    }
  }
  
  protected void preInit()
  {
    _cdiManager = InjectManager.create();
    
    _hostContainer = new HostContainer();
    _hostContainer.setClassLoader(getClassLoader());
    _hostContainer.setDispatchServer(this);

    _alarm = new Alarm(this);
    
    _bamService = BamService.create(getBamAdminName());

    _authManager = new ServerAuthManager();
    // XXX:
    _authManager.setAuthenticationRequired(false);
    _bamService.setLinkManager(_authManager);
    
    Config.setProperty("server", new ServerVar(_selfServer), getClassLoader());
    Config.setProperty("cluster", new ClusterVar(), getClassLoader());

    _distributedCacheManager = createDistributedCacheManager();
    
    _resinSystem.addService(new DeployNetworkService());
    
    // _selfServer.getServerProgram().configure(this);
  }

  /**
   * Returns the current server
   */
  public static Server getCurrent()
  {
    return _serverLocal.get();
  }
  
  public ResinSystem getResinSystem()
  {
    return _resinSystem;
  }
  
  public NetworkListenService getListenService()
  {
    if (_listenService == null)
      _listenService = NetworkListenService.getCurrent();
    
    return _listenService;
  }
  
  public boolean isResinServer()
  {
    if (_resin != null)
      return _resin.isResinServer();
    else
      return false;
  }

  public String getUniqueServerName()
  {
    return _resin.getUniqueServerName();
  }

  /**
   * Returns the classLoader
   */
  @Override
  public EnvironmentClassLoader getClassLoader()
  {
    return _resinSystem.getClassLoader();
  }

  /**
   * Returns the configuration exception
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns the configuration instance.
   */
  public void setConfigException(Throwable exn)
  {
    _configException = exn;
  }

  /**
   * Returns the resin server
   */
  public Resin getResin()
  {
    return _resin;
  }

  /**
   * Returns true for the watchdog server.
   */
  public boolean isWatchdog()
  {
    return getResin().isWatchdog();
  }

  /**
   * Returns the cluster
   */
  public CloudCluster getCluster()
  {
    return _selfServer.getCluster();
  }

  /**
   * Returns all the clusters
   */
  public CloudCluster []getClusterList()
  {
    return _selfServer.getCluster().getSystem().getClusterList();
  }

  /**
   * Returns the admin path
   */
  public Path getResinDataDirectory()
  {
    return _resin.getResinDataDirectory();
  }

  /**
   * Creates the bam broker manager
   */
  protected HempBrokerManager createBrokerManager()
  {
    return new HempBrokerManager();
  }

  /**
   * Returns the cluster server
   */
  protected ClusterServer getClusterServer()
  {
    return getSelfServer();
  }

  /**
   * Returns the self server
   */
  public ClusterServer getSelfServer()
  {
    return _selfServer;
  }

  /**
   * Returns the self server's pod
   */
  public CloudPod getPod()
  {
    return _selfServer.getCloudServer().getPod();
  }

  /**
   * Returns the distributed cache manager
   */
  public DistributedCacheManager getDistributedCacheManager()
  {
    return _distributedCacheManager;
  }

  /**
   * Returns the distributed cache manager
   */
  protected DistributedCacheManager createDistributedCacheManager()
  {
    return new FileCacheManager(this);
  }

  /**
   * Returns the distributed lock manager
   */
  public AbstractLockManager getDistributedLockManager()
  {
    if (_distributedLockManager == null)
      _distributedLockManager = createDistributedLockManager();

    return _distributedLockManager;
  }

  /**
   * Returns the distributed cache manager
   */
  protected AbstractLockManager createDistributedLockManager()
  {
    return new SingleLockManager(this);
  }

  /**
   * Returns the distributed vote manager
   */
  public AbstractVoteManager getDistributedVoteManager()
  {
    if (_distributedVoteManager == null)
      _distributedVoteManager = createDistributedVoteManager();

    return _distributedVoteManager;
  }

  /**
   * Returns the distributed vote manager
   */
  protected AbstractVoteManager createDistributedVoteManager()
  {
    return new SingleVoteManager(this);
  }

  /**
   * Returns the bam broker.
   */
  public Broker getBamBroker()
  {
    return _bamService.getBroker();
  }

  /**
   * Returns the stream to the public broker.
   */
  public ActorStream getBamStream()
  {
    return getBamBroker().getBrokerStream();
  }

  /**
   * Returns the bam broker.
   */
  public Broker getAdminBroker()
  {
    return getBamBroker();
  }
  
  /**
   * Creates a bam client to the admin.
   */
  public ActorClient createAdminClient(String uid)
  {
    return new SimpleActorClient(getAdminBroker(), uid, null);
  }

  /**
   * Returns the stream to the admin broker.
   */
  public ActorStream getAdminStream()
  {
    return getAdminBroker().getBrokerStream();
  }

  /**
   * Returns the bam name.
   */
  public String getBamAdminName()
  {
    return getClusterServer().getBamAdminName();
  }

  /**
   * Returns the admin broker.
   */
  public Broker getBroker()
  {
    return _bamService.getBroker();
  }

  public AdminAuthenticator getAdminAuthenticator()
  {
    if (_adminAuth == null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        thread.setContextClassLoader(getClassLoader());

        _adminAuth = _cdiManager.getReference(AdminAuthenticator.class);
      } catch (Exception e) {
        if (log.isLoggable(Level.FINEST))
          log.log(Level.FINEST, e.toString(), e);
        else
          log.finer(e.toString());

        _adminAuth = new AdminAuthenticator();
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }

    return _adminAuth;
  }
  
  //
  // <cluster>
  //

  /**
   * Development mode error pages.
   */
  public boolean isDevelopmentModeErrorPage()
  {
    return _isDevelopmentModeErrorPage;
  }

  /**
   * Development mode error pages.
   */
  public void setDevelopmentModeErrorPage(boolean isEnable)
  {
    _isDevelopmentModeErrorPage = isEnable;
  }

  /**
   * Creates a persistent store instance.
   */
  public PersistentStoreConfig createPersistentStore()
  {
    if (_persistentStoreConfig == null)
      _persistentStoreConfig = new PersistentStoreConfig();

    return _persistentStoreConfig;
  }

  /**
   * Creates a persistent store instance.
   */
  public PersistentStoreConfig getPersistentStoreConfig()
  {
    return _persistentStoreConfig;
  }

  public void startPersistentStore()
  {
  }

  public Object createJdbcStore()
    throws ConfigException
  {
    return null;
  }

  /**
   * Arguments on boot
   */
  public void addJavaExe(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addJvmArgLine(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addJvmClasspath(String args)
  {
  }

  /**
   * Sets the stage id
   */
  public void setStage(String stage)
  {
    if (stage == null || "".equals(stage))
      _stage = "default";
    else
      _stage = stage;

    _isPreview = "preview".equals(_stage);
  }

  /**
   * Returns the stage id
   */
  public String getStage()
  {
    return _stage;
  }

  /**
   * Returns true in preview mode
   */
  public boolean isPreview()
  {
    return _isPreview;
  }

  /**
   * The Resin system classloader
   */
  public void setSystemClassLoader(String loader)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogJvmArg(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogLog(ConfigProgram args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogPassword(String args)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogPort(int port)
  {
  }

  /**
   * Arguments on boot
   */
  public void addWatchdogAddress(String addr)
  {
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public void setMemoryFreeMin(Bytes min)
  {
    _memoryFreeMin = min.getBytes();
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public long getMemoryFreeMin()
  {
    return _memoryFreeMin;
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public void setPermGenFreeMin(Bytes min)
  {
    _permGenFreeMin = min.getBytes();
  }

  /**
   * Sets the minimum free memory after a GC
   */
  public long getPermGenFreeMin()
  {
    return _permGenFreeMin;
  }

  public Management createManagement()
  {
    if (_resin != null)
      return _resin.createResinManagement();
    else
      return null;
    
    /*
    if (_management == null && _resin != null) {
      _management = _resin.createResinManagement();

      // _management.setCluster(getCluster());
    }

    return _management;
    */
  }

  /**
   * Sets the redeploy mode
   */
  public void setRedeployMode(String redeployMode)
  {
  }

  /**
   * Sets the max wait time for shutdown.
   */
  public void setShutdownWaitMax(Period waitTime)
  {
    _shutdownWaitMax = waitTime.getPeriod();
  }

  /**
   * Gets the max wait time for a shutdown.
   */
  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }

  /**
   * Sets the maximum thread-based keepalive
   */
  public void setThreadMax(int max)
  {
    if (max < 0)
      throw new ConfigException(L.l("<thread-max> ({0}) must be greater than zero.",
                                    max));

    _threadMax = max;
  }

  /**
   * Sets the maximum executor (background) thread.
   */
  public void setThreadExecutorTaskMax(int max)
  {
    _threadExecutorTaskMax = max;
  }

  /**
   * Sets the minimum number of idle threads in the thread pool.
   */
  public void setThreadIdleMin(int min)
  {
    _threadIdleMin = min;
  }

  /**
   * Sets the maximum number of idle threads in the thread pool.
   */
  public void setThreadIdleMax(int max)
  {
    _threadIdleMax = max;
  }

  //
  // Configuration from <cluster>
  //

  /**
   * Sets the connection error page.
   */
  public void setConnectionErrorPage(String errorPage)
  {
    _connectionErrorPage = errorPage;
  }

  /**
   * Gets the connection error page.
   */
  public String getConnectionErrorPage()
  {
    return _connectionErrorPage;
  }

  /**
   * Return true if idle.
   */
  public boolean isDeployError()
  {
    return _configException != null;
  }

  /**
   * Returns the relax schema.
   */
  @Override
  public String getSchema()
  {
    return "com/caucho/server/resin/cluster.rnc";
  }

  /**
   * Returns the id.
   */
  @Override
  public String getServerId()
  {
    return _selfServer.getId();
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path path)
  {
    _hostContainer.setRootDirectory(path);

    Vfs.setPwd(path, getClassLoader());
  }

  /**
   * Sets the root directory.
   */
  public Path getRootDirectory()
  {
    return _hostContainer.getRootDirectory();
  }

  /**
   * Sets the root directory.
   */
  public void setRootDir(Path path)
  {
    setRootDirectory(path);
  }

  /**
   * Sets the server header.
   */
  public void setServerHeader(String server)
  {
    _serverHeader = server;
  }

  /**
   * Gets the server header.
   */
  public String getServerHeader()
  {
    return _serverHeader;
  }

  /**
   * Sets the url-length-max
   */
  public void setUrlLengthMax(int max)
  {
    _urlLengthMax = max;
  }

  /**
   * Gets the url-length-max
   */
  public int getUrlLengthMax()
  {
    return _urlLengthMax;
  }

  /**
   * Adds a WebAppDefault.
   */
  public void addWebAppDefault(WebAppConfig init)
  {
    _hostContainer.addWebAppDefault(init);
  }

  /**
   * Adds an EarDefault
   */
  public void addEarDefault(EarConfig config)
  {
    _hostContainer.addEarDefault(config);
  }

  /**
   * Adds a HostDefault.
   */
  public void addHostDefault(HostConfig init)
  {
    _hostContainer.addHostDefault(init);
  }

  /**
   * Adds a HostDeploy.
   */
  public HostExpandDeployGenerator createHostDeploy()
  {
    return _hostContainer.createHostDeploy();
  }

  /**
   * Adds a HostDeploy.
   */
  public void addHostDeploy(HostExpandDeployGenerator deploy)
  {
    _hostContainer.addHostDeploy(deploy);
  }

  /**
   * Adds the host.
   */
  public void addHost(HostConfig host)
  {
    _hostContainer.addHost(host);
  }

  /**
   * Adds rewrite-dispatch.
   */
  public RewriteDispatch createRewriteDispatch()
  {
    return _hostContainer.createRewriteDispatch();
  }

  public AbstractCache getProxyCache()
  {
    return _cache;
  }

  /**
   * Creates the proxy cache.
   */
  public AbstractCache createProxyCache()
    throws ConfigException
  {
    log.warning(L.l("<proxy-cache> requires Resin Professional.  Please see http://www.caucho.com for Resin Professional information and licensing."));

    if (_cache == null)
      _cache = instantiateProxyCache();

    return _cache;
  }

  /**
   * backward compatibility for proxy cache
   */
  public AbstractCache createCache()
    throws ConfigException
  {
    return createProxyCache();
  }
  
  protected AbstractCache instantiateProxyCache()
  {
    return new AbstractCache();
  }

  /**
   * Sets the access log.
   */
  public void setAccessLog(AccessLog log)
  {
    Environment.setAttribute("caucho.server.access-log", log);
  }

  /**
   * Returns the dependency check interval.
   */
  public long getDependencyCheckInterval()
  {
    return Environment.getDependencyCheckInterval(getClassLoader());
  }

  /**
   * Sets the session cookie
   */
  public void setSessionCookie(String cookie)
  {
    getInvocationDecoder().setSessionCookie(cookie);
  }

  /**
   * Gets the session cookie
   */
  public String getSessionCookie()
  {
    return getInvocationDecoder().getSessionCookie();
  }

  /**
   * Sets the ssl session cookie
   */
  public void setSSLSessionCookie(String cookie)
  {
    getInvocationDecoder().setSSLSessionCookie(cookie);
  }

  /**
   * Gets the ssl session cookie
   */
  public String getSSLSessionCookie()
  {
    return getInvocationDecoder().getSSLSessionCookie();
  }

  /**
   * Sets the session url prefix.
   */
  public void setSessionURLPrefix(String urlPrefix)
  {
    getInvocationDecoder().setSessionURLPrefix(urlPrefix);
  }

  /**
   * Gets the session url prefix.
   */
  public String getSessionURLPrefix()
  {
    return getInvocationDecoder().getSessionURLPrefix();
  }

  /**
   * Sets the alternate session url prefix.
   */
  public void setAlternateSessionURLPrefix(String urlPrefix)
    throws ConfigException
  {
    getInvocationDecoder().setAlternateSessionURLPrefix(urlPrefix);
  }

  /**
   * Gets the alternate session url prefix.
   */
  public String getAlternateSessionURLPrefix()
  {
    return getInvocationDecoder().getAlternateSessionURLPrefix();
  }

  /**
   * Sets URL encoding.
   */
  public void setURLCharacterEncoding(String encoding)
    throws ConfigException
  {
    getInvocationDecoder().setEncoding(encoding);
  }

  /**
   * Creates the ping.
   */
  public Object createPing()
    throws ConfigException
  {
    return createManagement().createPing();
  }

  /**
   * Adds the ping.
   */
  /*
  public void addPing(Object ping)
    throws ConfigException
  {
    createManagement().addPing(ping);
  }
  */

  /**
   * Sets true if the select manager should be enabled
   */
  @Override
  public boolean isSelectManagerEnabled()
  {
    return getSelectManager() != null;
  }

  public void addSelectManager(SelectManagerCompat selectManager)
  {

  }

  /**
   * Returns the number of select keepalives available.
   */
  public int getFreeKeepaliveSelect()
  {
    AbstractSelectManager selectManager = getSelectManager();

    if (selectManager != null)
      return selectManager.getFreeKeepalive();
    else
      return Integer.MAX_VALUE / 2;
  }

  /**
   * Adds an error page
   */
  public void addErrorPage(ErrorPage errorPage)
  {
    getErrorWebApp().addErrorPage(errorPage);
  }

  //
  // cluster server information
  //
  public int getServerIndex()
  {
    return _selfServer.getIndex();
  }

  //
  // statistics
  //

  /**
   * Returns the time the server started in ms.
   */
  public long getStartTime()
  {
    return _startTime;
  }

  /**
   * Returns the lifecycle state
   */
  public String getState()
  {
    return _lifecycle.getStateName();
  }

  /**
   * Returns the select keepalive count.
   */
  public int getKeepaliveSelectCount()
  {
    AbstractSelectManager selectManager = getSelectManager();

    if (selectManager != null)
      return selectManager.getSelectCount();
    else
      return -1;
  }

  /**
   * Returns the cache stuff.
   */
  public ArrayList<CacheItem> getCacheStatistics()
  {
    ArrayList<Invocation> invocationList = getInvocations();

    if (invocationList == null)
      return null;

    HashMap<String,CacheItem> itemMap = new HashMap<String,CacheItem>();

    for (int i = 0; i < invocationList.size(); i++) {
      Invocation inv = (Invocation) invocationList.get(i);

      String uri = inv.getURI();
      int p = uri.indexOf('?');
      if (p >= 0)
        uri = uri.substring(0, p);

      CacheItem item = itemMap.get(uri);

      if (item == null) {
        item = new CacheItem();
        item.setUrl(uri);

        itemMap.put(uri, item);
      }
    }

    return null;
  }

  public double getCpuLoad()
  {
    return 0;
  }

  //
  // runtime operations
  //

  /**
   * Sets the invocation
   */
  public Invocation buildInvocation(Invocation invocation)
    throws ConfigException
  {
    if (_configException != null) {
      invocation.setFilterChain(new ExceptionFilterChain(_configException));
      invocation.setWebApp(getErrorWebApp());
      invocation.setDependency(AlwaysModified.create());

      return invocation;
    }
    else if (_lifecycle.waitForActive(_waitForActiveTime)) {
      return _hostContainer.buildInvocation(invocation);
    }
    else {
      int code = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

      invocation.setFilterChain(new ErrorFilterChain(code));
      invocation.setWebApp(getErrorWebApp());
      invocation.setDependency(AlwaysModified.create());

      return invocation;
    }
  }

  /**
   * Returns the matching servlet pattern for a URL.
   */
  public String getServletPattern(String hostName, int port, String url)
  {
    try {
      Host host = _hostContainer.getHost(hostName, port);

      if (host == null)
        return null;

      WebApp app = host.findWebAppByURI(url);

      if (app == null)
        return null;

      String pattern = app.getServletPattern(url);

      return pattern;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the admin.
   */
  public ServerMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Returns the admin for the current environment.
   */
  public EnvironmentMXBean getEnvironmentAdmin()
  {
    return getClassLoader().getAdmin();
  }

  /**
   * Returns the default web-app or error web-app for top-level errors
   */
  public WebApp getDefaultWebApp()
  {
    WebApp webApp = getWebApp("", 80, "");

    if (webApp != null)
      return webApp;
    else
      return getErrorWebApp();
  }

  /**
   * Returns the matching web-app for a URL.
   */
  public WebApp getWebApp(String hostName, int port, String url)
  {
    try {
      HostContainer hostContainer = _hostContainer;

      if (hostContainer == null)
        return null;

      Host host = hostContainer.getHost(hostName, port);

      if (host == null)
        return null;

      return host.findWebAppByURI(url);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the error webApp during startup.
   */
  public WebApp getErrorWebApp()
  {
    HostContainer hostContainer = _hostContainer;

    if (hostContainer != null)
      return hostContainer.getErrorWebApp();
    else
      return null;
  }

  /**
   * Returns the host controllers.
   */
  public Collection<HostController> getHostControllers()
  {
    HostContainer hostContainer = _hostContainer;

    if (hostContainer == null)
      return Collections.emptyList();

    return Collections.unmodifiableList(hostContainer.getHostList());
  }

  /**
   * Returns the matching servlet pattern for a URL.
   */
  public Host getHost(String hostName, int port)
  {
    try {
      return _hostContainer.getHost(hostName, port);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the {@link SocketLinkListener}s for this server.
   */
  public Collection<SocketLinkListener> getPorts()
  {
    return getListenService().getListeners();
  }

  /**
   * Returns the reliable system store
   */
  public ClusterCache getSystemStore()
  {
    synchronized (this) {
      if (_systemStore == null) {
        _systemStore = new ClusterCache("resin:system");
        _systemStore.setGuid("resin:system");
        // XXX: need to set reliability values
      }
    }

    _systemStore.init();

    return _systemStore;
  }

  /**
   * Returns the reliable system store
   */
  public GlobalCache getGlobalStore()
  {
    synchronized (this) {
      if (_globalStore == null) {
        _globalStore = new GlobalCache();
        _globalStore.setName("resin:global");
        _globalStore.setGuid("resin:global");
        _globalStore.setLocalReadTimeoutMillis(60000);
        // XXX: need to set reliability values
      }
    }

    _globalStore.init();

    return _globalStore;
  }

  /**
   * Handles the case where a class loader is activated.
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
    try {
      //Jmx.register(_controller.getThreadPool(), "resin:type=ThreadPool");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    /*
    try {
      Jmx.unregister("resin:name=default,type=Server");
      Jmx.unregister("resin:type=ThreadPool");
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }
    */
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
  {
    if (! _lifecycle.toInit())
      return;
    
    // _classLoader.init();

    super.init();

    _admin = new ServerAdmin(this);

    /*
    if (_resin != null) {
      createManagement().setCluster(getCluster());
      createManagement().setServer(this);
      createManagement().init();
    }
    */

    if (_threadIdleMax > 0
        && _threadMax > 0
        && _threadMax < _threadIdleMax)
      throw new ConfigException(L.l("<thread-idle-max> ({0}) must be less than <thread-max> ({1})",
                                    _threadIdleMax, _threadMax));

    if (_threadIdleMin > 0
        && _threadIdleMax > 0
        && _threadIdleMax < _threadIdleMin)
      throw new ConfigException(L.l("<thread-idle-min> ({0}) must be less than <thread-idle-max> ({1})",
                                    _threadIdleMin, _threadIdleMax));

    if (_threadMax > 0
        && _threadExecutorTaskMax > 0
        && _threadMax < _threadExecutorTaskMax)
      throw new ConfigException(L.l("<thread-executor-task-max> ({0}) must be less than <thread-max> ({1})",
                                    _threadExecutorTaskMax, _threadMax));

    ThreadPool threadPool = ThreadPool.getThreadPool();

    if (_threadMax > 0)
      threadPool.setThreadMax(_threadMax);

    /*
    if (_threadIdleMax > 0)
      threadPool.setThreadIdleMax(_threadIdleMax);
      */

    if (_threadIdleMin > 0)
      threadPool.setIdleMin(_threadIdleMin);

    threadPool.setExecutorTaskMax(_threadExecutorTaskMax);

    /*
    if (_keepaliveSelectEnable) {
      try {
        Class<?> cl = Class.forName("com.caucho.server.connection.JniSelectManager");
        Method method = cl.getMethod("create", new Class[0]);

        initSelectManager((AbstractSelectManager) method.invoke(null, null));
      } catch (ClassNotFoundException e) {
        log.warning(L.l("'select-manager' requires Resin Professional.  See http://www.caucho.com for information and licensing."));
      } catch (Throwable e) {
        log.warning(L.l("Cannot enable select-manager {0}", e.toString()));

        log.log(Level.FINER, e.toString());
      }

      if (getSelectManager() != null) {
        if (_keepaliveSelectMax > 0)
          getSelectManager().setSelectMax(_keepaliveSelectMax);
      }
    }
    */
  }

  /**
   * Start the server.
   */
  public void start()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStarting())
        return;

      _startTime = Alarm.getCurrentTime();

      if (! Alarm.isTest()) {
        /*
        log.info("");

        log.info(System.getProperty("os.name")
                 + " " + System.getProperty("os.version")
                 + " " + System.getProperty("os.arch"));

        log.info(System.getProperty("java.runtime.name")
                 + " " + System.getProperty("java.runtime.version")
                 + ", " + System.getProperty("file.encoding")
                 + ", " + System.getProperty("user.language"));

        log.info(System.getProperty("java.vm.name")
                 + " " + System.getProperty("java.vm.version")
                 + ", " + System.getProperty("sun.arch.data.model")
                 + ", " + System.getProperty("java.vm.info")
                 + ", " + System.getProperty("java.vm.vendor"));
                 */

        log.info("");

        Resin resin = Resin.getCurrent();

        if (resin != null) {
          log.info("resin.home = " + resin.getResinHome().getNativePath());
          log.info("resin.root = " + resin.getRootDirectory().getNativePath());
          if (resin.getResinConf() != null)
            log.info("resin.conf = " + resin.getResinConf().getNativePath());

          log.info("");

          String serverType;

          if (resin.isWatchdog())
            serverType = "watchdog";
          else
            serverType = "server";

          log.info(serverType + "    = "
                   + _selfServer.getAddress()
                   + ":" + _selfServer.getPort()
                   + " (" + getCluster().getId()
                   + ":" + getServerId() + ")");
        }
        else {
          log.info("resin.home = " + System.getProperty("resin.home"));
        }

        // log.info("user.name  = " + System.getProperty("user.name"));
        log.info("stage      = " + _stage);
      }

      _lifecycle.toStarting();

      startImpl();

      if (_resin != null && _resin.getManagement() != null)
        _resin.getManagement().start(this);

      // initialize the system distributed store
      if (isResinServer())
        getSystemStore();

      // start the repository
      // AbstractRepository repository = getRepository();
      // if (repository != null)
      //   repository.start();

      // getCluster().start();

      // handled by the network server itself
      // _classLoader.start();

      _hostContainer.start();

      // initialize the environment admin
      // _classLoader.getAdmin();

      startPersistentStore();

      // getCluster().startRemote();

      _alarm.queue(ALARM_INTERVAL);

      _lifecycle.toActive();

      // dynamic updates from the cluster start after we're capable of
      // handling messages
      startClusterUpdate();

      logModules();
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      throw e;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      // if the server can't start, it needs to completely fail, especially
      // for the watchdog
      throw new RuntimeException(e);

      // log.log(Level.WARNING, e.toString(), e);

      // _configException = e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  protected void startImpl()
  {  
  }
  
  /**
   * Display activated modules
   */
  protected void logModules()
  {
  }

  public void startClusterUpdate()
  {
    /*
    try {
      if (_clusterStore != null)
        _clusterStore.startUpdate();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    */
  }

  public void bind(String address, int port, QServerSocket ss)
    throws Exception
  {
    getListenService().bind(address, port, ss);
  }

  /**   
   * Notifications to cluster servers that we've started
   */
  protected void notifyClusterStart()
  {
  }

  /**
   * Handles the alarm.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;

    try {
      if (isModified()) {
        // XXX: message slightly wrong
        String msg = L.l("Resin restarting due to configuration change");

        ShutdownService.getCurrent().shutdown(ExitCode.MODIFIED, msg);
        return;
      }
    } finally {
      alarm.queue(ALARM_INTERVAL);
    }
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  @Override
  public boolean isModified()
  {
    boolean isModified = getClassLoader().isModified();

    if (isModified)
      getClassLoader().logModified(log);

    return isModified;
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModifiedNow()
  {
    boolean isModified = getClassLoader().isModifiedNow();

    if (isModified)
      log.fine("server is modified");

    return isModified;
  }

  /**
   * Returns true if the server is starting or active
   */
  public boolean isAfterStarting()
  {
    return _lifecycle.isAfterStarting();
  }

  /**
   * Returns true if the server is stopped.
   */
  public boolean isStopping()
  {
    return _lifecycle.isStopping();
  }

  /**
   * Returns true if the server is stopped.
   */
  public boolean isStopped()
  {
    return _lifecycle.isStopped();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isDestroying()
  {
    return _lifecycle.isDestroying();
  }

  /**
   * Returns true if the server is currently active and accepting requests
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Clears the catch by matching the invocation.
   */
  public void clearCacheByPattern(String hostPattern, String uriPattern)
  {
    final Matcher hostMatcher;
    if (hostPattern != null)
      hostMatcher = Pattern.compile(hostPattern).matcher("");
    else
      hostMatcher = null;

    final Matcher uriMatcher;
    if (uriPattern != null)
      uriMatcher = Pattern.compile(uriPattern).matcher("");
    else
      uriMatcher = null;

    InvocationMatcher matcher = new InvocationMatcher() {
      public boolean isMatch(Invocation invocation)
      {
        if (hostMatcher != null) {
          hostMatcher.reset(invocation.getHost());
          if (! hostMatcher.find()) {
            return false;
          }
        }

        if (uriMatcher != null) {
          uriMatcher.reset(invocation.getURI());
          if (! uriMatcher.find()) {
            return false;
          }
        }

        return true;
      }
    };

    invalidateMatchingInvocations(matcher);
  }

  /**
   * Clears the proxy cache.
   */
  public void clearCache()
  {
    // skip the clear on restart
    if (isStopping())
      return;

    if (log.isLoggable(Level.FINEST))
      log.finest("ServletServer clearCache");

    // the invocation cache must be cleared first because the old
    // filter chain entries must not point to the cache's
    // soon-to-be-invalid entries
    super.clearCache();

    if (_cache != null)
      _cache.clear();
  }

  /**
   * Returns the proxy cache hit count.
   */
  public long getProxyCacheHitCount()
  {
    if (_cache != null)
      return _cache.getHitCount();
    else
      return 0;
  }

  /**
   * Returns the proxy cache miss count.
   */
  public long getProxyCacheMissCount()
  {
    if (_cache != null)
      return _cache.getMissCount();
    else
      return 0;
  }

  /**
   * Finds the TcpConnection given the threadId
   */
  public TcpSocketLink findConnectionByThreadId(long threadId)
  {
    for (SocketLinkListener port : getPorts()) {
      TcpSocketLink conn = port.findConnectionByThreadId(threadId);

      if (conn != null)
        return conn;
    }

    return null;
  }

  /**
   * Returns any HMTP stream
   */
  public ActorStream getHmtpStream()
  {
    return null;
  }

  public void addDynamicServer(String clusterId,
                               String serverId,
                               String address,
                               int port)
  {
  }

  public void removeDynamicServer(String clusterId,
                                  String address,
                                  int port)
  {
  }

  /**
   * Closes the server.
   */
  public void stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (! _lifecycle.toStopping())
        return;

      // notify other servers that we've stopped
      notifyStop();

      Alarm alarm = _alarm;
      _alarm = null;

      if (alarm != null)
        alarm.dequeue();

      if (getSelectManager() != null)
        getSelectManager().stop();

      try {
        if (_systemStore != null)
          _systemStore.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        if (_globalStore != null)
          _globalStore.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      
      /*
      try {
        if (_domainManager != null)
          _domainManager.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      */

      try {
        ThreadPool.getThreadPool().interrupt();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        Thread.sleep(1);
      } catch (Throwable e) {
      }

      try {
        if (_hostContainer != null)
          _hostContainer.stop();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        getClassLoader().stop();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      _lifecycle.toStop();
    } finally {
      thread.setContextClassLoader(oldLoader);

      super.stop();
    }
  }

  /**
   * Notifications to cluster servers that we've stopped.
   */
  protected void notifyStop()
  {
  }

  /**
   * Closes the server.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      try {
        Management management = _management;
        _management = null;

        if (management != null)
          management.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      try {
        _hostContainer.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      super.destroy();

      log.fine(this + " destroyed");

      // getClassLoader().destroy();

      if (_distributedCacheManager != null)
        _distributedCacheManager.close();

      _hostContainer = null;
      _cache = null;
    } finally {
      DynamicClassLoader.setOldLoader(thread, oldLoader);

      Resin resin = _resin;

      if (resin != null)
        resin.destroy();
    }
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[id=" + getServerId()
            + ",cluster=" + _selfServer.getCluster().getId() + "]");
  }

  /**
   * EL variables
   */
  public class ClusterVar {
    /**
     * Returns the resin.id
     */
    public String getId()
    {
      return getCluster().getId();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRoot()
    {
      return getRootDirectory();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRootDir()
    {
      return getRootDirectory();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRootDirectory()
    {
      return Server.this.getRootDirectory();
    }
  }

  public class ServerVar {
    private final ClusterServer _server;

    public ServerVar(ClusterServer server)
    {
      _server = server;
    }

    public String getId()
    {
      return _server.getId();
    }

    private int getPort(SocketLinkListener port)
    {
      if (port == null)
        return 0;

      return port.getPort();
    }

    private String getAddress(SocketLinkListener port)
    {
      if (port == null)
        return null;

      String address = port.getAddress();

      if (address == null || address.length() == 0)
        address = "INADDR_ANY";

      return address;
    }

    private SocketLinkListener getFirstPort(String protocol, boolean isSSL)
    {
      for (SocketLinkListener port : getListenService().getListeners()) {
        if (protocol.equals(port.getProtocolName()) && (port.isSSL() == isSSL))
          return port;
      }

      return null;
    }

    public String getAddress()
    {
      return _selfServer.getAddress();
    }

    public int getPort()
    {
      return _selfServer.getPort();
    }

    public String getHttpAddress()
    {
      return getAddress(getFirstPort("http", false));
    }

    public int getHttpPort()
    {
      return getPort(getFirstPort("http", false));
    }


    public String getHttpsAddress()
    {
      return getAddress(getFirstPort("http", true));
    }

    public int getHttpsPort()
    {
      return getPort(getFirstPort("http", true));
    }

    /**
     * @deprecated backwards compat.
     */
    public Path getRoot()
    {
      Resin resin =  Resin.getLocal();

      return resin == null ? Vfs.getPwd() : resin.getRootDirectory();
    }
  }

  public static class SelectManagerCompat {
    private boolean _isEnable = true;

    public void setEnable(boolean isEnable)
    {
      _isEnable = isEnable;
    }

    public boolean isEnable()
    {
      return _isEnable;
    }
  }
}
