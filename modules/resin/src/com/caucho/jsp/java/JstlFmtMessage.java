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

public class JstlFmtMessage extends JstlNode {
  private static final QName KEY = new QName("key");
  private static final QName BUNDLE = new QName("bundle");
  private static final QName VAR = new QName("var");
  private static final QName SCOPE = new QName("scope");
  
  private String _key;
  private JspAttribute _keyAttr;
  
  private String _bundle;
  private JspAttribute _bundleAttr;
  
  private String _var;
  private String _scope;

  private ArrayList<JstlFmtParam> _params = new ArrayList<JstlFmtParam>();
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (KEY.equals(name))
      _key = value;
    else if (BUNDLE.equals(name))
      _bundle = value;
    else if (VAR.equals(name))
      _var = value;
    else if (SCOPE.equals(name))
      _scope = value;
    else
      throw error(L.l("`{0}' is an unknown attribute for <{1}>.",
                      name.getName(), getTagName()));
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    if (KEY.equals(name))
      _keyAttr = value;
    else if (BUNDLE.equals(name))
      _bundleAttr = value;
    else
      throw error(L.l("`{0}' is an unsupported jsp:attribute for <{1}>.",
                      name.getName(), getTagName()));
  }

  /**
   * Adds a child element.
   */
  public void addChild(JspNode node)
    throws JspParseException
  {
    if (node instanceof JstlFmtParam)
      _params.add((JstlFmtParam) node);
    else
      super.addChild(node);
  }

  @Override
  public boolean hasCustomTag()
  {
    if (super.hasCustomTag())
      return true;

    for (JstlFmtParam param : _params) {
      if (param.hasCustomTag())
        return true;
    }

    return false;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<fmt:message");

    if (_key != null) {
      os.print(" key=\"" + xmlText(_key) + "\"");
    }

    if (_bundle != null)
      os.print(" bundle=\"" + xmlText(_bundle) + "\"");
    
    if (_var != null)
      os.print(" var=\"" + _var + "\"");
    
    if (_scope != null)
      os.print(" scope=\"" + _scope + "\"");

    os.print(">");

    for (int i = 0; i < _params.size(); i++)
      _params.get(i).printXml(os);

    printXmlChildren(os);

    os.print("</fmt:message>");
  }

  /**
   * Generates the code for the fmt:message tag.
   */
  @Override
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    for (int i = 0; i < _params.size(); i++) {
      _params.get(i).generatePrologue(out);
    }
  }

  /**
   * Generates the code for the fmt:message tag.
   */
  @Override
  public void generateTagState(JspJavaWriter out)
    throws Exception
  {
    for (int i = 0; i < _params.size(); i++) {
      _params.get(i).generateTagState(out);
    }
  }

  /**
   * Generates the code for the fmt:message tag.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String keyExpr = null;

    if (_key != null) {
      keyExpr = generateValue(String.class, _key);
    }
    else if (_keyAttr != null) {
      keyExpr = _keyAttr.generateValue();
    }
    else if (isChildrenStatic()) {
      keyExpr = '"' + escapeJavaString(getStaticText().trim()) + '"';
    }
    else {
      out.println("out = pageContext.pushBody();");

      generateChildren(out);

      keyExpr = "_caucho_var_" + _gen.uniqueId();

      out.println("String " + keyExpr + " = ((com.caucho.jsp.BodyContentImpl) out).getTrimString();");

      out.println("out = pageContext.popBody();");
    }

    // see if there's a fmt:bundle with a prefix

    String prefix = "";
    JspNode node;

    // jsp/1c51, jsp/1c54
    /*
    for (node = getParent(); node != null; node = node.getParent()) {
      if (node instanceof JstlFmtBundle) {
        prefix = ((JstlFmtBundle) node).getPrefixCode();
        break;
      }
    }
    */

    String paramVar = "null";

    if (_params.size() > 0) {
      paramVar = "_caucho_param_" + _gen.uniqueId();

      out.println("Object []" + paramVar + " = new Object[" + _params.size() + "];");

      for (int i = 0; i < _params.size(); i++) {
        JstlFmtParam param = _params.get(i);

        param.generateSet(out, paramVar + "[" + i + "]");

        /*
        String value = param.getAttribute("value");

        if (! value.equals("")) {
          int paramIndex = _gen.addExpr(value);

          out.println(paramVar + "[" + i + "] = _caucho_expr_" + paramIndex + ".evalObject(pageContext);");
        }
        else {
          out.println("out = pageContext.pushBody();");

          _gen.generateChildren(param.getFirstChild());

          out.println(paramVar + "[" + i + "] = ((com.caucho.jsp.BodyContentImpl) out).getTrimString();");

          out.println("out = pageContext.popBody();");
        }
        */
      }
    }


    String locObjVar = "_caucho_loc_object_" + _gen.uniqueId();
    out.println("Object " + locObjVar + ";");

    if (_bundleAttr != null) {
      out.println(locObjVar + " = " + _bundleAttr.generateValue() + ";");
    } else if (_bundle != null) {
      out.println(locObjVar + " = " + generateValue(Object.class, _bundle) + ";");
    } else {
      out.println(locObjVar + " = " + "(javax.servlet.jsp.jstl.fmt.LocalizationContext) pageContext.getAttribute(\"caucho.bundle\");");
    }

    String locCtxVar = "_caucho_loc_ctx_" + _gen.uniqueId();
    out.println("javax.servlet.jsp.jstl.fmt.LocalizationContext " + locCtxVar + ";");

    out.println("if (" + locObjVar + " instanceof javax.servlet.jsp.jstl.fmt.LocalizationContext)");
    out.pushDepth();
    out.println(locCtxVar + " = (javax.servlet.jsp.jstl.fmt.LocalizationContext)" + locObjVar + ";");
    out.popDepth();

    out.println(" else if (" + locObjVar + " instanceof String)");
    out.pushDepth();
    out.println(locCtxVar + " = pageContext.getBundle((String)" + locObjVar + ");");
    out.popDepth();

    out.println("else");
    out.pushDepth();
    out.println(locCtxVar + " = null;");
    out.popDepth();

    out.println();

    String messageVar = "_caucho_message_" + _gen.uniqueId();
    out.println("String " + messageVar + ";");

    out.println("if (" + locCtxVar + " != null) {");
    out.pushDepth();


    String localeVar = "_caucho_locale_" + _gen.uniqueId();
    out.println("java.util.Locale " + localeVar + "= " + locCtxVar + ".getLocale();");
    out.println("if (" + localeVar + " != null)");
    out.pushDepth();
    out.println("com.caucho.jstl.rt.I18NSupport.setResponseLocale(pageContext, " + localeVar + ");");
    out.popDepth();
    out.println(messageVar + " = " + "pageContext.getLocalizedMessage(" +
                locCtxVar + ", " + prefix + keyExpr + ", " + paramVar + ", null);");

    out.popDepth();
    out.println("}");
    out.println("else {");
    out.pushDepth();
    out.println(messageVar + " = " + "pageContext.getLocalizedMessage(null, "
                + prefix + keyExpr + ", " + paramVar + ", null);");
    out.popDepth();
    out.println("}");

    out.println();
    if (_var != null) {
      generateSetOrRemove(out, _var, _scope, messageVar);
    } else {
      out.println("out.print(" + messageVar + ");");
    }

/*
    if (_var != null) {
      String value;

      if (_bundleAttr != null) {
        value = ("pageContext.getLocalizedMessage(" +
                 _bundleAttr.generateValue() + ", " +
                 prefix + keyExpr + ", " + paramVar + ", null)");
      }
      else if (_bundle != null) {
        String bundleExpr = generateValue(Object.class, _bundle);

        value = ("pageContext.getLocalizedMessage(" + bundleExpr + ", " +
                 prefix + keyExpr + ", " + paramVar + ", null)");
      }
      else
        value = ("pageContext.getLocalizedMessage(null, " + prefix + keyExpr + ", " +
                 paramVar + ", null)");

      generateSetOrRemove(out, _var, _scope, value);
    }
    else {
      if (_bundleAttr != null) {
        String bundleExpr = _bundleAttr.generateValue();

        out.println("out.print(pageContext.getLocalizedMessage(" + bundleExpr + ", " + prefix + keyExpr + ", " + paramVar + ", null));");

      }
      else if (_bundle != null) {
        String bundleExpr = generateValue(Object.class, _bundle);

        out.println("out.print(pageContext.getLocalizedMessage(" + bundleExpr + ", " + prefix + keyExpr + ", " + paramVar + ", null));");

      }
      else
        out.println("out.print(pageContext.getLocalizedMessage(null, " + prefix + keyExpr + ", " + paramVar + ", null));");
    }
*/
  }
}
