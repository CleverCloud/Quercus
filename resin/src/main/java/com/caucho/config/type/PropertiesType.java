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

package com.caucho.config.type;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.QName;

import java.util.*;
import java.util.regex.*;

/**
 * Represents a Properties type.
 */
public final class PropertiesType extends ConfigType
{
  private static final L10N L = new L10N(LocaleType.class);
  
  public static final PropertiesType TYPE = new PropertiesType();

  /**
   * The PropertiesType is a singleton
   */
  private PropertiesType()
  {
  }
  
  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return Properties.class;
  }
  
  /**
   * Creates a new instance of the type.
   */
  public Object create(Object parent, QName name)
  {
    throw new ConfigException(L.l("java.util.Properties syntax is a string in .properties file syntax like the following:\n  a=b\n  b=c"));
  }

  
  /**
   * Converts the string to a value of the type.
   */
  public Object valueOf(String text)
  {
    if (text == null)
      return null;

    try {
      Properties props = new Properties();

      ReadStream is = Vfs.openString(text);

      props.load(is);

      return props;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value instanceof Properties)
      return value;
    else if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else
      throw new ConfigException(L.l("'{0}' is not a valid Properties value.",
                                    value));
  }
}
