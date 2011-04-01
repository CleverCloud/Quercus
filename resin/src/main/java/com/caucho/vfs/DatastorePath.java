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
 * @author Nam Nguyen
 */

package com.caucho.vfs;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.caucho.util.CharBuffer;

public class DatastorePath extends Path
{
  private static byte []NEWLINE = getNewlineString().getBytes();
  
  public static final boolean IS_USE_HASHMAP = false;
  
  private static HashMap<String, DatastoreFile> _fileMap
    = new HashMap<String, DatastoreFile>();
  
  private static DatastorePath PWD;
  
  protected DatastorePath _root;
  protected BindPath _bindRoot;
  private String _pathname;
  
  private DatastoreFile _file;
  
  @PersistenceContext(name="test")
  private EntityManager _entityManager;
  
  public DatastorePath(String path)
  {
    this(null, path);

    if (_root == null) {
      _root = new DatastorePath(null, "/");
      _root._root = _root;

      if (PWD == null)
        PWD = _root;
    }
  }
  
  protected DatastorePath(DatastorePath root,
                          String pathname)
  {
    super(root);

    if (pathname == null)
      throw new NullPointerException();

    _pathname = pathname;

    if (root != null) {
      _root = root;
      _bindRoot = root._bindRoot;
    }
  }
  
  public EntityManager getEntityManager()
  {
    return _entityManager;
  }
  
  public static DatastoreFile getFile(String name)
  {
    if (IS_USE_HASHMAP)
      return _fileMap.get(name);
    
    DatastorePath path = new DatastorePath(null, name);
    
    EntityManager em = path._entityManager;
    
    Query query
      = em.createQuery("SELECT FROM " + DatastoreFile.class
                       + " WHERE _pathname = :name");
    
    query.setParameter("name", name);
    
    List resultList = query.getResultList();
    
    if (resultList.size() == 0)
      return null;
    
    DatastoreFile file = (DatastoreFile) resultList.get(0);
    file.setPath(path);
    
    return file;
  }
  
  public static void dumpFiles()
  {
    if (IS_USE_HASHMAP) {
      System.out.println("DatastorePath->dumpFiles(): " + _fileMap.size());
      
      for (Map.Entry<String,DatastoreFile> entry : _fileMap.entrySet()) {
        System.out.println("\t" + entry.getKey() + " . " + entry.getValue());
      }
    }
    else {
      DatastorePath path = PWD;
      
      if (PWD == null)
        path = new DatastorePath(null, "/");
      
      Query query = path._entityManager.createQuery("SELECT FROM "
                                                    + DatastoreFile.class);

      List resultList = query.getResultList();
      
      System.out.println("DatastorePath->dumpFiles(): " + resultList.size());
      
      for (Object obj : resultList) {
        DatastoreFile file = (DatastoreFile) obj;
        System.out.println("\t" + file.getPathname());
      }
    }
    
  }

  /**
   * Returns the path.  e.g. for HTTP, returns the part after the
   * host and port.
   */
  public String getPath()
  {
    return _pathname;
  }
  
  /**
   * Tests if the file exists.
   */
  public boolean exists()
  {
    if (IS_USE_HASHMAP) {
      return _fileMap.get(_pathname) != null;
    }
    else {
      EntityManager em = _entityManager;
      
      Query query
        = em.createQuery("SELECT FROM " + DatastoreFile.class
                         + " WHERE _pathname = :name");
    
      query.setParameter("name", _pathname);
    
      List resultList = query.getResultList();
      
      return resultList.size() > 0;
    }
  }
  
  /**
   * Removes the file or directory named by this path.
   *
   * @return true if successful
   */
  public boolean remove()
    throws IOException
  {
    if (IS_USE_HASHMAP) {
      _fileMap.remove(_pathname);
      
      _file = null;
    }
    else {
      EntityManager em = _entityManager;
      
      Query query
        = em.createQuery("SELECT FROM " + DatastoreFile.class
                         + " WHERE _pathname = :name");
  
      query.setParameter("name", _pathname);
  
      List resultList = query.getResultList();
      
      try {
        em.getTransaction().begin();
        
        for (Object obj : resultList) {
          _entityManager.remove(obj);
        }
      } finally {
        _entityManager.getTransaction().commit();
      }
      
      _file = null;
    }
    
    return true;
  }
  
  public String getScheme()
  {
    return "datastore";
  }

  /**
   * Returns the stream implementation for a read stream.
   */
  public StreamImpl openReadImpl()
    throws IOException
  {
    DatastoreInputStream is = new DatastoreInputStream(getDatastoreFile());
    
    return new DatastoreReadStream(is, this);
  }
  
  public StreamImpl openWriteImpl() throws IOException
  {
    DatastoreOutputStream os = new DatastoreOutputStream(getDatastoreFile());
    
    DatastoreWriteStream ws = new DatastoreWriteStream(os, this);

    ws.setNewline(NEWLINE);

    return ws;
  }
  
  /**
   * Returns the stream implementation for a random-access stream.
   */
  public DatastoreRandomAccessStream openRandomAccess()
    throws IOException
  {
    DatastoreRandomAccessFile file
      = new DatastoreRandomAccessFile(getDatastoreFile());
    
    return new DatastoreRandomAccessStream(file);
  }
  
  public DatastoreFile getDatastoreFile()
  {
    if (_file != null)
      return _file;
    
    if (IS_USE_HASHMAP) {
      _file = _fileMap.get(_pathname);
      
      if (_file == null) {
        _file = new DatastoreFile(_pathname);
        
        _fileMap.put(_pathname, _file);
      }
    }
    else {
      EntityManager em = _entityManager;
      
      Query query = em.createQuery("SELECT FROM " + DatastoreFile.class
                                   + " WHERE _pathname = :path");
      query.setParameter("path", _pathname);
      
      List resultList = query.getResultList();
      
      if (resultList.size() != 0)
        _file = (DatastoreFile) resultList.get(0);
      else {
        em.getTransaction().begin();
        
        try {
          _file = new DatastoreFile(_pathname);
          em.persist(_file);
        } finally {
          em.getTransaction().commit();
        }
      }
    }
    
    _file.setPath(this);
    
    return _file;
  }
  
  /**
   * Lookup the path, handling windows weirdness
   */
  protected Path schemeWalk(String userPath,
                            Map<String,Object> attributes,
                            String filePath,
                            int offset)
  {
    if (! isWindows())
      return schemeWalkImpl(userPath, attributes, filePath, offset);
    
    if (filePath.length() < offset + 2)
      return schemeWalkImpl(userPath, attributes, filePath, offset);

    char ch1 = filePath.charAt(offset + 1);
    char ch2 = filePath.charAt(offset);

    if ((ch2 == '/' || ch2 == _separatorChar)
    && (ch1 == '/' || ch1 == _separatorChar))
      return schemeWalkImpl(userPath, attributes, filePath.substring(offset), 0);
    else
      return schemeWalkImpl(userPath, attributes, filePath, offset);
  }
  
  protected Path schemeWalkImpl(String userPath,
                                Map<String,Object> attributes,
                                String filePath,
                                int offset)
  {
    String canonicalPath;

    if (filePath.length() > offset
        && (filePath.charAt(offset) == '/'
        || filePath.charAt(offset) == _separatorChar))
      canonicalPath = normalizePath("/", filePath, offset, _separatorChar);
    else
      canonicalPath = normalizePath(_pathname, filePath, offset,
                                    _separatorChar);


    return fsWalk(userPath, attributes, canonicalPath);
  }
  
  public Path fsWalk(String userPath,
      Map<String,Object> attributes,
      String path)
  {
    return new DatastorePath(_root, path);
  }
  
  static protected String normalizePath(String oldPath, 
                                        String newPath,
                                         int offset,
                                         char separatorChar)
  {
    CharBuffer cb = new CharBuffer();
    normalizePath(cb, oldPath, newPath, offset, separatorChar);
    return cb.toString();
  }

  static protected void normalizePath(CharBuffer cb, String oldPath, 
                                      String newPath, int offset,
                                      char separatorChar)
  {
    cb.clear();
    cb.append(oldPath);
    if (cb.length() == 0 || cb.getLastChar() != '/')
    cb.append('/');

    int length = newPath.length();
    int i = offset;
    while (i < length) {
      char ch = newPath.charAt(i);
      char ch2;

      switch (ch) {
        default:
          if (ch != separatorChar) {
            cb.append(ch);
            i++;
            break;
          }
        // the separator character falls through to be treated as '/'

        case '/':
          // "//" -> "/"
          if (cb.getLastChar() != '/')
            cb.append('/');
          i++;
          break;

        case '.':
          if (cb.getLastChar() != '/') {
            cb.append('.');
            i++;
            break;
          }

          // "/." -> ""
          if (i + 1 >= length) {
            i += 2;
            break;
          }

          switch (newPath.charAt(i + 1)) {
            default:
              if (newPath.charAt(i + 1) != separatorChar) {
                cb.append('.');
                i++;
                break;
              }
            // the separator falls through to be treated as '/'

            // "/./" -> "/"
            case '/':
              i += 2;
              break;

              // "foo/.." -> ""
            case '.':
              if ((i + 2 >= length ||
                  (ch2 = newPath.charAt(i + 2)) == '/' || ch2 == separatorChar) &&
                  cb.getLastChar() == '/') {
                int segment = cb.lastIndexOf('/', cb.length() - 2);
                if (segment == -1) {
                  cb.clear();
                  cb.append('/');
                } else
                  cb.setLength(segment + 1);

                i += 3;
              } else {
                cb.append('.');
                i++;
              }
              
              break;
          }
      }
    }
  }
  
  public String toString()
  {
    return "DatastorePath[" + _pathname + "," + _file + "]";
  }
}
