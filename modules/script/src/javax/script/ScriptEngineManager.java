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

package javax.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * A manager for script engines.
 */
public class ScriptEngineManager {
  private static final Logger log =
    Logger.getLogger(ScriptEngineManager.class.getName());

  private ArrayList _engineFactories = new ArrayList();

  protected HashSet engineSpis = new HashSet();
  protected HashMap extensionAssociations = new HashMap();
  protected Bindings globalScope = new SimpleBindings();
  protected HashMap mimeTypeAssociations = new HashMap();
  protected HashMap nameAssociations = new HashMap();

  /**
   * The constructor checks for implementations of the factory.
   */
  public ScriptEngineManager()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * The constructor checks for implementations of the factory.
   */
  public ScriptEngineManager(ClassLoader loader)
  {
    initEngines(loader);
  }

  /**
   * Sets the global scope bindings.
   */
  public void setBindings(Bindings globalScope)
  {
    this.globalScope = globalScope;
  }

  /**
   * Gets the global scope bindings.
   */
  public Bindings getBindings()
  {
    return this.globalScope;
  }

  /**
   * Puts a value in the global scope.
   */
  public void put(String key, Object value)
  {
    getBindings().put(key, value);
  }

  /**
   * Gets a value in the global scope.
   */
  public Object get(String key)
  {
    return getBindings().get(key);
  }

  /**
   * Returns the engine for the script factory by name.
   */
  public ScriptEngine getEngineByName(String shortName)
  {
    Class fClass = (Class) this.nameAssociations.get(shortName);

    if (fClass != null)
      return getEngineByClass(fClass);
    else
      return null;
  }

  /**
   * Returns the engine for the script factory by extension.
   */
  public ScriptEngine getEngineByExtension(String ext)
  {
    Class fClass = (Class) this.extensionAssociations.get(ext);

    if (fClass != null)
      return getEngineByClass(fClass);
    else
      return null;
  }

  /**
   * Returns the engine for the script factory by mime-type.
   */
  public ScriptEngine getEngineByMimeType(String mimeType)
  {
    Class fClass = (Class) this.mimeTypeAssociations.get(mimeType);

    if (fClass != null)
      return getEngineByClass(fClass);
    else
      return null;
  }

  /**
   * Returns the known factories.
   */
  public List<ScriptEngineFactory> getEngineFactories()
  {
    return new ArrayList<ScriptEngineFactory>(_engineFactories);
  }

  /**
   * Registers an engine name.
   */
  public void registerEngineName(String name, Class factory)
  {
    this.nameAssociations.put(name, factory);
  }

  /**
   * Registers an engine mime-type.
   */
  public void registerEngineMimeType(String mimeType, Class factory)
  {
    this.mimeTypeAssociations.put(mimeType, factory);
  }

  /**
   * Registers an engine extension
   */
  public void registerEngineExtension(String ext, Class factory)
  {
    this.extensionAssociations.put(ext, factory);
  }

  /**
   * Initialize the script engine.
   */
  private void initEngines(ClassLoader loader)
  {
    try {
      Enumeration resources = loader.getResources("META-INF/services/javax.script.ScriptEngineFactory");

      while (resources.hasMoreElements()) {
        URL url = (URL) resources.nextElement();

        InputStream is = url.openStream();
        try {
          readFactoryFile(is);
        } finally {
          is.close();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initialize the script engine.
   */
  private void readFactoryFile(InputStream is)
    throws IOException
  {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line;

    while ((line = reader.readLine()) != null) {
      int p = line.indexOf('#');

      if (p >= 0)
        line = line.substring(0, p);

      line = line.trim();

      if (line.length() > 0) {
        addFactoryClass(line);
      }
    }
  }

  /**
   * Handles the factory class
   */
  private void addFactoryClass(String className)
  {
    try {
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      Class cl = Class.forName(className, false, loader);

      ScriptEngineFactory factory = (ScriptEngineFactory) cl.newInstance();

      if (this.engineSpis.contains(cl))
        return;

      _engineFactories.add(factory);

      this.engineSpis.add(cl);

      for (String name : factory.getNames()) {
        registerEngineName(name, cl);
      }

      for (String mimeType : factory.getMimeTypes()) {
        registerEngineMimeType(mimeType, cl);
      }

      for (String ext : factory.getExtensions()) {
        registerEngineExtension(ext, cl);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns an instance of the factory with the given class.
   */
  private ScriptEngine getEngineByClass(Class cl)
  {
    ScriptEngineFactory factory = getFactoryByClass(cl);

    if (factory != null) {
      ScriptEngine engine = factory.getScriptEngine();
      
      ScriptContext cxt = engine.getContext();
      cxt.setBindings(this.globalScope, ScriptContext.GLOBAL_SCOPE);
      
      return engine;
    }
    else
      return null;
  }

  /**
   * Returns the factory with the given class.
   */
  private ScriptEngineFactory getFactoryByClass(Class cl)
  {
    for (int i = 0; i < _engineFactories.size(); i++) {
      ScriptEngineFactory factory;
      factory = (ScriptEngineFactory) _engineFactories.get(i);

      if (factory.getClass().equals(cl))
        return factory;
    }

    return null;
  }
}

