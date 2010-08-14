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

package com.caucho.jsp.cfg;

import com.caucho.config.DependencyBean;
import com.caucho.server.webapp.WebApp;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;

import java.util.ArrayList;

/**
 * Configuration for the taglib in the .tld
 */
public class TldTaglib implements DependencyBean
{
  private String _tlibVersion;
  private String _jspVersion;
  private String _shortName;
  private String _uri;
  private String _displayName;
  private String _smallIcon;
  private String _largeIcon;
  private String _description;
  private String _info;
  private TldValidator _validator;
  private ArrayList<TldListener> _listeners = new ArrayList<TldListener>();
  private ArrayList<TldTag> _tags = new ArrayList<TldTag>();
  private ArrayList<TldTagFile> _tagFiles = new ArrayList<TldTagFile>();
  private ArrayList<TldFunction> _functionList = new ArrayList<TldFunction>();

  private Path _jarPath;
  private Throwable _configException;

  private ArrayList<PersistentDependency> _dependList
    = new ArrayList<PersistentDependency>();

  private boolean _isInit;

  public TldTaglib()
  {
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(PersistentDependency depend)
  {
    _dependList.add(depend);
  }

  /**
   * Sets the tld version.
   */
  public void setVersion(String version)
  {
    _jspVersion = version;
  }

  /**
   * Sets the schema location.
   */
  public void setSchemaLocation(String location)
  {
  }

  /**
   * Sets the icon.
   */
  public void setIcon(com.caucho.config.types.Icon icon)
  {
  }

  /**
   * Sets the taglib version.
   */
  public void setTlibVersion(String tlibVersion)
  {
    _tlibVersion = tlibVersion;
  }

  /**
   * Sets the taglib version (backwards compat).
   */
  public void setTlibversion(String tlibVersion)
  {
    setTlibVersion(tlibVersion);
  }

  /**
   * Gets the taglib version.
   */
  public String getTlibVersion()
  {
    return _tlibVersion;
  }

  /**
   * Sets the JSP version.
   */
  public void setJspVersion(String jspVersion)
  {
    _jspVersion = jspVersion;
  }

  /**
   * Sets the JSP version (backwards compat).
   */
  public void setJspversion(String jspVersion)
  {
    setJspVersion(jspVersion);
  }

  /**
   * Gets the jsp version.
   */
  public String getJspVersion()
  {
    return _jspVersion;
  }

  /**
   * Sets the info string
   */
  public void setInfo(String info)
  {
    _info = info;
  }

  /**
   * Gets the info string.
   */
  public String getInfo()
  {
    return _info;
  }

  /**
   * Sets the short name (prefix)
   */
  public void setShortName(String shortName)
  {
    _shortName = shortName;
  }

  /**
   * Sets the short name (backwards compat)
   */
  public void setShortname(String shortName)
  {
    setShortName(shortName);
  }

  /**
   * Gets the short name (prefix)
   */
  public String getShortName()
  {
    return _shortName;
  }

  /**
   * Sets the uri
   */
  public void setURI(String uri)
  {
    _uri = uri;
  }

  /**
   * Gets the uri
   */
  public String getURI()
  {
    return _uri;
  }

  /**
   * Sets the display-name
   */
  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  /**
   * Gets the display-name
   */
  public String getDisplayName()
  {
    return _displayName;
  }

  /**
   * Sets the small-icon
   */
  public void setSmallIcon(String smallIcon)
  {
    _smallIcon = smallIcon;
  }

  /**
   * Gets the small-icon
   */
  public String getSmallIcon()
  {
    return _smallIcon;
  }

  /**
   * Sets the large-icon
   */
  public void setLargeIcon(String largeIcon)
  {
    _largeIcon = largeIcon;
  }

  /**
   * Gets the large-icon
   */
  public String getLargeIcon()
  {
    return _largeIcon;
  }

  /**
   * Sets the description
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Gets the description
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * Sets the validator
   */
  public void setValidator(TldValidator validator)
  {
    _validator = validator;
  }

  /**
   * Gets the validator
   */
  public TldValidator getValidator()
  {
    return _validator;
  }

  /**
   * Adds a listener
   */
  public void addListener(TldListener listener)
  {
    _listeners.add(listener);
  }

  /**
   * Creates a new tag instance
   */
  public TldTag createTag()
  {
    return new TldTag(this);
  }

  /**
   * Adds a tag
   */
  public void addTag(TldTag tag)
  {
    _tags.add(tag);
  }

  /**
   * Returns the list of tags.
   */
  public ArrayList<TldTag> getTagList()
  {
    return _tags;
  }

  /**
   * Adds a tag-file
   */
  public void addTagFile(TldTagFile tagFile)
  {
    _tagFiles.add(tagFile);
  }

  /**
   * Returns the list of tag files.
   */
  public ArrayList<TldTagFile> getTagFileList()
  {
    return _tagFiles;
  }

  /**
   * Adds a jsf tag.
   */
  public void addJsfTag(JsfTag tag)
  {
    _tags.add(tag);
  }

  /**
   * Adds a function
   */
  public void addFunction(TldFunction function)
  {
    _functionList.add(function);
  }

  /**
   * Returns the list of functions.
   */
  public ArrayList<TldFunction> getFunctionList()
  {
    return _functionList;
  }

  /**
   * Sets the jar path.
   */
  public void setJarPath(Path path)
  {
    _jarPath = path;
  }

  /**
   * Gets the jar path.
   */
  public Path getJarPath()
  {
    return _jarPath;
  }

  /**
   * Sets any configuration exception
   */
  public void setConfigException(Throwable e)
  {
    _configException = e;
  }

  /**
   * Gets any configuration exception
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Applies the listeners.
   */
  public void initListeners(WebApp app)
    throws InstantiationException, IllegalAccessException
  {
    if (app == null)
      return;

    for (int i = 0; i < _listeners.size(); i++) {
      TldListener listener = _listeners.get(i);

      listener.register(app);
    }
  }

  public void mergeJsf(TldTaglib jsfTaglib)
  {
    ArrayList<TldTag> jsfTags = jsfTaglib.getTagList();

    for (int i = 0; i < jsfTags.size(); i++) {
      JsfTag jsfTag = (JsfTag) jsfTags.get(i);

      if (jsfTag.getBaseTag() != null)
        continue;

      int p = _tags.indexOf(jsfTag);

      if (p >= 0) {
        TldTag tag = _tags.remove(p);

        jsfTag.setBaseTag(tag);
      }
      else
        throw new IllegalStateException("No matching tag for : " + jsfTag);

      _tags.add(jsfTag);
    }
  }


  /**
   * Checks for modification.
   */
  public boolean isModified()
  {
    for (int i = 0; i < _dependList.size(); i++) {
      if (_dependList.get(i).isModified())
        return true;
    }

    return false;
  }
}
