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
import org.xml.sax.SAXException;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents an open session to a Repository workspace.
 */
abstract public class BaseSession implements Session {
  private boolean _isActive = true;
  
  /**
   * Returns the owning repository.
   */
  abstract public Repository getRepository();

  /**
   * Returns the user who opened this session.
   */
  public String getUserID()
  {
    return null;
  }

  /**
   * Returns a session attribute.
   *
   * @param name the session attribute name
   */
  public Object getAttribute(String name)
  {
    return null;
  }

  /**
   * Returns an array of the session attribute names.
   */
  public String[] getAttributeNames()
  {
    return new String[0];
  }

  /**
   * Returns the repository's workspace for this session.
   */
  public Workspace getWorkspace()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Create a new session with the new credentials.
   *
   * @param credentials security credentials for the new sessions.
   */
  public Session impersonate(Credentials credentials)
    throws LoginException, RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Returns the session's root node.
   */
  abstract public Node getRootNode()
    throws RepositoryException;

  /**
   * Finds a node by its UUID
   *
   * @param uuid the node's UUID.
   */
  public Node getNodeByUUID(String uuid)
    throws ItemNotFoundException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Returns an item based on an absolute path.
   *
   * @param absPath the path locating the item.
   *
   * @throws PathNotFoundException if the path does not name an item
   */
  public Item getItem(String absPath)
    throws PathNotFoundException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Returns true if the item named by the path exists.
   *
   * @param absPath a path locating the item.
   */
  public boolean itemExists(String absPath)
    throws RepositoryException
  {
    try {
      return getItem(absPath) != null;
    } catch (PathNotFoundException e) {
      return false;
    }
  }

  /**
   * Moves the node given by the source path to the destination path.
   *
   * @param srcAbsPath the absolute path name of the source node
   * @param destAbsPath the absolute path name of the destination node
   */
  public void move(String srcAbsPath, String destAbsPath)
    throws ItemExistsException,
           PathNotFoundException,
           VersionException,
           ConstraintViolationException,
           LockException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Saves changes to the workspace.
   */
  public void save()
    throws AccessDeniedException,
           ItemExistsException,
           ConstraintViolationException,
           InvalidItemStateException,
           VersionException, LockException,
           NoSuchNodeTypeException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Updates changes from the repository.
   */
  public void refresh(boolean keepChanges)
    throws RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Returns true if the session has changes.
   */
  public boolean hasPendingChanges()
    throws RepositoryException
  {
    return false;
  }

  /**
   * Returns the session's value factory.
   */
  public ValueFactory getValueFactory()
    throws UnsupportedRepositoryOperationException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Checks if the session can perform the given actions for the path.
   *
   * @param absPath absolute path to a node.
   * @param actions actions attempted on the node.
   */
  public void checkPermission(String absPath, String actions)
    throws java.security.AccessControlException,
           RepositoryException
  {
  }

  /**
   * Returns a SAX ContentHandler to important data.
   *
   * @param parentAbsPath the absolute path of the parent node
   */
  public ContentHandler getImportContentHandler(String parentAbsPath,
                                                int uuidBehavior)
    throws PathNotFoundException,
           ConstraintViolationException,
           VersionException,
           LockException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Import data based on an XML stream.
   *
   * @param parentAbsPath path to the node which will be the data's parent.
   * @param in InputStream to the XML data
   */
  public void importXML(String parentAbsPath,
                        InputStream in,
                        int uuidBehavior)
    throws IOException,
           PathNotFoundException,
           ItemExistsException,
           ConstraintViolationException,
           VersionException,
           InvalidSerializedDataException,
           LockException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Exports XML data from the given node based on the system view.
   *
   * @param absPath path to the node serving as root to export
   * @param contentHandler SAX ContentHandler to receive the XML
   */
  public void exportSystemView(String absPath,
                               ContentHandler contentHandler,
                               boolean skipBinary,
                               boolean noRecurse)
    throws PathNotFoundException,
           SAXException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }
  
  /**
   * Exports XML data from the given node based on the system view.
   *
   * @param absPath path to the node serving as root to export
   * @param out OutputStream to receive the XML
   */
  public void exportSystemView(String absPath,
                               OutputStream out,
                               boolean skipBinary,
                               boolean noRecurse)
    throws IOException,
           PathNotFoundException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }
  
  /**
   * Exports XML data from the given node based on the document view.
   *
   * @param absPath path to the node serving as root to export
   * @param out OutputStream to receive the XML
   */
  public void exportDocumentView(String absPath,
                                 ContentHandler contentHandler,
                                 boolean skipBinary,
                                 boolean noRecurse)
    throws PathNotFoundException,
           SAXException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }
  
  /**
   * Exports XML data from the given node based on the document view.
   *
   * @param absPath path to the node serving as root to export
   * @param out OutputStream to receive the XML
   */
  public void exportDocumentView(String absPath,
                                 OutputStream out,
                                 boolean skipBinary,
                                 boolean noRecurse)
    throws IOException,
           PathNotFoundException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }
  
  /**
   * Exports XML data from the given node based on the document view.
   *
   * @param absPath path to the node serving as root to export
   * @param out OutputStream to receive the XML
   */
  public void setNamespacePrefix(String newPrefix,
                                 String existingUri)
    throws NamespaceException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }
  
  /**
   * Returns the session's namespace prefixes.
   */
  public String[] getNamespacePrefixes()
    throws RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }
  
  /**
   * Returns the URI for a given namespace prefix.
   */
  public String getNamespaceURI(String prefix)
    throws NamespaceException,
           RepositoryException
  {
    return null;
  }
  
  /**
   * Returns the prefix for a given URI.
   */
  public String getNamespacePrefix(String uri)
    throws NamespaceException,
           RepositoryException
  {
    return null;
  }
  
  /**
   * Close the session.
   */
  public void logout()
  {
    _isActive = false;
  }
  
  /**
   * Return true if the session is active.
   */
  public boolean isLive()
  {
    return _isActive;
  }
  
  /**
   * Adds a lock token.
   */
  public void addLockToken(String lt)
    throws LockException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }
  
  /**
   * Returns the current lock tokens.
   */
  public String[] getLockTokens()
  {
    return new String[0];
  }
  
  /**
   * Removes the named lock token.
   */
  public void removeLockToken(String lt)
  {
  }
}
