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

package javax.xml.rpc;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents the base interface for client calls.
 */
public interface Call {
  public static final String USERNAME_PROPERTY =
    "javax.xml.rpc.security.auth.username";
  public static final String PASSWORD_PROPERTY =
    "javax.xml.rpc.security.auth.password";
  public static final String OPERATION_STYLE_PROPERTY =
    "javax.xml.rpc.soap.operation.style";
  public static final String SOAPACTION_USE_PROPERTY =
    "javax.xml.rpc.soap.http.soapaction.use";
  public static final String SOAPACTION_URI_PROPERTY =
    "javax.xml.rpc.soap.http.soapaction.uri";
  public static final String ENCODINGSTYLE_URI_PROPERTY =
    "javax.xml.rpc.encodingstyle.namespace.uri";
  public static final String SESSION_MAINTAIN_PROPERTY =
    "javax.xml.rpc.session.maintain";
  
  /**
   * Returns true if the parameter and return type should be invoked.
   */
  public boolean isParameterAndReturnSpecRequired(QName operationName);

  /**
   * Adds a parameter type and mode for the operation.
   */
  public void addParameter(String paramName,
                           QName xmlType,
                           ParameterMode parameterMode)
    throws JAXRPCException;

  /**
   * Adds a parameter type and mode for the operation.
   */
  public void addParameter(String paramName,
                           QName xmlType,
                           Class javaType,
                           ParameterMode parameterMode)
    throws JAXRPCException;

  /**
   * Adds Returns the XML type of a parameter.
   */
  public QName getParameterTypeByName(String paramName);

  /**
   * Sets the return type.
   */
  public void setReturnType(QName xmlType)
    throws JAXRPCException;

  /**
   * Sets the return type.
   */
  public void setReturnType(QName xmlType,
                            Class javaType)
    throws JAXRPCException;

  /**
   * Returns the return type.
   */
  public QName getReturnType();

  /**
   * Removes the parameters.
   */
  public void removeAllParameters()
    throws JAXRPCException;

  /**
   * Returns the operation name.
   */
  public QName getOperationName();

  /**
   * Returns the port type.
   */
  public QName getPortTypeName();

  /**
   * Sets the port type.
   */
  public void setPortTypeName(QName portType);

  /**
   * Sets the target endpoing.
   */
  public void setTargetEndpointAddress(String address);

  /**
   * Gets the target endpoing.
   */
  public String getTargetEndpointAddress();

  /**
   * Sets a property.
   */
  public void setProperty(String name, Object value)
    throws JAXRPCException;

  /**
   * Gets a property.
   */
  public Object getProperty(String name)
    throws JAXRPCException;

  /**
   * Removes a property.
   */
  public void removeProperty(String name)
    throws JAXRPCException;

  /**
   * Iterates over the property names.
   */
  public Iterator getPropertyNames();

  /**
   * Invokes the operation
   */
  public Object invoke(Object []params)
    throws java.rmi.RemoteException;

  /**
   * Invokes the operation
   */
  public Object invoke(QName operationName, Object []params)
    throws java.rmi.RemoteException;

  /**
   * Invokes the operation in one-way mode.
   */
  public void invokeOneWay(Object []params);

  /**
   * Returns the Map of the output parameters.
   */
  public Map getOutputParams();

  /**
   * Returns a list of theoutput parameters.
   */
  public List getOutputValues();
}
