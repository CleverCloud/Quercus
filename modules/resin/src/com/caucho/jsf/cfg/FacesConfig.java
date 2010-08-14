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

package com.caucho.jsf.cfg;

import com.caucho.config.program.ConfigProgram;
import java.util.*;

import javax.annotation.*;

import javax.faces.application.*;
import javax.faces.event.*;

public class FacesConfig
{
  private String _id;

  private String _version;

  private ApplicationConfig _application;

  private FactoryConfig _factory;

  private ArrayList<ComponentConfig> _componentList
    = new ArrayList<ComponentConfig>();

  private ArrayList<ConverterConfig> _converterList
    = new ArrayList<ConverterConfig>();

  private ArrayList<LifecycleConfig> _lifecycleList
    = new ArrayList<LifecycleConfig>();

  private ArrayList<ValidatorConfig> _validatorList
    = new ArrayList<ValidatorConfig>();

  private ArrayList<ReferencedBeanConfig> _referencedBeanList 
  = new ArrayList<ReferencedBeanConfig>();
  
  private ArrayList<RenderKitConfig> _renderKitList
    = new ArrayList<RenderKitConfig>();

  private ArrayList<ManagedBeanConfig> _managedBeanList
    = new ArrayList<ManagedBeanConfig>();

  private List<NavigationRule> _navigationRuleList
    = new ArrayList<NavigationRule>();

  public void setId(String id)
  {
  }

  public void setSchemaLocation(String location)
  {
  }
  
  public String getSchemaLocation()
  {
    return null;
  }

  public void setVersion(String version)
  {
  }

  public void setFacesConfigExtension(ConfigProgram program)
  {
  }

  public void addManagedBean(ManagedBeanConfig managedBean)
  {
    _managedBeanList.add(managedBean);
  }
  
  public ArrayList<ManagedBeanConfig> getManagedBeans()
  {
    return _managedBeanList;
  }

  public void addComponent(ComponentConfig component)
  {
    _componentList.add(component);
  }
  
  public void addReferencedBean(ReferencedBeanConfig referencedBean)
  {
    _referencedBeanList.add(referencedBean);
  }

  public void addConverter(ConverterConfig converter)
  {
    _converterList.add(converter);
  }

  public void addLifecycle(LifecycleConfig lifecycle)
  {
    _lifecycleList.add(lifecycle);
  }

  public void addValidator(ValidatorConfig validator)
  {
    _validatorList.add(validator);
  }

  public void addRenderKit(RenderKitConfig renderKit)
  {
    _renderKitList.add(renderKit);
  }

  public ApplicationConfig getApplication()
  {
    return _application;
  }

  public void setApplication(ApplicationConfig app)
  {
    _application = app;
  }

  public void setFactory(FactoryConfig factory)
  {
    _factory = factory;
  }

  public void addNavigationRule(NavigationRule rule)
  {
    _navigationRuleList.add(rule);
  }

  public List<NavigationRule> getNavigationRules()
  {
    return _navigationRuleList;
  }

  @PostConstruct
  public void init()
  {
    if (_factory != null)
      _factory.init();
  }

  public void configure(Application app)
  {
    for (int i = 0; i < _componentList.size(); i++)
      _componentList.get(i).configure(app);
    
    for (int i = 0; i < _converterList.size(); i++)
      _converterList.get(i).configure(app);
    
    for (int i = 0; i < _validatorList.size(); i++)
      _validatorList.get(i).configure(app);
    
    for (int i = 0; i < _renderKitList.size(); i++)
      _renderKitList.get(i).configure();
  }

  public void configurePhaseListeners(ArrayList<PhaseListener> list)
  {
    for (int i = 0; i < _lifecycleList.size(); i++)
      _lifecycleList.get(i).configurePhaseListeners(list);
  }
}
