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
 * @author Scott Ferguson
 */

package com.caucho.jsf.html;

import java.io.*;
import java.util.*;

import javax.faces.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

/**
 * The HTML text renderer
 */
abstract class HtmlRenderer extends BaseRenderer
{
  protected static void escapeText(ResponseWriter out,
                                   String text,
                                   String prop)
    throws IOException
  {
    int length = text.length();

    for (int i = 0; i < length; i++) {
      char ch = text.charAt(i);

      switch (ch) {
      case '<':
        out.writeText("&lt;", prop);
        break;
      case '>':
        out.writeText("&gt;", prop);
        break;
      case '&':
        out.writeText("&amp;", prop);
        break;
      default:
        out.write(ch);
        break;
      }
    }
  }
}
