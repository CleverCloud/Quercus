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
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.*;

public class RegexpState {
  private static final Logger log
    = Logger.getLogger(RegexpState.class.getName());

  private static final L10N L = new L10N(Regexp.class);

  public static final int FAIL = -1;
  public static final int SUCCESS = 0;

  private Regexp _regexp;

  private StringValue _subject;
  private int _subjectLength;

  boolean _isGlobal;

  int _first;
  int _start;

  // optim stuff
  CharBuffer _prefix; // initial string
  int _minLength; // minimum length possible for this regexp

  boolean _isUnicode;
  boolean _isPHP5String;

  boolean _isUTF8;
  boolean _isEval;

  int _groupLength;
  int []_groupBegin;
  int []_groupEnd;

  int []_loopCount;
  int []_loopOffset;

  private RegexpState()
  {
    int size = 32;

    _groupBegin = new int[size];
    _groupEnd = new int[size];

    _loopCount = new int[size];
    _loopOffset = new int[size];
  }

  private void init(Regexp regexp)
  {
    _regexp = regexp;

    int nGroup = regexp._nGroup;

    if (_groupBegin.length < nGroup) {
      _groupBegin = new int[nGroup];
      _groupEnd = new int[nGroup];
    }

    int nLoop = regexp._nLoop;

    if (_loopCount.length < nLoop) {
      _loopCount = new int[nLoop];
      _loopOffset = new int[nLoop];
    }

    _subject = null;
    _subjectLength = 0;
    _isGlobal = false;

    _first = 0;
    _start = 0;

    _prefix = null; // initial string
    _minLength = 0; // minimum length possible for this regexp

    _isUnicode = false;
    _isPHP5String = false;

    _isUTF8 = false;
    _isEval = false;
  }

  public static RegexpState create(Env env, Regexp regexp)
  {
    RegexpState state = env.allocateRegexpState();

    if (state == null)
      state = new RegexpState();

    state.init(regexp);

    return state;
  }

  public static RegexpState create(Env env, Regexp regexp, StringValue subject)
  {
    RegexpState state = create(env, regexp);

    state.setSubject(env, subject);

    return state;
  }

  public static void free(Env env, RegexpState state)
  {
    env.freeRegexpState(state);
  }

  public int getSubjectLength()
  {
    return _subjectLength;
  }

  public boolean setSubject(Env env, StringValue subject)
  {
    _subject = _regexp.convertSubject(env, subject);
    _subjectLength = _subject != null ? _subject.length() : 0;

    return _subject != null;
  }

  public void setFirst(int first)
  {
    _first = first;
    _start = first;
  }

  public boolean find()
  {
    try {
      if (log.isLoggable(Level.FINEST))
        log.finest(this + " find()");

      int minLength = _regexp._minLength;
      boolean []firstSet = _regexp._firstSet;

      if (_subject == null)
        return false;

      StringValue subject = _subject;
      int length = _subjectLength;

      /* php/4e85 XXX: optim doesn't work for greedy loops
      if (_regexp._isAnchorBegin) {
        if (_first + minLength <= length)
          length = _first + minLength;
      }
      */

      for (; _first + minLength <= length; _first++) {
        if (firstSet != null && _first < length) {
          char firstChar = subject.charAt(_first);

          if (firstChar < 256 && ! firstSet[firstChar])
            continue;
        }

        clearGroup();
        int offset = _regexp._prog.match(subject, length, _first, this);

        if (offset >= 0) {
          _groupBegin[0] = _first;
          _groupEnd[0] = offset;

          if (_first < offset)
            _first = offset;
          else
            _first += 1;

          return true;
        }
      }

      _first = length + 1;

      return false;
    } catch (StackOverflowError e) {
      log.warning(L.l("regexp '{0}' produces a StackOverflowError for\n{1}",
                      _regexp, _subject));

      throw new QuercusRuntimeException(
          L.l("regexp '{0}' produces a StackOverflowError", _regexp), e);
    }
  }

  public boolean find(Env env, StringValue subject)
  {
    try {
      subject = _regexp.convertSubject(env, subject);

      if (subject == null)
        throw new QuercusException(L.l("error converting subject to utf8"));

      _subject = subject;
      _subjectLength = subject != null ? subject.length() : 0;
      _first = 0;

      return find();
    } catch (StackOverflowError e) {
      log.warning(L.l("regexp '{0}' produces a StackOverflowError for\n{1}",
                      _regexp, subject));

      throw new QuercusRuntimeException(
          L.l("regexp '{0}' produces a StackOverflowError", _regexp), e);
    }
  }

  public int find(Env env, StringValue subject, int first)
  {
    try {
      if (log.isLoggable(Level.FINEST))
        log.finest(this + " find(" + subject + ")");

      subject = _regexp.convertSubject(env, subject);

      if (subject == null)
        throw new QuercusException(L.l("error converting subject to utf8"));

      _subject = subject;
      _subjectLength = subject != null ? subject.length() : 0;

      _first = first;
      clearGroup();

      return _regexp._prog.match(_subject, _subjectLength, first, this);
    } catch (StackOverflowError e) {
      log.warning(L.l("regexp '{0}' produces a StackOverflowError for\n{1}",
                      _regexp, subject));

      throw new QuercusRuntimeException(
          L.l("regexp '{0}' produces a StackOverflowError", _regexp), e);
    }
  }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(Env env, StringValue subject, int start)
  {
    try {
      if (log.isLoggable(Level.FINEST))
        log.finest(this + " exec(" + subject + ")");

      subject = _regexp.convertSubject(env, subject);

      if (subject == null) {
        if (log.isLoggable(Level.FINE))
          log.fine(L.l("error converting subject to utf8"));

        return -1;
      }

      clearGroup();

      _start = start;
      _first = start;

      _subject = subject;
      int subjectLength = subject != null ? subject.length() : 0;
      _subjectLength = subjectLength;

      int minLength = _regexp._minLength;
      boolean []firstSet = _regexp._firstSet;
      int end = subjectLength - minLength;
      RegexpNode prog = _regexp._prog;

      if (_regexp._isAnchorBegin)
        end = start;

      for (; start <= end; start++) {
        if (firstSet != null && (start < end || minLength > 0)) {
          char firstChar = subject.charAt(start);

          if (firstChar < 256 && ! firstSet[firstChar])
            continue;
        }

        int value = prog.match(subject, subjectLength, start, this);

        if (value >= 0) {
          _groupBegin[0] = start;
          _groupEnd[0] = value;

          return start;
        }
      }

      return -1;
    } catch (StackOverflowError e) {
      log.warning(L.l("regexp '{0}' produces a StackOverflowError for\n{1}",
                      _regexp, subject));

      throw new QuercusRuntimeException(
          L.l("regexp '{0}' produces a StackOverflowError", _regexp), e);
    }
  }

  private void clearGroup()
  {
    _groupLength = 0;

    for (int i = _groupBegin.length - 1; i > 0; i--) {
      _groupBegin[i] = -1;
      _groupEnd[i] = -1;
    }
  }

  public int getBegin(int i)
  {
    return _groupBegin[i];
  }

  public int getEnd(int i)
  {
    return _groupEnd[i];
  }

  public void setBegin(int i, int v)
  {
    _groupBegin[i] = v;
  }

  public void setEnd(int i, int v)
  {
    _groupEnd[i] = v;
  }

  public int getLength()
  {
    return _groupLength;
  }

  public void setLength(int length)
  {
    _groupLength = length;
  }

  public int length()
  {
    return _groupLength;
  }

  public int start()
  {
    return getBegin(0);
  }

  public int start(int i)
  {
    return getBegin(i);
  }

  public int end()
  {
    return getEnd(0);
  }

  public int end(int i)
  {
    return getEnd(i);
  }

  public int groupCount()
  {
    return _regexp._nGroup;
  }

  public boolean isMatchedGroup(int i)
  {
    return i <= _groupLength;
  }

  public StringValue group(Env env)
  {
    return group(env, 0);
  }

  public StringValue group(Env env, int i)
  {
    int begin = getBegin(i);
    int end = getEnd(i);

    StringValue s = _subject.substring(begin, end);

    return _regexp.convertResult(env, s);
  }

  public StringValue getGroupName(int i)
  {
    StringValue []groupNames = _regexp._groupNames;

    if (groupNames == null || groupNames.length <= i)
      return null;
    else
      return groupNames[i];
  }

  public StringValue substring(Env env, int start)
  {
    StringValue result = _subject.substring(start);

    return _regexp.convertResult(env, result);
  }

  public StringValue substring(Env env, int start, int end)
  {
    StringValue result = _subject.substring(start, end);

    return _regexp.convertResult(env, result);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _regexp + "]";
  }
}
