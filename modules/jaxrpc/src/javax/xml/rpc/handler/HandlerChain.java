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

package javax.xml.rpc.handler;

/**
 * Represents a handler.
 */
public interface HandlerChain extends java.util.List {
  /**
   * Initialize the handler.
   */
  public void init(HandlerInfo config);

  /**
   * Returns the roles handled by the chain.
   */
  public String []getRoles();

  /**
   * Returns the roles handled by the chain.
   */
  public void setRoles(String []roles);

  /**
   * Handles the request.
   *
   * @return true to continue handling.
   */
  public boolean handleRequest(MessageContext context);

  /**
   * Handles the response.
   *
   * @return true to continue handling.
   */
  public boolean handleResponse(MessageContext context);

  /**
   * Handles the fault.
   *
   * @return true to continue handling.
   */
  public boolean handleFault(MessageContext context);

  /**
   * Closes the handler.
   */
  public void destroy();
}
