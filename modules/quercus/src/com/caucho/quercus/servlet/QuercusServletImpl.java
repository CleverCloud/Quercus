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

package com.caucho.quercus.servlet;

import com.caucho.java.WorkDir;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusDieException;
import com.caucho.quercus.QuercusErrorException;
import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.QuercusLineRuntimeException;
import com.caucho.quercus.QuercusRequestAdapter;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusValueException;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class QuercusServletImpl extends HttpServlet
{
  private static final L10N L = new L10N(QuercusServletImpl.class);
  private static final Logger log
    = Logger.getLogger(QuercusServletImpl.class.getName());

  protected QuercusContext _quercus;
  protected ServletConfig _config;
  protected ServletContext _servletContext;
  
  /**
   * initialize the script manager.
   */
  public final void init(ServletConfig config)
    throws ServletException
  {
    _config = config;
    _servletContext = config.getServletContext();

    checkServletAPIVersion();
    
    Path pwd = new FilePath(_servletContext.getRealPath("/"));
    
    getQuercus().setPwd(pwd);

    // need to set these for non-Resin containers
    if (! Alarm.isTest() && ! getQuercus().isResin()) {
      Vfs.setPwd(pwd);
      WorkDir.setLocalWorkDir(pwd.lookup("WEB-INF/work"));
    }

    getQuercus().init();
    getQuercus().start();
  }
  
  protected void initImpl(ServletConfig config)
    throws ServletException
  {
  }

  /**
   * Sets the profiling mode
   */
  public void setProfileProbability(double probability)
  {
  }

  /**
   * Makes sure the servlet container supports Servlet API 2.4+.
   */
  protected void checkServletAPIVersion()
  {
    int major = _servletContext.getMajorVersion();
    int minor = _servletContext.getMinorVersion();

    if (major < 2 || major == 2 && minor < 4)
      throw new QuercusRuntimeException(
          L.l("Quercus requires Servlet API 2.4+."));
  }

  /**
   * Service.
   */
  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    Env env = null;
    WriteStream ws = null;
    
    try {
      Path path = getPath(request);

      QuercusPage page;

      try {
        page = getQuercus().parse(path);
      }
      catch (FileNotFoundException ex) {
        // php/2001
        log.log(Level.FINER, ex.toString(), ex);

        response.sendError(HttpServletResponse.SC_NOT_FOUND);

        return;
      }


      ws = openWrite(response);
      
      // php/6006
      ws.setNewlineString("\n");

      QuercusContext quercus = getQuercus();
      
      env = quercus.createEnv(page, ws, request, response);
      quercus.setServletContext(_servletContext);
      
      try {
        env.start();
        
        // php/2030, php/2032, php/2033
        // Jetty hides server classes from web-app
        // http://docs.codehaus.org/display/JETTY/Classloading
        //
        // env.setGlobalValue("request", env.wrapJava(request));
        // env.setGlobalValue("response", env.wrapJava(response));
        // env.setGlobalValue("servletContext", env.wrapJava(_servletContext));

        StringValue prepend
          = quercus.getIniValue("auto_prepend_file").toStringValue(env);
        if (prepend.length() > 0) {
          Path prependPath = env.lookup(prepend);
          
          if (prependPath == null)
            env.error(L.l("auto_prepend_file '{0}' not found.", prepend));
          else {
            QuercusPage prependPage = getQuercus().parse(prependPath);
            prependPage.executeTop(env);
          }
        }

        env.executeTop();

        StringValue append
          = quercus.getIniValue("auto_append_file").toStringValue(env);
        if (append.length() > 0) {
          Path appendPath = env.lookup(append);
          
          if (appendPath == null)
            env.error(L.l("auto_append_file '{0}' not found.", append));
          else {
            QuercusPage appendPage = getQuercus().parse(appendPath);
            appendPage.executeTop(env);
          }
        }
        //   return;
      }
      catch (QuercusExitException e) {
        throw e;
      }
      catch (QuercusErrorException e) {
        throw e;
      }
      catch (QuercusLineRuntimeException e) {
        log.log(Level.FINE, e.toString(), e);

        ws.println(e.getMessage());
        //  return;
      }
      catch (QuercusValueException e) {
        log.log(Level.FINE, e.toString(), e);
        
        ws.println(e.toString());

        //  return;
      }
      catch (Throwable e) {
        if (response.isCommitted())
          e.printStackTrace(ws.getPrintWriter());

        ws = null;

        throw e;
      }
      finally {
        if (env != null)
          env.close();

        // don't want a flush for an exception
        if (ws != null && env.getDuplex() == null)
          ws.close();
      }
    }
    catch (QuercusDieException e) {
      // normal exit
      log.log(Level.FINE, e.toString(), e);
    }
    catch (QuercusExitException e) {
      // normal exit
      log.log(Level.FINER, e.toString(), e);
    }
    catch (QuercusErrorException e) {
      // error exit
      log.log(Level.FINE, e.toString(), e);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  protected WriteStream openWrite(HttpServletResponse response)
    throws IOException
  {
    WriteStream ws;
    
    OutputStream out = response.getOutputStream();

    ws = Vfs.openWrite(out);

    return ws;
  }

  Path getPath(HttpServletRequest req)
  {
    String scriptPath = QuercusRequestAdapter.getPageServletPath(req);
    String pathInfo = QuercusRequestAdapter.getPagePathInfo(req);

    Path pwd = Vfs.lookup();

    Path path = pwd.lookup(req.getRealPath(scriptPath));

    if (path.isFile())
      return path;

    // XXX: include

    String fullPath;
    if (pathInfo != null)
      fullPath = scriptPath + pathInfo;
    else
      fullPath = scriptPath;

    return pwd.lookup(req.getRealPath(fullPath));
  }

  /**
   * Returns the Quercus instance.
   */
  protected QuercusContext getQuercus()
  {
    synchronized (this) {
      if (_quercus == null)
        _quercus = new QuercusContext();
    }

    return _quercus;
  }

  /**
   * Destroys the quercus instance.
   */
  public void destroy()
  {
    _quercus.close();
  }
}

