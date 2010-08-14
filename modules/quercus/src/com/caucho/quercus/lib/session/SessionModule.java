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

package com.caucho.quercus.lib.session;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.OutputModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.ModuleStartupListener;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.util.L10N;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;
import java.util.Iterator;

/**
 * Quercus session handling
 */
public class SessionModule extends AbstractQuercusModule 
  implements ModuleStartupListener {
  private static final L10N L = new L10N(SessionModule.class);
  private static final Logger log
    = Logger.getLogger(SessionModule.class.getName());

  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }

  public String []getLoadedExtensions()
  {
    return new String[] { "session" };
  }

  public void startup(Env env)
  {
    if (env.getConfigVar("session.auto_start").toBoolean())
      session_start(env);
  }

  /**
   * Returns and/or sets the value of session.cache_limiter, affecting the
   * cache related headers that are sent as a result of a call to
   * {@link #session_start(Env)}.
   *
   * If the optional parameter is not supplied, this function
   * simply returns the existing value.
   * If the optional parameter is supplied, the returned value
   * is the old value that was set before the new value is applied.
   *
   * Valid values are "nocache" (the default), "private", "private_no_expire",
   * and "public". If a value other than these values is supplied,
   * then a warning is produced
   * and no cache related headers will be sent to the client.
   */
  public Value session_cache_limiter(Env env, @Optional Value newValue)
  {
    Value value = env.getIni("session.cache_limiter");

    if (newValue.isDefault())
      return value;

    env.setIni("session.cache_limiter", newValue);

    return value;
  }

  public Value session_cache_expire(Env env, @Optional Value newValue)
  {
    Value value = (LongValue) env.getSpecialValue("cache_expire");

    if (value == null)
      value = env.getIni("session.cache_expire");

    if (newValue != null && ! newValue.isDefault())
      env.setSpecialValue("cache_expire", newValue);

    return LongValue.create(value.toLong());
  }

  /**
   * Alias of session_write_close.
   */
  public static Value session_commit(Env env)
  {
    return session_write_close(env);
  }

  /**
   * Encodes the session values.
   */
  public static boolean session_decode(Env env, StringValue value)
  {
    SessionArrayValue session = env.getSession();

    if (session == null) {
      env.warning(L.l("session_decode requires valid session"));
      return false;
    }

    return session.decode(env, value);
  }

  /**
   * Encodes the session values.
   */
  public static String session_encode(Env env)
  {
    SessionArrayValue session = env.getSession();

    if (session == null) {
      env.warning(L.l("session_encode requires valid session"));
      return null;
    }

    return session.encode(env);
  }

  /**
   * Destroys the session
   */
  public static boolean session_destroy(Env env)
  {
    SessionArrayValue session = env.getSession();

    if (session == null)
      return false;

    env.destroySession(session.getId());

    return true;
  }

  /**
   * Returns the session cookie parameters
   */
  public static ArrayValue session_get_cookie_params(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    array.put(env, "lifetime", env.getIniLong("session.cookie_lifetime"));
    array.put(env, "path", env.getIniString("session.cookie_path"));
    array.put(env, "domain", env.getIniString("session.cookie_domain"));
    array.put(env, "secure", env.getIniBoolean("session.cookie_secure"));

    return array;
  }

  /**
   * Returns the session id
   */
  public static String session_id(Env env, @Optional String id)
  {
    Value sessionIdValue = (Value) env.getSpecialValue("caucho.session_id");

    String oldValue;

    if (sessionIdValue != null)
      oldValue = sessionIdValue.toString();
    else
      oldValue = "";

    if (id != null && id.length() > 0)
      env.setSpecialValue("caucho.session_id", env.createString(id));

    return oldValue;
  }

  /**
   * Returns true if a session variable is registered.
   */
  public static boolean session_is_registered(Env env, StringValue name)
  {
    SessionArrayValue session = env.getSession();
    
    return session != null && session.get(name).isset();
  }

  /**
   * Returns the object's class name
   */
  public Value session_module_name(Env env, @Optional String newValue)
  {
    Value value = env.getIni("session.save_handler");

    if (newValue != null && ! newValue.equals(""))
      env.setIni("session.save_handler", newValue);

    return value;
  }

  /**
   * Returns the object's class name
   */
  public Value session_name(Env env, @Optional String newValue)
  {
    Value value = env.getIni("session.name");

    if (newValue != null && newValue.length() > 0)
      env.setIni("session.name", newValue);

    return value;
  }

  /**
   * Regenerates the session id.
   * 
   * This function first creates a new session id.  The old session
   * values are migrated to this new session.  Then a new session cookie
   * is sent (XXX: send only if URL rewriting is off?).  Changing the
   * session ID should be transparent. 
   * Therefore, session callbacks should not be called.
   */
  public static boolean session_regenerate_id(Env env,
                                              @Optional boolean deleteOld)
  {
    // php/1k82, php/1k83
    if (env.getSession() == null)
      return ! deleteOld;
    
    String sessionId = env.generateSessionId();
    
    if (deleteOld) {
      session_destroy(env);

      SessionArrayValue session = env.createSession(sessionId, true);
    }
    else {
      SessionArrayValue session = env.getSession();
      
      session.setId(sessionId);
    }

    // update environment to new session id
    session_id(env, sessionId);

    if (env.getIni("session.use_cookies").toBoolean())
      generateSessionCookie(env, sessionId);

    return true;
  }

  /**
   * Registers global variables in the session.
   */
  public boolean session_register(Env env, Value []values)
  {
    SessionArrayValue session = env.getSession();

    if (session == null) {
      session_start(env);
      session = env.getSession();
    }

    for (int i = 0; i < values.length; i++)
      sessionRegisterImpl(env, (ArrayValue) session, values[i]);

    return true;
  }

  /**
   * Registers global variables in the session.
   */
  private void sessionRegisterImpl(Env env, ArrayValue session, Value nameV)
  {
    nameV = nameV.toValue();

    if (nameV instanceof StringValue) {
      String name = nameV.toString();

      Value var = env.getGlobalVar(name);

      Value value = session.get(nameV);

      if (value.isset())
        var.set(value);

      session.put(nameV, var);
    } else if (nameV.isArray()) {
      ArrayValue array = (ArrayValue) nameV.toValue();

      for (Value subValue : array.values()) {
        sessionRegisterImpl(env, session, subValue);
      }
    }
  }

  /**
   * Returns the session's save path
   */
  public Value session_save_path(Env env, @Optional String newValue)
  {
    Value value = env.getIni("session.save_path");

    if (newValue != null && ! newValue.equals(""))
      env.setIni("session.save_path", newValue);

    if (value.isNull() || value.length() == 0) {
      // XXX: should we create work directory if does not exist?
      value = env.createString(env.getWorkDir().getPath());
    }
    
    return value;
  }

  /**
   * Sets the session cookie parameters
   */
  public Value session_set_cookie_params(Env env,
                                         long lifetime,
                                         @Optional Value path,
                                         @Optional Value domain,
                                         @Optional Value isSecure,
                                         @Optional Value isHttpOnly)
  {
    env.setIni("session.cookie_lifetime", String.valueOf(lifetime));

    if (path.isset())
      env.setIni("session.cookie_path", path.toString());

    if (domain.isset())
      env.setIni("session.cookie_domain", domain.toString());

    if (isSecure.isset())
      env.setIni("session.cookie_secure", 
                 isSecure.toBoolean() ? "1" : "0");

    return NullValue.NULL;
  }

  /**
   * Sets the session save handler
   */
  public boolean session_set_save_handler(Env env,
                                          Callable open,
                                          Callable close,
                                          Callable read,
                                          Callable write,
                                          Callable directory,
                                          Callable gc)

  {
    SessionCallback cb
      = new SessionCallback(open, close, read, write, directory, gc);

    env.setSessionCallback(cb);

    return true;
  }

  /**
   * Start the session
   */
  public static boolean session_start(Env env)
  {
    if (env.getSession() != null) {
      env.notice(L.l("session has already been started"));
      return true;
    }

    SessionCallback callback = env.getSessionCallback();

    Value sessionIdValue = (Value) env.getSpecialValue("caucho.session_id");
    String sessionId = null;

    env.removeConstant("SID");

    String cookieName = env.getIni("session.name").toString();
    boolean generateCookie = true;
    boolean create = false;

    if (callback != null) {
      String savePath = env.getIni("session.save_path").toString();

      if (savePath == null || "".equals(savePath))
        callback.open(env, env.getWorkDir().getPath(), cookieName);
      else
        callback.open(env, savePath, cookieName);
    }

    //
    // Use cookies to transmit session id
    // 
    if (env.getIni("session.use_cookies").toBoolean()) {
      if (sessionIdValue != null)
        sessionId = sessionIdValue.toString();

      if (sessionId == null || "".equals(sessionId)) {
        Cookie []cookies = env.getRequest().getCookies();

        for (int i = 0; cookies != null && i < cookies.length; i++) {
          if (cookies[i].getName().equals(cookieName)
              && ! "".equals(cookies[i].getValue())) {
            sessionId = cookies[i].getValue();
            generateCookie = false;
          }
        }
      }

      if (! generateCookie)
        env.addConstant("SID", env.getEmptyString(), false);
    }

    //
    // Use URL rewriting to transmit session id
    //

    if (env.getIniBoolean("session.use_trans_sid")
        && ! env.getIniBoolean("session.use_only_cookies")) {
      if (sessionId == null) {
        if (sessionIdValue != null)
          sessionId = sessionIdValue.toString();

        if (sessionId == null || "".equals(sessionId))
          sessionId = env.getRequest().getParameter(cookieName);

        if (sessionId == null || "".equals(sessionId)) {
          sessionId = env.generateSessionId();
          create = true;
        }
      }

      env.addConstant("SID", env.createString(cookieName + '=' + sessionId),
                      false);
      
      OutputModule.pushUrlRewriter(env);
    }
    
    if (sessionId == null || "".equals(sessionId)) {
      sessionId = env.generateSessionId();
      create = true;
    }
    
    HttpServletResponse response = env.getResponse();

    if (response == null) {
    }
    else if (response.isCommitted())
      env.warning(
          L.l("cannot send session cache limiter headers "
              + "because response is committed"));
    else {
      Value cacheLimiterValue = env.getIni("session.cache_limiter");
      String cacheLimiter = String.valueOf(cacheLimiterValue);

      Value cacheExpireValue = (LongValue)env.getSpecialValue("cache_expire");

      if (cacheExpireValue == null)
        cacheExpireValue = env.getIni("session.cache_expire");

      int cacheExpire = cacheExpireValue.toInt() * 60;

      if ("nocache".equals(cacheLimiter)) {
        response.setHeader("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
        response.setHeader(
            "Cache-Control",
            "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
      }
      else if ("private".equals(cacheLimiter)) {
        response.setHeader("Cache-Control", "private, max-age="
            + cacheExpire + ", pre-check=" + cacheExpire);
      }
      else if ("private_no_expire".equals(cacheLimiter)) {
        response.setHeader("Cache-Control", "private, max-age="
            + cacheExpire + ", pre-check=" + cacheExpire);
      }
      else if ("public".equals(cacheLimiter)) {
        response.setHeader(
            "Cache-Control", "public, max-age=" + cacheExpire
                + ", pre-check=" + cacheExpire);
      }
      else if ("none".equals(cacheLimiter)) {
      }
      else {
        // php/1k16
        //response.setHeader(
        // "Cache-Control", cacheLimiter + ", max-age=" + cacheExpire
        // + ", pre-check=" + cacheExpire);
      }
    }

    SessionArrayValue session = env.createSession(sessionId, create);
    sessionId = session.getId();

    if (env.getIni("session.use_cookies").toBoolean() && generateCookie) {
      generateSessionCookie(env, sessionId);
    }
    env.setSpecialValue("caucho.session_id", env.createString(sessionId));
    
    return true;
  }

  /**
   * Sends a new session cookie.
   */
  private static void generateSessionCookie(Env env, String sessionId)
  { 
    final HttpServletResponse response = env.getResponse();
   
    String cookieName = env.getIni("session.name").toString();
    
    StringValue cookieValue
      = env.createString(cookieName + '=' + sessionId);
    
    env.addConstant("SID", 
                            cookieValue,
                            false);

    Cookie cookie = new Cookie(cookieName, sessionId);
    // #2649
    String cookieVersion = env.getIniString("session.cookie_version");
    if (! "0".equals(cookieVersion))
      cookie.setVersion(1);

    if (response.isCommitted()) {
      env.warning(
          L.l("cannot send session cookie because response is committed"));
    }
    else {
      Value path = env.getIni("session.cookie_path");
      cookie.setPath(path.toString());

      Value maxAge = env.getIni("session.cookie_lifetime");

      if (maxAge.toInt() != 0)
        cookie.setMaxAge(maxAge.toInt());

      Value domain = env.getIni("session.cookie_domain");

      // this is for 3rd party servlet containers that don't check the domain
      // before sending the cookie
      if (domain.length() > 0) {
        cookie.setDomain(domain.toString());
      }
      
      Value secure = env.getIni("session.cookie_secure");
      cookie.setSecure(secure.toBoolean());

      response.addCookie(cookie);
    }
  }

  /**
   * Unsets the specified session values
   */
  public boolean session_unregister(Env env, Value key)
  {
    SessionArrayValue session = env.getSession();
    
    if (session == null)
      return false;

    session.remove(key);

    return true;
  }

  /**
   * Unsets the session values
   */
  public Value session_unset(Env env)
  {
    SessionArrayValue session = env.getSession();

    if (session == null)
      return NullValue.NULL;

    session.clear();

    return NullValue.NULL;
  }

  /**
   * Writes the session and closes it.
   */
  public static Value session_write_close(Env env)
  {
    env.sessionWriteClose();

    return NullValue.NULL;
  }

  /**
   * Converts an integer to a printable character
   */
  private static char encode(long code)
  {
    code = code & 0x3f;

    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }


  static final IniDefinition INI_SESSION_SAVE_PATH
    = _iniDefinitions.add("session.save_path", "", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_NAME
    = _iniDefinitions.add("session.name", "PHPSESSID", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_SAVE_HANDLER
    = _iniDefinitions.add("session.save_handler", "files", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_AUTO_START
    = _iniDefinitions.add("session.auto_start", false, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_GC_PROBABILITY_START
    = _iniDefinitions.add("session.gc_probability_start", true, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_GC_DIVISOR
    = _iniDefinitions.add("session.gc_divisor", 100, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_GC_MAXLIFETIME
    = _iniDefinitions.add("session.gc_maxlifetime", 1440, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_SERIALIZE_HANDLER
    = _iniDefinitions.add("session.serialize_handler", "quercus", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_COOKIE_LIFETIME
    = _iniDefinitions.add("session.cookie_lifetime", 0, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_COOKIE_PATH
    = _iniDefinitions.add("session.cookie_path", "/", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_COOKIE_DOMAIN
    = _iniDefinitions.add("session.cookie_domain", "", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_COOKIE_SECURE
    = _iniDefinitions.add("session.cookie_secure", "", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_USE_COOKIES
    = _iniDefinitions.add("session.use_cookies", true, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_USE_ONLY_COOKIES
    = _iniDefinitions.add("session.use_only_cookies", true, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_REFERER_CHECK
    = _iniDefinitions.add("session.referer_check", "", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_ENTROPY_FILE
    = _iniDefinitions.add("session.entropy_file", "", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_ENTROPY_LENGTH
    = _iniDefinitions.add("session.entropy_length", false, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_CACHE_LIMITER
    = _iniDefinitions.add("session.cache_limiter", "nocache", PHP_INI_ALL);
  static final IniDefinition INI_SESSION_CACHE_EXPIRE
    = _iniDefinitions.add("session.cache_expire", 180, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_USE_TRANS_SID
    = _iniDefinitions.add("session.use_trans_sid", false, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_BUG_COMPAT_42
    = _iniDefinitions.add("session.bug_compat_42", true, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_BUG_COMPAT_WARN
    = _iniDefinitions.add("session.bug_compat_warn", true, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_HASH_FUNCTION
    = _iniDefinitions.add("session.hash_function", false, PHP_INI_ALL);
  static final IniDefinition INI_SESSION_HASH_BITS_PER_CHARACTER
    = _iniDefinitions.add("session.hash_bits_per_character", 4, PHP_INI_ALL);
  static final IniDefinition INI_URL_REWRITER_TAGS
    = _iniDefinitions.add(
      "url_rewriter.tags",
      "a=href,area=href,frame=src,form=,fieldset=", PHP_INI_ALL);
}
