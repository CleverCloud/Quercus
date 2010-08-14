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

package com.caucho.util;

import java.util.Locale;

/**
 * Locale generator.
 */
public class LocaleUtil {
  public static Locale createLocale(String s)
  {
    if (s == null)
      return null;
    
    int len = s.length();
    char ch = ' ';

    int i = 0;
    for (;
         i < len && ('a' <= (ch = s.charAt(i)) && ch <= 'z'
                     || 'A' <= ch && ch <= 'Z'
                     || '0' <= ch && ch <= '9');
         i++) {
    }

    String language = s.substring(0, i);
    String country = null;
    String var = null;

    if (ch == '-' || ch == '_') {
      int head = ++i;
      
      for (;
           i < len && ('a' <= (ch = s.charAt(i)) && ch <= 'z'
                       || 'A' <= ch && ch <= 'Z'
                       || '0' <= ch && ch <= '9');
           i++) {
      }
      
      country = s.substring(head, i);
    }

    if (ch == '-' || ch == '_') {
      int head = ++i;
      
      for (;
           i < len && ('a' <= (ch = s.charAt(i)) && ch <= 'z'
                       || 'A' <= ch && ch <= 'Z'
                       || '0' <= ch && ch <= '9');
           i++) {
      }
      
      var = s.substring(head, i);
    }

    if (var != null)
      return new Locale(language, country, var);
    else if (country != null)
      return new Locale(language, country);
    else
      return new Locale(language);
  }
}
