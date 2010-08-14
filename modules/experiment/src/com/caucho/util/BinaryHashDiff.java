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
 * @author Scott Ferguson
 */

package com.caucho.util;

import com.caucho.vfs.*;

import java.io.*;
import java.util.*;

public class BinaryHashDiff {
  //private static long PRIME = (1L << 61) - 1;
  // private static long PRIME = (1L << 31) - 1;

  // Prime table at http://primes.utm.edu/lists/2small/0bit.html
  // 2^54 to avoid long overflow when multiplying byte data and MUL
  private static long PRIME = (1L << 54) - 33;
  private static long MUL = 251;
  
  int _oldLength;
  ArrayList<TempBuffer> _oldBuffers;
  byte[][] _oldBytes;

  int _copyMin = 8;
  int _chunkSize = 4;
  int _chunkCount;

  long _hashFactor;
  
  long []_hashArray;
  int []_suffixArray;

  int []_charMin;
  int []_charMax;

  public void delta(OutputStream out, InputStream oldFile, InputStream newFile)
    throws IOException
  {
    readBuffers(oldFile);

    processOrigFile();

    processNewFile(out, newFile);
  }

  protected void add(OutputStream out, byte []buffer, int offset, int length)
    throws IOException
  {
    System.out.println("ADD(" + length + "): '" + new String(buffer, offset, length) + "'");
  }

  protected void copy(OutputStream out, int offset, int length)
    throws IOException
  {
    System.out.println("COPY(" + offset + "," + length + ")");
  }

  private void readBuffers(InputStream oldFile)
    throws IOException
  {
    _oldBuffers = new ArrayList<TempBuffer>();
    _oldLength = 0;

    while (true) {
      TempBuffer buf = TempBuffer.allocate();
      byte []buffer = buf.getBuffer();

      int sublen = readAll(oldFile, buffer);

      if (sublen < 0) {
        TempBuffer.free(buf);
        break;
      }

      _oldLength += sublen;
      _oldBuffers.add(buf);

      if (sublen < buffer.length)
        break;
    }

    _oldBytes = new byte[_oldBuffers.size()][];
    for (int i = 0; i < _oldBytes.length; i++) {
      _oldBytes[i] = _oldBuffers.get(i).getBuffer();
    }
  }

  private void processOrigFile()
  {
    long factor = 1;

    for (int i = 1; i < _chunkSize; i++) {
      factor = (MUL * factor) % PRIME;
    }

    _hashFactor = factor;
      
    _chunkCount = _oldLength / _chunkSize;

    _hashArray = new long[_chunkCount];
    _suffixArray = new int[_chunkCount];

    int chunkSize = _chunkSize;
    int chunkCount = _chunkCount;
    
    long []hashArray = _hashArray;
    int []suffixArray = _suffixArray;

    byte [][]data = _oldBytes;

    for (int i = 0; i < chunkCount; i++) {
      int offset = i * chunkSize;
      
      suffixArray[i] = i;

      byte []buffer = data[offset / TempBuffer.SIZE];

      offset = offset % TempBuffer.SIZE;

      long hash = 0;
      for (int k = 0; k < chunkSize; k++) {
        hash = hash(hash, buffer[offset + k], 0, factor);
      }

      hashArray[i] = hash;
    }
    
    sort(suffixArray, hashArray);
  }

  private static void sort(int []suffixArray, long []hashArray)
  {
    int length = suffixArray.length;
    int dataLength = length;

    sort(suffixArray, hashArray, dataLength, 0, length);
  }

  private static void sort(int []suffixArray, long []hashArray,
                           int dataLength, int min, int max)
  {
    int delta = max - min;
    
    if (delta < 2) {
    }
    else if (delta == 2) {
      int aIndex = suffixArray[min];
      int bIndex = suffixArray[min + 1];

      long aValue = hashArray[aIndex];
      long bValue = hashArray[bIndex];

      if (bValue < aValue) {
        suffixArray[min] = bIndex;
        suffixArray[min + 1] = aIndex;
      }
    }
    else {
      int pivotIndex = suffixArray[min];
      long pivotValue = hashArray[pivotIndex];
      int pivotMax = max;
      
      int pivot = min;

      while (pivot + 1 < pivotMax) {
        long value = hashArray[suffixArray[pivot + 1]];

        if (value < pivotValue) {
          suffixArray[pivot] = suffixArray[pivot + 1];
          suffixArray[pivot + 1] = pivotIndex;

          pivot += 1;
        }
        else {
          int temp = suffixArray[pivotMax - 1];
          suffixArray[pivotMax - 1] = suffixArray[pivot + 1];
          suffixArray[pivot + 1] = temp;

          pivotMax -= 1;
        }
      }

      if (min < pivot) {
        sort(suffixArray, hashArray, dataLength, min, pivot);
        sort(suffixArray, hashArray, dataLength, pivot, max);
      }
      else {
        sort(suffixArray, hashArray, dataLength, pivot + 1, max);
      }
    }
  }

  private static int suffixCompareTo(int a, int b, long []hashArray,
                                     int length)
  {
    int sublen = length - a;

    if (length - b < sublen)
      sublen = length - b;

    for (int i = 0; i < sublen; i++) {
      long a1 = hashArray[a + i];
      long b1 = hashArray[b + i];

      if (a1 < b1)
        return -1;
      else if (b1 < a1)
        return 1;
    }

    return 0;
  }

  private void processNewFile(OutputStream out, InputStream newIn)
    throws IOException
  {
    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buffer = tempBuf.getBuffer();;

    int length = readAll(newIn, buffer);

    if (length < _chunkSize)
      return;

    int prevOffset = 0;
    int offset = 0;

    int chunkSize = _chunkSize;

    byte [][]data = _oldBytes;
    int []suffixArray = _suffixArray;
    
    long []hashArray = _hashArray;
    long hashFactor = _hashFactor;

    long hash = 0;

    for (int i = 0; i < chunkSize - 1; i++) {
      hash = hash(hash, buffer[i], 0, hashFactor);
    }

    loop:
    for (; offset + chunkSize < length; offset += 1) {
      byte oldValue = 0;

      if (offset > 0)
        oldValue = buffer[offset - 1];
      
      hash = hash(hash, buffer[offset + chunkSize - 1], oldValue, hashFactor);

      int suffixIndex = findBlock(hash, suffixArray, hashArray);

      if (suffixIndex < 0)
        continue;

      int suffixOffset = suffixArray[suffixIndex] + 1;
      int dataOffset = offset + chunkSize;
      long prevHash = hash;

      loop_match:
      for (;
           dataOffset + chunkSize < length
             && suffixOffset < hashArray.length;
           dataOffset += chunkSize, suffixOffset += 1) {
        long hash2 = 0;

        for (int i = 0; i < chunkSize; i++) {
          hash2 = hash(hash2, buffer[dataOffset + i], 0, hashFactor);
        }

        if (hash2 == hashArray[suffixOffset]) {
        }
        else if (hash2 < hashArray[suffixOffset]) {
          int delta = suffixOffset - suffixArray[suffixIndex];

          for (int i = suffixIndex - 1;
               i >= 0
                 && hashArray[suffixArray[i]] == hash
                 && suffixArray[i] + delta < hashArray.length
                 && hashArray[suffixArray[i] + delta - 1] == prevHash;
               i--) {
            if (hashArray[suffixArray[i] + delta] == hash2) {
              suffixIndex = i;
              suffixOffset = suffixArray[i] + delta;
              prevHash = hash2;

              // XXX: also need to revalidate in between
              continue loop_match;
            }
          }

          break;
        }
        else {
          int delta = suffixOffset - suffixArray[suffixIndex];

          for (int i = suffixIndex + 1;
               i < suffixArray.length
                 && hashArray[suffixArray[i]] == hash
                 && suffixArray[i] + delta < hashArray.length
                 && hashArray[suffixArray[i] + delta - 1] == prevHash;
               i++) {
            if (hashArray[suffixArray[i] + delta] == hash2) {
              suffixIndex = i;
              suffixOffset = suffixArray[i] + delta;
              prevHash = hash2;
              // XXX: also need to revalidate in between

              continue loop_match;
            }
          }

          break;
        }

        prevHash = hash2;
      }

      if (dataOffset - offset >= _copyMin) {
        if (prevOffset < offset)
          add(out, buffer, prevOffset, offset - prevOffset);

        copy(out,
             suffixArray[suffixIndex] * _chunkSize,
             dataOffset - offset);

        hash = 0;
        offset = dataOffset - 1;
        for (int i = 0; i < _chunkSize; i++)
          hash = hash(hash, buffer[offset + i], 0, hashFactor);

        prevOffset = offset + 1;
      }
    }

    if (prevOffset < length)
      add(out, buffer, prevOffset, length - prevOffset);

    TempBuffer.free(tempBuf);
  }

  private int findBlock(long hash, int []suffixArray, long []hashArray)
  {
    int min = 0;
    int max = suffixArray.length;

    while (min < max) {
      int pivot = (min + max) / 2;

      long hashValue = hashArray[suffixArray[pivot]];

      if (hash == hashValue)
        return pivot;
      else if (hash < hashValue)
        max = pivot;
      else
        min = pivot + 1;
    }

    return -1;
  }

  private int readAll(InputStream is, byte []buffer)
    throws IOException
  {
    int offset = 0;

    while (offset < buffer.length) {
      int sublen = buffer.length - offset;

      sublen = is.read(buffer, offset, sublen);

      if (sublen < 0)
        return offset > 0 ? offset : -1;

      offset += sublen;
    }

    return buffer.length;
  }

  public void testHash()
  {
    Random random = new Random();

    byte []data = new byte[8192];
    
    for (int k = 0; k < 256 * 1024 * 1024; k++) {
      int size = random.nextInt(256);

      if (size < 1)
        size = 1;
    
      long factor = 1;

      for (int i = 1; i < size; i++) {
        factor = (MUL * factor) % PRIME;
      }

      byte d0 = (byte) random.nextInt();

      for (int i = 0; i < size; i++) {
        data[i] = (byte) random.nextInt();
      }

      long hash = 0;
      hash = hash(hash, d0, 0, factor);
      for (int i = 0; i < size - 1; i++) {
        hash = hash(hash, data[i], 0, factor);
      }
      hash = hash(hash, data[size - 1], d0, factor);

      long oldHash = hash;

      hash = 0;
      for (int i = 0; i < size; i++) {
        hash = hash(hash, data[i], 0, factor);
      }
      
      long newHash = hash;

      if (oldHash != newHash)
        System.out.println("OLD: " + oldHash + " " + newHash);
    }
  }
  
  // rabin-karp hash (PRIME = (1L << 54) - 33, MUL = 13);
  private static long hash(long hash, int newData, int oldData, long factor)
  {
    oldData = oldData & 0xff;
    newData = newData & 0xff;
    
    long old = ((PRIME << 8) + hash - factor * oldData) % PRIME;

    return (MUL * old + newData) % PRIME;
  }
    
}


