/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits redistribution, modification and use
 * of this file in source and binary form ("the Software") under the
 * Caucho Developer Source License ("the License").  The following
 * conditions must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Redistributions of the Software in source or binary form must include
 *    an unmodified copy of the License, normally in a plain ASCII text
 *
 * 3. The names "Resin" or "Caucho" are trademarks of Caucho Technology and
 *    may not be used to endorse products derived from this software.
 *    "Resin" or "Caucho" may not appear in the names of products derived
 *    from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.
 *
 * @author Sam 
 */

package com.caucho.doc.javadoc;

import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.ReadStream;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parse a javadoc generated html index file.
 */
public class IndexParser {
  static protected final Logger log = Log.open(IndexParser.class);
  static final L10N L = new L10N(IndexParser.class);

  static private final int EOF = -1;
  static private final int EODL = -2;

  
  static public final int TYPE_PACKAGE = 0x01;
  static public final int TYPE_CLASS = 0x02;
  static public final int TYPE_INTERFACE = 0x04;
  static public final int TYPE_ENUM = 0x08;
  static public final int TYPE_ANNOTATION = 0x10;
  static public final int TYPE_EXCEPTION = 0x20;
  static public final int TYPE_ERROR = 0x40;
  static public final int TYPE_CONSTRUCTOR = 0x80;
  static public final int TYPE_METHOD = 0x100;
  static public final int TYPE_VARIABLE = 0x200;

  static public final int MODIFIER_STATIC = 0x1000;

  private ReadStream _rs = null;
  private Callback _callback;

  private int _currLine = 0;
  private int _lastDTLine = -1;

  public IndexParser(ReadStream rs, Callback callback)
  {
    _rs = rs;
    _callback = callback;
  }

  public void parse() 
    throws IOException
  {
    // find <DL>

    int ch = 0;;
    while (ch != EOF) {
      ch = readChar();
      if (ch == '<') {
        ch = readChar();
        if (ch == 'D' || ch == 'd') {
          ch = readChar();
          if (ch == 'L' || ch == 'l') {
            ch = readChar(); // eat '>'
            ch = parseDL();
          }
        }
      }
    }
  }

  public interface Callback {
    public void item(String path, String anchor, String name, String fullname, int typ, int modifier, String description);
  }

  /** 
   * A String describing the read source and the current line of parsing.
   */
  public String getLineInfo()
  {
    CharBuffer cb = CharBuffer.allocate();
    cb.append(_rs.getPath().toString());
    cb.append(':');
    cb.append(_currLine);
    return cb.close();
  }

  protected boolean readLine(CharBuffer cb)
    throws IOException
  {
    boolean n = _rs.readLine(cb);
    if (n) _currLine++;
    return n;
  }

  protected int readChar()
    throws IOException
  {
    int n = _rs.readChar();
    if (n == '\n') {
      _currLine++;
    } 
    else if (n == '\r') {
      if (_rs.readChar() != '\n') 
        _rs.unread();
      else {
        _currLine++;
        n = '\n';
      }
    }

    return n;
  }

  // return EOF or EODL
  private int parseDL() 
    throws IOException 
  {
    boolean ignore = true; // ignore the first, it's just junk until the first <DT>
    int ch = 0;

    while (ch != EOF && ch != EODL) {
      ch = readChar();

      _lastDTLine = _currLine;
      ch = parseDT(ignore);
      ignore = false;
    }
    return ch;
  }

  // return EOF or EODL or last char read
  // ignore means just ignore, used to get to first DT 
  private int parseDT(boolean ignore)
    throws IOException
  {
    int r = EOF;

    // fill dt until there is another <DT>
    // or there is a </DL>
    // once it's full, call readDT() 

    CharBuffer dt = ignore ? null : CharBuffer.allocate();
    CharBuffer cbb = CharBuffer.allocate();

    int ch;

    while ((ch = readChar()) != -1) {
      if (ch == '<') {
        cbb.append((char)ch);
        ch = readChar();
        if (ch == 'D' || ch == 'd') {
          cbb.append((char)ch);
          ch = readChar();
          if (ch == 'T' || ch == 't') {
            cbb.clear();
            if (!ignore) readDT(dt);
            break;
          }
        } else if (ch == '/') {
          cbb.append((char)ch);
          ch = readChar();
          if (ch == 'D' || ch == 'd') {
            cbb.append((char)ch);
            ch = readChar();
            if (ch == 'L' || ch == 'l') {
              if (!ignore) readDT(dt);
              ch =  EODL;
              break;
            }
          }
        }
        if (dt != null) dt.append(cbb);
        cbb.clear();
      }
      if (dt != null) dt.append((char)ch);
    }

    cbb.free();
    if (dt != null) dt.free();

    return ch;
  }

  private void readDT(CharBuffer cb)
    throws IOException 
  {
    String parseDescr = "";  // the step being performed

    CharBuffer t = CharBuffer.allocate();

    try {
      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("<DT> entry from line {0} is [[{1}]]",String.valueOf(_lastDTLine),cb.toString()));

      String path;
      String anchor = null;
      String fullname;
      String name;
      int typ;
      int modifier = 0;
      String description;

      int i = 0;
      parseDescr = "parsing href, looking for first \"";
      i = readToAndEat(cb,i,'\"',null);
      parseDescr = "parsing href, looking for next \"";
      i = readToAndEat(cb,i,'\"',t);
      while (t.startsWith("../"))
        t.delete(0,3);

      int ai = t.indexOf('#');
      if (ai > -1) {
        path = t.substring(0,ai);
        anchor = t.substring(ai + 1);
      } else {
        path = t.toString();
      }
      t.clear();
      if (log.isLoggable(Level.FINEST)) {
        log.finest(L.l("path: [{0}]",path));
        log.finest(L.l("anchor: [{0}]",anchor));
      }

      parseDescr = "using href to determine fullName";
      t.append(path);
      t.setLength(t.length() - 5); // drop .hmtl
      for (int ti = t.length() - 1; ti >= 0; ti--) {
        if (t.charAt(ti) == '/')
          t.setCharAt(ti,'.');
      }
      if (anchor != null) {
        t.append('.');
        t.append(anchor);
      }
      if (t.endsWith(".package-summary"))
          t.setLength(t.length() - 16);

      fullname = t.toString();
      t.clear();
      if (log.isLoggable(Level.FINEST)) {
        log.finest(L.l("fullname: [{0}]",fullname));
      }

      parseDescr = "parsing name, looking for opening <B>";
      i = readToAndEat(cb,i,"<B>",null);
      parseDescr = "parsing name, looking for closing </B>";
      i = readToAndEat(cb,i,"<",t);
      name = t.toString();
      t.clear();

      if (log.isLoggable(Level.FINEST)) {
        log.finest(L.l("name: [{0}]",name));
      }

      parseDescr = "parsing description, `-' marks beginning";
      i = readToAndEat(cb,i,'-',null);
      parseDescr = "parsing description, removing markup";
      clean(cb,i);

      parseDescr = "parsing description";

      //  < 1.4 has "package ", 1.5 has "Package "
      if (cb.startsWith("package ")) {
        typ = TYPE_PACKAGE;
      }
      else if (cb.startsWith("Package ")) {
        typ = TYPE_PACKAGE;
      }
      else if (cb.startsWith("class ")) {
        typ = TYPE_CLASS;
      }
      else if (cb.startsWith("Class ")) {
        typ = TYPE_CLASS;
      }
      else if (cb.startsWith("enum ")) {
        typ = TYPE_ENUM;
      }
      else if (cb.startsWith("Enum ")) {
        typ = TYPE_ENUM;
      }
      else if (cb.startsWith("annotation ")) {
        typ = TYPE_ANNOTATION;
      }
      else if (cb.startsWith("Annotation ")) {
        typ = TYPE_ANNOTATION;
      }
      else if (cb.startsWith("interface ")) {
        typ = TYPE_INTERFACE;
      }
      else if (cb.startsWith("Interface ")) {
        typ = TYPE_INTERFACE;
      }
      else if (cb.startsWith("exception ")) {
        typ = TYPE_EXCEPTION;
      }
      else if (cb.startsWith("Exception ")) {
        typ = TYPE_EXCEPTION;
      }
      else if (cb.startsWith("error ")) {
        typ = TYPE_ERROR;
      }
      else if (cb.startsWith("Error ")) {
        typ = TYPE_ERROR;
      }
      else if (cb.startsWith("Constructor")) {
        typ = TYPE_CONSTRUCTOR;
      }
      else if (cb.startsWith("Method")) {
        typ = TYPE_METHOD;
      }
      else if (cb.startsWith("Static method")) {
        typ = TYPE_METHOD;
        modifier = MODIFIER_STATIC;
      }
      else if (cb.startsWith("Variable")) {
        typ = TYPE_VARIABLE;
      }
      else if (cb.startsWith("Static variable")) {
        typ = TYPE_VARIABLE;
        modifier = MODIFIER_STATIC;
      }
      else {
        throw new IndexOutOfBoundsException(L.l("cannot determine type from `{0}'",cb.close()));
      }

      if (log.isLoggable(Level.FINEST)) {
        log.finest(L.l("type: [{0}]",typ));
      }

      parseDescr = "parsing description, remove first sentence";
      eatSentence(cb);
      description = cb.toString();

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("description: [{0}]",description));


      // do the callback
      _callback.item(path,anchor,name,fullname,typ,modifier,description);

    } catch (IndexOutOfBoundsException ex) {
      String msg = L.l("parsing error {0}: {1}, {2}",parseDescr, ex.getMessage(),getLineInfo());
      if (log.isLoggable(Level.FINE)) {
        log.fine(msg);
        log.fine(L.l("buffer was [[{0}]]",cb.toString()));
      }
      throw new IOExceptionWrapper(msg,ex);
    } finally {
      t.free();
    }
  }

  private int readToAndEat(CharBuffer in, int i, char after, CharBuffer out)
  {
    int l = in.length();
    char ch;
    while ( (ch = in.charAt(i)) != after) {
      if (out != null)
        out.append(ch);
      i++;
      if (i >= l)
        throw new IndexOutOfBoundsException(L.l("error looking for `{0}'",new Character(after)));
    }
    return ++i;
  }

  private int readToAndEat(CharBuffer in, int i, String after, CharBuffer out)
  {
    int al = after.length();

    while (!in.regionMatches(i,after,0,al) ) {
      if (out != null)
        out.append(in.charAt(i));
      i++;
      if (i >= in.length())
        throw new IndexOutOfBoundsException(L.l("error looking for `{0}'",after));
    }
    return i+al;
  }

  private void eatSentence(CharBuffer cb)
  {
    log.finest("eat sentence [[" + cb.toString() + "]]"); 
    int cbl = cb.length();
    int i = 0;
    if (cb.startsWith("package ")) {
      // second " " marks end of first sentence
      i = cb.indexOf(' ') + 1;
      if (i < cbl)
        i = cb.indexOf(' ',i) + 1;
      if (i <= 0)
        i = cbl;
    }
    else { 
      // ". " marks end of first sentence
      do {
        int d = cb.indexOf('.',i); 
        if (d > -1) {
          i = d + 1;
          if (i >= cbl || Character.isWhitespace(cb.charAt(i)))
            break;
          else {
            i++;
          }
        }
        else
          break;
      } while (i < cbl);
    }

    // strip whitespace from beginning
    while (i < cbl && (Character.isWhitespace(cb.charAt(i)) || cb.charAt(i) == '.')) {
      i++;
    }

    if (i >= cbl) {
      cb.clear();
    }
    else {
      cb.delete(0,i);
    }
  }

  /**
   * remove whitespace or '.' at begining and whitespace at end, fix first
   * sentence (add .), strip out equivalent of regexp match "<.*>", replace
   * &nbsp; with space, replace newlines with space, and merge multiple spaces
   * into a single space;
   */ 
  private void clean(CharBuffer cb, int i)
  {
    CharBuffer r = CharBuffer.allocate();

    for (;;) {
      i = eatWhitespace(cb,i);
      if (i < cb.length() && cb.charAt(i) == '.')
        i++;
      else
        break;
    }

    boolean lastws = false;  // reduce multiple ws to a single space
    while (i < cb.length()) {
      char ch = cb.charAt(i);
      if (ch == '\n')
        ch = ' ';
      if (ch == '\r')
        ch = ' ';

      if (Character.isWhitespace(ch)) {
        if (lastws) {
          i++;
          continue;
        }
      }

      if (ch == '<') {
        if (cb.charAt(i+1) == '/' && cb.charAt(i+2) == 'A')
          r.append(". ");

        // have to watch for stray < that are not really markup
        // only something that matches "</?[A-Za-z]>" counts as markup

        int cn = (i + 1 >= cb.length()) ? -1 : cb.charAt(i+1);
        if (cn == '/')
          cn = (i + 2 >= cb.length()) ? cn : cb.charAt(i+2);
        if ((cn >= 'a' && cn <= 'z') || (cn >= 'A' && cn <= 'Z')) {
          i = eatUntil(cb,++i,'>');
          if (cn == 'D' || cn == 'd')
            r.append(' ');
          i++;
          continue;
        }
      }
      if (cb.regionMatches(i,"&nbsp;",0,6) ) {
        r.append(' ');
        i += 5;
        lastws = true;
      }
      else {
        r.append(ch);
        lastws = Character.isWhitespace(ch);
      }
      i++;
    }

    int l = r.length() - 1;
    while (l > 0 && Character.isWhitespace(r.charAt(l))) {
      r.setLength(l--);
    }

    cb.clear();
    cb.append(r);
  }

  private int eatWhitespace(CharBuffer cb, int i)
  {
    while (i < cb.length() && Character.isWhitespace(cb.charAt(i))) {
      i++;
    } 
    return i;
  }

  private int eatUntil(CharBuffer cb, int i, char until)
  {
    int l = cb.length();
    while (cb.charAt(i) != until) {
      i++;
      if (i >= l)
        throw new IndexOutOfBoundsException(L.l("error looking for `{0}'",new Character(until)));
    } 
    return i;
  }

}
