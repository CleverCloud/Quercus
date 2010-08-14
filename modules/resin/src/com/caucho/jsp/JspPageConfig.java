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
 * @author Scott Ferguson
 */

package com.caucho.jsp;

/**
 * Configuration for a JSP page.  Includes directives.
 */
public class JspPageConfig {
  /*
   * Variables storing the JSP directives.
   */
  private boolean _isThreadSafe = true;
  private boolean _hasTrueSession = false;
  private boolean _hasFalseSession = false;
  private boolean _hasSession = true;
  
  private boolean _useEndTagHack = true;
  private boolean _ideHack = false;

  private int _bufferSize = 8 * 1024;
  private boolean _autoFlush = true;
  private boolean _isErrorPage = false;
  private String _errorPage = null;
  private String _servletInfo = null;
  private String _contentType = null;
  private String _charEncoding = null;
  private String _language = null;
  private String _session = null;
  private String _buffer = null;

  private boolean _staticEncoding;

  private boolean _isXml;

  // XXX: needed in combination with XTP
  private boolean _alwaysModified;

  private boolean _isELEnabled;
  private boolean _fastJstl = true;

  /**
   * Returns true if the JSP page is thread safe.
   */
  public boolean isThreadSafe()
  {
    return _isThreadSafe;
  }

  /**
   * Set true if the JSP page is thread safe.
   */
  public void setThreadSafe(boolean isThreadSafe)
  {
    _isThreadSafe = isThreadSafe;
  }

  /**
   * Returns true if static text encoding is allowed.
   */
  public boolean isStaticEncoding()
  {
    return _staticEncoding;
  }

  /**
   * Set true if static text encoding is allowed.
   */
  public void setStaticEncoding(boolean allowStaticEncoding)
  {
    _staticEncoding = allowStaticEncoding;
  }
}
