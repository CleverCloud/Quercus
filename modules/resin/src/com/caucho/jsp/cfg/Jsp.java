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

package com.caucho.jsp.cfg;

/**
 * Configuration from the web.xml.
 *
 * <pre>
 * element jsp { session }
 * </pre>
 */
public class Jsp {
  private boolean _isSession = true;
  private boolean _recycleTags = true;
  private boolean _precompile = true;

  /**
   * Set true if sessions are allowed by default.
   */
  public void setSession(boolean isSession)
  {
    _isSession = isSession;
  }

  /**
   * Set true if sessions are allowed by default.
   */
  public boolean getSession()
  {
    return _isSession;
  }

  /**
   * Set true if tags are recycled
   */
  public void setRecycleTags(boolean recycleTags)
  {
    _recycleTags = recycleTags;
  }

  /**
   * Return true if tags are recycled
   */
  public boolean getRecycleTags()
  {
    return _recycleTags;
  }

  /**
   * Set true if precompiled JSP files are allowed.
   */
  public void setPrecompile(boolean precompile)
  {
    _precompile = precompile;
  }

  /**
   * Return true if precompiled JSP files are allowed.
   */
  public boolean getPrecompile()
  {
    return _precompile;
  }
}
