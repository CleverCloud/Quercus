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

import java.io.IOException;
import java.util.ArrayList;

/**
 * An expression of the DTD grammar.
 */
class QContentParticle {
  ArrayList<Object> _children = new ArrayList<Object>();
  int _separator;
  int _repeat;

  void addChild(Object child)
  {
    _children.add(child);
  }

  /**
   * Returns the number of children.
   */
  public int getChildSize()
  {
    return _children.size();
  }
  /**
   * Returns the child specified by the index.
   */
  public Object getChild(int index)
  {
    return _children.get(index);
  }
  /**
   * Sets the child specified by the index.
   */
  public void setChild(int index, Object child)
  {
    _children.set(index, child);
  }

  public void print(XmlPrinter os) throws IOException
  {
    os.print("(");

    for (int i = 0; i < _children.size(); i++) {
      if (i != 0) {
        if (_separator == ',')
          os.print(", ");
        else {
          os.print(" ");
          os.print((char) _separator);
          os.print(" ");
        }
      }

      Object child = _children.get(i);

      if (child instanceof QContentParticle)
        ((QContentParticle) child).print(os);
      else
        os.print(String.valueOf(child));
    }

    os.print(")");
    if (_repeat != 0)
      os.print((char) _repeat);
  }
}
