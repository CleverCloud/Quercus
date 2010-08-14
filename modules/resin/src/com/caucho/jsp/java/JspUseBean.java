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
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Represents a Java scriptlet.
 */
public class JspUseBean extends JspContainerNode {
  private static final QName ID = new QName("id");
  private static final QName TYPE = new QName("type");
  private static final QName CLASS = new QName("class");
  private static final QName BEAN_NAME = new QName("beanName");
  private static final QName SCOPE = new QName("scope");
  
  private String _id;
  private String _typeName;
  private String _className;
  private String _beanName;
  private String _scope;

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (ID.equals(name))
      _id = value;
    else if (TYPE.equals(name))
      _typeName = value;
    else if (CLASS.equals(name))
      _className = value;
    else if (BEAN_NAME.equals(name))
      _beanName = value;
    else if (SCOPE.equals(name))
      _scope = value;
    else
      throw error(L.l("'{0}' is an invalid attribute in <jsp:useBean>",
                      name.getName()));
  }

  /**
   * Adds text to the scriptlet.
   */
  public JspNode addText(String text)
    throws JspParseException
  {
    JspNode node = new StaticText(_gen, text, this);
    
    addChild(node);

    return node;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:userBean");

    if (_id != null)
      printXmlAttribute(os, "id", _id);
    
    if (_typeName != null)
      printXmlAttribute(os, "type", _typeName);

    if (_className != null)
      printXmlAttribute(os, "class", _className);

    if (_beanName != null)
      printXmlAttribute(os, "beanName", _beanName);

    if (_scope != null)
      printXmlAttribute(os, "scope", _scope);

    os.print("/>");
  }

  /**
   * Generates the code for the scriptlet
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    if (_id == null)
      throw error(L.l("<jsp:useBean> expects an 'id' attribute.  id specifies the variable name for the bean."));

    if (_typeName == null)
      _typeName = _className;
    
    if (_typeName == null)
      throw error(L.l("<jsp:useBean> expects a 'type' or 'class' attribute.  The 'type' specifies the Java type of the bean."));

    // Save the bean's type
    _gen.addBeanClass(_id, _typeName);

    String context = null;
    if (_scope == null || _scope.equals("page"))
      context = "pageContext";
    else if (_scope.equals("request")) {
      context = "pageContext.getRequest()";
    }
    else if (_scope.equals("session")) {
      context = "pageContext.getSession()";
    }
    else if (_scope.equals("application")) {
      context = "pageContext.getApplication()";
    }
    else
      throw error(L.l("Unknown scope '{0}' in <jsp:useBean>.  Scope must be 'page', 'request', 'session', or 'application'.", _scope));

    // declare the bean
    out.println(_typeName + " " + _id + ";");

    // application and session beans need synchronization
    if ("application".equals(_scope) || "session".equals(_scope)) {
      out.println("synchronized (" + context + ") {");
      out.pushDepth();
    }

    // try to get the bean from the context
    out.print(_id + " = (" + _typeName + ") " + context);
    out.println(".getAttribute(\"" + _id + "\");");

    // If the bean is new, then instantiate it
    out.println("if (" + _id + " == null) {");
    out.pushDepth();

    boolean canInstantiate = false;

    // instantiate a class
    if (_className != null) {
      String msg = canInstantiateBean(_className);
      if (msg == null) {
        out.println(_id + " = new " + _className + "();");
        canInstantiate = true;
      }
      else
        out.println("throw new java.lang.InstantiationException(\"" + msg + "\");");
    }
    else if (_beanName == null)
      out.println("throw new java.lang.InstantiationException(\"jsp:useBean needs 'bean' or 'class'\");");
    // instantiate beans with a request time attribute
    else if (hasRuntimeAttribute(_beanName)) {
      String beanName = getRuntimeAttribute(_beanName);
      out.println(_id + " = (" + _typeName +
              ") java.beans.Beans.instantiate(getClass().getClassLoader(), " +
              beanName + ");");
      canInstantiate = true;
    }
    // instantiate a beans
    else {
      out.println(_id + " = (" + _typeName +
              ") java.beans.Beans.instantiate(getClass().getClassLoader(), \"" +
              _beanName + "\");");
      canInstantiate = true;
    }

    // Save it in the context
    if (! canInstantiate) {
    }
    else
      out.println(context + ".setAttribute(\"" + _id + "\", " + _id + ");");

    // Initialize the new bean
    if (canInstantiate)
      generateChildren(out);

    out.popDepth();
    out.println("}");

    // Close the synchronization if necessary
    if ("application".equals(_scope) || "session".equals(_scope)) {
      out.popDepth();
      out.println("}");
    }
  }

  /**
   * Tests if the bean can be instantiated.
   */
  private String canInstantiateBean(String className)
    throws Exception
  {
    try {
      Class cl = _gen.getBeanClass(className);
      int modifiers = cl.getModifiers();
      if (Modifier.isInterface(modifiers))
        return L.l("'{0}' is an interface", className);
      if (Modifier.isAbstract(modifiers))
        return L.l("'{0}' is abstract", className);
      if (! Modifier.isPublic(modifiers))
        return L.l("'{0}' must be public", className);

      Constructor []constructors = cl.getConstructors();
      for (int i = 0; i < constructors.length; i++) {

        Class []param = constructors[i].getParameterTypes();
        if (param.length == 0)
          return null;
      }
    } catch (Exception e) {
      throw error(e.getMessage());
    }

    return L.l("'{0}' has no public zero-arg constructor", className);
  }
}
