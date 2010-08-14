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

package com.caucho.quercus.lib.string;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Expect;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.UsesSymbolTable;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.DefaultValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LocaleInfo;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusLocale;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnexpectedValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.lib.file.BinaryOutput;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.CharBuffer;
import com.caucho.util.FreeList;
import com.caucho.util.IntSet;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.ByteToChar;
import com.caucho.vfs.Path;

/**
 * PHP functions implemented from the string module
 */
public class StringModule extends AbstractQuercusModule {
  private static final Logger log =
    Logger.getLogger(StringModule.class.getName());

  private static final L10N L = new L10N(StringModule.class);

  public static final int CRYPT_SALT_LENGTH = 2;
  public static final int CRYPT_STD_DES = 0;
  public static final int CRYPT_EXT_DES = 0;
  public static final int CRYPT_MD5 = 0;
  public static final int CRYPT_BLOWFISH = 0;

  public static final int CHAR_MAX = 1;

  public static final int LC_CTYPE = 1;
  public static final int LC_NUMERIC = 2;
  public static final int LC_TIME = 3;
  public static final int LC_COLLATE = 4;
  public static final int LC_MONETARY = 5;
  public static final int LC_ALL = 6;
  public static final int LC_MESSAGES = 7;

  public static final int STR_PAD_LEFT = 1;
  public static final int STR_PAD_RIGHT = 0;
  public static final int STR_PAD_BOTH = 2;

  private static final DecimalFormatSymbols DEFAULT_DECIMAL_FORMAT_SYMBOLS;

  private static final BigInteger BIG_TEN = new BigInteger("10");
  private static final BigInteger BIG_2_64
    = BigInteger.ONE.shiftLeft(64);

  private static final FreeList<MessageDigest> _md5FreeList
    = new FreeList<MessageDigest>(16);

  /**
   * Escapes a string using C syntax.
   *
   * @see #stripcslashes
   *
   * @param source the source string to convert
   * @param characters the set of characters to convert
   * @return the escaped string
   */
  public static StringValue addcslashes(
      Env env, StringValue source, String characters)
  {
    if (characters == null)
      characters = "";

    boolean []bitmap = parseCharsetBitmap(env, characters);

    int length = source.length();

    StringValue sb = source.createStringBuilder(length * 5 / 4);

    for (int i = 0; i < length; i++) {
      char ch = source.charAt(i);

      if (ch >= 256 || ! bitmap[ch]) {
        sb.append(ch);
        continue;
      }

      switch (ch) {
      case 0x07:
        sb.append("\\a");
        break;
      case '\b':
        sb.append("\\b");
        break;
      case '\t':
        sb.append("\\t");
        break;
      case '\n':
        sb.append("\\n");
        break;
      case 0xb:
        sb.append("\\v");
        break;
      case '\f':
        sb.append("\\f");
        break;
      case '\r':
        sb.append("\\r");
        break;
      default:
        if (ch < 0x20 || ch >= 0x7f) {
          // save as octal
          sb.append("\\");
          sb.append((char) ('0' + ((ch >> 6) & 7)));
          sb.append((char) ('0' + ((ch >> 3) & 7)));
          sb.append((char) ('0' + ((ch) & 7)));
          break;
        }
        else {
          sb.append("\\");
          sb.append(ch);
          break;
        }
      }
    }

    return sb;
  }

  /**
   * Parses the cslashes bitmap returning an actual bitmap.
   *
   * @param charset the bitmap string
   * @return  the actual bitmap
   */
  private static boolean []parseCharsetBitmap(Env env, String charset)
  {
    boolean []bitmap = new boolean[256];

    int length = charset.length();
    for (int i = 0; i < length; i++) {
      char ch = charset.charAt(i);

      // XXX: the bitmap eventual might need to deal with unicode
      if (ch >= 256)
        continue;

      bitmap[ch] = true;

      if (length <= i + 3)
        continue;

      if (charset.charAt(i + 1) != '.' || charset.charAt(i + 2) != '.')
        continue;

      char last = charset.charAt(i + 3);

      if (last < ch) {
        env.warning(L.l("character set range is invalid: {0}..{1}",
                        ch, last));
        continue;
      }

      i += 3;
      for (; ch <= last; ch++) {
        bitmap[ch] = true;
      }

      // XXX: handling of '@'?
    }

    return bitmap;
  }

  /**
   * Escapes a string for db characters.
   *
   * @param source the source string to convert
   * @return the escaped string
   */
  public static StringValue addslashes(StringValue source)
  {
    StringValue sb = source.createStringBuilder(source.length() * 5 / 4);

    int length = source.length();
    for (int i = 0; i < length; i++) {
      char ch = source.charAt(i);

      switch (ch) {
      case 0x0:
        sb.append("\\0");
        break;
      case '\'':
        sb.append("\\'");
        break;
      case '\"':
        sb.append("\\\"");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      default:
        sb.append(ch);
        break;
      }
    }

    return sb;
  }

  /**
   * Converts a binary value to a hex value.
   */
  public static StringValue bin2hex(Env env, InputStream is)
  {
    try {
      StringValue sb = env.createUnicodeBuilder();

      int ch;
      while ((ch = is.read()) >= 0) {
        int d = (ch >> 4) & 0xf;

        if (d < 10)
          sb.append((char) (d + '0'));
        else
          sb.append((char) (d + 'a' - 10));

        d = (ch) & 0xf;

        if (d < 10)
          sb.append((char) (d + '0'));
        else
          sb.append((char) (d + 'a' - 10));
      }

      return sb;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Alias of rtrim.  Removes trailing whitespace.
   *
   * @param env the quercus environment
   * @param str the string to be trimmed
   * @param charset optional set of characters to trim
   * @return the trimmed string
   */
  public static StringValue chop(Env env,
                                                 StringValue str,
                                                 @Optional String charset)
  {
    return rtrim(env, str, charset);
  }

  /**
   * converts a number to its character equivalent
   *
   * @param value the integer value
   *
   * @return the string equivalent
   */
  public static StringValue chr(Env env, long value)
  {
    if (! env.isUnicodeSemantics())
      value = value & 0xFF;

    StringValue sb = env.createUnicodeBuilder();

    sb.append((char) value);

    return sb;
  }

  /**
   * Splits a string into chunks
   *
   * @param body the body string
   * @param chunkLen the optional chunk length, defaults to 76
   * @param end the optional end value, defaults to "\r\n"
   */
  public static String chunk_split(String body,
                                   @Optional("76") int chunkLen,
                                   @Optional("\"\\r\\n\"") String end)
  {
    if (body == null)
      body = "";

    if (end == null)
      end = "";

    if (chunkLen < 1) // XXX: real exn
      throw new IllegalArgumentException(L.l("bad value {0}", chunkLen));

    StringBuilder sb = new StringBuilder();

    int i = 0;

    for (; i + chunkLen <= body.length(); i += chunkLen) {
      sb.append(body.substring(i, i + chunkLen));
      sb.append(end);
    }

    if (i < body.length()) {
      sb.append(body.substring(i));
      sb.append(end);
    }

    return sb.toString();
  }

  /**
   * Converts from one cyrillic set to another.
   *
   * This implementation does nothing, because quercus stores strings as
   * 16 bit unicode.
   */
  public static String convert_cyr_string(Env env,
                                          String str,
                                          String from,
                                          String to)
  {
    env.stub("convert_cyr_string");

    return str;
  }

  public static Value convert_uudecode(Env env, StringValue source)
  {
    try {
      int length = source.length();

      if (length == 0)
        return BooleanValue.FALSE;

      ByteToChar byteToChar = env.getByteToChar();

      int i = 0;
      while (i < length) {
        int ch1 = source.charAt(i++);

        if (ch1 == 0x60 || ch1 == 0x20)
          break;
        else if (ch1 < 0x20 || 0x5f < ch1)
          continue;

        int sublen = ch1 - 0x20;

        while (sublen > 0) {
          int code;

          code = ((source.charAt(i++) - 0x20) & 0x3f) << 18;
          code += ((source.charAt(i++) - 0x20) & 0x3f) << 12;
          code += ((source.charAt(i++) - 0x20) & 0x3f) << 6;
          code += ((source.charAt(i++) - 0x20) & 0x3f);

          byteToChar.addByte(code >> 16);

          if (sublen > 1)
            byteToChar.addByte(code >> 8);

          if (sublen > 2)
            byteToChar.addByte(code);

          sublen -= 3;
        }
      }

      return env.createString(byteToChar.getConvertedString());
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * uuencode a string.
   */
  public static Value convert_uuencode(StringValue source)
  {
    if (source.length() == 0)
      return BooleanValue.FALSE;

    StringValue result = source.createStringBuilder();

    int i = 0;
    int length = source.length();
    while (i < length) {
      int sublen = length - i;

      if (45 < sublen)
        sublen = 45;

      result.append((char) (sublen + 0x20));

      int end = i + sublen;

      while (i < end) {
        int code = source.charAt(i++) << 16;

        if (i < length)
          code += source.charAt(i++) << 8;

        if (i < length)
          code += source.charAt(i++);

        result.append(toUUChar(((code >> 18) & 0x3f)));
        result.append(toUUChar(((code >> 12) & 0x3f)));
        result.append(toUUChar(((code >> 6) & 0x3f)));
        result.append(toUUChar(((code) & 0x3f)));
      }

      result.append('\n');
    }

    result.append((char) 0x60);
    result.append('\n');

    return result;
  }
  /**
   * Returns an array of information about the characters.
   */
  public static Value count_chars(StringValue data,
                                  @Optional("0") int mode)
  {
    int []count = new int[256];

    int length = data.length();

    for (int i = 0; i < length; i++) {
      count[data.charAt(i) & 0xff] += 1;
    }

    switch (mode) {
    case 0:
      {
        ArrayValue result = new ArrayValueImpl();

        for (int i = 0; i < count.length; i++) {
          result.put(LongValue.create(i), LongValue.create(count[i]));
        }

        return result;
      }

    case 1:
      {
        ArrayValue result = new ArrayValueImpl();

        for (int i = 0; i < count.length; i++) {
          if (count[i] > 0)
            result.put(LongValue.create(i), LongValue.create(count[i]));
        }

        return result;
      }

    case 2:
      {
        ArrayValue result = new ArrayValueImpl();

        for (int i = 0; i < count.length; i++) {
          if (count[i] == 0)
            result.put(LongValue.create(i), LongValue.create(count[i]));
        }

        return result;
      }

    case 3:
      {
        StringValue sb = data.createStringBuilder();

        for (int i = 0; i < count.length; i++) {
          if (count[i] > 0)
            sb.append((char) i);
        }

        return sb;
      }

    case 4:
      {
        StringValue sb = data.createStringBuilder();

        for (int i = 0; i < count.length; i++) {
          if (count[i] == 0)
            sb.append((char) i);
        }

        return sb;
      }

    default:
      return BooleanValue.FALSE;
    }
  }

  /**
   * Calculates the crc32 value for a string
   *
   * @param str the string value
   *
   * @return the crc32 hash
   */
  public static long crc32(StringValue str)
  {
    return str.getCrc32Value();
  }

  public static String crypt(String string, @Optional String salt)
  {
    if (string == null)
      string = "";

    if (salt == null || salt.equals("")) {
      salt = ("" + Crypt.resultToChar(RandomUtil.nextInt(0x40))
          + Crypt.resultToChar(RandomUtil.nextInt(0x40)));
    }

    return Crypt.crypt(string, salt);
  }

  // XXX: echo

  /**
   * Explodes a string into an array
   *
   * @param separator the separator string
   * @param string the string to be exploded
   * @param limit the max number of elements
   * @return an array of exploded values
   */
  public static Value explode(Env env,
                              StringValue separator,
                              StringValue string,
                              @Optional("0x7fffffff") long limit)
  {
    if (separator.length() == 0) {
      env.warning(L.l("Delimiter is empty"));
      return BooleanValue.FALSE;
    }
    
    int head = 0;    
    ArrayValue array = new ArrayValueImpl();

    int separatorLength = separator.length();
    int stringLength = string.length();
    long ulimit;
    
    if (limit >= 0) {
      ulimit = limit;      
    } else {
      ulimit = 0x7fffffff;
    }

    for (int i = 0; i < stringLength; ++i) {
      
      if (ulimit <= array.getSize() + 1) {
        break;
      }
      
      if (string.regionMatches(i, separator, 0, separatorLength)) {

        StringValue chunk = string.substring(head, i);
        array.append(chunk);
        
        head = i + separatorLength;
        i = head - 1;
      }
    }

    StringValue chunk = string.substring(head);

    array.append(chunk);
    
    while (array.getSize() > 0 && limit++ < 0) {
      array.pop(env);
    }

    return array;
  }

  /**
   * Use printf style formatting to write a string to a file.
   * @param fd the file to write to
   * @param format the format string
   * @param args the valujes to apply to the format string
   */
  public static Value fprintf(Env env,
                              @NotNull BinaryOutput os,
                              StringValue format,
                              Value []args)
  {
    Value value = sprintf(env, format, args);

    return FileModule.fwrite(env, os, value.toInputStream(),
                             Integer.MAX_VALUE);
  }

  /**
   * implodes an array into a string
   *
   * @param glueV the separator string
   * @param piecesV the array to be imploded
   *
   * @return a string of imploded values
   */
  public static Value implode(Env env,
                              Value glueV,
                              @Optional Value piecesV)
  {
    StringValue glue;
    ArrayValue pieces;

    if ((piecesV.isArray() && glueV.isArray())
         || glueV.isArray()) {
      pieces = glueV.toArrayValue(env);
      glue = piecesV.toStringValue();   
    }
    else if (piecesV.isArray()) {
      pieces = piecesV.toArrayValue(env);
      glue = glueV.toStringValue();
    }
    else {
      env.warning(L.l("neither argument to implode is an array: {0}, {1}",
                    glueV.getClass().getName(), piecesV.getClass().getName()));

      return NullValue.NULL;
    }

    StringValue sb = glue.createStringBuilder();
    boolean isFirst = true;

    Iterator<Value> iter = pieces.getValueIterator(env);

    while (iter.hasNext()) {
      if (! isFirst)
        sb = sb.append(glue);

      isFirst = false;

      sb = sb.append(iter.next());
    }

    return sb;
  }

  /**
   * implodes an array into a string
   *
   * @param glueV the separator string
   * @param piecesV the array to be imploded
   *
   * @return a string of imploded values
   */
  public static Value join(Env env,
                           Value glueV,
                           Value piecesV)
  {
    return implode(env, glueV, piecesV);
  }

  // XXX: lcfirst
  // XXX: levenshtein

  /**
   * Gets locale-specific symbols.
   * XXX: locale charset
   */
  public static ArrayValue localeconv(Env env)
  {
    ArrayValueImpl array = new ArrayValueImpl();

    QuercusLocale money = env.getLocaleInfo().getMonetary();

    Locale locale = money.getLocale();

    DecimalFormatSymbols decimal = new DecimalFormatSymbols(locale);
    Currency currency = NumberFormat.getInstance(locale).getCurrency();

    array.put(env.createString("decimal_point"),
              env.createString(decimal.getDecimalSeparator()));
    array.put(env.createString("thousands_sep"),
              env.createString(decimal.getGroupingSeparator()));
    //array.put("grouping", "");
    array.put(env.createString("int_curr_symbol"),
              env.createString(decimal.getInternationalCurrencySymbol()));
    array.put(env.createString("currency_symbol"),
              env.createString(decimal.getCurrencySymbol()));
    array.put(env.createString("mon_decimal_point"),
              env.createString(decimal.getMonetaryDecimalSeparator()));
    array.put(env.createString("mon_thousands_sep"),
              env.createString(decimal.getGroupingSeparator()));
    //array.put("mon_grouping", "");
    array.put(env.createString("positive_sign"), env.getEmptyString());
    array.put(env.createString("negative_sign"),
              env.createString(decimal.getMinusSign()));
    array.put(env.createString("int_frac_digits"),
              LongValue.create(currency.getDefaultFractionDigits()));
    array.put(env.createString("frac_digits"),
              LongValue.create(currency.getDefaultFractionDigits()));
    //array.put("p_cs_precedes", "");
    //array.put("p_sep_by_space", "");
    //array.put("n_cs_precedes", "");
    //array.put("n_sep_by_space", "");
    //array.put("p_sign_posn", "");
    //array.put("n_sign_posn", "");

    return array;
  }

  /**
   * Removes leading whitespace.
   *
   * @param string the string to be trimmed
   * @param characters optional set of characters to trim
   * @return the trimmed string
   */
  public static StringValue ltrim(Env env,
                                  StringValue string,
                                  @Optional String characters)
  {
    if (characters == null)
      characters = "";

    boolean []trim;

    if (characters.equals(""))
      trim = TRIM_WHITESPACE;
    else
      trim = parseCharsetBitmap(env, characters);

    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);

      if (ch >= 256 || ! trim[ch]) {
        if (i == 0)
          return string;
        else
          return string.substring(i);
      }
    }

    return env.getEmptyString();
  }

  /**
   * returns the md5 hash
   *
   * @param source the string
   * @param rawOutput if true, return the raw binary
   *
   * @return a string of imploded values
   */
  public static Value md5(Env env,
                          InputStream is,
                                          @Optional boolean rawOutput)
  {
    try {
      MessageDigest md = _md5FreeList.allocate();

      if (md == null)
        md = MessageDigest.getInstance("MD5");

      md.reset();

      // XXX: iso-8859-1

      int ch;
      while ((ch = is.read()) >= 0) {
        md.update((byte) ch);
      }

      byte []digest = md.digest();

      _md5FreeList.free(md);

      return hashToValue(env, digest, rawOutput);
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * returns the md5 hash
   *
   * @param source the string
   * @param rawOutput if true, return the raw binary
   *
   * @return a string of imploded values
   */
  public static Value md5_file(Env env,
                               Path source,
                               @Optional boolean rawOutput)
  {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      InputStream is = null;

      try {
        is = source.openRead();
        int d;

        while ((d = is.read()) >= 0) {
          md.update((byte) d);
        }

        byte []digest = md.digest();

        return hashToValue(env, digest, rawOutput);

      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);

        return BooleanValue.FALSE;
      } finally {
        try {
          if (is != null)
            is.close();
        } catch (IOException e) {
        }
      }
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  private static StringValue digestToString(Env env, byte []digest)
  {
    StringValue sb = env.createUnicodeBuilder();
    for (int i = 0; i < digest.length; i++) {
      int d1 = (digest[i] >> 4) & 0xf;
      int d2 = (digest[i] & 0xf);

      sb.append(toHexChar(d1));
      sb.append(toHexChar(d2));
    }

    return sb;
  }

  /**
   * Returns the metaphone of a string.
   * This implementation produces identical results to the php version,
   * which does contain some bugs.
   */
  public static String metaphone(String string)
  {
    if (string == null)
      string = "";

    int length = string.length();
    int index = 0;
    char ch = 0;

    // ignore everything up until first letter
    for (; index < length; index++) {
      ch = toUpperCase(string.charAt(index));

      if ('A' <= ch && ch <= 'Z')
        break;
    }

    if (index == length)
      return "";

    int lastIndex = length - 1;

    StringBuilder result = new StringBuilder(length);

    // special case first letter

    char nextCh
      = index < lastIndex
      ? toUpperCase(string.charAt(index + 1))
      : 0;

    switch (ch) {
      case 'A':
        if (nextCh == 'E') {
          result.append('E');
          index += 2;
        }
        else {
          result.append('A');
          index += 1;
        }

        break;

      case 'E':
      case 'I':
      case 'O':
      case 'U':
        result.append(ch);
        index += 1;
        break;

      case 'G':
      case 'K':
      case 'P':
        if (nextCh == 'N') {
          result.append('N');
          index += 2;
        }

        break;

      case 'W':
        if (nextCh == 'H' || nextCh == 'R') {
          result.append(nextCh);
          index += 2;
        }
        else {
          switch (nextCh) {
            case 'A':
            case 'E':
            case 'I':
            case 'O':
            case 'U':
              result.append('W');
              index += 2;
              break;
            default:
              break;
          }
        }

        break;

      case 'X':
        result.append('S');
        index += 1;
        break;

      default:
        break;
    }

    // the rest of the letters

    char prevCh;

    for (; index < length; index++) {

      if (index > 0)
        prevCh = toUpperCase(string.charAt(index - 1));
      else
        prevCh = 0;

      ch = toUpperCase(string.charAt(index));

      if (ch < 'A' || ch > 'Z')
        continue;

      if (ch == prevCh && ch != 'C')
        continue;

      if (index + 1 < length)
        nextCh = toUpperCase(string.charAt(index + 1));
      else
        nextCh = 0;

      char nextnextCh;

      if (index + 2 < length)
        nextnextCh = toUpperCase(string.charAt(index + 2));
      else
        nextnextCh = 0;


      switch (ch) {
        case 'B':
          if (prevCh != 'M')
            result.append('B');
          break;

        case 'C':
            switch (nextCh) {
              case 'E':
              case 'I':
              case 'Y':
                // makesoft
                if (nextCh == 'I' && nextnextCh == 'A') {
                  result.append('X');
                }
                else if (prevCh == 'S') {
                }
                else {
                  result.append('S');
                }
                break;
              default:
                if (nextCh == 'H') {
                  result.append('X');
                  index++;
                }
                else {
                  result.append('K');
                }
                break;
            }

          break;

        case 'D':
          if (nextCh == 'G') {
            switch (nextnextCh) {
              case 'E':
              case 'I':
              case 'Y':
                // makesoft
                result.append('J');
                index++;
                break;
              default:
                result.append('T');
                break;
            }
          }
          else
            result.append('T');

          break;

        case 'G':
          if (nextCh == 'H') {
            boolean isSilent = false;

            if (index - 3 >= 0) {
              char prev3Ch = toUpperCase(string.charAt(index - 3));
              switch (prev3Ch) {
                // noghtof
                case 'B':
                case 'D':
                case 'H':
                  isSilent = true;
                  break;
                default:
                  break;
              }
            }

            if (!isSilent) {
              if (index - 4 >= 0) {
                char prev4Ch = toUpperCase(string.charAt(index - 4));

                isSilent = (prev4Ch == 'H');
              }
            }

            if (!isSilent) {
              result.append('F');
              index++;
            }
          }
          else if (nextCh == 'N') {
            char nextnextnextCh;

            if (index + 3 < length)
              nextnextnextCh = toUpperCase(string.charAt(index + 3));
            else
              nextnextnextCh = 0;

            if (nextnextCh < 'A' || nextnextCh > 'Z') {
            }
            else if (nextnextCh == 'E' && nextnextnextCh == 'D') {
            }
            else
              result.append('K');
          }
          else if (prevCh == 'G') {
            result.append('K');
          }
          else {
            switch (nextCh) {
              case 'E':
              case 'I':
              case 'Y':
                // makesoft
                result.append('J');
                break;
              default:
                result.append('K');
                break;
            }
          }

          break;

        case 'H':
        case 'W':
        case 'Y':
          switch (nextCh) {
            case 'A':
            case 'E':
            case 'I':
            case 'O':
            case 'U':
              // followed by a vowel

              if (ch == 'H') {
                switch (prevCh) {
                  case 'C':
                  case 'G':
                  case 'P':
                  case 'S':
                  case 'T':
                    // affecth
                    break;
                  default:
                    result.append('H');
                    break;
                }
              }
              else
                result.append(ch);

              break;
            default:
              // not followed by a vowel
              break;
          }

          break;

        case 'K':
          if (prevCh != 'C')
            result.append('K');

          break;

        case 'P':
          if (nextCh == 'H')
            result.append('F');
          else
            result.append('P');

          break;

        case 'Q':
          result.append('K');
          break;

        case 'S':
          if (nextCh == 'I' && (nextnextCh == 'O' || nextnextCh == 'A')) {
            result.append('X');
          }
          else if (nextCh == 'H') {
            result.append('X');
            index++;
          }
          else
            result.append('S');

          break;

        case 'T':
          if (nextCh == 'I' && (nextnextCh == 'O' || nextnextCh == 'A')) {
            result.append('X');
          }
          else if (nextCh == 'H') {
            result.append('0');
            index++;
          }
          else
            result.append('T');

          break;

        case 'V':
          result.append('F');

          break;

        case 'X':
          result.append('K');
          result.append('S');
          break;

        case 'Z':
          result.append('S');
          break;

        case 'F':
        case 'J':
        case 'L':
        case 'M':
        case 'N':
        case 'R':
          result.append(ch);
          break;

        default:
          break;
      }
    }

    return result.toString();
  }

  /**
   * Returns a formatted money value.
   * XXX: locale charset
   *
   * @param format the format
   * @param value the value
   *
   * @return a string of formatted values
   */
  public static String money_format(Env env, String format, double value)
  {
    QuercusLocale monetary = env.getLocaleInfo().getMonetary();

    Locale locale = monetary.getLocale();

    return NumberFormat.getCurrencyInstance(locale).format(value);
  }

  // XXX: nl_langinfo
  // XXX: nl2br

  /**
   * Returns a formatted number.
   *
   * @param value the value
   * @param decimals the number of decimals
   * @param pointValue the decimal point string
   * @param groupValue the thousands separator
   *
   * @return a string of the formatted number
   */
  public static String number_format(Env env,
                                     double value,
                                     @Optional int decimals,
                                     @Optional Value pointValue,
                                     @Optional Value groupValue)
  {
    boolean isGroupDefault = (groupValue instanceof DefaultValue);
    boolean isPointDefault = (pointValue instanceof DefaultValue);

    if  (!isPointDefault && isGroupDefault) {
      env.warning(L.l("wrong parameter count"));
      return null;
    }

    String pattern;

    char point = '.';

    if (! pointValue.isNull()) {
      String pointString = pointValue.toString();

      point =  (pointString.length() == 0) ? 0 : pointString.charAt(0);
    }

    char group = ',';

    if (! groupValue.isNull()) {
      String groupString = groupValue.toString();

      group = (groupString.length() == 0) ? 0 : groupString.charAt(0);
    }

    if (decimals > 0) {
      StringBuilder patternBuilder = new StringBuilder(6 + decimals);

      patternBuilder.append(group == 0 ? "###0." : "#,##0.");

      for (int i = 0; i < decimals; i++)
        patternBuilder.append('0');

      pattern = patternBuilder.toString();
    }
    else {
      pattern = group == 0 ? "###0" : "#,##0";
    }

    DecimalFormatSymbols decimalFormatSymbols;

    if (point == '.' && group == ',')
      decimalFormatSymbols = DEFAULT_DECIMAL_FORMAT_SYMBOLS;
    else {
      decimalFormatSymbols = new DecimalFormatSymbols();
      decimalFormatSymbols.setDecimalSeparator(point);
      decimalFormatSymbols.setGroupingSeparator(group);
      decimalFormatSymbols.setZeroDigit('0');
    }

    DecimalFormat format = new DecimalFormat(pattern, decimalFormatSymbols);

    String result = format.format(value);

    if (point == 0 && decimals > 0) {
      // no way to get DecimalFormat to output nothing for the point,
      // so remove it here
      int i = result.lastIndexOf(point);

      return result.substring(0, i) + result.substring(i + 1, result.length());
    }
    else
      return result;
  }

 /**
   * Converts the first character to an integer.
   *
   * @param string the string to be converted
   *
   * @return the integer value
   */
  public static long ord(StringValue string)
  {
    if (string.length() == 0)
      return 0;
    else {
      return string.charAt(0);
    }
  }

  /**
   * Parses the string as a query string.
   *
   * @param env the calling environment
   * @param str the query string
   * @param array the optional result array
   */
  @UsesSymbolTable
  public static Value parse_str(Env env, StringValue str,
                                @Optional @Reference Value ref)
  {
    boolean isRef = ref instanceof Var;

    ArrayValue result = null;

    if (isRef) {
      result = new ArrayValueImpl();
      ref.set(result);
    }
    else if (ref instanceof ArrayValue) {
      result = (ArrayValue) ref;
      isRef = true;
    }
    else
      result = new ArrayValueImpl();

    return StringUtility.parseStr(env,
                                  str,
                                  result,
                                  isRef,
                                  env.getHttpInputEncoding());
  }

  /**
   * Prints the string.
   *
   * @param env the quercus environment
   * @param value the string to print
   */
  public static long print(Env env, Value value)
  {
    value.print(env);

    return 1;
  }

  /**
   * print to the output with a formatter
   *
   * @param env the quercus environment
   * @param format the format string
   * @param args the format arguments
   *
   * @return the formatted string
   */
  public static int printf(Env env, StringValue format, Value []args)
  {
    Value str = sprintf(env, format, args);

    str.print(env);

    return str.length();
  }

  /**
   * Converts a RFC2045 quoted printable string to a string.
   */
  // XXX: i18n
  public static String quoted_printable_decode(String str)
  {
    if (str == null)
      str = "";

    StringBuilder sb = new StringBuilder();

    int length = str.length();

    for (int i = 0; i < length; i++) {
      char ch = str.charAt(i);

      if (33 <= ch && ch <= 60)
        sb.append(ch);
      else if (62 <= ch && ch <= 126)
        sb.append(ch);
      else if (ch == ' ' || ch == '\t') {
        if (i + 1 < str.length()
            && (str.charAt(i + 1) == '\r' || str.charAt(i + 1) == '\n')) {
          sb.append('=');
          sb.append(toUpperHexChar(ch >> 4));
          sb.append(toUpperHexChar(ch));
        }
        else
          sb.append(ch);
      }
      else if (ch == '\r' || ch == '\n') {
        sb.append(ch);
      }
      else {
        sb.append('=');
        sb.append(toUpperHexChar(ch >> 4));
        sb.append(toUpperHexChar(ch));
      }
    }

    return sb.toString();
  }

  /**
   * Escapes meta characters.
   *
   * @param string the string to be quoted
   *
   * @return the quoted
   */
  public static Value quotemeta(StringValue string)
  {
    int len = string.length();

    StringValue sb = string.createStringBuilder(len * 5 / 4);

    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case '.': case '\\': case '+': case '*': case '?':
      case '[': case '^': case ']': case '(': case ')': case '$':
        sb.append("\\");
        sb.append(ch);
        break;
      default:
        sb.append(ch);
        break;
      }
    }

    return sb;
  }

  private static final boolean[]TRIM_WHITESPACE = new boolean[256];

  static {
    TRIM_WHITESPACE['\0'] = true;
    TRIM_WHITESPACE['\b'] = true;
    TRIM_WHITESPACE[' '] = true;
    TRIM_WHITESPACE['\t'] = true;
    TRIM_WHITESPACE['\r'] = true;
    TRIM_WHITESPACE['\n'] = true;
    TRIM_WHITESPACE[0x0B] = true;
  }

  /**
   * Removes trailing whitespace.
   *
   * @param env the quercus environment
   * @param string the string to be trimmed
   * @param characters optional set of characters to trim
   * @return the trimmed string
   */
  public static StringValue rtrim(Env env,
                                  StringValue string,
                                  @Optional String characters)
  {
    if (characters == null)
      characters = "";

    boolean []trim;

    if (characters.equals(""))
      trim = TRIM_WHITESPACE;
    else
      trim = parseCharsetBitmap(env, characters);

    for (int i = string.length() - 1; i >= 0; i--) {
      char ch = string.charAt(i);

      if (ch >= 256 || ! trim[ch]) {
        if (i == string.length())
          return string;
        else
          return (StringValue) string.subSequence(0, i + 1);
      }
    }

    return env.getEmptyString();
  }

  /**
   * Sets locale configuration.
   */
  public static Value setlocale(Env env,
                                int category,
                                Value localeArg,
                                Value []fallback)
  {
    LocaleInfo localeInfo = env.getLocaleInfo();

    if (localeArg instanceof ArrayValue) {
      for (Value value : ((ArrayValue) localeArg).values()) {
        QuercusLocale locale = setLocale(localeInfo,
                                         category,
                                         value.toString());

        if (locale != null)
          return env.createString(locale.toString());
      }
    }
    else {
      QuercusLocale locale = setLocale(localeInfo,
                                       category,
                                       localeArg.toString());

      if (locale != null)
        return env.createString(locale.toString());
    }

    for (int i = 0; i < fallback.length; i++) {
      QuercusLocale locale = setLocale(localeInfo,
                                       category,
                                       fallback[i].toString());

      if (locale != null)
        return env.createString(locale.toString());
    }

    return BooleanValue.FALSE;
  }

  /**
   * Sets locale configuration.
   */
  private static QuercusLocale setLocale(LocaleInfo localeInfo,
                                         int category,
                                         String localeName)
  {
    QuercusLocale locale = findLocale(localeName);

    if (locale == null)
      return null;

    switch (category) {
    case LC_ALL:
      localeInfo.setAll(locale);
      return localeInfo.getMessages();
    case LC_COLLATE:
      localeInfo.setCollate(locale);
      return localeInfo.getCollate();
    case LC_CTYPE:
      localeInfo.setCtype(locale);
      return localeInfo.getCtype();
    case LC_MONETARY:
      localeInfo.setMonetary(locale);
      return localeInfo.getMonetary();
    case LC_NUMERIC:
      localeInfo.setNumeric(locale);
      return localeInfo.getNumeric();
    case LC_TIME:
      localeInfo.setTime(locale);
      return localeInfo.getTime();
    case LC_MESSAGES:
      localeInfo.setMessages(locale);
      return localeInfo.getMessages();
    default:
      return null;
    }
  }

  /*
   * Example locale: fr_FR.UTF-8@euro, french (on Windows)
   * (French, France, UTF-8, with euro currency support)
   */
  private static QuercusLocale findLocale(String localeName)
  {
    String language;
    String country;
    String charset = null;
    String variant = null;

    CharBuffer sb = CharBuffer.allocate();

    int len = localeName.length();
    int i = 0;

    char ch = 0;
    while (i < len && (ch = localeName.charAt(i++)) != '-' && ch != '_') {
      sb.append(ch);
    }

    language = sb.toString();
    sb.clear();

    while (i < len && (ch = localeName.charAt(i)) != '.' && ch != '@') {
      sb.append(ch);

      i++;
    }

    if (ch == '.')
      i++;

    country = sb.toString();
    sb.clear();

    while (i < len && (ch = localeName.charAt(i)) != '@') {
      sb.append(ch);

      i++;
    }

    if (sb.length() > 0)
      charset = sb.toString();

    if (i + 1 < len)
      variant = localeName.substring(i + 1);

    Locale locale;

    // java versions >= 1.5 should automatically use the euro sign
    if (variant != null && ! variant.equalsIgnoreCase("euro"))
      locale = new Locale(language, country, variant);
    else if (country != null)
      locale = new Locale(language, country);
    else
      locale = new Locale(language);

    if (isValidLocale(locale))
      return new QuercusLocale(locale, charset);
    else
      return null;
  }

  /**
   * Returns true if the locale is supported.
   */
  private static boolean isValidLocale(Locale locale)
  {
    Locale []validLocales = Locale.getAvailableLocales();

    for (int i = 0; i < validLocales.length; i++) {
      if (validLocales[i].equals(locale)) {
        return true;
      }
    }

    return false;
  }

  /**
   * returns the md5 hash
   *
   * @param source the string
   * @param rawOutput if true, return the raw binary
   *
   * @return a string of imploded values
   */
  public static Value sha1(Env env,
                           String source,
                           @Optional boolean rawOutput)
  {
    if (source == null)
      source = "";

    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");

      // XXX: iso-8859-1

      for (int i = 0; i < source.length(); i++) {
        char ch = source.charAt(i);

        md.update((byte) ch);
      }

      byte []digest = md.digest();

      return hashToValue(env, digest, rawOutput);
    } catch (Exception e) {
      throw new QuercusException(e);
    }
  }

  /**
   * returns the md5 hash
   *
   * @param source the string
   * @param rawOutput if true, return the raw binary
   *
   * @return a string of imploded values
   */
  public static Value sha1_file(Env env,
                                Path source,
                                @Optional boolean rawOutput)
  {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      InputStream is = null;

      try {
        is = source.openRead();
        int d;

        while ((d = is.read()) >= 0) {
          md.update((byte) d);
        }

        byte []digest = md.digest();

        return hashToValue(env, digest, rawOutput);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);

        return BooleanValue.FALSE;
      } finally {
        try {
          if (is != null)
            is.close();
        } catch (IOException e) {
        }
      }
    } catch (Exception e) {
      throw new QuercusException(e);
    }
  }

  private static Value hashToValue(Env env, byte []bytes, boolean isBinary)
  {
    if (isBinary) {
      StringValue v = env.createBinaryBuilder();
      v.append(bytes, 0, bytes.length);
      return v;
    }
    else {
      StringValue v = env.createUnicodeBuilder();

      for (int i = 0; i < bytes.length; i++) {
    int ch = bytes[i];
    int d1 = (ch >> 4) & 0xf;
    int d2 = (ch) & 0xf;

    if (d1 < 10)
      v.append((char) ('0' + d1));
    else
      v.append((char) ('a' + d1 - 10));

    if (d2 < 10)
      v.append((char) ('0' + d2));
    else
      v.append((char) ('a' + d2 - 10));
      }

      return v;
    }
  }

  // XXX: similar_text

  private static final char []SOUNDEX_VALUES = "01230120022455012623010202"
      .toCharArray();

  public static Value soundex(StringValue string)
  {
    int length = string.length();

    if (length == 0)
      return BooleanValue.FALSE;

    StringValue result = string.createStringBuilder();

    int count = 0;
    char lastCode = 0;


    for (int i = 0; i < length && count < 4; i++) {
      char ch = toUpperCase(string.charAt(i));

      if ('A' <= ch  && ch <= 'Z') {
        char code = SOUNDEX_VALUES[ch - 'A'];

        if (count == 0) {
          result.append(ch);
          count++;
        }
        else if (code != '0' && code != lastCode) {
          result.append(code);
          count++;
        }

        lastCode = code;
      }
    }

    for (; count < 4; count++) {
      result.append('0');
    }

    return result;
  }

  /**
   * Print to a string with a formatter
   *
   * @param format the format string
   * @param args the format arguments
   *
   * @return the formatted string
   */
  public static Value sprintf(Env env, StringValue format, Value []args)
  {
    ArrayList<PrintfSegment> segments = parsePrintfFormat(env, format);

    StringValue sb = format.createStringBuilder();

    for (PrintfSegment segment : segments) {
      if (! segment.apply(env, sb, args))
        return BooleanValue.FALSE;
    }

    return sb;
  }

  private static ArrayList<PrintfSegment> parsePrintfFormat(Env env,
                                                            StringValue format)
  {
    ArrayList<PrintfSegment> segments = new ArrayList<PrintfSegment>();

    StringBuilder sb = new StringBuilder();
    StringBuilder flags = new StringBuilder();

    int length = format.length();
    int index = 0;

    for (int i = 0; i < length; i++) {
      char ch = format.charAt(i);

      if (i + 1 < length && ch == '%') {
        // The C printf silently ignores invalid flags, so we need to
        // remove them if present.

        sb.append(ch);

        boolean isLeft = false;
        boolean isAlt = false;
        
        boolean isShowSign = false;
        
        int argIndex = -1;
        int leftPadLength = 0;
        int width = 0;
        
        int padChar = -1;

        flags.setLength(0);

        int j = i + 1;

        loop:
        for (; j < length; j++) {
          ch = format.charAt(j);
          
          switch (ch) {
          case '-':
            isLeft = true;
            
            if (j + 1 < length && format.charAt(j + 1) == '0') {
              padChar = '0';
              j++;
            }
            
            /*
            for (int k = j + 1; k < length; k++) {
              char digit = format.charAt(k);
              
              if ('0' <= digit && digit <= '9') {
                leftPadLength = leftPadLength * 10 + digit - '0';
                
                j++;
              }
              else
                break;
            }
            */
            
            break;
          case '#':
            isAlt = true;
            break;
          case '0':
            if (padChar < 0)
              padChar = '0';
            else {
              int value = 0;
              
              for (int k = j + 1; k < length; k++) {
                char digit = format.charAt(k);
                
                if ('0' <= digit && digit <= '9') {
                  value = value * 10 + digit - '0';
                  j++;
                }
                else
                  break;
              }
              
              if (j + 1 < length && format.charAt(j + 1) == '$') {
                argIndex = value - 1;
                j++;
              }
              else {
                width = value;
              }
            }

            break;
          case '1': case '2': case '3': case '4': case '5':
          case '6': case '7': case '8': case '9':
            
            int value = ch - '0';

            for (int k = j + 1; k < length; k++) {
              char digit = format.charAt(k);
              
              if ('0' <= digit && digit <= '9') {
                value = value * 10 + digit - '0';
                j++;
              }
              else
                break;
            }

            if (j + 1 < length && format.charAt(j + 1) == '$') {
              argIndex = value - 1;
              j++;
            }
            else {
              width = value;
            }
            
            break;
          case '\'':
            padChar = format.charAt(j + 1);
            j += 1;
            break;
          case '+':
            isShowSign = true;
            break;
          case ' ': case ',': case '(':
            flags.append(ch);
            break;
          default:
            break loop;
          }
        }

        int head = j;
        
        if (argIndex < 0)
          argIndex = index;
        
        loop:
        for (; j < length; j++) {
          ch = format.charAt(j);

          switch (ch) {
          case '%':
            i = j;
            segments.add(new TextPrintfSegment(sb));
            sb.setLength(0);
            break loop;

          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9':
          case '.': case '$':
            break;

          case 's': case 'S':
            sb.setLength(sb.length() - 1);
            
            if (width <= 0 && 0 < leftPadLength)
              width = leftPadLength;
            
            index++;

              segments.add(new StringPrintfSegment(
                  sb,
                  isLeft || isAlt,
                  padChar,
                  ch == 'S',
                  width,
                  format.substring(head, j).toString(),
                  argIndex));
              sb.setLength(0);
            i = j;
            break loop;

          case 'c': case 'C':
            sb.setLength(sb.length() - 1);
            
            if (width <= 0 && 0 < leftPadLength)
              width = leftPadLength;
            
            index++;

              segments.add(new CharPrintfSegment(
                  sb,
                  isLeft || isAlt,
                  padChar,
                  ch == 'C',
                  width,
                  format.substring(head, j).toString(),
                  argIndex));
              sb.setLength(0);
            i = j;
            break loop;

            /*
          case 'u':
            sb.setLength(sb.length() - 1);
            if (sb.length() > 0)
              segments.add(new TextPrintfSegment(sb));
            sb.setLength(0);

            if (isLeft)
              sb.append('-');
            if (isAlt)
              sb.append('#');
            sb.append(flags);

            sb.append(format, head, j);

            sb.append(ch);

            //segments.add(UnsignedLongPrintfSegment.create(
            env, sb.toString(), index++));
            sb.setLength(0);
            i = j;
            break loop;
            */

          case 'i':
            ch = 'd';
          case 'd': case 'x': case 'o': case 'X':
          case 'b': case 'B': case 'u':
            sb.setLength(sb.length() - 1);
            if (sb.length() > 0)
              segments.add(new TextPrintfSegment(sb));
            sb.setLength(0);

            if (isAlt)
              sb.append('#');

            if (isShowSign)
              sb.append('+');
            
            sb.append(flags);
            
            if (width > 0) {
              if (isLeft)
                sb.append('-');
              else if (padChar == '0')
                sb.append('0');

              sb.append(width);
            }

            sb.append(format, head, j);
            sb.append(ch);
            
            index++;
            
            segments.add(
                LongPrintfSegment.create(env, sb.toString(), argIndex));
            sb.setLength(0);
            i = j;
            break loop;

          case 'e': case 'E': case 'f': case 'g': case 'G':
          case 'F':
            QuercusLocale locale = null;

            if (ch == 'F')
              ch = 'f';
            else
              locale = env.getLocaleInfo().getNumeric();

            sb.setLength(sb.length() - 1);
            if (sb.length() > 0)
              segments.add(new TextPrintfSegment(sb));
            sb.setLength(0);
            
            if (isAlt)
              sb.append('#');

            if (isShowSign)
              sb.append('+');
            
            sb.append(flags);
            
            if (width > 0) {
              if (isLeft)
                sb.append('-');
              else if (padChar == '0')
                sb.append('0');
              
              // '-' and '0' together is not supported by java.util.Formatter
              //if (padChar == '0')
              //  sb.append((char) padChar);
              
              sb.append(width);
            }

            sb.append(format, head, j);
            sb.append(ch);

            index++;
       
            segments.add(new DoublePrintfSegment(sb.toString(),
                                                 isLeft && padChar == '0',
                                                 argIndex,
                                                 locale));
            sb.setLength(0);
            i = j;
            break loop;

          default:
            if (isLeft)
              sb.append('-');
            if (isAlt)
              sb.append('#');
            sb.append(flags);
            sb.append(format, head, j);
            sb.append(ch);
            i = j;
            break loop;
          }
        }
      } else
        sb.append(ch);
    }

    if (sb.length() > 0)
      segments.add(new TextPrintfSegment(sb));

    return segments;
  }

  /**
   * scans a string
   *
   * @param format the format string
   * @param args the format arguments
   *
   * @return the formatted string
   */
  public static Value sscanf(Env env,
                             StringValue string,
                             StringValue format,
                             @Optional @Reference Value []args)
  {
    ScanfSegment[] formatArray = sscanfParseFormat(env, format);

    int strlen = string.length();

    int sIndex = 0;

    boolean isReturnArray = args.length == 0;
    int argIndex = 0;

    if (strlen == 0) {
      return isReturnArray ? NullValue.NULL : LongValue.MINUS_ONE;
    }

    ArrayValue array = new ArrayValueImpl();

    for (int i = 0; i < formatArray.length; i++) {
      ScanfSegment segment = formatArray[i];

      Value var;

      if (! segment.isAssigned()) {
        var = null;
      } else if (isReturnArray) {
        var = array;
      } else {
        if (argIndex < args.length) {
          var = args[argIndex];
          
          if (sIndex < strlen)
            argIndex++;
          
        }
        else {
          env.warning(L.l("not enough vars passed in"));

          var = NullValue.NULL;
        }
      }

      sIndex = segment.apply(string,
                             strlen,
                             sIndex,
                             var,
                             isReturnArray);
 
      if (sIndex < 0) {
        if (isReturnArray)
          return sscanfFillNull(array, formatArray, i);
        else
          return LongValue.create(argIndex);
      }
      /*
      else if (sIndex == strlen) {
        if (isReturnArray)
          return sscanfFillNull(array, formatArray, i + 1);
        else
          return LongValue.create(argIndex);
      }
      */
    }

    return sscanfReturn(env, array, args, argIndex, isReturnArray, false);
  }

  private static Value sscanfFillNull(
      ArrayValue array, ScanfSegment[] formatArray, int fIndex)
  {
    for (; fIndex < formatArray.length; fIndex++) {
      ScanfSegment segment = formatArray[fIndex];

      if (segment.isAssigned())
        array.put(NullValue.NULL);
    }

    return array;
  }

  /**
   * scans a string
   *
   * @param format the format string
   * @param args the format arguments
   *
   * @return the formatted string
   */
  private static ScanfSegment[] sscanfParseFormat(Env env,
                                                  StringValue format)
  {
    int fmtLen = format.length();
    int fIndex = 0;

    ArrayList<ScanfSegment> segmentList = new ArrayList<ScanfSegment>();
    StringBuilder sb = new StringBuilder();

    while (fIndex < fmtLen) {
      char ch = format.charAt(fIndex++);

      if (isWhitespace(ch)) {
        for (;
             (fIndex < fmtLen
              && isWhitespace(ch = format.charAt(fIndex)));
             fIndex++) {
        }

        scanfAddConstant(segmentList, sb);

        segmentList.add(ScanfWhitespace.SEGMENT);
      }
      else if (ch == '%') {
        int maxLen = -1;

        loop:
        while (fIndex < fmtLen) {
          ch = format.charAt(fIndex++);

          switch (ch) {
          case '%':
            sb.append('%');
            break loop;

          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9':
            if (maxLen < 0)
              maxLen = 0;

            maxLen = 10 * maxLen + ch - '0';
            break;

          case 's':
          {
            scanfAddConstant(segmentList, sb);
            segmentList.add(new ScanfString(maxLen));
            break loop;
          }

          case 'c':
          {
            if (maxLen < 0)
              maxLen = 1;

            scanfAddConstant(segmentList, sb);
            segmentList.add(new ScanfString(maxLen));

            break loop;
          }
          case 'n':
          {
            scanfAddConstant(segmentList, sb);
            segmentList.add(ScanfStringLength.SEGMENT);

            break loop;
          }
          case 'd':
          {
            scanfAddConstant(segmentList, sb);
            segmentList.add(new ScanfInteger(maxLen, 10, false));
            break loop;
          }
          case 'u':
          {
            scanfAddConstant(segmentList, sb);
            segmentList.add(new ScanfInteger(maxLen, 10, true));
            break loop;
          }
          case 'o':
          {
            scanfAddConstant(segmentList, sb);
            segmentList.add(new ScanfInteger(maxLen, 8, false));
            break loop;
          }
          case 'x': case 'X':
          {
            scanfAddConstant(segmentList, sb);
            segmentList.add(new ScanfHex(maxLen));

            break loop;
          }
          case 'e': case 'f':
          {
            scanfAddConstant(segmentList, sb);
            segmentList.add(new ScanfScientific(maxLen));
            break loop;
          }
          case '[':
          {
            scanfAddConstant(segmentList, sb);
            
            if (fmtLen <= fIndex) {
              env.warning(L.l("expected ']', saw end of string"));
              break loop;
            }
            
            boolean isNegated = false;

            if (fIndex < fmtLen
                && format.charAt(fIndex) == '^') {
              isNegated = true;
              fIndex++;
            }

            IntSet set = new IntSet();
            
            while (true) {
              if (fIndex == fmtLen) {
                env.warning(L.l("expected ']', saw end of string"));
                break loop;
              }
              
              char ch2 = format.charAt(fIndex++);
              
              if (ch2 == ']') {
                break;
              }
              else {
                set.union(ch2);
              }
            }
            
            if (isNegated)
              segmentList.add(new ScanfSetNegated(set));
            else
              segmentList.add(new ScanfSet(set));
            
            break loop;
          }
          default:
            log.fine(L.l("'{0}' is a bad sscanf string.", format));
            env.warning(L.l("'{0}' is a bad sscanf string.", format));

            // XXX:
            //return isAssign ? LongValue.create(argIndex) : array;

            break loop;
          }
        }
      }
      else
        sb.append(ch);
    }

    scanfAddConstant(segmentList, sb);

    ScanfSegment[] segmentArray = new ScanfSegment[segmentList.size()];

    return segmentList.toArray(segmentArray);
  }

  private static void scanfAddConstant(
      ArrayList<ScanfSegment> segmentList, StringBuilder sb)
  {
    if (sb.length() == 0)
      return;

    segmentList.add(new ScanfConstant(sb.toString()));

    sb.setLength(0);
  }

  /**
   * scans a string
   *
   * @param format the format string
   * @param args the format arguments
   *
   * @return the formatted string
   */
  public static Value sscanfOld(Env env,
                             StringValue string,
                             StringValue format,
                             @Optional @Reference Value []args)
  {
    int fmtLen = format.length();
    int strlen = string.length();

    int sIndex = 0;
    int fIndex = 0;

    boolean isAssign = args.length != 0;
    boolean isReturnArray = ! isAssign;
    int argIndex = 0;

    if (strlen == 0) {
      return isAssign ? LongValue.MINUS_ONE : NullValue.NULL;
    }

    ArrayValue array = new ArrayValueImpl();

    while (fIndex < fmtLen) {
      char ch = format.charAt(fIndex++);

      if (isWhitespace(ch)) {
        for (;
             (fIndex < fmtLen
                 && isWhitespace(ch = format.charAt(fIndex)));
             fIndex++) {
        }

        /*ch = string.charAt(sIndex);
        if (! isWhitespace(ch)) {
          // XXX: return false?
          return sscanfReturn(env, array, args, argIndex, isAssign, true);
        }*/

        for (;
          sIndex < strlen && isWhitespace(string.charAt(sIndex));
          sIndex++) {
        }
      }
      else if (ch == '%') {
        int maxLen = -1;

        loop:
        while (fIndex < fmtLen) {
          ch = format.charAt(fIndex++);

          if (sIndex >= strlen && ch != 'n') {
            array.append(NullValue.NULL);
            break loop;
          }

          Value obj;

          if (isAssign) {
            if (argIndex < args.length)
              obj = args[argIndex++];
            else {
              env.warning(L.l("not enough vars passed in"));
              break loop;
            }
          }
          else
            obj = array;

          switch (ch) {
          case '%':
            if (string.charAt(sIndex) != '%')
              return sscanfReturn(env, array, args, argIndex, isAssign, true);
            else
              break loop;

          case '0': case '1': case '2': case '3': case '4':
          case '5': case '6': case '7': case '8': case '9':
            if (maxLen < 0)
              maxLen = 0;

            maxLen = 10 * maxLen + ch - '0';
            break;

          case 's':
          {
            ScanfSegment seg = new ScanfString(maxLen);

            sIndex = seg.apply(string, strlen, sIndex, obj, isReturnArray);
            break loop;
          }

          case 'c':
          {
            if (maxLen < 0)
              maxLen = 1;

            ScanfSegment seg = new ScanfString(maxLen);

            sIndex = seg.apply(string, strlen, sIndex, obj, isReturnArray);
            break loop;
          }
          case 'n':
          {
            ScanfSegment seg = ScanfStringLength.SEGMENT;

            sIndex = seg.apply(string, strlen, sIndex, obj, isReturnArray);

            break loop;
          }
          case 'd':
          {
            ScanfSegment seg = new ScanfInteger(maxLen, 10, false);

            sIndex = seg.apply(string, strlen, sIndex, obj, isReturnArray);

            break loop;
          }
          case 'u':
          {
            ScanfSegment seg = new ScanfInteger(maxLen, 10, true);

            sIndex = seg.apply(string, strlen, sIndex, obj, isReturnArray);

            break loop;
          }
          case 'o':
          {
            ScanfSegment seg = new ScanfInteger(maxLen, 8, false);

            sIndex = seg.apply(string, strlen, sIndex, obj, isReturnArray);

            break loop;
          }
          case 'x': case 'X':
          {
            ScanfSegment seg = new ScanfHex(maxLen);

            sIndex = seg.apply(string, strlen, sIndex, obj, isReturnArray);

            break loop;
          }
          case 'e': case 'f':
          {
            ScanfSegment seg = new ScanfScientific(maxLen);

            sIndex = seg.apply(string, strlen, sIndex, obj, isReturnArray);

            break loop;
          }
          default:
            log.fine(L.l("'{0}' is a bad sscanf string.", format));
            env.warning(L.l("'{0}' is a bad sscanf string.", format));

            return isAssign ? LongValue.create(argIndex) : array;
          }
        }
      }
      else if (ch == string.charAt(sIndex)) {
        sIndex++;
      }
      else
        return sscanfReturn(env, array, args, argIndex, false, true);
    }

    return sscanfReturn(env, array, args, argIndex, isAssign, false);
  }

  private static Value sscanfReturn(Env env,
                                    ArrayValue array,
                                    Value []args,
                                    int argIndex,
                                    boolean isReturnArray,
                                    boolean isWarn)
  {
    if (isReturnArray)
      return array;
    else {
      if (isWarn && argIndex != args.length)
        env.warning(
            L.l("{0} vars passed in but saw only {1} '%' args",
                args.length, argIndex));

      return LongValue.create(argIndex);
    }
  }

  /**
   * Scans a string with a given length.
   */
  private static int sscanfString(StringValue string,
                                  int sIndex,
                                  int maxLen,
                                  Value obj,
                                  boolean isAssignment)
  {
    int strlen = string.length();

    if (maxLen < 0)
      maxLen = Integer.MAX_VALUE;

    StringValue sb = string.createStringBuilder();

    for (; sIndex < strlen && maxLen-- > 0; sIndex++) {
      char ch = string.charAt(sIndex);

      if (! isWhitespace(ch))
        sb.append(ch);
      else
        break;
    }

    sscanfPut(obj, sb, isAssignment);

    return sIndex;
  }

  private static void sscanfPut(Value obj, Value val, boolean isAssignment)
  {
    if (isAssignment)
      obj.set(val);
    else
      obj.put(val);
  }

  /**
   * Scans a integer with a given length.
   */
  private static int sscanfInteger(StringValue string,
                                   int sIndex,
                                   int maxLen,
                                   Value obj,
                                   boolean isAssign,
                                   int base,
                                   boolean isUnsigned)
  {
    int strlen = string.length();

    if (maxLen < 0)
      maxLen = Integer.MAX_VALUE;

    int val = 0;
    int sign = 1;
    boolean isNotMatched = true;

    if (sIndex < strlen) {
      char ch = string.charAt(sIndex);

      if (ch == '+') {
        sIndex++;
        maxLen--;
      }
      else if (ch == '-') {
        sign = -1;

        sIndex++;
        maxLen--;
      }
    }

    int topRange = base + '0';

    for (; sIndex < strlen && maxLen-- > 0; sIndex++) {
      char ch = string.charAt(sIndex);

      if ('0' <= ch && ch < topRange) {
        val = val * base + ch - '0';
        isNotMatched = false;
      }
      else if (isNotMatched) {
        sscanfPut(obj, NullValue.NULL, isAssign);
        return sIndex;
      }
      else
        break;
    }

    if (isUnsigned) {
      if (sign == -1 && val != 0)
        sscanfPut(obj, StringValue.create(0xFFFFFFFFL - val + 1), isAssign);
      else
        sscanfPut(obj, LongValue.create(val), isAssign);
    }
    else
      sscanfPut(obj, LongValue.create(val * sign), isAssign);

    return sIndex;
  }

  /**
   * Scans a integer with a given length.
   */
  private static int sscanfHex(StringValue string,
                               int sIndex,
                               int maxLen,
                               Value obj,
                               boolean isAssign)
  {
    int strlen = string.length();

    if (maxLen < 0)
      maxLen = Integer.MAX_VALUE;

    int val = 0;
    int sign = 1;
    boolean isMatched = false;

    if (sIndex < strlen) {
      char ch = string.charAt(sIndex);

      if (ch == '+') {
        sIndex++;
        maxLen--;
      }
      else if (ch == '-') {
        sign = -1;

        sIndex++;
        maxLen--;
      }
    }

    for (; sIndex < strlen && maxLen-- > 0; sIndex++) {
      char ch = string.charAt(sIndex);

      if ('0' <= ch && ch <= '9') {
        val = val * 16 + ch - '0';
        isMatched = true;
      }
      else if ('a' <= ch && ch <= 'f') {
        val = val * 16 + ch - 'a' + 10;
        isMatched = true;
      }
      else if ('A' <= ch && ch <= 'F') {
        val = val * 16 + ch - 'A' + 10;
        isMatched = true;
      }
      else if (! isMatched) {
        sscanfPut(obj, NullValue.NULL, isAssign);
        return sIndex;
      }
      else
        break;
    }

    sscanfPut(obj, LongValue.create(val * sign), isAssign);

    return sIndex;
  }

  /**
   * Scans a integer with a given length.
   */
  private static int sscanfScientific(StringValue s,
                                      int i,
                                      int maxLen,
                                      Value obj,
                                      boolean isAssign)
  {
    if (maxLen < 0)
      maxLen = Integer.MAX_VALUE;

    int start = i;
    int len = s.length();
    int ch = 0;

    if (i < len && maxLen > 0 && ((ch = s.charAt(i)) == '+' || ch == '-')) {
      i++;
      maxLen--;
    }

    for (; i < len && maxLen > 0
           && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
      maxLen--;
    }

    if (ch == '.') {
      maxLen--;

      for (i++; i < len && maxLen > 0
                && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
        maxLen--;
      }
    }

    if (ch == 'e' || ch == 'E') {
      maxLen--;

      int e = i++;

      if (start == e) {
        sscanfPut(obj, NullValue.NULL, isAssign);
        return start;
      }

      if (i < len && maxLen > 0 && (ch = s.charAt(i)) == '+' || ch == '-') {
        i++;
        maxLen--;
      }

      for (; i < len && maxLen > 0
             && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
        maxLen--;
      }

      if (i == e + 1)
        i = e;
    }

    double val;

    if (i == 0)
      val = 0;
    else
      val = Double.parseDouble(s.substring(start, i).toString());

    sscanfPut(obj, DoubleValue.create(val), isAssign);

    return i;
  }

  // XXX: str_getcsv

  /**
   * replaces substrings.
   *
   * @param search search string
   * @param replace replacement string
   * @param subject replacement
   * @param count return value
   */
  public static Value str_ireplace(Env env,
                                  Value search,
                                  Value replace,
                                  Value subject,
                                  @Reference @Optional Value count)
  {
    return strReplace(env, search, replace, subject, count, true);
  }

  /**
   * Pads strings
   *
   * @param string string
   * @param length length
   * @param pad padding string
   * @param type padding type
   */
  public static StringValue str_pad(StringValue string,
                                    int length,
                                    @Optional("' '") String pad,
                                    @Optional("STR_PAD_RIGHT") int type)
  {
    int strLen = string.length();
    int padLen = length - strLen;

    if (padLen <= 0)
      return string;

    if (pad == null || pad.length() == 0)
      pad = " ";

    int leftPad = 0;
    int rightPad = 0;

    switch (type) {
    case STR_PAD_LEFT:
      leftPad = padLen;
      break;
    case STR_PAD_RIGHT:
    default:
      rightPad = padLen;
      break;
    case STR_PAD_BOTH:
      leftPad = padLen / 2;
      rightPad = padLen - leftPad;
      break;
    }

    int padStringLen = pad.length();

    StringValue sb = string.createStringBuilder(string.length() + padLen);

    for (int i = 0; i < leftPad; i++)
      sb.append(pad.charAt(i % padStringLen));

    sb = sb.append(string);

    for (int i = 0; i < rightPad; i++)
      sb.append(pad.charAt(i % padStringLen));

    return sb;
  }

  /**
   * repeats a string
   *
   * @param string string to repeat
   * @param count number of times to repeat
   */
  public static Value str_repeat(StringValue string, int count)
  {
    StringValue sb = string.createStringBuilder(count * string.length());

    for (int i = 0; i < count; i++)
      sb = sb.append(string);

    return sb;
  }

  /**
   * replaces substrings.
   *
   * @param search search string
   * @param replace replacement string
   * @param subject replacement
   * @param count return value
   */
  public static Value str_replace(Env env,
                                  Value search,
                                  Value replace,
                                  Value subject,
                                  @Reference @Optional Value count)
  {
    return strReplace(env, search, replace, subject, count, false);
  }

  /**
   * replaces substrings.
   *
   * @param search search string
   * @param replace replacement string
   * @param subject replacement
   * @param count return value
   */
  private static Value strReplace(Env env,
                                  Value search,
                                  Value replace,
                                  Value subject,
                                  @Reference @Optional Value count,
                                  boolean isInsensitive)
  {
    count.set(LongValue.ZERO);

    if (subject.isNull())
      return env.getEmptyString();

    if (search.isNull())
      return subject;

    if (subject instanceof ArrayValue) {
      ArrayValue subjectArray = (ArrayValue) subject;
      ArrayValue resultArray = new ArrayValueImpl();

      for (Map.Entry<Value, Value> entry : subjectArray.entrySet()) {

        if (entry.getValue() instanceof ArrayValue) {
          resultArray.append(entry.getKey(), entry.getValue());
        } else {
          Value result = strReplaceImpl(env,
                                      search,
                                      replace,
                                      entry.getValue().toStringValue(),
                                      count,
                                      isInsensitive);

          resultArray.append(entry.getKey(), result);
        }
      }

      return resultArray;
    }
    else {
      StringValue subjectString = subject.toStringValue();

      if (subjectString.length() == 0)
        return env.getEmptyString();

      return strReplaceImpl(env,
                            search,
                            replace,
                            subjectString,
                            count,
                            isInsensitive);
    }
  }

  /**
   * replaces substrings.
   *
   * @param search search string
   * @param replace replacement string
   * @param subject replacement
   * @param count return value
   */
  private static Value strReplaceImpl(Env env,
                                      Value search,
                                      Value replace,
                                      StringValue subject,
                                      Value count,
                                      boolean isInsensitive)
  {
    if (! search.isArray()) {
      StringValue searchString = search.toStringValue();

      if (searchString.length() == 0)
        return subject;

      if (replace instanceof ArrayValue) {
        env.warning(L.l("Array to string conversion"));
      }

      subject = strReplaceImpl(env,
                               searchString,
                               replace.toStringValue(),
                               subject,
                               count,
                               isInsensitive);
    }
    else if (replace instanceof ArrayValue) {
      ArrayValue searchArray = (ArrayValue) search;
      ArrayValue replaceArray = (ArrayValue) replace;

      Iterator<Value> searchIter = searchArray.values().iterator();
      Iterator<Value> replaceIter = replaceArray.values().iterator();

      while (searchIter.hasNext()) {
        Value searchItem = searchIter.next();
        Value replaceItem = replaceIter.next();

        if (replaceItem == null)
          replaceItem = NullValue.NULL;

        subject = strReplaceImpl(env,
                                 searchItem.toStringValue(),
                                 replaceItem.toStringValue(),
                                 subject,
                                 count,
                                 isInsensitive);
      }
    }
    else {
      ArrayValue searchArray = (ArrayValue) search;

      Iterator<Value> searchIter = searchArray.values().iterator();

      while (searchIter.hasNext()) {
        Value searchItem = searchIter.next();

        subject = strReplaceImpl(env,
                                 searchItem.toStringValue(),
                                 replace.toStringValue(),
                                 subject,
                                 count,
                                 isInsensitive);
      }
    }

    return subject;
  }

  /**
   * replaces substrings.
   *
   * @param search search string
   * @param replace replacement string
   * @param subject replacement
   * @param countV return value
   */
  private static StringValue strReplaceImpl(Env env,
                                            StringValue search,
                                            StringValue replace,
                                            StringValue subject,
                                            Value countV,
                                            boolean isInsensitive)
  {
    long count = countV.toLong();

    int head = 0;
    int next;

    int searchLen = search.length();

    StringValue result = null;

    while (head <= (next = indexOf(subject, search, head, isInsensitive))) {
      if (result == null)
        result = subject.createStringBuilder();

      result = result.append(subject, head, next);
      result = result.append(replace);

      if (head < next + searchLen)
        head = next + searchLen;
      else
        head += 1;

      count++;
    }

    if (count != 0 && result != null) {
      countV.set(LongValue.create(count));

      int subjectLength = subject.length();
      if (head > 0 && head < subjectLength)
        result = result.append(subject, head, subjectLength);

      return result;
    }
    else
      return subject;
  }

  /**
   * Returns the next index.
   */
  private static int indexOf(StringValue subject,
                             StringValue match,
                             int head,
                             boolean isInsensitive)
  {
    if (! isInsensitive)
      return subject.indexOf(match, head);
    else {
      int length = subject.length();
      int matchLen = match.length();

      if (matchLen <= 0)
        return -1;

      char ch = Character.toLowerCase(match.charAt(0));
      loop:
      for (; head + matchLen <= length; head++) {
        if (ch == Character.toLowerCase(subject.charAt(head))) {
          for (int i = 1; i < matchLen; i++) {
            if (Character.toLowerCase(subject.charAt(head + i))
                != Character.toLowerCase(match.charAt(i)))
              continue loop;
          }

          return head;
        }
      }

      return -1;
    }
  }

  /**
   * rot13 conversion
   *
   * @param string string to convert
   */
  public static Value str_rot13(StringValue string)
  {
    if (string == null)
      return NullValue.NULL;

    StringValue sb = string.createStringBuilder(string.length());

    int len = string.length();
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if ('a' <= ch && ch <= 'z') {
        int off = ch - 'a';

        sb.append((char) ('a' + (off + 13) % 26));
      }
      else if ('A' <= ch && ch <= 'Z') {
        int off = ch - 'A';

        sb.append((char) ('A' + (off + 13) % 26));
      }
      else {
        sb.append(ch);
      }
    }

    return sb;
  }

  /**
   * shuffles a string
   */
  public static String str_shuffle(String string)
  {
    if (string == null)
      string = "";

    char []chars = string.toCharArray();

    int length = chars.length;

    for (int i = 0; i < length; i++) {
      int rand = RandomUtil.nextInt(length);

      char temp = chars[rand];
      chars[rand] = chars[i];
      chars[i] = temp;
    }

    return new String(chars);
  }

  /**
   * split into an array
   *
   * @param string string to split
   * @param chunk chunk size
   */
  public static Value str_split(StringValue string,
                                @Optional("1") int chunk)
  {
    ArrayValue array = new ArrayValueImpl();

    if (string.length() == 0) {
      array.put(string);
      return array;
    }

    int strLen = string.length();

    for (int i = 0; i < strLen; i += chunk) {
      Value value;

      if (i + chunk <= strLen) {
        value = string.substring(i, i + chunk);
      } else {
        value = string.substring(i);
      }

      array.put(LongValue.create(i), value);
    }

    return array;
  }

  public static Value str_word_count(StringValue string,
                                     @Optional int format,
                                     @Optional String additionalWordCharacters)
  {
    if (format < 0 || format > 2)
      return NullValue.NULL;

    int strlen = string.length();
    boolean isAdditionalWordCharacters = false;

    if (additionalWordCharacters != null)
      isAdditionalWordCharacters = additionalWordCharacters.length() > 0;

    ArrayValueImpl resultArray = null;

    if (format > 0)
      resultArray = new ArrayValueImpl();

    boolean isBetweenWords = true;
    int wordCount = 0;
    int lastWordStart = 0;

    for (int i = 0; i <= strlen; i++) {
      boolean isWordCharacter;

      if (i < strlen) {
        int ch = string.charAt(i);

        isWordCharacter = Character.isLetter(ch)
                          || ch == '-'
                          || ch == '\''
                          || (isAdditionalWordCharacters
                              && additionalWordCharacters.indexOf(ch) > -1);
      }
      else
        isWordCharacter = false;

      if (isWordCharacter) {
        if (isBetweenWords) {
          // starting a word
          isBetweenWords = false;

          lastWordStart = i;
          wordCount++;
        }
      }
      else {
        if (!isBetweenWords) {
          // finished a word
          isBetweenWords = true;

          if (format > 0) {
            StringValue word = string.substring(lastWordStart, i);

            if (format == 1)
              resultArray.append(word);
            else if (format == 2)
              resultArray.put(LongValue.create(lastWordStart), word);
          }
        }
      }
    }

    if (resultArray == null)
      return LongValue.create(wordCount);
    else
      return resultArray;
  }

  /**
   * Case-insensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strcasecmp(StringValue a, StringValue b)
  {
    int aLen = a.length();
    int bLen = b.length();

    for (int i = 0; i < aLen && i < bLen; i++) {
      char chA = a.charAt(i);
      char chB = b.charAt(i);

      if (chA == chB)
        continue;

      if (Character.isUpperCase(chA))
        chA = Character.toLowerCase(chA);

      if (Character.isUpperCase(chB))
        chB = Character.toLowerCase(chB);

      if (chA == chB)
        continue;
      else if (chA < chB)
        return -1;
      else
        return 1;
    }

    if (aLen == bLen)
      return 0;
    else if (aLen < bLen)
      return -1;
    else
      return 1;
  }

  /**
   * Finds the index of a substring
   *
   * @param env the calling environment
   */
  public static Value strchr(Env env, StringValue haystack, Value needle)
  {
    return strstr(env, haystack, needle);
  }

  /**
   * Case-sensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strcmp(StringValue a, StringValue b)
  {
    int aLen = a.length();
    int bLen = b.length();

    for (int i = 0; i < aLen && i < bLen; i++) {
      char chA = a.charAt(i);
      char chB = b.charAt(i);

      if (chA == chB)
        continue;

      if (chA == chB)
        continue;
      else if (chA < chB)
        return -1;
      else
        return 1;
    }

    if (aLen == bLen)
      return 0;
    else if (aLen < bLen)
      return -1;
    else
      return 1;
  }

  /**
   * Locale-based comparison
   * XXX: i18n
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static Value strcoll(String a, String b)
  {
    if (a == null)
      a = "";

    if (b == null)
      b = "";

    int cmp = a.compareTo(b);

    if (cmp == 0)
      return LongValue.ZERO;
    else if (cmp < 0)
      return LongValue.MINUS_ONE;
    else
      return LongValue.ONE;
  }

  /**
   * Finds the number of initial characters in <i>string</i> that do not match
   * one of the characters in <i>characters</i>
   *
   * @param string the string to search in
   * @param characters the character set
   * @param offset the starting offset
   * @param length the length
   *
   * @return the length of the match or FALSE if
   * the offset or length are invalid
   */
  public static Value strcspn(StringValue string,
                              StringValue characters,
                              @Optional("0") int offset,
                              @Optional("-2147483648") int length)
  {
    if (characters.length() == 0) {
      characters = StringValue.create((char)0);
    }
    return strspnImpl(string, characters, offset, length, false);
  }


  /**
   * Removes tags from a string.
   *
   * @param string the string to remove
   * @param allowTags the allowable tags
   */
  public static StringValue strip_tags(StringValue string,
                                       @Optional Value allowTags)
  {
    StringValue result = string.createStringBuilder(string.length());

    HashSet<StringValue> allowedTagMap = null;

    if (! allowTags.isDefault())
      allowedTagMap = getAllowedTags(allowTags.toStringValue());

    int len = string.length();

    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (i + 1 >= len || ch != '<') {
        result.append(ch);
        continue;
      }

      ch = string.charAt(i + 1);

      if (Character.isWhitespace(ch)) {
        i++;

        result.append('<');
        result.append(ch);
        continue;
      }

      int tagNameStart = i + 1;

      if (ch == '/')
        tagNameStart++;

      int j = tagNameStart;

      while (j < len
             && (ch = string.charAt(j)) != '>'
             // && ch != '/'
             && ! Character.isWhitespace(ch)) {
        j++;
      }

      StringValue tagName = string.substring(tagNameStart, j);
      int tagEnd = 0;

      if (allowedTagMap != null && allowedTagMap.contains(tagName)) {
        result.append(string, i, Math.min(j + 1, len));
      }
      else {
        while (j < len && (ch = string.charAt(j)) != '<') {

          if (ch == '>') {
            tagEnd = j;
          }
          j++;
        }
      }

      i = (tagEnd != 0) ? tagEnd : j;
    }

    return result;
  }

  private static HashSet<StringValue> getAllowedTags(StringValue str)
  {
    int len = str.length();

    HashSet<StringValue> set = new HashSet<StringValue>();

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      switch (ch) {
        case '<':

          int j = i + 1;

          while (j < len
                 && (ch = str.charAt(j)) != '>'
                 //&& ch != '/'
                 && ! Character.isWhitespace(ch)) {
            j++;
          }

          if (ch == '>'
              && i + 1 < j
              && j < len)
            set.add(str.substring(i + 1, j));

          i = j;

        default:
          continue;
      }

    }

    return set;
  }

  /**
   * Strip out the backslashes, recognizing the escape sequences, octal,
   * and hexadecimal representations.
   *
   * @param source the string to clean
   * @see #addcslashes
   */
  public static String stripcslashes(String source)
  {
    if (source == null)
      source = "";

    StringBuilder result = new StringBuilder(source.length());

    int length = source.length();

    for (int i = 0; i < length; i++) {
      int ch = source.charAt(i);

      if (ch == '\\') {
        i++;

        if (i == length)
          ch = '\\';
        else {
          ch = source.charAt(i);

          switch (ch) {
          case 'a':
            ch = 0x07;
            break;
          case 'b':
            ch = '\b';
            break;
          case 't':
            ch = '\t';
            break;
          case 'n':
            ch = '\n';
            break;
          case 'v':
            ch = 0xb;
            break;
          case 'f':
            ch = '\f';
            break;
          case 'r':
            ch = '\r';
            break;
          case 'x':
            // up to two digits for a hex number
            if (i + 1 == length)
              break;

            int digitValue = hexToDigit(source.charAt(i + 1));

            if (digitValue < 0)
              break;

            ch = digitValue;
            i++;

            if (i + 1 == length)
              break;

            digitValue = hexToDigit(source.charAt(i + 1));

            if (digitValue < 0)
              break;

            ch = ((ch << 4) | digitValue);
            i++;

            break;
          default:
            // up to three digits from 0 to 7 for an octal number
            digitValue = octToDigit((char) ch);

            if (digitValue < 0)
              break;

            ch = digitValue;

            if (i + 1 == length)
              break;

            digitValue = octToDigit(source.charAt(i + 1));

            if (digitValue < 0)
              break;

            ch = ((ch << 3) | digitValue);
            i++;

            if (i + 1 == length)
              break;

            digitValue = octToDigit(source.charAt(i + 1));

            if (digitValue < 0)
              break;

            ch = ((ch << 3) | digitValue);
            i++;
          }
        }
      } // if ch == '/'

      result.append((char) ch);
    }

    return result.toString();
  }

  /**
   * Returns the position of a substring, testing case insensitive.
   *
   * @param haystack the full argument to check
   * @param needleV the substring argument to check
   * @param offsetV optional starting position
   */
  public static Value stripos(Env env, StringValue haystack,
                              Value needleV,
                              @Optional int offset)
  {
    StringValue needle;
    int len = haystack.length();

    if (len < offset) {
      env.warning(L.l("offset cannot exceed string length"));
      return BooleanValue.FALSE;
    }

    if (needleV instanceof StringValue)
      needle = (StringValue) needleV;
    else
      needle = StringValue.create((char) needleV.toInt());

    haystack = haystack.toLowerCase();
    needle = needle.toLowerCase();

    int pos = haystack.indexOf(needle, offset);

    if (pos < 0)
      return BooleanValue.FALSE;
    else
      return LongValue.create(pos);
  }

  /**
   * Strips out the backslashes.
   *
   * @param string the string to clean
   */
  public static StringValue stripslashes(StringValue string)
  {
    StringValue sb = string.createStringBuilder();
    int len = string.length();

    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (ch == '\\') {
        if (i + 1 < len) {
          char ch2 = string.charAt(i + 1);
          if (ch2 == '0') {
            ch2 = 0x0;
          }
          sb.append(ch2);
          i++;
        }
      }
      else
        sb.append(ch);
    }

    return sb;
  }

  /**
   * Finds the first instance of a substring, testing case insensitively
   *
   * @param haystack the string to search in
   * @param needleV the string to search for
   * @return the trailing match or FALSE
   */
  public static Value stristr(StringValue haystack,
                              Value needleV)
  {
    CharSequence needleLower;

    if (needleV instanceof StringValue) {
      needleLower = ((StringValue) needleV).toLowerCase();
    }
    else {
      char lower = Character.toLowerCase((char) needleV.toLong());

      needleLower = String.valueOf(lower);
    }

    StringValue haystackLower = haystack.toLowerCase();

    int i = haystackLower.indexOf(needleLower);

    if (i >= 0)
      return haystack.substring(i);
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the length of a string.
   *
   * @param value the argument value
   */
  public static Value strlen(Value value)
  {
    return LongValue.create(value.length());
  }

  /**
   * Case-insensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strnatcasecmp(StringValue a, StringValue b)
  {
    return naturalOrderCompare(a, b, true);
  }

  /**
   * Case-sensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static int strnatcmp(StringValue a, StringValue b)
  {
    return naturalOrderCompare(a, b, false);
  }

  /**
   * http://sourcefrog.net/projects/natsort/
   */
  private static int naturalOrderCompare(StringValue a,
                                         StringValue b,
                                         boolean ignoreCase)
  {
    SimpleStringReader aIn = new SimpleStringReader(a);
    SimpleStringReader bIn = new SimpleStringReader(b);

    int aChar = aIn.read();
    int bChar = bIn.read();

    if (aChar == -1 && bChar >= 0)
      return -1;
    else if (aChar >= 0 && bChar == -1)
      return 1;

    while (true) {
      while (Character.isWhitespace(aChar)) {
        aChar = aIn.read();
      }

      while (Character.isWhitespace(bChar)) {
        bChar = bIn.read();
      }

      if (aChar == -1 && bChar == -1) {
        return 0;
      }

      // leading zeros
      // '01' < '2'
      // '0a' > 'a'
      if (aChar == '0' && bChar == '0') {
        while (true) {
          aChar = aIn.read();
          bChar = bIn.read();

          if (aChar == '0' && bChar == '0') {
            continue;
          }
          else if (aChar == '0') {
            if ('1' <= bChar && bChar <= '9')
              return -1;
            else
              return 1;
          }
          else if (bChar == 0) {
            if ('1' <= aChar && aChar <= '9')
              return 1;
            else
              return -1;
          }
          else {
            break;
          }
        }
      }
      else if ('0' < aChar && aChar <= '9'
               && '0' < bChar && bChar <= '9')
      {
        int aInteger = aIn.readInt(aChar);
        int bInteger = bIn.readInt(bChar);

        if (aInteger > bInteger)
          return 1;
        else if (aInteger < bInteger)
          return -1;
        else {
          aChar = aIn.read();
          bChar = bIn.read();
        }
      }

      if (ignoreCase) {
        aChar = Character.toUpperCase(aChar);
        bChar = Character.toUpperCase(bChar);
      }

      if (aChar > bChar)
        return 1;
      else if (aChar < bChar)
        return -1;

      aChar = aIn.read();
      bChar = bIn.read();

      // trailing spaces
      // "abc " > "abc"
      if (aChar >= 0 && bChar == -1)
        return 1;
      else if (aChar == -1 && bChar >= 0)
        return -1;
    }
  }

  /**
   * Case-insensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static Value strncasecmp(
      Env env, StringValue a, StringValue b, int length)
  {
    if (length < 0) {
      env.warning(L.l("strncasecmp() length '{0}' must be non-negative",
                      length));
      return BooleanValue.FALSE;
    }

    int aLen = a.length();
    int bLen = b.length();

    for (int i = 0; i < length; i++) {
      if (aLen <= i)
        return LongValue.MINUS_ONE;
      else if (bLen <= i)
        return LongValue.ONE;

      char aChar = Character.toUpperCase(a.charAt(i));
      char bChar = Character.toUpperCase(b.charAt(i));

      if (aChar < bChar)
        return LongValue.MINUS_ONE;
      else if (bChar < aChar)
        return LongValue.ONE;
    }

    return LongValue.ZERO;
  }

  /**
   * Case-sensitive comparison
   *
   * @param a left value
   * @param b right value
   * @return -1, 0, or 1
   */
  public static Value strncmp(Env env, StringValue a, StringValue b, int length)
  {
    if (length < 0) {
      env.warning(L.l("strncmp() length '{0}' must be non-negative",
                      length));
      return BooleanValue.FALSE;
    }

    if (length < a.length())
      a = a.substring(0, length);

    if (length < b.length())
      b = b.substring(0, length);

    return LongValue.create(strcmp(a, b));
  }

  /**
   * Returns a substring of <i>haystack</i> starting from the earliest
   * occurence of any char in <i>charList</i>
   *
   * @param haystack the string to search in
   * @param charList list of chars that would trigger match
   * @return substring, else FALSE
   */
  public static Value strpbrk(StringValue haystack,
                              StringValue charList)
  {
    int len = haystack.length();
    int sublen = charList.length();

    for (int i = 0; i < len; i++) {
      for (int j = 0; j < sublen; j++) {
        if (haystack.charAt(i) == charList.charAt(j))
          return haystack.substring(i);
      }
    }

    return BooleanValue.FALSE;
  }

  /**
   * Returns the position of a substring.
   *
   * @param haystack the string to search in
   * @param needleV the string to search for
   */
  public static Value strpos(Env env,
                             StringValue haystack,
                             Value needleV,
                             @Optional int offset)
  {
    StringValue needle;
    
    if (offset > haystack.length()) {
      env.warning(L.l("offset cannot exceed string length"));
      
      return BooleanValue.FALSE;
    }

    if (needleV.isString())
      needle = needleV.toStringValue();
    else
      needle = StringValue.create((char) needleV.toInt());

    int pos = haystack.indexOf(needle, offset);

    if (pos < 0)
      return BooleanValue.FALSE;
    else
      return LongValue.create(pos);
  }

  /**
   * Finds the last instance of a substring
   *
   * @param haystack the string to search in
   * @param needleV the string to search for
   * @return the trailing match or FALSE
   */
  public static Value strrchr(StringValue haystack,
                              Value needleV)
  {
    CharSequence needle;

    if (needleV instanceof StringValue)
      needle = (StringValue) needleV;
    else
      needle = String.valueOf((char) needleV.toLong());

    int i = haystack.lastIndexOf(needle);

    if (i > 0)
      return haystack.substring(i);
    else
      return BooleanValue.FALSE;
  }

  /**
   * Reverses a string.
   *
   */
  public static Value strrev(StringValue string)
  {
    StringValue sb = string.createStringBuilder(string.length());

    for (int i = string.length() - 1; i >= 0; i--) {
      sb.append(string.charAt(i));
    }

    return sb;
  }

  /**
   * Returns the position of a substring, testing case-insensitive.
   *
   * @param haystack the full string to test
   * @param needleV the substring string to test
   * @param offsetV the optional offset to start searching
   */
  public static Value strripos(Env env,
                               String haystack,
                               Value needleV,
                               @Optional Value offsetV)
  {
    if (haystack == null)
      haystack = "";

    String needle;

    if (needleV instanceof StringValue)
      needle = needleV.toString();
    else
      needle = String.valueOf((char) needleV.toInt());

    int offset;

    if (offsetV instanceof DefaultValue)
      offset = haystack.length();
    else {
      offset = offsetV.toInt();
      
      if (haystack.length() < offset) {
        env.warning(L.l("offset cannot exceed string length"));
        
        return BooleanValue.FALSE;
      }
    }

    haystack = haystack.toLowerCase();
    needle = needle.toLowerCase();

    int pos = haystack.lastIndexOf(needle, offset);

    if (pos < 0)
      return BooleanValue.FALSE;
    else
      return LongValue.create(pos);
  }

  /**
   * Returns the position of a substring.
   *
   * @param haystack the string to search in
   * @param needleV the string to search for
   */
  public static Value strrpos(Env env,
                              StringValue haystack,
                              Value needleV,
                              @Optional Value offsetV)
  {
    StringValue needle;

    if (needleV instanceof StringValue)
      needle = needleV.toStringValue();
    else
      needle = StringValue.create((char) needleV.toInt());

    int offset = haystack.length() - offsetV.toInt();
      
    if (offset < 0) {
      env.warning(L.l("offset cannot exceed string length"));
      
      return BooleanValue.FALSE;
    }

    int pos = haystack.lastIndexOf(needle, offset);

    if (pos < 0)
      return BooleanValue.FALSE;
    else
      return LongValue.create(pos);
  }

  /**
   * Finds the number of initial characters in <i>string</i> that match one of
   * the characters in <i>characters</i>
   *
   * @param string the string to search in
   * @param characters the character set
   * @param offset the starting offset
   * @param length the length
   *
   * @return the length of the match or FALSE
   * if the offset or length are invalid
   */
  public static Value strspn(StringValue string,
                             StringValue characters,
                             @Optional int offset,
                             @Optional("-2147483648") int length)
  {
    return strspnImpl(string, characters, offset, length, true);
  }

  private static Value strspnImpl(StringValue string,
                                  StringValue characters,
                                  int offset,
                                  int length,
                                  boolean isMatch)
  {
    int strlen = string.length();

    // see also strcspn which uses the same procedure for determining
    // effective offset and length
    if (offset < 0) {
      offset += strlen;

      if (offset < 0)
        offset = 0;
    }

    if (offset > strlen)
      return BooleanValue.FALSE;

    if (length ==  -2147483648)
      length = strlen;
    else if (length < 0) {
      length += (strlen - offset);

      if (length < 0)
        length = 0;
    }

    int end = offset + length;

    if (strlen < end)
      end = strlen;

    int count = 0;

    for (; offset < end; offset++) {
      char ch = string.charAt(offset);
      boolean isPresent = characters.indexOf(ch) > -1;

      if (isPresent == isMatch)
        count++;
      else
        return LongValue.create(count);
    }

    return LongValue.create(count);
  }

  /**
   * Finds the first instance of a needle in haystack and returns
   * the portion of haystack from the beginning of
   * needle to the end of haystack.
   *
   * @param env the calling environment
   * @param haystackV the string to search in
   * @param needleV the string to search for, or the
   * original value of a character
   * @return the trailing match or FALSE if needle is not found
   */
  public static Value strstr(Env env,
                             StringValue haystackV,
                             Value needleV)
  {
    if (haystackV == null)
      haystackV = env.getEmptyString();

    String needle;

    if (needleV instanceof StringValue) {
      needle = needleV.toString();
    }
    else {
      needle = String.valueOf((char) needleV.toLong());
    }

    if (needle.length() == 0) {
      env.warning("empty needle");
      return BooleanValue.FALSE;
    }

    int i = haystackV.indexOf(needle);

    if (i >= 0)
      return haystackV.substring(i);
    else
      return BooleanValue.FALSE;
  }

  /**
   * Split a string into tokens using any character
   * in another string as a delimiter.
   *
   * The first call establishes the string to
   * search and the characters to use as tokens,
   * the first token is returned:
   * <pre>
   *   strtok("hello, world", ", ")
   *     => "hello"
   * </pre>
   *
   * Subsequent calls pass only the token
   * characters, the next token is returned:
   * <pre>
   *   strtok("hello, world", ", ")
   *     => "hello"
   *   strtok(", ")
   *     => "world"
   * </pre>
   *
   * False is returned if there are no more tokens:
   * <pre>
   *   strtok("hello, world", ", ")
   *     => "hello"
   *   strtok(", ")
   *     => "world"
   *   strtok(", ")
   *     => false
   * </pre>
   *
   * Calls that pass two arguments reset the search string:
   * <pre>
   *   strtok("hello, world", ", ")
   *     => "hello"
   *   strtok("goodbye, world", ", ")
   *     => "goodbye"
   *   strtok("world")
   *     => false
   *   strtok(", ")
   *     => false
   * </pre>
   */
  public static Value strtok(Env env,
                             StringValue string1,
                             @Optional Value string2)
  {
    StringValue string;
    StringValue characters;
    int offset;
    //StringValue savedToken = null;

    if (string2.isNull()) {
      StringValue savedString = (StringValue) env
          .getSpecialValue("caucho.strtok_string");
      Integer savedOffset = (Integer) env
          .getSpecialValue("caucho.strtok_offset");
      //savedToken = (StringValue) env.getSpecialValue("caucho.strtok_token");

      string = savedString == null ? env.getEmptyString() : savedString;
      offset = savedOffset == null ? 0 : savedOffset;
      //savedToken = savedToken == null ? env.getEmptyString() : savedToken;
      characters = string1;
    }
    else {
      string = string1;
      offset = 0;
      characters = string2.toStringValue();

      env.setSpecialValue("caucho.strtok_string", string);
      //env.setSpecialValue("caucho.strtok_token", string2);
    }

    int strlen = string.length();

    // skip any at beginning
    for (; offset < strlen; offset++) {
      char ch = string.charAt(offset);

      if (characters.indexOf(ch) < 0)
        break;
    }

    Value result;

    if (offset == strlen)
      result = BooleanValue.FALSE;
    else {

      //if (string2.isNull() && !(string1.eq(savedToken))) {
      //  offset = offset + savedToken.length();
      //}

      int start = offset;
      int end = start;

      // find end
      for (; end < strlen; end++) {
        char ch = string.charAt(end);

        if (characters.indexOf(ch) > -1)
          break;
      }

      for (offset = end; offset < strlen; offset++) {
        char ch = string.charAt(offset);

        if (characters.indexOf(ch) < 0)
          break;
      }

      result = string.substring(start, end);
    }

    env.setSpecialValue("caucho.strtok_offset", offset);

    return result;
  }

  /**
   * Converts to lower case.
   *
   * @param string the input string
   */
  public static StringValue strtolower(StringValue string)
  {
    return string.toLowerCase();
  }

  /**
   * Converts to upper case.
   *
   * @param string the input string
   */
  public static StringValue strtoupper(StringValue string)
  {
    return string.toUpperCase();
  }

  /**
   * Translates characters in a string to target values.
   *
   * @param string the source string
   * @param fromV the from characters
   * @param to the to character map
   */
  public static StringValue strtr(Env env,
                                  StringValue string,
                                  Value fromV,
                                  @Optional StringValue to)
  {
    if (fromV instanceof ArrayValue)
      return strtrArray(string, (ArrayValue) fromV);

    StringValue from = fromV.toStringValue();

    int len = from.length();

    if (to.length() < len)
      len = to.length();

    char []map = new char[256];
    for (int i = len - 1; i >= 0; i--)
      map[from.charAt(i)] = to.charAt(i);

    StringValue sb = string.createStringBuilder();

    len = string.length();
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (map[ch] != 0)
        sb.append(map[ch]);
      else
        sb.append(ch);
    }

    return sb;
  }

  /**
   * Translates characters in a string to target values.
   *
   * @param string the source string
   * @param map the character map
   */
  private static StringValue strtrArray(StringValue string, ArrayValue map)
  {
    int size = map.getSize();

    StringValue []fromList = new StringValue[size];
    StringValue []toList = new StringValue[size];

    Map.Entry<Value,Value> [] entryArray = new Map.Entry[size];

    int i = 0;
    for (Map.Entry<Value,Value> entry : map.entrySet()) {
      entryArray[i++] = entry;
    }

    // sort entries in descending fashion
    Arrays.sort(entryArray, new StrtrComparator<Map.Entry<Value,Value>>());

    boolean []charSet = new boolean[256];

    for (i = 0; i < size; i++) {
      fromList[i] = entryArray[i].getKey().toStringValue();
      toList[i] = entryArray[i].getValue().toStringValue();

      charSet[fromList[i].charAt(0)] = true;
    }

    StringValue result = string.createStringBuilder();
    int len = string.length();
    int head = 0;

    top:
    while (head < len) {
      char ch = string.charAt(head);

      if (charSet.length <= ch || charSet[ch]) {
        fromLoop:
        for (i = 0; i < fromList.length; i++) {
          StringValue from = fromList[i];
          int fromLen = from.length();

          if (head + fromLen > len)
            continue;

          if (ch != from.charAt(0))
            continue;

          for (int j = 0; j < fromLen; j++) {
            if (string.charAt(head + j) != from.charAt(j))
              continue fromLoop;
          }

          result = result.append(toList[i]);
          head = head + fromLen;

          continue top;
        }
      }

      result.append(ch);
      head++;
    }

    return result;
  }

  /*
   * Comparator for sorting in descending fashion based on length.
   */
  static class StrtrComparator<T extends Map.Entry<Value,Value>>
    implements Comparator<T>
  {
    public int compare(T a, T b)
    {
      int lenA = a.getKey().length();
      int lenB = b.getKey().length();

      if (lenA < lenB)
        return 1;
      else if (lenA == lenB)
        return 0;
      else
        return -1;
    }
  }

  /**
   * Returns a substring
   *
   * @param env the calling environment
   * @param string the string
   * @param start the start offset
   * @param lenV the optional length
   */
  public static Value substr(Env env,
                             StringValue string,
                             int start,
                             @Optional Value lenV)
  {
    int len = lenV.toInt();

    int strLen = string.length();
    if (start < 0)
      start = strLen + start;

    if (start < 0 || start >= strLen)
      return BooleanValue.FALSE;

    if (lenV.isDefault())
      return string.substring(start);
    else if (len == 0)
      return string.EMPTY;
    else {
      int end;

      if (len < 0)
        end = strLen + len;
      else
        end = (strLen < len) ? strLen : start + len;

      if (end <= start)
        return BooleanValue.FALSE;
      else if (strLen <= end)
        return string.substring(start);
      else
        return string.substring(start, end);
    }
  }

  public static Value substr_compare(Env env,
                                     StringValue mainStr,
                                     StringValue str,
                                     int offset,
                                     @Optional Value lenV,
                                     @Optional boolean isCaseInsensitive)
  {
    int strLen = mainStr.length();
    int len = lenV.toInt();

    if (! lenV.isDefault() && len == 0)
      return BooleanValue.FALSE;

    if (strLen < offset) {
      env.warning(L.l("offset can not be greater than length of string"));
      return BooleanValue.FALSE;
    }

    if (len > strLen
        || len + offset > strLen) {
      return BooleanValue.FALSE;
    }

    mainStr = substr(env, mainStr, offset, lenV).toStringValue();
    str = substr(env, str, 0, lenV).toStringValue();

    if (isCaseInsensitive)
      return LongValue.create(strcasecmp(mainStr, str));
    else
      return LongValue.create(strcmp(mainStr, str));
  }

  public static Value substr_count(Env env,
                                   StringValue haystackV,
                                   StringValue needleV,
                                   @Optional int offset,
                                   @Optional("-1") int length)
  {
    String haystack = haystackV.toString();

    String needle = needleV.toString();

    if (needle.length() == 0) {
      env.warning(L.l("empty substr"));
      return BooleanValue.FALSE;
    }

    int haystackLength = haystack.length();
    
    if (offset < 0 || offset > haystackLength) {
      env.warning(L.l("offset cannot exceed string length", offset));
      return BooleanValue.FALSE;
    }
    
    if (length >= 0) {
      int newLength = offset + length;
      
      if (newLength < 0 || newLength > haystackLength) {
        env.warning(L.l("length cannot exceed string length", length));
        return BooleanValue.FALSE;
      }
      
      haystackLength = newLength;
    }

    int needleLength = needle.length();

    int count = 0;

    int end = haystackLength - needleLength + 1;

    for (int i = offset; i < end; i++) {
      if (haystack.startsWith(needle, i)) {
        count++;
        i += needleLength;
      }
    }

    return LongValue.create(count);
  }

  /**
   * Replaces a substring with a replacement
   *
   * @param subjectV a string to modify, or an array of strings to modify
   * @param replacement the replacement string
   * @param startV the start offset
   * @param lengthV the optional length
   */
  public static Value substr_replace(Value subjectV,
                                     StringValue replacement,
                                     Value startV,
                                     @Optional Value lengthV)
  {
    int start = 0;
    int length = Integer.MAX_VALUE / 2;

    if (!(lengthV.isNull() || lengthV.isArray()))
      length = lengthV.toInt();

    if (!(startV.isNull() || startV.isArray()))
      start = startV.toInt();

    Iterator<Value> startIterator =
      startV.isArray()
      ? ((ArrayValue) startV).values().iterator()
      : null;

    Iterator<Value> lengthIterator =
      lengthV.isArray()
      ? ((ArrayValue) lengthV).values().iterator()
      : null;

    if (subjectV.isArray()) {
      ArrayValue resultArray = new ArrayValueImpl();

      ArrayValue subjectArray = (ArrayValue) subjectV;

      for (Value value : subjectArray.values()) {

        if (lengthIterator != null && lengthIterator.hasNext())
          length = lengthIterator.next().toInt();

        if (startIterator != null && startIterator.hasNext())
          start = startIterator.next().toInt();

        Value result = substrReplaceImpl(
            value.toStringValue(), replacement, start, length);

        resultArray.append(result);
      }

      return resultArray;
    }
    else {
      if (lengthIterator != null && lengthIterator.hasNext())
        length = lengthIterator.next().toInt();

      if (startIterator != null && startIterator.hasNext())
        start = startIterator.next().toInt();

      return substrReplaceImpl(
          subjectV.toStringValue(), replacement, start, length);
    }
  }

  private static Value substrReplaceImpl(StringValue string,
                                         StringValue replacement,
                                         int start,
                                         int len)
  {
    int strLen = string.length();

    if (start > strLen)
      start = strLen;
    else if (start < 0)
      start = Math.max(strLen + start, 0);

    int end;

    if (len < 0)
      end = Math.max(strLen + len, start);
    else {
      end = (strLen < len) ? strLen : (start + len);
    }


    StringValue result = string.createStringBuilder();

    result = result.append(string.substring(0, start));
    result = result.append(replacement);
    result = result.append(string.substring(end));

    return result;
  }

  /**
   * Removes leading and trailing whitespace.
   *
   * @param string the string to be trimmed
   * @param characters optional set of characters to trim
   * @return the trimmed string
   */
  public static Value trim(Env env,
                           StringValue string,
                           @Optional String characters)
  {
    boolean []trim;

    if (characters == null || characters.equals(""))
      trim = TRIM_WHITESPACE;
    else
      trim = parseCharsetBitmap(env, characters.toString());

    int len = string.length();

    int head = 0;
    for (; head < len; head++) {
      char ch = string.charAt(head);

      if (ch >= 256 || ! trim[ch]) {
        break;
      }
    }

    int tail = len - 1;
    for (; tail >= 0; tail--) {
      char ch = string.charAt(tail);

      if (ch >= 256 || ! trim[ch]) {
        break;
      }
    }

    if (tail < head)
      return env.getEmptyString();
    else {
      return (StringValue) string.subSequence(head, tail + 1);
    }
  }

  /**
   * Uppercases the first character
   *
   * @param string the input string
   */
  public static StringValue ucfirst(Env env, StringValue string)
  {
    if (string == null)
      return env.getEmptyString();
    else if (string.length() == 0)
      return string;

    StringValue sb = string.createStringBuilder();

    sb = sb.append(Character.toUpperCase(string.charAt(0)));
    sb = sb.append(string, 1, string.length());

    return sb;
  }

  /**
   * Uppercases the first character of each word
   *
   * @param string the input string
   */
  public static String ucwords(String string)
  {
    if (string == null)
      string = "";

    int strLen = string.length();

    boolean isStart = true;
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < strLen; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case ' ': case '\t': case '\r': case '\n':
        isStart = true;
        sb.append(ch);
        break;
      default:
        if (isStart)
          sb.append(Character.toUpperCase(ch));
        else
          sb.append(ch);
        isStart = false;
        break;
      }
    }

    return sb.toString();
  }

  /**
   * Formatted strings with array arguments
   *
   * @param format the format string
   * @param array the arguments to apply to the format string
   */
  public static int vprintf(Env env,
                            StringValue format,
                            @NotNull ArrayValue array)
  {
    Value []args;

    if (array != null) {
      args = new Value[array.getSize()];
      int i = 0;
      for (Value value : array.values())
        args[i++] = value;
    }
    else
      args = new Value[0];

    return printf(env, format, args);
  }

  /**
   * Formatted strings with array arguments
   *
   * @param format the format string
   * @param array the arguments to apply to the format string
   */
  public static Value vsprintf(Env env,
                               StringValue format,
                               @NotNull ArrayValue array)
  {
    Value []args;

    if (array != null) {
      args = new Value[array.getSize()];
      int i = 0;
      for (Value value : array.values())
        args[i++] = value;
    }
    else
      args = new Value[0];

    return sprintf(env, format, args);
  }

  /**
   * Wraps a string to the given number of characters.
   *
   * @param string the input string
   * @param width the width
   * @param breakString the break string
   * @param cut if true, break on exact match
   */
  public static Value wordwrap(Env env,
      @Expect(type = Expect.Type.STRING) Value value,
      @Optional @Expect(type = Expect.Type.NUMERIC) Value widthV,
      @Optional @Expect(type = Expect.Type.STRING) Value breakV,
      @Optional @Expect(type = Expect.Type.BOOLEAN) Value cutV)
  {
    if (value instanceof UnexpectedValue) {
      env.warning(L.l("word must be a string, but {0} given",
                      value.getType()));
      return NullValue.NULL;
    }
    
    if (widthV instanceof UnexpectedValue) {
      env.warning(L.l("width must be numeric, but {0} given",
                      widthV.getType()));
      return NullValue.NULL;
    }
    
    int width = 0;
    
    if (widthV.isDefault())
      width = 75;
    else
      width = widthV.toInt();
    
    String string = value.toString();
    
    if (cutV instanceof UnexpectedValue) {
      env.warning(L.l("cut must be a boolean, but {0} given",
                      cutV.getType()));
      return NullValue.NULL;
    }
    
    boolean isCut = cutV.toBoolean();
    
    if (isCut && width == 0 && string.length() > 0) {
      env.warning(L.l("cannot cut string to width 0"));
      return BooleanValue.FALSE;
    }

    int len = string != null ? string.length() : 0;
    
    if (breakV instanceof UnexpectedValue) {
      env.warning(L.l("break string must be a string, but {0} given",
                      breakV.getType()));
      return NullValue.NULL;
    }
    
    String breakString = "\n";
    
    if (! breakV.isDefault())
      breakString = breakV.toString();
    
    if (breakString == null || breakString.length() == 0) {
      env.warning(L.l("break string cannot be empty"));
      return BooleanValue.FALSE;
    }
    
    int breakLen = breakString.length();
    int breakChar;

    if (breakLen == 0)
      breakChar = -1;
    else
      breakChar = breakString.charAt(0);

    int head = 0;
    int lastSpace = 0;

    StringValue sb = env.createStringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);
      
      if (ch == breakChar && string.regionMatches(
          i, breakString, 0, breakLen)) {
        sb.append(string, head, i + breakLen);
        head = i + breakLen;
      } else if (width <= i - head) {
        if (ch == ' ') {
          sb.append(string, head, i);
          sb.append(breakString);
          head = i + 1;
        } else if (head < lastSpace) {
          sb.append(string, head, lastSpace);
          sb.append(breakString);
          head = lastSpace + 1;
        } else if (isCut) {
          sb.append(string, head, i);
          sb.append(breakString);
          head = i;
        }
      } else if (ch == ' ') {
        lastSpace = i;
      }
    }

    if (head < len) {
      sb.append(string, head, len);
    }

    return sb;
  }

  /**
   * Returns true if the character is a whitespace character.
   */
  protected static boolean isWhitespace(char ch)
  {
    return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
  }

  /**
   * Returns the uppercase equivalent of the caharacter
   */
  protected static char toUpperCase(char ch)
  {
    if (ch >= 'a' && ch <= 'z')
      return (char) ('A' + (ch - 'a'));
    else
      return ch;
  }

  /**
   * Converts an integer digit to a uuencoded char.
   */
  protected static char toUUChar(int d)
  {
    if (d == 0)
      return (char) 0x60;
    else
      return (char) (0x20 + (d & 0x3f));
  }

  protected static char toHexChar(int d)
  {
    d &= 0xf;

    if (d < 10)
      return (char) (d + '0');
    else
      return (char) (d - 10 + 'a');
  }

  protected static char toUpperHexChar(int d)
  {
    d &= 0xf;

    if (d < 10)
      return (char) (d + '0');
    else
      return (char) (d - 10 + 'A');
  }

  protected static int hexToDigit(char ch)
  {
    if ('0' <= ch && ch <= '9')
      return ch - '0';
    else if ('a' <= ch && ch <= 'f')
      return ch - 'a' + 10;
    else if ('A' <= ch && ch <= 'F')
      return ch - 'A' + 10;
    else
      return -1;
  }

  protected static int octToDigit(char ch)
  {
    if ('0' <= ch && ch <= '7')
      return ch - '0';
    else
      return -1;
  }

  abstract static class PrintfSegment {
    abstract public boolean apply(Env env, StringValue sb, Value []args);

    static boolean hasIndex(String format)
    {
      return format.indexOf('$') >= 0;
    }

    static int getIndex(String format)
    {
      int value = 0;

      for (int i = 0; i < format.length(); i++) {
        char ch;

        if ('0' <= (ch = format.charAt(i)) && ch <= '9')
          value = 10 * value + ch - '0';
        else
          break;
      }

      return value - 1;
    }

    static String getIndexFormat(String format)
    {
      int p = format.indexOf('$');

      return '%' + format.substring(p + 1);
    }
  }

  static class TextPrintfSegment extends PrintfSegment {
    private final char []_text;

    TextPrintfSegment(StringBuilder text)
    {
      _text = new char[text.length()];

      text.getChars(0, _text.length, _text, 0);
    }

    @Override
    public boolean apply(Env env, StringValue sb, Value []args)
    {
      sb.append(_text, 0, _text.length);

      return true;
    }
  }

  static class LongPrintfSegment extends PrintfSegment {
    private final String _format;
    private final int _index;
    private final QuercusLocale _locale;

    private LongPrintfSegment(String format, int index, QuercusLocale locale)
    {
      _format = format;
      _index = index;
      _locale = locale;
    }

    static PrintfSegment create(Env env, String format, int index)
    {
      if (hasIndex(format)) {
        index = getIndex(format);
        format = getIndexFormat(format);
      }
      else {
        format = '%' + format;
        //index = index;
      }

      // php/115b
      // strip out illegal precision specifier from phpBB vote function
      if (format.length() > 1 && format.charAt(1) == '.') {
        int i;

        for (i = 2; i < format.length(); i++) {
          char ch = format.charAt(i);

          if (! ('0' <= ch && ch <= '9'))
            break;
        }

        format = '%' + format.substring(i);
      }

      if (format.charAt(format.length() - 1) == 'x'
          || format.charAt(format.length() - 1) == 'X') {
        HexPrintfSegment hex = HexPrintfSegment.create(format, index);

        if (hex != null)
          return hex;
      }

      if (format.charAt(format.length() - 1) == 'b'
          || format.charAt(format.length() - 1) == 'B') {
        BinaryPrintfSegment bin = BinaryPrintfSegment.create(format, index);

        if (bin != null)
          return bin;
      }

      if (format.charAt(format.length() - 1) == 'u') {
        UnsignedPrintfSegment unsign
          = UnsignedPrintfSegment.create(format, index);

        if (unsign != null)
          return unsign;
      }

      return new LongPrintfSegment(format, index,
                                   env.getLocaleInfo().getNumeric());
    }

    @Override
    public boolean apply(Env env, StringValue sb, Value []args)
    {
      long value;

      if (_index < args.length)
        value = args[_index].toLong();
      else {
        env.warning(L.l("printf(): not enough arguments to match format."));

        return false;
      }

      sb.append(String.format(_locale.getLocale(), _format, value));

      return true;
    }
  }

  static class HexPrintfSegment extends PrintfSegment {
    private final int _index;
    private final int _min;
    private final char _pad;
    private boolean _isUpper;

    HexPrintfSegment(int index, int min, int pad, boolean isUpper)
    {
      _index = index;
      _min = min;
      
      if (pad >= 0)
        _pad = (char) pad;
      else
        _pad = ' ';
      
      _isUpper = isUpper;
    }

    static HexPrintfSegment create(String format, int index)
    {
      int length = format.length();
      int offset = 1;

      boolean isUpper = format.charAt(length - 1) == 'X';
      char pad = ' ';

      if (format.charAt(offset) == ' ') {
        pad = ' ';
        offset++;
      }
      else if (format.charAt(offset) == '0') {
        pad = '0';
        offset++;
      }

      int min = 0;
      for (; offset < length - 1; offset++) {
        char ch = format.charAt(offset);

        if ('0' <= ch && ch <= '9')
          min = 10 * min + ch - '0';
        else
          return null;
      }

      return new HexPrintfSegment(index, min, pad, isUpper);
    }

    @Override
    public boolean apply(Env env, StringValue sb, Value []args)
    {
      long value;

      if (_index >= 0 && _index < args.length)
        value = args[_index].toLong();
      else {
        env.warning(L.l("printf(): not enough arguments to match format."));

        return false;
      }

      int digits = 0;

      long shift = value;
      for (int i = 0; i < 16; i++) {
        if (shift != 0)
          digits = i;

        shift = shift >>> 4;
      }

      for (int i = digits + 1; i < _min; i++)
        sb.append(_pad);

      for (; digits >= 0; digits--) {
        int digit = (int) (value >>> (4 * digits)) & 0xf;

        if (digit <= 9)
          sb.append((char) ('0' + digit));
        else if (_isUpper)
          sb.append((char) ('A' + digit - 10));
        else
          sb.append((char) ('a' + digit - 10));
      }

      return true;
    }
  }

  static class UnsignedPrintfSegment extends PrintfSegment {
    private final int _index;
    private final int _min;
    private final char _pad;

    UnsignedPrintfSegment(int index, int min, int pad)
    {
      _index = index;
      _min = min;
      
      if (pad >= 0)
        _pad = (char) pad;
      else
        _pad = ' ';
    }

    static UnsignedPrintfSegment create(String format, int index)
    {
      int length = format.length();
      int offset = 1;
      
      if (format.charAt(offset) == '+')
        offset++;

      char pad = ' ';

      if (format.charAt(offset) == ' ') {
        pad = ' ';
        offset++;
      }
      else if (format.charAt(offset) == '0') {
        pad = '0';
        offset++;
      }

      int min = 0;
      for (; offset < length - 1; offset++) {
        char ch = format.charAt(offset);

        if ('0' <= ch && ch <= '9')
          min = 10 * min + ch - '0';
        else
          return null;
      }
      
      return new UnsignedPrintfSegment(index, min, pad);
    }

    @Override
    public boolean apply(Env env, StringValue sb, Value []args)
    {
      long value;

      if (_index >= 0 && _index < args.length)
        value = args[_index].toLong();
      else {
        env.warning(L.l("printf(): not enough arguments to match format."));

        return false;
      }

      char []buf = new char[32];
      int digits = buf.length;
      
      if (value == 0) {
        buf[--digits] = '0';
      }
      else if (value > 0) {
        while (value != 0) {
          int digit = (int) (value % 10);

          buf[--digits] = (char) ('0' + digit);

          value = value / 10;
        }
      }
      else {
        BigInteger bigInt = new BigInteger(String.valueOf(value));

        bigInt = bigInt.add(BIG_2_64);

        while (bigInt.compareTo(BigInteger.ZERO) != 0) {
          int digit = bigInt.mod(BIG_TEN).intValue();

          buf[--digits] = (char) ('0' + digit);

          bigInt = bigInt.divide(BIG_TEN);
        }
      }
      
      for (int i = buf.length - digits; i < _min; i++)
        sb.append(_pad);

      for (; digits < buf.length; digits++) {
        sb.append(buf[digits]);
      }
      
      return true;
    }
  }

  static class BinaryPrintfSegment extends PrintfSegment {
    private final int _index;
    private final int _min;
    private final char _pad;

    BinaryPrintfSegment(int index, int min, int pad)
    {
      _index = index;
      _min = min;
      
      if (pad >= 0)
        _pad = (char) pad;
      else
        _pad = ' ';
    }

    static BinaryPrintfSegment create(String format, int index)
    {
      int length = format.length();
      int offset = 1;

      char pad = ' ';

      if (format.charAt(offset) == ' ') {
        pad = ' ';
        offset++;
      }
      else if (format.charAt(offset) == '0') {
        pad = '0';
        offset++;
      }

      int min = 0;
      for (; offset < length - 1; offset++) {
        char ch = format.charAt(offset);

        if ('0' <= ch && ch <= '9')
          min = 10 * min + ch - '0';
        else
          return null;
      }

      return new BinaryPrintfSegment(index, min, pad);
    }

    @Override
    public boolean apply(Env env, StringValue sb, Value []args)
    {
      long value;

      if (_index >= 0 && _index < args.length)
        value = args[_index].toLong();
      else {
        env.warning(L.l("printf(): not enough arguments to match format."));

        return false;
      }

      int digits = 0;

      long shift = value;
      for (int i = 0; i < 64; i++) {
        if (shift != 0)
          digits = i;

        shift = shift >>> 1;
      }

      for (int i = digits + 1; i < _min; i++)
        sb.append(_pad);

      for (; digits >= 0; digits--) {
        int digit = (int) (value >>> (digits)) & 0x1;

        sb.append((char) ('0' + digit));
      }

      return true;
    }
  }

  static class DoublePrintfSegment extends PrintfSegment {
    private final String _format;
    private final boolean _isLeftZero;
    private final int _index;
    private final QuercusLocale _locale;

    DoublePrintfSegment(String format,
                        boolean isLeftZero,
                        int index,
                        QuercusLocale locale)
    {
      if (hasIndex(format)) {
        _index = getIndex(format);
        _format = getIndexFormat(format);
      }
      else {
        _format = '%' + format;
        _index = index;
      }

      _isLeftZero = isLeftZero;

      _locale = locale;
    }

    @Override
    public boolean apply(Env env, StringValue sb, Value []args)
    {
      double value;

      if (_index < args.length)
        value = args[_index].toDouble();
      else {
        env.warning(L.l("printf(): not enough arguments to match format."));

        return false;
      }

      String s;
      if (_locale == null)
        s = String.format(_format, value);
      else
        s = String.format(_locale.getLocale(), _format, value);

      if (_isLeftZero) {
        int len = s.length();

        // php/1174  "-0" not allowed by java formatter
        for (int i = 0; i < len; i++) {
          char ch = s.charAt(i);

          if (ch == ' ')
            sb.append('0');
          else
            sb.append(ch);
        }
      }
      else {
        sb.append(s);
      }

      return true;
    }
  }

  static class StringPrintfSegment extends PrintfSegment {
    protected final char []_prefix;
    protected final int _min;
    protected final int _max;
    protected final boolean _isLeft;
    protected final boolean _isUpper;
    protected final char _pad;
    protected final int _index;

    StringPrintfSegment(StringBuilder prefix,
                        boolean isLeft, int pad, boolean isUpper,
                        int width,
                        String format, int index)
    {
      _prefix = new char[prefix.length()];

      _isLeft = isLeft;
      _isUpper = isUpper;

      if (pad >= 0)
        _pad = (char) pad;
      else
        _pad = ' ';

      prefix.getChars(0, _prefix.length, _prefix, 0);

      if (hasIndex(format)) {
        index = getIndex(format);
        format = getIndexFormat(format);
      }

      int i = 0;
      int len = format.length();

      int min = width;
      int max = Integer.MAX_VALUE;
      char ch = ' ';

      /*
      for (; i < len && '0' <= (ch = format.charAt(i)) && ch <= '9'; i++) {
        min = 10 * min + ch - '0';
      }
      */
      
      if (0 < len && format.charAt(0) == '.') {
        max = 0;

        for (i++; i < len && '0' <= (ch = format.charAt(i)) && ch <= '9'; i++) {
          max = 10 * max + ch - '0';
        }
      }

      _min = min;
      _max = max;

      _index = index;
    }
    
    protected String toValue(Value []args)
    {
      return args[_index].toString();
    }

    @Override
    public boolean apply(Env env, StringValue sb, Value []args)
    {
      sb.append(_prefix, 0, _prefix.length);

      String value;

      if (_index < args.length)
        value = toValue(args);
      else {
        env.warning(L.l("printf(): not enough arguments to match format."));

        return false;
      }

      int len = value.length();

      if (_max < len) {
        value = value.substring(0, _max);
        len = _max;
      }

      if (_isUpper)
        value = value.toUpperCase();

      if (! _isLeft) {
        for (int i = len; i < _min; i++) {
          sb.append(_pad);
        }
      }

      sb.append(value);

      if (_isLeft) {
        for (int i = len; i < _min; i++) {
          sb.append(_pad);
        }
      }

      return true;
    }
  }

  static class CharPrintfSegment extends StringPrintfSegment {
    CharPrintfSegment(StringBuilder prefix,
                      boolean isLeft, int pad, boolean isUpper,
                      int width,
                      String format, int index)
    {
      super(prefix, isLeft, pad, isUpper, width, format, index);
    }

    @Override
    protected String toValue(Value []args)
    {
      if (args.length <= _index)
        return "";

      Value v = args[_index];

      if (v.isLongConvertible())
        return String.valueOf((char) v.toLong());
      else
        return v.charValueAt(0).toString();
    }
  }

  static class SimpleStringReader {
    StringValue _str;

    int _length;
    int _index;

    SimpleStringReader(StringValue str)
    {
      _str = str;
      _length = str.length();
      _index = 0;
    }

    int read()
    {
      if (_index < _length)
        return _str.charAt(_index++);
      else
        return -1;
    }

    int peek()
    {
      if (_index < _length)
        return _str.charAt(_index);
      else
        return -1;

    }

    int readInt(int currChar)
    {
      int number = currChar - '0';

      while (true) {
        currChar = peek();

        if ('0' <= currChar && currChar <= '9') {
          number = number * 10 + currChar - '0';
          _index++;
        }
        else {
          break;
        }
      }

      return number;
    }
  }

  // sscanf

  abstract static class ScanfSegment {
    abstract public boolean isAssigned();

    abstract public int apply(StringValue string,
                              int strlen,
                              int sIndex,
                              Value var,
                              boolean isReturnArray);

    void sscanfPut(Value var, Value val, boolean isReturnArray)
    {
      if (isReturnArray)
        var.put(val);
      else
        var.set(val);
    }
  }

  static class ScanfConstant extends ScanfSegment {
    private final String _string;
    private final int _strlen;

    private ScanfConstant(String string)
    {
      _string = string;
      _strlen = string.length();
    }

    @Override
    public boolean isAssigned()
    {
      return false;
    }

    @Override
    public int apply(StringValue string,
                     int strlen,
                     int sIndex,
                     Value var,
                     boolean isReturnArray)
    {
      int fStrlen = _strlen;
      String fString = _string;

      if (strlen - sIndex < fStrlen)
        return -1;

      for (int i = 0; i < fStrlen; i++) {
        if (string.charAt(sIndex++) != fString.charAt(i))
          return -1;
      }

      return sIndex;
    }
  }

  static class ScanfWhitespace extends ScanfSegment {
    static final ScanfWhitespace SEGMENT = new ScanfWhitespace();

    private ScanfWhitespace()
    {

    }

    @Override
    public boolean isAssigned()
    {
      return false;
    }

    @Override
    public int apply(StringValue string,
                     int strlen,
                     int sIndex,
                     Value var,
                     boolean isReturnArray)
    {
      for (;
           sIndex < strlen && isWhitespace(string.charAt(sIndex));
           sIndex++) {
      }

      return sIndex;
    }
  }

  static class ScanfStringLength extends ScanfSegment {
    static final ScanfStringLength SEGMENT = new ScanfStringLength();

    private ScanfStringLength()
    {
    }

    @Override
    public boolean isAssigned()
    {
      return true;
    }

    @Override
    public int apply(StringValue string,
                     int strlen,
                     int sIndex,
                     Value var,
                     boolean isReturnArray)
    {
      sscanfPut(var, LongValue.create(sIndex), isReturnArray);

      return sIndex;
    }
  }

  static class ScanfSet extends ScanfSegment {
    private IntSet _set;
    
    private ScanfSet(IntSet set)
    {
      _set = set;
    }

    @Override
    public boolean isAssigned()
    {
      return true;
    }

    @Override
    public int apply(StringValue string,
                     int strlen,
                     int sIndex,
                     Value var,
                     boolean isReturnArray)
    {
      StringValue sb = string.createStringBuilder();
      
      for (; sIndex < strlen; sIndex++) {
        char ch = string.charAt(sIndex);
        
        if (_set.contains(ch)) {
          sb.append(ch);
        }
        else {
          break;
        }
      }
      
      if (sb.length() > 0)
        sscanfPut(var, sb, isReturnArray);
      else if (isReturnArray)
        var.put(NullValue.NULL);
      
      return sIndex;
    }
  }
  
  static class ScanfSetNegated extends ScanfSegment {
    private IntSet _set;
    
    private ScanfSetNegated(IntSet set)
    {
      _set = set;
    }

    @Override
    public boolean isAssigned()
    {
      return true;
    }

    @Override
    public int apply(StringValue string,
                     int strlen,
                     int sIndex,
                     Value var,
                     boolean isReturnArray)
    {
      StringValue sb = string.createStringBuilder();
      
      for (; sIndex < strlen; sIndex++) {
        char ch = string.charAt(sIndex);
        
        if (! _set.contains(ch)) {
          sb.append(ch);
        }
        else {
          break;
        }
      }
      
      if (sb.length() > 0)
        sscanfPut(var, sb, isReturnArray);
      else if (isReturnArray)
        var.put(NullValue.NULL);
      
      return sIndex;
    }
  }
  
  static class ScanfScientific extends ScanfSegment {
    private final int _maxLen;

    ScanfScientific(int maxLen)
    {
      if (maxLen < 0)
        maxLen = Integer.MAX_VALUE;

      _maxLen  = maxLen;
    }

    @Override
    public boolean isAssigned()
    {
      return true;
    }

    @Override
    public int apply(StringValue s,
                     int strlen,
                     int i,
                     Value var,
                     boolean isReturnArray)
    {
      if (i == strlen) {
        if (isReturnArray)
          var.put(NullValue.NULL);
        
        return i;
      }
      
      int start = i;
      int len = strlen;
      int ch = 0;

      int maxLen = _maxLen;

      if (i < len && maxLen > 0 && ((ch = s.charAt(i)) == '+' || ch == '-')) {
        i++;
        maxLen--;
      }

      for (; i < len && maxLen > 0
             && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
        maxLen--;
      }

      if (ch == '.') {
        maxLen--;

        for (i++; i < len && maxLen > 0
                  && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
          maxLen--;
        }
      }

      if (ch == 'e' || ch == 'E') {
        maxLen--;

        int e = i++;

        if (start == e) {
          sscanfPut(var, NullValue.NULL, isReturnArray);
          return start;
        }

        if (i < len && maxLen > 0 && (ch = s.charAt(i)) == '+' || ch == '-') {
          i++;
          maxLen--;
        }

        for (; i < len && maxLen > 0
               && '0' <= (ch = s.charAt(i)) && ch <= '9'; i++) {
          maxLen--;
        }

        if (i == e + 1)
          i = e;
      }

      double val;

      if (i == 0)
        val = 0;
      else
        val = Double.parseDouble(s.substring(start, i).toString());

      sscanfPut(var, DoubleValue.create(val), isReturnArray);

      return i;
    }
  }

  static class ScanfHex extends ScanfSegment {
    private final int _maxLen;;

    ScanfHex(int maxLen)
    {
      if (maxLen < 0)
        maxLen = Integer.MAX_VALUE;

      _maxLen = maxLen;
    }

    @Override
    public boolean isAssigned()
    {
      return true;
    }

    @Override
    public int apply(StringValue string,
                     int strlen,
                     int sIndex,
                     Value var,
                     boolean isReturnArray)
    {
      if (sIndex == strlen) {
        if (isReturnArray)
          var.put(NullValue.NULL);
        
        return sIndex;
      }
      
      int val = 0;
      int sign = 1;
      boolean isMatched = false;

      int maxLen = _maxLen;

      if (sIndex < strlen) {
        char ch = string.charAt(sIndex);

        if (ch == '+') {
          sIndex++;
          maxLen--;
        }
        else if (ch == '-') {
          sign = -1;

          sIndex++;
          maxLen--;
        }
      }

      for (; sIndex < strlen && maxLen-- > 0; sIndex++) {
        char ch = string.charAt(sIndex);

        if ('0' <= ch && ch <= '9') {
          val = val * 16 + ch - '0';
          isMatched = true;
        }
        else if ('a' <= ch && ch <= 'f') {
          val = val * 16 + ch - 'a' + 10;
          isMatched = true;
        }
        else if ('A' <= ch && ch <= 'F') {
          val = val * 16 + ch - 'A' + 10;
          isMatched = true;
        }
        else if (! isMatched) {
          sscanfPut(var, NullValue.NULL, isReturnArray);
          return sIndex;
        }
        else
          break;
      }

      sscanfPut(var, LongValue.create(val * sign), isReturnArray);

      return sIndex;
    }
  }

  static class ScanfInteger extends ScanfSegment {
    private final int _maxLen;
    private final int _base;
    private final boolean _isUnsigned;

    ScanfInteger(int maxLen, int base, boolean isUnsigned)
    {
      if (maxLen < 0)
        maxLen = Integer.MAX_VALUE;

      _maxLen = maxLen;

      _base = base;
      _isUnsigned = isUnsigned;
    }

    @Override
    public boolean isAssigned()
    {
      return true;
    }

    @Override
    public int apply(StringValue string,
                     int strlen,
                     int sIndex,
                     Value var,
                     boolean isReturnArray)
    {
      if (sIndex == strlen) {
        if (isReturnArray)
          var.put(NullValue.NULL);
        
        return sIndex;
      }
      
      // XXX: 32-bit vs 64-bit
      int val = 0;

      int sign = 1;
      boolean isNotMatched = true;

      int maxLen = _maxLen;

      if (sIndex < strlen) {
        char ch = string.charAt(sIndex);

        if (ch == '+') {
          sIndex++;
          maxLen--;
        }
        else if (ch == '-') {
          sign = -1;

          sIndex++;
          maxLen--;
        }
      }

      int base = _base;

      int topRange = base + '0';

      for (; sIndex < strlen && maxLen-- > 0; sIndex++) {
        char ch = string.charAt(sIndex);

        if ('0' <= ch && ch < topRange) {
          val = val * base + ch - '0';
          isNotMatched = false;
        }
        else if (isNotMatched) {
          sscanfPut(var, NullValue.NULL, isReturnArray);
          return sIndex;
        }
        else
          break;
      }

      if (_isUnsigned) {
        if (sign == -1 && val != 0)
          sscanfPut(
              var, StringValue.create(0xffffffffL - val + 1), isReturnArray);
        else
          sscanfPut(var, LongValue.create(val), isReturnArray);
      }
      else
        sscanfPut(var, LongValue.create(val * sign), isReturnArray);

      return sIndex;
    }
  }

  static class ScanfString extends ScanfSegment {
    private final int _maxLen;

    ScanfString(int maxLen)
    {
      if (maxLen < 0)
        maxLen = Integer.MAX_VALUE;

      _maxLen = maxLen;
    }

    @Override
    public boolean isAssigned()
    {
      return true;
    }

    /**
     * Scans a string with a given length.
     */
    @Override
    public int apply(StringValue string,
                     int strlen,
                     int sIndex,
                     Value var,
                     boolean isReturnArray)
    {
      if (sIndex == strlen) {
        if (isReturnArray)
          var.put(NullValue.NULL);
        
        return sIndex;
      }
      
      StringValue sb = string.createStringBuilder();

      int maxLen = _maxLen;

      for (; sIndex < strlen && maxLen-- > 0; sIndex++) {
        char ch = string.charAt(sIndex);

        if (isWhitespace(ch))
          break;

        sb.append(ch);
      }

      sscanfPut(var, sb, isReturnArray);

      return sIndex;
    }
 }

  static {
    DEFAULT_DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols();
    DEFAULT_DECIMAL_FORMAT_SYMBOLS.setDecimalSeparator('.');
    DEFAULT_DECIMAL_FORMAT_SYMBOLS.setGroupingSeparator(',');
    DEFAULT_DECIMAL_FORMAT_SYMBOLS.setZeroDigit('0');
  }
}

