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

package com.caucho.xsl;

import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.Vfs;
import com.caucho.xml.XMLWriter;

import org.w3c.dom.Node;

import javax.xml.transform.Templates;
import javax.xml.transform.URIResolver;
import java.util.ArrayList;
import java.util.Properties;

/**
 * A compiled XSL stylesheet.  Stylesheets use 'transform' to transform
 * an XML tree to an XML Document.
 *
 * <p>The resulting document can be printed, or it can be added to another
 * XML tree.
 */
abstract public class AbstractStylesheet
  implements CauchoStylesheet, Templates {
  // The path to the stylesheet source
  private Path _path;
  private ArrayList<PersistentDependency> _depends =
    new ArrayList<PersistentDependency>();
  private ArrayList<String> _cacheDepends = new ArrayList<String>();
  private ArrayList _globalParameters;

  protected Properties _output = new Properties();
  protected AbstractStylesheet _stylesheet;

  protected String _errorPage;
  protected URIResolver _uriResolver;
  boolean _escapeEntities = true;

  /**
   * Initialize the stylesheet with the search path.
   *
   * @param path the path of the stylepath used to search for stylesheets.
   */
  public void init(Path path) throws Exception
  {
    /*
     * Can't use best path because the stylesheets are relative to
     * the search path.
    if (path instanceof MergePath)
      path = ((MergePath) path).getBestPath();
    */
    
    _path = path;
  }

  public void setURIResolver(URIResolver resolver)
  {
    _uriResolver = resolver;
  }

  public URIResolver getURIResolver()
  {
    return _uriResolver;
  }

  /**
   * Copies the current stylesheet into the new stylesheet.  Used to
   * create the transformer.
   *
   * @param stylesheet the new stylesheet which will contain the copied values.
   */
  protected void copy(AbstractStylesheet stylesheet)
  {
    stylesheet._stylesheet = this;
    stylesheet._depends = (ArrayList) _depends.clone();
    stylesheet._output = (Properties) _output.clone();
    stylesheet._errorPage = _errorPage;
    stylesheet._globalParameters = _globalParameters;
    stylesheet._path = _path;
    stylesheet._uriResolver = _uriResolver;
  }

  /**
   * Clone the stylesheet.  Used to create transformer.
   */
  public Object clone()
  {
    try {
      AbstractStylesheet instance;
      instance = (AbstractStylesheet) getClass().newInstance();

      copy(instance);

      if (_path != null)
        instance.init(_path);
      else
        instance.init(Vfs.lookup("anonymous.xsl"));

      return instance;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the output properties for the stylesheet.
   */
  public Properties getOutputProperties()
  {
    return _output;
  }

  public Path getPath()
  {
    return _path;
  }
  
  /**
   * Returns a stylesheet property.
   */
  public Object getProperty(String name)
  {
    if (name.equals(DEPENDS))
      return _depends;
    else if (name.equals(CACHE_DEPENDS))
      return _cacheDepends;
    else if (name.equals("caucho.path"))
      return _path;
    else if (name.equals(GLOBAL_PARAM))
      return _globalParameters;
    else
      return null;
  }

  /**
   * Sets a stylesheet property.
   */
  public void setProperty(String name, Object value)
  {
    if (name.equals(GLOBAL_PARAM))
      _globalParameters = (ArrayList) value;
  }

  /**
   * Creates a new transformer.
   */
  public javax.xml.transform.Transformer newTransformer()
  {
    return new TransformerImpl((StylesheetImpl) clone());
  }

  /**
   * Returns true if the any of the source stylesheets have been modified
   * since this stylesheet was compiled.
   */
  public boolean isModified()
  {
    for (int i = 0; i < _depends.size(); i++) {
      PersistentDependency depend = _depends.get(i);

      if (depend.isModified())
        return true;
    }

    return false;
  }

  /**
   * Add a dependency to the stylesheet.  Used to keep track of source
   * stylesheets.
   *
   * @param path the path of the source stylesheet.
   */
  protected void addDepend(PersistentDependency depend)
  {
    if (! _depends.contains(depend))
      _depends.add(depend);
  }

  /**
   * Returns the dependency list of the stylesheet.
   */
  public ArrayList<PersistentDependency> getDepends()
  {
    return _depends;
  }

  /**
   * Adds a cache dependency.
   */
  protected void addCacheDepend(String path)
  {
    _cacheDepends.add(path);
  }

  public ArrayList<String> getCacheDepends()
  {
    return _cacheDepends;
  }

  /**
   * Transforms the XML node to a new XML document based on this stylesheet.
   *
   * <p>Since Documents are DocumentFragments, calling functions can insert
   * the contents using appendChild.
   *
   * @param xml source xml to convert
   * @param out source xml to convert
   * @return the converted document
   */
  abstract public void transform(Node xml,
                                 XMLWriter out,
                                 TransformerImpl transformer)
    throws Exception;
}
    
