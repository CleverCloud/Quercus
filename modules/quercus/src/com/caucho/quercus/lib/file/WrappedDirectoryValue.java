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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.file;

import com.caucho.quercus.env.*;

/**
 * Represents a PHP directory listing
 */
public class WrappedDirectoryValue extends DirectoryValue {
  private static final ConstStringValue DIR_CLOSEDIR
    = new ConstStringValue("dir_closedir");
  private static final ConstStringValue DIR_OPENDIR
    = new ConstStringValue("dir_opendir");
  private static final ConstStringValue DIR_READDIR
    = new ConstStringValue("dir_readdir");
  private static final ConstStringValue DIR_REWINDDIR
    = new ConstStringValue("dir_rewinddir");
  
  private static final UnicodeBuilderValue DIR_CLOSEDIR_U
    = new UnicodeBuilderValue("dir_closedir");
  private static final UnicodeBuilderValue DIR_OPENDIR_U
    = new UnicodeBuilderValue("dir_opendir");
  private static final UnicodeBuilderValue DIR_READDIR_U
    = new UnicodeBuilderValue("dir_readdir");
  private static final UnicodeBuilderValue DIR_REWINDDIR_U
    = new UnicodeBuilderValue("dir_rewinddir");
  
  private Env _env;
  private Value _wrapper;

  public WrappedDirectoryValue(Env env, QuercusClass qClass)
  {
    super(env);
    
    _env = env;
    _wrapper = qClass.callNew(_env, Value.NULL_ARGS);
  }

  public boolean opendir(StringValue path, LongValue flags)
  {
    if (_env.isUnicodeSemantics())
      return _wrapper.callMethod(_env, DIR_OPENDIR_U, path, flags).toBoolean();
    else
      return _wrapper.callMethod(_env, DIR_OPENDIR, path, flags).toBoolean();
  }

  /**
   * Returns the next value.
   */
  public Value readdir()
  {
    if (_env.isUnicodeSemantics())
      return _wrapper.callMethod(_env, DIR_READDIR_U);
    else
      return _wrapper.callMethod(_env, DIR_READDIR);
  }

  /**
   * Rewinds the directory
   */
  public void rewinddir()
  {
    if (_env.isUnicodeSemantics())
      _wrapper.callMethod(_env, DIR_REWINDDIR_U);
    else
      _wrapper.callMethod(_env, DIR_REWINDDIR);
  }

  /**
   * Closes the directory
   */
  public void close()
  {
    if (_env.isUnicodeSemantics())
      _wrapper.callMethod(_env, DIR_CLOSEDIR_U);
    else
      _wrapper.callMethod(_env, DIR_CLOSEDIR);
  }

  /**
   * Converts to a string.
   * @param env
   */
  public String toString()
  {
    return "Directory[]";
  }
}

