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

package javax.xml.rpc.encoding;

import javax.xml.namespace.QName;

/**
 * Represents a type mapping
 */
public interface TypeMapping {
  /**
   * Returns the encoding URIs.
   */
  public String []getSupportedEncodings();

  /**
   * Sets the encoding URIs.
   */
  public void setSupportedEncodings(String []uris);

  /**
   * Tests if the type is registered.
   */
  public boolean isRegistered(Class javaType, QName xmlType);

  /**
   * Registers the type.
   */
  public void register(Class javaType, QName xmlType,
                       SerializerFactory sf,
                       DeserializerFactory dsf);

  /**
   * Returns the serializer for the java type.
   */
  public SerializerFactory getSerializer(Class javaType,
                                         QName xmlType);

  /**
   * Returns the deserializer for the java type.
   */
  public DeserializerFactory getDeserializer(Class javaType,
                                             QName xmlType);

  /**
   * Removes the serializer for the java type.
   */
  public void removeSerializer(Class javaType,
                               QName xmlType);

  /**
   * Removes the deserializer for the java type.
   */
  public void removeDeserializer(Class javaType,
                                 QName xmlType);
}
