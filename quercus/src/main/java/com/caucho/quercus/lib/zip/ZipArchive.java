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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Kevin Decherf <kdecherf@gmail.com>
 */
package com.caucho.quercus.lib.zip;

import com.caucho.quercus.env.EnvCleanup;
import com.caucho.util.L10N;

import java.util.logging.Logger;

public class ZipArchive implements EnvCleanup {

   private static final Logger log = Logger.getLogger(ZipArchive.class.getName());
   private static final L10N L = new L10N(ZipArchive.class);
   public static final int CREATE = 1;
   public static final int EXCL = 2;
   public static final int CHECKCONS = 4;
   public static final int OVERWRITE = 8;
   public static final int FL_NOCASE = 1;
   public static final int FL_NODIR = 2;
   public static final int FL_COMPRESSED = 4;
   public static final int FL_UNCHANGED = 8;
   public static final int FL_RECOMPRESS = 16;
   public static final int CM_DEFAULT = -1;
   public static final int CM_STORE = 0;
   public static final int CM_SHRINK = 1;
   public static final int CM_REDUCE_1 = 2;
   public static final int CM_REDUCE_2 = 3;
   public static final int CM_REDUCE_3 = 4;
   public static final int CM_REDUCE_4 = 5;
   public static final int CM_IMPLODE = 6;
   public static final int CM_DEFLATE = 8;
   public static final int CM_DEFLATE64 = 9;
   public static final int CM_PKWARE_IMPLODE = 10;
   public static final int CM_BZIP2 = 12;
   public static final int ER_OK = 0;
   public static final int ER_MULTIDISK = 1;
   public static final int ER_RENAME = 2;
   public static final int ER_CLOSE = 3;
   public static final int ER_SEEK = 4;
   public static final int ER_READ = 5;
   public static final int ER_WRITE = 6;
   public static final int ER_CRC = 7;
   public static final int ER_ZIPCLOSED = 8;
   public static final int ER_NOENT = 9;
   public static final int ER_EXISTS = 10;
   public static final int ER_OPEN = 11;
   public static final int ER_TMPOPEN = 12;
   public static final int ER_ZLIB = 13;
   public static final int ER_MEMORY = 14;
   public static final int ER_CHANGED = 15;
   public static final int ER_COMPNOTSUPP = 16;
   public static final int ER_EOF = 17;
   public static final int ER_INVAL = 18;
   public static final int ER_NOZIP = 19;
   public static final int ER_INTERNAL = 20;
   public static final int ER_INCONS = 21;
   public static final int ER_REMOVE = 22;
   public static final int ER_DELETED = 23;

   @Override
   public void cleanup() throws Exception {
      throw new UnsupportedOperationException("Not supported yet.");
   }
}
