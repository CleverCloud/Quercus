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

package com.caucho.jsp.cfg;

import com.caucho.config.types.InitParam;
import com.caucho.util.BeanUtil;
import com.caucho.util.RegistryException;

import javax.servlet.jsp.tagext.TagLibraryValidator;
import java.util.HashMap;

/**
 * Configuration for the taglib validator in the .tld
 */
public class TldValidator {
  private Class _validatorClass;
  private HashMap<String, Object> _initParamMap = new HashMap<String, Object>();
  private String _description;

  /**
   * Sets the validator class.
   */
  public void setValidatorClass(Class validatorClass)
    throws RegistryException
  {
    _validatorClass = validatorClass;
    
    BeanUtil.validateClass(_validatorClass, TagLibraryValidator.class);
  }

  /**
   * Gets the validator class.
   */
  public Class getValidatorClass()
  {
    return _validatorClass;
  }

  /**
   * Adds an init-param.
   */
  public void addInitParam(String name, String value)
  {
    _initParamMap.put(name, value);
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(InitParam initParam)
  {
    _initParamMap.putAll(initParam.getParameters());
  }

  /**
   * Gets the jsp version.
   */
  public HashMap getInitParamMap()
  {
    return _initParamMap;
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Gets the description.
   */
  public String getDescription()
  {
    return _description;
  }

  /**
   * Returns the validator.
   */
  public TagLibraryValidator getValidator()
    throws Exception
  {
    TagLibraryValidator validator;

    validator = (TagLibraryValidator) _validatorClass.newInstance();

    validator.setInitParameters(_initParamMap);

    return validator;
  }
}
