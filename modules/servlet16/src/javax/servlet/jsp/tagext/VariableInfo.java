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
 * TagExtraInfo classes generate VariableInfo objects to create
 * runtime variables available to the tags.
 *
 * <p/>For example, a NESTED variable could be used like:
 *
 * <code><pre>
 * &lt;mytag:loop name='i' min='0' max='10'>
 *   Iter: &lt;%= i %>
 * &lt;/mytag:loop>
 * </pre></code>
 */
public class VariableInfo {
  /**
   * Constant for nested scope.  A nested variable is only alive
   * inside a tag's body:
   *
   * <code><pre>
   * tag1.doInitBody();
   * do {
   *   int foo = Integer.intValue(pageContext.getAttribute("foo"));
   *   ...
   * } while (tag1.doAfterBody() == EVAL_BODY_TAG)
   * </pre></code>
   */
  public final static int NESTED = 0;
  /**
   * Constant for variables initialized at the beginning of a tag.
   * These variables are initialized after doStartTag()
   *
   * <code><pre>
   * int _tmp = tag1.doStartTag();
   * int foo = Integer.intValue(pageContext.getAttribute("foo"));
   * if (_tmp == EVAL_BODY_INCLUDE) {
   *   ...
   * }
   * </pre></code>
   */
  public final static int AT_BEGIN = 1;
  /**
   * Constant for variables initialized at the end of a tag.
   * These variables are initialized after doEndTag()
   *
   * <code><pre>
   * int _tmp = tag1.doStartTag();
   * if (_tmp == EVAL_BODY_INCLUDE) {
   *   ...
   * }
   * tag1.doEndTag();
   * int foo = Integer.intValue(pageContext.getAttribute("foo"));
   * </pre></code>
   */
  public final static int AT_END = 2;

  private String varName;
  private String className;
  private boolean declare;
  private int scope;

  /**
   * Creates information for a variable.  Generally called from a
   * TagExtraInfo class.
   *
   * @param varName name of the variable
   * @param className the java classname of the variable
   * @param declare true if the variable should be declared
   * @param scope the scope of the variable
   */
  public VariableInfo(String varName, String className,
                      boolean declare, int scope)
  {
    this.varName = varName;
    this.className = className;
    this.declare = declare;
    this.scope = scope;
  }

  /**
   * Returns the variable name.
   */
  public String getVarName()
  {
    return varName;
  }

  /**
   * Returns the variable's Java class.
   */
  public String getClassName()
  {
    return className;
  }

  /**
   * True if the variable should be declared.  If false, the JSP engine
   * assumes the variable is already declared and just assigns the value.
   */
  public boolean getDeclare()
  {
    return declare;
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
    return scope;
  }
}
