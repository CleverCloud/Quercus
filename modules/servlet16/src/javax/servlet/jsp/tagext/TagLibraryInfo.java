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
 * Information about the entire tag library.
 *
 * Tag libraries are declared in a JSP file like:
 * <code><pre>
 *  &lt;%@ taglib prefix='foo' uri='WEB-INF/tags.tld' %>
 * </pre></code>
 *
 * <p>The tags.tld will look something like:
 *
 * <code><pre>
 * &lt;taglib>
 *   &lt;uri>http://www.caucho.com/taglibs/2000-03-04/mytaglib.tld&lt;/uri>
 *   &lt;info>A sample tag library&lt;/info>
 *   &lt;shortname>mytaglib&lt;/shortname>
 *   &lt;jspversion>1.1&lt;/jspversion>
 *
 *   ...
 *
 *   &lt;tag>
 *     ...
 *   &lt;/tag>
 * &lt;/taglib>
 * </pre></code>
 */
abstract public class TagLibraryInfo {
  protected FunctionInfo []functions;
  protected String info;
  protected String jspversion;
  protected String prefix;
  protected String shortname;
  protected TagFileInfo []tagFiles;
  protected TagInfo []tags;
  protected String tlibversion;
  protected String uri;
  protected String urn;

  /**
   * Called by the JSP engine to collect tag library information.
   */
  protected TagLibraryInfo(String prefix, String uri)
  {
    this.prefix = prefix;
    this.uri    = uri;
  }

  /**
   * Returns a descriptive string for the library.  Taken from the
   * <code>info</code> attribute in the TLD.
   *
   * <code><pre>
   * &lt;taglib>
   *   &lt;info>A sample tag library&lt;/info>
   * </pre></code>
   */
  public String getInfoString()
  {
    return this.info;
  }

  /**
   * Returns the library's prefix string from the taglib declaration:
   *
   * <code><pre>
   *  &lt;%@ taglib prefix='foo' uri='WEB-INF/tags.tld' %>
   * </pre></code>
   */
  public String getPrefixString()
  {
    return this.prefix;
  }

  /**
   * Returns the library's uri from the taglib declaration:
   *
   * <code><pre>
   *  &lt;%@ taglib prefix='foo' uri='WEB-INF/tags.tld' %>
   * </pre></code>
   */
  public String getURI()
  {
    return this.uri;
  }

  /**
   * The preferred short name for the library.
   */
  public String getShortName()
  {
    return this.shortname;
  }

  /**
   * Returns a canonical name representing this tag library.  Taken
   * from the <code>uri</code> attribute in the taglib.
   *
   * <code><pre>
   * &lt;taglib>
   *   &lt;uri>http://www.caucho.com/taglibs/2000-03-04/mytaglib.tld&lt;/uri>
   * </pre></code>
   */
  public String getReliableURN()
  {
    return this.urn;
  }

  /**
   * Returns the minimum required JSP version for the tag library.
   */
  public String getRequiredVersion()
  {
    return this.jspversion;
  }

  /**
   * Returns an array of all the tags in the library
   */
  public TagInfo []getTags()
  {
    return this.tags;
  }

  /**
   * Returns the information for a specific tag.
   */
  public TagInfo getTag(String name)
  {
    if (this.tags == null)
      return null;

    for (int i = 0; i < this.tags.length; i++) {
      if (this.tags[i].getTagName().equals(name))
        return this.tags[i];
    }

    return null;
  }

  /**
   * Returns the named function.
   *
   * @since JSP 2.0
   */
  public FunctionInfo getFunction(String name)
  {
    if (this.functions == null)
      return null;

    for (int i = 0; i < this.functions.length; i++)
      if (this.functions[i].getName().equals(name))
        return this.functions[i];

    return null;
  }

  /**
   * Returns the functions for the tag.
   *
   * @since JSP 2.0
   */
  public FunctionInfo []getFunctions()
  {
    return this.functions;
  }

  /**
   * Returns the tag from the tag file.
   */
  public TagFileInfo []getTagFiles()
  {
    return this.tagFiles;
  }

  /**
   * Returns the information from the tag file.
   */
  public abstract TagLibraryInfo []getTagLibraryInfos();

  /**
   * Returns the tag from the tag file.
   */
  public TagFileInfo getTagFile(String shortname)
  {
    if (this.tagFiles == null)
      return null;

    for (int i = 0; i < this.tagFiles.length; i++)
      if (this.tagFiles[i].getName().equals(shortname))
        return this.tagFiles[i];

    return null;
  }
}
