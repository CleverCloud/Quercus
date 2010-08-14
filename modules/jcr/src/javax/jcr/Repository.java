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

/**
 * The top-level interface for a content repository.  The Repository
 * object will generally live in JNDI in java:comp/env/jcr/*
 */
public interface Repository {
  public static final String SPEC_VERSION_DESC
    = "jcr.specification.version";
  public static final String SPEC_NAME_DESC
    = "jcr.specification.name";
  public static final String REP_VENDOR_DESC
    = "jcr.repository.vendor";
  public static final String REP_VENDOR_URL_DESC
    = "jcr.repository.vendor.url";
  public static final String REP_NAME_DESC
    = "jcr.repository.name";
  public static final String REP_VERSION_DESC
    = "jcr.repository.version";
  public static final String LEVEL_1_SUPPORTED
    = "level.1.supported";
  public static final String LEVEL_2_SUPPORTED
    = "level.2.supported";
  public static final String OPTION_TRANSACTIONS_SUPPORTED
    = "option.transactions.supported";
  public static final String OPTION_VERSIONING_SUPPORTED
    = "option.versioning.supported";
  public static final String OPTION_OBSERVATION_SUPPORTED
    = "option.observation.supported";
  public static final String OPTION_LOCKING_SUPPORTED
    = "option.locking.supported";
  public static final String OPTION_QUERY_SQL_SUPPORTED
    = "option.query.sql.supported";
  public static final String QUERY_XPATH_POS_INDEX
    = "query.xpath.pos.index";
  public static final String QUERY_XPATH_DOC_ORDER
    = "query.xpath.doc.order";

  /**
   * Returns the Repository's feature descriptor keys.
   */
  public String []getDescriptorKeys();

  /**
   * Returns the value for Repository feature descriptor keys.
   */
  public String getDescriptor(String key);

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
           RepositoryException;

  /**
   * Opens a new session with the default workspace.
   *
   * @param credentials security credentials
   */
  public Session login(Credentials credentials)
    throws LoginException, RepositoryException;
  
  /**
   * Opens a new session with the default (anonymous) security
   * credentials.
   *
   * @param workspaceName the name of the workspace to open.
   */
  public Session login(String workspaceName)
    throws LoginException,
           NoSuchWorkspaceException,
           RepositoryException;

  /**
   * Opens a new session with the default workspace and security credentials.
   */
  public Session login()
    throws LoginException,
           RepositoryException;
}
