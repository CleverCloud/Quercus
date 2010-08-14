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
 *
 * $Id: TagExtraInfo.java,v 1.2 2004/09/29 00:12:48 cvs Exp $
 */

package javax.servlet.jsp.tagext;

/**
 * Extra tag information for a custom tag.  It performs a similar function
 * to BeanInfo classes, but for tag libraries.  The TagExtraInfo class will
 * be called at JSP compile time to:
 * <ol>
 * <li>instantiate and initialize runtime variables
 * <li>validate tags
 * </ol>
 *
 * <p/>The TagExtraInfo class is specified in the TLD
 * using the teiclass attribute:
 *
 * <code><pre>
 * &lt;tag>
 *   &lt;name>foo&lt;/name>
 *   &lt;tagclass>com.caucho.tags.FooTag&lt;/tagclass>
 *   &lt;teiclass>com.caucho.tags.FooTagInfo&lt;/teiclass>
 * &lt;/tag>
 * </pre></code>
 */
public abstract class TagExtraInfo {
  private TagInfo _tagInfo;

  /**
   * Implementing classes must implement a zero-arg constructor.
   */
  public TagExtraInfo()
  {
  }

  /**
   * Returns information needed to instantiate runtime variables.
   *
   * <p>The default implementation returns null.
   *
   * <p>For example, if the tag initializes a nested integer named by
   * the foo attribute, getVariableInfo might return the following:
   * <code><pre>
   * public VariableInfo []getVariableInfo(TagData data)
   * {
   *   VariableInfo []info = new VariableInfo[1];
   *   String foo = data.getAttribute("foo");
   *   info[0] = new VariableInfo(foo, "int", true, VariableInfo.NESTED);
   *   return info;
   * }
   * </pre></code>
   *
   * @param data The tag's static attributes and values.
   */
  public VariableInfo []getVariableInfo(TagData data)
  {
    return null;
  }

  /**
   * Validates the tag, so errors can be caught at compile-time instead of
   * waiting for runtime.
   *
   * <p>The default implementation returns true.
   */
  public boolean isValid(TagData data)
  {
    return true;
  }

  /**
   * Sets the TLD tag info for this tag.
   */
  public final void setTagInfo(TagInfo tagInfo)
  {
    _tagInfo = tagInfo;
  }

  /**
   * Gets the TLD tag info for this tag.
   */
  public final TagInfo getTagInfo()
  {
    return _tagInfo;
  }

  /**
   * Validates the tag.
   */
  public ValidationMessage []validate(TagData data)
  {
    if (isValid(data))
      return null;
    else {
      ValidationMessage msg;
      String prefix = getTagInfo().getTagLibrary().getShortName();
      String name = getTagInfo().getTagName();

      if (prefix != null)
        name = prefix + ':' + name;

      msg = new ValidationMessage(null, "tag <" + name + "> was not valid according to validator `" + getClass().getName() + "'");

      return new ValidationMessage[] { msg };
    }
  }
}
