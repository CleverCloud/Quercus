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
 * @author Sam 
 */


package javax.portlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

public class ValidatorException extends PortletException
{
  private transient ArrayList _failed = new ArrayList();

  private ValidatorException()
  {
  }

  public ValidatorException(String msg, Collection failedKeys)
  {
    super(msg);

    if (failedKeys != null)
      _failed.addAll(failedKeys);
  }

  public ValidatorException(String msg,
                            Throwable cause,
                            Collection failedKeys)
  {
    super(msg, cause);

    if (failedKeys != null)
      _failed.addAll(failedKeys);
  }

  public ValidatorException(Throwable cause,
                            Collection failedKeys)
  {
    super(cause);

    if (failedKeys != null)
      _failed.addAll(failedKeys);
  }

  public Enumeration getFailedKeys()
  {
    return Collections.enumeration(_failed);
  }
}
