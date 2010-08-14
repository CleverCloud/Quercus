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
import com.caucho.util.CharCursor;
import com.caucho.util.CharScanner;
import com.caucho.util.IntMap;
import com.caucho.util.StringCharCursor;

import org.w3c.dom.Element;

import java.io.IOException;

/**
 * Policy for parsing an HTML file.
 */
class HtmlPolicy extends Policy {
  static final int DOCUMENT = 1;
  static final int COMMENT = DOCUMENT + 1;
  static final int TEXT = COMMENT + 1;
  static final int JSP = TEXT + 1;
  static final int WHITESPACE = JSP + 1;

  static final int HTML = WHITESPACE + 1;
  static final int HEAD = HTML + 1;
  static final int TITLE = HEAD + 1;
  static final int ISINDEX = TITLE + 1;
  static final int BASE = ISINDEX + 1;
  static final int SCRIPT = BASE + 1;
  static final int STYLE = SCRIPT + 1;
  static final int META = STYLE + 1;
  static final int LINK = META + 1;
  static final int OBJECT = LINK + 1;

  static final int BODY = OBJECT + 1;

  static final int BASEFONT = BODY + 1;
  static final int BR = BASEFONT + 1;
  static final int AREA = BR + 1;
  static final int IMG = AREA + 1;
  static final int PARAM = IMG + 1;
  static final int HR = PARAM + 1;
  static final int INPUT = HR + 1;

  static final int P = INPUT + 1;
  static final int DT = P + 1;
  static final int DD = DT + 1;
  static final int LI = DD + 1;
  static final int OPTION = LI + 1;

  static final int TABLE = OPTION + 1;
  static final int CAPTION = TABLE + 1;
  static final int THEAD = CAPTION + 1;
  static final int TFOOT = THEAD + 1;
  static final int COL = TFOOT + 1;
  static final int COLGROUP = COL + 1;
  static final int TBODY = COLGROUP + 1;
  static final int TR = TBODY + 1;
  static final int TD = TR + 1;
  static final int TH = TD + 1;

  static final int FRAME = TH + 1;
  static final int FRAMESET = FRAME + 1;

  static final int BLOCK = FRAMESET + 1;
  static final int INLINE = BLOCK + 1;

  static IntMap names;
  static IntMap cbNames;
  
  static QName htmlName = new QName(null, "html", null);
  static QName headName = new QName(null, "head", null);
  static QName bodyName = new QName(null, "body", null);

  boolean toLower = true;
  boolean isJsp = false;
  boolean autoHtml = false;
  boolean hasBody = false;
  boolean autoHead = false;
  
  CharBuffer cb = new CharBuffer();

  public void init()
  {
    toLower = true;
    isJsp = false;
    autoHtml = false;
    hasBody = false;
    autoHead = false;
  }

  /**
   * When true, HTML parsing normalizes HTML tags to lower case.
   */
  public void setToLower(boolean toLower)
  {
    this.toLower = toLower;
  }

  /**
   * When true, treat text before HTML specially.
   */
  public void setJsp(boolean isJsp)
  {
    this.isJsp = isJsp;
  }

  /**
   * Return the normalized name.
   *
   * @param tag the raw name in the XML file.
   *
   * @return the normalized name.
   */
  QName getName(CharBuffer tag)
  {
    if (! toLower)
      return super.getName(tag);
    
    cb.clear();
    cb.append(tag);
    cb.toLowerCase();

    int name = cbNames.get(cb);

    if (name >= 0)
      return super.getName(cb);
    else
      return super.getName(tag);
  }

  QName getAttributeName(CharBuffer eltName, CharBuffer source)
  {
    if (! toLower)
      return super.getName(source);
    
    cb.clear();
    cb.append(eltName);
    cb.toLowerCase();
    int name = cbNames.get(cb);

    if (name < 0)
      return super.getName(source);
    else {
      source.toLowerCase();
      return super.getName(source);
    }
  }

  /**
   * Returns the appropriate action when opening a HTML tag.
   *
   * @param parser the XML parser
   * @param node the parent node
   * @param next the next child
   * @return the action code
   */
  int openAction(XmlParser parser, QName node, QName next)
    throws XmlParseException
  {
    String nodeName = node == null ? "#document" : node.getName();
    String nextName = next.getName();

    int nextCode = names.get(nextName);

    switch (names.get(nodeName)) {
    case DOCUMENT:
      switch (nextCode) {
      case HTML:
        return PUSH;

      case COMMENT:
        return PUSH;

      case HEAD: case TITLE: case ISINDEX: case BASE: case SCRIPT: 
      case STYLE: case META: case LINK: case OBJECT:
        opt = htmlName;
        return PUSH_OPT;

      case WHITESPACE:
        return IGNORE;

      case JSP:
        return PUSH;

      default:
        if (autoHtml)
          return PUSH;
        
        autoHtml = true;
        opt = htmlName;
        return PUSH_OPT;
      }

    case HTML:
      switch (nextCode) {
      case HTML:
        return ERROR;

      case HEAD:
      case COMMENT:
      case FRAMESET:
        return PUSH;
        
      case BODY:
        hasBody = true;
        return PUSH;

      case TITLE: case ISINDEX: case BASE: case SCRIPT: 
      case STYLE: case META: case LINK: case OBJECT:
        opt = headName;
        autoHead = true;
        return PUSH_OPT;

      case WHITESPACE:
        return PUSH;

      case JSP:
        return PUSH;

      default:
        if (hasBody)
          return PUSH;
        
        hasBody = true;
        opt = bodyName;
        return PUSH_OPT;
      }

    case HEAD:
      switch (nextCode) {
      case META:
        // checkMetaEncoding((Element) next);
        return PUSH_EMPTY;

      case LINK: case ISINDEX: case BASE: 
        return PUSH_EMPTY;
        
      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;
        
      case TITLE:
      case OBJECT:
        return PUSH;

      case WHITESPACE:
        return PUSH;
        
      case JSP:
      case TEXT:
        if (autoHead)
          return POP;
        else
          return PUSH;

      default:
        return POP;
      }

    case LI:
      switch (nextCode) {
      case LI:
        return POP;

      case BASEFONT: case BR: case AREA: case LINK: case IMG: case PARAM: 
      case HR: case INPUT: case COL: case FRAME: case ISINDEX: 
      case BASE: case META:
        return PUSH_EMPTY;

      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;

      default:
        return PUSH;
      }

    case OPTION:
      switch (nextCode) {
      case WHITESPACE:
      case TEXT:
        return PUSH;

      default:
        return POP;
      }

    case DD:
      switch (nextCode) {
      case DD: case DT:
        return POP;

      case BASEFONT: case BR: case AREA: case LINK: case IMG: case PARAM: 
      case HR: case INPUT: case COL: case FRAME: case ISINDEX: 
      case BASE: case META:
        return PUSH_EMPTY;

      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;

      default:
        return PUSH;
      }

    case THEAD: case TFOOT: case COLGROUP:
      switch (nextCode) {
      case THEAD: case TFOOT: case TBODY: case COLGROUP: case COL:
        return POP;

      case BASEFONT: case BR: case AREA: case LINK: case IMG: case PARAM: 
      case HR: case INPUT: case FRAME: case ISINDEX: 
      case BASE: case META:
        return PUSH_EMPTY;

      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;

      default:
        return PUSH;
      }

    case TR:
      switch (nextCode) {
      case THEAD: case TFOOT: case TBODY: case COLGROUP: case COL: case TR:
        return POP;

      case BASEFONT: case BR: case AREA: case LINK: case IMG: case PARAM: 
      case HR: case INPUT: case FRAME: case ISINDEX: 
      case BASE: case META:
        return PUSH_EMPTY;

      case TD: case TH:
        return PUSH;

      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;

      default:
        return PUSH;
      }

    case TD: case TH:
      switch (nextCode) {
      case THEAD: case TFOOT: case TBODY: case COLGROUP: case COL: case TR:
      case TD: case TH:
        return POP;

      case BASEFONT: case BR: case AREA: case LINK: case IMG: case PARAM: 
      case HR: case INPUT: case FRAME: case ISINDEX: 
      case BASE: case META:
        return PUSH_EMPTY;

      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;

      default:
        return PUSH;
      }

    case P: case DT:
      switch (nextCode) {
      case BLOCK: case P: case TABLE: case CAPTION: case THEAD:
      case TFOOT: case COLGROUP: case TBODY: case TR: case TD: 
      case TH: case DT: case LI:
        return POP;

      case BASEFONT: case BR: case AREA: case LINK: case IMG: case PARAM: 
      case HR: case INPUT: case COL: case FRAME: case ISINDEX: 
      case BASE: case META:
        return PUSH_EMPTY;

      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;

      default:
        return PUSH;
      }

    case TABLE:
      switch (nextCode) {
      case CAPTION: case THEAD: case TFOOT: case COL: case COLGROUP:
      case TBODY: case TR:
        return PUSH;

      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;

      default:
        /*
        opt = "tr";
        return PUSH_OPT;
        */
        return PUSH;
      }

    default:
      switch (nextCode) {
      case BASEFONT: case BR: case AREA: case LINK: case IMG: case PARAM: 
      case HR: case INPUT: case COL: case FRAME: case ISINDEX: 
      case BASE: case META:
        return PUSH_EMPTY;

      case SCRIPT: case STYLE:
        return PUSH_VERBATIM;

      default:
        return PUSH;
      }
    }
  }

  private static CharScanner charsetScanner = new CharScanner(" \t=;");

  private void checkMetaEncoding(Element elt)
  {
    String http = elt.getAttribute("http-equiv");
    String content = elt.getAttribute("content");
    if (http.equals("") || content.equals("") ||
        ! http.equalsIgnoreCase("content-type"))
      return;

    CharCursor cursor = new StringCharCursor(content);
    charsetScanner.scan(cursor);
    charsetScanner.skip(cursor);
    CharBuffer buf = CharBuffer.allocate();
    while (cursor.current() != cursor.DONE) {
      buf.clear();
      charsetScanner.scan(cursor, buf);
      if (buf.toString().equalsIgnoreCase("charset")) {
        charsetScanner.skip(cursor);
        buf.clear();
        charsetScanner.scan(cursor, buf);
        if (buf.length() > 0) {
          try {
            is.setEncoding(buf.close());
          } catch (IOException e) {
          }
          return;
        }
      }
    }
  }

  int elementCloseAction(XmlParser parser, QName node, String tagEnd)
    throws XmlParseException
  {
    String nodeName = node.getName();
    if (nodeName.equals(tagEnd))
      return POP;

    if (nodeName == "#document" && tagEnd.equals("")) {
      /*
      Document doc = (Document) node;

      // If JSP, move any text into the body element
      if (isJsp && doc.getDocumentElement() == null &&
          node.getFirstChild() instanceof Text) {
        Element html = doc.createElement("html");
        doc.appendChild(html);
        Element body = doc.createElement("body");
        html.appendChild(body);
        Node child;
        while ((child = doc.getFirstChild()) instanceof Text ||
        child instanceof Comment) {
          body.appendChild(child);
        }
      }
      */
      return POP;
    }
    switch (names.get(tagEnd)) {
    case BASEFONT: case BR: case AREA: case LINK: case IMG: case PARAM: 
    case HR: case INPUT: case COL: case FRAME: case ISINDEX: 
    case BASE: case META:
      String errorTagEnd;
      if (tagEnd.equals(""))
        errorTagEnd = L.l("end of file");
      else
        errorTagEnd = "`<" + tagEnd + ">'";

      throw parser.error(L.l("{0} expects to be empty",
                             errorTagEnd));
    }

    switch (names.get(nodeName)) {
    case BODY: case P:
    case DT: case DD: case LI: case OPTION:
    case THEAD: case TFOOT: case TBODY: case COLGROUP: 
    case TR: case TH: case TD:
      return POP_AND_LOOP;

    case HTML:
    case HEAD:
      // If JSP and missing a body, move any text into the body element
      /*
      if (isJsp && node.getLastChild() instanceof Text) {
        Node child;

        for (child = node.getLastChild();
             child != null;
             child = child.getPreviousSibling()) {
          if (child.getNodeName().equals("body"))
            return POP_AND_LOOP;
        }

        Document doc = node.getOwnerDocument();
        Element body = doc.createElement("body");
        
        while ((child = node.getLastChild()) instanceof Text ||
               child instanceof Comment) {
          body.insertBefore(child, body.getFirstChild());
        }
        
        doc.getDocumentElement().appendChild(body);
      }
      */
      return POP_AND_LOOP;

    default:

      if (forgiving) {
        /*
        Node parent = node;
        for (; parent != null; parent = parent.getParentNode()) {
          if (parent.getNodeName().equals(tagEnd))
            return POP_AND_LOOP;
        }
        return IGNORE;
        */
        return POP_AND_LOOP;
      }
      
      String errorTagEnd;
      if (tagEnd.equals(""))
        errorTagEnd = L.l("end of file");
      else
        errorTagEnd = "`</" + tagEnd + ">'";

      String expect;
      if (nodeName.equals("#document")) {
        throw parser.error(L.l("expected {0} at {1}",
                               L.l("end of document"), errorTagEnd));
      }
      else
        expect = "`</" + nodeName + ">'";

      throw parser.error(L.l("expected {0} at {1} (open at {2})",
                             expect, errorTagEnd,
                             "" + parser.getNodeLine()));
    }
  }

  private static void addName(String name, int code)
  {
    names.put(name, code);
    cbNames.put(new CharBuffer(name), code);

    String upper = name.toUpperCase();
    names.put(upper, code);
    cbNames.put(new CharBuffer(upper), code);
  }

  static {
    names = new IntMap();
    cbNames = new IntMap();
    
    addName("#document", DOCUMENT);
    addName("#comment", COMMENT);
    addName("#text", TEXT);
    addName("#jsp", JSP);
    addName("#whitespace", WHITESPACE);
    addName("html", HTML);

    addName("head", HEAD);
    addName("title", TITLE);
    addName("isindex", ISINDEX);
    addName("base", BASE);
    addName("script", SCRIPT);
    addName("style", STYLE);
    addName("meta", META);
    addName("link", LINK);
    addName("object", OBJECT);

    addName("body", BODY);

    addName("basefont", BASEFONT);
    addName("br", BR);
    addName("area", AREA);
    addName("link", LINK);
    addName("img", IMG);
    addName("param", PARAM);
    addName("hr", HR);
    addName("input", INPUT);
    addName("frame", FRAME);

    addName("p", P);
    addName("dt", DT);
    addName("dd", DD);
    addName("li", LI);
    addName("option", OPTION);

    addName("table", TABLE);
    addName("caption", CAPTION);
    addName("thead", THEAD);
    addName("tfoot", TFOOT);
    addName("col", COL);
    addName("colgroup", COLGROUP);
    addName("tbody", TBODY);
    addName("tr", TR);
    addName("th", TH);
    addName("td", TD);

    addName("h1", BLOCK);
    addName("h2", BLOCK);
    addName("h3", BLOCK);
    addName("h4", BLOCK);
    addName("h5", BLOCK);
    addName("h6", BLOCK);
    addName("ul", BLOCK);
    addName("ol", BLOCK);
    addName("dir", BLOCK);
    addName("menu", BLOCK);
    addName("pre", BLOCK);
    addName("dl", BLOCK);
    addName("div", BLOCK);
    addName("center", BLOCK);
    addName("noscript", BLOCK);
    addName("noframes", BLOCK);
    addName("blockquote", BLOCK);
    addName("form", BLOCK);
    addName("fieldset", BLOCK);
    addName("address", BLOCK);

    addName("tt", INLINE);
    addName("i", INLINE);
    addName("b", INLINE);
    addName("u", INLINE);
    addName("s", INLINE);
    addName("strike", INLINE);
    addName("big", INLINE);
    addName("small", INLINE);

    addName("em", INLINE);
    addName("strong", INLINE);
    addName("dfn", INLINE);
    addName("code", INLINE);
    addName("samp", INLINE);
    addName("kbd", INLINE);
    addName("var", INLINE);
    addName("cite", INLINE);
    addName("abbr", INLINE);
    addName("acronym", INLINE);
    addName("font", INLINE);
    addName("iframe", INLINE);
    addName("applet", INLINE);
    addName("ins", INLINE);
    addName("del", INLINE);

    addName("a", INLINE);
    addName("map", INLINE);
    addName("q", INLINE);
    addName("sub", INLINE);
    addName("sup", INLINE);
    addName("span", INLINE);
    addName("bdo", INLINE);

    addName("select", INLINE);
    addName("textarea", INLINE);
    addName("label", INLINE);
    addName("optgroup", INLINE);
    addName("button", INLINE);
    addName("legend", INLINE);
    addName("frameset", FRAMESET);

    // CDATA -- STYLE, SCRIPT
  }
}
