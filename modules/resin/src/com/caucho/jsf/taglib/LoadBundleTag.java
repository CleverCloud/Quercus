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

package com.caucho.jsf.taglib;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.webapp.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import javax.servlet.jsp.tagext.*;

import com.caucho.jsp.*;
import com.caucho.util.*;

public class LoadBundleTag extends TagSupport
{
  private static final L10N L = new L10N(LoadBundleTag.class);
  private static final Logger log
    = Logger.getLogger(LoadBundleTag.class.getName());
  
  private String _var;
  private ValueExpression _basename;

  public void setVar(String var)
  {
    _var = var;
  }

  public void setBasename(ValueExpression basename)
  {
    _basename = basename;
  }

  @Override
  public int doStartTag()
    throws JspException
  {
    FacesContext context = FacesContext.getCurrentInstance();

    String basename = (String) _basename.getValue(context.getELContext());

    Locale locale = context.getViewRoot().getLocale();

    BundleManager bundleManager = BundleManager.create();

    LocalizationContext lc = null;

    if (locale != null)
      lc = bundleManager.getBundle(basename, locale);

    if (lc == null)
      lc = bundleManager.getBundle(basename);

    if (lc == null)
      throw new JspException(L.l("'{0}' is an unknown ResourceBundle basename.",
                                 basename));

    ResourceBundle bundle = lc.getResourceBundle();

    this.pageContext.getRequest().setAttribute(_var, new BundleMap(bundle));
    
    return SKIP_BODY;
  }

  static class BundleMap extends AbstractMap<String,String> {
    private ResourceBundle _bundle;
    private BundleEntrySet _entrySet;

    BundleMap(ResourceBundle bundle)
    {
      _bundle = bundle;
    }

    @Override
    public String get(Object key)
    {
      try {
        String value = _bundle.getString(String.valueOf(key));

        if (value != null)
          return value;
        else
          return "???" + key + "???";
      } catch (MissingResourceException e) {
        log.log(Level.FINER, e.toString(), e);

        return "???" + key + "???";
      }
    }

    public Set<Map.Entry<String,String>> entrySet()
    {
      if (_entrySet == null)
        _entrySet = new BundleEntrySet(_bundle);

      return _entrySet;
    }
  }
    
  static class BundleEntrySet extends AbstractSet<Map.Entry<String,String>> {
    private ResourceBundle _bundle;
    private ArrayList<String> _keys = new ArrayList<String>();

    BundleEntrySet(ResourceBundle bundle)
    {
      _bundle = bundle;

      Enumeration<String> e = bundle.getKeys();
      
      while (e.hasMoreElements())
        _keys.add(e.nextElement());
    }

    public int size()
    {
      return _keys.size();
    }

    public Iterator<Map.Entry<String,String>> iterator()
    {
      return new EntryIterator(_bundle, _keys);
    }
  }

  static class EntryIterator implements Iterator<Map.Entry<String,String>> {
    private ResourceBundle _bundle;
    private ArrayList<String> _keys;
    private Entry _entry = new Entry();
    private int _index;
    
    EntryIterator(ResourceBundle bundle, ArrayList<String> keys)
    {
      _bundle = bundle;
      _keys = keys;
    }

    public boolean hasNext()
    {
      return _index < _keys.size();
    }

    public Map.Entry<String,String> next()
    {
      if (_index < _keys.size()) {
        _index++;
        return _entry;
      }
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
    
    class Entry implements Map.Entry<String,String> {
      public String getKey()
      {
        return _keys.get(_index - 1);
      }
      
      public String setKey(String key)
      {
        throw new UnsupportedOperationException();
      }

      public String getValue()
      {
        return _bundle.getString(getKey());
      }
      
      public String setValue(String key)
      {
        throw new UnsupportedOperationException();
      }
    }
  }
}
