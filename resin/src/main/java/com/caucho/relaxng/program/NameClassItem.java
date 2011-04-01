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

package com.caucho.relaxng.program;

import com.caucho.relaxng.RelaxException;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Matches names.
 */
abstract public class NameClassItem {
  protected final static L10N L = new L10N(NameClassItem.class);
  protected final static Logger log
    = Logger.getLogger(NameClassItem.class.getName());

  /**
   * Adds to the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
  }

  /**
   * Returns true if the name matches.
   */
  abstract public boolean matches(QName name);

  /**
   * Returns the pretty printed syntax.
   */
  abstract public String toSyntaxDescription(String prefix);

  /**
   * Throws an error.
   */
  protected RelaxException error(String msg)
  {
    return new RelaxException(msg);
  }
}

