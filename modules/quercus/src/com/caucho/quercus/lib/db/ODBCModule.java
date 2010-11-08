/*
 * Copyright (c) 2010 Clever Cloud -- all rights reserved
 *
 * This file is part of Quercus(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Quercus Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Quercus Open Source is distributed in the hope that it will be useful,
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
 * @author Kevin Decherf
 */
package com.caucho.quercus.lib.db;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quercus ODBC ROutine
 */
public class ODBCModule extends AbstractQuercusModule {

    private static final Logger log = Log.open(ODBCModule.class);
    private static final L10N L = new L10N(ODBCModule.class);
    public static final String ODBC_TYPE = "jdbcODBC";
    public static final int ODBC_BINMODE_PASSTHRU = 0x0;
    public static final int ODBC_BINMODE_RETURN = 0x1;
    public static final int ODBC_BINMODE_CONVERT = 0x2;

    public ODBCModule() {
    }

    /**
     * Returns true for the mysql extension.
     */
    public String[] getLoadedExtensions() {
	return new String[]{"odbc"};
    }
}
