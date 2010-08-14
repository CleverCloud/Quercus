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

package javax.faces.convert;

import java.util.*;
import java.text.*;

import javax.faces.context.*;
import javax.faces.component.*;

class Util
{
  public static String l10n(FacesContext context, String id,
                            String defaultMessage, Object ... args)
  {
    String message = defaultMessage;
    
    StringBuilder sb = new StringBuilder();

    int len = message.length();
    for (int i = 0; i < len; i++) {
      char ch = message.charAt(i);

      if (ch == '{' && i + 2 < len
          && '0' <= message.charAt(i + 1)
          && message.charAt(i + 1) <= '9'
          && message.charAt(i + 2) == '}') {
        int index = message.charAt(i + 1) - '0';

        if (index < args.length)
          sb.append(args[index]);

        i += 2;
      }
      else
        sb.append(ch);
    }
    
    return sb.toString();
  }

  public static String getLabel(FacesContext context, UIComponent component)
  {
    String label = (String) component.getAttributes().get("label");

    if (label != null && ! "".equals(label))
      return label;
    else
      return component.getClientId(context);
  }
}
