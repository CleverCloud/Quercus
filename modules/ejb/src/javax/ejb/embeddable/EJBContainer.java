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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package javax.ejb.embeddable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;

import javax.ejb.EJBException;
import javax.ejb.spi.EJBContainerProvider;

import javax.naming.Context;

/**
 * The main ejb context.
 */
public abstract class EJBContainer {
  private static Logger log 
    = Logger.getLogger(EJBContainer.class.getName());

  private static final String PROVIDER_RESOURCE 
    = "META-INF/services/javax.ejb.spi.EJBContainerProvider";

  public static final String APP_NAME = "javax.ejb.embeddable.appName";
  public static final String MODULES = "javax.ejb.embeddable.modules";
  public static final String PROVIDER = "javax.ejb.embeddable.provider";

  public static EJBContainer createEJBContainer()
    throws EJBException
  {
    return createEJBContainer(null);
  }
  
  public static EJBContainer createEJBContainer(Map<?,?> properties)
    throws EJBException
  {
    Class<?> cl = null;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Enumeration<URL> providerURLs = 
        loader.getResources(PROVIDER_RESOURCE);

      while (providerURLs.hasMoreElements()) {
        cl = loadProviderClass(providerURLs.nextElement(), loader);

        try {
          EJBContainerProvider provider 
            = (EJBContainerProvider) cl.newInstance();

          EJBContainer container = provider.createEJBContainer(properties);

          if (container != null)
            return container;
        }
        catch (IllegalAccessException e) {
          throw new EJBException(e);
        }
        catch (InstantiationException e) {
          throw new EJBException(e);
        }
      }
    }
    catch (IOException e) {
      throw new EJBException(e);
    }

    throw new UnsupportedOperationException("No EJBProvider Found");
  }
  
  private static Class loadProviderClass(URL url, ClassLoader loader)
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

          return Class.forName(className, false, loader);
        }
      }
    } 
    catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } 
    finally {
      try {
        if (is != null)
          is.close();
      } catch (Throwable e) {
      }
    }

    return null;
  }

  public abstract Context getContext();
  public abstract void close();
}
