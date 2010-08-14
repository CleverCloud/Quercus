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

package javax.el;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Represents an EL expression factory
 */
public abstract class ExpressionFactory {
  private static final Logger log
    = Logger.getLogger(ExpressionFactory.class.getName());

  private static final String SERVICE
    = "META-INF/services/javax.el.ExpressionFactory";

  public abstract Object coerceToType(Object obj,
                                      Class<?> targetType)
    throws ELException;

  public abstract MethodExpression
    createMethodExpression(ELContext context,
                           String expression,
                           Class<?> expectedReturnType,
                           Class<?>[] expectedParamTypes)
    throws ELException;

  public abstract ValueExpression
    createValueExpression(ELContext context,
                          String expression,
                          Class<?> expectedType)
    throws ELException;

  public abstract ValueExpression
    createValueExpression(Object instance,
                          Class<?> expectedType)
    throws ELException;

  public static ExpressionFactory newInstance()
  {
    return newInstance(null);
  }

  public static ExpressionFactory newInstance(Properties properties)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    String factoryName = null;

    URL url = loader.getResource(SERVICE);

    if (url != null) {
      InputStream is = null;
      try {
        is = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        factoryName = reader.readLine();
      } catch (IOException e) {
        log.log(Level.FINEST, "error reading from " + url, e);
      } finally {
        try {
          if (is != null)
            is.close();
        } catch (IOException e) {
          log.log(Level.FINEST, "error closing input stream " + url, e);
        }
      }
    }

    if (factoryName == null) {
      String javaHome = System.getProperty("java.home");
      char slash = File.separatorChar;
      File file = new File(javaHome + slash + "lib" + slash + "el.properties");

      if (file.exists()) {
        Properties elProperties = new Properties();
        Reader reader = null;
        try {
          reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
          elProperties.load(reader);
          factoryName = elProperties.getProperty("javax.el.ExpressionFactory");
        } catch (FileNotFoundException e) {
          log.log(Level.FINEST, "file " + file + " does not exist", e);
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
          log.log(Level.FINEST, "error reading from file " + file);
        } finally {
          try {
            if (reader != null)
              reader.close();
          } catch (IOException e) {
            log.log(Level.FINEST, e.getMessage(), e);
          }
        }
      }
    }

    if (factoryName == null)
      factoryName = System.getProperty("javax.el.ExpressionFactory");

    if (factoryName == null)
      factoryName = "com.caucho.el.ExpressionFactoryImpl";

    try {
      Class c = loader.loadClass(factoryName);

      ExpressionFactory result = null;

      if (properties != null) {
        try {
          Constructor constructor = c.getConstructor(Properties.class);
          result = (ExpressionFactory) constructor.newInstance(properties);
        } catch (NoSuchMethodException e) {
          log.finest("class "
            + factoryName
            + " does not declare constructor accepting instance of java.util.Properties");
        } catch (InvocationTargetException e) {
          String error = "exception initializing " + factoryName
            + " using constructor accepting java.util.Properties";
          log.log(Level.FINEST, error, e);

          throw new ELException(error, e);
        }
      }

      if (result == null)
        result = (ExpressionFactory) c.newInstance();

      return result;
    } catch (InstantiationException e) {
      throw new ELException("can't create an instance of class " + factoryName,
                            e);
    } catch (IllegalAccessException e) {
      throw new ELException("can't create an instance of class " + factoryName,
                            e);
    } catch (ClassNotFoundException e) {
      throw new ELException(e.getMessage(), e);
    }
  }
}
