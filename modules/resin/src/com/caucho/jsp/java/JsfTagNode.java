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

import com.caucho.jsp.*;
import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.xml.QName;
import com.caucho.vfs.*;

import javax.servlet.jsp.tagext.*;
import javax.faces.component.*;
import javax.faces.event.*;
import javax.el.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;

/**
 * Represents a custom tag.
 */
public class JsfTagNode extends JsfNode
{
  private JspNode _next;
  private Class _componentClass;

  private String _facetName;

  private Attr _idAttr;
  private String _prevJsfId;

  private Attr _bindingAttr;

  private ArrayList<Attr> _attrList = new ArrayList<Attr>();

  public void setComponentClass(Class cl)
  {
    _componentClass = cl;
  }

  public void setNext(JspNode next)
  {
    _next = next;
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName qName, String value)
    throws JspParseException
  {
    String name = qName.getName();

    String setterName = ("set" + Character.toUpperCase(name.charAt(0))
                         + name.substring(1));

    if (name.equals("action")
        && ActionSource2.class.isAssignableFrom(_componentClass))
      setterName = "setActionExpression";
    else if (name.equals("actionListener")
             && ActionSource2.class.isAssignableFrom(_componentClass))
      setterName = "addActionListener";
    else if (name.equals("escape") &&
             UISelectItem.class.isAssignableFrom(_componentClass))
      setterName = "setItemEscaped";

    Method method = findSetter(_componentClass, setterName);

    if (method != null) {
      _attrList.add(new Attr(name, method, value));
    }
    else if (name.equals("binding")) {
      if (! value.startsWith("#{"))
        throw error(L.l("JSF binding attribute requires a deferred value at '{0}'",
                        value));

      Attr attr = new Attr(name, method, value);

      _bindingAttr = attr;
      _attrList.add(attr);
    }
    else {
      super.addAttribute(qName, value);
    }
  }

  /**
   * Adds a JspAttribute attribute.
   *
   * @param qName the name of the attribute.
   * @param value the value of the attribute.
   */
  public void addAttribute(QName qName, JspAttribute value)
    throws JspParseException
  {
    if (value.isStatic()) {
      addAttribute(qName, value.getStaticText().trim());
    }
    else {
      String name = qName.getName();

      String setterName = ("set" + Character.toUpperCase(name.charAt(0))
                           + name.substring(1));

      Method method = findSetter(_componentClass, setterName);

      if (method != null) {
        _attrList.add(new Attr(name, method, value));
      }
      else {
        super.addAttribute(qName, value);
        return;
      }
    }
  }

  /**
   * Called after all the attributes from the tag.
   */
  public void endAttributes()
    throws JspParseException
  {
    if (_parent instanceof JsfFacetNode) {
      _facetName = ((JsfFacetNode) _parent).getName();
    }
    else if (_parent instanceof CustomTag) {
      final QName qName = _parent.getQName();

      if ((qName.getNamespaceURI().indexOf("http://java.sun.com/jsf/core") > -1)
          && "facet".equals(qName.getLocalName())) {

        final Object facetName = ((CustomTag) _parent).getAttribute("name");

        if (facetName instanceof String) {
          _facetName = facetName.toString();
        }
      }
    }

    _idAttr = findAttribute("id");

    if (_idAttr != null)
      addJsfId(_parent);
    else if (isJsfIdRequired())
      addJsfId(this);

    if (_idAttr == null) {
      _prevJsfId = getPrevJsfId();
    }
  }

  public void endElement()
  {
    if (isJsfParentRequired()) {
      addJsfId(this);
    }
  }

  String getJsfId()
  {
    if (_idAttr != null)
      return _idAttr.getValue();
    else
      return null;
  }

  private boolean isJsfIdRequired()
  {
    if (EditableValueHolder.class.isAssignableFrom(_componentClass))
      return true;
    else if (ActionSource.class.isAssignableFrom(_componentClass))
      return true;
    else if (ActionSource2.class.isAssignableFrom(_componentClass))
      return true;
    else if (_bindingAttr != null)
      return true;
    else if (UISelectItem.class.isAssignableFrom(_componentClass))
      return true;
    else if (UISelectItems.class.isAssignableFrom(_componentClass))
      return true;
    
    JspNode parent = _parent;

    ArrayList<JspNode> children = parent.getChildren();

    boolean isJsfIdRequired = false;
      
    for (int i = 0; i < children.size(); i++) {
      JspNode child = children.get(i);

      if (child == this)
        return isJsfIdRequired;
      else if (child instanceof JsfTagNode) {
        JsfTagNode jsfNode = (JsfTagNode) child;
        String id = jsfNode.getJsfId();

        if (id != null)
          isJsfIdRequired = false;
      }
      else if (child.isJsfParentRequired())
        isJsfIdRequired = true;
    }

    return false;
  }
  
  private String getPrevJsfId()
  {
    JspNode parent = _parent;

    ArrayList<JspNode> children = parent.getChildren();

    if (children != null) {
      boolean isFound = false;
      
      for (int i = children.size() - 1; i >= 0; i--) {
        JspNode child = children.get(i);

        if (child == this)
          isFound = true;
        else if (isFound && child instanceof JsfTagNode) {
          JsfTagNode jsfNode = (JsfTagNode) child;
          String id = jsfNode.getJsfId();

          if (id != null)
            return id;
        }
      }
    }

    return null;
  }

  private static void addJsfId(JspNode node)
  {
    if (node == null)
      return;

    addJsfId(node.getParent());

    if (node instanceof JsfTagNode) {
      JsfTagNode jsfNode = (JsfTagNode) node;

      if (jsfNode._idAttr != null)
        return;
      
      jsfNode._idAttr = jsfNode.findAttribute("id");

      if (jsfNode._idAttr == null) {
        Method method = jsfNode.findSetter(jsfNode._componentClass, "setId");

        if (method == null)
          throw new IllegalStateException(jsfNode._componentClass + " id");
      
        String id = "j_id_" + jsfNode._gen.generateJspId();

        jsfNode._idAttr = new Attr("id", method, id);

        jsfNode._attrList.add(jsfNode._idAttr);
      }
    }
  }

  private Attr findAttribute(String name)
  {
    for (int i = 0; i < _attrList.size(); i++) {
      Attr attr = _attrList.get(i);
      
      if (attr.getName().equals(name))
        return attr;
    }

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
    if (_next != null)
      _next.printXml(os);
  }
  
  /**
   * generates prologue data.
   */
  @Override
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    _var = "_jsp_comp" + _gen.uniqueId();
    
    super.generatePrologue(out);
  }
  
  /**
   * Generates the code for a custom tag.
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String contextVar = "null";

    if (_gen.isJsfPrologueInit())
      contextVar = "_jsp_faces_context";
    
    String parentVar = _parent.getJsfVar();

    String parentBodyVar = _parent.getJsfBodyVar();

    String prevId;

    String oldParentVar = null;

    String bindingVar = null;

    if (isJsfParentRequired()) {
      oldParentVar = "_jsp_jsf_parent_" + _gen.uniqueId();

      out.println("Object " + oldParentVar
                  + " = request.getAttribute(\"caucho.jsf.parent\");");
    }

    if (_prevJsfId != null)
      prevId = "\"" + _prevJsfId + "\"";
    else
      prevId = null;

    if (parentBodyVar != null) {
      out.println("com.caucho.jsp.jsf.JsfTagUtil.addVerbatim("
                  + parentVar
                  + ", " + prevId
                  + ", " + parentBodyVar + ");");
    }

    if (! hasBodyContent()) {
    }
    else if (parentBodyVar != null)
      _bodyVar = parentBodyVar;
    else {
      _bodyVar = "_jsp_body" + _gen.uniqueId();

      out.println("com.caucho.jsp.BodyContentImpl " + _bodyVar
                  + " = (com.caucho.jsp.BodyContentImpl) pageContext.pushBody();");
      out.println("out =  " + _bodyVar + ";");
    }

    String className;

    if (_bindingAttr != null)
      className = UIComponent.class.getName();
    else
      className = _componentClass.getName();

    out.println(className + " " + _var + ";");

    if (_bindingAttr != null) {
      bindingVar = ("_caucho_value_expr_" +
                    _gen.addValueExpr(_bindingAttr.getValue(),
                                      UIComponent.class.getName()));
    }

    if (_facetName != null) {
      out.print(_var + " = (" + className + ")");
      out.println(" com.caucho.jsp.jsf.JsfTagUtil.findFacet("
                  + contextVar
                  + ", request"
                  + ", " + parentVar
                  + ", \"" + _facetName + "\");");

      out.println("if (" + _var + " == null) {");
      out.pushDepth();
      
      out.print(_var + " = (" + className + ")");
      out.println(" com.caucho.jsp.jsf.JsfTagUtil.addFacet("
                  + contextVar
                  + ", request"
                  + ", " + parentVar
                  + ", \"" + _facetName + "\""
                  + ", " + bindingVar
                  + ", " + _componentClass.getName() + ".class);");
    }
    else if (_idAttr != null) {
      out.print(_var + " = (" + className + ")");
      out.println(" com.caucho.jsp.jsf.JsfTagUtil.findPersistent("
                  + contextVar
                  + ", request"
                  + ", " + parentVar
                  + ", \"" + _idAttr.getValue() + "\");");

      out.println("if (" + _var + " == null) {");
      out.pushDepth();
      
      out.print(_var + " = (" + className + ")");
      out.println(" com.caucho.jsp.jsf.JsfTagUtil.addPersistent("
                  + contextVar
                  + ", request"
                  + ", " + parentVar
                  + ", " + bindingVar
                  + ", " + _componentClass.getName() + ".class);");
    }
    else {
      out.print(_var + " = (" + className + ")");

      out.println(" com.caucho.jsp.jsf.JsfTagUtil.addTransient("
                  + contextVar
                  + ", request"
                  + ", " + parentVar
                  + ", " + prevId
                  + ", " + _componentClass.getName() + ".class);");
    }

    for (int i = 0; i < _attrList.size(); i++) {
      Attr attr = (Attr) _attrList.get(i);

      Method method = attr.getMethod();
      Class type = null;

      if (method != null)
        type = method.getParameterTypes()[0];
      else if (attr.getName().equals("binding"))
        type = UIComponent.class;
      
      JspAttribute jspAttr = attr.getAttr();
      String value = attr.getValue();

      if (jspAttr != null) {
        generateSetParameter(out, _var, jspAttr, method,
                             true, null, false, false, null);
      }
      else if (ActionListener.class.isAssignableFrom(type)) {
        String exprVar = "_caucho_method_expr_" + _gen.addMethodExpr(value, "void foo(javax.faces.event.ActionEvent)");

        out.println(_var + ".addActionListener(new javax.faces.event.MethodExpressionActionListener(" + exprVar + "));");
      }
      else if ("validator".equals(attr.getName())
               && UIInput.class.isAssignableFrom(_componentClass)) {
        out.print("((javax.faces.component.UIInput)" + _var + ").addValidator(new javax.faces.validator.MethodExpressionValidator(");

        String signature
          = "void foo(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)";

        String exprVar = "_caucho_method_expr_" +
                         _gen.addMethodExpr(value, signature);

        out.println(exprVar + "));");
      }
      else if ("valueChangeListener".equals(attr.getName())
               && UIInput.class.isAssignableFrom(_componentClass)) {
        out.print("((javax.faces.component.UIInput)" + _var + ").addValueChangeListener(new javax.faces.event.MethodExpressionValueChangeListener(");

        String signature = "void foo(javax.faces.event.ValueChangeEvent)";

        String exprVar = "_caucho_method_expr_" +
                         _gen.addMethodExpr(value, signature);

        out.println(exprVar + "));");
      }
      else if (_bindingAttr != null && ! "id".equals(attr.getName())
               || (value.indexOf("#{") >= 0
                   && ! ValueExpression.class.isAssignableFrom(type)
                   && ! MethodExpression.class.isAssignableFrom(type))) {
        // jsf/3153
        out.print(_var + ".setValueExpression(\"" + attr.getName() + "\", ");

        String exprVar = "_caucho_value_expr_" + _gen.addValueExpr(value, type.getName());

        out.println(exprVar + ");");
      }
      else {
        out.print(_var + "." + method.getName() + "(");

        out.print(generateParameterValue(type, value, true, null, false));

        out.println(");");
      }
    }

    if (_idAttr != null || _facetName != null) {
      if (oldParentVar != null)
        out.println("request.setAttribute(\"caucho.jsf.parent\""
                    + ", new com.caucho.jsp.jsf.JsfComponentTag("
                    + _var + ", true, " + _bodyVar + "));");
      
      out.popDepth();
      out.println("}");

      if (oldParentVar != null) {
        out.println("else");
        out.println("  request.setAttribute(\"caucho.jsf.parent\""
                    + ", new com.caucho.jsp.jsf.JsfComponentTag("
                    + _var + ", false, " + _bodyVar + "));");
      }
    }

    generateChildren(out);

    if (oldParentVar != null) {
      out.println("request.setAttribute(\"caucho.jsf.parent\", "
                  + oldParentVar + ");");
    }

    if (_bodyVar != null && parentBodyVar == null) {
      out.println("out = pageContext.popBody();");
      out.println("pageContext.releaseBody(" + _bodyVar + ");");
    }
  }
}
