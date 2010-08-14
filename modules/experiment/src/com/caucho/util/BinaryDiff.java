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

public class BinaryDiff {
  int _oldLength;
  ArrayList<TempBuffer> _oldBuffers;
  byte[][] _oldBytes;

  int _chunkSize = 1;
  int _chunkCount;

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
    _chunkCount = _oldLength / _chunkSize;

    _suffixArray = new int[_chunkCount];

    int chunkSize = _chunkSize;
    int chunkCount = _chunkCount;
    int []suffixArray = _suffixArray;

    for (int i = 0; i < chunkCount; i++) {
      suffixArray[i] = i * chunkSize;
    }

    byte [][]data = _oldBytes;
    
    sort(suffixArray, _oldBytes, TempBuffer.SIZE, _chunkSize);

    int []charMin = _charMin = new int[256];
    int []charMax = _charMax = new int[256];

    for (int i = 0; i < suffixArray.length; i++) {
      int offset = suffixArray[i];
      
      int ch = data[offset / TempBuffer.SIZE][offset % TempBuffer.SIZE] & 0xff;

      if (charMin[ch] == 0)
        charMin[ch] = i;

      charMax[ch] = i + 1;
    }
  }

  private static void sort(int []suffixArray, byte [][]data,
                           int bufLength, int chunkSize)
  {
    int length = suffixArray.length;
    int dataLength = length * chunkSize;

    for (int i = 0; i < suffixArray.length - 1; i++) {
      for (int j = suffixArray.length - 2; i <= j; j--) {
        int a = suffixArray[j];
        int b = suffixArray[j + 1];

        if (suffixCompareTo(a, b, data, dataLength, bufLength) > 0) {
          int temp = suffixArray[j];
          suffixArray[j] = suffixArray[j + 1];
          suffixArray[j + 1] = temp;
        }
      }
    }
  }

  private static int suffixCompareTo(int a, int b, byte [][]data,
                                     int length, int bufLength)
  {
    int sublen = length - a;

    if (length - b < sublen)
      sublen = length - b;

    for (int i = 0; i < sublen; i++) {
      int a1 = data[a / bufLength][a % bufLength];
      int b1 = data[b / bufLength][b % bufLength];

      if (a1 < b1)
        return -1;
      else if (b1 < a1)
        return 1;

      a++;
      b++;
    }

    return 0;
  }

  private void processNewFile(OutputStream out, InputStream newIn)
    throws IOException
  {
    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buffer = tempBuf.getBuffer();;

    int length = readAll(newIn, buffer);

    int prevOffset = 0;
    int offset = 0;

    int []charMin = _charMin;
    int []charMax = _charMax;
    int minLength = 8;

    byte [][]data = _oldBytes;
    int []suffixArray = _suffixArray;

    loop:
    for (; offset + minLength < length; offset += 1) {
      int ch = buffer[offset] & 0xff;

      int min = charMin[ch];
      int max = charMax[ch];

      byte ch2 = buffer[offset + 1];

      for (int i = min; i < max; i++) {
        int suffixOffset = suffixArray[i];
        int dataOffset = suffixOffset + 1;

        byte d1 = data[suffixOffset / TempBuffer.SIZE][suffixOffset % TempBuffer.SIZE];
        byte d2 = data[dataOffset / TempBuffer.SIZE][dataOffset % TempBuffer.SIZE];
        if (d2 == ch2) {
          dataOffset++;
          int bufOffset = offset + 2;

          match_loop:
          while (bufOffset < length && dataOffset < _oldLength) {
            byte bufCh = buffer[bufOffset];
            byte dataCh = data[dataOffset / TempBuffer.SIZE][dataOffset % TempBuffer.SIZE];

            if (bufCh == dataCh) {
              bufOffset += 1;
              dataOffset += 1;
            }
            else if (bufCh < dataCh) {
              if (dataOffset - suffixOffset >= minLength) {
                if (prevOffset < offset)
                  add(out, buffer, prevOffset, offset - prevOffset);

                offset = bufOffset;
                copy(out, suffixOffset, dataOffset - suffixOffset);
                prevOffset = offset;
                continue loop;
              }
              else
                break;
            }
            else {
              for (i++; i < max; i++) {
                int newSuffix = suffixArray[i];
                int newOffset = newSuffix + (dataOffset - suffixOffset);

                boolean isMatch = true;
                for (int j = newOffset; newSuffix <= j; j--) {
                  dataCh = data[j / TempBuffer.SIZE][j % TempBuffer.SIZE];
                  bufCh = buffer[offset + (j - newSuffix)];

                  if (bufCh != dataCh) {
                    isMatch = false;
                    break;
                  }
                }

                if (isMatch) {
                  dataOffset = newOffset;
                  suffixOffset = newSuffix;
                  continue match_loop;
                }
              }

              if (dataOffset - suffixOffset >= minLength) {
                if (prevOffset < offset)
                  add(out, buffer, prevOffset, offset - prevOffset);

                offset = bufOffset;
                prevOffset = offset;
                copy(out, suffixOffset, dataOffset - suffixOffset);
                continue loop;
              }
              else
                break;
            }
          }

          if (dataOffset - suffixOffset >= minLength) {
            if (prevOffset < offset)
              add(out, buffer, prevOffset, offset - prevOffset);

            offset = bufOffset;
            prevOffset = offset;
            copy(out, suffixOffset, dataOffset - suffixOffset);
          }

          break;
        }
        else if (ch2 < d2)
          break;
      }
    }

    if (prevOffset < length)
      add(out, buffer, prevOffset, length - prevOffset);

    TempBuffer.free(tempBuf);
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
}


