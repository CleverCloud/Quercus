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

import com.caucho.config.inject.InjectManager.ReferenceFactory;
import com.caucho.jsp.JspParseException;
import com.caucho.jsp.JspUtil;
import com.caucho.jsp.TagInstance;
import com.caucho.util.BeanUtil;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import javax.servlet.jsp.tagext.*;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a custom tag.
 */
abstract public class GenericTag extends JspContainerNode
{
  private static final Logger log
    = Logger.getLogger(GenericTag.class.getName());
  private static final String DEFAULT_VAR_TYPE = "java.lang.String";

  private static final HashSet<String> _primTypes
    = new HashSet<String>();
  
  protected TagInstance _tag;
  protected TagInfo _tagInfo;
  protected Class _tagClass;
  protected VariableInfo []_varInfo;

  private boolean _isDeclaringInstance;
  
  public GenericTag()
  {
  }
  
  public void setTagInfo(TagInfo tagInfo)
  {
    _tagInfo = tagInfo;
  }

  public TagInfo getTagInfo()
  {
    return _tagInfo;
  }

  @Override
  public boolean isJsp21()
  {
    return ! isPre21Taglib();
  }

  @Override
  public boolean isPre21Taglib()
  {
    if (_tagInfo == null)
      return false;
    
    TagLibraryInfo library = _tagInfo.getTagLibrary();

    if (library == null || library.getRequiredVersion() == null)
      return false;

    return "2.1".compareTo(library.getRequiredVersion()) > 0;
  }

  public TagInstance getTag()
  {
    return _tag;
  }

  /**
   * Returns the tag name for the current tag.
   */
  public String getCustomTagName()
  {
    return _tag.getId();
  }

  /**
   * Returns true if the tag is a simple tag.
   */
  public boolean isSimple()
  {
    return _tag.isSimpleTag();
  }

  /**
   * True if this is a jstl node.
   */
  @Override
  public boolean isJstl()
  {
    String uri = _tag.getTagInfo().getTagLibrary().getURI();
    
    return (JavaJspBuilder.JSTL_CORE_URI.equals(uri)
            || JavaJspBuilder.JSTL_EL_CORE_URI.equals(uri)
            || JavaJspBuilder.JSTL_FMT_URI.equals(uri)
            || JavaJspBuilder.JSTL_XML_URI.equals(uri)
            || JavaJspBuilder.JSTL_EL_XML_URI.equals(uri)
            || JavaJspBuilder.JSTL_SQL_URI.equals(uri)
            || JavaJspBuilder.JSTL_EL_SQL_URI.equals(uri));
  }

  public void setTagClass(Class cl)
  {
    _tagClass = cl;
  }

  public VariableInfo []getVarInfo()
  {
    return _varInfo;
  }

  /**
   * Returns the body content.
   */
  @Override
  public String getBodyContent()
  {
    return _tagInfo.getBodyContent();
  }
  
  /**
   * Adds a child node.
   */
  @Override
  public void addChild(JspNode node)
    throws JspParseException
  {
    if (! "empty".equals(getBodyContent()))
      super.addChild(node);
    else if (node instanceof JspAttribute) {
      super.addChild(node);
    }
    else if (node instanceof StaticText
             && ((StaticText) node).isWhitespace()) {
    }
    else {
      throw error(L.l("<{0}> must be empty.  Since <{0}> has a body-content of 'empty', it must not have any content.",
                      getTagName()));
    }
  }

  /**
   * Completes the element
   */
  @Override
  public void endElement()
    throws Exception
  {
    addTagDepend();
    
    Hashtable<String,Object> tags = new Hashtable<String,Object>();

    for (int i = 0; i < _attributeNames.size(); i++) {
      QName qName = _attributeNames.get(i);
      Object value = _attributeValues.get(i);
      String name = qName.getName();

      TagAttributeInfo attrInfo = getAttributeInfo(qName);
      Method method = getAttributeMethod(qName);
      
      Class type = null;

      if (method != null)
        type = method.getParameterTypes()[0];
      
      if (value instanceof JspAttribute) {
        JspAttribute attr = (JspAttribute) value;

        if (attr.isStatic()) {
          String textValue = attr.getStaticText();

          /*
          if (type != null)
            tags.put(name, staticStringToValue(type, textValue));
          else
            tags.put(name, textValue);
          */
          tags.put(name, textValue);
        }
        else
          tags.put(name, TagData.REQUEST_TIME_VALUE);
      }
      else if (value instanceof String && hasRuntimeAttribute((String) value))
        tags.put(name, TagData.REQUEST_TIME_VALUE);
      else
        tags.put(name, value);

      String typeName = null;

      boolean isFragment = false;

      if (attrInfo != null) {
        typeName = attrInfo.getTypeName();
        isFragment = attrInfo.isFragment();

        if (isFragment &&
            type != null && type.isAssignableFrom(JspFragment.class))
          typeName = JspFragment.class.getName();
      }
      else if (method != null)
        typeName = type.getName();

      if (! isFragment && ! JspFragment.class.getName().equals(typeName)) {
      }
      else if (value instanceof JspAttribute) {
        JspAttribute jspAttr = (JspAttribute) value;

        jspAttr.setJspFragment(true);
      }
    }

    TagData tagData = new TagData(tags);

    try {
      _varInfo = _tagInfo.getVariableInfo(tagData);
    } catch (Exception e) {
      throw error(e);
    }

    if (_varInfo == null)
      _varInfo = fillVariableInfo(_tagInfo.getTagVariableInfos(), tagData);

    TagExtraInfo tei = _tagInfo.getTagExtraInfo();
    ValidationMessage []messages;
    if (tei != null) {
      messages = tei.validate(tagData);

      _gen.addDepend(tei.getClass());

      if (messages != null && messages.length != 0) {
        throw error(messages[0].getMessage());
      }
    }
  }

  protected void addTagDepend()
  {
    if (_tagClass != null)
      _gen.addDepend(_tagClass);
  }
  
  /**
   * True if the node has scripting
   */
  @Override
  public boolean hasScripting()
  {
    if (super.hasScripting())
      return true;

    // Any conflicting values must be set each time.
    for (int i = 0; i < _attributeValues.size(); i++) {
      QName name = _attributeNames.get(i);
      Object value = _attributeValues.get(i);

      try {
        if (value instanceof String && hasRuntimeAttribute((String) value))
          return true;
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
        return true;
      }
    }
    
    return false;
  }

  /**
   * True if the jsf-parent setting is required.
   */
  @Override
  public boolean isJsfParentRequired()
  {
    return true;
  }

  /**
   * Returns true if this instance declares the tag.
   */
  public boolean isDeclaringInstance()
  {
    return _isDeclaringInstance;
  }
  

  /**
   * Returns the variable containing the jsf component
   */
  @Override
  public String getJsfVar()
  {
    return null;
  }

  /**
   * Returns the variable containing the jsf body
   */
  @Override
  public String getJsfBodyVar()
  {
    return null;
  }
  
  @Override
  public void generateClassEpilogue(JspJavaWriter out)
    throws IOException
  {
    super.generateClassEpilogue(out);
    
    if (_tag.getAnalyzedTag().getHasInjection()) {
      out.println();
      out.print("static ");
      out.printClass(ReferenceFactory.class);
      out.println(" _jsp_inject_" + _tag.getId());
      out.print("  = ");
      out.printClass(JspUtil.class);
      out.print(".getInjectFactory(");
      out.printClass(_tag.getTagClass());
      out.println(".class);");
    }
  }
    
  /**
   * Generates code before the actual JSP.
   */
  @Override
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    for (int i = 0; i < _attributeNames.size(); i++) {
      QName name = _attributeNames.get(i);
      Object value = _attributeValues.get(i);
      
      if (! (value instanceof JspFragmentNode))
        continue;
      
      JspFragmentNode frag = (JspFragmentNode) value;
      
      TagAttributeInfo attribute = getAttributeInfo(name);
      String typeName = null;

      boolean isFragment = false;

      if (attribute != null && attribute.isFragment())
        isFragment = true;

      String fragmentClass = JspFragment.class.getName();
      
      if (attribute != null && fragmentClass.equals(attribute.getTypeName()))
        isFragment = true;

      Method method = getAttributeMethod(name);

      if (method != null) {
        typeName = method.getParameterTypes()[0].getName();
        if (fragmentClass.equals(typeName))
          isFragment = true;
      }

      if (isFragment)
        frag.generateFragmentPrologue(out);
    }
      
    TagInstance parent = getParent().getTag();

    boolean isBodyTag = BodyTag.class.isAssignableFrom(_tagClass);
    boolean isEmpty = isEmpty();
    boolean hasBodyContent = isBodyTag && ! isEmpty;

    _tag = parent.findTag(getQName(), _attributeNames,
                          hasBodyContent);

    if (_tag == null || ! _parseState.isRecycleTags()) {
      _tag = parent.addTag(_gen, getQName(), _tagInfo, _tagClass,
                           _attributeNames, _attributeValues,
                           hasBodyContent);

      if (! JspTagFileSupport.class.isAssignableFrom(_tagClass)) {
        out.printClass(_tagClass);
        out.println(" " + _tag.getId() + " = null;");

      }
      _isDeclaringInstance = true;

      /*
      if (SimpleTag.class.isAssignableFrom(_tagClass) && hasCustomTag())
        out.println("javax.servlet.jsp.tagext.Tag " + _tag.getId() + "_adapter = null;");
      */
    }
    else {
      // Any conflicting values must be set each time.
      for (int i = 0; i < _attributeNames.size(); i++) {
        QName name = _attributeNames.get(i);
        Object value = _attributeValues.get(i);
        
        _tag.addAttribute(name, value);
      }
    }

    if (_tag == null)
      throw new NullPointerException();

    /* already taken care of
    if (! isEmpty())
      _tag.setBodyContent(true);
    */

    generatePrologueDeclare(out);
    generatePrologueChildren(out);
  }

  @Override
  public void generatePrologueDeclare(JspJavaWriter out)
    throws Exception
  {
    // Any AT_END variables
    for (int i = 0; _varInfo != null && i < _varInfo.length; i++) {
      VariableInfo var = _varInfo[i];

      if (var == null) {
      }
      else if (! _gen.hasScripting()) {
      }
      else if ((var.getScope() == VariableInfo.AT_END
                || var.getScope() == VariableInfo.AT_BEGIN)
               && var.getDeclare()
               && ! _gen.isDeclared(var.getVarName())) {
        String className = var.getClassName();

        if (className == null
            || "".equals(className)
            || "null".equals(className))
          className = DEFAULT_VAR_TYPE;

        validateClass(className, var.getVarName());
        
        out.print(className + " " + var.getVarName() + " = ");

        _gen.addDeclared(var.getVarName());
        
        if ("byte".equals(var.getClassName())
            || "short".equals(var.getClassName())
            || "char".equals(var.getClassName())
            || "int".equals(var.getClassName())
            || "long".equals(var.getClassName())
            || "float".equals(var.getClassName())
            || "double".equals(var.getClassName()))
          out.println("0;");
        else if ("boolean".equals(var.getClassName()))
          out.println("false;");
        else
          out.println("null;");
      }
    }
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  @Override
  public void printXml(WriteStream os)
    throws IOException
  {
    TagInfo tag = getTagInfo();

    String prefix = tag.getTagLibrary().getPrefixString();
    String uri = tag.getTagLibrary().getURI();
    
    String name = prefix + ':' + tag.getTagName();

    os.print("<" + name);
    
    os.print(" xmlns:" + prefix + "=\"" + uri + "\"");
    
    printJspId(os);

    for (int i = 0; i < _attributeNames.size(); i++) {
      QName attrName = _attributeNames.get(i);
      Object value = _attributeValues.get(i);

      if (value instanceof String) {
        String string = (String) value;

        printXmlAttribute(os, attrName.getName(), string);
      }
    }

    os.print(">");

    printXmlChildren(os);

    os.print("</" + name + ">");
  }

  /**
   * Generates the code for a custom tag.
   *
   * @param out the output writer for the generated java.
   */
  abstract public void generate(JspJavaWriter out)
    throws Exception;

  protected void fillAttributes(JspJavaWriter out, String name)
    throws Exception
  {
    TagAttributeInfo attrs[] = _tagInfo.getAttributes();

    // clear any attributes mentioned in the taglib that aren't set
    for (int i = 0; attrs != null && i < attrs.length; i++) {
      int p = getAttributeIndex(attrs[i].getName());
      
      if (p < 0 && attrs[i].isRequired()) {
        throw error(L.l("required attribute '{0}' missing from <{1}>",
                        attrs[i].getName(),
                        getTagName()));
      }
    }

    boolean isDynamic = DynamicAttributes.class.isAssignableFrom(_tagClass);
    
    // fill all mentioned attributes
    for (int i = 0; i < _attributeNames.size(); i++) {
      QName attrName = _attributeNames.get(i);
      Object value = _attributeValues.get(i);
      
      TagAttributeInfo attribute = getAttributeInfo(attrName);
      
      if (attrs != null && attribute == null && ! isDynamic)
        throw error(L.l("unexpected attribute '{0}' in <{1}>",
                        attrName.getName(), getTagName()));

      if (_tag.getAttribute(attrName) != null)
        continue;

      boolean isFragment = false;

      if (attribute != null) {
        isFragment = (attribute.isFragment()
                      || attribute.getTypeName().equals(JspFragment.class.getName()));
      }

      if (value instanceof JspAttribute
          && ((JspAttribute) value).isJspFragment())
        isFragment = true;

      generateSetAttribute(out, name, attrName, value,
                           attribute == null || attribute.canBeRequestTime(),
                           isFragment, attribute);
    }
  }

  private TagAttributeInfo getAttributeInfo(QName attrName)
  {
    TagAttributeInfo attrs[] = _tagInfo.getAttributes();

    int j = 0;
    for (j = 0; attrs != null && j < attrs.length; j++) {
      if (isNameMatch(attrs[j].getName(), attrName))
        return attrs[j];
    }

    return null;
  }

  private int getAttributeIndex(String name)
  {
    for (int i = 0; i < _attributeNames.size(); i++) {
      QName attrName = _attributeNames.get(i);

      if (isNameMatch(name, attrName))
        return i;
    }

    return -1;
  }

  private boolean isNameMatch(String defName, QName attrName)
  {
    if (defName.equals(attrName.getName())) {
      return true;
    }
    else if (defName.equals(attrName.getLocalName()) &&
             attrName.getPrefix().equals(getQName().getPrefix())) {
      return true;
    }
    else
      return false;
  }

  /**
   * Sets an attribute for a tag
   *
   * @param info the tag's introspected information
   * @param name the tag's Java variable name
   * @param attrName the attribute name to set
   * @param value the new value of the tag.
   */
  void generateSetAttribute(JspJavaWriter out,
                            String name, QName attrName, Object value,
                            boolean allowRtexpr, boolean isFragment,
                            TagAttributeInfo attrInfo)
    throws Exception
  {
    Method method = getAttributeMethod(attrName);

    boolean isDynamic = DynamicAttributes.class.isAssignableFrom(_tagClass);
    
    if (method != null) {
      // jsp/18cq
      if (Modifier.isStatic(method.getModifiers()))
        throw error(L.l("attribute '{0}' may not be a static method.",
                        method.getName()));

      generateSetParameter(out, name, value, method,
                           allowRtexpr, "pageContext", false, isFragment, attrInfo);
    }
    else if (! isDynamic) {
      throw error(L.l("attribute '{0}' in tag '{1}' has no corresponding set method in tag class '{2}'",
                  attrName.getName(), getTagName(), _tagClass.getName()));
    }
    else if (isFragment) {
      String uri = attrName.getNamespaceURI();
      String local = attrName.getLocalName();

      out.print(name + ".setDynamicAttribute(");

      if (uri == null)
        out.print("null, ");
      else
        out.print("\"" + escapeJavaString(uri) + "\", ");
      
      JspFragmentNode frag = (JspFragmentNode) value;
      out.print("\"" + escapeJavaString(local) + "\", ");
      out.print(frag.generateValue());
      out.println(");");
    }
    else {
      String uri = attrName.getNamespaceURI();
      String local = attrName.getLocalName();
      
      out.print(name + ".setDynamicAttribute(");

      if (uri == null)
        out.print("null, ");
      else
        out.print("\"" + escapeJavaString(uri) + "\", ");
      
      out.print("\"" + escapeJavaString(local) + "\", ");
      out.print(generateRTValue(Object.class, value));
      out.println(");");
    }
  }

  private Method getAttributeMethod(QName attrName)
    throws Exception
  {
    Method method = null;
    
    try {
      BeanInfo info = Introspector.getBeanInfo(_tagClass);

      if (info != null)
        method = BeanUtil.getSetMethod(info, attrName.getLocalName());

      if (method != null)
        return method;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    /*
    try {
      method = BeanUtil.getSetMethod(_tagClass, attrName.getLocalName());

      if (method != null)
        return method;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
    */

    return method;
  }

  /**
   * Returns true if there is a tag variable declaration matching the scope.
   */
  protected boolean hasVarDeclaration(int scope)
    throws Exception
  {
    for (int i = 0; _varInfo != null && i < _varInfo.length; i++) {
      VariableInfo var = _varInfo[i];
      
      if (var != null && var.getScope() == scope)
        return true;
    }

    return false;
  }

  /**
   * Prints a tag variable declaration.  Only the variables matching the
   * scope will be printed.
   *
   * @param out the stream to the java code.
   * @param scope the variable scope to print
   */
  protected void printVarDeclaration(JspJavaWriter out, int scope)
    throws Exception
  {
    for (int i = 0; _varInfo != null && i < _varInfo.length; i++) {
      VariableInfo var = _varInfo[i];
      
      if (var != null) {
        printVarDeclare(out, scope, var);
        printVarAssign(out, scope, var);
      }
    }
  }

  /**
   * Prints a tag variable declaration.  Only the variables matching the
   * scope will be printed.
   *
   * @param out the stream to the java code.
   * @param scope the variable scope to print
   */
  protected void printVarDeclare(JspJavaWriter out, int scope)
    throws Exception
  {
    for (int i = 0; _varInfo != null && i < _varInfo.length; i++) {
      VariableInfo var = _varInfo[i];

      if (var != null)
        printVarDeclare(out, scope, var);
    }
  }

  /**
   * Prints a tag variable declaration.  Only the variables matching the
   * scope will be printed.
   *
   * @param out the stream to the java code.
   * @param scope the variable scope to print
   */
  protected void printVarAssign(JspJavaWriter out, int scope)
    throws Exception
  {
    for (int i = 0; _varInfo != null && i < _varInfo.length; i++) {
      VariableInfo var = _varInfo[i];

      if (var != null)
        printVarAssign(out, scope, var);
    }
  }

  /**
   * Returns the VariableInfo corresponding the to tag vars and the tag
   * data.  Mainly, this means looking up the variable names from the
   * attributes for the name-from-attribute.
   *
   * @param tagVars the implicit tag variables for the tag
   * @param tagData the parsed tag attributes
   *
   * @return an array of filled VariableInfo
   */
  protected VariableInfo []fillVariableInfo(TagVariableInfo []tagVars,
                                            TagData tagData)
    throws JspParseException
  {
    if (tagVars == null)
      return null;

    VariableInfo []vars = new VariableInfo[tagVars.length];

    for (int i = 0; i < tagVars.length; i++) {
      TagVariableInfo tagVar = tagVars[i];

      String name = null;
      
      String attributeName = tagVar.getNameFromAttribute();

      if (attributeName != null) {
        Object value = tagData.getAttribute(attributeName);
        
        if (value != null && ! (value instanceof String)) {
          throw error(L.l("tag variable '{0}' may not be a request time attribute",
                          attributeName));
        }
        
        name = tagData.getAttributeString(attributeName);
      }

      if (name == null || "".equals(name) || "null".equals(name))
        name = null;

      if (tagVar.getNameGiven() != null) {
        if (name != null)
          throw error(L.l("name-given='{0}' conflicts with name-from-attribute='{1}' because the attribute value is the same as name-given.",
                          tagVar.getNameGiven(),
                          attributeName));
        
        
        name = tagVar.getNameGiven();
      }
      else if (name == null)
        continue;

      vars[i] = new VariableInfo(name, tagVar.getClassName(),
                                 tagVar.getDeclare(), tagVar.getScope());
    }

    return vars;
  }

  /**
   * Prints a tag variable declaration.  Only the variables matching the
   * scope will be printed.
   *
   * @param out the stream to the java code.
   * @param scope the variable scope to print
   */
  protected void printVarDeclare(JspJavaWriter out,
                                 int scope,
                                 VariableInfo var)
    throws Exception
  {
    if (! _gen.hasScripting()
        || var == null
        || var.getVarName() == null
        || "".equals(var.getVarName())
        || "null".equals(var.getVarName()))
      return;
    
    if (var.getScope() == scope
        || var.getScope() == VariableInfo.AT_BEGIN) {
      if (var.getVarName() == null)
        throw error(L.l("tag variable expects a name"));

      String className = var.getClassName();

      if (className == null
          || "".equals(className)
          || "null".equals(className))
        className = DEFAULT_VAR_TYPE;

      /*
      if (var.getClassName() == null)
        throw error(L.l("tag variable '{0}' expects a classname",
                        var.getVarName()));
      */

      validateVarName(var.getVarName());

      // jsp/107r
      if (var.getDeclare()
          && var.getScope() == scope
          && (var.getScope() == VariableInfo.NESTED && hasScripting()
              || var.getScope() == VariableInfo.AT_BEGIN)
                 && ! varAlreadyDeclared(var.getVarName())) {
        validateClass(className, var.getVarName());
        
        out.println(className + " " + var.getVarName() + ";");
      }
    }
  }

  /**
   * Prints a tag variable declaration.  Only the variables matching the
   * scope will be printed.
   *
   * @param out the stream to the java code.
   * @param scope the variable scope to print
   */
  protected void printVarAssign(JspJavaWriter out, int scope, VariableInfo var)
    throws Exception
  {
    if ("".equals(var.getVarName())
        || "null".equals(var.getVarName()))
      return;
    
    if (var.getScope() == scope
        || var.getScope() == VariableInfo.AT_BEGIN) {
      if (var.getVarName() == null)
        throw error(L.l("tag variable expects a name"));

      String className = var.getClassName();

      if (className == null || className.equals("null"))
        className = DEFAULT_VAR_TYPE;
      
      /*
      if (var.getClassName() == null)
        throw error(L.l("tag variable '{0}' expects a classname",
                        var.getVarName()));
      */

      validateVarName(var.getVarName());

      if (! _gen.hasScripting()) {
      }
      else if (var.getScope() != VariableInfo.NESTED || hasScripting()) {
        out.setLocation(_filename, _startLine);
        out.print(var.getVarName() + " = ");
        String v = "pageContext.findAttribute(\"" + var.getVarName() + "\")";
        convertParameterValue(out, className, v);
        out.println(";");
      }

      try {
        _gen.addBeanClass(var.getVarName(), className);
      } catch (Exception e) {
        Throwable cause = e.getCause();

        if (cause == null)
          cause = e;

        throw error(L.l("'{0}' is an unknown class for tag variable '{1}' in <{2}>",
                        className, var.getVarName(), getTagInfo().getTagName()),
                        cause);
      }
    }
  }

  private void validateVarName(String name)
    throws JspParseException
  {
    if (! Character.isJavaIdentifierStart(name.charAt(0)))
      throw error(L.l("tag variable '{0}' is an illegal Java identifier.", name));

    for (int i = 0; i < name.length(); i++) {
      if (! Character.isJavaIdentifierPart(name.charAt(i)))
        throw error(L.l("tag variable '{0}' is an illegal Java identifier.", name));
    }
  }

  /**
   * Returns true if the variable has been declared.
   */
  private boolean varAlreadyDeclared(String varName)
  {
    if (_gen.isDeclared(varName))
      return true;
    
    for (JspNode node = getParent();
         node != null;
         node = node.getParent()) {
      if (! (node instanceof GenericTag))
        continue;
      if (node instanceof JspFragmentNode)
        break;

      GenericTag tag = (GenericTag) node;
      
      VariableInfo []varInfo = tag.getVarInfo();

      for (int i = 0; varInfo != null && i < varInfo.length; i++) {
        if (varInfo[i] == null)
          continue;
        else if (varInfo[i].getVarName().equals(varName))
          return true;
      }
    }

    return false;
  }

  /**
   * Returns true if the tag instance has been declared
   */
  protected boolean isDeclared()
  {
    if (! _gen.getRecycleTags())
      return false;

    JspNode parent = getParent();

    if (! (parent instanceof JspRoot)
        && ! (parent instanceof JspTop)
        && ! (parent instanceof GenericTag)
        && ! (parent instanceof JspAttribute))
      return false;

    boolean isDeclared = false;

    ArrayList<JspNode> siblings = getParent().getChildren();
    for (int i = 0; i < siblings.size(); i++) {
      JspNode node = siblings.get(i);

      if (node == this) {
        return isDeclared;
      }

      if (hasScriptlet(node)) {
        return false;
      }

      if (node instanceof GenericTag) {
        GenericTag customTag = (GenericTag) node;

        if (customTag.getTag() == getTag())
          isDeclared = true;
      }
    }

    return isDeclared;
  }

  /**
   * Returns true if the node or one of its children is a scriptlet
   */
  protected boolean hasScriptlet(JspNode node)
  {
    if (node instanceof JspScriptlet || node instanceof JspExpression)
      return true;

    ArrayList<JspNode> children = node.getChildren();

    if (children == null)
      return false;

    for (int i = 0; i < children.size(); i++) {
      JspNode child = children.get(i);

      if (hasScriptlet(child))
        return true;
    }

    return false;
  }

  /**
   * Checks that the given class is a valid variable class.
   */
  protected void validateClass(String className, String varName)
    throws JspParseException
  {
    try {
      if (_primTypes.contains(className))
        return;
      else if (className.endsWith("[]")) {
        validateClass(className.substring(0, className.length() - 2), varName);
        return;
      }

      Class<?> cl = _gen.getBeanClass(className);
    } catch (ClassNotFoundException e) {
      throw error(L.l("'{0}' is an unknown class for tag variable '{1}'.",
                      className, varName));
    }
  }

  static {
    _primTypes.add("boolean");
    _primTypes.add("byte");
    _primTypes.add("short");
    _primTypes.add("int");
    _primTypes.add("long");
    _primTypes.add("float");
    _primTypes.add("double");
    _primTypes.add("char");
  }
}
