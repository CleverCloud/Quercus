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

package javax.xml.bind;
import javax.xml.namespace.QName;
import java.io.Serializable;

public class JAXBElement<T> implements Serializable {

  protected final Class<T> declaredType;

  protected final QName name;

  protected final Class scope;

  protected T value;

  protected boolean nil;

  public JAXBElement(QName name, Class<T> declaredType,
                     Class scope, T value)
  {
    this.name = name;
    this.declaredType = declaredType;
    this.scope = scope;
    this.value = value;
    this.nil = false;
  }

  public JAXBElement(QName name,
                     Class<T> declaredType, T value)
  {
    this(name, declaredType, JAXBElement.GlobalScope.class, value);
  }

  public Class<T> getDeclaredType()
  {
    return declaredType;
  }

  public QName getName()
  {
    return name;
  }

  public Class getScope()
  {
    return scope;
  }

  public T getValue()
  {
    return value;
  }

  public boolean isGlobalScope()
  {
    return scope == JAXBElement.GlobalScope.class;
  }

  public boolean isNil()
  {
    return nil;
  }

  public boolean isTypeSubstituted()
  {
    return !declaredType.isInstance(value);
  }

  public void setNil(boolean value)
  {
    nil = value;
  }

  public void setValue(T t)
  {
    value = t;
  }

  public static final class GlobalScope {
    public GlobalScope()
    {
    }
  }
}

