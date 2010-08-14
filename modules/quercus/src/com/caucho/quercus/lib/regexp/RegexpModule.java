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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.annotation.Hide;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.UsesSymbolTable;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.i18n.MbstringModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegexpModule
  extends AbstractQuercusModule
{
  private static final Logger log =
    Logger.getLogger(RegexpModule.class.getName());

  private static final L10N L = new L10N(RegexpModule.class);

  public static final int PREG_REPLACE_EVAL = 0x01;
  public static final int PCRE_UTF8 = 0x02;

  public static final int PREG_PATTERN_ORDER = 0x01;
  public static final int PREG_SET_ORDER = 0x02;
  public static final int PREG_OFFSET_CAPTURE = 0x100;

  public static final int PREG_SPLIT_NO_EMPTY = 0x01;
  public static final int PREG_SPLIT_DELIM_CAPTURE = 0x02;
  public static final int PREG_SPLIT_OFFSET_CAPTURE = 0x04;

  public static final int PREG_GREP_INVERT = 1;

  public static final int PREG_NO_ERROR = 0;
  public static final int PREG_INTERNAL_ERROR = 1;
  public static final int PREG_BACKTRACK_LIMIT_ERROR = 2;
  public static final int PREG_RECURSION_LIMIT_ERROR = 3;
  public static final int PREG_BAD_UTF8_ERROR = 4;
  public static final int PREG_BAD_UTF8_OFFSET_ERROR = 5;
  public static final String PCRE_VERSION = "7.0 18-Dec-2006";

  // #2526, possible JIT/OS problem with max comparison
  private static final long LONG_MAX = Long.MAX_VALUE - 1;

  public static final boolean [] PREG_QUOTE = new boolean[256];

  private static LruCache<StringValue, RegexpCacheItem> _regexpCache
    = new LruCache<StringValue, RegexpCacheItem>(1024);

  private static LruCache<StringValue, Ereg> _eregCache
    = new LruCache<StringValue, Ereg>(1024);

  private static LruCache<StringValue, Eregi> _eregiCache
    = new LruCache<StringValue, Eregi>(1024);

  private static LruCache<UnicodeEregKey, UnicodeEreg> _unicodeEregCache
    = new LruCache<UnicodeEregKey, UnicodeEreg>(1024);

  private static LruCache<UnicodeEregKey, UnicodeEregi> _unicodeEregiCache
    = new LruCache<UnicodeEregKey, UnicodeEregi>(1024);

  private static LruCache<StringValue, ArrayList<Replacement>> _replacementCache
    = new LruCache<StringValue, ArrayList<Replacement>>(1024);

  @Override
  public String []getLoadedExtensions()
  {
    return new String[] { "ereg", "pcre" };
  }

  @Hide
  public static int getRegexpCacheSize()
  {
    return _regexpCache.getCapacity();
  }

  @Hide
  public static void setRegexpCacheSize(int size)
  {
    if (size < 0 || size == _regexpCache.getCapacity())
      return;

    _regexpCache = new LruCache<StringValue, RegexpCacheItem>(size);

    _eregCache = new LruCache<StringValue, Ereg>(size);

    _eregiCache = new LruCache<StringValue, Eregi>(size);

    _unicodeEregCache = new LruCache<UnicodeEregKey, UnicodeEreg>(size);

    _unicodeEregiCache = new LruCache<UnicodeEregKey, UnicodeEregi>(size);

    _replacementCache
      = new LruCache<StringValue, ArrayList<Replacement>>(size);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value ereg(Env env,
                           Ereg regexp,
                           StringValue string,
                           @Optional @Reference Value regsV)
  {

    if (regexp.getRawRegexp().length() == 0) {
      env.warning(L.l("empty pattern argument"));
      return BooleanValue.FALSE;
    }

    return eregImpl(env, regexp, string, regsV);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value eregi(Env env,
                            Eregi regexp,
                            StringValue string,
                            @Optional @Reference Value regsV)
  {

    if (regexp.getRawRegexp().length() == 0) {
      env.warning(L.l("empty pattern argument"));
      return BooleanValue.FALSE;
    }

    //  php/1511 : error when pattern argument is null or an empty string
    return eregImpl(env, regexp, string, regsV);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value eregImpl(Env env,
                               Ereg regexp,
                               StringValue string,
                               Value regsV)
  {
    if (regexp == null)
      return BooleanValue.FALSE;

    // php/1512 : non-string pattern argument is converted to
    // an integer value and formatted as a string.

    /*
    if (! rawPattern.isString())
      rawPatternStr = rawPattern.toLongValue().toStringValue();
    else
      rawPatternStr = rawPattern.toStringValue();
    */

    RegexpState regexpState = RegexpState.create(env, regexp, string);

    if (regexpState.exec(env, string, 0) < 0) {
      RegexpState.free(env, regexpState);

      return BooleanValue.FALSE;
    }

    if (regsV != null && ! (regsV instanceof NullValue)) {
      ArrayValue regs = new ArrayValueImpl();
      regsV.set(regs);

      regs.put(LongValue.ZERO, regexpState.group(env));
      int count = regexpState.groupCount();

      for (int i = 1; i < count; i++) {
        StringValue group = regexpState.group(env, i);

        Value value;
        if (group == null || group.length() == 0)
          value = BooleanValue.FALSE;
        else
          value = group;

        regs.put(LongValue.create(i), value);
      }

      int len = regexpState.end() - regexpState.start();

      RegexpState.free(env, regexpState);

      if (len == 0)
        return LongValue.ONE;
      else
        return LongValue.create(len);
    }
    else {
      RegexpState.free(env, regexpState);

      return LongValue.ONE;
    }
  }

  public static Regexp compileRegexp(StringValue regexpValue)
  {
    try {
      return createRegexp(regexpValue);
    } catch (Exception e) {
      // XXX: should create special error regexp.
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  public static Regexp createRegexp(StringValue regexpValue)
  {
    try {
      return createRegexpImpl(regexpValue);
    }
    catch (IllegalRegexpException e) {
      throw new QuercusException(e);
    }
  }

  public static Regexp createRegexp(Env env, StringValue regexpValue)
  {
    try {
      return createRegexpImpl(regexpValue);
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return null;
    }
  }

  private static Regexp createRegexpImpl(StringValue regexpValue)
    throws IllegalRegexpException
  {
    if (regexpValue.length() < 2) {
      throw new QuercusException(
          L.l("Regexp pattern must have opening and closing delimiters"));
    }

    RegexpCacheItem cacheItem = _regexpCache.get(regexpValue);

    if (cacheItem == null) {
      cacheItem = new RegexpCacheItem(regexpValue);
        
      _regexpCache.putIfNew(regexpValue, cacheItem);
        
      cacheItem = _regexpCache.get(regexpValue);
    }
      
    return cacheItem.get();
  }

  public static Regexp []createRegexpArray(Value pattern)
  {
    if (pattern.isArray()) {
      ArrayValue array = (ArrayValue) pattern;

      Regexp []regexpArray = new Regexp[array.getSize()];

      int i = 0;
      for (Value value : array.values()) {
        Regexp regexp = createRegexp(value.toStringValue());
        regexpArray[i++] = regexp;
      }

      return regexpArray;
    }
    else {
      Regexp regexp = createRegexp(pattern.toStringValue());

      return new Regexp [] { regexp };
    }
  }

  public static Regexp []createRegexpArray(Env env, Value pattern)
  {
    try {
      if (pattern.isArray()) {
        ArrayValue array = pattern.toArrayValue(env);

        Regexp []regexpArray = new Regexp[array.getSize()];

        int i = 0;
        for (Value value : array.values()) {
          Regexp regexp = createRegexp(env, value.toStringValue(env));
          regexpArray[i++] = regexp;
        }

        return regexpArray;
      }
      else {
        Regexp regexp = createRegexp(env, pattern.toStringValue(env));

        return new Regexp [] { regexp };
      }
    } catch (Exception e) {
      env.warning(e);

      return null;
    }
  }

  public static Ereg createEreg(Env env, Value value)
  {
    try {
      StringValue regexpStr;

      if (value.isNull() || value.isBoolean())
        regexpStr = env.getEmptyString();
      else if (! value.isString())
        regexpStr = value.toLongValue().toStringValue();
      else
        regexpStr = value.toStringValue();

      Ereg ereg = _eregCache.get(regexpStr);

      if (ereg == null) {
        StringValue cleanPattern = cleanEregRegexp(regexpStr, false);

        ereg = new Ereg(cleanPattern);

        _eregCache.put(regexpStr, ereg);
      }

      return ereg;
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return null;
    }
  }

  public static Ereg createEreg(Value value)
  {
    try {
      StringValue regexpStr;

      if (value.isNull() || value.isBoolean())
        regexpStr = StringValue.EMPTY;
      else if (! value.isString())
        regexpStr = value.toLongValue().toStringValue();
      else
        regexpStr = value.toStringValue();

      Ereg ereg = _eregCache.get(regexpStr);

      if (ereg == null) {
        StringValue cleanPattern = cleanEregRegexp(regexpStr, false);

        ereg = new Ereg(cleanPattern);

        _eregCache.put(regexpStr, ereg);
      }

      return ereg;
    }
    catch (IllegalRegexpException e) {
      throw new QuercusException(e);
    }
  }

  public static Eregi createEregi(Env env, Value value)
  {
    try {
      StringValue regexpStr;

      if (value.isNull() || value.isBoolean())
        regexpStr = env.getEmptyString();
      else if (! value.isString())
        regexpStr = value.toLongValue().toStringValue();
      else
        regexpStr = value.toStringValue();

      Eregi eregi = _eregiCache.get(regexpStr);

      if (eregi == null) {
        StringValue cleanPattern = cleanEregRegexp(regexpStr, false);

        eregi = new Eregi(cleanPattern);

        _eregiCache.put(regexpStr, eregi);
      }

      return eregi;
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return null;
    }
  }

  public static Eregi createEregi(Value value)
  {
    try {
      StringValue regexpStr;

      if (value.isNull() || value.isBoolean())
        regexpStr = StringValue.EMPTY;
      else if (! value.isString())
        regexpStr = value.toLongValue().toStringValue();
      else
        regexpStr = value.toStringValue();

      Eregi eregi = _eregiCache.get(regexpStr);

      if (eregi == null) {
        StringValue cleanPattern = cleanEregRegexp(regexpStr, false);

        eregi = new Eregi(cleanPattern);

        _eregiCache.put(regexpStr, eregi);
      }

      return eregi;
    }
    catch (IllegalRegexpException e) {
      throw new QuercusException(e);
    }
  }

  public static UnicodeEreg createUnicodeEreg(Env env, StringValue pattern)
  {
    return createUnicodeEreg(env, pattern, MbstringModule.getEncoding(env));
  }

  public static UnicodeEreg createUnicodeEreg(Env env,
                                              StringValue pattern,
                                              String encoding)
  {
    try {
      UnicodeEregKey key = new UnicodeEregKey(pattern, encoding);

      UnicodeEreg ereg = _unicodeEregCache.get(key);

      if (ereg == null) {
        pattern = pattern.convertToUnicode(env, encoding);

        StringValue cleanPattern = cleanEregRegexp(pattern, false);

        ereg = new UnicodeEreg(cleanPattern);

        _unicodeEregCache.put(key, ereg);
      }

      return ereg;
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return null;
    }
  }

  public static UnicodeEregi createUnicodeEregi(Env env, StringValue pattern)
  {
    return createUnicodeEregi(env, pattern, MbstringModule.getEncoding(env));
  }

  public static UnicodeEregi createUnicodeEregi(Env env,
                                               StringValue pattern,
                                               String encoding)
  {
    try {
      UnicodeEregKey key = new UnicodeEregKey(pattern, encoding);

      UnicodeEregi ereg = _unicodeEregiCache.get(key);

      if (ereg == null) {
        pattern = pattern.convertToUnicode(env, encoding);

        StringValue cleanPattern = cleanEregRegexp(pattern, false);

        ereg = new UnicodeEregi(cleanPattern);

        _unicodeEregiCache.put(key, ereg);
      }

      return ereg;
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return null;
    }
  }

  /**
   * Returns the last regexp error
   */
  public static Value preg_last_error(Env env)
  {
    return LongValue.ZERO;
  }

  public static Value preg_match(Env env,
                                 Regexp regexp,
                                 StringValue subject,
                                 @Optional @Reference Value matchRef,
                                 @Optional int flags,
                                 @Optional int offset)
  {
    if (regexp == null)
      return BooleanValue.FALSE;

    StringValue empty = subject.EMPTY;

    RegexpState regexpState = RegexpState.create(env, regexp, subject);

    ArrayValue regs;

    if (matchRef.isDefault())
      regs = null;
    else
      regs = new ArrayValueImpl();

    if (regexpState == null || regexpState.exec(env, subject, offset) < 0) {
      if (regs != null)
        matchRef.set(regs);

      env.freeRegexpState(regexpState);

      return LongValue.ZERO;
    }

    boolean isOffsetCapture = (flags & PREG_OFFSET_CAPTURE) != 0;

    if (regs != null) {
      if (isOffsetCapture) {
        ArrayValueImpl part = new ArrayValueImpl();
        part.append(regexpState.group(env));
        part.append(LongValue.create(regexpState.start()));

        regs.put(LongValue.ZERO, part);
      }
      else
        regs.put(LongValue.ZERO, regexpState.group(env));

      int count = regexpState.groupCount();
      for (int i = 1; i < count; i++) {
        if (! regexpState.isMatchedGroup(i))
          continue;

        StringValue group = regexpState.group(env, i);

        if (isOffsetCapture) {
          // php/151u
          // add unmatched groups first
          for (int j = regs.getSize(); j < i; j++) {
            ArrayValue part = new ArrayValueImpl();

            part.append(empty);
            part.append(LongValue.MINUS_ONE);

            regs.put(LongValue.create(j), part);
          }

          ArrayValueImpl part = new ArrayValueImpl();
          part.append(group);
          part.append(LongValue.create(regexpState.start(i)));

          StringValue name = regexpState.getGroupName(i);
          if (name != null)
            regs.put(name, part);

          regs.put(LongValue.create(i), part);
        }
        else {
          // php/151u
          // add unmatched groups first
          for (int j = regs.getSize(); j < i; j++) {
            regs.put(LongValue.create(j), empty);
          }

          StringValue name = regexp.getGroupName(i);
          if (name != null)
            regs.put(name, group);

          regs.put(LongValue.create(i), group);
        }
      }

      matchRef.set(regs);
    }

    env.freeRegexpState(regexpState);

    return LongValue.ONE;
  }

  /**
   * Returns the number of full pattern matches or FALSE on error.
   *
   * @param env the calling environment
   */
  public static Value preg_match_all(Env env,
                                     Regexp regexp,
                                     StringValue subject,
                                     @Reference Value matchRef,
                                     @Optional("PREG_PATTERN_ORDER") int flags,
                                     @Optional int offset)
  {
    if (regexp == null)
      return BooleanValue.FALSE;

    if (offset < 0)
      offset = subject.length() + offset;

    if ((flags & PREG_PATTERN_ORDER) == 0) {
      // php/152m
      if ((flags & PREG_SET_ORDER) == 0) {
        flags = flags | PREG_PATTERN_ORDER;
      }
    }
    else {
      if ((flags & PREG_SET_ORDER) != 0) {
        env.warning((
            L.l("Cannot combine PREG_PATTER_ORDER and PREG_SET_ORDER")));
        return BooleanValue.FALSE;
      }
    }

    RegexpState regexpState = RegexpState.create(env, regexp, subject);

    if (offset > 0)
      regexpState.setFirst(offset);

    ArrayValue matches;

    if (matchRef instanceof ArrayValue)
      matches = (ArrayValue) matchRef;
    else
      matches = new ArrayValueImpl();

    matches.clear();

    matchRef.set(matches);

    Value result = null;

    if ((flags & PREG_PATTERN_ORDER) != 0) {
      result = pregMatchAllPatternOrder(env,
                                        regexpState,
                                        subject,
                                        matches,
                                        flags,
                                        offset);
    }
    else if ((flags & PREG_SET_ORDER) != 0) {
      result = pregMatchAllSetOrder(env,
                                    regexp,
                                    regexpState,
                                    subject,
                                    matches,
                                    flags,
                                    offset);
    }
    else
      throw new UnsupportedOperationException();

    env.freeRegexpState(regexpState);

    return result;
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static LongValue pregMatchAllPatternOrder(Env env,
                                                   RegexpState regexpState,
                                                   StringValue subject,
                                                   ArrayValue matches,
                                                   int flags,
                                                   int offset)
  {
    int groupCount = regexpState == null ? 0 : regexpState.groupCount();

    ArrayValue []matchList = new ArrayValue[groupCount + 1];

    StringValue emptyStr = subject.EMPTY;

    for (int j = 0; j < groupCount; j++) {
      ArrayValue values = new ArrayValueImpl();

      Value patternName = regexpState.getGroupName(j);

      // XXX: named subpatterns causing conflicts with array indexes?
      if (patternName != null)
        matches.put(patternName, values);

      matches.put(values);
      matchList[j] = values;
    }

    int count = 0;

    while (regexpState.find()) {
      count++;

      for (int j = 0; j < groupCount; j++) {
        ArrayValue values = matchList[j];

        if (! regexpState.isMatchedGroup(j)) {
          /*
          if (j == groupCount || (flags & PREG_OFFSET_CAPTURE) == 0)
            values.put(emptyStr);
          else {
            Value result = new ArrayValueImpl();

            result.put(emptyStr);
            result.put(LongValue.MINUS_ONE);

            values.put(result);
          }
          */

          values.put(emptyStr);

          continue;
        }

        StringValue groupValue = regexpState.group(env, j);

        Value result = NullValue.NULL;

        if (groupValue != null) {
          if ((flags & PREG_OFFSET_CAPTURE) != 0) {
            result = new ArrayValueImpl();
            result.put(groupValue);

            result.put(LongValue.create(regexpState.getBegin(j)));
          } else {
            result = groupValue;
          }
        }

        values.put(result);
      }
    }

    return LongValue.create(count);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  private static LongValue pregMatchAllSetOrder(Env env,
                                                Regexp regexp,
                                                RegexpState regexpState,
                                                StringValue subject,
                                                ArrayValue matches,
                                                int flags,
                                                int offset)
  {
    if (regexpState == null || ! regexpState.find()) {
      return LongValue.ZERO;
    }

    StringValue empty = subject.EMPTY;

    int count = 0;

    do {
      count++;

      ArrayValue matchResult = new ArrayValueImpl();
      matches.put(matchResult);

      int groupCount = regexpState.groupCount();

      for (int i = 0; i < groupCount; i++) {
        int start = regexpState.start(i);
        int end = regexpState.end(i);

        // php/1542
        // php/1545
        // group is unmatched, skip
        if (start < 0 || end < start
            || (end == start && i == groupCount - 1))
          continue;

        StringValue groupValue = regexpState.group(env, i);

        Value result = NullValue.NULL;

        if (groupValue != null) {
          // php/1544
          Value patternName = regexpState.getGroupName(i);

          if (patternName != null)
            matchResult.put(patternName, groupValue);

          if ((flags & PREG_OFFSET_CAPTURE) != 0) {

            // php/152n
            // add unmatched groups first
            for (int j = matchResult.getSize(); j < i; j++) {
              ArrayValue part = new ArrayValueImpl();

              part.append(empty);
              part.append(LongValue.MINUS_ONE);

              matchResult.put(LongValue.create(j), part);
            }

            result = new ArrayValueImpl();

            result.put(groupValue);
            result.put(LongValue.create(start));
          } else {
            // php/
            // add any unmatched groups that was skipped
            for (int j = matchResult.getSize(); j < i; j++) {
              matchResult.put(LongValue.create(j), empty);
            }

            result = groupValue;
          }
        }

        matchResult.put(result);
      }
    } while (regexpState.find());

    return LongValue.create(count);
  }

  /**
   * Quotes regexp values
   */
  public static StringValue preg_quote(StringValue string,
                                       @Optional StringValue delim)
  {
    StringValue sb = string.createStringBuilder();

    boolean []extra = null;

    if (delim != null && delim.length() > 0) {
      extra = new boolean[256];

      for (int i = 0; i < delim.length(); i++)
        extra[delim.charAt(i)] = true;
    }

    int length = string.length();
    for (int i = 0; i < length; i++) {
      char ch = string.charAt(i);

      if (ch >= 256)
        sb.append(ch);
      else if (PREG_QUOTE[ch]) {
        sb.append('\\');
        sb.append(ch);
      }
      else if (extra != null && extra[ch]) {
        sb.append('\\');
        sb.append(ch);
      }
      else if (ch == 0) {
        // php/153q
        sb.append("\\000");
      }
      else
        sb.append(ch);
    }

    return sb;
  }

  /**
   * Loops through subject if subject is array of strings
   *
   * @param env
   * @param pattern string or array
   * @param replacement string or array
   * @param subject string or array
   * @param limit
   * @param count
   * @return
   */
  @UsesSymbolTable
  public static Value preg_replace(Env env,
                                   Regexp regexp,
                                   Value replacement,
                                   Value subject,
                                   @Optional("-1") long limit,
                                   @Optional @Reference Value count)
  {
    if (regexp == null)
      return BooleanValue.FALSE;

    if (count != null)
      count.set(LongValue.ZERO);

    try {
      if (subject instanceof ArrayValue) {
        ArrayValue result = new ArrayValueImpl();

        for (Map.Entry<Value,Value> entry : ((ArrayValue) subject).entrySet()) {
          Value key = entry.getKey();
          Value value = entry.getValue();

          result.put(key, pregReplace(env,
                                      regexp,
                                      replacement,
                                      value.toStringValue(),
                                      limit,
                                      count));
        }

        return result;

      }
      else if (subject.isset()) {
        return pregReplace(env,
                           regexp,
                           replacement,
                           subject.toStringValue(),
                           limit, count);
      } else
        return env.getEmptyString();
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Replaces values using regexps
   */
  private static Value pregReplace(Env env,
                                   Regexp regexp,
                                   Value replacement,
                                   StringValue subject,
                                   @Optional("-1") long limit,
                                   Value countV)
    throws IllegalRegexpException
  {
    StringValue string = subject;

    if (limit < 0)
      limit = LONG_MAX;

    if (replacement.isArray()) {
      ArrayValue replacementArray = (ArrayValue) replacement;

      Iterator<Value> replacementIter = replacementArray.values().iterator();

      StringValue replacementStr;

      if (replacementIter.hasNext())
        replacementStr = replacementIter.next().toStringValue();
      else
        replacementStr = env.getEmptyString();

      string = pregReplaceString(env,
                                 regexp,
                                 replacementStr,
                                 string,
                                 limit,
                                 countV);
    } else {
      string = pregReplaceString(env,
                                 regexp,
                                 replacement.toStringValue(),
                                 string,
                                 limit,
                                 countV);
    }

    if (string != null)
      return string;
    else
      return NullValue.NULL;
  }

  /**
   * Loops through subject if subject is array of strings
   *
   * @param env
   * @param pattern string or array
   * @param replacement string or array
   * @param subject string or array
   * @param limit
   * @param count
   * @return
   */
  @UsesSymbolTable
  public static Value preg_replace(Env env,
                                   Value pattern,
                                   Value replacement,
                                   Value subject,
                                   @Optional("-1") long limit,
                                   @Optional @Reference Value count)
  {
    try {
      Regexp []regexpList = createRegexpArray(env, pattern);

      if (regexpList == null)
        return NullValue.NULL;

      if (subject instanceof ArrayValue) {
        ArrayValue result = new ArrayValueImpl();

        for (Value value : ((ArrayValue) subject).values()) {
          result.put(pregReplace(env,
                                 regexpList,
                                 replacement,
                                 value.toStringValue(),
                                 limit,
                                 count));
        }

        return result;

      }
      else if (subject.isset()) {
        return pregReplace(env,
                           regexpList,
                           replacement,
                           subject.toStringValue(),
                           limit, count);
      } else
        return env.getEmptyString();
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Replaces values using regexps
   */
  private static Value pregReplace(Env env,
                                   Regexp []regexpList,
                                   Value replacement,
                                   StringValue subject,
                                   @Optional("-1") long limit,
                                   Value countV)
    throws IllegalRegexpException
  {
    StringValue string = subject;

    if (limit < 0)
      limit = LONG_MAX;

    if (replacement.isArray()) {
      ArrayValue replacementArray = (ArrayValue) replacement;

      Iterator<Value> replacementIter = replacementArray.values().iterator();

      for (int i = 0; i < regexpList.length; i++) {
        StringValue replacementStr;

        if (replacementIter.hasNext())
          replacementStr = replacementIter.next().toStringValue();
        else
          replacementStr = env.getEmptyString();

        string = pregReplaceString(env,
                                   regexpList[i],
                                   replacementStr,
                                   string,
                                   limit,
                                   countV);
      }
    } else {
      for (int i = 0; i < regexpList.length; i++) {
        string = pregReplaceString(env,
                                   regexpList[i],
                                   replacement.toStringValue(),
                                   string,
                                   limit,
                                   countV);
      }
    }

    if (string != null)
      return string;
    else
      return NullValue.NULL;
  }

  /**
   * replaces values using regexps and callback fun
   * @param env
   * @param patternString
   * @param fun
   * @param subject
   * @param limit
   * @param countV
   * @return subject with everything replaced
   */
  private static StringValue pregReplaceCallbackImpl(Env env,
                                                     Regexp regexp,
                                                     Callable fun,
                                                     StringValue subject,
                                                     long limit,
                                                     Value countV)
    throws IllegalRegexpException
  {
    StringValue empty = subject.EMPTY;

    long numberOfMatches = 0;

    if (limit < 0)
      limit = LONG_MAX;

    RegexpState regexpState = RegexpState.create(env, regexp);

    regexpState.setSubject(env, subject);

    StringValue result = subject.createStringBuilder();
    int tail = 0;

    while (regexpState.find() && numberOfMatches < limit) {
      // Increment countV (note: if countV != null, then it should be a Var)
      if (countV != null && countV instanceof Var) {
        long count = countV.toValue().toLong();
        countV.set(LongValue.create(count + 1));
      }

      int start = regexpState.start();
      if (tail < start)
        result = result.append(regexpState.substring(env, tail, start));

      ArrayValue regs = new ArrayValueImpl();

      for (int i = 0; i < regexpState.groupCount(); i++) {
        StringValue group = regexpState.group(env, i);

        if (group != null)
          regs.put(group);
        else
          regs.put(empty);
      }

      Value replacement = fun.call(env, regs);

      result = result.append(replacement);

      tail = regexpState.end();

      numberOfMatches++;
    }

    if (tail < regexpState.getSubjectLength())
      result = result.append(regexpState.substring(env, tail));

    env.freeRegexpState(regexpState);

    return result;
  }

  static StringValue pregReplaceString(Env env,
                                       Regexp regexp,
                                       StringValue replacement,
                                       StringValue subject,
                                       long limit,
                                       Value countV)
  {
    RegexpState regexpState = RegexpState.create(env, regexp);

    if (! regexpState.setSubject(env, subject))
      return null;

    // check for e modifier in patternString
    boolean isEval = regexp.isEval();

    ArrayList<Replacement> replacementProgram
      = _replacementCache.get(replacement);

    if (replacementProgram == null) {
      replacementProgram = compileReplacement(env, replacement, isEval);
      if (replacementProgram == null)
        return null;

      _replacementCache.put(replacement, replacementProgram);
    }

    StringValue result = pregReplaceStringImpl(env,
                                               regexp,
                                               regexpState,
                                               replacementProgram,
                                               subject,
                                               limit,
                                               countV,
                                               isEval);

    env.freeRegexpState(regexpState);

    return result;
  }

  /**
   * Replaces values using regexps
   */
  public static Value ereg_replace(Env env,
                                   Value regexpValue,
                                   Value replacement,
                                   StringValue subject)
  {
    StringValue regexpStr;

    if (regexpValue.isLong())
      regexpStr = env.createString((char) regexpValue.toInt());
    else
      regexpStr = regexpValue.toStringValue(env);

    if (regexpStr.length() == 0) {
      env.warning(L.l("empty pattern argument"));
      return BooleanValue.FALSE;
    }

    Ereg regexp = createEreg(env, regexpStr);

    return eregReplaceImpl(env, regexp, replacement, subject, false);
  }

  /**
   * Replaces values using regexps
   */
  public static Value eregi_replace(Env env,
                                    Value regexpValue,
                                    Value replacement,
                                    StringValue subject)
  {
    StringValue regexpStr;

    if (regexpValue.isLong())
      regexpStr = env.createString((char) regexpValue.toInt());
    else
      regexpStr = regexpValue.toStringValue(env);

    if (regexpStr.length() == 0) {
      env.warning(L.l("empty pattern argument"));
      return BooleanValue.FALSE;
    }

    Ereg regexp = createEregi(env, regexpStr);

    return eregReplaceImpl(env, regexp, replacement, subject, true);
  }

  /**
   * Replaces values using regexps
   */

  public static Value eregReplaceImpl(Env env,
                                      Ereg regexp,
                                      Value replacement,
                                      StringValue subject,
                                      boolean isCaseInsensitive)
  {
    StringValue replacementStr;

    // php/150u : If a non-string type argument is passed
    // for the pattern or replacement argument, it is
    // converted to a string of length 1 that contains
    // a single character.

    if (replacement instanceof NullValue) {
      replacementStr = env.getEmptyString();
    } else if (replacement instanceof StringValue) {
      replacementStr = replacement.toStringValue();
    } else {
      replacementStr = env.createString(
        String.valueOf((char) replacement.toLong()));
    }

    RegexpState regexpState = RegexpState.create(env, regexp);

    regexpState.setSubject(env, subject);

    ArrayList<Replacement> replacementProgram
      = _replacementCache.get(replacementStr);

    if (replacementProgram == null) {
      replacementProgram = compileReplacement(env, replacementStr, false);
      if (replacementProgram == null)
        return null;
      _replacementCache.put(replacementStr, replacementProgram);
    }

    StringValue result = pregReplaceStringImpl(env,
                                               regexp,
                                               regexpState,
                                               replacementProgram,
                                               subject,
                                               -1,
                                               NullValue.NULL,
                                               false);

    env.freeRegexpState(regexpState);

    return result;
  }

  /**
   * Replaces values using regexps
   */
  private static StringValue
      pregReplaceStringImpl(Env env,
                            Regexp regexp,
                            RegexpState regexpState,
                            ArrayList<Replacement> replacementProgram,
                            StringValue subject,
                            long limit,
                            Value countV,
                            boolean isEval)
  {
    if (limit < 0)
      limit = LONG_MAX;

    StringValue result = subject.createStringBuilder();

    int tail = 0;
    boolean isMatched = false;

    int replacementLen = replacementProgram.size();

    while (limit-- > 0 && regexpState.find()) {
      isMatched = true;

      // Increment countV (note: if countV != null, then it should be a Var)
      if (countV != null && countV instanceof Var) {
        countV.set(LongValue.create(countV.toLong() + 1));
      }

      // append all text up to match
      int start = regexpState.start();
      if (tail < start)
        result = result.append(regexpState.substring(env, tail, start));

      // if isEval then append replacement evaluated as PHP code
      // else append replacement string
      if (isEval) {
        StringValue evalString = subject.createStringBuilder();

        try {
          for (int i = 0; i < replacementLen; i++) {
            Replacement replacement = replacementProgram.get(i);

            evalString = replacement.eval(env, evalString, regexpState);
          }
        } catch (Exception e) {
          env.warning(e);
        }

        try {
          if (evalString.length() > 0) { // php/152z
            result = result.append(env.evalCode(evalString.toString()));
          }
        } catch (Exception e) {
          env.warning(e);
        }

      } else {
        for (int i = 0; i < replacementLen; i++) {
          Replacement replacement = replacementProgram.get(i);

          result = replacement.eval(env, result, regexpState);
        }
      }

      tail = regexpState.end();
    }

    if (! isMatched)
      return subject;

    if (tail < regexpState.getSubjectLength())
      result = result.append(regexpState.substring(env, tail));

    return result;
  }

  /**
   * Loops through subject if subject is array of strings
   *
   * @param env
   * @param pattern
   * @param fun
   * @param subject
   * @param limit
   * @param count
   * @return
   */
  public static Value preg_replace_callback(Env env,
                                            Regexp regexp,
                                            @NotNull Callable fun,
                                            Value subject,
                                            @Optional("-1") long limit,
                                            @Optional @Reference Value count)
  {
    if (fun == null) {
      env.warning(
          L.l("callable argument can't be null in preg_replace_callback"));
      return subject;
    }

    if (regexp == null)
      return NullValue.NULL;
    else if (regexp.isEval()) {
      env.warning(L.l("regexp can't use /e preg_replace_callback /{0}/",
                      regexp.getPattern()));

      return NullValue.NULL;
    }

    // php/153s
    if (count != null)
      count.set(LongValue.ZERO);

    try {
      if (subject instanceof ArrayValue) {
        ArrayValue result = new ArrayValueImpl();

        for (Value value : ((ArrayValue) subject).values()) {
          result.put(pregReplaceCallback(env,
                                         regexp,
                                         fun,
                                         value.toStringValue(),
                                         limit,
                                         count));
        }

        return result;
      } else if (subject.isset()) {
        return pregReplaceCallback(env,
                                   regexp,
                                   fun,
                                   subject.toStringValue(),
                                   limit,
                                   count);
      } else {
        return env.getEmptyString();
      }
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Loops through subject if subject is array of strings
   */
  public static Value preg_replace_callback(Env env,
                                            Value regexpValue,
                                            Callable fun,
                                            Value subject,
                                            @Optional("-1") long limit,
                                            @Optional @Reference Value count)
  {
    if (! regexpValue.isArray()) {
      Regexp regexp = createRegexp(env, regexpValue.toStringValue());


      return preg_replace_callback(env, regexp,
                                   fun, subject, limit, count);
    }

    Regexp []regexpList = createRegexpArray(env, regexpValue);

    if (regexpList == null)
      return NullValue.NULL;

    try {
      if (subject instanceof ArrayValue) {
        ArrayValue result = new ArrayValueImpl();

        for (Value value : ((ArrayValue) subject).values()) {
          result.put(pregReplaceCallback(env,
                                         regexpList,
                                         fun,
                                         value.toStringValue(),
                                         limit,
                                         count));
        }

        return result;

      } else if (subject.isset()) {
        return pregReplaceCallback(env,
                                   regexpList,
                                   fun,
                                   subject.toStringValue(),
                                   limit,
                                   count);
      } else {
        return env.getEmptyString();
      }
    }
    catch (IllegalRegexpException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Replaces values using regexps
   */
  private static Value pregReplaceCallback(Env env,
                                           Regexp regexp,
                                           Callable fun,
                                           StringValue subject,
                                           @Optional("-1") long limit,
                                           @Optional @Reference Value countV)
    throws IllegalRegexpException
  {
    if (limit < 0)
      limit = LONG_MAX;

    if (! subject.isset()) {
      return env.getEmptyString();
    }
    else {
      return pregReplaceCallbackImpl(env,
                                     regexp,
                                     fun,
                                     subject,
                                     limit,
                                     countV);
    }
  }

  /**
   * Replaces values using regexps
   */
  private static Value pregReplaceCallback(Env env,
                                           Regexp []regexpList,
                                           Callable fun,
                                           StringValue subject,
                                           @Optional("-1") long limit,
                                           @Optional @Reference Value countV)
    throws IllegalRegexpException
  {
    if (limit < 0)
      limit = LONG_MAX;

    if (! subject.isset()) {
      return env.getEmptyString();
    }
    else {
      for (int i = 0; i < regexpList.length; i++) {
        subject = pregReplaceCallbackImpl(env,
                                          regexpList[i],
                                          fun,
                                          subject,
                                          limit,
                                          countV);
      }

      return subject;
    }
  }

  /**
   * Returns array of substrings or
   * of arrays ([0] => substring [1] => offset) if
   * PREG_SPLIT_OFFSET_CAPTURE is set
   *
   * @param env the calling environment
   */
  public static Value preg_split(Env env,
                                 Regexp regexp,
                                 StringValue string,
                                 @Optional("-1") long limit,
                                 @Optional int flags)
  {
    if (regexp == null)
      return BooleanValue.FALSE;

    if (limit <= 0)
      limit = LONG_MAX;

    StringValue empty = StringValue.EMPTY;

    RegexpState regexpState = RegexpState.create(env, regexp);
    regexpState.setSubject(env, string);

    ArrayValue result = new ArrayValueImpl();

    int head = 0;
    long count = 0;

    boolean allowEmpty = (flags & PREG_SPLIT_NO_EMPTY) == 0;
    boolean isCaptureOffset = (flags & PREG_SPLIT_OFFSET_CAPTURE) != 0;
    boolean isCaptureDelim = (flags & PREG_SPLIT_DELIM_CAPTURE) != 0;

    GroupNeighborMap neighborMap
      = new GroupNeighborMap(regexp.getPattern(), regexpState.groupCount());

    while (regexpState.find()) {
      int startPosition = head;
      StringValue unmatched;

      // Get non-matching sequence
      if (count == limit - 1) {
        unmatched = regexpState.substring(env, head);
        head = regexpState.getSubjectLength();
      }
      else {
        // php/153y
        unmatched = regexpState.substring(env, head, regexpState.start());
        head = regexpState.end();
      }

      // Append non-matching sequence
      if (unmatched.length() != 0 || allowEmpty) {
        if (isCaptureOffset) {
          ArrayValue part = new ArrayValueImpl();

          part.put(unmatched);
          part.put(LongValue.create(startPosition));

          result.put(part);
        }
        else {
          result.put(unmatched);
        }

        count++;
      }

      if (count == limit)
        break;

      // Append parameterized delimiters
      if (isCaptureDelim) {
        for (int i = 1; i < regexpState.groupCount(); i++) {
          int start = regexpState.start(i);
          int end = regexpState.end(i);

          // Skip empty groups
          if (! regexpState.isMatchedGroup(i)) {
            continue;
          }

          // Append empty OR neighboring groups that were skipped
          // php/152r
          if (allowEmpty) {
            int group = i;
            while (neighborMap.hasNeighbor(group)) {
              group = neighborMap.getNeighbor(group);

              if (regexpState.isMatchedGroup(group))
                break;

              if (isCaptureOffset) {
                ArrayValue part = new ArrayValueImpl();

                part.put(empty);
                part.put(LongValue.create(startPosition));

                result.put(part);
              }
              else
                result.put(empty);
            }
          }

          if (end - start <= 0 && ! allowEmpty) {
            continue;
          }

          StringValue groupValue = regexpState.group(env, i);

          if (isCaptureOffset) {
            ArrayValue part = new ArrayValueImpl();

            part.put(groupValue);
            part.put(LongValue.create(start));

            result.put(part);
          }
          else {
            result.put(groupValue);
          }
        }
      }
    }

    // Append non-matching sequence at the end
    if (count < limit
        && (head < regexpState.getSubjectLength() || allowEmpty)) {
      if (isCaptureOffset) {
        ArrayValue part = new ArrayValueImpl();

        part.put(regexpState.substring(env, head));
        part.put(LongValue.create(head));

        result.put(part);
      }
      else {
        result.put(regexpState.substring(env, head));
      }
    }

    env.freeRegexpState(regexpState);

    return result;
  }

  /**
   * Makes a regexp for a case-insensitive match.
   */
  public static StringValue sql_regcase(StringValue string)
  {
    StringValue sb = string.createStringBuilder();

    int len = string.length();
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (Character.isLowerCase(ch)) {
        sb.append('[');
        sb.append(Character.toUpperCase(ch));
        sb.append(ch);
        sb.append(']');
      }
      else if (Character.isUpperCase(ch)) {
        sb.append('[');
        sb.append(ch);
        sb.append(Character.toLowerCase(ch));
        sb.append(']');
      }
      else
        sb.append(ch);
    }

    return sb;
  }

  /**
   * Returns an array of strings produces from splitting the passed string
   * around the provided pattern.  The pattern is case sensitive.
   *
   * @param patternString the pattern
   * @param string the string to split
   * @param limit if specified, the maximum number of elements in the array
   * @return an array of strings split around the pattern string
   */
  public static Value split(Env env,
                            Ereg regexp,
                            StringValue string,
                            @Optional("-1") long limit)
  {
    return splitImpl(env, regexp, string, limit);
  }

  /**
   * Returns an array of strings produces from splitting the passed string
   * around the provided pattern.  The pattern is case insensitive.
   *
   * @param patternString the pattern
   * @param string the string to split
   * @param limit if specified, the maximum number of elements in the array
   * @return an array of strings split around the pattern string
   */
  public static Value spliti(Env env,
                             Eregi regexp,
                             StringValue string,
                             @Optional("-1") long limit)
  {
    return splitImpl(env, regexp, string, limit);
  }

  /**
   * Split string into array by regular expression
   *
   * @param env the calling environment
   */

  private static Value splitImpl(Env env,
                                 Ereg regexp,
                                 StringValue string,
                                 long limit)
  {
    if (limit < 0)
      limit = LONG_MAX;

    // php/151c

    RegexpState regexpState = RegexpState.create(env, regexp);

    regexpState.setSubject(env, string);

    ArrayValue result = new ArrayValueImpl();

    long count = 0;
    int head = 0;

    while (regexpState.find() && count < limit) {
      StringValue value;
      if (count == limit - 1) {
        value = regexpState.substring(env, head);
        head = string.length();
      } else {
        value = regexpState.substring(env, head, regexpState.start());
        head = regexpState.end();
      }

      result.put(value);

      count++;
    }

    if (head <= string.length() && count != limit) {
      result.put(regexpState.substring(env, head));
    }

    env.freeRegexpState(regexpState);

    return result;
  }

  /**
   * Returns an array of all the values that matched the given pattern if the
   * flag no flag is passed.  Otherwise it will return an array of all the
   * values that did not match.
   *
   * @param patternString the pattern
   * @param input the array to check the pattern against
   * @param flag 0 for matching and 1 for elements that do not match
   * @return an array of either matching elements are non-matching elements
   */
  public static Value preg_grep(Env env,
                                Regexp regexp,
                                ArrayValue input,
                                @Optional("0") int flag)
  {
    if (input == null)
      return NullValue.NULL;

    if (regexp == null)
      return BooleanValue.FALSE;

    RegexpState regexpState = RegexpState.create(env, regexp);

    ArrayValue matchArray = new ArrayValueImpl();

    for (ArrayValue.Entry entry = input.getHead();
         entry != null;
         entry = entry.getNext()) {
      // php/153v
      Value entryValue = entry.getRawValue();
      Value entryKey = entry.getKey();

      boolean found = regexpState.find(env, entryValue.toStringValue());

      if (! found && flag == PREG_GREP_INVERT)
        matchArray.append(entryKey, entryValue);
      else if (found && flag != PREG_GREP_INVERT)
        matchArray.append(entryKey, entryValue);
    }

    env.freeRegexpState(regexpState);

    return matchArray;
  }

  private static StringValue addDelimiters(Env env,
                                           StringValue str,
                                           String startDelim,
                                           String endDelim)
  {
    StringValue sb = str.createStringBuilder();

    sb = sb.appendBytes(startDelim);
    sb = sb.append(str);
    sb = sb.appendBytes(endDelim);

    return sb;
  }

  private static ArrayList<Replacement>
    compileReplacement(Env env, StringValue replacement, boolean isEval)
  {
    ArrayList<Replacement> program = new ArrayList<Replacement>();
    StringBuilder text = new StringBuilder();

    for (int i = 0; i < replacement.length(); i++) {
      char ch = replacement.charAt(i);

      if ((ch == '\\' || ch == '$') && i + 1 < replacement.length()) {
        char digit;

        if ('0' <= (digit = replacement.charAt(i + 1)) && digit <= '9') {
          int group = digit - '0';
          i++;

          if (i + 1 < replacement.length()
          && '0' <= (digit = replacement.charAt(i + 1)) && digit <= '9') {
            group = 10 * group + digit - '0';
            i++;
          }

          if (text.length() > 0) {
            program.add(new TextReplacement(text));
          }

          if (isEval)
            program.add(new GroupEscapeReplacement(group));
          else
            program.add(new GroupReplacement(group));

          text.setLength(0);
        }
        else if (ch == '\\') {
          i++;

          if (digit != '\\') {
            text.append('\\');
          }
          text.append(digit);
          // took out test for ch == '$' because must be true
          //} else if (ch == '$' && digit == '{') {
        } else if (digit == '{') {
          i += 2;

          int group = 0;

          while (i < replacement.length()
                 && '0' <= (digit = replacement.charAt(i)) && digit <= '9') {
            group = 10 * group + digit - '0';

            i++;
          }

          if (digit != '}') {
//             env.warning(L.l("expected '}' to close replacement at '{1}' " +
//                 "replacement {0}", (char) digit, replacement));
            text.append("${");
            text.append(group);
            continue;
          }

          if (text.length() > 0)
            program.add(new TextReplacement(text));

          if (isEval)
            program.add(new GroupEscapeReplacement(group));
          else
            program.add(new GroupReplacement(group));

          text.setLength(0);
        }
        else
          text.append(ch);
      }
      else
        text.append(ch);
    }

    if (text.length() > 0)
      program.add(new TextReplacement(text));

    return program;
  }

  /**
   * Cleans the regexp from valid values that the Java regexps can't handle.
   * Ereg has a different syntax so need to handle it differently from preg.
   */
  private static StringValue cleanEregRegexp(StringValue regexp,
                                             boolean isComments)
  {
    int len = regexp.length();

    StringValue sb = regexp.createStringBuilder();
    char quote = 0;

    for (int i = 0; i < len; i++) {
      char ch = regexp.charAt(i);

      switch (ch) {
      case '\\':
        if (quote == '[') {
          sb = sb.appendByte('\\');
          sb = sb.appendByte('\\');
          continue;
        }

        if (i + 1 < len) {
          i++;

          ch = regexp.charAt(i);

          if (
              ch == '0'
              || '1' <= ch && ch <= '3'
              && i + 1 < len
              && '0' <= regexp.charAt(i + 1)
              && ch <= '7'
              ) {
            // Java's regexp requires \0 for octal

            sb = sb.appendByte('\\');
            sb = sb.appendByte('0');
            sb = sb.appendByte(ch);
          }
          else if (ch == 'x' && i + 1 < len && regexp.charAt(i + 1) == '{') {
            sb = sb.appendByte('\\');

            int tail = regexp.indexOf('}', i + 1);

            if (tail > 0) {
              StringValue hex = regexp.substring(i + 2, tail);

              int length = hex.length();

              if (length == 1)
                sb = sb.appendBytes("x0" + hex);
              else if (length == 2)
                sb = sb.appendBytes("x" + hex);
              else if (length == 3)
                sb = sb.appendBytes("u0" + hex);
              else if (length == 4)
                sb = sb.appendBytes("u" + hex);
              else
                throw new QuercusRuntimeException(L.l("illegal hex escape"));

              i = tail;
            }
            else {
              sb = sb.appendByte('\\');
              sb = sb.appendByte('x');
            }
          }
          else if (Character.isLetter(ch)) {
            switch (ch) {
            case 'a':
            case 'c':
            case 'e':
            case 'f':
            case 'n':
            case 'r':
            case 't':
            case 'x':
            case 'd':
            case 'D':
            case 's':
            case 'S':
            case 'w':
            case 'W':
            case 'b':
            case 'B':
            case 'A':
            case 'Z':
            case 'z':
            case 'G':
            case 'p': //XXX: need to translate PHP properties to Java ones
            case 'P': //XXX: need to translate PHP properties to Java ones
            case 'X':
              //case 'C': byte matching, not supported
              sb = sb.appendByte('\\');
              sb = sb.appendByte(ch);
              break;
            default:
              sb = sb.appendByte(ch);
            }
          }
          else {
            sb = sb.appendByte('\\');
            sb = sb.appendByte(ch);
          }
        }
        else
          sb = sb.appendByte('\\');
        break;

      case '[':
        if (quote == '[') {
          if (i + 1 < len && regexp.charAt(i + 1) == ':') {
            sb = sb.appendByte('[');
          }
          else {
            sb = sb.appendByte('\\');
            sb = sb.appendByte('[');
          }
        }
        else if (i + 1 < len && regexp.charAt(i + 1) == '['
          && ! (i + 2 < len && regexp.charAt(i + 2) == ':')) {
          // XXX: check regexp grammar
          // php/151n
          sb = sb.appendByte('[');
          sb = sb.appendByte('\\');
          sb = sb.appendByte('[');
          i += 1;
        }
        /*
        else if (i + 2 < len &&
                regexp.charAt(i + 1) == '^' &&
                regexp.charAt(i + 2) == ']') {
          sb.append("[^\\]");
          i += 2;
        }
        */
        else
          sb = sb.appendByte('[');

        if (quote == 0)
          quote = '[';
        break;

      case '#':
        if (quote == '[') {
          sb = sb.appendByte('\\');
          sb = sb.appendByte('#');
        }
        else if (isComments) {
          sb = sb.appendByte(ch);

          for (i++; i < len; i++) {
            ch = regexp.charAt(i);

            sb = sb.appendByte(ch);

            if (ch == '\n' || ch == '\r')
              break;
          }
        }
        else {
          sb = sb.appendByte(ch);
        }

        break;

      case ']':
        sb = sb.appendByte(ch);

        if (quote == '[')
          quote = 0;
        break;

      case '{':
        if (i + 1 < len
            && (
              '0' <= (ch = regexp.charAt(i + 1))
              && ch <= '9'
              || ch == ','
              )
            ) {
          sb = sb.appendByte('{');
          for (i++;
          i < len
              && ('0' <= (ch = regexp.charAt(i)) && ch <= '9' || ch == ',');
          i++) {
            sb = sb.appendByte(ch);
          }

          if (i < len)
            sb = sb.appendByte(regexp.charAt(i));
        }
        else {
          sb = sb.appendByte('\\');
          sb = sb.appendByte('{');
        }
        break;

      case '}':
        sb = sb.appendByte('\\');
        sb = sb.appendByte('}');
        break;

      case '|':
        sb = sb.appendByte('|');
        break;

      default:
        sb = sb.appendByte(ch);
      }
    }

    return sb;
  }

  abstract static class Replacement {
    abstract StringValue eval(Env env,
                  StringValue sb,
                  RegexpState regexpState);

    public String toString()
    {
      return getClass().getSimpleName() + "[]";
    }
  }

  static class TextReplacement
    extends Replacement
  {
    private char []_text;

    TextReplacement(StringBuilder text)
    {
      int length = text.length();

      _text = new char[length];

      text.getChars(0, length, _text, 0);
    }

    @Override
    StringValue eval(Env env,
                     StringValue sb,
                     RegexpState regexpState)
    {
      return sb.appendBytes(_text, 0, _text.length);
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      sb.append(getClass().getSimpleName());

      sb.append('[');

      for (char ch : _text)
        sb.append(ch);

      sb.append(']');

      return sb.toString();
    }
  }

  static class GroupReplacement
    extends Replacement
  {
    private int _group;

    GroupReplacement(int group)
    {
      _group = group;
    }

    @Override
    StringValue eval(Env env,
                     StringValue sb,
                     RegexpState regexpState)
    {
      if (_group < regexpState.groupCount())
        sb = sb.append(regexpState.group(env, _group));

      return sb;
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _group + "]";
    }
  }

  static class GroupEscapeReplacement
    extends Replacement
  {
    private int _group;

    GroupEscapeReplacement(int group)
    {
      _group = group;
    }

    @Override
    StringValue eval(Env env,
                     StringValue sb,
                     RegexpState regexpState)
    {
      if (_group < regexpState.groupCount()) {
        StringValue group = regexpState.group(env, _group);

        int len = group.length();

        for (int i = 0; i < len; i++) {
          char ch = group.charAt(i);

          switch (ch) {
            case '\'':
              sb = sb.appendByte('\\');
              sb = sb.appendByte('\'');
              break;
            case '"':
              sb = sb.appendByte('\\');
              sb = sb.appendByte('"');
              break;
            case '\\':
              sb = sb.appendByte('\\');
              sb = sb.appendByte('\\');
              break;
            case 0:
              sb = sb.appendByte('\\');
              sb = sb.appendByte('0');
              break;
            default:
              sb = sb.appendByte(ch);
          }
        }
      }

      return sb;
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _group + "]";
    }
  }

  /**
   * Holds information about the left neighbor of a particular group.
   */
  static class GroupNeighborMap
  {
    private int []_neighborMap;

    private static int UNSET = -1;

    public GroupNeighborMap(CharSequence regexp, int groups)
    {
      _neighborMap = new int[groups + 1];

      for (int i = 1; i <= groups; i++) {
        _neighborMap[i] = UNSET;
      }

      boolean sawEscape = false;
      boolean sawVerticalBar = false;
      boolean isLiteral = false;

      int group = 0;
      int parent = UNSET;
      int length = regexp.length();

      ArrayList<Boolean> openParenStack = new ArrayList<Boolean>(groups);

      for (int i = 0; i < length; i++) {
        char ch = regexp.charAt(i);

        if (ch == ' ' || ch == '\t' || ch == '\n' || ch == 'r' || ch == '\f') {
          continue;
        }
        else if (ch == '\\') {
          sawEscape = ! sawEscape;
          continue;
        }
        else if (ch == '[' && ! sawEscape) {
          isLiteral = true;
        }
        else if (ch == ']' && ! sawEscape) {
          isLiteral = false;
        }
        else if (isLiteral || sawEscape) {
          sawEscape = false;
        }
        else if (ch == '(') {
          if (i + 1 < length && regexp.charAt(i + 1) == '?') {
            openParenStack.add(true);
            continue;
          }

          openParenStack.add(false);
          group++;

          if (sawVerticalBar) {
            sawVerticalBar = false;
            _neighborMap[group] = group - 1;
          }
          else {
            _neighborMap[group] = parent;
            parent = group;
          }
        }
        else if (ch == ')') {
          if (openParenStack.remove(openParenStack.size() - 1))
            continue;

          sawVerticalBar = false;
        }
        else if (ch == '|') {
          sawVerticalBar = true;
        }
        else {
        }
      }
    }

    public boolean hasNeighbor(int group)
    {
      return _neighborMap[group] != UNSET;
    }

    public int getNeighbor(int group)
    {
      return _neighborMap[group];
    }
  }

  static class UnicodeEregKey
  {
    StringValue _regexpValue;
    String _encoding;

    UnicodeEregKey(StringValue regexpValue, String encoding)
    {
      _regexpValue = regexpValue;
      _encoding = encoding;
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof UnicodeEregKey))
        return false;

      UnicodeEregKey ereg = (UnicodeEregKey) o;

      return _regexpValue.equals(ereg._regexpValue)
        && _encoding.equals(ereg._encoding);
    }
  }
  
  static final class RegexpCacheItem {
    private final StringValue _pattern;
    
    private Regexp _regexp;
    private IllegalRegexpException _exn;
    
    RegexpCacheItem(StringValue pattern)
    {
      _pattern = pattern;
    }
    
    public Regexp get()
      throws IllegalRegexpException
    {
      if (_regexp != null)
        return _regexp;
      else if (_exn != null)
        throw _exn;
      
      synchronized (this) {
        try {
          _regexp = new Regexp(_pattern);
          
          return _regexp;
        } catch (IllegalRegexpException e) {
          _exn = e;
          
          throw e;
        }
      }
    }
  }

  static {
    PREG_QUOTE['\\'] = true;
    PREG_QUOTE['+'] = true;
    PREG_QUOTE['*'] = true;
    PREG_QUOTE['?'] = true;
    PREG_QUOTE['['] = true;
    PREG_QUOTE['^'] = true;
    PREG_QUOTE[']'] = true;
    PREG_QUOTE['$'] = true;
    PREG_QUOTE['('] = true;
    PREG_QUOTE[')'] = true;
    PREG_QUOTE['{'] = true;
    PREG_QUOTE['}'] = true;
    PREG_QUOTE['='] = true;
    PREG_QUOTE['!'] = true;
    PREG_QUOTE['<'] = true;
    PREG_QUOTE['>'] = true;
    PREG_QUOTE['|'] = true;
    PREG_QUOTE[':'] = true;
    PREG_QUOTE['.'] = true;
    PREG_QUOTE['-'] = true; // php/153w

  }
}
