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

package com.caucho.quercus.env;

import java.util.Locale;

public class LocaleInfo {
  private QuercusLocale _collate;
  private QuercusLocale _ctype;
  private QuercusLocale _monetary;
  private QuercusLocale _numeric;
  private QuercusLocale _time;
  private QuercusLocale _messages;

  LocaleInfo()
  {
    Locale locale = Locale.getDefault();
    
    setAll(new QuercusLocale(locale, null));
  }

  public void setAll(QuercusLocale locale)
  {
    setCollate(locale);
    setCtype(locale);
    setMonetary(locale);
    setNumeric(locale);
    setTime(locale);
    setMessages(locale);
  }

  public QuercusLocale getCollate()
  {
    return _collate;
  }

  public void setCollate(QuercusLocale locale)
  {
    _collate = locale;
  }

  public QuercusLocale getCtype()
  {
    return _ctype;
  }

  public void setCtype(QuercusLocale locale)
  {
    _ctype = locale;
  }

  public QuercusLocale getMonetary()
  {
    return _monetary;
  }

  public void setMonetary(QuercusLocale locale)
  {
    _monetary = locale;
  }

  public QuercusLocale getTime()
  {
    return _time;
  }

  public void setTime(QuercusLocale locale)
  {
    _time = locale;
  }

  public QuercusLocale getNumeric()
  {
    return _numeric;
  }

  public void setNumeric(QuercusLocale locale)
  {
    _numeric = locale;
  }

  public QuercusLocale getMessages()
  {
    return _messages;
  }

  public void setMessages(QuercusLocale locale)
  {
    _messages = locale;
  }
}
