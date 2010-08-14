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

/**
 * TagVariableInfo objects create runtime variables available to the tags.
 *
 * <p/>For example, a NESTED variable could be used like:
 *
 * <code><pre>
 * &lt;mytag:loop name='i' min='0' max='10'>
 *   Iter: &lt;%= i %>
 * &lt;/mytag:loop>
 * </pre></code>
 *
 * @since JSP 1.2
 */
public class TagVariableInfo {
  private String _nameGiven;
  private String _nameFromAttribute;
  private String _className;
  private boolean _declare;
  private int _scope;

  /**
   * Creates information for a variable.
   *
   * @param nameGiven name of the variable
   * @param nameFromAttribute name of the variable
   * @param className the java classname of the variable
   * @param declare true if the variable should be declared
   * @param scope the scope of the variable
   */
  public TagVariableInfo(String nameGiven, String nameFromAttribute,
                         String className, boolean declare, int scope)
  {
    _nameGiven = nameGiven;
    _nameFromAttribute = nameFromAttribute;
    _className = className;
    _declare = declare;
    _scope = scope;
  }

  /**
   * Returns the variable name, if it's static.
   */
  public String getNameGiven()
  {
    return _nameGiven;
  }

  /**
   * Returns the attribute name that will contain the variable name
   */
  public String getNameFromAttribute()
  {
    return _nameFromAttribute;
  }

  /**
   * Returns the variable's Java class.
   */
  public String getClassName()
  {
    return _className;
  }

  /**
   * True if the variable should be declared.  If false, the JSP engine
   * assumes the variable is already declared and just assigns the value.
   */
  public boolean getDeclare()
  {
    return _declare;
  }

  /**
   * Returns the variable's scope.
   *
   * <ul>
   * <li>AT_BEGIN - available as soon as the tag starts
   * <li>NESTED - only available in the tag body
   * <li>AT_END - only available after the tag ends.
   */
  public int getScope()
  {
    return _scope;
  }
}
