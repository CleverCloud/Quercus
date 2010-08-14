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

package javax.servlet.jsp.tagext;

import java.util.Map;

/**
 * Abstract class for a JSP page validator.  The validator works on the
 * XML version of the page.
 */
public abstract class TagLibraryValidator {
  private Map<String, Object> _initParameters;
  /**
   * Zero-arg constructor for the validator.
   */
  public TagLibraryValidator()
  {
  }

  /**
   * Returns an map of the init parameters specified in the .tld.
   */
  public java.util.Map<String, Object> getInitParameters()
  {
    return _initParameters;
  }

  /**
   * Sets the map of the init parameters specified in the .tld.
   */
  public void setInitParameters(java.util.Map<String, Object> initParameters)
  {
    _initParameters = initParameters;
  }

  /**
   * Validate the page.  This will be called once per directive.
   *
   * @param prefix the value of the directive's prefix.
   * @param uri the value of the directive's URI.
   * @param data the PageData representing the page.
   *
   * @return an array of validation messages
   */
  public ValidationMessage []validate(String prefix,
                                      String uri,
                                      PageData data)
  {
    return null;
  }

  /**
   * Release any data stored by the validator.
   */
  public void release()
  {
    _initParameters = null;
  }
}
