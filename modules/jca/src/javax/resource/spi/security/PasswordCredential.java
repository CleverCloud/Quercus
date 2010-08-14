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

package javax.resource.spi.security;

import javax.resource.spi.ManagedConnectionFactory;
import java.util.Arrays;

/**
 * @deprecated
 */
public final class PasswordCredential
  implements java.io.Serializable
{
  private String userName;
  private char[] password;
  private ManagedConnectionFactory mcf;

  /**
   * Creates the credential
   */
  public PasswordCredential(String userName, char[] password)
  {
    this.userName = userName;
    this.password = password;
  }

  /**
   * /** Returns the user name of the principal.
   */
  public String getUserName()
  {
    return this.userName;
  }

  /**
   * Returns the user password.
   */
  public char[] getPassword()
  {
    return this.password;
  }

  /**
   * Returns the connection factory.
   */
  public ManagedConnectionFactory getManagedConnectionFactory()
  {
    return this.mcf;
  }

  /**
   * Sets the connection factory.
   */
  public void setManagedConnectionFactory(ManagedConnectionFactory factory)
  {
    this.mcf = factory;
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    PasswordCredential that = (PasswordCredential) o;

    if (!Arrays.equals(password, that.password))
      return false;

    if (!userName.equals(that.userName))
      return false;

    return true;
  }

  public int hashCode()
  {
    int result = userName.hashCode();
    result = 31 * result + (password != null ? Arrays.hashCode(password) : 0);

    return result;
  }
}
