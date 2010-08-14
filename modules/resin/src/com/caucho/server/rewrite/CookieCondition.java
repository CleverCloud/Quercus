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

package com.caucho.server.rewrite;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.annotation.PostConstruct;
import java.util.regex.Pattern;

/**
 * A rewrite condition that passes if a named cookie exists and has a value
 * that matches a regular expression.
 */
public class CookieCondition
  extends AbstractCondition
{
  private String _name;
  private Pattern _regexp;
  private boolean _caseInsensitive;
  private boolean _sendVary = true;

  CookieCondition(String name)
  {
    _name = name;
  }

  public void setCaseInsensitive(boolean caseInsensitive)
  {
    _caseInsensitive = caseInsensitive;
  }

  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }

  public void setSendVary(boolean sendVary)
  {
    _sendVary = sendVary;
  }

  public String getTagName()
  {
    return "cookie";
  }

  @PostConstruct
  public void init()
  {
    if (_regexp != null && _caseInsensitive)
      _regexp = Pattern.compile(_regexp.pattern(), Pattern.CASE_INSENSITIVE);
  }

  public boolean isMatch(HttpServletRequest request,
                         HttpServletResponse response)
  {
    if (_sendVary)
      addHeaderValue(response, "Vary", "Cookie");

    Cookie[] cookies = request.getCookies();

    if (cookies != null) {
      for (int i = 0; i < cookies.length; i++) {
        Cookie cookie = cookies[i];

        if (cookie.getName().equals(_name)) {
          return _regexp == null || _regexp.matcher(cookie.getValue()).find();
        }
      }
    }

    return false;
  }
}
