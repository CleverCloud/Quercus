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

package com.caucho.relaxng;

import com.caucho.relaxng.pattern.*;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builder for the relax.
 */
public class CompactParser {
  private static final L10N L = new L10N(CompactParser.class);
  private static final Logger log
    = Logger.getLogger(CompactParser.class.getName());

  private static final boolean []NAME_CHAR;

  private static final int IDENTIFIER = 256;
  
  private static final int NAMESPACE = IDENTIFIER + 1;
  private static final int DEFAULT = NAMESPACE + 1;
  
  private static final int START = DEFAULT + 1;
  private static final int DIV = START + 1;
  private static final int INCLUDE = DIV + 1;
  
  private static final int ELEMENT = INCLUDE + 1;
  private static final int ATTRIBUTE = ELEMENT + 1;
  
  private static final int TEXT = ATTRIBUTE + 1;
  private static final int STRING = TEXT + 1;
  private static final int TOKEN = STRING + 1;
  private static final int LITERAL = TOKEN + 1;
  
  private static final int EMPTY = LITERAL + 1;
  
  private static final int COMMENT = EMPTY + 1;

  private static final IntMap _tokenMap = new IntMap();

  private GrammarPattern _grammar;
  private Pattern _pattern;

  private String _ns = "";
  private HashMap<String,String> _nsMap;

  private Path _pwd;
  private ReadStream _is;
  private String _filename;
  private int _line;

  private int _peekToken = -1;

  private final byte []_buffer = new byte[256];
  private int _offset;
  private int _length;

  private CharBuffer _cb = new CharBuffer(256);
  private String _lexeme;

  private int _generatedId;

  CompactParser()
  {
  }

  /**
   * Gets the root pattern.
   */
  public GrammarPattern getGrammar()
  {
    return _grammar;
  }

  public void setGeneratedId(int id)
  {
    _generatedId = id;
  }

  public String generateId()
  {
    _cb.setLength(0);
    _cb.append("__caucho_");
    _cb.append(_generatedId++);
    
    return _cb.toString();
  }

  /**
   * Parses the relax file.
   */
  public void parse(InputSource source)
    throws SAXException, IOException, RelaxException
  {
    InputStream is = source.getByteStream();

    _pwd = null;

    if (is instanceof ReadStream) {
      _is = (ReadStream) is;
      _filename = _is.getUserPath();
      _pwd = _is.getPath().getParent();
    }
    if (is != null)
      _is = Vfs.openRead(is);
    else
      _is = Vfs.openRead(source.getSystemId());

    if (_filename == null)
      _filename = source.getSystemId();
    _line = 1;

    if (_pwd == null)
      _pwd = Vfs.lookup(_filename).getParent();

    try {
      parse();
    } catch (RelaxException e) {
      log.log(Level.FINER, e.toString(), e);
      
      // xml/1196
      //throw new SAXException(_filename + ":" + _line + ": " + e.getMessage(), e);
      throw new SAXException(_filename + ":" + _line + ": " + e.getMessage());
    } finally {
      _is.close();
    }
  }

  /**
   * Internal parser.
   */
  private void parse()
    throws SAXException, IOException, RelaxException
  {
    _grammar = new GrammarPattern();
    _nsMap = new HashMap<String,String>();

    parseDeclarations();
    
    int token = parseToken();
    _peekToken = token;

    switch (token) {
    case START:
    case IDENTIFIER:
    case INCLUDE:
      parseGrammar(_grammar);
      break;

    case COMMENT:
      break;

    default:
      _grammar.setStart(parsePattern(_grammar));
      break;
    }
  }

  /**
   * Parses declarations.
   */
  private void parseDeclarations()
    throws SAXException, IOException, RelaxException
  {
    while (true) {
      int token = parseToken();

      _peekToken = token;
      
      switch (token) {
      case DEFAULT:
      case NAMESPACE:
        parseNamespace();
        break;
        
      case COMMENT:
        break;

      default:
        return;
      }
    }
  }
  
  /**
   * Parses the namespace declaration
   */
  private void parseNamespace()
    throws SAXException, IOException, RelaxException
  {
    boolean isDefault = false;
    int token = parseToken();

    if (token == DEFAULT) {
      isDefault = true;
      token = parseToken();
    }

    if (token != NAMESPACE)
      throw error(L.l("expected 'namespace' at {0}", errorToken(token)));
      
    token = parseToken();

    if (token != IDENTIFIER)
      throw error(L.l("expected identifier at {0}", errorToken(token)));

    String prefix = _lexeme;

    token = parseToken();
    
    if (token != '=')
      throw error(L.l("expected '=' at {0}", errorToken(token)));

    String value = parseLiteral();

    if (isDefault)
      _ns = value;

    _nsMap.put(prefix, value);
  }

  /**
   * Parses top-level grammar stuff.
   */
  private void parseGrammar(GrammarPattern grammar)
    throws IOException, SAXException, RelaxException, RelaxException
  {
    while (true) {
      int token = parseToken();
      Pattern pattern;

      switch (token) {
      case -1:
        return;

      case COMMENT:
        break;

      case START:
        int next = parseToken();
        if (next == '=')
          grammar.setStart(parsePattern(grammar));
        else
          throw error(L.l("expected '=' at {0}", errorToken(next)));
        break;

      case IDENTIFIER:
        String name = _lexeme;
        Pattern oldPattern = grammar.getDefinition(name);
        pattern = new GroupPattern();
        next = parseToken();
        if (next == '=') {
          if (grammar.getDefinition(name) != null)
            throw error(L.l("duplicate definition of {0}", name));
            
          grammar.setDefinition(name, parsePattern(grammar));
        }
        else
          throw error(L.l("expected '=' at {0}", errorToken(next)));
        break;

      case INCLUDE:
        parseInclude(grammar);
        break;

      default:
        throw error(L.l("unexpected token {0}", errorToken(token)));
      }
    }
  }
        
  private void parseInclude(GrammarPattern grammar)
    throws IOException, SAXException, RelaxException
  {
    String uri = parseLiteral();

    Path sub = _pwd.lookup(uri);

    ReadStream is = null;
    
    try {
      is = sub.openRead();

      InputSource source = new InputSource(is);
      source.setSystemId(uri);

      CompactParser parser = new CompactParser();
      parser.setGeneratedId(_generatedId);
      parser.parse(source);

      GrammarPattern subGrammar = parser.getGrammar();

      _generatedId = parser._generatedId;

      grammar.mergeInclude(subGrammar);
    } finally {
      if (is != null)
        is.close();
    }
  }
        
  /**
   * Parses a pattern.
   */
  private Pattern parsePattern(GrammarPattern grammar)
    throws IOException, SAXException, RelaxException
  {
    Pattern pattern = parseTerm(grammar);

    int token = parseToken();

    switch (token) {
    case '|':
      return parseChoicePattern(grammar, pattern);
    case '&':
      return parseInterleavePattern(grammar, pattern);
    case ',':
      return parseGroupPattern(grammar, pattern);

    default:
      _peekToken = token;
      return pattern;
    }
  }

  /**
   * Parses a interleave pattern.
   */
  private Pattern parseInterleavePattern(GrammarPattern grammar,
                                         Pattern pattern)
    throws IOException, SAXException, RelaxException
  {
    int token;

    do {
      if (! (pattern instanceof InterleavePattern)) {
        Pattern child = pattern;
        pattern = new InterleavePattern();
        pattern.addChild(child);
      }
      
      pattern.addChild(parseTerm(grammar));
    } while ((token = parseToken()) == '&');

    _peekToken = token;

    return pattern;
  }

  /**
   * Parses a group pattern.
   */
  private Pattern parseGroupPattern(GrammarPattern grammar, Pattern pattern)
    throws IOException, SAXException, RelaxException
  {
    int token;

    do {
      if (! (pattern instanceof GroupPattern)) {
        Pattern child = pattern;
        pattern = new GroupPattern();
        pattern.addChild(child);
      }
      
      pattern.addChild(parseTerm(grammar));
    } while ((token = parseToken()) == ',');

    _peekToken = token;

    return pattern;
  }

  /**
   * Parses a choice pattern.
   */
  private Pattern parseChoicePattern(GrammarPattern grammar, Pattern pattern)
    throws IOException, SAXException, RelaxException
  {
    int token;

    do {
      if (! (pattern instanceof ChoicePattern)) {
        Pattern child = pattern;
        pattern = new ChoicePattern();
        pattern.addChild(child);
      }
      
      pattern.addChild(parseTerm(grammar));
    } while ((token = parseToken()) == '|');

    _peekToken = token;

    return pattern;
  }

  /**
   * Parses a term
   */
  private Pattern parseTerm(GrammarPattern grammar)
    throws IOException, SAXException, RelaxException
  {
    int token = parseToken();

    while (token == COMMENT) {
      token = parseToken();
    }

    Pattern pattern;
    switch (token) {
    case EMPTY:
      return new EmptyPattern();
      
    case TEXT:
      return new TextPattern();
      
    case STRING:
    case LITERAL:
      return new DataPattern("string");
      
    case TOKEN:
      return new DataPattern("token");
      
    case ELEMENT:
      pattern = parseElement(grammar);
      break;
      
    case ATTRIBUTE:
      pattern = parseAttribute(grammar);
      break;

    case '(':
      pattern = parsePattern(grammar);

      token = parseToken();
      if (token != ')')
        throw error(L.l("expected ')' at {0}", errorToken(token)));
      break;

    case IDENTIFIER:
      pattern = new RefPattern(_grammar, _lexeme);
      pattern.setFilename(_filename);
      pattern.setLine(_line);
      break;

    default:
      throw error(L.l("unknown token {0}", errorToken(token)));
    }

    token = parseToken();

    if (token == '*')
      pattern = new ZeroOrMorePattern(pattern);
    else if (token == '?') {
      ChoicePattern choice = new ChoicePattern();
      choice.addChild(new EmptyPattern());
      choice.addChild(pattern);
      return choice;
    }
    else if (token == '+') {
      GroupPattern group = new GroupPattern();
      group.addChild(pattern);
      group.addChild(new ZeroOrMorePattern(pattern));
      return group;
    }
    else {
      _peekToken = token;
    }

    return pattern;
  }

  /**
   * Parses an element.
   */
  private Pattern parseElement(GrammarPattern grammar)
    throws IOException, SAXException, RelaxException
  {
    String id = generateId();
    ElementPattern elt = new ElementPattern(id);
    grammar.setDefinition(id, elt);
    
    elt.addNameChild(parseNameClass(grammar, true));

    int token = parseToken();
    if (token == '{') {
      elt.addChild(parsePattern(grammar));

      token = parseToken();
      if (token != '}')
        throw error(L.l("expected '}' at {0}", errorToken(token)));
    }

    return elt;
  }

  /**
   * Parses an element.
   */
  private Pattern parseAttribute(GrammarPattern grammar)
    throws IOException, SAXException, RelaxException
  {
    AttributePattern elt = new AttributePattern();
    elt.addNameChild(parseNameClass(grammar, false));

    int token = parseToken();
    if (token == '{') {
      token = parseToken();

      if (token == '}')
        return elt;

      _peekToken = token;
      
      elt.addChild(parsePattern(grammar));

      token = parseToken();
      if (token != '}')
        throw error(L.l("expected '}' at {0}", errorToken(token)));
    }

    return elt;
  }

  /**
   * Parses a name class.
   */
  private NameClassPattern parseNameClass(GrammarPattern grammar,
                                          boolean isElement)
    throws IOException, SAXException, RelaxException
  {
    NameClassPattern left = parseName(grammar, isElement);
    ChoiceNamePattern choice = null;

    int ch;
    while ((ch = skipWhitespace()) == '|') {
      NameClassPattern right = parseName(grammar, isElement);

      if (choice == null) {
        choice = new ChoiceNamePattern();
        choice.addNameChild(left);
      }

      choice.addNameChild(right);
    }

    unread();

    if (choice != null)
      return choice;
    else
      return left;
  }

  /**
   * Parses a name class.
   */
  private NameClassPattern parseName(GrammarPattern grammar, boolean isElement)
    throws IOException, SAXException, RelaxException
  {
    int ch = skipWhitespace();
    if (ch == '(') {
      NameClassPattern name = parseNameClass(grammar, isElement);
      ch = skipWhitespace();
      if (ch != ')')
        throw error(L.l("expected ')' at '{0}'", String.valueOf((char) ch)));
      return name;
    }

    char []cbuf = _cb.getBuffer();
    int i = 0;
    
    while (ch > 0 && ch < 256 && NAME_CHAR[ch]) {
      cbuf[i++] = (char) ch;

      if (_offset < _length) {
        ch = _buffer[_offset++] & 0xff;
        if (ch == '\n')
          _line++;
      }
      else
        ch = read();
    }
    _cb.setLength(i);
    
    if (ch == '*')
      _cb.append('*');
    else
      unread();

    if (_cb.length() == 0)
      throw error(L.l("expected name at '{0}'", String.valueOf((char) ch)));

    NameClassPattern pattern;

    String lexeme = _cb.toString();

    int p = lexeme.lastIndexOf(':');
    String ns = _ns;
    String localName;
    
    if (p < 0) {
      localName = lexeme;

      if (! isElement)
        ns = null;
    }
    else {
      String prefix = lexeme.substring(0, p);
      localName = lexeme.substring(p + 1);
      ns = _nsMap.get(prefix);

      if (ns == null && localName.equals("*"))
        throw error(L.l("'{0}' does not match a defined namespace.", lexeme));
      
      if (ns == null) {// && isElement) {
        pattern = createNamePattern(lexeme, "");

        return pattern;
      }
    }

    if (lexeme.equals("*")) {
      AnyNamePattern namePattern = new AnyNamePattern();
      
      namePattern.setExcept(parseExcept(grammar, isElement));
      
      return namePattern;
    }
    else if (localName.equals("*")) {
      NsNamePattern namePattern = new NsNamePattern(lexeme, ns);
      
      namePattern.setExcept(parseExcept(grammar, isElement));
      
      return namePattern;
    }
    else if ("".equals(ns) || ns == null) {
      pattern = createNamePattern(localName, "");

      return pattern;
    }
    else {
      pattern = createNamePattern(lexeme, ns);

      return pattern;
    }
  }

  private NamePattern createNamePattern(String localName, String namespace)
  {
    return new NamePattern(new QName(localName, namespace));
  }

  /**
   * Parses a name class.
   */
  private NameClassPattern parseExcept(GrammarPattern grammar,
                                       boolean isElement)
    throws IOException, SAXException, RelaxException
  {
    int ch = skipWhitespace();

    if (ch != '-') {
      unread();
      return null;
    }

    return parseName(grammar, isElement);
  }

  /**
   * Parses a token.
   */
  private int parseToken()
    throws IOException, SAXException, RelaxException
  {
    int ch = _peekToken;

    if (ch >= 0) {
      _peekToken = -1;
      return ch;
    }

    _cb.clear();
      
    while (true)  {
      if (_offset < _length) {
        ch = _buffer[_offset++];
        if (ch == '\n')
          _line++;
      }
      else
        ch = read();

      switch (ch) {
      case ' ':
      case '\t':
      case '\n':
      case '\r':
        break;
      
      case '?':
      case '*':
      case '+':
      case ',':
      case '|':
      case '&':
      case '{':
      case '}':
      case '(':
      case ')':
      case '=':
        return ch;

      case '\"':
      case '\'':
        unread();
        _lexeme = parseLiteral();
        return LITERAL;

      case '#':
        do {
          ch = read();
          if (ch != '#')
            throw error(L.l("expected '#' at '{0}'", String.valueOf((char) ch)));
        
          if (_cb.length() > 0)
            _cb.append('\n');

          for (ch = read(); ch > 0 && ch != '\n' && ch != '\r'; ch = read())
            _cb.append((char) ch);

          if (ch == '\r') {
            ch = read();
            if (ch != '\n')
              unread();
          }

          ch = read();
        } while (ch == '#');

        unread();
        return COMMENT;

      case -1:
        _cb.append("end of file");
        return -1;

      default:
        if (XmlChar.isNameStart(ch)) {
          char []cbuf = _cb.getBuffer();
          int i = 0;

          while (ch > 0 && ch < 256 && NAME_CHAR[ch]) {
            cbuf[i++] = (char) ch;

            if (_offset < _length) {
              ch = _buffer[_offset++] & 0xff;
              if (ch == '\n')
                _line++;
            }
            else
              ch = read();
          }
          _cb.setLength(i);
          unread();

          int token = _tokenMap.get(_cb);

          if (token > 0) {
            _lexeme = null;
            return token;
          }
          else {
            _lexeme = _cb.toString().intern();
            return IDENTIFIER;
          }
        }
        else if (ch < 0) {
          _cb.append("end of file");
          return -1;
        }
        else {
          throw error(L.l("Unknown character '{0}'", String.valueOf((char) ch)));
        }
      }
    }
  }

  private String parseLiteral()
    throws IOException, SAXException, RelaxException
  {
    int end = skipWhitespace();

    if (end != '"' && end != '\'')
      throw error(L.l("expected '\"' at '{0}'", String.valueOf((char) end)));

    _cb.clear();
    int ch = read();
    for (; ch >= 0 && ch != end; ch = read()) {
      _cb.append((char) ch);
    }

    if (ch != end)
      throw error(L.l("expected '\"' at '{0}'", String.valueOf((char) ch)));

    return _cb.toString();
  }


  private String parseIdentifier()
    throws IOException, SAXException, RelaxException
  {
    int ch = skipWhitespace();

    if (! XmlChar.isNameChar(ch))
      throw error(L.l("expected identifier character at '{0}'", String.valueOf((char) ch)));

    _cb.clear();
    for (; XmlChar.isNameChar(ch); ch = read()) {
      _cb.append((char) ch);
    }

    return _cb.toString();
  }

  /**
   * Parses whitespace.
   */
  private int skipWhitespace()
    throws IOException, SAXException
  {
    int ch;
      
    for (ch = read(); XmlChar.isWhitespace(ch); ch = read()) {
    }

    return ch;
  }

  private String errorToken(int ch)
  {
    switch (ch) {
    case -1:
      return "end of file";
      
    case '?':
    case '*':
    case '+':
    case ',':
    case '|':
    case '&':
    case '{':
    case '}':
    case '(':
    case ')':
    case '=':
      return String.valueOf((char) ch);
      
    default:
      return _cb.toString();
    }
  }

  /**
   * Creates an error.
   */
  private SAXException error(String msg)
  {
    return new SAXException(_filename + ":" + _line + ": " + msg);
  }

  /**
   * Returns the current location string.
   */
  /*
  public String getLocation()
  {
    return _filename + ":" + _line;
  }
  */
  
  /**
   * Reads a character.
   */
  private int read()
    throws IOException
  {
    if (_length <= _offset) {
      fillBuffer();

      if (_length < 0)
        return -1;
    }
    
    int ch = _buffer[_offset++];
    
    if (ch == '\n')
      _line++;
    else if (ch == '\r') {
      _line++;

      if (_length <= _offset)
        fillBuffer();
      
      ch = _buffer[_offset++];
      
      if (ch != '\n') {
        unread();
        ch = '\n';
      }
    }

    return ch;
  }

  private void fillBuffer()
    throws IOException
  {
    _length = _is.read(_buffer, 0, _buffer.length);
    _offset = 0;
  }

  private void unread()
  {
    if (_offset > 0) {
      _offset--;
      int ch = _buffer[_offset];
      if (ch == '\n') {
        _line--;
      }
    }
  }

  static {
    _tokenMap.put(new CharBuffer("namespace"), NAMESPACE);
    _tokenMap.put(new CharBuffer("default"), DEFAULT);
    
    _tokenMap.put(new CharBuffer("start"), START);
    _tokenMap.put(new CharBuffer("div"), DIV);
    
    _tokenMap.put(new CharBuffer("element"), ELEMENT);
    _tokenMap.put(new CharBuffer("attribute"), ATTRIBUTE);
    
    _tokenMap.put(new CharBuffer("text"), TEXT);
    _tokenMap.put(new CharBuffer("string"), STRING);
    _tokenMap.put(new CharBuffer("token"), TOKEN);
    
    _tokenMap.put(new CharBuffer("empty"), EMPTY);
    
    _tokenMap.put(new CharBuffer("include"), INCLUDE);

    NAME_CHAR = new boolean[256];
    for (int i = 0; i < NAME_CHAR.length; i++) {
      if (XmlChar.isNameChar((char) i))
        NAME_CHAR[i] = true;
    }
  }
}
