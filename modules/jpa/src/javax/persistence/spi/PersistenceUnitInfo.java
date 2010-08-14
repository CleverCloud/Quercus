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

package javax.persistence.spi;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.sql.DataSource;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Container interface when creating an EntityManagerFactory.
 */
public interface PersistenceUnitInfo {
  /**
   * Returns the name.
   */
  public String getPersistenceUnitName();

  /**
   * Returns the full class name of the persistence provider.
   */
  public String getPersistenceProviderClassName();

  /**
   * Returns the transaction handling.
   */
  public PersistenceUnitTransactionType getTransactionType();

  /**
   * Returns the jta-enabled data source.
   */
  public DataSource getJtaDataSource();

  /**
   * Returns the non-jta-enabled data source.
   */
  public DataSource getNonJtaDataSource();

  /**
   * Returns the mapping file names.  The files are resource-loadable
   * from the classpath.
   */
  public List<String> getMappingFileNames();

  /**
   * Returns the list of jars for the managed classes.
   */
  public List<URL> getJarFileUrls();

  /**
   * Returns the root persistence unit.
   */
  public URL getPersistenceUnitRootUrl();

  /**
   * Returns the list of managed classes.
   */
  public List<String> getManagedClassNames();

  /**
   * Returns true if only listed classes are allowed.
   */
  public boolean excludeUnlistedClasses();
  
  /**
   * @since JPA 2.0
   */
  public SharedCacheMode getSharedCacheMode();
  
  /**
   * @since JPA 2.0
   */
  public ValidationMode getValidationMode();

  /**
   * Returns a properties object.
   */
  public Properties getProperties();
  
  public String getPersistenceXMLSchemaVersion();

  /**
   * Returns the classloader the provider should use to load classes,
   * resources or URLs.
   */
  public ClassLoader getClassLoader();

  /**
   * Adds a class transformer.
   */
  public void addTransformer(ClassTransformer transformer);

  /**
   * Returns a temporary class loader.
   */
  public ClassLoader getNewTempClassLoader();
}
