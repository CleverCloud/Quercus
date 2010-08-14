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
 * @author Scott Ferguson
 */

package javax.xml.rpc;

import javax.xml.namespace.QName;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

/**
 * Abstract implementation of a XML-RPC service.
 */
abstract public class ServiceFactory {
  public final static String SERVICEFACTORY_PROPERTY =
    "javax.xml.rpc.ServiceFactory";
  
  private final static String DEFAULT_FACTORY =
    "com.caucho.soap.rpc.ServiceFactory";

  /**
   * Constructor.
   */
  protected ServiceFactory()
  {
  }

  /**
   * Creates an instance of the factory.
   */
  public static ServiceFactory newInstance()
    throws ServiceException
  {
    String className = getFactoryClassName();

    Class cl = null;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      if (loader != null)
        cl = Class.forName(className, false, loader);
    } catch (NoSuchMethodError e) {
    } catch (ClassNotFoundException e) {
      throw new ServiceException(e);
    }

    if (cl == null) {
      try {
        cl = Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new ServiceException(e);
      }
    }

    try {
      return (ServiceFactory) cl.newInstance();
    } catch (IllegalAccessException e) {
      throw new ServiceException(e);
    } catch (InstantiationException e) {
      throw new ServiceException(e);
    }
  }

  /**
   * Finds the configured parser factory.
   *
   * <ol>
   * <li>System.getProperty("javax.xml.parsers.SAXParserFactory");
   * <li>META-INF/services/javax.xml.parsers.SAXParserFactory
   * <li>$JAVA_HOME/lib/jaxp.properties
   * </ol>
   */
  private static String getFactoryClassName()
  {
    String className = null;
    
    try {
      className = System.getProperty(SERVICEFACTORY_PROPERTY);
      if (className != null)
        return className;
    } catch (SecurityException e) {
    }

    String serviceName = "META-INF/services/" + SERVICEFACTORY_PROPERTY;
    
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      InputStream is;

      if (loader != null)
        is = loader.getResourceAsStream(serviceName);
      else
        is = ClassLoader.getSystemResourceAsStream(serviceName);

      Reader rawReader = new InputStreamReader(is);
      BufferedReader reader = new BufferedReader(rawReader);

      className = reader.readLine();
      reader.close();
      is.close();

      if (className != null)
        className = className.trim();
      if (className != null && ! "".equals(className))
        return className;
    } catch (Throwable e) {
    }

    return DEFAULT_FACTORY;
  }

  /**
   * Creates an instance of the factory.
   */
  public abstract Service createService(URL wsdlDocumentLocation,
                                        QName serviceName)
    throws ServiceException;

  /**
   * Creates an instance of the factory.
   */
  public abstract Service createService(QName serviceName)
    throws ServiceException;

  /**
   * Creates an instance of the factory.
   */
  public abstract Service loadService(Class serviceInterface)
    throws ServiceException;

  /**
   * Creates an instance of the factory.
   */
  public abstract Service loadService(URL wsdlDocumentLocation,
                                      Class serviceInterface,
                                      Properties properties)
    throws ServiceException;

  /**
   * Loads the service.
   */
  public abstract Service loadService(URL wsdlDocumentLocation,
                                      QName serviceName,
                                      Properties properties)
    throws ServiceException;
}
