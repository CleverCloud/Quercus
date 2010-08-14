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

package com.caucho.jws;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class SoapCall {

  private Envelope _envelope = null;

  public SoapCall()
  {
  }

  public SoapCall(String s) throws Exception
  {
    this(new ByteArrayInputStream(s.getBytes()));
  }

  public SoapCall(InputStream is) throws Exception
  {
    new com.caucho.config.Config().configure(this, is);
  }

  public String toString()
  {
    return "<soapcall>"+_envelope+"</soapcall>";
  }

  public Envelope.Body createBody() {
    return createEnvelope().createBody();
  }
  public Envelope createEnvelope()
  {
    if (_envelope == null)
      _envelope = new Envelope();

    return _envelope;
  }

  public class Envelope {
    private Body _body;

    public Body createBody()
    {
      if (_body == null)
        _body = new Body();

      return _body;
    }

    public String toString()
    {
      return "<envelope>"+_body+"</envelope>";
    }
    
    public class Body {

      public CallResponse _callResponse;

      public CallResponse createNullCallResponse() {
        if (_callResponse == null)
          _callResponse = new CallResponse();

        return _callResponse;
      }

      public String toString()
      {
        return "<body>"+_callResponse+"</body>";
      }
      
      public class CallResponse
      {
        public String toString()
        {
          return "<nullCallResponse/>";
        }
      }
    }
  }
}
