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
 * @author Emil Ong
 */

package com.caucho.soap.jaxws.handlerchain;

import java.util.List;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

@XmlRootElement(
  name="handler-chains", 
  namespace="http://java.sun.com/xml/ns/javaee"
)
public class HandlerChains implements HandlerResolver {
  private List<HandlerChain> _chains;

  @XmlElement(name="handler-chain",
              namespace="http://java.sun.com/xml/ns/javaee")
  public List<HandlerChain> getHandlerChainList()
  {
    return _chains;
  }

  public void setHandlerChainList(List<HandlerChain> chains)
  {
    _chains = chains;
  }

  public List<javax.xml.ws.handler.Handler> getHandlerChain(PortInfo portInfo)
  {
    HandlerChain defaultHandlerChain = null;
    List<javax.xml.ws.handler.Handler> list = null;

    for (int i = 0; i < _chains.size(); i++) {
      HandlerChain chain = _chains.get(i);

      if (chain.isDefault() ||
          (chain.getPortNamePattern() != null &&
           chain.getPortNamePattern().equals(portInfo.getPortName())) ||
          (chain.getBindingID() != null &&
           chain.getBindingID().equals(portInfo.getBindingID()))) {
        if (list == null)
          list = chain.toHandlerList();
        else
          list.addAll(chain.toHandlerList());
      }

      // XXX else if (chain.getProtocolBindings().equals(???)) {}
    }

    return list;
  }
}
