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

package com.caucho.jaxb.mapping;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.accessor.Accessor;
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Node;

import java.util.Map;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.JAXBException;

import javax.xml.namespace.QName;

public abstract class SingleQNameXmlMapping extends XmlMapping {
  private static final L10N L = new L10N(SingleQNameXmlMapping.class);

  protected QName _qname;

  protected SingleQNameXmlMapping(JAXBContextImpl context, Accessor accessor)
  {
    super(context, accessor);
  }

  public void putQNames(Map<QName,XmlMapping> map)
    throws JAXBException
  {
    if (_qname != null) {
      if (map.containsKey(_qname))
        throw new JAXBException(L.l("Class contains two elements with the same QName {0}", _qname));

      map.put(_qname, (XmlMapping) this);
    }
  }

  public QName getQName(Object obj)
    throws JAXBException
  {
    return _qname;
  }
}
