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

package com.caucho.quercus.lib;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.UsesSymbolTable;
import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.*;

/**
 * PHP options
 */
public class OptionsModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(OptionsModule.class);
  private static final Logger log
    = Logger.getLogger(OptionsModule.class.getName());

  // php/1a0q (phpMyAdmin)
  public static final String PHP_OS
    = System.getProperty("os.name").toUpperCase();

  public static final int ASSERT_ACTIVE = 1;
  public static final int ASSERT_CALLBACK = 2;
  public static final int ASSERT_BAIL = 3;
  public static final int ASSERT_WARNING = 4;
  public static final int ASSERT_QUIET_EVAL = 5;

  public static final int CREDITS_GROUP = 1;
  public static final int CREDITS_GENERAL = 2;
  public static final int CREDITS_SAPI = 4;
  public static final int CREDITS_MODULES = 8;
  public static final int CREDITS_DOCS = 16;
  public static final int CREDITS_FULLPAGE = 32;
  public static final int CREDITS_QA = 64;
  public static final int CREDITS_ALL = -1;

  public static final int INFO_GENERAL = 1;
  public static final int INFO_CREDITS = 2;
  public static final int INFO_CONFIGURATION = 4;
  public static final int INFO_MODULES = 8;
  public static final int INFO_ENVIRONMENT = 16;
  public static final int INFO_VARIABLES = 32;
  public static final int INFO_LICENSE = 64;
  public static final int INFO_ALL = -1;

  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }

  /**
   * Checks the assertion
   */
  @UsesSymbolTable(replace = false)
  @Name("assert")
  public static Value q_assert(Env env, String code)
  {
    try {
      QuercusContext quercus = env.getQuercus();

      QuercusProgram program = quercus.parseCode(code);

      program = program.createExprReturn();

      Value value = program.execute(env);

      if (value == null || ! value.toBoolean()) {
        env.warning(L.l("Assertion \"{0}\" failed", code));
        return NullValue.NULL;
      }

      return BooleanValue.TRUE;

    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Checks the assertion
   */
  public static Value assert_options(Env env,
                                     int code,
                                     @Optional("null") Value value)
  {
    Value result;

    switch (code) {
      case ASSERT_ACTIVE:
        result = INI_ASSERT_ACTIVE.getAsLongValue(env);
        if (!value.isNull()) INI_ASSERT_ACTIVE.set(env, value);
        break;
      case ASSERT_WARNING:
        result = INI_ASSERT_WARNING.getAsLongValue(env);
        if (!value.isNull()) INI_ASSERT_WARNING.set(env, value);
        break;
      case ASSERT_BAIL:
        result = INI_ASSERT_BAIL.getAsLongValue(env);
        if (!value.isNull()) INI_ASSERT_BAIL.set(env, value);
        break;
      case ASSERT_QUIET_EVAL:
        result = INI_ASSERT_QUIET_EVAL.getAsLongValue(env);
        if (!value.isNull()) INI_ASSERT_QUIET_EVAL.set(env, value);
        break;
      case ASSERT_CALLBACK:
        result = INI_ASSERT_CALLBACK.getValue(env);
        if (!value.isNull()) INI_ASSERT_CALLBACK.set(env, value);
        break;
      default:
        env.warning(L.l("unknown value {0}", code));
        result = BooleanValue.FALSE;
    }

    return result;
  }

  /**
   * Stubs the dl.
   */
  public static boolean dl(Env env, String dl)
  {
    env.stub("dl is stubbed for dl(" + dl + ")");

    return false;
  }

  /**
   * Returns true if the given extension is loaded
   */
  public static boolean extension_loaded(Env env, String ext)
  {
    return env.isExtensionLoaded(ext);
  }

  /**
   * Returns the configuration value of a configuration.
   */
  public static Value get_cfg_var(Env env, String name)
  {
    Value value = env.getConfigVar(name);

    if (!value.isNull())
      return value;
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the owner of the current script.
   */
  public static String get_current_user(Env env)
  {
    env.stub("get_current_user");

    return String.valueOf(env.getSelfPath().getOwner());
  }

  /**
   * Returns the constants as an array
   */
  public static Value get_defined_constants(Env env)
  {
    return env.getDefinedConstants();
  }

  /**
   * Returns extension function with a given name.
   */
  public static Value get_extension_funcs(Env env, String name)
  {
    return env.getExtensionFuncs(name);
  }

  /**
   * Returns the include path
   */
  public static Value get_include_path(Env env)
  {
    return QuercusContext.INI_INCLUDE_PATH.getAsStringValue(env);
  }

  /**
   * Returns an array of all the included path.
   */
  public static ArrayValue get_included_files(Env env)
  {
    return env.getIncludedFiles();
  }

  /**
   * Returns true if the given extension is loaded
   */
  public static Value get_loaded_extensions(Env env)
  {
    ArrayValue value = new ArrayValueImpl();

    for (String ext : env.getLoadedExtensions()) {
      value.put(ext);
    }

    return value;
  }

  /**
   * Gets the magic quotes value.
   */
  public static LongValue get_magic_quotes_gpc(Env env)
  {
    return (env.getIniBoolean("magic_quotes_gpc")
            ? LongValue.ONE
            : LongValue.ZERO);
  }

  /**
   * Gets the magic quotes runtime value.
   */
  public static Value get_magic_quotes_runtime(Env env)
  {
    return LongValue.ZERO; // PHP 6 removes, so we don't support
  }

  /**
   * Returns an array of all the included path.
   */
  public static ArrayValue get_required_files(Env env)
  {
    return get_included_files(env);
  }

  /**
   * Gets an environment value.
   */
  public static Value getenv(Env env, StringValue key)
  {
    Value serverVars = env.getGlobalVar("_SERVER");
    Value val = serverVars.get(key);

    if (val == null || ! val.isset())
      return BooleanValue.FALSE;

    return val;
  }

  /**
   * Returns the gid for the script path.
   */
  public static Value getlastmod(Env env)
  {
    return FileModule.filemtime(env, env.getSelfPath());
  }

  /**
   * Returns the gid for the script path.
   */
  public static Value getmygid(Env env)
  {
    return FileModule.filegroup(env, env.getSelfPath());
  }

  /**
   * Returns the inode for the script path.
   */
  public static Value getmyinode(Env env)
  {
    return FileModule.fileinode(env, env.getSelfPath());
  }

  /**
   * Returns the uid for the script path.
   */
  public static Value getmyuid(Env env)
  {
    return FileModule.fileowner(env, env.getSelfPath());
  }

  /**
   * Returns the thread for the script.
   */
  public static long getmypid(Env env)
  {
    return Thread.currentThread().getId();
  }

  // XXX: getopt

  /**
   * Stub value for getrusage.
   */
  public static Value getrusage(Env env, @Optional int who)
  {
    ArrayValue value = new ArrayValueImpl();

    value.put(env.createString("ru_inblock"),
              LongValue.create(0));
    value.put(env.createString("ru_outblock"),
              LongValue.create(0));
    value.put(env.createString("ru_msgsnd"),
              LongValue.create(0));
    value.put(env.createString("ru_msgrcv"),
              LongValue.create(0));
    value.put(env.createString("ru_maxrss"),
              LongValue.create(0));
    value.put(env.createString("ru_ixrss"),
              LongValue.create(0));
    value.put(env.createString("ru_idrss"),
              LongValue.create(0));
    value.put(env.createString("ru_minflt"),
              LongValue.create(0));
    value.put(env.createString("ru_majflt"),
              LongValue.create(0));
    value.put(env.createString("ru_nsignals"),
              LongValue.create(0));
    value.put(env.createString("ru_nvcsw"),
              LongValue.create(0));
    value.put(env.createString("ru_nswap"),
              LongValue.create(0));
    value.put(env.createString("ru_utime.tv_sec"), LongValue.create(0));
    value.put(env.createString("ru_utime.tv_usec"), LongValue.create(0));
    value.put(env.createString("ru_stime.tv_sec"), LongValue.create(0));
    value.put(env.createString("ru_stime.tv_usec"), LongValue.create(0));

    return value;
  }

  /**
   * Sets an initialization value.
   */
  public static Value ini_alter(Env env, String varName, StringValue value)
  {
    return ini_set(env, varName, value);
  }

  /**
   * Returns an initialization value.
   */
  public static StringValue ini_get(Env env, String varName)
  {
    StringValue v = env.getIni(varName);

    if (v != null)
      return v;
    else
      return env.getEmptyString();
  }

  /**
   * Returns all initialization values.
   * XXX: access levels dependent on PHP_INI, PHP_INI_PERDIR, PHP_INI_SYSTEM.
   *
   * @param extension assumes ini values are prefixed by extension names.
   */
  public static Value ini_get_all(Env env,
                                  @Optional() String extension)
  {
    if (extension.length() > 0) {
      if (! env.isExtensionLoaded(extension)) {
        env.warning(L.l("extension '" + extension + "' not loaded."));
        return BooleanValue.FALSE;
      }
      extension += ".";
    }

    return getAllDirectives(env, extension);
  }

  private static Value getAllDirectives(Env env, String prefix)
  {
    ArrayValue directives = new ArrayValueImpl();

    Value global = env.createString("global_value");
    Value local = env.createString("local_value");
    Value access = env.createString("access");

    IniDefinitions iniDefinitions = env.getQuercus().getIniDefinitions();

    TreeSet<String> names = new TreeSet<String>();

    names.addAll(iniDefinitions.getNames());

    for (String name : names) {
      if (name.startsWith(prefix)) {
        IniDefinition iniDefinition = iniDefinitions.get(name);

        // php/1a0n - do not add unless defined
        if (!iniDefinition.isRuntimeDefinition()) {

          ArrayValue inner = new ArrayValueImpl();

          inner.put(global, iniDefinition.getAsStringValue(env.getQuercus()));
          inner.put(local, iniDefinition.getAsStringValue(env));
          inner.put(access, LongValue.create(iniDefinition.getScope()));

          directives.put(env.createString(name), inner);
        }
      }
    }

    return directives;
  }

  /**
   * Restore the initial configuration value
   */
  public static Value ini_restore(Env env, String name)
  {
    Value value = env.getConfigVar(name);

    if (value != null)
      env.setIni(name, value.toStringValue());

    return NullValue.NULL;
  }

  /**
   * Sets an initialization value.
   */
  public static StringValue ini_set(Env env, String varName, StringValue value)
  {
    StringValue oldValue = env.setIni(varName, value);

    if (oldValue != null)
      return oldValue;
    else
      return StringValue.EMPTY;
  }

  /**
   * Gets the magic quotes value.
   */
  public static Value magic_quotes_runtime(Env env)
  {
    return BooleanValue.FALSE; // PHP 6 removes, so we don't support
  }

  /**
   * Stub value for memory get usage.
   */
  public static Value memory_get_peak_usage(Env env, @Optional boolean real)
  {
    return LongValue.create(Runtime.getRuntime().maxMemory());
  }

  /**
   * Stub value for memory get usage.
   */
  public static Value memory_get_usage(Env env, @Optional boolean real)
  {
    return LongValue.create(Runtime.getRuntime().maxMemory());
  }

  // XXX: php_ini_loaded_file
  // XXX: php_ini_scanned_files
  // XXX: php_logo_guid
  // XXX: phpcredits

  /**
   * Returns the sapi type.
   */
  public static String php_sapi_name(Env env)
  {
    return env.getQuercus().getSapiName();
  }

  /**
   * Returns system information
   */
  public static String php_uname(@Optional("'a'") String mode)
  {
    // XXX: stubbed

    if (mode == null || mode.equals(""))
      mode = "a";

    switch (mode.charAt(0)) {
    case 's':
      return System.getProperty("os.name");

    case 'n':
      try {
        InetAddress addr = InetAddress.getLocalHost();

        return addr.getHostName();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);

        return "localhost";
      }

    case 'r':
      return "2.4.0";

    case 'v':
      return "Version 2.6.24";

    case 'm':
      return "i686";

    case 'a':
    default:
      return (php_uname("s") + " "
              + php_uname("n") + " "
              + php_uname("r") + " "
              + php_uname("v") + " "
              + php_uname("m"));
    }
  }

  public static void phpinfo(Env env, @Optional("-1") int what)
  {
    if (hasRequest(env))
      env.println("<html><body>");

    if ((what & INFO_GENERAL) != 0)
      phpinfoGeneral(env);
    if ((what & INFO_VARIABLES) != 0)
      phpinfoVariables(env);

    if (hasRequest(env))
      env.println("</body></html>");
  }

  private static void phpinfoGeneral(Env env)
  {
    if (hasRequest(env))
      env.println("<h1>Quercus</h1>");
    else
      env.println("Quercus");

    if (hasRequest(env)) {
      env.println("<pre>");
    }

    env.println("PHP Version => " + phpversion(env, env.createString("std")));
    env.println("System => " + System.getProperty("os.name") + " "
              + System.getProperty("os.version") + " "
              + System.getProperty("os.arch"));
    env.println("Build Date => " + env.getQuercus().getVersionDate());
    env.println("Configure Command => n/a");
    env.println("Server API => CGI");
    env.println("Virtual Directory Support => disabled");

    env.println("Configuration File (php.ini) Path => "
                + env.getQuercus().getIniFile());

    env.println("PHP API => 20031224");
    env.println("PHP Extension => 20041030");
    env.println("Debug Build => no");
    env.println("Thread Safety => enabled");
    env.println("Registered PHP Streams => php, file, http, https");

    if (hasRequest(env)) {
      env.print("</pre>");
    }
  }

  private static void phpinfoVariables(Env env)
  {
    if (hasRequest(env)) {
      env.println("<h2>PHP Variables</h2>");
      env.println("<table>");
      env.println("<tr><th>Variable</th><th>Value</th></tr>");
    }
    else {
      env.println("Variable => Value");
    }

    if (hasRequest(env)) {
      phpinfoVariable(env, "_REQUEST", env.getGlobalVar("_REQUEST"));
      phpinfoVariable(env, "_GET", env.getGlobalVar("_GET"));
      phpinfoVariable(env, "_POST", env.getGlobalVar("_POST"));
    }

    phpinfoVariable(env, "_SERVER", env.getGlobalVar("_SERVER"));

    if (hasRequest(env))
      env.print("</table>");

    env.println();
  }

  private static void phpinfoVariable(Env env, String name, Value value)
  {
    if (value.isArray()) {
      ArrayValue array = value.toArrayValue(env);

      for (Map.Entry<Value,Value> entry : array.entrySet()) {
        Value key = escape(env, entry.getKey());

        if (hasRequest(env))
          env.print("<tr><td>");

        env.print(name + "[\"" + key + "\"]");

        if (hasRequest(env))
          env.println("</td><td>");
        else
          env.print(" => ");

        phpinfoVariable(env, entry.getValue());

        if (hasRequest(env))
          env.println("</td></tr>");
      }
    }
    else {
      if (hasRequest(env))
        env.println("<tr><td>" + name + "</td><td>");

      phpinfoVariable(env, value);

      if (hasRequest(env))
        env.println("</td></tr>");
    }
  }

  private static void phpinfoVariable(Env env, Value value)
  {
    if (value.isString()) {
      env.println(escape(env, value).toString());
    }
    else {
      if (hasRequest(env))
        env.print("<pre>");

      VariableModule.var_dump(env, escape(env, value), null);

      if (hasRequest(env))
        env.print("</pre>");
    }
  }

  /**
   * Returns the quercus version.
   */
  public static StringValue phpversion(Env env, @Optional StringValue module)
  {
    return env.getQuercus().getPhpVersionValue();
  }

  /**
   * Sets an environment name/value pair.
   */
  public static boolean putenv(Env env, StringValue settings)
  {
    int eqIndex = settings.indexOf('=');

    if (eqIndex < 0)
      return false;

    StringValue key = settings.substring(0, eqIndex);
    StringValue val = settings.substring(eqIndex + 1);

    env.getGlobalVar("_SERVER").put(key, val);

    return true;
  }

  /**
   * Sets the include path
   */
  public static Value restore_include_path(Env env)
  {
    env.restoreIncludePath();

    return NullValue.NULL;
  }

  /**
   * Sets the include path
   */
  public static String set_include_path(Env env, String includePath)
  {
    return env.setIncludePath(includePath);
  }

  /**
   * Sets the magic quotes value.
   */
  public static Value set_magic_quotes_runtime(Env env, Value value)
  {
    return BooleanValue.FALSE; // PHP 6 removes magic_quotes
  }

  /**
   * Sets the time limit and resets the timeout.
   */
  public static Value set_time_limit(Env env, long seconds)
  {
    env.setTimeLimit(seconds * 1000L);

    env.resetTimeout();

    return NullValue.NULL;
  }

  /*
   * Returns the directory used for temp files like uploads.
   */
  public static String sys_get_temp_dir(Env env)
  {
    Path tmp = env.getTempDirectory();

    return tmp.getNativePath() + Path.getFileSeparatorChar();
  }

  /**
   * Compares versions
   */
  public static Value version_compare(Env env,
                                      StringValue version1,
                                      StringValue version2,
                                      @Optional("cmp") String op)
  {
    ArrayList<Value> expanded1 = expandVersion(env, version1);
    ArrayList<Value> expanded2 = expandVersion(env, version2);

    int cmp = compareTo(expanded1, expanded2);

    if ("eq".equals(op) || "==".equals(op) || "=".equals(op))
      return cmp == 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("ne".equals(op) || "!=".equals(op) || "<>".equals(op))
      return cmp != 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("lt".equals(op) || "<".equals(op))
      return cmp < 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("le".equals(op) || "<=".equals(op))
      return cmp <= 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("gt".equals(op) || ">".equals(op))
      return cmp > 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("ge".equals(op) || ">=".equals(op))
      return cmp >= 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else {
      if (cmp == 0)
        return LongValue.ZERO;
      else if (cmp < 0)
        return LongValue.MINUS_ONE;
      else
        return LongValue.ONE;
    }
  }

  // XXX: zend_logo_guid
  // XXX: zend_thread_id

  public static String zend_version()
  {
    return "2.0.4";
  }
  
  /**
   * JVM takes care of circular reference collection.
   */
  public static boolean gc_enabled()
  {
    return true;
  }
  
  public static void gc_enable()
  {
  }
  
  public static void gc_disable()
  {
  }

  private static ArrayList<Value> expandVersion(Env env, StringValue version)
  {
    ArrayList<Value> expand = new ArrayList<Value>();

    int len = version.length();
    int i = 0;

    while (i < len) {
      char ch = version.charAt(i);

      if ('0' <= ch && ch <= '9') {
        int value = 0;

        for (; i < len && '0' <= (ch = version.charAt(i)) && ch <= '9'; i++) {
          value = 10 * value + ch - '0';
        }

        expand.add(LongValue.create(value));
      }
      else if (Character.isLetter((char) ch)) {
        StringBuilder sb = new StringBuilder();

        for (; i < len && Character.isLetter(version.charAt(i)); i++) {
          sb.append((char) ch);
        }

        String s = sb.toString();

        if (s.equals("dev"))
          s = "a";
        else if (s.equals("alpha") || s.equals("a"))
          s = "b";
        else if (s.equals("beta") || s.equals("b"))
          s = "c";
        else if (s.equals("RC"))
          s = "d";
        else if (s.equals("pl"))
          s = "e";
        else
          s = "z" + s;

        expand.add(env.createString(s));
      }
      else
        i++;
    }

    return expand;
  }

  private static boolean hasRequest(Env env)
  {
    return env.getRequest() != null;
  }

  private static Value escape(Env env, Value value)
  {
    if (value.isArray()) {
      ArrayValue array = value.toArrayValue(env);

      ArrayValue result = new ArrayValueImpl();

      for (Map.Entry<Value,Value> entry : array.entrySet()) {
        Value key = escape(env, entry.getKey());
        Value val = escape(env, entry.getValue());

        result.put(key, val);
      }

      return result;
    }
    else if (value.isObject()) {
      ObjectValue obj = (ObjectValue)value.toObject(env);

      ObjectValue result = new ObjectExtValue(obj.getQuercusClass());

      for (Map.Entry<Value,Value> entry : obj.entrySet()) {
        Value key = escape(env, entry.getKey());
        Value val = escape(env, entry.getValue());

        result.putField(env, key.toString(), val);
      }

      return result;
    }
    else {
      return HtmlModule.htmlspecialchars(env,
                                         value.toStringValue(),
                                         HtmlModule.ENT_COMPAT,
                                         null);
    }
  }

  private static int compareTo(ArrayList<Value> a, ArrayList<Value> b)
  {
    int i = 0;

    while (true) {
      if (a.size() <= i && b.size() <= i)
        return 0;
      else if (a.size() <= i)
        return -1;
      else if (b.size() <= i)
        return 1;

      int cmp = compareTo(a.get(i), b.get(i));

      if (cmp != 0)
        return cmp;

      i++;
    }
  }

  private static int compareTo(Value a, Value b)
  {
    if (a.equals(b))
      return 0;
    else if (a.isLongConvertible() && ! b.isLongConvertible())
      return -1;
    else if (b.isLongConvertible() && ! a.isLongConvertible())
      return 1;
    else if (a.lt(b))
      return -1;
    else
      return 1;
  }

  static final IniDefinition INI_ASSERT_ACTIVE
    = _iniDefinitions.add("assert.active", true, PHP_INI_ALL);
  static final IniDefinition INI_ASSERT_BAIL
    = _iniDefinitions.add("assert.bail", false, PHP_INI_ALL);
  static final IniDefinition INI_ASSERT_WARNING
    = _iniDefinitions.add("assert.warning", true, PHP_INI_ALL);
  static final IniDefinition INI_ASSERT_CALLBACK
    = _iniDefinitions.add("assert.callback", null, PHP_INI_ALL);
  static final IniDefinition INI_ASSERT_QUIET_EVAL
    = _iniDefinitions.add("assert.quiet_eval", false, PHP_INI_ALL);
  static final IniDefinition INI_ENABLE_DL
    = _iniDefinitions.add("enable_dl", true, PHP_INI_SYSTEM);
  static final IniDefinition INI_MAX_EXECUTION_TIME
    = _iniDefinitions.add("max_execution_time", "600", PHP_INI_ALL);
  static final IniDefinition INI_MAX_INPUT_TIME
    = _iniDefinitions.add("max_input_time", "-1", PHP_INI_PERDIR);
  static final IniDefinition INI_MAGIC_QUOTES_GPC
    = _iniDefinitions.add("magic_quotes_gpc", false, PHP_INI_PERDIR);

  static final IniDefinition INI_TRACK_VARS
    = _iniDefinitions.add("track_vars", "On", PHP_INI_ALL);
  static final IniDefinition INI_ARG_SEPARATOR_OUTPUT
    = _iniDefinitions.add("arg_separator.output", "&", PHP_INI_ALL);
  static final IniDefinition INI_ARG_SEPARATOR_INPUT
    = _iniDefinitions.add("arg_separator.input", "&", PHP_INI_ALL);
  static final IniDefinition INI_VARIABLES_ORDER
    = _iniDefinitions.add("variables_order", "EGPCS", PHP_INI_ALL);
  static final IniDefinition INI_AUTO_GLOBALS_JIT
    = _iniDefinitions.add("auto_globals_jit", "1", PHP_INI_ALL);
  static final IniDefinition INI_REGISTER_ARGC_ARGV
    = _iniDefinitions.add("register_argc_argv", false, PHP_INI_ALL);
  static final IniDefinition INI_POST_MAX_SIZE
    = _iniDefinitions.add("post_max_size", "8M", PHP_INI_ALL);
  static final IniDefinition INI_GPC_ORDER
    = _iniDefinitions.add("gpc_order", "GPC", PHP_INI_ALL);
  static final IniDefinition INI_AUTO_PREPEND_FILE
    = _iniDefinitions.add("auto_prepend_file", null, PHP_INI_ALL);
  static final IniDefinition INI_AUTO_APPEND_FILE
    = _iniDefinitions.add("auto_append_file", null, PHP_INI_ALL);
  static final IniDefinition INI_DEFAULT_MIMETYPE
    = _iniDefinitions.add("default_mimetype", "text/html", PHP_INI_ALL);
  static final IniDefinition INI_DEFAULT_CHARSET
    = _iniDefinitions.add("default_charset", "", PHP_INI_ALL);
  static final IniDefinition INI_ALWAYS_POPULATE_RAW_POST_DATA =
    _iniDefinitions.add("always_populate_raw_post_data", false, PHP_INI_ALL);
  static final IniDefinition INI_ALLOW_WEBDAV_METHODS
    = _iniDefinitions.add("allow_webdav_methods", false, PHP_INI_ALL);
  static final IniDefinition INI_MEMORY_LIMIT
    = _iniDefinitions.add("memory_limit", "512M", PHP_INI_ALL);

  // unsupported
  static final IniDefinition MAGIC_QUOTES_RUNTIME
    = _iniDefinitions.addUnsupported(
      "magic_quotes_runtime", false, PHP_INI_ALL);
  static final IniDefinition MAGIC_QUOTES_SYBASE
    = _iniDefinitions.addUnsupported("magic_quotes_sybase", false, PHP_INI_ALL);
  static final IniDefinition INI_REGISTER_GLOBALS
    = _iniDefinitions.addUnsupported("register_globals", false, PHP_INI_ALL);
}

