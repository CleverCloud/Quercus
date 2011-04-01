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

import static javax.xml.XMLConstants.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 *  Maintains a stack of namespace contexts
 */
public class NamespaceReaderContext extends NamespaceContextImpl
{
  // The current prefix -> namespace bindings
  private final HashMap<String,NamespaceBinding> _bindings
    = new HashMap<String,NamespaceBinding>();

  private NamespaceBinding _nullEltBinding
    = new NamespaceBinding(null, null, 0);
  private NamespaceBinding _nullAttrBinding
    = new NamespaceBinding(null, null, 0);

  protected void remove(String prefix, String uri)
  {
    _bindings.remove(prefix);
  }

  /**
   * declares a new namespace prefix in the current context
   */
  public void declare(String prefix, String uri)
  {
    NamespaceBinding binding = getElementNamespace(prefix);

    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding == null) {
      eltBinding = new ElementBinding();
      
      _stack.set(_stack.size() - 1, eltBinding);
    }

    eltBinding.addOldBinding(binding, prefix, binding.getUri(), uri);

    _version++;
    binding.setUri(uri);
    binding.setVersion(_version);
  }

  /**
   * looks up the prefix, returns the uri it corresponds to
   */
  public String getUri(String prefix)
  {
    NamespaceBinding binding = _bindings.get(prefix);

    if (binding != null)
      return binding.getUri();
    else
      return null;
  }

  // --> javax.xml.namespace.NamespaceContext 
  
  public String getNamespaceURI(String prefix)
  {
    if (prefix == null)
      throw new IllegalArgumentException("Prefix may not be null");

    NamespaceBinding binding = _bindings.get(prefix);

    if (binding != null)
      return binding.getUri();

    String uri = null;
    
    if (XML_NS_PREFIX.equals(prefix))
      uri = XML_NS_URI;
    else if (XMLNS_ATTRIBUTE.equals(prefix))
      uri = XMLNS_ATTRIBUTE_NS_URI;

    return uri;
  }

  // inefficient, but the TCK uses it and virtually nobody else should
  public String getPrefix(String uri)
  {
    if (uri == null)
      throw new IllegalArgumentException("URI may not be null");
    else if ("".equals(uri))
      throw new IllegalArgumentException("URI may not be empty");
    else if (XML_NS_URI.equals(uri))
      return XML_NS_PREFIX;
    else if (XMLNS_ATTRIBUTE_NS_URI.equals(uri))
      return XMLNS_ATTRIBUTE;

    Iterator<NamespaceBinding> iterator = _bindings.values().iterator();

    while (iterator.hasNext()) {
      NamespaceBinding binding = iterator.next();

      if (uri.equals(binding.getUri()))
        return binding.getPrefix();
    }

    return null;
  }

  public Iterator getPrefixes(String uri)
  {
    return new PrefixIterator(uri);
  }

  // <-- javax.xml.namespace.NamespaceContext 
  
  // --> used by StaxIntern 
  public int getNumDecls()
  {
    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding != null) {
      ArrayList<Decl> oldBindingList = eltBinding.getOldBindingList();

      if (oldBindingList != null)
       return oldBindingList.size();
      else
       return 0;
    }
    else
      return 0;
  }

  public String getUri(int i)
  {
    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding != null) {
      return eltBinding.getOldBindingList().get(i).getNewUri();
    }
    else
      return null;
  }

  public String getPrefix(int i)
  {
    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding != null) {
      return eltBinding.getOldBindingList().get(i).getPrefix();
    }

    return null;
  }
  // <-- used by StaxIntern 

  NamespaceBinding getElementNamespace(String prefix)
  {
    NamespaceBinding binding;

    // default namespace
    if (prefix == null)
      binding = _nullEltBinding;
    else
      binding = _bindings.get(prefix);

    if (binding != null)
      return binding;
    else {
      binding = new NamespaceBinding(prefix, null, _version);

      _bindings.put(prefix, binding);

      return binding;
    }
  }

  NamespaceBinding getAttributeNamespace(String prefix)
  {
    NamespaceBinding binding;

    // default namespace
    if (prefix == null || DEFAULT_NS_PREFIX.equals(prefix))
      binding = _nullAttrBinding;
    else
      binding = _bindings.get(prefix);

    if (binding != null)
      return binding;
    else {
      binding = new NamespaceBinding(prefix, null, _version);

      _bindings.put(prefix, binding);

      return binding;
    }
  }

  private class PrefixIterator implements Iterator {
    private String _uri;
    private String _prefix;
    private Iterator<NamespaceBinding> _iterator;

    public PrefixIterator(String uri)
    {
      _uri = uri;
      _iterator = _bindings.values().iterator();
      advance();
    }

    public boolean hasNext()
    {
      return _prefix != null;
    }

    public Object next()
    {
      if (! hasNext())
        throw new NoSuchElementException();

      String prefix = _prefix;

      advance();

      return prefix;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    private void advance()
    {
      _prefix = null;

      while (_iterator.hasNext()) {
        NamespaceBinding binding = _iterator.next();

        if (_uri.equals(binding.getUri())) {
          _prefix = binding.getPrefix();
          break;
        }
      }
    }
  }

  public void print()
  {
    for (NamespaceBinding binding : _bindings.values())
      System.out.println(binding.toString());
  }
}
