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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.service.AbstractResinService;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownService;
import com.caucho.env.thread.ThreadPool;
import com.caucho.env.warning.WarningService;
import com.caucho.log.EnvironmentStream;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

/**
 * The wait-for-exit service waits for Resin to exit.
 */
class ResinWaitForExitService extends AbstractResinService
{
  private static final Logger log 
    = Logger.getLogger(ResinWaitForExitService.class.getSimpleName());
  
  private static final L10N L = new L10N(ResinWaitForExitService.class);
  
  private Resin _resin;
  private ResinSystem _resinSystem;
  private InputStream _waitIn;
  private Socket _pingSocket;
  private ResinActor _resinActor;

  /**
   * Creates a new resin server.
   */
  ResinWaitForExitService(Resin resin, 
                          ResinSystem resinSystem,
                          InputStream waitIn,
                          Socket pingSocket)
  {
    _resin = resin;
    _resinSystem = resinSystem;
    _waitIn = waitIn;
    _pingSocket = pingSocket;
  }
  
  ResinActor getResinActor()
  {
    return _resinActor;
  }

  /**
   * Thread to wait until Resin should be stopped.
   */
  void waitForExit()
  {
    int socketExceptionCount = 0;
    Runtime runtime = Runtime.getRuntime();
    
    ShutdownService shutdown = _resinSystem.getService(ShutdownService.class);
    
    if (shutdown == null) {
      throw new IllegalStateException(L.l("'{0}' requires an active {1}",
                                          this,
                                          ShutdownService.class.getSimpleName()));
    }

    /*
     * If the server has a parent process watching over us, close
     * gracefully when the parent dies.
     */
    while (! _resin.isClosing()) {
      try {
        Thread.sleep(10);

        if (! checkMemory(runtime)) {
          shutdown.startFailSafeShutdown("Resin shutdown from out of memory");
          // dumpHeapOnExit();
          return;
        }

        if (! checkFileDescriptor()) {
          shutdown.startFailSafeShutdown("Resin shutdown from out of file descriptors");
          //dumpHeapOnExit();
          return;
        }

        if (_waitIn != null) {
          if (_waitIn.read() >= 0) {
            socketExceptionCount = 0;
          }
          else
            log.warning(L.l("Stopping due to watchdog or user."));

          return;
        }
        else {
          synchronized (this) {
            wait(10000);
          }
        }
      } catch (SocketTimeoutException e) {
        socketExceptionCount = 0;
      } catch (InterruptedIOException e) {
        socketExceptionCount = 0;
      } catch (InterruptedException e) {
        socketExceptionCount = 0;
      } catch (SocketException e) {
        // The Solaris JVM will throw SocketException periodically
        // instead of interrupted exception, so those exceptions need to
        // be ignored.

        // However, the Windows JVMs will throw connection reset by peer
        // instead of returning an end of file in the read.  So those
        // need to be trapped to close the socket.
        if (socketExceptionCount++ == 0) {
          log.log(Level.FINE, e.toString(), e);
        }
        else if (socketExceptionCount > 100)
          return;
      } catch (OutOfMemoryError e) {
        String msg = "Resin shutdown from out of memory";
        
        ShutdownService.shutdownActive(ExitCode.MEMORY, msg);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);

        return;
      }
    }
  }

  private boolean checkMemory(Runtime runtime)
    throws InterruptedException
  {
    long minFreeMemory = 0;//_resinConfig.getMinFreeMemory();

    if (minFreeMemory <= 0) {
      // memory check disabled
      return true;
    }
    else if (2 * minFreeMemory < getFreeMemory(runtime)) {
      // plenty of free memory
      return true;
    }
    else {
      if (log.isLoggable(Level.FINER)) {
        log.finer(L.l("free memory {0} max:{1} total:{2} free:{3}",
                          "" + getFreeMemory(runtime),
                          "" + runtime.maxMemory(),
                          "" + runtime.totalMemory(),
                          "" + runtime.freeMemory()));
      }

      log.info(L.l("Forcing GC due to low memory. {0} free bytes.",
                       getFreeMemory(runtime)));

      runtime.gc();

      Thread.sleep(1000);

      runtime.gc();

      if (getFreeMemory(runtime) < minFreeMemory) {
        log.severe(L.l("Restarting due to low free memory. {0} free bytes",
                           getFreeMemory(runtime)));

        return false;
      }
    }

    // second memory check
    allocateMemory();

    return true;
  }
  
  private Object allocateMemory()
  {
    return new Object();
  }

  private boolean checkFileDescriptor()
  {
    try {
      ReadStream is = _resin.getResinConf().openRead();
      is.close();

      return true;
    } catch (IOException e) {
      log.severe(L.l("Restarting due to file descriptor failure:\n{0}",
                     e));

      return false;
    }
  }

  private static long getFreeMemory(Runtime runtime)
  {
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();

    // Some JDKs (JRocket) return 0 for the maxMemory
    if (maxMemory < totalMemory)
      return freeMemory;
    else
      return maxMemory - totalMemory + freeMemory;
  }

  void startResinActor()
    throws IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_resinSystem.getClassLoader());
      
      _resinActor = new ResinActor(_resin);
      
      WarningService warning = WarningService.create();
      
      warning.addHandler(new ResinWarningHandler(_resinActor));

      if (_pingSocket != null) {
        InputStream is = _pingSocket.getInputStream();
        OutputStream os = _pingSocket.getOutputStream();

        ResinLink link = new ResinLink(_resinActor, is, os);

        ThreadPool.getThreadPool().schedule(link);
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + _resinSystem.getId() + "]";
  }

}
