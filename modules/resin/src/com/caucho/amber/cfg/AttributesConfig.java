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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import java.util.HashMap;

/**
 * &lt;attributes> tag in the orm.xml
 */
public class AttributesConfig {

  // no attributes

  // elements
  private HashMap<String, IdConfig> _idMap
    = new HashMap<String, IdConfig>();

  private HashMap<String, BasicConfig> _basicMap
    = new HashMap<String, BasicConfig>();

  private EmbeddedIdConfig _embeddedId;

  private HashMap<String, VersionConfig> _versionMap
    = new HashMap<String, VersionConfig>();

  private HashMap<String, ManyToOneConfig> _manyToOneMap
    = new HashMap<String, ManyToOneConfig>();

  private HashMap<String, OneToManyConfig> _oneToManyMap
    = new HashMap<String, OneToManyConfig>();

  private HashMap<String, OneToOneConfig> _oneToOneMap
    = new HashMap<String, OneToOneConfig>();

  private HashMap<String, ManyToManyConfig> _manyToManyMap
    = new HashMap<String, ManyToManyConfig>();

  private HashMap<String, EmbeddedConfig> _embeddedMap
    = new HashMap<String, EmbeddedConfig>();

  private HashMap<String, TransientConfig> _transientMap
    = new HashMap<String, TransientConfig>();


  /**
   * Adds a new <basic>.
   */
  public void addBasic(BasicConfig basic)
  {
    _basicMap.put(basic.getName(), basic);
  }

  /**
   * Returns the <basic> map.
   */
  public HashMap<String, BasicConfig> getBasicMap()
  {
    return _basicMap;
  }

  /**
   * Returns a <basic> config.
   */
  public BasicConfig getBasic(String name)
  {
    return _basicMap.get(name);
  }

  /**
   * Adds a new <id>.
   */
  public void addId(IdConfig id)
  {
    _idMap.put(id.getName(), id);
  }

  /**
   * Returns the <id> map.
   */
  public HashMap<String, IdConfig> getIdMap()
  {
    return _idMap;
  }

  /**
   * Returns an <id> config.
   */
  public IdConfig getId(String name)
  {
    return _idMap.get(name);
  }

  /**
   * Returns the <embedded-id> config.
   */
  public EmbeddedIdConfig getEmbeddedId()
  {
    return _embeddedId;
  }

  /**
   * Sets the <embedded-id> config.
   */
  public void setEmbeddedId(EmbeddedIdConfig embeddedId)
  {
    _embeddedId = embeddedId;
  }

  public HashMap<String, VersionConfig> getVersionMap()
  {
    return _versionMap;
  }

  public void addVersion(VersionConfig version)
  {
    _versionMap.put(version.getName(), version);
  }

  public VersionConfig getVersion(String name)
  {
    return _versionMap.get(name);
  }

  public HashMap<String, ManyToOneConfig> getManyToOneMap()
  {
    return _manyToOneMap;
  }

  public void addManyToOne(ManyToOneConfig manyToOne)
  {
    _manyToOneMap.put(manyToOne.getName(), manyToOne);
  }

  public ManyToOneConfig getManyToOne(String name)
  {
    return _manyToOneMap.get(name);
  }

  public HashMap<String, OneToManyConfig> getOneToManyMap()
  {
    return _oneToManyMap;
  }

  public void addOneToMany(OneToManyConfig oneToMany)
  {
    _oneToManyMap.put(oneToMany.getName(), oneToMany);
  }

  public OneToManyConfig getOneToMany(String name)
  {
    return _oneToManyMap.get(name);
  }

  public HashMap<String, OneToOneConfig> getOneToOneMap()
  {
    return _oneToOneMap;
  }

  public void addOneToOne(OneToOneConfig oneToOne)
  {
    _oneToOneMap.put(oneToOne.getName(), oneToOne);
  }

  public OneToOneConfig getOneToOne(String name)
  {
    return _oneToOneMap.get(name);
  }

  public HashMap<String, ManyToManyConfig> getManyToManyMap()
  {
    return _manyToManyMap;
  }

  public void addManyToMany(ManyToManyConfig manyToMany)
  {
    _manyToManyMap.put(manyToMany.getName(), manyToMany);
  }

  public ManyToManyConfig getManyToMany(String name)
  {
    return _manyToManyMap.get(name);
  }

  public HashMap<String, EmbeddedConfig> getEmbeddedMap()
  {
    return _embeddedMap;
  }

  public void addEmbedded(EmbeddedConfig embedded)
  {
    _embeddedMap.put(embedded.getName(), embedded);
  }

  public EmbeddedConfig getEmbedded(String name)
  {
    return _embeddedMap.get(name);
  }

  public HashMap<String, TransientConfig> getTransientMap()
  {
    return _transientMap;
  }

  public void addTransient(TransientConfig transientConfig)
  {
    _transientMap.put(transientConfig.getName(), transientConfig);
  }

  public TransientConfig getTransient(String name)
  {
    return _transientMap.get(name);
  }
}
