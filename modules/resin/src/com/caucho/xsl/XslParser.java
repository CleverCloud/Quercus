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

import com.caucho.util.CharBuffer;
import com.caucho.util.CharCursor;
import com.caucho.util.CharScanner;
import com.caucho.util.L10N;
import com.caucho.util.StringCharCursor;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.xml.*;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Parses a 'StyleScript' file.  StyleScript is compatible with XSLT, adding
 * some syntactical sugar:
 *
 * <p>path &lt;&lt; ... >> is shorthand for
 * &lt;xsl:template match='path'> ... &lt;/xsl:template>

 * <p>&lt;{...}> is shorthand for
 * &lt;xsl:value-of select='...'/>
 */
class XslParser {
  private static final Logger log
    = Logger.getLogger(XslParser.class.getName());
  static final L10N L = new L10N(XslParser.class);

  private static final String XSLNS = Generator.XSLNS;
  private static final String XTPNS = Generator.XTPNS;

  // this is the value Axis wants
  static final String XMLNS = "http://www.w3.org/2000/xmlns/";
  
  static HashMap<String,String> _xslCommands;
  static HashMap<String,String> _xtpCommands;

  public boolean strictXsl;
  boolean rawText;

  int line;
  int textLine;
  ReadStream is;
  CharBuffer tag = new CharBuffer();
  CharBuffer text = new CharBuffer();
  QDocument xsl;
  private int peek = -1;
  private boolean seenCr;
  private String defaultMode;
  private HashMap<String,String> _namespaces;
  private HashMap macros = new HashMap();
  private boolean inTemplate;

  XslParser()
  {
  }

  /**
   * Parse an XSLT-lite document from the input stream, returning a Document.
   */
  Document parse(ReadStream is)
    throws IOException, XslParseException
  {
    this.is = is;

    line = 1;
    defaultMode = null;
    xsl = (QDocument) Xml.createDocument();
    if (is.getPath().getLastModified() > 0) {
      ArrayList<Path> depends = new ArrayList<Path>();
      depends.add(is.getPath());
      xsl.setProperty(xsl.DEPENDS, depends);
    }

    xsl.setRootFilename(is.getPath().getURL());

    _namespaces = new HashMap<String,String>();

    QNode top = (QNode) xsl.createDocumentFragment();
    top.setLocation(is.getPath().getURL(), is.getUserPath(), line, 0);
    rawText = false;

    String encoding = null;
    int ch = read();

    if (ch != 0xef) {
    } else if ((ch = read()) != 0xbb) {
      peek = 0xbb;
      ch = 0xef;
    } else if ((ch = read()) != 0xbf) {
      throw error(L.l("Expected 0xbf in UTF-8 header"));
    } else {
      is.setEncoding("UTF-8");
      ch = read();
    }
    
    if (ch == '<') {
      ch = read();
      if (ch == '?') {
        ProcessingInstruction pi = parsePi();
        if (pi.getNodeName().equals("xml")) {
          encoding = XmlUtil.getPIAttribute(pi.getNodeValue(), "encoding");
          if (encoding != null)
            is.setEncoding(encoding);
        }
        else
          top.appendChild(pi);
        ch = read();
      } else {
        peek = ch;
        ch = '<';
      }
    }

    parseNode(top, "", true, ch);

    QElement elt = null;
    for (Node node = top.getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      if (node.getNodeType() == Node.ELEMENT_NODE &&
          node.getNodeName().equals("xsl:stylesheet")) {
        if (elt != null)
          throw error(L.l("xsl:stylesheet must be sole top element"));
        elt = (QElement) node;
      }
    }
    if (elt == null) {
      elt = (QElement) xsl.createElementNS(XSLNS, "xsl:stylesheet");
      elt.setAttribute("version", "1.0");
      elt.setLocation(is.getURL(), is.getUserPath(), 1, 0);
      elt.setAttribute("resin:stylescript", "true");

      Element out = xsl.createElementNS(XSLNS, "xsl:output");
      //out.setAttribute("method", "xtp");
      //out.setAttribute("disable-output-escaping", "true");
      //out.setAttribute("omit-xml-declaration", "true");
      
      elt.appendChild(out);
      elt.appendChild(top);
    }

    // elt.setAttribute("xsl-caucho", "true");

    if (encoding != null) {
      Element out = xsl.createElementNS(XSLNS, "xsl:output");
      out.setAttribute("encoding", encoding);
      elt.insertBefore(out, elt.getFirstChild());
    }

    xsl.appendChild(elt);

    /*
    if (dbg.canWrite()) {
      new XmlPrinter(dbg).printPrettyXml(xsl);
    }
    */

    return xsl;
  }

  /**
   * Parses in the middle of a node
   *
   * @param parent parsed children are attached to the parent node
   */
  private void parseNode(Node parent, String tagEnd,
                         boolean isSpecial, int ch)
    throws IOException, XslParseException
  {
    boolean hasContent = false;
    
    text.clear();
    if (tagEnd == ">>" && (ch == '\n' || ch == '\r'))
      ch = read();

    while (ch >= 0) {
      switch (ch) {
      case '\\':
        hasContent = true;
        ch = read();
        if (ch == '<') {
          addText('<');
          ch = read();
        }
        else
          addText('\\');
        break;

      case '<':
        hasContent = true;
        ch = read();

        if (ch == '/') {
          ch = readTag(read());
          String tag = this.tag.toString();
          if (tag.equals(tagEnd)) {
            ch = skipWhitespace(ch);
            if (ch != '>')
              throw error(L.l("expected `{0}' at {1}", ">", badChar(ch)));
            addText(parent);
            if (tag.equals("xsl:template"))
              inTemplate = false;
            return;
          }
          else if (rawText) {
            addText("</" + tag + ">");
            ch = read();
            break;
          }
          else {
            throw error(L.l("`</{0}>' has no matching open tag", tag));
          }
        } else if (ch == '#') {
          addText(parent);
          ch = parseScriptlet(parent);
          break;
        } else if (ch == '?') {
          addText(parent);
          ProcessingInstruction pi = parsePi();
          parent.appendChild(pi);
          ch = read();
          break;
        } else if (ch == '!') {
          addText(parent);
          ch = parseDecl(parent);
          break;
        } else if (ch == '{') {
          addText(parent);
          parseValueOf(parent);
          ch = read();
          break;
        }

        ch = readTag(ch);
        String tag = this.tag.toString();

        // treat the tag as XML when it has a known prefix or we aren't
        // in rawText mode
        if (! rawText && ! tag.equals("") ||
            tag.startsWith("xsl:") ||
            tag.startsWith("jsp:") || tag.startsWith("xtp:") ||
            macros.get(tag) != null) {
          addText(parent);

          parseElement(parent, tag, ch, isSpecial);

          ch = read();
        }
        // otherwise tread the tag as text
        else {
          addText("<");
          addText(tag);
        }
        break;

      case '>':
        int ch1 = read();
        if (ch1 == '>' && tagEnd == ">>") {
          if (text.length() > 0 && text.charAt(text.length() - 1) == '\n')
            text.setLength(text.length() - 1);
          if (text.length() > 0 && text.charAt(text.length() - 1) == '\r')
            text.setLength(text.length() - 1);
          if (! hasContent) {
            Element elt = xsl.createElementNS(XSLNS, "xsl:text");
            parent.appendChild(elt);
            addText(elt);
          }
          else
            addText(parent);
          return;
        }
        else {
          hasContent = true;
          addText('>');
          ch = ch1;
        }
        break;

      case '$':
        hasContent = true;
        ch = read();
        if (ch == '$') {
          addText('$');
          ch = read();
        }
        else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
          String name = parseName(ch);
          addText(parent);
          text.clear();

          ch = parseExtension(parent, name);
        }
        else if (ch == '(') {
          addText(parent);
          Element elt = xsl.createElementNS(XSLNS, "xsl:value-of");
          CharBuffer test = CharBuffer.allocate();
          lexToRparen(test);
          elt.setAttribute("select", test.close());
          parent.appendChild(elt);
          ch = read();
        }
        else {
          addText('$');
          ch = read();
        }
        break;

      case ' ': case '\t': case '\n': case '\r':
        addText((char) ch);
        ch = read();
        break;

      case '&':
        ch = parseEntityReference();
        break;

      default:
        hasContent = true;
        if (isSpecial) {
          parseSpecial(parent, ch);
          ch = read();
        }
        else {
          addText((char) ch);
          ch = read();
        }
        break;
      }
    }

    addText(parent);

    if (! tagEnd.equals(""))
      throw error(L.l("expected close of `{0}' (open at {1})",
                      tagEnd, ((CauchoNode) parent).getLine()));
  }

  /**
   * Parses an element.
   *
   * @param parent the owning node of the new child
   * @param name the name of the element
   * @param ch the current character
   * @param isSpecial ??
   *
   * @return the new child element
   */
  private Element parseElement(Node parent, String name,
                               int ch, boolean isSpecial)
    throws IOException, XslParseException
  {
    HashMap<String,String> oldNamespaces = _namespaces;

    QElement element = null;
    
    int p = name.indexOf(':');
    if (p >= 0) {
      String prefix = name.substring(0, p);
      String uri = _namespaces.get(prefix);

      if (uri != null)
        element = (QElement) xsl.createElementNS(uri, name);
      else if (prefix.equals("xsl"))
        element = (QElement) xsl.createElementNS(XSLNS, name);
    }

    try {
      if (element == null)
        element = (QElement) xsl.createElement(name);
    } catch (DOMException e) {
      throw error(e);
    }
    
    element.setLocation(is.getURL(), is.getUserPath(), line, 0);

    ch = parseAttributes(null, element, ch, false);

    if (name.equals("xsl:stylesheet")) {
      if (element.getAttribute("parsed-content").equals("false")) {
        rawText = true;
        Element child = xsl.createElementNS(XSLNS, "xsl:output");
        child.setAttribute("disable-output-escaping", "yes");
        element.appendChild(child);
      }
    }

    if (rawText && (name.startsWith("xsl") || name.startsWith("xtp")) &&
        element.getAttribute("xml:space").equals(""))
      element.setAttribute("xml:space", "default");

    if (name.equals("xsl:template")) {
      inTemplate = true;
      String macroName = element.getAttribute("name");
      if (! macroName.equals(""))
        macros.put(macroName, macroName);
    }

    String oldMode = defaultMode;
    if (name.equals("xtp:mode")) {
      defaultMode = element.getAttribute("mode");
    }
    else {
      parent.appendChild(element);
      parent = element;
    }

    if (ch == '>') {
      parseNode(parent, name, isSpecial && name.equals("xsl:stylesheet"),
                read());
    } else if (ch == '/') {
      if ((ch = read()) != '>')
        throw error(L.l("expected `{0}' at {1}", ">", badChar(ch)));
    } else
      throw error(L.l("expected `{0}' at {1}", ">", badChar(ch)));

    defaultMode = oldMode;
    _namespaces = oldNamespaces;

    return element;
  }

  /**
   * Parses an entity reference, e.g. &amp;lt;
   */
  private int parseEntityReference()
    throws IOException, XslParseException
  {
    int ch = read();

    if (ch == '#') {
      int code = 0;

      ch = read();

      if (ch == 'x') {
        for (ch = read(); ch > 0 && ch != ';'; ch = read()) {
          if (ch >= '0' && ch <= '9')
            code = 16 * code + ch - '0';
          else if (ch >= 'a' && ch <= 'f')
            code = 16 * code + ch - 'a' + 10;
          else if (ch >= 'A' && ch <= 'F')
            code = 16 * code + ch - 'A' + 10;
          else
            break;
        }

        if (ch == ';') {
          addText((char) code);
          return read();
        }
        else {
          addText("&#x");
          addText(String.valueOf(code));
          return ch;
        }
      }
      else {
        for (; ch >= '0' && ch <= '9'; ch = read()) {
          code = 10 * code + ch - '0';
        }
      }

      if (ch == ';') {
        addText((char) code);
        return read();
      }
      else {
        addText("&#");
        addText(String.valueOf(code));
        return ch;
      }
    }

    CharBuffer cb = CharBuffer.allocate();
    for (; ch >= 'a' && ch <= 'z'; ch = read())
      cb.append((char) ch);

    if (ch != ';') {
      addText('&');
      addText(cb.close());
    }
    else if (cb.matches("lt")) {
      addText('<');
      return read();
    }
    else if (cb.matches("gt")) {
      addText('>');
      return read();
    }
    else if (cb.matches("amp")) {
      addText('&');
      return read();
    }
    else if (cb.matches("quot")) {
      addText('"');
      return read();
    }
    else if (cb.matches("apos")) {
      addText('\'');
      return read();
    }
    else {
      addText('&');
      addText(cb.close());
    }

    return ch;
  }
  /**
   * Parses the contents of a '<#' section.
   */
  private int parseScriptlet(Node parent)
    throws IOException, XslParseException
  {
    String filename = is.getUserPath();
    int line = this.line;
    int ch = read();

    QNode node;
    if (ch == '=') {
      node = (QNode) xsl.createElementNS(XTPNS, "xtp:expression");
      ch = read();
    }
    else if (ch == '!') {
      node = (QNode) xsl.createElementNS(XTPNS, "xtp:declaration");
      ch = read();
    }
    else if (ch == '@') {
      parseDirective(parent);
      return read();
    } else
      node = (QNode) xsl.createElementNS(XTPNS, "xtp:scriptlet");
    node.setLocation(is.getURL(), is.getUserPath(), line, 0);
    parent.appendChild(node);

    text.clear();
    while (ch >= 0) {
      if (ch == '#') {
        ch = read();
        if (ch == '>')
          break;
        else
          addText('#');
      } else {
        addText((char) ch);
        ch = read();
      }
    }

    node.appendChild(xsl.createTextNode(text.toString()));
    text.clear();

    return read();
  }

  /**
   * parses an xtp directive: <#@
   */
  private void parseDirective(Node parent)
    throws IOException, XslParseException
  {
    int ch;

    ch = skipWhitespace(read());
    ch = readTag(ch);
    String name = tag.toString();
    if (! name.equals("page") && ! name.equals("cache"))
      throw error(L.l("unknown directive `{0}'", name));

    QElement elt = (QElement) xsl.createElementNS(XTPNS, "xtp:directive." + name);
    elt.setLocation(is.getURL(), is.getUserPath(), line, 0);
    parent.appendChild(elt);

    ch = parseAttributes(parent, elt, ch, true);

    if (ch != '#')
      throw error(L.l("expected `{0}' at {1}", "#", badChar(ch)));
    if ((ch = read()) != '>')
      throw error(L.l("expected `{0}' at {1}", ">", badChar(ch)));

    if (name.equals("page")) {
      String contentType = elt.getAttribute("contentType");
      if (! contentType.equals(""))
        parseContentType(parent, contentType);
    }
  }
  
  private int parseStatement(Node parent, int ch)
    throws IOException, XslParseException
  {
    ch = skipWhitespace(ch);

    if (ch == '$') {
      ch = read();
      
      if (XmlChar.isNameStart(ch)) {
        String name = parseName(ch);

        return parseExtension(parent, name);
      }
      else if (ch == '(') {
        Element elt = xsl.createElementNS(XSLNS, "xsl:value-of");
        CharBuffer test = CharBuffer.allocate();
        lexToRparen(test);
        elt.setAttribute("select", test.close());
        parent.appendChild(elt);
        return read();
      }
      else
        throw error(L.l("expected statement at {0}", badChar(ch)));
    }
    else if (ch == '<') {
      parseBlock(parent, ch);
      return read();
    }
    else if (ch == ';')
      return read();
    else
      throw error(L.l("expected statement at {0}", badChar(ch)));
  }

  private int parseExtension(Node parent, String name)
    throws IOException, XslParseException
  {
    int ch = read();

    if (name.equals("if"))
      return parseIf(parent, ch);

    String arg = (String) _xslCommands.get(name);

    if (arg != null) {
      QElement elt = (QElement) xsl.createElementNS(XSLNS, "xsl:" + name);
      elt.setLocation(is.getURL(), is.getUserPath(), line, 0);
      parent.appendChild(elt);

      ch = skipWhitespace(ch);

      if (ch == '(') {
        parseArgs(elt, arg);

        ch = skipWhitespace(read());
      }
      
      return parseStatement(elt, ch);
    }

    arg = (String) _xtpCommands.get(name);
    if (arg != null) {
      QElement elt = (QElement) xsl.createElement("xtp:" + name);
      elt.setLocation(is.getURL(), is.getUserPath(), line, 0);
      parent.appendChild(elt);

      ch = skipWhitespace(ch);

      if (ch == '(') {
        parseArgs(elt, arg);

        ch = skipWhitespace(read());
      }
      
      return parseStatement(elt, ch);
    }
    
    ch = skipWhitespace(ch);
      
    if (ch == '=') {
      QElement elt = (QElement) xsl.createElement("xtp:assign");
      elt.setLocation(is.getURL(), is.getUserPath(), line, 0);
      elt.setAttribute("name", name.intern());
      parent.appendChild(elt);
      ch = skipWhitespace(read());

      if (ch != '$')
        return parseStatement(elt, ch);
      else if ((ch = read()) != '(') {
        peek = ch;
        return parseStatement(elt, ch);
      }
      else {
        CharBuffer test = CharBuffer.allocate();
        lexToRparen(test);
        elt.setAttribute("select", test.close());
        return read();
      }
    }

    QElement elt = (QElement) xsl.createElement(name);
    elt.setLocation(is.getURL(), is.getUserPath(), line, 0);
    parent.appendChild(elt);

    if (ch == '(') {
      parseArgs(elt, arg);

      ch = skipWhitespace(read());
    }
      
    return parseStatement(elt, ch);
  }

  private int parseIf(Node parent, int ch)
    throws IOException, XslParseException
  {
    QElement choose = (QElement) xsl.createElementNS(XSLNS, "xsl:choose");
    choose.setLocation(is.getURL(), is.getUserPath(), line, 0);
    parent.appendChild(choose);

    while (true) {
      lexExpect(ch, '(');
      CharBuffer test = CharBuffer.allocate();
      lexToRparen(test);

      QElement elt = (QElement) xsl.createElementNS(XSLNS, "xsl:when");
      choose.appendChild(elt);
      elt.setLocation(is.getURL(), is.getUserPath(), line, 0);
      elt.setAttribute("test", test.close());

      ch = parseStatement(elt, skipWhitespace(read()));

      ch = skipWhitespace(ch);
      if (ch != '$')
        return ch;

      ch = read();
      if (! XmlChar.isNameStart(ch)) {
        peek = ch;
        return '$';
      }
      
      String name = parseName(ch);

      if (! name.equals("else"))
        return parseExtension(parent, name);
      
      ch = skipWhitespace(read());

      if (ch == '<') {
        elt = (QElement) xsl.createElementNS(XSLNS, "xsl:otherwise");
        choose.appendChild(elt);
        elt.setLocation(is.getURL(), is.getUserPath(), line, 0);
        
        return parseStatement(elt, skipWhitespace(ch));
      }

      name = parseName(read());
      if (! name.equals("if"))
        throw error(L.l("expected $if at `${0}'", name));

      ch = read();
    }
  }

  private String parseName(int ch)
    throws IOException, XslParseException
  {
    CharBuffer cb = CharBuffer.allocate();

    for (; XmlChar.isNameChar(ch); ch = read())
      cb.append((char) ch);

    peek = ch;

    return cb.close();
  }

  private void parseArgs(Element elt, String arg)
    throws IOException, XslParseException
  {
    CharBuffer cb = CharBuffer.allocate();
    String key = null;
    boolean isFirst = true;
    int ch;
    
    for (ch = read(); ch >= 0 && ch != ')'; ch = read()) {
      cb.append((char) ch);
      
      switch (ch) {
      case '(':
        lexToRparen(cb);
        cb.append(')');
        break;

      case '"': case '\'':
        lexString(cb, ch);
        break;
        
      case '=':
        ch = read();
        if (ch == '>') {
          cb.setLength(cb.length() - 1);
          key = cb.toString().trim();
          cb.clear();
        }
        else {
          peek = ch;
        }
        break;
        
      case ',':
        cb.setLength(cb.length() - 1);
        if (key != null)
          elt.setAttribute(key, cb.toString());
        else if (arg != null && isFirst)
          elt.setAttribute(arg, cb.toString());
        else
          throw error(L.l("unexpected arg `{0}'", cb));
        cb.clear();
        isFirst = false;
        key = null;
        break;
      }

    }

    if (ch != ')')
      throw error(L.l("expected `{0}' at {1}", ")", badChar(ch)));

    if (key != null)
      elt.setAttribute(key, cb.close());
    else if (arg != null && cb.length() > 0 && isFirst)
      elt.setAttribute(arg, cb.close());
    else if (cb.length() > 0)
      throw error(L.l("unexpected arg `{0}'", cb));
  }

  /**
   * Scan the buffer up to the right parenthesis.
   *
   * @param cb buffer holding the contents.
   */
  private void lexToRparen(CharBuffer cb)
    throws IOException, XslParseException
  {
    String filename = getFilename();
    int line = getLine();
    int ch;
    
    for (ch = read(); ch >= 0 && ch != ')'; ch = read()) {
      cb.append((char) ch);
      
      switch (ch) {
      case '(':
        lexToRparen(cb);
        cb.append(')');
        break;

      case '"': case '\'':
        lexString(cb, ch);
        break;
      }
    }

    if (ch != ')')
      throw error(L.l("expected `{0}' at {1}.  Open at {2}",
                      ")", badChar(ch), filename + ":" + line));
  }

  private void lexString(CharBuffer cb, int end)
    throws IOException, XslParseException
  {
    int ch;
    
    for (ch = read(); ch >= 0 && ch != end; ch = read()) {
      cb.append((char) ch);
    }

    if (ch != end)
      throw error(L.l("expected `{0}' at {1}", "" + (char) end, badChar(ch)));

    cb.append((char) end);
  }

  private void lexExpect(int ch, int match)
    throws IOException, XslParseException
  {
    for (; XmlChar.isWhitespace((char) ch); ch = read()) {
    }

    if (ch != match)
      throw error(L.l("expected `{0}' at {1}",
                      "" + (char) match, badChar(ch)));
  }

  private CharScanner wsScanner = new CharScanner(" \t");
  private CharScanner delimScanner = new CharScanner(" \t;=");

  /**
   * parse the content-type, possibly changing character-encoding.
   */
  private void parseContentType(Node parent, String contentType)
    throws IOException, XslParseException
  {
    CharCursor cursor = new StringCharCursor(contentType);

    CharBuffer buf = new CharBuffer();
    wsScanner.skip(cursor);
    delimScanner.scan(cursor, buf);

    if (buf.length() <= 0)
      return;

    Element output = xsl.createElementNS(XSLNS, "xsl:output");
    parent.appendChild(output);
    output.setAttribute("media-type", buf.toString());
    delimScanner.skip(cursor);

    buf.clear();
    delimScanner.scan(cursor, buf);
    wsScanner.skip(cursor);
    if (cursor.current() == '=' && buf.toString().equals("charset")) {
      delimScanner.skip(cursor);
      buf.clear();
      delimScanner.scan(cursor, buf);
      if (buf.length() > 0) {
        output.setAttribute("encoding", Encoding.getMimeName(buf.toString()));
        is.setEncoding(buf.toString());
      }
    }
  }

  /**
   * Parses the attributes of an element.
   *
   * @param parent the elements parent.
   * @param elt the element itself
   * @param ch the next character
   * @param isDirective true if this is a special directive
   *
   * @return the next character
   */
  private int parseAttributes(Node parent, Element elt,
                              int ch, boolean isDirective)
    throws IOException, XslParseException
  {
    HashMap<String,String> newNamespaces = null;
    
    ch = skipWhitespace(ch);
    while (XmlChar.isNameStart(ch)) {
      ch = readTag(ch);
      String name = tag.toString();

      ch = skipWhitespace(ch);
      String value = null;
      if (ch == '=') {
        ch = skipWhitespace(read());
        ch = readValue(ch);
        ch = skipWhitespace(ch);

        value = tag.toString();
      }

      int p;
      if (isDirective && name.equals("import")) {
        Element copy = (Element) elt.cloneNode(false);
        copy.setAttribute(name, value);
        parent.appendChild(copy);
      }
      else if (name.startsWith("xmlns")) {
        QElement qElt = (QElement) elt;
        if (newNamespaces == null) {
          newNamespaces = new HashMap<String,String>(_namespaces);
          _namespaces = newNamespaces;
        }

        String prefix;
        if (name.startsWith("xmlns:"))
          prefix = name.substring(6);
        else
          prefix = "";

        _namespaces.put(prefix, value);
        qElt.setAttributeNS(XMLNS, name, value);

        // backpatch if modify own name
        if (prefix != "" && qElt.getNodeName().startsWith(prefix)) {
          QDocument doc = (QDocument) xsl;
          QName newName = doc.createName(value, qElt.getNodeName());
          qElt.setName(newName);
        }
      }
      else if ((p = name.indexOf(':')) >= 0) {
        String prefix = name.substring(0, p);
        String uri = _namespaces.get(prefix);

        if (uri != null) {
          QElement qElt = (QElement) elt;
          qElt.setAttributeNS(uri, name, value);
        }
        else
          elt.setAttribute(name, value);
      }
      else {
        elt.setAttribute(name, value);
      }
    }

    return ch;
  }

  /**
   * Parses a processing instruction
   */
  private ProcessingInstruction parsePi()
    throws IOException, XslParseException
  {
    int ch = read();

    if (! XmlChar.isNameStart(ch))
      throw error(L.l("expected name at {0}", badChar(ch)));

    ch = readTag(ch);
    String name = tag.toString();

    text.clear();
    while (ch >= 0) {
      if (ch == '?') {
        if ((ch = read()) == '>') {
          ProcessingInstruction pi;
          pi =  xsl.createProcessingInstruction(name, text.toString());
          text.clear();
          return pi;
        }
        else
          addText('?');
      } else {
        addText((char) ch);
        ch = read();
      }
    }

    throw error(L.l("expected `{0}' at {1}", ">", badChar(-1)));
  }

  private int parseDecl(Node parent)
    throws IOException, XslParseException
  {
    int ch = read();

    if (ch == '[') {
      if ((ch = read()) != 'C') {
        addText("<![");
        return ch;
      } else if ((ch = read()) != 'D') {
        addText("<![C");
        return ch;
      } else if ((ch = read()) != 'A') {
        addText("<![CD");
        return ch;
      } else if ((ch = read()) != 'T') {
        addText("<![CDA");
        return ch;
      } else if ((ch = read()) != 'A') {
        addText("<![CDAT");
        return ch;
      } else if ((ch = read()) != '[') {
        addText("<![CDATA");
        return ch;
      } else {
        ch = read();
        
        while (ch > 0) {
          if (ch == ']') {
            ch = read();

            while (ch == ']') {
              if ((ch = read()) == '>')
                return read();
              else
                addText(']');
            }

            addText(']');
          }
          else {
            addText((char) ch);
            ch = read();
          }
        }

        return ch;
      }
    }

    if (ch != '-') {
      addText("<!");
      return ch;
    }

    if ((ch = read()) != '-') {
      addText("<!-");
      return ch;
    }

    while (ch >= 0) {
      if ((ch = read()) == '-') {
        ch = read();
        while (ch == '-') {
          if ((ch = read()) == '>')
            return read();
        }
      }
    }

    throw error(L.l("expected `{0}' at {1}", "-->", badChar(-1)));
  }

  /**
   * Parses the shortcut for valueOf <{...}>
   */
  private void parseValueOf(Node parent)
    throws IOException, XslParseException
  {
    int ch = read();
    while (ch >= 0) {
      if (ch == '}') {
        ch = read();
        if (ch == '>') {
          QElement elt;
          elt = (QElement) xsl.createElementNS(XSLNS, "xsl:value-of");
          elt.setAttribute("select", text.toString());
          elt.setLocation(is.getURL(), is.getUserPath(), line, 0);
          parent.appendChild(elt);
          text.clear();
          return;
        }
        else
          addText('}');
      }
      else {
        addText((char) ch);
        ch = read();
      }
    }
  }

  /**
   * parses top-level templates:
   *
   * pattern << ... >>     -- <xsl:template match='pattern'>...</xsl:template>
   * pattern <# ... #>     -- <xsl:template match='pattern'>
   *                            <xtp:scriptlet>...</xtp:scriptlet>
   *                          </xsl:template>
   * pattern <#= ... #>    -- <xsl:template match='pattern'>
   *                            <xtp:expression>...</xtp:expression>
   *                          </xsl:template>
   */
  private void parseSpecial(Node parent, int ch)
    throws IOException, XslParseException
  {
    char tail = '#';
    String element = "xtp:scriptlet";

    text.clear();
    String filename = is.getUserPath();
    int line = this.line;
    while (ch >= 0) {
      if (ch == '<') {
        filename = is.getUserPath();
        line = this.line;
        ch = read();
        if (ch == '#') {
          tail = '#';
          ch = read();
          if (ch == '=') {
            ch = read();
            element = "xtp:expression";
          }
          break;
        }
        else if (ch == '<') {
          tail = '>';
          break;
        }
        else if (ch == '\\') {
          addText((char) read());
          ch = read();
        }
      } else {
        addText((char) ch);
        ch = read();
      }
    }

    while (text.length() > 0 &&
           Character.isSpace(text.charAt(text.length() - 1))) {
      text.setLength(text.length() - 1);
    }
    
    QElement template = (QElement) xsl.createElementNS(XSLNS, "xsl:template");
    parent.appendChild(template);
    String match = text.toString();
    template.setAttribute("match", match);
    boolean isName = true;

    for (int i = 0; i < match.length(); i++) {
      if (! XmlChar.isNameChar(match.charAt(i))) {
        isName = false;
        break;
      }
    }

    if (isName && false) // XXX: problems
      template.setAttribute("name", match);
    if (defaultMode != null)
      template.setAttribute("mode", defaultMode);
    template.setLocation(filename, filename, line, 0);

    text.clear();
    inTemplate = true;

    if (tail == '>') {
      if (rawText)
        template.setAttribute("xml:space", "preserve");
      parseNode(template, ">>", false, read());
      inTemplate = false;
      return;
    }

    QNode scriptlet = (QNode) xsl.createElementNS(XTPNS, element);
    scriptlet.setLocation(filename, filename, line, 0);

    while (ch >= 0) {
      if (ch == tail) {
        ch = read();
        if (ch == '>')
          break;
        else
          addText(tail);
      } else {
        addText((char) ch);
        ch = read();
      }
    }

    scriptlet.appendChild(xsl.createTextNode(text.toString()));
    template.appendChild(scriptlet);
    text.clear();
    inTemplate = false;
  }
  
  private void parseBlock(Node parent, int ch)
    throws IOException, XslParseException
  {
    char tail = '#';
    String element = "xtp:scriptlet";

    for (; XmlChar.isWhitespace((char) ch); ch = read()) {
    }

    if (ch == ';')
      return;

    if (ch != '<')
      throw error(L.l("expected `{0}' at {1}", "<", badChar(ch)));
    
    String filename = is.getUserPath();
    int line = this.line;
    ch = read();
    if (ch == '#') {
      tail = '#';
      ch = read();
      if (ch == '=') {
        ch = read();
        element = "xtp:expression";
      }
    }
    else if (ch == '<') {
      tail = '>';
    }
    else
      throw error(L.l("expected block at {1}", "block", badChar(ch)));
    
    if (tail == '>') {
      if (rawText)
        ((Element) parent).setAttribute("xml:space", "preserve");
      parseNode(parent, ">>", false, read());
      return;
    }

    QNode scriptlet = (QNode) xsl.createElementNS(XTPNS, element);
    scriptlet.setLocation(filename, filename, line, 0);

    while (ch >= 0) {
      if (ch == tail) {
        ch = read();
        if (ch == '>')
          break;
        else
          addText(tail);
      } else {
        addText((char) ch);
        ch = read();
      }
    }

    scriptlet.appendChild(xsl.createTextNode(text.toString()));
    parent.appendChild(scriptlet);
    text.clear();
  }

  private void addText(char ch)
  {
    if (text.length() == 0) {
      if (ch == '\n')
        textLine = line - 1;
      else
        textLine = line;
    }
    text.append(ch);
  }

  private void addText(String s)
  {
    if (text.length() == 0)
      textLine = line;
    text.append(s);
  }

  private int skipWhitespace(int ch) throws IOException
  {
    for (; XmlChar.isWhitespace(ch); ch = read()) {
    }

    return ch;
  }

  private int readTag(int ch) throws IOException
  {
    tag.clear();
    for (; XmlChar.isNameChar(ch); ch = read())
      tag.append((char) ch);

    return ch;
  }

  /**
   * Scans an attribute value, storing the results in <code>tag</code>.
   *
   * @param ch the current read character.
   * @return the next read character after the value.
   */
  private int readValue(int ch) throws IOException, XslParseException
  {
    tag.clear();

    if (ch == '\'') {
      for (ch = read(); ch >= 0 && ch != '\''; ch = read()) {
        if (ch == '&') {
          ch = parseEntityReference();
          tag.append(text);
          text.clear();
          unread(ch);
        }
        else
          tag.append((char) ch);
      }

      if (ch != '\'')
        throw error(L.l("expected `{0}' at {1}", "'", badChar(ch)));
      return read();
    } else if (ch == '"') {
      for (ch = read(); ch >= 0 && ch != '"'; ch = read()) {
        if (ch == '&') {
          ch = parseEntityReference();
          tag.append(text);
          text.clear();
          unread(ch);
        }
        else
          tag.append((char) ch);
      }

      if (ch != '\"')
        throw error(L.l("expected `{0}' at {1}", "\"", badChar(ch)));

      return read();
    } else if (XmlChar.isNameChar(ch)) {
      for (; XmlChar.isNameChar(ch); ch = read())
        tag.append((char) ch);

      return ch;
    } else
      throw error(L.l("expected attribute value at {0}", badChar(ch)));
  }

  /**
   * Add the current accumulated text to the parent as a text node.
   *
   * @param parent node to contain the text.
   */
  private void addText(Node parent)
  {
    if (text.getLength() == 0) {
    }
    else {
      Text textNode = (Text) xsl.createTextNode(text.toString());
      QAbstractNode node = (QAbstractNode) textNode;

      node.setLocation(is.getURL(), is.getUserPath(), textLine, 0);
      parent.appendChild(textNode);
    }
    text.clear();
  }

  /**
   * Returns an error including the current filename and line in emacs style.
   *
   * @param message the error message.
   */
  private XslParseException error(String message)
  {
    return new XslParseException(getFilename() + ":" + getLine() + ": " +
                                 message);
  }

  /**
   * Returns an error including the current filename and line in emacs style.
   *
   * @param message the error message.
   */
  private XslParseException error(Exception e)
  {
    if (e.getMessage() != null)
      return new XslParseException(getFilename() + ":" + getLine() + ": " +
                                   e.getMessage());
    else
      return new XslParseException(getFilename() + ":" + getLine() + ": " +
                                   e);
  }

  /**
   * Return the source filename.
   */
  private String getFilename()
  {
    return is.getPath().getUserPath();
  }

  /**
   * Return the source line.
   */
  private int getLine()
  {
    return line;
  }

  /**
   * Returns a string for the error character.
   */
  private String badChar(int ch)
  {
    if (ch < 0)
      return L.l("end of file");
    else if (ch == '\n' || ch == '\r')
      return L.l("end of line");
    else
      return "`" + (char) ch + "'";
  }

  /**
   * Reads a character from the stream, keeping track of newlines.
   */
  public int read() throws IOException
  {
    if (peek >= 0) {
      int ch = peek;
      peek = -1;
      return ch;
    }

    int ch = is.readChar();
    if (ch == '\r') {
      if ((ch = is.readChar()) != '\n') {
        if (ch >= 0) {
          if (ch == '\r')
            peek = '\n';
          else
            peek = ch;
        }
      }
      ch = '\n';
    }
      
    if (ch == '\n')
      line++;

    return ch;
  }

  void unread(int ch)
  {
    peek = ch;
  }

  static {
    _xslCommands = new HashMap<String,String>();
    _xslCommands.put("apply-templates", "select");
    _xslCommands.put("call-template", "name");
    _xslCommands.put("apply-imports", "");
    _xslCommands.put("for-each", "select");
    _xslCommands.put("value-of", "select");
    _xslCommands.put("copy-of", "select");
    _xslCommands.put("number", "value");
    _xslCommands.put("choose", "");
    _xslCommands.put("when", "test");
    _xslCommands.put("otherwise", "");
    _xslCommands.put("if", "test");
    _xslCommands.put("text", "");
    _xslCommands.put("copy", "");
    _xslCommands.put("variable", "name");
    _xslCommands.put("param", "name");
    _xslCommands.put("with-param", "name");
    _xslCommands.put("message", "");
    _xslCommands.put("fallback", "");
    _xslCommands.put("processing-instruction", "name");
    _xslCommands.put("comment", "");
    _xslCommands.put("element", "name");
    _xslCommands.put("attribute", "name");
    _xslCommands.put("import", "href");
    _xslCommands.put("include", "href");
    _xslCommands.put("strip-space", "elements");
    _xslCommands.put("preserve-space", "elements");
    _xslCommands.put("output", "");
    _xslCommands.put("key", "");
    _xslCommands.put("decimal-format", "");
    _xslCommands.put("attribute-set", "name");
    _xslCommands.put("variable", "name");
    _xslCommands.put("param", "name");
    _xslCommands.put("template", "match");
    _xslCommands.put("namespace-alias", ""); // two args
    // xslt 2.0
    _xslCommands.put("result-document", "href");
    
    _xtpCommands = new HashMap<String,String>();
    _xtpCommands.put("while", "test");
    _xtpCommands.put("expression", "expr");
    _xtpCommands.put("expr", "expr");
    _xtpCommands.put("scriptlet", "");
    _xtpCommands.put("declaration", "");
    _xtpCommands.put("directive.page", "");
    _xtpCommands.put("directive.cache", "");
  }
}
