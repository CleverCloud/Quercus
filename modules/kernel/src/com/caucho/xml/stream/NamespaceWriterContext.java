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

package com.caucho.xml.stream;

import com.caucho.vfs.WriteStream;

import org.w3c.dom.*;

import static javax.xml.XMLConstants.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *  Maintains a stack of namespace contexts
 */
public class NamespaceWriterContext extends NamespaceContextImpl
{
  // map from URIs -> NamespaceBinding
  private final LinkedHashMap<String,NamespaceBinding> _bindings
    = new LinkedHashMap<String,NamespaceBinding>();

  // xml/310w
  private ArrayList<NamespaceBinding> _duplicatePrefixes
    = new ArrayList<NamespaceBinding>();
  private int _uniqueId = 0;
  private NamespaceBinding _nullBinding = new NamespaceBinding(null, null, 0);

  private boolean _repair = false;

  public NamespaceWriterContext()
  {
    this(false);
  }

  public NamespaceWriterContext(boolean repair)
  {
    super();

    _repair = repair;
  }
  
  protected void remove(String prefix, String uri)
  {
    _bindings.remove(uri);
  }

  public boolean getRepair()
  {
    return _repair;
  }

  public void setRepair(boolean repair)
  {
    _repair = repair;
  }

  public void declare(String prefix, String uri)
  {
    declare(prefix, uri, false);
  }

  /**
   * declares a new namespace prefix in the current context
   */
  public void declare(String prefix, String uri, boolean forceEmit)
  {
    NamespaceBinding binding;

    if (uri == null)
      binding = _nullBinding;

    else {
      binding = _bindings.get(uri);

      if (binding != null
          && binding.getPrefix() != null
          && binding.getPrefix().equals(prefix)) {
        // for writing, ignore matching prefixes
        if (forceEmit)
          binding.setEmit(true);

        return;
      }
      else if (binding == null) {
        // set the URI to null so that addOldBinding registers that there
        // was no previous binding
        binding = new NamespaceBinding(prefix, null, _version);

        _bindings.put(uri, binding);
      }
    }

    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding == null) {
      eltBinding = new ElementBinding();
      
      _stack.set(_stack.size() - 1, eltBinding);
    }

    eltBinding.addOldBinding(binding, prefix, binding.getUri(), uri);

    if (binding.isEmit() && ! prefix.equals(binding.getPrefix())) {
      NamespaceBinding copy = new NamespaceBinding(binding.getPrefix(), 
                                                   binding.getUri(),
                                                   binding.getVersion());
      copy.setEmit(true);
      _duplicatePrefixes.add(copy);
    }

    _version++;
    binding.setPrefix(prefix);
    binding.setUri(uri);
    binding.setVersion(_version);
    binding.setEmit(forceEmit);
  }

  /**
   *  declares a new namespace prefix in the current context; the
   *  auto-allocated prefix is returned
   */
  public String declare(String uri)
  {
    NamespaceBinding binding = _bindings.get(uri);

    // without an explicit prefix, don't add a new prefix
    if (binding != null)
      return binding.getPrefix();

    String prefix = "ns" + _uniqueId++;

    declare(prefix, uri, _repair);
    
    return prefix;
  }

  /**
   * looks up the uri, returns the prefix it corresponds to
   */
  public String getPrefix(String uri)
  {
    NamespaceBinding binding = _bindings.get(uri);

    if (binding == null)
      return null;
    
    return binding.getPrefix();
  }

  public String getNamespaceURI(String prefix)
  {
    for (NamespaceBinding binding : _bindings.values()) {
      if (prefix.equals(binding.getPrefix()))
        return binding.getUri();
    }

    return null;
  }

  public Iterator getPrefixes(String uri)
  {
    ArrayList<String> prefixes = new ArrayList<String>();

    for (NamespaceBinding binding : _bindings.values()) {
      if (uri.equals(binding.getUri()))
        prefixes.add(binding.getPrefix());
    }

    return prefixes.iterator();
  }

  public void emitDeclarations(WriteStream ws)
    throws IOException
  {
    for (NamespaceBinding binding : _bindings.values())
      binding.emit(ws);

    for (int i = 0; i < _duplicatePrefixes.size(); i++)
      _duplicatePrefixes.get(i).emit(ws);

    _duplicatePrefixes.clear();
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append("NamespaceWriterContext:\n");

    for (Map.Entry<String,NamespaceBinding> entry : _bindings.entrySet())
      sb.append(entry.getKey() + "->" + entry.getValue() + "\n");

    return sb.toString();
  }
}
