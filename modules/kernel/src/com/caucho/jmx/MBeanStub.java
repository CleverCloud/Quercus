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

package com.caucho.jmx;

import com.caucho.util.L10N;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Encapsulates the managed object.
 */
public class MBeanStub implements Serializable {
  private static final Logger log 
    = Logger.getLogger(MBeanStub.class.getName());
  private static final L10N L = new L10N(MBeanStub.class);

  private String _name;

  MBeanStub(String name)
  {
    _name = name;
  }

  MBeanStub()
  {
  }

  /**
   * Returns the admin name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Replaces the object.
   */
  public Object readResolve()
    throws ObjectStreamException
  {
    return null;
    /*
    // MBeanView view = Jmx.getLocalView();

    try {
      // XXX: return view.find(_name);
      return null;
    } catch (MalformedObjectNameException e) {
      throw new StubException(e);
    }
    */
  }

  static class StubException extends ObjectStreamException {
    Throwable _cause;
    
    StubException(Throwable e)
    {
      _cause = e;
    }

    public Throwable getCause()
    {
      return _cause;
    }
  }
}

