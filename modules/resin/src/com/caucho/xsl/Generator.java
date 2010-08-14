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

import com.caucho.java.JavaWriter;
import com.caucho.java.LineMap;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharScanner;
import com.caucho.util.IntArray;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.util.StringCharCursor;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.xml.CauchoDocument;
import com.caucho.xml.QAbstractNode;
import com.caucho.xml.QElement;
import com.caucho.xml.QName;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlChar;
import com.caucho.xpath.Expr;
import com.caucho.xpath.NamespaceContext;
import com.caucho.xpath.XPath;
import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xpath.pattern.UnionPattern;
import com.caucho.xsl.fun.FormatNumberFun;
import com.caucho.xsl.fun.KeyFun;
import com.caucho.xsl.java.XslAttributeSet;
import com.caucho.xsl.java.XslNode;
import com.caucho.xsl.java.XslStylesheet;
import com.caucho.xsl.java.XslTemplate;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for generating code from an XSL tree.  JavaGenerator and
 * JavaScriptGenerator extend this class for language-specific code
 * generation.
 */
abstract class Generator {
  private static final Logger log
    = Logger.getLogger(Generator.class.getName());
  protected static final L10N L = new L10N(Generator.class);

  public static final String XSLNS = "http://www.w3.org/1999/XSL/Transform";
  public static final String XTPNS = "http://www.caucho.com/XTP/1.0";

  private static final int STYLESHEET = 0;
  private static final int OUTPUT = STYLESHEET + 1;
  private static final int IMPORT = OUTPUT + 1;
  private static final int INCLUDE = IMPORT + 1;
  private static final int TEMPLATE = INCLUDE + 1;
  private static final int STRIP_SPACE = TEMPLATE + 1;
  private static final int PRESERVE_SPACE = STRIP_SPACE + 1;
  private static final int KEY = PRESERVE_SPACE + 1;
  private static final int LOCALE = KEY + 1;
  private static final int ATTRIBUTE_SET = LOCALE + 1;
  private static final int NAMESPACE_ALIAS = ATTRIBUTE_SET + 1;

  private static final int APPLY_TEMPLATES = NAMESPACE_ALIAS + 1;
  private static final int APPLY_IMPORTS = APPLY_TEMPLATES + 1;
  private static final int CALL_TEMPLATE = APPLY_IMPORTS + 1;
  private static final int PARAM = CALL_TEMPLATE + 1;
  private static final int VARIABLE = PARAM + 1;
  private static final int VALUE_OF = VARIABLE + 1;
  private static final int COPY_OF = VALUE_OF + 1;
  private static final int FOR_EACH = COPY_OF + 1;
  private static final int IF = FOR_EACH + 1;
  private static final int CHOOSE = IF + 1;

  private static final int TEXT = CHOOSE + 1;
  private static final int XSL_TEXT = TEXT + 1;
  private static final int NUMBER = XSL_TEXT + 1;
  private static final int COPY = NUMBER + 1;
  private static final int COPY_ELEMENT = COPY + 1;
  private static final int ELEMENT = COPY_ELEMENT + 1;
  private static final int ATTRIBUTE = ELEMENT + 1;
  private static final int PI = ATTRIBUTE + 1;
  private static final int COMMENT = PI + 1;

  private static final int MESSAGE = COMMENT + 1;

  private static final int EXPRESSION = MESSAGE + 1;
  private static final int SCRIPTLET = EXPRESSION + 1;
  private static final int DECLARATION = SCRIPTLET + 1;
  private static final int DIRECTIVE_CACHE = DECLARATION + 1;
  private static final int DIRECTIVE_PAGE = DIRECTIVE_CACHE + 1;
  private static final int WHILE = DIRECTIVE_PAGE + 1;

  private static final int ASSIGN = WHILE + 1;
  private static final int IGNORE = ASSIGN + 1;

  // xslt 2.0
  private static final int RESULT_DOCUMENT = IGNORE + 1;

  private static IntMap _tags;
  private static IntMap _xtpTags;
  private String _version = "1.0";

  String _xslName;
  // the root context
  Path _topContext;
  // the pwd for the file
  Path _baseURL;
  Path _context;
  CharBuffer _text;
  HashMap<String,String> _names = new HashMap<String,String>();
  int _loopDepth;
  Path _workPath;
  int _uniqueId;

  protected HashMap<String,String> _preserve = new HashMap<String,String>();
  protected HashMap<String,String> _strip = new HashMap<String,String>();

  HashMap<String,XslAttributeSet> _attributeSets =
    new HashMap<String,XslAttributeSet>();

  protected HashMap<String,String[]> _namespaceAliases =
    new HashMap<String,String[]>();
  protected HashMap<String,String> _excludedNamespaces =
    new HashMap<String,String>();
  protected KeyFun _keyFun;
  protected FormatNumberFun _formatNumberFun;

  protected NamespaceContext _namespace;
  protected ArrayList _globalActions = new ArrayList();
  protected ArrayList<String> _globalParameters =
    new ArrayList<String>();

  protected Document _doc;
  protected CauchoDocument _qDoc;

  protected Path _path;

  boolean _lineContent;
  int _lineWs;
  String _systemId;
  String _filename;
  int _line;
  protected LineMap _lineMap;
  private ArrayList _frags;
  protected int _destLine = 1;
  boolean _defaultCacheable = true;
  boolean _isCacheable;
  protected String _encoding;

  HashMap<String,ArrayList<Template>> _templates = new HashMap<String,ArrayList<Template>>();
  int _minImportance; // for included files, the minimum importance
  int _importance;
  int _templateCount;
  private IntArray _vars = new IntArray();
  private ArrayList<XslNode> _inits = new ArrayList<XslNode>();
  protected ArrayList<Path> _depends = new ArrayList<Path>();
  protected ArrayList<String> _cacheDepends = new ArrayList<String>();

  private boolean _isCauchoXsl;
  protected boolean _isRawText;
  protected String _errorPage;
  boolean _hasSession;
  protected AbstractPattern _nodeListContext;
  private boolean _isTop;
  private ClassLoader _loader;

  protected boolean _isSpecial;
  protected boolean _isStyleScript;

  HashMap<String,String> _outputAttributes = new HashMap<String,String>();
  HashMap<String,String> _macros;
  HashMap<String,Document> _files;
  
  protected AbstractStylesheetFactory _xslGenerator;

  protected ArrayList<String> _imports = new ArrayList<String>();

  Generator(AbstractStylesheetFactory xslGenerator)
  {
    _xslGenerator = xslGenerator;
    
    _workPath = xslGenerator.getWorkPath();

    Path path = xslGenerator.getStylePath();

    _context = path;
    _topContext = _context;
    _loader = xslGenerator.getClassLoader();
    if (_loader == null)
      _loader = Thread.currentThread().getContextClassLoader();
    
    _text = new CharBuffer();
    _frags = new ArrayList();
    _macros = new HashMap<String,String>();

    _keyFun = new KeyFun();
    _formatNumberFun = new FormatNumberFun();
  }

  void init(String filename)
  {
    _lineMap = new LineMap(filename);
  }
  
  public void setErrorPage(String errorPage)
  {
    _errorPage = errorPage;
  }

  public void setStyleScript(boolean stylescript)
  {
    _isStyleScript = stylescript;
  }

  /**
   * Adds a Java import to the generated stylesheet.
   */
  public void addImport(String pkg)
  {
    if (! _imports.contains(pkg))
      _imports.add(pkg);
  }
  
  public void setContentType(String type)
  {
    // contentType = type;
  }

  void setPath(Path path)
  {
    _path = path;
    _context = path;
  }

  void setWorkPath(Path path)
  {
    _workPath = path;
  }

  public int getMinImportance()
  {
    return _minImportance;
  }

  public int getMaxImportance()
  {
    return _importance;
  }

  public NamespaceContext getNamespace()
  {
    return _namespace;
  }

  public AbstractPattern getNodeListContext()
  {
    return _nodeListContext;
  }

  public void addLocale(String name, DecimalFormatSymbols format)
  {
    _formatNumberFun.addLocale(name, format);
  }

  /**
   * Generates a uniqueId
   */
  public int uniqueId()
  {
    return _uniqueId++;
  }

  /**
   * Starts the generation from the top of the document.
   *
   * @param xsl the stylesheet document.
   */
  public StylesheetImpl generate(Node node) throws Exception
  {
    Document xsl = node.getOwnerDocument();
    if (xsl == null)
      xsl = (Document) node;

    DocumentType dtd = xsl.getDoctype();

    if (dtd != null && dtd.getSystemId() != null) {
      _context = _path.lookup(dtd.getSystemId());
      _topContext = _context;
    }
    
    Element top = (Element) xsl.getDocumentElement();

    if (top == null)
      throw error(xsl, L.l("xsl:stylesheet must be top element."));

    _doc = xsl;
    if (_doc instanceof CauchoDocument)
      _qDoc = (CauchoDocument) _doc;

    QElement qTop = null;
    if (top instanceof QElement)
      qTop = (QElement) top;

    /*
    if (qTop != null && qTop.getFilename() != null)
      context = topContext.lookup(qTop.getFilename()).getParent();
    */
    
    _isTop = true;
    _files = new HashMap<String,Document>();
    scanFiles(top);

    if (_qDoc != null) {
      ArrayList depends = (ArrayList) _qDoc.getProperty(CauchoDocument.DEPENDS);
      for (int i = 0; depends != null && i < depends.size(); i++) {
        Path path = (Path) depends.get(i);
        addDepend(path);
      }
    }
    else {
      addDepend(_path);
    }

    if ("stylesheet".equals(getXslLocal(top)) ||
        "transform".equals(getXslLocal(top))) {
      generateStylesheet(top, true);
    }
    else {
      // literal result element
      printHeader();
      boolean oldCacheable = _isCacheable;
      boolean oldDefaultCacheable = _defaultCacheable;
      _isCacheable = true;
    
      XslNode literal = createChild(top);

      XslTemplate template = new XslTemplate();
      template.setGenerator((JavaGenerator) this);
      template.addAttribute(new QName("match"), "/");
      template.addChild(literal);

      template.generateDeclaration(getOut());
    
      template.generate(getOut());

      // printTemplate(top, null, "/", null, 0.0/0.0);
      
      _isCacheable = oldCacheable;
      _defaultCacheable = oldDefaultCacheable;
    }

    addNamespace(top);
    StylesheetImpl stylesheet = completeGenerate(_inits, _globalActions);

    return stylesheet;
  }

  private static CharScanner commaDelimScanner = new CharScanner(" \t\n\r,");

  /**
   * Scan the stylesheet for imported packages and files.  The imported
   * stylesheets will be read and stored in the 'files' HashMap for when
   * they're actually needed.
   */
  private void scanFiles(Element top)
    throws XslParseException, IOException
  {
    _isCauchoXsl = ! top.getAttribute("xsl-caucho").equals("");

    Iterator iter;
    try {
      iter = XPath.select("//xtp:directive.page/@*", top);
    } catch (Exception e) {
      throw new XslParseException(e);
    }
    while (iter.hasNext()) {
      Attr attr = (Attr) iter.next();
      String name = attr.getNodeName();
      String value = attr.getNodeValue();

      if (name.equals("import")) {
        StringCharCursor cursor = new StringCharCursor(value);
        CharBuffer cb = new CharBuffer();
        while (cursor.current() != cursor.DONE) {
          char ch;
          commaDelimScanner.skip(cursor);

          cb.clear();
          ch = commaDelimScanner.scan(cursor, cb);

          if (cb.length() != 0) {
            addImport(cb.toString());
          }
          else if (ch != cursor.DONE)
            throw new IOException(L.l("illegal `import' directive"));
        }
      }
    }

    try {
      iter = XPath.select("//xsl:import|xsl:include", top);
    } catch (Exception e) {
      throw new XslParseException(e);
    }
    while (iter.hasNext()) {
      Element elt = (Element) iter.next();
      String href = elt.getAttribute("href");

      ReadStream rs;

      try {
        rs = _xslGenerator.openPath(href, _context.getURL());
      } catch (Exception e) {
        throw new XslParseException(e);
      }

      Path path = rs.getPath();
      
      Document xsl = readXsl(rs);
      Element subtop = xsl.getDocumentElement();

      if (subtop == null)
        throw error(elt, L.l("xsl:import file {0} is empty", path.getFullPath()));
      
      Path oldContext = _context;

      Path virtualPath = _context.getParent().lookup(href);
      _context = virtualPath;

      _files.put(virtualPath.getPath(), xsl);
      
      scanFiles(subtop);
      _context = oldContext;
    }
  }

  public void addImportList(String value)
    throws XslParseException
  {
    StringCharCursor cursor = new StringCharCursor(value);
    CharBuffer cb = new CharBuffer();
    while (cursor.current() != cursor.DONE) {
      char ch;
      commaDelimScanner.skip(cursor);

      cb.clear();
      ch = commaDelimScanner.scan(cursor, cb);

      if (cb.length() != 0) {
        addImport(cb.toString());
      }
      else if (ch != cursor.DONE)
        throw error(L.l("illegal `import' directive"));
    }
  }

  /**
   * Read in an imported or included XSL file.
   *
   * @param path Path to the include files.
   *
   * @return XML tree describing the XSL.
   */
  Document readXsl(Path path)
    throws IOException, XslParseException
  {
    return readXsl(path.openRead());
  }

  /**
   * Read in an imported or included XSL file.
   *
   * @param path Path to the include files.
   *
   * @return XML tree describing the XSL.
   */
  Document readXsl(ReadStream file)
    throws IOException, XslParseException
  {
    try {
      addDepend(file.getPath());
      
      if (_isStyleScript) {
        XslParser parser = new XslParser();

        return parser.parse(file);
      }
      else
        return new Xml().parseDocument(file);
    } catch (org.xml.sax.SAXException e) {
      throw new XslParseException(e);
    } finally {
      file.close();
    }
  }

  /**
   * Start the top-level stylesheet generation.
   */
  private void generateStylesheet(Element elt, boolean isTop)
    throws Exception
  {
    QElement element = (QElement) elt;
    _isCauchoXsl = ! element.getAttribute("xsl-caucho").equals("");

    String systemId = element.getBaseURI();
    Path oldContext = _context;

    if (systemId != null)
      _context = _context.lookup(systemId);
    
    XslNode stylesheet = createChild(element);

    addNamespace(element);
    
    if (isTop)
      printHeader();

    stylesheet.generateDeclaration(getOut());
    
    stylesheet.generate(getOut());

    _context = oldContext;

    /*
    _version = element.getAttribute("version");
    if (_version.equals(""))
      _version = "1.0";
    
    // generateAttributeSets(element);

    excludeNamespaces(element);

    String xslSpace = element.getAttribute("xsl-space");
    ArrayList<XslNode> children = new ArrayList<XslNode>();
    for (Node child = element.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (! (child instanceof Element))
        continue;
      
      int code = -1;
      String name = getXslLocal(child);
      if (name != null)
        code = _tags.get(name);
      else if ((name = getXtpLocal(child)) != null)
        code = _xtpTags.get(name);
      else {
        String childName = child.getNodeName();
        
        if (childName.startsWith("jsp:directive.") ||
            childName.equals("jsp:declaration") ||
            childName.equals("jsp:scriptlet"))
          addGlobalAction((Element) child);
        continue;
      }

      NamespaceContext oldNamespace = addNamespace((Element) child);
      // generateTopLevelNode(code, (Element) child);
      XslNode node = createChild(child);

      children.add(node);
      _namespace = oldNamespace;
    }

    for (int i = 0; i < children.size(); i++) {
      XslNode node = children.get(i);
      
      node.generate(getOut());
    }
    */
  }

  abstract protected JavaWriter getOut();

  abstract protected XslNode createChild(Node child)
    throws Exception;
  
  abstract protected XslNode createChild(XslNode parent, Node child)
    throws Exception;

  private void addGlobalAction(Element elt)
  {
    _globalActions.add(elt);
  }

  /**
   * Converts the exclude-result-prefixes into the "excludedNamespaces"
   * hashMap for later use.
   */
  private void excludeNamespaces(Element element)
    throws Exception
  {
    if (! (element instanceof QElement))
      return;
    
    QElement elt = (QElement) element;
    String excludeNamespace;
    excludeNamespace = element.getAttribute("exclude-result-prefixes");
    if (! excludeNamespace.equals("")) {
      for (String prefix : excludeNamespace.split("[,\\s]+")) {

        String ns = elt.getNamespace(prefix);
        if (ns == null)
          throw error(elt, L.l("`{0}' must be a namespace prefix",
                               prefix));
        _excludedNamespaces.put(ns, "");
      }
    }
  }

  public void addExcludedNamespace(String ns)
  {
    _excludedNamespaces.put(ns, "");
  }

  public void addInit(XslNode node)
  {
    _inits.add(node);
  }

  public void addGlobalParameter(String param)
  {
    _globalParameters.add(param);
  }

  /**
   * Adds a file dependency for cacheable references to the output of
   * the stylesheet.
   */
  private void addCacheDepends(String attr)
  {
    if (attr.equals(""))
      return;

    int i = 0;
    int ch = 0;
    int len = attr.length();
    for (;
         i < len && XmlChar.isWhitespace((ch = attr.charAt(i))) || ch == ',';
         i++) {
    }
    CharBuffer cb = new CharBuffer();
    while (i < len) {
      cb.clear();
      for (;
           i < len && ! XmlChar.isWhitespace((ch = attr.charAt(i))) &&
             ch != ',';
           i++) {
        cb.append((char) ch);
      }

      _cacheDepends.add(cb.toString());

      for (;
           i < len && XmlChar.isWhitespace((ch = attr.charAt(i))) || ch == ',';
           i++) {
      }
    }
  }

  private void generateCacheDepends(String attr)
    throws Exception
  {
    if (attr.equals(""))
      return;

    int i = 0;
    int ch = 0;
    int len = attr.length();
    for (;
         i < len && XmlChar.isWhitespace((ch = attr.charAt(i))) || ch == ',';
         i++) {
    }
    CharBuffer cb = new CharBuffer();
    while (i < len) {
      cb.clear();
      for (;
           i < len && ! XmlChar.isWhitespace((ch = attr.charAt(i))) &&
             ch != ',';
           i++) {
        cb.append((char) ch);
      }

      printCacheDepends(cb.toString());

      for (;
           i < len && XmlChar.isWhitespace((ch = attr.charAt(i))) || ch == ',';
           i++) {
      }
    }
  }

  /**
   * Generate code for xsl:template
   */
  void generateTemplate(Element element)
    throws Exception
  {
    String name = element.getAttribute("name");
    String patternString = element.getAttribute("match");
    String mode = element.getAttribute("mode");
    String priority = element.getAttribute("priority");
    double dPriority = 0.0/0.0;

    if (! name.equals(""))
      _macros.put(name, name);

    if (name.equals("") && patternString.equals(""))
      throw error("xsl:template expects a `name' or a `match' attribute.");

    if (! priority.equals("")) {
      try {
        dPriority = Double.valueOf(priority).doubleValue();
      } catch (Exception e) {
        throw error("xsl:template expects `priority' must be a double.");
      }
    }

    boolean oldCacheable = _isCacheable;
    boolean oldDefaultCacheable = _defaultCacheable;
    AbstractPattern oldNodeListContext = _nodeListContext;
    if (! patternString.equals(""))
      _nodeListContext = parseMatch(patternString);

    _isCacheable = true;
    printTemplate(element, name, patternString, mode, dPriority);
    _nodeListContext = oldNodeListContext;
    _isCacheable = oldCacheable;
    _defaultCacheable = oldDefaultCacheable;
  }

  public XslNode generateImport(String href)
    throws Exception
  {
    Path path = lookupPath(href);

    if (_files.get(path.getPath()) != null)
      return null;

    Document xsl = readFile(href, path);
    
    if (xsl == null)
      throw new FileNotFoundException(href);

    QElement top = (QElement) xsl.getDocumentElement();
    if (top == null ||
        ! "stylesheet".equals(getXslLocal(top)) &&
        ! "transform".equals(getXslLocal(top))) {
      throw error("imported stylesheet `" + href + 
                  "' missing xsl:stylesheet.");
    }

    int oldMinImportance = _minImportance;
    Path oldContext = _context;
    Path virtualPath = _context.getParent().lookup(href);
    _context = virtualPath;
    _minImportance = _importance;
    boolean oldTop = _isTop;
    boolean oldRaw = _isRawText;
    _isTop = false;
    _isRawText = false;
    
    String systemId = top.getBaseURI();

    if (systemId != null)
      _context = _context.lookup(systemId);
    
    XslStylesheet stylesheet = (XslStylesheet) createChild(top);

    _isRawText = oldRaw;
    _isTop = oldTop;
    _minImportance = oldMinImportance;
    _context = oldContext;

    incrementImportance();

    return stylesheet;
  }

  /**
   * Generates code for xsl:include.  The included file has the same
   * importance as the containing file.
   */
  void generateInclude(Element element)
    throws Exception
  {
    String href = element.getAttribute("href");
    if (href.equals(""))
      throw error("xsl:include expects `href' attribute.");
    
    if (element.getFirstChild() != null)
      throw error("xsl:include must be empty");
  }

  public void generateInclude(XslNode parent, String href)
    throws Exception
  {
    Path path = lookupPath(href);

    if (_files.get(path.getPath()) != null)
      return;
    
    Document xsl = readFile(href, path);
    
    Element top = (Element) xsl.getDocumentElement();
    if (top == null ||
        ! "stylesheet".equals(getXslLocal(top)) &&
        ! "transform".equals(getXslLocal(top))) {
      throw error("imported stylesheet `" + href + 
                  "' missing xsl:stylesheet.");
    }

    Path oldContext = _context;
    _context = path;

    for (Node node = top.getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      XslNode child = createChild(parent, node);

      if (child != null)
        parent.addChild(child);
    }
    
    // generateStylesheet(top, false);
    
    _context = oldContext;
  }

  /**
   * Returns the actual path for the relative href.
   */
  private Path lookupPath(String href)
  {
    return _context.getParent().lookup(href);
  }

  private Document readFile(String href, Path virtualPath)
    throws Exception
  {
    Document xsl = (Document) _files.get(virtualPath.getPath());

    if (xsl != null)
      throw new IllegalStateException(L.l("'{0}' is a duplicated path",
                                          virtualPath.getPath()));
    
    ReadStream rs;

    try {
      rs = _xslGenerator.openPath(href, _context.getURL());
    } catch (Exception e) {
      throw new XslParseException(e);
    }

    Path path = rs.getPath();
      
    xsl = readXsl(rs);
    Element subtop = xsl.getDocumentElement();

    if (subtop == null)
      throw error(L.l("xsl:import file {0} is empty", path.getFullPath()));
      
    Path oldContext = _context;

    _context = virtualPath;

    _files.put(virtualPath.getPath(), xsl);
      
    _context = oldContext;

    return xsl;
  }

  void generateKey(Element element)
    throws Exception
  {
    String name = element.getAttribute("name");
    if (name.equals(""))
      throw error("xsl:key expects `name' attribute.");
    String match = element.getAttribute("match");
    if (match.equals(""))
      throw error("xsl:key expects `match' attribute.");
    String use = element.getAttribute("use");
    if (use.equals(""))
      throw error("xsl:key expects `use' attribute.");
    
    if (element.getFirstChild() != null)
      throw error("xsl:key must be empty");

    _keyFun.add(name, parseMatch(match), parseExpr(use));
  }

  public void addKey(String name, AbstractPattern match, Expr use)
  {
    _keyFun.add(name, match, use);
  }

  void generateLocale(Element element)
    throws Exception
  {
    String name = element.getAttribute("name");
    if (name.equals(""))
      name = "*";

    DecimalFormatSymbols format = new DecimalFormatSymbols();

    String value = element.getAttribute("decimal-separator");
    if (value.length() > 0) 
      format.setDecimalSeparator(value.charAt(0));

    value = element.getAttribute("grouping-separator");
    if (value.length() > 0) 
      format.setGroupingSeparator(value.charAt(0));

    value = element.getAttribute("infinity");
    if (! value.equals(""))
      format.setInfinity(value);

    value = element.getAttribute("minus-sign");
    if (value.length() > 0)
      format.setMinusSign(value.charAt(0));

    value = element.getAttribute("NaN");
    if (! value.equals(""))
      format.setNaN(value);

    value = element.getAttribute("percent");
    if (value.length() > 0)
      format.setPercent(value.charAt(0));

    value = element.getAttribute("per-mille");
    if (value.length() > 0)
      format.setPerMill(value.charAt(0));

    value = element.getAttribute("zero-digit");
    if (value.length() > 0)
      format.setZeroDigit(value.charAt(0));

    value = element.getAttribute("digit");
    if (value.length() > 0)
      format.setDigit(value.charAt(0));

    value = element.getAttribute("pattern-separator");
    if (value.length() > 0)
      format.setPatternSeparator(value.charAt(0));

    _formatNumberFun.addLocale(name, format);
  }

  void generateNamespaceAlias(Element element)
    throws Exception
  {
    if (! (element instanceof QElement))
      return;
    
    QElement elt = (QElement) element;
    String stylesheetPrefix;
    String resultPrefix;

    stylesheetPrefix = (String) element.getAttribute("stylesheet-prefix");
    resultPrefix = (String) element.getAttribute("result-prefix");

    if (stylesheetPrefix.equals(""))
      throw error(element, "xsl:namespace-alias needs `stylesheet-prefix'");
    if (resultPrefix.equals(""))
      throw error(element, "xsl:namespace-alias needs `result-prefix'");
  }

  public void addNamespaceAlias(String stylesheetPrefix,
                                String resultPrefix)
  {
    /*
    String stylesheetNs = getNamespace(stylesheetPrefix);
    if (stylesheetPrefix.equals("#default"))
      stylesheetNs = "";
    else if (stylesheetNs.equals(""))
      throw error("`" + stylesheetPrefix + "' is not a valid namespace prefix");
    String resultNs =  getNamespace(resultPrefix);
    if (resultPrefix.equals("#default")) {
      resultPrefix = "";
      resultNs = "";
    }
    else if (resultNs.equals(""))
      throw error("`" + resultPrefix + "' is not a valid namespace prefix");
    
    String result[] = new String[] { resultPrefix, resultNs };
    _namespaceAliases.put(stylesheetNs, result);
    */
  }

  public void addNamespaceAlias(String namespace, String []result)
  {
    _namespaceAliases.put(namespace, result);
  }

  public String []getNamespaceAlias(String namespace)
  {
    return _namespaceAliases.get(namespace);
  }

  /**
   * Scans through the stylesheet, grabbing the attribute set
   * definitions.
   *
   * @param element the current nost
   */
  /*
  void generateAttributeSets(Element element)
    throws Exception
  {
    Node child = element.getFirstChild();

    for (; child != null; child = child.getNextSibling()) {
      if (! "attribute-set".equals(getXslLocal(child)))
        continue;

      QElement elt = (QElement) child;
      String name = elt.getAttribute("name");
      if (name.equals(""))
        throw error(L.l("xsl:attribute-set expects `name' attribute."));

      generateAttributeSet(element, name);
    }
  }
  */

  /**
   * Scans through the stylesheet, grabbing the attribute set
   * definitions.
   *
   * @param element the current node
   */
  /*
  HashMap<String,String> generateAttributeSet(Element element, String setName)
    throws Exception
  {
    Node child = element.getFirstChild();

    for (; child != null; child = child.getNextSibling()) {
      if (! "attribute-set".equals(getXslLocal(child)))
        continue;

      QElement elt = (QElement) child;
      String name = elt.getAttribute("name");
      if (name.equals(""))
        throw error(L.l("xsl:attribute-set expects `name' attribute."));

      if (! name.equals(setName))
        continue;

      HashMap<String,String> set = _attributeSets.get(name);
      if (set != null)
        return set;

      set = new HashMap<String,String>();
      _attributeSets.put(name, set);

      // add any attributes from use-attribute-sets
      for (Node attr = elt.getFirstAttribute();
           attr != null;
           attr = attr.getNextSibling()) {
        if (attr.getNodeName().equals("use-attribute-sets")) {
          HashMap<String,String> subset = generateAttributeSet(element, attr.getNodeValue());
          
          if (subset == null)
            throw error(elt, L.l("Unknown attribute-set `{0}'.  Each use-attribute-sets needs a matching xsl:attribute-set.", attr.getNodeValue()));
          Iterator<String> iter = subset.keySet().iterator();
          while (iter.hasNext()) {
            String key = iter.next();
            set.put(key, subset.get(key));
          }
        }
      }

      for (Node attr = elt.getFirstChild();
           attr != null;
           attr = attr.getNextSibling()) {
        if (! "attribute".equals(getXslLocal(attr)))
          continue;
        Element attrElt = (Element) attr;
        String attrName = attrElt.getAttribute("name");
        if (attrName.equals(""))
          throw error(L.l("xsl:attribute expects `name' attribute."));

        set.put(attrName, ((QElement) attr).getTextValue());
      }

      for (Attr attr = ((QElement) elt).getFirstAttribute();
           attr != null;
           attr = (Attr) attr.getNextSibling()) {
        if (attr.getNodeName().equals("name") ||
            attr.getNodeName().equals("use-attribute-sets"))
          continue;

        set.put(attr.getNodeName(), attr.getNodeValue());
      }

      return set;
    }

    return null;
  }
  */

  public void addAttributeSet(String name, XslAttributeSet attributeSet)
  {
    _attributeSets.put(name, attributeSet);
  }

  /*
  public XslAttributeSet getAttributeSet(String name)
  {
    return _attributeSets.get(name);
  }
  */

  public void setDisableOutputEscaping(boolean disable)
  {
    _isRawText = disable;
  }

  public boolean getDisableOutputEscaping()
  {
    return _isRawText;
  }
  
  private void generateOutput(Element element)
    throws Exception
  {
    Node attr = ((QElement) element).getFirstAttribute();

    if (element.getFirstChild() != null)
      throw error("xsl:output must be empty");

    String disableEscaping;
    disableEscaping = element.getAttribute("resin:disable-output-escaping");
    if (disableEscaping.equals(""))
      disableEscaping = element.getAttribute("disable-output-escaping");
    if (disableEscaping.equals("no") ||
        disableEscaping.equals("false"))
      _isRawText = false;
    else if (! disableEscaping.equals(""))
      _isRawText = true;

    // Only top-level xsl:output matters (XXX: spec?)
    if (! _isTop)
      return;

    if (_outputAttributes == null)
      _outputAttributes = new HashMap<String,String>();

    for (; attr != null; attr = attr.getNextSibling()) {
      _outputAttributes.put(attr.getNodeName(), attr.getNodeValue());
    }
  }

  public void setOutputAttribute(String name, String value)
  {
    _outputAttributes.put(name, value);
  }

  private void generatePreserveSpace(Element element)
    throws Exception
  {
    String elements = element.getAttribute("elements");
    if (elements.equals(""))
      throw error("xsl:preserve-space expects `elements' attribute.");
    
    if (element.getFirstChild() != null)
      throw error("xsl:preserve-space must be empty");
    
    int i = 0;
    int len = elements.length();
    for (; i < len && XmlChar.isWhitespace(elements.charAt(i)); i++) {
    }
    CharBuffer cb = new CharBuffer();
    while (i < len) {
      cb.clear();

      for (; i < len && ! XmlChar.isWhitespace(elements.charAt(i)); i++)
        cb.append(elements.charAt(i));

      _preserve.put(cb.toString(), "true");

      for (; i < len && XmlChar.isWhitespace(elements.charAt(i)); i++) {
      }
    }
  }

  private void generateStripSpace(Element element)
    throws Exception
  {
    throw new UnsupportedOperationException();
    /*
    String elements = element.getAttribute("elements");
    if (elements.equals(""))
      throw error("xsl:strip-space expects `elements' attribute.");
    
    if (element.getFirstChild() != null)
      throw error("xsl:strip-space must be empty");
    */
  }

  public void addStripSpace(String elements)
  {
    int i = 0;
    int len = elements.length();
    for (; i < len && XmlChar.isWhitespace(elements.charAt(i)); i++) {
    }
    CharBuffer cb = new CharBuffer();
    while (i < len) {
      cb.clear();

      for (; i < len && ! XmlChar.isWhitespace(elements.charAt(i)); i++) {
        cb.append(elements.charAt(i));
      }

      _strip.put(cb.toString(), "true");

      for (; i < len && XmlChar.isWhitespace(elements.charAt(i)); i++) {
      }
    }
  }

  public void addPreserveSpace(String elements)
  {
    int i = 0;
    int len = elements.length();
    for (; i < len && XmlChar.isWhitespace(elements.charAt(i)); i++) {
    }
    CharBuffer cb = new CharBuffer();
    while (i < len) {
      cb.clear();

      for (; i < len && ! XmlChar.isWhitespace(elements.charAt(i)); i++) {
        cb.append(elements.charAt(i));
      }

      _preserve.put(cb.toString(), "true");

      for (; i < len && XmlChar.isWhitespace(elements.charAt(i)); i++) {
      }
    }
  }

  protected void generateChildren(Node node)
    throws Exception
  {
    _vars.add(0);
    for (Node child = node.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      generateChild(child);
    }
    int count = _vars.pop();
    if (count > 0 && _vars.size() > 0)
      printPopScope(count);
  }
  
  protected void generateChild(Node child)
    throws Exception
  {
    generateChildImpl(child);
  }

  public void generateChildImpl(Node child)
    throws Exception
  {
    String nodeName = getXslLocal(child);
    int code = -1;
    if (nodeName != null)
      code = _tags.get(nodeName);
    else if ((nodeName = getXtpLocal(child)) != null)
      code = _xtpTags.get(nodeName);
    /* XXX: xsl/04al
    else if (macros.get(child.getNodeName()) != null) {
      generateMacro((Element) child);
      return;
    }
    */

    if (nodeName == null) {
      if (child.getNodeType() == child.TEXT_NODE)
        generateText(child);
      else if (child.getNodeType() == child.ELEMENT_NODE) {
        NamespaceContext oldNamespace = addNamespace((Element) child);
        printElement((Element) child);
        _namespace = oldNamespace;
      }
      return;
    }

    if (child instanceof QElement) {
      NamespaceContext oldNamespace = addNamespace((QElement) child);
      generateChild(child, code);
      _namespace = oldNamespace;
    }
    else
      generateChild(child, code);
  }
  
  public void generateChild(Node child, int code)
    throws Exception
  {
    if (child instanceof QAbstractNode) {
      QAbstractNode qChild = (QAbstractNode) child;
      setLocation(qChild.getBaseURI(), qChild.getFilename(), qChild.getLine());
    }

    switch (code) {
    case TEXT:
      generateText(child);
      break;

    case XSL_TEXT:
      generateXslText((Element) child);
      break;

    case APPLY_TEMPLATES:
      generateApplyTemplates((Element) child);
      break;

    case APPLY_IMPORTS:
      generateApplyImports((Element) child);
      break;

    case CALL_TEMPLATE:
      generateCallTemplate((Element) child);
      break;

    case PARAM:
      generateParamVariable((Element) child);
      break;

    case VARIABLE:
      generateVariable((Element) child);
      break;

    case VALUE_OF:
      generateValueOf((Element) child);
      break;

    case COPY_OF:
      generateCopyOf((Element) child);
      break;

    case FOR_EACH:
      generateForEach((Element) child);
      break;

    case IF:
      generateIf((Element) child);
      break;

    case CHOOSE:
      generateChoose((Element) child);
      break;

    case NUMBER:
      generateNumber((Element) child);
      break;

    case COPY:
      printCopy((Element) child);
      break;

    case COPY_ELEMENT:
      printCopyElement((Element) child);
      break;

    case ELEMENT:
      generateElement((Element) child);
      break;

    case ATTRIBUTE:
      generateAttribute((Element) child);
      break;

    case PI:
      printPi((Element) child);
      break;

    case COMMENT:
      printComment((Element) child);
      break;

    case MESSAGE:
      printMessage((Element) child);
      break;

    case EXPRESSION:
      if (! _defaultCacheable)
        _isCacheable = false;
      printExpression((Element) child);
      break;

    case SCRIPTLET:
      if (! _defaultCacheable)
        _isCacheable = false;
      printScriptlet((Element) child);
      break;

    case DIRECTIVE_CACHE:
      generateCacheDepends(((Element) child).getAttribute("file"));
      if (! ((Element) child).getAttribute("no-cache").equals("")) {
        _isCacheable = false;
        _defaultCacheable = false;
      }
      else
        _defaultCacheable = true;
      break;

    case WHILE:
      generateWhile((Element) child);
      break;

    case ASSIGN:
      generateAssign((Element) child);
      break;

    case RESULT_DOCUMENT:
      generateResultDocument((Element) child);
      break;

    case IGNORE:
      break;

    default:
      if (child instanceof QElement &&
          XSLNS.equals(((QElement) child).getNamespaceURI()) &&
          _version != null && _version.equals("1.0"))
        throw error(child, "unknown XSL element `" +
                    child.getNodeName() + "'");
      else {
        Node subchild = child.getFirstChild();
        boolean hasFallback = false;
        for (; subchild != null; subchild = subchild.getNextSibling()) {
          String local = getXslLocal(subchild);
          if (local != null && local.equals("fallback")) {
            hasFallback = true;
            generateChildren(subchild);
          }
        }
        if (! hasFallback) // && child.getNamespace().equals(XSLNS))
          printError(L.l("expected xsl tag at `{0}'",
                         child.getNodeName()));
      }
      break;
    }
  }

  private void generateText(Node node)
    throws Exception
  {
    String data = node.getNodeValue();
    int length = data.length();

    if (length == 0)
      return;

    int i = 0;
    for (; i < length && XmlChar.isWhitespace(data.charAt(i)); i++) {
    }

    if (i == length && stripNode(node))
      return;

    if (data != null && data.length() > 0 && node instanceof QAbstractNode) {
      setLocation(node);
      writeText(data);
    }
  }

  private boolean stripNode(Node node)
  {
    for (node = node.getParentNode();
         node != null;
         node = node.getParentNode()) {
      if (node instanceof Element) {
        Element elt = (Element) node;
        String space = elt.getAttribute("xml:space");
        if (! space.equals(""))
          return ! space.equals("preserve");
      }
    }

    return true;
  }

  void generateXslText(Element element)
    throws Exception
  {
    _text.clear();
    for (Node child = element.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (! (child instanceof Text))
        continue;

      String data = child.getNodeValue();
      int length = data.length();
      
      _text.append(data);
    }

    String disableEscaping = element.getAttribute("disable-output-escaping");
    if (disableEscaping.equals(""))
      disableEscaping = "no";

    if (_text.length() <= 0) {
    }
    else if (! disableEscaping.equals("yes") &&
             ! disableEscaping.equals("true"))
      writeText(_text.toString());
    else {
      startDisableEscaping();
      writeText(_text.toString());
      endDisableEscaping();
    }
  }

  private void generateApplyTemplates(Node node)
    throws Exception
  {
    QElement element = (QElement) node;
    String select = element.getAttribute("select");
    String mode = element.getAttribute("mode");
    AbstractPattern selectPattern = null;
    if (! select.equals(""))
      selectPattern = parseSelect(select, node);

    Sort []sort = generateSort(node);

    if (sort != null && selectPattern == null)
      selectPattern = parseSelect("*", node);

    pushCall();
    generateArgs(element);
    printApplyTemplates(selectPattern, mode, sort);
    popCall();
  }

  private void generateApplyImports(Node node)
    throws Exception
  {
    QElement element = (QElement) node;
    String mode = element.getAttribute("mode");

    if (element.getFirstChild() != null)
      throw error(L.l("xsl:apply-imports must be empty"));

    pushCall();
    generateArgs(element);
    printApplyImports(mode, _minImportance, _importance);
    popCall();
  }

  private void generateCallTemplate(Element element)
    throws Exception
  {
    String name = element.getAttribute("name");
    String mode = element.getAttribute("mode");

    if (name.equals(""))
      throw error(L.l("{0} expects `{1}' attribute",
                      "xsl:call-template", "name"));

    if (findMacro(name) == null)
      throw error(element, L.l("`{0}' is an unknown macro for xsl:call-template.  All macros must be defined in an <xsl:template name='...'> element.", name));

    pushCall();
    generateArgs(element);
    printCallTemplate(name, mode);
    popCall();
  }

  private Element findMacro(String name)
    throws Exception
  {
    Element template = findMacroInDocument(_doc, name);
    if (template != null)
      return template;
    
    Iterator iter = _files.values().iterator();
    while (iter.hasNext()) {
      Document doc = (Document) iter.next();

      template = findMacroInDocument(doc, name);

      if (template != null)
        return template;
    }

    return null;
  }

  private Element findMacroInDocument(Document doc, String name)
  {
    Element elt = doc.getDocumentElement();

    for (Node child = elt.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (! child.getNodeName().equals("xsl:template"))
        continue;

      Element template = (Element) child;

      if (template.getAttribute("name").equals(name))
        return template;
    }

    return null;
  }

  private void generateMacro(Element element)
    throws Exception
  {
    QElement elt = (QElement) element;

    String name = element.getNodeName();
    String mode = element.getAttribute("mode");

    pushCall();
    for (Node node = elt.getFirstAttribute();
         node != null;
         node = node.getNextSibling()) {
      String argName = node.getNodeName();
      String argValue = node.getNodeValue();

      printParam(argName, argValue, elt);
    }
    printParam("contents", elt);
    printCallTemplate(name, mode);
    popCall();
  }

  private void generateArgs(Element element)
    throws Exception
  {
    for (Node node = element.getFirstChild(); node != null;
         node = node.getNextSibling()) {
      String localName = getXslLocal(node);

      if ("with-param".equals(localName)) {
        String key = ((Element) node).getAttribute("name");
        String expr = ((Element) node).getAttribute("select");
        if (key.equals(""))
          throw error(L.l("{0} requires `{1}' attribute",
                          "xsl:with-param", "name"));

        if (! expr.equals(""))
          printParam(key, parseExpr(expr));
        else
          printParam(key, node);
      }
    }
  }

  private void generateParamVariable(Element element)
    throws Exception
  {
    int i = _vars.size() - 1;
    _vars.set(i, _vars.get(i) + 1);
    
    String name = element.getAttribute("name");
    String expr = element.getAttribute("select");
    if (name.equals(""))
      throw error(L.l("{0} expects `{1}' attribute",
                      "xsl:param", "name"));

    if (! expr.equals(""))
      printParamVariable(name, parseExpr(expr));
    else
      printParamVariable(name, element);
  }

  private void generateVariable(Element element)
    throws Exception
  {
    int i = _vars.size() - 1;
    _vars.set(i, _vars.get(i) + 1);

    String name = element.getAttribute("name");
    String expr = element.getAttribute("select");
    if (name.equals(""))
      throw error(L.l("{0} expects `{1}' attribute.",
                      "xsl:variable", "name"));

    if (! expr.equals(""))
      printVariable(name, parseExpr(expr));
    else
      printVariable(name, element);
  }

  private void generateAssign(Element element)
    throws Exception
  {
    String name = element.getAttribute("name");
    String expr = element.getAttribute("select");
    if (name.equals(""))
      throw error(L.l("{0} expects `{1}' attribute.",
                      "xtp:assign", "name"));

    if (! expr.equals(""))
      printAssign(name, parseExpr(expr));
    else
      printAssign(name, element);
  }

  private void generateResultDocument(Element element)
    throws Exception
  {
    String href = element.getAttribute("href");
    String format = element.getAttribute("format");
    if (href.equals(""))
      throw error(L.l("{0} expects `{1}' attribute.",
                      "xtp:result-document", "href"));

    printResultDocument(element, href, format);
  }

  private void generateValueOf(Element element)
    throws Exception
  {
    String select = element.getAttribute("select");
    if (select.equals(""))
      throw error(L.l("{0} expects `{1}' attribute.",
                      "xsl:value-of", "select"));
    
    if (element.getFirstChild() != null)
      throw error(L.l("{0} must be empty", "xsl:value-of"));

    String disable = element.getAttribute("disable-output-escaping");
    boolean isDisabled = disable.equals("yes");
    if (isDisabled)
      startDisableEscaping();

    printSelectValue(select, element);

    if (isDisabled)
      endDisableEscaping();
  }

  private void generateCopyOf(Element element)
    throws Exception
  {
    String select = element.getAttribute("select");
    if (select.equals(""))
      throw error(L.l("{0} expects `{1}' attribute",
                      "xsl:copy-of", "select"));

    if (element.getFirstChild() != null)
      throw error(L.l("{0} must be empty", "xsl:copy-of"));
    
    printCopyOf(select, element);
  }

  /**
   * Generates code for the xsl:for-each element
   */
  void generateForEach(Element element)
    throws Exception
  {
    String select = element.getAttribute("select");
    if (select.equals(""))
      throw error(L.l("{0} expects `{1}' attribute",
                      "xsl:for-each", "select"));

    Sort []sort = generateSort(element);

    if (sort != null)
      printForEach(element, select, sort);
    else
      printForEach(element, select);
  }

  private Sort []generateSort(Node node)
    throws XslParseException, IOException
  {
    ArrayList<Sort> sorts = new ArrayList<Sort>();
  sort:
    for (Node child = node.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (child.getNodeType() == child.TEXT_NODE) {
        String data = child.getNodeValue();
        for (int i = 0; i < data.length(); i++) {
          if (! XmlChar.isWhitespace(data.charAt(i)))
            break sort;
        }
        continue;
      }
      else if (child.getNodeType() == child.COMMENT_NODE ||
               child.getNodeType() == child.PROCESSING_INSTRUCTION_NODE)
        continue;

      String name = getXslLocal(child);
      if (! "sort".equals(name))
        break;

      Element elt = (Element) child;
      String select = elt.getAttribute("select");
      if (select.equals(""))
        throw error(L.l("{0} expects attribute `{1}'",
                        "xsl:sort", "select"));

      Expr expr = parseExpr(select);
      String order = elt.getAttribute("order");
      Expr isAscending = null;
      if (order.equals("")) {
        isAscending = parseExpr("true()");
      }
      else if (order.startsWith("{") && order.endsWith("}")) {
        order = order.substring(1, order.length() - 1);
        
        isAscending = parseExpr(order + " = 'ascending'");
      }
      else if (order.equals("ascending"))
        isAscending = parseExpr("true()");
      else
        isAscending = parseExpr("false()");
      
      String dataType = elt.getAttribute("data-type");
      boolean isText = true;
      if (dataType.equals("number"))
        isText = false;

      String lang = elt.getAttribute("lang");
      if (lang.equals("")) {
        sorts.add(Sort.create(expr, isAscending, isText));
      }
      else {
        if (lang.startsWith("{") && lang.endsWith("}"))
          lang = lang.substring(1, lang.length() - 1);
        else
          lang = "'" + lang + "'";

        sorts.add(Sort.create(expr, isAscending, parseExpr(lang)));
        /*
        int p = lang.indexOf('-');
        Locale locale = null;

        if (p < 0) {
          locale = new Locale(lang, "");
        }
        else {
          String language = lang.substring(0, p);
          
          int q = lang.indexOf(p + 1, '-');

          if (q < 0) {
            String country = lang.substring(p + 1);

            locale = new Locale(language, country);
          }
          else {
            String country = lang.substring(p + 1, q);
            String variant = lang.substring(q);

            locale = new Locale(language, country, variant);
          }
        }

        sorts.add(Sort.create(expr, isAscending, lang));
        */
      }
    }

    if (sorts.size() > 0)
      return (Sort []) sorts.toArray(new Sort[sorts.size()]);
    else
      return null;
  }

  void generateIf(Element element)
    throws Exception
  {
    String test = (String) element.getAttribute("test");
    if (test.equals(""))
      throw error(L.l("{0} expects `{1}' attribute", "xsl:if", "test"));

    printIf(element, parseExpr(test));
  }

  void generateWhile(Element element)
    throws Exception
  {
    String test = (String) element.getAttribute("test");
    if (test.equals(""))
      throw error(L.l("{0} expects `{1}' attribute", "xsl:while", "test"));

    printWhile(element, parseExpr(test));
  }

  void generateChoose(Element element)
    throws Exception
  {
    boolean first = true;
    for (Node child = element.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (! (child instanceof Element))
        continue;
      
      String name = getXslLocal(child);

      if ("when".equals(name)) {
        Element elt = (Element) child;
        String test = elt.getAttribute("test");
        if (test.equals(""))
          throw error(L.l("{0} expects `{1}' attribute", "xsl:when", "test"));

        printChoose(elt, parseExpr(test), first);
        first = false;
      }
      else if ("otherwise".equals(name)) {
        printOtherwise((Element) child, first);
      }
      else
        throw error(L.l("xsl:choose expects `xsl:when' or `xsl:otherwise' at `{0}'",
                        child.getNodeName()));
    }
  }

  void generateElement(Element element)
    throws Exception
  {
    String name = (String) element.getAttribute("name");
    if (name.equals(""))
      throw error(L.l("{0} expects `{1}' attribute.", "xsl:element", "name"));
    Attr nsAttr = element.getAttributeNode("namespace");

    if (nsAttr == null)
      printElement(element, name);
    else
      printElement(element, name, nsAttr.getNodeValue());
  }

  void generateAttribute(Element element)
    throws Exception
  {
    String name = (String) element.getAttribute("name");
    if (name.equals(""))
      throw error(L.l("{0} expects `{1}' attribute",
                      "xsl:attribute", "name"));
    Attr nsAttr = element.getAttributeNode("namespace");

    boolean oldSpecial = _isSpecial;
    _isSpecial = true;
    
    if (nsAttr == null)
      printAttribute(element, name);
    else
      printAttribute(element, name, nsAttr.getNodeValue());

    _isSpecial = oldSpecial;
  }

  void generateNumber(Element element)
    throws Exception
  {
    String value = element.getAttribute("value");
    String count = element.getAttribute("count");
    String from = element.getAttribute("from");
    String level = element.getAttribute("level");
    String format = element.getAttribute("format");
    String letter = element.getAttribute("letter-value");
    String separator = element.getAttribute("grouping-separator");
    String lang = element.getAttribute("lang");
    String size_name = element.getAttribute("grouping-size");

    int size = 0;
    for (int i = 0; i < size_name.length(); i++) {
        char ch = size_name.charAt(i);
        if (ch >= '0' && ch <= '9')
            size = 10 * size + ch - '0';
    }

    boolean isAlphabetic = true;
    if (! letter.equals("alphabetic"))
      isAlphabetic = false;

    AbstractPattern countPattern = null;
    if (! count.equals(""))
      countPattern = parseMatch(count);

    AbstractPattern fromPattern = null;
    if (! from.equals(""))
      fromPattern = parseMatch(from);

    if (level.equals("") || level.equals("single"))
      level = "single";
    else if (level.equals("multiple")) {
    }
    else if (level.equals("any")) {
    }
    else
      throw error(L.l("xsl:number can't understand level=`{0}'",
                      level));

    XslNumberFormat xslFormat;
    xslFormat = new XslNumberFormat(format, lang, isAlphabetic,
                                    separator, size);

    if (! value.equals(""))
      printNumber(parseExpr(value), xslFormat);
    else
      printNumber(level, countPattern, fromPattern, xslFormat);
  }

  void printNumber(Expr expr, XslNumberFormat format)
    throws Exception
  {
    
  }

  void printNumber(String level,
                   AbstractPattern countPattern,
                   AbstractPattern fromPattern,
                   XslNumberFormat format)
    throws Exception
  {
    
  }

  /**
   * Sets location code for the node.
   */
  void setLocation(Node node)
    throws Exception
  {
    if (node instanceof QAbstractNode) {
      setLocation(((QAbstractNode) node).getBaseURI(),
                  ((QAbstractNode) node).getFilename(),
                  ((QAbstractNode) node).getLine());
    }
  }

  public void setLocation(String systemId, String filename, int line)
    throws XslParseException, IOException
  {
    if (filename != null) {
      _systemId = systemId;
      _filename = filename;
      _line = line;
      // _lineMap.add(filename, line, getOut().getDestLine());
    }
  }

  int getTextLength()
  {
    return _text.length();
  }

  protected void printHeader()
    throws XslParseException, IOException
  {
  }
  
  abstract protected void startDisableEscaping()
    throws Exception;
   
  abstract protected void endDisableEscaping()
    throws Exception;

  abstract protected void writeText(String text)
    throws Exception;

  abstract protected void printTemplate(Element node,
                                        String name, String pattern,
                                        String mode, double priority)
    throws Exception;


  /**
   * Prints location code for the node.
   */
  void printLocation(Node node)
    throws Exception
  {
    if (node instanceof QAbstractNode) {
      printLocation(((QAbstractNode) node).getBaseURI(),
                    ((QAbstractNode) node).getFilename(),
                    ((QAbstractNode) node).getLine());
    }
  }

  abstract protected void printLocation(String systemId, String filename, int line)
    throws Exception;
  
  abstract protected void printElement(Node node)
    throws Exception;

  abstract protected void
    printApplyTemplates(AbstractPattern select, String mode, Sort []sort)
    throws Exception;

  abstract protected void
    printApplyImports(String mode, int min, int max)
    throws Exception;
  
  abstract protected void
    printCallTemplate(String name, String mode)
    throws Exception;
  
  abstract protected void pushCall()
    throws Exception;
  
  abstract protected void popCall()
    throws Exception;
  
  abstract protected void printParam(String name, Object value)
    throws Exception;

  abstract protected void printParam(String name, String value, Element elt)
    throws Exception;
    
  abstract protected void printParamVariable(String name, Expr expr)
    throws Exception;
  
  abstract protected void printParamVariable(String name, Element elt)
    throws Exception;
  
  abstract protected void printVariable(String name, Object value)
    throws Exception;
  
  protected void printAssign(String name, Object value)
    throws Exception
  {
    printVariable(name, value);
  }
  
  abstract protected void printPopScope(int count)
    throws Exception;
  
  abstract protected void printCopyOf(String select, Element element)
    throws Exception;

  abstract protected void printSelectValue(String select, Element element)
    throws Exception;

  abstract protected void printForEach(Element element, String select)
    throws Exception;

  abstract protected void printForEach(Element element, String select,
                                       Sort []sort)
    throws Exception;

  protected void printIf(Element element, Expr expr)
    throws Exception
  {
  }

  protected void printChoose(Element element, Expr expr, boolean first)
    throws Exception
  {
  }

  protected void printOtherwise(Element element, boolean first)
    throws Exception
  {
  }

  protected void printCopy(Element element)
    throws Exception
  {
  }

  protected void printCopyElement(Element element)
    throws Exception
  {
  }

  protected void printElement(Element element, String name)
    throws Exception
  {
  }

  protected void printElement(Element element, String name, String namespace)
    throws Exception
  {
  }

  protected void printAttribute(Element node, String name)
    throws Exception
  {
  }

  protected void printAttribute(Element node, String name, String namespace)
    throws Exception
  {
  }

  protected void printPi(Element node)
    throws Exception
  {
  }

  protected void printComment(Element node)
    throws Exception
  {
  }

  protected void printError(String msg)
    throws Exception
  {
  }

  protected void printMessage(Element node)
    throws Exception
  {
  }

  // extension

  protected void printExpression(Element node)
    throws Exception
  {
  }

  protected void printScriptlet(Element node)
    throws Exception
  {
  }

  protected void printDeclaration(Element node)
    throws Exception
  {
  }

  protected void printCacheDepends(String path)
    throws Exception
  {
  }

  protected void printWhile(Element element, Expr expr)
    throws Exception
  {
  }

  protected void printResultDocument(Element element, String href,
                                     String format)
    throws Exception
  {
  }

  public int getImportance()
  {
    return _importance;
  }

  public void setMinImportance(int importance)
  {
    _minImportance = importance;
  }
  
  public void incrementImportance()
  {
    _importance++;
  }

  /**
   * Adds a new template pattern.
   *
   * @param pattern the match pattern.
   * @param mode the template mode.
   * @param priority the template priority.
   * @param function the associated function name.
   * @param funId the function id.
   */
  Template addPattern(AbstractPattern pattern, String mode, double priority,
                      String function, int funId)
  {
    if (pattern instanceof UnionPattern) {
      UnionPattern union = (UnionPattern) pattern;
      addPattern(union.getLeft(), mode, priority, function, funId);
      return addPattern(union.getRight(), mode, priority, function, funId);
    }

    if (Double.isNaN(priority))
      priority = pattern.getPriority();

    if (log.isLoggable(Level.FINER))
      log.finer("add " + pattern.getNodeName() + " " + pattern + " fun:" +
                function + " mode:" + mode + " priority:" + priority);

    Template template = new Template(pattern, mode,
                                     _minImportance, _importance,
                                     priority, _templateCount++,
                                     function, funId);

    addTemplate(pattern.getNodeName(), template);

    return template;
  }
  
  private void addTemplate(String nodeName, Template template)
  {
    ArrayList<Template> templateList = _templates.get(nodeName);
    
    if (templateList == null) {
      templateList = new ArrayList<Template>();
      _templates.put(nodeName, templateList);
    }

    for (int i = templateList.size() - 1; i >= 0; i--) {
      Template item = templateList.get(i);

      if (template.compareTo(item) <= 0) {
        templateList.add(i + 1, template);
        return;
      }
    }

    templateList.add(0, template);
  }

  public AbstractPattern parseMatch(String pattern)
    throws XslParseException, IOException
  {
    if (true)
      throw new RuntimeException();
    try {
      return XPath.parseMatch(pattern, _namespace).getPattern();
    } catch (Exception e) {
      throw error(L.l("{0} in pattern `{1}'",
                      e.toString(), pattern));
    }
  }

  public AbstractPattern parseSelect(String pattern)
    throws IOException, XslParseException
  {
    if (true)
      throw new RuntimeException();
    
    try {
      return XPath.parseSelect(pattern, _namespace).getPattern();
    } catch (Exception e) {
      throw error(e);
    }
  }

  protected AbstractPattern parseSelect(String pattern, Node node)
    throws IOException, XslParseException
  {
    if (true)
      throw new UnsupportedOperationException();
    try {
      return XPath.parseSelect(pattern, _namespace).getPattern();
    } catch (Exception e) {
      throw error(node, e);
    }
  }

  public Expr parseExpr(String pattern)
    throws XslParseException
  {
    if (true)
      throw new UnsupportedOperationException();
    try {
      return XPath.parseExpr(pattern, _namespace, _nodeListContext);
    } catch (Exception e) {
      throw error(e);
    }
  }

  XslParseException error(Exception e)
  {
    if (e.getMessage() != null)
      return error(e.getMessage());
    else {
      log.log(Level.WARNING, e.toString(), e);
      
      return error(e.toString());
    }
  }

  XslParseException error(Node node, Exception e)
  {
    if (e.getMessage() != null)
      return error(node, e.getMessage());
    else {
      log.log(Level.WARNING, e.toString(), e);

      return error(e.toString());
    }
  }
  
  XslParseException error(String message)
  {
    return new XslParseException(_filename + ":" + _line + ": " + message);
  }

  /**
   * Creates an error message with filename and line number based on
   * the source node.
   *
   * @param node XML node of the source XSL.
   * @param message the error message.
   */
  XslParseException error(Node node, String message)
  {
    if (! (node instanceof QAbstractNode))
      return error(message);
    
    QAbstractNode qnode = (QAbstractNode) node;

    String filename = qnode.getFilename();
    int line = qnode.getLine();
    
    if (filename != null)
      return new XslParseException(filename + ":" +
                                   line + ": " +
                                   message);
    else
      return error(message);
  }

  /**
   * Returns the local name of an XSL element.  Non-XSL elements return
   * null.  So xsl:copy will return "copy", while "foo:bar" returns null.
   *
   * @param node the XSL source node
   * @return the local part of the XSL name.
   */
  protected String getXslLocal(Node node)
  {
    if (! (node instanceof Element))
      return null;

    QElement elt = (QElement) node;

    String ns = elt.getNamespaceURI();
    String prefix = elt.getPrefix();

    if (ns == null || ns.equals("")) {
      return (elt.getNodeName().startsWith("xsl:") ?
              elt.getNodeName().substring(4) :
              null);
    }
    else if (ns.startsWith(XSLNS) &&
             (ns.length() == XSLNS.length() || ns.charAt(XSLNS.length()) == '/'))
      return elt.getLocalName();
    else
      return null;
  }

  protected String getXtpLocal(Node node)
  {
    if (! (node instanceof Element))
      return null;

    QElement elt = (QElement) node;

    String ns = elt.getNamespaceURI();
    String prefix = elt.getPrefix();

    if (ns == null || ns.equals("")) {
      return (elt.getNodeName().startsWith("xtp:") ?
              elt.getNodeName().substring(4) :
              null);
    }
    else if (ns.startsWith(XTPNS))
      return elt.getLocalName();
    else
      return null;
  }

  /**
   * Parses an expression in a context
   */
  private Expr parseExpr(Node node, String expr)
    throws Exception
  {
    try {
      return XPath.parseExpr(expr, _namespace, _nodeListContext);
    } catch (Exception e) {
      throw error(node, e.getMessage());
    }
  }

  /**
   * Adds the namespaces in the element to the current NamespaceContext.
   * The XPath pattern parsing uses NamespaceContext to associate the right
   * context with element patterns.
   *
   * @param elt the XSL element being processed.
   *
   * @return the old namespace context
   */
  protected NamespaceContext addNamespace(Element elt)
  {
    NamespaceContext oldNamespace = _namespace;

    Node attr = ((QElement) elt).getFirstAttribute();
    for (; attr != null; attr = attr.getNextSibling()) {
      String name = attr.getNodeName();

      if (name.startsWith("xmlns:"))
        name = name.substring(6);
      else if (name.equals("xmlns"))
        name = "";
      else
        continue;

      // Note: according to the spec, the default namespace is not used
      
      String url = attr.getNodeValue();
      if (url.equals(XSLNS) || url.equals(XTPNS))
        continue;

      if (url.startsWith("quote:"))
        url = url.substring(6);

      _namespace = new NamespaceContext(_namespace, name, url);
    }

    return oldNamespace;
  }

  void addDepend(Path depend)
  {
    if (depend != null)
      _depends.add(depend);
  }

  abstract protected StylesheetImpl completeGenerate(ArrayList<XslNode> inits,
                                                     ArrayList globals)
    throws Exception;

  /**
   * Close call when an error occurs.
   */
  public void close()
    throws IOException, XslParseException
  {
  }

  static {
    _tags = new IntMap();
    _tags.put("stylesheet", STYLESHEET);
    _tags.put("transform", STYLESHEET);
    _tags.put("output", OUTPUT);
    _tags.put("template", TEMPLATE);
    _tags.put("preserve-space", PRESERVE_SPACE);
    _tags.put("strip-space", STRIP_SPACE);
    _tags.put("import", IMPORT);
    _tags.put("include", INCLUDE);
    _tags.put("key", KEY);
    _tags.put("decimal-format", LOCALE);
    _tags.put("attribute-set", ATTRIBUTE_SET);
    _tags.put("namespace-alias", NAMESPACE_ALIAS);

    _tags.put("apply-templates", APPLY_TEMPLATES);
    _tags.put("apply-imports", APPLY_IMPORTS);
    _tags.put("call-template", CALL_TEMPLATE);
    _tags.put("param", PARAM);
    _tags.put("variable", VARIABLE);
    _tags.put("for-each", FOR_EACH);
    _tags.put("if", IF);
    _tags.put("choose", CHOOSE);

    _tags.put("value-of", VALUE_OF);
    _tags.put("copy-of", COPY_OF);
    _tags.put("text", XSL_TEXT);
    _tags.put("#text", TEXT);
    _tags.put("number", NUMBER);
    _tags.put("copy", COPY);
    _tags.put("element", ELEMENT);
    _tags.put("attribute", ATTRIBUTE);
    _tags.put("pi", PI);
    _tags.put("processing-instruction", PI);
    _tags.put("comment", COMMENT);

    _tags.put("message", MESSAGE);

    _tags.put("sort", IGNORE);
    _tags.put("fallback", IGNORE);
    // xslt 2.0
    _tags.put("result-document", RESULT_DOCUMENT);

    _xtpTags = new IntMap();
    _xtpTags.put("expression", EXPRESSION);
    _xtpTags.put("expr", EXPRESSION);
    _xtpTags.put("eval", EXPRESSION);
    _xtpTags.put("scriptlet", SCRIPTLET);
    _xtpTags.put("script", SCRIPTLET);
    _xtpTags.put("decl", DECLARATION);
    _xtpTags.put("declaration", DECLARATION);
    _xtpTags.put("directive.cache", DIRECTIVE_CACHE);
    _xtpTags.put("while", WHILE);
    _xtpTags.put("assign", ASSIGN);
  }
}
