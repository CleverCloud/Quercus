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

package com.caucho.xsl;

import java.util.ArrayList;

/**
 * Encapsulates the xsl:output attributes.
 *
 * @since Resin 1.2
 */
public class OutputFormat {
  private String method = null;
  private String encoding = null;
  private String mediaType = null;
  private String indent = null;
  private String version = null;
  private String omitDeclaration = null;
  private String standalone = null;
  private String systemId = null;
  private String publicId = null;
  private ArrayList cdataSectionElements;

  /**
   * Returns the output method: xml, html, or text.
   */
  public String getMethod()
  {
    return method;
  }

  /**
   * Sets the output method: xml, html, or text.
   */
  public void setMethod(String method)
  {
    this.method = method;
  }

  /**
   * Returns the output version, e.g. 1.0 for XML or 3.2 for HTML.
   */
  public String getVersion()
  {
    return version;
  }

  /**
   * Sets the output version, e.g. 1.0 for XML or 3.2 for HTML.
   */
  public void setVersion(String version)
  {
    this.version = version;
  }

  /**
   * Returns "true" if the output declaration should be omitted.
   * A null value means that the XML printer may use its own heuristics
   * to decide.
   */
  public String getOmitDeclaration()
  {
    return omitDeclaration;
  }

  /**
   * Set to "true" if the output declaration should be omitted.
   */
  public void setOmitDeclaration(String omitDeclaration)
  {
    this.omitDeclaration = omitDeclaration;
  }

  public void setStandalone(String standalone)
  {
    this.standalone = standalone;
  }

  public String getStandalone()
  {
    return standalone;
  }

  public void setSystemId(String systemId)
  {
    this.systemId = systemId;
  }

  public String getSystemId()
  {
    return systemId;
  }

  public void setPublicId(String publicId)
  {
    this.publicId = publicId;
  }

  public String getPublicId()
  {
    return publicId;
  }

  public ArrayList getCdataSectionElements()
  {
    return cdataSectionElements;
  }

  public void setCdataSectionElements(ArrayList list)
  {
    cdataSectionElements = list;
  }

  public String getEncoding()
  {
    return encoding;
  }

  public void setEncoding(String encoding)
  {
    this.encoding = encoding;
  }

  public String getMediaType()
  {
    return mediaType;
  }

  public void setMediaType(String mediaType)
  {
    this.mediaType = mediaType;
  }

  public String getIndent()
  {
    return indent;
  }

  public void setIndent(String indent)
  {
    this.indent = indent;
  }
}
    
