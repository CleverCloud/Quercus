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

package com.caucho.db.blob;

import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
import com.caucho.db.xa.RawTransaction;
import com.caucho.db.xa.StoreTransaction;
import com.caucho.util.L10N;
import com.caucho.util.FreeList;
import com.caucho.vfs.OutputStreamWithBuffer;
import com.caucho.vfs.TempCharBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the indexes for a BLOB or CLOB.
 *
 * The inode contains 16 long values
 * <pre>
 *  0) length of the saved file
 *  1-14) direct fragment addresses (to 224k)
 *  15) pointer to the indirect block
 * </pre>
 *
 * <h3>Inline storage (120)</h3>
 *
 * If the length of the blob is less than 120, the blob is stored directly
 * in the inode.
 *
 * <h3>mini fragment storage (3840)</h3>
 *
 * If the length of the blob is less than 3840, the blob is stored
 * in mini-fragments of size 256 pointed by the inode's addresses.
 *
 * The maximum wasted space for mini-fragment storage is 255 bytes.
 *
 * <h3>direct block storage</h3>
 *
 * The first 14 block pointers (224k)
 *
 * <h3>indirect storage</h3>
 *
 * The indirect block (a 16k block) itself is divided into sections:
 * <pre>
 *  0-1023) single indirect fragment addresses (16M, 2^24)
 *  1024-1535) double indirect block addresses (16G, 2^34)
 *  1536-2047) triple indirect block addresses (to 8T, 2^43)
 * </pre>
 */
public class Inode {
  private static final L10N L = new L10N(Inode.class);
  private static final Logger log
    = Logger.getLogger(Inode.class.getName());

  public static final int INODE_SIZE = 128;
  public static final int INLINE_BLOB_SIZE = INODE_SIZE - 8;
  public static final int BLOCK_SIZE = BlockStore.BLOCK_SIZE;

  public static final int MINI_FRAG_SIZE = BlockStore.MINI_FRAG_SIZE;

  public static final int MINI_FRAG_BLOB_SIZE
    = (INLINE_BLOB_SIZE / 8) * MINI_FRAG_SIZE;

  public static final int INDIRECT_BLOCKS = BLOCK_SIZE / 8;

  // direct addresses are stored in the inode itself (112k of data).
  public static final int DIRECT_BLOCKS = 14;
  // single indirect addresses are stored in the indirect block (16M data)
  public static final int SINGLE_INDIRECT_BLOCKS = INDIRECT_BLOCKS / 2;
  // double indirect addresses (2^34 = 16G data)
  public static final int DOUBLE_INDIRECT_BLOCKS = INDIRECT_BLOCKS / 4;
  // triple indirect addresses (2^43 = 8T data)
  public static final int TRIPLE_INDIRECT_BLOCKS = INDIRECT_BLOCKS / 4;

  // size cutoffs for various inline modes
  public static final long INLINE_MAX = INLINE_BLOB_SIZE;

  public static final int MINI_FRAG_MAX
    = (INLINE_BLOB_SIZE / 8) * MINI_FRAG_SIZE;

  public static final long DIRECT_MAX
    = BLOCK_SIZE * DIRECT_BLOCKS;

  public static final long SINGLE_INDIRECT_MAX
    = DIRECT_MAX + SINGLE_INDIRECT_BLOCKS * BLOCK_SIZE;

  public static final long DOUBLE_INDIRECT_MAX
    = (SINGLE_INDIRECT_MAX
       + DOUBLE_INDIRECT_BLOCKS * (BLOCK_SIZE / 8L) * BLOCK_SIZE);

  private static final FreeList<byte[]> _freeBytes = new FreeList<byte[]>(16);

  private BlockStore _store;
  private final byte []_bytes = new byte[INODE_SIZE];

  public Inode()
  {
  }

  public Inode(BlockStore store, StoreTransaction xa)
  {
    _store = store;
  }

  public Inode(BlockStore store)
  {
    this(store, RawTransaction.create());
  }

  /**
   * Returns the backing store.
   */
  public BlockStore getStore()
  {
    return _store;
  }

  /**
   * Returns the buffer.
   */
  public byte []getBuffer()
  {
    return _bytes;
  }

  /**
   * Returns the length.
   */
  public long getLength()
  {
    return readLong(_bytes, 0);
  }

  public void init(BlockStore store, StoreTransaction xa,
                   byte []buffer, int offset)
  {
    _store = store;
    System.arraycopy(buffer, offset, _bytes, 0, _bytes.length);
  }

  /**
   * Opens a read stream to the inode.
   */
  public InputStream openInputStream()
  {
    return new BlobInputStream(this);
  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToStream(OutputStreamWithBuffer os)
    throws IOException
  {
    writeToStream(os, 0, Long.MAX_VALUE / 2);
  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToStream(OutputStreamWithBuffer os,
                            long offset, long length)
    throws IOException
  {
    byte []buffer = os.getBuffer();
    int writeLength = buffer.length;
    int writeOffset = os.getBufferOffset();

    while (length > 0) {
      int sublen = writeLength - writeOffset;

      if (sublen == 0) {
        buffer = os.nextBuffer(writeOffset);
        writeOffset = os.getBufferOffset();
        sublen = writeLength - writeOffset;
      }

      if (length < sublen)
        sublen = (int) length;

      int len = read(_bytes, 0, _store,
                     offset,
                     buffer, writeOffset, sublen);

      if (len <= 0)
        break;

      writeOffset += len;
      offset += len;
      length -= len;
    }

    os.setBufferOffset(writeOffset);
  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToWriter(Writer writer)
    throws IOException
  {
    TempCharBuffer tempBuffer = TempCharBuffer.allocate();

    char []buffer = tempBuffer.getBuffer();
    int writeLength = buffer.length;
    long offset = 0;

    while (true) {
      int sublen = writeLength;

      int len = read(_bytes, 0, _store,
                     offset,
                     buffer, 0, sublen);

      if (len <= 0)
        break;

      writer.write(buffer, 0, len);

      offset += 2 * len;
    }

    TempCharBuffer.free(tempBuffer);
  }

  /**
   * Reads into a buffer.
   *
   * @param inode the inode buffer
   * @param inodeOffset the offset of the inode data in the buffer
   * @param store the owning store
   * @param fileOffset the offset into the file to read
   * @param buffer the buffer receiving the data
   * @param bufferOffset the offset into the receiving buffer
   * @param bufferLength the maximum number of bytes to read
   *
   * @return the number of bytes read
   */
  static int read(byte []inode, int inodeOffset,
                  BlockStore store,
                  long fileOffset,
                  byte []buffer, int bufferOffset, int bufferLength)
    throws IOException
  {
    long fileLength = readLong(inode, inodeOffset);

    int sublen = bufferLength;
    if (fileLength - fileOffset < sublen)
      sublen = (int) (fileLength - fileOffset);

    if (sublen <= 0)
      return -1;

    if (fileLength <= INLINE_MAX) {
      System.arraycopy(inode, inodeOffset + 8 + (int) fileOffset,
                       buffer, bufferOffset, sublen);

      return sublen;
    }
    else if (fileLength <= MINI_FRAG_MAX) {
      long fragAddr = readMiniFragAddr(inode, inodeOffset, store, fileOffset);
      int fragOffset = (int) (fileOffset % MINI_FRAG_SIZE);

      if (MINI_FRAG_SIZE - fragOffset < sublen)
        sublen = MINI_FRAG_SIZE - fragOffset;

      store.readMiniFragment(fragAddr, fragOffset,
                             buffer, bufferOffset, sublen);

      return sublen;
    }
    else {
      long addr = readBlockAddr(inode, inodeOffset, store, fileOffset);
      int offset = (int) (fileOffset % BLOCK_SIZE);

      if (BLOCK_SIZE - offset < sublen)
        sublen = BLOCK_SIZE - offset;

      store.readBlock(addr, offset, buffer, bufferOffset, sublen);

      return sublen;
    }
  }

  /**
   * Updates the buffer.  Called only from the blob classes.
   */
  static void append(byte []inode, int inodeOffset,
                     BlockStore store, StoreTransaction xa,
                     byte []buffer, int offset, int length)
    throws IOException
  {
    long currentLength = readLong(inode, inodeOffset);
    long newLength = currentLength + length;

    writeLong(inode, inodeOffset, newLength);

    if (newLength <= INLINE_MAX) {
      assert(currentLength == 0);
      
      System.arraycopy(buffer, offset,
                       inode, (int) (inodeOffset + 8 + currentLength),
                       length);
    }
    else if (newLength <= MINI_FRAG_MAX) {
      assert(currentLength == 0);
      
      while (length > 0) {
        int sublen = length;

        if (MINI_FRAG_SIZE < sublen)
          sublen = MINI_FRAG_SIZE;

        long miniFragAddr = store.allocateMiniFragment();

        if (miniFragAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("{0} illegal mini fragment",
                                              store));
        }

        writeMiniFragAddr(inode, inodeOffset,
                          store, xa,
                          currentLength, miniFragAddr);

        Block writeBlock = store.writeMiniFragment(miniFragAddr, 0,
                                                   buffer, offset, sublen);
        xa.addUpdateBlock(writeBlock);

        offset += sublen;
        length -= sublen;
        currentLength += sublen;
      }
    }
    else {
      if (currentLength > 0 && currentLength < MINI_FRAG_MAX)
        throw new IllegalStateException(L.l("illegal length transition {0} to {1} because mini-fragmentation must be decided initially.",
                                            currentLength, newLength));

      appendBlock(inode, inodeOffset, store, xa,
                  buffer, offset, length, currentLength);
    }
  }

  private static void appendBlock(byte []inode, int inodeOffset,
                                  BlockStore store, StoreTransaction xa,
                                  byte []buffer, int offset, int length,
                                  long currentLength)
    throws IOException
  {
    while (length > 0) {
      if (currentLength % BLOCK_SIZE != 0) {
        long addr = readBlockAddr(inode, inodeOffset,
                                  store,
                                  currentLength);

        if (addr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("{0}: inode: illegal block at {1}",
                                              store, currentLength));
        }

        int blockOffset = (int) (currentLength % BLOCK_SIZE);
        int sublen = length;

        if (BLOCK_SIZE - blockOffset < sublen)
          sublen = BLOCK_SIZE - blockOffset;

        Block block = store.writeBlock(addr, blockOffset,
                                       buffer, offset, sublen);
        xa.addUpdateBlock(block);

        offset += sublen;
        length -= sublen;

        currentLength += sublen;
      }
      else {
        int sublen = length;

        if (BLOCK_SIZE < sublen)
          sublen = BLOCK_SIZE;

        Block block = store.allocateBlock();

        long blockAddr = BlockStore.blockIdToAddress(block.getBlockId());

        block.free();

        if (blockAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("{0}: illegal block",
                                              store));
        }

        writeBlockAddr(inode, inodeOffset,
                       store, xa,
                       currentLength, blockAddr);

        Block writeBlock = store.writeBlock(blockAddr, 0,
                                            buffer, offset, sublen);
        xa.addUpdateBlock(writeBlock);

        offset += sublen;
        length -= sublen;

        currentLength += sublen;
      }
    }
  }

  /**
   * Reads into a buffer.
   *
   * @param inode the inode buffer
   * @param inodeOffset the offset of the inode data in the buffer
   * @param store the owning store
   * @param fileOffset the offset into the file to read
   * @param buffer the buffer receiving the data
   * @param bufferOffset the offset into the receiving buffer
   * @param bufferLength the maximum number of chars to read
   *
   * @return the number of characters read
   */
  static int read(byte []inode, int inodeOffset, BlockStore store,
                  long fileOffset,
                  char []buffer, int bufferOffset, int bufferLength)
    throws IOException
  {
    long fileLength = readLong(inode, inodeOffset);

    int charSublen = (int) (fileLength - fileOffset) / 2;
    if (bufferLength < charSublen)
      charSublen = bufferLength;

    if (charSublen <= 0)
      return -1;

    if (fileLength <= INLINE_MAX) {
      int baseOffset = inodeOffset + 8 + (int) fileOffset;

      for (int i = 0; i < charSublen; i++) {
        char ch = (char) (((inode[baseOffset] & 0xff) << 8)
                          + ((inode[baseOffset + 1] & 0xff)));

        buffer[bufferOffset + i] = ch;

        baseOffset += 2;
      }

      return charSublen;
    }
    else if (fileLength <= MINI_FRAG_MAX) {
      long fragAddr = readMiniFragAddr(inode, inodeOffset, store, fileOffset);
      int fragOffset = (int) (fileOffset % Inode.MINI_FRAG_SIZE);

      if (MINI_FRAG_SIZE - fragOffset < 2 * charSublen)
        charSublen = (MINI_FRAG_SIZE - fragOffset) / 2;

      store.readMiniFragment(fragAddr, fragOffset,
                             buffer, bufferOffset, charSublen);

      return charSublen;
    }
    else {
      long addr = readBlockAddr(inode, inodeOffset, store, fileOffset);
      int offset = (int) (fileOffset % BLOCK_SIZE);

      if (BLOCK_SIZE - offset < 2 * charSublen)
        charSublen = (BLOCK_SIZE - offset) / 2;

      store.readBlock(addr, offset, buffer, bufferOffset, charSublen);

      return charSublen;
    }
  }

  /**
   * Updates the buffer.  Called only from the clob classes.
   */
  static void append(byte []inode, int inodeOffset,
                     BlockStore store, StoreTransaction xa,
                     char []buffer, int offset, int charLength)
    throws IOException
  {
    long currentLength = readLong(inode, inodeOffset);
    long newLength = currentLength + 2 * charLength;

    writeLong(inode, inodeOffset, newLength);

    if (newLength <= INLINE_BLOB_SIZE) {
      assert(currentLength == 0);
      
      int writeOffset = (int) (inodeOffset + 8 + currentLength);

      for (int i = 0; i < charLength; i++) {
        char ch = buffer[offset + i];

        inode[writeOffset++] = (byte) (ch >> 8);
        inode[writeOffset++] = (byte) (ch);
      }
    }
    else if (newLength <= MINI_FRAG_MAX) {
      assert(currentLength == 0);
      
      while (charLength > 0) {
        int sublen = 2 * charLength;

        if (MINI_FRAG_SIZE < sublen)
          sublen = MINI_FRAG_SIZE;

        long miniFragAddr = store.allocateMiniFragment();

        if (miniFragAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("{0} illegal mini fragment",
                                              store));
        }

        writeMiniFragAddr(inode, inodeOffset,
                          store, xa,
                          currentLength, miniFragAddr);

        int charSublen = sublen / 2;

        // XXX: store in XA?
        Block writeBlock = store.writeMiniFragment(miniFragAddr, 0,
                                                   buffer, offset, charSublen);
        xa.addUpdateBlock(writeBlock);

        offset += charSublen;
        charLength -= charSublen;
        currentLength += sublen;
      }
    }
    else {
      if (currentLength > 0 && currentLength < MINI_FRAG_MAX)
        throw new IllegalStateException(L.l("illegal length transition {0} to {1} because mini-fragmentation must be decided initially.",
                                            currentLength, newLength));

      appendBlock(inode, inodeOffset, store, xa,
                  buffer, offset, charLength, currentLength);
    }
  }

  static void appendBlock(byte []inode, int inodeOffset,
                          BlockStore store, StoreTransaction xa,
                          char []buffer, int offset, int charLength,
                          long currentLength)
    throws IOException
  {
    // XXX: theoretically deal with case of appending to inline, although
    // the blobs are the only writers and will avoid that case.

    while (charLength > 0) {
      if (currentLength % BLOCK_SIZE != 0) {
        long addr = readBlockAddr(inode, inodeOffset,
                                  store,
                                  currentLength);

        if (addr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(store + " inode: illegal block at " + currentLength);
        }

        int blockOffset = (int) (currentLength % BLOCK_SIZE);
        int sublen = 2 * charLength;

        if (BLOCK_SIZE - blockOffset < sublen)
          sublen = BLOCK_SIZE - blockOffset;

        int charSublen = sublen / 2;

        Block writeBlock = store.writeBlock(addr, blockOffset,
                                            buffer, offset, charSublen);
        xa.addUpdateBlock(writeBlock);

        offset += charSublen;
        charLength -= charSublen;

        currentLength += sublen;
      }
      else {
        int sublen = 2 * charLength;

        if (BLOCK_SIZE < sublen)
          sublen = BLOCK_SIZE;

        int charSublen = sublen / 2;

        Block block = store.allocateBlock();
        long blockAddr = block.getBlockId();
        block.free();

        Block writeBlock = store.writeBlock(blockAddr, 0,
                                            buffer, offset, charSublen);
        
        xa.addUpdateBlock(writeBlock);

        writeBlockAddr(inode, inodeOffset,
                       store, xa,
                       currentLength, blockAddr);

        offset += charSublen;
        charLength -= charSublen;

        currentLength += sublen;
      }
    }
  }

  /**
   * Opens a byte output stream to the inode.
   */
  public OutputStream openOutputStream()
  {
    return new BlobOutputStream(this);
  }

  /**
   * Closes the output stream.
   */
  void closeOutputStream()
  {
    try {
      _store.saveAllocation();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Opens a char reader to the inode.
   */
  public Reader openReader()
  {
    return new ClobReader(this);
  }

  /**
   * Opens a char writer to the inode.
   */
  public Writer openWriter()
  {
    return new ClobWriter(this);
  }

  /**
   * Deletes the inode
   */
  public void remove()
  {
    byte []bytes = _freeBytes.allocate();

    if (bytes == null)
      bytes = new byte[INODE_SIZE];
    
    for (int i = 0; i < INODE_SIZE; i++) {
      bytes[i] = _bytes[i];
      _bytes[i] = 0;
    }
    
    long length = readLong(bytes, 0);

    try {
      if (length <= INLINE_BLOB_SIZE || bytes == null)
        return;
      else if (length <= MINI_FRAG_BLOB_SIZE) {
        for (; length > 0; length -= MINI_FRAG_SIZE) {
          long fragAddr = readMiniFragAddr(bytes, 0, _store, length - 1);

          if ((fragAddr & BlockStore.BLOCK_MASK) == 0) {
            _store.setCorrupted(true);

            String msg = _store + ": inode block " + Long.toHexString(length) + " has 0 fragment";

            throw stateError(msg);
          }
          else if (fragAddr < 0) {
            String msg = _store + ": inode block " + Long.toHexString(length) + " has invalid fragment " + Long.toHexString(fragAddr);

            _store.setCorrupted(true);

            throw stateError(msg);
          }

          _store.deleteMiniFragment(fragAddr);
        }
      }
      else {
        long indAddr = readLong(bytes, (DIRECT_BLOCKS + 1) * 8);

        for (; length > 0; length -= BLOCK_SIZE) {
          int blockCount = (int) ((length - 1) / BLOCK_SIZE);
          long blockAddr = readBlockAddr(bytes, 0, _store, length - 1);

          if (! validateBlockAddr(blockAddr, length)) {
            continue;
          }

          _store.freeBlock(blockAddr);

          int dblBlockCount
            = (blockCount - DIRECT_BLOCKS - SINGLE_INDIRECT_BLOCKS);

          // remove the double indirect blocks
          if (dblBlockCount >= 0
              && dblBlockCount % INDIRECT_BLOCKS == 0) {
            int dblIndex = (int) 8 * (dblBlockCount / INDIRECT_BLOCKS
                                      + SINGLE_INDIRECT_BLOCKS);

            blockAddr = _store.readBlockLong(indAddr, dblIndex);

            if (! validateBlockAddr(blockAddr, length)) {
              continue;
            }
              
            _store.freeBlock(blockAddr);
          }

          // remove the indirect block
          if (blockCount == DIRECT_BLOCKS) {
            _store.freeBlock(indAddr);
          }
        }
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _freeBytes.free(bytes);

      // XXX: saved by caller
      /*
      try {
        _store.saveAllocation();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
      */
    }
  }

  private boolean validateBlockAddr(long blockAddr, long length)
  {
    if ((blockAddr & BlockStore.BLOCK_MASK) == 0) {
      String msg = _store + ": inode block " + Long.toHexString(length) + " has 0 block";
      log.warning(msg);
      _store.setCorrupted(true);

      return false;
    }
    else if (blockAddr < 0) {
      String msg = _store + ": inode block " + Long.toHexString(length) + " has invalid block " + Long.toHexString(blockAddr);

      log.warning(msg);
      _store.setCorrupted(true);

      return false;
    }
    else
      return true;
  }

  /**
   * Clears the inode.
   */
  static void clear(byte []inode, int inodeOffset)
  {
    int end = inodeOffset + INODE_SIZE;

    for (; inodeOffset < end; inodeOffset++)
      inode[inodeOffset] = 0;
  }

  /**
   * Returns the fragment id for the given offset.
   */
  static long readMiniFragAddr(byte []inode, int inodeOffset,
                                   BlockStore store, long fileOffset)
    throws IOException
  {
    long fragCount = fileOffset / MINI_FRAG_SIZE;

    return readLong(inode, (int) (inodeOffset + 8 + 8 * fragCount));
  }

  /**
   * Writes the block id into the inode.
   */
  private static void writeMiniFragAddr(byte []inode, int offset,
                                        BlockStore store, StoreTransaction xa,
                                        long fragLength, long fragAddr)
    throws IOException
  {
    int fragCount = (int) (fragLength / MINI_FRAG_SIZE);

    if ((fragAddr & BlockStore.BLOCK_MASK) == 0) {
      store.setCorrupted(true);

      throw new IllegalStateException(store + ": inode block " + fragLength + " has zero value " + fragAddr);
    }

    writeLong(inode, offset + (fragCount + 1) * 8, fragAddr);
  }

  /**
   * Returns the fragment id for the given offset.
   */
  static long readBlockAddr(byte []inode, int inodeOffset,
                            BlockStore store,
                            long fileOffset)
    throws IOException
  {
    int blockCount = (int) (fileOffset / BLOCK_SIZE);
    
    if (fileOffset < DIRECT_MAX)
      return readLong(inode, inodeOffset + (blockCount + 1) * 8);
    else {
      long indAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
        store.setCorrupted(true);

        throw new IllegalStateException(L.l("{0} null block id", store));
      }

      if (fileOffset < SINGLE_INDIRECT_MAX) {
        int blockOffset = 8 * (blockCount - DIRECT_BLOCKS);

        long blockAddr = store.readBlockLong(indAddr, blockOffset);

        return blockAddr;
      }
      else if (fileOffset < DOUBLE_INDIRECT_MAX) {
        blockCount = blockCount - DIRECT_BLOCKS - SINGLE_INDIRECT_BLOCKS;

        int dblBlockCount = blockCount / INDIRECT_BLOCKS;

        int dblBlockIndex = 8 * (SINGLE_INDIRECT_BLOCKS + dblBlockCount);

        long dblIndAddr = store.readBlockLong(indAddr, dblBlockIndex);

        if (dblIndAddr == 0) {
          store.setCorrupted(true);

          throw new IllegalStateException(L.l("null indirect block id"));
        }

        int blockOffset = 8 * (blockCount % INDIRECT_BLOCKS);

        return store.readBlockLong(dblIndAddr, blockOffset);
      }
      else {
        store.setCorrupted(true);

        throw new IllegalStateException(L.l("{0} size over {1}M not supported",
                                            store,
                                            (DOUBLE_INDIRECT_MAX / (1024 * 1024))));
      }
    }
  }

  /**
   * Writes the block id into the inode.
   */
  private static void writeBlockAddr(byte []inode, int inodeOffset,
                                     BlockStore store, StoreTransaction xa,
                                     long fileOffset, long blockAddr)
    throws IOException
  {
    int blockCount = (int) (fileOffset / BLOCK_SIZE);

    // XXX: not sure if correct, needs XA?
    if ((blockAddr & BlockStore.BLOCK_MASK) == 0) {
      store.setCorrupted(true);

      String msg = store + ": inode block " + blockCount + " writing 0 fragment";
      throw stateError(msg);
    }

    if (fileOffset < DIRECT_MAX) {
      writeLong(inode, inodeOffset + 8 * (blockCount + 1), blockAddr);
    }
    else if (fileOffset < SINGLE_INDIRECT_MAX) {
      long indAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
        Block block = store.allocateBlock();
        indAddr = block.getBlockId();
        block.free();

        writeLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8, indAddr);
      }

      int blockOffset = 8 * (blockCount - DIRECT_BLOCKS);

      Block writeBlock = store.writeBlockLong(indAddr, blockOffset, blockAddr);
      xa.addUpdateBlock(writeBlock);
    }
    else if (fileOffset < DOUBLE_INDIRECT_MAX) {
      long indAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
        store.setCorrupted(true);

        throw new IllegalStateException(L.l("{0} null block id", store));
      }
      
      blockCount = blockCount - DIRECT_BLOCKS - SINGLE_INDIRECT_BLOCKS;
        
      int dblBlockCount = blockCount / INDIRECT_BLOCKS;

      int dblBlockIndex = 8 * (SINGLE_INDIRECT_BLOCKS + dblBlockCount);

      long dblIndAddr = store.readBlockLong(indAddr, dblBlockIndex);

      if (dblIndAddr == 0) {
        Block block = store.allocateBlock();

        dblIndAddr = BlockStore.blockIdToAddress(block.getBlockId());

        block.free();

        Block writeBlock = store.writeBlockLong(indAddr, dblBlockIndex, dblIndAddr);
        xa.addUpdateBlock(writeBlock);
      }

      int blockOffset = 8 * (blockCount % INDIRECT_BLOCKS);

      Block writeBlock = store.writeBlockLong(dblIndAddr, blockOffset, blockAddr);
      xa.addUpdateBlock(writeBlock);
    }
    else {
      store.setCorrupted(true);

      throw new IllegalStateException(L.l("{0} size over {1}M not supported",
                                          store,
                                          (DOUBLE_INDIRECT_MAX / (1024 * 1024))));
    }
  }

  /**
   * Reads the long.
   */
  public static long readLong(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xffL) << 56)
            + ((buffer[offset + 1] & 0xffL) << 48)
            + ((buffer[offset + 2] & 0xffL) << 40)
            + ((buffer[offset + 3] & 0xffL) << 32)
            + ((buffer[offset + 4] & 0xffL) << 24)
            + ((buffer[offset + 5] & 0xffL) << 16)
            + ((buffer[offset + 6] & 0xffL) << 8)
            + ((buffer[offset + 7] & 0xffL)));
  }

  /**
   * Writes the long.
   */
  public static void writeLong(byte []buffer, int offset, long v)
  {
    buffer[offset + 0] = (byte) (v >> 56);
    buffer[offset + 1] = (byte) (v >> 48);
    buffer[offset + 2] = (byte) (v >> 40);
    buffer[offset + 3] = (byte) (v >> 32);

    buffer[offset + 4] = (byte) (v >> 24);
    buffer[offset + 5] = (byte) (v >> 16);
    buffer[offset + 6] = (byte) (v >> 8);
    buffer[offset + 7] = (byte) (v);
  }

  private static IllegalStateException stateError(String msg)
  {
    IllegalStateException e = new IllegalStateException(msg);
    e.fillInStackTrace();
    log.log(Level.WARNING, e.toString(), e);
    return e;
  }
}
