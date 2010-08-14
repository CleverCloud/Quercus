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

package com.caucho.config.extension;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.ProcessProducerField;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ProducesFieldBean;
import com.caucho.inject.Module;

@Module
public class ProcessProducerFieldImpl<X,T> extends ProcessBeanImpl<T>
  implements ProcessProducerField<X,T>
{
  private ProducesFieldBean<X,T> _bean;
  
  protected ProcessProducerFieldImpl(InjectManager manager, 
                                     ProducesFieldBean<X,T> bean)
  {
    super(manager, bean, bean.getAnnotatedType());
    
    _bean = bean;
  }

  @Override
  public AnnotatedField<X> getAnnotatedProducerField()
  {
    return _bean.getField();
  }
}
