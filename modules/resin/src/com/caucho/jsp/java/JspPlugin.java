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
import java.util.ArrayList;

/**
 * Represents a Java scriptlet.
 */
public class JspPlugin extends JspNode {
  private static final QName NAME = new QName("name");
  private static final QName TYPE = new QName("type");
  private static final QName CODE = new QName("code");
  private static final QName CODEBASE = new QName("codebase");
  private static final QName IEPLUGINURL = new QName("iepluginurl");
  private static final QName JREVERSION = new QName("jreversion");
  private static final QName NSPLUGINURL = new QName("nspluginurl");
  private static final QName WIDTH = new QName("width");
  private static final QName HEIGHT = new QName("height");
  private static final QName ALIGN = new QName("align");
  private static final QName VSPACE = new QName("vspace");
  private static final QName HSPACE = new QName("hspace");
  private static final QName ARCHIVE = new QName("archive");
  
  static final String IE_CLSID = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";
  static final String IE_URL = "http://java.sun.com/products/plugin/1.2.2/jinstall-1_2_2-win.cab#Version=1,2,2,0";
  static final String NS_URL = "http://java.sun.com/products/plugin/";
  
  private String _name;
  private String _type;
  private String _code;

  private String _jreversion;
  private String _nspluginurl;
  private String _iepluginurl;

  private String _width;
  private String _height;
  private String _archive;

  private ArrayList<String> _attrNames = new ArrayList<String>();
  private ArrayList<String> _attrValues = new ArrayList<String>();

  private JspParams _params;

  private JspFallback _fallback;
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (NAME.equals(name)) {
      _name = value;
      
      checkForbidExpression("name", value);
    }
    else if (TYPE.equals(name)) {
      _type = value;

      checkForbidExpression("type", value);
    }
    else if (CODE.equals(name)) {
      _code = value;

      checkForbidExpression("code", value);
    }
    else if (IEPLUGINURL.equals(name)) {
      _iepluginurl = value;
      
      checkForbidExpression("ispluginurl", value);
    }
    else if (JREVERSION.equals(name)) {
      _jreversion = value;
    
      checkForbidExpression("jreversion", value);
    }
    else if (NSPLUGINURL.equals(name)) {
      _nspluginurl = value;
    
      checkForbidExpression("nspluginurl", value);
    }
    else if (WIDTH.equals(name)) {
    }
    else if (HEIGHT.equals(name)) {
    }
    else if (ALIGN.equals(name)) {
      checkForbidExpression("align", value);
    }
    else if (VSPACE.equals(name)) {
      checkForbidExpression("vspace", value);
    }
    else if (HSPACE.equals(name)) {
      checkForbidExpression("hspace", value);
    }
    else if (ARCHIVE.equals(name)) {
      checkForbidExpression("archive", value);
    }
    else if (CODEBASE.equals(name)) {
      checkForbidExpression("codebase", value);
    }
    else
      super.addAttribute(name, value);

    _attrNames.add(name.getName());
    _attrValues.add(value);
  }

  private void checkForbidExpression(String attribute, String  value)
    throws JspParseException
  {
    if (hasELAttribute(value))
      throw error(L.l("'{0}' may not have EL expression '{1}'",
                      attribute, value));
    else if (hasRuntimeAttribute(value))
      throw error(L.l("'{0}' may not have RT expression '{1}'",
                      attribute, value));
  }

  /**
   * Adds a child.
   */
  public void addChild(JspNode node)
    throws JspParseException
  {
    if (node instanceof JspParams) {
      _params = (JspParams) node;
    }
    else if (node instanceof JspFallback) {
      _fallback = (JspFallback) node;
    }
    else if (node instanceof JspBody) {
    }
    else {
      super.addChild(node);
    }
  }

  /**
   * Adds a child.
   */
  public void addChildEnd(JspNode node)
    throws JspParseException
  {
    if (node instanceof JspBody) {
      JspBody body = (JspBody) node;

      for (JspNode child : body.getChildren())
        addChild(child);
    }
    else
      super.addChildEnd(node);
  }

  /**
   * Called after the attributes complete.
   */
  public void endElement()
    throws JspParseException
  {
    if (_code == null)
      throw error(L.l("<jsp:plugin> expects a `code' attribute."));
    
    if (_type == null)
      throw error(L.l("<jsp:plugin> expects a `type' attribute."));
    else if (! _type.equals("applet") && ! _type.equals("bean"))
      throw error(L.l("`type' attribute of <jsp:plugin> must either be `applet' or `bean'."));
    
    if (_iepluginurl == null)
      _iepluginurl = IE_URL;
    
    if (_nspluginurl == null)
      _nspluginurl = NS_URL;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:plugin/>");
  }

  /**
   * Generates the code for the scriptlet
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String type = null;
    if (_type.equals("applet"))
      type = "application/x-java-applet";
    else if (_type.equals("bean"))
      type = "application/x-java-bean";

    if (_jreversion != null)
      type = type + ";version=" + _jreversion;
    
    printText(out, "<object classid=\"" + IE_CLSID + "\"");
    printText(out, " codebase=\"" + _iepluginurl + "\"");

    generateObjectParams(out, false);

    printText(out, "<param name=\"type\" value=\"" + type + "\">\n");

    printText(out, "<comment>");
    
    printText(out, "<embed type=\"" + type + "\"");
    printText(out, " codebase=\"" + _nspluginurl + "\"");
    
    generateObjectParams(out, true);
    
    printText(out, ">\n<noembed></comment>");

    if (_fallback != null)
      _fallback.generateFallback(out);

    printText(out, "</noembed></embed></object>\n");
  }

  /**
   * Generates the parameters for the jsp:plugin object.
   */
  protected void generateObjectParams(JspJavaWriter out, boolean isEmbed)
    throws Exception
  {
    for (int i = 0; i < _attrNames.size(); i++) {
      String name = _attrNames.get(i);
      String value = _attrValues.get(i);
      
      if (name.equals("type") || name.equals("jreversion") ||
          name.equals("iepluginurl") || name.equals("nspluginurl") ||
          name.equals("code") || name.equals("archive") ||
          name.equals("codebase") || name.equals("object"))
        continue;

      printText(out, " " + name + "=\"");

      out.println("out.print(" + generateValue(String.class, value) + ");");
      
      printText(out, "\"");
    }
    
    if (! isEmbed)
      printText(out, ">\n");

    for (int i = 0; i < _attrNames.size(); i++) {
      String name = _attrNames.get(i);
      String value = _attrValues.get(i);
      
      if (name.equals("archive"))
        name = "java_archive";
      else if (name.equals("codebase"))
        name = "java_codebase";
      else if (name.equals("code"))
        name = "java_code";
      else if (name.equals("object"))
        name = "java_object";
      else
        continue;

      if (isEmbed) {
        printText(out, " " + name + "=\"");

        if (hasRuntimeAttribute(value))
          out.println("out.print(" + getRuntimeAttribute(value) + ");");
        else
          printText(out, value);

        printText(out, "\"");
      }
      else {
        printText(out, "<param name=\"" + name + "\" value=\"");

        if (hasRuntimeAttribute(value))
          out.println("out.print(" + getRuntimeAttribute(value) + ");");
        else
          printText(out, value);

        printText(out, "\">\n");
      }
    }

    if (_params == null)
      return;
    
    ArrayList<JspParam> paramList = _params.getParams();

    for (int i = 0; i < paramList.size(); i++) {
      JspParam param = paramList.get(i);

      String name = param.getName();
      String value = param.getValue();

      if (isEmbed)
        printText(out, " " + name + "=\"");
      else
        printText(out, "<param name=\"" + name + "\" value=\"");

      if (hasRuntimeAttribute(value)) {
        out.println("out.print(" + getRuntimeAttribute(value) + ");");
      }
      else
        printText(out, value);

      if (isEmbed)
        printText(out, "\"");
      else
        printText(out, "\">\n");
    }
  }

  private void printText(JspJavaWriter out, String text)
    throws Exception
  {
    out.print("out.print(\"");
    out.printJavaString(text);
    out.println("\");");
  }
}

