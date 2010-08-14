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

package javax.servlet.jsp;

import javax.el.ELContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import java.io.Writer;
import java.util.Enumeration;

public abstract class JspContext {
  /**
   * Returns the current output for the page.
   */
  public abstract JspWriter getOut();

  /**
   * Gets the named page attribute.
   *
   * @param name of the attribute
   */
  public abstract Object getAttribute(String name);
  /**
   * Sets the named page attribute.
   *
   * @param name name of the attribute
   * @param attribute non-null attribute value.
   */
  public abstract void setAttribute(String name, Object attribute);
  
  /**
   * Removes the named page attribute.
   */
  public abstract void removeAttribute(String name);
  
  /**
   * Sets an attribute in a given scope.  You should use the scope-specific
   * routines instead, like request.setAttribute.
   *
   * @param name attribute name
   * @param o attribute value
   * @param scope attribute scope
   */
  public abstract void setAttribute(String name, Object o, int scope);
  
  /**
   * Gets an attribute in a given scope.  You should use the scope-specific
   * routines instead, like request.getAttribute.
   *
   * @param name attribute name
   * @param scope attribute scope
   */
  public abstract Object getAttribute(String name, int scope);
  
  /**
   * Removes an attribute in a given scope.  You should use the scope-specific
   * routines instead, like request.removeAttribute.
   */
  public abstract void removeAttribute(String name, int scope);
  
  /**
   * Lists attribute names in a given scope.  You should use the scope-specific
   * routines instead, like request.getAttributeNames
   */
  public abstract Enumeration<String> getAttributeNamesInScope(int scope);
  
  /**
   * Returns the scope for an attribute.
   */
  public abstract int getAttributesScope(String name);
  
  /**
   * Finds an attribute in all scopes.
   */
  public abstract Object findAttribute(String name);
  
  /**
   * Internal routine to support BodyTags.
   */
  public JspWriter pushBody(Writer writer)
  {
    return null;
  }
  
  /**
   * Internal routine to support BodyTags.
   */
  public JspWriter popBody()
  {
    return null;
  }

  /**
   * Returns an expression evaluator for creating JSP EL expressions.
   * @Deprecated
   */
  public abstract ExpressionEvaluator getExpressionEvaluator();

  /**
   * Returns a variable resolver for evaluating JSP EL expressions.
   * @Deprecated
   */
  public abstract VariableResolver getVariableResolver();
  
  /**
   * Returns the EL context with the JspContext
   */
  public abstract ELContext getELContext();
}
