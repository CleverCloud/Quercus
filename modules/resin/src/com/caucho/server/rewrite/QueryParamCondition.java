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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.i18n.CharacterEncoding;
import com.caucho.server.http.Form;
import com.caucho.util.HashMapImpl;
import com.caucho.vfs.Encoding;

/**
* A rewrite condition that passes if the value of a query parameter exists
 * and matches a regular expression.
*/
public class QueryParamCondition
  extends AbstractCondition
{
  private static final Logger log
    = Logger.getLogger(QueryParamCondition.class.getName());
  
  private final String _param;
  private Pattern _regexp;
  private boolean _caseInsensitive;

  QueryParamCondition(String param)
  {
    _param = param;
  }
  
  public String getTagName()
  {
    return "query-param";
  }

  public void setRegexp(Pattern pattern)
  {
    _regexp = pattern;
  }

  public void setCaseInsensitive(boolean caseInsensitive)
  {
    _caseInsensitive = caseInsensitive;
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
    String query = request.getQueryString();

    if (query == null)
      return false;
    
    String charEncoding = request.getCharacterEncoding();
    
    if (charEncoding == null)
      charEncoding = CharacterEncoding.getLocalEncoding();

    String javaEncoding = Encoding.getJavaName(charEncoding);

    Form formParser = new Form();
    HashMapImpl<String,String[]> form = new HashMapImpl<String,String[]>();

    try {
      formParser.parseQueryString(form, query, javaEncoding, true);
    } catch (java.io.IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
    
    String []value = form.get(_param);

    if (value == null || value.length == 0)
      return false;
    else
      return _regexp == null || _regexp.matcher(value[0]).find();
  }
}
