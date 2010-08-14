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

package com.caucho.hmtp;

/**
 * Query to get the public key
 */
public class GetPublicKeyQuery implements java.io.Serializable {
  private static final long serialVersionUID = -1166286457258394604L;
  
  private String _algorithm;
  private String _format;
  private byte []_encoded;
  
  /**
   * Null constructor
   */
  public GetPublicKeyQuery()
  {
  }
  
  /**
   * Constructor for the result
   */
  public GetPublicKeyQuery(String algorithm,
                           String format,
                           byte []encoded)
  {
    _algorithm = algorithm;
    _format = format;
    _encoded = encoded;
  }

  public String getAlgorithm()
  {
    return _algorithm;
  }

  public String getFormat()
  {
    return _format;
  }

  public byte []getEncoded()
  {
    return _encoded;
  }

  @Override
  public String toString()
  {
    if (_algorithm != null) {
      return (getClass().getSimpleName()
            + "[" + _algorithm + "," + _format + "]");
    }
    else {
      return (getClass().getSimpleName() + "[]");
    }
  }
}
