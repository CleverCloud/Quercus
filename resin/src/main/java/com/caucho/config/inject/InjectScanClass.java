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

package com.caucho.config.inject;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.decorator.Delegate;
import javax.ejb.MessageDriven;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.ejb.Stateful;
import javax.enterprise.context.NormalScope;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Stereotype;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;

import com.caucho.config.inject.InjectScanManager.AnnType;
import com.caucho.inject.Jndi;
import com.caucho.inject.MBean;
import com.caucho.loader.enhancer.ScanClass;

/**
 * The web beans container for a given environment.
 */
class InjectScanClass implements ScanClass
{
  private static final Logger log
    = Logger.getLogger(InjectScanClass.class.getName());
  
  private static final char []PRODUCES
    = Produces.class.getName().toCharArray();
  private static final char []DISPOSES
    = Disposes.class.getName().toCharArray();
  private static final char []OBSERVES
    = Observes.class.getName().toCharArray();
  private static final char []OBJECT
    = Object.class.getName().toCharArray();
  
  private static final HashSet<Class<?>> _registerAnnotationSet
    = new HashSet<Class<?>>();
  
  private final String _className;
  private final InjectScanManager _scanManager;
  
  private ArrayList<InjectScanClass> _children;
  
  private boolean _isScanClass;
  private boolean _isRegisterRequired;
  private boolean _isRegistered;
  
  private boolean _isObserves;
  
  private boolean _isVeto;
  
  InjectScanClass(String className, InjectScanManager manager)
  {
    _className = className;
    _scanManager = manager; 
  }

  /**
   * Returns the bean's class name.
   */
  public String getClassName()
  {
    return _className;
  }
  
  public void setScanClass()
  {
    _isScanClass = true;
  }
  
  public boolean isScanClass()
  {
    return _isScanClass;
  }
  
  /**
   * Returns true if registration is required
   */
  public boolean isRegisterRequired()
  {
    return _isRegisterRequired;
  }
  
  public boolean isRegistered()
  {
    return _isRegistered;
  }
  
  public boolean isObserves()
  {
    return _isObserves;
  }
  
  @Override
  public void addInterface(char[] buffer, int offset, int length)
  {
    addParent(new String(buffer, offset, length));
  }

  @Override
  public void addSuperClass(char[] buffer, int offset, int length)
  {
    if (isMatch(buffer, offset, length, OBJECT)) {
      return;
    }
    
    addParent(new String(buffer, offset, length));
  }

  @Override
  public void addClassAnnotation(char[] buffer, int offset, int length)
  {
    try {
      AnnType annType = _scanManager.loadAnnotation(buffer, offset, length);
      
      if (annType == null)
        return;
      
      if (_registerAnnotationSet.contains(annType.getType())) {
        if (annType.getType() == Observes.class)
          _isObserves = true;
        
        _isRegisterRequired = true;
        return;
      }
      
      for (Annotation ann : annType.getAnnotations()) {
        Class<? extends Annotation> metaAnnType = ann.annotationType();
      
        if (metaAnnType == Stereotype.class) {
          _isRegisterRequired = true;
        }
        else if (metaAnnType == Scope.class) {
          _isRegisterRequired = true;
        }
        else if (metaAnnType == NormalScope.class) {
          // ioc/02a3
          _isRegisterRequired = true;
        }
      }
    } catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  @Override
  public void addPoolString(char[] buffer, int offset, int length)
  {
    if (isMatch(buffer, offset, length, PRODUCES)) {
      _isRegisterRequired = true;
    }
    else if (isMatch(buffer, offset, length, DISPOSES)) {
      _isRegisterRequired = true;
    }
    else if (isMatch(buffer, offset, length, OBSERVES)) {
      _isRegisterRequired = true;
      _isObserves = true;
    }
  }

  @Override
  public void finishScan()
  {
    if (_isRegisterRequired || _scanManager.isCustomExtension()) {
      register();
    }
  }  
  
  private void addParent(String className)
  {
    InjectScanClass parent = _scanManager.createScanClass(className);
    
    parent.addChild(this);
  }
  
  private void addChild(InjectScanClass child)
  {
    if (_children == null)
      _children = new ArrayList<InjectScanClass>();
    
    if (! _children.contains(child))
      _children.add(child);
  }
  
  void register()
  {
    if (_isScanClass && ! _isRegistered) {
      _isRegistered = true;

      _scanManager.addDiscoveredClass(this);
    }
    
    if (_children != null) {
      for (InjectScanClass child : _children) {
        child.register();
      }
    }
  }
  
  private boolean isMatch(char []buffer, int offset, int length,
                          char []matchBuffer)
  {
    if (length != matchBuffer.length)
      return false;
    
    for (int i = 0; i < length; i++) {
      if (buffer[offset + i] != matchBuffer[i])
        return false;
    }
    
    return true;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _className + "]";
  }

  static {
    _registerAnnotationSet.add(Inject.class);
    _registerAnnotationSet.add(Named.class);
    _registerAnnotationSet.add(Specializes.class);
    _registerAnnotationSet.add(Delegate.class);
    _registerAnnotationSet.add(Startup.class);
    _registerAnnotationSet.add(Jndi.class);
    _registerAnnotationSet.add(MBean.class);
    _registerAnnotationSet.add(Stateless.class);
    _registerAnnotationSet.add(Stateful.class);
    _registerAnnotationSet.add(javax.ejb.Singleton.class);
    _registerAnnotationSet.add(MessageDriven.class);
    _registerAnnotationSet.add(Qualifier.class);
  }
}