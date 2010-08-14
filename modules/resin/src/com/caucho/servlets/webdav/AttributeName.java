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

package com.caucho.servlets.webdav;

/**
 * Represents an attribute name.
 */
public class AttributeName {
  private String prefix;
  private String namespace;
  private String local;
  private String name;

  /**
   * Creates a new attribute name.
   *
   * @param uri the XML namespace
   * @param localName the XML local name
   * @param qName the name
   */
  public AttributeName(String uri, String localName, String qName)
  {
    this.local = localName;

    int p = qName.indexOf(':');
    if (p < 0)
      prefix = "";
    else
      prefix = qName.substring(0, p);

    this.name = qName;
    this.namespace = uri;
  }

  /**
   * Returns the prefix:name QName.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Returns the local part of the name.
   */
  public String getLocal()
  {
    return local;
  }

  /**
   * Returns the prefix of the name.
   */
  public String getPrefix()
  {
    return prefix;
  }

  /**
   * Returns the namespace.
   */
  public String getNamespace()
  {
    return namespace;
  }

  public int hashCode()
  {
    return local.hashCode() * 65521 + namespace.hashCode();
  }

  public boolean equals(Object obj)
  {
    if (! (obj instanceof AttributeName))
      return false;

    AttributeName name = (AttributeName) obj;

    return local.equals(name.local) && namespace.equals(name.namespace);
  }

  public String toString()
  {
    return "[Name " + name + "]";
  }
}
