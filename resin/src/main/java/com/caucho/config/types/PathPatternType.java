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

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Abstract type building a path pattern.  The pattern follows ant.
 */
public class PathPatternType {
  static final L10N L = new L10N(PathPatternType.class);
  static final Logger log = Logger.getLogger(PathPatternType.class.getName());

  private Pattern _pattern;

  public PathPatternType()
  {
  }

  public PathPatternType(String pattern)
    throws ConfigException, PatternSyntaxException
  {
    setName(pattern);
  }

  /**
   * Sets the pattern name.
   */
  public void setName(String pattern)
    throws ConfigException, PatternSyntaxException
  {
    CharBuffer cb = new CharBuffer();

    cb.append("^");

    int i = 0;
    int length = pattern.length();

    while (i < length && pattern.charAt(i) == '/')
      i++;

    for (; i < length; i++) {
      char ch = pattern.charAt(i);

      if (ch == '/')
        cb.append('/');
      else if (ch != '*')
        cb.append(ch);
      else if (length <= i + 1 || pattern.charAt(i + 1) != '*')
        cb.append("[^/]*");
      else if (i > 0 && pattern.charAt(i - 1) != '/')
        throw new ConfigException(L.l("'{0}' is an invalid pattern at '**'",
                                      pattern));
      else if (i + 2 < length && pattern.charAt(i + 2) == '/') {
        cb.append("([^/]*/)*");
        i += 2;
      }
      else if (i + 2 < length)
        throw new ConfigException(L.l("'{0}' is an invalid pattern at '**'",
                                      pattern));
      else {
        cb.append(".*");
        i++;
      }
    }

    cb.append("$");

    _pattern = Pattern.compile(cb.toString());
  }

  /**
   * Sets the pattern name.
   */
  public void addText(String text)
    throws ConfigException, PatternSyntaxException
  {
    text = text.trim();

    if (! text.equals(""))
      setName(text);
  }

  /**
   * initialize the pattern.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_pattern == null)
      throw new ConfigException(L.l("pattern requires 'name' attribute."));
  }

  /**
   * Check for a match.
   */
  public boolean isMatch(String path)
  {
    return _pattern.matcher(path).matches();
  }

  public String toString()
  {
    return "PathPatternType[" + _pattern.pattern() + "]";
  }
}
