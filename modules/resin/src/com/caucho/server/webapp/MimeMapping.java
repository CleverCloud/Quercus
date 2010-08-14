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

package com.caucho.server.webapp;

import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;

/**
 * Configuration for a mime-mapping.
 */
public class MimeMapping {
  static L10N L = new L10N(MimeMapping.class);

  // The mime-mapping extension
  private String _extension;
  
  // The mimeType
  private String _mimeType;

  /**
   * Creates the mime mapping.
   */
  public MimeMapping()
  {
  }

  /**
   * Sets the extension
   */
  public void setExtension(String extension)
    throws ServletException
  {
    if (! extension.startsWith("."))
      extension = "." + extension;
    
    _extension = extension;
  }

  /**
   * Gets the extension.
   */
  public String getExtension()
  {
    return _extension;
  }

  /**
   * Sets the mime type
   */
  public void setMimeType(String mimeType)
  {
    _mimeType = mimeType;
  }

  /**
   * Gets the mime type
   */
  public String getMimeType()
  {
    return _mimeType;
  }

  /**
   * Init
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_extension == null)
      throw new ServletException(L.l("mime-mapping needs 'extension' attribute."));
    if (_mimeType == null)
      throw new ServletException(L.l("mime-mapping needs 'mime-type' attribute."));
  }
}
