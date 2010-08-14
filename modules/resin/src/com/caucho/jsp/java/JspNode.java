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

import com.caucho.el.Expr;
import com.caucho.jsp.JspLineParseException;
import com.caucho.jsp.JspParseException;
import com.caucho.jsp.JspParser;
import com.caucho.jsp.Namespace;
import com.caucho.jsp.ParseState;
import com.caucho.jsp.TagInstance;
import com.caucho.util.CharBuffer;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.util.LineCompileException;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;
import com.caucho.xpath.NamespaceContext;
import com.caucho.xpath.XPath;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JspNode {
  static final L10N L = new L10N(JspNode.class);
  private static final Logger log 
    = Logger.getLogger(JspNode.class.getName());

  static final String JSP_NS = JspParser.JSP_NS;

  protected Path _sourcePath;
  protected String _filename;
  protected int _startLine;
  protected int _endAttributeLine;
  protected int _endLine;

  protected Namespace _ns;
  
  protected JavaJspGenerator _gen;
  protected ParseState _parseState;

  protected QName _name;
  protected JspNode _parent;

  protected JspNode()
  {
  }

  /**
   * Sets the Java generator.
   */
  public void setGenerator(JavaJspGenerator gen)
  {
    _gen = gen;
  }

  /**
   * Sets the parse state
   */
  public void setParseState(ParseState parseState)
  {
    _parseState = parseState;
  }

  /**
   * Returns the qname of the node.
   */
  public QName getQName()
  {
    return _name;
  }

  /**
   * Sets the node's qname
   */
  public void setQName(QName name)
  {
    _name = name;
  }

  /**
   * Returns the qname of the node.
   */
  public String getTagName()
  {
    if (_name != null)
      return _name.getName();
    else
      return "jsp:unknown";
  }
  
  /**
   * Returns the parent node.
   */
  public JspNode getParent()
  {
    return _parent;
  }

  /**
   * Sets the parent node
   */
  public void setParent(JspNode parent)
  {
    _parent = parent;
  }

  /**
   * Sets the start location of the node.
   */
  public void setStartLocation(Path sourcePath, String filename, int line)
  {
    _sourcePath = sourcePath;
    _filename = filename;
    _startLine = line;
    _endAttributeLine = line;
  }

  /**
   * Sets the end location of the node.
   */
  public void setEndAttributeLocation(String filename, int line)
  {
    if (_filename != null && _filename.equals(filename))
      _endAttributeLine = line;
  }

  /**
   * Sets the end location of the node.
   */
  public void setEndLocation(String filename, int line)
  {
    if (_filename != null && _filename.equals(filename))
      _endLine = line;
  }

  /**
   * Gets the filename of the node
   */
  public String getFilename()
  {
    return _filename;
  }

  /**
   * Gets the starting line number
   */
  public int getStartLine()
  {
    return _startLine;
  }

  /**
   * Gets the attribute ending line number
   */
  public int getEndAttributeLine()
  {
    return _endAttributeLine;
  }

  /**
   * Gets the ending line number
   */
  public int getEndLine()
  {
    return _endLine;
  }
  
  /**
   * True if the node only has static text.
   */
  public boolean isStatic()
  {
    return false;
  }

  /**
   * True if this is a jstl node.
   */
  public boolean isJstl()
  {
    return false;
  }

  /**
   * True for 2.1 or later taglib
   */
  public boolean isJsp21()
  {
    return true;
  }

  /**
   * Returns the static text.
   */
  public String getStaticText()
  {
    CharBuffer cb = CharBuffer.allocate();

    getStaticText(cb);

    return cb.close();
  }

  /**
   * Returns the static text.
   */
  public void getStaticText(CharBuffer cb)
  {
  }
  
  /**
   * True if the node has scripting (counting rtexpr)
   */
  public boolean hasScripting()
  {
    return false;
  }
  
  /**
   * True if the node has scripting element (i.e. not counting rtexpr values)
   */
  public boolean hasScriptingElement()
  {
    return false;
  }
  
  /**
   * Finds the first scripting node
   */
  public JspNode findScriptingNode()
  {
    if (hasScripting())
      return this;
    else
      return null;
  }

  /**
   * Returns the body content.
   */
  public String getBodyContent()
  {
    return "jsp";
  }
  
  /**
   * True if the node contains a child tag.
   */
  public boolean hasCustomTag()
  {
    return false;
  }
  
  /**
   * True if the node contains a child tag.
   */
  public boolean hasTag()
  {
    return hasCustomTag();
  }

  /**
   * Returns the tag name for the current tag.
   */
  public String getCustomTagName()
  {
    return null;
  }

  /**
   * Returns true for a simple tag.
   */
  public boolean isSimpleTag()
  {
    return false;
  }

  /**
   * Returns parent tag node
   */
  public JspNode getParentTagNode()
  {
    if (getCustomTagName() != null)
      return this;
    else {
      JspNode parent = getParent();

      if (parent != null)
        return parent.getParentTagNode();
      else
        return null;
    }
  }

  /**
   * Returns parent tag node
   */
  public String getParentTagName()
  {
    if (getCustomTagName() != null)
      return getCustomTagName();
    else {
      JspNode parent = getParent();

      if (parent != null)
        return parent.getParentTagName();
      else
        return null;
    }
  }

  /**
   * Returns true if the namespace decl has been printed.
   */
  public boolean hasNamespace(String prefix, String uri)
  {
    if (_parent == null)
      return false;
    else
      return _parent.hasNamespace(prefix, uri);
  }

  /**
   * Adds a namespace, e.g. from a prefix declaration.
   */
  public final void addNamespace(String prefix, String value)
  {
    addNamespaceRec(prefix, value);
  }

  /**
   * Adds a namespace, e.g. from a prefix declaration.
   */
  public final void setNamespace(Namespace ns)
  {
    _ns = ns;
  }

  /**
   * Returns the XPath namespace context.
   */
  public final NamespaceContext getNamespaceContext()
  {
    NamespaceContext ns = null;

    for (Namespace ptr = _ns; ptr != null; ptr = ptr.getNext()) {
      // jsp/1g58
      if (! "".equals(ptr.getPrefix()))
        ns = new NamespaceContext(ns, ptr.getPrefix(), ptr.getURI());
    }

    return ns;
  }

  /**
   * Adds a namespace, e.g. from a prefix declaration.
   */
  public void addNamespaceRec(String prefix, String value)
  {
    if (_parent != null)
      _parent.addNamespaceRec(prefix, value);
  }

  /**
   * Adds a namespace, e.g. from a prefix declaration.
   */
  public String getNamespacePrefix(String uri)
  {
    if (_parent != null)
      return _parent.getNamespacePrefix(uri);
    else
      return null;
  }

  /**
   * Returns true if the namespace decl has been printed.
   */
  public boolean hasNamespace(QName name)
  {
    return hasNamespace(name.getPrefix(), name.getNamespaceURI());
  }

  /**
   * Adds an attribute.
   */
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
  public void addAttribute(QName name, JspAttribute value)
    throws JspParseException
  {
    if (value.isStatic()) {
      addAttribute(name, value.getStaticText().trim());
    }
    else
      throw error(L.l("attribute '{0}' is not allowed in <{1}>.",
                      name.getName(), getTagName()));
  }

  /**
   * Called after all the attributes from the tag.
   */
  public void endAttributes()
    throws JspParseException
  {
  }

  /**
   * Adds text.
   */
  public JspNode addText(String text)
    throws JspParseException
  {
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);

      if (! XmlChar.isWhitespace(ch))
        throw error(L.l("Text is not allowed in <{0}> at '{1}'.",
                        _name.getName(), text));
    }

    return null;
  }

  /**
   * Adds a child node.
   */
  public void addChild(JspNode node)
    throws JspParseException
  {
    node.setParent(this);
    
    if (node instanceof JspAttribute) {
    }
    else if (node instanceof StaticText
        && ((StaticText) node).isWhitespace()) {
    }
    else
      throw node.error(L.l("<{0}> does not allow any child elements at {1}",
                           getTagName(), node.getTagName()));
  }


  /**
   * Adds a child node after its completely initialized..
   */
  public void addChildEnd(JspNode node)
    throws JspParseException
  {
    if (node instanceof JspAttribute) {
      JspAttribute attr = (JspAttribute) node;

      QName name = attr.getName();

      addAttribute(name, attr);
    }
  }
  
  /**
   * Called when the tag closes.
   */
  public void endElement()
    throws Exception
  {
  }

  /**
   * Returns the children.
   */
  public ArrayList<JspNode> getChildren()
  {
    return null;
  }

  /**
   * Returns the TagInstance of the enclosing parent.
   */
  public TagInstance getTag()
  {
    JspNode parent = getParent();

    if (parent != null)
      return parent.getTag();
    else {
      return _gen.getRootTag();
    }
  }

  /**
   * Return true for pre-21 taglib.
   */
  public boolean isPre21Taglib()
  {
    return false;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  abstract public void printXml(WriteStream os)
    throws IOException;

  /**
   * Prints the jsp:id
   */
  public void printJspId(WriteStream os)
    throws IOException
  {
    os.print(" jsp:id=\"" + _gen.generateJspId() + "\"");
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXmlText(WriteStream os, String text)
    throws IOException
  {
    os.print(xmlText(text));
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXmlAttribute(WriteStream os, String name, String text)
    throws IOException
  {
    os.print(" ");
    os.print(name);
    os.print("=\"");

    if (text.startsWith("<%=") && text.endsWith("%>")) {
      os.print("%=");
      os.print(xmlAttrText(text.substring(3, text.length() - 2)));
      os.print("%");
    }
    else
      os.print(xmlAttrText(text));
    os.print("\"");
  }

  /**
   * Generates the XML text.
   */
  public String xmlText(String text)
  {
    if (text == null)
      return "";
    
    CharBuffer cb = new CharBuffer();
    
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);

      switch (ch) {
      case '<':
        cb.append("&lt;");
        break;
      case '>':
        cb.append("&gt;");
        break;
      case '&':
        cb.append("&amp;");
        break;
      case '"':
        cb.append("&quot;");
        break;
      default:
        cb.append(ch);
        break;
      }
    }

    return cb.toString();
  }

  /**
   * Generates the XML text.
   */
  public String xmlAttrText(String text)
  {
    if (text == null)
      return "";
    
    CharBuffer cb = new CharBuffer();
    
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);

      switch (ch) {
      case '&':
        cb.append("&amp;");
        break;
      case '<':
        cb.append("&lt;");
        break;
      case '>':
        cb.append("&gt;");
        break;
      case '"':
        cb.append("&quot;");
        break;
      case '\'':
        cb.append("&apos;");
        break;
      default:
        cb.append(ch);
        break;
      }
    }

    return cb.toString();
  }


  /**
   * Generates the start location.
   */
  public void generateStartLocation(JspJavaWriter out)
    throws IOException
  {
    out.setLocation(_filename, _startLine);
  }

  /**
   * Generates the start location.
   */
  public void generateEndLocation(JspJavaWriter out)
    throws IOException
  {
    out.setLocation(_filename, _endLine);
  }

  /**
   * generates prologue data.
   */
  public void generatePrologue(JspJavaWriter out)
    throws Exception
  {
    generatePrologueChildren(out);
  }

  /**
   * generates prologue data.
   */
  public void generatePrologueDeclare(JspJavaWriter out)
    throws Exception
  {
  }

  /**
   * generates data for prologue children.
   */
  public void generatePrologueChildren(JspJavaWriter out)
    throws Exception
  {
  }

  /**
   * generates declaration data.
   */
  public void generateDeclaration(JspJavaWriter out)
    throws IOException
  {
    generateDeclarationChildren(out);
  }

  /**
   * generates data for declaration children.
   */
  public void generateDeclarationChildren(JspJavaWriter out)
    throws IOException
  {
  }

  /**
   * generates tag state
   */
  public void generateTagState(JspJavaWriter out)
    throws Exception
  {
    generateTagStateChildren(out);
  }

  /**
   * generates tag state
   */
  public void generateTagStateChildren(JspJavaWriter out)
    throws Exception
  {
  }

  /**
   * generates tag state release
   */
  public void generateTagRelease(JspJavaWriter out)
    throws Exception
  {
    generateTagReleaseChildren(out);
  }

  /**
   * generates tag state
   */
  public void generateTagReleaseChildren(JspJavaWriter out)
    throws Exception
  {
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  abstract public void generate(JspJavaWriter out)
    throws Exception;

  /**
   * Generates the code for the children.
   *
   * @param out the output writer for the generated java.
   */
  public void generateChildren(JspJavaWriter out)
    throws Exception
  {
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generateClassEpilogue(JspJavaWriter out)
    throws IOException
  {
    generateClassEpilogueChildren(out);
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generateClassEpilogueChildren(JspJavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generateStatic(JspJavaWriter out)
    throws Exception
  {
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generateEmpty()
    throws Exception
  {
    generateChildrenEmpty();
  }

  /**
   * Generates the code for the children.
   *
   * @param out the output writer for the generated java.
   */
  public void generateChildrenEmpty()
    throws Exception
  {
  }

  /**
   * Converts the string to a boolean.
   */
  protected boolean attributeToBoolean(String attr, String value)
    throws JspParseException
  {
    if (value.equals("yes") || value.equals("true"))
      return true;
    else if (value.equals("no") || value.equals("false"))
      return false;
    else
      throw error(L.l("'{0}' is an unknown value for {1}.  'true' or 'false' are the expected values.",
                      value, attr));
  }

  /**
   * Returns true if in a fragment
   */
  public boolean isInFragment()
  {
    for (JspNode node = getParent(); node != null; node = node.getParent()) {
      if (node instanceof JspAttribute || node instanceof CustomSimpleTag)
        return true;
    }

    return false;
  }
    
  void generateSetParameter(JspJavaWriter out,
                            String obj, Object objValue, Method method,
                            boolean allowRtexpr, 
                            String contextVar, boolean isParentSimpleTag,
                            boolean isFragment, TagAttributeInfo attrInfo)
    throws Exception
  {
    Class<?> type = method.getParameterTypes()[0];

    if (isFragment || JspFragment.class.equals(type)) {
      generateFragmentParameter(out, obj, objValue, method,
                                allowRtexpr, contextVar,
                                isParentSimpleTag);
      return;
    }

    if (objValue instanceof JspAttribute) {
      JspAttribute attr = (JspAttribute) objValue;

      if (attr.isStatic())
        objValue = attr.getStaticText();
      else {
        String str = "_jsp_str_" + _gen.uniqueId();
        out.println("String " + str + " = " + attr.generateValue() + ";");
        out.println(obj + "." + method.getName() + "(" + stringToValue(type, str) + ");");
        return;
      }
    }
    else if (objValue instanceof JspNode)
      throw error(L.l("jsp:attribute may not set this attribute."));
    
    String strValue = (String) objValue;
    
    String convValue = generateParameterValue(type,
                                              strValue,
                                              allowRtexpr,
                                              attrInfo,
                                              _parseState.isELIgnored());

    PropertyEditor editor;
    
    if (convValue != null)
      out.println(obj + "." + method.getName() + "(" + convValue + ");");
    else if ((editor = PropertyEditorManager.findEditor(type)) != null) {
      generateSetParameter(out, obj, strValue, method, editor.getClass());
    }
    else
      throw error(L.l("expected '<%= ... %>' at '{0}' for tag attribute setter '{1}'.  Tag attributes which can't be converted from strings must use a runtime attribute expression.",
                      strValue, method.getName() + "(" + type.getName() + ")"));
  }

  void generateSetParameter(JspJavaWriter out,
                            String obj, String value, Method method,
                            Class<?> editorClass)
    throws Exception
  {
    Class<?> type = method.getParameterTypes()[0];
    
    String name = "_jsp_editor" + _gen.uniqueId();
    out.print("java.beans.PropertyEditor " + name + " = new " + editorClass.getName() + "();");
    out.println(name + ".setAsText(\"" + escapeJavaString(value) + "\");");
    out.println(obj + "." + method.getName() + "((" +
            type.getName() + ") " + name + ".getValue());");
  }
  
  void generateFragmentParameter(JspJavaWriter out,
                                 Object obj, Object objValue, Method method,
                                 boolean allowRtexpr, String contextVar,
                                 boolean isParentSimpleTag)
    throws Exception
  {
    out.print(obj + "." + method.getName() + "(");
    if (objValue instanceof JspFragmentNode)
      generateFragment(out, (JspFragmentNode) objValue, contextVar,
                       isParentSimpleTag);
    else if (objValue instanceof String) {
      String string = (String) objValue;

      int index = _gen.addExpr(string);

      out.print("new com.caucho.jsp.ELExprFragment(pageContext, _caucho_expr_" + index + ")");
    }
    else {
      throw error(L.l("can't handle fragment '{0}' of type {1}",
                      objValue, objValue.getClass()));
    }
    out.println(");");
  }
  
  String generateFragmentParameter(String string, boolean allowRtexpr)
    throws Exception
  {
    int index = _gen.addExpr(string);

    return ("new com.caucho.jsp.ELExprFragment(pageContext, _caucho_expr_" + index + ")");
  }

  /**
   * Returns the containing segment.
   */
  public JspSegmentNode getSegment()
  {
    JspNode parent = getParent();

    if (parent != null)
      return parent.getSegment();
    else
      return null;
  }

  void generateFragment(JspJavaWriter out,
                        JspFragmentNode frag,
                        String contextVar,
                        boolean isParentSimpleTag)
    throws Exception
  {
    out.print(generateFragment(frag, contextVar));
  }

  void generateParentTag(JspJavaWriter out, TagInstance parent)
    throws IOException
  {
    String parentId = parent.getId();
    if (parentId == null || parentId.startsWith("top_")) {
      out.print("null");
    }
    else if (parent.isSimpleTag()) {
      out.print("(" + parentId + "_adapter != null ? ");
      out.print(parentId + "_adapter : ");
      out.print("(" + parentId + "_adapter = new javax.servlet.jsp.tagext.TagAdapter(" + parentId + ")))");
    }
    else
      out.print(parentId);
  }

  String generateRTValue(Class<?> type, Object value)
    throws Exception
  {
    if (value instanceof String)
      return generateParameterValue(type, (String) value,
                                    true, null, _parseState.isELIgnored());
    else {
      JspAttribute attr = (JspAttribute) value;
      
      return stringToValue(type, attr.generateValue());
    }
  }

  /**
   * Generates the code invoking a fragment to a string.
   */
  protected String invokeFragment(JspFragmentNode frag)
    throws Exception
  {
    return frag.generateValue();
  }

  /**
   * Generates the code for a fragment.
   */
  protected String generateFragment(JspFragmentNode frag, 
                                    String contextVar)
    throws Exception
  {
    int index = _gen.addFragment(frag);
    
    StringBuffer cb = new StringBuffer();

    if (frag.isStatic()) {
      String fragmentVar = frag.getFragmentName();

      cb.append(fragmentVar + " = com.caucho.jsp.StaticJspFragmentSupport.create(" + fragmentVar + ", " + contextVar + ", \"");

      cb.append(escapeJavaString(frag.getStaticText()));
      cb.append("\")");

      return cb.toString();
    }

    String fragmentVar = frag.getFragmentName();

    JspNode parentTag = getParentTagNode();
    
    // jsp/0800
    boolean isParentSimpleTag = (parentTag instanceof CustomSimpleTag); 

    if (! isParentSimpleTag) {
      cb.append(fragmentVar + " = createFragment(" + fragmentVar
                + ", " + index
                + ", _jsp_parentContext"
                + ", " + contextVar
                + ", ");
    }
    else {
      cb.append(fragmentVar + " = createFragment(null"
                + ", " + index
                + ", _jsp_parentContext"
                + ", " + contextVar
                + ", ");
    }

    if (parentTag == null)
      cb.append("null");
    else if (frag.hasCustomTag() && parentTag instanceof CustomSimpleTag)
      cb.append(parentTag.getCustomTagName() + "_adapter");
    else
      cb.append(parentTag.getCustomTagName());

    if (_gen instanceof JavaTagGenerator) {
      JavaTagGenerator tagGen = (JavaTagGenerator) _gen;
      
      if (tagGen.isStaticDoTag()) // jsp/1025
        cb.append(", _jspBody");
      else
        cb.append(", getJspBody()");
    }
    else
      cb.append(", null");

    cb.append(", _jsp_state");
    cb.append(", _jsp_pageManager");

    cb.append(")");

    return cb.toString();
  }

  /**
   * Generates the code for the value of a parent tag.
   */
  protected String generateParentTag(TagInstance parent)
    throws IOException
  {
    String parentId = parent.getId();
    if (parent.isTop()) {
      return "null";
    }
    else if (parent.isSimpleTag()) {
      CharBuffer cb = CharBuffer.allocate();
      
      cb.append("(" + parentId + "_adapter != null ? ");
      cb.append(parentId + "_adapter : ");
      cb.append("(" + parentId + "_adapter = new javax.servlet.jsp.tagext.TagAdapter(" + parentId + ")))");

      return cb.close();
    }
    else
      return parentId;
  }

  //
  // JSF functions
  //

  /**
   * Returns the variable containing the jsf component
   */
  public String getJsfVar()
  {
    if (_parent != null)
      return _parent.getJsfVar();
    else
      return null;
  }

  /**
   * Returns the variable containing the jsf body
   */
  public String getJsfBodyVar()
  {
    if (_parent != null)
      return _parent.getJsfBodyVar();
    else
      return null;
  }

  /**
   * True if the jsf-parent setting is required.
   */
  public boolean isJsfParentRequired()
  {
    return false;
  }

  //
  // value generation
  //

  /**
   * Generate include params.
   */
  void generateIncludeParams(JspJavaWriter out,
                             ArrayList params)
    throws Exception
  {
    boolean hasQuery = false;
    
    for (int i = 0; i < params.size(); i++) {
      JspParam param = (JspParam) params.get(i);
      String value = param.getValue();
        
      if (hasQuery)
        out.print("+ \"&\" + ");
        
      hasQuery = true;
      out.print("\"" + param.getName() + "=\"");

      String outValue = generateParameterValue(String.class, value);

      if (outValue.equals("null")) {
      }
      else if (outValue.startsWith("\""))
        out.print(" + (" + outValue + ")");
      else
        out.print(" + com.caucho.el.Expr.toString(" + outValue + ", null)");
    }
  }
  
  protected void generateIncludeUrl(JspJavaWriter out, String page,
                                    ArrayList<JspParam> params)
    throws Exception
  {
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        out.print("pageContext.encode(");
      }

      out.print("pageContext.encode(");
    }

    if (hasRuntimeAttribute(page)) {
      out.print(getRuntimeAttribute(page));
    }
    else {
      out.print(generateParameterValue(String.class, page));
    }

    if (params != null) {
      out.print(")");

      for (int i = 0; i < params.size(); i++) {
        if (i > 0)
          out.print(".append('&')");

        out.print(", ");

        generateIncludeParam(out, params.get(i));

        out.print(")");
      }

      out.print(".toString()");
    }
  }

  /**
   * Generate include params.
   */
  void generateIncludeParam(JspJavaWriter out,
                            JspParam param)
    throws Exception
  {
    String value = param.getValue();
        
    out.print("\"" + param.getName() + "=\"");

    String outValue = generateParameterValue(String.class, value);

    if (outValue.equals("null")) {
    }
    else if (outValue.startsWith("\""))
      out.print(" + (" + outValue + ")");
    else
      out.print(" + com.caucho.el.Expr.toString(" + outValue + ", null)");
  }

  String generateJstlValue(Class type, String value)
    throws Exception
  {
    return generateParameterValue(type, value, true, null, false);
  }

  String generateValue(Class type, String value)
    throws Exception
  {
    return generateParameterValue(type, value, true, null,
                                  _parseState.isELIgnored());
  }

  String generateParameterValue(Class<?> type, String value)
    throws Exception
  {
    return generateParameterValue(type, value, true, null,
                                  _parseState.isELIgnored());
  }

  String generateParameterValue(Class<?> type,
                                String value,
                                boolean rtexpr,
                                TagAttributeInfo attrInfo,
                                boolean isELIgnored)
    throws Exception
  {
    // jsp/1c2m
    if (isJstl())
      isELIgnored = false;
    
    boolean isEmpty = value == null || value.equals("");
    if (isEmpty)
      value = "0";

    try {
      String typeName = attrInfo != null ? attrInfo.getExpectedTypeName() : "";

      boolean isValueDeferred
        = (attrInfo != null && attrInfo.isDeferredValue()
           || typeName != null && ! "".equals(typeName));

      boolean isMethodDeferred
        = (attrInfo != null && attrInfo.isDeferredMethod());

      if (JspFragment.class.equals(type))
        return generateFragmentParameter(value, rtexpr);
      else if (type.equals(ValueExpression.class)) {
        int exprIndex;

        if (isEmpty)
          exprIndex = _gen.addValueExpr("", typeName);
        else
          exprIndex = _gen.addValueExpr(value, typeName);

        if (isValueDeferred
            && value.indexOf("#{") < 0
            && value.indexOf("${") >= 0) {
          throw error(L.l("ValueExpression '{0}' must use deferred syntax '#{...}'",
                          value));
        }
        else if (! isValueDeferred
                 && value.indexOf("#{") >= 0
                 && value.indexOf("${") < 0) {
          throw error(L.l("Deferred syntax '#{...}' is not allowed for '{0}'",
                          value));
        }


        if (value.indexOf("#{") < 0 && value.indexOf("${") < 0) {
          return ("_caucho_value_expr_" + exprIndex);
        }
        else {
          StringBuilder sb = new StringBuilder();

          sb.append("pageContext.createExpr(_caucho_value_expr_");
          sb.append(exprIndex);
          sb.append(", \"");
          sb.append(escapeJavaString(value));
          sb.append("\", ");
          if (null == typeName || "".equals(typeName)) {
            sb.append("java.lang.Object.class)");
          } else {
            sb.append(escapeJavaString(typeName));
            sb.append(".class)");
          }

          return sb.toString();
        }
      }
      else if (type.equals(MethodExpression.class)) {
        int exprIndex;

        String sig = attrInfo != null ? attrInfo.getMethodSignature() : "java.lang.String misc()";

        if (isEmpty)
          exprIndex = _gen.addMethodExpr("", sig);
        else
          exprIndex = _gen.addMethodExpr(value, sig);

        if (value.indexOf("${") >= 0)
          throw error(L.l("MethodExpression '{0}' must use deferred syntax '$#{...}'",
                          value));
      
        return ("_caucho_method_expr_" + exprIndex);
      }
      else if (! isValueDeferred
               && ! _gen.getParseState().isDeferredSyntaxAllowedAsLiteral()
               && value.indexOf("#{") >= 0
               && value.indexOf("${") < 0
               && rtexpr
               && isJsp21()) {
        // jsp/10h2, jsp/1cn0, jsp/10h3
        throw error(L.l("deferred expression '{0}' is not allowed here",
                        value));
      }
      else if (com.caucho.el.Expr.class.equals(type)) {
        int exprIndex;

        if (isEmpty)
          exprIndex = _gen.addExpr("");
        else
          exprIndex = _gen.addExpr(value);
      
        return ("_caucho_expr_" + exprIndex);
      }
      else if (com.caucho.xpath.Expr.class.equals(type)) {
        int exprIndex;

        com.caucho.xpath.Expr expr;
        if (isEmpty)
          expr = XPath.parseExpr("");
        else
          expr = XPath.parseExpr(value, getNamespaceContext());

        return _gen.addXPathExpr(expr);
      }
      else if (rtexpr && hasRuntimeAttribute(value)) {
        return getRuntimeAttribute(value);
      }
      else if (rtexpr && hasELAttribute(value, isELIgnored)) {
        // jsp/0138, jsp/18s0, jsp/1ce5, jsp/1e0a
        return generateELValue(type, value);
      }
      else if (! rtexpr && hasELAttribute(value, isELIgnored)) {
        // JSP.2.3.6 says this is an error
        // jsp/184v vs jsp/18cr vs jsp/18f5 vs jsp/18f7 (tck)
        // #2112

        if (String.class.equals(type) && _gen.isELIgnore())
          return '"' + escapeJavaString(value) + '"';
        else
          throw error(L.l("EL expression '{0}' is only allowed for attributes with rtexprvalue='true'.",
                          value));
      }
      else if (rtexpr && hasDeferredAttribute(value, false)) {
        // jsp/1c2m, jsp/1ce8
        if (type.equals(String.class))
          return '"' + value + '"';
        else
          return generateELValue(type, value);
      }
      else if (! rtexpr
               && hasDeferredAttribute(value, isELIgnored)
               && ! _gen.getParseState().isDeferredSyntaxAllowedAsLiteral()) {
        throw error(L.l("Deferred syntax '{0}' is not allowed as a literal.",
                        value));
      }
      else if (type.equals(boolean.class))
        return String.valueOf(Boolean.valueOf(isEmpty ? "false" : value));
      else if (type.equals(Boolean.class)) {
        if (isEmpty)
          return "java.lang.Boolean.FALSE";
        else
          return "new java.lang.Boolean(" + Boolean.valueOf(value) + ")";
      }
      else if (type.equals(byte.class))
        return "(byte) " + Byte.valueOf(value);
      else if (type.equals(Byte.class))
        return "new java.lang.Byte((byte) " + Byte.valueOf(value) + ")";
      else if (type.equals(char.class)) {
        if (isEmpty)
          return "'\\0'";
        else
          return "'" + value.charAt(0) + "'";
      }
      else if (type.equals(Character.class)) {
        if (isEmpty)
          return "new java.lang.Character('\\0')";
        else
          return ("new Character('" + value.charAt(0) + "')");
      }
      else if (type.equals(short.class))
        return ("(short) " + Short.valueOf(value));
      else if (type.equals(Short.class))
        return ("new java.lang.Short((short) " + Short.valueOf(value) + ")");
      else if (type.equals(int.class))
        return String.valueOf(Integer.valueOf(value));
      else if (type.equals(Integer.class))
        return ("new java.lang.Integer(" + Integer.valueOf(value) + ")");
      else if (type.equals(long.class))
        return String.valueOf(Long.valueOf(value));
      else if (type.equals(Long.class))
        return ("new java.lang.Long(" + Long.valueOf(value) + ")");
      else if (type.equals(float.class))
        return ("(float) " + Float.valueOf(value));
      else if (type.equals(Float.class))
        return ("new java.lang.Float((float) " + Float.valueOf(value) + ")");
      else if (type.equals(double.class))
        return String.valueOf(Double.valueOf(value));
      else if (type.equals(Double.class)) {
        double v = Double.valueOf(value);

        if (Double.isNaN(v))
          return ("new java.lang.Double(Double.NaN)");
        else
          return ("new java.lang.Double(" + v + ")");
      }
      else if (! type.equals(String.class)
               && ! type.equals(Object.class)) {
        return null;
      }
      else if (! isEmpty) {
        return '"' + escapeJavaString(value) + '"';
      }
      else
        return "\"\"";
    } catch (NumberFormatException e) {
      throw error(L.l("parameter format error: {0}", e.getMessage()), e);
    }
  }
    
  protected String generateELValue(Class type, String value)
    throws Exception
  {
    if (type.equals(com.caucho.el.Expr.class)) {
      int exprIndex;

      exprIndex = _gen.addExpr(value);
      
      return ("_caucho_expr_" + exprIndex);
    }
    else if (type.equals(ValueExpression.class)) {
      int exprIndex;

      exprIndex = _gen.addValueExpr(value, "");
      
      return ("_caucho_value_expr_" + exprIndex);
    }
    else if (type.equals(Object.class)
             && value.contains("#{")
             && CustomTag.class.equals(getClass())) {
      int exprIndex;

      exprIndex = _gen.addValueExpr(value, "");

      return ("_caucho_value_expr_" + exprIndex);
    }
    else if (type.equals(com.caucho.xpath.Expr.class)) {
      com.caucho.xpath.Expr expr;

      expr = XPath.parseExpr(value, getNamespaceContext());
      
      return _gen.addXPathExpr(expr);
    }

    Expr expr = _gen.genExpr(value);

    if (expr.isConstant()) {
      try {
        if (expr.evalObject(null) != null) {
        }
        else if (Character.class.isAssignableFrom(type)) {
          // jsp/18s0
          return "new Character((char) 0)";
        }
        else if (Boolean.class.isAssignableFrom(type)) {
          // jsp/18s1
          return "Boolean.FALSE";
        }
        else if (String.class.isAssignableFrom(type)) {
          // jsp/18s2
          return "\"\"";
        }
        else if (BigInteger.class.isAssignableFrom(type)) {
          return "java.math.BigInteger.ZERO";
        }
        else if (BigDecimal.class.isAssignableFrom(type)) {
          return "java.math.BigDecimal.ZERO";
        }
        else if (Number.class.isAssignableFrom(type)) {
          // jsp/18s6
          return "new " + type.getName() + "((byte) 0)";
        }
        else if (Object.class.isAssignableFrom(type))
          return "null";

        if (boolean.class.equals(type))
          return expr.evalBoolean(null) ? "true" : "false";
        else if (Boolean.class.equals(type))
          return expr.evalBoolean(null) ? "java.lang.Boolean.TRUE" : "java.lang.Boolean.FALSE";
        else if (byte.class.equals(type))
          return "(byte) " + expr.evalLong(null);
        else if (Byte.class.equals(type))
          return "new java.lang.Byte((byte) " + expr.evalLong(null) + "L)";
        else if (short.class.equals(type))
          return "(short) " + expr.evalLong(null);
        else if (Short.class.equals(type))
          return "new java.lang.Short((short) " + expr.evalLong(null) + "L)";
        else if (int.class.equals(type))
          return "(int) " + expr.evalLong(null);
        else if (Integer.class.equals(type))
          return "new java.lang.Integer((int) " + expr.evalLong(null) + "L)";
        else if (long.class.equals(type))
          return "" + expr.evalLong(null) + "L";
        else if (Long.class.equals(type))
          return "new java.lang.Long(" + expr.evalLong(null) + "L)";
        else if (float.class.equals(type))
          return "(float) " + expr.evalDouble(null);
        else if (Float.class.equals(type))
          return "new java.lang.Float((float) " + expr.evalDouble(null) + ")";
        else if (double.class.equals(type)) {
          double v = expr.evalDouble(null);

          if (Double.isNaN(v))
            return "Double.NaN";
          else
            return "" + v;
        }
        else if (Double.class.equals(type)) {
          double v = expr.evalDouble(null);

          if (Double.isNaN(v))
            return "new Double(Double.NaN)";
          else
            return "new java.lang.Double(" + v + ")";
        }
        else if (char.class.equals(type))
          return "((char) " + (int) expr.evalCharacter(null) + ")";
        else if (Character.class.equals(type)) {
          // jsp/18s0
          return "new Character((char) " + (int) expr.evalCharacter(null) + ")";
        }
        else if (String.class.equals(type))
          return "\"" + escapeJavaString(expr.evalString(null)) + "\"";
        else if (BigInteger.class.equals(type)) {
          String v = expr.evalBigInteger(null).toString();

          // 18s3
          if (v.equals("") || v.equals("0"))
            return "java.math.BigInteger.ZERO";
          else
            return "new java.math.BigInteger(\"" + v + "\")";
        }
        else if (BigDecimal.class.equals(type)) {
          String v = expr.evalBigDecimal(null).toString();

          // 18s4
          if (v.equals("") || v.equals("0.0"))
            return "java.math.BigDecimal.ZERO";
          else
            return "new java.math.BigDecimal(\"" + v + "\")";
        }
        else if (Object.class.equals(type)) {
          Object cValue = expr.evalObject(null);

          String result = generateObject(cValue);

          if (result != null)
            return result;
        }
        else {
          Object cValue = expr.evalObject(null);

          // jsp/184t
          if ("".equals(cValue))
            return "null";
        }
      } catch (Throwable e) {
        // jsp/18co
        // exceptions are caught at runtime
        log.log(Level.FINER, e.toString(), e);

        log.fine(e.getMessage());
      }
    }
    
    int exprIndex = _gen.addExpr(value);
    String var = "_caucho_expr_" + exprIndex;

    if (boolean.class.equals(type))
      return var + ".evalBoolean(_jsp_env)";
    else if (Boolean.class.equals(type))
      return var + ".evalBoolean(_jsp_env) ? java.lang.Boolean.TRUE : java.lang.Boolean.FALSE";
    else if (byte.class.equals(type))
      return "(byte) " + var + ".evalLong(_jsp_env)";
    else if (Byte.class.equals(type))
      return "new java.lang.Byte((byte) " + var + ".evalLong(_jsp_env))";
    else if (short.class.equals(type))
      return "(short) " + var + ".evalLong(_jsp_env)";
    else if (Short.class.equals(type))
      return "new java.lang.Short((short) " + var + ".evalLong(_jsp_env))";
    else if (int.class.equals(type))
      return "(int) " + var + ".evalLong(_jsp_env)";
    else if (Integer.class.equals(type))
      return "new java.lang.Integer((int) " + var + ".evalLong(_jsp_env))";
    else if (long.class.equals(type))
      return var + ".evalLong(_jsp_env)";
    else if (Long.class.equals(type))
      return "new java.lang.Long(" + var + ".evalLong(_jsp_env))";
    else if (float.class.equals(type))
      return "(float) " + var + ".evalDouble(_jsp_env)";
    else if (Float.class.equals(type))
      return "new java.lang.Float((float) " + var + ".evalDouble(_jsp_env))";
    else if (double.class.equals(type))
      return var + ".evalDouble(_jsp_env)";
    else if (Double.class.equals(type))
      return "new java.lang.Double(" + var + ".evalDouble(_jsp_env))";
    else if (java.math.BigDecimal.class.equals(type))
      return "" + var + ".evalBigDecimal(_jsp_env)";
    else if (java.math.BigInteger.class.equals(type))
      return "" + var + ".evalBigInteger(_jsp_env)";
    else if (char.class.equals(type))
      return var + ".evalCharacter(_jsp_env)";
    else if (Character.class.equals(type))
      return "new Character(" + var + ".evalCharacter(_jsp_env))";
    else if (String.class.equals(type))
      return var + ".evalString(_jsp_env)";
    else if (BigInteger.class.equals(type))
      return var + ".evalBigInteger(_jsp_env)";
    else if (BigDecimal.class.equals(type))
      return var + ".evalBigDecimal(_jsp_env)";
    else if (Object.class.equals(type))
      return var + ".evalObject(_jsp_env)";
    else {
      return "(" + classToString(type) + ") " + var + ".evalObject(_jsp_env)";
    }
  }

  public void convertParameterValue(JspJavaWriter out, String type, String value)
    throws IOException
  {
    if (type.equals("boolean"))
      out.print("java.lang.Boolean.TRUE.equals(" + value + ")");
    else if (type.equals("byte"))
      out.print("java.lang.Byte.valueOf(" + value + ")");
    else if (type.equals("char"))
      out.print("java.lang.Character.valueOf(" + value + ")");
    else if (type.equals("short"))
      out.print("java.lang.Short.valueOf(" + value + ")");
    else if (type.equals("int"))
      out.print("((java.lang.Integer) " + value + ").intValue()");
    else if (type.equals("long"))
      out.print("((java.lang.Long) " + value + ").longValue()");
    else if (type.equals("float"))
      out.print("((java.lang.Float) " + value + ").floatValue()");
    else if (type.equals("double"))
      out.print("((java.lang.Double) " + value + ").doubleValue()");
    else
      out.print("(" + type + ")" + value);
  }

  protected String classToString(Class cl)
  {
    if (cl.isArray())
      return classToString(cl.getComponentType()) + "[]";
    else
      return cl.getName();
  }

  /**
   * Returns true if the value is a runtime attribute.
   */
  public boolean hasRuntimeAttribute(String value)
    throws JspParseException
  {
    if (_parseState.isScriptingInvalid()) {
      // && value.indexOf("<%=") >= 0) {
      return false;
      /*
      throw error(L.l("Runtime expressions are forbidden here.  Scripting has been disabled either:\n1) disabled by the web.xml scripting-invalid\n2) disabled in a tag's descriptor\n3) forbidden in <jsp:attribute> or <jsp:body> tags."));
      */
    }
        
    if (value.startsWith("<%=") && value.endsWith("%>"))
      return true;
    else if (value.startsWith("%=") && value.endsWith("%"))
      return true;
    else if (value.indexOf("<%=") >= 0 &&
             value.indexOf("<%=") < value.indexOf("%>"))
      throw error(L.l("interpolated runtime values are forbidden by the JSP spec at '{0}'",
                      value));
    else
      return false;
  }

  /**
   * Returns true if the string has scripting.
   */
  public boolean hasScripting(String value)
  {
    try {
      return value != null && hasRuntimeAttribute(value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns true if the string has scripting.
   */
  public boolean hasScripting(JspAttribute value)
  {
    return value != null && value.hasScripting();
  }
  
  /**
   * Returns true if the value is a runtime attribute.
   */
  public boolean hasELAttribute(String value)
  {
    return ! _parseState.isELIgnored() && value.indexOf("${") >= 0;
  }
  
  /**
   * Returns true if the value is a runtime attribute.
   */
  public boolean hasDeferredAttribute(String value)
  {
    if (value.indexOf("#{") < 0)
      return false;
    else if (isPre21Taglib())
      return false;
    else
      return ! _parseState.isELIgnored();
  }
  
  /**
   * Returns true if the value is a runtime attribute.
   */
  public boolean hasELAttribute(String value, boolean isELIgnored)
  {
    return ! isELIgnored && value.indexOf("${") >= 0;
  }
  
  /**
   * Returns true if the value is a runtime attribute.
   */
  public boolean hasDeferredAttribute(String value, boolean isELIgnored)
  {
    if (isELIgnored)
      return false;
    else if (value.indexOf("#{") < 0)
      return false;
    else if (isPre21Taglib())
      return false;
    else
      return true;
  }

  /**
   * Returns the runtime attribute of the value.
   */
  public String getRuntimeAttribute(String value)
    throws Exception
  {
    if (value.startsWith("<%=") && value.endsWith("%>"))
      return value.substring(3, value.length() - 2);
    else if (value.startsWith("%=") && value.endsWith("%"))
      return value.substring(2, value.length() - 1);
    else
      return value;
  }

  /**
   * Converts a string-valued expression to the given type.
   */
  String stringToValue(Class type, String obj)
  {
    if (boolean.class.equals(type))
      return "com.caucho.jsp.PageContextImpl.toBoolean(" + obj + ")";
    else if (Boolean.class.equals(type))
      return "java.lang.Boolean.valueOf(" + obj + ")";
    else if (byte.class.equals(type))
      return "java.lang.Byte.parseByte(" + obj + ")";
    else if (Byte.class.equals(type))
      return "java.lang.Byte.valueOf(" + obj + ")";
    else if (char.class.equals(type))
      return obj + ".charAt(0)";
    else if (Character.class.equals(type))
      return "new java.lang.Character(" + obj + ".charAt(0))";
    else if (short.class.equals(type))
      return "java.lang.Short.parseShort(" + obj + ")";
    else if (Short.class.equals(type))
      return "java.lang.Short.valueOf(" + obj + ")";
    else if (int.class.equals(type))
      return "java.lang.Integer.parseInt(" + obj + ")";
    else if (Integer.class.equals(type))
      return "java.lang.Integer.valueOf(" + obj + ")";
    else if (long.class.equals(type))
      return "java.lang.Long.parseLong(" + obj + ")";
    else if (Long.class.equals(type))
      return "java.lang.Long.valueOf(" + obj + ")";
    else if (float.class.equals(type))
      return "java.lang.Float.parseFloat(" + obj + ")";
    else if (Float.class.equals(type))
      return "java.lang.Float.valueOf(" + obj + ")";
    else if (double.class.equals(type))
      return "java.lang.Double.parseDouble(" + obj + ")";
    else if (Double.class.equals(type))
      return "java.lang.Double.valueOf(" + obj + ")";
    else if (type.isAssignableFrom(String.class))
      return obj;
    else
      return null;
  }

  /**
   * Converts a string-valued expression to the given type.
   */
  Object staticStringToValue(Class type, String obj)
  {
    if (boolean.class.equals(type) || Boolean.class.equals(type))
      return Boolean.valueOf(obj);
    else if (byte.class.equals(type) || Byte.class.equals(type))
      return Byte.parseByte(obj);
    else if (char.class.equals(type) || Character.class.equals(type))
      return obj.charAt(0);
    else if (short.class.equals(type) || Short.class.equals(type))
      return Short.parseShort(obj);
    else if (int.class.equals(type) || Integer.class.equals(type))
      return Integer.parseInt(obj);
    else if (long.class.equals(type) || Long.class.equals(type))
      return Long.parseLong(obj);
    else if (float.class.equals(type) || Float.class.equals(type))
      return Float.parseFloat(obj);
    else if (double.class.equals(type) || Double.class.equals(type))
      return Double.parseDouble(obj);
    else if (type.isAssignableFrom(String.class))
      return obj;
    else
      return obj; // XXX:
  }
  
  protected String generateObject(Object obj)
  {
    if (obj instanceof String)
      return "\"" + escapeJavaString((String) obj) + "\"";
    else if (obj instanceof Long)
      return "new java.lang.Long(" + obj + "L)";
    else if (obj instanceof Integer)
      return "new java.lang.Integer((int) " + obj + "L)";
    else if (obj instanceof Double) {
      double v = (Double) obj;

      if (Double.isNaN(v))
        return "new java.lang.Double(Double.NaN)";
      else
        return "new java.lang.Double(" + v + ")";
    }
    else if (obj instanceof Boolean)
      return ((Boolean) obj).booleanValue() ? "java.lang.Boolean.TRUE" : "java.lang.Boolean.FALSE";
    else
      return null;
  }
  
  public static String toELObject(String expr, Class type)
  {
    if (boolean.class.equals(type))
      return "((" + expr + ") ? Boolean.TRUE : Boolean.FALSE)";
    else if (byte.class.equals(type))
      return "new Long(" + expr + ")";
    else if (short.class.equals(type))
      return "new Long(" + expr + ")";
    else if (int.class.equals(type))
      return "new Long(" + expr + ")";
    else if (long.class.equals(type))
      return "new Long(" + expr + ")";
    else if (float.class.equals(type))
      return "new Double(" + expr + ")";
    else if (double.class.equals(type))
      return "new Double(" + expr + ")";
    else if (char.class.equals(type))
      return "String.valueOf(" + expr + ")";
    else
      return expr;
  }

  /**
   * Escapes a java string.
   */
  public static String escapeJavaString(String s)
  {
    if (s == null)
      return "";

    CharBuffer cb = CharBuffer.allocate();
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '\\')
        cb.append("\\\\");
      else if (s.charAt(i) == '"')
        cb.append("\\\"");
      else if (s.charAt(i) == '\n')
        cb.append("\\n");
      else if (s.charAt(i) == '\r')
        cb.append("\\r");
      else
        cb.append(s.charAt(i));
    }

    return cb.close();
  }

  protected Class loadClass(String type)
    throws JspParseException
  {
    if (type == null)
      return null;
    else {
      try {
        return _gen.getBeanClass(type);
      } catch (Exception e) {
        throw new JspParseException(e);
      }
    }
  }

  /**
   * Creates a parse exception with the proper line information.
   */
  protected JspParseException error(String msg)
  {
    return error(msg, null);
  }

  /**
   * Creates a parse exception with the proper line information.
   */
  protected JspParseException error(String msg, Throwable e)
  {
    if (_filename != null) {
      String lines = _gen.getSourceLines(_sourcePath, _startLine);
      
      return new JspLineParseException(_filename + ":" + _startLine + ": " + msg + lines, e);
    }
    else
      return new JspParseException(msg, e);
  }

  /**
   * Creates a parse exception with the proper line information.
   */
  protected JspParseException error(Throwable e)
  {
    if (e instanceof JspLineParseException)
      return (JspParseException) e;
    else if (_filename == null || e instanceof LineCompileException)
      return new JspLineParseException(e);

    String msg;
    
    if (e instanceof CompileException)
      msg = e.getMessage();
    else
      msg = String.valueOf(e);
    
    String lines = _gen.getSourceLines(_sourcePath, _startLine);
      
    return new JspLineParseException(_filename + ":" + _startLine + ": " + msg + lines, e);
  }

  /**
   * Returns a printable version of the node.
   */
  public String toString()
  {
    if (_name == null)
      return "<" + getClass().getName() + ">";
    else
      return "<" + _name.getName() + ">";
  }
}
