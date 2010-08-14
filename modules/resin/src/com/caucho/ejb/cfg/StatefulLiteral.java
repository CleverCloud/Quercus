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

package com.caucho.ejb.cfg;

import javax.ejb.Stateful;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Configuration for an ejb singleton session bean.
 */
public class StatefulLiteral extends AnnotationLiteral<Stateful> 
  implements Stateful {
  private String _description;
  private String _mappedName;
  private String _name;
  
  public StatefulLiteral(String name,
                          String mappedName,
                          String description)
  {
    _name = name;
    _mappedName = mappedName;
    _description = description;
  }

  @Override
  public String description()
  {
    return _description;
  }

  @Override
  public String mappedName()
  {
    return _mappedName;
  }

  @Override
  public String name()
  {
    return _name;
  }
}
