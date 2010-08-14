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

package javax.servlet.jsp.jstl.core;

import javax.el.*;
import java.util.*;
import java.lang.reflect.Array;

public final class IteratedExpression {
  protected final ValueExpression orig;
  protected final String delims; // XXX: needs test, equals/hashcode

  public IteratedExpression(ValueExpression orig, String delims)
  {
    this.orig = orig;
    this.delims = delims;
  }

  public ValueExpression getValueExpression()
  {
    return this.orig;
  }

  public Object getItem(ELContext context, int i)
  {
    Object items = this.orig.getValue(context);

    if (items == null)
      return null;
    else if (items instanceof List) {
      List list = (List) items;

      if (i >= 0 && i < list.size())
        return list.get(i);
      else
        return null;
    }
    else if (items instanceof Iterable) {
      Iterator iter = ((Iterable) items).iterator();
      Object value = null;

      while (i-- >= 0) {
        if (! iter.hasNext())
          return null;

        value = iter.next();
      }

      return value;
    }
    else if (items.getClass().isArray())
      return Array.get(items, i);
    else if (items instanceof Iterator) {
      Iterator iter = (Iterator) items;
      Object value = null;

      while (i-- >= 0) {
        if (! iter.hasNext())
          return null;

        value = iter.next();
      }

      return value;
    }
    else if (items instanceof Enumeration) {
      Enumeration e = (Enumeration) items;
      Object value = null;

      while (i-- >= 0) {
        if (! e.hasMoreElements())
          return null;

        value = e.nextElement();
      }

      return value;
    }
    else if (items instanceof Map) {
      Iterator iter = ((Map) items).entrySet().iterator();
      Object value = null;

      while (i-- >= 0) {
        if (! iter.hasNext())
          return null;

        value = iter.next();
      }

      return value;
    } else if (items instanceof String) {
      StringTokenizer tokenizer;
      String value = null;

      if (delims == null)
        tokenizer = new StringTokenizer((String) items);
      else
        tokenizer = new StringTokenizer((String) items, delims);

      while (i-- >= 0) {
        if (! tokenizer.hasMoreTokens())
          return null;

        value = tokenizer.nextToken().trim();
      }

      return value;
    }
    else
      throw new IllegalStateException("unknown items value " + items);
  }

  public int hashCode()
  {
    return this.orig.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof IteratedExpression))
      return false;

    IteratedExpression expr = (IteratedExpression) o;

    return this.orig.equals(expr.orig);
  }
}
