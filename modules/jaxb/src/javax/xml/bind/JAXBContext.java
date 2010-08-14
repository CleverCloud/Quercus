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

package javax.xml.bind;

import org.w3c.dom.Node;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

public abstract class JAXBContext {
  private static final Logger log =
    Logger.getLogger(JAXBContext.class.getName());

  protected JAXBContext()
  {
  }

  /** subclasses must override */
  public Binder<Node> createBinder()
  {
    return null;
  }

  /** subclasses must override */
  public <T> Binder<T> createBinder(Class<T> domType)
  {
    return null;

  }
  
  /** subclasses must override */
  public JAXBIntrospector createJAXBIntrospector()
  {
    return null;
  }

  public abstract Marshaller createMarshaller() throws JAXBException;

  public abstract Unmarshaller createUnmarshaller() throws JAXBException;

  public abstract Validator createValidator() throws JAXBException;
  
  /** subclasses must override */
  public void generateSchema(SchemaOutputResolver outputResolver)
    throws IOException
  {
  }

  public static JAXBContext newInstance(Class... classesToBeBound)
      throws JAXBException
  {
    return newInstance(classesToBeBound, null);
  }

  public static JAXBContext newInstance(Class[] classesToBeBound,
                                        Map<String,?> properties)
    throws JAXBException
  {
    try {
      ClassLoader classLoader =
        Thread.currentThread().getContextClassLoader();
      
      FactoryLoader factoryLoader =
        FactoryLoader.getFactoryLoader("javax.xml.bind.JAXBContext");
      
      Class cl = factoryLoader.getFactoryClass(classLoader);

      if (cl != null) {
        Method m = cl.getMethod("createContext", Class[].class, Map.class);
        return (JAXBContext) m.invoke(null, classesToBeBound, properties);
      }
      
      cl = Class.forName("com.caucho.jaxb.JAXBContextImpl");
      Constructor con = cl.getConstructor(new Class[] { Class[].class, 
                                                        Map.class });
      return (JAXBContext) con.newInstance(new Object[] { classesToBeBound,
                                                          properties });
    }
    catch (InvocationTargetException e) {
      if (e.getCause() instanceof JAXBException) 
        throw (JAXBException) e.getCause();
      else
        throw new JAXBException (e);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public static JAXBContext newInstance(String contextPath) throws JAXBException
  {
    return newInstance(contextPath,
                       Thread.currentThread().getContextClassLoader(),
                       null);
  }

  public static JAXBContext newInstance(String contextPath,
                                        ClassLoader classLoader)
    throws JAXBException
  {
    return newInstance(contextPath, classLoader, null);
  }

  public static JAXBContext newInstance(String contextPath,
                                        ClassLoader classLoader,
                                        Map<String,?> properties)
      throws JAXBException
  {
    try {
      FactoryLoader factoryLoader =
        FactoryLoader.getFactoryLoader("javax.xml.bind.JAXBContext");
      
      Class cl = factoryLoader.getFactoryClass(classLoader, contextPath);

      if (cl != null) {
        try {
          Method m = cl.getMethod("createContext", 
                                  String.class, ClassLoader.class, Map.class);
          return (JAXBContext) m.invoke(null, contextPath, classLoader, 
                                        properties);
        }
        catch (NoSuchMethodException e) {
        }

        Method m = cl.getMethod("createContext", 
                                String.class, ClassLoader.class);
        return (JAXBContext) m.invoke(null, contextPath, classLoader);
      }

      cl = Class.forName("com.caucho.jaxb.JAXBContextImpl");
      Constructor con = cl.getConstructor(new Class[] {String.class,
                                                       ClassLoader.class,
                                                       Map.class});
      
      return (JAXBContext) con.newInstance(new Object[] { contextPath,
                                                          classLoader,
                                                          properties });
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }
}

