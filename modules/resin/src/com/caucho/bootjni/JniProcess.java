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

package com.caucho.bootjni;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.caucho.config.ConfigException;
import com.caucho.inject.Module;
import com.caucho.util.JniTroubleshoot;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;
import com.caucho.server.util.CauchoSystem;

/**
 * Resin's bootstrap class.
 */
@Module
public class JniProcess extends Process
{
  private static final L10N L
    = new L10N(JniProcess.class);

  private static final JniTroubleshoot _jniTroubleshoot;
  
  private int _stdoutFd = -1;
  private int _pid = -1;
  private int _status = -1;

  private ReadStream _is;

  public JniProcess()
  {
  }

  private JniProcess(ArrayList<String> args,
                     HashMap<String,String> env,
                     String chroot,
                     String pwd,
                     String user,
                     String group)
  {
    String []argv = new String[args.size()];
    args.toArray(argv);

    String []envp = new String[env.size()];

    int i = 0;
    for (Map.Entry<String,String> entry : env.entrySet()) {
      envp[i++] = entry.getKey() + '=' + entry.getValue();
    }

    int fdMax = setFdMax();
    if (CauchoSystem.isUnix() && fdMax <= 0) {
      System.out.println(L.l("process file descriptors: {0}", fdMax));
    }

    if (! exec(argv, envp, chroot, pwd, user, group))
      throw new IllegalStateException("exec failed");

    int stdoutFd = _stdoutFd;
    _stdoutFd = -1;

    try {
      StreamImpl stream;

      Class<?> cl = Class.forName("com.caucho.vfs.JniFileStream",
                               false, getClass().getClassLoader());

      Constructor<?> ctor = cl.getConstructor(new Class[] { int.class, boolean.class, boolean.class });
      
      stream = (StreamImpl) ctor.newInstance(stdoutFd, true, false);
      
      _is = new ReadStream(stream);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public JniProcess create(ArrayList<String> args,
                           HashMap<String,String> env,
                           String chroot,
                           String pwd,
                           String user,
                           String group)
  {
    _jniTroubleshoot.checkIsValid();

    assert(isNativeBootAvailable());

    return new JniProcess(args, env, chroot, pwd, user, group);
  }

  public boolean isEnabled()
  {
    return _jniTroubleshoot.isEnabled() && isNativeBootAvailable();
  }
  
  public String getTroubleshootMessage()
  {
    return _jniTroubleshoot.getMessage();
  }
  
  public OutputStream getOutputStream()
  {
    return new NullOutputStream();
  }

  public InputStream getInputStream()
  {
    return _is;
  }

  public InputStream getErrorStream()
  {
    return getInputStream();
  }

  public int getPid()
  {
    return _pid;
  }

  public void chown(String path, String user, String group)
  {
    _jniTroubleshoot.checkIsValid();
    
    byte []name = path.getBytes();
    int len = name.length;

    nativeChown(name, len, user, group);
  }

  public int waitFor()
  {
    int pid = _pid;
    _pid = 0;
    
    if (pid > 0) {
      _status = waitpid(pid, true);
    }

    return _status;
  }

  public int exitValue()
  {
    if (_status >= 0)
      return _status;

    if (_pid > 0) {
      int result = waitpid(_pid, false);

      if (result < 0)
        throw new IllegalThreadStateException("Pid " + _pid + " not yet closed");
      _pid = 0;
      _status = result;
    }

    return _status;
  }

  public void destroy()
  {
  }

  static class NullOutputStream extends OutputStream {
    public void write(int ch)
    {
    }
    
    public void flush()
    {
    }
    
    public void close()
    {
    }
  }
  
  public native boolean isNativeBootAvailable();
  
  public native boolean clearSaveOnExec();

  public native static int getFdMax();
  public native static int setFdMax();
  
  private native boolean exec(String []argv,
                              String []envp,
                              String chroot,
                              String pwd,
                              String user,
                              String group);
  
  private native void nativeChown(byte []name, int length,
                                  String user, String group);
  
  private native int waitpid(int pid, boolean isBlock);

  static {
    JniTroubleshoot jniTroubleshoot = null;

    try {
      System.loadLibrary("resin_os");

      jniTroubleshoot = new JniTroubleshoot(JniProcess.class, "resin_os");
    } catch (Throwable e) {
      jniTroubleshoot = new JniTroubleshoot(JniProcess.class, "resin_os", e);
    }

    _jniTroubleshoot = jniTroubleshoot;
  }
}
