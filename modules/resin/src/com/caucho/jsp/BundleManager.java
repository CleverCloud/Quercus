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

package com.caucho.jsp;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;
import com.caucho.util.TimedCache;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

/**
 * Manages i18n bundles
 */
public class BundleManager {
  private static final L10N L = new L10N(BundleManager.class);
  private static final Logger log
    = Logger.getLogger(BundleManager.class.getName());

  public static final LocalizationContext NULL_BUNDLE
    = new LocalizationContext();

  private static EnvironmentLocal<BundleManager> _envBundle
    = new EnvironmentLocal<BundleManager>();

  private TimedCache<String,LocalizationContext> _bundleCache;

  private Method _bundleSetParentMethod;

  private BundleManager()
  {
    long updateInterval = Environment.getDependencyCheckInterval();
    
    _bundleCache = new TimedCache(256, updateInterval);

    try {
      _bundleSetParentMethod
        = ResourceBundle.class.getDeclaredMethod("setParent",
                                         new Class[]{
                                                   ResourceBundle.class});
      
      _bundleSetParentMethod.setAccessible(true);
    }
    catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }
  }

  /**
   * Returns the environment's bundle.
   */
  public static BundleManager create()
  {
    BundleManager manager;

    synchronized (_envBundle) {
      manager = _envBundle.get();
      if (manager == null) {
        manager = new BundleManager();
        _envBundle.set(manager);
      }
    }

    return manager;
  }
  /**
   * Returns the named ResourceBundle.
   */
  public LocalizationContext getBundle(String name, String cacheKey,
                                       Enumeration<Locale> locales)
  {
    LocalizationContext cachedValue = _bundleCache.get(cacheKey);

    if (cachedValue != null)
      return cachedValue == NULL_BUNDLE ? null : cachedValue;

    while (locales.hasMoreElements()) {
      Locale locale = locales.nextElement();

      LocalizationContext bundle = getBundle(name, locale);

      if (bundle != null) {
        _bundleCache.put(cacheKey, bundle);
        return bundle;
      }
    }

    _bundleCache.put(cacheKey, NULL_BUNDLE);

    return null;
  }

  /**
   * Returns the named ResourceBundle.
   */
  public LocalizationContext getBundle(String name, Locale locale)
  {
    String cacheName = (name
                        + '_' + locale.getLanguage()
                        + '_' + locale.getCountry()
                        + '_' + locale.getVariant());

    LocalizationContext context;

    context = _bundleCache.get(cacheName);

    if (context != null)
      return context != NULL_BUNDLE ? context : null;

    ResourceBundle parent = getBaseBundle(name);

    ResourceBundle bundle = getBaseBundle(name
                                          + '_' + locale.getLanguage());

    ResourceBundle matchBundle = null;

    
    if (bundle != null) {
      if (parent != null) {
        try {
          _bundleSetParentMethod.invoke(bundle, parent);
        }
        catch (Exception e) {
          log.log(Level.WARNING, e.getMessage(), e);
        }
      }
      
      parent = bundle;
      matchBundle = bundle;
    }

    bundle = getBaseBundle(name
                           + '_' + locale.getLanguage()
                           + '_' + locale.getCountry());
    
    if (bundle != null) {
      if (parent != null) {
        try {
          _bundleSetParentMethod.invoke(bundle, parent);
        }
        catch (Exception e) {
          log.log(Level.WARNING, e.getMessage(), e);
        }
      }
      
      parent = bundle;
      matchBundle = bundle;
    }

    bundle = getBaseBundle(name
                           + '_' + locale.getLanguage()
                           + '_' + locale.getCountry()
                           + '_' + locale.getVariant());
    
    if (bundle != null) {
      if (parent != null) {
        try {
          _bundleSetParentMethod.invoke(bundle, parent);
        }
        catch (Exception e) {
          log.log(Level.WARNING, e.getMessage(), e);
        }
      }
      
      parent = bundle;
      matchBundle = bundle;
    }

    if (matchBundle != null) {
      context = new LocalizationContext(matchBundle, locale);
      
      _bundleCache.put(cacheName, context);

      return context;
    }

    _bundleCache.put(cacheName, NULL_BUNDLE);

    return null;
  }


  /**
   * Returns the named ResourceBundle.
   */
  public LocalizationContext getBundle(String name)
  {
    if (name == null)
      return null;
    
    LocalizationContext bundle = _bundleCache.get(name);
    if (bundle != null)
      return bundle != NULL_BUNDLE ? bundle : null;
    
    ResourceBundle resourceBundle = getBaseBundle(name);
    if (resourceBundle != null) {
      bundle = new LocalizationContext(resourceBundle);
      
      _bundleCache.put(name, bundle);
      
      return bundle;
    }

    _bundleCache.put(name, NULL_BUNDLE);

    return null;
  }

  /**
   * Returns the base resource bundle.
   */
  private ResourceBundle getBaseBundle(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    try {
      Class cl = Class.forName(name, false, loader);

      if (cl != null) {
        ResourceBundle rb = (ResourceBundle) cl.newInstance();

        if (rb != null)
          return rb;
      }
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }
    
    try {
      InputStream is = loader.getResourceAsStream(name.replace('.', '/') + ".properties");
      
      if (is instanceof ReadStream) {
        Path path = ((ReadStream) is).getPath();
        Environment.addDependency(path.createDepend());
      }

      ResourceBundle bundle = new PropertyResourceBundle(is);

      is.close();

      return bundle;
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    return null;
  }
}
