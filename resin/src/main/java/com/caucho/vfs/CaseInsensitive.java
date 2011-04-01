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

package com.caucho.vfs;

import com.caucho.loader.EnvironmentLocal;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * Configuration for CaseInsensitive environments.
 */
public class CaseInsensitive {
  private final static EnvironmentLocal<Boolean> _caseInsensitive =
  new EnvironmentLocal<Boolean>("caucho.vfs.case-insensitive");
  
  private boolean _isCaseInsensitive = true;

  public CaseInsensitive()
  {
  }

  /**
   * Returns true if the local environment is case sensitive.
   */
  public static boolean isCaseInsensitive()
  {
    Boolean value = _caseInsensitive.get();

    if (value == null)
      return File.separatorChar == '\\';
    else
      return value.booleanValue();
  }

  /**
   * Sets true if case sensitive.
   */
  public void setValue(boolean isInsensitive)
  {
    _isCaseInsensitive = isInsensitive;
  }

  /**
   * Init.
   */
  @PostConstruct
  public void init()
  {
    _caseInsensitive.set(new Boolean(_isCaseInsensitive));
  }
}
