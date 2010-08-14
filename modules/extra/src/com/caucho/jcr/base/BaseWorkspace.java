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

package com.caucho.jcr.base;

import org.xml.sax.ContentHandler;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a open workspace to a repository.
 */
public class BaseWorkspace implements Workspace {
  private String _name;
  private Session _session;

  public BaseWorkspace(String name, Session session)
  {
    _name = name;
    _session = session;
  }
  
  /**
   * Returns the owning session.
   */
  public Session getSession()
  {
    return _session;
  }
  
  /**
   * Returns the workspace name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Copy from one node to another.
   */
  public void copy(String srcAbsPath, String destAbsPath)
    throws ConstraintViolationException,
           VersionException,
           AccessDeniedException,
           PathNotFoundException,
           ItemExistsException,
           LockException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Copy from one node to another, starting in another workspace
   */
  public void copy(String srcWorkspace,
                   String srcAbsPath,
                   String destAbsPath)
    throws NoSuchWorkspaceException,
           ConstraintViolationException,
           VersionException,
           AccessDeniedException,
           PathNotFoundException,
           ItemExistsException,
           LockException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Clones a node from another workspace.
   */
  public void clone(String srcWorkspace,
                    String srcAbsPath,
                    String destAbsPath,
                    boolean removeExisting)
    throws NoSuchWorkspaceException,
           ConstraintViolationException,
           VersionException,
           AccessDeniedException,
           PathNotFoundException,
           ItemExistsException,
           LockException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Move a node from another workspace.
   */
  public void move(String srcAbsPath, String destAbsPath)
    throws ConstraintViolationException,
           VersionException,
           AccessDeniedException,
           PathNotFoundException,
           ItemExistsException,
           LockException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Restores from given versions.
   */
  public void restore(Version[] versions, boolean removeExisting)
    throws ItemExistsException,
           UnsupportedRepositoryOperationException,
           VersionException,
           LockException,
           InvalidItemStateException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns the query manager.
   */
  public QueryManager getQueryManager()
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns the namespace registry.
   */
  public NamespaceRegistry getNamespaceRegistry()
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns the node type manager.
   */
  public NodeTypeManager getNodeTypeManager()
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns the observation manager.
   */
  public ObservationManager getObservationManager()
    throws UnsupportedRepositoryOperationException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns the workspace names.
   */
  public String[] getAccessibleWorkspaceNames()
    throws RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Returns a handler for importing data.
   */
  public ContentHandler getImportContentHandler(String parentAbsPath,
                                                int uuidBehavior)
    throws PathNotFoundException,
           ConstraintViolationException,
           VersionException,
           LockException,
           AccessDeniedException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Import based on XML.
   */
  public void importXML(String parentAbsPath,
                        InputStream in,
                        int uuidBehavior)
    throws IOException,
           PathNotFoundException,
           ItemExistsException,
           ConstraintViolationException,
           InvalidSerializedDataException,
           LockException,
           AccessDeniedException,
           RepositoryException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
