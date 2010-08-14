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

package com.caucho.jsp.cfg;

import javax.servlet.descriptor.TaglibDescriptor;

/**
 * Configuration for the taglib in the web.xml
 */
public class JspTaglib implements TaglibDescriptor {
  private String _id;
  private String _taglibUri;
  private String _taglibLocation;

  /**
   * Sets the taglib id.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Gets the taglib id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Sets the taglib uri.
   */
  public void setTaglibUri(String taglibUri)
  {
    _taglibUri = taglibUri;
  }

  /**
   * Gets the taglib uri.
   */
  public String getTaglibUri()
  {
    return _taglibUri;
  }

  @Override
  public String getTaglibURI()
  {
    return _taglibUri;
  }

  /**
   * Sets the taglib location.
   */
  public void setTaglibLocation(String taglibLocation)
  {
    _taglibLocation = taglibLocation;
  }

  /**
   * Gets the taglib location.
   */
  public String getTaglibLocation()
  {
    return _taglibLocation;
  }

  @Override
  public String toString()
  {
    return getClass().getName()
      + '['
      + _taglibUri
      + "->"
      + _taglibLocation
      + ']';
  }
}
