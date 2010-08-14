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

package com.caucho.jsp.cfg;

import java.util.ArrayList;

/**
 * Configuration for the taglib attribute in the .tld
 */
public class TldFragmentAttribute {
  private String _name;
  private boolean _required;
  private String _description;
  private ArrayList _fragmentInputList;

  /**
   * Sets the attribute name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the attribute name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets true if the attribute is required.
   */
  public void setRequired(boolean required)
  {
    _required = required;
  }

  /**
   * Returns true if the attribute is required.
   */
  public boolean getRequired()
  {
    return _required;
  }

  /**
   * Sets the description
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Returns the description
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * Adds a fragment-input
   */
  public void setFragmentInput(TldFragmentInput fragmentInput)
  {
    _fragmentInputList.add(fragmentInput);
  }

  /**
   * Returns the description
   */
  public ArrayList getFragmentInputList()
  {
    return _fragmentInputList;
  }
}
