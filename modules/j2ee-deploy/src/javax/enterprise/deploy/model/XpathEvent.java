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

package javax.enterprise.deploy.model;

import java.beans.PropertyChangeEvent;

/**
 * Listener for XpathEvents.
 */
public final class XpathEvent {
  public static final Object BEAN_ADDED = new Object();
  public static final Object BEAN_REMOVED = new Object();
  public static final Object BEAN_CHANGED = new Object();

  private DDBean _bean;
  private Object _type;

  private PropertyChangeEvent _event;

  public XpathEvent(DDBean bean, Object type)
  {
    _bean = bean;
    _type = type;
  }

  /**
   * Returns the change event.
   */
  public PropertyChangeEvent getChangeEvent()
  {
    return _event;
  }

  /**
   * Sets the change event.
   */
  public void setChangeEvent(PropertyChangeEvent event)
  {
    _event = event;
  }

  /**
   * Returns the changing bean.
   */
  public DDBean getBean()
  {
    return _bean;
  }

  /**
   * Returns true if this is an add event.
   */
  public boolean isAddEvent()
  {
    return _type == BEAN_ADDED;
  }

  /**
   * Returns true if this is a remove event.
   */
  public boolean isRemoveEvent()
  {
    return _type == BEAN_REMOVED;
  }

  /**
   * Returns true if this is a change event.
   */
  public boolean isChangeEvent()
  {
    return _type == BEAN_CHANGED;
  }
}

