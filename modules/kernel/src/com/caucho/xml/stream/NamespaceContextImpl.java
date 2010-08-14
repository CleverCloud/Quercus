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
 * @author Adam Megacz
 */

package com.caucho.xml.stream;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  Maintains a stack of namespace contexts
 */
public abstract class NamespaceContextImpl implements NamespaceContext
{
  public static final L10N L = new L10N(NamespaceContextImpl.class);

  // The stack of element bindings
  protected final ArrayList<ElementBinding> _stack
    = new ArrayList<ElementBinding>();

  protected int _version = 0;

  NamespaceContextImpl()
  {
    _stack.add(null);
  }

  public int getDepth()
  {
    return _stack.size() - 1;
  }

  /**
   * Creates a new subcontext and enters it
   */
  public void push()
  {
    _stack.add(null);
  }

  /**
   * deletes the current context and enters its parent
   */
  public void pop()
    throws XMLStreamException
  {
    if (_stack.size() == 0)
      throw new XMLStreamException(L.l("Multiple root elements in document"));

    ElementBinding eltBinding = _stack.remove(_stack.size() - 1);

    if (eltBinding != null) {
      ArrayList<Decl> oldBinding = eltBinding.getOldBindingList();

      for (int i = 0; oldBinding != null && i < oldBinding.size(); i++) {
        Decl decl = oldBinding.get(i);
        NamespaceBinding binding = decl.getBinding();

        _version++;

        if (decl.getOldUri() == null) {
          binding.setUri(decl.getOldUri());
          binding.setVersion(_version);
          // remove(binding.getPrefix(), binding.getUri());
        }
        else {
          binding.setUri(decl.getOldUri());
          binding.setVersion(_version);
        }
      }

      eltBinding.clear();
    }
  }
  
  public void setElementName(QName name)
  {
    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding == null) {
      eltBinding = new ElementBinding();
      
      _stack.set(_stack.size() - 1, eltBinding);
    }

    eltBinding.setName(name);
  }

  public QName getElementName()
  {
    ElementBinding eltBinding = _stack.get(_stack.size() - 1);

    if (eltBinding != null)
      return eltBinding.getName();
    else
      return null;
  }

  /**
   * declares a new namespace prefix in the current context
   */
  public abstract void declare(String prefix, String uri);
  protected abstract void remove(String prefix, String uri);

  static class ElementBinding
  {
    private QName _name;
    private ArrayList<Decl> _declList;

    public void setName(QName name)
    {
      _name = name;
    }

    public void clear()
    {
      _declList = null;
    }

    public QName getName()
    {
      return _name;
    }

    public void addOldBinding(NamespaceBinding binding, String prefix,
                              String oldUri, String newUri)
    {
      if (_declList == null)
        _declList = new ArrayList<Decl>();

      _declList.add(new Decl(binding, prefix, oldUri, newUri));
    }

    public ArrayList<Decl> getOldBindingList()
    {
      return _declList;
    }
  }

  static class Decl {
    private final NamespaceBinding _binding;
    private final String _prefix;
    private final String _oldUri;
    private final String _newUri;

    Decl(NamespaceBinding binding, String prefix,
         String oldUri, String newUri)
    {
      _binding = binding;
      _prefix = prefix;
      _oldUri = oldUri;
      _newUri = newUri;
    }

    NamespaceBinding getBinding()
    {
      return _binding;
    }

    String getPrefix()
    {
      return _prefix;
    }

    String getOldUri()
    {
      return _oldUri;
    }

    String getNewUri()
    {
      return _newUri;
    }

    public String toString()
    {
      return "Decl[binding=" + _binding + ",prefix=" + _prefix + ",oldUri=" + _oldUri + ",newUri=" + _newUri + "]";
    }
  }

  static class PrefixIterator
  {
    private ElementBinding _eltBinding;
    private int _index = 0;

    PrefixIterator(NamespaceContextImpl context, ElementBinding eltBinding)
    {
      _eltBinding = eltBinding;
    }

    public void remove()
    {
      throw new RuntimeException("not supported");
    }

    public boolean hasNext()
    {
      if (_eltBinding == null)
        return false;

      return _index < _eltBinding.getOldBindingList().size();
    }

    public Object next()
    {
      if (_eltBinding == null)
        return null;

      ArrayList<Decl> oldBindingList = _eltBinding.getOldBindingList();

      if (_index < oldBindingList.size())
        return oldBindingList.get(_index++).getPrefix();

      return null;
    }
  }
}
