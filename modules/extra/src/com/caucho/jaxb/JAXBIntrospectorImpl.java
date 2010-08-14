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
* @author Adam Megacz
*/

package com.caucho.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;

import javax.xml.bind.annotation.XmlRootElement;

import javax.xml.namespace.QName;

class JAXBIntrospectorImpl extends JAXBIntrospector {

  private JAXBContextImpl _context;
  
  JAXBIntrospectorImpl(JAXBContextImpl context)
  {
    _context = context;
  }

  public QName getElementName(Object object)
  {
    if (! isElement(object))
      return null;

    try {
      if (object instanceof JAXBElement)
        return ((JAXBElement) object).getName();
      else
        return _context.getSkeleton(object.getClass()).getElementName();
    }
    catch (JAXBException e) {
      return null;
    }
  }

  public boolean isElement(Object object)
  {
    if (object == null)
      return false;

    if (object instanceof JAXBElement)
      return true;

    Class cl = object.getClass();

    if (! cl.isAnnotationPresent(XmlRootElement.class))
      return false;

    return _context.hasSkeleton(cl);
  }
}

