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
 * @author Emil Ong
 */

package com.caucho.xml.schema;

import java.util.*;
import static javax.xml.XMLConstants.*;
import javax.xml.bind.annotation.*;

/**
 * JAXB annotated Schema data structure.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="sequence", namespace=W3C_XML_SCHEMA_NS_URI)
public class Sequence {
  @XmlTransient
  private Schema _schema;

  @XmlAttribute(name="name")
  private String _name;

  @XmlElements({
      @XmlElement(name="element", 
                  namespace=W3C_XML_SCHEMA_NS_URI,
                  type=com.caucho.xml.schema.Element.class),
      @XmlElement(name="annotation",
                  namespace=W3C_XML_SCHEMA_NS_URI,
                  type=com.caucho.xml.schema.Annotation.class)})
  private List<Object> _contents;

  public List<Object> getContents()
  {
    if (_contents == null)
      _contents = new ArrayList<Object>();

    return _contents;
  }

  @XmlTransient
  public Schema getSchema()
  {
    return _schema;
  }

  public void setSchema(Schema schema)
  {
    _schema = schema;

    if (_contents != null) {
      for (int i = 0; i < _contents.size(); i++) { 
        Object content = _contents.get(i);

        if (content instanceof Element) {
          Element element = (Element) content;
          element.setSchema(_schema);
        }
      }
    }
  }
}
