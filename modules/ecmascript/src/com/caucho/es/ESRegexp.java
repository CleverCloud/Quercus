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

package com.caucho.es;

import com.caucho.util.IntMap;

import java.util.Iterator;
import java.util.regex.Pattern;

public class ESRegexp extends ESObject {
  static ESId GLOBAL = ESId.intern("global");
  static ESId IGNORE_CASE = ESId.intern("ignoreCase");
  static ESId LAST_INDEX = ESId.intern("lastIndex");
  static ESId SOURCE = ESId.intern("source");

  ESString pattern;
  ESString flags;
  Pattern _regexp;

  boolean hasSetProps;
  int lastIndex;
  ESString lastString;
  int lastStart;

  ESRegexp(ESString pattern, ESString flags) throws ESException
  {
    super("RegExp", getPrototype());

    this.pattern = pattern;
    this.flags = flags;
    lastString = ESString.NULL;

    /* java.util.regex
    try {
      _regexp = Pattern.parse(pattern.toString(), flags.toString());
    } catch (Exception e) {
      throw new ESException("regexp: " + e.getMessage());
    }
    */
  }

  public ESRegexp(String pattern, String flags) throws ESException
  {
    super("RegExp", getPrototype());

    this.pattern = new ESString(pattern);
    this.flags = new ESString(flags);
    lastString = ESString.NULL;

    try {
      _regexp = Pattern.compile(pattern); // , flags
    } catch (Exception e) {
      throw new ESException("regexp: " + e.getMessage());
    }
  }

  protected ESRegexp() {}

  private static ESBase getPrototype()
  {
    Global resin = Global.getGlobalProto();
    if (resin == null)
      return null;
    else
      return resin.getRegexpProto();
  }

  private void setProps()
  {
    if (hasSetProps)
      return;

    int flags = READ_ONLY|DONT_DELETE;
    hasSetProps = true;

    // java.util.regex
    // put(GLOBAL, ESBoolean.create(_regexp.isGlobal())); // , flags);
    // put(IGNORE_CASE, ESBoolean.create(_regexp.ignoreCase())); // , flags);
    put(LAST_INDEX, ESNumber.create(lastIndex), DONT_DELETE);
    put(SOURCE, pattern, flags);
  }

  int getLastIndex() throws Throwable
  {
    if (! hasSetProps)
      return lastIndex;
    else
      return getProperty(LAST_INDEX).toInt32();
  }

  void setLastIndex(int index)
  {
    lastIndex = index;
    hasSetProps = false;
  }

  public ESBase getProperty(ESString key) throws Throwable
  {
    if (! hasSetProps)
      setProps();

    return super.getProperty(key);
  }

  public void setProperty(ESString key, ESBase value) throws Throwable
  {
    if (! hasSetProps)
      setProps();

    super.setProperty(key, value);
  }

  public ESBase delete(ESString key) throws Throwable
  {
    if (! hasSetProps)
      setProps();

    return super.delete(key);
  } 

  public Iterator keys() throws ESException
  {
    if (! hasSetProps)
      setProps();

    return super.keys();
  } 

  public ESString toSource(IntMap map, boolean isLoopPass) throws Throwable
  {
    if (isLoopPass)
      return null;
    else
      return toStr();
  }

  void compile(ESString pattern, ESString flags) throws ESException
  {
    if (! this.pattern.equals(pattern) || ! this.flags.equals(flags)) {
      this.pattern = pattern;
      this.flags = flags;

      try {
        // XXX: java.util.regex
        // _regexp = Pattern.parse(pattern.toString(), flags.toString());
      } catch (Exception e) {
        throw new ESException("regexp: " + e);
      }
    }

    lastIndex = 0;
    hasSetProps = false;
  }

  boolean exec(ESString string, boolean useGlobal) throws Throwable
  {
    return false;

    /* change to java.util.regex
    lastString = string;
    lastStart = getLastIndex();

    if (! useGlobal) {
      lastStart = 0;
      return _regexp.match(string.toString());
    }
    else if (regexp.match(string.toString(), lastStart)) {
      hasSetProps = false;
      lastIndex = 0;

      return false;
    } else {
      hasSetProps = false;
      lastIndex = regexp.getEnd(0);

      if (regexp.getBegin(0) == lastIndex) 
        lastIndex++;

      return true;
    }
    */
  }

  public Object toJavaObject()
  {
    // java.util.regex
    // return regexp;
    return null;
  }

  boolean exec(ESString string) throws Throwable
  {
    return false;
    // java.util.regex
    // return exec(string, regexp.isGlobal());
  }

  public ESBase call(Call call, int length) throws Throwable
  {
    call.setThis(this);

    return NativeRegexp.exec(call, length);
  }

   protected ESObject dup() { return new ESRegexp(); }
 
   protected void copy(Object newObj)
   {
     ESRegexp newRegexp = (ESRegexp) newObj;
 
     super.copy(newObj);
 
     newRegexp.pattern = pattern;
     newRegexp.flags = flags;
     newRegexp.lastString = lastString;
 
     // XXX: bogus
     try {
       // java.util.regex
       // newRegexp.regexp = new Regexp(pattern.toString(), flags.toString());
     } catch (Exception e) {
     }
   }
}
