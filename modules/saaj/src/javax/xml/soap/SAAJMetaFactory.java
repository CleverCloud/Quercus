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

public abstract class SAAJMetaFactory {
  private static SAAJMetaFactory _instance;

  static synchronized SAAJMetaFactory getInstance()
    throws SOAPException
  {
    if (_instance == null) {
      try {
        Class cl = Class.forName("com.caucho.xml.saaj.SAAJMetaFactoryImpl");

        _instance = (SAAJMetaFactory) cl.newInstance();
      }
      catch (ClassNotFoundException e) {
        throw new SOAPException(e);
      }
      catch (InstantiationException e) {
        throw new SOAPException(e);
      }
      catch (IllegalAccessException e) {
        throw new SOAPException(e);
      }
    }

    return _instance;
  }

  protected SAAJMetaFactory()
  {
  }

  protected abstract MessageFactory newMessageFactory(String protocol) 
    throws SOAPException;

  protected abstract SOAPFactory newSOAPFactory(String protocol) 
    throws SOAPException;
}

