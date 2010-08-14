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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

public class SetRule
  extends AbstractRuleWithConditions
{
  private Boolean _requestSecure;
  private String _requestCharacterEncoding;
  private String _responseCharacterEncoding;
  private String _responseContentType;

  protected SetRule(RewriteDispatch rewriteDispatch)
  {
    super(rewriteDispatch);
  }

  public String getTagName()
  {
    return "set";
  }

  /**
   * Sets the character encoding of the request,
   * {@link javax.servlet.ServletRequest#setCharacterEncoding(String)}.
   */
  public void setRequestCharacterEncoding(String requestCharacterEncoding)
  {
    _requestCharacterEncoding = requestCharacterEncoding;
  }

  /**
   * Sets the security of the request,
   * {@link javax.servlet.ServletRequest#isSecure()}.
   */
  public void setRequestSecure(boolean requestSecure)
  {
    _requestSecure = requestSecure;
  }

  /**
   * Sets the character encoding of the response,
   * {@link javax.servlet.ServletResponse#setCharacterEncoding(String)}.
   */
  public void setResponseCharacterEncoding(String responseCharacterEncoding)
  {
    _responseCharacterEncoding = responseCharacterEncoding;
  }

  /**
   * Sets the content-type of the response,
   * {@link javax.servlet.ServletResponse#setContentType(String)}.
   */
  public void setResponseContentType(String responseContentType)
  {
    _responseContentType = responseContentType;
  }

  public FilterChain dispatch(String targetUri,
                              String queryString,
                              FilterChain accept,
                              FilterChainMapper next)
    throws ServletException
  {
    return new SetFilterChain(targetUri,
                              queryString,
                              accept,
                              next,
                              _requestCharacterEncoding,
                              _requestSecure,
                              _responseCharacterEncoding,
                              _responseContentType);

  }
}
