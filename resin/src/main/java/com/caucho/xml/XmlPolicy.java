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

package com.caucho.xml;

class XmlPolicy extends Policy {
  /**
   * Initialize the policy.
   */
  public void init()
  {
    super.init();

  }

  int openAction(XmlParser parser, QName node, QName next)
    throws XmlParseException
  {
    return PUSH;
  }

  /**
   * Returns the close action for the current tag.  For XML, the only
   * possible action is POP.
   */
  int elementCloseAction(XmlParser parser, QName name, String tagEnd)
    throws XmlParseException
  {
    String qname = name.getName();

    if (qname == "#document" && tagEnd.equals(""))
      return POP;
    else if (qname.equals(tagEnd))
      return POP;
    else {
      if (qname.equals("#document"))
        qname = L.l("end of document");
      else
        qname = "`</" + qname + ">'";
      if (tagEnd.equals(""))
        tagEnd = L.l("end of file");
      else
        tagEnd = "`</" + tagEnd + ">'";

      throw parser.error(L.l("expected {0} at {1} (open at {2})",
                             qname, tagEnd,
                             String.valueOf(parser.getNodeLine())));
    }
  }
}
