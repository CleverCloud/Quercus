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

import java.util.HashMap;
import java.util.Iterator;

class ESRegexpWrapper extends NativeWrapper {
  static ESId INPUT = ESId.intern("input");
  static ESId MULTILINE = ESId.intern("multiline");
  static ESId LAST_MATCH = ESId.intern("lastMatch");
  static ESId LAST_PAREN = ESId.intern("lastParen");
  static ESId LEFT_CONTEXT = ESId.intern("leftContext");
  static ESId RIGHT_CONTEXT = ESId.intern("rightContext");
  static ESId G1 = ESId.intern("$1");
  static ESId G2 = ESId.intern("$2");
  static ESId G3 = ESId.intern("$3");
  static ESId G4 = ESId.intern("$4");
  static ESId G5 = ESId.intern("$5");
  static ESId G6 = ESId.intern("$6");
  static ESId G7 = ESId.intern("$7");
  static ESId G8 = ESId.intern("$8");
  static ESId G9 = ESId.intern("$9");

  static HashMap aliases;

  static {
    aliases = new HashMap();

    aliases.put(ESId.intern("$_"), INPUT);
    aliases.put(ESId.intern("$`"), LEFT_CONTEXT);
    aliases.put(ESId.intern("$'"), RIGHT_CONTEXT);
    aliases.put(ESId.intern("$&"), LAST_MATCH);
    aliases.put(ESId.intern("$0"), LAST_MATCH);
    aliases.put(ESId.intern("$+"), LAST_PAREN);
  }

  ESRegexp regexp;

  boolean hasSetProps;
  
  ESRegexpWrapper(Global resin, Native fun, ESRegexp proto)
  {
    super(resin, fun, proto, ESThunk.REGEXP_THUNK);

    regexp = proto;
    hasSetProps = false;

    put(INPUT, ESString.NULL, DONT_DELETE);
    put(MULTILINE, ESBoolean.FALSE, DONT_DELETE);
  }

  protected ESRegexpWrapper()
  {
  }

  private void setProps()
  {
    /* convert to java.util.regex
    if (hasSetProps)
      return;

    int flags = READ_ONLY|DONT_DELETE;
    hasSetProps = true;

    ESString string = regexp.lastString;
    Regexp reg = regexp.regexp;

    put(LAST_MATCH, string.substring(reg.getBegin(0), reg.getEnd(0)), flags);
    put(G1, string.substring(reg.getBegin(1), reg.getEnd(1)), flags);
    put(G2, string.substring(reg.getBegin(2), reg.getEnd(2)), flags);
    put(G3, string.substring(reg.getBegin(3), reg.getEnd(3)), flags);
    put(G4, string.substring(reg.getBegin(4), reg.getEnd(4)), flags);
    put(G5, string.substring(reg.getBegin(5), reg.getEnd(5)), flags);
    put(G6, string.substring(reg.getBegin(6), reg.getEnd(6)), flags);
    put(G7, string.substring(reg.getBegin(7), reg.getEnd(7)), flags);
    put(G8, string.substring(reg.getBegin(8), reg.getEnd(8)), flags);
    put(G9, string.substring(reg.getBegin(9), reg.getEnd(9)), flags);

    if (reg.length() > 0)
      put(LAST_PAREN, string.substring(reg.getBegin(reg.length() - 1),
                                       reg.getEnd(reg.length() - 1)), flags);
    else
      put(LAST_PAREN, string.NULL, flags);

    if (regexp.lastStart >= string.length())
      put(LEFT_CONTEXT, ESString.NULL, flags);
    else
      put(LEFT_CONTEXT, string.substring(regexp.lastStart, reg.getBegin(0)), 
          flags);

    put(RIGHT_CONTEXT, string.substring(reg.getEnd(0)), flags);
    */
  }

  void setRegexp(ESRegexp regexp)
  {
    this.regexp = regexp;
    hasSetProps = false;
  }

  public ESBase getProperty(ESString key) throws Throwable
  {
    if (! hasSetProps)
      setProps();

    ESId alias = (ESId) aliases.get(key);
    return super.getProperty(alias != null ? alias : key);
  }

  public void setProperty(ESString key, ESBase value) throws Throwable
  {
    if (! hasSetProps && regexp != null)
      setProps();

    ESId alias = (ESId) aliases.get(key);
    super.setProperty(alias != null ? alias : key, value);
  }

  public ESBase delete(ESString key) throws Throwable
  {
    if (! hasSetProps)
      setProps();

    ESId alias = (ESId) aliases.get(key);
    return super.delete(alias != null ? alias : key);
  } 

  public Iterator keys() throws ESException
  {
    if (! hasSetProps)
      setProps();

    return super.keys();
  } 

  protected ESObject dup() { return new ESRegexpWrapper(); }

  protected void copy(Object obj)
  {
    ESRegexpWrapper wrap = (ESRegexpWrapper) obj;
 
    super.copy(obj);
 
    hasSetProps = false;
  }
}
