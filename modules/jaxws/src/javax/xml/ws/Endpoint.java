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

package javax.xml.ws;
import javax.xml.transform.Source;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/** XXX */
public abstract class Endpoint {

  /** XXX */
  public static final String WSDL_PORT="javax.xml.ws.wsdl.port";


  /** XXX */
  public static final String WSDL_SERVICE="javax.xml.ws.wsdl.service";

  public Endpoint()
  {
  }


  /** XXX */
  public static Endpoint create(Object implementor)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public static Endpoint create(String bindingId, Object implementor)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public abstract Binding getBinding();


  /** XXX */
  public abstract Executor getExecutor();


  /** XXX */
  public abstract Object getImplementor();


  /** XXX */
  public abstract List<Source> getMetadata();


  /** XXX */
  public abstract Map<String,Object> getProperties();


  /** XXX */
  public abstract boolean isPublished();


  /** XXX */
  public abstract void publish(Object serverContext);


  /** XXX */
  public abstract void publish(String address);


  /** XXX */
  public static Endpoint publish(String address, Object implementor)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public abstract void setExecutor(Executor executor);


  /** XXX */
  public abstract void setMetadata(List<Source> metadata);


  /** XXX */
  public abstract void setProperties(Map<String,Object> properties);


  /** XXX */
  public abstract void stop();

}

