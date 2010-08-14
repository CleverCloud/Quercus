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
import com.caucho.util.NullIterator;
import com.caucho.vfs.Path;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Represents a virtual filesystem.
 */
public class ApplicationPath extends AbstractPath {
  /**
   * Returns true if the named file is a file.
   *
   * @param path the requested relative path
   */
  public boolean isFile(String path,
                        HttpServletRequest request,
                        ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).isFile();
  }
  
  /**
   * Returns true if the named file is a directory.
   *
   * @param path the requested relative path
   */
  public boolean isDirectory(String path,
                             HttpServletRequest request,
                             ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).isDirectory();
  }
  
  /**
   * Returns true if the file can be read.
   *
   * @param path the requested relative path
   */
  public boolean canRead(String path,
                         HttpServletRequest request,
                         ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).canRead();
  }
  
  /**
   * Returns true if the file exists.
   *
   * @param path the requested relative path
   */
  public boolean exists(String path,
                        HttpServletRequest request,
                        ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).exists();
  }
  
  /**
   * Returns the length of the named file.
   *
   * @param path the requested relative path
   */
  public long getLength(String path,
                        HttpServletRequest request,
                        ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).getLength();
  }
  
  /**
   * Returns the last modified time of the named file.
   *
   * @param path the requested relative path
   */
  public long getLastModified(String path,
                              HttpServletRequest request,
                              ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).getLastModified();
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
    return NullIterator.create();
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
  public void removeAttribute(String name, 
                              String path,
                              HttpServletRequest request,
                              ServletContext app)
    throws IOException
  {
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
    return getPath(path, request, app).list();
  }
  
  /**
   * Creates the named directory.
   *
   * @param path the requested relative path
   *
   * @return true if the creation succeeded.
   */
  public boolean mkdir(String path,
                       HttpServletRequest request,
                       ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).mkdir();
  }
  
  /**
   * Removes the named directory.
   *
   * @param path the requested relative path
   *
   * @return true if the remove succeeded.
   */
  public boolean rmdir(String path,
                       HttpServletRequest request,
                       ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).remove();
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
    return getPath(path, request, app).remove();
  }
  
  /**
   * Opens an OutputStream for writing.
   *
   * @param path the requested relative path
   *
   * @return the output stream to the resource.
   */
  public OutputStream openWrite(String path,
                                HttpServletRequest request,
                                ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).openWrite();
  }
  
  /**
   * Opens an InputStream for reading
   *
   * @param path the requested relative path
   *
   * @return the input stream to the resource.
   */
  public InputStream openRead(String path,
                              HttpServletRequest request,
                              ServletContext app)
    throws IOException
  {
    return getPath(path, request, app).openRead();
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

    return appDir.lookup("./" + path);
  }
}
