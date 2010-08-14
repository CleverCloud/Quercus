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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.enhancer.ScanClass;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Path;

/**
 * The web beans container for a given environment.
 */
class InjectScanManager
  implements ScanListener
{
  private static final Logger log
    = Logger.getLogger(InjectScanManager.class.getName());
  
  private InjectManager _injectManager;
  
  private final HashMap<Path,ScanRootContext> _scanRootMap
    = new HashMap<Path,ScanRootContext>();
  
  private final ArrayList<ScanRootContext> _pendingScanRootList
    = new ArrayList<ScanRootContext>();
  
  private final ConcurrentHashMap<String,InjectScanClass> _scanClassMap
    = new ConcurrentHashMap<String,InjectScanClass>();
  
  private final ConcurrentHashMap<NameKey,AnnType> _annotationMap
    = new ConcurrentHashMap<NameKey,AnnType>();
  
  private boolean _isCustomExtension;
  
  private ArrayList<InjectScanClass> _pendingScanClassList
    = new ArrayList<InjectScanClass>();

  InjectScanManager(InjectManager injectManager)
  {
    _injectManager = injectManager;
  }

  /**
   * Returns the injection manager.
   */
  public InjectManager getInjectManager()
  {
    return _injectManager;
  }

  /**
   * True if a custom extension exists.
   */
  public void setIsCustomExtension(boolean isCustomExtension)
  {
    _isCustomExtension = isCustomExtension;
    
    if (isCustomExtension) {
      for (InjectScanClass scanClass : _scanClassMap.values()) {
        scanClass.register();
      }
    }
  }
  
  public boolean isCustomExtension()
  {
    return _isCustomExtension;
  }
  
  public ArrayList<ScanRootContext> getPendingScanRootList()
  {
    ArrayList<ScanRootContext> contextList
      = new ArrayList<ScanRootContext>(_pendingScanRootList);
    
    _pendingScanRootList.clear();
    
    return contextList;
  }
  
  public boolean isPending()
  {
    return _pendingScanClassList.size() > 0;
  }

  public void addDiscoveredClass(InjectScanClass injectScanClass)
  {
    if (! _pendingScanClassList.contains(injectScanClass)) {
      _pendingScanClassList.add(injectScanClass);
    }
  }
  
  /**
   * discovers pending beans.
   */
  public void discover()
  {
    ArrayList<InjectScanClass> pendingScanClassList
      = new ArrayList<InjectScanClass>(_pendingScanClassList);
    
    _pendingScanClassList.clear();
    
    for (InjectScanClass scanClass : pendingScanClassList) {
      getInjectManager().discoverBean(scanClass);
    }
  }
  
  //
  // ScanListener

  /**
   * Since CDI doesn't enhance, it's priority 1
   */
  @Override
  public int getScanPriority()
  {
    return 1;
  }
  
  @Override
  public boolean isRootScannable(Path root, String packageRoot)
  {
    ScanRootContext context = _scanRootMap.get(root);
    
    Path scanRoot = root;
    
    if (packageRoot != null) {
      scanRoot = scanRoot.lookup(packageRoot.replace('.', '/'));
      
      if (! scanRoot.lookup("beans.xml").canRead())
        return false;
    }
    else if (! (root.lookup("META-INF/beans.xml").canRead()
             || (root.getFullPath().endsWith("WEB-INF/classes/")
                 && root.lookup("../beans.xml").canRead()))) {
      return false;
    }

    if (context == null) {
      context = new ScanRootContext(scanRoot, packageRoot);
      _scanRootMap.put(root, context);
      _pendingScanRootList.add(context);
    }
    
    if (context.isScanComplete())
      return false;
    else {
      if (log.isLoggable(Level.FINER))
        log.finer("CanDI scanning " + root.getURL());

      context.setScanComplete(true);
      return true;
    }
  }

  /**
   * Checks if the class can be a simple class
   */
  @Override
  public ScanClass scanClass(Path root, String packageRoot,
                             String className, int modifiers)
  {
    // ioc/0j0k - package private allowed
    
    if (Modifier.isPrivate(modifiers))
      return null;
    else {
      InjectScanClass scanClass = createScanClass(className);
      
      scanClass.setScanClass();
      
      return scanClass;
    }
  }
  
  InjectScanClass getScanClass(String className)
  {
    return _scanClassMap.get(className);
  }
  
  InjectScanClass createScanClass(String className)
  {
    InjectScanClass scanClass = _scanClassMap.get(className);
    
    if (scanClass == null) {
      scanClass = new InjectScanClass(className, this);
      InjectScanClass oldScanClass;
      oldScanClass = _scanClassMap.putIfAbsent(className, scanClass);
      
      if (oldScanClass != null)
        scanClass = oldScanClass;
    }
    
    return scanClass;
  }
  
  /**
   * Loads an annotation for scanning.
   */
  public AnnType loadAnnotation(char[] buffer, int offset, int length)
    throws ClassNotFoundException
  {
    NameKey key = new NameKey(buffer, offset, length);
    
    AnnType annType = _annotationMap.get(key);
    
    if (annType != null)
      return annType;
    
    ClassLoader loader = getInjectManager().getClassLoader();
    
    String className = new String(buffer, offset, length);
    
    Class<?> cl = Class.forName(className, false, loader);
    
    annType = new AnnType(cl);
    
    _annotationMap.put(key.dup(), annType);
    
    return annType;
  }

  @Override
  public boolean isScanMatchAnnotation(CharBuffer string)
  {
    return false;
  }

  @Override
  public void classMatchEvent(EnvironmentClassLoader loader, 
                              Path root,
                              String className)
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _injectManager + "]";
  }
  
  static class NameKey {
    private char []_buffer;
    private int _offset;
    private int _length;
    
    NameKey(char []buffer, int offset, int length)
    {
      _buffer = buffer;
      _offset = offset;
      _length = length;
    }
    
    public NameKey dup()
    {
      char []buffer = new char[_length];
      
      System.arraycopy(_buffer, _offset, buffer, 0, _length);
      
      _buffer = buffer;
      _offset = 0;
      
      return this;
    }
    
    public int hashCode()
    {
      char []buffer = _buffer;
      int offset = _offset;
      int length = _length;
      int hash = length;
      
      for (length--; length >= 0; length--) {
        char value = buffer[offset + length];
        
        hash = 65521 * hash + value;
      }
      
      return hash;
    }
    
    public boolean equals(Object o)
    {
      if (! (o instanceof NameKey))
        return false;
      
      NameKey key = (NameKey) o;
     
      if (_length != key._length)
        return false;
      
      char []bufferA = _buffer;
      char []bufferB = key._buffer;
      
      int offsetA = _offset;
      int offsetB = key._offset;
      
      for (int i = _length - 1; i >= 0; i--) {
        if (bufferA[offsetA + i] != bufferB[offsetB + i])
          return false;
      }
      
      return true;
    }
  }
  
  static class AnnType {
    private Class<?> _type;
    private Annotation []_annotations;
    
    AnnType(Class<?> type)
    {
      _type = type;
    }
    
    public Class<?> getType()
    {
      return _type;
    }
    
    public Annotation []getAnnotations()
    {
      if (_annotations == null)
        _annotations = _type.getAnnotations();
      
      return _annotations;
    }
  }

}