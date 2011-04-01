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

package com.caucho.naming;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QBindingEnumeration implements NamingEnumeration {
  private static final Logger log
    = Logger.getLogger(QBindingEnumeration.class.getName());
  private static final L10N L = new L10N(QBindingEnumeration.class);

  private ContextImpl _context;
  private List _list;
  private int _index;

  QBindingEnumeration(ContextImpl context, List list)
  {
    _context = context;
    _list = list;
  }

  public boolean hasMore()
  {
    return _index < _list.size();
  }

  public Binding next()
    throws NamingException
  {
    String name = (String) _list.get(_index++);
    
    Object obj = _context.lookup(name);

    return new Binding(name, obj, true);
  }

  public boolean hasMoreElements()
  {
    return hasMore();
  }

  public Object nextElement()
  {
    try {
      return next();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    }
  }

  public void close()
  {
  }

  public String toString()
  {
    CharBuffer cb = new CharBuffer();

    ArrayList list = new ArrayList(_list);
    Collections.sort(list);
    
    cb.append("QBindingEnumeration[");
    for (int i = 0; i < list.size(); i++) {
      String name = (String) list.get(i);
      
      if (i != 0)
        cb.append(" ");
      
      try {
        String valueName = String.valueOf(_context.lookup(name));

        // server/158a, for QA comparison
        if (valueName.indexOf('@') >= 0)
          valueName = valueName.substring(0, valueName.indexOf('@'));

        cb.append("{" + name + ", " + valueName + "}");
      } catch (Exception e) {
        cb.append("{" + name + ", " + e + "}");
      }
    }
    cb.append("]");

    return cb.toString();
  }
}
