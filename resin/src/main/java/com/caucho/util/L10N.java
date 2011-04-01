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

package com.caucho.util;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Localization
 */
public class L10N {
  private static Logger _log;
  
  private static final HashMap<String,Properties> _l10nMap
    = new HashMap<String,Properties>();

  private Class _cl;
  
  private Boolean _isMessageFileAvailable;
  private Boolean _isStringFileAvailable;
  
  private Properties _messages;
  private Properties _strings;
  
  public L10N(Class cl)
  {
    _cl = cl;
  }
  
  public String l(String msg)
  {
    msg = getTranslated(msg);
    
    msg = getTranslated(msg);

    StringBuilder cb = new StringBuilder();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else {
          i = parseString(msg, i + 1, cb);
        }
      }
    }
    
    return cb.toString();
  }
  
  public String l(String msg, long l)
  {
    return l(msg, String.valueOf(l));
  }
  
  public String l(String msg, Object o)
  {
    msg = getTranslated(msg);

    StringBuilder cb = new StringBuilder();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (ch == '0') {
          cb.append(o);
          i += 3;
        }
        else if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else {
          i = parseString(msg, i + 1, cb);
        }
      }
    }
    
    return cb.toString();
  }
  
  public String l(String msg, Object o1, Object o2)
  {
    msg = getTranslated(msg);
    
    StringBuilder cb = new StringBuilder();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else {
          i = parseString(msg, i + 1, cb);
        }
      }
    }
    
    return cb.toString();
  }

  public String l(String msg, Object o1, int i2)
  {
    return l(msg, o1, String.valueOf(i2));
  }

  public String l(String msg, int i1, int i2)
  {
    return l(msg, String.valueOf(i1), String.valueOf(i2));
  }
  
  public String l(String msg, Object o1, Object o2, Object o3)
  {
    msg = getTranslated(msg);
    
    StringBuilder cb = new StringBuilder();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '2') {
          cb.append(o3);
          i += 3;
        }
        else if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else {
          i = parseString(msg, i + 1, cb);
        }
      }
    }
    
    return cb.toString();
  }
  
  public String l(String msg, Object o1, Object o2, Object o3, Object o4)
  {
    msg = getTranslated(msg);
    
    StringBuilder cb = new StringBuilder();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '2') {
          cb.append(o3);
          i += 3;
        }
        else if (ch == '3') {
          cb.append(o4);
          i += 3;
        }
        else if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else {
          i = parseString(msg, i + 1, cb);
        }
      }
    }
    
    return cb.toString();
  }
  
  public String l(String msg, Object o1, Object o2,
                  Object o3, Object o4, Object o5)
  {
    msg = getTranslated(msg);
    
    StringBuilder cb = new StringBuilder();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '2') {
          cb.append(o3);
          i += 3;
        }
        else if (ch == '3') {
          cb.append(o4);
          i += 3;
        }
        else if (ch == '4') {
          cb.append(o5);
          i += 3;
        }
        else if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else {
          i = parseString(msg, i + 1, cb);
        }
      }
    }
    
    return cb.toString();
  }
  
  public String l(String msg, Object o1, Object o2,
                  Object o3, Object o4, Object o5, Object o6)
  {
    msg = getTranslated(msg);
    
    StringBuilder cb = new StringBuilder();

    int length = msg.length();
    int i = 0;

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '{' || i + 2 >= length) {
        cb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 1);
        
        if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else if (ch == '0') {
          cb.append(o1);
          i += 3;
        }
        else if (ch == '1') {
          cb.append(o2);
          i += 3;
        }
        else if (ch == '2') {
          cb.append(o3);
          i += 3;
        }
        else if (ch == '3') {
          cb.append(o4);
          i += 3;
        }
        else if (ch == '4') {
          cb.append(o5);
          i += 3;
        }
        else if (ch == '5') {
          cb.append(o6);
          i += 3;
        }
        else if (ch == '{') {
          cb.append('{');
          i += 2;
        }
        else {
          i = parseString(msg, i + 1, cb);
        }
      }
    }
    
    return cb.toString();
  }

  private int parseString(String msg, int i, StringBuilder sb)
  {
    StringBuilder arg = new StringBuilder();
    int len = msg.length();
    char ch;

    for (; i < len && (ch = msg.charAt(i)) != '}'; i++) {
      arg.append(ch);
    }

    Properties stringMap = getStringMap();

    if (stringMap != null) {
      String string = (String) stringMap.get(arg.toString());

      if (string != null) {
        sb.append(string);
        return i + 1;
      }
    }

    sb.append('{');
    sb.append(arg);
    
    return i;
  }

  private String getTranslated(String msg)
  {
    Properties messages = getMessageMap();
    
    if (messages == null)
      return msg;
    
    String translated = (String) messages.get(msg);

    if (translated == null) {
      return msg;
    }
    else
      return translated;
  }

  private Properties getMessageMap()
  {
    if (_messages != null)
      return _messages;
    
    if (Boolean.FALSE.equals(_isMessageFileAvailable))
      return null;
    
    String name = _cl.getName().replace('.', '/');
    int p = name.lastIndexOf('/');

    if (p > 0)
      name = name.substring(0, p);

    name = name + "/messages";

    _messages = _l10nMap.get(name);

    if (_messages != null) {
      _isMessageFileAvailable = Boolean.TRUE;
      
      return _messages;
    }

    _messages = loadProperties(name);

    if (_messages != null) {
      _isMessageFileAvailable = Boolean.TRUE;
      _l10nMap.put(name, _messages);
      return _messages;
    }
    else {
      _isMessageFileAvailable = Boolean.FALSE;
      return null;
    }
  }

  private Properties getStringMap()
  {
    if (_strings != null)
      return _strings;
    
    if (Boolean.FALSE.equals(_isStringFileAvailable))
      return null;
    
    String name = _cl.getName().replace('.', '/');
    int p = name.lastIndexOf('/');

    if (p > 0)
      name = name.substring(0, p);

    name = name + "/strings";

    _strings = _l10nMap.get(name);

    if (_strings != null) {
      _isStringFileAvailable = Boolean.TRUE;
      
      return _strings;
    }

    _strings = loadProperties(name);

    if (_strings != null) {
      _isStringFileAvailable = Boolean.TRUE;
      _l10nMap.put(name, _strings);
      return _strings;
    }
    else {
      _isStringFileAvailable = Boolean.FALSE;
      return null;
    }
  }

  private Properties loadProperties(String name)
  {
    InputStream is = null;
    
    try {
      name = name + ".properties";
      
      is = _cl.getClassLoader().getResourceAsStream(name);

      if (is != null) {
        Properties messages = new Properties();
        messages.load(is);

        return messages;
      }
    } catch (Exception e) {
      e.printStackTrace();
      // can't log in logging routines
    } finally {
      IoUtil.close(is);
    }

    return null;
  }

  private void init(String path)
  {
    if (! path.startsWith("/"))
      path = "/" + path;

    Properties messages = _l10nMap.get(path);
    
    InputStream is = null;
    Locale locale = Locale.getDefault();
    
    try {
      String language = locale.getLanguage();

      String xmlName = path + "_" + language;

      /* XXX: punt for now
      is = getClass().getResourceAsStream(xmlName);
      
      if (is != null) {
        RegistryNode registry = null;

        try {
          ReadStream rs = Vfs.openRead(is);
        
          Registry root = Registry.parse(rs);
          registry = root.getTop();

          rs.close();
        } finally {
          is.close();
        }

        messages = new HashMap<String,String>();
        
        l10nMap.put(path, messages);

        RegistryNode localization = registry.lookup("localization");
        Iterator iter = localization.select("message");
        while (iter.hasNext()) {
          RegistryNode msg = (RegistryNode) iter.next();

          String key = msg.getString("key", null);
          String value = msg.getString("value", null);

          if (key != null && value != null)
            messages.put(key, value);
        }
      }
      */
    } catch (Exception e) {
      log().log(Level.FINE, e.toString(), e);
    }
  }

  private Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(L10N.class.getName());

    return _log;
  }
}
