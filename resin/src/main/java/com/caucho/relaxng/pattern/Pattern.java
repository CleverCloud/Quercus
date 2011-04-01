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

package com.caucho.relaxng.pattern;

import com.caucho.relaxng.RelaxException;
import com.caucho.relaxng.program.Item;
import com.caucho.relaxng.program.NameClassItem;
import com.caucho.util.L10N;

/**
 * Relax pattern
 */
abstract public class Pattern {
  protected static final L10N L = new L10N(Pattern.class);
  
  private Pattern _parent;
  private String _elementName;

  private String _filename;
  private int _line;

  /**
   * Returns the relax config name.
   */
  public String getTagName()
  {
    return getClass().getName();
  }

  /**
   * Sets the pattern source location.
   */
  public void setFilename(String filename)
  {
    _filename = filename;
  }

  /**
   * Sets the pattern line
   */
  public void setLine(int line)
  {
    _line = line;
  }

  /**
   * Gets the location.
   */
  public String getLocation()
  {
    if (_filename != null)
      return _filename + ":" + _line;
    else if (_parent != null)
      return _parent.getLocation();
    else
      return null;
  }
  
  /**
   * Returns the element-name.
   */
  public String getElementName()
  {
    return _elementName;
  }
  
  /**
   * Sets the element-name.
   */
  public void setElementName(String elementName)
  {
    _elementName = elementName;
  }
  
  /**
   * Sets the parent.
   */
  public void setParent(Pattern parent)
    throws RelaxException
  {
    _parent = parent;
  }

  /**
   * Gets the parent.
   */
  public Pattern getParent()
  {
    return _parent;
  }

  /**
   * Returns true if it contains a data element.
   */
  public boolean hasData()
  {
    return false;
  }

  /**
   * Returns true if it contains an element.
   */
  public boolean hasElement()
  {
    return false;
  }

  /**
   * Adds a name child.
   */
  public void addNameChild(NameClassPattern child)
    throws RelaxException
  {
    throw new RelaxException(L.l("<{0}> is not an allowed child for <{1}>.",
                                 child.getTagName(), getTagName()));
  }

  /**
   * Adds an element child.
   */
  public void addChild(Pattern child)
    throws RelaxException
  {
    throw new RelaxException(L.l("<{0}> is not an allowed child for <{1}>.",
                                 child.getTagName(), getTagName()));
  }

  /**
   * Ends the element.
   */
  public void endElement()
    throws RelaxException
  {
  }

  /**
   * Creates the current state
   */
  public Item createItem(GrammarPattern grammar)
    throws RelaxException
  {
    throw new RelaxException(L.l("item isn't allowed in `{0}'.",
                                 getClass().getName()));
  }

  /**
   * Creates the name program
   */
  public NameClassItem createNameItem()
    throws RelaxException
  {
    throw new RelaxException(L.l("name-item isn't allowed in `{0}'.",
                                 getClass().getName()));
  }

  abstract public boolean equals(Object o);

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    return "unknown";
  }
  
  /**
   * creates an error.
   */
  public RelaxException error(String msg)
  {
    String location = getLocation();

    if (location != null)
      return new RelaxException(location + ": " + msg);
    else
      return new RelaxException(msg);
  }
}
