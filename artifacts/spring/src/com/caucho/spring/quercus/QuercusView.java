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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */


package com.caucho.spring.quercus;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.quercus.*;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.*;
import com.caucho.quercus.page.*;

import com.caucho.util.L10N;
import com.caucho.vfs.*;

import org.springframework.web.servlet.*;
import org.springframework.web.servlet.view.*;

public class QuercusView
  extends AbstractUrlBasedView
{
  private static final L10N L = new L10N(QuercusView.class);

  private static final Logger log
    = Logger.getLogger(QuercusView.class.getName());

  protected QuercusContext _quercus;
  protected ServletContext _servletContext;

  public QuercusView()
  {
    super();
  }

        protected void initServletContext(ServletContext servletContext)
  {
    _servletContext = servletContext;

    checkServletAPIVersion();

    getQuercus().setPwd(new FilePath(_servletContext.getRealPath("/")));

    getQuercus().init();
  }

  protected void checkServletAPIVersion()
  {
    int major = _servletContext.getMajorVersion();
    int minor = _servletContext.getMinorVersion();

    if (major < 2 || major == 2 && minor < 4)
      throw new QuercusRuntimeException(L.l("Quercus requires Servlet API 2.4+."));
  }

  protected void renderMergedOutputModel(Map model,
                                         HttpServletRequest request,
                                         HttpServletResponse response)
    throws Exception
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

      StreamImpl out;

      try {
        out = new VfsStream(null, response.getOutputStream());
      }
      catch (IllegalStateException e) {
        WriterStreamImpl writer = new WriterStreamImpl();
        writer.setWriter(response.getWriter());

        out = writer;
      }

      ws = new WriteStream(out);

      ws.setNewlineString("\n");

      QuercusContext quercus = getQuercus();
      quercus.setServletContext(_servletContext);

      env = quercus.createEnv(page, ws, request, response);

      // retro... thanks, Spring
      for (Object entryObj : model.entrySet()) {
        Map.Entry entry = (Map.Entry) entryObj;
        env.setScriptGlobal((String) entry.getKey(), entry.getValue());
      }

      try {
        env.start();

        env.setScriptGlobal("request", request);
        env.setScriptGlobal("response", response);
        env.setScriptGlobal("servletContext", _servletContext);

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

        // don't want a flush for a thrown exception
        if (ws != null)
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

  Path getPath(HttpServletRequest req)
  {
    String scriptPath = getUrl();
    String pathInfo = QuercusRequestAdapter.getPagePathInfo(req);

    Path pwd = new FilePath(System.getProperty("user.dir"));

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
   * Gets the script manager.
   */
  public void destroy()
  {
    _quercus.close();
  }

}

