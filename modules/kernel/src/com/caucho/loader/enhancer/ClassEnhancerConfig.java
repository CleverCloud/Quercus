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

package com.caucho.loader.enhancer;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JavaClass;
import com.caucho.config.ConfigException;
import com.caucho.java.gen.GenClass;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Configuration for a class-enhancer builder.
 */
public class ClassEnhancerConfig implements ClassEnhancer {
  private static final L10N L = new L10N(ClassEnhancerConfig.class);

  private static final Logger log =
    Logger.getLogger(ClassEnhancerConfig.class.getName());
  
  private EnhancerManager _manager;
  
  private Class _annotation;
  private Class _type;
  private boolean _isStatic = true;

  private ClassEnhancer _enhancer;

  /**
   * Sets the manager.
   */
  public void setEnhancerManager(EnhancerManager manager)
  {
    _manager = manager;
  }

  /**
   * Sets the annotation.
   */
  public void setAnnotation(Class ann)
  {
    _annotation = ann;
  }

  /**
   * Gets the annotation.
   */
  public Class getAnnotation()
  {
    return _annotation;
  }
  
  /**
   * Sets the type of the method enhancer.
   */
  public void setType(Class type)
    throws Exception
  {
    _type = type;

    if (ClassEnhancer.class.isAssignableFrom(type)) {
      _enhancer = (ClassEnhancer) type.newInstance();
    }
    else
      throw new ConfigException(L.l("'{0}' is an unsupported class enhancer type.  ClassEnhancer is required.",
                                    type.getName()));
  }

  /**
   * Set true for a static instance.
   */
  public void setStatic(boolean isStatic)
  {
    _isStatic = isStatic;
  }

  /**
   * Initializes the enhancer.
   */
  public Object createInit()
  {
    return _enhancer;
  }

  /**
   * Initializes the config.
   */
  public void init()
    throws ConfigException
  {
    // _enhancer.setAnnotation(_annotation);
  }
  
  /**
   * Returns true if the class will be enhanced.
   */
  public boolean shouldEnhance(String className)
  {
    return _enhancer.shouldEnhance(className);
  }

  /**
   * Fixups for the pre-enhancement class.
   */
  public void preEnhance(JavaClass baseClass)
    throws Exception
  {
    _enhancer.preEnhance(baseClass);
  }

  /**
   * Enhances the class by adding to the GenClass.
   */
  public void enhance(GenClass genClass,
                      JClass baseClass,
                      String extClassName)
    throws Exception
  {
    _enhancer.enhance(genClass, baseClass, extClassName);
  }
  
  /**
   * Any post compilation fixups.
   */
  public void postEnhance(JavaClass extClass)
    throws Exception
  {
    _enhancer.postEnhance(extClass);
  }
}
