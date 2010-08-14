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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib.regexp;

import com.caucho.quercus.env.StringValue;

class PeekString extends PeekStream {
  CharSequence _string;
  int _length;
  int _index;

  PeekString(CharSequence string)
  {
    _string = string;
    _length = string.length();
    _index = 0;
  }

  int read() 
  { 
    if (_index < _length)
      return _string.charAt(_index++);
    else
      return -1; 
  }

  int peek() 
  {
    if (_index < _length)
      return _string.charAt(_index);
    else
      return -1; 
  }

  void ungetc(int ch) { 
    if (_index <= 0)
      throw new RuntimeException();

    _index--;
  }

  StringValue createStringBuilder()
  {
    return ((StringValue) _string).createStringBuilder();
  }

  @Override
  public String getPattern()
  {
    return "/" + _string + "/";
  }

  public String toString()
  {
    return "PeekString[" + _string + "]";
  }
}





