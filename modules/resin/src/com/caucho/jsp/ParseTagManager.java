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

import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.xml.QName;

import javax.servlet.jsp.tagext.TagInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Stores the information for the .tags
 */
public class ParseTagManager {
  static final L10N L = new L10N(ParseTagManager.class);
  private static final Logger log
    = Logger.getLogger(ParseTagManager.class.getName());

  private JspResourceManager _resourceManager;
  private TaglibManager _taglibManager;
  private TagFileManager _tagFileManager;

  private HashMap<QName,TagInfo> _tagMap = new HashMap<QName,TagInfo>();
  private HashMap<String,Taglib> _taglibMap = new HashMap<String,Taglib>();

  public ParseTagManager(JspResourceManager resourceManager,
                         TaglibManager taglibManager,
                         TagFileManager tagFileManager)
    throws JspParseException, IOException
  {
    _resourceManager = resourceManager;
    _taglibManager = taglibManager;

    _tagFileManager = tagFileManager;
  }

  /**
   * Analyzes the tag.
   */
  public synchronized  AnalyzedTag analyzeTag(Class cl)
  {
    return _taglibManager.analyzeTag(cl);
  }

  /**
   * Returns the tag with the given qname.
   */
  public synchronized TagInfo getTag(QName qname)
    throws JspParseException
  {
    TagInfo tag = getTagImpl(qname);

    if (tag instanceof TagInfoImpl)
      ((TagInfoImpl) tag).validate();

    return tag;
  }

  /**
   * Temporary tag info for recursive tags
   */
  TagInfo addTempTagInfo(QName qname, TagInfo info)
  {
    if (_tagMap.get(qname) == null)
      _tagMap.put(qname, info);

    return _tagMap.get(qname);
  }

  /**
   * Returns the tag with the given qname.
   */
  private TagInfo getTagImpl(QName qname)
    throws JspParseException
  {
    TagInfo tag = _tagMap.get(qname);

    if (tag != null)
      return tag;
    
    tag = _tagFileManager.getTag(qname.getPrefix(),
                                 qname.getLocalName(),
                                 qname.getNamespaceURI());
    _tagMap.put(qname, tag);

    if (tag != null)
      return tag;
    
    Taglib taglib = addTaglib(qname);
    if (taglib == null)
      return null;
    
    String name = qname.getName();
    String tail = qname.getLocalName();

    if (qname.getNamespaceURI() == null) {
      int p = name.lastIndexOf(':');

      if (p < 0)
        return null;
    
      tail = name.substring(p + 1);
    }

    if (taglib != null)
      tag = taglib.getTag(tail);

    if (tag == null) {
      String tagLocation = taglib.getTagFilePath(tail);
      Path path = taglib.getPath();

      if (path != null && tagLocation != null) {
        path = path.lookup(tagLocation);

        tag = _tagFileManager.getTag(path, qname.getPrefix(), tagLocation);

        if (tag != null) {
          return tag;
        }
      }
      
      if (tagLocation != null) {
        tag = _tagFileManager.getTag(qname.getPrefix(), tagLocation);

        if (tag == null)
          throw new JspParseException(L.l("'{0}' is an unknown tag-file in tag library '{1}'.",
                                          tagLocation, taglib.getURI()));
      }
    }

    if (tag == null)
      throw new JspParseException(L.l("'{0}' is an unknown tag in tag library '{1}'.",
                                      tail, taglib.getURI()));

    _tagMap.put(qname, tag);

    return tag;
  }

  /**
   * Returns the tag with the given qname.
   */
  public synchronized Class getTagClass(QName qname)
    throws Exception
  {
    TagInfo tagInfo = getTag(qname);
    
    if (tagInfo == null)
      return null;
    
    String className = tagInfo.getTagClassName();

    if (className != null)
      return _tagFileManager.loadClass(className);
    else
      return null;
  }

  public synchronized Taglib addTaglib(QName qname)
    throws JspParseException
  {
    String prefix = qname.getPrefix();

    Taglib taglib = (Taglib) _taglibMap.get(prefix);
    if (_taglibMap.get(prefix) != null)
      return taglib;
    
    String uri = qname.getNamespace();

    taglib = addTaglib(prefix, uri);
    
    _taglibMap.put(prefix, taglib);

    return taglib;
  }

  /**
   * Lookup and add a taglib based on a prefix and uri
   */
  public synchronized Taglib addTaglib(String prefix, String uri)
    throws JspParseException
  {
    Taglib taglib = null;
    
    boolean hasTld = false;

    if (uri == null)
      return null;
    else if (uri.startsWith("urn:jsptagdir:")) {
      String tagDir = uri.substring("urn:jsptagdir:".length());

      taglib = addTaglibDir(prefix, tagDir);
      hasTld = true;

      if (taglib == null) {
        throw error(L.l("'{0}' has no matching tag.  The taglib uri must match a <uri> element in a taglib.tld.", uri));
      }
    }
    else {
      if (uri.startsWith("urn:jsptld:")) {
        hasTld = true;
        uri = uri.substring("urn:jsptld:".length());
      }

      String location = uri;

      taglib = _taglibManager.getTaglib(prefix, uri, location);

      if (hasTld && taglib == null) {
        throw error(L.l("'{0}' has no matching tag.  The taglib uri must match a <uri> element in a taglib.tld.", uri));
      }
    }

    return taglib;
  }

  /**
   * Adds a taglib.
   */
  public synchronized Taglib addTaglibDir(String prefix, String dir)
    throws JspParseException
  {
    return  _taglibManager.getTaglibDir(prefix, dir);
  }

  /**
   * Adds a taglib.
   */
  public synchronized Taglib addTaglib(String prefix,
                                       String uri,
                                       String location)
    throws JspParseException
  {
    Taglib taglib = _taglibManager.getTaglib(prefix, uri, location);

    return addTaglib(taglib);
  }

  private Taglib addTaglib(Taglib taglib)
    throws JspParseException
  {
    if (taglib == null)
      return null;
    
    taglib = taglib.copy();

    for (Taglib oldTaglib : _taglibMap.values()) {
      if (oldTaglib != null) {
        oldTaglib.addTaglib(taglib);
        taglib.addTaglib(oldTaglib);
      }
    }
    
    _taglibMap.put(taglib.getPrefixString(), taglib);

    return taglib;
  }
  

  public boolean hasTags()
  {
    return _taglibMap != null && _taglibMap.size() > 1;
  }

  public JspParseException error(String message)
  {
    return new JspParseException(message);
  }
}
