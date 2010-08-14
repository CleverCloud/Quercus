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

package com.caucho.server.session;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.distcache.ByteStreamCache;
import com.caucho.distcache.AbstractCache;
import com.caucho.distcache.ClusterByteStreamCache;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.env.meter.AverageSensor;
import com.caucho.env.meter.MeterService;
import com.caucho.hessian.io.*;
import com.caucho.management.server.SessionManagerMXBean;
import com.caucho.security.Authenticator;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.distcache.PersistentStoreConfig;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.WeakAlarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.TempOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.SessionCookieConfig;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

// import com.caucho.server.http.ServletServer;
// import com.caucho.server.http.VirtualHost;

/**
 * Manages sessions in a web-webApp.
 */
public final class SessionManager implements SessionCookieConfig, AlarmListener
{
  static protected final L10N L = new L10N(SessionManager.class);
  static protected final Logger log
    = Logger.getLogger(SessionManager.class.getName());

  private static final int FALSE = 0;
  private static final int COOKIE = 1;
  private static final int TRUE = 2;

  private static final int UNSET = 0;
  private static final int SET_TRUE = 1;
  private static final int SET_FALSE = 2;

  private static final int SAVE_BEFORE_HEADERS = 0x1;
  private static final int SAVE_BEFORE_FLUSH = 0x2;
  private static final int SAVE_AFTER_REQUEST = 0x4;
  private static final int SAVE_ON_SHUTDOWN = 0x8;

  private final WebApp _webApp;
  private final SessionManagerAdmin _admin;

  private final Server _server;
  private final ClusterServer _selfServer;
  private final int _selfIndex;

  private AbstractCache _sessionStore;

  // active sessions
  private LruCache<String,SessionImpl> _sessions;
  // iterator to purge sessions (to reduce gc)
  private Iterator<SessionImpl> _sessionIter;
  // array list for session timeout
  private ArrayList<SessionImpl> _sessionList = new ArrayList<SessionImpl>();
  // generate cookies
  private boolean _enableSessionCookies = true;
  // allow session rewriting
  private boolean _enableSessionUrls = true;

  private boolean _isAppendServerIndex = false;
  private boolean _isTwoDigitSessionIndex = false;

  // invalidate the session after the listeners have been called
  private boolean _isInvalidateAfterListener;

  // maximum number of sessions
  private int _sessionMax = 8192;
  // how long a session will be inactive before it times out
  private long _sessionTimeout = 30 * 60 * 1000;

  private String _cookieName = "JSESSIONID";
  private String _sslCookieName;

  // Rewriting strings.
  private String _sessionSuffix = ";jsessionid=";
  private String _sessionPrefix;

  // default cookie version
  private int _cookieVersion;
  private String _cookieDomain;
  private String _cookieDomainRegexp;
  private boolean _isCookieUseContextPath;
  private String _cookiePath;
  private long _cookieMaxAge;
  private int _isCookieHttpOnly;
  private String _cookieComment;
  private String _cookiePort;
  private int _reuseSessionId = COOKIE;
  private int _cookieLength = 21;
  //Servlet 3.0 plain | ssl session tracking cookies become secure when set to true
  private boolean _isSecure;

  // persistence configuration

  private int _sessionSaveMode = SAVE_AFTER_REQUEST;

  private boolean _isPersistenceEnabled = false;
  private boolean _isSaveTriplicate = true;
  private boolean _isSaveBackup = true;

  // If true, serialization errors should not be logged
  // XXX: changed for JSF
  private boolean _ignoreSerializationErrors = true;
  private boolean _isHessianSerialization = true;

  // List of the HttpSessionListeners from the configuration file
  private ArrayList<HttpSessionListener> _listeners;

  // List of the HttpSessionListeners from the configuration file
  private ArrayList<HttpSessionActivationListener> _activationListeners;

  // List of the HttpSessionAttributeListeners from the configuration file
  private ArrayList<HttpSessionAttributeListener> _attributeListeners;

  //
  // Compatibility fields
  //

  // private Store _sessionStore;
  private int _alwaysLoadSession;
  private int _alwaysSaveSession;

  private boolean _isClosed;

  private String _distributionId;

  private Alarm _alarm;

  // statistics
  private volatile long _sessionCreateCount;
  private volatile long _sessionTimeoutCount;
  private volatile long _sessionInvalidateCount;

  private final AverageSensor _sessionSaveSample;

  /**
   * Creates and initializes a new session manager
   *
   * @param webApp the web-webApp webApp
   */
  public SessionManager(WebApp webApp)
    throws Exception
  {
    _webApp = webApp;

    _server = Server.getCurrent();

    if (_server == null) {
      throw new IllegalStateException(L.l("Server is not active in this context {0}",
                                          Thread.currentThread().getContextClassLoader()));
    }
    _selfServer = _server.getSelfServer();
    _selfIndex = _selfServer.getIndex();

    // copy defaults from store for backward compat
    PersistentStoreConfig cfg = _server.getPersistentStoreConfig();
    if (cfg != null) {
      setAlwaysSaveSession(cfg.isAlwaysSave());

      _isSaveBackup = cfg.isSaveBackup();
      _isSaveTriplicate = cfg.isSaveTriplicate();
    }

    DispatchServer server = webApp.getDispatchServer();
    if (server != null) {
      InvocationDecoder decoder = server.getInvocationDecoder();

      _sessionSuffix = decoder.getSessionURLPrefix();
      _sessionPrefix = decoder.getAlternateSessionURLPrefix();

      _cookieName = decoder.getSessionCookie();
      _sslCookieName = decoder.getSSLSessionCookie();
      
      if (_sslCookieName != null && ! _sslCookieName.equals(_cookieName))
        _isSecure = true;
    }

    String hostName = webApp.getHostName();
    String contextPath = webApp.getContextPath();

    if (hostName == null || hostName.equals(""))
      hostName = "default";

    String name = hostName + contextPath;

    if (_distributionId == null)
      _distributionId = name;

    _alarm = new WeakAlarm(this);
    _sessionSaveSample
      = MeterService.createAverageMeter("Resin|WebApp|Session Save", "Size");

    _admin = new SessionManagerAdmin(this);
  }

  /**
   * Returns the admin.
   */
  public SessionManagerMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Returns the session prefix, ie.. ";jsessionid=".
   */
  public String getSessionPrefix()
  {
    return _sessionSuffix;
  }

  /**
   * Returns the alternate session prefix, before the URL for wap.
   */
  public String getAlternateSessionPrefix()
  {
    return _sessionPrefix;
  }

  /**
   * Returns the cookie version.
   */
  public int getCookieVersion()
  {
    return _cookieVersion;
  }

  /**
   * Sets the cookie version.
   */
  public void setCookieVersion(int cookieVersion)
  {
    _cookieVersion = cookieVersion;
  }

  /**
   * Sets the cookie ports.
   */
  public void setCookiePort(String port)
  {
    _cookiePort = port;
  }

  /**
   * Sets the cookie ports.
   */
  public void setCookieUseContextPath(boolean isCookieUseContextPath)
  {
    _isCookieUseContextPath = isCookieUseContextPath;
  }

  /**
   * Gets the cookie ports.
   */
  public String getCookiePort()
  {
    return _cookiePort;
  }

  /**
   * Returns the debug log
   */
  public Logger getDebug()
  {
    return log;
  }

  /**
   * Returns the SessionManager's webApp
   */
  WebApp getWebApp()
  {
    return _webApp;
  }

  /**
   * Returns the SessionManager's authenticator
   */
  Authenticator getAuthenticator()
  {
    return _webApp.getAuthenticator();
  }

  /**
   * Returns the session cache
   */
  ByteStreamCache getCache()
  {
    if (_isPersistenceEnabled)
      return _sessionStore;
    else
      return null;
  }

  /**
   * True if sessions should always be loadd.
   */
  boolean getAlwaysLoadSession()
  {
    return _alwaysLoadSession == SET_TRUE;
  }

  /**
   * True if sessions should always be loadd.
   */
  public void setAlwaysLoadSession(boolean load)
  {
    _alwaysLoadSession = load ? SET_TRUE : SET_FALSE;
  }

  /**
   * True if sessions should always be saved.
   */
  boolean getAlwaysSaveSession()
  {
    return _alwaysSaveSession == SET_TRUE;
  }

  /**
   * True if sessions should always be saved.
   */
  public void setAlwaysSaveSession(boolean save)
  {
    _alwaysSaveSession = save ? SET_TRUE : SET_FALSE;
  }

  /**
   * True if sessions should be saved on shutdown.
   */
  public boolean isSaveOnShutdown()
  {
    return (_sessionSaveMode & SAVE_ON_SHUTDOWN) != 0;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public boolean isSaveOnlyOnShutdown()
  {
    return (_sessionSaveMode & SAVE_ON_SHUTDOWN) == SAVE_ON_SHUTDOWN;
  }

  /**
   * True if sessions should be saved before the HTTP headers.
   */
  public boolean isSaveBeforeHeaders()
  {
    return (_sessionSaveMode & SAVE_BEFORE_HEADERS) != 0;
  }

  /**
   * True if sessions should be saved before each flush.
   */
  public boolean isSaveBeforeFlush()
  {
    return (_sessionSaveMode & SAVE_BEFORE_FLUSH) != 0;
  }

  /**
   * True if sessions should be saved after the request.
   */
  public boolean isSaveAfterRequest()
  {
    return (_sessionSaveMode & SAVE_AFTER_REQUEST) != 0;
  }

  /**
   * Determines how many digits are used to encode the server
   */
  boolean isTwoDigitSessionIndex()
  {
   return _isTwoDigitSessionIndex;
  }

  /**
   * Sets the save-mode: before-flush, before-headers, after-request,
   * on-shutdown
   */
  public void setSaveMode(String mode)
    throws ConfigException
  {
    /* XXX: probably don't want to implement this.
    if ("before-flush".equals(mode)) {
      _sessionSaveMode = (SAVE_BEFORE_FLUSH|
                          SAVE_BEFORE_HEADERS|
                          SAVE_AFTER_REQUEST|
                          SAVE_ON_SHUTDOWN);
    }
    else
    */

    if ("before-headers".equals(mode)) {
      _sessionSaveMode = (SAVE_BEFORE_HEADERS
                          | SAVE_ON_SHUTDOWN);
    }
    else if ("after-request".equals(mode)) {
      _sessionSaveMode = (SAVE_AFTER_REQUEST
                          | SAVE_ON_SHUTDOWN);
    }
    else if ("on-shutdown".equals(mode)) {
      _sessionSaveMode = (SAVE_ON_SHUTDOWN);
    }
    else
      throw new ConfigException(L.l("'{0}' is an unknown session save-mode.  Values are: before-headers, after-request, and on-shutdown.",
                                    mode));

  }

  /**
   * Returns the string value of the save-mode.
   */
  public String getSaveMode()
  {
    if (isSaveBeforeFlush())
      return "before-flush";
    else if (isSaveBeforeHeaders())
      return "before-headers";
    else if (isSaveAfterRequest())
      return "after-request";
    else if (isSaveOnShutdown())
      return "on-shutdown";
    else
      return "unknown";
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public void setSaveOnlyOnShutdown(boolean save)
  {
    log.warning("<save-only-on-shutdown> is deprecated.  Use <save-mode>on-shutdown</save-mode> instead");

    if (save)
      _sessionSaveMode = SAVE_ON_SHUTDOWN;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public void setSaveOnShutdown(boolean save)
  {
    log.warning("<save-on-shutdown> is deprecated.  Use <save-only-on-shutdown> instead");

    setSaveOnlyOnShutdown(save);
  }

  /**
   * Sets the serialization type.
   */
  public void setSerializationType(String type)
  {
    if ("hessian".equals(type))
      _isHessianSerialization = true;
    else if ("java".equals(type))
      _isHessianSerialization = false;
    else
      throw new ConfigException(L.l("'{0}' is an unknown valud for serialization-type.  The valid types are 'hessian' and 'java'.",
                                    type));
  }

  /**
   * Returns true for Hessian serialization.
   */
  public boolean isHessianSerialization()
  {
    return _isHessianSerialization;
  }

  /**
   * True if the session should be invalidated after the listener.
   */
  public void setInvalidateAfterListener(boolean inv)
  {
    _isInvalidateAfterListener = inv;
  }

  /**
   * True if the session should be invalidated after the listener.
   */
  public boolean isInvalidateAfterListener()
  {
    return _isInvalidateAfterListener;
  }

  /**
   * Returns the current number of active sessions.
   */
  public int getActiveSessionCount()
  {
    if (_sessions == null)
      return -1;
    else
      return _sessions.size();
  }

  /**
   * Returns the active sessions.
   */
  public int getSessionActiveCount()
  {
    return getActiveSessionCount();
  }

  /**
   * Returns the created sessions.
   */
  public long getSessionCreateCount()
  {
    return _sessionCreateCount;
  }

  /**
   * Returns the timeout sessions.
   */
  public long getSessionTimeoutCount()
  {
    return _sessionTimeoutCount;
  }

  /**
   * Returns the invalidate sessions.
   */
  public long getSessionInvalidateCount()
  {
    return _sessionInvalidateCount;
  }

  /**
   * Adds a new HttpSessionListener.
   */
  public void addListener(HttpSessionListener listener)
  {
    if (_listeners == null)
      _listeners = new ArrayList<HttpSessionListener>();

    _listeners.add(listener);
  }

  /**
   * Adds a new HttpSessionListener.
   */
  ArrayList<HttpSessionListener> getListeners()
  {
    return _listeners;
  }

  /**
   * Adds a new HttpSessionActivationListener.
   */
  public void addActivationListener(HttpSessionActivationListener listener)
  {
    if (_activationListeners == null)
      _activationListeners = new ArrayList<HttpSessionActivationListener>();

    _activationListeners.add(listener);
  }

  /**
   * Returns the activation listeners.
   */
  ArrayList<HttpSessionActivationListener> getActivationListeners()
  {
    return _activationListeners;
  }

  /**
   * Adds a new HttpSessionAttributeListener.
   */
  public void addAttributeListener(HttpSessionAttributeListener listener)
  {
    if (_attributeListeners == null)
      _attributeListeners = new ArrayList<HttpSessionAttributeListener>();

    _attributeListeners.add(listener);
  }

  /**
   * Gets the HttpSessionAttributeListener.
   */
  ArrayList<HttpSessionAttributeListener> getAttributeListeners()
  {
    return _attributeListeners;
  }

  /**
   * True if serialization errors should just fail silently.
   */
  boolean getIgnoreSerializationErrors()
  {
    return _ignoreSerializationErrors;
  }

  /**
   * True if serialization errors should just fail silently.
   */
  public void setIgnoreSerializationErrors(boolean ignore)
  {
    _ignoreSerializationErrors = ignore;
  }

  /**
   * True if the server should reuse the current session id if the
   * session doesn't exist.
   */
  public int getReuseSessionId()
  {
    return _reuseSessionId;
  }

  /**
   * True if the server should reuse the current session id if the
   * session doesn't exist.
   */
  public boolean reuseSessionId(boolean fromCookie)
  {
    int reuseSessionId = _reuseSessionId;

    return reuseSessionId == TRUE || fromCookie && reuseSessionId == COOKIE;
  }

  /**
   * True if the server should reuse the current session id if the
   * session doesn't exist.
   */
  public void setReuseSessionId(String reuse)
    throws ConfigException
  {
    if (reuse == null)
      _reuseSessionId = COOKIE;
    else if (reuse.equalsIgnoreCase("true")
             || reuse.equalsIgnoreCase("yes")
             || reuse.equalsIgnoreCase("cookie"))
      _reuseSessionId = COOKIE;
    else if (reuse.equalsIgnoreCase("false") || reuse.equalsIgnoreCase("no"))
      _reuseSessionId = FALSE;
    else if (reuse.equalsIgnoreCase("all"))
      _reuseSessionId = TRUE;
    else
      throw new ConfigException(L.l("'{0}' is an invalid value for reuse-session-id.  'true' or 'false' are the allowed values.",
                                    reuse));
  }

  /**
   * Returns true if the sessions are closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Sets the cluster store.
   */
  public void setUsePersistentStore(boolean enable)
    throws Exception
  {
    _isPersistenceEnabled = enable;
  }
  
  public boolean isUsePersistentStore()
  {
    return isPersistenceEnabled();
  }

  public boolean isPersistenceEnabled()
  {
    return _isPersistenceEnabled;
  }

  public String getDistributionId()
  {
    return _distributionId;
  }

  public void setDistributionId(String distributionId)
  {
    _distributionId = distributionId;
  }

  /**
   * Returns the default session timeout in milliseconds.
   */
  public long getSessionTimeout()
  {
    return _sessionTimeout;
  }

  /**
   * Set the default session timeout in minutes
   */
  public void setSessionTimeout(long timeout)
  {
    if (timeout <= 0 || Integer.MAX_VALUE / 2 < timeout)
      _sessionTimeout = Long.MAX_VALUE / 2;
    else
      _sessionTimeout = 60000L * timeout;
  }

  /**
   * Returns the idle time.
   */
  public long getMaxIdleTime()
  {
    return _sessionTimeout;
  }

  /**
   * Returns the maximum number of sessions.
   */
  public int getSessionMax()
  {
    return _sessionMax;
  }

  /**
   * Returns the maximum number of sessions.
   */
  public void setSessionMax(int max)
  {
    if (max < 1)
      throw new ConfigException(L.l("session-max '{0}' is too small.  session-max must be a positive number", max));

    _sessionMax = max;
  }

  /**
   * Returns true if sessions use the cookie header.
   */
  public boolean enableSessionCookies()
  {
    return _enableSessionCookies;
  }

  /**
   * Returns true if sessions use the cookie header.
   */
  public void setEnableCookies(boolean enableCookies)
  {
    _enableSessionCookies = enableCookies;
  }

  /**
   * Returns true if sessions can use the session rewriting.
   */
  public boolean enableSessionUrls()
  {
    return _enableSessionUrls;
  }

  /**
   * Returns true if sessions can use the session rewriting.
   */
  public void setEnableUrlRewriting(boolean enableUrls)
  {
    _enableSessionUrls = enableUrls;
  }

  //SessionCookieConfig implementation (Servlet 3.0)
  public void setName(String name)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    setCookieName(name);
  }

  public String getName()
  {
    return getCookieName();
  }

  public void setDomain(String domain)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    setCookieDomain(domain);
  }

  public String getDomain()
  {
    return getCookieDomain();
  }

  public void setPath(String path)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    _cookiePath = path;
  }

  public String getPath()
  {
    return _cookiePath;
  }

  public void setComment(String comment)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    _cookieComment = comment;
  }

  public String getComment()
  {
    return _cookieComment;
  }

  public void setHttpOnly(boolean httpOnly)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    setCookieHttpOnly(httpOnly);
  }

  public boolean isHttpOnly()
  {
    return isCookieHttpOnly();
  }

  public void setSecure(boolean secure)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException(L.l("SessionCookieConfig must be set during initialization"));

    _isSecure = secure;
  }

  public boolean isSecure()
  {
    return _isSecure;
  }

  public void setMaxAge(int maxAge)
  {
    if (! _webApp.isInitializing())
      throw new IllegalStateException();

    _cookieMaxAge = maxAge * 1000;
  }

  public int getMaxAge()
  {
    return (int) (_cookieMaxAge / 1000);
  }

  public void setCookieName(String cookieName)
  {
    _cookieName = cookieName;
  }
  /**
   * Returns the default cookie name.
   */
  public String getCookieName()
  {
    return _cookieName;
  }

  /**
   * Returns the SSL cookie name.
   */
  public String getSSLCookieName()
  {
    if (_sslCookieName != null)
      return _sslCookieName;
    else
      return _cookieName;
  }

  /**
   * Returns the default session cookie domain.
   */
  public String getCookieDomain()
  {
    return _cookieDomain;
  }

  /**
   * Sets the default session cookie domain.
   */
  public void setCookieDomain(String domain)
  {
    _cookieDomain = domain;
  }

  public String getCookieDomainRegexp() {
    return _cookieDomainRegexp;
  }

  public void setCookieDomainRegexp(String regexp)
  {
    _cookieDomainRegexp = regexp;
  }

  /**
   * Sets the default session cookie domain.
   */
  public void setCookiePath(String path)
  {
    _cookiePath = path;
  }

  /**
   * Returns the max-age of the session cookie.
   */
  public long getCookieMaxAge()
  {
    return _cookieMaxAge;
  }

  /**
   * Sets the max-age of the session cookie.
   */
  public void setCookieMaxAge(Period maxAge)
  {
    _cookieMaxAge = maxAge.getPeriod();
  }

  /**
   * Returns the secure of the session cookie.
   */
  public boolean getCookieSecure()
  {
    if (_isSecure)
      return true;
    else
      return ! _cookieName.equals(_sslCookieName);
  }

  /**
   * Sets the secure of the session cookie.
   */
  public void setCookieSecure(boolean isSecure)
  {
    _isSecure = isSecure;
  }

  /**
   * Returns the http-only of the session cookie.
   */
  public boolean isCookieHttpOnly()
  {
    if (_isCookieHttpOnly == SET_TRUE)
      return true;
    else if (_isCookieHttpOnly == SET_FALSE)
      return false;
    else
      return getWebApp().getCookieHttpOnly();
  }

  /**
   * Sets the http-only of the session cookie.
   */
  public void setCookieHttpOnly(boolean httpOnly)
  {
    _isCookieHttpOnly = httpOnly ? SET_TRUE : SET_FALSE;
  }

  /**
   * Sets the cookie length
   */
  public void setCookieLength(int cookieLength)
  {
    if (cookieLength < 7)
      cookieLength = 7;

    _cookieLength = cookieLength;
  }

  /**
   * Returns the cookie length.
   */
  public long getCookieLength()
  {
    return _cookieLength;
  }

  /**
   * Sets module session id generation.
   */
  public void setCookieModuloCluster(boolean isModulo)
  {
  }

  /**
   * Sets module session id generation.
   */
  public void setCookieAppendServerIndex(boolean isAppend)
  {
    _isAppendServerIndex = isAppend;
  }

  /**
   * Sets module session id generation.
   */
  public boolean isCookieAppendServerIndex()
  {
    return _isAppendServerIndex;
  }

  public void init()
  {
    if (_sessionSaveMode == SAVE_ON_SHUTDOWN
        && (_alwaysSaveSession == SET_TRUE
            || _alwaysLoadSession == SET_TRUE))
      throw new ConfigException(L.l("save-mode='on-shutdown' cannot be used with <always-save-session/> or <always-load-session/>"));
    _sessions = new LruCache<String,SessionImpl>(_sessionMax);
    _sessionIter = _sessions.values();

    if (_sessionStore != null)
      _sessionStore.setIdleTimeoutMillis(_sessionTimeout);

    if (_isPersistenceEnabled) {
      AbstractCache sessionCache = new ClusterByteStreamCache();

      sessionCache.setName("resin:session");
      sessionCache.setBackup(_isSaveBackup);
      sessionCache.setTriplicate(_isSaveTriplicate);
      sessionCache.init();

      _sessionStore = sessionCache;
    }

    if (_cookiePath != null) {
    }
    else if (_isCookieUseContextPath)
      _cookiePath = _webApp.getContextPath();

    if (_cookiePath == null || "".equals(_cookiePath))
      _cookiePath = "/";
  }

  public void start()
    throws Exception
  {
    _alarm.queue(60000);
  }

  /**
   * Returns the session store.
   */
  public ByteStreamCache getSessionStore()
  {
    return _sessionStore;
  }

  public SessionSerializer createSessionSerializer(OutputStream os)
    throws IOException
  {
    if (_isHessianSerialization)
      return new HessianSessionSerializer(os);
    else
      return new JavaSessionSerializer(os);
  }

  public SessionDeserializer createSessionDeserializer(InputStream is)
    throws IOException
  {
    if (_isHessianSerialization)
      return new HessianSessionDeserializer(is);
    else
      return new JavaSessionDeserializer(is);
  }

  /**
   * Returns true if the session exists in this manager.
   */
  public boolean containsSession(String id)
  {
    return _sessions.get(id) != null;
  }

  /**
   * Creates a pseudo-random session id.  If there's an old id and the
   * group matches, then use it because different webApps on the
   * same matchine should use the same cookie.
   *
   * @param request current request
   */
  public String createSessionId(HttpServletRequest request)
  {
    return createSessionId(request, false);
  }

  /**
   * Creates a pseudo-random session id.  If there's an old id and the
   * group matches, then use it because different webApps on the
   * same machine should use the same cookie.
   *
   * @param request current request
   */
  public String createSessionId(HttpServletRequest request,
                                boolean create)
  {
    String id;

    do {
      id = createSessionIdImpl(request);
    } while (create && getSession(id, 0, create, true) != null);

    if (id == null || id.equals(""))
      throw new RuntimeException();

    return id;
  }

  public String createCookieValue()
  {
    return createCookieValue(null);
  }

  public String createSessionIdImpl(HttpServletRequest request)
  {
    // look at caucho.session-server-id for a hint of the owner
    Object owner = request.getAttribute("caucho.session-server-id");

    return createCookieValue(owner);
  }

  public boolean isOwner(String id)
  {
    return id.startsWith(_selfServer.getServerClusterId());
  }

  protected String createCookieValue(Object owner)
  {
    StringBuilder sb = new StringBuilder();
    // this section is the host specific session index
    // the most random bit is the high bit
    int index = _selfIndex;

    CloudServer server = _selfServer.getCloudServer();

    if (owner == null) {
    }
    else if (owner instanceof Number) {
      index = ((Number) owner).intValue();

      int podIndex = _selfServer.getCloudPod().getIndex();

      server = _selfServer.getCluster().findServer(podIndex, index);

      if (server == null)
        server = _selfServer.getCloudServer();
    }
    else if (owner instanceof String) {
      server = _selfServer.getCluster().findServer((String) owner);

      if (server == null)
        server = _selfServer.getCloudServer();
    }

    index = server.getIndex();
    
    ClusterServer clusterServer = server.getData(ClusterServer.class);

    clusterServer.generateIdPrefix(sb);
    // XXX: _cluster.generateBackup(sb, index);

    int length = _cookieLength;

    length -= sb.length();

    long random = RandomUtil.getRandomLong();

    for (int i = 0; i < 11 && length-- > 0; i++) {
      sb.append(convert(random));
      random = random >> 6;
    }

    if (length > 0) {
      long time = Alarm.getCurrentTime();

      // The QA needs to add a millisecond for each server start so the
      // clustering test will work, but all the session ids are generated
      // based on the timestamp.  So QA sessions don't have milliseconds
      if (Alarm.isTest())
        time -= time % 1000;

      for (int i = 0; i < 7 && length-- > 0; i++) {
        sb.append(convert(time));
        time = time >> 6;
      }
    }

    while (length > 0) {
      random = RandomUtil.getRandomLong();
      for (int i = 0; i < 11 && length-- > 0; i++) {
        sb.append(convert(random));
        random = random >> 6;
      }
    }

    if (_isAppendServerIndex) {
      sb.append('.');
      sb.append((index + 1));
    }

    return sb.toString();
  }

  /**
   * Finds a session in the session store, creating one if 'create' is true
   *
   * @param isCreate if the session doesn't exist, create it
   * @param request current request
   * @sessionId a desired sessionId or null
   * @param now the time in milliseconds
   * @param fromCookie true if the session id comes from a cookie
   *
   * @return the cached session.
   */
  public SessionImpl createSession(boolean isCreate,
                                   HttpServletRequest request,
                                   String sessionId,
                                   long now,
                                   boolean fromCookie)
  {
    if (_sessions == null)
      return null;

    SessionImpl session = _sessions.get(sessionId);
    
    boolean isNew = false;
    boolean killSession = false;

    if (session == null
        && sessionId != null
        && _sessionStore != null) {
      ExtCacheEntry entry = _sessionStore.getExtCacheEntry(sessionId);

      if (entry != null && ! entry.isValueNull()) {
        session = create(sessionId, now, isCreate);

        isNew = true;
      }
    }

    if (session != null) {
      if (session.load(isNew)) {
        session.addUse();

        if (isCreate) {
          // TCK only set access on create
          session.setAccess(now);
        }

        return session;
      }
      else {
        // if the load failed, then the session died out from underneath
        if (! isNew) {
          if (log.isLoggable(Level.FINER))
            log.fine(session + " load failed for existing session");

          // server/0174
          session.reset(0);
          /*
          session.setModified();

          // Return the existing session for timing reasons, e.g.
          // if a second request hits before the first has finished saving

          return session;
          */
        }
      }
    }

    if (! isCreate)
      return null;

    if (sessionId == null
        || sessionId.length() <= 6
        || ! reuseSessionId(fromCookie)) {
      sessionId = createSessionId(request, true);
    }

    session = new SessionImpl(this, sessionId, now);

    // If another thread has created and stored a new session,
    // putIfNew will return the old session
    session = _sessions.putIfNew(sessionId, session);

    if (! sessionId.equals(session.getId()))
      throw new IllegalStateException(sessionId + " != " + session.getId());

    if (! session.addUse())
      throw new IllegalStateException(L.l("Can't use session for unknown reason"));

    _sessionCreateCount++;

    session.create(now, true);

    handleCreateListeners(session);

    return session;
  }

  /**
   * Returns a session from the session store, returning null if there's
   * no cached session.
   *
   * @param key the session id
   * @param now the time in milliseconds
   *
   * @return the cached session.
   */
  public SessionImpl getSession(String key, long now,
                                boolean create, boolean fromCookie)
  {
    SessionImpl session;
    boolean isNew = false;
    boolean killSession = false;

    if (_sessions == null)
      return null;

    session = _sessions.get(key);

    if (session != null && ! session.getId().equals(key))
      throw new IllegalStateException(key + " != " + session.getId());

    if (now <= 0) // just generating id
      return session;

    if (session != null && ! session.addUse()) {
      session = null;
    }

    if (session == null && _sessionStore != null) {
      /*
      if (! _objectManager.isInSessionGroup(key))
        return null;
      */

      session = create(key, now, create);

      if (! session.addUse())
        session = null;

      isNew = true;
    }

    if (session == null)
      return null;

    if (isNew) {
      killSession = ! load(session, now, create);
      isNew = killSession;
    }
    else if (! session.load(isNew)) {
      // if the load failed, then the session died out from underneath
      if (log.isLoggable(Level.FINER))
        log.fine(session + " load failed for existing session");

      session.setModified();

      isNew = true;
    }

    if (killSession && (! create || ! reuseSessionId(fromCookie))) {
      // XXX: session.setClosed();
      session.endUse();
      _sessions.remove(key);
      // XXX:
      // session._isValid = false;

      return null;
    }
    else if (isNew)
      handleCreateListeners(session);
    //else
      //session.setAccess(now);

    return session;
  }

  /**
   * Create a new session.
   *
   * @param oldId the id passed to the request.  Reuse if possible.
   * @param request - current HttpServletRequest
   * @param fromCookie
   */
  public SessionImpl createSession(String oldId, long now,
                                   HttpServletRequest request,
                                   boolean fromCookie)
  {
    if (_sessions == null) {
      log.fine(this + " createSession called when sessionManager closed");

      return null;
    }

    String id = oldId;

    if (id == null
        || id.length() < 4
        || ! reuseSessionId(fromCookie)) {
      // server/0175
      // || ! _objectManager.isInSessionGroup(id)

      id = createSessionId(request, true);
    }

    SessionImpl session = create(id, now, true);

    if (session == null)
      return null;

    session.addUse();

    _sessionCreateCount++;

    synchronized (session) {
      if (_isPersistenceEnabled && id.equals(oldId))
        load(session, now, true);
      else
        session.create(now, true);
    }

    // after load so a reset doesn't clear any setting
    handleCreateListeners(session);

    return session;
  }

  /**
   * Creates a session.  It's already been established that the
   * key does not currently have a session.
   */
  private SessionImpl create(String key, long now, boolean isCreate)
  {
    SessionImpl session = new SessionImpl(this, key, now);

    // If another thread has created and stored a new session,
    // putIfNew will return the old session
    session = _sessions.putIfNew(key, session);

    if (! key.equals(session.getId()))
      throw new IllegalStateException(key + " != " + session.getId());

    return session;
  }

  /**
   * Notification from the cluster.
   */
  public void notifyRemove(String id)
  {
    SessionImpl session = _sessions.get(id);

    if (session != null)
      session.invalidateRemote();
  }

  private void handleCreateListeners(SessionImpl session)
  {
    if (_listeners != null) {
      HttpSessionEvent event = new HttpSessionEvent(session);

      for (int i = 0; i < _listeners.size(); i++) {
        HttpSessionListener listener = _listeners.get(i);

        listener.sessionCreated(event);
      }
    }
  }

  /**
   * Loads the session from the backing store.  The caller must synchronize
   * the session.
   *
   * @param session the session to load.
   * @param now current time in milliseconds.
   */
  private boolean load(SessionImpl session, long now, boolean isCreate)
  {
    try {
      // XXX: session.setNeedsLoad(false);

      /*
      if (session.getUseCount() > 1) {
        // if used by more than just us,
        return true;
      }
      else*/

      if (now <= 0) {
        return false;
      }
      else if (session.load(true)) { // load for a newly created session
        session.setAccess(now);
        return true;
      }
      else {
        session.create(now, isCreate);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      session.reset(now);
    }

    return false;
  }

  /**
   * Adds a session from the cache.
   */
  void addSession(SessionImpl session)
  {
    _sessions.put(session.getId(), session);
  }

  /**
   * Removes a session from the cache.
   */
  void removeSession(SessionImpl session)
  {
    _sessions.remove(session.getId());
  }

  /**
   * Adds a new session save event
   */
  void addSessionSaveSample(long size)
  {
    _sessionSaveSample.add(size);
  }

  /**
   * Returns a debug string for the session
   */
  public String getSessionSerializationDebug(String id)
  {
    ByteStreamCache cache = getCache();

    if (cache == null)
      return null;

    try {
      TempOutputStream os = new TempOutputStream();

      if (cache.get(id, os)) {
        InputStream is = os.getInputStream();

        StringWriter writer = new StringWriter();

        HessianDebugInputStream dis
          = new HessianDebugInputStream(is, new PrintWriter(writer));

        int ch;
        while ((ch = dis.read()) >= 0) {
        }

        return writer.toString();
      }

      os.close();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return e.toString();
    }

    return null;
  }

  /**
   * Timeout for reaping old sessions
   *
   * @return number of live sessions for stats
   */
  public void handleAlarm(Alarm alarm)
  {
    try {
      _sessionList.clear();

      int liveSessions = 0;

      if (_isClosed)
        return;

      long now = Alarm.getCurrentTime();

      synchronized (_sessions) {
        _sessionIter = _sessions.values(_sessionIter);
        while (_sessionIter.hasNext()) {
          SessionImpl session = _sessionIter.next();

          if (session.isIdle(now))
            _sessionList.add(session);
          else
            liveSessions++;
        }
      }

      _sessionTimeoutCount += _sessionList.size();

      for (int i = 0; i < _sessionList.size(); i++) {
        SessionImpl session = _sessionList.get(i);

        try {
          session.timeout();

          _sessions.remove(session.getId());
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    } finally {
      if (! _isClosed)
        _alarm.queue(60000);
    }
  }

  /**
   * Cleans up the sessions when the WebApp shuts down gracefully.
   */
  public void close()
  {
    synchronized (this) {
      if (_isClosed)
        return;
      _isClosed = true;
    }

    if (_sessions == null)
      return;

    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null)
      alarm.dequeue();

    ArrayList<SessionImpl> list = new ArrayList<SessionImpl>();

    // XXX: messy way of dealing with saveOnlyOnShutdown
    synchronized (_sessions) {
      _sessionIter = _sessions.values(_sessionIter);
      while (_sessionIter.hasNext()) {
        SessionImpl session = _sessionIter.next();

        if (session.isValid())
          list.add(session);
      }

      // XXX: if cleared here, will remove the session
      // _sessions.clear();
    }

    boolean isError = false;
    for (int i = list.size() - 1; i >= 0; i--) {
      SessionImpl session = list.get(i);

      if (log.isLoggable(Level.FINE))
        log.fine("close session " + session.getId());

      try {
        session.saveOnShutdown();

        _sessions.remove(session.getId());
      } catch (Exception e) {
        if (! isError)
          log.log(Level.WARNING, "Can't store session: " + e, e);
        isError = true;
      }
    }

    if (_admin != null)
      _admin.unregister();

    _sessionList = new ArrayList<SessionImpl>();
  }

  /**
   * Converts an integer to a printable character
   */
  private static char convert(long code)
  {
    code = code & 0x3f;

    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _webApp.getContextPath() + "]";
  }
}
