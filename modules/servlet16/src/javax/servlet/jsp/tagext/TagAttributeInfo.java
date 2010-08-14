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

package javax.servlet.jsp.tagext;

/**
 * Information about tag attributes.  This information comes from the
 * Tag Library Descriptor (TLD).  Generally, a TagExtraInfo class will
 * grab this information from the TagLibraryInfo class.
 *
 * <code><pre>
 * &lt;tag>
 *   &lt;name>foo&lt;/name>
 *   &lt;tagclass>com.caucho.tags.FooTag&lt;/tagclass>
 *
 *   &lt;attribute>
 *     &lt;name>bar&lt;/name>
 *     &lt;required>true&lt;/required>
 *     &lt;rtexprvalue>false&lt;/rtexprvalue>
 * &lt;/tag>
 * </pre></code>
 */
public class TagAttributeInfo {
  /**
   * ID is "id"
   */
  public static final String ID = "id";

  private String _name;
  private boolean _reqTime;
  private boolean _required;
  private String _type;

  private boolean _fragment;

  private String _description;

  private boolean _isDeferredValue;
  private boolean _isDeferredMethod;

  private String _expectedTypeName;
  private String _methodSignature;

  /**
   * Creates a new TagAttributeInfo object.  Only the JSP engine will
   * call this.  It's not intended to be a public constructor.
   *
   * @param name the name of the attribute
   * @param required true if the attribute must be present in the tag
   * @param reqTime true if the attribute can be a request time attribute
   * @param type the Java type of the attribute
   */
  public TagAttributeInfo(String name,
                          boolean required,
                          String type,
                          boolean reqTime)
  {
    _name = name;
    _required = required;
    _type = type;
    _reqTime = reqTime;
  }

  /**
   * Creates a new TagAttributeInfo object.  Only the JSP engine will
   * call this.  It's not intended to be a public constructor.
   *
   * @param name the name of the attribute
   * @param required true if the attribute must be present in the tag
   * @param reqTime true if the attribute can be a request time attribute
   * @param type the Java type of the attribute
   */
  public TagAttributeInfo(String name,
                          boolean required,
                          String type,
                          boolean reqTime,
                          boolean fragment)
  {
    this(name, required, type, reqTime);

    _fragment = fragment;
  }

  /**
   * Creates a new TagAttributeInfo object.  Only the JSP engine will
   * call this.  It's not intended to be a public constructor.
   *
   * @param name the name of the attribute
   * @param required true if the attribute must be present in the tag
   * @param reqTime true if the attribute can be a request time attribute
   * @param type the Java type of the attribute
   */
  public TagAttributeInfo(String name,
                          boolean required,
                          String type,
                          boolean reqTime,
                          boolean fragment,
                          String description,
                          boolean deferredValue,
                          boolean deferredMethod,
                          String expectedTypeName,
                          String methodSignature)
  {
    this(name, required, type, reqTime, fragment);

    _description = description;
    _isDeferredValue = deferredValue;
    _isDeferredMethod = deferredMethod;
    _expectedTypeName = expectedTypeName;
    _methodSignature = methodSignature;
  }

  /**
   * Returns the attribute name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the type name of the attribute.
   */
  public String getTypeName()
  {
    return _type;
  }

  /**
   * Returns true if the attribute can be a request time attribute (&lt;%= ... %>).
   */
  public boolean canBeRequestTime()
  {
    return _reqTime;
  }

  /**
   * True if the attribute must exist in the tag.
   */
  public boolean isRequired()
  {
    return _required;
  }

  /**
   * True if the attribute is of type fragment
   */
  public boolean isFragment()
  {
    return _fragment;
  }

  /**
   * Returns the tag's description.
   *
   * @since JSP 2.1
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * Returns the expected type of the attribute.
   *
   * @since JSP 2.1
   */
  public String getExpectedTypeName()
  {
    return _expectedTypeName;
  }

  /**
   * Returns the expected method signature.
   *
   * @since JSP 2.1
   */
  public String getMethodSignature()
  {
    return _methodSignature;
  }

  /**
   * Returns true if the attribute is deferred.
   *
   * @since JSP 2.1
   */
  public boolean isDeferredMethod()
  {
    return _isDeferredMethod;
  }

  /**
   * Returns true if the attribute is deferred.
   *
   * @since JSP 2.1
   */
  public boolean isDeferredValue()
  {
    return _isDeferredValue;
  }

  /**
   * Convenience for finding a TagAttributeInfo in a TagAttributeInfo
   * array.
   */
  public static TagAttributeInfo getIdAttribute(TagAttributeInfo []a)
  {
    for (int i = 0; i < a.length; i++)
      if (a[i].getName().equals(ID))
        return a[i];

    return null;
  }
}
