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

package com.caucho.db.debug;

import com.caucho.db.block.BlockStore;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Manager for a basic Java-based database.
 */
public class DebugStore {
  private static final Logger log = Log.open(DebugStore.class);
  private static final L10N L = new L10N(DebugStore.class);

  Path _path;
  BlockStore _store;
  
  public DebugStore(Path path)
    throws Exception
  {
    _path = path;

    _store = BlockStore.create(path);
  }

  public static void main(String []args)
    throws Exception
  {
    if (args.length == 0) {
      System.out.println("usage: DebugStore store.db");
      return;
    }

    Path path = Vfs.lookup(args[0]);

    WriteStream out = Vfs.openWrite(System.out);
    
    new DebugStore(path).test(out);

    out.close();
  }
  
  public void test(WriteStream out)
    throws Exception
  {
    out.println("file-size   : " + _store.getFileSize());
    out.println("block-count : " + _store.getBlockCount());

    debugAllocation(out, _store.getAllocationTable(), _store.getBlockCount());
    
    debugFragments(out, _store.getAllocationTable(), _store.getBlockCount());
  }

  private void debugAllocation(WriteStream out, byte []allocTable, long count)
    throws IOException
  {
    out.println();

    for (int i = 0; i < 2 * count; i += 2) {
      int v = allocTable[i];

      int code = v & 0xf;

      switch (code) {
      case BlockStore.ALLOC_FREE:
        out.print('.');
        break;
      case BlockStore.ALLOC_ROW:
        out.print('r');
        break;
      case BlockStore.ALLOC_USED:
        out.print('u');
        break;
        /*
      case BlockStore.ALLOC_FRAGMENT:
        out.print('f');
        break;
        */
      case BlockStore.ALLOC_MINI_FRAG:
        out.print('m');
        break;
      case BlockStore.ALLOC_INDEX:
        out.print('i');
        break;
      default:
        out.print('?');
      }
      
      if (i % 64 == 63)
        out.println();
      else if (i % 8 == 7)
        out.print(' ');
    }
    
    out.println();
  }

  private void debugFragments(WriteStream out, byte []allocTable, long count)
    throws Exception
  {
    long totalUsed = 0;
    
    byte []block = new byte[BlockStore.BLOCK_SIZE];
    
    for (int i = 0; i < 2 * count; i += 2) {
      int code = allocTable[i];

      /*
      if (code == BlockStore.ALLOC_FRAGMENT) {
        int fragCount = 0;

        for (int j = 0; j < 8; j++) {
          if ((allocTable[i + 1] & (1 << j)) != 0)
            fragCount++;
        }

        totalUsed += fragCount;

        out.println();

        out.print("Fragment Block " + (i / 2) + ": ");
        for (int j = 0; j < 8; j++) {
          if ((allocTable[i + 1] & (1 << j)) != 0)
            out.print("1");
          else
            out.print(".");
        }
      }
      */
    }

    out.println();
    out.println("Total-used: " + totalUsed);
  }

  private void readBlock(byte []block, long count)
    throws Exception
  {
    ReadStream is = _path.openRead();

    try {
      is.skip(count * BlockStore.BLOCK_SIZE);

      is.read(block, 0, block.length);
    } finally {
      is.close();
    }
  }

  private int readShort(byte []block, int offset)
  {
    return ((block[offset] & 0xff) << 8) + (block[offset + 1] & 0xff);
  }
}
