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

package javax.servlet;

/**
 * The event class for changes to servlet context attributes.
 *
 * @since Servlet 2.3
 */
public class ServletContextAttributeEvent extends ServletContextEvent {
  // The name of the changed attribute
  private String name;
  // The value of the changed attribute
  private Object value;
  /**
   * Creates a ServletContextAttributeEvent for the changed application.
   *
   * @param application the servlet context that has changed.
   * @param name the name of the attribute that changed
   * @param value the value of the attribute that changed
   */
  public ServletContextAttributeEvent(ServletContext application,
                                      String name,
                                      Object value)
  {
    super(application);

    this.name = name;
    this.value = value;
  }

  /**
   * Returns the name of the attribute that changed.
   */
  public String getName()
  {
    return name;
  }
  /**
   * Returns the value of the attribute that changed.
   *
   * <table>
   * <tr><td>add<td>the new value
   * <tr><td>remove<td>the old value
   * <tr><td>replace<td>the old value
   * </table>
   */
  public Object getValue()
  {
    return value;
  }

  @Override 
  public String toString()
  {
    return (getClass().getName() 
            + "[name=" + this.name 
            + ",source=" + getSource() 
            + "]");
  }
}
