/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

package com.caucho.jsp.java;

import com.caucho.jsp.JspParseException;


public class JstlTlvXmlChoose
  extends CustomTag
{
  private boolean _hasWhen = false;
  private boolean _hasOtherwise = false;

  public void addChild(JspNode node)
    throws JspParseException
  {
    if (node instanceof JstlTlvXmlWhen) {
      _hasWhen = true;

      if (_hasOtherwise)
        throw node.error(L.l("<{0}:when> is not allowed after <{0}:otherwise>",
                        getQName().getPrefix()));
    }
    else if (node instanceof JstlTlvXmlOtherwise) {
      _hasOtherwise = true;
    }
    else if (node instanceof CustomTag)
      throw node.error(L.l(
        "<{0}> is not allowed as a child of <{1}>.  Only <{2}:when> and <{2}:otherwise> are allowed children.",
        node.getTagName(),
        getTagName(),
        getQName().getPrefix()));

    super.addChild(node);
  }

  public void endElement()
    throws Exception
  {
    if (!_hasWhen)
      throw error(L.l("<{0}> must have at least one <{1}:when> clause.",
                      getTagName(), getQName().getPrefix()));

    super.endElement();
  }
}