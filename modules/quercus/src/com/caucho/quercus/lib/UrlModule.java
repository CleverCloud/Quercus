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

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.BinaryStream;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * PHP URL
 */
public class UrlModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(UrlModule.class);
  private static final Logger log
    = Logger.getLogger(UrlModule.class.getName());

  public static final int PHP_URL_SCHEME = 0;
  public static final int PHP_URL_HOST = 1;
  public static final int PHP_URL_PORT = 2;
  public static final int PHP_URL_USER = 3;
  public static final int PHP_URL_PASS = 4;
  public static final int PHP_URL_PATH = 5;
  public static final int PHP_URL_QUERY = 6;
  public static final int PHP_URL_FRAGMENT = 7;
  
  private static final StringValue SCHEME_V
    = new ConstStringValue("scheme");
  private static final StringValue SCHEME_U
    = new UnicodeBuilderValue("scheme");
  
  private static final StringValue USER_V
    = new ConstStringValue("user");
  private static final StringValue USER_U
    = new UnicodeBuilderValue("user");
  
  private static final StringValue PASS_V
    = new ConstStringValue("pass");
  private static final StringValue PASS_U
    = new UnicodeBuilderValue("pass");
  
  private static final StringValue HOST_V
    = new ConstStringValue("host");
  private static final StringValue HOST_U
    = new UnicodeBuilderValue("host");
  
  private static final StringValue PORT_V
    = new ConstStringValue("port");
  private static final StringValue PORT_U
    = new UnicodeBuilderValue("port");
  
  private static final StringValue PATH_V
    = new ConstStringValue("path");
  private static final StringValue PATH_U
    = new UnicodeBuilderValue("path");
  
  private static final StringValue QUERY_V
    = new ConstStringValue("query");
  private static final StringValue QUERY_U
    = new UnicodeBuilderValue("query");
  
  private static final StringValue FRAGMENT_V
    = new ConstStringValue("fragment");
  private static final StringValue FRAGMENT_U
    = new UnicodeBuilderValue("fragment");
  
  /**
   * Encodes base64
   */
  public static String base64_encode(StringValue s)
  {
    CharBuffer cb = new CharBuffer();

    byte []buffer = new byte[3];

    int strlen = s.length();
    int offset = 0;
    
    for (; offset + 3 <= strlen; offset += 3) {
      buffer[0] = (byte) s.charAt(offset);
      buffer[1] = (byte) s.charAt(offset + 1);
      buffer[2] = (byte) s.charAt(offset + 2);
        
      Base64.encode(cb, buffer, 0, 3);
    }

    if (offset < strlen)
      buffer[0] = (byte) s.charAt(offset);
    if (offset + 1 < strlen)
      buffer[1] = (byte) s.charAt(offset + 1);
    if (offset + 2 < strlen)
      buffer[2] = (byte) s.charAt(offset + 2);
      
    Base64.encode(cb, buffer, 0, strlen - offset);

    return cb.toString();
  }

  /**
   * Decodes base64
   */
  public static Value base64_decode(Env env,
                                    StringValue str,
                                    @Optional boolean isStrict)
  {
    if (str.length() == 0)
      return str;
    
    StringValue sb = env.createStringBuilder();
    
    OutputStream os = new StringBuilderOutputStream(sb);

    try {
      Base64.decodeIgnoreWhitespace(str.toSimpleReader(), os);
    } catch (IOException e) {
      
      env.warning(e);
      return BooleanValue.FALSE;
    }
    
    return sb;
  }

  /**
   * Connects to the given URL using a HEAD request to retreive
   * the headers sent in the response.
   */
  public static Value get_headers(Env env, String urlString,
                                  @Optional Value format)
  {
    Socket socket = null;

    try {
      URL url = new URL(urlString);

      if (! url.getProtocol().equals("http")
          && ! url.getProtocol().equals("https")) {
        env.warning(L.l("Not an HTTP URL"));
        return null;
      }

      int port = 80;

      if (url.getPort() < 0) {
        if (url.getProtocol().equals("http"))
          port = 80;
        else if (url.getProtocol().equals("https"))
          port = 443;
      } else {
        port = url.getPort();
      }

      socket = new Socket(url.getHost(), port);

      OutputStream out = socket.getOutputStream();
      InputStream in = socket.getInputStream();

      StringBuilder request = new StringBuilder();

      request.append("HEAD ");

      if (url.getPath() != null)
        request.append(url.getPath());

      if (url.getQuery() != null)
        request.append("?" + url.getQuery());

      if (url.getRef() != null)
        request.append("#" + url.getRef());

      request.append(" HTTP/1.0\r\n");

      if (url.getHost() != null)
        request.append("Host: " + url.getHost() + "\r\n");

      request.append("\r\n");

      OutputStreamWriter writer = new OutputStreamWriter(out);
      writer.write(request.toString());
      writer.flush();

      LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));

      ArrayValue result = new ArrayValueImpl();

      if (format.toBoolean()) {
        for (String line = reader.readLine();
             line != null;
             line = reader.readLine()) {
          line = line.trim();

          if (line.length() == 0)
            continue;

          int colon = line.indexOf(':');

          ArrayValue values;

          if (colon < 0)
            result.put(env.createString(line.trim()));
          else {
            StringValue key =
              env.createString(line.substring(0, colon).trim());

            StringValue value;

            if (colon < line.length())
              value = env.createString(line.substring(colon + 1).trim());
            else
              value = env.getEmptyString();


            if (result.get(key) != UnsetValue.UNSET)
              values = (ArrayValue)result.get(key);
            else {
              values = new ArrayValueImpl();

              result.put(key, values);
            }

            values.put(value);
          }
        }

        // collapse single entries
        for (Value key : result.keySet()) {
          Value value = result.get(key);

          if (value.isArray() && ((ArrayValue)value).getSize() == 1)
            result.put(key, ((ArrayValue)value).get(LongValue.ZERO));
        }
      } else {
        for (String line = reader.readLine();
             line != null;
             line = reader.readLine()) {
          line = line.trim();

          if (line.length() == 0)
            continue;

          result.put(env.createString(line.trim()));
        }
      }

      return result;
    } catch (Exception e) {
      env.warning(e);

      return BooleanValue.FALSE;
    } finally {
      try {
        if (socket != null)
          socket.close();
      } catch (IOException e) {
        env.warning(e);
      }
    }
  }

  /**
   * Extracts the meta tags from a file and returns them as an array.
   */
  public static Value get_meta_tags(Env env, StringValue filename,
                                    @Optional("false") boolean use_include_path)
  {
    InputStream in = null;

    ArrayValue result = new ArrayValueImpl();

    try {
      BinaryStream stream
        = FileModule.fopen(env, filename, "r", use_include_path, null);

      if (stream == null || ! (stream instanceof BinaryInput))
        return result;

      BinaryInput input = (BinaryInput) stream;

      while (! input.isEOF()) {
        String tag = getNextTag(input);

        if (tag.equalsIgnoreCase("meta")) {
          String name = null;
          String content = null;

          String [] attr;

          while ((attr = getNextAttribute(input)) != null) {
            if (name == null && attr[0].equalsIgnoreCase("name")) {
              if (attr.length > 1)
                name = attr[1];
            } else if (content == null && attr[0].equalsIgnoreCase("content")) {
              if (attr.length > 1)
                content = attr[1];
            }

            if (name != null && content != null) {
              result.put(env.createString(name),
                         env.createString(content));
              break;
            }
          }
        } else if (tag.equalsIgnoreCase("/head"))
          break;
      }
    } catch (IOException e) {
      env.warning(e);
    } finally {
      try {
        if (in != null)
          in.close();
      } catch (IOException e) {
        env.warning(e);
      }
    }

    return result;
  }

  public static Value http_build_query(
      Env env,
      Value formdata,
      @Optional StringValue numeric_prefix,
      @Optional("'&'") StringValue separator) {
    StringValue result = env.createUnicodeBuilder();

    httpBuildQueryImpl(env,
                       result,
                       formdata,
                       env.getEmptyString(),
                       numeric_prefix,
                       separator);

    return result;
  }
  
  private static void httpBuildQueryImpl(Env env,
                                         StringValue result,
                                         Value formdata,
                                         StringValue path,
                                         StringValue numeric_prefix,
                                         StringValue separator)
  {
    Set<Map.Entry<Value,Value>> entrySet;

    if (formdata.isArray())
      entrySet = ((ArrayValue)formdata).entrySet();
    else if (formdata.isObject()) {
      Set<? extends Map.Entry<Value,Value>> stringEntrySet
        = ((ObjectValue)formdata).entrySet();

      LinkedHashMap<Value,Value> valueMap = new LinkedHashMap<Value,Value>();

      for (Map.Entry<Value,Value> entry : stringEntrySet)
        valueMap.put(entry.getKey(), entry.getValue());

      entrySet = valueMap.entrySet();
    } else {
      env.warning(L.l("formdata must be an array or object"));

      return;
    }

    boolean isFirst = true;
    for (Map.Entry<Value,Value> entry : entrySet) {
      if (! isFirst) {
        if (separator != null)
          result.append(separator);
        else
          result.append("&");
      }
      isFirst = false;
      
      StringValue newPath = makeNewPath(path, entry.getKey(), numeric_prefix);
      Value entryValue = entry.getValue();

      if (entryValue.isArray() || entryValue.isObject()) {
        // can always throw away the numeric prefix on recursive calls
        httpBuildQueryImpl(env, result, entryValue, newPath, null, separator);

      } else {
        result.append(newPath);
        result.append("=");
        result.append(urlencode(entry.getValue().toStringValue()));
      }
    }
  }

  private static StringValue makeNewPath(StringValue oldPath,
                                         Value key,
                                         StringValue numeric_prefix)
  {
    StringValue path = oldPath.createStringBuilder();
    
    if (oldPath.length() != 0) {
      path.append(oldPath);
      //path.append('[');
      path.append("%5B");
      urlencode(path, key.toStringValue());
      //path.append(']');
      path.append("%5D");

      return path;
    }
    else if (key.isLongConvertible() && numeric_prefix != null) {
      urlencode(path, numeric_prefix);
      urlencode(path, key.toStringValue());
      
      return path;
    }
    else {
      urlencode(path, key.toStringValue());
      
      return path;
    }
  }

  /**
   * Creates a http string.
   */
  /*
  public String http_build_query(Value value,
                                 @Optional String prefix)
  {
    StringBuilder sb = new StringBuilder();

    int index = 0;
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      for (Map.Entry<Value,Value> entry : array.entrySet()) {
        Value keyValue = entry.getKey();
        Value v = entry.getValue();

        String key;

        if (keyValue.isLongConvertible())
          key = prefix + keyValue;
        else
          key = keyValue.toString();

        if (v instanceof ArrayValue)
          http_build_query(sb, key, (ArrayValue) v);
        else {
          if (sb.length() > 0)
            sb.append('&');

          sb.append(key);
          sb.append('=');
          urlencode(sb, v.toString());
        }
      }
    }

    return sb.toString();
  }
  */

  /**
   * Creates a http string.
   */
  /*
  private void http_build_query(StringBuilder sb,
                                String prefix,
                                ArrayValue array)
  {
    for (Map.Entry<Value,Value> entry : array.entrySet()) {
      Value keyValue = entry.getKey();
      Value v = entry.getValue();

      String key = prefix + '[' + keyValue + ']';

      if (v instanceof ArrayValue)
        http_build_query(sb, key, (ArrayValue) v);
      else {
        if (sb.length() > 0)
          sb.append('&');

        sb.append(key);
        sb.append('=');
        urlencode(sb, v.toString());
      }
    }
  }
  */

  /**
   * Parses the URL into an array.
   */
  public static Value parse_url(Env env,
                                StringValue str,
                                @Optional("-1") int component)
  {
    boolean isUnicode = env.isUnicodeSemantics();
    
    ArrayValueImpl array = new ArrayValueImpl();

    parseUrl(env, str, array, isUnicode);

    switch (component) {
      case PHP_URL_SCHEME:
        return array.get(isUnicode ? SCHEME_U : SCHEME_V);
      case PHP_URL_HOST:
        return array.get(isUnicode ? HOST_U : HOST_V);
      case PHP_URL_PORT:
        return array.get(isUnicode ? PORT_U : PORT_V);
      case PHP_URL_USER:
        return array.get(isUnicode ? USER_U : USER_V);
      case PHP_URL_PASS:
        return array.get(isUnicode ? PASS_U : PASS_V);
      case PHP_URL_PATH:
        return array.get(isUnicode ? PATH_U : PATH_V);
      case PHP_URL_QUERY:
        return array.get(isUnicode ? QUERY_U : QUERY_V);
      case PHP_URL_FRAGMENT:
        return array.get(isUnicode ? FRAGMENT_U : FRAGMENT_V);
    }
    
    return array;
  }
  
  private static void parseUrl(Env env,
                               StringValue str,
                               ArrayValue array,
                               boolean isUnicode)
  {
    int strlen = str.length();
    
    if (strlen == 0) {
      array.put(PATH_V, PATH_U, env.getEmptyString(), isUnicode);
      return;
    }
    
    int i = 0;
    char ch;
    
    int colon = str.indexOf(":");
    
    boolean hasHost = false;
    
    if (0 <= colon) {
      int end = colon;
      
      if (colon + 1 < strlen && str.charAt(colon + 1) == '/') {
        if (colon + 2 < strlen && str.charAt(colon + 2) == '/') {
          end = colon + 2;
          
          if (colon + 3 < strlen && str.charAt(colon + 3) == '/') {
          }
          else {
            hasHost = true;
          }
        }
        
        StringValue sb = env.createStringBuilder();
        sb.append(str, 0, colon);
        array.put(SCHEME_V, SCHEME_U, sb, isUnicode);
        
        i = end + 1;
      }
      else if (colon + 1 == strlen
               || (ch = str.charAt(colon + 1)) <= '0'
               || '9' <= ch) {
        StringValue sb = env.createStringBuilder();
        sb.append(str, 0, colon);
        array.put(SCHEME_V, SCHEME_U, sb, isUnicode);
        
        i = colon + 1;
      }
      else {
        hasHost = true;
      }
    }
    
    colon = str.indexOf(':', i);
    int atSign = str.lastIndexOf('@');
    
    StringValue user = null;
    StringValue pass = null;
    
    // username:password
    if (0 <= atSign && hasHost) {
      if (0 <= colon && colon < atSign) {
        if (i < colon) {
          user = env.createStringBuilder();
          user.append(str, i, colon);
        }
        
        if (colon + 1 < atSign) {
          pass = env.createStringBuilder();
          pass.append(str, colon + 1, atSign);
        }
        
        i = atSign + 1;
        
        colon = str.indexOf(':', i);
      }
      else {
        user = env.createStringBuilder();
        user.append(str, i, atSign);
        
        i = atSign + 1;
      }
    }
    
    int question = str.indexOf('?', i);
    int pound = str.indexOf('#', i);

    if (0 <= i && hasHost) {
      int slash = str.indexOf('/', i);

      if (i < colon) {
        StringValue sb = env.createStringBuilder();
        sb.append(str, i, colon);
        array.put(HOST_V, HOST_U, sb, isUnicode);
        
        int end;
        if (i < slash)
          end = slash;
        else if (i < question)
          end = question + 1;
        else if (i < pound)
          end = pound + 1;
        else
          end = strlen;

        if (0 < end - (colon + 1)) {
          int port = 0;
          
          for (int j = colon + 1; j < end; j++) {
            ch = str.charAt(j);
            
            if ('0' <= ch && ch <= '9')
              port = port * 10 + ch - '0';
            else
              break;
          }

          array.put(PORT_V, PORT_U, LongValue.create(port), isUnicode);
        }

        i = end;
      }
      else if (i < question && (slash < i || question < slash)) {
        StringValue sb = env.createStringBuilder();
        sb.append(str, i, question);
        array.put(HOST_V, HOST_U, sb, isUnicode);
        
        i = question + 1;
      }
      else if (i < slash) {
        StringValue sb = env.createStringBuilder();
        sb.append(str, i, slash);
        array.put(HOST_V, HOST_U, sb, isUnicode);
        
        i = slash;
      }
      else if (i < pound) {
        StringValue sb = env.createStringBuilder();
        sb.append(str, i, pound);
        array.put(HOST_V, HOST_U, sb, isUnicode);

        i = pound + 1;
      }
      else {
        StringValue sb = env.createStringBuilder();
        sb.append(str, i, strlen);
        array.put(HOST_V, HOST_U, sb, isUnicode);
        
        i = strlen;
      }
    }
    
    // insert user and password after port
    if (user != null)
      array.put(USER_V, USER_U, user, isUnicode);
    
    if (pass != null)
      array.put(PASS_V, PASS_U, pass, isUnicode);

    if (i < question) {
      StringValue sb = env.createStringBuilder();
      sb.append(str, i, question);
      array.put(PATH_V, PATH_U, sb, isUnicode);
      
      i = question + 1;
    }
    
    if (0 <= pound) {
      if (i < pound) {
        StringValue sb = env.createStringBuilder();
        
        sb.append(str, i, pound);
        
        if (0 <= question)
          array.put(QUERY_V, QUERY_U, sb, isUnicode);
        else
          array.put(PATH_V, PATH_U, sb, isUnicode);
      }

      if (pound + 1 < strlen) {
        StringValue sb = env.createStringBuilder();
        sb.append(str, pound + 1, strlen);
        array.put(FRAGMENT_V, FRAGMENT_U, sb, isUnicode);
      }
    }
    else if (i < strlen) {
      StringValue sb = env.createStringBuilder();
      sb.append(str, i, strlen);
      
      if (0 <= question)
        array.put(QUERY_V, QUERY_U, sb, isUnicode);
      else
        array.put(PATH_V, PATH_U, sb, isUnicode);
    }
  }

  /**
   * Returns the decoded string.
   */
  public static String rawurldecode(String s)
  {
    if (s == null)
      return "";

    int len = s.length();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '%' && i + 2 < len) {
        int d1 = s.charAt(i + 1);
        int d2 = s.charAt(i + 2);

        int v = 0;

        if ('0' <= d1 && d1 <= '9')
          v = 16 * (d1 - '0');
        else if ('a' <= d1 && d1 <= 'f')
          v = 16 * (d1 - 'a' + 10);
        else if ('A' <= d1 && d1 <= 'F')
          v = 16 * (d1 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        if ('0' <= d2 && d2 <= '9')
          v += (d2 - '0');
        else if ('a' <= d2 && d2 <= 'f')
          v += (d2 - 'a' + 10);
        else if ('A' <= d2 && d2 <= 'F')
          v += (d2 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        i += 2;
        sb.append((char) v);
      }
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  /**
   * Encodes the url
   */
  public static String rawurlencode(String str)
  {
    if (str == null)
      return "";
    
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);

      if ('a' <= ch && ch <= 'z'
          || 'A' <= ch && ch <= 'Z'
          || '0' <= ch && ch <= '9'
          || ch == '-' || ch == '_' || ch == '.' || ch == '~') {
        sb.append(ch);
      }
      else {
        sb.append('%');
        sb.append(toHexDigit(ch >> 4));
        sb.append(toHexDigit(ch));
      }
    }

    return sb.toString();
  }

  enum ParseUrlState {
    INIT, USER, PASS, HOST, PORT, PATH, QUERY, FRAGMENT
  };

  /**
   * Gets the magic quotes value.
   */
  public static StringValue urlencode(StringValue str)
  {
    StringValue sb = str.createStringBuilder();

    urlencode(sb, str);

    return sb;
  }

  /**
   * Gets the magic quotes value.
   */
  private static void urlencode(StringValue sb, StringValue str)
  {
    int len = str.length();

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if ('a' <= ch && ch <= 'z')
        sb.append(ch);
      else if ('A' <= ch && ch <= 'Z')
        sb.append(ch);
      else if ('0' <= ch && ch <= '9')
        sb.append(ch);
      else if (ch == '-' || ch == '_' || ch == '.')
        sb.append(ch);
      else if (ch == ' ')
        sb.append('+');
      else {
        sb.append('%');
        sb.append(toHexDigit(ch / 16));
        sb.append(toHexDigit(ch));
      }
    }
  }

  /**
   * Returns the decoded string.
   */
  public static String urldecode(String s)
  {
    if (s == null)
      return "";
  
    int len = s.length();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '%' && i + 2 < len) {
        int d1 = s.charAt(i + 1);
        int d2 = s.charAt(i + 2);

        int v = 0;

        if ('0' <= d1 && d1 <= '9')
          v = 16 * (d1 - '0');
        else if ('a' <= d1 && d1 <= 'f')
          v = 16 * (d1 - 'a' + 10);
        else if ('A' <= d1 && d1 <= 'F')
          v = 16 * (d1 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        if ('0' <= d2 && d2 <= '9')
          v += (d2 - '0');
        else if ('a' <= d2 && d2 <= 'f')
          v += (d2 - 'a' + 10);
        else if ('A' <= d2 && d2 <= 'F')
          v += (d2 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        i += 2;
        sb.append((char) v);
      }
      else if (ch == '+')
        sb.append(' ');
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  private static String getNextTag(BinaryInput input)
    throws IOException
  {
    StringBuilder tag = new StringBuilder();

    for (int ch = 0; ! input.isEOF() && ch != '<'; ch = input.read()) {
      // intentionally left empty
    }

    while (! input.isEOF()) {
      int ch = input.read();

      if (Character.isWhitespace(ch))
        break;

      tag.append((char) ch);
    }

    return tag.toString();
  }

  /**
   * Finds the next attribute in the stream and return the key and value
   * as an array.
   */
  private static String [] getNextAttribute(BinaryInput input)
    throws IOException
  {
    int ch;

    consumeWhiteSpace(input);

    StringBuilder attribute = new StringBuilder();

    while (! input.isEOF()) {
      ch = input.read();

      if (isValidAttributeCharacter(ch))
        attribute.append((char) ch);
      else {
        input.unread();
        break;
      }
    }

    if (attribute.length() == 0)
      return null;

    consumeWhiteSpace(input);

    if (input.isEOF())
      return new String[] { attribute.toString() };

    ch = input.read();
    if (ch != '=') {
      input.unread();

      return new String[] { attribute.toString() };
    }

    consumeWhiteSpace(input);

    // check for quoting
    int quote = ' ';
    boolean quoted = false;

    if (input.isEOF())
      return new String[] { attribute.toString() };

    ch = input.read();

    if (ch == '"' || ch == '\'') {
      quoted = true;
      quote = ch;
    } else
      input.unread();

    StringBuilder value = new StringBuilder();

    while (! input.isEOF()) {
      ch = input.read();

      // mimics PHP behavior
      if ((quoted && ch == quote)
          || (! quoted && Character.isWhitespace(ch)) || ch == '>')
        break;

      value.append((char) ch);
    }

    return new String[] { attribute.toString(), value.toString() };
  }

  private static void consumeWhiteSpace(BinaryInput input)
    throws IOException
  {
    int ch = 0;

    while (! input.isEOF() && Character.isWhitespace(ch = input.read())) {
      // intentionally left empty
    }

    if (! Character.isWhitespace(ch))
      input.unread();
  }

  private static boolean isValidAttributeCharacter(int ch)
  {
    return Character.isLetterOrDigit(ch)
        || (ch == '-') || (ch == '.') || (ch == '_') || (ch == ':');
  }

  private static char toHexDigit(int d)
  {
    d = d & 0xf;

    if (d < 10)
      return (char) ('0' + d);
    else
      return (char) ('A' + d - 10);
  }
}

