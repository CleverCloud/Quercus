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

package com.caucho.servlets.ssi;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * Represents a SSI include statement
 */
public class IncludeStatement extends Statement{
  private final SSIExpr _virtual;

  private IncludeStatement(SSIExpr virtual)
  {
    _virtual = virtual;
  }

  static Statement create(HashMap<String,String> attr, Path path)
  {
    String virtual = attr.get("virtual");

    if (virtual == null)
      return new ErrorStatement("['virtual' is a required attribute of #include]");

    return new IncludeStatement(ExprParser.parseConcat(virtual, path));
  }
  
  /**
   * Executes the SSI statement.
   *
   * @param out the output stream
   * @param request the servlet request
   * @param response the servlet response
   */
  public void apply(WriteStream out,
                    HttpServletRequest request,
                    HttpServletResponse response)
    throws IOException, ServletException
  {
    out.flushBuffer();
    
    RequestDispatcher disp;

    String path = _virtual.evalString(request, response);
    
    disp = request.getRequestDispatcher(path);

    disp.include(request, response);
  }
}
