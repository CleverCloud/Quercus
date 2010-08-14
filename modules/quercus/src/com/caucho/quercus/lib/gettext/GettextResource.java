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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.gettext;

import com.caucho.vfs.BasicDependencyContainer;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeValue;
import com.caucho.quercus.lib.gettext.expr.PluralExpr;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Represents a container for gettext translations.
 */
class GettextResource
{
  private Env _env;
  protected Path _pathPO;
  private Path _pathMO;
  private Path _currentPath;

  private BasicDependencyContainer _depend;

  private PluralExpr _pluralExpr;

  private HashMap<StringValue, ArrayList<StringValue>> _translations;
  private String _charset;

  protected GettextResource(Env env,
                              Path root,
                              Locale locale,
                              CharSequence category,
                              CharSequence domain)
  {
    _env = env;
    
    StringBuilder sb = new StringBuilder(locale.toString());
    sb.append('/');
    sb.append(category);
    sb.append('/');
    sb.append(domain);
    sb.append(".po");

    _pathPO = lookupPath(env, root, sb.toString());

    sb.setCharAt(sb.length() - 2, 'm');
    _pathMO = lookupPath(env, root, sb.toString());

    init();
  }

  private Path lookupPath(Env env, Path root, String relPath)
  {
    return root.lookup(relPath);
  }

  private void init()
  {
    if (_pathPO != null && _pathPO.exists())
      _currentPath = _pathPO;
    else if (_pathMO != null && _pathMO.exists())
      _currentPath = _pathMO;
    else
      return;

    try {
      GettextParser parser;

      if (_depend == null)
        _depend = new BasicDependencyContainer();

      _depend.add(new Depend(_currentPath));

      if (_currentPath == _pathPO)
        parser = new POFileParser(_env, _currentPath);
      else
        parser = new MOFileParser(_env, _currentPath);

      _pluralExpr = parser.getPluralExpr();
      _translations = parser.readTranslations();
      _charset = parser.getCharset();
      
      parser.close();

    } catch (IOException e) {
      throw new QuercusModuleException(e.getMessage());
    }
  }

  /**
   * Returns the translation for this singular key.
   *
   * @param key
   */
  protected StringValue getTranslation(StringValue key)
  {
    if (isModified())
      init();

    return getTranslationImpl(key, 0);
  }

  private boolean isModified()
  {
    if (_depend == null)
      return true;

    return _depend.isModified();
  }

  /**
   * Returns the translation for this plural key.
   *
   * @param key
   * @param quantity
   */
  protected StringValue getTranslation(StringValue key, int quantity)
  {
    if (isModified())
      init();

    if (_pluralExpr != null)
      return getTranslationImpl(key, _pluralExpr.eval(quantity));
    else
      return null;
  }

  /**
   * Returns the translation for this key at the specified index in the array.
   *
   * @param key to find translation of
   * @param index in the array for this key
   *
   * @return translated string, else null on error.
   */
  protected StringValue getTranslationImpl(StringValue key, int index)
  {
    if (_translations == null)
      return null;

    ArrayList<StringValue> pluralForms = _translations.get(key);

    if (pluralForms == null || pluralForms.size() == 0)
      return null;

    if (index < pluralForms.size())
      return pluralForms.get(index);
    else
      return pluralForms.get(0);
  }
  
  protected String getCharset()
  {
    return _charset;
  }
}
