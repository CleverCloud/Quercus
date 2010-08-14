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

package com.caucho.jsp.java;

import com.caucho.jsp.JspParseException;
import com.caucho.util.BeanUtil;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Represents a Java scriptlet.
 */
public class JspGetProperty extends JspContainerNode {
  private static final QName NAME = new QName("name");
  private static final QName PROPERTY = new QName("property");
  
  private String _name;
  private String _property;
  
  private String _text;

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (NAME.equals(name))
      _name = value;
    else if (PROPERTY.equals(name))
      _property = value;
    else
      throw error(L.l("`{0}' is an invalid attribute in <jsp:getProperty>",
                      name.getName()));
  }

  /**
   * Adds text to the scriptlet.
   */
  public JspNode addText(String text)
  {
    _text = text;

    return null;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:getProperty name=\"" + _name + "\"");
    os.print(" property=\"" + _property + "\"/>");
  }

  /**
   * Generates the code for the scriptlet
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_name == null)
      throw error(L.l("<jsp:getProperty> expects attribute `name'.  name specifies the variable name of the bean."));

    if (_property == null)
      throw error(L.l("<jsp:getProperty> expects attribute `property'.  property specifies the bean's get method."));

    Class cl = _gen.getClass(_name);
    if (cl == null)
      throw error(L.l("`{0}' is not a declared bean.  Beans used in <jsp:getProperty> must be declared in a <jsp:useBean>.", _name));

    Class beanClass = cl;
    Method reader = BeanUtil.getGetMethod(cl, _property);
    if (reader == null)
      throw error(L.l("`{0}' has no get method for `{1}'",
                      _name, _property));

    out.print("  out.print(((");
    out.printClass(beanClass);
    out.print(") ");
    out.print("pageContext.findAttribute(\"" + _name + "\"))");
    out.println("." + reader.getName() + "());");
  }
}
