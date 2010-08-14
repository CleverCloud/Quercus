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

package javax.xml.ws.spi;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Provider {
  private static final Logger log
    = Logger.getLogger(Provider.class.getName());
  
  public static final String JAXWSPROVIDER_PROPERTY
    = "javax.xml.ws.spi.Provider";

  private static final WeakHashMap<ClassLoader,String> _providerMap
    = new WeakHashMap<ClassLoader,String>();

  protected Provider()
  {
  }

  public abstract Endpoint createAndPublishEndpoint(String address,
                                                    Object implementor);


  public abstract Endpoint createEndpoint(String bindingId,
                                          Object implementor);

  public abstract ServiceDelegate
    createServiceDelegate(URL wsdlDocumentLocation,
                          QName serviceName,
                          Class serviceClass);


  /** XXX */
  public static Provider provider()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      synchronized (_providerMap) {
        String className = _providerMap.get(loader);

        if (className == null) {
          className = findServiceName(loader);

          if (log.isLoggable(Level.FINER) && className != null)
            log.finer("jaxws.Provider implementation " + className);

          _providerMap.put(loader, className);
        }

        Class cl = Class.forName(className, false, loader);

        return (Provider) cl.newInstance();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String findServiceName(ClassLoader loader)
  {
    InputStream is = null;
    try {
      is = loader.getResourceAsStream("META-INF/services/javax.xml.ws.spi.Provider");

      if (is != null) {
        StringBuilder sb = new StringBuilder();
        int ch;

        while (Character.isWhitespace((ch = is.read()))) {
        }

        for (; ch >= 0 && ! Character.isWhitespace(ch); ch = is.read()) {
          sb.append((char) ch);
        }

        return sb.toString();
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (is != null)
        try { is.close(); } catch (IOException e) {}
    }

    String name = System.getProperty("javax.xml.ws.spi.Provider");

    if (name != null)
      return name;
    else
      return "com.caucho.soap.jaxws.ProviderImpl";
  }
}

