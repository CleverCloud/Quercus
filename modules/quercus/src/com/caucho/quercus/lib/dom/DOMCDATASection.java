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

import org.w3c.dom.CDATASection;

public class DOMCDATASection
  extends DOMCharacterData<CDATASection>
{
  DOMCDATASection(DOMImplementation impl, CDATASection delegate)
  {
    super(impl, delegate);
  }

  public String getWholeText()
  {
    return _delegate.getWholeText();
  }

  public boolean isElementContentWhitespace()
  {
    return _delegate.isElementContentWhitespace();
  }

  public boolean isWhitespaceInElementContent()
  {
    return _delegate.isElementContentWhitespace();
  }

  public DOMText replaceWholeText(String content)
    throws DOMException
  {
    try {
      return wrap(_delegate.replaceWholeText(content));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }

  public DOMText splitText(int offset)
    throws DOMException
  {
    try {
      return wrap(_delegate.splitText(offset));
    }
    catch (org.w3c.dom.DOMException ex) {
      throw wrap(ex);
    }
  }
}
