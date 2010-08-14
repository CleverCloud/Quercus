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

package com.caucho.ejb.cfg;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;

/**
 * Configuraton for a method transaction
 */
public class ContainerTransaction {
  private static final L10N L = new L10N(ContainerTransaction.class);

  private EjbConfig _config;
  private MethodSignature _method;
  private String _trans;

  /**
   * Creates a new cmp-relation
   */
  public ContainerTransaction(EjbConfig config)
  {
    _config = config;
  }

  public void setConfigLocation(String filename, int line)
  {
  }

  public void setDescription(String description)
  {
  }

  public void setMethod(MethodSignature method)
  {
    _method = method;
  }

  public void setTransAttribute(String trans)
    throws ConfigException
  {
    if (trans.equals("Required")) {
    }
    else if (trans.equals("RequiresNew")) {
    }
    else if (trans.equals("Mandatory")) {
    }
    else if (trans.equals("NotSupported")) {
    }
    else if (trans.equals("Never")) {
    }
    else if (trans.equals("Supports")) {
    }
    else
      throw new ConfigException(L.l("'{0}' is an unknown transaction type.  The transaction types are:\n  Required - creates a new transaction if none is active.\n  RequiresNew - always creates a new transaction.\n  Mandatory - requires an active transaction.\n  NotSupported - suspends any active transaction.\n  Never - forbids any active transaction.\n  Supports - allows a transaction or no transaction.", trans));

    _trans = trans;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    EjbBean<?> bean = _config.getBeanConfig(_method.getEJBName());

    if (bean == null)
      throw new ConfigException(L.l("'{0}' is an unknown entity bean.",
                                    _method.getEJBName()));

    EjbMethodPattern method = bean.createMethod(_method);

    method.setTransAttribute(_trans);

    // ejb/0593
    setInternalTransactionAttribute("ejb", bean);

    // ejb/0596
    setInternalTransactionAttribute("ejbHome", bean);
  }

  private void setInternalTransactionAttribute(String prefix, EjbBean<?> bean)
  {
    // XXX: it might need to check <method-intf>

    if (! _method.getName().startsWith(prefix)) {
      MethodSignature signature = new MethodSignature();

      signature.setEJBName(_method.getEJBName());

      String methodName = _method.getName();

      methodName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);

      signature.setMethodName(prefix + methodName);

      EjbMethodPattern method = bean.createMethod(signature);

      method.setTransAttribute(_trans);
    }
  }
}
