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

package javax.jcr;

import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Represents a directory node in the repository.
 */
public interface Node extends Item {
  /**
   * Creates a new node given by the relative path.
   *
   * @param relPath relative path to the new node.
   */
  public Node addNode(String relPath)
    throws ItemExistsException,
           PathNotFoundException,
           VersionException,
           ConstraintViolationException,
           LockException,
           RepositoryException;
  
  /**
   * Creates a new node given by the relative path.
   *
   * @param relPath relative path to the new node.
   * @param primaryNodeTypeName the node type of the new node
   */
  public Node addNode(String relPath,
                      String primaryNodeTypeName)
    throws ItemExistsException,
           PathNotFoundException,
           NoSuchNodeTypeException,
           LockException,
           VersionException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Moves the source node before the dest
   *
   * @param srcChildRelPath relative path to the source item
   * @param destChildRelPath relative path to the destination item
   */
  public void orderBefore(String srcChildRelPath,
                          String destChildRelPath)
    throws UnsupportedRepositoryOperationException,
           VersionException,
           ConstraintViolationException,
           ItemNotFoundException,
           LockException,
           RepositoryException;

  /**
   * Sets a property of the node.
   *
   * @param name single-level name identifying the property
   * @param value the property's new value
   */
  public Property setProperty(String name, Value value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node.
   *
   * @param name single-level name identifying the property
   * @param value the property's new value
   * @param type the property's value type
   */
  public Property setProperty(String name, Value value, int type)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node with a value array.
   *
   * @param name single-level name identifying the property
   * @param values array of values for the property
   */
  public Property setProperty(String name, Value[] values)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node with a value array.
   *
   * @param name single-level name identifying the property
   * @param values array of values for the property
   * @param type the expected type of the property
   */
  public Property setProperty(String name,
                              Value[] values,
                              int type)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node with an array of string values
   *
   * @param name single-level name identifying the property
   * @param values array of values for the property
   */
  public Property setProperty(String name, String[] values)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;

  /**
   * Sets a property of the node with an array of string values
   *
   * @param name single-level name identifying the property
   * @param values array of values for the property
   * @param type the expected type of the property
   */
  public Property setProperty(String name, String[] values, int type)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;

  /**
   * Sets a property of the node with a single string value
   *
   * @param name single-level name identifying the property
   * @param values array of values for the property
   * @param type the expected type of the property
   */
  public Property setProperty(String name, String value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;

  /**
   * Sets a property of the node with a single string value
   *
   * @param name single-level name identifying the property
   * @param values array of values for the property
   * @param type the expected type of the property
   */
  public Property setProperty(String name, String value, int type)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node from an input stream
   *
   * @param name single-level name identifying the property
   * @param value input stream containing the data
   */
  public Property setProperty(String name, InputStream value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node from a boolean
   *
   * @param name single-level name identifying the property
   * @param value boolean data
   */
  public Property setProperty(String name, boolean value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node from a double
   *
   * @param name single-level name identifying the property
   * @param value double data
   */
  public Property setProperty(String name, double value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node from a long
   *
   * @param name single-level name identifying the property
   * @param value long data
   */
  public Property setProperty(String name, long value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Sets a property of the node from a date
   *
   * @param name single-level name identifying the property
   * @param value calendar data
   */
  public Property setProperty(String name, Calendar value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;

  /**
   * Sets a property of the node from a based on a reference to a node
   *
   * @param name single-level name identifying the property
   * @param value node reference
   */
  public Property setProperty(String name, Node value)
    throws ValueFormatException,
           VersionException,
           LockException,
           ConstraintViolationException,
           RepositoryException;
  
  /**
   * Returns the node with the given relative path.
   *
   * @param name relPath path to the given ndoe.
   */
  public Node getNode(String relPath)
    throws PathNotFoundException,
           RepositoryException;
  
  /**
   * Returns the direct child nodes.
   */
  public NodeIterator getNodes()
    throws RepositoryException;
  
  /**
   * Returns the child nodes matching the name pattern.
   */
  public NodeIterator getNodes(String namePattern)
    throws RepositoryException;
  
  /**
   * Returns the property based on the relative path.
   */
  public Property getProperty(String relPath)
    throws PathNotFoundException,
           RepositoryException;
  
  /**
   * Returns the an iterator of the properties of the node.
   */
  public PropertyIterator getProperties()
    throws RepositoryException;
  
  /**
   * Returns the an iterator of the properties of the node matching
   * the pattern.
   */
  public PropertyIterator getProperties(String namePattern)
    throws RepositoryException;

  /**
   * Returns the node's primary item.
   */
  public Item getPrimaryItem()
    throws ItemNotFoundException,
           RepositoryException;

  /**
   * Returns the node's UUID
   */
  public String getUUID()
    throws UnsupportedRepositoryOperationException,
           RepositoryException;
  
  /**
   * Returns the node's index
   */
  public int getIndex()
    throws RepositoryException;
  
  /**
   * Returns the an iterator of the references
   */
  public PropertyIterator getReferences()
    throws RepositoryException;
  
  /**
   * Returns true if the path points to a node.
   *
   * @param relPath path to a property
   */
  public boolean hasNode(String relPath)
    throws RepositoryException;
  
  /**
   * Returns true if the path points to a property.
   *
   * @param relPath path to a property
   */
  public boolean hasProperty(String relPath)
    throws RepositoryException;
  
  /**
   * Returns true if the node has child nodes.
   */
  public boolean hasNodes()
    throws RepositoryException;
  
  /**
   * Returns true if the node has any properties.
   */
  public boolean hasProperties()
    throws RepositoryException;
  
  /**
   * Returns the node's primary type.
   */
  public NodeType getPrimaryNodeType()
    throws RepositoryException;
  
  /**
   * Returns any mixin types for the node.
   */
  public NodeType[] getMixinNodeTypes()
    throws RepositoryException;
  
  /**
   * Returns true if the node supports the given node tyep.
   */
  public boolean isNodeType(String nodeTypeName)
    throws RepositoryException;
  
  /**
   * Adds a mixin type to the node.
   */
  public void addMixin(String mixinName)
    throws NoSuchNodeTypeException,
           VersionException,
           ConstraintViolationException,
           LockException,
           RepositoryException;
  
  /**
   * Removes a mixin type to the node.
   */
  public void removeMixin(String mixinName)
    throws NoSuchNodeTypeException,
           VersionException,
           ConstraintViolationException,
           LockException,
           RepositoryException;
  
  /**
   * Returns true if the given mixin type can be added to the node.
   */
  public boolean canAddMixin(String mixinName)
    throws NoSuchNodeTypeException,
           RepositoryException;
  
  /**
   * Returns a description of the node.
   */
  public NodeDefinition getDefinition()
    throws RepositoryException;
  
  /**
   * Checks in a new version for to the node.
   */
  public Version checkin()
    throws VersionException,
           UnsupportedRepositoryOperationException,
           InvalidItemStateException,
           LockException,
           RepositoryException;
  
  /**
   * Checks out a version.
   */
  public void checkout()
    throws UnsupportedRepositoryOperationException,
           LockException,
           RepositoryException;
  
  /**
   * Mark the version merge as complete.
   */
  public void doneMerge(Version version)
    throws VersionException,
           InvalidItemStateException,
           UnsupportedRepositoryOperationException,
           RepositoryException;
  
  /**
   * Cancel a version merge.
   */
  public void cancelMerge(Version version)
    throws VersionException,
           InvalidItemStateException,
           UnsupportedRepositoryOperationException,
           RepositoryException;
  
  /**
   * Updates a workspace
   */
  public void update(String srcWorkspaceName)
    throws NoSuchWorkspaceException,
           AccessDeniedException,
           LockException,
           InvalidItemStateException,
           RepositoryException;
  
  /**
   * Merges child nodes.
   */
  public NodeIterator merge(String srcWorkspace, boolean bestEffort)
    throws NoSuchWorkspaceException,
           AccessDeniedException,
           MergeException,
           LockException,
           InvalidItemStateException,
           RepositoryException;
  
  /**
   * Returns the node path to a workspace.
   */
  public String getCorrespondingNodePath(String workspaceName)
    throws ItemNotFoundException,
           NoSuchWorkspaceException,
           AccessDeniedException,
           RepositoryException;
  
  /**
   * Returns true for a checked out node.
   */
  public boolean isCheckedOut()
    throws RepositoryException;
  
  /**
   * Restore the node based on an older version.
   */
  public void restore(String versionName, boolean removeExisting)
    throws VersionException,
           ItemExistsException,
           UnsupportedRepositoryOperationException,
           LockException,
           InvalidItemStateException,
           RepositoryException;
  
  /**
   * Restore the node based on an older version.
   */
  public void restore(Version version, boolean removeExisting)
    throws VersionException,
           ItemExistsException,
           UnsupportedRepositoryOperationException,
           LockException,
           RepositoryException;
  
  /**
   * Restore the node based on an older version.
   */
  public void restore(Version version,
                      String relPath,
                      boolean removeExisting)
    throws PathNotFoundException,
           ItemExistsException,
           VersionException,
           ConstraintViolationException,
           UnsupportedRepositoryOperationException,
           LockException,
           InvalidItemStateException,
           RepositoryException;
  
  /**
   * Restore the node based on an older version.
   */
  public void restoreByLabel(String versionLabel,
                             boolean removeExisting)
    throws VersionException,
           ItemExistsException,
           UnsupportedRepositoryOperationException,
           LockException,
           InvalidItemStateException,
           RepositoryException;
  
  /**
   * Returns the node's version history.
   */
  public VersionHistory getVersionHistory()
    throws UnsupportedRepositoryOperationException,
           RepositoryException;
  
  /**
   * Returns the base version.
   */
  public Version getBaseVersion()
    throws UnsupportedRepositoryOperationException,
           RepositoryException;
  
  /**
   * Lock the node.
   */
  public Lock lock(boolean isDeep, boolean isSessionScoped)
    throws UnsupportedRepositoryOperationException,
           LockException,
           AccessDeniedException,
           InvalidItemStateException,
           RepositoryException;
  
  /**
   * Returns the current lock.
   */
  public Lock getLock()
    throws UnsupportedRepositoryOperationException,
           LockException,
           AccessDeniedException,
           RepositoryException;
  
  /**
   * Unlocks the node.
   */
  public void unlock()
    throws UnsupportedRepositoryOperationException,
           LockException,
           AccessDeniedException,
           InvalidItemStateException,
           RepositoryException;
  
  /**
   * Returns true if the node owns a lock.
   */
  public boolean holdsLock()
    throws RepositoryException;
  
  /**
   * Returns true if the node is locked.
   */
  public boolean isLocked()
    throws RepositoryException;
}
