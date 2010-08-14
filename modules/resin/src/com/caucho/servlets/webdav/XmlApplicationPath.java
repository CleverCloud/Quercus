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

package com.caucho.servlets.webdav;

import com.caucho.server.webapp.WebApp;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Path;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Represents a virtual filesystem using xml files to store the attribute.
 */
public class XmlApplicationPath extends ApplicationPath {
  private Path _root;
  private HashMap _map = new HashMap();

  public XmlApplicationPath()
  {
  }

  /**
   * path the root path.
   */
  public void setRoot(Path path)
  {
    _root = path;
  }

  /**
   * Returns the root path.
   */
  public Path getRoot()
  {
    return _root;
  }
  
  /**
   * Deletes the file
   *
   * @param path the requested relative path
   *
   * @return true if the remove succeeded.
   */
  public boolean remove(String path,
                        HttpServletRequest request,
                        ServletContext app)
    throws IOException
  {
    removeAttributes(path);
    
    return super.remove(path, request, app);
  }
    
  /**
   * Returns an iterator over the attribute names.
   * Each attribute name is of the type AttributeName.
   *
   * @param path the requested relative path
   * @param request the servlet request
   * @param app the servlet context
   */
  public Iterator getAttributeNames(String path,
                                    HttpServletRequest request,
                                    ServletContext app)
    throws IOException
  {
    FileAttributes attrs = getAttributes(path);

    if (attrs != null)
      return attrs.getAttributeNames();
    else
      return null;
  }
  
  /**
   * Returns an attribute value.
   *
   * @param name the attribute name
   * @param path the requested relative path
   * @param request the servlet request
   * @param app the servlet context
   */
  public String getAttribute(AttributeName name,
                             String path,
                             HttpServletRequest request,
                             ServletContext app)
    throws IOException
  {
    FileAttributes attrs = getAttributes(path);

    if (attrs != null)
      return attrs.getAttribute(name);
    else
      return null;
  }
  
  /**
   * Sets an attribute value.
   *
   * @param name the attribute name
   * @param value the attribute value
   * @param path the requested relative path
   * @param request the servlet request
   * @param app the servlet context
   *
   * @return true if the setting was successful
   */
  public boolean setAttribute(AttributeName name, String value,
                              String path,
                              HttpServletRequest request,
                              ServletContext app)
    throws IOException
  {
    FileAttributes attrs = getAttributes(path);

    if (attrs != null)
      return attrs.setAttribute(name, value);
    else
      return false;
  }
  
  /**
   * Removes an attribute value.
   *
   * @param name the attribute name
   * @param path the requested relative path
   * @param request the servlet request
   * @param app the servlet context
   */
  public boolean removeAttribute(AttributeName name, 
                                 String path,
                                 HttpServletRequest request,
                                 ServletContext app)
    throws IOException
  {
    FileAttributes attrs = getAttributes(path);

    if (attrs != null)
      attrs.removeAttribute(name);

    return true;
  }

  protected FileAttributes getAttributes(String path)
  {
    FileAttributes attrs = (FileAttributes) _map.get(path);

    if (attrs == null) {
      attrs = new FileAttributes();
      _map.put(path, attrs);
    }
    
    return attrs;
  }

  protected void removeAttributes(String path)
  {
    _map.remove(path);
  }
  
  /**
   * Returns a list of the files in the directory.
   *
   * @param path the requested relative path
   */
  public String []list(String path,
                       HttpServletRequest request,
                       ServletContext app)
    throws IOException
  {
    ArrayList filteredList = new ArrayList();

    String []names = getPath(path, request, app).list();

    for (int i = 0; i < names.length; i++) {
      if (! names[i].startsWith("."))
        filteredList.add(names[i]);
    }
    
    return (String []) filteredList.toArray(new String[filteredList.size()]);
  }

  /**
   * Returns the underlying path.
   */
  protected Path getPath(String path,
                         HttpServletRequest request,
                         ServletContext app)
    throws IOException
  {
    Path appDir = ((WebApp) app).getAppDir();

    if (_root != null)
      appDir = _root;

    Path filePath = appDir.lookup("./" + path);
    String tail = filePath.getTail();

    if (tail.startsWith("."))
      return filePath.getParent().lookup(".bogus");
    else
      return filePath;
  }

  public static class FileAttributes {
    HashMap attributes = new HashMap();
    
    /**
     * Returns an iterator over the attribute names.
     * Each attribute name is of the type AttributeName.
     */
    public Iterator getAttributeNames()
      throws IOException
    {
      return attributes.keySet().iterator();
    }
  
    /**
     * Returns an attribute value.
     */
    public String getAttribute(AttributeName name)
      throws IOException
    {
      return (String) attributes.get(name);
    }
  
    /**
     * Sets an attribute value.
     *
     * @param name the attribute name
     * @param value the attribute value
     *
     * @return true if the setting was successful
     */
    public boolean setAttribute(AttributeName name, String value)
    {
      attributes.put(name, value);
      
      return true;
    }
  
    /**
     * Removes an attribute value.
     *
     * @param name the attribute name
     */
    public void removeAttribute(AttributeName name)
      throws IOException
    {
      attributes.remove(name);
    }
  }

  static class AttributeHandler extends org.xml.sax.helpers.DefaultHandler {
    HashMap fileMap = new HashMap();
    AttributeName attributeName;
    String fileName;
    CharBuffer value;

    boolean inHref;
    FileAttributes fileAttributes;

    HashMap getFileMap()
    {
      return fileMap;
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
    {
      if (localName.equals("file")) {
        fileName = null;
        fileAttributes = new FileAttributes();
      }
      else if (localName.equals("href")) {
        inHref = true;
        value = CharBuffer.allocate();
      }
      else if (attributeName == null) {
        attributeName = new AttributeName(uri, localName, qName);
        value = CharBuffer.allocate();
      }
    }

    public void characters(char []buffer, int offset, int length)
    {
      if (value != null)
        value.append(buffer, offset, length);
    }

    public void endElement (String uri, String localName, String qName)
        throws SAXException
    {
      if (localName.equals("file")) {
        if (fileName != null)
          fileMap.put(fileName, fileAttributes);
        fileName = null;
        fileAttributes = null;
      }
      else if (localName.equals("href")) {
        fileName = value.close();
        value = null;
      }
      else if (attributeName == null) {
      }
      else if (localName.equals(attributeName.getLocal()) &&
               uri.equals(attributeName.getNamespace())) {
        fileAttributes.setAttribute(attributeName, value.close());
        
        value = null;
        attributeName = null;
      }
    }
  }
}
