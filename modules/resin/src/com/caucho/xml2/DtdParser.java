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

package com.caucho.xml2;

import com.caucho.util.*;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReaderWriterStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml2.readers.MacroReader;
import com.caucho.xml2.readers.Utf16Reader;
import com.caucho.xml2.readers.Utf8Reader;
import com.caucho.xml2.readers.XmlReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.namespace.QName;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

/**
 * A configurable XML parser.  Loose versions of XML and HTML are supported
 * by changing the Policy object.
 *
 * <p>Normally, applications will use Xml, LooseXml, Html, or LooseHtml.
 */
public class DtdParser
{
  private static final L10N L = new L10N(DtdParser.class);
  
  static HashMap<String,String> _attrTypes = new HashMap<String,String>();
  static Entities _entities = new XmlEntities();
  
  private XmlParser _xmlParser;

  
  QAttributes _attributes;
  QAttributes _nullAttributes;

  boolean _inDtd;
  boolean _strictComments = true;
  
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
  QDocumentType _dtd;

  public DtdParser(XmlParser xmlParser, QDocumentType dtd)
  {
    _xmlParser = xmlParser;
    _dtd = dtd;
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
  int parseDoctypeDecl(QDocumentType doctype)
    throws IOException, SAXException
  {
    _hasDoctype = true;
    int ch = 0;

    for (ch = _xmlParser.skipWhitespace(read()); 
         ch >= 0 && ch != ']';
         ch = _xmlParser.skipWhitespace(read())) {
      if (ch == '<') {
        if ((ch = read()) == '!') {
          if (XmlChar.isNameStart(ch = read())) {
            ch = _xmlParser.parseName(_text, ch);
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
              throw error("unknown declaration '" + name + "'");
          }
          else if (ch == '-')
            parseComment();
          else if (ch == '[') {
            ch = _xmlParser.parseName(_text, read());
            String name = _text.toString();

            if (name.equals("IGNORE")) {
              parseIgnore();
            }
            else if (name.equals("INCLUDE")) {
              parseIgnore();
            }
            else
              throw error(L.l("unknown declaration '{0}'", name));
          }
        }
        else if (ch == '?') {
          parsePI();
        }
        else
          throw error(L.l("expected markup at {0}", badChar(ch)));
      }
      else if (ch == '%') {
        ch = _xmlParser.parseName(_buf, read());

        if (ch != ';')
          throw error(L.l("'%{0};' expects ';' at {1}.  Parameter entities have a '%name;' syntax.", _buf, badChar(ch)));

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

  private int parseNameToken(CharBuffer name, int ch)
    throws IOException, SAXException
  {
    name.clear();

    if (! XmlChar.isNameChar(ch))
      throw error(L.l("expected name at {0}", badChar(ch)));

    for (; XmlChar.isNameChar(ch); ch = read())
      name.append((char) ch);

    return ch;
  }

  private void appendText(String s)
  {
    if (_text.length() == 0) {
      _textFilename = getFilename();
      _textLine = getLine();
    }

    _text.append(s);
  }

  private int parseCharacterReference()
    throws IOException, SAXException
  {
    int ch = read();

    int radix = 10;
    if (ch == 'x') {
      radix = 16;
      ch = read();
    }

    int value = 0;
    for (; ch != ';'; ch = read()) {
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
    if (! isChar(value))
      throw error(L.l("illegal character ref at {0}", badChar(value)));

    return value;
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
      ch = read();
    else {
      value.append((char) end);
      for (ch = read();
           ch >= 0 && XmlChar.isNameChar(ch);
           ch = read())
        value.append((char) ch);
      
      throw error(L.l("XML attribute value must be quoted at '{0}'.  XML attribute syntax is either attr=\"value\" or attr='value'.",
                      value));
    }

    while (ch != -1 && (end != 0 && ch != end
                        || end == 0 && isAttributeChar(ch))) {
      if (end == 0 && ch == '/') {
        ch = read();
        if (! isWhitespace(ch) && ch != '>') {
          value.append('/');
          value.append((char) ch);
        }
        else {
          unread(ch);
          return '/';
        }
      }
      else if (ch == '&') {
        if ((ch = read()) == '#')
          value.append((char) parseCharacterReference());
        else if (! isGeneral) {
          value.append('&');
          value.append((char) ch);
        }
        else if (XmlChar.isNameStart(ch)) {
          ch = _xmlParser.parseName(_buf, ch);
          String name = _buf.toString();

          if (ch != ';')
            throw error(L.l("expected '{0}' at {1}", ";", badChar(ch)));
          else {
            int lookup = _entities.getEntity(name);

            if (lookup >= 0 && lookup <= 0xffff) {
              ch = read();
              value.append((char) lookup);
              continue;
            }
            
            QEntity entity = _dtd == null ? null : _dtd.getEntity(name);
            if (entity != null && entity._value != null)
              _xmlParser.setMacroAttr(entity._value);
            else
              throw error(L.l("expected local reference at '&{0};'", name));
          }
        }
      }
      else if (ch == '%' && ! isGeneral) {
        ch = read();

        if (! XmlChar.isNameStart(ch)) {
          value.append('%');
          continue;
        }
        else {
          ch = _xmlParser.parseName(_buf, ch);

          if (ch != ';')
            throw error(L.l("expected '{0}' at {1}", ";", badChar(ch)));
          else
            addPEReference(value, _buf.toString());
        }
      } 
      else if (isGeneral) {
        if (ch == '\r') {
          ch = read();
          if (ch != '\n') {
            value.append('\n');
            continue;
          }
        }
        value.append((char) ch);
      }
      else if (ch == '\r') {
        value.append(' ');
        
        if ((ch = read()) != '\n')
          continue;
      }
      else if (ch == '\n')
        value.append(' ');
      else
        value.append((char) ch);

      ch = read();
    }

    if (end != 0)
      ch = read();

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

  private int parsePI()
    throws IOException, SAXException
  {
    int ch;

    ch = read();
    if (! XmlChar.isNameStart(ch))
      throw error(L.l("expected name after '<?' at {0}.  Processing instructions expect a name like <?foo ... ?>", badChar(ch)));
    ch = _xmlParser.parseName(_text, ch);

    String piName = _text.toString();
    if (! piName.equals("xml"))
      return parsePITail(piName, ch);
    else {
      throw error(L.l("<?xml ... ?> occurs after content.  The <?xml ... ?> prolog must be at the document start."));

    }
  }

  private int parsePITail(String piName, int ch)
    throws IOException, SAXException
  {
    ch = _xmlParser.skipWhitespace(ch);

    _text.clear();
    while (ch != -1) {
      if (ch == '?') {
        if ((ch = read()) == '>')
          break;
        else
          _text.append('?');
      } else {
        _text.append((char) ch);
        ch = read();
      }
    }

    QProcessingInstruction pi;
    pi = new QProcessingInstruction(piName, _text.toString());
    pi._owner = _dtd._owner;
    _dtd.appendChild(pi);

    return read();
  }

  /**
   * Parses a comment.  The "&lt;!--" has already been read.
   */
  private void parseComment()
    throws IOException, SAXException
  {
    int ch = read();

    if (ch != '-')
      throw error(L.l("expected comment at {0}", badChar(ch)));

    ch = read();

  comment:
    while (ch != -1) {
      if (ch == '-') {
        ch = read();

        while (ch == '-') {
          if ((ch = read()) == '>')
            break comment;
          else if (_strictComments)
            throw error(L.l("XML forbids '--' in comments"));
          else if (ch == '-') {
          }
          else {
            break;
          }
        }
      } else if (! XmlChar.isChar(ch)) {
        throw error(L.l("bad character {0}", hex(ch)));
      } else {
        ch = read();
      }
    }

    QComment comment = new QComment(_buf.toString());
    comment._owner = _dtd._owner;
    _dtd.appendChild(comment);
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
      ch = _xmlParser.parseName(_text, ch);
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
        throw error(L.l("expected EMPTY or ANY at '{0}'", name));
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

    ch = expandPE(read());
    
    for (; ch != -1; ch = expandPE(ch)) {
      if (ch == '(') {
        QContentParticle child = new QContentParticle();
        cp.addChild(child);

        ch = parseContentParticle(child, false);
      }
      else if (XmlChar.isNameStart(ch)) {
        ch = _xmlParser.parseName(_text, ch);
        cp.addChild(_text.toString());
      }
      else if (ch == '#') {
        ch = _xmlParser.parseName(_text, read());
        String name = _text.toString();

        if (cp._children.size() != 0)
          throw error(L.l("'#{0}' must occur first", name));
        if (! isTop)
          throw error(L.l("'#{0}' may only occur at top level", name));

        if (name.equals("PCDATA"))
          cp.addChild("#PCDATA");
        else
          throw error(L.l("illegal content particle at '#{0}'", name));

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

        ch = expandPE(read());
      }

      if (ch == ')')
        break;
      else if (cp._separator == 0) {
        if (ch == '|')
          cp._separator = ch;
        else if (hasCdata)
          throw error(L.l("#PCDATA must be separated by '|' at {0}",
                          badChar(ch)));
        else if (ch == ',')
          cp._separator = ch;
        else
          throw error(L.l("expected separator at {0}", badChar(ch)));

        ch = read();
      } else if (ch != cp._separator)
        throw error(L.l("expected '{0}' at {1}",
                        "" + (char) cp._separator, badChar(ch)));
      else
        ch = read();
    }

    ch = expandPE(read());

    if (hasCdata && (ch == '+' || ch == '?'))
      throw error(L.l("pcdata clause can not have {0}", badChar(ch)));
    else if (ch == '*' || ch == '+' || ch == '?') {
      cp._repeat = ch;
      return read();
    }
    else
      return ch;
  }

  private int expandPE(int ch)
    throws IOException, SAXException
  {
    ch = _xmlParser.skipWhitespace(ch);
    
    while (ch == '%') {
      parsePEReference();
      ch = _xmlParser.skipWhitespace(read());
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
    int ch = _xmlParser.parseName(_buf, read());

    if (ch != ';')
      throw error(L.l("'%{0};' expects ';' at {1}.  Parameter entities have a '%name;' syntax.", _buf, badChar(ch)));

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
      throw error(L.l("'%{0};' is an unknown parameter entity.  Parameter entities must be defined in an <!ENTITY> declaration before use.", name));
    else if (entity != null && entity._value != null) {
      _xmlParser.setMacro(entity._value);
    }
    else if (entity != null && entity.getSystemId() != null) {
      _xmlParser.pushInclude(entity.getPublicId(), entity.getSystemId());
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
    int ch = _xmlParser.skipWhitespace(read());

    ch = _xmlParser.parseName(_text, ch);
    String name = _text.toString();

    ch = _xmlParser.skipWhitespace(ch);

    QElementDef def = _dtd.addElement(name);
    def.setLocation(getSystemId(), getFilename(), getLine(), getColumn());

    boolean needsStartTag = true;
    boolean needsEndTag = true;

    ch = parseContentSpec(def, ch);

    ch = _xmlParser.skipWhitespace(ch);

    if (ch != '>')
      throw error(L.l("'<!ELEMENT' must close with '>' at {0}", badChar(ch)));
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
    int ch = _xmlParser.skipWhitespace(read());

    ch = _xmlParser.parseName(_text, ch);
    String name = _text.toString();

    ch = _xmlParser.skipWhitespace(ch);

    QElementDef def = _dtd.addElement(name);

    while (XmlChar.isNameStart((ch = expandPE(ch)))) {
      ch = _xmlParser.parseName(_text, ch);
      String attrName = _text.toString();

      String attrType = null;
      ArrayList<String> enumeration = null;
      ch = expandPE(ch);
      if (ch == '(') {
        attrType = "#ENUM";
        enumeration = new ArrayList<String>();
        do {
          ch = expandPE(read());

          ch = parseNameToken(_text, ch);
          enumeration.add(_text.toString());

          ch = expandPE(ch);
        } while (ch == '|');

        if (ch != ')')
          throw error(L.l("expected '{0}' at {1}.  <!ATTRLIST> enumerations definitions are enclosed in '(' ... ')'.", ")", badChar(ch)));
        ch = read();
      }
      else {
        ch = _xmlParser.parseName(_text, ch);
        attrType = _text.toString();

        if (attrType.equals("NOTATION")) {
          enumeration = new ArrayList<String>();
          ch = expandPE(ch);
          if (ch != '(')
            throw error(L.l("expected '{0}' at {1}", "(", badChar(ch)));

          do {
            ch = expandPE(read());

            ch = _xmlParser.parseName(_text, ch);
            enumeration.add(_text.toString());

            ch = expandPE(ch);
          } while (ch == '|');

          if (ch != ')')
            throw error(L.l("expected '{0}' at {1}", ")", badChar(ch)));
          ch = read();
        }
        else if (_attrTypes.get(attrType) != null) {
        }
        else
          throw error(L.l("expected attribute type at '{0}'", attrType));
      }

      ch = _xmlParser.skipWhitespace(ch);
      String qualifier = null;
      String attrDefault = null;
      if (ch == '#') {
        ch = _xmlParser.parseName(_text, read());
        qualifier = "#" + _text.toString();

        if (qualifier.equals("#IMPLIED")) {
        }
        else if (qualifier.equals("#REQUIRED")) {
        }
        else if (qualifier.equals("#FIXED")) {
          ch = _xmlParser.skipWhitespace(ch);
          ch = parseValue(_text, ch, false);
          attrDefault = _text.toString();
        } else
          throw error(L.l("expected attribute default at '{0}'",
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

      ch = _xmlParser.skipWhitespace(ch);
    }

    if (ch != '>')
      throw error(L.l("expected '{0}' at {1}", ">", badChar(ch)));
  }

  /**
   * <!NOTATION name systemId publicId>
   */
  private void parseNotationDecl(QDocumentType doctype)
    throws IOException, SAXException
  {
    int ch = _xmlParser.skipWhitespace(read());

    ch = _xmlParser.parseName(_text, ch);
    String name = _text.toString();

    ch = _xmlParser.skipWhitespace(ch);
    ch = _xmlParser.parseName(_text, ch);
    String key = _text.toString();

    ch = _xmlParser.skipWhitespace(ch);
    ch = parseValue(_text, ch, false);
    String id = _text.toString();

    ch = _xmlParser.skipWhitespace(ch);

    QNotation notation;

    if (key.equals("PUBLIC")) {
      String systemId = null;

      if (ch == '"' || ch == '\'') {
        ch = parseValue(_text, ch, false);
        ch = _xmlParser.skipWhitespace(ch);
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
      throw error(L.l("expected PUBLIC or SYSTEM at '{0}'", key));
    
    doctype.addNotation(notation);
    doctype.appendChild(notation);

    if (ch != '>')
      throw error(L.l("expected '{0}' at {1}", ">", badChar(ch)));
  }

  /**
   * externalID ::= PUBLIC publicId systemId
   *            ::= SYSTEM systemId
   */
  private int parseExternalID(int ch)
    throws IOException, SAXException
  {
    ch = _xmlParser.parseName(_text, ch);
    String key = _text.toString();
    ch = _xmlParser.skipWhitespace(ch);

    _extSystemId = null;
    _extPublicId = null;
    if (key.equals("PUBLIC")) {
      ch = parseValue(_text, ch, false);
      _extPublicId = _text.toString();
      ch = _xmlParser.skipWhitespace(ch);

      if (_extPublicId.indexOf('&') > 0)
        throw error(L.l("Illegal character '&' in PUBLIC identifier '{0}'",
                        _extPublicId));

      ch = parseValue(_text, ch, false);
      ch = _xmlParser.skipWhitespace(ch);
      _extSystemId = _text.toString();
    }
    else if (key.equals("SYSTEM")) {
      ch = parseValue(_text, ch, false);
      _extSystemId = _text.toString();
    }
    else
      throw error(L.l("expected PUBLIC or SYSTEM at '{0}'", key));

    return ch;
  }

  /**
   * <!ENTITY name systemId publicId>
   */
  private void parseEntityDecl(QDocumentType doctype)
    throws IOException, SAXException
  {
    int ch = _xmlParser.skipWhitespace(read());

    boolean isPe = ch == '%';

    if (isPe)
      ch = _xmlParser.skipWhitespace(read());

    ch = _xmlParser.parseName(_text, ch);
    String name = _text.toString();

    ch = _xmlParser.skipWhitespace(ch);

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

      ch = _xmlParser.skipWhitespace(ch);
      if (! isPe && XmlChar.isNameStart(ch)) {
        ch = _xmlParser.parseName(_text, ch);
        String key = _text.toString();
        if (key.equals("NDATA")) {
          ch = _xmlParser.skipWhitespace(ch);
          ch = _xmlParser.parseName(_text, ch);

          String ndata = _text.toString();

          entity._ndata = ndata;
        } else
          throw error(L.l("expected 'NDATA' at '{0}'", key));
      }
    }
      
    entity._isPe = isPe;

    if (isPe)
      doctype.addParameterEntity(entity);
    else
      doctype.addEntity(entity);

    doctype.appendChild(entity);

    ch = _xmlParser.skipWhitespace(ch);

    if (ch != '>')
      throw error(L.l("expected '>' at {0}", badChar(ch)));
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

  private int read()
    throws IOException, SAXException
  {
    return _xmlParser.read();
  }

  public void unread(int ch)
  {
    _xmlParser.unread(ch);
  }

  private String getSystemId()
  {
    return _xmlParser.getSystemId();
  }

  private String getFilename()
  {
    return _xmlParser.getFilename();
  }

  private XmlParseException error(String msg)
  {
    return _xmlParser.error(msg);
  }

  private int getLine()
  {
    return _xmlParser.getLine();
  }

  private int getColumn()
  {
    return _xmlParser.getColumn();
  }

  private String badChar(int ch)
  {
    return _xmlParser.badChar(ch);
  }
  
  static {
    _attrTypes.put("CDATA", "CDATA");
    _attrTypes.put("ID", "ID");
    _attrTypes.put("IDREF", "IDREF");
    _attrTypes.put("IDREFS", "IDREFS");
    _attrTypes.put("ENTITY", "ENTITY");
    _attrTypes.put("ENTITIES", "ENTITIES");
    _attrTypes.put("NMTOKEN", "NMTOKEN");
    _attrTypes.put("NMTOKENS", "NMTOKENS");
  }
}
