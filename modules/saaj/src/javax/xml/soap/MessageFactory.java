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

package javax.xml.soap;

import java.io.*;
import java.util.*;

public abstract class MessageFactory {
  private static final WeakHashMap<ClassLoader,Class> _factoryMap
    = new WeakHashMap<ClassLoader,Class>();

  public MessageFactory()
  {
  }

  public abstract SOAPMessage createMessage() throws SOAPException;

  public abstract SOAPMessage createMessage(MimeHeaders headers, InputStream in)
    throws IOException, SOAPException;

  public static MessageFactory newInstance() 
    throws SOAPException
  {
    // XXX synchronize? 
    
    try {
      ClassLoader classLoader =
        Thread.currentThread().getContextClassLoader();

      Class cl = _factoryMap.get(classLoader);

      if (cl == null) {
        FactoryLoader factoryLoader = 
          FactoryLoader.getFactoryLoader("javax.xml.soap.MessageFactory");
        
        cl = factoryLoader.newClass(classLoader);

        if (cl != null)
          _factoryMap.put(classLoader, cl);
        else
          return newInstance(SOAPConstants.DEFAULT_SOAP_PROTOCOL);
      }

      return (MessageFactory) cl.newInstance();
    } 
    catch (Exception e) {
      throw new SOAPException(e);
    }
  }

  public static MessageFactory newInstance(String protocol) 
    throws SOAPException
  {
    return SAAJMetaFactory.getInstance().newMessageFactory(protocol);
  }
}

