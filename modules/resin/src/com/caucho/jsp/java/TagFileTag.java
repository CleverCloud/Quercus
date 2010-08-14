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

import com.caucho.xml.QName;

import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.VariableInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.Field;

/**
 * Represents a custom tag.
 */
public class TagFileTag extends GenericTag {
  private boolean _oldLocalScriptingInvalid;
  private JspBody _body;
  private int _maxFragmentIndex;
  private String _contextVarName;
  private Boolean _hasCustomTag;

  /**
   * Called when the attributes end.
   */
  public void endAttributes()
  {
    _oldLocalScriptingInvalid = _parseState.isLocalScriptingInvalid();
    _parseState.setLocalScriptingInvalid(true);
  }
  
  /**
   * Adds a child node.
   */
  public void endElement()
    throws Exception
  {
    super.endElement();
    
    _parseState.setLocalScriptingInvalid(_oldLocalScriptingInvalid);
    
    if (_children == null || _children.size() == 0)
      return;

    for (int i = 0; i < _children.size(); i++) {
      JspNode node = _children.get(i);

      if (node instanceof JspBody) {
        if (_body != null)
          throw error(L.l("Only one <jsp:body> is allowed as a child of a tag."));
        _body = (JspBody) node;
        _children.remove(i);
        return;
      }
    }

    for (int i = 0; i < _children.size(); i++) {
      JspNode node = _children.get(i);

      if (! (node instanceof JspAttribute)) {
        if (_body == null) {
          _body = new JspBody();
          _body.setParent(this);
          _body.setGenerator(_gen);
          _body.endAttributes();
        }

        _body.addChild(node);
      }
    }
    _body.endElement();
    _children = null;
  }

  @Override
  public boolean hasCustomTag()
  {
    return true;
    /*
    if (true) return true;
    
    if (Boolean.TRUE.equals(_hasCustomTag))
      return true;

    if (_hasCustomTag == null && _tagClass != null) {
      try {
        Field hasCustomTagField = _tagClass.getDeclaredField("_caucho_hasCustomTag");
        _hasCustomTag = (Boolean) hasCustomTagField.get(null);

        if (Boolean.TRUE.equals(_hasCustomTag))
          return true;
      } catch (NoSuchFieldException e) {
        //
      } catch (IllegalAccessException e) {
        //
      }
    }

    return super.hasCustomTag() || (_body != null && _body.hasCustomTag());
    */
  }
  /**
   * Generates code before the actual JSP.
   */
  @Override
  public void generateTagState(JspJavaWriter out)
    throws Exception
  {
    if (! isDeclaringInstance())
      return;
    
    out.print("private ");
    out.print(getTagClassName());
    out.println(" " + _tag.getId() + ";");
                                           
    out.println();
    out.print("final ");
    out.print(getTagClassName());
//  out.println(" get" + _tag.getId() + "(PageContext pageContext, javax.servlet.jsp.tagext.JspTag _jsp_parent_tag) throws Throwable");
    out.println(" get" + _tag.getId() + "() throws Throwable");
    out.println("{");
    out.pushDepth();
    
    out.println("if (" + _tag.getId() + " == null) {");
    out.pushDepth();
    
    out.println(_tag.getId() + " = new " + getTagClassName() + "();");

    out.popDepth();
    out.println("}");
    out.println();
    out.println("return " + _tag.getId() + ";");
    
    out.popDepth();
    out.println("}");

    super.generateTagState(out);
  }
  

  /**
   * Generates the code for a custom tag.
   *
   * @param out the output writer for the generated java.
   */
  public void generateDeclaration(JspJavaWriter out)
    throws IOException
  {
    super.generateDeclaration(out);

    /*
    out.println();
    out.println("private static final com.caucho.jsp.java.JspTagFileSupport " + name + " = ");
    out.println("  new " + className + "();");
    */
  }

  /**
   * Returns true if the tag file invocation contains a child tag.
   */
  @Override
  public boolean hasTag()
  {
    return super.hasTag() || _body != null && _body.hasTag();
  }
  /**
   * Returns null, since tag files aren't parent tags.
   */
  /*
  public String getCustomTagName()
  {
    return null;
  }
  */
  
  public String getTagClassName()
  {
    return _tagInfo.getTagClassName();
  }
  
  /**
   * Generates code before the actual JSP.
   */
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    super.generatePrologue(out);

    if (_body != null) {
      _body.setJspFragment(true);
      _body.generateFragmentPrologue(out);
    }

    if (isDeclaringInstance()) {
      out.println(getTagClassName() + " " + getCustomTagName() + ";");
    }
  }
  
  /**
   * Generates the code for a custom tag.
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
    String className = getTagClassName();
 
    _gen.addTagFileClass(className);

    String parentTagName;
    
    JspNode parentTagNode = getParent().getParentTagNode();

    if (parentTagNode == null) {
      parentTagName = null;
    }
    else if (parentTagNode.isSimpleTag()) {
      parentTagName = null;

      String parentName = parentTagNode.getCustomTagName();
      
      out.println("if (" + parentName + "_adapter == null)");
      out.println("  " + parentName + "_adapter = new javax.servlet.jsp.tagext.TagAdapter(" + parentName + ");");
    }
    else {
      parentTagName = parentTagNode.getCustomTagName();
    }
    
    String customTagName = getCustomTagName();
    
    String name = className;

    out.println(customTagName + " = _jsp_state.get" + _tag.getId() + "();");
    
    String childContext = fillTagFileAttributes(out, name, customTagName);
    
    if (true || _body != null) {
      // jsp/1025      
      out.print(customTagName + ".setJspBody(");
      if (_body != null)
        generateFragment(out, _body, "pageContext", false);
      else
        out.print("null");
      out.println(");");
    }

    out.print(customTagName + ".doTag(pageContext, " + childContext + ", out, ");
    
    /*
    if (_body != null)
      generateFragment(out, _body, "pageContext");
    else
      out.print("null");
      */
    out.print(customTagName + ".getJspBody()");

    out.print(", " + parentTagName);

    out.println(");");

    printVarDeclaration(out, VariableInfo.AT_END);
  }
  
  public String fillTagFileAttributes(JspJavaWriter out, 
                                      String tagName,
                                      String customTagName)
    throws Exception
  {
    _contextVarName = "_jsp_context" + _gen.uniqueId();

    String name = _contextVarName;

    out.println("com.caucho.jsp.PageContextWrapper " + name);
    out.println(" = _jsp_pageManager.createPageContextWrapper(pageContext);");

    TagAttributeInfo attrs[] = _tagInfo.getAttributes();

    // clear any attributes mentioned in the taglib that aren't set
    for (int i = 0; attrs != null && i < attrs.length; i++) {
      int p = indexOf(_attributeNames, attrs[i].getName());
      
      if (p < 0 && attrs[i].isRequired()) {
        throw error(L.l("required attribute '{0}' missing from <{1}>",
                        attrs[i].getName(),
                        getTagName()));
      }
    }

    boolean isDynamic = _tagInfo.hasDynamicAttributes();
    String mapAttribute = null;
    String mapName = null;

    if (isDynamic) {
      TagInfoExt tagInfoImpl = (TagInfoExt) _tagInfo;
      mapAttribute = tagInfoImpl.getDynamicAttributesName();
    }
    
    // fill all mentioned attributes
    for (int i = 0; i < _attributeNames.size(); i++) {
      QName attrName = _attributeNames.get(i);
      Object value = _attributeValues.get(i);
      
      TagAttributeInfo attribute = null;
      int j = 0;
      for (j = 0; attrs != null && j < attrs.length; j++) {
        if (attrs[j].getName().equals(attrName.getName())) {
          attribute = attrs[j];
          break;
        }
      }

      if (attribute == null && ! isDynamic)
        throw error(L.l("unexpected attribute `{0}' in <{1}>",
                        attrName.getName(), getTagName()));

      boolean rtexprvalue = true;

      Class cl = null;

      if (attribute != null) {
        cl = _gen.loadBeanClass(attribute.getTypeName());

        rtexprvalue = attribute.canBeRequestTime();
      }
      
      if (cl == null)
        cl = String.class;

      if (attribute == null) {
        /*
        if (mapName == null) {
          mapName = "_jsp_map_" + _gen.uniqueId();
          out.println("java.util.HashMap " + mapName + " = new java.util.HashMap(8);");
          out.println(name + ".setAttribute(\"" + mapAttribute + "\", " + mapName + ");");
        }
        */

        out.print(customTagName + ".setDynamicAttribute(null, \"" + attrName.getName() + "\", ");
      }
      else
        out.print(name + ".setAttribute(\"" + attrName.getName() + "\", ");

      if (value instanceof JspNode) {
        JspFragmentNode frag = (JspFragmentNode) value;

        if (attribute != null &&
            attribute.getTypeName().equals(JspFragment.class.getName())) {
          out.println(generateFragment(frag, "pageContext") + ");");
        }
        else
          out.println(frag.generateValue() + ");");
      }
      else {
        String convValue = generateParameterValue(cl,
                                                  (String) value,
                                                  rtexprvalue,
                                                  attribute,
                                                  _parseState.isELIgnored());
      
        //                                        attribute.allowRtexpr());

        out.println(toObject(cl, convValue) + ");");

        String localName = attrName.getLocalName();
        String upperName = Character.toUpperCase(localName.charAt(0)) + localName.substring(1);

        if (attribute != null) {
          // needed by TeamCity
          out.println(customTagName + ".set" + upperName + "(" + convValue + ");");
        }
        else {
         // out.println(customTagName + ".set" + upperName + "(" + convValue + ");");
        }
      }
      
      /*
      generateSetAttribute(out, customTagName, attrName, value,
                           rtexprvalue,
                           false, attribute);
      */
    }
    
    return name;
  }

  @Override
  protected void addTagDepend()
  {
  }

  private int indexOf(ArrayList<QName> names, String name)
  {
    for (int i = 0; i < names.size(); i++) {
      if (names.get(i).getName().equals(name))
        return i;
    }

    return -1;
  }

  private String toObject(Class cl, String value)
  {
    if (boolean.class.equals(cl))
      return "new Boolean(" + value + ")";
    else if (byte.class.equals(cl))
      return "new Byte(" + value + ")";
    else if (short.class.equals(cl))
      return "new Short(" + value + ")";
    else if (int.class.equals(cl))
      return "new Integer(" + value + ")";
    else if (long.class.equals(cl))
      return "new Long(" + value + ")";
    else if (char.class.equals(cl))
      return "new Character(" + value + ")";
    else if (float.class.equals(cl))
      return "new Float(" + value + ")";
    else if (double.class.equals(cl))
      return "new Double(" + value + ")";
    else
      return value;
  }
}
