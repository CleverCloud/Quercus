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

package javax.xml.bind.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.transform.sax.SAXResult;

public class JAXBResult extends SAXResult {
  private Object _result;
  private UnmarshallerHandler _handler;

  public JAXBResult(JAXBContext context) 
    throws JAXBException
  {
    if (context == null)
      throw new JAXBException("Context may not be null");

    _handler = context.createUnmarshaller().getUnmarshallerHandler();

    setHandler(_handler);
  }

  public JAXBResult(Unmarshaller unmarshaller) 
    throws JAXBException
  {
    if (unmarshaller == null)
      throw new JAXBException("Unmarshaller may not be null");

    _handler = unmarshaller.getUnmarshallerHandler();

    setHandler(_handler);
  }

  public Object getResult() throws JAXBException
  {
    return _handler.getResult();
  }
}

