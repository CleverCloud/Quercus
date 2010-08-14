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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import javax.enterprise.context.spi.CreationalContext;

import org.w3c.dom.Node;

import com.caucho.config.ConfigException;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.util.L10N;
import com.caucho.xml.QElement;
import com.caucho.xml.QName;
import com.caucho.xml.QNode;

/**
 * Stored configuration program for an attribute.
 */
public class NodeBuilderProgram extends FlowProgram {
  static final L10N L = new L10N(NodeBuilderProgram.class);

  public static final NodeBuilderProgram NULL
    = new NodeBuilderProgram(new QElement());

  private final Node _node;

  public NodeBuilderProgram(Node node)
  {
    _node = node;
  }

  @Override
  public QName getQName()
  {
    if (_node instanceof QNode)
      return ((QNode) _node).getQName();
    else
      return null;
  }

  @Override
  public <T> void inject(T bean, CreationalContext<T> cxt)
    throws ConfigException
  {
    XmlConfigContext env = XmlConfigContext.getCurrent();
    
    env.configureBean(bean, _node);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _node + "]";
  }
}
