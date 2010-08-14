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

package com.caucho.boot;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.env.service.ResinSystem;
import com.caucho.loader.Environment;
import com.caucho.server.resin.ResinELContext;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * ResinBoot is the main bootstrap class for Resin.  It parses the
 * resin.xml and looks for the &lt;server> block matching the -server
 * argument.
 *
 * <h3>Start Modes:</h3>
 *
 * The start modes are STATUS, DIRECT, START, STOP, KILL, RESTART, SHUTDOWN.
 *
 * <ul>
 * <li>DIRECT starts a <server> from the command line
 * <li>START starts a <server> with a Watchdog in the background
 * <li>STOP stop the <server> Resin in the background
 * </ul>
 */
public class ResinBoot {
  private static L10N _L;
  private static Logger _log;

  private WatchdogArgs _args;

  private WatchdogClient _client;
  private ResinGUI _ui;

  ResinBoot(String []argv)
    throws Exception
  {
    _args = new WatchdogArgs(argv);

    Path resinHome = _args.getResinHome();

    ClassLoader loader = ProLoader.create(resinHome);
    if (loader != null) {
      System.setProperty("resin.home", resinHome.getNativePath());

      Thread.currentThread().setContextClassLoader(loader);

      Environment.init();

      Vfs.initJNI();

      resinHome = Vfs.lookup(resinHome.getFullPath());

      _args.setResinHome(resinHome);
    }
    else {
      Environment.init();
    }
    
    String jvmVersion = System.getProperty("java.runtime.version");
    
    if ("1.6".compareTo(jvmVersion) > 0) {
      throw new ConfigException(L().l("Resin requires Java 1.6 or later but was started with {0}",
                                      jvmVersion));
    }

    // required for license check
    System.setProperty("resin.home", resinHome.getNativePath());

    // watchdog/0210
    // Vfs.setPwd(_rootDirectory);

    if (! _args.getResinConf().canRead()) {
      throw new ConfigException(L().l("Resin/{0} can't open configuration file '{1}'",
                                      VersionFactory.getVersion(),
                                      _args.getResinConf().getNativePath()));
    }
    
    Path rootDirectory = _args.getRootDirectory();
    Path dataDirectory = rootDirectory.lookup("watchdog-data");
    
    ResinSystem networkServer = new ResinSystem("watchdog",
                                                rootDirectory,
                                                dataDirectory);

    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(networkServer.getClassLoader());

    Config config = new Config();
    BootResinConfig bootManager = new BootResinConfig(_args);

    ResinELContext elContext = _args.getELContext();

    /**
     * XXX: the following setVar calls should not be necessary, but the
     * EL.setEnviornment() call above is not effective:
     */
    InjectManager beanManager = InjectManager.create();

    Config.setProperty("resinHome", elContext.getResinHome());
    Config.setProperty("java", elContext.getJavaVar());
    Config.setProperty("resin", elContext.getResinVar());
    Config.setProperty("server", elContext.getServerVar());
    Config.setProperty("system", System.getProperties());
    Config.setProperty("getenv", System.getenv());

    ResinConfigLibrary.configure(beanManager);

    config.configure(bootManager, _args.getResinConf(),
                     "com/caucho/server/resin/resin.rnc");

    if (_args.isDynamicServer()) {
      _client = bootManager.addDynamicClient(_args);
    }
    else {
      _client = bootManager.findClient(_args.getServerId());
    }

    if (_client == null) {
      throw new ConfigException(L().l("Resin/{0}: -server '{1}' does not match any defined <server>\nin {2}.",
                                      VersionFactory.getVersion(), _args.getServerId(), _args.getResinConf()));
    }
  }

  boolean start()
    throws Exception
  {
    if (_args.isStatus()) {
      try {
        String status = _client.statusWatchdog();

        System.out.println(L().l("Resin/{0} status for watchdog at {1}:{2}",
                                 VersionFactory.getVersion(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort()));
        System.out.println(status);
      } catch (Exception e) {
        System.out.println(L().l("Resin/{0} can't retrieve status of -server '{1}' for watchdog at {2}:{3}.\n{4}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort(),
                                 e.toString()));

        log().log(Level.FINE, e.toString(), e);

        System.exit(1);
      }

      return false;
    }
    else if (_args.isStart()) {
      try {
        _client.startWatchdog(_args.getArgv());

        System.out.println(L().l("Resin/{0} started -server '{1}' for watchdog at {2}:{3}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort()));
      } catch (Exception e) {
        String eMsg;

        if (e instanceof ConfigException)
          eMsg = e.getMessage();
        else
          eMsg = e.toString();

        System.out.println(L().l("Resin/{0} can't start -server '{1}' for watchdog at {2}:{3}.\n  {4}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort(),
                                 eMsg));

        log().log(Level.FINE, e.toString(), e);

        System.exit(1);
      }

      return false;
    }
    else if (_args.isStop()) {
      try {
        _client.stopWatchdog();

        System.out.println(L().l("Resin/{0} stopped -server '{1}' for watchdog at {2}:{3}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort()));
      } catch (Exception e) {
        System.out.println(L().l("Resin/{0} can't stop -server '{1}' for watchdog at {2}:{3}.\n{4}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort(),
                                 e.toString()));

        log().log(Level.FINE, e.toString(), e);

        System.exit(1);
      }

      return false;
    }
    else if (_args.isKill()) {
      try {
        _client.killWatchdog();

        System.out.println(L().l("Resin/{0} killed -server '{1}' for watchdog at {2}:{3}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort()));
      } catch (Exception e) {
        System.out.println(L().l("Resin/{0} can't kill -server '{1}' for watchdog at {2}:{3}.\n{4}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort(),
                                 e.toString()));

        log().log(Level.FINE, e.toString(), e);

        System.exit(1);
      }

      return false;
    }
    else if (_args.isRestart()) {
      try {
        _client.restartWatchdog(_args.getArgv());

        System.out.println(L().l("Resin/{0} restarted -server '{1}' for watchdog at {2}:{3}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort()));
      } catch (Exception e) {
        System.out.println(L().l("Resin/{0} can't restart -server '{1}'.\n{2}",
                                 VersionFactory.getVersion(), _client.getId(),
                                 e.toString()));

        log().log(Level.FINE, e.toString(), e);

        System.exit(1);
      }

      return false;
    }
    else if (_args.isShutdown()) {
      try {
        _client.shutdown();

        System.out.println(L().l("Resin/{0} shutdown watchdog at {1}:{2}",
                                 VersionFactory.getVersion(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort()));
      } catch (Exception e) {
        System.out.println(L().l("Resin/{0} can't shutdown watchdog at {1}:{2}.\n{3}",
                                 VersionFactory.getVersion(),
                                 _client.getWatchdogAddress(),
                                 _client.getWatchdogPort(),
                                 e.toString()));

        log().log(Level.FINE, e.toString(), e);

        System.exit(1);
      }

      return false;
    }
    else if (_args.isConsole()) {
      return _client.startConsole() != 0;
    }
    else if (_args.isGui()) {
      if (_ui != null && _ui.isVisible())
        return true;
      else if (_ui != null)
        return false;

      _ui = new ResinGUI(this, _client);
      _ui.setVisible(true);

      return true;
    }
    else {
      throw new IllegalStateException(L().l("Unknown start mode"));
    }
  }

  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.xml  : alternate configuration file
   * -server web-a    : &lt;server> to start
   * <pre>
   */
  public static void main(String []argv)
  {
    if (System.getProperty("log.level") != null)
      Logger.getLogger("").setLevel(Level.FINER);

    try {
      ResinBoot boot = new ResinBoot(argv);

      while (boot.start()) {
        try {
          synchronized (boot) {
            boot.wait(5000);
          }
        } catch (Exception e) {
        }
      }
      
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();

      if (e instanceof ConfigException) {
        System.out.println(e.getMessage());

        System.exit(2);
      }
      else {
        e.printStackTrace();

        System.exit(3);
      }
    }
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(ResinBoot.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(ResinBoot.class.getName());

    return _log;
  }
}
