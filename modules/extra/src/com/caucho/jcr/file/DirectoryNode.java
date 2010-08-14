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

package com.caucho.jcr.file;

import com.caucho.jcr.base.BaseNode;
import com.caucho.jcr.base.BaseNodeIterator;
import com.caucho.jcr.base.BaseNodeType;
import com.caucho.vfs.Path;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.io.IOException;

/**
 * Represents a node in the filesystem
 */
public class DirectoryNode extends BaseNode {
  private FileSession _session;
  private Path _path;

  DirectoryNode(FileSession session, Path path)
  {
    _session = session;
    _path = path;
  }

  /**
   * Returns the node type.
   */
  public NodeType getPrimaryNodeType()
  {
    return BaseNodeType.NT_FOLDER;
  }
  
  /**
   * Returns the full absolute pathname of the item.
   */
  public String getPath()
    throws RepositoryException
  {
    return _path.getPath();
  }
  
  /**
   * Returns the tail name of the item.
   */
  public String getName()
    throws RepositoryException
  {
    return _path.getTail();
  }

  /**
   * Returns the owning session.
   */
  public Session getSession()
    throws RepositoryException
  {
    return _session;
  }

  /**
   * Returns the children nodes.
   */
  public NodeIterator getNodes()
    throws RepositoryException
  {
    try {
      String []list = _path.list();

      BaseNode []children = new BaseNode[list.length];

      for (int i = 0; i < list.length; i++) {
        Path childPath = _path.lookup(list[i]);

        if (childPath.isDirectory())
          children[i] = new DirectoryNode(_session, childPath);
        else
          children[i] = new FileNode(_session, childPath);
      }

      return new BaseNodeIterator(children);
    } catch (IOException e) {
      throw new RepositoryException(e);
    }
  }

  public String toString()
  {
    return "FileNode[" + _path + "]";
  }
}
