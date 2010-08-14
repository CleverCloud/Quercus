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
 * Represents the tag information from the tld.  TagExtraInfo classes
 * can use this to help in validation or in variable creation.
 *
 * <code><pre>
 * &lt;tag>
 *   &lt;name>foo&lt;/name>
 *   &lt;tagclass>com.caucho.tags.FooTag&lt;/tagclass>
 *   &lt;teiclass>com.caucho.tags.FooTagInfo&lt;/teiclass>
 *   &lt;bodycontent>jsp&lt;/bodycontent>
 *
 *   &lt;attribute>
 *     ...
 *   &lt;/attribute>
 * &lt;/tag>
 * </pre></code>
 */
public class TagInfo {
  /**
   * Constant for the "empty" body content
   */
  public static final String BODY_CONTENT_EMPTY = "empty";
  /**
   * Constant for standard "jsp" processed body content
   */
  public static final String BODY_CONTENT_JSP = "JSP";
  /**
   * Constant for tags which forbid scripts
   */
  public static final String BODY_CONTENT_SCRIPTLESS = "scriptless";
  /**
   * Constant for "tag-dependent" unprocessed verbatim body content
   */
  public static final String BODY_CONTENT_TAG_DEPENDENT = "tagdependent";

  private String bodyContent;
  private String infoString;
  private String tagClassName;
  private TagExtraInfo tagExtraInfo;
  private TagLibraryInfo tagLibrary;
  private String tagName;
  private TagAttributeInfo []attributeInfo;
  private String displayName;
  private String smallIcon;
  private String largeIcon;
  private TagVariableInfo []tvi;
  private boolean dynamicAttributes;

  /**
   * Constructor for TagInfo.  Created by the JSP engine at compile time
   * for the benefit of TagExtraInfo classes.
   *
   * @param tagName tag name
   * @param tagClassName the tag's class name
   * @param bodyContent description of the expected body contents
   * @param infoString informatino string of the tag
   * @param taglib pointer to the TagLibraryInfo
   * @param tagExtraInfo the tag's custom TagExtraInfo.
   * @param tagAttributeInfo information about the tags attribute from the tld.
   */
  public TagInfo(String tagName,
                 String tagClassName,
                 String bodyContent,
                 String infoString,
                 TagLibraryInfo taglib,
                 TagExtraInfo tagExtraInfo,
                 TagAttributeInfo []attributeInfo)
  {
    this.tagName = tagName;
    this.tagClassName = tagClassName;
    this.bodyContent = bodyContent;
    this.infoString = infoString;
    this.tagLibrary = taglib;
    this.tagExtraInfo = tagExtraInfo;
    this.attributeInfo = attributeInfo;

    if (tagExtraInfo != null)
      tagExtraInfo.setTagInfo(this);
  }

  /**
   * Constructor for TagInfo.  Created by the JSP engine at compile time
   * for the benefit of TagExtraInfo classes.
   *
   * @param tagName tag name
   * @param tagClassName the tag's class name
   * @param bodyContent description of the expected body contents
   * @param infoString informatino string of the tag
   * @param taglib pointer to the TagLibraryInfo
   * @param tagExtraInfo the tag's custom TagExtraInfo.
   * @param tagAttributeInfo information about the tags attribute from the tld.
   * @param displayName the GUI builder's display name
   * @param smallIcon small icon for a GUI builder
   * @param largeIcon large icon for a GUI builder
   * @param tvi variable info in the tld
   */
  public TagInfo(String tagName,
                 String tagClassName,
                 String bodyContent,
                 String infoString,
                 TagLibraryInfo taglib,
                 TagExtraInfo tagExtraInfo,
                 TagAttributeInfo []attributeInfo,
                 String displayName,
                 String smallIcon,
                 String largeIcon,
                 TagVariableInfo []tvi)
  {
    this(tagName, tagClassName, bodyContent, infoString, taglib, tagExtraInfo,
         attributeInfo);

    this.displayName = displayName;
    this.smallIcon = smallIcon;
    this.largeIcon = largeIcon;
    this.tvi = tvi;
  }

  /**
   * Constructor for TagInfo.  Created by the JSP engine at compile time
   * for the benefit of TagExtraInfo classes.
   *
   * @param tagName tag name
   * @param tagClassName the tag's class name
   * @param bodyContent description of the expected body contents
   * @param infoString informatino string of the tag
   * @param taglib pointer to the TagLibraryInfo
   * @param tagExtraInfo the tag's custom TagExtraInfo.
   * @param tagAttributeInfo information about the tags attribute from the tld.
   * @param displayName the GUI builder's display name
   * @param smallIcon small icon for a GUI builder
   * @param largeIcon large icon for a GUI builder
   * @param tvi variable info in the tld
   */
  public TagInfo(String tagName,
                 String tagClassName,
                 String bodyContent,
                 String infoString,
                 TagLibraryInfo taglib,
                 TagExtraInfo tagExtraInfo,
                 TagAttributeInfo []attributeInfo,
                 String displayName,
                 String smallIcon,
                 String largeIcon,
                 TagVariableInfo []tvi,
                 boolean dynamicAttributes)
  {
    this(tagName, tagClassName, bodyContent, infoString, taglib, tagExtraInfo,
         attributeInfo, displayName, smallIcon, largeIcon, tvi);

    this.dynamicAttributes = dynamicAttributes;
  }

  /**
   * Returns the tag's name.
   */
  public String getTagName()
  {
    return this.tagName;
  }

  /**
   * Returns the tag's infomation string.
   */
  public String getInfoString()
  {
    return this.infoString;
  }

  /**
   * Returns the tag's class name.
   */
  public String getTagClassName()
  {
    return this.tagClassName;
  }

  /**
   * Returns the body content type.
   *
   * <ul>
   * <li>empty - the tag must be empty
   * <li>jsp - the body is processed as normal jsp
   * <li>tag-dependent - the body copied as verbatim text.
   * </ul>
   */
  public String getBodyContent()
  {
    return this.bodyContent;
  }

  /**
   * Returns the display name to be displayed by tools.
   */
  public String getDisplayName()
  {
    return this.displayName;
  }

  /**
   * Returns the path to the large icon to be displayed by the tools.
   */
  public String getLargeIcon()
  {
    return this.largeIcon;
  }

  /**
   * Returns the path to the small icon to be displayed by the tools.
   */
  public String getSmallIcon()
  {
    return this.smallIcon;
  }

  /**
   * Returns information about the tags allowed attributes.
   */
  public TagAttributeInfo[] getAttributes()
  {
    return this.attributeInfo;
  }

  /**
   * Returns the tag variable info in the tld
   */
  public TagVariableInfo []getTagVariableInfos()
  {
    return this.tvi;
  }

  /**
   * Information about the variables created by the tag at runtime.
   *
   * @param data information about the tag instance
   */
  public VariableInfo[] getVariableInfo(TagData data)
  {
    if (this.tagExtraInfo == null)
      return null;
    else
      return this.tagExtraInfo.getVariableInfo(data);
  }

  /**
   * Retursn true if dynamic attributes are supported.
   */
  public boolean hasDynamicAttributes()
  {
    return this.dynamicAttributes;
  }

  /**
   * Returns true if the tag instance is valid.
   *
   * @param data information about the tag instance
   */
  public boolean isValid(TagData data)
  {
    if (this.tagExtraInfo == null)
      return true;
    else
      return this.tagExtraInfo.isValid(data);
  }

  /**
   * Returns the tag extra info for the tag.
   */
  public TagExtraInfo getTagExtraInfo()
  {
    return this.tagExtraInfo;
  }

  /**
   * Sets the tag extra info for the tag.
   */
  public void setTagExtraInfo(TagExtraInfo tei)
  {
    this.tagExtraInfo = tei;
  }

  /**
   * Returns the TagLibraryInfo for the tag.
   */
  public TagLibraryInfo getTagLibrary()
  {
    return this.tagLibrary;
  }

  /**
   * Sets the TagLibraryInfo for the tag.
   */
  public void setTagLibrary(TagLibraryInfo info)
  {
    this.tagLibrary = info;
  }

  /**
   * Validate attributes.
   */
  public ValidationMessage []validate(TagData data)
  {
    if (this.tagExtraInfo == null)
      return null;
    else
      return this.tagExtraInfo.validate(data);
  }
}
