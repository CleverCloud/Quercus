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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bootjni.JniProcess;
import com.caucho.config.ConfigException;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.thread.ThreadPool;
import com.caucho.hmtp.HmtpLink;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.log.RotateStream;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Encapsulation of the process running resin.
 *
 * Each WatchdogProcess instance corresponds to a Resin JVM.  When the
 * JVM exits, the WatchdogProcess will finish.  It is not reused on a
 * restart.
 */
class WatchdogChildProcess
{
  private static final L10N L = new L10N(WatchdogChildProcess.class);
  private static final Logger log
    = Logger.getLogger(WatchdogChildProcess.class.getName());

  private static Boot _jniBoot;

  private final String _id;
  private final WatchdogChild _watchdog;
  private final Lifecycle _lifecycle = new Lifecycle();

  private WatchdogActor _watchdogActor;

  private Socket _childSocket;
  private Process _process;
  private OutputStream _stdOs;
  private int _pid;

  private int _status = -1;

  WatchdogChildProcess(String id, WatchdogChild watchdog)
  {
    _id = id;
    _watchdog = watchdog;
  }

  int getPid()
  {
    return _pid;
  }
  
  public String getId()
  {
    return _id;
  }
  
  /**
   * General queries of the Resin instance.
   */
  Serializable queryGet(Serializable payload)
  {
    if (_watchdogActor != null)
      return _watchdogActor.queryGet(payload);
    else
      return null;
  }

  public void run()
  {
    if (! _lifecycle.toActive())
      return;
    
    WriteStream jvmOut = null;
    ServerSocket ss = null;
    Socket s = null;

    try {
      ss = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));

      int port = ss.getLocalPort();

      log.warning("Watchdog starting Resin[" + _watchdog.getId() + "]");

      jvmOut = createJvmOut();

      _process = createProcess(port, jvmOut);
      
      if (_process != null) {
        if (_process instanceof JniProcess)
          _pid = ((JniProcess) _process).getPid();
        else
          _pid = 0;

        InputStream stdIs = _process.getInputStream();
        _stdOs = _process.getOutputStream();

        WatchdogProcessLogThread logThread
          = new WatchdogProcessLogThread(stdIs, jvmOut);

        ThreadPool.getCurrent().schedule(logThread);

        s = connectToChild(ss);

        _status = _process.waitFor();
        
        logStatus(_status);
      }
    } catch (Exception e) {
      log.log(Level.INFO, e.toString(), e);

      try {
        Thread.sleep(5000);
      } catch (Exception e1) {
      }
    } finally {
      if (ss != null) {
        try {
          ss.close();
        } catch (Exception e) {
        }
      }

      try {
        if (s != null)
          s.close();
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }

      kill();

      if (jvmOut != null && ! _watchdog.isConsole()) {
        try {
          synchronized (jvmOut) {
            jvmOut.close();
          }
        } catch (Exception e) {
        }
      }

      synchronized (this) {
        if (_status < 0)
          _status = 666;

        notifyAll();
      }
    }
  }
  
  private void logStatus(int status)
  {
    String type = "unknown";
    
    if (status == 0)
      type = "normal exit";
    else if (status >= 0 && status < ExitCode.values().length) {
      type = ExitCode.values()[status].toString();
    }
    else if (status > 128 && status < 128 + 31) {
      switch (status - 128) {
      case 1:
        type = "SIGHUP";
        break;
      case 2:
        type = "SIGINT";
        break;
      case 3:
        type = "SIGQUIT";
        break;
      case 7:
        type = "SIGBUS";
        break;
      case 9:
        type = "SIGKILL";
        break;
      case 11:
        type = "SIGSEGV";
        break;
      case 14:
        type = "SIGALRM";
        break;
      case 19:
        type = "SIGSTOP";
        break;
      default:
        type = "signal=" + (status - 128);
        break;
      }
    }
    
    log.warning("Watchdog detected close of "
                + "Resin[" + _watchdog.getId() + ",pid=" + _pid + "]"
                + "\n  exit reason: " + type + " (exit code=" + status + ")");
  }

  /**
   * Stops the instance, waiting for the completion.
   */
  void stop()
  {
    _lifecycle.toDestroy();

    if (_watchdogActor != null)
      _watchdogActor.sendShutdown();
  }

  void kill()
  {
    _lifecycle.toDestroy();

    OutputStream stdOs = _stdOs;
    _stdOs = null;

    if (stdOs != null) {
      try {
        stdOs.close();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    Socket childSocket = _childSocket;
    _childSocket = null;

    if (childSocket != null) {
      try {
        childSocket.close();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    Process process = _process;
    _process = null;

    if (process != null) {
      try {
        process.destroy();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }

      try {
        process.waitFor();
      } catch (Exception e) {
        log.log(Level.INFO, e.toString(), e);
      }
    }
  }

  void waitForExit()
  {
    synchronized (this) {
      if (_status < 0) {
        try {
          wait(60000);
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * Waits for a socket connection from the child, returning the socket
   *
   * @param ss TCP ServerSocket from the watchdog for the child to connect to
   */
  private Socket connectToChild(ServerSocket ss)
    throws IOException
  {
    Socket s = null;

    try {
      ss.setSoTimeout(60000);

      for (int i = 0; _lifecycle.isActive() && i < 120 && s == null; i++) {
        try {
          s = ss.accept();
        } catch (SocketTimeoutException e) {
        }
      }

      if (s != null) {
        _childSocket = s;

        startWatchdogActor(s);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      ss.close();
    }

    return s;
  }

  /**
   * Starts the BAM actor to communicate with the child.
   *
   * @param s the socket to the child.
   */
  private void startWatchdogActor(Socket s)
    throws IOException
  {
    InputStream watchdogIs = s.getInputStream();
    OutputStream watchdogOs = s.getOutputStream();

    _watchdogActor = new WatchdogActor(this);

    HmtpLink link = new HmtpLink(_watchdogActor, watchdogIs, watchdogOs);

    try {
      ThreadPool.getCurrent().schedule(link);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Creates a new Process for the Resin JVM, initializing the environment
   * and passing value to the new process.
   *
   * @param socketPort the watchdog socket port
   * @param out the debug log jvm-default.log
   */
  private Process createProcess(int socketPort,
                                WriteStream out)
    throws IOException
  {
    // watchdog/0210
    // Path pwd = rootDirectory;
    Path chroot = _watchdog.getChroot();
    Path processPwd = _watchdog.getPwd();

    HashMap<String,String> env = buildEnv();

    ArrayList<String> jvmArgs = buildJvmArgs();

    ArrayList<String> resinArgs = buildResinArgs(socketPort);

    addCommandLineArguments(jvmArgs, resinArgs);

    jvmArgs.add("com.caucho.server.resin.Resin");

    jvmArgs.addAll(resinArgs);

    if (_watchdog.isVerbose()) {
      logVerboseArguments(out, jvmArgs);

      logVerboseEnvironment(out, env);
    }

    Boot boot = getJniBoot();
    if (boot != null) {
      boot.clearSaveOnExec();

      ArrayList<QServerSocket> boundSockets = new ArrayList<QServerSocket>();

      try {
        if (_watchdog.getUserName() != null) {
          for (SocketLinkListener port : _watchdog.getPorts()) {
            QServerSocket ss = port.bindForWatchdog();

            if (ss == null)
              continue;

            boundSockets.add(ss);

            if (ss.setSaveOnExec()) {
              jvmArgs.add("-port");
              jvmArgs.add(String.valueOf(ss.getSystemFD()));
              jvmArgs.add(String.valueOf(port.getAddress()));
              jvmArgs.add(String.valueOf(port.getPort()));
            }
            else {
              ss.close();
            }
          }
        }

        String chrootPath = null;

        if (chroot != null)
          chrootPath = chroot.getNativePath();

        Process process = boot.exec(jvmArgs, env,
                                    chrootPath,
                                    processPwd.getNativePath(),
                                    _watchdog.getUserName(),
                                    _watchdog.getGroupName());

        if (process != null) {
          return process;
        }
      } catch (ConfigException e) {
        log.warning(e.getMessage());
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        for (int i = 0; i < boundSockets.size(); i++) {
          try {
            boundSockets.get(i).close();
          } catch (Throwable e) {
          }
        }
      }
    }

    if (_watchdog.getUserName() != null) {
      if (_watchdog.isConsole())
        throw new ConfigException(L.l("<user-name> requires compiled JNI started with 'start'.  Resin cannot use <user-name> when started as a console process."));
      else
        throw new ConfigException(L.l("<user-name> requires compiled JNI."));
    }

    if (_watchdog.getGroupName() != null) {
      if (_watchdog.isConsole())
        throw new ConfigException(L.l("<group-name> compiled JNI started with 'start'.  Resin cannot use <group-name> when started as a console process."));
      else
        throw new ConfigException(L.l("<group-name> compiled JNI."));
    }

    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(processPwd.getNativePath()));

    builder.environment().putAll(env);

    builder = builder.command(jvmArgs);

    builder.redirectErrorStream(true);

    return builder.start();
  }

  private HashMap<String,String> buildEnv()
    throws IOException
  {
    HashMap<String,String> env = new HashMap<String,String>();

    env.putAll(System.getenv());

    Path resinHome = _watchdog.getResinHome();

    ArrayList<String> classPathList = new ArrayList<String>();
    classPathList.addAll(_watchdog.getJvmClasspath());
    String classPath
      = WatchdogArgs.calculateClassPath(classPathList, resinHome);

    env.put("CLASSPATH", classPath);

    if (_watchdog.is64bit()) {
      WatchdogClient.appendEnvPath(env,
                    "LD_LIBRARY_PATH",
                    resinHome.lookup("libexec64").getNativePath());
      WatchdogClient.appendEnvPath(env,
                    "DYLD_LIBRARY_PATH",
                    resinHome.lookup("libexec64").getNativePath());
      if (CauchoSystem.isWindows())
        WatchdogClient.appendEnvPath(env,
                      "Path",
                      resinHome.lookup("win64").getNativePath());
    }
    else {
      WatchdogClient.appendEnvPath(env,
                    "LD_LIBRARY_PATH",
                    resinHome.lookup("libexec").getNativePath());
      WatchdogClient.appendEnvPath(env,
                    "DYLD_LIBRARY_PATH",
                    resinHome.lookup("libexec").getNativePath());

      if (CauchoSystem.isWindows())
        WatchdogClient.appendEnvPath(env,
                      "Path",
                      resinHome.lookup("win32").getNativePath());
    }

    return env;
  }

  private ArrayList<String> buildJvmArgs()
  {
    ArrayList<String> jvmArgs = new ArrayList<String>();

    jvmArgs.add(_watchdog.getJavaExe());

    // user args are first so they're displayed by ps
    for (String arg : _watchdog.getJvmArgs()) {
      if (! arg.startsWith("-Djava.class.path"))
        jvmArgs.add(arg);
    }
    
    jvmArgs.add("-Dresin.server=" + _id);

    jvmArgs.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");

    // This is needed for JMX to work correctly.
    String systemClassLoader = _watchdog.getSystemClassLoader();
    if (systemClassLoader != null && ! "".equals(systemClassLoader)) {
      jvmArgs.add("-Djava.system.class.loader=" + systemClassLoader);
    }
    // #2567
    jvmArgs.add("-Djavax.management.builder.initial=com.caucho.jmx.MBeanServerBuilderImpl");
    jvmArgs.add("-Djava.awt.headless=true");

    Path resinHome = _watchdog.getResinHome();
    jvmArgs.add("-Dresin.home=" + resinHome.getFullPath());

    if (! _watchdog.hasXss())
      jvmArgs.add("-Xss1m");

    if (! _watchdog.hasXmx())
      jvmArgs.add("-Xmx256m");

    String[] argv = _watchdog.getArgv();

    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];

      if (arg.startsWith("-D") || arg.startsWith("-X")) {
        jvmArgs.add(arg);
      }
      else if (arg.startsWith("-J")) {
        jvmArgs.add(arg.substring(2));
      }
      else if ("--debug-port".equals(arg) || "-debug-port".equals(arg)) {
        jvmArgs.add("-Xdebug");
        jvmArgs.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address="
                    + argv[i + 1]);
        i++;
      }
      else if ("--jmx-port".equals(arg) || "-jmx-port".equals(arg)) {
        jvmArgs.add("-Dcom.sun.management.jmxremote.port=" + argv[i + 1]);
        jvmArgs.add("-Dcom.sun.management.jmxremote.authenticate=false");
        jvmArgs.add("-Dcom.sun.management.jmxremote.ssl=false");
        i++;
      }
    }

    if (! jvmArgs.contains("-d32") && ! jvmArgs.contains("-d64")
        && _watchdog.is64bit() && ! CauchoSystem.isWindows()) {
      jvmArgs.add("-d64");
    }

    if (! jvmArgs.contains("-server")
        && ! jvmArgs.contains("-client")
        && ! CauchoSystem.isWindows()) {
      // #3331, windows can't add -server automatically
      jvmArgs.add("-server");
    }

    return jvmArgs;
  }

  private ArrayList<String> buildResinArgs(int socketPort)
  {
    ArrayList<String> resinArgs = new ArrayList<String>();

    Path resinRoot = _watchdog.getResinRoot();

    if (resinRoot != null) {
      resinArgs.add("--root-directory");
      resinArgs.add(resinRoot.getFullPath());
    }

    if (_watchdog.getResinConf() != null) {
      resinArgs.add("-conf");
      resinArgs.add(_watchdog.getResinConf().getNativePath());
    }

    resinArgs.add("-socketwait");
    resinArgs.add(String.valueOf(socketPort));

    return resinArgs;
  }

  private void addCommandLineArguments(ArrayList<String> jvmArgs,
                                       ArrayList<String> resinArgs)
  {
    String []argv = _watchdog.getArgv();
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-conf")) {
        // resin conf handled below
        i++;
      }
      else if (argv[i].startsWith("-Djava.class.path=")) {
        // IBM JDK startup issues
      }
      else if (argv[i].startsWith("-J")) {
        jvmArgs.add(argv[i].substring(2));
      }
      else if (argv[i].startsWith("-Djava.class.path")) {
      }
      else
        resinArgs.add(argv[i]);
    }
  }

  private void logVerboseArguments(WriteStream out, ArrayList<String> list)
    throws IOException
  {
    for (int i = 0; i < list.size(); i++) {
      if (i > 0)
        out.print("  ");

      out.print(list.get(i));

      if (i + 1 < list.size())
        out.println(" \\");
      else
        out.println();
    }
  }

  private void logVerboseEnvironment(WriteStream out, Map<String,String> env)
    throws IOException
  {
    for (Map.Entry<String, String> envEntry : env.entrySet()) {
      String key = envEntry.getKey();
      String value = envEntry.getValue();

      if ("CLASSPATH".equals(key)
          || "LD_LIBRARY_PATH".equals(key)
          || "DYLD_LIBRARY_PATH".equals(key)) {
        out.println(key + ": ");

        int len = (key + ": ").length();

        for (String v : value.split("[" + File.pathSeparatorChar + "]")) {
          for (int i = 0; i < len; i++)
            out.print(" ");

          out.println(v);
        }
      }
      else
        out.println("" + key + ": " + value);
    }
  }

  Boot getJniBoot()
  {
    if (_jniBoot != null)
      return _jniBoot.isValid() ? _jniBoot : null;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName("com.caucho.bootjni.JniBoot", false, loader);
      _jniBoot = (Boot) cl.newInstance();
    } catch (ClassNotFoundException e) {
      log.fine(e.toString());
    } catch (IllegalStateException e) {
      log.fine(e.toString());
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return _jniBoot != null && _jniBoot.isValid() ? _jniBoot : null;
  }

  //
  // logging utilities
  //

  /**
   * Creates the log/jvm-default.log file where all the Resin log messages
   * go.
   */
  private WriteStream createJvmOut()
    throws IOException
  {
    if (_watchdog.isConsole()) {
      return Vfs.openWrite(System.out);
    }

    Path jvmPath = _watchdog.getLogPath();

    try {
      Path dir = jvmPath.getParent();

      if (! dir.exists()) {
        dir.mkdirs();

        String userName = _watchdog.getUserName();
        if (userName != null)
          dir.changeOwner(userName);

        String groupName = _watchdog.getGroupName();
        if (groupName != null)
          dir.changeGroup(groupName);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    RotateStream rotateStream = RotateStream.create(jvmPath);
    rotateStream.getRolloverLog().setRolloverSizeBytes(64L * 1024 * 1024);
    _watchdog.getConfig().logInit(rotateStream);
    rotateStream.init();
    return rotateStream.getStream();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _watchdog + "," + _id + "]";
  }

  /**
   * Watchdog thread responsible for writing jvm-default.log by reading the
   * JVM's stdout and copying it to the log.
   */
  class WatchdogProcessLogThread implements Runnable {
    private InputStream _is;
    private WriteStream _out;

    /**
     * @param is the stdout stream from the Resin
     * @param out stream to the log/jvm-default.log file
     */
    WatchdogProcessLogThread(InputStream is, WriteStream out)
    {
      _is = is;
      _out = out;
    }

    @Override
    public void run()
    {
      Thread thread = Thread.currentThread();
      thread.setName("watchdog-process-log-" + _pid + "-" + _id);

      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
      
      WriteStream out = _out;

      try {
        int len;

        byte []data = new byte[4096];

        while ((len = _is.read(data, 0, data.length)) > 0) {
          out.write(data, 0, len);
          out.flush();
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        try {
          if (! _watchdog.isConsole()) {
            synchronized (out) {
              out.close();
            }
          }
        } catch (IOException e) {
        }

        kill();
      }
    }
  }
}
