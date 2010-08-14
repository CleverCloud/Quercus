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

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.ref.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

public final class JAXB {
  private static WeakHashMap<Class,SoftReference<JAXBContext>> _contextMap
    = new WeakHashMap<Class,SoftReference<JAXBContext>>();
  
  public static void marshal(Object obj, File xml)
  {
    try {
      getContext(obj).createMarshaller().marshal(obj, xml);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static void marshal(Object obj, URL xml)
  {
    try {
      URLConnection conn = xml.openConnection();
      conn.setDoInput(false);
      conn.setDoOutput(false);
      conn.connect();

      OutputStream os = conn.getOutputStream();
      try {
        StreamResult result = new StreamResult(os);

        marshal(obj, result);
      } finally {
        os.close();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static void marshal(Object obj, URI xml)
  {
    try {
      marshal(obj, xml.toURL());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static void marshal(Object obj, String xml)
  {
    try {
      StreamResult result = new StreamResult(xml);
      
      getContext(obj).createMarshaller().marshal(obj, result);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static void marshal(Object obj, OutputStream xml)
  {
    try {
      getContext(obj).createMarshaller().marshal(obj, xml);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static void marshal(Object obj, Writer xml)
  {
    try {
      getContext(obj).createMarshaller().marshal(obj, xml);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static void marshal(Object obj, Result xml)
  {
    try {
      getContext(obj).createMarshaller().marshal(obj, xml);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static <T> T unmarshal(File xml, Class<T> type)
  {
    try {
      return (T) getContext(type).createUnmarshaller().unmarshal(xml);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static <T> T unmarshal(URL url, Class<T> type)
  {
    try {
      return (T) getContext(type).createUnmarshaller().unmarshal(url);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static <T> T unmarshal(URI uri, Class<T> type)
  {
    try {
      return unmarshal(uri.toURL(), type);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static <T> T unmarshal(String xml, Class<T> type)
  {
    try {
      StreamSource source = new StreamSource(xml);

      return (T) getContext(type).createUnmarshaller().unmarshal(source);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static <T> T unmarshal(InputStream xml, Class<T> type)
  {
    try {
      return (T) getContext(type).createUnmarshaller().unmarshal(xml);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static <T> T unmarshal(Reader xml, Class<T> type)
  {
    try {
      return (T) getContext(type).createUnmarshaller().unmarshal(xml);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }
  
  public static <T> T unmarshal(Source xml, Class<T> type)
  {
    try {
      return (T) getContext(type).createUnmarshaller().unmarshal(xml);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new DataBindingException(e);
    }
  }

  private static JAXBContext getContext(Object obj)
    throws JAXBException
  {
    if (obj instanceof JAXBElement) {
      JAXBElement elt = (JAXBElement) obj;

      return getContext(elt.getDeclaredType());
    }
    else
      return getContext(obj.getClass());
  }

  private static JAXBContext getContext(Class cl)
    throws JAXBException
  {
    SoftReference<JAXBContext> ref = _contextMap.get(cl);

    JAXBContext context = null;

    if (ref != null)
      context = ref.get();

    if (context == null) {
      context = JAXBContext.newInstance(cl);

      _contextMap.put(cl, new SoftReference<JAXBContext>(context));
    }
    
    return context;
  }
}

