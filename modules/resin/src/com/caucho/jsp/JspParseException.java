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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp;

import com.caucho.java.LineMap;
import com.caucho.java.LineMapException;
import com.caucho.util.CompileException;
import com.caucho.util.LineCompileException;

import javax.servlet.ServletException;

public class JspParseException extends ServletException
  implements CompileException, LineMapException
{
  private String _errorPage;
  private LineMap _lineMap;

  public JspParseException()
  {
  }

  public JspParseException(String msg)
  {
    super(msg);
  }

  public JspParseException(Throwable e)
  {
    super(e.getMessage(), e);
  }

  public JspParseException(String msg, Throwable e)
  {
    super(msg, e);
  }

  public static JspParseException create(Throwable e)
  {
    if (e instanceof JspParseException)
      return (JspParseException) e;
    else if (e instanceof LineCompileException)
      return new JspLineParseException(e);
    else
      return new JspParseException(e);
  }

  public static JspParseException create(Throwable e, LineMap lineMap)
  {
    if (e instanceof JspParseException) {
      JspParseException jspExn = (JspParseException) e;
      jspExn.setLineMap(lineMap);
      
      return jspExn;
    }
    else {
      JspParseException jspExn = new JspParseException(e);
      jspExn.setLineMap(lineMap);
      
      return jspExn;
    }
  }

  public void setErrorPage(String errorPage)
  {
    _errorPage = errorPage;
  }

  public String getErrorPage()
  {
    return _errorPage;
  }

  public void setLineMap(LineMap lineMap)
  {
    _lineMap = lineMap;
  }

  public LineMap getLineMap()
  {
    return _lineMap;
  }
}
