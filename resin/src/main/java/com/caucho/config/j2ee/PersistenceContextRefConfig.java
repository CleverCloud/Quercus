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
 * @author Rodrigo Westrupp
 */

package com.caucho.config.j2ee;

public class PersistenceContextRefConfig
{
  private String _id;
  private String _description;
  private String _persistenceContextRefName;
  private String _persistenceUnitName;
  private String _persistenceContextType;

  public String getDescription()
  {
    return _description;
  }

  public String getId()
  {
    return _id;
  }

  public String getPersistenceContextRefName()
  {
    return _persistenceContextRefName;
  }

  public String getPersistenceContextType()
  {
    return _persistenceContextType;
  }

  public String getPersistenceUnitName()
  {
    return _persistenceUnitName;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public void setPersistenceContextRefName(String persistenceContextRefName)
  {
    _persistenceContextRefName = persistenceContextRefName;
  }

  public void setPersistenceContextType(String persistenceContextType)
  {
    _persistenceContextType = persistenceContextType;
  }

  public void setPersistenceUnitName(String persistenceUnitName)
  {
    _persistenceUnitName = persistenceUnitName;
  }
}
