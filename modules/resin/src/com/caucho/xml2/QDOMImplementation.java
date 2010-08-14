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

package com.caucho.xml2;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

public class QDOMImplementation implements DOMImplementation, java.io.Serializable {
  public boolean hasFeature(String feature, String version)
  {
    if (feature.equalsIgnoreCase("xml") &&
        (version.equals("2.0") || version.equals("1.0") ||
          version == null || version.equals("")))
      return true;
    else if (feature.equalsIgnoreCase("Core") &&
             (version.equals("2.0") || version.equals("1.0") ||
              version == null || version.equals("")))
      return true;
    else
      return false;
  }
  
  public Object getFeature(String feature, String version)
  {
    return null;
  }

  public DocumentType createDocumentType(String name,
                                         String publicId,
                                         String systemId)
  {
    QDocumentType type = new QDocumentType(name);
    type.setPublicId(publicId);
    type.setSystemId(systemId);

    return type;
  }

  public Document createDocument(String namespaceURI, String name,
                                 DocumentType docType)
  {
    QDocument doc = new QDocument(this);
    doc.setDoctype(docType);

    Element elt = doc.createElementNS(namespaceURI, name);
    doc.appendChild(elt);

    return doc;
  }

  // DOM LEVEL 3

  /**
   * Returns specialized implementation interfaces, e.g. MATHML.
   */
  public DOMImplementation getInterface(String feature)
  {
    return null;
  }
}
