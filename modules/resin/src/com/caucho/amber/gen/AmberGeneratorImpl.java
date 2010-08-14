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

package com.caucho.amber.gen;

import com.caucho.amber.entity.Embeddable;
import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.Listener;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.*;
import com.caucho.java.JavaCompiler;
import com.caucho.java.gen.DependencyComponent;
import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Enhancing the java objects for Amber mapping.
 */
public class AmberGeneratorImpl implements AmberGenerator {
  private static final L10N L = new L10N(AmberGeneratorImpl.class);
  private static final Logger log
    = Logger.getLogger(AmberGeneratorImpl.class.getName());

  private AmberPersistenceUnit _amberPersistenceUnit;

  private ArrayList<String> _pendingClassNames =
    new ArrayList<String>();

  public AmberGeneratorImpl(AmberPersistenceUnit manager)
  {
    _amberPersistenceUnit = manager;
  }

  /**
   * Configures the type.
   */
  public void configure(AbstractEnhancedType type)
    throws Exception
  {
  }

  /**
   * Generates the type.
   */
  public void generate(AbstractEnhancedType type)
    throws Exception
  {
    generateJava(null, type);
  }

  /**
   * Generates the type.
   */
  public void generateJava(JavaClassGenerator javaGen,
                           AbstractEnhancedType type)
    throws Exception
  {
    if (isPreload(javaGen, type) || type.isGenerated())
      return;

    type.setGenerated(true);
    //type.setInstanceClassLoader(javaGen.getClassLoader());

    GenClass genClass = new GenClass(type.getInstanceClassName());

    genClass.setSuperClassName(type.getBeanClass().getName());
    
    genClass.addImport("com.caucho.amber.manager.*");
    genClass.addImport("com.caucho.amber.entity.*");
    genClass.addImport("com.caucho.amber.type.*");

    AmberMappedComponent componentGenerator
      = (AmberMappedComponent) type.getComponentGenerator();

    if (componentGenerator != null) {
      // type is EntityType or MappedSuperclassType

      genClass.addInterfaceName(type.getComponentInterfaceName());

      componentGenerator.setRelatedType((EntityType) type);
      componentGenerator.setBaseClassName(type.getBeanClass().getName());
      componentGenerator.setExtClassName(type.getInstanceClassName());

      genClass.addComponent(componentGenerator);

      DependencyComponent depend = genClass.addDependencyComponent();
      depend.addDependencyList(componentGenerator.getDependencies());
    }
    else if (type instanceof EmbeddableType) {
      genClass.addInterfaceName("com.caucho.amber.entity.Embeddable");

      EmbeddableComponent embeddable = new EmbeddableComponent();

      embeddable.setEmbeddableType((EmbeddableType) type);
      embeddable.setBaseClassName(type.getBeanClass().getName());
      embeddable.setExtClassName(type.getInstanceClassName());

      genClass.addComponent(embeddable);
    }
    else {
      genClass.addInterfaceName("com.caucho.amber.entity.Listener");

      ListenerComponent listener = new ListenerComponent();

      listener.setListenerType((ListenerType) type);
      listener.setBaseClassName(type.getBeanClass().getName());
      listener.setExtClassName(type.getInstanceClassName());

      genClass.addComponent(listener);
    }

    javaGen.generate(genClass);
  }

  /**
   * Generates the type.
   */
  public boolean isPreload(JavaClassGenerator javaGen,
                           AbstractEnhancedType type)
    throws Exception
  {
    Class cl;

    if (type.isEnhanced())
      cl = javaGen.loadClass(type.getBeanClass().getName());
    else
      cl = javaGen.preload(type.getInstanceClassName());

    Class expectedClass = Listener.class;

    if (type instanceof EntityType)
      expectedClass = Entity.class;
    else if (type instanceof EmbeddableType)
      expectedClass = Embeddable.class;

    return cl != null && expectedClass.isAssignableFrom(cl);
  }

  /**
   * Compiles the pending classes.
   */
  public void compile()
    throws Exception
  {
    if (_pendingClassNames.size() == 0)
      return;

    String []javaFiles = new String[_pendingClassNames.size()];

    for (int i = 0; i < _pendingClassNames.size(); i++) {
      String name = _pendingClassNames.get(i);
      name = name.replace('.', '/') + ".java";

      javaFiles[i] = name;
    }
    _pendingClassNames.clear();

    EntityGenerator gen = new EntityGenerator();
    JavaCompiler compiler = gen.getCompiler();

    compiler.compileBatch(javaFiles);
  }
}
