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

package com.caucho.quercus.lib.regexp;

import java.util.*;
import java.util.logging.*;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeBuilderValue;
import com.caucho.quercus.lib.i18n.Utf8Encoder;
import com.caucho.util.*;

public class Regexp {
  private static final Logger log
    = Logger.getLogger(Regexp.class.getName());
  
  private static final L10N L = new L10N(Regexp.class);
  
  public static final int FAIL = -1;
  public static final int SUCCESS = 0;

  final StringValue _rawRegexp;
  StringValue _pattern;
  int _flags;
  
  RegexpNode _prog;
  boolean _ignoreCase;
  boolean _isGlobal;

  int _nLoop;
  int _nGroup;
  
  // optim stuff
  
  CharBuffer _prefix; // initial string
  int _minLength; // minimum length possible for this regexp
  int _firstChar;
  boolean []_firstSet;
  boolean _isAnchorBegin;

  StringValue []_groupNames;
  
  boolean _isUnicode;
  boolean _isPHP5String;
  
  boolean _isUtf8;
  boolean _isEval;
  
  public Regexp(StringValue rawRegexp)
    throws IllegalRegexpException
  {
    _rawRegexp = rawRegexp;
    _pattern = rawRegexp;

    init();
    
    Regcomp comp = new Regcomp(_flags);
    _prog = comp.parse(new PeekString(_pattern));

    compile(_prog, comp);
  }
  
  protected void init()
  {
    StringValue rawRegexp = _rawRegexp;
    
    if (rawRegexp.length() < 2) {
      throw new IllegalStateException(L.l(
          "Can't find delimiters in regexp '{0}'.",
          rawRegexp));
    }

    int head = 0;
    
    char delim = '/';

    for (;
     head < rawRegexp.length()
       && Character.isWhitespace((delim = rawRegexp.charAt(head)));
     head++) {
    }

    if (delim == '{')
      delim = '}';
    else if (delim == '[')
      delim = ']';
    else if (delim == '(')
      delim = ')';
    else if (delim == '<')
      delim = '>';
    else if (delim == '\\' || Character.isLetterOrDigit(delim)) {
      throw new QuercusException(L.l(
          "Delimiter {0} in regexp '{1}' must "
              + "not be backslash or alphanumeric.",
          String.valueOf(delim),
          rawRegexp));
    }

    int tail = rawRegexp.lastIndexOf(delim);

    if (tail <= 0)
      throw new QuercusException(L.l(
          "Can't find second {0} in regexp '{1}'.",
          String.valueOf(delim),
          rawRegexp));

    StringValue sflags = rawRegexp.substring(tail + 1);
    StringValue pattern = rawRegexp.substring(head + 1, tail); 
    
    _pattern = pattern;
    
    int flags = 0;
    
    for (int i = 0; sflags != null && i < sflags.length(); i++) {
      switch (sflags.charAt(i)) {
      case 'm': flags |= Regcomp.MULTILINE; break;
      case 's': flags |= Regcomp.SINGLE_LINE; break;
      case 'i': flags |= Regcomp.IGNORE_CASE; break;
      case 'x': flags |= Regcomp.IGNORE_WS; break;
      case 'g': flags |= Regcomp.GLOBAL; break;
        
      case 'A': flags |= Regcomp.ANCHORED; break;
      case 'D': flags |= Regcomp.END_ONLY; break;
      case 'U': flags |= Regcomp.UNGREEDY; break;
      case 'X': flags |= Regcomp.STRICT; break;
      case 'S': /* speedup */; break;
        
      case 'u': flags |= Regcomp.UTF8; break;
      case 'e': _isEval = true; break;

      default:
        throw new QuercusException(L.l("'{0}' is an unknown regexp flag in {1}",
                                       (char) sflags.charAt(i), rawRegexp));
      }
    }
    
    _flags = flags;

    // XXX: what if unicode.semantics='true'?
    
    if ((flags & Regcomp.UTF8) != 0) {
      _pattern = fromUtf8(pattern);
      
      if (pattern == null)
        throw new QuercusException(
            L.l("Regexp: error converting subject to utf8"));
    }
  }
  
  public StringValue getRawRegexp()
  {
    return _rawRegexp;
  }
  
  public StringValue getPattern()
  {
    return _pattern;
  }
  
  public boolean isUTF8()
  {
    return _isUtf8;
  }
  
  public boolean isEval()
  {
    return _isEval;
  }

  public StringValue convertSubject(Env env, StringValue subject)
  {
    if (isUTF8())
      return fromUtf8(subject);
    else
      return subject;
  }

  public StringValue convertResult(Env env, StringValue result)
  {
    if (isUTF8())
      return toUtf8(env, result);
    else
      return result;
  }

  private void compile(RegexpNode prog, Regcomp comp)
  {
    _ignoreCase = (comp._flags & Regcomp.IGNORE_CASE) != 0;
    _isGlobal = (comp._flags & Regcomp.GLOBAL) != 0;
    _isAnchorBegin = (comp._flags & Regcomp.ANCHORED) != 0;
    _isUtf8 = (comp._flags & Regcomp.UTF8) != 0;

    if (prog.isAnchorBegin())
      _isAnchorBegin = true;

    /*
    if (_ignoreCase)
      RegOptim.ignoreCase(prog);

    if (! _ignoreCase)
      RegOptim.eliminateBacktrack(prog, null);
    */

    _minLength = prog.minLength();
    _firstChar = prog.firstChar();
    _firstSet = prog.firstSet(new boolean[256]);
    _prefix = new CharBuffer(prog.prefix());

    //this._prog = RegOptim.linkLoops(prog);

    _nGroup = comp._maxGroup;
    _nLoop = comp._nLoop;
    
    _groupNames = new StringValue[_nGroup + 1];
    for (Map.Entry<Integer,StringValue> entry
           : comp._groupNameMap.entrySet()) {
      StringValue groupName = entry.getValue();

      if (_isUnicode) {
      }
      else if (isUTF8())
        groupName.toBinaryValue("UTF-8");

      _groupNames[entry.getKey().intValue()] = groupName;
    }
  }

  public StringValue getGroupName(int i)
  {
    return _groupNames[i];
  }
  
  public boolean isGlobal() { return _isGlobal; }
  public boolean ignoreCase() { return _ignoreCase; }

  static StringValue fromUtf8(StringValue source)
  {
    StringValue target = new UnicodeBuilderValue();
    int len = source.length();

    for (int i = 0; i < len; i++) {
      char ch = source.charAt(i);

      if (ch < 0x80) {
        target.append(ch);
      }
      else if ((ch & 0xe0) == 0xc0) {
        if (len <= i + 1) {
          log.fine(L.l("Regexp: bad UTF-8 sequence, saw EOF"));
          return null;
        }
        
        char ch2 = source.charAt(++i);

        target.append((char) (((ch & 0x1f) << 6)
                              + (ch2 & 0x3f)));
      }
      else if ((ch & 0xf0) == 0xe0) {
        if (len <= i + 2) {
          log.fine(L.l("Regexp: bad UTF-8 sequence, saw EOF"));
          return null;
        }
        
        char ch2 = source.charAt(++i);
        char ch3 = source.charAt(++i);
        
        target.append((char) (((ch & 0x0f) << 12)
                              + ((ch2 & 0x3f) << 6)
                              + (ch3 & 0x3f)));
      }
      else {
        if (i + 3 >= len) {
          log.fine(L.l("Regexp: bad UTF-8 sequence, saw EOF"));
          return null;
        }
        
        char ch2 = source.charAt(++i);
        char ch3 = source.charAt(++i);
        char ch4 = source.charAt(++i);
        
        int codePoint = ((ch & 0x07) << 18)
                         + ((ch2 & 0x3F) << 12)
                         + ((ch3 & 0x3F) << 6)
                         + (ch4 & 0x3F);
        
        int high = ((codePoint - 0x10000) >> 10) + 0xD800;
        int low = (codePoint & 0x3FF) + 0xDC00;
        
        target.append((char) high);
        target.append((char) low);
      }
    }

    return target;
  }

  static StringValue toUtf8(Env env, StringValue source)
  {
    Utf8Encoder encoder = new Utf8Encoder();
    
    return encoder.encode(env, source);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _pattern + "]";
  }
}
