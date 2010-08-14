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

package com.caucho.jsp.java;

import com.caucho.jsp.JspParseException;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Java scriptlet.
 */
public class JspParam extends JspNode {
  private static final Logger log
    = Logger.getLogger(JspParam.class.getName());
  private static final QName NAME = new QName("name");
  private static final QName VALUE = new QName("value");
  
  private String _name;
  private String _value;

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (NAME.equals(name)) {
      _name = value;

      if (hasRuntimeAttribute(value) || hasELAttribute(value))
        throw error(L.l("'name' attribute may not have a runtime value at {0}",
                        value));
    }
    else if (VALUE.equals(name))
      _value = value;
    else
      throw error(L.l("'{0}' is an invalid attribute in <jsp:param>",
                      name.getName()));
  }

  /**
   * Returns the param name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the param value.
   */
  public String getValue()
  {
    return _value;
  }
  
  /**
   * Called when the tag closes.
   */
  public void endElement()
    throws Exception
  {
    if (_name == null)
      throw error(L.l("jsp:param requires a 'name' attribute"));
    
    if (_value == null)
      throw error(L.l("jsp:param requires a 'value' attribute"));
  }

  /**
   * Returns true if the param has scripting elements.
   */
  public boolean hasScripting()
  {
    try {
      return hasRuntimeAttribute(getName()) || hasRuntimeAttribute(getValue());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      return true;
    }
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    throw new IOException(L.l("<jsp:param> does not have a direct xml."));
  }

  /**
   * Generates the code for the scriptlet
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    throw error(L.l("<jsp:param> does not generate code directly."));
  }

  /**
   * Generates the code for the scriptlet
   *
   * @param out the output writer for the generated java.
   */
  public void generateEmpty()
    throws Exception
  {
    throw error(L.l("<jsp:param> does not generate code directly."));
  }
}
