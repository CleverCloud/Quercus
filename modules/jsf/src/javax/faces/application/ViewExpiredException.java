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

package javax.faces.application;

import javax.faces.FacesException;

public class ViewExpiredException extends FacesException
{
  private String _viewId;
  
  public ViewExpiredException()
  {
  }

  public ViewExpiredException(String viewId)
  {
    _viewId = viewId;
  }

  public ViewExpiredException(String msg, String viewId)
  {
    super(msg);
    
    _viewId = viewId;
  }

  public ViewExpiredException(String msg, Throwable cause, String viewId)
  {
    super(msg, cause);

    _viewId = viewId;
  }

  public ViewExpiredException(Throwable cause, String viewId)
  {
    super(cause);

    _viewId = viewId;
  }

  public String getViewId()
  {
    return _viewId;
  }

  public String getMessage()
  {
    String msg = super.getMessage();

    if (_viewId != null)
      return _viewId + ": " + msg;
    else
      return msg;
  }
}

  

  
  
