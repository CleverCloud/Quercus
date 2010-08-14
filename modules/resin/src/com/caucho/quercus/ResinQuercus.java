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

package com.caucho.quercus;

import com.caucho.VersionFactory;
import com.caucho.loader.*;
import com.caucho.distcache.*;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.ResinModuleContext;
import com.caucho.server.webapp.*;
import com.caucho.server.cluster.Server;
import com.caucho.server.session.*;
import com.caucho.sql.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.java.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Facade for the PHP language.
 */
public class ResinQuercus extends QuercusContext
{
  private static L10N L = new L10N(ResinQuercus.class);
  private static final Logger log
    = Logger.getLogger(ResinQuercus.class.getName());
  
  private static EnvironmentLocal<ModuleContext> _localModuleContext
    = new EnvironmentLocal<ModuleContext>();

  private AbstractCache _sessionCache;

  private WebApp _webApp;
  private long _dependencyCheckInterval = 2000L;
  
  /**
   * Constructor.
   */
  public ResinQuercus()
  {
    super();

    setPwd(Vfs.lookup());
    setWorkDir(WorkDir.getLocalWorkDir());

    EnvironmentClassLoader loader = Environment.getEnvironmentClassLoader();
    if (loader != null)
      _dependencyCheckInterval = loader.getDependencyCheckInterval();
  }

  public void setWebApp(WebApp webApp)
  {
    _webApp = webApp;
  }

  public WebApp getWebApp()
  {
    return _webApp;
  }
  
  /**
   * Returns the current time.
   */
  @Override
  public long getCurrentTime()
  {
    return Alarm.getCurrentTime();
  }
  
  /**
   * Returns the current time in nanoseconds.
   */
  @Override
  public long getExactTimeNanoseconds()
  {
    return Alarm.getExactTimeNanoseconds();
  }
  
  /**
   * Returns the exact current time in milliseconds.
   */
  @Override
  public long getExactTime()
  {
    return Alarm.getExactTime();
  }

  @Override
  public ModuleContext getLocalContext(ClassLoader loader)
  {
    synchronized (_localModuleContext) {
      ModuleContext context = _localModuleContext.getLevel(loader);

      if (context == null) {
        ClassLoader envLoader = Environment.getEnvironmentClassLoader(loader);

        ModuleContext parent = null;
    
        if (envLoader != null) {
          parent = getLocalContext(envLoader.getParent());
        }

        context = createModuleContext(parent, loader);

        _localModuleContext.set(context, loader);

        context.init();
      }

      return context;
    }
  }

  @Override
  protected ModuleContext createModuleContext(ModuleContext parent,
                                              ClassLoader loader)
  {
    return new ResinModuleContext(parent, loader);
  }

  public String getCookieName()
  {
    SessionManager sm = getSessionManager();

    if (sm != null)
      return sm.getCookieName();
    else
      return "JSESSIONID";
  }

  @Override
  public Map getSessionCache()
  {
    if (_sessionCache == null && Server.getCurrent() != null) {
      CacheManager cacheManager = CacheManager.createManager();
      AbstractCache cache = cacheManager.create("resin:quercus:session");

      cache.setIdleTimeoutMillis(3600 * 1000L);

      _sessionCache = cache;
    }
    
    return _sessionCache;
  }
  
  @Override
  public void setSessionTimeout(long sessionTimeout)
  {
    if (_sessionCache == null)
      getSessionCache();
    
    _sessionCache.setIdleTimeoutMillis(sessionTimeout);
  }

  public SessionManager getSessionManager()
  {
    if (_webApp != null)
      return _webApp.getSessionManager();
    else
      return null;
  }

  @Override
  public long getDependencyCheckInterval()
  {
    return _dependencyCheckInterval;
  }

  @Override
  public String getVersion()
  {
    return VersionFactory.getVersion();
  }

  @Override
  public String getVersionDate()
  {
    return VersionFactory.getVersionDate();
  }

  @Override
  public DataSource findDatabase(String driver, String url)
  {
    try {
      if (getDatabase() != null)
        return getDatabase();
      else if (isConnectionPool())
        return DatabaseManager.findDatabase(url, driver);
      else
        return super.findDatabase(driver, url);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Unwrap connection if necessary.
   */
  @Override
  public Connection getConnection(Connection conn)
  {
    try {
      return ((UserConnection) conn).getConnection();
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }
  
  /**
   * Marks the connection for removal from the connection pool.
   */
  @Override
  public void markForPoolRemoval(Connection conn)
  {
    ManagedConnectionImpl mConn = ((UserConnection) conn).getMConn();
    
    String url = mConn.getURL();
    String driver = mConn.getDriverClass().getCanonicalName();
    
    DataSource ds = findDatabase(driver, url);
    
    ((DBPool) ds).markForPoolRemoval(mConn);
  }

  /**
   * Unwrap statement if necessary.
   */
  public Statement getStatement(Statement stmt)
  {
    return ((UserStatement) stmt).getStatement();
  }
  
  /**
   * Returns true if Quercus is running under Resin.
   */
  @Override
  public boolean isResin()
  {
    return true;
  }

  @Override
  public void start()
  {
    new Alarm(getQuercusSessionManager()).queue(60000);
    
    new WeakAlarm(new EnvTimeoutAlarmListener()).queue(_envTimeout);
  }
  
  class EnvTimeoutAlarmListener implements AlarmListener {
    public void handleAlarm(Alarm alarm)
    {
      try {
        ArrayList<Env> activeEnv = new ArrayList<Env>(_activeEnvSet.keySet());
      
        for (Env env : activeEnv) {
          env.updateTimeout();
        }
      } finally {
        if (! isClosed()) {
          alarm.queue(_envTimeout);
        }
      }
    }
  }
}

