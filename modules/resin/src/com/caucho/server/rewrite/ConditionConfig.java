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
 * @author Sam
 */

package com.caucho.server.rewrite;

import java.net.UnknownHostException;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.util.InetNetwork;
import com.caucho.util.L10N;

public class ConditionConfig {
  private static final L10N L = new L10N(ConditionConfig.class);

  private Condition _condition;
  private ContainerProgram _builderProgram;

  private void setCondition(Condition condition)
  {
    if (_condition != null)
      throw new ConfigException(L.l("Condition '{0}' has already been set, cannot use '{1}' here",
                                    _condition.getTagName(), condition.getTagName()));

    _condition = condition;
  }

  /**
   * Sets the el expression.
   */
  public void setAuthType(String authType)
  {
    setCondition(new AuthTypeCondition(authType));
  }

  public void setCookie(String cookie)
  {
    setCondition(new CookieCondition(cookie));
  }

  public void setExpr(RawString expr)
  {
    setCondition(new ExprCondition(expr.getValue()));
  }

  public void setExists(RawString expr)
  {
    setCondition(new ExistsCondition(expr.getValue()));
  }

  public void setHeader(String header)
  {
    setCondition(new HeaderCondition(header));
  }

  public void setLocale(String locale)
  {
    setCondition(new LocaleCondition(locale));
  }

  public void setLocalPort(int localPort)
  {
    setCondition(new LocalPortCondition(localPort));
  }

  public void setMethod(String method)
  {
    setCondition(new MethodCondition(method));
  }

  public void setQueryParam(String queryParam)
  {
    setCondition(new QueryParamCondition(queryParam));
  }
  
  public void setRemoteAddr(String addr) 
    throws UnknownHostException
  {
    setCondition(new RemoteAddrCondition(InetNetwork.create(addr)));
  }

  public void setRemoteUser(String user)
  {
    setCondition(new RemoteUserCondition(user));
  }

  public void setSecure(boolean isSecure)
  {
    setCondition(new SecureCondition(isSecure));
  }

  public void setServerName(String serverName)
  {
    setCondition(new ServerNameCondition(serverName));
  }

  public void setServerPort(int serverPort)
  {
    setCondition(new ServerPortCondition(serverPort));
  }

  public void setUserInRole(String role)
  {
    setCondition(new UserInRoleCondition(role));
  }

  /**
   * Adds an init program.
   */
  public void addBuilderProgram(ConfigProgram init)
  {
    if (_builderProgram == null)
      _builderProgram = new ContainerProgram();

    _builderProgram.addProgram(init);
  }

  Condition getCondition()
  {
    if (_condition == null)
      throw new ConfigException(L.l("A condition is required"));

    if (_builderProgram != null)
      _builderProgram.configure(_condition);
    
    Config.init(_condition);
    
    return _condition;
  }
}
