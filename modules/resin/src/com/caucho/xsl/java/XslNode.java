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

package com.caucho.xsl.java;

import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.util.LineCompileException;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;
import com.caucho.xpath.Expr;
import com.caucho.xpath.NamespaceContext;
import com.caucho.xpath.XPath;
import com.caucho.xpath.expr.NumericExpr;
import com.caucho.xpath.pattern.*;
import com.caucho.xsl.JavaGenerator;
import com.caucho.xsl.XslParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents any XSL node from the stylesheet.
 */
public abstract class XslNode {
  static final L10N L = new L10N(XslNode.class);
  private static final Logger log
    = Logger.getLogger(XslNode.class.getName());

  protected String _systemId;
  protected String _filename;
  protected int _startLine;
  protected int _endLine;
  
  protected JavaGenerator _gen;

  protected QName _name;
  protected XslNode _parent;

  protected ArrayList<XslNode> _children;
  protected NamespaceContext _matchNamespace;
  protected NamespaceContext _outputNamespace;

  private int _varCount;

  protected XslNode()
  {
  }

  /**
   * Sets the Java generator.
   */
  public void setGenerator(JavaGenerator gen)
  {
    _gen = gen;
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
      return getClass().getName();
  }
  
  /**
   * Returns the parent node.
   */
  public XslNode getParent()
  {
    return _parent;
  }

  /**
   * Sets the parent node
   */
  public void setParent(XslNode parent)
  {
    _parent = parent;

    if (parent != null) {
      _matchNamespace = parent.getMatchNamespace();
      _outputNamespace = parent.getOutputNamespace();
    }
  }

  /**
   * Add variable.
   */
  public void addVariableCount()
  {
    if (_parent != null)
      _parent._varCount++;
  }

  /**
   * Sets the start location of the node.
   */
  public void setStartLocation(String systemId, String filename, int line)
  {
    _systemId = systemId;
    _filename = filename;
    _startLine = line;
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
   * Gets the system id of the node
   */
  public String getSystemId()
  {
    return _systemId;
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
   * Gets the ending line number
   */
  public int getEndLine()
  {
    return _endLine;
  }

  /**
   * Returns the base URI.
   */
  public String getBaseURI()
  {
    return _filename;
  }

  /**
   * Returns the namespaces.
   */
  public NamespaceContext getMatchNamespace()
  {
    return _matchNamespace;
  }

  /**
   * Returns the namespaces.
   */
  public NamespaceContext getOutputNamespace()
  {
    return _outputNamespace;
  }

  /**
   * Returns the matching node in the namespace.
   */
  public String getNamespace(String prefix)
  {
    return NamespaceContext.find(getOutputNamespace(), prefix);
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().startsWith("xmlns")) {
      addNamespaceAttribute(name, value);
      return;
    }
    
    if (name.getName().startsWith("xml"))
      return;
    
    throw error(L.l("attribute `{0}' is not allowed in <{1}>.",
                    name.getName(), getTagName()));
  }

  /**
   * Adds an attribute.
   */
  protected void addNamespaceAttribute(QName name, String url)
    throws XslParseException
  {
    // Note: according to the spec, the default namespace is not used

    /*
    if (url.equals(JavaGenerator.XSLNS) || url.equals(JavaGenerator.XTPNS))
      return;

    if (url.startsWith("quote:"))
      url = url.substring(6);
    */
    
    String localName = name.getLocalName();

    _outputNamespace = new NamespaceContext(_outputNamespace, localName, url);

    if (! localName.equals("xmlns")) {
      // xsl/04w3
      _matchNamespace = new NamespaceContext(_matchNamespace, localName, url);
    }
  }

  /**
   * Called after all the attributes from the tag.
   */
  public void endAttributes()
    throws XslParseException
  {
  }

  /**
   * Adds text.
   */
  public void addText(String text)
    throws XslParseException
  {
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);

      if (! XmlChar.isWhitespace(ch))
        throw error(L.l("Text is not allowed in <{0}> at `{1}'.",
                        _name.getName(), text));
    }
  }

  /**
   * Adds a child node.
   */
  public void addChild(XslNode node)
    throws XslParseException
  {
    if (node == null)
      return;
    
    if (_children == null)
      _children = new ArrayList<XslNode>();

    _children.add(node);
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
  public ArrayList<XslNode> getChildren()
  {
    return _children;
  }

  /**
   * Returns true if there are any children.
   */
  public boolean hasChildren()
  {
    return _children != null && _children.size() > 0;
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  abstract public void generate(JavaWriter out)
    throws Exception;

  /**
   * Generates the code for the children.
   *
   * @param out the output writer for the generated java.
   */
  public void generateChildren(JavaWriter out)
    throws Exception
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      XslNode child = _children.get(i);

      out.setLocation(child.getFilename(), child.getStartLine());
      
      child.generate(out);
    }

    popScope(out);
  }

  /**
   * Generates the prelude code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generateDeclaration(JavaWriter out)
    throws Exception
  {
    generateDeclarationChildren(out);
  }

  /**
   * Generates the declaration code for the children.
   *
   * @param out the output writer for the generated java.
   */
  public void generateDeclarationChildren(JavaWriter out)
    throws Exception
  {
    if (_children == null)
      return;

    for (int i = 0; i < _children.size(); i++) {
      XslNode child = _children.get(i);

      child.generateDeclaration(out);
    }
  }

  /**
   * Prints an attribute value.
   */
  protected void printAttributeValue(JavaWriter out, String name, String value)
    throws Exception
  {
    out.print("out.attribute(");
    out.print(name == null ? "null" : ("\"" + name + "\""));
    out.print(", ");
    if (value == null)
      out.print("null");
    else {
      out.print("\"");
      out.printJavaString(value);
      out.print("\"");
    }
    out.println(");");
  }

  /**
   * Prints an attribute value.
   */
  protected void printAttributeValue(JavaWriter out, String value)
    throws Exception
  {
    if (value == null) {
      out.print("null");
      return;
    }

    if (value.indexOf("{") < 0) {
      out.print("\"");
      out.printJavaString(value);
      out.print("\"");
    }
    else {
      generateString(out, value);
    }
  }

  /**
   * Produces code to generate an attribute value template.  The same
   * code is used to produce a string ('a{b}c' -> "a" + b + "c") or a series of
   * print statements (',').
   *
   * @param string the source template
   * @param mode separator: either '+' or ','
   * @param elt the containing element.  Needed for namespaces.
   */
  void generateString(JavaWriter out, String string)
    throws Exception
  {
    int i = 0;
    boolean first = true;
    int length = string.length();
    CharBuffer cb = CharBuffer.allocate();

    for (; i < length; i++) {
      char ch = string.charAt(i);

      if (ch == '\n') {
        cb.append("\\n");
      }
      else if (ch == '"') {
        cb.append("\\\"");
      }
      else if (ch == '{' && i + 1 < length) {
        // {{ is treated as a single {
        if (string.charAt(i + 1) == '{') {
          cb.append('{');
          i++;
        }
        // the value is computed from an XPath expr
        else {
          // print the gathered text if any
          if (cb.length() > 0) {
            out.print("out.print(\"");
            out.printJavaString(cb.toString());
            out.println("\");");
          }

          // scan the contents of '{' ... '}'
          cb.clear();
          for (i++; i < length && string.charAt(i) != '}'; i++)
            cb.append(string.charAt(i));

          printStringExpr(out, cb.toString());

          cb.clear();
          first = false;
        }
      }
      // }} is treated as a single }
      else if (ch == '}' && i + 1 < length) {
        if (string.charAt(i + 1) == '}') {
          cb.append('}');
          i++;
        }
        else
          cb.append('}');
      }
      // <#= interpolates
      else if (i + 2 < length && ch == '<' && 
               string.charAt(i + 1) == '#' &&
               string.charAt(i + 2) == '=') {
        // print the gathered text if any
        if (cb.length() > 0) {
          out.print("out.print(\"");
          out.printJavaString(cb.toString());
          out.println("\");");
        }

        // scan the contents of '{' ... '}'
        cb.clear();
        for (i += 3;
             i + 1 < length && string.charAt(i) != '#' &&
               string.charAt(i + 1) != '>';
             i++)
          cb.append(string.charAt(i));

        i++;

        // and add the results
        out.println("out.print(" + cb + ");");

        cb.clear();
        first = false;
      }
      else
        cb.append((char) ch);
    }

    // add any trailing text
    if (cb.length() > 0)
      out.println("out.print(\"" + cb + "\");");
  }

  /**
   * Produces code to generate an attribute value template.  The same
   * code is used to produce a string ('a{b}c' -> "a" + b + "c") or a series of
   * print statements (',').
   *
   * @param string the source template
   * @param mode separator: either '+' or ','
   * @param elt the containing element.  Needed for namespaces.
   */
  void generateString(JavaWriter out, String string, int mode)
    throws Exception
  {
    CharBuffer cb = new CharBuffer();
    int i = 0;
    boolean first = true;
    int length = string.length();

    for (; i < length; i++) {
      char ch = string.charAt(i);

      if (ch == '\n') {
        cb.append("\\n");
      }
      else if (ch == '"') {
        cb.append("\\\"");
      }
      else if (ch == '{' && i + 1 < length) {
        // {{ is treated as a single {
        if (string.charAt(i + 1) == '{') {
          cb.append('{');
          i++;
        }
        // the value is computed from an XPath expr
        else {
          // print the gathered text if any
          if (mode == ',') {
            if (cb.length() > 0)
              out.println("out.print(\"" + cb.toString() + "\");");
          }
          else {
            if (! first)
              out.print((char) mode);

            if (cb.length() > 0) {
              out.print("\"");
              out.print(cb.toString());
              out.print("\"");
              out.print((char) mode);
            }
          }

          // scan the contents of '{' ... '}'
          cb.clear();
          for (i++; i < length && string.charAt(i) != '}'; i++)
            cb.append(string.charAt(i));

          // and add the results
          if (mode == ',')
            printStringExpr(out, cb.toString());
          else
            stringExpr(out, cb.toString());
          cb.clear();
          first = false;
        }
      }
      // }} is treated as a single }
      else if (ch == '}' && i + 1 < length) {
        if (string.charAt(i + 1) == '}') {
          cb.append('}');
          i++;
        }
        else
          cb.append('}');
      }
      // <#= interpolates
      else if (i + 2 < length && ch == '<' && 
               string.charAt(i + 1) == '#' &&
               string.charAt(i + 2) == '=') {
        // print the gathered text if any
        if (mode == ',') {
          if (cb.length() > 0)
            out.println("out.print(\"" + cb.toString() + "\");");
        }
        else {
          if (! first)
            out.print((char) mode);

          if (cb.length() > 0) {
            out.print("\"");
            out.print(cb.toString());
            out.print("\"");
            out.print((char) mode);
          }
        }

        // scan the contents of '{' ... '}'
        cb.clear();
        for (i += 3;
             i + 1 < length && string.charAt(i) != '#' &&
               string.charAt(i + 1) != '>';
             i++)
          cb.append(string.charAt(i));

        i++;

        // and add the results
        if (mode == ',')
          out.println("out.print(" + cb + ");");
        else {
          out.print("(" + cb + ")");
        }
        cb.clear();
        first = false;
      }
      else
        cb.append((char) ch);
    }

    // add any trailing text
    if (cb.length() > 0) {
      if (mode == ',') 
        out.println("out.print(\"" + cb + "\");");
      else {
        if (! first)
          out.print((char) mode);

        out.print("\"" + cb + "\"");
      }
    } else if (first && mode == '+')
      out.print("\"\"");
  }

  /**
   * Prints a value-of expression
   */
  protected void printStringExpr(JavaWriter out, String exprString)
    throws Exception
  {
    if (exprString == null)
      return;
    
    int length = exprString.length();
    
    if (length == 0)
      return;

    AbstractPattern select = null;
    try {
      select = parseSelect(exprString);
    } catch (Exception e) {
      // this is expected in case where the expr is not a select expression
    }
    
    if (exprString.equals(".")) {
      out.println("out.valueOf(node);");
      return;
    }
    else if (exprString.charAt(0) == '@') {
      boolean isSimple = true;
      
      for (int i = 1; i < length; i++) {
        char ch = exprString.charAt(i);
        if (! XmlChar.isNameChar(ch) || ch == ':')
          isSimple = false;
      }

      if (isSimple) {
        out.println("if (node instanceof Element)");
        out.print("  out.print(((Element) node).getAttribute(\"");
        out.print(exprString.substring(1));
        out.println("\"));");
        return;
      }
    }
    else if (allowJavaSelect(select)) {
      int oldSelectDepth = _gen.getSelectDepth();

      String loop = "_xsl_loop" + _gen.generateId();
      _gen.setSelectLoopDepth(0);
      
      String ptr = printSelectBegin(out, select, true, loop);

      out.println("out.valueOf(" + ptr + ");");
      out.println("break " + loop + ";");

      int selectDepth = _gen.getSelectDepth();
      for (; oldSelectDepth < selectDepth; selectDepth--) {
        out.popDepth();
        out.println("}");
      }
      _gen.setSelectDepth(oldSelectDepth);

      return;
    }

    out.println("out.valueOf(_exprs[" + addExpr(exprString) +
                "].evalObject(node, " + _gen.getEnv() + "));");
  }

  protected void stringExpr(JavaWriter out, String exprString)
    throws Exception, XslParseException
  {
    out.print("_exprs[" + _gen.addExpr(parseExpr(exprString)) +
          "].evalString(node, " + getEnv() + ")");
  }

  protected void pushCall(JavaWriter out)
    throws IOException
  {
    out.println("{");
    out.pushDepth();

    int callDepth = _gen.pushCallDepth();
    
    out.println("Env _xsl_arg" + callDepth + " = XPath.createCall(env);");
  }

  protected void popCall(JavaWriter out)
    throws IOException
  {
    int callDepth = _gen.popCallDepth();
    out.println("_xsl_arg" + callDepth + ".free();");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Prints iterator code to start a select.
   */
  protected String printSelectBegin(JavaWriter out,
                                    AbstractPattern select,
                                    boolean isForEach, String loopVar)
    throws IOException, XslParseException
  {
    if (select == null)
      throw new NullPointerException();
    
    if (select instanceof FromContext
        && ((FromContext) select).getCount() == 0)
      return "node";

    else if (select instanceof FromRoot)
      return "ownerDocument(node)";

    boolean useXPath = allowJavaSelect(select);
    
    String name = "node";

    if (! useXPath) {
      // punt and let XPath handle it.
      String iterName = "_xsl_iter" + _gen.generateId();
      
      String ptrName = "_xsl_ptr" + _gen.generateId();

      if (isForEach)
        out.println("env.setCurrentNode(node);");
      
      out.println("Iterator " + iterName + " = _select_patterns[" +
                  _gen.addSelect(select) + "].select(" + name + ", env);");
      
      if (loopVar != null && _gen.getSelectLoopDepth() == 0)
        out.println(loopVar + ":");
      
      out.println("while (" + iterName + ".hasNext()) {");
      out.pushDepth();
      _gen.pushSelectDepth();
      _gen.pushSelectLoopDepth();
      out.println("Node " + ptrName + " = (Node) " + iterName + ".next();");

      return ptrName;
    }

    if (select instanceof FromChildren) {
      name = printSelectBegin(out, select.getParent(), isForEach, loopVar);
      
      String ptrName = "_xsl_ptr" + _gen.generateId();

      if (loopVar != null && _gen.getSelectLoopDepth() == 0)
        out.println(loopVar + ":");
      
      out.println("for (Node " + ptrName + " = " + name + ".getFirstChild();");
      out.println("     " + ptrName + " != null;");
      out.println("     " + ptrName + " = " + ptrName + ".getNextSibling()) {");
      out.pushDepth();
      _gen.pushSelectDepth();
      _gen.pushSelectLoopDepth();

      return ptrName;
    }
    else if (select instanceof FromNextSibling) {
      name = printSelectBegin(out, select.getParent(), isForEach, loopVar);
      
      String ptrName = "_xsl_ptr" + _gen.generateId();
      
      if (loopVar != null && _gen.getSelectLoopDepth() == 0)
        out.println(loopVar + ":");
      
      out.println("for (Node " + ptrName + " = " + name + ".getNextSibling();");
      out.println("     " + ptrName + " != null;");
      out.println("     " + ptrName + " = " + ptrName + ".getNextSibling()) {");
      out.pushDepth();
      _gen.pushSelectDepth();
      _gen.pushSelectLoopDepth();

      return ptrName;
    }
    else if (select instanceof NodePattern) {
      name = printSelectBegin(out, select.getParent(), isForEach, loopVar);
      
      NodePattern pat = (NodePattern) select;
      
      out.println("if (" + name + ".getNodeName().equals(\"" + pat.getNodeName() + "\") &&");
      out.println("    " + name + " instanceof Element) {");
      out.pushDepth();
      _gen.pushSelectDepth();

      return name;
    }
    else if (select instanceof NodeTypePattern) {
      name = printSelectBegin(out, select.getParent(), isForEach, loopVar);
      
      NodeTypePattern pat = (NodeTypePattern) select;

      if (pat.getNodeType() >= 0) {
        out.println("if (" + name + ".getNodeType() == " + pat.getNodeType() + ") {");
        out.pushDepth();
        _gen.pushSelectDepth();
      }

      return name;
    }
    else if (select instanceof FilterPattern) {
      String posId = "_xsl_pos" + _gen.generateId();

      out.println("int " + posId + " = 0;");
      
      name = printSelectBegin(out, select.getParent(), isForEach, loopVar);

      out.println(posId + "++;");
      
      FilterPattern pat = (FilterPattern) select;
      Expr expr = pat.getExpr();

      if (expr instanceof NumericExpr) {
        NumericExpr num = (NumericExpr) expr;
        if (num.isConstant()) {
          out.println("if (" + posId + " > " + (int) num.getValue() + ")");
          out.println("  break;");
          out.println("else if (" + posId + " == " + (int) num.getValue() + ") {");
          out.pushDepth();
          _gen.pushSelectDepth();

          return name;
        }
      }

      throw new RuntimeException();
    }

    throw new RuntimeException(String.valueOf(select));
  }

  /**
   * Returns true if we can compile in the java select.
   */
  protected boolean allowJavaSelect(AbstractPattern select)
  {
    if (select == null)
      return false;

    else if (! select.isStrictlyAscending())
      return false;
    
    else if (select instanceof FromContext)
      return ((FromContext) select).getCount() == 0;

    else if (select instanceof FromRoot)
      return true;

    else if (select instanceof NodePattern)
      return allowJavaSelect(select.getParent());

    else if (select instanceof NodeTypePattern)
      return allowJavaSelect(select.getParent());

    else if (select instanceof FromChildren)
      return allowJavaSelect(select.getParent());

    else if (select instanceof FromNextSibling)
      return allowJavaSelect(select.getParent());

    else if (select instanceof FilterPattern) {
      if (! allowJavaSelect(select.getParent()))
        return false;

      Expr expr = ((FilterPattern) select).getExpr();

      return ((expr instanceof NumericExpr) &&
              ((NumericExpr) expr).isConstant());
    }

    else
      return false;
  }

  protected void printNamespace(JavaWriter out, NamespaceContext namespace)
    throws Exception
  {
    int index = _gen.addNamespace(namespace);

    out.print("_namespaces[" + index + "]");
  }

  /**
   * Prints the children as a fragment stored in a variable.
   */
  protected void printFragmentString(JavaWriter out, String id)
    throws Exception
  {
    String fragId = "_frag_" + _gen.generateId();
    
    out.println("XMLWriter " + fragId + " = out.pushFragment();");

    generateChildren(out);

    out.println(id + " = com.caucho.xml.XmlUtil.textValue(out.popFragment(" + fragId + "));");
  }

  /**
   * Prints the children as a fragment stored in a variable.
   */
  protected void printFragmentValue(JavaWriter out, String id)
    throws Exception
  {
    String fragId = "_frag_" + _gen.generateId();
    
    out.println("XMLWriter " + fragId + " = out.pushFragment();");

    generateChildren(out);

    out.println(id + " = out.popFragment(" + fragId + ");");
  }

  protected void popScope(JavaWriter out)
    throws Exception
  {
    printPopScope(out);
  }

  protected void printPopScope(JavaWriter out)
    throws Exception
  {
    if (_varCount > 0)
      out.println("env.popVars(" + _varCount + ");");
  }

  /**
   * Prints a test expr.
   */
  protected void printExprTest(JavaWriter out, int id, String node)
    throws IOException
  {
    out.print("_exprs[" + id + "].evalBoolean(" + node + 
              ", " + getEnv() + ")");
  }

  public AbstractPattern parseMatch(String pattern)
    throws XslParseException, IOException
  {
    try {
      return XPath.parseMatch(pattern, getMatchNamespace()).getPattern();
    } catch (Exception e) {
      throw error(L.l("{0} in pattern `{1}'",
                      e.toString(), pattern));
    }
  }

  protected AbstractPattern parseSelect(String pattern)
    throws XslParseException
  {
    try {
      return XPath.parseSelect(pattern, getMatchNamespace()).getPattern();
    } catch (Exception e) {
      throw error(e);
    }
  }

  protected int addExpr(String pattern)
    throws XslParseException
  {
    return _gen.addExpr(parseExpr(pattern));
  }

  /**
   * Parses an XPath expression in the current context.
   */
  protected Expr parseExpr(String pattern)
    throws XslParseException
  {
    try {
      return XPath.parseExpr(pattern,
                             getMatchNamespace(),
                             _gen.getNodeListContext());
    } catch (Exception e) {
      throw error(e);
    }
  }

  protected int generateId()
  {
    return _gen.generateId();
  }

  protected String getEnv()
  {
    return _gen.getEnv();
  }

  public String escapeJavaString(String s)
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

  /**
   * Creates a parse exception with the proper line information.
   */
  protected XslParseException error(String msg)
  {
    String filename = _filename;
    if (filename == null)
      filename = _systemId;
    
    if (filename != null)
      return new XslParseException(filename + ":" + _startLine + ": " + msg);
    else
      return new XslParseException(msg);
  }

  /**
   * Creates a parse exception with the proper line information.
   */
  protected XslParseException error(Throwable e)
  {
    String filename = _filename;
    if (filename == null)
      filename = _systemId;
    
    if (filename == null || e instanceof LineCompileException)
      return new XslParseException(e);
    else if (e instanceof CompileException)
      return new XslParseException(filename + ":" + _startLine + ": " +
                                   e.getMessage(), e);
    else
      return new XslParseException(_filename + ":" + _startLine + ": " +
                                   String.valueOf(e), e);
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
