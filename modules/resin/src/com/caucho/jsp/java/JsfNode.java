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

import java.lang.reflect.*;

import com.caucho.jsp.JspParseException;
import com.caucho.xml.QName;
import com.caucho.util.*;
import com.caucho.config.*;

abstract public class JsfNode extends JspContainerNode {
  protected String _var;
  protected String _bodyVar;

  public JsfNode()
  {
    Thread.dumpStack();
  }

  /**
   * Returns the variable containing the jsf parent
   */
  @Override
  public String getJsfVar()
  {
    return _var;
  }

  /**
   * Returns the variable containing the jsf body
   */
  @Override
  public String getJsfBodyVar()
  {
    return _bodyVar;
  }

  /**
   * Adds an attribute.
   */
  @Override
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    throw error(L.l("attribute '{0}' is not allowed in <{1}>.",
                    name.getName(), getTagName()));
  }

  /**
   * Adds a JspAttribute attribute.
   *
   * @param name the name of the attribute.
   * @param value the value of the attribute.
   */
  @Override
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    throw error(L.l("attribute '{0}' is not allowed in <{1}>.",
                    name.getName(), getTagName()));
  }

  
  /**
   * generates prologue data.
   */
  @Override
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    super.generatePrologue(out);
    
    if (! _gen.isJsfPrologueInit()) {
      _gen.setJsfPrologueInit(true);

      out.println("final javax.faces.context.FacesContext _jsp_faces_context");
      out.println("  = javax.faces.context.FacesContext.getCurrentInstance();");
    }
  }

  /**
   * Generates the code for the children.
   *
   * @param out the output writer for the generated java.
   */
  @Override
  public void generateChildren(JspJavaWriter out)
    throws Exception
  {
    if (_children == null)
      return;

    String prevId = null;
    boolean isFirst = (this instanceof JsfTagNode);
    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      if (isFirst
          && child instanceof StaticText
          && (i + 1 == _children.size()
              || _children.get(i + 1) instanceof JsfNode)) {
        StaticText text = (StaticText) child;

        if (isWhitespaceOrComment(text.getText())) {
        }
        else if (i + 1 == _children.size()) {
          out.print("com.caucho.jsp.jsf.JsfTagUtil.addVerbatim("
                    + _var
                    + ", \"");
          out.printJavaString(text.getText());
          out.println("\");");
        }
        else {
          out.print("com.caucho.jsp.jsf.JsfTagUtil.addVerbatim("
                    + _var
                    + ", " + prevId
                    + ", \"");
          out.printJavaString(text.getText());
          out.println("\");");
        }

        continue;
      }

      isFirst = false;

      child.generateStartLocation(out);
      try {
        child.generate(out);
      } catch (Exception e) {
        if (e instanceof LineCompileException)
          throw e;
        else
          throw child.error(e);
      }
      child.generateEndLocation(out);
      
      if (child instanceof JsfTagNode) {
        JsfTagNode jsfNode = (JsfTagNode) child;

        if (jsfNode.getJsfId() != null)
          prevId = "\"" + jsfNode.getJsfId() + "\"";

        isFirst = true;
      }
    }

    if (_bodyVar != null && ! isFirst)
      out.println("com.caucho.jsp.jsf.JsfTagUtil.addVerbatim("
                  + _var + ", " + _bodyVar + ");");
  }

  private boolean isWhitespaceOrComment(String text)
  {
    text = text.trim();

    return (text.equals("")
            || text.startsWith("<!--") && text.endsWith("-->"));
  }

  /**
   * Generates the code for the children.
   *
   * @param out the output writer for the generated java.
   */
  protected boolean hasBodyContent()
    throws Exception
  {
    if (_children == null)
      return false;

    String bodyVar = null;
    String prevId = null;
    boolean isFirst = true;
    for (int i = 0; i < _children.size(); i++) {
      JspNode child = _children.get(i);

      if (isFirst
          && child instanceof StaticText
          && (i + 1 == _children.size()
              || _children.get(i + 1) instanceof JsfTagNode)) {
        continue;
      }

      if (! (child instanceof JsfTagNode)) {
        if (isFirst) {
          return true;
        }

        // push body
      }
    }

    return false;
  }

  protected Method findSetter(Class cl, String name)
  {
    Method []methods = cl.getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (! method.getName().equals(name))
        continue;
      
      if (! Modifier.isPublic(method.getModifiers())
          || Modifier.isStatic(method.getModifiers()))
        continue;

      if (method.getParameterTypes().length != 1)
        continue;

      return method;
    }

    return null;
  }

  static class Attr {
    private String _name;
    private Method _method;
    
    private String _value;
    private JspAttribute _attr;

    Attr(String name, Method method, String value)
    {
      _name = name;
      _method = method;
      _value = value;
    }

    Attr(String name, Method method, JspAttribute attr)
    {
      _name = name;
      _method = method;
      _attr = attr;
    }

    String getName()
    {
      return _name;
    }

    Method getMethod()
    {
      return _method;
    }

    String getValue()
    {
      return _value;
    }

    JspAttribute getAttr()
    {
      return _attr;
    }
  }
}
