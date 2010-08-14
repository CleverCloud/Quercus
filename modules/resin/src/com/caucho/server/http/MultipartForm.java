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

package com.caucho.server.http;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.HashMapImpl;
import com.caucho.util.L10N;
import com.caucho.vfs.MultipartStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;

import javax.servlet.http.Part;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.HashMap;

/**
 * Multipart form handling.
 */
class MultipartForm {
  private static final Logger log
    = Logger.getLogger(MultipartForm.class.getName());
  static final L10N L = new L10N(MultipartForm.class);
  
  static void parsePostData(HashMapImpl<String,String[]> table,
                            List<Part> parts,
                            ReadStream rawIs, String boundary,
                            HttpServletRequestImpl request,
                            String javaEncoding,
                            long uploadMax,
                            long fileUploadMax)
    throws IOException
  {
    MultipartStream ms = new MultipartStream(rawIs, boundary);
    ms.setEncoding(javaEncoding);
    ReadStream is;

    while ((is = ms.openRead()) != null) {
      String attr = (String) ms.getAttribute("content-disposition");

      if (attr == null || ! attr.startsWith("form-data")) {
        // XXX: is this an error?
        continue;
      }

      String name = getAttribute(attr, "name");
      String filename = getAttribute(attr, "filename");

      if (name == null) {
        // XXX: is this an error?
        continue;
      }
      else if (filename != null) {
        String contentType = (String) ms.getAttribute("content-type");

        Path tempDir = CauchoSystem.getWorkPath().lookup("form");
        try {
          tempDir.mkdirs();
        } catch (IOException e) {
        }
        Path tempFile = tempDir.createTempFile("form", ".tmp");
        request.addCloseOnExit(tempFile);

        WriteStream os = tempFile.openWrite();

        TempBuffer tempBuffer = TempBuffer.allocate();
        byte []buf = tempBuffer.getBuffer();

        int totalLength = 0;

        try {
          int len;

          while ((len = is.read(buf, 0, buf.length)) > 0) {
            os.write(buf, 0, len);
            totalLength += len;
          }
        } finally {
          os.close();

          TempBuffer.free(tempBuffer);
          tempBuffer = null;
        }

        if (uploadMax > 0 && uploadMax < tempFile.getLength()) {
          String msg = L.l("multipart form data '{0}' too large",
                           "" + tempFile.getLength());
          request.setAttribute("caucho.multipart.form.error", msg);
          request.setAttribute("caucho.multipart.form.error.size",
                               new Long(tempFile.getLength()));
          
          tempFile.remove();
          
          throw new IOException(msg);
        } else if (fileUploadMax > 0 && fileUploadMax < tempFile.getLength()){
          String msg = L.l("multipart form data part '{0}':'{1}' is greater then the accepted value of '{2}'",
                           name, "" + tempFile.getLength(), fileUploadMax);

          tempFile.remove();

          throw new IllegalStateException(msg);
        }
        else if (tempFile.getLength() != totalLength) {
          String msg = L.l("multipart form upload failed (possibly due to full disk).");

          request.setAttribute("caucho.multipart.form.error", msg);
          request.setAttribute("caucho.multipart.form.error.size",
                               new Long(tempFile.getLength()));
          
          tempFile.remove();
          
          throw new IOException(msg);
        }

        // server/136u, server/136v, #2578
        if (table.get(name + ".filename") == null) {
          table.put(name, new String[] { tempFile.getNativePath() });
          table.put(name + ".file", new String[] { tempFile.getNativePath() });
          table.put(name + ".filename", new String[] { filename });
          table.put(name + ".content-type", new String[] { contentType });
        }
        else {
          addTable(table, name, tempFile.getNativePath());
          addTable(table, name + ".file", tempFile.getNativePath());
          addTable(table, name + ".filename", filename);
          addTable(table, name + ".content-type", contentType);
        }

      if (log.isLoggable(Level.FINE))
          log.fine("mp-file: " + name + "(filename:" + filename + ")");
      } else {
        CharBuffer value = new CharBuffer();
        int ch;

        for (ch = is.readChar(); ch >= 0; ch = is.readChar())
          value.append((char) ch);
      
        if (log.isLoggable(Level.FINE))
          log.fine("mp-form: " + name + "=" + value);

        addTable(table, name, value.toString());
      }

      parts.add(request.createPart(name,
                                   new HashMap<String, List<String>>(ms.getHeaders())));
    }

    if (! ms.isComplete()) {
      throw new IOException("Incomplete form");
    }
  }

  private static void addTable(HashMapImpl<String,String[]> table, String name, String value)
  {
    String []oldArray = table.get(name);

    if (oldArray != null) {
      String []newArray = new String[oldArray.length + 1];
      System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
      newArray[oldArray.length] = value;
      table.put(name, newArray);
    }
    else
      table.put(name, new String[] {value});
  }

  private static String getAttribute(String attr, String name)
  {
    if (attr == null)
      return null;
    
    int length = attr.length();
    int i = attr.indexOf(name);
    if (i < 0)
      return null;

    for (i += name.length(); i < length && attr.charAt(i) != '='; i++) {
    }
    
    for (i++; i < length && attr.charAt(i) == ' '; i++) {
    }

    CharBuffer value = CharBuffer.allocate();
    if (i < length && attr.charAt(i) == '\'') {
      for (i++; i < length && attr.charAt(i) != '\''; i++)
        value.append(attr.charAt(i));
    }
    else if (i < length && attr.charAt(i) == '"') {
      for (i++; i < length && attr.charAt(i) != '"'; i++)
        value.append(attr.charAt(i));
    }
    else if (i < length) {
      char ch;
      for (; i < length && (ch = attr.charAt(i)) != ' ' && ch != ';'; i++)
        value.append(ch);
    }

    return value.close();
  }
}
