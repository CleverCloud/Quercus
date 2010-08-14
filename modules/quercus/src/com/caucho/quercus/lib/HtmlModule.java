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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.ConstArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.i18n.EncodingWriter;

/**
 * PHP functions implementing html code.
 */
public class HtmlModule extends AbstractQuercusModule {
  private static final Logger log
  = Logger.getLogger(HtmlModule.class.getName());
  
  private static final L10N L = new L10N(HtmlModule.class);

  public static final int HTML_SPECIALCHARS = 0;
  public static final int HTML_ENTITIES = 1;

  public static final int ENT_HTML_QUOTE_NONE = 0;
  public static final int ENT_HTML_QUOTE_SINGLE = 1;
  public static final int ENT_HTML_QUOTE_DOUBLE = 2;

  public static final int ENT_COMPAT = ENT_HTML_QUOTE_DOUBLE;
  public static final int ENT_QUOTES =
      ENT_HTML_QUOTE_SINGLE | ENT_HTML_QUOTE_DOUBLE;
  public static final int ENT_NOQUOTES = ENT_HTML_QUOTE_NONE;

  private static StringValue []HTML_SPECIALCHARS_MAP;
  
  private static ArrayValue HTML_SPECIALCHARS_ARRAY;
  private static ArrayValue HTML_ENTITIES_ARRAY;
  private static ArrayValue HTML_ENTITIES_ARRAY_ENTITY_KEY;

  private static ArrayValueImpl HTML_ENTITIES_ARRAY_UNICODE;
  private static ArrayValueImpl HTML_SPECIALCHARS_ARRAY_UNICODE;
  private static ArrayValueImpl HTML_ENTITIES_ARRAY_UNICODE_ENTITY_KEY;

  public HtmlModule()
  {
  }

  private static ConstArrayValue toUnicodeArray(Env env, ArrayValue array)
  {
    ArrayValueImpl copy = new ArrayValueImpl();
    
    Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);

    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();

      Value key = entry.getKey();
      Value value = entry.getValue();

      if (key.isString())
        key = key.toUnicodeValue(env);

      if (value.isString())
        value = value.toUnicodeValue(env);

      copy.put(key, value);
    }

    return new ConstArrayValue(copy);
  }

  /**
   * Returns HTML translation tables.
   */
  public Value get_html_translation_table(
      Env env,
      @Optional("HTML_SPECIALCHARS") int table,
      @Optional("ENT_COMPAT") int quoteStyle) {
    Value result;

    if (! env.isUnicodeSemantics()) {
      if (table == HTML_ENTITIES)
        result = HTML_ENTITIES_ARRAY.copy();
      else
        result = HTML_SPECIALCHARS_ARRAY.copy();
    }
    else {
      if (table == HTML_ENTITIES) {
        if (HTML_ENTITIES_ARRAY_UNICODE == null) {
          HTML_ENTITIES_ARRAY_UNICODE = toUnicodeArray(
              env, HTML_ENTITIES_ARRAY);
        }

        result = HTML_ENTITIES_ARRAY_UNICODE.copy();
      }
      else {
        if (HTML_SPECIALCHARS_ARRAY_UNICODE == null) {
          HTML_SPECIALCHARS_ARRAY_UNICODE = toUnicodeArray(
              env, HTML_SPECIALCHARS_ARRAY);
        }

        result = HTML_SPECIALCHARS_ARRAY_UNICODE.copy();
      }
    }

    if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0)
      result.put(env.createString('\''), env.createString("&#39;"));

    if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0)
      result.put(env.createString('"'), env.createString("&quot;"));

    return result;
  }

  /*
   * Converts escaped HTML entities back to characters.
   * 
   * @param str escaped string
   * @param quoteStyle optional quote style used
   */
  public static StringValue htmlspecialchars_decode(Env env,
                                        StringValue str,
                                        @Optional("ENT_COMPAT") int quoteStyle)
  {
    int len = str.length();
    
    StringValue sb = str.createStringBuilder(len * 4 / 5);

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if (ch != '&') {
        sb.append(ch);
        
        continue;
      }
      
      switch (str.charAt(i + 1)) {
        case 'a':
          sb.append('&');
          if (i + 4 < len
              && str.charAt(i + 2) == 'm'
              && str.charAt(i + 3) == 'p'
              && str.charAt(i + 4) == ';') {
            i += 4;
          }
          break;
          
        case 'q':
          if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0
              && i + 5 < len
              && str.charAt(i + 2) == 'u'
              && str.charAt(i + 3) == 'o'
              && str.charAt(i + 4) == 't'
              && str.charAt(i + 5) == ';') {
            i += 5;
            sb.append('"');
          }
          else
            sb.append('&');
          break;
          
        case '#':
          if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0
              && i + 5 < len
              && str.charAt(i + 2) == '0'
              && str.charAt(i + 3) == '3'
              && str.charAt(i + 4) == '9'
              && str.charAt(i + 5) == ';') {
            i += 5;
            sb.append('\'');
          }
          else
            sb.append('&');
          
          break;

        case 'l':
          if (i + 3 < len
              && str.charAt(i + 2) == 't'
              && str.charAt(i + 3) == ';') {
                i += 3;
                
                sb.append('<');
          }
          else
            sb.append('&');
          break;

        case 'g':
          if (i + 3 < len
              && str.charAt(i + 2) == 't'
              && str.charAt(i + 3) == ';') {
                i += 3;
                
                sb.append('>');
          }
          else
            sb.append('&');
          break;

        default:
          sb.append('&');
      }
    }

    return sb;
  }
  
  /**
   * Escapes HTML
   *
   * @param env the calling environment
   * @param string the string to be trimmed
   * @param quoteStyleV optional quote style
   * @param charsetV optional charset style
   * @return the trimmed string
   */
  public static Value htmlspecialchars(Env env,
                                       StringValue string,
                                       @Optional("ENT_COMPAT") int quoteStyle,
                                       @Optional String charset)
  {
    int len = string.length();
    
    StringValue sb = string.createStringBuilder(len * 5 / 4);

    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case '&':
        sb.append("&amp;");
        break;
      case '"':
        if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0)
          sb.append("&quot;");
        else
          sb.append(ch);
        break;
      case '\'':
        if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0)
          sb.append("&#039;");
        else
          sb.append(ch);
        break;
      case '<':
        sb.append("&lt;");
        break;
      case '>':
        sb.append("&gt;");
        break;
      default:
        sb.append(ch);
        break;
      }
    }

    return sb;
  }

  /**
   * Escapes HTML
   *
   * @param env the calling environment
   * @param stringV the string to be trimmed
   * @param quoteStyleV optional quote style
   * @param charsetV optional charset style
   * @return the trimmed string
   */
  public static Value htmlentities(Env env,
                                   StringValue string,
                                   @Optional("ENT_COMPAT") int quoteStyle,
                                   @Optional String charset)
  {
    if (charset == null || charset.length() == 0)
      charset = "ISO-8859-1";
    
    Reader reader;
    
    try {
      reader = string.toReader(charset);
    } catch (UnsupportedEncodingException e) {
      env.warning(e);
      
      reader = new StringReader(string.toString());
    }
    
    StringValue sb = string.createStringBuilder(string.length() * 5 / 4);
    
    int ch;
    try {
      while ((ch = reader.read()) >= 0) {
        StringValue entity = HTML_SPECIALCHARS_MAP[ch & 0xffff];
        
        if (ch == '"') {
          if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0)
            sb.append("&quot;");
          else
            sb.append('"');
        }
        else if (ch == '\'') {
          if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0)
            sb.append("&#039;");
          else
            sb.append('\'');
        }
        else if (entity != null)
          sb.append(entity);
        else if (env.isUnicodeSemantics() || 0x00 <= ch && ch <= 0xff) {
          sb.append((char) ch);
        }
        else {
          sb.append("&#");
          sb.append(hexdigit(ch >> 12));
          sb.append(hexdigit(ch >> 8));
          sb.append(hexdigit(ch >> 4));
          sb.append(hexdigit(ch));
          sb.append(";");
        }
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }

    return sb;
  }

  private static char hexdigit(int ch)
  {
    ch = ch & 0xf;

    if (ch < 10)
      return (char) (ch + '0');
    else
      return (char) (ch - 10 + 'a');
  }

  /**
   * Escapes HTML
   *
   * @param string the string to be trimmed
   * @param quoteStyle optional quote style
   * @param charset optional charset style
   * @return the trimmed string
   */
  public static StringValue html_entity_decode(Env env,
                                               StringValue string,
                                               @Optional int quoteStyle,
                                               @Optional String charset)
  {
    if (string.length() == 0)
      return env.getEmptyString();

    ArrayValue htmlEntities = null;
    
    boolean isUnicode = env.isUnicodeSemantics();

    if (isUnicode) {
      if (HTML_ENTITIES_ARRAY_UNICODE_ENTITY_KEY == null) {
        HTML_ENTITIES_ARRAY_UNICODE_ENTITY_KEY = toUnicodeArray(
            env, HTML_ENTITIES_ARRAY_ENTITY_KEY);
      }
      
      htmlEntities = HTML_ENTITIES_ARRAY_UNICODE_ENTITY_KEY;
    }
    else
      htmlEntities = HTML_ENTITIES_ARRAY_ENTITY_KEY;

    EncodingWriter out = null;

    if (! isUnicode) {
      if (charset == null || charset.length() == 0)
        charset = env.getRuntimeEncoding();

      out = Encoding.getWriteEncoding(charset);
    }

    int len = string.length();
    int htmlEntityStart = -1;
    StringValue result = env.createStringBuilder();
    
    try {
      // Loop through each character
      for (int i = 0; i < len; i++) {      
        char ch = string.charAt(i);
        
        // Check whether it's a html entity
        // i.e. starts with '&' and ends with ';'
        if (ch == '&' && htmlEntityStart < 0) {                    
          htmlEntityStart = i;
        } 
        else if (htmlEntityStart < 0) {
          // else add it to result.
          result.append(ch);
        } 
        else if (ch == ';') {
          // If so substitute the entity and add it to result.
          StringValue entity = string.substring(htmlEntityStart, i + 1);
          Value value = htmlEntities.get(entity);
          
          if (value.isNull()) {
            result.append(entity);
          }
          else if (isUnicode) {
            result.append((char)value.toInt());            
          }
          else {
            out.write(result, (char)value.toInt());            
          }
          
          htmlEntityStart = -1; 
        } else if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z')) {
        } 
        else {
          result.append('&');
          i = htmlEntityStart;
          htmlEntityStart = -1;
        }
      }
      
      if (htmlEntityStart > 0) {
        result.append(string, htmlEntityStart, len);
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
   
    return result;
  }

  /**
   * Replaces newlines with HTML breaks.
   *
   * @param env the calling environment
   */
  public static Value nl2br(Env env, StringValue string)
  {
    int strLen = string.length();

    StringValue sb = string.createStringBuilder(strLen * 5 / 4);

    for (int i = 0; i < strLen; i++) {
      char ch = string.charAt(i);

      if (ch == '\n') {
        sb.append("<br />\n");
      }
      else if (ch == '\r') {
        if (i + 1 < strLen && string.charAt(i + 1) == '\n') {
          sb.append("<br />\r\n");
          i++;
        }
        else {
          sb.append("<br />\r");
        }
      }
      else {
        sb.append(ch);
      }
    }

    return sb;
  }

  private static void entity(ArrayValue array, StringValue []map,
                             ArrayValue revMap, int ch, String entity)
  {
    // XXX: i18n and optimize static variables usage
    array.put("" + (char) ch, entity);
    StringValue entityValue = new StringBuilderValue(entity);
    map[ch & 0xffff] = entityValue;
    revMap.put(entityValue, LongValue.create(ch));
  }

  static {
    ArrayValueImpl array = new ArrayValueImpl();
    
    array.put("<", "&lt;");
    array.put(">", "&gt;");
    array.put("&", "&amp;");
    
    HTML_SPECIALCHARS_ARRAY = new ConstArrayValue(array);
    StringValue []map = new StringValue[65536];
    HTML_SPECIALCHARS_MAP = map;
    
    ArrayValue revMap = new ArrayValueImpl();
    HTML_ENTITIES_ARRAY_ENTITY_KEY = revMap; 

    array = new ArrayValueImpl();
    entity(array, map, revMap, '<', "&lt;");
    entity(array, map, revMap, '>', "&gt;");
    entity(array, map, revMap, '&', "&amp;");

    entity(array, map, revMap, 160, "&nbsp;");
    entity(array, map, revMap, 161, "&iexcl;");
    entity(array, map, revMap, 162, "&cent;");
    entity(array, map, revMap, 163, "&pound;");
    entity(array, map, revMap, 164, "&curren;");
    entity(array, map, revMap, 165, "&yen;");
    entity(array, map, revMap, 166, "&brvbar;");
    entity(array, map, revMap, 167, "&sect;");
    entity(array, map, revMap, 168, "&uml;");
    entity(array, map, revMap, 169, "&copy;");
    entity(array, map, revMap, 170, "&ordf;");
    entity(array, map, revMap, 171, "&laquo;");
    entity(array, map, revMap, 172, "&not;");
    entity(array, map, revMap, 173, "&shy;");
    entity(array, map, revMap, 174, "&reg;");
    entity(array, map, revMap, 175, "&macr;");
    entity(array, map, revMap, 176, "&deg;");
    entity(array, map, revMap, 177, "&plusmn;");
    entity(array, map, revMap, 178, "&sup2;");
    entity(array, map, revMap, 179, "&sup3;");
    entity(array, map, revMap, 180, "&acute;");
    entity(array, map, revMap, 181, "&micro;");
    entity(array, map, revMap, 182, "&para;");
    entity(array, map, revMap, 183, "&middot;");
    entity(array, map, revMap, 184, "&cedil;");
    entity(array, map, revMap, 185, "&sup1;");
    entity(array, map, revMap, 186, "&ordm;");
    entity(array, map, revMap, 187, "&raquo;");
    entity(array, map, revMap, 188, "&frac14;");
    entity(array, map, revMap, 189, "&frac12;");
    entity(array, map, revMap, 190, "&frac34;");
    entity(array, map, revMap, 191, "&iquest;");
    entity(array, map, revMap, 192, "&Agrave;");
    entity(array, map, revMap, 193, "&Aacute;");
    entity(array, map, revMap, 194, "&Acirc;");
    entity(array, map, revMap, 195, "&Atilde;");
    entity(array, map, revMap, 196, "&Auml;");
    entity(array, map, revMap, 197, "&Aring;");
    entity(array, map, revMap, 198, "&AElig;");
    entity(array, map, revMap, 199, "&Ccedil;");
    entity(array, map, revMap, 200, "&Egrave;");
    entity(array, map, revMap, 201, "&Eacute;");
    entity(array, map, revMap, 202, "&Ecirc;");
    entity(array, map, revMap, 203, "&Euml;");
    entity(array, map, revMap, 204, "&Igrave;");
    entity(array, map, revMap, 205, "&Iacute;");
    entity(array, map, revMap, 206, "&Icirc;");
    entity(array, map, revMap, 207, "&Iuml;");
    entity(array, map, revMap, 208, "&ETH;");
    entity(array, map, revMap, 209, "&Ntilde;");
    entity(array, map, revMap, 210, "&Ograve;");
    entity(array, map, revMap, 211, "&Oacute;");
    entity(array, map, revMap, 212, "&Ocirc;");
    entity(array, map, revMap, 213, "&Otilde;");
    entity(array, map, revMap, 214, "&Ouml;");
    entity(array, map, revMap, 215, "&times;");
    entity(array, map, revMap, 216, "&Oslash;");
    entity(array, map, revMap, 217, "&Ugrave;");
    entity(array, map, revMap, 218, "&Uacute;");
    entity(array, map, revMap, 219, "&Ucirc;");
    entity(array, map, revMap, 220, "&Uuml;");
    entity(array, map, revMap, 221, "&Yacute;");
    entity(array, map, revMap, 222, "&THORN;");
    entity(array, map, revMap, 223, "&szlig;");
    entity(array, map, revMap, 224, "&agrave;");
    entity(array, map, revMap, 225, "&aacute;");
    entity(array, map, revMap, 226, "&acirc;");
    entity(array, map, revMap, 227, "&atilde;");
    entity(array, map, revMap, 228, "&auml;");
    entity(array, map, revMap, 229, "&aring;");
    entity(array, map, revMap, 230, "&aelig;");
    entity(array, map, revMap, 231, "&ccedil;");
    entity(array, map, revMap, 232, "&egrave;");
    entity(array, map, revMap, 233, "&eacute;");
    entity(array, map, revMap, 234, "&ecirc;");
    entity(array, map, revMap, 235, "&euml;");
    entity(array, map, revMap, 236, "&igrave;");
    entity(array, map, revMap, 237, "&iacute;");
    entity(array, map, revMap, 238, "&icirc;");
    entity(array, map, revMap, 239, "&iuml;");
    entity(array, map, revMap, 240, "&eth;");
    entity(array, map, revMap, 241, "&ntilde;");
    entity(array, map, revMap, 242, "&ograve;");
    entity(array, map, revMap, 243, "&oacute;");
    entity(array, map, revMap, 244, "&ocirc;");
    entity(array, map, revMap, 245, "&otilde;");
    entity(array, map, revMap, 246, "&ouml;");
    entity(array, map, revMap, 247, "&divide;");
    entity(array, map, revMap, 248, "&oslash;");
    entity(array, map, revMap, 249, "&ugrave;");
    entity(array, map, revMap, 250, "&uacute;");
    entity(array, map, revMap, 251, "&ucirc;");
    entity(array, map, revMap, 252, "&uuml;");
    entity(array, map, revMap, 253, "&yacute;");
    entity(array, map, revMap, 254, "&thorn;");
    entity(array, map, revMap, 255, "&yuml;");
    
    // XXX: charset, order it.
    entity(array, map, revMap, 0x2002, "&ensp;");
    entity(array, map, revMap, 0x2009, "&thinsp;");
    entity(array, map, revMap, 0x2018, "&lsquo;");
    entity(array, map, revMap, 0x2020, "&dagger;");
    entity(array, map, revMap, 0x2032, "&prime;");
    entity(array, map, revMap, 0x2044, "&frasl;");
    entity(array, map, revMap, 0x20ac, "&euro;");    
    
    HTML_ENTITIES_ARRAY = new ConstArrayValue(array);
  }
}

