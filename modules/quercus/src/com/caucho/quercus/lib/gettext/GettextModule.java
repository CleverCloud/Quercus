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

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;


/**
 * Module to find translated strings and return them in desired charset.
 * Translations are LRU cached.
 */
public class GettextModule
  extends AbstractQuercusModule
{
  private LruCache<Object,GettextResource> _cache
    = new LruCache<Object,GettextResource>(16);

  private final Logger log
    = Logger.getLogger(GettextModule.class.getName());
  private final L10N L = new L10N(GettextModule.class);

  public String []getLoadedExtensions()
  {
    return new String[] { "gettext" };
  }

  /**
   * Sets charset of translated strings that are returned from this domain.
   *
   * @param env
   * @param domain
   * @param codeset
   * @return codeset
   */
  public String bind_textdomain_codeset(Env env,
                                             String domainName,
                                             String charset)
  {
    getDomain(env, domainName).setCharset(charset);
    
    return charset;
  }

  /**
   * Changes root directory of domain.
   *
   * @param env
   * @param domain
   * @param directory
   * @return directory
   */
  public Value bindtextdomain(Env env,
                              String domainName,
                              StringValue directory)
  {
    GettextDomain domain = getDomain(env, domainName);
    
    domain.setPath(env, directory);
    
    if (domain.getPath() == null)
      return BooleanValue.FALSE;
    else
      return directory;
  }

  /**
   * Same as gettext, but allows overriding of domain and category.
   *
   * @param env
   * @param domain
   * @param message
   * @param category
   */
  public StringValue dcgettext(Env env,
                              String domainName,
                              StringValue message,
                              int category,
                              Value []args)
  {
    return translate(env,
                     getDomain(env, domainName),
                     getCategory(env, category),
                     message,
                     args);
  }

  /**
   * Same as ngettext, but allows overriding of domain and category.
   *
   * @param env
   * @param domain
   * @param msgid1
   * @param msgid2
   * @param n
   * @param category
   */
  public StringValue dcngettext(Env env,
                                String domainName,
                                StringValue msgid1,
                                StringValue msgid2,
                                int n,
                                int category,
                                Value []args)
  {
    return translate(env,
                     getDomain(env, domainName),
                     getCategory(env, category),
                     msgid1,
                     msgid2,
                     n,
                     args);
  }

  /**
   * Same as gettext, but allows overriding of current domain.
   *
   * @param env
   * @param domain
   * @param message
   */
  public StringValue dgettext(Env env,
                              String domainName,
                              StringValue message,
                              Value []args)
  {
    return translate(env,
                     getDomain(env, domainName),
                     "LC_MESSAGES",
                     message,
                     args);
  }

  /**
   * Same as ngettext, but allows overriding of current domain.
   *
   * @param env
   * @param domain
   * @param msgid1
   * @param msgid2
   * @param n
   */
  public StringValue dngettext(Env env,
                               String domainName,
                               StringValue msgid1,
                               StringValue msgid2,
                               int n,
                               Value []args)
  {
    return translate(env,
                     getDomain(env, domainName),
                     "LC_MESSAGES",
                     msgid1,
                     msgid2,
                     n,
                     args);
  }

  /**
   * Alias of gettext().
   *
   * @param env
   * @param message
   */
  public StringValue _(Env env, StringValue message, Value []args)
  {
    return gettext(env, message, args);
  }

  /**
   * Returns translated string from current domain and default category.
   *
   * @param env
   * @param message
   */
  public StringValue gettext(Env env, StringValue message, Value []args)
  {
    return translate(env,
                     getCurrentDomain(env),
                     "LC_MESSAGES",
                     message,
                     args);
  }

  /**
   * Returns translated plural string form from current domain and default
   * category.
   *
   * @param env
   * @param msgid1
   * @param msgid2
   * @param n
   * @return translated string, or original plural string if n == 1,
   *     else return original singular string
   */
  public StringValue ngettext(Env env,
                              StringValue msgid1,
                              StringValue msgid2,
                              int n,
                              Value []args)
  {
    return translate(env,
                     getCurrentDomain(env),
                     "LC_MESSAGES",
                     msgid1,
                     msgid2,
                     n,
                     args);
  }

  /**
   * Changes the current domain.
   *
   * @param env
   * @param domain
   * @return name of current domain after change.
   */
  public String textdomain(Env env,
                                @Optional Value domain)
  {
    if (! domain.isNull()) {
      String name = domain.toString();
      
      setCurrentDomain(env, name);
      
      return name;
    }
    
    return getCurrentDomain(env).getName();
  }

  /**
   * Retrieves the translation for message.
   * 
   * @param env
   * @param domain
   * @param category
   * @param message
   *
   * @return translation found, else message
   */
  private StringValue translate(Env env,
                                GettextDomain domain,
                                CharSequence category,
                                StringValue message,
                                Value []args)
  {
    Locale locale = env.getLocaleInfo().getMessages().getLocale();

    GettextResource resource = getResource(env,
                                           domain.getPath(),
                                           locale,
                                           category,
                                           domain.getName());

    StringValue unicodeTranslation = resource.getTranslation(message);

    if (unicodeTranslation == null)
      unicodeTranslation = message;

    StringValue translation
      = message.create(env, unicodeTranslation, domain.getCharset());
    
    return format(env, translation, args);
  }

  /**
   * Retrieves the plural translation for msgid1.
   * 
   * @param env
   * @param domain
   * @param category
   * @param msgid1
   * @param msgid2
   *
   * @return translation found, else msgid1 if n == 1, else msgid2
   */
  private StringValue translate(Env env,
                                GettextDomain domain,
                                CharSequence category,
                                StringValue msgid1,
                                StringValue msgid2,
                                int quantity,
                                Value []args)
  {
    Locale locale = env.getLocaleInfo().getMessages().getLocale();

    GettextResource resource = getResource(env,
                                           domain.getPath(),
                                           locale,
                                           category,
                                           domain.getName());

    StringValue unicodeTranslation
      = resource.getTranslation(msgid1, quantity);

    if (unicodeTranslation == null)
      unicodeTranslation = errorReturn(msgid1, msgid2, quantity);

    StringValue translation = msgid1.create(
        env, unicodeTranslation, domain.getCharset());
    
    return format(env, translation, args);
  }

  private GettextResource getResource(Env env,
                              Path path,
                              Locale locale,
                              CharSequence category,
                              String domain)
  {
    ArrayList<Object> key = new ArrayList<Object>();

    key.add(path.getFullPath());
    key.add(locale);
    key.add(category);
    key.add(domain);

    GettextResource resource = _cache.get(key);

    if (resource == null) {
      resource = new GettextResource(env, path, locale, category, domain);
      _cache.put(key, resource);
    }

    return resource;
  }

  private GettextDomainMap getDomains(Env env)
  {
    Object val = env.getSpecialValue("caucho.gettext_domains");

    if (val == null) {
      val = new GettextDomainMap();

      env.setSpecialValue("caucho.gettext_domains", val);
    }
    
    return (GettextDomainMap) val;
  }
  
  private GettextDomain getDomain(Env env, String name)
  {
    return getDomains(env).getDomain(env, name);
  }
  
  private GettextDomain getCurrentDomain(Env env)
  {
    return getDomains(env).getCurrent(env);
  }

  private void setCurrentDomain(Env env, String name)
  {
    getDomains(env).setCurrent(name);
  }

  /**
   * Gets the name for this category.
   */
  private String getCategory(Env env, int category)
  {
    switch (category) {
      case StringModule.LC_MESSAGES:
        return "LC_MESSAGES";
      case StringModule.LC_ALL:
        return "LC_ALL";
      case StringModule.LC_CTYPE:
        return "LC_CTYPE";
      case StringModule.LC_NUMERIC:
        return "LC_NUMERIC";
      case StringModule.LC_TIME:
        return "LC_TIME";
      case StringModule.LC_COLLATE:
        return "LC_COLLATE";
      case StringModule.LC_MONETARY:
        return "LC_MONETARY";
      default:
        env.warning(L.l("Invalid category. Please use named constants"));
        return "LC_MESSAGES";
    }
  }

  private static StringValue errorReturn(StringValue msgid1,
                              StringValue msgid2,
                              int n)
  {
    if (n == 1)
      return msgid1;
    else
      return msgid2;
  }

  private static StringValue format(Env env,
                              StringValue msg,
                              Value []args)
  {
    if (args.length == 0)
      return msg;
    
    StringValue sb;
    
    if (msg.isUnicode())
      sb = env.createUnicodeBuilder();
    else
      sb = env.createBinaryBuilder();
    
    return formatImpl(env, msg, args, sb);
  }

  /*
  private static StringValue formatBinary(Env env,
                                          StringValue msg,
                                          Value []args,
                                          String charset)
  {
    StringValue sb = env.createBinaryBuilder();

    byte []bytes = null;
    
    try {
      bytes = msg.toString().getBytes(charset);
    }
    catch (UnsupportedEncodingException e) {
      throw new QuercusModuleException(e);
    }
    
    int i = 0;
    int len = bytes.length;
    
    
    while (i < len) {
      byte ch = bytes[i];

      if (ch != '[' || i + 4 > len) {
        sb.appendByte(ch);
        i++;
      }
      else if (bytes[i + 1] != '_') {
        sb.appendByte('[');
        i++;
      }
      else if (bytes[i + 3] != ']') {
        sb.appendByte('[');
        sb.appendByte('_');
        i += 2;
      }
      else {
        ch = bytes[i + 2];
        int argIndex = ch - '0';

        if (0 <= argIndex && argIndex < args.length) {
          sb.append(args[argIndex]);
          i += 4;
        }
        else {
          sb.appendByte('[');
          i++;
        }
      }
    }

    return sb;
  }
  */

  private static StringValue formatImpl(Env env,
                                        StringValue msg,
                                        Value []args,
                                        StringValue sb)
  {
    int i = 0;
    int length = msg.length();

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '[' || i + 4 > length) {
        sb.append(ch);
        i++;
      }
      else if (msg.charAt(i + 1) != '_') {
        sb.append(ch);
        i++;
      }
      else if (msg.charAt(i + 3) != ']') {
        sb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 2);
        int argIndex = ch - '0';

        if (0 <= argIndex && argIndex < args.length) {
          sb.append(args[argIndex]);
          i += 4;
        }
        else {
          sb.append('[');
          i++;
        }
      }
    }
    
    return sb;
  }
}
