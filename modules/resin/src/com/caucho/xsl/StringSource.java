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

import javax.xml.transform.Source;
import java.io.File;

/**
 * Source for an input string.
 */
public class StringSource implements Source {
  /**
   * The feature name to tell if the transformer can handle stream input.
   */
  public static final String FEATURE = "stringsource";

  /**
   * Underlying string.
   */
  private String string;

  /**
   * System identifier (URL).
   */
  private String systemId;

  /**
   * Public identifier.
   */
  private String publicId;

  /**
   * Zero-arg constructor.
   */
  public StringSource()
  {
  }

  /**
   * Create a StringSource with a given string.
   *
   * @param source the source string.
   */
  public StringSource(String source)
  {
    this.string = source;
  }

  /**
   * Create a StringSource with a given string and systemId.
   *
   * @param source the source string.
   * @param systemId the URL representing the string location.
   */
  public StringSource(String source, String systemId)
  {
    this.string = source;
    this.systemId = systemId;
  }

  /**
   * Returns the source string.
   */
  public String getString()
  {
    return string;
  }

  /**
   * Sets the source string stream.
   */
  public void setString(String is)
  {
    this.string = is;
  }

  /**
   * Returns the system identifier (URL).
   */
  public String getSystemId()
  {
    return systemId;
  }

  /**
   * Sets the system identifier (URL).
   */
  public void setSystemId(String systemId)
  {
    this.systemId = systemId;
  }

  /**
   * Sets the system identifier (URL) from a File.
   */
  public void setSystemId(File file)
  {
    this.systemId = file.toString();
  }

  /**
   * Returns the public identifier (URL).
   */
  public String getPublicId()
  {
    return publicId;
  }

  /**
   * Sets the public identifier (URL).
   */
  public void setPublicId(String publicId)
  {
    this.publicId = publicId;
  }
}
