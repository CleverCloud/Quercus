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

package com.caucho.quercus.lib.pdf;

import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * pdf object oriented API facade
 */
public class PDFWriter {
  private static final Logger log
    = Logger.getLogger(PDFWriter.class.getName());
  private static final L10N L = new L10N(PDFWriter.class);

  private long _offset;
  private WriteStream _out;

  private int _objectId = 1;

  private int _lastObject;

  private String _creator = "Quercus PDF";
  private String _author;
  private String _title;

  private ArrayList<PDFObject> _pendingObjects
    = new ArrayList<PDFObject>();

  private ArrayList<ObjectDef> _xref = new ArrayList<ObjectDef>();

  PDFWriter(WriteStream out)
  {
    _out = out;
  }

  public void setCreator(String creator)
  {
    _creator = creator;
  }

  public void setAuthor(String author)
  {
    _author = author;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void beginDocument()
    throws IOException
  {
    println("%PDF-1.4");
    println("#\u00c0\u00c3\u00c4\u00c9");
  }

  public void writeCatalog(int catalogId, int pagesId,
                           ArrayList<Integer> pagesList, int pageCount)
    throws IOException
  {
    int pageId = pagesId;

    if (pagesList.size() > 0)
      pageId = allocateId(1);

    beginObject(catalogId);

    println("  << /Type /Catalog");
    println("     /Pages " + pageId + " 0 R");
    println("  >>");

    endObject();

    if (pagesList.size() > 0) {
      beginObject(pageId);

      println("  << /Type /Pages");
      print("     /Kids [");

      for (int i = 0; i < pagesList.size(); i++) {
        if (i != 0)
          print(" ");

        print(pagesList.get(i) + " 0 R");
      }

      println("]");
      println("     /Count " + pageCount);
      println("  >>");

      endObject();
    }
  }

  public void writePageGroup(int id, ArrayList<PDFPage> pages)
    throws IOException
  {
    beginObject(id);
    println("  << /Type /Pages");
    print("     /Kids [");

    for (int i = 0; i < pages.size(); i++) {
      if (i != 0)
        print(" ");

      print(pages.get(i).getId() + " 0 R");
    }

    println("]");
    println("     /Count " + pages.size());
    println("  >>");
    endObject();

    for (int i = 0; i < pages.size(); i++) {
      pages.get(i).write(this);
    }
  }

  public void writeStream(int id, PDFStream stream)
    throws IOException
  {
    stream.flush();
    int length = stream.getLength();

    beginObject(id);
    println(" << /Length " + length + " >>");

    println("stream");
    stream.writeToStream(_out);
    _offset += length;
    println();
    println("endstream");
    endObject();
  }

  public void endDocument()
    throws IOException
  {
    long xrefOffset = _offset;

    println("xref");
    println("0 " + (_xref.size() + 1) + "");
    println("0000000000 65535 f");

    for (int i = 0; i < _xref.size(); i++)
      _xref.get(i).write();

    println("trailer");
    println(" << /Size " + (_xref.size() + 1));
    println("    /Root 1 0 R");
    println(" >>");
    println("startxref");
    println(xrefOffset);
    println("%%EOF");
  }

  public int allocateId(int count)
  {
    int id = _objectId;

    _objectId += count;

    return id;
  }

  public void addPendingObject(PDFObject obj)
    throws IOException
  {
    if (_lastObject + 1 == obj.getId()) {
      beginObject(obj.getId());
      obj.writeObject(this);
      endObject();
    }
    else
      _pendingObjects.add(obj);
  }

  public void beginObject(int id)
    throws IOException
  {
    while (_xref.size() < id)
      _xref.add(null);

    _xref.set(id - 1, new ObjectDef(id, _offset));

    println(id + " 0 obj");
  }

  public void endObject()
    throws IOException
  {
    println("endobj");

    _lastObject++;

    for (int i = _pendingObjects.size() - 1; i >= 0; i--) {
      PDFObject obj = _pendingObjects.get(i);

      if (_lastObject + 1 == obj.getId()) {
        _pendingObjects.remove(i);

        beginObject(obj.getId());
        obj.writeObject(this);
        endObject();
        break;
      }
    }
  }

  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    _out.write(buffer, offset, length);

    _offset += length;
  }

  public void print(String s)
    throws IOException
  {
    _out.print(s);

    _offset += s.length();
  }

  public void println(String s)
    throws IOException
  {
    _out.println(s);

    _offset += s.length() + 1;
  }

  public void println()
    throws IOException
  {
    _out.println();

    _offset += 1;
  }

  public void print(long v)
    throws IOException
  {
    println(String.valueOf(v));
  }

  public void println(long v)
    throws IOException
  {
    println(String.valueOf(v));
  }

  public void print(double v)
    throws IOException
  {
    if ((long) v == v)
      print(String.valueOf((long) v));
    else
      print(String.valueOf(v));
  }

  public void println(double v)
    throws IOException
  {
    if ((long) v == v)
      println(String.valueOf((long) v));
    else
      println(String.valueOf(v));
  }

  public String toString()
  {
    return "PDF[]";
  }

  class ObjectDef {
    private int _id;
    private long _offset;

    ObjectDef(int id, long offset)
    {
      _id = id;
      _offset = offset;
    }

    void write()
      throws IOException
    {
      _out.print(_offset / 1000000000L % 10);
      _out.print(_offset / 100000000L % 10);
      _out.print(_offset / 10000000L % 10);
      _out.print(_offset / 1000000L % 10);
      _out.print(_offset / 100000L % 10);

      _out.print(_offset / 10000L % 10);
      _out.print(_offset / 1000L % 10);
      _out.print(_offset / 100L % 10);
      _out.print(_offset / 10L % 10);
      _out.print(_offset % 10);

      _out.print(' ');
      _out.println("00000 n");

      _offset += 19;
    }
  }
}
