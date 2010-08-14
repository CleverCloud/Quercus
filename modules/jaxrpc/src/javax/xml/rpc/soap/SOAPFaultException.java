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

package javax.xml.rpc.soap;

/**
 * The base SOAP exception.
 */
public class SOAPFaultException extends RuntimeException {
  /**
   * Base constructor
   */
  public SOAPFaultException()
  {
  }
  
  /**
   * SOAPFault exception with a message.
   */
  public SOAPFaultException(String msg)
  {
    super(msg);
  }
  
  /**
   * SOAPFault exception with a message and cause.
   */
  public SOAPFaultException(String msg, Throwable cause)
  {
    super(msg, cause);
  }
  
  /**
   * SOAPFault exception with a cause.
   */
  public SOAPFaultException(Throwable cause)
  {
    super(cause);
  }

  /**
   * Returns the linked cause.
   */
  public Throwable getLinkedCause()
  {
    return getCause();
  }
}
