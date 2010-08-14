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
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import org.w3c.dom.DocumentType;

public class DOMDocumentType
  extends DOMNode<DocumentType>
{
  DOMDocumentType(DOMImplementation impl, DocumentType delegate)
  {
    super(impl, delegate);
  }

  public DOMNamedNodeMap getEntities()
  {
    return wrap(_delegate.getEntities());
  }

  public String getInternalSubset()
  {
    return _delegate.getInternalSubset();
  }

  public String getName()
  {
    return _delegate.getName();
  }

  public DOMNamedNodeMap getNotations()
  {
    return wrap(_delegate.getNotations());
  }

  public String getPublicId()
  {
    return _delegate.getPublicId();
  }

  public String getSystemId()
  {
    return _delegate.getSystemId();
  }
}
