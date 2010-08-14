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
 * @author Sam
 */

package com.caucho.netbeans;

import com.caucho.netbeans.PluginL10N;
import com.caucho.netbeans.PluginLogger;

import org.openide.execution.NbProcessDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResinProcess
{
  private static final PluginL10N L = new PluginL10N(ResinProcess.class);
  private static final Logger log
    = Logger.getLogger(ResinProcess.class.getName());

  public static final int LIFECYCLE_NEW = 0;
  public static final int LIFECYCLE_INITIALIZING = 1;
  public static final int LIFECYCLE_INIT = 2;
  public static final int LIFECYCLE_STARTING = 3;
  public static final int LIFECYCLE_STANDBY = 4;
  public static final int LIFECYCLE_WARMUP = 5;
  public static final int LIFECYCLE_ACTIVE = 6;
  public static final int LIFECYCLE_FAILED = 7;
  public static final int LIFECYCLE_STOPPING = 8;
  public static final int LIFECYCLE_STOPPED = 9;
  public static final int LIFECYCLE_DESTROYING = 10;
  public static final int LIFECYCLE_DESTROYED = 11;

  private int _lifecycle = LIFECYCLE_NEW;

  private static final int TIMEOUT_TICK = 250;

  private final String _uri;
  private final ResinConfiguration _resinConfiguration;

  private boolean _isDebug;
  private int _activeServerPort;
  private int _activeDebugPort;

  private Process _process;
  private Console _console;

  private final Object _lock = new Object();
  private File _javaExe;
  private File _resinJar;

  public ResinProcess(String uri, ResinConfiguration resinConfiguration)
  {
    _uri = uri;
    _resinConfiguration = resinConfiguration;
  }

  public ResinConfiguration getResinConfiguration()
  {
    return _resinConfiguration;
  }

  private boolean isInit()
  {
    return _lifecycle >= LIFECYCLE_INIT;
  }

  public boolean isActive()
  {
    return _lifecycle == LIFECYCLE_ACTIVE;
  }

  public void init()
    throws IllegalStateException
  {
    synchronized(_lock) {
      if (isInit())
        return;

      _lifecycle = LIFECYCLE_INITIALIZING;

      _resinConfiguration.validate();

      File javaExe;

      File javaHome = _resinConfiguration.calculateJavaHome();

      javaExe = new File(javaHome, "bin/java");

      if (!javaExe.exists())
        javaExe = new File(javaHome, "bin/java.exe");

      if (!javaExe.exists())
        throw new IllegalStateException(L.l("Cannot find java exe in ''{0}''", javaHome));

      _javaExe = javaExe;

      File resinHome = _resinConfiguration.getResinHome();

      File resinJar = new File(resinHome, "lib/resin.jar");

      if (!resinJar.exists())
        throw new IllegalStateException(L.l("Cannot find lib/resin.jar in ''{0}''", resinHome));

      _resinJar = resinJar;

      _lifecycle = LIFECYCLE_INIT;
    }
  }

  public void start()
    throws IllegalStateException, IOException
  {
    init();

    synchronized(_lock) {
      if (isActive())
        stopImpl(false);

      _isDebug = false;

      startImpl();
    }
  }

  public void startDebug()
    throws IllegalStateException, IOException
  {
    init();

    synchronized(_lock) {
      if (isActive())
        stopImpl(false);

      _isDebug = true;

      startImpl();
    }
  }

  public boolean isDebug()
  {
    return _isDebug;
  }

  private void startImpl()
    throws IllegalStateException, IOException
  {
    _lifecycle = LIFECYCLE_STARTING;

    int serverPort = _resinConfiguration.getServerPort();
    int debugPort = _resinConfiguration.getDebugPort();
    File resinHome = _resinConfiguration.getResinHome();
    File resinConf = _resinConfiguration.getResinConf();
    String serverId = _resinConfiguration.getServerId();
    log.info("Resin starting on " + serverPort);
    if (!isPortFree(serverPort))
      throw new IllegalStateException(L.l("Cannot start Resin, server-port {0} is already in use", serverPort));

    if (_isDebug) {
      if (debugPort == 0) {
        ServerSocket ss = new ServerSocket(0, 5,
                                           InetAddress.getByName("127.0.0.1"));

        debugPort = ss.getLocalPort();

        try {
          Thread.sleep(100);
        }
        catch (InterruptedException e) {
        }
        ss.close();
      }

      if (!isPortFree(debugPort))
        throw new IllegalStateException(L.l("Cannot start Resin, debug-port {0} is already in use", debugPort));
    }
    
    StringBuilder cp = new StringBuilder();
    File lib = new File(resinHome, "lib");
    for (String jar : lib.list()) {
      if (jar.endsWith(".jar")) {
        cp.append(File.pathSeparatorChar);
        cp.append(new File(lib, jar).getAbsolutePath());
      }
    }

    StringBuilder args = new StringBuilder();

    args.append(" -Dresin.home='" + resinHome + "'");
    args.append(" com.caucho.resin.ResinEmbed");
    
    args.append(" --port=");
    args.append(serverPort);
    args.append(" --deploy:role=any");

    if (_isDebug)
      throw new IllegalStateException("debug mode not implemented");

    // open the ServerLog
    synchronized (this) {
      if (_console != null) {
        _console.takeFocus();
      }
      else {
        _console = new Console(_uri);
      }
    }

    String classpath = null;
    
    String []envp = new String[] { "CLASSPATH=" + cp };
    
    String displayName = _resinConfiguration.getDisplayName();

    NbProcessDescriptor processDescriptor
      = new NbProcessDescriptor(_javaExe.getAbsolutePath(),
                                args.toString(),
                                displayName);

    _console.println(L.l("Starting Resin process {0} {1}",
                         processDescriptor.getProcessName(),
                         processDescriptor.getArguments()));

    _console.flush();

    _process = processDescriptor.exec(null, envp, true, resinHome);

    _console.println();

    _console.start(new InputStreamReader(_process.getInputStream()),
                   new InputStreamReader(_process.getErrorStream()));

    new Thread("resin-" + _uri + "-process-monitor")
    {
      public void run()
      {
        try {
          _process.waitFor();
          Thread.sleep(2000);
        }
        catch (InterruptedException e) {
        }
        finally {
          handleProcessDied();
        }
      }
    }.start();

    // wait for server port to become active

    _activeServerPort = serverPort;
    _activeDebugPort = debugPort;

    int startTimeout =  _resinConfiguration.getStartTimeout();

    boolean isResponding = false;

    for (int i = startTimeout; i > 0; i-= TIMEOUT_TICK) {
      if (isResponding()) {
        isResponding = true;
        break;
      }

      try {
        Thread.sleep(TIMEOUT_TICK);
      }
      catch (InterruptedException ex) {
        log.log(Level.WARNING, ex.toString(), ex);
      }
    }

    if (!isResponding) {
      String msg = L.l("Resin process failed to respond on server-port {0}", serverPort);

      log.log(Level.WARNING, msg);

      try {
        stopImpl(false);
      }
      catch (Exception ex) {
        log.log(Level.WARNING, ex.toString(), ex);
      }

      throw new IOException(msg);
    }


    _lifecycle = LIFECYCLE_ACTIVE;
  }

  public Console getConsole()
  {
    return _console;
  }

  private static boolean isPortFree(int port)
  {
    ServerSocket ss = null;

    try {
      ss = new ServerSocket(port);
      return true;
    }
    catch (IOException ioe) {
      return false;
    }
    finally {
      if (ss != null) {
        try {
          ss.close();
        }
        catch (IOException ex) {
        }
      }
    }
  }

  public String getHttpUrl()
  {
    return null;
  }

  /**
   * Return true if the process is running
   */
  public boolean isProcessRunning()
  {
    Process process = _process;

    if (process != null) {
      try {
        process.exitValue();

        return false;
      }
      catch (IllegalThreadStateException e) {
        return true;
      }
    }

    return false;
  }

  /**
   * Return true if the server is responding
   */
  public boolean isResponding()
  {
    // XXX: could be more robust, contact the server and make sure there is a response
    return _activeServerPort > 0 && !isPortFree(_activeServerPort);
  }

  public Process getJavaProcess()
  {
    return _process;
  }

  private void handleProcessDied()
  {
    stopImpl(false);
  }

  public void stop()
  {
    synchronized(_lock) {

      if (_lifecycle != LIFECYCLE_ACTIVE)
        return;

      stopImpl(true);
    }
  }

  private void stopImpl(boolean isGraceful)
  {
    _lifecycle = LIFECYCLE_STOPPING;

    Process process = _process;
    _process = null;

    Console console = _console;
    _console = null;

    int activeServerPort = _activeServerPort;
    _activeServerPort = 0;

    int activeDebugPort = _activeDebugPort;
    _activeDebugPort = 0;

    _isDebug = false;

    try {
      // XXX: graceful shutdown, send message to server,
      // then use isPortFree in a while loop that times out

      /*
      if (isGraceful) {
      try {
        printConsoleLine(L.l("Stopping Resin process ..."));
      }
      catch (Exception ex) {
        // no-op
      }

      for (int i = STOP_TIMEOUT; !isPortFree(activeServerPort) && i > 0; i-= TICK) {
        try {
          Thread.sleep(TICK);
        }
        catch (InterruptedException ex) {
          if (log.isLoggable(Level.WARNING))
            log.log(Level.WARNING, e), ex);

        }
      }
      }
      */
    }
    finally {

      try {
        if (process != null)
          process.destroy();
      }
      finally {
        try {
          console.println(L.l("Resin process destroyed"));
          console.flush();
        }
        catch (Exception ex) {
          // no-op
        }

        if (console != null)
          console.destroy();
      }
    }

    _lifecycle = LIFECYCLE_STOPPED;
  }

  public void destroy()
  {
    synchronized(_lock) {
      _lifecycle = LIFECYCLE_DESTROYING;

      try {
        try {
          stop();
        }
        catch (Exception ex) {
          log.log(Level.WARNING, ex.toString(), ex);

          try {
            stopImpl(false);
          }
          catch (Exception ex2) {
            log.log(Level.WARNING, ex2.toString(), ex);
          }
        }
      }
      finally {
        _lifecycle = LIFECYCLE_DESTROYED;
      }
    }
  }
}
