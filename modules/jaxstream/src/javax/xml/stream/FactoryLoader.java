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
 * @author Adam Megacz
 */

package javax.xml.stream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

class FactoryLoader {
  private static Logger log =
    Logger.getLogger("javax.xml.stream.FactoryLoader");

  private static HashMap<String,FactoryLoader>
    _factoryLoaders = new HashMap<String,FactoryLoader>();

  private String _factoryId;

  private WeakHashMap<ClassLoader,Class[]>
    _providerMap = new WeakHashMap<ClassLoader,Class[]>();

  public static FactoryLoader getFactoryLoader(String factoryId)
  {
    FactoryLoader ret = _factoryLoaders.get(factoryId);

    if (ret == null) {
      ret = new FactoryLoader(factoryId);
      _factoryLoaders.put(factoryId, ret);
    }

    return ret;
  }

  private FactoryLoader(String factoryId)
  {
    this._factoryId = factoryId;
  }

  public Object newInstance(ClassLoader classLoader)
    throws FactoryConfigurationError
  {
    Class cl = newClass(classLoader);

    if (cl != null) {
      try {
        return cl.newInstance();
      }
      catch (Exception e) {
        throw new FactoryConfigurationError(e);
      }
    }

    return null;
  }

  public Class newClass(ClassLoader classLoader)
    throws FactoryConfigurationError
  {
    String className = null;

    className = System.getProperty(_factoryId);

    if (className == null) {
      
      String fileName = (System.getProperty("java.home")
                         + File.separatorChar
                         + "lib"
                         + File.separatorChar
                         + "stax.properties");

      FileInputStream is = null;
      try {
        is = new FileInputStream(new File(fileName));

        Properties props = new Properties();
        props.load(is);

        className = props.getProperty(_factoryId);
      } catch (IOException e) {
        log.log(Level.FINEST, "ignoring exception", e);
      }
      finally {
        if (is != null)
          try {
            is.close();
          } catch (IOException e) {
            log.log(Level.FINER, "ignoring exception", e);
          }
      }
    }

    if (className == null) {
      Class cl = createFactoryClass("META-INF/services/"+_factoryId,
                                     classLoader);
      if (cl != null)
        return cl;
    }

    if (className != null) {
      try {
        return classLoader.loadClass(className);
      }
      catch (Exception e) {
        throw new FactoryConfigurationError(e);
      }
    }

    return null;
  }

  public Class createFactoryClass(String name, ClassLoader loader)
  {
    Class[] providers = getProviderList(name, loader);

    for (int i = 0; i < providers.length; i++) {
      Class factory;

      factory = providers[i];

      if (factory != null)
        return factory;
    }
    
    return null;
  }
  
  private Class []getProviderList(String service, ClassLoader loader)
  {
    Class []providers = _providerMap.get(loader);

    if (providers != null)
      return providers;
    
    ArrayList<Class> list = new ArrayList<Class>();

    try {
      Enumeration e = loader.getResources(service);

      while (e.hasMoreElements()) {
        URL url = (URL) e.nextElement();

        Class provider = loadProvider(url, loader);

        if (provider != null)
          list.add(provider);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    providers = new Class[list.size()];
    list.toArray(providers);

    _providerMap.put(loader, providers);
    
    return providers;
  }

  private Class loadProvider(URL url, ClassLoader loader)
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

          Class cl = Class.forName(className, false, loader);

          return cl;
        }
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (IOException e) {
      }
    }

    return null;
  }
}


