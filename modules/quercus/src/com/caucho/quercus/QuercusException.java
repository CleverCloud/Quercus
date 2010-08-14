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

package com.caucho.quercus;

import java.util.*;
import java.lang.reflect.*;

/**
 * Parent of PHP exceptions
 */
public class QuercusException extends RuntimeException
{
  private ArrayList<String> _quercusStackTrace;
  
  public QuercusException()
  {
  }

  public QuercusException(String msg)
  {
    super(msg);
  }

  public QuercusException(Throwable cause)
  {
    super(cause);
  }

  public QuercusException(String msg, Throwable cause)
  {
    super(msg, cause);
  }

  public static QuercusException create(Throwable e,
                                        ArrayList<String> stackTrace)
  {
    QuercusException qExn;
    
    if (e instanceof QuercusException)
      qExn = (QuercusException) e;
    else {
      if (e instanceof InvocationTargetException && e.getCause() != null)
        e = e.getCause();
      
      qExn = new QuercusException(e);
    }

    if (qExn.getQuercusStackTrace() == null)
      qExn.setQuercusStackTrace(stackTrace);

    return qExn;
  }

  public String getMessage()
  {
    String msg = super.getMessage();
    
    if (_quercusStackTrace != null) {
      StringBuilder sb = new StringBuilder();
      sb.append(msg);
      sb.append("\n");

      for (int i = 0; i < _quercusStackTrace.size(); i++) {
        sb.append("   " + _quercusStackTrace.get(i) + "\n");
      }

      return sb.toString();
    }
    else
      return msg;
  }

  public ArrayList<String> getQuercusStackTrace()
  {
    return _quercusStackTrace;
  }

  public void setQuercusStackTrace(ArrayList<String> stackTrace)
  {
    _quercusStackTrace = stackTrace;
  }
}
