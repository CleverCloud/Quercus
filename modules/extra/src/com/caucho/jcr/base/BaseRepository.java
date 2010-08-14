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

import javax.jcr.*;

/**
 * Abstract interface for a content repository.  The Repository
 * object will generally live in JNDI in java:comp/env/jcr/*
 */
public class BaseRepository implements Repository {
  /**
   * Returns the Repository's feature descriptor keys.
   */
  public String []getDescriptorKeys()
  {
    return new String[] {
      SPEC_VERSION_DESC,
      SPEC_NAME_DESC,
      REP_VENDOR_DESC,
      REP_VENDOR_URL_DESC,
      REP_NAME_DESC,
      LEVEL_1_SUPPORTED,
      LEVEL_2_SUPPORTED,
      OPTION_TRANSACTIONS_SUPPORTED,
      OPTION_VERSIONING_SUPPORTED,
      OPTION_OBSERVATION_SUPPORTED,
      OPTION_LOCKING_SUPPORTED,
      OPTION_QUERY_SQL_SUPPORTED,
      QUERY_XPATH_POS_INDEX,
      QUERY_XPATH_DOC_ORDER
    };
  }

  /**
   * Returns the value for Repository feature descriptor keys.
   */
  public String getDescriptor(String key)
  {
    if (SPEC_VERSION_DESC.equals(key))
      return "1.0.1";
    else if (SPEC_NAME_DESC.equals(key))
      return "Java Content Repository";
    else if (REP_VENDOR_DESC.equals(key))
      return "Caucho Technology, inc.";
    else if (REP_VENDOR_URL_DESC.equals(key))
      return "http://www.caucho.com";
    else if (REP_NAME_DESC.equals(key))
      return "Resin";
    else if (LEVEL_1_SUPPORTED.equals(key))
      return "false";
    else if (LEVEL_2_SUPPORTED.equals(key))
      return "false";
    else if (OPTION_TRANSACTIONS_SUPPORTED.equals(key))
      return "false";
    else if (OPTION_OBSERVATION_SUPPORTED.equals(key))
      return "false";
    else if (OPTION_VERSIONING_SUPPORTED.equals(key))
      return "false";
    else if (OPTION_LOCKING_SUPPORTED.equals(key))
      return "false";
    else if (OPTION_QUERY_SQL_SUPPORTED.equals(key))
      return "false";
    else if (QUERY_XPATH_POS_INDEX.equals(key))
      return "1";
    else if (QUERY_XPATH_DOC_ORDER.equals(key))
      return "true";
    else
      return null;
  }

  /**
   * Opens a new session, specifying the security credentials
   * and a workspace.
   *
   * @param credentials the security credentials
   * @param workspaceName select an optional workspace
   */
  public Session login(Credentials credentials, String workspaceName)
    throws LoginException,
           NoSuchWorkspaceException,
           RepositoryException
  {
    throw new UnsupportedRepositoryOperationException(getClass().getName());
  }

  /**
   * Opens a new session with the default workspace.
   *
   * @param credentials security credentials
   */
  public Session login(Credentials credentials)
    throws LoginException, RepositoryException
  {
    return login(credentials, null);
  }
  
  /**
   * Opens a new session with the default (anonymous) security
   * credentials.
   *
   * @param workspaceName the name of the workspace to open.
   */
  public Session login(String workspaceName)
    throws LoginException,
           NoSuchWorkspaceException,
           RepositoryException
  {
    return login(null, workspaceName);
  }

  /**
   * Opens a new session with the default workspace and security credentials.
   */
  public Session login()
    throws LoginException,
           RepositoryException
  {
    return login(null, null);
  }
}
