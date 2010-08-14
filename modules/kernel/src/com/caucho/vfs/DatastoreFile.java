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

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

@Entity
public class DatastoreFile
{
  private String _pathname;
  private int _blockSize;
  private int _fileSize;
  
  @Enumerated
  private byte []_block = new byte[1024 * 1024 * 4];
  
  //@Enumerated
  //private DatastoreBlock _block;
  
  @Transient DatastorePath _path;
  
  public DatastoreFile(String pathname)
  {
    _pathname = pathname;
  }
  
  public String getPathname()
  {
    return _pathname;
  }
  
  public long getLength()
    throws IOException
  {
    return _fileSize;
  }
  
  public void setPath(DatastorePath path)
  {
    _path = path;
  }
  
  public DatastorePath getPath()
  {
    return _path;
  }
  
  public void write(byte []buffer, int off, int len)
    throws IOException
  {
    EntityManager em = _path.getEntityManager();
    
    if (! DatastorePath.IS_USE_HASHMAP)
      em.getTransaction().begin();
    
    try {
      System.arraycopy(buffer, off, _block, _fileSize, len);

      _fileSize += len;
    } catch (Exception e) {
      
      e.printStackTrace();
    } finally {
      if (! DatastorePath.IS_USE_HASHMAP) {
        em.merge(this);
        em.getTransaction().commit();
      }
    }
  }
  
  public void write(int position, byte []buffer, int off, int len)
    throws IOException
  {
    EntityManager em = _path.getEntityManager();
    
    if (! DatastorePath.IS_USE_HASHMAP)
      em.getTransaction().begin();
    
    try {
      System.arraycopy(buffer, off, _block, position, len);
    
    if (_fileSize < position + len)
      _fileSize = position + len;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (! DatastorePath.IS_USE_HASHMAP) {
        em.merge(this);
        em.getTransaction().commit();
      }
    }
  }
  
  public int read(int position, byte []buffer, int off, int len)
    throws IOException
  {
    len = Math.min(_fileSize - position, len);
    
    if (len <= 0)
      return 0;
    
    System.arraycopy(_block, position, buffer, off, len);
    
    return len;
  }
}
