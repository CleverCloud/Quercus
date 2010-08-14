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

package com.caucho.jsp;

import com.caucho.java.LineMap;
import com.caucho.jsp.java.JspNode;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.LineCompileException;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses the JSP page.  Both the XML and JSP tags are understood.  However,
 * escaping is always done using JSP rules.
 */
public class JspParser {
  static L10N L = new L10N(JspParser.class);
  private static final Logger log
    = Logger.getLogger(JspParser.class.getName());

  public static final String JSP_NS = "http://java.sun.com/JSP/Page";
  public static final String JSTL_CORE_URI = "http://java.sun.com/jsp/jstl/core";
  public static final String JSTL_FMT_URI = "http://java.sun.com/jsp/jstl/fmt";

  public static final QName PREFIX = new QName("prefix");
  public static final QName TAGLIB = new QName("taglib");
  public static final QName TAGDIR = new QName("tagdir");
  public static final QName URI = new QName("uri");

  public static final QName JSP_DECLARATION =
    new QName("jsp", "declaration", JSP_NS);

  public static final QName JSP_SCRIPTLET =
    new QName("jsp", "scriptlet", JSP_NS);

  public static final QName JSP_EXPRESSION =
    new QName("jsp", "expression", JSP_NS);

  public static final QName JSP_DIRECTIVE_PAGE =
    new QName("jsp", "directive.page", JSP_NS);

  public static final QName JSP_DIRECTIVE_INCLUDE =
    new QName("jsp", "directive.include", JSP_NS);

  public static final QName JSP_DIRECTIVE_CACHE =
    new QName("jsp", "directive.cache", JSP_NS);

  public static final QName JSP_DIRECTIVE_TAGLIB =
    new QName("jsp", "directive.taglib", JSP_NS);

  public static final QName JSP_DIRECTIVE_ATTRIBUTE =
    new QName("jsp", "directive.attribute", JSP_NS);

  public static final QName JSP_DIRECTIVE_VARIABLE =
    new QName("jsp", "directive.variable", JSP_NS);

  public static final QName JSP_DIRECTIVE_TAG =
    new QName("jsp", "directive.tag", JSP_NS);

  public static final QName JSTL_CORE_OUT =
    new QName("resin-c", "out", "urn:jsptld:" + JSTL_CORE_URI);

  public static final QName JSTL_CORE_CHOOSE =
    new QName("resin-c", "choose", "urn:jsptld:" + JSTL_CORE_URI);

  public static final QName JSTL_CORE_WHEN =
    new QName("resin-c", "when", "urn:jsptld:" + JSTL_CORE_URI);

  public static final QName JSTL_CORE_OTHERWISE =
    new QName("resin-c", "otherwise", "urn:jsptld:" + JSTL_CORE_URI);

  public static final QName JSTL_CORE_FOREACH =
    new QName("resin-c", "forEach", "urn:jsptld:" + JSTL_CORE_URI);

  private static final int TAG_UNKNOWN = 0;
  private static final int TAG_JSP = 1;
  private static final int TAG_RAW = 2;

  private ParseState _parseState;
  private JspBuilder _jspBuilder;
  private ParseTagManager _tagManager;

  private LineMap _lineMap;

  private ArrayList<String> _preludeList = new ArrayList<String>();
  private ArrayList<String> _codaList = new ArrayList<String>();

  private ArrayList<Include> _includes = new ArrayList<Include>();

  private Set<String> _prefixes = new HashSet<String>();
  // jsp/18cy, jsp/18cz
  private Set<String> _localPrefixes = new HashSet<String>();

  private Path _jspPath;
  private ReadStream _stream;
  private String _uriPwd;

  private String _contextPath = "";
  private String _filename = "";
  private int _line;
  private int _lineStart;

  private int _charCount;
  private int _startText;

  private int _peek = -1;
  private boolean _seenCr = false;

  private Namespace _namespaces
    = new Namespace(null, "jsp", JSP_NS);

  private boolean _isXml;
  private boolean _isTop = true;

  private CharBuffer _tag = new CharBuffer();
  private CharBuffer _value = new CharBuffer();

  private CharBuffer _text = new CharBuffer();

  /**
   * Sets the JSP builder, which receives the SAX-like events from
   * JSP parser.
   */
  void setJspBuilder(JspBuilder builder)
  {
    _jspBuilder = builder;
  }

  /**
   * Sets the context path for error messages.
   */
  void setContextPath(String contextPath)
  {
    _contextPath = contextPath;
  }

  /**
   * Sets the parse state, which stores state information for the parsing.
   */
  void setParseState(ParseState parseState)
  {
    _parseState = parseState;
  }

  /**
   * Sets the parse state, which stores state information for the parsing.
   */
  ParseState getParseState()
  {
    return _parseState;
  }

  /**
   * Sets the tag manager
   */
  void setTagManager(ParseTagManager manager)
  {
    _tagManager = manager;
  }

  /**
   * Returns true if JSP EL expressions are enabled.
   */
  private boolean isELIgnored()
  {
    return _parseState.isELIgnored();
  }

  /**
   * Returns true if Velocity-style statements are enabled.
   */
  private boolean isVelocity()
  {
    return _parseState.isVelocityEnabled();
  }

  /**
   * Returns true if JSP EL expressions are enabled.
   */
  private boolean isDeferredSyntaxAllowedAsLiteral()
  {
    return _parseState.isDeferredSyntaxAllowedAsLiteral();
  }

  /**
   * Adds a prelude.
   */
  public void addPrelude(String prelude)
  {
    _preludeList.add(prelude);
  }

  /**
   * Adds a coda.
   */
  public void addCoda(String coda)
  {
    _codaList.add(coda);
  }

  /**
   * Starts parsing the JSP page.
   *
   * @param path the JSP source file
   * @param uri the URI for the JSP source file.
   */
  void parse(Path path, String uri)
    throws Exception
  {
    _parseState.pushNamespace("jsp", JSP_NS);

    _isXml = _parseState.isXml();

    _filename = _contextPath + uri;

    if (uri != null) {
      int p = uri.lastIndexOf('/');
      _uriPwd = p <= 0 ? "/" : uri.substring(0, p + 1);
    }
    else {
      _uriPwd = "/";
    }
    _parseState.setUriPwd(_uriPwd);

    ReadStream is = path.openRead();
    path.setUserPath(uri);

    try {
      parseJsp(is);
    } finally {
      is.close();
      for (int i = 0; i < _includes.size(); i++) {
        Include inc = _includes.get(i);
        inc._stream.close();
      }
    }
  }

  /**
   * Starts parsing the JSP page as a tag.
   *
   * @param path the JSP source file
   * @param uri the URI for the JSP source file.
   */
  void parseTag(Path path, String uri)
    throws Exception
  {
    _parseState.setTag(true);

    parse(path, uri);
  }

  /**
   * Top-level JSP parser.
   *
   * @param stream the read stream containing the JSP file
   *
   * @return an XML DOM containing the JSP.
   */
  private void parseJsp(ReadStream stream)
    throws Exception
  {
    _text.clear();
    _includes.clear();

    String uriPwd = _uriPwd;

    for (int i = _codaList.size() - 1; i >= 0; i--)
      pushInclude(_codaList.get(i), true);

    addInclude(stream, uriPwd);

    for (int i = _preludeList.size() - 1; i >= 0; i--)
      pushInclude(_preludeList.get(i), true);

    setLocation();
    _jspBuilder.startDocument();

    String pageEncoding = _parseState.getPageEncoding();

    int ch;

    if (pageEncoding != null) {
      _parseState.setPageEncoding(pageEncoding);

      stream.setEncoding(pageEncoding);
    }

    switch ((ch = stream.read())) {
    case 0xfe:
      if ((ch = stream.read()) != 0xff) {
        throw error(L.l("Expected 0xff in UTF-16 header.  UTF-16 pages with the initial byte 0xfe expect 0xff immediately following.  The 0xfe 0xff sequence is used by some application to suggest UTF-16 encoding without a directive."));
      }
      else {
        //_parseState.setContentType("text/html; charset=UTF-16BE");
        log.finer(L.l("JSP '{0}': setting page encoding using BOM 'fe ff' -> 'UTF-16BE'", _jspPath.toString()));
        _parseState.setBom(0xfeff);
        _parseState.setPageEncoding("UTF-16BE");
        stream.setEncoding("UTF-16BE");
      }
      break;

    case 0xff:
      if ((ch = stream.read()) != 0xfe) {
        throw error(L.l("Expected 0xfe in UTF-16 header.  UTF-16 pages with the initial byte 0xff expect 0xfe immediately following.  The 0xff 0xfe sequence is used by some application to suggest UTF-16 encoding without a directive."));
      }
      else {
        //_parseState.setContentType("text/html; charset=UTF-16LE");
        log.finer(L.l("JSP '{0}': setting page encoding using BOM 'ff fe' -> 'UTF-16LE'", _jspPath.toString()));
        _parseState.setBom(0xfffe);
        _parseState.setPageEncoding("UTF-16LE");
        stream.setEncoding("UTF-16LE");
      }
      break;

    case 0xef:
      if ((ch = stream.read()) != 0xbb) {
        stream.unread();
        stream.unread();
      }
      else if ((ch = stream.read()) != 0xbf) {
        throw error(L.l("Expected 0xbf in UTF-8 header.  UTF-8 pages with the initial byte 0xbb expect 0xbf immediately following.  The 0xbb 0xbf sequence is used by some application to suggest UTF-8 encoding without a directive."));
      }
      else {
        // jsp/002a, #3062
        // _parseState.setContentType("text/html; charset=UTF-8");
        log.finer(L.l("JSP '{0}': setting page encoding using BOM 'ef bb bf' -> 'UTF-8'", _jspPath.toString()));
        _parseState.setBom(0xefbbbf);
        _parseState.setPageEncoding("UTF-8");
        stream.setEncoding("UTF-8");
      }
      break;

    case -1:
      break;

    default:
      stream.unread();
      break;
    }

    ch = read();

    ch = parseXmlDeclaration(ch);

    try {
      parseNode(ch);
    } finally {
      for (int i = 0; i < _includes.size(); i++) {
        Include inc = _includes.get(i);
        inc._stream.close();
      }
    }

    setLocation();
    _jspBuilder.endDocument();
  }

  private int parseXmlDeclaration(int ch)
    throws IOException, JspParseException
  {
    if (ch != '<')
      return ch;
    else if ((ch = read()) != '?') {
      unread(ch);
      return '<';
    }
    else if ((ch = read()) != 'x') {
      addText("<?");
      return ch;
    }
    else if ((ch = read()) != 'm') {
      addText("<?x");
      return ch;
    }
    else if ((ch = read()) != 'l') {
      addText("<?xm");
      return ch;
    }
    else if (! XmlChar.isWhitespace((ch = read()))) {
      addText("<?xml");
      return ch;
    }

    String encoding = null;

    addText("<?xml ");
    ch = skipWhitespace(ch);
    while (XmlChar.isNameStart(ch)) {
      ch = readName(ch);
      String name = _tag.toString();

      addText(name);
      if (XmlChar.isWhitespace(ch))
        addText(' ');

      ch = skipWhitespace(ch);
      if (ch != '=')
        return ch;

      readValue(name, ch, true);
      String value = _value.toString();

      addText("=\"");
      addText(value);
      addText("\"");

      if (name.equals("encoding"))
        encoding = value;

      ch = read();
      if (XmlChar.isWhitespace(ch))
        addText(' ');
      ch = skipWhitespace(ch);
    }

    if (ch != '?')
      return ch;
    else if ((ch = read()) != '>') {
      addText('?');
      return ch;
    }
    else {
      addText("?>");

      if (encoding != null) {
        _stream.setEncoding(encoding);
        _parseState.setPageEncoding(encoding);
      }

      return read();
    }
  }

  private void parseNode(int ch)
    throws IOException, JspParseException
  {
    while (ch != -1) {
      switch (ch) {
      case '<':
        {
          switch ((ch = read())) {
          case '%':
            if (_isXml)
              throw error(L.l("'<%' syntax is not allowed in JSP/XML syntax."));

            parseScriptlet();
            _startText = _charCount;

            // escape '\\' after scriptlet at end of line
            if ((ch = read()) == '\\') {
              if ((ch = read()) == '\n') {
                ch = read();
              }
              else if (ch == '\r') {
                if ((ch = read()) == '\n')
                  ch = read();
              }
              else
                addText('\\');
            }
            break;

          case '/':
            ch = parseCloseTag();
            break;

          case '\\':
            if ((ch = read()) == '%') {
              addText("<%");
              ch = read();
            }
            else
              addText("<\\");
            break;

          case '!':
            if (! _isXml)
              addText("<!");
            else if ((ch = read()) == '[')
              parseCdata();
            else if (ch == '-' && (ch = read()) == '-')
              parseXmlComment();
            else
              throw error(L.l("'{0}' was not expected after '<!'.  In the XML syntax, only <!-- ... --> and <![CDATA[ ... ]> are legal.  You can use '&amp;!' to escape '<!'.",
                              badChar(ch)));

            ch = read();
            break;

          default:
            if (! XmlChar.isNameStart(ch)) {
              addText('<');
              break;
            }

            ch = readName(ch);
            String name = _tag.toString();
            int tagCode = getTag(name);
            if (! _isXml && tagCode == TAG_UNKNOWN) {
              addText("<");
              addText(name);
              break;
            }

            if (_isTop && name.equals("jsp:root")) {
              if (_parseState.isForbidXml())
                throw error(L.l("jsp:root must be in a JSP (XML) document, not a plain JSP."));

              _text.clear();
              _isXml = true;
              _parseState.setELIgnoredDefault(false);
              _parseState.setXml(true);
            }

            _isTop = false;
            parseOpenTag(name, ch, tagCode == TAG_UNKNOWN);

            ch = read();

            // escape '\\' after scriptlet at end of line
            if (! _isXml && ch == '\\') {
              if ((ch = read()) == '\n') {
                ch = read();
              }
              else if (ch == '\r') {
                if ((ch = read()) == '\n')
                  ch = read();
              }
            }
          }
          break;
        }

      case '&':
        if (! _isXml)
          addText((char) ch);
        else {
          addText((char) parseEntity());
        }
        ch = read();
        break;

      case '$':
        ch = read();

        if (ch == '{' && ! isELIgnored())
          ch = parseJspExpression();
        else
          addText('$');
        break;

      case '#':
        ch = read();

        if (isVelocity()) {
          ch = parseVelocity(ch);
        }
        else if (ch != '{' || isELIgnored()) {
          addText('#');
        }
        else if (isDeferredSyntaxAllowedAsLiteral()) {
          addText('#');
        }
        else
          throw error(L.l("Deferred syntax ('#{...}') not allowed as literal."));
        break;

      case '\\':
        switch (ch = read()) {
        case '$':
          if (! isELIgnored()) {
            addText('$');
            ch = read();
          }
          else
            addText('\\');
          break;

        case '#':
          if (! isELIgnored()) {
            addText('#');
            ch = read();
          }
          else
            addText('\\');
          break;

        case '\\':
          addText('\\');
          break;

        default:
          addText('\\');
          break;
        }
        break;

      default:
        addText((char) ch);
        ch = read();
        break;
      }
    }

    addText();

    /* XXX: end document
    if (! _activeNode.getNodeName().equals("jsp:root"))
      throw error(L.l("'</{0}>' expected at end of file.  For XML, the top-level tag must have a matching closing tag.",
                      activeNode.getNodeName()));
    */
  }

  /**
   * JSTL-style expressions.  Currently understood:
   *
   * <code><pre>
   * ${a * b} - any arbitrary expression
   * </pre></code>
   */
  private int parseJspExpression()
    throws IOException, JspParseException
  {
    addText();

    Path jspPath = _jspPath;
    String filename = _filename;
    int line = _line;

    CharBuffer cb = CharBuffer.allocate();
    int ch;
    cb.append("${");
    for (ch = read(); ch >= 0 && ch != '}'; ch = read())
      cb.append((char) ch);
    cb.append("}");

    ch = read();

    String prefix = _parseState.findPrefix(JSTL_CORE_URI);

    if (prefix == null) {
      prefix = "resin-c";

      /*
      _jspBuilder.startElement(JSP_DIRECTIVE_TAGLIB);
      _jspBuilder.attribute(new QName("prefix"), prefix);
      _jspBuilder.attribute(new QName("uri"), JSTL_CORE_URI);
      _jspBuilder.endAttributes();
      _jspBuilder.endElement(JSP_DIRECTIVE_TAGLIB.getName());
      */
      _jspBuilder.addNamespace(prefix, JSTL_CORE_URI);

      processTaglib(prefix, JSTL_CORE_URI);
    }

    setLocation(jspPath, filename, line);
    _jspBuilder.startElement(JSTL_CORE_OUT);
    _jspBuilder.attribute(new QName("value"), cb.close());
    _jspBuilder.attribute(new QName("escapeXml"), "false");
    _jspBuilder.endAttributes();
    _jspBuilder.endElement(JSTL_CORE_OUT.getName());

    return ch;
  }

  private int parseVelocity(int ch)
    throws IOException, JspParseException
  {
    if (ch == '{') {
      return parseVelocityScriptlet();
    }
    else if ('a' <= ch && ch <= 'z') {
      ch = readName(ch);
      String name = _tag.toString();

      if (name.equals("if")) {
        ch = parseVelocityIf("if");
      }
      else if (name.equals("elseif")) {
        addText();
        setLocation();

        JspNode node = _jspBuilder.getCurrentNode();
        if (! "resin-c:when".equals(node.getTagName()))
          throw error(L.l("#elseif is missing a corresponding #if.  Velocity-style #if syntax needs matching #if ... #elseif ... #else ... #end.  The #if statements must also nest properly with any tags."));

        _jspBuilder.endElement("resin-c:when");
        ch = parseVelocityIf("elseif");
      }
      else if (name.equals("else")) {
        addText();
        setLocation();
        _jspBuilder.endElement("resin-c:when");

        setLocation(_jspPath, _filename, _lineStart);
        _lineStart = _line;
        _jspBuilder.startElement(JSTL_CORE_OTHERWISE);
        _jspBuilder.endAttributes();

        ch = skipWhitespaceToEndOfLine(ch);
      }
      else if (name.equals("foreach")) {
        ch = parseVelocityForeach("resin-c:forEach");
      }
      else if (name.equals("end")) {
        addText();

        JspNode node = _jspBuilder.getCurrentNode();
        String nodeName = null;
        if (node != null)
          nodeName = node.getTagName();

        if (nodeName.equals("resin-c:when") ||
            nodeName.equals("resin-c:otherwise")) {
          _jspBuilder.endElement(nodeName);
          _jspBuilder.endElement(JSTL_CORE_CHOOSE.getName());
        }
        else if (nodeName.equals("resin-c:forEach"))
          _jspBuilder.endElement(nodeName);
        else {
          throw error(L.l("#end is missing a corresponding #if or #foreach.  Velocity-style #if syntax needs matching #if ... #elseif ... #else ... #end. The #if statements must also nest properly with any tags."));
        }

        ch = skipWhitespaceToEndOfLine(ch);
      }
      else {
        addText('#');
        addText(name);
      }
    }
    else
      addText('#');

    return ch;
  }

  /**
   * This syntax isn't part of velocity.
   *
   * <code><pre>
   * #{ int foo = 3; }#
   * </pre></code>
   */
  private int parseVelocityScriptlet()
    throws IOException, JspParseException
  {
    addText();

    setLocation(_jspPath, _filename, _line);
    _lineStart = _line;
    _jspBuilder.startElement(JSP_SCRIPTLET);
    _jspBuilder.endAttributes();

    int ch = read();
    while (ch >= 0) {
      if (ch == '}') {
        ch = read();
        if (ch == '#')
          break;
        else
          addText('}');
      }
      else {
        addText((char) ch);
        ch = read();
      }
    }

    createText();

    _jspBuilder.endElement(JSP_SCRIPTLET.getName());

    ch = read();

    if (ch == '\r') {
      ch = read();
      if (ch == '\n')
        return read();
      else
        return ch;
    }
    else if (ch == '\n')
      return read();
    else
      return ch;
  }

  /**
   * parses a #foreach statement
   *
   * <pre>
   * #foreach ([Type] var in expr)
   * ...
   * #end
   * </pre>
   *
   * <pre>
   * #foreach ([Type] var in [min .. max])
   * ...
   * #end
   * </pre>
   */
  private int parseVelocityForeach(String eltName)
    throws IOException, JspParseException
  {
    int ch;

    for (ch = read(); XmlChar.isWhitespace(ch); ch = read()) {
    }

    if (ch != '(')
      throw error(L.l("Expected `(' after #foreach at `{0}'.  The velocity-style #foreach syntax needs parentheses: #foreach ($a in expr)",
                      badChar(ch)));

    addText();

    processTaglib("resin-c", JSTL_CORE_URI);

    setLocation(_jspPath, _filename, _lineStart);
    _lineStart = _line;
    _jspBuilder.startElement(JSTL_CORE_FOREACH);

    CharBuffer cb = CharBuffer.allocate();
    parseVelocityName(cb);

    if (cb.length() == 0) {
      throw error(L.l("Expected iteration variable for #foreach at `{0}'.  The velocity-style #foreach syntax is: #foreach ($a in expr)",
                      badChar(ch)));
    }

    String name = cb.toString();

    cb.clear();
    parseVelocityName(cb);

    if (cb.length() == 0) {
      throw error(L.l("Expected 'in' for #foreach at `{0}'.  The velocity-style #foreach syntax is: #foreach ($a in expr)",
                      badChar(ch)));
    }

    String value = cb.toString();
    if (! value.equals("in")) {
      throw error(L.l("Expected 'in' for #foreach at `{0}'.  The velocity-style #foreach syntax is: #foreach ($a in expr)",
                      badChar(ch)));
    }

    if (name.startsWith("$"))
      name = name.substring(1);

    _jspBuilder.attribute(new QName("var"), name);

    cb.clear();
    parseVelocityExpr(cb, ')');
    String expr = cb.close();

    if (expr.indexOf("..") > 0) {
      int h = 0;
      for (; Character.isWhitespace(expr.charAt(h)); h++) {
      }

      if (expr.charAt(h) != '[')
        throw error(L.l("Expected '[' for #foreach `{0}'.  The velocity-style #foreach syntax is: #foreach ([Type] $a in [min .. max])",
                        badChar(expr.charAt(h))));

      int t = expr.length() - 1;
      for (; Character.isWhitespace(expr.charAt(t)); t--) {
      }

      if (expr.charAt(t) != ']')
        throw error(L.l("Expected ']' for #foreach `{0}'.  The velocity-style #foreach syntax is: #foreach ($a in [min .. max])",
                        badChar(expr.charAt(t))));

      int p = expr.indexOf("..");

      String min = expr.substring(h + 1, p);
      String max = expr.substring(p + 2, t);

      _jspBuilder.attribute(new QName("begin"), "${" + min + "}");
      _jspBuilder.attribute(new QName("end"), "${" + max + "}");
    }
    else {
      _jspBuilder.attribute(new QName("items"), "${" + expr + "}");
    }
    _jspBuilder.endAttributes();

    return skipWhitespaceToEndOfLine(read());
  }

  /**
   * parses an #if statement
   */
  private int parseVelocityIf(String eltName)
    throws IOException, JspParseException
  {
    int ch;

    for (ch = read(); XmlChar.isWhitespace(ch); ch = read()) {
    }

    if (ch != '(')
      throw error(L.l("Expected `(' after #if at `{0}'.  The velocity-style #if syntax needs parentheses: #if (...)",
                      badChar(ch)));

    addText();

    processTaglib("resin-c", JSTL_CORE_URI);

    setLocation(_jspPath, _filename, _line);
    if (eltName.equals("if")) {
      _jspBuilder.startElement(JSTL_CORE_CHOOSE);
      _jspBuilder.endAttributes();
    }
    _jspBuilder.startElement(JSTL_CORE_WHEN);
    _lineStart = _line;

    CharBuffer cb = CharBuffer.allocate();
    parseVelocityExpr(cb, ')');
    _jspBuilder.attribute(new QName("test"), "${" + cb.close() + "}");
    _jspBuilder.endAttributes();

    return skipWhitespaceToEndOfLine(read());
  }

  private int parseVelocityName(CharBuffer cb)
    throws IOException, JspParseException
  {
    int ch;

    for (ch = read(); XmlChar.isWhitespace(ch); ch = read()) {
    }

    for (; Character.isJavaIdentifierPart((char) ch); ch = read())
      cb.append((char) ch);

    return ch;
  }

  private int parseVelocityMin(CharBuffer cb)
    throws IOException, JspParseException
  {
    int ch;

    for (ch = read(); ch >= 0; ch = read()) {
      if (ch != '$')
        cb.append((char) ch);

      if (ch == '(') {
        ch = parseVelocityExpr(cb, ')');
        cb.append((char) ch);
      }
      else if (ch == '[') {
        ch = parseVelocityExpr(cb, ']');
        cb.append((char) ch);
      }
      else if (ch == '.') {
        ch = read();
        if (ch == '.')
          return ch;
        else {
          cb.append('.');
          _peek = ch;
        }
      }
    }

    return ch;
  }

  private int parseVelocityExpr(CharBuffer cb, int end)
    throws IOException, JspParseException
  {
    int ch;

    for (ch = read(); ch >= 0 && ch != end; ch = read()) {
      if (ch != '$')
        cb.append((char) ch);

      if (ch == '(') {
        ch = parseVelocityExpr(cb, ')');
        cb.append((char) ch);
      }
      else if (ch == '[') {
        ch = parseVelocityExpr(cb, ']');
        cb.append((char) ch);
      }
    }

    return ch;
  }

  /**
   * Parses a &lt;![CDATA[ block.  All text in the CDATA is uninterpreted.
   */
  private void parseCdata()
    throws IOException, JspParseException
  {
    int ch;

    ch = readName(read());

    String name = _tag.toString();

    if (! name.equals("CDATA"))
      throw error(L.l("Expected <![CDATA[ at <!['{0}'.", name,
                      "XML only recognizes the <![CDATA directive."));

    if (ch != '[')
      throw error(L.l("Expected '[' at '{0}'.  The XML CDATA syntax is <![CDATA[...]]>.",
                      String.valueOf(ch)));

    String filename = _filename;
    int line = _line;

    while ((ch = read()) >= 0) {
      while (ch == ']') {
        if ((ch = read()) != ']')
          addText(']');
        else if ((ch = read()) != '>')
          addText("]]");
        else
          return;
      }

      addText((char) ch);
    }

    throw error(L.l("Expected closing ]]> at end of file to match <![[CDATA at {0}.", filename + ":" + line));
  }

  /**
   * Parses an XML name for elements and attribute names.  The parsed name
   * is stored in the 'tag' class variable.
   *
   * @param ch the next character
   *
   * @return the character following the name
   */
  private int readName(int ch)
    throws IOException, JspParseException
  {
    _tag.clear();

    for (; XmlChar.isNameChar((char) ch); ch = read())
      _tag.append((char) ch);

    return ch;
  }

  private void parsePageDirective(String name, String value)
    throws IOException, JspParseException
  {
    if ("isELIgnored".equals(name)) {
      if ("true".equals(value))
        _parseState.setELIgnored(true);
    }
  }

  /**
   * Parses a special JSP syntax.
   */
  private void parseScriptlet()
    throws IOException, JspParseException
  {
    addText();

    _lineStart = _line;

    int ch = read();

    // probably should be a qname
    QName eltName = null;

    switch (ch) {
    case '=':
      eltName = JSP_EXPRESSION;
      ch = read();
      break;

    case '!':
      eltName = JSP_DECLARATION;
      ch = read();
      break;

    case '@':
      parseDirective();
      return;

    case '-':
      if ((ch = read()) == '-') {
        parseComment();
        return;
      }
      else {
        eltName = JSP_SCRIPTLET;
        addText('-');
      }
      break;

    default:
      eltName = JSP_SCRIPTLET;
      break;
    }

    setLocation(_jspPath, _filename, _lineStart);
    _jspBuilder.startElement(eltName);
    _jspBuilder.endAttributes();

    while (ch >= 0) {
      switch (ch) {
      case '\\':
        addText('\\');
        ch = read();
        if (ch >= 0)
          addText((char) ch);
        ch = read();
        break;

      case '%':
        ch = read();
        if (ch == '>') {
          createText();
          setLocation();
          _jspBuilder.endElement(eltName.getName());
          return;
        }
        else if (ch == '\\') {
          ch = read();
          if (ch == '>') {
            addText("%");
          }
          else
            addText("%\\");
        }
        else
          addText('%');
        break;

      default:
        addText((char) ch);
        ch = read();
        break;
      }
    }

    createText();
    setLocation();
    _jspBuilder.endElement(eltName.getName());
  }

  /**
   * Parses the JSP directive syntax.
   */
  private void parseDirective()
    throws IOException, JspParseException
  {
    String language = null;

    int ch = skipWhitespace(read());
    String directive = "";
    if (XmlChar.isNameStart(ch)) {
      ch = readName(ch);
      directive = _tag.toString();
    }
    else
      throw error(L.l("Expected jsp directive name at '{0}'.  JSP directive syntax is <%@ name attr1='value1' ... %>",
                      badChar(ch)));

    QName qname;

    if (directive.equals("page"))
      qname = JSP_DIRECTIVE_PAGE;
    else if (directive.equals("include"))
      qname = JSP_DIRECTIVE_INCLUDE;
    else if (directive.equals("taglib"))
      qname = JSP_DIRECTIVE_TAGLIB;
    else if (directive.equals("cache"))
      qname = JSP_DIRECTIVE_CACHE;
    else if (directive.equals("attribute"))
      qname = JSP_DIRECTIVE_ATTRIBUTE;
    else if (directive.equals("variable"))
      qname = JSP_DIRECTIVE_VARIABLE;
    else if (directive.equals("tag"))
      qname = JSP_DIRECTIVE_TAG;
    else
      throw error(L.l("'{0}' is an unknown jsp directive.  Only <%@ page ... %>, <%@ include ... %>, <%@ taglib ... %>, and <%@ cache ... %> are known.", directive));

    unread(ch);

    ArrayList<QName> keys = new ArrayList<QName>();
    ArrayList<String> values = new ArrayList<String>();
    ArrayList<String> prefixes = new ArrayList<String>();
    ArrayList<String> uris = new ArrayList<String>();

    parseAttributes(keys, values, prefixes, uris);

    ch = skipWhitespace(read());

    if (ch != '%' || (ch = read()) != '>') {
      throw error(L.l("expected '%>' at {0}.  JSP directive syntax is '<%@ name attr1='value1' ... %>'.  (Started at line {1})",
                      badChar(ch), _lineStart));
    }

    setLocation(_jspPath, _filename, _lineStart);
    _lineStart = _line;
    _jspBuilder.startElement(qname);

    for (int i = 0; i < keys.size(); i++) {
      _jspBuilder.attribute(keys.get(i), values.get(i));
    }
    _jspBuilder.endAttributes();

    if (qname.equals(JSP_DIRECTIVE_TAGLIB))
      processTaglibDirective(keys, values);

    setLocation();
    _jspBuilder.endElement(qname.getName());

    if (qname.equals(JSP_DIRECTIVE_PAGE)
        || qname.equals(JSP_DIRECTIVE_TAG)) {
      String contentEncoding = _parseState.getPageEncoding();
      if (contentEncoding == null)
        contentEncoding = _parseState.getCharEncoding();

      if (contentEncoding != null) {
        try {
          _stream.setEncoding(contentEncoding);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);

          throw error(L.l("unknown content encoding '{0}'", contentEncoding),
                      e);
        }
      }
    }
    /*
    if (directive.equals("include"))
      parseIncludeDirective(elt);
    else if (directive.equals("taglib"))
      parseTaglibDirective(elt);
    */
  }

  /**
   * Parses an XML comment.
   */
  private void parseComment()
    throws IOException, JspParseException
  {
    int ch = read();

    while (ch >= 0) {
      if (ch == '-') {
        ch = read();
        while (ch == '-') {
          if ((ch = read()) == '-')
            continue;
          else if (ch == '%' && (ch = read()) == '>')
            return;
          else if (ch == '-')
            ch = read();
        }
      }
      else
        ch = read();
    }
  }

  private void parseXmlComment()
    throws IOException, JspParseException
  {
    int ch;

    while ((ch = read()) >= 0) {
      while (ch == '-') {
        if ((ch = read()) == '-' && (ch = read()) == '>')
          return;
      }
    }
  }

  /**
   * Parses the open tag.
   */
  private void parseOpenTag(String name, int ch, boolean isXml)
    throws IOException, JspParseException
  {
    addText();

    ch = skipWhitespace(ch);

    ArrayList<QName> keys = new ArrayList<QName>();
    ArrayList<String> values = new ArrayList<String>();
    ArrayList<String> prefixes = new ArrayList<String>();
    ArrayList<String> uris = new ArrayList<String>();

    unread(ch);

    parseAttributes(keys, values, prefixes, uris);

    QName qname = getElementQName(name);

    setLocation(_jspPath, _filename, _lineStart);
    _lineStart = _line;

    _jspBuilder.startElement(qname);


    for (int i = 0; i < keys.size(); i++) {
      QName key = keys.get(i);
      String value = values.get(i);

      _jspBuilder.attribute(key, value);
    }

    _jspBuilder.endAttributes();

    for (int i = 0; i < prefixes.size(); i++) {
      String prefix = prefixes.get(i);
      String uri = uris.get(i);

      _jspBuilder.addNamespace(prefix, uri);
    }

    if (qname.equals(JSP_DIRECTIVE_TAGLIB))
      processTaglibDirective(keys, values);

    ch = skipWhitespace(read());

    JspNode node = _jspBuilder.getCurrentNode();

    if (ch == '/') {
      if ((ch = read()) != '>')
        throw error(L.l("expected '/>' at '{0}' (for tag '<{1}>' at line {2}).  The XML empty tag syntax is: <tag attr1='value1'/>",
                        badChar(ch), name, String.valueOf(_lineStart)));

      setLocation();
      _jspBuilder.endElement(qname.getName());
    }
    else if (ch != '>')
      throw error(L.l("expected '>' at '{0}' (for tag '<{1}>' at line {2}).  The XML tag syntax is: <tag attr1='value1'>",
                      badChar(ch), name, String.valueOf(_lineStart)));
    // If tagdependent and not XML mode, then read the raw text.
    else if ("tagdependent".equals(node.getBodyContent()) && ! _isXml) {
      String tail = "</" + name + ">";
      for (ch = read(); ch >= 0; ch = read()) {
        _text.append((char) ch);
        if (_text.endsWith(tail)) {
          _text.setLength(_text.length() - tail.length());
          addText();
          _jspBuilder.endElement(qname.getName());
          return;
        }
      }
      throw error(L.l("expected '{0}' at end of file (for tag <{1}> at line {2}).  Tags with 'tagdependent' content need close tags.",
                      tail, String.valueOf(_lineStart)));
    }
  }

  /**
   * Returns the full QName for the JSP page's name.
   */
  private QName getElementQName(String name)
  {
    int p = name.lastIndexOf(':');

    if (p > 0) {
      String prefix = name.substring(0, p);
      String url = Namespace.find(_namespaces, prefix);

      _prefixes.add(prefix);

      if (url != null)
        return new QName(prefix, name.substring(p + 1), url);
      else
        return new QName("", name, "");
    }
    else {
      String url = Namespace.find(_namespaces, "");

      if (url != null)
        return new QName("", name, url);
      else
        return new QName("", name, "");
    }
  }

  /**
   * Returns the full QName for the JSP page's name.
   */
  private QName getAttributeQName(String name)
  {
    int p = name.lastIndexOf(':');

    if (p > 0) {
      String prefix = name.substring(0, p);
      String url = Namespace.find(_namespaces, prefix);

      if (url != null)
        return new QName(prefix, name.substring(p + 1), url);
      else
        return new QName("", name, "");
    }
    else
      return new QName("", name, "");
  }

  /**
   * Parses the attributes of an element.
   */
  private void parseAttributes(ArrayList<QName> names,
                               ArrayList<String> values,
                               ArrayList<String> prefixes,
                               ArrayList<String> uris)
    throws IOException, JspParseException
  {
    int ch = skipWhitespace(read());

    while (XmlChar.isNameStart(ch)) {
      ch = readName(ch);
      String key = _tag.toString();

      readValue(key, ch, _isXml);
      String value = _value.toString();

      if (key.startsWith("xmlns:")) {
        String prefix = key.substring(6);

        _jspBuilder.startPrefixMapping(prefix, value);
        //_parseState.pushNamespace(prefix, value);
        prefixes.add(prefix);
        uris.add(value);

        _namespaces = new Namespace(_namespaces, prefix, value);
      }
      else if (key.equals("xmlns")) {
        _jspBuilder.startPrefixMapping("", value);
        //_parseState.pushNamespace(prefix, value);
        //_parseState.pushNamespace("", value);
        _namespaces = new Namespace(_namespaces, "", value);
      }
      else {
        names.add(getAttributeQName(key));
        values.add(value);
      }

      ch = skipWhitespace(read());
    }

    unread(ch);
  }

  /**
   * Reads an attribute value.
   */
  private void readValue(String attribute, int ch, boolean isXml)
    throws IOException, JspParseException
  {
    boolean isRuntimeAttribute = false;

    _value.clear();

    ch = skipWhitespace(ch);

    if (ch != '=') {
      unread(ch);
      return;
    }

    ch = skipWhitespace(read());

    if (ch != '\'' && ch != '"') {
      if (XmlChar.isNameChar(ch)) {
        ch = readName(ch);

        throw error(L.l("'{0}' attribute value must be quoted at '{1}'.  JSP attribute syntax is either attr=\"value\" or attr='value'.",
                        attribute, _tag));
      }
      else
        throw error(L.l("'{0}' attribute value must be quoted at '{1}'.  JSP attribute syntax is either attr=\"value\" or attr='value'.",
                        attribute, badChar(ch)));
    }

    int end = ch;
    int lastCh = 0;

    ch = read();
    if (ch != '<') {
    }
    else if ((ch = read()) != '%')
      _value.append('<');
    else if ((ch = read()) != '=')
      _value.append("<%");
    else {
      _value.append("<%");
      isRuntimeAttribute = true;
    }

    while (ch != -1 && (ch != end || isRuntimeAttribute)) {
      if (ch == '\\') {
        switch ((ch = read())) {
        case '\\':
        case '\'':
        case '\"':
          // jsp/1505 vs jsp/184s
          // _value.append('\\');
          _value.append((char) ch);
          ch = read();
          break;

        case '>':
          if (lastCh == '%') {
            _value.append('>');
            ch = read();
          }
          else
            _value.append('\\');
          break;

        case '%':
          if (lastCh == '<') {
            _value.append('%');
            ch = read();
          }
          else
            _value.append('\\');
          break;

        default:
          _value.append('\\');
          break;
        }
      }
      else if (ch == '%' && isRuntimeAttribute) {
        _value.append('%');
        ch = read();
        if (ch == '>')
          isRuntimeAttribute = false;
      }
      else if (ch == '&' && isXml) {
        lastCh = -1;
        _value.append((char) parseEntity());
        ch = read();
      }
      else if (ch == '&') {
        if ((ch = read()) == 'a') {
          if ((ch = read()) != 'p')
            _value.append("&a");
          else if ((ch = read()) != 'o')
            _value.append("&ap");
          else if ((ch = read()) != 's')
            _value.append("&apo");
          else if ((ch = read()) != ';')
            _value.append("&apos");
          else {
            _value.append('\'');
            ch = read();
          }
        }
        else if (ch == 'q') {
          if ((ch = read()) != 'u')
            _value.append("&q");
          else if ((ch = read()) != 'o')
            _value.append("&qu");
          else if ((ch = read()) != 't')
            _value.append("&quo");
          else if ((ch = read()) != ';')
            _value.append("&quot");
          else {
            _value.append('"');
            ch = read();
          }
        }
        else
          _value.append('&');
      }
      else {
        _value.append((char) ch);
        lastCh = ch;
        ch = read();
      }
    }
  }

  /**
   * Parses an XML entity.
   */
  int parseEntity()
    throws IOException, JspParseException
  {
    int ch = read();

    if (_isXml && ch == '#') {
      int value = 0;

      for (ch = read(); ch >= '0' && ch <= '9'; ch = read())
        value = 10 * value + ch - '0';

      if (ch != ';')
        throw error(L.l("expected ';' at '{0}' in character entity.  The XML character entities syntax is &#nn;",
                        badChar(ch)));

      return (char) value;
    }

    CharBuffer cb = CharBuffer.allocate();
    for (; ch >= 'a' && ch <= 'z'; ch = read())
      cb.append((char) ch);

    if (ch != ';') {

      log.warning(L.l("expected ';' at '{0}' in entity '&{1}'.  The XML entity syntax is &name;",
                      badChar(ch), cb));
    }

    String entity = cb.close();
    if (entity.equals("lt"))
      return '<';
    else if (entity.equals("gt"))
      return '>';
    else if (entity.equals("amp"))
      return '&';
    else if (entity.equals("apos"))
      return '\'';
    else if (entity.equals("quot"))
      return '"';
    else
      throw error(L.l("unknown entity '&{0};'.  XML only recognizes the special entities &lt;, &gt;, &amp;, &apos; and &quot;", entity));
  }

  private int parseCloseTag()
    throws IOException, JspParseException
  {
    int ch;

    if (! XmlChar.isNameStart(ch = read())) {
      addText("</");
      return ch;
    }

    ch = readName(ch);
    String name = _tag.toString();
    if (! _isXml && getTag(name) == TAG_UNKNOWN) {
      addText("</");
      addText(name);
      return ch;
    }

    ch = skipWhitespace(ch);
    if (ch != '>')
      throw error(L.l("expected '>' at {0}.  The XML close tag syntax is </name>.", badChar(ch)));

    JspNode node = _jspBuilder.getCurrentNode();
    String nodeName = node.getTagName();
    if (nodeName.equals(name)) {
    }
    else if (nodeName.equals("resin-c:when")) {
      throw error(L.l("#if expects closing #end before </{0}> (#if at {1}).  #if statements require #end before the enclosing tag closes.",
                      name, String.valueOf(node.getStartLine())));
    }
    else if (nodeName.equals("resin-c:otherwise")) {
      throw error(L.l("#else expects closing #end before </{0}> (#else at {1}).  #if statements require #end before the enclosing tag closes.",
                      name, String.valueOf(node.getStartLine())));
    }
    else {
      throw error(L.l("expected </{0}> at </{1}>.  Closing tags must match opened tags.",
                      nodeName, name));
    }

    addText();

    setLocation();
    _jspBuilder.endElement(name);

    return read();
  }

  private void processTaglibDirective(ArrayList<QName> keys,
                                      ArrayList<String> values)
    throws IOException, JspParseException
  {
    int p = keys.indexOf(PREFIX);
    if (p < 0)
      throw error(L.l("The taglib directive requires a 'prefix' attribute.  'prefix' is the XML prefix for all tags in the taglib."));
    String prefix = values.get(p);

    if (_prefixes.contains(prefix)
        && _parseState.getQName(prefix) == null) {
      throw error(L.l("The taglib prefix '{0}' must be defined before it is used.",
                      prefix));
    }

    if (_localPrefixes.contains(prefix))
      throw error(L.l(
        "<{0}> cannot occur after an action that uses the same prefix: {1}.",
        JSP_DIRECTIVE_TAGLIB.getName(),
        prefix));

    String uri = null;
    p = keys.indexOf(URI);
    if (p >= 0)
      uri = values.get(p);

    String tagdir = null;
    p = keys.indexOf(TAGDIR);
    if (p >= 0)
      tagdir = values.get(p);

    if (uri != null)
      processTaglib(prefix, uri);
    else if (tagdir != null)
      processTaglibDir(prefix, tagdir);
  }

  /**
   * Adds a new known taglib prefix to the current namespace.
   */
  private void processTaglib(String prefix, String uri)
    throws JspParseException
  {
    Taglib taglib = null;

    int colon = uri.indexOf(':');
    int slash = uri.indexOf('/');

    String location = null;

    if (colon > 0 && colon < slash)
      location = uri;
    else if (slash == 0)
      location = uri;
    else
      location = _uriPwd + uri;

    try {
      taglib = _tagManager.addTaglib(prefix, uri, location);
      String tldURI = "urn:jsptld:" + uri;

      _parseState.pushNamespace(prefix, tldURI);
      _namespaces = new Namespace(_namespaces, prefix, tldURI);
      return;
    } catch (JspParseException e) {
      throw error(e);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (colon > 0 && colon < slash)
      throw error(L.l("Unknown taglib '{0}'.  Taglibs specified with an absolute URI must either be:\n1) specified in the web.xml\n2) defined in a jar's .tld in META-INF\n3) defined in a .tld in WEB-INF\n4) predefined by Resin",
                      uri));
  }

  /**
   * Adds a new known tag dir to the current namespace.
   */
  private void processTaglibDir(String prefix, String tagDir)
    throws JspParseException
  {
    Taglib taglib = null;

    try {
      taglib = _tagManager.addTaglibDir(prefix, tagDir);
      String tagURI = "urn:jsptagdir:" + tagDir;
      _parseState.pushNamespace(prefix, tagURI);
      _namespaces = new Namespace(_namespaces, prefix, tagURI);
      return;
    } catch (JspParseException e) {
      throw error(e);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  private void processIncludeDirective(ArrayList keys, ArrayList values)
    throws IOException, JspParseException
  {
    int p = keys.indexOf("file");
    if (p < 0)
      throw error(L.l("The include directive requires a 'file' attribute."));
    String file = (String) values.get(p);

    pushInclude(file);
  }

  public void pushInclude(String value)
    throws IOException, JspParseException
  {
    pushInclude(value, false);
  }

  public void pushInclude(String value, boolean allowDuplicate)
    throws IOException, JspParseException
  {
    if (value.equals(""))
      throw error("include directive needs 'file' attribute. Use either <%@ include file='myfile.jsp' %> or <jsp:directive.include file='myfile.jsp'/>");

    Path include;
    if (value.length() > 0 && value.charAt(0) == '/')
      include = _parseState.resolvePath(value);
    else
      include = _parseState.resolvePath(_uriPwd + value);

    String newUrl = _uriPwd;

    if (value.startsWith("/"))
      newUrl = value;
    else
      newUrl = _uriPwd + value;

    include.setUserPath(newUrl);

    String newUrlPwd;
    int p = newUrl.lastIndexOf('/');
    newUrlPwd = newUrl.substring(0, p + 1);

    if (_jspPath != null && _jspPath.equals(include) && ! allowDuplicate)
      throw error(L.l("circular include of '{0}' forbidden.  A JSP file may not include itself.", include));
    for (int i = 0; i < _includes.size(); i++) {
      Include inc = _includes.get(i);
      if (inc._stream != null && inc._stream.getPath() != null
          && inc._stream.getPath().equals(include) && ! allowDuplicate)
        throw error(L.l("circular include of '{0}'.  A JSP file may not include itself.", include));
    }

    try {
      addInclude(include.openRead(), newUrlPwd);
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);

      if (include.exists())
        throw error(L.l("can't open include of '{0}'.  '{1}' exists but it's not readable.",
                        value, include.getNativePath()));
      else
        throw error(L.l("can't open include of '{0}'.  '{1}' does not exist.",
                        value, include.getNativePath()));
    }
  }

  private void addInclude(ReadStream stream, String newUrlPwd)
    throws IOException, JspParseException
  {
    addText();

    readLf();

    Include inc = null;

    if (_stream != null) {
      inc = new Include(_localPrefixes,
                        _stream,
                        _line,
                        _uriPwd,
                        _parseState.isLocalScriptingInvalid());

      _parseState.setLocalScriptingInvalid(false);

      _includes.add(inc);

      _localPrefixes = new HashSet<String>();
    }

    _parseState.addDepend(stream.getPath());

    try {
      String encoding = _stream.getEncoding();
      if (encoding != null)
        stream.setEncoding(encoding);
    } catch (Exception e) {
    }
    _stream = stream;

    _filename = stream.getUserPath();
    _jspPath = stream.getPath();
    _line = 1;
    _lineStart = _line;
    _uriPwd = newUrlPwd;
    _parseState.setUriPwd(_uriPwd);
  }

  /**
   * Skips whitespace characters.
   *
   * @param ch the current character
   *
   * @return the first non-whitespace character
   */
  private int skipWhitespace(int ch)
    throws IOException, JspParseException
  {
    for (; XmlChar.isWhitespace(ch); ch = read()) {
    }

    return ch;
  }

  /**
   * Skips whitespace to end of line
   *
   * @param ch the current character
   *
   * @return the first non-whitespace character
   */
  private int skipWhitespaceToEndOfLine(int ch)
    throws IOException, JspParseException
  {
    for (; XmlChar.isWhitespace(ch); ch = read()) {
      if (ch == '\n')
        return read();
      else if (ch == '\r') {
        ch = read();
        if (ch == '\n')
          return read();
        else
          return ch;
      }
    }

    return ch;
  }

  private void addText(char ch)
  {
    _text.append(ch);
  }

  private void addText(String s)
  {
    _text.append(s);
  }

  private void addText()
    throws JspParseException
  {
    if (_text.length() > 0)
      createText();

    _startText = _charCount;
    _lineStart = _line;
  }

  private void createText()
    throws JspParseException
  {
    String string = _text.toString();

    setLocation(_jspPath, _filename, _lineStart);

    if (_parseState.isTrimWhitespace() && isWhitespace(string)) {
    }
    else
      _jspBuilder.text(string, _filename, _lineStart, _line);

    _lineStart = _line;
    _text.clear();
    _startText = _charCount;
  }

  private boolean isWhitespace(String s)
  {
    int length = s.length();

    for (int i = 0; i < length; i++) {
      if (! Character.isWhitespace(s.charAt(i)))
        return false;
    }

    return true;
  }


  /**
   * Checks to see if the element name represents a tag.
   */
  private int getTag(String name)
    throws JspParseException
  {
    int p = name.indexOf(':');
    if (p < 0)
      return TAG_UNKNOWN;

    String prefix = name.substring(0, p);
    String local = name.substring(p + 1);

    _prefixes.add(prefix);
    _localPrefixes.add(prefix);

    String url = Namespace.find(_namespaces, prefix);

    if (url != null)
      return TAG_JSP;
    else
      return TAG_UNKNOWN;

    /*
    QName qname;

    if (url != null)
      qname = new QName(prefix, local, url);
    else
      qname = new QName(prefix, local, null);

    TagInfo tag = _tagManager.getTag(qname);

    if (tag != null)
      return TAG_JSP;
    else
      return TAG_UNKNOWN;
    */
  }

  private void unread(int ch)
  {
    _peek = ch;
  }

  /**
   * Reads the next character we're in the middle of cr/lf.
   */
  private void readLf() throws IOException, JspParseException
  {
    if (_seenCr) {
      int ch = read();

      if (ch != '\n')
        _peek = ch;
    }
  }

  /**
   * Reads the next character.
   */
  private int read() throws IOException, JspParseException
  {
    int ch;

    if (_peek >= 0) {
      ch = _peek;
      _peek = -1;
      return ch;
    }

    try {
      if ((ch = _stream.readChar()) >= 0) {
        _charCount++;

        if (ch == '\r') {
          _line++;
          _charCount = 0;
          _seenCr = true;
        }
        else if (ch == '\n' && _seenCr) {
          _seenCr = false;
          _charCount = 0;
        }
        else if (ch == '\n') {
          _line++;
          _charCount = 0;
        }
        else {
          _seenCr = false;
        }

        return ch;
      }
    } catch (IOException e) {
      throw error(e.toString());
    }

    _stream.close();
    _seenCr = false;

    if (_includes.size() > 0) {
      setLocation(_jspPath, _filename, _line);

      Include include = _includes.get(_includes.size() - 1);
      _includes.remove(_includes.size() - 1);

      _stream = include._stream;
      _filename = _stream.getUserPath();
      _jspPath = _stream.getPath();
      _line = include._line;
      _lineStart = _line;
      _uriPwd = include._uriPwd;
      _localPrefixes = include._localPrefixes;

      _parseState.setUriPwd(_uriPwd);
      _parseState.setLocalScriptingInvalid(include._oldLocalScriptingDisabled);

      setLocation(_jspPath, _filename, _line);

      return read();
    }

    return -1;
  }

  void clear(Path appDir, String errorPage)
  {
  }

  /**
   * Creates an error message adding the filename and line.
   *
   * @param e the exception
   */
  public JspParseException error(Exception e)
  {
    String message = e.getMessage();

    if (e instanceof JspParseException) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (e instanceof JspLineParseException)
      return (JspLineParseException) e;
    else if (e instanceof LineCompileException)
      return new JspLineParseException(e);

    if (_lineMap == null)
      return new JspLineParseException(_filename + ":" + _line + ": "  + message,
                                       e);
    else {
      LineMap.Line line = _lineMap.getLine(_line);

      return new JspLineParseException(line.getSourceFilename() + ":" +
                                       line.getSourceLine(_line) + ": "  +
                                       message,
                                       e);
    }
  }

  /**
   * Creates an error message adding the filename and line.
   *
   * @param message the error message
   */
  public JspParseException error(String message)
  {
    JspGenerator gen = _jspBuilder.getGenerator();

    if (_lineMap == null) {
      if (gen != null)
        return new JspLineParseException(_filename + ":" + _line + ": "  + message + gen.getSourceLines(_jspPath, _line));
      else
        return new JspLineParseException(_filename + ":" + _line + ": "  + message);
    }
    else {
      LineMap.Line line = _lineMap.getLine(_line);

      return new JspLineParseException(line.getSourceFilename() + ":" +
                                       line.getSourceLine(_line) + ": "  +
                                       message);
    }
  }

  /**
   * Creates an error message adding the filename and line.
   *
   * @param message the error message
   */
  public JspParseException error(String message, Throwable e)
  {
    if (_lineMap == null)
      return new JspLineParseException(_filename + ":" + _line + ": "  + message, e);
    else {
      LineMap.Line line = _lineMap.getLine(_line);

      return new JspLineParseException(line.getSourceFilename() + ":" +
                                       line.getSourceLine(_line) + ": "  +
                                       message,
                                       e);
    }
  }

  /**
   * Sets the current location in the original file
   */
  private void setLocation()
  {
    setLocation(_jspPath, _filename, _line);
  }

  /**
   * Sets the current location in the original file
   *
   * @param filename the filename
   * @param line the line in the source file
   */
  private void setLocation(Path jspPath, String filename, int line)
  {
    if (_lineMap == null) {
      _jspBuilder.setLocation(jspPath, filename, line);
    }
    else {
      LineMap.Line srcLine = _lineMap.getLine(line);

      if (srcLine != null) {
        _jspBuilder.setLocation(jspPath,
                                srcLine.getSourceFilename(),
                                srcLine.getSourceLine(line));
      }
    }
  }

  private String badChar(int ch)
  {
    if (ch < 0)
      return "end of file";
    else if (ch == '\n' || ch == '\r')
      return "end of line";
    else if (ch >= 0x20 && ch <= 0x7f)
      return "'" + (char) ch + "'";
    else
      return "'" + (char) ch + "' (\\u" + hex(ch) + ")";
  }

  private String hex(int value)
  {
    CharBuffer cb = new CharBuffer();

    for (int b = 3; b >= 0; b--) {
      int v = (value >> (4 * b)) & 0xf;
      if (v < 10)
        cb.append((char) (v + '0'));
      else
        cb.append((char) (v - 10 + 'a'));
    }

    return cb.toString();
  }

  class Include {
    ReadStream _stream;
    int _line;
    String _uriPwd;
    Set<String> _localPrefixes;
    boolean _oldLocalScriptingDisabled;

    Include(Set<String> prefixes,
            ReadStream stream,
            int line,
            String uriPwd,
            boolean oldLocalScriptingDisabled
    )
    {
      _stream = stream;
      _line = line;
      _uriPwd = uriPwd;
      _localPrefixes = prefixes;
      _oldLocalScriptingDisabled = oldLocalScriptingDisabled;
    }
  }
}
