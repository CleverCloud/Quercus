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

package com.caucho.jsp;

import com.caucho.jsp.java.TagTaglib;
import com.caucho.loader.SimpleLoader;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Stores the information for the .tags
 */
public class TagFileManager {
  static final L10N L = new L10N(TagFileManager.class);
  private static final Logger log = Log.open(TagFileManager.class);
  
  private JspCompiler _jspCompiler;

  public TagFileManager(JspCompiler compiler)
  {
    _jspCompiler = compiler;
  }

  public TagInfo getTag(String prefix,
                        String shortName,
                        String location)
    throws JspParseException
  {
    if (location == null)
      return null;
    
    try {
      String originalLocation = location;
      
      if (location.startsWith("urn:jsptagdir:"))
        location = location.substring("urn:jsptagdir:".length());

      TagTaglib taglib = new TagTaglib(prefix, originalLocation);
      
      String uri = location;
      
      TagInfo tag = null;

      try {
        tag = getTag(uri, taglib);
        if (tag != null)
          return tag;
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }
      
      if (! location.endsWith("/"))
        location = location + "/";
      
      uri = location + shortName + ".tag";
      
      tag = getTag(uri, taglib);
      if (tag != null)
        return tag;

      uri = location + shortName + ".tagx";
      return getTag(uri, taglib);
    } catch (Exception e) {
      throw JspParseException.create(e);
    }
  }

  public TagInfo getTag(String prefix,
                        String location)
    throws JspParseException
  {
    TagTaglib taglib = new TagTaglib(prefix, location);
    
    return getTag(location, taglib);
  }

  public TagInfo getTag(String location,
                        TagTaglib taglib)
    throws JspParseException
  {
    JspResourceManager resourceManager = _jspCompiler.getResourceManager();
    if (resourceManager == null)
      return null;
    
    Path path = resourceManager.resolvePath(location);
    
    return getTag(path, location, taglib);
  }

  public TagInfo getTag(Path path,
                        String prefix,
                        String location)
    throws JspParseException
  {
    TagTaglib taglib = new TagTaglib(prefix, location);
    
    return getTag(path, location, taglib);
  }

  public TagInfo getTag(Path path,
                        String location,
                        TagTaglib taglib)
    throws JspParseException
  {
    if (path == null || ! path.canRead())
      return null;

    try {
      if (location.endsWith(".tag")) {
        JspCompilerInstance tagCompiler;

        tagCompiler = _jspCompiler.getCompilerInstance(path, location);
        tagCompiler.setXML(false);

        return tagCompiler.compileTag(taglib);
      }
      else if (location.endsWith(".tagx")) {
        JspCompilerInstance tagCompiler;

        tagCompiler = _jspCompiler.getCompilerInstance(path, location);
        tagCompiler.setXML(true);

        return tagCompiler.compileTag(taglib);
      }
      else
        throw new JspParseException(L.l("tag file '{0}' must end with .tag or .tagx",
                                        location));
    } catch (Exception e) {
      throw JspParseException.create(e);
    }
  }

  public Class loadClass(String className)
    throws Exception
  {
    Path classDir = _jspCompiler.getClassDir();

    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader jspLoader = SimpleLoader.create(parentLoader,
                                                classDir,
                                                null);
    
    return Class.forName(className, false, jspLoader);
  }
}
