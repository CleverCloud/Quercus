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
import com.caucho.config.types.DescriptionGroupConfig;
import com.caucho.config.types.Signature;
import com.caucho.util.L10N;

import java.util.*;
import javax.annotation.PostConstruct;

/**
 * Configuration for an ejb bean.
 */
public class EjbJar extends DescriptionGroupConfig {
  private static final L10N L = new L10N(EjbJar.class);

  private final EjbConfig _config;
  private String _ejbModuleName;

  private boolean _isMetadataComplete;

  public EjbJar(EjbConfig config, String ejbModuleName)
  {
    _config = config;
    _ejbModuleName = ejbModuleName;
  }

  public String getModuleName()
  {
    return _ejbModuleName;
  }

  public void setModuleName(String moduleName)
  {
    _ejbModuleName = moduleName;
  }

  public void setVersion(String version)
  {
  }

  public void setSchemaLocation(String value)
  {
  }

  public void setMetadataComplete(boolean isMetadataComplete)
  {
    _isMetadataComplete = isMetadataComplete;
  }
  
  public boolean isMetadataComplete()
  {
    return _isMetadataComplete;
  }

  public EjbEnterpriseBeans createEnterpriseBeans()
    throws ConfigException
  {
    return new EjbEnterpriseBeans(_config, _ejbModuleName);
  }

  public InterceptorsConfig createInterceptors()
    throws ConfigException
  {
    return new InterceptorsConfig(_config);
  }

  public Relationships createRelationships()
    throws ConfigException
  {
    return new Relationships(_config);
  }

  public AssemblyDescriptor createAssemblyDescriptor()
    throws ConfigException
  {
    return new AssemblyDescriptor(_config);
  }

  public void addQueryFunction(QueryFunction fun)
  {
  }

  public void setBooleanLiteral(BooleanLiteral literal)
  {
  }

  public static class MethodPermission {
    EjbConfig _config;
    MethodSignature _method;
    ArrayList<String> _roles;

    MethodPermission(EjbConfig config)
    {
      _config = config;
    }

    public void setDescription(String description)
    {
    }

    public void setUnchecked(boolean unchecked)
    {
    }

    public void setRoleName(String roleName)
    {
      if (_roles == null)
        _roles = new ArrayList<String>();

      _roles.add(roleName);
    }

    public void setMethod(MethodSignature method)
    {
      _method = method;
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      EjbBean bean = _config.getBeanConfig(_method.getEJBName());

      if (bean == null)
        throw new ConfigException(L.l("'{0}' is an unknown bean.",
                                      _method.getEJBName()));

      EjbMethodPattern method = bean.createMethod(_method);

      if (_roles != null)
        method.setRoles(_roles);
    }
  }

  public static class QueryFunction {
    FunctionSignature _sig;
    String _sql;

    public void setSignature(Signature sig)
      throws ConfigException
    {
      _sig = new FunctionSignature(sig.getSignature());
    }

    public FunctionSignature getSignature()
    {
      return _sig;
    }

    public void setSQL(String sql)
      throws ConfigException
    {
      _sql = sql;
    }

    public String getSQL()
    {
      return _sql;
    }

    @PostConstruct
    public void init()
    {
      _sig.setSQL(_sql);
    }
  }

  public static class Relationships {
    EjbConfig _config;

    Relationships(EjbConfig config)
    {
      _config = config;
    }
  }
}
