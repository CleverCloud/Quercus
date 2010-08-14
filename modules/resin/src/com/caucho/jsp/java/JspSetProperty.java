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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Java scriptlet.
 */
public class JspSetProperty extends JspContainerNode {
  private static final Logger log
    = Logger.getLogger(JspSetProperty.class.getName());
  private static final QName NAME = new QName("name");
  private static final QName PROPERTY = new QName("property");
  private static final QName PARAM = new QName("param");
  private static final QName VALUE = new QName("value");
  
  private String _name;
  private String _property;
  private String _param;
  
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
    else if (PARAM.equals(name))
      _param = value;
    else if (VALUE.equals(name))
      super.addAttribute(name, value);
    else
      throw error(L.l("`{0}' is an invalid attribute in <jsp:setProperty>",
                      name.getName()));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:setProperty name=\"" + _name + "\"");
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
      throw error(L.l("<jsp:setProperty> expects a `name' attribute."));

    if (_property == null)
      throw error(L.l("<jsp:setProperty> expects a `property' attribute."));

    Object value = getAttribute("value");

    if (value == null) {
      generateSetParamProperty(out, _name, _property, _param);
      return;
    }

    Class cl = _gen.getClass(_name);
  
    if (cl == null)
      throw error(L.l("`{0}' is an unknown bean in <jsp:setProperty>.  All beans must be declared in a <jsp:useBean>.", _name));

    PropertyDescriptor []pds = Introspector.getBeanInfo(cl).getPropertyDescriptors();
    for (int i = 0; i < pds.length; i++) {
      if (pds[i].getName().equals(_property) &&
          pds[i].getWriteMethod() != null &&
          pds[i].getPropertyEditorClass() != null) {
        generateSetParameter(out, _name, (String) value,
                             pds[i].getWriteMethod(),
                             pds[i].getPropertyEditorClass());
        return;
      }
    }

    Method setMethod = BeanUtil.getSetMethod(cl, _property);
    if (setMethod == null)
      throw error(L.l("bean `{0}' has no set property `{1}'",
                      _name, _property));
    
    generateSetParameter(out, _name, value, setMethod, true,
                         "pageContext", false, false, null);
  }

  private void generateSetParamProperty(JspJavaWriter out,
                                        String name, String property,
                                        String param)
    throws Exception
  {
    boolean foundProp = property.equals("*");
    Class cl = _gen.getClass(name);
    if (cl == null)
      throw error(L.l("{0} unknown variable `{1}'",
                      "jsp:setProperty", name));

    out.println("{");
    out.pushDepth();
    out.println("java.lang.String _jspParam;");
    try {
      Class beanClass = cl;
      BeanInfo info = Introspector.getBeanInfo(beanClass);
      Method []methods = beanClass.getMethods();
      boolean hasParams = false;
      for (int i = 0; i < methods.length; i++) {
        Method setMethod = methods[i];

        String methodName = setMethod.getName();

        if (! methodName.startsWith("set"))
          continue;
        
        String propName = methodNameToPropertyName(info, methodName);

        if (propName == null)
          continue;

        if (! property.equals("*") && ! propName.equals(property))
          continue;

        Class []params = setMethod.getParameterTypes();
        if (params.length != 1)
          continue;

        if (hasBetterMethod(methods, setMethod))
          continue;

        Class paramType = params[0];
        String type = paramType.getName();
        String tail = null;
        boolean isArray = false;

        String p = param;
        if (p == null)
          p = propName;

        if (! paramType.isArray())
          tail = stringToValue(paramType, "_jspParam");

        PropertyEditor editor;

        if (tail != null) {
        }
        else if (paramType.isArray()) {
          Class compType = paramType.getComponentType();
          if (! hasParams)
            out.println("java.lang.String []_jspParams;");
          hasParams = true;
          out.println("_jspParams = request.getParameterValues(\"" + p + "\");");
          isArray = true;
          
          if (String.class.equals(compType) || Object.class.equals(compType)) {
            foundProp = true;
            out.println("if (_jspParams != null)");
            out.println("  " + name + "." + methodName + "(_jspParams);");
          }
          else if ((tail = stringToValue(compType, "_jspParams[_jsp_i]")) != null) {
            foundProp = true;
            out.println("if (_jspParams != null) {");
            out.println("  " + compType.getName() + " []_jsp_values = " +
                    " new " + compType.getName() + "[_jspParams.length];");
            out.println("  for (int _jsp_i = _jspParams.length - 1; _jsp_i >= 0; _jsp_i--)");
            out.println("    _jsp_values[_jsp_i] = " + tail + ";");
            out.println("  " + name + "." + methodName + "(_jsp_values);");
            out.println("}");
          }
          else if ((editor = PropertyEditorManager.findEditor(paramType)) != null) {
            foundProp = true;
            out.println("if (_jspParams != null) {");
            out.println("  " + compType.getName() + " []_jsp_values = " +
                    " new " + compType.getName() + "[_jspParams.length];");
            out.println("   java.beans.PropertyEditor _editor = "+
                    "  java.beans.PropertyEditorManager.findEditor(" +
                    compType.getName() + ".class);");
            out.println("  for (int _jsp_i = _jspParams.length - 1; _jsp_i >= 0; _jsp_i--) {");
            out.println("    _editor.setAsText(_jspParams[_jsp_i]);");
            
            out.println("    _jsp_values[_jsp_i] = (" + compType.getName() + ") _editor.getValue();");
            
            out.println("  " + name + "." + methodName + "(_jsp_values);");
            out.println("}");
          }
        }

        if (isArray) {
        }
        else if (tail != null) {
          out.println("_jspParam = request.getParameter(\"" + p + "\");");
          out.println("if (_jspParam != null && ! _jspParam.equals(\"\"))");
          out.println("  " + name + "." + methodName + "(" + tail + ");");
          foundProp = true;
        }
        else if ((editor = PropertyEditorManager.findEditor(paramType)) != null) {
          out.println("_jspParam = request.getParameter(\"" + p + "\");");
          out.println("if (_jspParam != null && ! _jspParam.equals(\"\")) {");
          out.println("   java.beans.PropertyEditor _editor = "+
                  "  java.beans.PropertyEditorManager.findEditor(" +
                  paramType.getName() + ".class);");
          out.println("  _editor.setAsText(_jspParam);");
          out.println("  " + name + "." + methodName + "((" + paramType.getName() + ") _editor.getValue());");
          out.println("}");
          foundProp = true;
        }
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      throw error(L.l("{0} can't find class `{1}'",
                      "jsp:setProperty", name));
    }
    
    if (! foundProp)
      throw error(L.l("bean `{0}' has no property named `{1}'",
                      name, property));
      
    out.popDepth();
    out.println("}");
  }

  /**
   * Returns true if there's a better method to set.
   */
  private boolean hasBetterMethod(Method []methods, Method setMethod)
  {
    Class []setParam = setMethod.getParameterTypes();

    if (setParam[0].equals(String.class))
      return false;
    
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (method == setMethod)
        continue;

      if (! method.getName().equals(setMethod.getName()))
        continue;

      Class []param = method.getParameterTypes();
      
      if (param.length != 1)
        continue;

      if (param[0].equals(String.class))
        return true;
    }

    return false;
  }

  private String methodNameToPropertyName(BeanInfo info, String methodName)
  {
    PropertyDescriptor []pds = info.getPropertyDescriptors();
    
    for (int i = 0; i < pds.length; i++) {
      Method setter = pds[i].getWriteMethod();

      if (setter != null && setter.getName().equals(methodName))
        return pds[i].getName();
    }

    return BeanUtil.methodNameToPropertyName(methodName);
  }
}
