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
import com.caucho.jsp.cfg.TldAttribute;
import com.caucho.jsp.cfg.TldVariable;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import java.io.IOException;
import java.util.ArrayList;

public class JspDirectiveVariable extends JspNode
{
  private static final QName NAME_GIVEN = new QName("name-given");
  private static final QName NAME_FROM_ATTRIBUTE =
    new QName("name-from-attribute");
  private static final QName ALIAS = new QName("alias");
  private static final QName VARIABLE_CLASS = new QName("variable-class");
  private static final QName DECLARE = new QName("declare");
  private static final QName SCOPE = new QName("scope");
  private static final QName DESCRIPTION = new QName("description");
  
  static final L10N L = new L10N(JspDirectiveVariable.class);

  private String _nameGiven;
  private String _nameFromAttribute;
  private String _alias;
  private String _variableClass;
  private boolean _isDeclare = true;
  private String _scope;
  private String _description;
  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (! _gen.getParseState().isTag())
      throw error(L.l("'{0}' is only allowed in .tag files.  Attribute directives are not allowed in normal JSP files.",
                      getTagName()));
    
    JavaTagGenerator gen = (JavaTagGenerator) _gen;
    if (NAME_GIVEN.equals(name)) {
      if (gen.findVariable(value) != null) {
        throw error(L.l("@variable name-given '{0}' is already used by another variable.",
                        value));
      }
      else if (gen.findNameFromAttributeVariable(value) != null) {
        throw error(L.l("@variable name-from-attribute '{0}' is already used by another variable.",
                        value));
      }
      else if (gen.findAttribute(value) != null) {
        throw error(L.l("@variable name-given '{0}' is already used by an attribute.",
                        value));
      }
      else if (value.equals(gen.getDynamicAttributes())) {
        throw error(L.l("@variable name-given '{0}' cannot be the same as the tag's dynamic-attributes.",
                        value));
      }
      
      _nameGiven = value;
    }
    else if (NAME_FROM_ATTRIBUTE.equals(name)) {
      if (gen.findVariable(value) != null) {
        throw error(L.l("@variable name-from-attribute '{0}' is already used by another variable.",
                        value));
      }
      else if (gen.findNameFromAttributeVariable(value) != null) {
        throw error(L.l("@variable name-from-attribute '{0}' is already used by another variable.",
                        value));
      }
      /*
      else if (gen.findAttribute(value) != null) {
        throw error(L.l("@variable name-from-attribute '{0}' is already used by an attribute.",
                        value));
      }
      */
      
      _nameFromAttribute = value;
    }
    else if (ALIAS.equals(name)) {
      if (gen.findAttribute(value) != null) {
        throw error(L.l("@variable alias '{0}' is already used by an attribute.",
                        value));
      }
      
      _alias = value;
    }
    else if (VARIABLE_CLASS.equals(name))
      _variableClass = value;
    else if (DECLARE.equals(name))
      _isDeclare = attributeToBoolean(name.getName(), value);
    else if (SCOPE.equals(name)) {
      if (! "NESTED".equals(value) &&
          ! "AT_BEGIN".equals(value) &&
          ! "AT_END".equals(value))
        throw error(L.l("'{0}' is an illegal scope value.  NESTED, AT_BEGIN, and AT_END are the only accepted values.",
                        value));

      _scope = value;
    }
    else if (DESCRIPTION.equals(name))
      _description = value;
    else {
      throw error(L.l("'{0}' is an unknown JSP variable directive attributes.  Valid attributes are: alias, declare, description, name-from-attribute, name-given, scope, variable-class.",
                      name.getName()));
    }
  }

  /**
   * When the element complets.
   */
  public void endElement()
    throws JspParseException
  {
    if (! _gen.getParseState().isTag())
      throw error(L.l("'{0}' is only allowed in .tag files.  Variable directives are not allowed in normal JSP files.",
                      getTagName()));
    
    if (_nameGiven == null && _nameFromAttribute == null)
      throw error(L.l("<{0}> needs a 'name-given' or 'name-from-attribute' attribute.",
                      getTagName()));

    if (_nameFromAttribute != null && _alias == null)
      throw error(L.l("<{0}> needs an 'alias' attribute.  name-from-attribute requires an alias attribute.",
                      getTagName()));
    if (_alias != null && _nameFromAttribute == null)
      throw error(L.l("<{0}> needs an 'name-from-attribute' attribute.  alias requires a name-from-attribute attribute.",
                      getTagName()));

    JavaTagGenerator tagGen = (JavaTagGenerator) _gen;

    TldVariable var = new TldVariable();
    var.setNameGiven(_nameGiven);
    var.setNameFromAttribute(_nameFromAttribute);
    var.setAlias(_alias);

    String name = _nameGiven;
    if (name == null)
      name = _nameFromAttribute;

    if (_variableClass != null)
      var.setVariableClass(_variableClass);

    var.setDeclare(_isDeclare);
    if (_scope != null)
      var.setScope(_scope);

    tagGen.addVariable(var);
  }
  
  /**
   * Return true if the node only has static text.
   */
  public boolean isStatic()
  {
    return true;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:directive.variable");
    os.print(" jsp:id=\"" + _gen.generateJspId() + "\"");

    if (_nameGiven != null)
      os.print(" name-given=\"" + _nameGiven + "\"");

    if (_nameFromAttribute != null)
      os.print(" name-from-attribute=\"" + _nameFromAttribute + "\"");

    if (_variableClass != null)
      os.print(" variable-class=\"" + _variableClass + "\"");

    os.print("/>");
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    JavaTagGenerator gen = (JavaTagGenerator) _gen;

    if (_nameFromAttribute == null)
      return;

    ArrayList<TldAttribute> attributes = gen.getAttributes();
    for (int i = 0; i < attributes.size(); i++) {
      TldAttribute attr = attributes.get(i);

      if (! attr.getName().equals(_nameFromAttribute))
        continue;

      if (! String.class.equals(attr.getType()))
        throw error(L.l("name-from-attribute variable '{0}' needs a matching String attribute, not '{1}', because the JSP 2.1 specification requires a String.",
                        _nameFromAttribute, attr.getType().getName()));

      if (! attr.getRequired() && attr.getRequiredVar() != null)
        throw error(L.l("name-from-attribute '{0}' needs an attribute declaration with the required=\"true\" attribute, according to the JSP 2.1 specification.",
                        _nameFromAttribute));

      return;
    }
    
    throw error(L.l("name-from-attribute variable '{0}' needs a matching String attribute.",
                    _nameFromAttribute));
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
  }
}
