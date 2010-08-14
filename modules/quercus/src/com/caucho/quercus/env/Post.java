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

package com.caucho.quercus.env;

import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.lib.string.StringUtility;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.util.L10N;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.MultipartStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Handling of POST requests.
 */
public class Post
{
  private static final L10N L = new L10N(Post.class);
  
  private static StringValue MAX_FILE_SIZE
    = new ConstStringValue("MAX_FILE_SIZE");
  
  private static StringValue MAX_FILE_SIZE_U
    = new UnicodeBuilderValue("MAX_FILE_SIZE");
  
  static void fillPost(Env env,
                       ArrayValue postArray,
                       ArrayValue files,
                       HttpServletRequest request,
                       boolean addSlashesToValues,
                       boolean isAllowUploads)
  {
    InputStream is = null;

    try {
      String encoding = request.getCharacterEncoding();
      String contentType = request.getHeader("Content-Type");

      is = request.getInputStream();

      fillPost(env,
               postArray,
               files,
               is,
               contentType,
               encoding,
               Integer.MAX_VALUE,
               addSlashesToValues,
               isAllowUploads);

      if (postArray.getSize() == 0) {
        // needs to be last or else this function will consume the inputstream
        putRequestMap(env, postArray, files, request,
                      addSlashesToValues, isAllowUploads);
      }

    } catch (IOException e) {
      env.warning(e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (IOException e) {
      }
    }
  }
  
  static void fillPost(Env env,
                       ArrayValue postArray,
                       ArrayValue files,
                       InputStream is,
                       String contentType,
                       String encoding,
                       int contentLength,
                       boolean addSlashesToValues,
                       boolean isAllowUploads)
  {
    long maxPostSize = env.getIniBytes("post_max_size", 0);

    try {
      if (encoding == null)
        encoding = env.getHttpInputEncoding();
      
      if (contentType != null
          && contentType.startsWith("multipart/form-data")) {

        String boundary = getBoundary(contentType);

        ReadStream rs = new ReadStream(new VfsStream(is, null));

        if (boundary == null) {
          env.warning(L.l("multipart/form-data POST is missing boundary"));
          
          return;
        }
        
        MultipartStream ms = new MultipartStream(rs, boundary);
        
        if (encoding != null)
          ms.setEncoding(encoding);

        readMultipartStream(env, ms, postArray, files,
                            addSlashesToValues, isAllowUploads);

        rs.close();
        
        if (rs.getLength() > maxPostSize) {
          env.warning(L.l("POST length of {0} exceeds max size of {1}",
                          rs.getLength(),
                          maxPostSize));
          
          postArray.clear();
          files.clear();
          
          return;
        }
      }
      else {
        StringValue bb = env.createBinaryBuilder();
        
        bb.appendReadAll(is, Integer.MAX_VALUE);
        
        if (bb.length() > maxPostSize) {
          env.warning(L.l("POST length of {0} exceeds max size of {1}",
                          bb.length(),
                          maxPostSize));
          return;
        }
        
        env.setInputData(bb);
        
        if (contentType != null
            && contentType.startsWith("application/x-www-form-urlencoded"))
          StringUtility.parseStr(env, bb, postArray, false, encoding);
      }
      
    } catch (IOException e) {
      env.warning(e);
    } finally {
    }
  }

  private static void readMultipartStream(Env env,
                                          MultipartStream ms,
                                          ArrayValue postArray,
                                          ArrayValue files,
                                          boolean addSlashesToValues,
                                          boolean isAllowUploads)
    throws IOException
  {
    ReadStream is;

    while ((is = ms.openRead()) != null) {
      String attr = (String) ms.getAttribute("content-disposition");

      if (attr == null || ! attr.startsWith("form-data")) {
        // XXX: is this an error?
        continue;
      }

      String name = getAttribute(attr, "name", addSlashesToValues);
      String filename = getAttribute(attr, "filename", addSlashesToValues);
      
      if (filename != null) {
        int slashIndex = filename.lastIndexOf('/');
        int slashIndex2 = filename.lastIndexOf('\\');
        
        slashIndex = Math.max(slashIndex, slashIndex2);
        
        if (slashIndex >= 0)
          filename = filename.substring(slashIndex + 1);
      }

      int bracketIndex = -1;
      
      if (name != null)
        bracketIndex = name.lastIndexOf(']');
      
      if (bracketIndex >= 0 && bracketIndex < name.length() - 1) {
        // php/085c
      }
      else if (filename == null) {
        StringValue value = env.createStringBuilder();

        value.appendReadAll(is, Integer.MAX_VALUE);

        if (name != null) {
          addFormValue(env, postArray, name, value, null, addSlashesToValues);
        }
        else {
          env.warning(L.l("file upload is missing name and filename"));
        }
      }
      else {
        if (! isAllowUploads) {
          continue;
        }
        
        String tmpName = "";
        long tmpLength = 0;

        // A POST file upload with an empty string as the filename does not
        // create a temp file in the upload directory.

        if (filename.length() > 0) {
          Path tmpPath = env.getUploadDirectory().createTempFile("php", ".tmp");

          env.addRemovePath(tmpPath);

          WriteStream os = tmpPath.openWrite();
          try {
            os.writeStream(is);
          } finally {
            os.close();
          }

          tmpName = tmpPath.getFullPath();
          tmpLength = tmpPath.getLength();
        }

        // php/0865
        //
        // A header like "Content-Type: image/gif" indicates the mime type
        // for an uploaded file.

        String mimeType = getAttribute(attr, "mime-type", addSlashesToValues);
        if (mimeType == null) {
          mimeType = (String) ms.getAttribute("content-type");
          
          // php/085f
          if (mimeType != null && mimeType.endsWith(";"))
            mimeType = mimeType.substring(0, mimeType.length() - 1);
        }

        // php/0864
        //
        // mime type is empty string when no file is uploaded.

        if (filename.length() == 0) {
          mimeType = "";
        }

        long maxFileSize = Long.MAX_VALUE;
        
        Value maxFileSizeV = postArray.get(MAX_FILE_SIZE);
        if (! maxFileSizeV.isNull())
          maxFileSize = maxFileSizeV.toLong();

        if (name != null) {
          addFormFile(env, files, name, filename, tmpName,
                      mimeType, tmpLength, addSlashesToValues, maxFileSize);
        }
        else {
          addFormFile(env, files, filename, tmpName,
                      mimeType, tmpLength, addSlashesToValues, maxFileSize);
        }
      }
    }
  }

  private static void addFormFile(Env env,
                                  ArrayValue files,
                                  String fileName,
                                  String tmpName,
                                  String mimeType,
                                  long fileLength,
                                  boolean addSlashesToValues,
                                  long maxFileSize)
  {
    ArrayValue entry = new ArrayValueImpl();
    int error;

    // php/1667
    long uploadMaxFilesize
      = env.getIniBytes("upload_max_filesize", 2 * 1024 * 1024);

    if (fileName.length() == 0)
      // php/0864
      error = FileModule.UPLOAD_ERR_NO_FILE;
    else if (fileLength > uploadMaxFilesize)
      error = FileModule.UPLOAD_ERR_INI_SIZE;
    else if (fileLength > maxFileSize)
      error = FileModule.UPLOAD_ERR_FORM_SIZE;
    else
      error = FileModule.UPLOAD_ERR_OK;

    addFormValue(env, entry, "name", env.createString(fileName),
                 null, addSlashesToValues);

    long size;

    if (error != FileModule.UPLOAD_ERR_INI_SIZE) {
      size = fileLength;
    }
    else {
      mimeType = "";
      tmpName = "";
      size = 0;
    }

    if (mimeType != null) {
      addFormValue(env, entry, "type", env.createString(mimeType),
                   null, addSlashesToValues);

      entry.put("type", mimeType);
    }

    addFormValue(env, entry, "tmp_name", env.createString(tmpName),
                 null, addSlashesToValues);

    addFormValue(env, entry, "error", LongValue.create(error),
                 null, addSlashesToValues);

    addFormValue(env, entry, "size", LongValue.create(size),
                 null, addSlashesToValues);
    
    addFormValue(env, files, null, entry, null, addSlashesToValues);
  }
  
  private static void addFormFile(Env env,
                                  ArrayValue files,
                                  String name,
                                  String fileName,
                                  String tmpName,
                                  String mimeType,
                                  long fileLength,
                                  boolean addSlashesToValues,
                                  long maxFileSize)
  {
    int p = name.indexOf('[');
    String index = "";
    if (p >= 0) {
      index = name.substring(p);
      name = name.substring(0, p);
    }

    StringValue nameValue = env.createString(name);
    Value v = files.get(nameValue).toValue();
    ArrayValue entry = null;
    if (v instanceof ArrayValue)
      entry = (ArrayValue) v;

    if (entry == null) {
      entry = new ArrayValueImpl();
      files.put(nameValue, entry);
    }

    int error;

    // php/1667
    long uploadMaxFilesize
      = env.getIniBytes("upload_max_filesize", 2 * 1024 * 1024);

    if (fileName.length() == 0)
      // php/0864
      error = FileModule.UPLOAD_ERR_NO_FILE;
    else if (fileLength > uploadMaxFilesize)
      error = FileModule.UPLOAD_ERR_INI_SIZE;
    else if (fileLength > maxFileSize)
      error = FileModule.UPLOAD_ERR_FORM_SIZE;
    else
      error = FileModule.UPLOAD_ERR_OK;

    addFormValue(env, entry, "name" + index, env.createString(fileName),
                 null, addSlashesToValues);

    long size;

    
    if (error == FileModule.UPLOAD_ERR_OK) {
      size = fileLength;
    }
    else {
      mimeType = "";
      tmpName = "";
      size = 0;
    }

    if (mimeType != null) {
      addFormValue(env, entry, "type" + index, env.createString(mimeType),
                   null, addSlashesToValues);
    }

    addFormValue(env, entry, "tmp_name" + index, env.createString(tmpName),
                 null, addSlashesToValues);

    addFormValue(env, entry, "error" + index, LongValue.create(error),
                 null, addSlashesToValues);

    addFormValue(env, entry, "size" + index, LongValue.create(size),
                 null, addSlashesToValues);

    addFormValue(env, files, name, entry, null, addSlashesToValues);
  }
  
  public static void addFormValue(Env env,
                                  ArrayValue array,
                                  String key,
                                  String []formValueList,
                                  boolean addSlashesToValues)
  {
    // php/081b
    String formValue = formValueList[formValueList.length - 1];
    Value value;

    if (formValue != null)
      value = env.createString(formValue);
    else
      value = NullValue.NULL;

    addFormValue(env, array, key,
                 value,
                 formValueList,
                 addSlashesToValues);
  }
  
  public static void addFormValue(Env env,
                                  ArrayValue array,
                                  String key,
                                  Value formValue,
                                  String []formValueList,
                                  boolean addSlashesToValues)
  {
    int p = -1;
    int q = -1;
    
    if (key != null) {
      p = key.indexOf('[');
      q = key.indexOf(']', p);
    }

    if (p >= 0 && p < q) {
      String index = key;
      
      Value keyValue;
      Value existingValue;

      if (p > 0) {
        key = key.substring(0, p);
        
        key = key.replaceAll("\\.", "_");
        
        keyValue = env.createString(key);
        existingValue = array.get(keyValue);

        if (existingValue == null || ! existingValue.isset()) {
          existingValue = new ArrayValueImpl();
          array.put(keyValue, existingValue);
        }
        else if (! existingValue.isArray()) {
          //existing is overwritten
          // php/115g

          existingValue = new ArrayValueImpl();
          array.put(keyValue, existingValue);
        }

        array = (ArrayValue) existingValue;
      }

      int p1;
      while ((p1 = index.indexOf('[', q)) > 0) {
        key = index.substring(p + 1, q);

        if (key.equals("")) {
          existingValue = new ArrayValueImpl();
          array.put(existingValue);
        }
        else {
          keyValue = env.createString(key);
          existingValue = array.get(keyValue);

          if (existingValue == null || ! existingValue.isset()) {
            existingValue = new ArrayValueImpl();
            array.put(keyValue, existingValue);
          }
          else if (! existingValue.isArray()) {
            existingValue = new ArrayValueImpl().put(existingValue);
            array.put(keyValue, existingValue);
          }
        }

        array = (ArrayValue) existingValue;

        p = p1;
        q = index.indexOf(']', p);
      }

      if (q > 0)
        index = index.substring(p + 1, q);
      else
        index = index.substring(p + 1);

      if (index.equals("")) {
        if (formValueList != null) {
          for (int i = 0; i < formValueList.length; i++) {
            Value value;

            if (formValueList[i] != null)
              value = env.createString(formValueList[i]);
            else
              value = NullValue.NULL;

            put(array, null, value, addSlashesToValues);
          }
        }
        else
          array.put(formValue);
      }
      else if ('0' <= index.charAt(0) && index.charAt(0) <= '9')
        put(array,
            LongValue.create(StringValue.toLong(index)),
            formValue,
            addSlashesToValues);
      else
        put(array, env.createString(index), formValue, addSlashesToValues);
    }
    else {
      if (key != null) {
        key = key.replaceAll("\\.", "_");
        put(array, env.createString(key), formValue, addSlashesToValues);
      }
      else {
        put(array, null, formValue, addSlashesToValues);
      }
    }
  }

  private static void put(ArrayValue array,
                          Value key,
                          Value value,
                          boolean addSlashes)
  {
    if (addSlashes && value.isString()) {
      value = StringModule.addslashes(value.toStringValue());
    }

    if (key == null)
      array.put(value);
    else
      array.put(key, value);
  }

  private static String getBoundary(String contentType)
  {
    int i = contentType.indexOf("boundary=");
    if (i < 0)
      return null;

    i += "boundary=".length();

    int length = contentType.length();

    char ch;

    if (length <= i)
      return null;
    else if ((ch = contentType.charAt(i)) == '\'') {
      StringBuilder sb = new StringBuilder();

      for (
        i++;
        i < length && (ch = contentType.charAt(i)) != '\'';
        i++
        ) {
        sb.append(ch);
      }

      return sb.toString();
    }
    else if (ch == '"') {
      StringBuilder sb = new StringBuilder();

      for (i++; i < length && (ch = contentType.charAt(i)) != '"'; i++) {
        sb.append(ch);
      }

      return sb.toString();
    }
    else {
      StringBuilder sb = new StringBuilder();

      for (/* intentionally left empty */;
                                         i < length
                                         && (ch = contentType.charAt(i)) != ' '
                                         && ch != ';'
                                         && ch != ',';
                                         i++) {
        sb.append(ch);
      }

      return sb.toString();
    }
  }

  private static String getAttribute(String attr,
                                     String name,
                                     boolean addSlashesToValues)
  {
    if (attr == null)
      return null;

    int length = attr.length();
    int i = attr.indexOf(name);
    
    if (i < 0)
      return null;
    
    while (true) {
      char ch = attr.charAt(i - 1);

      if (i > 0 && ch != ' ' && ch != ';') {
        i = attr.indexOf(name, i + name.length());
      }
      else
        break;
      
      if (i < 0)
        return null;
    }

    for (i += name.length(); i < length && attr.charAt(i) != '='; i++) {
    }

    for (i++; i < length && attr.charAt(i) == ' '; i++) {
    }

    StringBuilder value = new StringBuilder();

    if (i < length && attr.charAt(i) == '\'') {
      for (i++; i < length && attr.charAt(i) != '\''; i++) {
        char ch = attr.charAt(i);
        
        if (ch == '"' && addSlashesToValues)
          break;
        
        value.append(ch);
      }
    }
    else if (i < length && attr.charAt(i) == '"') {
      for (i++; i < length && attr.charAt(i) != '"'; i++) {
        char ch = attr.charAt(i);
        
        if (ch == '\'' && addSlashesToValues)
          break;
        
        value.append(ch);
      }
    }
    else if (i < length) {
      char ch;
      for (; i < length && (ch = attr.charAt(i)) != ' ' && ch != ';'; i++)
        value.append(ch);
    }

    return value.toString();
  }
  
  private static void putRequestMap(Env env,
                                    ArrayValue post,
                                    ArrayValue files,
                                    HttpServletRequest request,
                                    boolean addSlashesToValues,
                                    boolean isAllowUploads)
  {
    // this call consumes the inputstream
    Map<String,String[]> map = request.getParameterMap();

    if (map == null)
      return;
    
    long maxFileSize = Long.MAX_VALUE;
    
    Value maxFileSizeV = post.get(MAX_FILE_SIZE);
    if (maxFileSizeV.isNull())
      maxFileSize = maxFileSizeV.toLong();
      
    if (isAllowUploads) {
      for (Map.Entry<String,String[]> entry : map.entrySet()) {
        String key = entry.getKey();

        int len = key.length();

        if (len < 10 || ! key.endsWith(".filename"))
          continue;

        String name = key.substring(0, len - 9);

        String []fileNames = request.getParameterValues(name + ".filename");
        String []tmpNames = request.getParameterValues(name + ".file");
        String []mimeTypes
          = request.getParameterValues(name + ".content-type");

        for (int i = 0; i < fileNames.length; i++) {
          long fileLength = new FilePath(tmpNames[i]).getLength();

          addFormFile(env, files, name, fileNames[i], tmpNames[i],
                      mimeTypes[i],
                      fileLength,
                      addSlashesToValues,
                      maxFileSize);
        }
      }
    }

    ArrayList<String> keys = new ArrayList<String>();

    keys.addAll(request.getParameterMap().keySet());

    Collections.sort(keys);

    for (String key : keys) {   
      String []value = request.getParameterValues(key);
      
      Post.addFormValue(env, post, key, value, addSlashesToValues);
    }
  }
}

