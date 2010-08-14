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

package javax.servlet.jsp.tagext;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Tag instance attributes and values.  This information is used by
 * TagExtraInfo to prepare VariableInfo.
 */
public class TagData implements Cloneable {
  /**
   * Constant object used as a key for request time attributes.
   */
  public static final Object REQUEST_TIME_VALUE = new RequestTimeValue();

  private Hashtable<String, Object> attributes;

  /**
   * Create a new TagData object based on an attribute key/value hash table.
   */
  public TagData(Hashtable<String, Object> attrs)
  {
    if (attrs == null)
      throw new NullPointerException();

    this.attributes = attrs;
  }

  /**
   * Create a new TagData object based on and array of attribute key/values.
   */
  public TagData(Object [][]attrs)
  {
    attributes = new Hashtable<String, Object>();

    for (int i = 0; attrs != null && i < attrs.length; i++) {
      attributes.put((String)attrs[i][0], attrs[i][1]);
    }
  }

  /**
   * Returns the attribute with the given name.
   */
  public Object getAttribute(String attribute)
  {
    return this.attributes.get(attribute);
  }

  /**
   * Sets the attribute with the given name.
   */
  public void setAttribute(String attribute, Object value)
  {
    this.attributes.put(attribute, value);
  }

  /**
   * Enumerates the attribute names.
   */
  public Enumeration<String> getAttributes()
  {
    return this.attributes.keys();
  }

  /**
   * Return the attribute as a string.
   */
  public String getAttributeString(String name)
  {
    Object value = this.attributes.get(name);

    return (String) value;
  }

  /**
   * Return the id of the attribute as a string.
   */
  public String getId()
  {
    return getAttributeString(TagAttributeInfo.ID);
  }

  /**
   * Clone the tag data.
   */
  protected Object clone()
    throws CloneNotSupportedException
  {
    return new TagData((Hashtable<String, Object>) attributes.clone());
  }

  static class RequestTimeValue {
    public String toString()
    {
      return "RequestTimeValue[]";
    }
  }
}
