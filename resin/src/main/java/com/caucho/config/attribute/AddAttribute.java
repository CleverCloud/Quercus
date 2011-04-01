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

package com.caucho.config.attribute;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.enterprise.context.spi.CreationalContext;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.type.ConfigType;
import com.caucho.config.type.TypeFactory;
import com.caucho.config.types.AnnotationConfig;
import com.caucho.config.xml.XmlBeanConfig;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public class AddAttribute<T> extends Attribute {
  private static final L10N L = new L10N(AddAttribute.class);

  public static final AddAttribute<XmlBeanConfig> ATTR
    = new AddAttribute<XmlBeanConfig>(XmlBeanConfig.class);
  
  private final ConfigType<T> _configType;
  private final Method _setMethod;

  private AddAttribute(Class<T> cl)
  {
    this(null, (ConfigType<T>) TypeFactory.getType(cl));
  }
  
  public AddAttribute(Method setMethod, ConfigType<T> configType)
  {
    _configType = configType;
    _setMethod = setMethod;
  }

  @Override
  public ConfigType<T> getConfigType()
  {
    return _configType;
  }

  public Method getMethod()
  {
    return _setMethod;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, QName qName)
    throws ConfigException
  {
    Class<?> cl = TypeFactory.loadClass(qName);

    if (cl == null) {
      ConfigType<?> type = TypeFactory.getFactory().getEnvironmentType(qName);

      if (type != null)
        return type.create(parent, qName);

      throw new ConfigException(L.l("'{0}.{1}' is an unknown class for element '{2}'",
                                    qName.getNamespaceURI(), qName.getLocalName(), qName));
    }

    if (Annotation.class.isAssignableFrom(cl)) {
      return new AnnotationConfig(cl);
    }
    else {
      XmlBeanConfig<?> config = new XmlBeanConfig(qName, cl);
      config.setInlineBean(true);

      // config.setScope("singleton");

      return config;
    }
  }
  
  /**
   * Sets the value of the attribute as text
   */
  @Override
  public void setText(Object bean, QName name, String value)
    throws ConfigException
  {
    Object objValue = create(bean, name);

    if (objValue instanceof XmlBeanConfig<?>) {
      XmlBeanConfig<?> config = (XmlBeanConfig<?>) objValue;

      if (! value.trim().equals("")) {
        config.addArg(new TextArgProgram(value));
      }

      config.init();
    }

    setValue(bean, name, objValue);
  }
  
  
  /**
   * Sets the value of the attribute
   */
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    try {
      if (value instanceof XmlBeanConfig<?>) {
        XmlBeanConfig<?> config = (XmlBeanConfig<?>) value;

        value = config.toObject();
      }
      else if (value instanceof AnnotationConfig) {
        AnnotationConfig config = (AnnotationConfig) value;

        value = config.replace();
      }

      if (_setMethod != null && value != null) {
        if (! _setMethod.getParameterTypes()[0].isAssignableFrom(value.getClass()))
          throw new ConfigException(L.l("'{0}.{1}' is not assignable from {2}",
                                        _setMethod.getDeclaringClass().getSimpleName(),
                                        _setMethod.getName(),
                                        value));

        _setMethod.invoke(bean, value);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  static class TextArgProgram extends ConfigProgram {
    private String _arg;

    TextArgProgram(String arg)
    {
      _arg = arg;
    }
    
    @Override
    public <T> void inject(T bean, CreationalContext<T> env)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    public Object configure(ConfigType<?> type, CreationalContext<?> env)
      throws ConfigException
    {
      return type.valueOf(_arg);
    }
  }
}
