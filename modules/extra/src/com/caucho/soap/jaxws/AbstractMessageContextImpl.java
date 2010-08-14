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
* @author Emil Ong
*/

package com.caucho.soap.jaxws;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.bind.JAXBContext;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;

import javax.xml.ws.*;
import javax.xml.ws.handler.*;

import com.caucho.util.L10N;

public abstract class AbstractMessageContextImpl extends HashMap<String,Object>
                                                 implements MessageContext 
{
  private final Map<String,Scope> _scopes = new HashMap<String,Scope>();

  protected AbstractMessageContextImpl()
  {
  }

  public Scope getScope(String name)
  {
    Scope scope = _scopes.get(name);

    if (scope == null)
      throw new IllegalArgumentException();

    return scope;
  }

  public void setScope(String name, Scope scope)
  {
    if (! containsKey(name))
      throw new IllegalArgumentException();

    _scopes.put(name, scope);
  }

  public void clear()
  {
    super.clear();
    _scopes.clear();
  }

  public Object put(String key, Object value)
  {
    Object previous = super.put(key, value);

    if (previous == null)
      _scopes.put(key, Scope.HANDLER);

    return previous;
  }

  public void putAll(Map<? extends String,? extends Object> m)
  {
    if (m instanceof MessageContext) { 
      MessageContext context = (MessageContext) m;

      for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
        super.put(entry.getKey(), entry.getValue());
        setScope(entry.getKey(), context.getScope(entry.getKey()));
      }
    }
    else {
      for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet())
        put(entry.getKey(), entry.getValue());
    }
  }

  public void putAll(Map<? extends String,? extends Object> m, Scope scope)
  {
    for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
      super.put(entry.getKey(), entry.getValue());
      setScope(entry.getKey(), scope);
    }
  }

  public Map<String,Object> getScopedSubMap(Scope scope)
  {
    Map<String,Object> map = new HashMap<String,Object>();

    for (Map.Entry<? extends String, ? extends Object> entry : entrySet()) {
      if (scope.equals(getScope(entry.getKey())))
        map.put(entry.getKey(), entry.getValue());
    }

    return map;
  }

  public Object remove(Object key)
  {
    Object previous = super.remove(key);

    if (previous != null)
      _scopes.remove(key);

    return previous;
  }
}
