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

package com.caucho.xml;

import com.caucho.util.CharBuffer;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReaderWriterStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.readers.MacroReader;
import com.caucho.xml.readers.Utf16Reader;
import com.caucho.xml.readers.Utf8Reader;
import com.caucho.xml.readers.XmlReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * A configurable XML parser.  Loose versions of XML and HTML are supported
 * by changing the Policy object.
 *
 * <p>Normally, applications will use Xml, LooseXml, Html, or LooseHtml.
 */
public class XmlParser extends AbstractParser {
  // Xerces uses the following
  public static final String XMLNS = "http://www.w3.org/2000/xmlns/";
  public static final String XML = "http://www.w3.org/XML/1998/namespace";

  static final QName DOC_NAME = new QName(null, "#document", null);
  static final QName TEXT_NAME = new QName(null, "#text", null);
  static final QName JSP_NAME = new QName(null, "#jsp", null);
  static final QName WHITESPACE_NAME = new QName(null, "#whitespace", null);
  static final QName JSP_ATTRIBUTE_NAME = new QName("xtp", "jsp-attribute", null);
  
  QAttributes _attributes;
  QAttributes _nullAttributes;

  boolean _inDtd;
  
  CharBuffer _text;
  CharBuffer _eltName;
  CharBuffer _cb;
  CharBuffer _buf = new CharBuffer();
  String _textFilename;
  int _textLine;

  char []_textBuffer = new char[1024];
  int _textLength;
  int _textCapacity = _textBuffer.length;
  boolean _isIgnorableWhitespace;
  boolean _isJspText;
  
  CharBuffer _name = new CharBuffer();
  CharBuffer _nameBuffer = new CharBuffer();
  
  MacroReader _macro = new MacroReader();
  int _macroIndex = 0;
  int _macroLength = 0;
  char []_macroBuffer;

  QName []_elementNames = new QName[64];
  NamespaceMap []_namespaces = new NamespaceMap[64];
  int []_elementLines = new int[64];
  int _elementTop;

  NamespaceMap _namespaceMap;

  ArrayList<String> _attrNames = new ArrayList<String>();
  ArrayList<String> _attrValues = new ArrayList<String>();

  ReadStream _is;
  XmlReader _reader;
  
  String _extPublicId;
  String _extSystemId;
  
  QName _activeNode;
  QName _topNamespaceNode;
  boolean _isTagStart;
  boolean _stopOnIncludeEnd;
  boolean _hasTopElement;
  boolean _hasDoctype;
  boolean _isHtml;
  Locator _locator = new LocatorImpl(this);

  public XmlParser()
  {
    clear();
  }

  /**
   * Creates a new parser with a given parsing policy and dtd.
   *
   * @param policy the parsing policy, handling optional tags.
   * @param dtd the parser's dtd.
   */
  XmlParser(Policy policy, QDocumentType dtd)
  {
    super(policy, dtd);
    
    clear();
  }

  /**
   * Initialize the parser.
   */
  void init()
  {
    super.init();
    
    _attributes = new QAttributes();
    _nullAttributes = new QAttributes();
    _eltName = new CharBuffer();
    _text = new CharBuffer();

    _isHtml = _policy instanceof HtmlPolicy;

    // jsp/193b
    // _namespaceMap = null;
    
    _textLength = 0;
    _isIgnorableWhitespace = true;
    _elementTop = 0;
    _elementLines[0] = 1;

    _line = 1;

    _dtd = null;
    _inDtd = false;
    _isTagStart = false;
    _stopOnIncludeEnd = false;

    _extPublicId = null;
    _extSystemId = null;

    // _filename = null;
    _publicId = null;
    _systemId = null;
    
    _hasTopElement = false;
    _hasDoctype = false;

    _macroIndex = 0;
    _macroLength = 0;

    _reader = null;

    // _owner = null;
    
    _policy.init();
  }

  /**
   * Parse the document from a read stream.
   *
   * @param is read stream to parse from.
   *
   * @return The parsed document.
   */
  Document parseInt(ReadStream is)
    throws IOException, SAXException
  {
    _is = is;

    if (_filename == null && _systemId != null)
      _filename = _systemId;
    else if (_filename == null)
      _filename = _is.getUserPath();

    if (_systemId == null) {
      _systemId = _is.getPath().getURL();
      if ("null:".equals(_systemId) || "string:".equals(_systemId))
        _systemId = "stream";
    }

    /* xsl/0401
    if (_isNamespaceAware)
      _namespaceMap = new NamespaceMap(null, "", "");
    */
    _policy.setNamespaceAware(_isNamespaceAware);
    
    if (_filename == null)
      _filename = _systemId;

    if (_filename == null)
      _filename = "stream";

    if (_dtd != null)
      _dtd.setSystemId(_systemId);
    
    if (_builder != null) {
      if (! "string:".equals(_systemId) && ! "stream".equals(_systemId))
        _builder.setSystemId(_systemId);
      _builder.setFilename(_is.getPath().getURL());
    }

    if (_contentHandler == null)
      _contentHandler = new org.xml.sax.helpers.DefaultHandler();

    _contentHandler.setDocumentLocator(_locator);

    if (_owner == null)
      _owner = new QDocument();
    if (_defaultEncoding != null)
      _owner.setAttribute("encoding", _defaultEncoding);
    _owner.addDepend(is.getPath());
    
    _activeNode = DOC_NAME;
    
    _policy.setStream(is);
    _policy.setNamespace(_namespaceMap);

    _contentHandler.startDocument();
    
    int ch = parseXMLDeclaration(null);
    
    ch = skipWhitespace(ch);
    parseNode(ch, false);

    /*
    if (dbg.canWrite()) {
      printDebugNode(dbg, doc, 0);
      dbg.flush();
    }
    */

    if (_strictXml && ! _hasTopElement)
      throw error(L.l("XML file has no top-element.  All well-formed XML files have a single top-level element."));

    if (_contentHandler != null)
      _contentHandler.endDocument();

    QDocument owner = _owner;
    _owner = null;
      
    return owner;
  }

  /**
   * The main dispatch loop.
   *
   * @param node the current node
   * @param ch the next character
   * @param special true for the short form, &lt;foo/bar/>
   */
  private void parseNode(int ch, boolean special)
    throws IOException, SAXException
  {
    //boolean isTop = node instanceof QDocument;

    _text.clear();

  loop:
    while (true) {
      if (_textLength == 0) {
        _textFilename = getFilename();
        _textLine = getLine();
      }

      switch (ch) {
      case -1:
        if (_textLength != 0)
          appendText();
        if (! _stopOnIncludeEnd && _reader.getNext() != null) {
          popInclude();
          if (_reader != null)
            parseNode(_reader.read(), special);
          return;
        }
        closeTag("");
        return;

      case ' ': case '\t': case '\n': case '\r':
        if (! _normalizeWhitespace)
          addText((char) ch);
        else if (_textLength == 0) {
          if (! _isTagStart)
            addText(' ');
        }
        else if (_textBuffer[_textLength - 1] != ' ') {
          addText(' ');
        }
        ch = _reader.read();
        break;

      case 0xffff:
        // marker for end of text for serialization
        return;

      default:
        addText((char) ch);
        ch = _reader.read();
        break;

      case '/':
        if (! special) {
          addText((char) ch);
          ch = _reader.read();
          continue;
        }
        ch = _reader.read();
        if (ch == '>' || ch == -1) {
          appendText();
          popNode();
          return;
        }
        addText('/');
        break;

      case '&':
        ch = parseEntityReference();
        break;

      case '<':
        boolean endTag = false;
        ch = _reader.read();

        if (ch == '/' && ! special) {
          if (_normalizeWhitespace &&
              _textLength > 0 && _textBuffer[_textLength - 1] == ' ') {
            _textLength--;
          }
          appendText();

          ch = _reader.parseName(_name, _reader.read());

          if (ch != '>') {
            // XXX: Hack for Java PetStore
            while (XmlChar.isWhitespace(ch))
              ch = _reader.read();

            if (ch != '>')
              throw error(L.l("`</{0}>' expected `>' at {1}.  Closing tags must close immediately after the tag name.", _name, badChar(ch)));
          }

          closeTag(_policy.getName(_name).getName());
          ch = _reader.read();
        }
        // element: <tag attr=value ... attr=value> ...
        else if (XmlChar.isNameStart(ch)) {
          appendText();

          parseElement(ch);
          ch = _reader.read();
        }
        // <! ...
        else if (ch == '!') {
          // <![CDATA[ ... ]]>
          if ((ch = _reader.read()) == '[') {
            parseCdata();
            ch = _reader.read();
          }
          // <!-- ... -->
          else if (ch == '-') {
            parseComment();

            ch = _reader.read();
          }
          else if (XmlChar.isNameStart(ch)) {
            appendText();
            ch = _reader.parseName(_name, ch);
            String declName = _name.toString();
            if (declName.equals("DOCTYPE")) {
              parseDoctype(ch);
              if (_contentHandler instanceof DOMBuilder)
                ((DOMBuilder) _contentHandler).dtd(_dtd);

              ch = _reader.read();
            } else if (_forgiving && declName.equalsIgnoreCase("doctype")) {
              parseDoctype(ch);
              if (_contentHandler instanceof DOMBuilder)
                ((DOMBuilder) _contentHandler).dtd(_dtd);
              
              ch = _reader.read();
            } else
              throw error(L.l("expected `<!DOCTYPE' declaration at {0}", declName));

            if (isDtdValidating()) {
              generateDtdValidator(_dtd);
            }
          } else if (_forgiving) {
            addText("<!");
          } else
            throw error(L.l("expected `<!DOCTYPE' declaration at {0}", badChar(ch)));
        }
        // PI: <?tag attr=value ... attr=value?>
        else if (ch == '?') {
          ch = parsePI();
        }
        else if (_strictXml) {
          throw error(L.l("expected tag name after `<' at {0}.  Open tag names must immediately follow the open brace like `<foo ...>'", badChar(ch)));
        }
        // implicit <![CDATA[ for <% ... %>
        else if (_isJsp && ch == '%') {
          ch = _reader.read();

          appendText();
          _isJspText = ch != '=';
          
          addText("<%");

          while (ch >= 0) {
            if (ch == '%') {
              ch = _reader.read();
              if (ch == '>') {
                addText("%>");
                ch = _reader.read();
                break;
              }
              else
                addText('%');
            }
            else {
              addText((char) ch);
              ch = _reader.read();
            }
          }

          appendText();
          _isJspText = false;
        }
        else {
          addText('<');
        }
      }
    }
  }

  /**
   * Parses the &lt;!DOCTYPE> declaration.
   */
  private void parseDoctype(int ch)
    throws IOException, SAXException
  {
    if (_activeNode != DOC_NAME)
      throw error(L.l("<!DOCTYPE immediately follow the <?xml ...?> declaration."));
    
    _inDtd = true;

    ch = skipWhitespace(ch);
    ch = _reader.parseName(_nameBuffer, ch);
    String name = _nameBuffer.toString();
    ch = skipWhitespace(ch);

    if (_dtd == null)
      _dtd = new QDocumentType(name);

    _dtd.setName(name);

    if (XmlChar.isNameStart(ch)) {
      ch = parseExternalID(ch);
      ch = skipWhitespace(ch);

      _dtd._publicId = _extPublicId;
      _dtd._systemId = _extSystemId;
    }

    if (_dtd._systemId != null && ! _dtd._systemId.equals("")) {
      InputStream is = null;

      unread(ch);
      
      XmlReader oldReader = _reader;
      boolean hasInclude = false;

      try {
        pushInclude(_extPublicId, _extSystemId);
        hasInclude = true;
      } catch (Exception e) {
        if (log.isLoggable(Level.FINEST))
          log.log(Level.FINER, e.toString(), e);
        else
          log.finer(e.toString());
      }

      if (hasInclude) {
        _stopOnIncludeEnd = true;
        try {
          ch = parseDoctypeDecl(_dtd);
        } catch (XmlParseException e) {
          if (_extSystemId != null &&
              _extSystemId.startsWith("http")) {
            log.log(Level.FINE, e.toString(), e);
          }
          else
            throw e;
        }
        _stopOnIncludeEnd = false;

        while (_reader != null && _reader != oldReader)
          popInclude();
      }

      if (_reader != null)
        ch = skipWhitespace(read());
    }
    
    if (ch == '[')
      ch = parseDoctypeDecl(_dtd);

    ch = skipWhitespace(ch);

    _inDtd = false;

    if (ch != '>')
      throw error(L.l("expected `>' in <!DOCTYPE at {0}",
                      badChar(ch)));
  }

  /**
   * Parses the DTD.
   *
   * <pre>
   * dtd-item ::= &lt!ELEMENT ...  |
   *              &lt!ATTLIST ...  |
   *              &lt!NOTATION ... |
   *              &lt!ENTITY ...   |
   *              &lt!-- comment   |
   *              &lt? pi          |
   *              %pe-ref;
   * </pre>
   *
   * @return the next character.
   */
  private int parseDoctypeDecl(QDocumentType doctype)
    throws IOException, SAXException
  {
    _hasDoctype = true;
    int ch = 0;

    for (ch = skipWhitespace(read()); 
         ch >= 0 && ch != ']';
         ch = skipWhitespace(read())) {
      if (ch == '<') {
        if ((ch = read()) == '!') {
          if (XmlChar.isNameStart(ch = read())) {
            ch = _reader.parseName(_text, ch);
            String name = _text.toString();

            if (name.equals("ELEMENT"))
              parseElementDecl(doctype);
            else if (name.equals("ATTLIST"))
              parseAttlistDecl(doctype);
            else if (name.equals("NOTATION"))
              parseNotationDecl(doctype);
            else if (name.equals("ENTITY"))
              parseEntityDecl(doctype);
            else
              throw error("unknown declaration `" + name + "'");
          }
          else if (ch == '-')
            parseComment();
          else if (ch == '[') {
            ch = _reader.parseName(_text, read());
            String name = _text.toString();

            if (name.equals("IGNORE")) {
              parseIgnore();
            }
            else if (name.equals("INCLUDE")) {
              parseIgnore();
            }
            else
              throw error("unknown declaration `" + name + "'");
          }
        }
        else if (ch == '?') {
          parsePI();
        }
        else
          throw error(L.l("expected markup at {0}", badChar(ch)));
      }
      else if (ch == '%') {
        ch = _reader.parseName(_buf, read());

        if (ch != ';')
          throw error(L.l("`%{0};' expects `;' at {1}.  Parameter entities have a `%name;' syntax.", _buf, badChar(ch)));

        addPEReference(_text, _buf.toString());
      }
      else {
        throw error(L.l("expected '<' at {0}", badChar(ch)));
      }

      _text.clear();
    }
    _text.clear();

    return read();
  }

  /**
   * Parses an element.
   *
   * @param ch the current character
   */
  private void parseElement(int ch)
    throws IOException, SAXException
  {
    ch = _reader.parseName(_eltName, ch);

    NamespaceMap oldNamespace = _namespaceMap;
    
    if (ch != '>' && ch != '/')
      ch = parseAttributes(ch, true);
    else
      _attributes.clear();

    QName qname = _policy.getName(_eltName);

    if (_isValidating && _dtd != null) {
      QElementDef elementDef = _dtd.getElement(qname.getName());
      
      if (elementDef != null)
        elementDef.fillDefaults(_attributes);
    }

    if (ch == '/') {
      // empty tag: <foo/>
      if ((ch = _reader.read()) == '>') {
        addElement(qname, true, _attributes, oldNamespace);
      }
      // short tag: </foo/some text here/>
      else {
        addElement(qname, false, _attributes, oldNamespace);
        parseNode(ch, true);
      }
    } else if (ch == '>') {
      addElement(qname, false, _attributes, oldNamespace);
    } else
      throw error(L.l("unexpected character {0} while parsing `{1}' attributes.  Expected an attribute name or `>' or `/>'.  XML element syntax is:\n  <name attr-1=\"value-1\" ... attr-n=\"value-n\">",
                      badChar(ch), qname.getName()));
  }

  /**
   * Parses the attributes in an element.
   *
   * @param ch the next character to reader.read.
   *
   * @return the next character to read.
   */
  private int parseAttributes(int ch, boolean isElement)
    throws IOException, SAXException
  {
    ch = skipWhitespace(ch);
    _attributes.clear();

    _attrNames.clear();
    _attrValues.clear();

    boolean hasWhitespace = true;

    while (ch != -1) {
      if (! XmlChar.isNameStart(ch)) {
        if (! _isJsp || ch != '<')
          break;

        ch = parseJspAttribute(isElement);
        continue;
      }

      if (! hasWhitespace)
        throw error(L.l("attributes must be separated by whitespace"));

      hasWhitespace = false;
      
      ch = _reader.parseName(_text, ch);

      if (! _text.startsWith("xmlns")) {
      }
      else {
        QName name;

        if (_isNamespaceAware && _contentHandler instanceof DOMBuilder)
          name = _policy.getNamespaceName(_text);
        else
          name = new QName(_text.toString(), null);

        String prefix;

        if (_text.length() > 5) {
          prefix = _text.substring(6);

          if (prefix.equals(""))
            throw error(L.l("'{0}' is an illegal namespace declaration.",
                            _text));
        }
        else {
          prefix = "";
        }

        _text.clear();
        ch = skipWhitespace(ch);
        if (ch != '=')
          throw error(L.l("xmlns: needs value at {0}", badChar(ch)));
        ch = skipWhitespace(_reader.read());
        ch = parseValue(_text, ch, true);

        hasWhitespace = isWhitespace(ch);

        ch = skipWhitespace(ch);

        // topNamespaceNode = element;
        String uri = _text.toString();

        if (_isXmlnsPrefix) {
          _namespaceMap = new NamespaceMap(_namespaceMap, prefix, uri);
          _policy.setNamespace(_namespaceMap);

          _contentHandler.startPrefixMapping(prefix, uri);
        }

        // needed for xml/032e vs xml/00ke
        if (isElement && _isXmlnsAttribute
            && _contentHandler instanceof DOMBuilder) {
          _attributes.add(name, uri);
        }

        continue;
      }

      String attrName = _text.toString();
      _attrNames.add(attrName);

      _text.clear();
      ch = skipWhitespace(ch);

      String value = null;

      if (ch == '=') {
        ch = skipWhitespace(_reader.read());
        ch = parseValue(_text, ch, true);

        hasWhitespace = isWhitespace(ch);

        ch = skipWhitespace(ch);

        value = _text.toString();
      }
      else if (_strictAttributes) {
        throw error(L.l("attribute `{0}' expects value at {1}.  XML requires attributes to have explicit values.",
                        attrName, badChar(ch)));
      }
      else {
        value = attrName; // xxx: conflict xsl/0432
        hasWhitespace = true;
      }

      _attrValues.add(value);
    }

    int len = _attrNames.size();
    for (int i = 0; i < len; i++) {
      String attrName = _attrNames.get(i);
      String value = _attrValues.get(i);

      _text.clear();
      _text.append(attrName);
      QName name;

      if (_contentHandler instanceof DOMBuilder)
        name = _policy.getAttributeName(_eltName, _text, true);
      else
        name = _policy.getAttributeName(_eltName, _text);

      _attributes.add(name, value);
    }
    
    return ch;
  }

  /**
   * Special parser to handle the use of &lt;%= as an attribute in JSP
   * files.  Covers cases like the following:
   *
   * <pre>
   * &lt;options>
   * &lt;option name="foo" &lt;%= test.isSelected("foo") %>/>
   * &lt;/options>
   * </pre>
   *
   * @param element the parent element
   *
   * @return the next character to read.
   */
  private int parseJspAttribute(boolean isElement)
    throws IOException, XmlParseException
  {
    int ch = _reader.read();

    if (ch != '%')
      throw error(L.l("unexpected char `{0}' in element", "%"));

    ch = _reader.read();
    if (ch != '=')
      throw error(L.l("unexpected char `{0}' in element", "="));

    _text.clear();
    ch = _reader.read();
    while (ch >= 0) {
      if (ch == '%') {
        ch = _reader.read();
        if (ch == '>') {
          ch = _reader.read();
          break;
        }
        _text.append((char) ch);
      }
      else {
        _text.append((char) ch);
        ch = _reader.read();
      }
    }

    String value = _text.toString();

    if (isElement)
      _attributes.add(JSP_ATTRIBUTE_NAME, value);

    return ch;
  }

  /**
   * Handle processing at a close tag.  For strict XML, this will normally
   * just change the current node to its parent, but HTML has a more
   * complicated policy.
   */
  private void closeTag(String endTagName)
    throws IOException, SAXException
  {
    while (_activeNode != null && _activeNode != DOC_NAME) {
      switch (_policy.elementCloseAction(this, _activeNode, endTagName)) {
      case Policy.POP:
        //if (dbg.canWrite())
        //  dbg.println("</" + activeNode.getNodeName() + ">");

        popNode();
        return;

      case Policy.POP_AND_LOOP:
        //if (dbg.canWrite())
        //  dbg.println("</" + activeNode.getNodeName() + ">");

        popNode();
        break;

      case Policy.IGNORE:
        return;

      default:
        throw new RuntimeException();
      }
    }

    if (! _extraForgiving && endTagName != null && ! endTagName.equals(""))
      throw error(L.l("Unexpected end tag `</{0}>' at top-level.  All open tags have already been closed.",
                    endTagName));
  }

  /**
   * Handles processing of the resin:include tag.
   */
  private void handleResinInclude()
    throws IOException, SAXException
  {
    String filename = _attributes.getValue("path");
    
    if (filename == null || filename.equals(""))
      filename = _attributes.getValue("href");

    if (filename.equals(""))
      throw error(L.l("<resin:include> expects a `path' attribute."));

    pushInclude(filename);
  }

  /**
   * Handles processing of the resin:include tag.
   */
  private void handleResinIncludeDirectory()
    throws IOException, SAXException
  {
    String filename = _attributes.getValue("path");

    if (filename == null || filename.equals(""))
      filename = _attributes.getValue("href");

    String extension = _attributes.getValue("extension");

    if (filename.equals(""))
      throw error(L.l("<resin:include> expects a `path' attribute."));

    Path pwd;
    if (_searchPath != null)
      pwd = _searchPath;
    else
      pwd = Vfs.lookup(_systemId).getParent();

    Path dir = pwd.lookup(filename);
    if (! dir.isDirectory())
      throw error(L.l("`{0}' is not a directory for resin:include-directory.  The href for resin:include-directory must refer to a directory.",
                      dir.getNativePath()));

    String []list = dir.list();
    Arrays.sort(list);
    for (int i = list.length - 1; i >= 0; i--) {
      if (list[i].startsWith(".") ||
          extension != null && ! list[i].endsWith(extension))
        continue;

      pushInclude(dir.lookup(list[i]).getPath());
    }
  }

  private int parseNameToken(CharBuffer name, int ch)
    throws IOException, SAXException
  {
    name.clear();

    if (! XmlChar.isNameChar(ch))
      throw error(L.l("expected name at {0}", badChar(ch)));

    for (; XmlChar.isNameChar(ch); ch = _reader.read())
      name.append((char) ch);

    return ch;
  }

  /**
   * Pop the top-level node
   */
  private void popNode()
    throws SAXException
  {
    QName node = _activeNode;

    if (_activeNode != DOC_NAME) {
      String uri = _activeNode.getNamespaceURI();
      String localName = _activeNode.getLocalName();
      
      if (uri == null) {
        uri = "";

        if (_isNamespaceAware)
          localName = _activeNode.getName();
        else
          localName = "";
      }

      _contentHandler.endElement(uri,
                                 localName,
                                 _activeNode.getName());
    }

    if (_elementTop > 0) {
      _elementTop--;
      NamespaceMap oldMap = _namespaces[_elementTop];

      popNamespaces(oldMap);
      
      _activeNode = _elementNames[_elementTop];
    }
    
    if (_elementTop == 0)
      _activeNode = DOC_NAME;
  }

  public void pushNamespace(String prefix, String uri)
  {
    _namespaceMap = new NamespaceMap(_namespaceMap, prefix, uri);

    _policy.setNamespace(_namespaceMap);
  }

  private void popNamespaces(NamespaceMap oldMap)
    throws SAXException
  {
    for (;
         _namespaceMap != null && _namespaceMap != oldMap;
         _namespaceMap = _namespaceMap.next) {
      _contentHandler.endPrefixMapping(_namespaceMap.prefix);
    }
    _namespaceMap = oldMap;
    _policy.setNamespace(_namespaceMap);
  }

  private void appendText(String s)
  {
    if (_text.length() == 0) {
      _textFilename = getFilename();
      _textLine = getLine();
    }

    _text.append(s);
  }

  /**
   * Parses an entity reference:
   *
   * <pre>
   * er ::= &#d+;
   *    ::= &name;
   * </pre>
   */
  private int parseEntityReference()
    throws IOException, SAXException
  {
    int ch;

    ch = _reader.read();

    // character reference
    if (ch == '#') {
      addText((char) parseCharacterReference());

      return _reader.read();
    } 
    // entity reference
    else if (XmlChar.isNameStart(ch)) {
      ch = _reader.parseName(_buf, ch);

      if (ch != ';' && _strictXml)
        throw error(L.l("`&{0};' expected `;' at {0}.  Entity references have a `&name;' syntax.", _buf, badChar(ch)));
      else if (ch != ';') {
        addText('&');
        addText(_buf.toString());
        return ch;
      }

      addEntityReference(_buf.toString());

      ch = _reader.read();

      return ch;
    } else if (_strictXml) {
      throw error(L.l("expected name at {0}", badChar(ch)));
    } else {
      addText('&');
      return ch;
    }
  }

  private int parseCharacterReference()
    throws IOException, SAXException
  {
    int ch = _reader.read();

    int radix = 10;
    if (ch == 'x') {
      radix = 16;
      ch = _reader.read();
    }

    int value = 0;
    for (; ch != ';'; ch = _reader.read()) {
      if (ch >= '0' && ch <= '9')
        value = radix * value + ch - '0';
      else if (radix == 16 && ch >= 'a' && ch <= 'f')
        value = radix * value + ch - 'a' + 10;
      else if (radix == 16 && ch >= 'A' && ch <= 'F')
        value = radix * value + ch - 'A' + 10;
      else
        throw error(L.l("malformed entity ref at {0}", badChar(ch)));
    }

    if (value > 0xffff)
      throw error(L.l("malformed entity ref at {0}", "" + value));

    // xml/0072
    if (_strictCharacters && ! isChar(value))
      throw error(L.l("illegal character ref at {0}", badChar(value)));

    return value;
  }

  /**
   * Looks up a named entity reference, filling the text.
   */
  private void addEntityReference(String name)
    throws IOException, SAXException
  {
    boolean expand = ! _entitiesAsText || _hasDoctype || ! _switchToXml;
    // XXX: not quite the right logic.  There should be a soft expandEntities

    if (! expand) {
      addText("&" + name + ";");
      return;
    }
    
    int ch = _entities.getEntity(name);
    if (ch >= 0 && ch <= 0xffff) {
      addText((char) ch);
      return;
    }

    QEntity entity = _dtd == null ? null : _dtd.getEntity(name);

    if (! _expandEntities) {
      addText("&" + name + ";");
      return;
    }

    if (entity == null && (_dtd == null || _dtd.getName() == null ||
                           ! _dtd.isExternal())) {
      if (_strictXml)
        throw error(L.l("`&{0};' is an unknown entity.  XML predefines only `&lt;', `&amp;', `&gt;', `&apos;' and  `&quot;'. All other entities must be defined in an &lt;!ENTITY> definition in the DTD.", name));
      else {
        if (expand && _contentHandler instanceof DOMBuilder) {
          appendText();
          ((DOMBuilder) _contentHandler).entityReference(name);
        }
        else
          addText("&" + name + ";");
      }
    }
    else if (entity != null) {
      if (expand && entity._isSpecial && entity._value != null)
        addText(entity._value);
      else if (entity.getSystemId() != null) {
        if (pushSystemEntity(entity)) {
        }
        /* XXX:??
        else if (strictXml) {
          throw error(L.l("can't open external entity at `&{0};'", name));
        }
        */
        else if (_contentHandler instanceof DOMBuilder) {
          appendText();
          ((DOMBuilder) _contentHandler).entityReference(name);
        }
        else
          addText("&" + name + ";");
      }
      else if (expand && entity._value != null)
        setMacro(entity._value);
      else
        addText("&" + name + ";");
    }
    else {
      if (expand && _contentHandler instanceof DOMBuilder) {
        appendText();
        ((DOMBuilder) _contentHandler).entityReference(name);
      }
      else // XXX: error?
        addText("&" + name + ";");
    }
  }

  private boolean pushSystemEntity(QEntity entity)
    throws IOException, SAXException
  {
    String publicId = entity.getPublicId();
    String systemId = entity.getSystemId();
    String value = null;
    InputSource source = null;
    ReadStream is = null;

    if (_entityResolver != null)
      source = _entityResolver.resolveEntity(publicId, systemId);

    if (source != null && source.getByteStream() != null)
      is = Vfs.openRead(source.getByteStream());
    else if (source != null && source.getCharacterStream() != null)
      is = Vfs.openRead(source.getCharacterStream());
    else if (source != null && source.getSystemId() != null &&
             _searchPath.lookup(source.getSystemId()).isFile()) {
      _owner.addDepend(_searchPath.lookup(source.getSystemId()));
      is = _searchPath.lookup(source.getSystemId()).openRead();
    }
    else if (systemId != null && ! systemId.equals("")) {
      String path = systemId;
      if (path.startsWith("file:"))
        path = path.substring(5);
      if (_searchPath != null && _searchPath.lookup(path).isFile()) {
        _owner.addDepend(_searchPath.lookup(path));
        is = _searchPath.lookup(path).openRead();
      }
    }

    if (is == null)
      return false;

    _filename = systemId;
    _systemId = systemId;

    Path oldSearchPath = _searchPath;
    Path path = is.getPath();
    if (path != null) {
      _owner.addDepend(path);
      
      if (_searchPath != null) {
        _searchPath = path.getParent();
        _reader.setSearchPath(oldSearchPath);
      }
    }

    _is = is;
    _line = 1;
    
    XmlReader oldReader = _reader;
    _reader = null;

    int ch = parseXMLDeclaration(oldReader);
    unread(ch);

    return true;
  }

  /**
   * Parses an attribute value.
   *
   * <pre>
   * value ::= '[^']*'
   *       ::= "[^"]*"
   *       ::= [^ />]*
   * </pre>
   *
   * @param value the CharBuffer which will contain the value.
   * @param ch the next character from the input stream.
   * @param isGeneral true if general entities are allowed.
   *
   * @return the following character from the input stream
   */
  private int parseValue(CharBuffer value, int ch, boolean isGeneral)
    throws IOException, SAXException
  {
    int end = ch;

    value.clear();

    if (end == '\'' || end == '"')
      ch = _reader.read();
    else if (_strictAttributes) {
      value.append((char) end);
      for (ch = _reader.read();
           ch >= 0 && XmlChar.isNameChar(ch);
           ch = _reader.read())
        value.append((char) ch);
      
      throw error(L.l("XML attribute value must be quoted at `{0}'.  XML attribute syntax is either attr=\"value\" or attr='value'.",
                      value));
    }
    else
      end = 0;

    while (ch != -1 && (end != 0 && ch != end ||
                        end == 0 && isAttributeChar(ch))) {
      if (end == 0 && ch == '/') {
        ch = _reader.read();
        if (! isWhitespace(ch) && ch != '>') {
          value.append('/');
          value.append((char) ch);
        }
        else {
          unread(ch);
          return '/';
        }
      }
      else if (ch == '&' && ! _entitiesAsText) {
        if ((ch = _reader.read()) == '#')
          value.append((char) parseCharacterReference());
        else if (! isGeneral) {
          value.append('&');
          value.append((char) ch);
        }
        else if (XmlChar.isNameStart(ch)) {
          ch = _reader.parseName(_buf, ch);
          String name = _buf.toString();

          if (ch != ';' && _strictXml)
            throw error(L.l("expected `{0}' at {1}", ";", badChar(ch)));
          else if (ch != ';') {
            value.append('&');
            value.append(name);
            continue;
          } else {
            int lookup = _entities.getEntity(name);

            if (lookup >= 0 && lookup <= 0xffff) {
              ch = _reader.read();
              value.append((char) lookup);
              continue;
            }
            
            QEntity entity = _dtd == null ? null : _dtd.getEntity(name);
            if (entity != null && entity._value != null)
              setMacroAttr(entity._value);
            else if (_strictXml)
              throw error(L.l("expected local reference at `&{0};'", name));
            else {
              value.append('&');
              value.append(name);
              value.append(';');
            }
          }
        }
      }
      else if (ch == '%' && ! isGeneral) {
        ch = _reader.read();

        if (! XmlChar.isNameStart(ch)) {
          value.append('%');
          continue;
        }
        else {
          ch = _reader.parseName(_buf, ch);

          if (ch != ';')
            throw error(L.l("expected `{0}' at {1}", ";", badChar(ch)));
          else
            addPEReference(value, _buf.toString());
        }
      } 
      else if (ch == '<' && _isJsp) {
        value.append('<');
        
        ch = _reader.read();

        if (ch != '%')
          continue;

        value.append('%');

        ch = _reader.read();
        while (ch >= 0) {
          if (ch == '%') {
            ch = _reader.read();
            if (ch == '>') {
              value.append("%>");
              break;
            }
            else
              value.append('%');
          }
          else {
            value.append((char) ch);
            ch = _reader.read();
          }
        }
      }
      else if (isGeneral) {
        if (ch == '\r') {
          ch = _reader.read();
          if (ch != '\n') {
            value.append('\n');
            continue;
          }
        }
        value.append((char) ch);
      }
      else if (ch == '\r') {
        value.append(' ');
        
        if ((ch = _reader.read()) != '\n')
          continue;
      }
      else if (ch == '\n')
        value.append(' ');
      else
        value.append((char) ch);

      ch = _reader.read();
    }

    if (end != 0)
      ch = _reader.read();

    return ch;
  }

  private boolean isAttributeChar(int ch)
  {
    switch (ch) {
    case ' ': case '\t': case '\n': case '\r':
      return false;
    case '<': case '>': case '\'':case '"': case '=':
      return false;
    default:
      return true;
    }
  }

  private void parsePcdata(QNode node) throws IOException, SAXException
  {
    int ch;
    String tail = "</" + node.getNodeName() + ">";

    _text.clear();
    ch = _reader.read();
    if (ch == '\n')
      ch = _reader.read();

    for (; ch != -1; ch = _reader.read()) {
      addText((char) ch);

      if (_text.endsWith(tail)) {
        _text.setLength(_text.length() - tail.length());
        if (_text.length() > 1 && _text.charAt(_text.length() - 1) == '\n')
          _text.setLength(_text.length() - 1);
        appendText();
        return;
      }
    }
    
    throw error("bad pcdata");
  }

  private int parseXMLDeclaration(XmlReader oldReader)
    throws IOException, SAXException
  {
    int startOffset = _is.getOffset();
    boolean isEBCDIC = false;
    int ch = _is.read();

    XmlReader reader = null;
    
    // utf-16 starts with \xfe \xff
    if (ch == 0xfe) {
      ch = _is.read();
      if (ch == 0xff) {
        _owner.setAttribute("encoding", "UTF-16");
        _is.setEncoding("utf-16");

        reader = new Utf16Reader(this, _is);
        
        ch = reader.read();
      }
    }
    // utf-16 rev starts with \xff \xfe
    else if (ch == 0xff) {
      ch = _is.read();
      if (ch == 0xfe) {
        _owner.setAttribute("encoding", "UTF-16");
        _is.setEncoding("utf-16");

        reader = new Utf16Reader(this, _is);
        ((Utf16Reader) reader).setReverse(true);
        
        ch = reader.read();
      }
    }
    // utf-16 can also start with \x00 <
    else if (ch == 0x00) {
      ch = _is.read();
      _owner.setAttribute("encoding", "UTF-16");
      _is.setEncoding("utf-16");
      
      reader = new Utf16Reader(this, _is);
    }
    // utf-8 BOM is \xef \xbb \xbf
    else if (ch == 0xef) {
      ch = _is.read();
      if (ch == 0xbb) {
        ch = _is.read();

        if (ch == 0xbf) {
          ch = _is.read();

          _owner.setAttribute("encoding", "UTF-8");
          _is.setEncoding("utf-8");
      
          reader = new Utf8Reader(this, _is);
        }
      }
    }
    else if (ch == 0x4c) {
      // ebcdic
      // xml/00l1
      _is.unread();
      // _is.setEncoding("cp037");
      _is.setEncoding("cp500");

      isEBCDIC = true;

      reader = new XmlReader(this, _is);

      ch = reader.read();
    }
    else {
      int ch2 = _is.read();

      if (ch2 == 0x00) {
        _owner.setAttribute("encoding", "UTF-16LE");
        _is.setEncoding("utf-16le");

        reader = new Utf16Reader(this, _is);
        ((Utf16Reader) reader).setReverse(true);
      }
      else if (ch2 > 0)
        _is.unread();
    }

    if (reader != null && reader != oldReader) {
    }
    else if (_policy instanceof HtmlPolicy ||
             _is.getSource() instanceof ReaderWriterStream) {
      reader = new XmlReader(this, _is);
    }
    else {
      reader = new Utf8Reader(this, _is);
    }

    if (ch == '\n')
      reader.setLine(2);

    reader.setSystemId(_systemId);
    if (_systemId == null)
      reader.setSystemId(_filename);
    reader.setFilename(_filename);
    reader.setPublicId(_publicId);

    reader.setNext(oldReader);

    _reader = reader;

    /* XXX: this might be too strict. */
    /*
    if (! strictXml) {
      for (; XmlChar.isWhitespace(ch); ch = reader.read()) {
      }
    }
    */

    if (ch != '<')
      return ch;

    if (parseXMLDecl(_reader) && isEBCDIC) {
      // EBCDIC requires a re-read
      _is.setOffset(startOffset);

      ch = _reader.read();
      if (ch != '<')
        throw new IllegalStateException();
      
      parseXMLDecl(_reader);
    }

    return _reader.read();    
  }

  private boolean parseXMLDecl(XmlReader reader)
    throws IOException, SAXException
  {
    int ch = reader.read();
    if (ch != '?') {
      unread((char) ch);
      unread('<');
      return false;
    }

    ch = _reader.read();
    if (! XmlChar.isNameStart(ch))
      throw error(L.l("expected name after '<?' at {0}.  Processing instructions expect a name like <?foo ... ?>", badChar(ch)));
    ch = _reader.parseName(_text, ch);

    String piName = _text.toString();
    if (! piName.equals("xml")) {
      ch = parsePITail(piName, ch);
      unread(ch);
      return false;
    }
          
    if (_switchToXml && _activeNode == DOC_NAME && ! _inDtd) {
      _policy = new XmlPolicy();
    }

    ch = parseAttributes(ch, false);
      
    if (ch != '?')
      throw error(L.l("expected `?' at {0}.  Processing instructions end with `?>' like <?foo ... ?>", badChar(ch)));
    if ((ch = _reader.read()) != '>')
      throw error(L.l("expected `>' at {0}.  Processing instructions end with `?>' like <?foo ... ?>", ">", badChar(ch)));

    for (int i = 0; i < _attributes.getLength(); i++) {
      QName name = _attributes.getName(i);
      String value = _attributes.getValue(i);

      if (_owner != null)
        _owner.setAttribute(name.getName(), value);

      if (name.getName().equals("encoding")) { // xml/00hb // && ! _inDtd) {
        String encoding = value;

        if (! _isStaticEncoding &&
            ! encoding.equalsIgnoreCase("UTF-8") &&
            ! encoding.equalsIgnoreCase("UTF-16") &&
            ! (_is.getSource() instanceof ReaderWriterStream)) {
          _is.setEncoding(encoding);

          XmlReader oldReader = _reader;

          _reader = new XmlReader(this, _is);
          // _reader.setNext(oldReader);

          _reader.setLine(oldReader.getLine());

          _reader.setSystemId(_filename);
          _reader.setPublicId(null);
        }
      }
    }

    return true;
  }

  private int parsePI()
    throws IOException, SAXException
  {
    int ch;

    appendText();
    ch = _reader.read();
    if (! XmlChar.isNameStart(ch))
      throw error(L.l("expected name after '<?' at {0}.  Processing instructions expect a name like <?foo ... ?>", badChar(ch)));
    ch = _reader.parseName(_text, ch);

    String piName = _text.toString();
    if (! piName.equals("xml"))
      return parsePITail(piName, ch);
    else if (_switchToXml && _activeNode == DOC_NAME && ! _inDtd) {
      _policy = new XmlPolicy();
      return parsePITail(piName, ch);
    }
    else {
      throw error(L.l("<?xml ... ?> occurs after content.  The <?xml ... ?> prolog must be at the document start."));

    }
  }

  private int parsePITail(String piName, int ch)
    throws IOException, SAXException
  {
    ch = skipWhitespace(ch);

    _text.clear();
    while (ch != -1) {
      if (ch == '?') {
        if ((ch = _reader.read()) == '>')
          break;
        else
          _text.append('?');
      } else {
        _text.append((char) ch);
        ch = _reader.read();
      }
    }

    if (_inDtd) {
      QProcessingInstruction pi;
      pi = new QProcessingInstruction(piName, _text.toString());
      pi._owner = _dtd._owner;
      _dtd.appendChild(pi);
    }
    else
      _contentHandler.processingInstruction(piName, _text.toString());

    return _reader.read();
  }

  /**
   * Parses a comment.  The "&lt;!--" has already been read.
   */
  private void parseComment()
    throws IOException, SAXException
  {
    if (! _skipComments)
      appendText();

    int ch = _reader.read();

    if (ch != '-')
      throw error(L.l("expected comment at {0}", badChar(ch)));

    ch = _reader.read();

    if (! _skipComments)
      _buf.clear();

  comment:
    while (ch != -1) {
      if (ch == '-') {
        ch = _reader.read();

        while (ch == '-') {
          if ((ch = _reader.read()) == '>')
            break comment;
          else if (_strictComments)
            throw error(L.l("XML forbids `--' in comments"));
          else if (ch == '-') {
            if (! _skipComments)
              _buf.append('-');
          }
          else {
            if (! _skipComments)
              _buf.append("--");
            break;
          }
        }

        _buf.append('-');
      } else if (! XmlChar.isChar(ch)) {
        throw error(L.l("bad character {0}", hex(ch)));
      } else {
        _buf.append((char) ch);
        ch = _reader.read();
      }
    }

    if (_inDtd) {
      QComment comment = new QComment(_buf.toString());
      comment._owner = _dtd._owner;
      _dtd.appendChild(comment);
    }
    else if (_skipComments) {
    }
    else if (_contentHandler instanceof XMLWriter && ! _skipComments) {
      ((XMLWriter) _contentHandler).comment(_buf.toString());
      _isIgnorableWhitespace = true;
    }
    else if (_lexicalHandler != null) {
      _lexicalHandler.comment(_buf.getBuffer(), 0, _buf.getLength());
      _isIgnorableWhitespace = true;
    }
  }

  /**
   * Parses the contents of a cdata section.
   *
   * <pre>
   * cdata ::= &lt;![CDATA[ ... ]]>
   * </pre>
   */
  private void parseCdata()
    throws IOException, SAXException
  {
    int ch;

    if (_forgiving) {
      if ((ch = _reader.read()) != 'C') {
        appendText("<![" + (char) ch);
        return;
      }
      else if ((ch = _reader.read()) != 'D') {
        appendText("<![C" + (char) ch);
        return;
      }
      else if ((ch = _reader.read()) != 'A') {
        appendText("<![CD" + (char) ch);
        return;
      }
      else if ((ch = _reader.read()) != 'T') {
        appendText("<![CDA" + (char) ch);
        return;
      }
      else if ((ch = _reader.read()) != 'A') {
        appendText("<![CDAT" + (char) ch);
        return;
      }
      else if ((ch = _reader.read()) != '[') {
        appendText("<![CDATA" + (char) ch);
        return;
      }
    }
    else if ((ch = _reader.read()) != 'C' ||
             (ch = _reader.read()) != 'D' ||
             (ch = _reader.read()) != 'A' ||
             (ch = _reader.read()) != 'T' ||
             (ch = _reader.read()) != 'A' ||
             (ch = _reader.read()) != '[') {
      throw error(L.l("expected `<![CDATA[' at {0}", badChar(ch)));
    }

    ch = _reader.read();

    if (_lexicalHandler != null) {
      _lexicalHandler.startCDATA();
      appendText();
    }
    else if (! _isCoalescing)
      appendText();

  cdata:
    while (ch != -1) {
      if (ch == ']') {
        ch = _reader.read();

        while (ch == ']') {
          if ((ch = _reader.read()) == '>')
            break cdata;
          else if (ch == ']')
            addText(']');
          else {
            addText(']');
            break;
          }
        }

        addText(']');
      } else if (_strictCharacters && ! isChar(ch)) {
        throw error(L.l("expected character in cdata at {0}", badChar(ch)));
      } else {
        addText((char) ch);
        ch = _reader.read();
      }
    }
    
    if (_lexicalHandler != null) {
      appendText();
      _lexicalHandler.endCDATA();
    }
    else if (! _isCoalescing)
      appendText();
  }

  /**
   * Ignores content to the ']]>'
   */
  private void parseIgnore()
    throws IOException, SAXException
  {
    int ch = read();

    while (ch >= 0) {
      if (ch != ']') {
        ch = read();
      }
      else if ((ch = read()) != ']') {
      }
      else if ((ch = read()) == '>')
        return;
    }
  }

  private int parseContentSpec(QElementDef def, int ch)
    throws IOException, SAXException
  {
    ch = expandPE(ch);
    
    if (XmlChar.isNameStart(ch)) {
      ch = _reader.parseName(_text, ch);
      String name = _text.toString();

      if (name.equals("EMPTY")) {
        def._content = "EMPTY";
        return ch;
      }
      else if (name.equals("ANY")) {
        def._content = "ANY";
        return ch;
      }
      else
        throw error(L.l("expected EMPTY or ANY at `{0}'", name));
    }
    else if (ch != '(') {
      throw error(L.l("expected grammar definition starting with '(' at {0}.  <!ELEMENT> definitions have the syntax <!ELEMENT name - - (grammar)>", badChar(ch)));
    }
    else {
      QContentParticle cp = new QContentParticle();
      def._content = cp;

      return parseContentParticle(cp, true);
    }
  }

  /**
   * Parses a content-particle, i.e. a grammer particle in the DTD
   * regexp.
   */
  private int parseContentParticle(QContentParticle cp, boolean isTop)
    throws IOException, SAXException
  {
    boolean hasCdata = false;
    cp._separator = 0;
    cp._repeat = 0;
    int ch;

    ch = expandPE(_reader.read());
    
    for (; ch != -1; ch = expandPE(ch)) {
      if (ch == '(') {
        QContentParticle child = new QContentParticle();
        cp.addChild(child);

        ch = parseContentParticle(child, false);
      }
      else if (XmlChar.isNameStart(ch)) {
        ch = _reader.parseName(_text, ch);
        cp.addChild(_text.toString());
      }
      else if (ch == '#') {
        ch = _reader.parseName(_text, _reader.read());
        String name = _text.toString();

        if (_strictXml && cp._children.size() != 0)
          throw error(L.l("`#{0}' must occur first", name));
        if (_strictXml && ! isTop)
          throw error(L.l("`#{0}' may only occur at top level", name));

        if (name.equals("PCDATA"))
          cp.addChild("#PCDATA");
        else
          throw error(L.l("illegal content particle at `#{0}'", name));

        hasCdata = true;
      }
      else
        throw error(L.l("expected content particle at {0}", badChar(ch)));

      ch = expandPE(ch);

      if (ch == '?' || ch == '*' || ch == '+') {
        Object child = cp.getChild(cp.getChildSize() - 1);
        if (child instanceof QContentParticle) {
          QContentParticle cpChild = (QContentParticle) child;
          cpChild._repeat = ch;
        }
        else {
          QContentParticle cpChild = new QContentParticle();
          cpChild.addChild(child);
          cpChild._repeat = ch;
          cp.setChild(cp.getChildSize() - 1, cpChild);
        }

        ch = expandPE(_reader.read());
      }

      if (ch == ')')
        break;
      else if (cp._separator == 0) {
        if (ch == '|')
          cp._separator = ch;
        else if (hasCdata && _strictXml)
          throw error(L.l("#PCDATA must be separated by `|' at {0}",
                          badChar(ch)));
        else if (ch == ',')
          cp._separator = ch;
        else if (! _strictXml && ch =='&')
          cp._separator = ch;
        else
          throw error(L.l("expected separator at {0}", badChar(ch)));

        ch = _reader.read();
      } else if (ch != cp._separator)
        throw error(L.l("expected `{0}' at {1}",
                        "" + (char) cp._separator, badChar(ch)));
      else
        ch = _reader.read();
    }

    ch = expandPE(_reader.read());

    if (_strictXml && hasCdata && (ch == '+' || ch == '?'))
      throw error(L.l("pcdata clause can not have {0}", badChar(ch)));
    else if (ch == '*' || ch == '+' || ch == '?') {
      cp._repeat = ch;
      return _reader.read();
    }
    else
      return ch;
  }

  private int expandPE(int ch)
    throws IOException, SAXException
  {
    ch = skipWhitespace(ch);
    
    while (ch == '%') {
      parsePEReference();
      ch = skipWhitespace(_reader.read());
    }

    return ch;
  }

  /**
   * Parses a PE reference %foo; and inserts the macro text to the input
   * stream.
   */
  private void parsePEReference()
    throws IOException, SAXException
  {
    int ch = _reader.parseName(_buf, _reader.read());

    if (ch != ';')
      throw error(L.l("`%{0};' expects `;' at {1}.  Parameter entities have a `%name;' syntax.", _buf, badChar(ch)));

    addPEReference(_text, _buf.toString());
  }

  /**
   * Expands the macro value of a PE reference.
   */
  private void addPEReference(CharBuffer value, String name)
    throws IOException, SAXException
  {
    QEntity entity = _dtd.getParameterEntity(name);

    if (entity == null && ! _dtd.isExternal())
      throw error(L.l("`%{0};' is an unknown parameter entity.  Parameter entities must be defined in an <!ENTITY> declaration before use.", name));
    else if (entity != null && entity._value != null) {
      setMacro(entity._value);
    }
    else if (entity != null && entity.getSystemId() != null) {
      pushInclude(entity.getPublicId(), entity.getSystemId());
    }
    else {
      value.append("%");
      value.append(name);
      value.append(";");
    }
  }

  /**
   * <!ELEMENT name contentspec>
   */
  private void parseElementDecl(QDocumentType doctype)
    throws IOException, SAXException
  {
    int ch = skipWhitespace(_reader.read());

    ch = _reader.parseName(_text, ch);
    String name = _text.toString();

    ch = skipWhitespace(ch);

    QElementDef def = _dtd.addElement(name);
    def.setLocation(getSystemId(), getFilename(), getLine(), getColumn());

    boolean needsStartTag = true;
    boolean needsEndTag = true;

    if (_optionalTags && (ch == 'O' || ch == '-')) {
      needsStartTag = ch == '-';

      ch = skipWhitespace(ch);

      if (ch == '0')
        needsEndTag = false;
      else if (ch == '-')
        needsEndTag = true;
      else
        throw error(L.l("unknown short tag"));
    }

    ch = parseContentSpec(def, ch);

    ch = skipWhitespace(ch);

    if (ch != '>')
      throw error(L.l("`<!ELEMENT' must close with `>' at {0}", badChar(ch)));
  }

  private static String toAttrDefault(CharBuffer text)
  {
    for (int i = 0; i < text.length(); i++) {
      int ch = text.charAt(i);

      if (ch == '"') {
        text.delete(i, i + 1);
        text.insert(i, "&#34;");
        i--;
      } else if (ch == '\'') {
        text.delete(i, i + 1);
        text.insert(i, "&#39;");
        i--;
      }
    }

    return text.toString();
  }

  /**
   * <!ATTLIST name (attr type def)*>
   */
  private void parseAttlistDecl(QDocumentType doctype)
    throws IOException, SAXException
  {
    int ch = skipWhitespace(_reader.read());

    ch = _reader.parseName(_text, ch);
    String name = _text.toString();

    ch = skipWhitespace(ch);

    QElementDef def = _dtd.addElement(name);

    while (XmlChar.isNameStart((ch = expandPE(ch)))) {
      ch = _reader.parseName(_text, ch);
      String attrName = _text.toString();

      String attrType = null;
      ArrayList<String> enumeration = null;
      ch = expandPE(ch);
      if (ch == '(') {
        attrType = "#ENUM";
        enumeration = new ArrayList<String>();
        do {
          ch = expandPE(_reader.read());

          ch = parseNameToken(_text, ch);
          enumeration.add(_text.toString());

          ch = expandPE(ch);
        } while (ch == '|');

        if (ch != ')')
          throw error(L.l("expected `{0}' at {1}.  <!ATTRLIST> enumerations definitions are enclosed in '(' ... ')'.", ")", badChar(ch)));
        ch = _reader.read();
      }
      else {
        ch = _reader.parseName(_text, ch);
        attrType = _text.toString();

        if (attrType.equals("NOTATION")) {
          enumeration = new ArrayList<String>();
          ch = expandPE(ch);
          if (ch != '(')
            throw error(L.l("expected `{0}' at {1}", "(", badChar(ch)));

          do {
            ch = expandPE(_reader.read());

            ch = _reader.parseName(_text, ch);
            enumeration.add(_text.toString());

            ch = expandPE(ch);
          } while (ch == '|');

          if (ch != ')')
            throw error(L.l("expected `{0}' at {1}", ")", badChar(ch)));
          ch = _reader.read();
        }
        else if (_attrTypes.get(attrType) != null) {
        }
        else
          throw error(L.l("expected attribute type at `{0}'", attrType));
      }

      ch = skipWhitespace(ch);
      String qualifier = null;
      String attrDefault = null;
      if (ch == '#') {
        ch = _reader.parseName(_text, _reader.read());
        qualifier = "#" + _text.toString();

        if (qualifier.equals("#IMPLIED")) {
        }
        else if (qualifier.equals("#REQUIRED")) {
        }
        else if (qualifier.equals("#FIXED")) {
          ch = skipWhitespace(ch);
          ch = parseValue(_text, ch, false);
          attrDefault = _text.toString();
        } else
          throw error(L.l("expected attribute default at `{0}'",
                      qualifier));
      }
      else if (ch != '>') {
        ch = parseValue(_text, ch, false);
        attrDefault = _text.toString();
      }

      def.addAttribute(attrName, attrType, enumeration, 
                       qualifier, attrDefault);
      if (attrType != null && attrType.equals("ID"))
        doctype.setElementId(name, attrName);

      ch = skipWhitespace(ch);
    }

    if (ch != '>')
      throw error(L.l("expected `{0}' at {1}", ">", badChar(ch)));
  }

  /**
   * <!NOTATION name systemId publicId>
   */
  private void parseNotationDecl(QDocumentType doctype)
    throws IOException, SAXException
  {
    int ch = skipWhitespace(_reader.read());

    ch = _reader.parseName(_text, ch);
    String name = _text.toString();

    ch = skipWhitespace(ch);
    ch = _reader.parseName(_text, ch);
    String key = _text.toString();

    ch = skipWhitespace(ch);
    ch = parseValue(_text, ch, false);
    String id = _text.toString();

    ch = skipWhitespace(ch);

    QNotation notation;

    if (key.equals("PUBLIC")) {
      String systemId = null;

      if (ch == '"' || ch == '\'') {
        ch = parseValue(_text, ch, false);
        ch = skipWhitespace(ch);
        systemId = _text.toString();
      }

      notation = new QNotation(name, id, systemId);
      notation._owner = doctype._owner;
      notation.setLocation(getSystemId(), getFilename(), getLine(), getColumn());
    }
    else if (key.equals("SYSTEM")) {
      notation = new QNotation(name, null, id);
      notation._owner = doctype._owner;
      notation.setLocation(getSystemId(), getFilename(), getLine(), getColumn());
    }
    else
      throw error(L.l("expected PUBLIC or SYSTEM at `{0}'", key));
    
    doctype.addNotation(notation);
    doctype.appendChild(notation);

    if (ch != '>')
      throw error(L.l("expected `{0}' at {1}", ">", badChar(ch)));
  }

  /**
   * externalID ::= PUBLIC publicId systemId
   *            ::= SYSTEM systemId
   */
  private int parseExternalID(int ch)
    throws IOException, SAXException
  {
    ch = _reader.parseName(_text, ch);
    String key = _text.toString();
    ch = skipWhitespace(ch);

    _extSystemId = null;
    _extPublicId = null;
    if (key.equals("PUBLIC") || _forgiving && key.equalsIgnoreCase("public")) {
      ch = parseValue(_text, ch, false);
      _extPublicId = _text.toString();
      ch = skipWhitespace(ch);

      if (_extPublicId.indexOf('&') > 0)
        throw error(L.l("Illegal character '&' in PUBLIC identifier '{0}'",
                        _extPublicId));

      ch = parseValue(_text, ch, false);
      ch = skipWhitespace(ch);
      _extSystemId = _text.toString();
    }
    else if (key.equals("SYSTEM") ||
             _forgiving && key.equalsIgnoreCase("system")) {
      ch = parseValue(_text, ch, false);
      _extSystemId = _text.toString();
    }
    else
      throw error(L.l("expected PUBLIC or SYSTEM at `{0}'", key));

    return ch;
  }

  /**
   * <!ENTITY name systemId publicId>
   */
  private void parseEntityDecl(QDocumentType doctype)
    throws IOException, SAXException
  {
    int ch = skipWhitespace(_reader.read());

    boolean isPe = ch == '%';

    if (isPe)
      ch = skipWhitespace(_reader.read());

    ch = _reader.parseName(_text, ch);
    String name = _text.toString();

    ch = skipWhitespace(ch);

    QEntity entity;
    if (ch == '"' || ch == '\'') {
      ch = parseValue(_text, ch, false);
      
      entity = new QEntity(name, _text.toString(), null, null);
      entity._owner = doctype._owner;
      entity.setLocation(getSystemId(), getFilename(), getLine(), getColumn());
    }
    else {
      ch = parseExternalID(ch);

      entity = new QEntity(name, null, _extPublicId, _extSystemId);
      entity._owner = doctype._owner;
      entity.setLocation(getSystemId(), getFilename(), getLine(), getColumn());

      ch = skipWhitespace(ch);
      if (! isPe && XmlChar.isNameStart(ch)) {
        ch = _reader.parseName(_text, ch);
        String key = _text.toString();
        if (key.equals("NDATA")) {
          ch = skipWhitespace(ch);
          ch = _reader.parseName(_text, ch);

          String ndata = _text.toString();

          entity._ndata = ndata;
        } else
          throw error(L.l("expected `NDATA' at `{0}'", key));
      }
    }
      
    entity._isPe = isPe;

    if (isPe)
      doctype.addParameterEntity(entity);
    else
      doctype.addEntity(entity);

    doctype.appendChild(entity);

    ch = skipWhitespace(ch);

    if (ch != '>')
      throw error(L.l("expected `>' at {0}", badChar(ch)));
  }

  private boolean isWhitespace(int ch)
  {
    return ch <= 0x20 && (ch == 0x20 || ch == 0x9 || ch == 0xa || ch == 0xd);
  }

  private boolean isChar(int ch)
  {
    return (ch >= 0x20 && ch <= 0xd7ff ||
            ch == 0x9 ||
            ch == 0xa ||
            ch == 0xd ||
            ch >= 0xe000 && ch <= 0xfffd);
  }

  /**
   * Returns the hex representation of a byte.
   */
  private static String hex(int value)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int b = 3; b >= 0; b--) {
      int v = (value >> (4 * b)) & 0xf;
      if (v < 10)
        cb.append((char) (v + '0'));
      else
        cb.append((char) (v - 10 + 'a'));
    }

    return cb.close();
  }

  /**
   * Returns the current filename.
   */
  public String getFilename()
  {
    return _filename;
  }

  /**
   * Returns the current line.
   */
  public int getLine()
  {
    return _line;
  }
  
  /**
   * Returns the current column.
   */
  private int getColumn()
  {
    return 0;
  }

  /**
   * Returns the opening line of the current node.
   */
  int getNodeLine()
  {
    if (_elementTop > 0)
      return _elementLines[_elementTop - 1];
    else
      return 1;
  }

  /**
   * Returns the current public id being read.
   */
  public String getPublicId()
  {
    if (_reader != null)
      return _reader.getPublicId();
    else
      return _publicId;
  }
  
  /**
   * Returns the current system id being read.
   */
  public String getSystemId()
  {
    if (_reader != null)
      return _reader.getSystemId();
    else if (_systemId != null)
      return _systemId;
    else
      return _filename;
  }

  public void setLine(int line)
  {
    _line = line;
  }
  
  public int getLineNumber() { return getLine(); }
  public int getColumnNumber() { return getColumn(); }

  /**
   * Adds a string to the current text buffer.
   */
  private void addText(String s)
    throws IOException, SAXException
  {
    int len = s.length();
    
    for (int i = 0; i < len; i++)
      addText(s.charAt(i));
  }
  
  /**
   * Adds a character to the current text buffer.
   */
  private void addText(char ch)
    throws IOException, SAXException
  {
    if (_textLength >= _textCapacity) {
      appendText();
    }

    if (_textLength > 0 && _textBuffer[_textLength - 1] == '\r') {
      _textBuffer[_textLength - 1] = '\n';
      if (ch == '\n')
        return;
    }
    
    if (_isIgnorableWhitespace && ! XmlChar.isWhitespace(ch))
      _isIgnorableWhitespace = false;
    
    _textBuffer[_textLength++] = ch;
  }

  /**
   * Flushes the text buffer to the SAX callback.
   */
  private void appendText()
    throws IOException, SAXException
  {
    if (_textLength > 0) {
      if (_activeNode == DOC_NAME) {
        if (_isJspText) {
          _contentHandler.characters(_textBuffer, 0, _textLength);
        }
        else if (_isIgnorableWhitespace) {
        }
        else if (_strictXml)
          throw error(L.l("expected top element at `{0}'",
                          new String(_textBuffer, 0, _textLength)));
        else {
          addChild(TEXT_NAME);
          _contentHandler.characters(_textBuffer, 0, _textLength);
        }
      }
      else if (_isJspText) {
        _contentHandler.characters(_textBuffer, 0, _textLength);
      }
      else if (_isIgnorableWhitespace) {
        if (_isHtml)
          _contentHandler.characters(_textBuffer, 0, _textLength);
        else
          _contentHandler.ignorableWhitespace(_textBuffer, 0, _textLength);
      }
      else if (_strictXml && ! _isIgnorableWhitespace && _activeNode == DOC_NAME) {
      }
      else {
        if (_isJspText) {
        }
        else if (_isIgnorableWhitespace)
          addChild(WHITESPACE_NAME);
        else
          addChild(TEXT_NAME);
        _contentHandler.characters(_textBuffer, 0, _textLength);
      }
        
      _textLength = 0;
      _isIgnorableWhitespace = true;
    }
  }

  private void addElement(String child, boolean isEmpty,
                          QAttributes attributes,
                          NamespaceMap oldNamespace)
    throws IOException, SAXException
  {
    _text.clear();
    _text.append(child);
    addElement(_policy.getName(_text), isEmpty, attributes, oldNamespace);
  }
  
  /**
   * Adds an element as a child of the current tree.  Some
   * DTDs, like HTML, will push additional nodes to make
   * the tree work, e.g. the body tag.
   *
   * @param child the new child to be added.
   * @param isEmpty true if the tag is already closed.
   */
  private void addElement(QName child, boolean isEmpty,
                          QAttributes attributes, NamespaceMap oldNamespace)
    throws IOException, SAXException
  {
    if (! _doResinInclude) {
    }
    else if (child.getName() == "include" &&
             child.getNamespaceURI() == "http://caucho.com/ns/resin/core" ||
             child.getName() == "resin:include") {
      if (! isEmpty)
        throw error(L.l("resin:include must be an empty tag"));
      
      handleResinInclude();
      return;
    }
    else if (child.getName() == "include-directory" &&
             child.getNamespaceURI() == "http://caucho.com/ns/resin/core" ||
             child.getName() == "resin:include-directory") {
      if (! isEmpty)
        throw error(L.l("resin:include-directory must be an empty tag"));
      
      handleResinIncludeDirectory();
      return;
    }

    if (_activeNode == DOC_NAME && _hasTopElement && _strictXml)
      throw error(L.l("expected a single top-level element at `{0}'",
                      child.getName()));

    _hasTopElement = true;
    
    String childURI = child.getNamespaceURI();
    String childLocal = child.getLocalName();

    if (childURI == null) {
      childURI = "";

      if (_isNamespaceAware)
        childLocal = child.getName();
      else
        childLocal = "";
    }

    while (true) {
      int action = _policy.openAction(this, _activeNode, child);

      switch (action) {
      case Policy.IGNORE:
        return;

      case Policy.PUSH:
        //if (dbg.canWrite())
        //  dbg.println("<" + child.getNodeName() + ">");

        if (_contentHandler instanceof DOMBuilder)
          ((DOMBuilder) _contentHandler).startElement(child, attributes);
        else {
          _contentHandler.startElement(childURI,
                                       childLocal,
                                       child.getName(),
                                       attributes);
        }
        
        if (isEmpty) {
          _contentHandler.endElement(childURI,
                                     childLocal,
                                     child.getName());

          popNamespaces(oldNamespace);
        }
        else {
          if (_elementTop == _elementNames.length) {
            int len = _elementNames.length;
            QName []names = new QName[2 * len];
            NamespaceMap []newNamespaces = new NamespaceMap[2 * len];
            int []lines = new int[2 * len];
            System.arraycopy(_elementNames, 0, names, 0, len);
            System.arraycopy(_elementLines, 0, lines, 0, len);
            System.arraycopy(_namespaces, 0, newNamespaces, 0, len);
            _elementNames = names;
            _elementLines = lines;
            _namespaces = newNamespaces;
          }
          _namespaces[_elementTop] = oldNamespace;
          _elementLines[_elementTop] = getLine();
          _elementNames[_elementTop] = _activeNode;
          _elementTop++;
          _activeNode = child;
          _isTagStart = true;
        }
        return;

      case Policy.PUSH_EMPTY:
        //if (dbg.canWrite())
        //  dbg.println("<" + child.getNodeName() + "/>");

        if (_contentHandler instanceof DOMBuilder)
          ((DOMBuilder) _contentHandler).startElement(child, attributes);
        else {
          _contentHandler.startElement(childURI,
                                       childLocal,
                                       child.getName(),
                                       attributes);
        }

        _contentHandler.endElement(childURI,
                                   childLocal,
                                   child.getName());

        popNamespaces(oldNamespace);
        return;

      case Policy.PUSH_OPT:
        addElement(_policy.getOpt(), false, _nullAttributes, oldNamespace);
        break;
        
      case Policy.PUSH_VERBATIM:
        if (_contentHandler instanceof DOMBuilder)
          ((DOMBuilder) _contentHandler).startElement(child, attributes);
        else
          _contentHandler.startElement(childURI,
                                       childLocal,
                                       child.getName(),
                                       attributes);

        scanVerbatim(child.getName());
        appendText();
        _contentHandler.endElement(childURI,
                                   childLocal,
                                   child.getName());
        return;

      case Policy.POP:
        //if (dbg.canWrite())
        //  dbg.println("</" + activeNode.getNodeName() + ">");

        popNode();

        if (_activeNode == null)
          return;
        break;
        
      default:
        throw error(L.l("can't add `{0}' to `{1}'",
                        child.getName(), _activeNode.getName()));
      }
    }
  }

  /**
   * Adds a child node to the current node.
   */
  private void addChild(QName child)
    throws IOException, SAXException
  {
    while (_activeNode != null) {
      int action = _policy.openAction(this, _activeNode, child);

      switch (action) {
      case Policy.IGNORE:
        return;

      case Policy.PUSH:
        _isTagStart = true;
        
      case Policy.PUSH_EMPTY:
        //if (dbg.canWrite())
        //  dbg.println("<" + child.getNodeName() + ">");

        /*
        if (child.getNodeType() == child.TEXT_NODE) {
          String value = child.getNodeValue();
          contentHandler.characters(value.toCharArray(), 0, value.length());
        }
        */
        return;

      case Policy.PUSH_OPT:
        addElement(_policy.getOpt(), false, _nullAttributes, _namespaceMap);
        break;
        
      case Policy.PUSH_VERBATIM:
        scanVerbatim(child.getName());
        return;

      case Policy.POP:
        // if (dbg.canWrite())
        //   dbg.println("</" + activeNode.getNodeName() + ">");

        popNode();
        break;
      default:
        throw error(L.l("cannot add `{0}' to `{1}'",
                        child.getName(), _activeNode.getName()));
      }
    }
  }

  private void scanVerbatim(String name)
    throws IOException, SAXException
  {
    int ch = _reader.read(); 

    while (ch >= 0) {
      if (ch != '<') {
        addText((char) ch);
        ch = _reader.read();
      }
      else if ((ch = _reader.read()) != '/')
        addText('<');
      else {
        ch = _reader.parseName(_eltName, _reader.read());

        if (! _eltName.matchesIgnoreCase(name)) {
          addText("</");
          addText(_eltName.toString());
        }
        else if (ch != '>') {
          addText("</");
          addText(_eltName.toString());
        }
        else {
          return;
        }
      }
    }

    throw error(L.l("expected </{0}> at {1}", name,
                    badChar(ch)));
  }
  
  private int skipWhitespace(int ch)
    throws IOException, SAXException
  {
    while (ch <= 0x20 && (ch == 0x20 || ch == 0x9 || ch == 0xa || ch == 0xd)) {
      ch = read();
    }

    return ch;
  }


  public void setReader(XmlReader reader)
  {
    _reader = reader;
  }

  /**
   * Adds text to the macro, escaping attribute values.
   */
  private void setMacroAttr(String text)
    throws IOException, SAXException
  {
    if (_reader != _macro) {
      _macro.init(this, _reader);
      _reader = _macro;
    }

    int j = _macroIndex;
    for (int i = 0; i < text.length(); i++) {
      int ch = text.charAt(i);

      if (ch == '\'')
        _macro.add("&#39;");
      else if (ch == '"')
        _macro.add("&#34;");
      else
        _macro.add((char) ch);
    }
  }

  private void pushInclude(String systemId)
    throws IOException, SAXException
  {
    pushInclude(null, systemId);
  }
  /**
   * Pushes the named file as a lexical include.
   *
   * @param systemId the name of the file to include.
   */
  private void pushInclude(String publicId, String systemId)
    throws IOException, SAXException
  {
    InputStream stream = openStream(systemId, publicId);
    if (stream == null)
      throw new FileNotFoundException(systemId);
    _is = Vfs.openRead(stream);
    Path oldSearchPath = _searchPath;
    Path path = _is.getPath();
    if (path != null) {
      _owner.addDepend(path);
      
      if (_searchPath != null) {
        _searchPath = path.getParent();
        _reader.setSearchPath(oldSearchPath);
      }
    }

    _filename = systemId;
    /*
    XmlReader nextReader;
    if (_reader instanceof Utf8Reader)
      nextReader = new Utf8Reader(this, _is);
    else {
      _is.setEncoding(_reader.getReadStream().getEncoding());
      nextReader = new XmlReader(this, _is);
    }
    _reader = nextReader;
    */

    XmlReader oldReader = _reader;
    _reader = null;
    
    _line = 1;

    int ch = parseXMLDeclaration(oldReader);

    XmlReader reader = _reader;

    if (reader instanceof MacroReader)
      reader = reader.getNext();
    
    reader.setSystemId(systemId);
    reader.setFilename(systemId);
    reader.setPublicId(publicId);
    reader.setNext(oldReader);

    unread(ch);
  }

  private void popInclude()
    throws IOException, SAXException
  {
    XmlReader oldReader = _reader;
    _reader = _reader.getNext();
    oldReader.setNext(null);
    _filename = _reader.getFilename();
    _line = _reader.getLine();
    _is = _reader.getReadStream();
    if (_reader.getSearchPath() != null)
      _searchPath = _reader.getSearchPath();
  }

  private void setMacro(String text)
    throws IOException, SAXException
  {
    if (_reader == _macro) {
    }
    else if (_macro.getNext() == null) {
      _macro.init(this, _reader);
      _reader = _macro;
    }
    else {
      _macro = new MacroReader();
      _macro.init(this, _reader);
      _reader = _macro;
    }
    
    _macro.add(text);
  }

  private int read()
    throws IOException, SAXException
  {
    int ch = _reader.read();
    while (ch < 0 && _reader.getNext() != null) {
      if (_stopOnIncludeEnd)
        return -1;
      
      popInclude();
      ch = _reader.read();
    }
    
    return ch;
  }
    

  public void unread(int ch)
  {
    if (ch < 0) {
      return;
    }
    else if (_reader == _macro) {
    }
    else if (_macro.getNext() == null) {
      _macro.init(this, _reader);
      _reader = _macro;
    }
    else {
      _macro = new MacroReader();
      _macro.init(this, _reader);
      _reader = _macro;
    }
    
    _macro.prepend((char) ch);
  }

  /**
   * Returns an error including the current line.
   *
   * @param text the error message text.
   */
  XmlParseException error(String text)
  {

    StringBuilder lines = new StringBuilder();

    try {
      Path path = Vfs.lookup(_systemId);

      if (path.canRead()) {
        ReadStream is = path.openRead();

        lines.append("\n");

        try {
          for (int i = 1; i < _line + 3; i++) {
            String line = is.readLine();

            if (line == null)
              break;

            if (_line - 3 < i && i < _line + 3) {
              lines.append(i).append(": ").append(line).append("\n");
            }
          }
        } finally {
          is.close();
        }
      }
    } catch (IOException e) {
    }

    text = text + lines;
    
    if (_errorHandler != null) {
      SAXParseException e = new SAXParseException(text, _locator);

      try {
        _errorHandler.fatalError(e);
      } catch (SAXException e1) {
      }
    }
    
    return new XmlParseException(_filename, _line, text);
  }

  private void generateDtdValidator(QDocumentType dtd)
    throws SAXException
  {
    DtdRelaxGenerator gen = new DtdRelaxGenerator(dtd);
    ContentHandler handler = gen.generate();

    if (handler != null) {
      handler.setDocumentLocator(_locator);
      handler.startDocument();

      _contentHandler = new TeeContentHandler(handler, _contentHandler);
    }
  }

  public void free()
  {
    _filename = null;
  }

  /**
   * Returns a user-readable string for an error character.
   */
  static String badChar(int ch)
  {
    if (ch < 0 || ch == 0xffff)
      return L.l("end of file");
    else if (ch == '\n' || ch == '\r')
      return L.l("end of line");
    else if (ch >= 0x20 && ch <= 0x7f)
      return "`" + (char) ch + "'";
    else
      return "`" + (char) ch + "' (\\u" + hex(ch) + ")";
  }

  private void printDebugNode(WriteStream s, Node node, int depth)
    throws IOException
  {
    if (node == null)
      return;

    for (int i = 0; i < depth; i++)
      s.print(' ');

    if (node.getFirstChild() != null) {
      s.println("<" + node.getNodeName() + ">");
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        printDebugNode(s, child, depth + 2);
      }
      for (int i = 0; i < depth; i++)
        s.print(' ');
      s.println("</" + node.getNodeName() + ">");
    }
    else
      s.println("<" + node.getNodeName() + "/>");
  }

  public static class LocatorImpl implements ExtendedLocator {
    XmlParser _parser;

    LocatorImpl(XmlParser parser)
    {
      _parser = parser;
    }
    
    public String getSystemId()
    {
      if (_parser._reader != null && _parser._reader.getSystemId() != null)
        return _parser._reader.getSystemId();
      else if (_parser.getSystemId() != null)
        return _parser.getSystemId();
      else if (_parser._reader != null && _parser._reader.getFilename() != null)
        return _parser._reader.getFilename();
      else if (_parser.getFilename() != null)
        return _parser.getFilename();
      else
        return null;
    }
    
    public String getFilename()
    {
      if (_parser._reader != null && _parser._reader.getFilename() != null)
        return _parser._reader.getFilename();
      else if (_parser.getFilename() != null)
        return _parser.getFilename();
      else if (_parser._reader != null && _parser._reader.getSystemId() != null)
        return _parser._reader.getSystemId();
      else if (_parser.getSystemId() != null)
        return _parser.getSystemId();
      else
        return null;
    }
    
    public String getPublicId()
    {
      if (_parser._reader != null)
        return _parser._reader.getPublicId();
      else
        return _parser.getPublicId();
    }

    public int getLineNumber()
    {
      if (_parser._reader != null)
        return _parser._reader.getLine();
      else
        return _parser.getLineNumber();
    }

    public int getColumnNumber()
    {
      return _parser.getColumnNumber();
    }
  }
}
