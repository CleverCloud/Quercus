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

import com.caucho.quercus.*;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusValueException;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.resin.Resin;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class ResinQuercusServlet extends QuercusServletImpl
{
  private static final L10N L = new L10N(ResinQuercusServlet.class);
  private static final Logger log
    = Logger.getLogger(ResinQuercusServlet.class.getName());

  private WebApp _webApp;

  /**
   * initialize the script manager.
   */
  @Override
  protected void initImpl(ServletConfig config)
    throws ServletException
  {
    _webApp = (WebApp) config.getServletContext();

    ResinQuercus quercus = (ResinQuercus) getQuercus();
    
    quercus.setWebApp(_webApp);
    getQuercus().setPwd(Vfs.lookup());

    quercus.setIni("caucho.server_id", Resin.getLocal().getServerId());
  }

  protected WriteStream openWrite(HttpServletResponse response)
    throws IOException
  {
    WriteStream ws;
    
    // XXX: check if correct.  PHP doesn't expect the lower levels
    // to deal with the encoding, so this may be okay
    if (response instanceof CauchoResponse) {
      ws = Vfs.openWrite(((CauchoResponse) response).getResponseStream());
    } else {
      OutputStream out = response.getOutputStream();

      ws = Vfs.openWrite(out);
    }

    return ws;
  }

  /*
  @Override
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

    return Vfs.lookup().lookup(req.getRealPath(fullPath));
  }
  */

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    synchronized (this) {
      if (_quercus == null)
        _quercus = new ResinQuercus();
    }

    return _quercus;
  }
}

