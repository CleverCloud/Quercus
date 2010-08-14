/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces.el;

/**
 * @deprecated
 */
public abstract class PropertyResolver
{
  public abstract Object getValue(Object base,
                                  Object property)
    throws EvaluationException,
           PropertyNotFoundException;
  
  public abstract Object getValue(Object base,
                                  int index)
    throws EvaluationException,
           PropertyNotFoundException;
  
  public abstract void setValue(Object base,
                                Object property,
                                Object value)
    throws EvaluationException,
           PropertyNotFoundException;
  
  public abstract void setValue(Object base,
                                int index,
                                Object value)
    throws EvaluationException,
           PropertyNotFoundException;
  
  public abstract boolean isReadOnly(Object base,
                                     Object property)
    throws EvaluationException,
           PropertyNotFoundException;
  
  public abstract boolean isReadOnly(Object base,
                                     int index)
    throws EvaluationException,
           PropertyNotFoundException;
  
  public abstract Class getType(Object base,
                                Object property)
    throws EvaluationException,
           PropertyNotFoundException;
  
  public abstract Class getType(Object base,
                                int index)
    throws EvaluationException,
           PropertyNotFoundException;
}
