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
package javax.jcr.version;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

public interface VersionHistory extends Node {
  public String getVersionableUUID()
    throws RepositoryException;
  
  public Version getRootVersion()
    throws RepositoryException;
  
  public VersionIterator getAllVersions()
    throws RepositoryException;
  
  public Version getVersion(String versionName)
    throws VersionException,
           RepositoryException;
  
  public Version getVersionByLabel(String label)
    throws RepositoryException;
  
  public void addVersionLabel(String versionName,
                              String label,
                              boolean moveLabel)
    throws VersionException,
           RepositoryException;
  
  public void removeVersionLabel(String label)
    throws VersionException,
           RepositoryException;
  
  public boolean hasVersionLabel(String label)
    throws RepositoryException;
  
  public boolean hasVersionLabel(Version version, String label)
    throws VersionException,
           RepositoryException;
  
  public String[] getVersionLabels()
    throws RepositoryException;
  
  public String[] getVersionLabels(Version version)
    throws VersionException,
           RepositoryException;

  public void removeVersion(String versionName)
    throws ReferentialIntegrityException,
           AccessDeniedException,
           UnsupportedRepositoryOperationException,
           VersionException,
           RepositoryException;
}
