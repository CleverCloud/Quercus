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

package com.caucho.server.webapp;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.inject.Qualifier;
import javax.servlet.annotation.WebServlet;

import com.caucho.config.ConfigException;
import com.caucho.config.Enhanced;
import com.caucho.config.EnhancedLiteral;
import com.caucho.config.extension.ProcessBeanImpl;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.inject.LazyExtension;
import com.caucho.inject.Module;
import com.caucho.remote.annotation.ProxyType;
import com.caucho.remote.annotation.ServiceType;
import com.caucho.remote.client.ProtocolProxyFactory;
import com.caucho.remote.server.ProtocolServletFactory;
import com.caucho.server.dispatch.ServletMapping;

/**
 * Standard XML behavior for META-INF/beans.xml
 */
@Module
public class WebAppInjectExtension implements Extension
{
  private InjectManager _beanManager;
  private WebApp _webApp;

  public WebAppInjectExtension(InjectManager manager,
                               WebApp webApp)
  {
    _beanManager = manager;
    _webApp = webApp;
  }

  @LazyExtension
  public void processAnnotatedType(@Observes ProcessAnnotatedType<?> event)
  {
    try {
      AnnotatedType<?> annotated = event.getAnnotatedType();

      if (annotated == null
          || annotated.getAnnotations() == null
          || annotated.isAnnotationPresent(Enhanced.class)) {
        return;
      }

      for (Annotation ann : annotated.getAnnotations()) {
        Class<?> annType = ann.annotationType();

        if (annType.isAnnotationPresent(ProxyType.class)) {
          ProxyType proxyType
          = (ProxyType) annType.getAnnotation(ProxyType.class);

          Class<?> factoryClass = proxyType.defaultFactory();
          ProtocolProxyFactory proxyFactory
          = (ProtocolProxyFactory) factoryClass.newInstance();

          proxyFactory.setProxyType(ann);
          proxyFactory.setAnnotated(annotated);

          Object proxy = proxyFactory.createProxy((Class<?>) annotated.getBaseType());

          AnnotatedTypeImpl<?> annotatedType
          = new AnnotatedTypeImpl((AnnotatedType) annotated);

          annotatedType.addAnnotation(EnhancedLiteral.ANNOTATION);

          BeanBuilder<?> builder
          = _beanManager.createBeanFactory(annotatedType);

          /*
          factory.name(bean.getName());

          for (Type type : bean.getTypes()) {
            factory.type(type);
          }

           */
          for (Annotation binding : annotated.getAnnotations()) {
            Class<?> bindingType = binding.annotationType();

            if (bindingType.isAnnotationPresent(Qualifier.class))
              builder.qualifier(binding);
          }

          _beanManager.addBean(builder.singleton(proxy));

          event.veto();
        }
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @LazyExtension
  public void processBean(@Observes ProcessBeanImpl<?> event)
  {
    try {
      Annotated annotated = event.getAnnotated();
      Bean<?> bean = event.getBean();

      if (annotated == null
          || annotated.getAnnotations() == null
          || annotated.isAnnotationPresent(Enhanced.class)) {
        return;
      }

      for (Annotation ann : annotated.getAnnotations()) {
        Class<?> annType = ann.annotationType();
        
        if (annType.equals(WebServlet.class)) {
          WebServlet webServlet = (WebServlet) ann;
      
          ServletMapping mapping = new ServletMapping();

          for (String value : webServlet.value()) {
            mapping.addURLPattern(value);
          }
          
          for (String value : webServlet.urlPatterns()) {
            mapping.addURLPattern(value);
          }
          
          mapping.setBean(bean);
        
          mapping.init();

          _webApp.addServletMapping(mapping);

          event.veto();
        }
        else if (annType.isAnnotationPresent(ServiceType.class)) {
          ServiceType serviceType
            = (ServiceType) annType.getAnnotation(ServiceType.class);

          Class<?> factoryClass = serviceType.defaultFactory();
          ProtocolServletFactory factory
            = (ProtocolServletFactory) factoryClass.newInstance();

          factory.setServiceType(ann);
          factory.setAnnotated(annotated);

          Method urlPatternMethod = annType.getMethod("urlPattern");

          String urlPattern = (String) urlPatternMethod.invoke(ann);
      
          ServletMapping mapping = new ServletMapping();
          mapping.addURLPattern(urlPattern);
          mapping.setBean(bean);

          mapping.setProtocolFactory(factory);
        
          mapping.init();

          _webApp.addServletMapping(mapping);

          // event.veto();
        }
        else if (annType.isAnnotationPresent(ProxyType.class)) {
          event.veto();
        }
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
