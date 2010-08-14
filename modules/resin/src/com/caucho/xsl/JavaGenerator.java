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

package com.caucho.xsl;

import com.caucho.VersionFactory;
import com.caucho.java.JavaCompiler;
import com.caucho.java.JavaWriter;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntArray;
import com.caucho.util.IntMap;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QAbstractNode;
import com.caucho.xml.QAttr;
import com.caucho.xml.QElement;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;
import com.caucho.xpath.Expr;
import com.caucho.xpath.NamespaceContext;
import com.caucho.xpath.expr.NumericExpr;
import com.caucho.xpath.pattern.*;
import com.caucho.xsl.fun.KeyFun;
import com.caucho.xsl.java.*;

import org.w3c.dom.*;

import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Generates code for a Java based stylesheet.
 *
 * <pre>
 * package work.xsl;
 * public class foo extends JavaStylesheet {
 * }
 * </pre>
 */
public class JavaGenerator extends Generator {
  private static final Logger log
    = Logger.getLogger(JavaGenerator.class.getName());

  private static HashMap<QName,Class> _tagMap;
  private static HashMap<QName,Class> _topTagMap;
  
  private static int _count;

  Path _path;
  WriteStream _s;
  JavaWriter _out;

  ArrayList<AbstractPattern> _matchPatterns = new ArrayList<AbstractPattern>();
  IntMap _matchMap = new IntMap();
  ArrayList<AbstractPattern> _selectPatterns = new ArrayList<AbstractPattern>();
  IntMap _selectMap = new IntMap();
  
  ArrayList<Expr> _exprs = new ArrayList<Expr>();
  IntMap _exprMap = new IntMap();
  
  ArrayList<Sort[]> _sorts = new ArrayList<Sort[]>();
  ArrayList<NamespaceContext> _namespaces = new ArrayList<NamespaceContext>();
  ArrayList<XslNumberFormat> _formats = new ArrayList<XslNumberFormat>();
  ArrayList<String> _functions = new ArrayList<String>();
  ArrayList<Template> _templateList = new ArrayList<Template>();
  ArrayList<String> _stylesheets = new ArrayList<String>();
  int _templateCount = 0;
  // integer counting unique identifier
  int _unique;
  HashMap<String,String> _macros = new HashMap<String,String>();
  ArrayList<Object> _fragments = new ArrayList<Object>();
  
  ArrayList<String> _strings = new ArrayList<String>();
  IntMap _stringMap = new IntMap();
  
  IntArray _envDepth = new IntArray();

  ArrayList<String> _modes = new ArrayList<String>();

  private XslNode _xslNode;

  private boolean _isLineBegin;
  private int _depth;
  private int _callDepth;

  // integer counting the depth of nested selects
  private int _selectDepth;
  private int _selectLoopDepth;
  
  private int _flagCount;
  private String _className;
  private String _pkg;

  private String _currentPos;
  
  private ClassLoader _parentLoader;
  private JavaCompiler _compiler;
  private boolean _disableEscaping;
  private boolean _printLocation = true;

  private String _oldFilename = null;
  private int _oldLine = -1;

  private boolean _hasHeader;

  /**
   * Creates a new XSL generator for Java.
   *
   * @param xslGenerator the owning factory.
   * @param className the name of the generated class
   * @param encoding the generated output encoding.
   */
  JavaGenerator(AbstractStylesheetFactory xslGenerator, 
                String className, String encoding)
    throws IOException
  {
    super(xslGenerator);

    _parentLoader = xslGenerator.getClassLoader();

    ArrayList pathDepends = new ArrayList();

    _compiler = JavaCompiler.create(_parentLoader);
    _compiler.setClassDir(_workPath);

    if (encoding == null) {
    }
    else if (encoding.equalsIgnoreCase("UTF-16")) {
      // utf-16 isn't supported by some javac
      encoding = "UTF-8";
      _compiler.setEncoding(encoding);
    } else {
      _compiler.setEncoding(encoding);
    }

    int p = className.lastIndexOf('.');
    if (p >= 0) {
      _pkg = className.substring(0, p);
      className = className.substring(p + 1);
    }
    else {
      _pkg = "_xsl";
      className = className;
    }
      
    _className = className;
    init((_pkg + "." + className).replace('.', '/') + ".java");

    String fileName = (_pkg + "." + className).replace('.', '/') + ".java";
    _path = _workPath.lookup(fileName);
    _path.getParent().mkdirs();

    _s = _path.openWrite();
    if (encoding != null)
      _s.setEncoding(encoding);
    if (_s.getEncoding() == null || _s.getEncoding().equals("ISO-8859-1"))
      _s.setEncoding("JAVA");
    _out = new JavaWriter(_s);
    _out.setLineMap(_lineMap);
    
    _matchPatterns = new ArrayList<AbstractPattern>();
    _selectPatterns = new ArrayList<AbstractPattern>();

    _modes = new ArrayList<String>();
    _modes.add("");
  }

  protected JavaWriter getOut()
  {
    return _out;
  }

  public int getSelectDepth()
  {
    return _selectDepth;
  }

  public void setSelectDepth(int depth)
  {
    _selectDepth = depth;
  }

  public int pushSelectDepth()
  {
    return ++_selectDepth;
  }

  public int popSelectDepth()
  {
    return _selectDepth--;
  }

  public int getSelectLoopDepth()
  {
    return _selectLoopDepth;
  }

  public int pushSelectLoopDepth()
  {
    return ++_selectLoopDepth;
  }

  public int popSelectLoopDepth()
  {
    return _selectLoopDepth--;
  }

  public void setSelectLoopDepth(int depth)
  {
    _selectLoopDepth = depth;
  }

  public int generateId()
  {
    return _unique++;
  }

  public void clearUnique()
  {
    _unique = 0;
  }
    

  /**
   * Prints the generated header.
   */
  protected void printHeader()
    throws IOException
  {
    if (_hasHeader)
      return;
    _hasHeader = true;
    
    println("/*");
    println(" * Generated by " + VersionFactory.getFullVersion());
    println(" */");
    println();
    println("package " + _pkg + ";");
    println();
    println("import java.io.*;");
    println("import java.util.*;");
    println("import org.w3c.dom.*;");
    println("import org.xml.sax.*;");
    println("import com.caucho.util.*;");
    println("import com.caucho.xml.*;");
    println("import com.caucho.xpath.*;");
    println("import com.caucho.xpath.expr.*;");
    println("import com.caucho.xpath.pattern.*;");
    println("import com.caucho.xsl.*;");

    try {
      Class.forName("javax.servlet.Servlet");
      println("import javax.servlet.*;");
      println("import javax.servlet.jsp.*;");
      println("import javax.servlet.http.*;");
    } catch (Throwable e) {
    }
    
    for (int i = 0; i < _imports.size(); i++)
      println("import " + _imports.get(i) + ";");
    println();

    println("public class " + _className + " extends JavaStylesheet {");
    pushDepth();

    println("private StylesheetEnv stylesheets[];");
  }
  
  protected void generateChild(Node child)
    throws Exception
  {
    XslNode node = createChild(child);

    if (node != null)
      node.generate(_out);
  }

  protected XslNode createChild(XslNode parent, Node childNode)
    throws Exception
  {
    XslNode xslNode = _xslNode;

    _xslNode = parent;

    XslNode child = createChild(childNode);

    _xslNode = xslNode;

    return child;
  }
  
  protected XslNode createChild(Node child)
    throws Exception
  {
    XslNode xslNode = null;

    if (child instanceof QElement) {
      QElement elt = (QElement) child;

      Class cl = _tagMap.get(elt.getQName());

      if (cl != null) {
        xslNode = (XslNode) cl.newInstance();
        xslNode.setGenerator(this);
        xslNode.setParent(_xslNode);

        xslNode.setStartLocation(((QAbstractNode) child).getBaseURI(),
                                 ((QAbstractNode) child).getFilename(),
                                 ((QAbstractNode) child).getLine());

        QAttr attr = (QAttr) elt.getFirstAttribute();
        for (; attr != null; attr = (QAttr) attr.getNextSibling()) {
          xslNode.addAttribute(attr.getQName(), attr.getNodeValue());
        }

        xslNode.endAttributes();

        XslNode oldNode = _xslNode;
        _xslNode = xslNode;

        Node node = elt.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
          XslNode xslChild = createChild(node);

          if (xslChild != null)
            xslNode.addChild(xslChild);
        }

        xslNode.endElement();

        _xslNode = oldNode;
      }
      /*
      else if (elt.getNodeName().equals("jsp:decl") ||
               elt.getNodeName().equals("jsp:declaration") ||
               elt.getNodeName().startsWith("jsp:directive")) {
      }
      */
      else if (child.getNodeName().startsWith("xsl:") &&
               ! XSLNS.equals(child.getNamespaceURI())) {
        throw error(child, L.l("<{0}> has an xsl: prefix, but is not in the {1} namespace.  XSL requires an xmlns:xsl=\"{1}\" namespace attribute.",
                               child.getNodeName(),
                               XSLNS));
      }
      else if (! XSLNS.equals(child.getNamespaceURI()) &&
               ! XTPNS.equals(child.getNamespaceURI())) {
        xslNode = new XslElementNode(elt.getQName());
        xslNode.setGenerator(this);
        xslNode.setParent(_xslNode);

        xslNode.setStartLocation(((QAbstractNode) child).getBaseURI(),
                                 ((QAbstractNode) child).getFilename(),
                                 ((QAbstractNode) child).getLine());

        QAttr attr = (QAttr) elt.getFirstAttribute();
        for (; attr != null; attr = (QAttr) attr.getNextSibling())
          xslNode.addAttribute(attr.getQName(), attr.getNodeValue());

        xslNode.endAttributes();

        XslNode oldNode = _xslNode;
        _xslNode = xslNode;

        Node node = elt.getFirstChild();
        for (; node != null; node = node.getNextSibling()) {
          XslNode xslChild = createChild(node);

          xslNode.addChild(xslChild);
        }

        xslNode.endElement();

        _xslNode = oldNode;
      }
      else {
        throw error(child, L.l("<{0}> is an unknown XSL tag.",
                        child.getNodeName()));
        /*
          XslWrapperNode wrapNode = new XslWrapperNode();
          wrapNode.setNode(child);
          xslNode = wrapNode;
          xslNode.setGenerator(this);
        */
      }
    }
    else if (child instanceof Text) {
      xslNode = new TextNode(((Text) child).getData());
      xslNode.setGenerator(this);
      xslNode.setParent(_xslNode);
    }
    else if (child instanceof Comment) {
    }
    else if (child instanceof ProcessingInstruction) {
    }
    else
      throw new UnsupportedOperationException(String.valueOf(child));

    if (xslNode != null) {
      xslNode.setStartLocation(((QAbstractNode) child).getBaseURI(),
                               ((QAbstractNode) child).getFilename(),
                               ((QAbstractNode) child).getLine());
    }

    return xslNode;
  }
  
  /**
   * Generates code for a template
   *
   * @param absNode the XSL node for the emplace
   * @param name the template name
   * @param pattern the pattern string
   * @param mode the template's mode
   * @param priority the template's priority
   */
  protected void printTemplate(Element absNode, String name, 
                               String pattern, String mode, double priority)
    throws Exception
  {
    throw new RuntimeException();
    /*
    QElement node = (QElement) absNode;
    
    if (name != null && ! name.equals(""))
      addMacro(name, node);
    
    if (! pattern.equals("")) {
      String fun = createTemplatePattern(name, pattern,
                                         mode, priority);
      
      print("// '" + pattern.replace('\n', ' ') + "'");
      
      if (mode != null && mode != "") {
        if (! _modes.contains(mode))
          _modes.add(mode);
        println(" mode '" + mode + "'");
      }
      else
        println();
      
      printString("// " + node.getFilename() + ":" + node.getLine());
      println();
      
      println("private void " + fun +
              "(XslWriter out, Node inputNode, Env env)");
      println("  throws Exception");
      println("{");
      pushDepth();

      println("Object _xsl_tmp;");
      println("Node node = inputNode;");
      println("int _xsl_top = env.getTop();");

      if (_isRawText)
        println("boolean oldEscaping = out.disableEscaping(true);");
      else
        println("boolean oldEscaping = out.disableEscaping(false);");

      String filename = node.getBaseURI();
      if (filename != null) {
        int pos = _stylesheets.indexOf(filename);
        if (pos < 0) {
          pos = _stylesheets.size();
          _stylesheets.add(filename);
        }
        
        println("env.setStylesheetEnv(stylesheets[" + pos + "]);");
      }

      _selectDepth = 0;
      _unique = 0;
      
      if (node.getLocalName().equals("template") ||
          node.getLocalName().equals("xsl:template"))
        generateChildren(node);
      else
        generateChild((QAbstractNode) node);
      
      if (! _isCacheable)
        println("out.setNotCacheable();");

      println("out.disableEscaping(oldEscaping);");
      println("env.popToTop(_xsl_top);");
      popDepth();
      println("}");
      println();
    }
    */
  }

  public void addMacro(String name, String functionName)
  {
    _macros.put(name, functionName);
  }

  /*
  public void addMacro(String name)
  {
    addMacro(name, "_xsl_macro_" + toJavaIdentifier(name);
  }
  */

  public boolean hasMacro(String name)
  {
    return _macros.keySet().contains(name);
  }

  /**
   * Generates the pattern for a matching pattern
   *
   * @param name the mangled name of the function
   * @param match the XPath match pattern
   * @param mode the template mode
   * @param priority the template priority
   * @param node the source XML node from the XSL file
   *
   * @return the name of the function
   */
  public String createTemplatePattern(String name, AbstractPattern match,
                                      String mode, double priority)
    throws Exception
  {
    String tagName;

    if (name != null)
      tagName = getName(name);
    else
      tagName = getName(match.toString());

    String function = "_xsl_template_" + tagName;
    _functions.add(function);

    if (match != null) {
      Template template = addPattern(match,
                                     mode, priority, function,
                                     _functions.size());
      _templateList.add(template);
    }
    else
      _templateList.add(null);

    return function;
  }
  
  protected void startDisableEscaping()
    throws IOException
  {
    if (! _isRawText)
      println("out.disableEscaping(true);");
  }
 
  protected void endDisableEscaping()
    throws IOException
  {
    if (! _isRawText)
      println("out.disableEscaping(false);");
  }

  /**
   * Creates Java code to print plain text.
   */
  protected void writeText(String text)
    throws Exception
  {
    if (text == null || text.length() == 0)
      return;

    int index = _stringMap.get(text);
    if (index < 0) {
      index = _strings.size();
      _stringMap.put(text, index);
      _strings.add(text);
    }

    printLocation(_systemId, _filename, _line);
    println("out.write(_xsl_string" + index + ", 0, " + text.length() + ");");
  }

  protected void printElement(Node node)
    throws Exception
  {
    QElement elt = (QElement) node;
    String name = node.getNodeName();

    if (name.equals("jsp:decl") || name.equals("jsp:declaration")) {
      println("if (out.isFlagFirst(" + _flagCount++ + ")) {");
      pushDepth();
    }
    
    String prefix = elt.getPrefix();
    String local = elt.getLocalName();
    String namespace = elt.getNamespaceURI();

    String []postPrefix = (String []) _namespaceAliases.get(namespace);
    if (postPrefix != null) {
      prefix = postPrefix[0];
      namespace = postPrefix[1];
      if (prefix == null || prefix.equals(""))
        name = local;
      else
        name = prefix + ":" + local;
    }
    if (_excludedNamespaces.get(namespace) != null)
      namespace = null;

    printLocation(_systemId, _filename, _line);
    if (namespace == null || namespace.equals("")) {
      print("out.pushElement(");
      print(name == null ? "null" : ("\"" + name + "\""));
      println(");");
    } else {
      print("out.pushElement(");
      print(namespace == null ? "null" : ("\"" + namespace + "\""));
      print(prefix == null ? ", null" : (", \"" + prefix + "\""));
      print(local == null ? ", null" : (", \"" + local + "\""));
      print(name == null ? ", null" : (", \"" + name + "\""));
      println(");");
    }
    
    printUseAttributeSet((QElement) node, false);
    
    NamedNodeMap list = node.getAttributes();
    for (int i = 0; i < list.getLength(); i++) {
      QAbstractNode attr = (QAbstractNode) list.item(i);

      printAttribute(attr, elt);
    }
    
    generateChildren(node);
    
    println("out.popElement();");

    if (node.getNodeName().equals("jsp:decl") ||
        node.getNodeName().equals("jsp:declaration")) {
      popDepth();
      println("}");
    }
  }

  /**
   * Prints a command to set the current file and line into the
   * generated document.
   *
   * @param filename the source filename
   * @param line the source line number.
   */
  public void printLocation(String systemId, String filename, int line)
    throws Exception
  {
    if (_printLocation && filename != null && ! _isSpecial) {
      print("out.setLocation(");
      if (systemId != null) {
        print("\"");
        printString(systemId);
        print("\"");
      }
      else
        print("null");
      print(", \"");
      printString(filename);
      println("\", " + line + ");");
      _oldFilename = filename;
      _oldLine = line;
    }
  }

  /**
   * Prints code for an element's attributes.
   */
  private void printAttribute(QAbstractNode attr, QElement elt)
    throws Exception
  {
    if (attr.getNodeName().equals("xsl:use-attribute-sets")) {
    }
    else if (XSLNS.equals(elt.getNamespace(attr.getPrefix()))) {
    }
    else if (XTPNS.equals(elt.getNamespace(attr.getPrefix()))) {
    }
    else {
      QAbstractNode qnode = (QAbstractNode) attr;
      String prefix = qnode.getPrefix();
      String local = qnode.getLocalName();
      String namespace = qnode.getNamespaceURI();
      String value = attr.getNodeValue();

      String []postSuffix = (String []) _namespaceAliases.get(namespace);
      if (postSuffix != null) {
        prefix = postSuffix[0];
        namespace = postSuffix[1];
      }

      else if (value.equals(XSLNS) && prefix.equals("xmlns"))
        return;
      else if (value.equals(XTPNS) && prefix.equals("xmlns"))
        return;

      if (_excludedNamespaces.get(namespace) != null)
        namespace = null;
      
      if ("".equals(prefix) && ("".equals(namespace) || namespace == null)) {
        String var = generateStringVar(value, elt);
        println("out.setAttribute(\"" + local + "\", " + var + ");");
      }
      else {
        print("out.pushAttribute(");
        print(prefix == null ? "null" : ("\"" + prefix + "\""));
        print(local == null ? ", null" : (", \"" + local + "\""));
        print(namespace == null ? ", null" : (", \"" + namespace + "\""));
        println(");");
        generateString(value, ',', elt);
        println("out.popAttribute();");
      }
    }
  }

  protected void pushCall()
    throws IOException
  {
    println("{");
    pushDepth();
    _callDepth++;
    println("Env _xsl_arg" + _callDepth + " = XPath.createCall(env);");
  }

  public int pushCallDepth()
  {
    return ++_callDepth;
  }

  public int popCallDepth()
  {
    return _callDepth--;
  }

  public int getCallDepth()
  {
    return _callDepth;
  }

  protected void popCall()
    throws IOException
  {
    //println("_xsl_arg" + callDepth + ".free();");
    _callDepth--;
    popDepth();
    println("}");
  }

  /**
   * Prints code for xsl:apply-templates
   *
   * @param select the select pattern
   * @param mode the template mode
   * @param sort the sort expressions
   */
  protected void printApplyTemplates(AbstractPattern select,
                                     String mode,
                                     Sort []sort)
    throws Exception
  {
    int min = 0;
    int max = Integer.MAX_VALUE;

    String applyName = "applyNode" + getModeName(mode);
    String env = "_xsl_arg" + _callDepth;

    if (select == null && sort == null) {
      println("for (Node _xsl_node = node.getFirstChild();");
      println("     _xsl_node != null;");
      println("     _xsl_node = _xsl_node.getNextSibling()) {");
      println("  " + env + ".setSelect(node, null);");
      println("  " + env + ".setCurrentNode(_xsl_node);");
      println("  " + applyName + "(out, _xsl_node, " + env + ", " +
              min + ", " + max + ");");
      println("}");
    }
    else if (sort == null) {
      int oldSelectDepth = _selectDepth;
      println(env + ".setSelect(node, _select_patterns[" +
              addSelect(select) + "]);");

      String name = printSelectBegin(select, false, null);

      println(env + ".setCurrentNode(" + name + ");");

      println(applyName + "(out, " + name + ", " + env + ", " + 
              min + ", " + max + ");");

      for (; _selectDepth > oldSelectDepth; _selectDepth--) {
        popDepth();
        println("}");
      }
    }
    else {
      println("{");
      pushDepth();
      println("ArrayList _xsl_list = xslSort(node, env" +
              ", _select_patterns[" + addSelect(select) + "]" +
              ", _xsl_sorts[" + _sorts.size() + "]);");
      println(env + ".setContextSize(_xsl_list.size());");
      println("for (int _xsl_i = 0; _xsl_i < _xsl_list.size(); _xsl_i++) {");
      println("  " + env + ".setContextPosition(_xsl_i + 1);");
      println("  " + applyName + "(out, (Node) _xsl_list.get(_xsl_i)" + 
              ", " + env + ", " + min + ", " + max + ");");
      println("}");
      popDepth();
      println("}");

      _sorts.add(sort);
    }
  }

  public int addSort(Sort []sort)
  {
    int index = _sorts.size();
    
    _sorts.add(sort);

    return index;
  }

  /**
   * Prints code to implement xsl:apply-imports
   *
   * @param mode the mode of the imported files
   * @param min the min importance
   * @param max the max importance
   */
  protected void printApplyImports(String mode, int min, int max)
    throws Exception
  {
  }

  protected void printCallTemplate(String name, String mode)
    throws Exception
  {
    println(getMacroName(name) + "(out, node, _xsl_arg" +
            _callDepth + ");");
  }

  public String getMacroName(String name)
  {
    return _macros.get(name);
    //return "_xsl_macro_" + toJavaIdentifier(name);
  }

  /**
   * Prints the value for a parameter.
   */
  protected void printParam(String name, String value, Element elt)
    throws Exception
  {
    print("_xsl_arg" + _callDepth + ".addVar(\"" + name + "\", ");
    generateString(value, '+', elt);
    println(");");
  }
  
  protected void printParam(String name, Object value)
    throws Exception
  {
    if (value instanceof Expr) {
      print("_exprs[" + addExpr((Expr) value) + "]");
      println(".addVar(_xsl_arg" + _callDepth + ", \"" + name + "\", " +
              "node, env);");
    }
    else {
      print("_xsl_arg" + _callDepth + ".addVar(\"");
      print(name);
      print("\", ");
      printVariableValue(value);
      println(");");
    }
  }

  /**
   * Prints code to add the value of an expression as a parameter.
   */
  protected void printParamVariable(String name, Expr value)
    throws Exception
  {
    print("_exprs[" + addExpr(value) + "]");
    println(".addParam(env, \"" + name + "\", " +
            "node, env);");
  }

  protected void printParamVariable(String name, Element value)
    throws Exception
  {
    if (value.getFirstChild() != null) {
      println("_xsl_tmp = env.getVar(\"" + name + "\");"); 
      println("if (_xsl_tmp == null)");
      print("  _xsl_tmp = ");
      printVariableValue(value);
      println(";");
      println("env.addVar(\"" + name + "\", _xsl_tmp);");
    }
  }

  protected void printVariable(String name, Object value)
    throws Exception
  {
    if (value instanceof Expr) {
      print("_exprs[" + addExpr((Expr) value) + "]");
      println(".addVar(env, \"" + name + "\", node, env);");
    }
    else {
      print("env.addVar(\"");
      print(name);
      print("\", ");
      printVariableValue(value);
      println(");");
    }
  }

  protected void printAssign(String name, Object value)
    throws Exception
  {
    if (value instanceof Expr) {
      print("_exprs[" + addExpr((Expr) value) + "]");
      println(".setVar(\"" + name + "\", node, env, node);");
    }
    else {
      print("env.setVar(\"");
      print(name);
      print("\", ");
      printVariableValue(value);
      println(");");
    }
  }

  private void printVariableValue(Object value)
    throws Exception
  {
    if (value instanceof Expr) {
      print("_exprs[" + addExpr((Expr) value) + "].evalObject(node, env)");
    }
    else if (value instanceof Node) {
      print("_xsl_fragment" + _fragments.size() + "(out, node, env)");
      _fragments.add(value);
    }
    else
      throw new RuntimeException();
  }

  protected void printPopScope(int count)
    throws Exception
  {
    if (count > 0)
      println("env.popVars(" + count + ");");
  }

  protected void printCopyOf(String select, Element elt)
    throws Exception
  {
    println("out.copyOf(_exprs[ " + addExpr(select) +
            "].evalObject(node, env));");
  }

  protected void printSelectValue(String select, Element elt)
    throws Exception
  {
    printStringExpr(select, elt);
  }

  protected void printForEach(Element element, String select)
    throws Exception
  {
    println("{");
    pushDepth();

    AbstractPattern selectPattern = null;
    try {
      selectPattern = parseSelect(select);
    } catch (Exception e) {
    }
    
    boolean hasExprEnv = ! allowJavaSelect(selectPattern);

    int id = _unique++;
    
    String sel = "_xsl_sel" + id;
    String oldCxt = "_xsl_cxt" + id;
    String oldCur = "_xsl_cur" + id;
    String oldSel = "_xsl_old_sel" + id;
    String oldEnv = "_xsl_env" + id;

    println("com.caucho.xpath.pattern.AbstractPattern " + sel + ";");
    print(sel + " = _select_patterns[");
    print(createNodeSet(select, element));
    println("];");
    println("Node " + oldCxt + " = env.getContextNode();");
    println("Node " + oldCur + " = env.getCurrentNode();");
    
    if (! hasExprEnv) {
      println("AbstractPattern " + oldSel + " = env.setSelect(node, " + sel + ");");
    }
    
    
    // String pos = "_xsl_pos" + unique++;
    String iter = "_xsl_iter" + _unique++;

    int oldSelectDepth = _selectDepth;
    
    // println("int " + pos + " = 0;");

    boolean hasEnv = false;
    
    if (allowJavaSelect(selectPattern)) {
      println("ExprEnvironment " + oldEnv + " = env.setExprEnv(null);");
      
      String ptr = printSelectBegin(selectPattern, true, null);

      pushLoop();
      println("Node " + getElement() + " = node;");
      println("node = " + ptr + ";");
    }
    else {
      print("NodeIterator " + iter + " = " + sel);
      println(".select(node, " + getEnv() + ");");
      println("ExprEnvironment " + oldEnv + " = env.setExprEnv(" + iter + ");");
      println("while (" + iter + ".hasNext()) {");
      pushDepth();
      _selectDepth++;
      
      pushLoop();
      
      println("Node " + getElement() + " = node;");
      println("node = " + iter + ".nextNode();");
      
    }
    println("env.setCurrentNode(node);");
    
    // println(pos + "++;");

    // String oldPos = currentPos;
    // currentPos = pos;

    AbstractPattern oldNodeListContext = _nodeListContext;
    _nodeListContext = parseMatch(select);

    generateChildren(element);

    _nodeListContext = oldNodeListContext;
    
    // currentPos = oldPos;
    
    println("node = " + getElement() + ";");
    println("env.setCurrentNode(" + oldCur + ");");
    
    for (; _selectDepth > oldSelectDepth; _selectDepth--) {
      popDepth();
      println("}");
    }
    
    println("env.setExprEnv(" + oldEnv + ");");
    
    if (! hasExprEnv) {
      println("env.setSelect(" + oldCxt + ", " + oldSel + ");");
    //println("env.setCurrentNode(node);");
    }
    
    popDepth();
    println("}");
    popLoop();
  }

  /**
   * Prints code for xsl:for-each when the for-each has any xsl:sort.
   */
  protected void printForEach(Element element, String select, Sort []sort)
    throws Exception
  {
    println("{");
    pushDepth();
    println("env.setCurrentNode(node);");
    String pos = "_xsl_pos" + _unique++;
    String list = "_xsl_list" + _unique++;
    
    println("ArrayList " + list +
            " = xslSort(node, env" +
            ", _select_patterns[" + addSelect(select) + "]" +
            ", _xsl_sorts[" + _sorts.size() + "]);");
    println("env.setContextSize(" + list + ".size());");
    println("for (int " + pos + " = 1; " + pos +
            " <= " + list + ".size(); " + pos + "++) {");
    pushLoop();
    pushDepth();
    println("Node " + getElement() + " = node;");
    println("node = (Node) " + list + ".get(" + pos + " - 1);");

    String oldPos = _currentPos;
    _currentPos = pos;
    
    println("env.setPosition(" + _currentPos + ");");
    
    _sorts.add(sort);
    
    AbstractPattern oldNodeListContext = _nodeListContext;
    _nodeListContext = parseMatch(select);

    generateChildren(element);

    _currentPos = oldPos;

    _nodeListContext = oldNodeListContext;
    
    println("node = " + getElement() + ";");
    
    popDepth();
    println("}");
    popLoop();
    popDepth();
    println("}");
  }

  public String getCurrentPosition()
  {
    return _currentPos;
  }

  public void setCurrentPosition(String pos)
  {
    _currentPos = pos;
  }

  public AbstractPattern getNodeListContext()
  {
    return _nodeListContext;
  }

  public void setNodeListContext(AbstractPattern context)
  {
    _nodeListContext = context;
  }

  protected void printIf(Element element, Expr test)
    throws Exception
  {
    print("if (");
    printExprTest(test, "node");
    println(") {");
    pushDepth();
    generateChildren(element);
    popDepth();
    println("}");
  }

  protected void printChoose(Element element, Expr expr, boolean first)
    throws Exception
  {
    if (! first)
      print("else if (");
    else
      print("if (");
    printExprTest(expr, "node");
    println(") {");
    pushDepth();
    generateChildren(element);
    popDepth();
    println("}");
  }

  protected void printOtherwise(Element element, boolean first)
    throws Exception
  {
    if (! first)
      print("else ");
    println("{");
    pushDepth();
    generateChildren(element);
    popDepth();
    println("}");
  }

  void printNumber(Expr expr, XslNumberFormat format)
    throws Exception
  {
    print("exprNumber(out, node, env, _exprs[" + addExpr(expr) + "]");
    print(", _xsl_formats[" + _formats.size() + "]");
    println(");");

    _formats.add(format);
  }

  void printNumber(String level,
                   AbstractPattern countPattern,
                   AbstractPattern fromPattern,
                   XslNumberFormat format)
    throws Exception
  {
    if (level.equals("single"))
      print("singleNumber(out, ");
    else if (level.equals("multiple"))
      print("multiNumber(out, ");
    else if (level.equals("any"))
      print("anyNumber(out, ");
    else
      throw error("xsl:number cannot understand level=`" + level + "'");

    print("node, env, ");
    printPattern(countPattern);
    print(", ");
    printPattern(fromPattern);
    print(", _xsl_formats[" + _formats.size() + "]");
    println(");");

    _formats.add(format);
  }

  public int addFormat(XslNumberFormat format)
  {
    int index = _formats.size();

    _formats.add(format);

    return index;
  }

  protected void printCopy(Element element)
    throws Exception
  {
    println("out.pushCopy(node);");
    printUseAttributeSet(element, true);
    generateChildren(element);      
    println("out.popCopy(node);");
  }

  protected void printResultDocument(Element element, String href, String format)
    throws Exception
  {
    println("XslWriter oldOut = out;");
    println("OutputStream os = null;");
    println("try {");
    pushDepth();
    print("os = out.openWrite(env, ");
    generateString(href, '+', element);
    println(");");

    println("out = out.openResultDocument(os);");
    generateChildren(element);
    println("out.close();");
    popDepth();
    println("} finally {");
    println("  if (os != null)");
    println("    os.close();");
    println("  out = oldOut;");
    println("}");
  }

  protected void printElement(Element element, String name)
    throws Exception
  {
    print("out.pushElement(");
    generateString(name, '+', element);
    if (_namespace != null) {
      print(", ");
      printNamespace(_namespace);
    }
    println(");");
    printUseAttributeSet(element, true);
    generateChildren(element);      
    println("out.popElement();");
  }

  protected void printElement(Element element, String name, String namespace)
    throws Exception
  {
    print("out.pushElementNs(");
    generateString(name, '+', element);
    print(", ");
    generateString(namespace, '+', element);
    println(");");
    printUseAttributeSet(element, true);
    generateChildren(element);      
    print("out.popElement();");
  }

  /**
   * Prints the attributes in a use-attribute-set.
   */
  private void printUseAttributeSet(Element element, boolean isXSL)
    throws Exception
  {
    Attr attr = (Attr) ((QElement) element).getFirstAttribute();
    for (; attr != null; attr = (Attr) attr.getNextSibling()) {
      if (isXSL && attr.getNodeName().equals("use-attribute-sets") ||
          ! isXSL && attr.getNodeName().equals("xsl:use-attribute-sets")) {
        HashMap set = getAttributeSet(attr.getNodeValue());
        if (set == null)
          continue;
        Iterator iter = set.keySet().iterator();
        while (iter.hasNext()) {
          String key = (String) iter.next();
          String value = (String) set.get(key);
          
          printAttributeValue(key, value, element);
        }
      }
    }
  }

  /**
   * Returns the named attribute set.
   */
  public HashMap<String,String> getAttributeSet(String name)
  {
    CharBuffer cb = CharBuffer.allocate();
    int i = 0;
    int len = name.length();

    HashMap<String,String> map = new HashMap<String,String>();
    
    while (i < len) {
      for (; i < len && name.charAt(i) == ' '; i++) {
      }

      cb.clear();
      for (; i < len && name.charAt(i) != ' '; i++)
        cb.append(name.charAt(i));

      if (cb.length() > 0) {
        XslAttributeSet newSet = _attributeSets.get(cb.toString());

        if (newSet != null) {
          ArrayList<XslAttribute> attrList = newSet.getAttributes();

          for (int j = 0; j < attrList.size(); j++) {
            XslAttribute attr = attrList.get(j);

            map.put(attr.getName(), attr.getValue());
          }
        }
      }
    }

    return map;
  }

  /**
   * Returns the named attribute set.
   */
  public ArrayList<XslAttribute> getAttributeSetList(String name)
  {
    CharBuffer cb = CharBuffer.allocate();
    int i = 0;
    int len = name.length();

    ArrayList<XslAttribute> set = new ArrayList<XslAttribute>();
    
    while (i < len) {
      for (; i < len && name.charAt(i) == ' '; i++) {
      }

      cb.clear();
      for (; i < len && name.charAt(i) != ' '; i++)
        cb.append(name.charAt(i));

      if (cb.length() > 0) {
        XslAttributeSet newSet = _attributeSets.get(cb.toString());

        if (newSet != null) {
          set.addAll(newSet.getAttributes());
        }
      }
    }

    return set;
  }

  /**
   * Prints an xsl:attribute
   */
  protected void printAttribute(Element element, String name)
    throws Exception
  {
    print("out.pushAttribute(");
    generateString(name, '+', element);
    
    if (_namespace != null) {
      print(", ");
      printNamespace(_namespace);
    }
    
    println(");");
      
    generateChildren(element);      
    println("out.popAttribute();");
  }

  /**
   * Prints a single attribute value.
   */
  private void printAttributeValue(String key, String value, Element elt)
    throws Exception
  {
    if (_namespace == null && ! attributeHasSpecial(key) &&
        ! attributeHasSpecial(value)) {
      print("out.setAttribute(");
      generateString(key, '+', elt);
      print(", ");
      generateString(value, '+', elt);
      println(");");
    }
    else {
      print("out.pushAttribute(");
      generateString(key, '+', elt);
      if (_namespace != null) {
        print(", ");
        printNamespace(_namespace);
      }
      println(");");
      generateString(value, ',', elt);
      println("out.popAttribute();");
    }
  }

  public void printNamespace(NamespaceContext namespace)
    throws Exception
  {
    for (int i = 0; i < _namespaces.size(); i++) {
      if (_namespaces.get(i).equals(namespace)) {
        print("_namespaces[" + i + "]");
        return;
      }
    }
    
    print("_namespaces[" + _namespaces.size() + "]");
    _namespaces.add(namespace);
  }

  public int addNamespace(NamespaceContext namespace)
    throws Exception
  {
    for (int i = 0; i < _namespaces.size(); i++) {
      if (_namespaces.get(i).equals(namespace)) {
        return i;
      }
    }
    
    _namespaces.add(namespace);

    return _namespaces.size() - 1;
  }

  protected void printAttribute(Element element, String name, String namespace)
    throws Exception
  {
    print("out.pushAttributeNs(");
    generateString(name, '+', element);
    print(", ");
    generateString(namespace, '+', element);
    println(");");
    generateChildren(element);      
    println("out.popAttribute();");
  }

  protected void printPi(Element element)
    throws Exception
  {
    String name = element.getAttribute("name");
    if (name.equals(""))
      throw error("xsl:pi expected `name' attribute.");

    print("out.pushPi();");
    
    generateChildren(element);
    println("out.popPi(");
    generateString(name, '+', element);
    println(");");
  }

  protected void printComment(Element element)
    throws Exception
  {
    println("out.pushComment();");
    generateChildren(element);
    println("out.popComment();");
  }

  protected void printError(String msg)
    throws Exception
  {
    println("if (true) throw new javax.xml.transform.TransformerException(\"" + msg + "\");");
  }

  protected void printMessage(Element msg)
    throws Exception
  {
    int unique = _unique++;
    
    println("XMLWriter frag" + unique + " = out.pushFragment();");
    generateChildren(msg);

    String terminate = msg.getAttribute("terminate");
    if (terminate.equals("yes"))
      println("if (true) throw new javax.xml.transform.TransformerException(((QAbstractNode) out.popFragment(frag" + unique + ")).getTextValue());");
    else
      println("System.err.println(((QAbstractNode) out.popFragment(frag" + unique + ")).getTextValue());");
  }

  /**
   * Prints code to implement the xtp:expression tag, i.e. print
   * the value of the Java expression.
   */
  protected void printExpression(Element element)
    throws Exception
  {
    String expr = element.getAttribute("expr");

    if (! expr.equals("")) {
      print("out.print(");
      print(expr);
      println(");");
    }
    else {
      print("out.print(");
      print(((QAbstractNode) element).getTextValue());
      println(");");
    }
  }

  protected void printScriptlet(Element element)
    throws Exception
  {
    println(((QAbstractNode) element).getTextValue());
  }

  protected void printWhile(Element element, Expr test)
    throws Exception
  {
    print("while (");
    printExprTest(test, "node");
    println(") {");
    pushDepth();
    generateChildren(element);
    popDepth();
    println("}");
  }

  protected void printDeclaration(Element element)
    throws Exception
  {
    println(((QAbstractNode) element).getTextValue());
  }

  protected void printCacheDepends(String name)
    throws Exception
  {
    print("out.addCacheDepend(((com.caucho.vfs.Path) out.getProperty(\"caucho.pwd\")).lookup(\"");
    printString(name);
    println("\"));");
  }

  public String getElement()
  {
    return "node" + _loopDepth;
  }

  public void pushLoop()
  {
    _loopDepth++;
  }

  public void popLoop()
  {
    _loopDepth--;
  }

  public String getEnv()
  {
    return "env";
  }

  void pushEnv()
  {
    _envDepth.add(0);
  }

  void popEnv()
  {
    _envDepth.pop();
  }

  void printPattern(AbstractPattern pattern)
    throws Exception
  {
    if (pattern == null)
      print("null");
    else {
      print("_match_patterns[" + _matchPatterns.size() + "]");
      _matchPatterns.add(pattern);
    }
  }

  private int createNodeSet(String select, Element element)
    throws Exception
  {
    return addSelect(select);
  }
  
  int createSelectPattern(AbstractPattern pattern)
    throws Exception
  {
    return addSelect(pattern);
  }

  int createMatchPattern(String select, Element element)
    throws Exception
  {
    AbstractPattern pattern = parseMatch(select);

    _matchPatterns.add(pattern);

    return _matchPatterns.size() - 1;
  }

  String getName(String tag)
  {
    CharBuffer newTag = new CharBuffer();

    for (int i = 0; i < tag.length(); i++) {
      int ch = tag.charAt(i);
      switch (ch) {
      case ' ':
      case '\t':
      case '\r':
      case '\n':
      case '(':
      case ')':
        break;

      case ':':
      case '.':
      case '|':
        newTag.append('_');
        break;

      default:
        if (ch >= 'a' && ch <= 'z' ||
            ch >= 'A' && ch <= 'Z' ||
            ch >= '0' && ch <= '9')
          newTag.append((char) ch);
      }
    }
    tag = newTag.toString();

    if (_names.get(tag) == null) {
      _names.put(tag, tag);
      return tag;
    }

    int i = 0;
    while (true) {
      String subname = tag + i;
      if (_names.get(subname) == null) {
        _names.put(subname, subname);
        return subname;
      }

      i++;
    }
  }

  void printExprTest(Expr expr, String element)
    throws Exception
  {
    print("_exprs[" + addExpr(expr) + "].evalBoolean(" + element + 
          ", " + getEnv() + ")");
  }

  public void printExprTest(int exprId, String element)
    throws Exception
  {
    print("_exprs[" + exprId + "].evalBoolean(" + element + 
          ", " + getEnv() + ")");
  }

  private boolean attributeHasSpecial(String string)
  {
    int length = string.length();
    
    for (int i = 0; i < length; i++) {
      char ch = string.charAt(i);

      if (ch == '{' && i + 1 < length) {
        // {{ is treated as a single {
        if (string.charAt(i + 1) == '{') {
          i++;
          continue;
        }

        return true;
      }
      // <#= interpolates
      else if (i + 2 < length && ch == '<' && 
               string.charAt(i + 1) == '#' &&
               string.charAt(i + 2) == '=')
        return true;
    }

    return false;
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
  void generateString(String string, int mode, Element elt)
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
              println("out.print(\"" + cb.toString() + "\");");
          }
          else {
            if (! first)
              print((char) mode);

            if (cb.length() > 0) {
              print("\"");
              print(cb.toString());
              print("\"");
              print((char) mode);
            }
          }

          // scan the contents of '{' ... '}'
          cb.clear();
          for (i++; i < length && string.charAt(i) != '}'; i++)
            cb.append(string.charAt(i));

          // and add the results
          if (mode == ',')
            printStringExpr(cb.toString(), elt);
          else
            stringExpr(cb.toString(), elt);
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
            println("out.print(\"" + cb.toString() + "\");");
        }
        else {
          if (! first)
            print((char) mode);

          if (cb.length() > 0) {
            print("\"");
            print(cb.toString());
            print("\"");
            print((char) mode);
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
          println("out.print(" + cb + ");");
        else {
          print("(" + cb + ")");
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
        println("out.print(\"" + cb + "\");");
      else {
        if (! first)
          print((char) mode);

        print("\"" + cb + "\"");
      }
    } else if (first && mode == '+')
      print("\"\"");
  }

  /**
   * Produces code to generate an attribute value template.  The same
   * code is used to produce a string ('a{b}c' -> "a" + b + "c") or a series of
   * print statements (',').
   *
   * @param string the source template
   * @param elt the containing element.  Needed for namespaces.
   *
   * @return the variable storing the generated string.
   */
  String generateStringVar(String string, Element elt)
    throws Exception
  {
    CharBuffer cb = new CharBuffer();
    int i = 0;
    boolean first = true;
    int length = string.length();

    String strVar = "_xsl_str" + _unique++;

    if (string.indexOf('{') < 0 &&
        string.indexOf('}') < 0) {
      print("String " + strVar + " = \"");
      printString(string);
      println("\";");
      
      return strVar;
    }
    else if (string.lastIndexOf('{') == 0 &&
        string.indexOf('}') == string.length() - 1) {
      println("String " + strVar + " = \"\";");
      string = string.substring(1, string.length() - 1);
      
      addStringExpr(strVar, string, elt, true);
      return strVar;
    }

    
    String cbVar = "_xsl_cb" + _unique++;

    println("com.caucho.util.CharBuffer " + cbVar +
            " = com.caucho.util.CharBuffer.allocate();");

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
          if (cb.length() > 0)
            println(cbVar + ".append(\"" + cb.toString() + "\");");

          // scan the contents of '{' ... '}'
          cb.clear();
          for (i++; i < length && string.charAt(i) != '}'; i++)
            cb.append(string.charAt(i));

          // and add the results
          addStringExpr(cbVar, cb.toString(), elt, false);
          
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
        if (cb.length() > 0)
          println(cbVar + ".append(\"" + cb.toString() + "\");");

        // scan the contents of '<#=' ... '#>'
        cb.clear();
        for (i += 3;
             i + 1 < length && string.charAt(i) != '#' &&
               string.charAt(i + 1) != '>';
             i++)
          cb.append(string.charAt(i));

        i++;

        // and add the results
        println(cbVar + ".append(" + cb + ");");
        cb.clear();
        first = false;
      }
      else
        cb.append((char) ch);
    }

    // add any trailing text
    if (cb.length() > 0)
      println(cbVar + ".append(\"" + cb + "\");");

    println("String " + strVar + " = " + cbVar + ".close();");

    return strVar;
    
  }

  /**
   * Prints a value-of expression
   */
  private void printStringExpr(String exprString, Element elt)
    throws Exception
  {
    int length = exprString.length();
    
    if (length == 0)
      return;

    AbstractPattern select = null;
    try {
      select = parseSelect(exprString);
    } catch (Exception e) {
    }
    
    if (exprString.equals(".")) {
      println("out.valueOf(node);");
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
        println("if (node instanceof Element)");
        print("  out.print(((Element) node).getAttribute(\"");
        print(exprString.substring(1));
        println("\"));");
        return;
      }
    }
    else if (allowJavaSelect(select)) {
      int oldSelectDepth = _selectDepth;

      String loop = "_xsl_loop" + _unique++;
      _selectLoopDepth = 0;
      
      String ptr = printSelectBegin(select, true, loop);

      println("out.valueOf(" + ptr + ");");
      println("break " + loop + ";");

      for (; _selectDepth > oldSelectDepth; _selectDepth--) {
        popDepth();
        println("}");
      }

      return;
    }

    println("out.valueOf(_exprs[" + addExpr(exprString) +
            "].evalObject(node, " + getEnv() + "));");
  }

  /**
   * Prints a value-of expression
   */
  private void addStringExpr(String var, String exprString,
                             Element elt, boolean isSingleString)
    throws Exception
  {
    int length = exprString.length();
    
    if (length == 0)
      return;

    AbstractPattern select = null;
    try {
      select = parseSelect(exprString);
    } catch (Exception e) {
    }
    
    if (exprString.equals(".")) {
      if (isSingleString)
        println(var + " = XmlUtil.textValue(node);");
      else
        println("XmlUtil.textValue(" + var + ", node);");
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
        println("if (node instanceof Element)");
        if (isSingleString) {
          print("  " + var + " = ((Element) node).getAttribute(\"");
          print(exprString.substring(1));
          println("\");");
        }
        else {
          print("  " + var + ".append(((Element) node).getAttribute(\"");
          print(exprString.substring(1));
          println("\"));");
        }
        return;
      }
    }
    else if (allowJavaSelect(select)) {
      int oldSelectDepth = _selectDepth;

      String loopVar = "_xsl_loop" + _unique++;
      _selectLoopDepth = 0;
      
      String ptr = printSelectBegin(select, true, loopVar);

      if (isSingleString)
        println(var + " = XmlUtil.textValue(" + ptr + ");");
      else
        println("XmlUtil.textValue(" + var + ", " + ptr + ");");
      println("break " + loopVar + ";");

      for (; _selectDepth > oldSelectDepth; _selectDepth--) {
        popDepth();
        println("}");
      }

      return;
    }

    if (isSingleString) {
      println(var + " = _exprs[" + addExpr(exprString) +
              "].evalString(node, " + getEnv() + ");");
    }
    else {
      println("_exprs[" + addExpr(exprString) + "].evalString(" +
              var + ", node, " + getEnv() + ");");
    }
  }

  /**
   * Prints iterator code to start a select.
   */
  private String printSelectBegin(AbstractPattern select,
                                  boolean isForEach, String loopVar)
    throws IOException, XslParseException
  {
    if (select == null)
      throw new NullPointerException();
    
    if (select instanceof FromContext &&
        ((FromContext) select).getCount() == 0)
      return "node";

    else if (select instanceof FromRoot)
      return "ownerDocument(node)";

    boolean useXPath = allowJavaSelect(select);
    
    String name = "node";

    if (! useXPath) {
      // punt and let XPath handle it.
      String iterName = "_xsl_iter" + _unique++;
      
      String ptrName = "_xsl_ptr" + _unique++;

      if (isForEach)
        println("env.setCurrentNode(node);");
      println("Iterator " + iterName + " = _select_patterns[" +
              addSelect(select) + "].select(" + name + ", env);");
      
      if (loopVar != null && _selectLoopDepth == 0)
        println(loopVar + ":");
      
      println("while (" + iterName + ".hasNext()) {");
      pushDepth();
      _selectDepth++;
      _selectLoopDepth++;
      println("Node " + ptrName + " = (Node) " + iterName + ".next();");

      return ptrName;
    }

    if (select instanceof FromChildren) {
      name = printSelectBegin(select.getParent(), isForEach, loopVar);
      
      String ptrName = "_xsl_ptr" + _unique++;

      if (loopVar != null && _selectLoopDepth == 0)
        println(loopVar + ":");
      
      println("for (Node " + ptrName + " = " + name + ".getFirstChild();");
      println("     " + ptrName + " != null;");
      println("     " + ptrName + " = " + ptrName + ".getNextSibling()) {");
      pushDepth();
      _selectDepth++;
      _selectLoopDepth++;

      return ptrName;
    }
    else if (select instanceof FromNextSibling) {
      name = printSelectBegin(select.getParent(), isForEach, loopVar);
      
      String ptrName = "_xsl_ptr" + _unique++;
      
      if (loopVar != null && _selectLoopDepth == 0)
        println(loopVar + ":");
      
      println("for (Node " + ptrName + " = " + name + ".getNextSibling();");
      println("     " + ptrName + " != null;");
      println("     " + ptrName + " = " + ptrName + ".getNextSibling()) {");
      pushDepth();
      _selectDepth++;
      _selectLoopDepth++;

      return ptrName;
    }
    else if (select instanceof NodePattern) {
      name = printSelectBegin(select.getParent(), isForEach, loopVar);
      
      NodePattern pat = (NodePattern) select;
      
      println("if (" + name + ".getNodeName() == \"" + pat.getNodeName() + "\" &&");
      println("    " + name + " instanceof Element) {");
      pushDepth();
      _selectDepth++;

      return name;
    }
    else if (select instanceof NodeTypePattern) {
      name = printSelectBegin(select.getParent(), isForEach, loopVar);
      
      NodeTypePattern pat = (NodeTypePattern) select;

      if (pat.getNodeType() >= 0) {
        println("if (" + name + ".getNodeType() == " + pat.getNodeType() + ") {");
        pushDepth();
        _selectDepth++;
      }

      return name;
    }
    else if (select instanceof FilterPattern) {
      String posId = "_xsl_pos" + _unique++;

      println("int " + posId + " = 0;");
      
      name = printSelectBegin(select.getParent(), isForEach, loopVar);

      println(posId + "++;");
      
      FilterPattern pat = (FilterPattern) select;
      Expr expr = pat.getExpr();

      if (expr instanceof NumericExpr) {
        NumericExpr num = (NumericExpr) expr;
        if (num.isConstant()) {
          println("if (" + posId + " > " + (int) num.getValue() + ")");
          println("  break;");
          println("else if (" + posId + " == " + (int) num.getValue() + ") {");
          pushDepth();
          _selectDepth++;

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
  private boolean allowJavaSelect(AbstractPattern select)
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

  private void stringExpr(String exprString, Element element)
    throws Exception, XslParseException
  {
    print("_exprs[" + addExpr(exprString) +
          "].evalString(node, " + getEnv() + ")");
  }

  /**
   * Adds an expression constant returning its index.
   *
   * @param expr the expression to add.
   *
   * @return the index into the runtime expression array
   */
  public int addExpr(Expr expr)
    throws XslParseException
  {
    String exprStr = expr.toString();

    int i = _exprMap.get(exprStr);
    if (i >= 0)
      return i;

    i = _exprs.size();
    _exprMap.put(exprStr, i);
    _exprs.add(expr);
    
    return i;
  }

  /**
   * Adds an expression constant returning its index.
   *
   * @param exprString the expression to add.
   *
   * @return the index into the runtime expression array
   */
  public int addExpr(String exprString)
    throws XslParseException
  {
    int i = _exprMap.get(exprString);

    if (i >= 0)
      return i;
    
    Expr expr = parseExpr(exprString);
    i = _exprs.size();
    _exprs.add(expr);

    _exprMap.put(exprString, i);

    return i;
  }

  /**
   * Adds a select pattern returning its index.
   *
   * @param select the select pattern to add.
   *
   * @return the index into the runtime expression array
   */
  public int addSelect(AbstractPattern select)
    throws IOException, XslParseException
  {
    String selectStr = select.toString();

    int i = _selectMap.get(selectStr);
    if (i >= 0)
      return i;

    i = _selectPatterns.size();
    _selectMap.put(selectStr, i);
    _selectPatterns.add(select);
    
    return i;
  }

  /**
   * Adds a select pattern, returning its index.
   *
   * @param selectString the expression to add.
   *
   * @return the index into the runtime select pattern array
   */
  public int addSelect(String selectString)
    throws IOException, XslParseException
  {
    int i = _selectMap.get(selectString);

    if (i >= 0)
      return i;
    
    AbstractPattern select = parseSelect(selectString);
    i = _selectPatterns.size();
    _selectPatterns.add(select);

    _selectMap.put(selectString, i);

    return i;
  }

  /**
   * Adds a match pattern, returning its index.
   *
   * @param pattern the expression to add.
   *
   * @return the index into the runtime expression array
   */
  public int addMatch(AbstractPattern pattern)
    throws XslParseException
  {
    int index = _matchPatterns.size();

    _matchPatterns.add(pattern);

    return index;
  }

  protected StylesheetImpl completeGenerate(ArrayList<XslNode> inits,
                                            ArrayList globals)
    throws Exception
  {
    printTemplates();
    printMacros();

    printInitVars(inits);
    printFragments();

    printInit();
    
    printStrings();
    printExpressions();
    printPatterns();

    popDepth();
    println("}");
    _s.close();
    _s = null;

    /*
    if (dbg.canWrite()) {
      ReadStream is = path.openRead();
      dbg.writeStream(is);
      is.close();
    }
    */
    
    if (_parentLoader instanceof DynamicClassLoader)
      ((DynamicClassLoader) _parentLoader).make();
    
    _compiler.compile(_path.getPath(), _lineMap);

    StylesheetImpl stylesheet;

    stylesheet = (StylesheetImpl) _xslGenerator.loadStylesheet(_path.getFullPath(),
                                                               _pkg + "." + _className);
    //if (stylesheet != null)
    //  stylesheet.init(context);

    return stylesheet;
  }

  private long getLastModified()
  {
    long lastModified = 0;
    for (int i = 0; i < _depends.size(); i++) {
      Path path = _depends.get(i);
      if (path.getLastModified() > lastModified)
        lastModified = path.getLastModified();
    }

    return lastModified;
  }

  /**
   * Generate code executed for all transformations.
   * <ul>
   * <li>Add the stylesheet namespaces to the generated document.
   * <li>Assign the global variables.
   * <li>Initialize the cache dependencies.
   */
  protected void printInitVars(ArrayList<XslNode> inits)
    throws Exception
  {
    println("private void _xsl_init_vars(XslWriter out, Node node, Env env)");
    println("  throws Exception");
    println("{");
    pushDepth();

    // Add the stylesheet namespaces to the generated document.
    HashMap namespaces = _qDoc.getNamespaces();
    if (namespaces != null) {
      Iterator prefixes = namespaces.keySet().iterator();
      while (prefixes.hasNext()) {
        String prefix = (String) prefixes.next();
        String url = (String) namespaces.get(prefix);

        if (url.startsWith("http://www.w3.org/XSL/Transform/") ||
            url.startsWith("http://www.w3.org/1999/XSL/Transform") ||
            url.startsWith("http://www.w3.org/XML/2000/xmlns") ||
            url.startsWith("http://www.w3.org/2000/xmlns") ||
            url.equals(XTPNS))
          continue;
        else if (_excludedNamespaces.get(url) != null)
          continue;
        else if (_namespaceAliases.get(url) != null)
          continue;

        if (prefix == null)
          println("out.addNamespace(\"\", \"" + url + "\");");
        else
          println("out.addNamespace(\"" + prefix + "\", \"" + url + "\");");
      }
    }

    // Initialize the global stylesheet variables
    println("Object _xsl_tmp;");
    for (int i = 0; i < inits.size(); i++) {
      XslNode node = inits.get(i);
      // NamespaceContext oldNamespace = addNamespace(elt);

      node.generate(getOut());

      /*
      if ("variable".equals(getXslLocal(elt)) ||
          "assign".equals(getXslLocal(elt))) {
        String name = elt.getAttribute("name");
        String expr = elt.getAttribute("select");
        print("env.setGlobal(\"" + name + "\", ");
        if (! expr.equals(""))
          printVariableValue(parseExpr(expr));
        else
          printVariableValue(elt);
        println(");");
      }
      else if ("param".equals(getXslLocal(elt))) {
        String name = elt.getAttribute("name");
        String expr = elt.getAttribute("select");
        print("env.setGlobal(\"" + name + "\", ");
        if (! expr.equals(""))
          printVariableValue(parseExpr(expr));
        else
          printVariableValue(elt);
        println(");");
         
        println("_xsl_tmp = out.getParameter(\"" + name + "\");");
        println("if (_xsl_tmp != null)");
        println("  env.setGlobal(\"" + name + "\", _xsl_tmp);");
      }
      */
      
      // oldNamespace = _namespace;
    }

    // Initialize the cache dependencies.
    println("com.caucho.vfs.Path pwd;");
    println("pwd = (com.caucho.vfs.Path) out.getProperty(\"caucho.pwd\");");
    for (int i = 0; i < _cacheDepends.size(); i++) {
      String depend = (String) _cacheDepends.get(i);

      print("out.addCacheDepend(pwd.lookup(\"");
      printString(depend);
      println("\"));");
    }
    
    popDepth();
    println("}");
  }

  protected void printInit()
    throws Exception
  {
    println("protected void _xsl_init(XslWriter out, Node node, Env env)");
    println("  throws Exception");
    println("{");
    pushDepth();
    println("Object _xsl_tmp;");
    println("_xsl_init_vars(out, node, env);");

    // Generic init vars
    // println("templates = _staticTemplates;");

    for (int i = 0; _globalActions != null && i < _globalActions.size(); i++) {
      QAbstractNode node = (QAbstractNode) _globalActions.get(i);
      generateChild(node);
    }

    popDepth();
    println("}");

    // depends
    println("public boolean isModified()");
    println("{");
    pushDepth();
    println("return com.caucho.server.util.CauchoSystem.getVersionId() != " +
            CauchoSystem.getVersionId() + "L ||");
    println("       super.isModified();");
    popDepth();
    println("}");

    println("public void init(com.caucho.vfs.Path path)");
    println("  throws Exception");
    println("{");
    pushDepth();
    println("super.init(path);");
    println("com.caucho.vfs.Path pwd = path.getParent();");

    for (int i = 0; i < _depends.size(); i++) {
      Path path = _depends.get(i);
      
      if (path.canRead() && ! path.isDirectory()) {
        Depend depend = new Depend(path);
 
        print("addDepend(new com.caucho.vfs.Depend(pwd.lookup(\"");
        printString(path.getRelativePath());
        println("\"), " + depend.getDigest() + "L));");
      }
    }

    println("stylesheets = new StylesheetEnv[" + _stylesheets.size() + "];");
    println("StylesheetEnv env;");
    
    for (int i = 0; i < _stylesheets.size(); i++) {
      String ss = _stylesheets.get(i);

      println("env = new StylesheetEnv();");
      println("stylesheets[" + i + "] = env;");
      print("env.setPath(pwd.lookup(\"");
      printString(ss);
      println("\"));");
    }

    if (! _strip.isEmpty()) {
      println("HashMap preserve = new HashMap();");
      println("HashMap preservePrefix = new HashMap();");
      Iterator iter = _preserve.keySet().iterator();
      while (iter.hasNext()) {
        String key = (String) iter.next();
        if (key.endsWith(":*")) {
          String prefix = key.substring(0, key.length() - 2);
          println("preservePrefix.put(\"" + prefix + "\", \"true\");");
        }
        else
          println("preserve.put(\"" + key + "\", \"true\");");
      }
      println("HashMap strip = new HashMap();");
      println("HashMap stripPrefix = new HashMap();");
      iter = _strip.keySet().iterator();
      while (iter.hasNext()) {
        String key = (String) iter.next();
        if (key.endsWith(":*")) {
          String prefix = key.substring(0, key.length() - 2);
          println("stripPrefix.put(\"" + prefix + "\", \"true\");");
        }
        else
          println("strip.put(\"" + key + "\", \"true\");");
      }
      println("setSpaces(preserve, preservePrefix, strip, stripPrefix);");
    }

    printOutput();

    if (_errorPage != null) {
      print("setProperty(\"caucho.error.page\", \"");
      printString(_errorPage);
      println("\");");
    }

    if (_globalParameters != null && _globalParameters.size() > 0) {
      println("ArrayList params = new ArrayList();");
      for (int i = 0; i < _globalParameters.size(); i++) {
        String param = _globalParameters.get(i);

        println("params.add(\"" + param + "\");");
      }
      print("setProperty(\"caucho.global.param\", params);");
    }
    
    String disable = null;
    /*
    if (_outputAttributes != null)
      disable = (String) _outputAttributes.get("disable-output-escaping");
    if (disable != null && ! disable.equals("no") && ! disable.equals("false"))
      println("defaultDisableEscaping = true;");
    */
    
    if (_isRawText)
      println("_defaultDisableEscaping = true;");

    printNamespaces();
    printFunctions();
    printSorts();
    printFormats();
    
    popDepth();
    println("}");
  }

  /**
   * Sets the property for the xsl:output keys.
   */
  private void printOutput() throws Exception
  {
    Iterator iter = _outputAttributes.keySet().iterator();

    if (_outputAttributes.get("encoding") == null)
      println("_output.put(\"encoding\", \"utf-8\");");
    
    while (iter.hasNext()) {
      String key = (String) iter.next();
      String value = (String) _outputAttributes.get(key);

      println("_output.put(\"" + key + "\", \"" + value + "\");");
    }
  }

  private void printSorts() throws Exception
  {
    if (_sorts.size() == 0)
      return;

    println();
    println("_xsl_sorts = new com.caucho.xsl.Sort[][] { ");
    pushDepth();

    for (int i = 0; i < _sorts.size(); i++) {
      Sort []sorts = _sorts.get(i);

      print("new com.caucho.xsl.Sort[] {");
      
      for (int j = 0; j < sorts.length; j++) {
        Sort sort = sorts[j];

        Expr lang = sort.getLang();
        Expr caseOrder = sort.getCaseOrder();

        if (lang != null || caseOrder != null) {
          print("new com.caucho.xsl.Sort(\"" + sort.getExpr() + "\", " +
                "\"" + sort.getAscending() + "\", " +
                (lang == null ? "null, " : "\"" + lang + "\", ") +
                (caseOrder == null ? "null), " : "\"" + caseOrder + "\"), "));
        }
        else
          print("new com.caucho.xsl.Sort(\"" + sort.getExpr() + "\", " +
                "\"" + sort.getAscending() + "\", " +
                sort.isText() + "), ");
      }
      
      println("},");
    }
    popDepth();
    println("};");
  }

  private void printLocale(Locale locale) throws Exception
  {
    String language = locale.getLanguage();
    String country = locale.getCountry();
    String variant = locale.getVariant();

    if (variant != null && country != null) {
      print("new java.util.Locale(\"" + language + "\", " +
            "\"" + country + "\", \"" + variant + "\")");
    }
    else if (country != null) {
      print("new java.util.Locale(\"" + language + "\", " +
            "\"" + country + "\")");
    }
    else {
      print("new java.util.Locale(\"" + language + "\")");
    }
  }

  private void printFormats() throws Exception
  {
    if (_formats.size() == 0)
      return;

    println();
    println("_xsl_formats = new XslNumberFormat[] { ");
    pushDepth();

    for (int i = 0; i < _formats.size(); i++) {
      XslNumberFormat format = (XslNumberFormat) _formats.get(i);

      println("new XslNumberFormat(\"" + format.getFormat() + "\", \"" +
              format.getLang() + "\", " + format.isAlphabetic() + ", \"" +
              format.getGroupSeparator() + "\", " +
              format.getGroupSize() + "),");
    }
    popDepth();
    println("};");
  }

  private void printNamespaces() throws Exception
  {
    if (_namespaces.size() == 0)
      return;

    println();
    println("_namespaces = new NamespaceContext[] { ");
    pushDepth();

    for (int i = 0; i < _namespaces.size(); i++) {
      NamespaceContext ns = _namespaces.get(i);

      printNamespaceDef(ns);
      println(",");
    }
    popDepth();
    println("};");
  }

  private void printNamespaceDef(NamespaceContext ns) throws Exception
  {
    if (ns == null) {
      print("null");
      return;
    }

    print("new NamespaceContext(");
    printNamespaceDef(ns.getPrev());
    print(", \"" + ns.getPrefix() + "\", \"" + ns.getUrl() + "\")"); 
  }

  private void printFunctions() throws Exception
  {
    println();
    println("com.caucho.xsl.fun.KeyFun keyFun = new com.caucho.xsl.fun.KeyFun();");
    HashMap keys = _keyFun.getKeys();
    Iterator iter = keys.keySet().iterator();
    while (iter.hasNext()) {
      String name = (String) iter.next();
      KeyFun.Key key = (KeyFun.Key) keys.get(name);

      println("keyFun.add(\"" + name + "\", XPath.parseMatch(\"" +
              key.getMatch() + "\").getPattern(), XPath.parseExpr(\"" +
              key.getUse() + "\"));");
    }
    println("addFunction(\"key\", keyFun);");
    
    println();
    println("com.caucho.xsl.fun.FormatNumberFun formatFun = new com.caucho.xsl.fun.FormatNumberFun();");
    println("java.text.DecimalFormatSymbols symbols;");
    JavaWriter out = _out;

    HashMap locales = _formatNumberFun.getLocales();
    iter = locales.keySet().iterator();
    while (iter.hasNext()) {
      String name = (String) iter.next();
      DecimalFormatSymbols symbols = (DecimalFormatSymbols) locales.get(name);

      out.println("symbols = new java.text.DecimalFormatSymbols();");
      
      out.print("symbols.setDecimalSeparator(\'");
      out.printJavaChar(symbols.getDecimalSeparator());
      out.println("\');");
      
      out.print("symbols.setGroupingSeparator(\'");
      out.printJavaChar(symbols.getGroupingSeparator());
      out.println("\');");
      
      out.print("symbols.setInfinity(\"");
      out.printJavaString(symbols.getInfinity());
      out.println("\");");
      
      out.print("symbols.setMinusSign(\'");
      out.printJavaChar(symbols.getMinusSign());
      out.println("\');");
      
      out.print("symbols.setNaN(\"");
      out.printJavaString(symbols.getNaN());
      out.println("\");");
      
      out.print("symbols.setPercent(\'");
      out.printJavaChar(symbols.getPercent());
      out.println("\');");
      
      out.print("symbols.setPerMill(\'");
      out.printJavaChar(symbols.getPerMill());
      out.println("\');");
      
      out.print("symbols.setZeroDigit(\'");
      out.printJavaChar(symbols.getZeroDigit());
      out.println("\');");
      
      out.print("symbols.setDigit(\'");
      out.printJavaChar(symbols.getDigit());
      out.println("\');");
      
      out.print("symbols.setPatternSeparator(\'");
      out.printJavaChar(symbols.getPatternSeparator());
      out.println("\');");

      println("formatFun.addLocale(\"" + name + "\", symbols);");
    }
    
    println("addFunction(\"format-number\", formatFun);");
  }

  private void printMacros() throws Exception
  {
    /*
    for (int i = 0; i < _macros.size(); i++) {
      Macro macro = _macros.get(i);

      println("void " + getMacroName(macro.getName()) +
              "(XslWriter out, Node inputNode, Env env)");
      println("  throws Exception");
      println("{");
      pushDepth();
      println("Object _xsl_tmp;");
      println("Node node = inputNode;");
      generateChildren(macro.getElement());
      popDepth();
      println("}");
    }
    */
  }

  private void printFragments() throws Exception
  {
    for (int i = 0; i < _fragments.size(); i++) {
      Element elt = (Element) _fragments.get(i);

      println("Object _xsl_fragment" + i +
              "(XslWriter out, Node inputNode, Env env )");
      println("  throws Exception");
      println("{");
      pushDepth();
      println("Object _xsl_tmp;");
      println("Node node = inputNode;");
      println("XMLWriter _xsl_frag = out.pushFragment();");
      generateChildren(elt);
      println("return out.popFragment(_xsl_frag);");
      popDepth();
      println("}");
    }
  }

  /**
   * Prints the template definitions, i.e. the set of XPath
   * match patterns to test a node.
   */
  private void printTemplates() throws Exception
  {
    for (int j = 0; j < _modes.size(); j++) {
      String mode = _modes.get(j);
      String modeName = getModeName(mode);

      printApplyNode(mode);

      println();
      println("static HashMap _static_templates" + modeName + ";");
      println();
      println("static {");
      pushDepth();
      println("_static_templates" + modeName + " = new HashMap();");
      println("Template []values;");

      println("try {");
      pushDepth();

      ArrayList defaultTemplateList = (ArrayList) _templates.get("*");
      if (defaultTemplateList == null)
        defaultTemplateList = new ArrayList();
      println("Template []star = new Template[] {");
      pushDepth();
      for (int i = 0; i < defaultTemplateList.size(); i++) {
        Template template = (Template) defaultTemplateList.get(i);
        
        if (template.getMode().equals(mode)) {
          printTemplate(template);
          println(",");
        }
      }
      popDepth();
      println("};");
      println();
      println("_static_templates" + modeName + ".put(\"*\", star);");

      int count = _templates.size();

      for (int i = 0; i < count; i += 64)
        println("_init_templates" + modeName + "_" + i + "(star);");

      popDepth();
      println("} catch (Exception e) {");
      println("  e.printStackTrace();");
      println("}");
    
      popDepth();
      println("}");

      for (int i = 0; i < count; i += 64)
        printTemplateInitFun(mode, i, 64, defaultTemplateList);
    }
  }
  
  /**
   * Prints a function to initialize some of the templates.
   */
  private void printApplyNode(String mode)
    throws Exception
  {
    String modeName = getModeName(mode);
    
    print("protected void applyNode" + modeName);
    println("(XslWriter out, Node node, Env env, int _xsl_min, int _xsl_max)");
    println("  throws Exception");
    println("{");
    pushDepth();
    println("Object _xsl_tmp;");
    println();
    println("switch (getTemplateId(_static_templates" + modeName + ", " +
            "node, env, _xsl_min, _xsl_max)) {");
    // XXX: name issue below functions/templateList
    for (int i = 0; i < _functions.size(); i++) {
      Template template = (Template) _templateList.get(i);

      if (template == null || ! template.getMode().equals(mode))
        continue;
      
      println("case " + (i + 1) + ":");
      println("  " + _functions.get(i) + "(out, node, env);");
      println("  break;");
    }
    println("default:");
    println("  switch (node.getNodeType()) {");
    println("  case Node.ELEMENT_NODE:");
    println("  case Node.DOCUMENT_NODE:");
    println("  case Node.DOCUMENT_FRAGMENT_NODE:");
    println("    env.setSelect(node, null);");
    println("    for (Node child = node.getFirstChild();");
    println("         child != null;");
    println("         child = child.getNextSibling()) {");
    println("      env.setCurrentNode(child);");
    println("      applyNode" + modeName + "(out, child, env, 0, " + Integer.MAX_VALUE + ");");
    println("    }");

    println("    break;");
    println("  default:");
    println("    applyNodeDefault(out, node, env);");
    println("    break;");
    println("  }");
    println("  break;");
    println("}");
    popDepth();
    println("}");
  }

  /**
   * Prints a function to initialize some of the templates.
   */
  private void printTemplateInitFun(String mode, int offset, int length,
                                    ArrayList defaultTemplateList)
    throws Exception
  {
    String modeName = getModeName(mode);
                                  
    println("private static void _init_templates" + modeName + "_" + offset + "(Template []star)");
    println("  throws Exception");
    println("{");
    pushDepth();

    Iterator<String> iter = _templates.keySet().iterator();
    while (iter.hasNext() && length > 0) {
      String name = iter.next();
      
      if (name.equals("*"))
        continue;

      ArrayList templateList = (ArrayList) _templates.get(name);
      
      if (modeTemplateCount(mode, templateList) == 0)
        continue;

      if (offset > 0) {
        offset--;
        continue;
      }

      println("_static_templates" + modeName + ".put(\"" + name + "\", ");
      println("  mergeTemplates(star, new Template[] {");
      pushDepth();
      pushDepth();
      
      for (int i = 0; i < templateList.size(); i++) {
        Template template = (Template) templateList.get(i);

        if (template.getMode().equals(mode)) {
          printTemplate(template);
          println(",");
        }
      }

      popDepth();
      popDepth();
      println("}));");

      length--;
    }

    popDepth();
    println("}");
  }

  /**
   * Returns true if the template list contains a template with the given
   * mode.
   */
  private int modeTemplateCount(String mode, ArrayList templateList)
  {
    int count = 0;
    
    for (int i = 0; i < templateList.size(); i++) {
      Template template = (Template) templateList.get(i);

      if (template.getMode().equals(mode))
        count++;
    }

    return count;
  }

  /**
   * Prints initialization code for a single template.
   */
  private void printTemplate(Template template)
    throws IOException
  {
    print("new Template(");
    AbstractPattern pattern = template.getPattern();
    print("XPath.parseMatch(\"" +
          template.getPattern().toPatternString() + "\").getPattern(), ");
    print("\"" + template.getMode() + "\", ");
    print(template.getMin() + ", ");
    print(template.getMax() + ", ");
    print(template.getPriority() + ", ");
    print(template.getCount() + ", ");
    print("\"" + template.getFunction() + "\", ");
    print("" + template.getId() + ")");
  }

  /**
   * Prints the constant strings.
   */
  private void printStrings() throws Exception
  {
    for (int j = 0; j < _strings.size(); j++) {
      String text = (String) _strings.get(j);
    
      print("static char[] _xsl_string" + j + " = \"");

      printString(text);

      println("\".toCharArray();");
    }
  }

  /**
   * Prints the precompiled XPath expressions as static variables.
   */
  private void printExpressions() throws Exception
  {
    if (_exprs.size() == 0)
      return;
    
    println("private static Expr []_exprs;");
    println("static {");
    pushDepth();
    println("try {");
    pushDepth();
    
    println("_exprs = new Expr[] { ");
    pushDepth();

    for (int i = 0; i < _exprs.size(); i++) {
      Expr expr = _exprs.get(i);

      println("XPath.parseExpr(\"" + expr + "\"),");

      // System.out.println("EXPR: " + expr + " " + expr.getListContext());
      /*
      if (expr.getListContext() == null) // || currentPos != null)
        println("XPath.parseExpr(\"" + expr + "\"),");
      else {
        print("XPath.parseExpr(\"" + expr + "\", null,");
        println("XPath.parseMatch(\"" + expr.getListContext() +"\").getPattern()),");
      }
      */

    }
    popDepth();
    println("};");

    popDepth();
    println("} catch (Exception e) {");
    println("  e.printStackTrace();");
    println("}");
    
    popDepth();
    println("}");
  }

  /**
   * Prints the precompiled XPath select and match patterns as static
   * variables.
   */
  private void printPatterns() throws Exception
  {
    if (_selectPatterns.size() == 0 && _matchPatterns.size() == 0)
      return;
    
    println("private static com.caucho.xpath.pattern.AbstractPattern []_select_patterns;");
    println("private static com.caucho.xpath.pattern.AbstractPattern []_match_patterns;");
    println("static {");
    pushDepth();
    println("try {");
    pushDepth();
    
    println("_select_patterns = new com.caucho.xpath.pattern.AbstractPattern[] { ");
    pushDepth();

    for (int i = 0; i < _selectPatterns.size(); i++) {
      AbstractPattern pattern = _selectPatterns.get(i);

      println("XPath.parseSelect(\"" + pattern + "\").getPattern(),");
    }
    popDepth();
    println("};");
    
    println("_match_patterns = new com.caucho.xpath.pattern.AbstractPattern[] { ");
    pushDepth();

    for (int i = 0; i < _matchPatterns.size(); i++) {
      AbstractPattern pattern = _matchPatterns.get(i);

      println("XPath.parseMatch(\"" + pattern + "\").getPattern(),");
    }
    popDepth();
    println("};");

    popDepth();
    println("} catch (Exception e) {");
    println("  e.printStackTrace();");
    println("}");
    
    popDepth();
    println("}");
  }

  private boolean isSingleStylesheet()
  {
    // return stylesheets.size() < 2;
    return false;
  }

  /**
   * Prints a character to the generated Java.
   */
  private void print(char ch)
    throws IOException
  {
    _out.print(ch);
  }

  /**
   * Prints a string to the generated Java.
   */
  private void print(String string)
    throws IOException
  {
    _out.print(string);
  }

  /**
   * Prints an integer to the generated Java.
   */
  private void print(int i)
    throws IOException
  {
    _out.print(i);
  }

  /**
   * Prints a new line.
   */
  private void println()
    throws IOException
  {
    _out.println();
  }

  private void println(char ch)
    throws IOException
  {
    _out.println(ch);
  }

  private void println(String s)
    throws IOException
  {
    _out.println(s);
  }

  /**
   * Pushes the pretty-printed depth of the generated java.
   */
  private void pushDepth()
    throws IOException
  {
    _out.pushDepth();
  }

  /**
   * Pops the pretty-printed depth of the generated java.
   */
  private void popDepth()
    throws IOException
  {
    _out.popDepth();
  }

  /**
   * Prints the contents of a string, taking care of escapes.
   */
  protected void printString(String str) throws IOException
  {
    _out.printJavaString(str);
  }

  /**
   * Returns the name of the applyNode method.
   *
   * @param mode the template's mode.
   */
  public String getModeName(String mode)
  {
    if (mode != null && ! _modes.contains(mode))
      _modes.add(mode);

    if (mode == null || mode.equals(""))
      return "";
    else
      return "_" + toJavaIdentifier(mode);
  }

  public void addMode(String mode)
  {
    if (! _modes.contains(mode))
      _modes.add(mode);
  }

  public int addStylesheet(String filename)
  {
    int pos = _stylesheets.indexOf(filename);
    
    if (pos < 0) {
      pos = _stylesheets.size();
      _stylesheets.add(filename);
    }

    return pos;
  }

  /**
   * Converts a string to a Java identifier, encoding unknown characters
   * as "_"
   */
  public String toJavaIdentifier(String name)
  {
    CharBuffer cb = new CharBuffer();

    char ch = name.charAt(0);
    if (Character.isJavaIdentifierStart(ch))
      cb.append(ch);
    else
      cb.append("_");

    for (int i = 1; i < name.length(); i++) {
      ch = name.charAt(i);
      
      if (Character.isJavaIdentifierPart(ch))
        cb.append(ch);
      else {
        cb.append("_");
        cb.append((char) ((ch & 0xf) + 'a'));
        cb.append((char) ((ch / 16 & 0xf) + 'a'));
      }
    }

    return cb.toString();
  }

  /**
   * Close call when an error occurs.
   */
  public void close()
    throws IOException
  {
    if (_s != null)
      _s.close();
  }
  
  static class Macro {
    String _name;
    Element _elt;

    Macro(String name, Element elt)
    {
      _name = name;
      _elt = elt;
    }

    public Element getElement()
    {
      return _elt;
    }
    
    public String getName()
    {
      return _name;
    }
  }

  static {
    _tagMap = new HashMap<QName,Class>();
    
    _tagMap.put(new QName("xsl", "attribute", XSLNS), XslAttribute.class);
    _tagMap.put(new QName("xsl", "attribute-set", XSLNS),
                XslAttributeSet.class);
    _tagMap.put(new QName("xsl", "apply-imports", XSLNS),
                XslApplyImports.class);
    _tagMap.put(new QName("xsl", "apply-templates", XSLNS),
                XslApplyTemplates.class);
    _tagMap.put(new QName("xsl", "call-template", XSLNS), XslCallTemplate.class);
    _tagMap.put(new QName("xsl", "choose", XSLNS), XslChoose.class);
    _tagMap.put(new QName("xsl", "comment", XSLNS), XslComment.class);
    _tagMap.put(new QName("xsl", "copy", XSLNS), XslCopy.class);
    _tagMap.put(new QName("xsl", "copy-of", XSLNS), XslCopyOf.class);
    _tagMap.put(new QName("xsl", "decimal-format", XSLNS),
                XslDecimalFormat.class);
    _tagMap.put(new QName("xsl", "element", XSLNS), XslElement.class);
    _tagMap.put(new QName("xsl", "for-each", XSLNS), XslForEach.class);
    _tagMap.put(new QName("xsl", "if", XSLNS), XslIf.class);
    _tagMap.put(new QName("xsl", "import", XSLNS), XslImport.class);
    _tagMap.put(new QName("xsl", "include", XSLNS), XslInclude.class);
    _tagMap.put(new QName("xsl", "key", XSLNS), XslKey.class);
    _tagMap.put(new QName("xsl", "message", XSLNS), XslMessage.class);
    _tagMap.put(new QName("xsl", "namespace-alias", XSLNS),
                XslNamespaceAlias.class);
    _tagMap.put(new QName("xsl", "number", XSLNS), XslNumber.class);
    _tagMap.put(new QName("xsl", "otherwise", XSLNS), XslOtherwise.class);
    _tagMap.put(new QName("xsl", "output", XSLNS), XslOutput.class);
    _tagMap.put(new QName("xsl", "param", XSLNS), XslParam.class);
    _tagMap.put(new QName("xsl", "processing-instruction", XSLNS),
                XslProcessingInstruction.class);
    _tagMap.put(new QName("xsl", "sort", XSLNS), XslSort.class);
    _tagMap.put(new QName("xsl", "stylesheet", XSLNS), XslStylesheet.class);
    _tagMap.put(new QName("xsl", "text", XSLNS), XslText.class);
    _tagMap.put(new QName("xsl", "transform", XSLNS), XslTransform.class);
    _tagMap.put(new QName("xsl", "value-of", XSLNS), XslValueOf.class);
    _tagMap.put(new QName("xsl", "variable", XSLNS), XslVariable.class);
    _tagMap.put(new QName("xsl", "when", XSLNS), XslWhen.class);
    _tagMap.put(new QName("xsl", "with-param", XSLNS), XslWithParam.class);
    
    _tagMap.put(new QName("xsl", "template", XSLNS),
                XslTemplate.class);
    _tagMap.put(new QName("xsl", "strip-space", XSLNS),
                XslStripSpace.class);
    _tagMap.put(new QName("xsl", "preserve-space", XSLNS),
                XslPreserveSpace.class);
    _tagMap.put(new QName("xsl", "result-document", XSLNS),
                XslResultDocument.class);
    
    _tagMap.put(new QName("xtp", "expression", XTPNS),
                XtpExpression.class);
    _tagMap.put(new QName("xtp:expression", null), XtpExpression.class);
    _tagMap.put(new QName("xtp", "eval", XTPNS), XtpExpression.class);
    _tagMap.put(new QName("xtp:eval", null), XtpExpression.class);
    _tagMap.put(new QName("xtp", "expr", XTPNS), XtpExpression.class);
    _tagMap.put(new QName("xtp:expr", null), XtpExpression.class);
    _tagMap.put(new QName("xtp", "scriptlet", XTPNS),
                XtpScriptlet.class);
    _tagMap.put(new QName("xtp:scriptlet", null), XtpScriptlet.class);
    _tagMap.put(new QName("xtp", "declaration", XTPNS),
                XtpDeclaration.class);
    _tagMap.put(new QName("xtp", "decl", XTPNS),
                XtpDeclaration.class);
    _tagMap.put(new QName("xtp:declaration", null), XtpDeclaration.class);
    _tagMap.put(new QName("xtp:decl", null), XtpDeclaration.class);
    _tagMap.put(new QName("xtp:directive.page", null), XtpDirectivePage.class);
    _tagMap.put(new QName("xtp", "directive.page", XTPNS),
                XtpDirectivePage.class);
    _tagMap.put(new QName("xtp:directive.cache", null), XtpDirectiveCache.class);
    _tagMap.put(new QName("xtp", "directive.cache", XTPNS),
                XtpDirectiveCache.class);
    _tagMap.put(new QName("xtp:assign", null), XslVariable.class);
    _tagMap.put(new QName("xtp", "assign", XTPNS),
                XslVariable.class);
  }
}
