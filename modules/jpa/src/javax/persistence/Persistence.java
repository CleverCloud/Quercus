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

package javax.persistence;

import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.ProviderUtil;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bootstrap class to obtain an EntityManagerFactory.
 */
public class Persistence {
  private static final Logger log
    = Logger.getLogger(Persistence.class.getName());

  private static final String SERVICE
    = "META-INF/services/javax.persistence.spi.PersistenceProvider";
  
  private static WeakHashMap<ClassLoader,PersistenceProvider[]>
    _providerMap = new WeakHashMap<ClassLoader,PersistenceProvider[]>();
  
  private static final String AMBER_PROVIDER
    = "com.caucho.amber.manager.AmberPersistenceProvider";

  @Deprecated
  protected static final java.util.Set<PersistenceProvider> providers
    = new HashSet<PersistenceProvider>();

  @Deprecated
  public static final String PERSISTENCE_PROVIDER
    = "javax.persistence.spi.PeristenceProvider";

  /**
   * Create an return an EntityManagerFactory for the named unit.
   *
   * @param name - the name of the persistence unit
   */
  public static EntityManagerFactory createEntityManagerFactory(String name)
  {
    PersistenceProvider []providers = getProviderList();

    for (int i = 0; i < providers.length; i++) {
      EntityManagerFactory factory;

      factory = providers[i].createEntityManagerFactory(name, null);

      if (factory != null)
        return factory;
    }

    return null;
  }

  /**
   * Create and return an EntityManagerFactory for the named unit.
   *
   * @param name - the name of the persistence unit
   * @param props - persistence unit properties
   */
  @SuppressWarnings("unchecked")
  public static EntityManagerFactory 
  createEntityManagerFactory(String name, Map props)
  {
    for (PersistenceProvider provider : getProviderList()) {
      EntityManagerFactory factory;

      factory = provider.createEntityManagerFactory(name, props);

      if (factory != null)
        return factory;
    }

    return null;
  }
  
  public static PersistenceUtil getPersistenceUtil()
  {
    return new PersistenceUtilImpl(getProviderList());
  }

  private static PersistenceProvider []getProviderList()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    PersistenceProvider []providers = _providerMap.get(loader);

    if (providers != null)
      return providers;

    ArrayList<PersistenceProvider> list = new ArrayList<PersistenceProvider>();

    try {
      Class<?> cl = Class.forName(AMBER_PROVIDER, false, loader);

      PersistenceProvider provider = (PersistenceProvider) cl.newInstance();

      list.add(provider);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      Enumeration<URL> e = loader.getResources(SERVICE);

      while (e.hasMoreElements()) {
        URL url = e.nextElement();

        PersistenceProvider provider = loadProvider(url, loader);

        if (provider != null)
          list.add(provider);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    providers = new PersistenceProvider[list.size()];
    list.toArray(providers);

    _providerMap.put(loader, providers);

    return providers;
  }

  private static PersistenceProvider loadProvider(URL url, ClassLoader loader)
  {
    InputStream is = null;
    try {
      is = url.openStream();
      int ch;

      while ((ch = is.read()) >= 0) {
        if (Character.isWhitespace((char) ch)) {
        }
        else if (ch == '#') {
          for (; ch >= 0 && ch != '\n' && ch != '\r'; ch = is.read()) {
          }
        }
        else {
          StringBuilder sb = new StringBuilder();

          for (;
               ch >= 0 && ! Character.isWhitespace((char) ch);
               ch = is.read()) {
            sb.append((char) ch);
          }

          String className = sb.toString();

          Class<?> cl = Class.forName(className, false, loader);

          return (PersistenceProvider) cl.newInstance();
        }
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Throwable e) {
      }
    }

    return null;
  }
  
  private static class PersistenceUtilImpl implements PersistenceUtil {
    private PersistenceProvider []_providerList;
    
    PersistenceUtilImpl(PersistenceProvider []providerList)
    {
      _providerList = providerList;
    }

    @Override
    public boolean isLoaded(Object entity, String attributeName)
    {
      for (PersistenceProvider provider : _providerList) {
        try {
          ProviderUtil util = provider.getProviderUtil();
      
          if (util != null) {
            LoadState state = util.isLoadedWithoutReference(entity, attributeName);
          
            if (state == LoadState.LOADED)
              return true;
            else if (state == LoadState.NOT_LOADED)
              return false;
          }
        } catch (Exception e) {
          log.log(Level.FINER, provider + ": " + e.toString(), e);
        } catch (AbstractMethodError e) {
          log.log(Level.FINER, provider + ": " + e.toString(), e);
        }
      }
      
      for (PersistenceProvider provider : _providerList) {
        try {
          ProviderUtil util = provider.getProviderUtil();
      
          if (util != null) {
            LoadState state = util.isLoadedWithReference(entity, attributeName);
          
            if (state == LoadState.LOADED)
              return true;
            else if (state == LoadState.NOT_LOADED)
              return false;
          }
        } catch (Exception e) {
          log.log(Level.FINER, provider + ": " + e.toString(), e);
        } catch (AbstractMethodError e) {
          log.log(Level.FINER, provider + ": " + e.toString(), e);
        }
      }
      
      return true;
    }

    @Override
    public boolean isLoaded(Object entity)
    {
      for (PersistenceProvider provider : _providerList) {
        try {
          ProviderUtil util = provider.getProviderUtil();
      
          if (util != null) {
            LoadState state = util.isLoaded(entity);
          
            if (state == LoadState.LOADED)
              return true;
            else if (state == LoadState.NOT_LOADED)
              return false;
          }
        } catch (AbstractMethodError e) {
          log.log(Level.FINER, provider + ": " + e.toString(), e);
        } catch (Exception e) {
          log.log(Level.FINER, provider + ": " + e.toString(), e);
        }
      }
      
      return false;
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[]";
    }
  }
}
