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
 * @author Emil Ong, Adam Megacz
 */

package com.caucho.jaxb.accessor;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;
import javax.xml.namespace.QName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.caucho.util.L10N;

/** an Accessor is either a getter/setter pair or a field */
public abstract class Accessor {
  public static final L10N L = new L10N(Accessor.class);

  protected int _order = -1;
  protected String _name;

  public void setOrder(int order)
  {
    _order = order;
  }

  public int getOrder()
  {
    return _order;
  }

  public boolean checkOrder(int order, ValidationEventHandler handler)
  {
    if (_order < 0 || _order == order)
      return true;

    ValidationEvent event = 
      new ValidationEventImpl(ValidationEvent.ERROR, 
                              L.l("ordering error"), 
                              new ValidationEventLocatorImpl());

    return handler.handleEvent(event);
  }

  public abstract Object get(Object o) throws JAXBException;
  public abstract void set(Object o, Object value) throws JAXBException;
  public abstract String getName();
  public abstract Class getType();
  public abstract Type getGenericType();
  public abstract Package getPackage();
  public abstract <A extends Annotation> A getAnnotation(Class<A> c);
  public abstract <A extends Annotation> A getPackageAnnotation(Class<A> c);
}
