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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.i18n;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.VariableArguments;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.mail.MailModule;
import com.caucho.quercus.lib.regexp.Ereg;
import com.caucho.quercus.lib.regexp.Eregi;
import com.caucho.quercus.lib.regexp.RegexpModule;
import com.caucho.quercus.lib.regexp.UnicodeEreg;
import com.caucho.quercus.lib.regexp.UnicodeEregi;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.util.L10N;
import com.caucho.vfs.Encoding;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MbstringModule
  extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(MbstringModule.class.getName());
  
  private static final L10N L = new L10N(MbstringModule.class);
  
  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  public static final int MB_CASE_UPPER = 0;
  public static final int MB_CASE_LOWER = 1;
  public static final int MB_CASE_TITLE = 2;

  /**
   * Returns the extensions implemented by the module.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "mbstring" };
  }

  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }
  
  /**
   * Checks if the string is correctly encoded.
   * XXX: no args
   */
  public boolean mb_check_encoding(Env env,
                                   @Optional Value var,
                                   @Optional String encoding)
  {
    if (encoding == null || encoding.length() == 0)
      encoding = getEncoding(env);
   
    Decoder decoder = Decoder.create(encoding);
    
    if (! var.isDefault())
      return decoder.isDecodable(env, var.toStringValue());
   
    else
      throw new UnimplementedException("mb_check_encoding() with no args");
  }

  /**
   * Upper-cases, lower-cases, or capitalizes first letter of words.
   */
  public static StringValue mb_convert_case(Env env,
                              StringValue str,
                              int mode,
                              @Optional("") String encoding)
  {
    if (mode == MB_CASE_TITLE) {
      encoding = getEncoding(env, encoding);
      
      CharSequence unicodeStr = decode(env, str, encoding);

      unicodeStr = toUpperCaseTitle(env, unicodeStr);
      
      return encode(env, unicodeStr, encoding);
    }
    else if (mode == MB_CASE_LOWER)
      return mb_strtolower(env, str, encoding);
    else if (mode == MB_CASE_UPPER)
      return mb_strtoupper(env, str, encoding);
    else
      return str;
  }

  /**
   * Converts string of one encoding to another.
   */
  public static Value mb_convert_encoding(Env env,
                                          StringValue str,
                                          String destEncoding,
                                          @Optional Value fromEncodings)
  {
    ArrayList<String> charsetList = getEncodingList(env, fromEncodings);
    
    CharSequence unicodeStr = null;
    for (int i = 0; i < charsetList.size(); i++) {
      String charset = charsetList.get(i);

      try {
        unicodeStr = decode(env, str, charset);
      } catch (UnsupportedCharsetException e) {
        // should probably not log anything here because this is normal
        // behavior because of fallback encodings
        continue;
      }
    }
    
    if (unicodeStr == null) {
      log.log(Level.FINE,
          L.l("unsupported character encoding {0}", fromEncodings));
      env.warning(L.l("unsupported character encoding {0}", fromEncodings));
      return str;
    }
    
    try {
      return encode(env, unicodeStr, destEncoding);
    } catch (UnsupportedCharsetException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l("unsupported character encoding {0}", fromEncodings));

      return BooleanValue.FALSE;
    }
  }

  /**
   * Performs Japanese specific charset conversion.
   */
  public static StringValue mb_convert_kana(Env env,
                              StringValue str,
                              @Optional("") String option,
                              @Optional("") String encoding)
  {
    throw new UnimplementedException("mb_convert_kana");
  }

  /**
   * Decodes and then encodes variables.
   *
   * XXX: variable arguments to convert.
   */
  @VariableArguments
  public static StringValue mb_convert_variables(Env env,
                                                  String toEncoding,
                                                  String fromEncodings,
                                                  @Reference Value vars)
  {
    // XXX: fallback encoding
    int tail = fromEncodings.indexOf(',', 1);

    if (tail < 0)
      tail = fromEncodings.length();

    String srcEncoding;

    if (tail < 0)
      srcEncoding = fromEncodings;
    else
      srcEncoding = getEncoding(env, fromEncodings.substring(0, tail).trim());

    Value decoded = decodeAll(env, vars, srcEncoding);
    vars.set(encodeAll(env, decoded, toEncoding));

    return env.createString(srcEncoding);
  }

  /**
   * Decodes mime field.
   */
  public static Value mb_decode_mimeheader(Env env,
                              StringValue str)
  {
    String encoding = getEncoding(env);

    try {
      return QuercusMimeUtility.decodeMime(env, str, encoding);

    } catch (UnsupportedEncodingException e) {
      throw new QuercusModuleException(e.getMessage());
    }
  }

  /**
   * Decodes HTML numeric entity.
   */
  public static StringValue mb_decode_numericentity(Env env,
                              StringValue str,
                              ArrayValue convmap,
                              @Optional String encoding)
  {
    throw new UnimplementedException("mb_decode_numericentity");
  }

  /**
   * Detects encoding of string.
   */
  public static Value mb_detect_encoding(Env env,
                                         StringValue str,
                                         @Optional Value encodingV,
                                         @Optional boolean isStrict)
  {
    // XXX: strict
    
    ArrayList<String> encodingList = getDetectOrderList(env, encodingV);   
    
    int len = encodingList.size();
    for (int i = 0; i < len; i++) {
      String charset = encodingList.get(i);
      
      Decoder decoder = Decoder.create(charset);
      
      if (decoder.isDecodable(env, str))
        return env.createString(charset);
    }
    
    return BooleanValue.FALSE;
  }

  /**
   * Specifies order of charsets to test when detecting encoding.
   */
  public static Value mb_detect_order(Env env,
                                      @Optional Value encodingV)
  {
    if (encodingV.isDefault()) {
      ArrayValue array = new ArrayValueImpl();
      
      ArrayList<String> list = getDetectOrderList(env, encodingV);
      
      for (String encoding : list) {
        array.put(encoding);
      }
      
      return array;
    }
    else {
      ArrayList<String> list = new ArrayList<String>();
      
      if (encodingV.isArray()) {
        Iterator<Value> iter = encodingV.getValueIterator(env);
        
        while (iter.hasNext()) {
          list.add(iter.next().toString());
        }
      }
      else
        parseCommaSeparatedList(list, encodingV.toString());
      
      env.setSpecialValue("mb.detect_order", list);
      
      return BooleanValue.TRUE;
    }
  }
  
  private static ArrayList<String> getDetectOrderList(Env env,
                                                         Value encodingV)
  {
    if (encodingV.isDefault() && env.getSpecialValue("mb.detect_order") != null)
      return (ArrayList<String>) env.getSpecialValue("mb.detect_order");
    
    ArrayList<String> list = new ArrayList<String>();
    
    if (encodingV.isDefault()) {
      String encodings = env.getIniString("mbstring.detect_order");
      
      if (encodings != null)
        parseCommaSeparatedList(list, encodings);
      else {
        list.add("ASCII");
        list.add("UTF-8");
      }
    }
    else if (encodingV.isArray()) {
      Iterator<Value> iter = encodingV.getValueIterator(env);
      
      while (iter.hasNext()) {
        list.add(iter.next().toString());
      }
    }
    else {
      String encodings = encodingV.toString();
      
      if (encodings.equalsIgnoreCase("auto")) {
        list.add("ASCII");
        list.add("JIS");
        list.add("UTF-8");
        list.add("EUC-JP");
        list.add("SJIS");
      }
      else
        parseCommaSeparatedList(list, encodingV.toString());
    }
    
    return list;
  }
  
  private static void parseCommaSeparatedList(ArrayList<String> list,
                                              String str)
  {
    int start = 0;
    int index;
    
    while ((index = str.indexOf(",", start)) >= 0) {
      String charset = str.substring(start, index).trim();
      
      start = index + 1;
      
      list.add(charset);
    }
    
    list.add(str.substring(start).trim());
  }

  /**
   * Encodes a string into mime.
   */
  public static StringValue mb_encode_mimeheader(Env env,
                              StringValue str,
                              @Optional("") String charset,
                              @Optional("B") String transfer_encoding,
                              @Optional("") String linefeed)
  {
    charset = getEncoding(env, charset);

    try {
      String mime = QuercusMimeUtility.encodeMimeWord(str.toString(),
                                                      charset,
                                                      transfer_encoding,
                                                      linefeed,
                                                      76);
      return env.createString(mime);

    } catch (UnsupportedEncodingException e) {
      throw new QuercusModuleException(e.getMessage());
    }

  }

  /**
   * Encodes HTML numeric string entity.
   */
  public static StringValue mb_encode_numericentity(Env env,
                              StringValue str,
                              ArrayValue convmap,
                              @Optional String encoding)
  {
    throw new UnimplementedException();
  }

  /**
   * Returns true if pattern matches a part of string.
   */
  public static BooleanValue mb_ereg_match(Env env,
                                           UnicodeEreg ereg,
                                           StringValue string,
                                           @Optional String option)
  {
    String encoding = getEncoding(env);

    string = string.convertToUnicode(env, encoding);

    // XXX: option

    Value val = RegexpModule.eregImpl(env, ereg, string, null);

    if (val == BooleanValue.FALSE)
      return BooleanValue.FALSE;
    else
      return BooleanValue.TRUE;
  }

  /**
   * Multibyte version of ereg_replace.
   */
  public static Value mb_ereg_replace(Env env,
                                      Value eregValue,
                                      StringValue replacement,
                                      StringValue subject,
                                      @Optional String option)
  {
    String encoding = getEncoding(env);

    StringValue eregStr;
    
    if (eregValue.isLong())
      eregStr = UnicodeBuilderValue.create((char) eregValue.toInt());
    else
      eregStr = eregValue.toStringValue(env).convertToUnicode(env, encoding);
    
    replacement = replacement.convertToUnicode(env, encoding);
    subject = subject.convertToUnicode(env, encoding);

    //XXX: option
    
    Value val = RegexpModule.ereg_replace(env,
                                          eregStr,
                                          replacement,
                                          subject);

    return encodeAll(env, val, encoding);
  }

  /**
   * Multibyte version of ereg.
   */
  public static Value mb_ereg(Env env,
                              UnicodeEreg ereg,
                              StringValue string,
                              @Optional ArrayValue regs)
  {
    return eregImpl(env, ereg, string, regs);
  }

  /**
   * Multibyte version of eregi_replace.
   */
  public static Value mb_eregi_replace(Env env,
                                       Value pattern,
                                       StringValue replacement,
                                       StringValue subject,
                                       @Optional String option)
  {
    String encoding = getEncoding(env);

    StringValue eregStr;
    
    if (pattern.isLong())
      eregStr = UnicodeBuilderValue.create((char) pattern.toInt());
    else
      eregStr = pattern.toStringValue(env).convertToUnicode(env, encoding);
    
    replacement = replacement.convertToUnicode(env, encoding);
    subject = subject.convertToUnicode(env, encoding);

    //XXX: option

    Value val = RegexpModule.eregi_replace(env, eregStr, replacement, subject);

    return encodeAll(env, val, encoding);
  }

  /**
   * Multibyte version of eregi.
   */
  public static Value mb_eregi(Env env,
                               UnicodeEregi eregi,
                               StringValue string,
                               @Optional ArrayValue regs)
  {
    return eregImpl(env, eregi, string, regs);
  }

  private static Value eregImpl(Env env,
                                UnicodeEreg ereg,
                                StringValue string,
                                ArrayValue regs)
  {
    String encoding = getEncoding(env);

    string = string.convertToUnicode(env, encoding);

    if (regs == null) {
      return RegexpModule.eregImpl(env, ereg, string, null);
    }

    Value val;
    Var regVar = new Var();

    val = RegexpModule.eregImpl(env, ereg, string, regVar);
    
    if (regVar.isset()) {
      regs.clear();
      ArrayValue results = regVar.toArrayValue(env);

      for (Map.Entry<Value,Value> entry : results.entrySet()) {

        Value bytes = encodeAll(env, entry.getValue(), encoding);
        regs.put(entry.getKey(), bytes);
      }

      val = LongValue.create(
              regs.get(LongValue.ZERO).toStringValue().length());
    }

    return val;
  }

  /**
   * Gets current position of ereg state object.
   */
  public static LongValue mb_ereg_search_getpos(Env env)
  {
    EregSearch ereg = getEreg(env);

    if (ereg == null)
      return LongValue.ZERO;

    return LongValue.create(ereg._position);
  }

  /**
   * Gets the last match of ereg state object from previous matching.
   */
  public static Value mb_ereg_search_getregs(Env env)
  {
    EregSearch ereg = getEreg(env);

    if (ereg == null || ereg._lastMatch == null)
      return BooleanValue.FALSE;

    return ereg._lastMatch;
  }

  /**
   * Initializes a ereg state object.
   */
  public static BooleanValue mb_ereg_search_init(Env env,
                                                 StringValue string,
                                                 @Optional Value rawRegexp,
                                                 @Optional Value option)
  {
    UnicodeEregi regexp = null;
    
    if (! rawRegexp.isDefault()) {
      regexp
        = RegexpModule.createUnicodeEregi(env, rawRegexp.toStringValue(env));
    }
    
    EregSearch ereg = new EregSearch(env, string, regexp, option);
    env.setSpecialValue("mb.search", ereg);

    return BooleanValue.TRUE;
  }

  /**
   * Returns index and position after matching.
   */
  public static Value mb_ereg_search_pos(Env env,
                                         @Optional Value rawRegexp,
                                         @Optional Value option)
  {
    UnicodeEregi regexp = null;
    
    if (! rawRegexp.isDefault()) {
      regexp
        = RegexpModule.createUnicodeEregi(env, rawRegexp.toStringValue(env));
    }
    
    EregSearch ereg = getEreg(env, regexp, option);

    if (ereg == null) {
      env.warning(L.l("Regular expression not set"));
      return BooleanValue.FALSE;
    }

    return ereg.search(env, true);
  }

  /**
   * Returns match array after matching.
   */
  public static Value mb_ereg_search_regs(Env env,
                                          @Optional Value rawRegexp,
                                          @Optional Value option)
  {
    UnicodeEregi regexp = null;
    
    if (! rawRegexp.isDefault()) {
      regexp
        = RegexpModule.createUnicodeEregi(env, rawRegexp.toStringValue(env));
    }
    
    EregSearch ereg = getEreg(env, regexp, option);

    if (ereg == null) {
      env.warning(L.l("Regular expression not set"));
      return BooleanValue.FALSE;
    }

    if (ereg.search(env, false) == BooleanValue.FALSE)
      return BooleanValue.FALSE;

    return ereg._lastMatch;
  }

  /**
   * Sets the position of the ereg state object.
   */
  public static BooleanValue mb_ereg_search_setpos(Env env,
                              int position)
  {
    EregSearch ereg = getEreg(env);

    if (ereg == null)
      return BooleanValue.FALSE;

    ereg._position = position;
    return BooleanValue.TRUE;
  }

  /**
   * Returns whether or not pattern matches string.
   */
  public static BooleanValue mb_ereg_search(Env env,
                                            @Optional Value rawRegexp,
                                            @Optional Value option)
  {
    UnicodeEregi regexp = null;
    
    if (! rawRegexp.isDefault()) {
      regexp
        = RegexpModule.createUnicodeEregi(env, rawRegexp.toStringValue(env));
    }
    
    EregSearch ereg = getEreg(env, regexp, option);

    if (ereg == null) {
      env.warning(L.l("Regular expression not set"));
      return BooleanValue.FALSE;
    }

    Value result = ereg.search(env, false);

    return BooleanValue.create(result.toBoolean());
  }

  /**
   * Returns the ereg state object from the environment.
   */
  private static EregSearch getEreg(Env env)
  {
    Object obj = env.getSpecialValue("mb.search");

    if (obj == null)
      return null;

    return (EregSearch) obj;
  }

  /**
   * Returns the ereg state object from the environment iff the ereg object
   * is a valid one.
   */
  private static EregSearch getEreg(Env env,
                                    UnicodeEregi regexp,
                                    Value option)
  {
    Object obj = env.getSpecialValue("mb.search");

    if (obj != null) {
      EregSearch ereg = (EregSearch) obj;

      if (regexp != null)
        ereg.init(regexp, option);

      if (ereg._isValidRegexp)
        return ereg;
      else
        return null;
    }
    else
      return null;
  }

  /**
   * Returns current mb settings.
   */
  public static Value mb_get_info(Env env,
                              @Optional("") String type)
  {
    if (type.length() == 0) {
      ArrayValue array = new ArrayValueImpl();

      array.put(env.createString("internal_encoding"),
                env.createString(getEncoding(env)));
      
      array.put(env.createString("http_output"),
                env.createString(getOutputEncoding(env)));

      return array;
    }
    else if (type.equals("internal_encoding")) {
      return env.createString(getEncoding(env));
    }
    else if (type.equals("http_output")) {
      return env.createString(getOutputEncoding(env));
    }
    else {
      env.warning(L.l("unsupported option: {0}", type));
      
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns and/or sets the http input encoding
   */
  public static Value mb_http_input(Env env,
                              @Optional String type)
  {
    throw new UnimplementedException("mb_http_input");
  }
 
  /**
   * Returns and/or sets the http output encoding
   */
  public static Value mb_http_output(Env env,
                                     @Optional String encoding)
  {
    if (encoding.length() == 0) {
      return env.createString(getOutputEncoding(env));
    }
    else {
      env.setIni("mbstring.http_output", encoding);
      
      return BooleanValue.TRUE;
    }
  }

  /**
   * Returns and/or sets the internal encoding.
   */
  public static Value mb_internal_encoding(Env env,
                              @Optional String encoding)
  {
    if (encoding.length() == 0)
      return env.createString(getEncoding(env));
    else {
      setEncoding(env, encoding);
      return BooleanValue.TRUE;
    }
  }

  /**
   * Returns and/or sets the encoding for mail.
   */
  public static Value mb_language(Env env,
                                  @Optional String language)
  {
    String encoding = getEncodingLanguage(env);

    if (language == null || language.length() == 0) {
      if (encoding.equalsIgnoreCase("ISO-2022-JP"))
        return env.createString("Japanese");
      else if (encoding.equalsIgnoreCase("ISO-8859-1"))
        return env.createString("English");
      else if (encoding.equalsIgnoreCase("UTF-8"))
        return env.createString("uni");
      else
        return env.createString(encoding);
    }
    else if (language.equals("Japanese") || language.equals("ja"))
      setEncodingLanguage(env, "ISO-2022-JP");
    else if (language.equals("English") || language.equals("en"))
      setEncodingLanguage(env, "ISO-8859-1");
    else if (language.equals("uni"))
      setEncodingLanguage(env, "UTF-8");
    else
      return BooleanValue.FALSE;

    return BooleanValue.TRUE;
  }
  
  private static String getEncodingLanguage(Env env)
  {
    String encoding = (String) env.getSpecialValue("mb.internal_encoding");
    
    if (encoding == null)
      return "ISO-8859-1";
    
    return encoding;
  }
  
  private static void setEncodingLanguage(Env env, String encoding)
  {
    env.setSpecialValue("mb.internal_encoding", encoding);
  }

  /**
   * Get all supported encodings.
   */
  public static ArrayValue mb_list_encodings(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    Map<String,Charset> charsetMap = Charset.availableCharsets();

    for (String name : charsetMap.keySet()) {
      array.put(env.createString(name));
    }

    return array;
  }

  /**
   * ob_start() handler
   */
  public static StringValue mb_output_handler(Env env,
                                              StringValue contents,
                                              int status)
  {
    // XXX: status?

    String toEncoding = getOutputEncoding(env);

    if (toEncoding.equals("pass"))
      return contents;
    
    String fromEncoding = getEncoding(env);
    
    Decoder decoder = getDecoder(env, fromEncoding);
    CharSequence contentsUnicode = decoder.decode(env, contents);

    Encoder encoder = getEncoder(env, toEncoding);
    return encoder.encode(env, contentsUnicode);
  }

  /**
   * Multibyte version of parse_str.
   */
  public static BooleanValue mb_parse_str(Env env,
                              StringValue strValue,
                              @Optional @Reference Value result)
  {
    String encoding = getEncoding(env);
    StringModule.parse_str(env, strValue, result);

    if (result == null) {
      // XXX: encode newly added global variables
      return BooleanValue.TRUE;
    }
    else {
      Value array = encodeAll(env, result, encoding);
      result.set(array);

      return BooleanValue.TRUE;
    }
  }

  /**
   * Returns the preferred mime name of this encoding.
   */
  public static StringValue mb_preferred_mime_name(Env env,
                              StringValue encoding)
  {
    String mimeName = Encoding.getMimeName(encoding.toString());

    return env.createString(mimeName);
  }

  /**
   * Returns and/or sets encoding for mb regular expressions.
   */
  public static Value mb_regex_encoding(Env env,
                              @Optional("") String encoding)
  {
    return mb_internal_encoding(env, encoding);
  }

  /**
   * XXX: what does this actually do?
   */
  public static StringValue mb_regex_set_options(Env env,
                              @Optional String options)
  {
    throw new UnimplementedException("mb_regex_set_options");
  }

  /**
   * Multibyte version of mail.
   */
  public static BooleanValue mb_send_mail(Env env,
                              StringValue to,
                              StringValue subject,
                              StringValue message,
                              @Optional StringValue additionalHeaders,
                              @Optional StringValue additionalParameters)
  {
    //XXX: not correct
    
    String encoding = getEncoding(env);

    subject = subject.toBinaryValue(encoding);
    message = message.toBinaryValue(encoding);
    additionalHeaders = additionalHeaders.toBinaryValue(encoding);

    boolean result = MailModule.mail(env,
                                     to.toString(),
                                     subject.toString(),
                                     message,
                                     additionalHeaders.toString(),
                                     additionalParameters.toString());

    return BooleanValue.create(result);
  }

  /**
   * Multibyte version of split.
   */
  public static Value mb_split(Env env,
                               UnicodeEreg ereg,
                              StringValue string,
                              @Optional("-1") long limit)
  {
    String encoding = getEncoding(env);

    string = string.convertToUnicode(env, encoding);
    
    Value val = RegexpModule.split(env, ereg, string, limit);

    return encodeAll(env, val, encoding);
  }

  /**
   * Similar to substr except start index is at the beginning of char
   * boundaries.
   */
  public static StringValue mb_strcut(Env env,
                              final StringValue str,
                              int start,
                              @Optional("7fffffff") int length,
                              @Optional String encoding)
  {
    encoding = getEncoding(env, encoding);
    
    CharSequence unicodeStr = decode(env, str, encoding);

    int len = unicodeStr.length();
    int end = start + length;

    if (end > len)
      end = len;

    if (start < 0 || start > end)
      return str.EMPTY;

    // XXX: not quite exactly the same behavior as PHP
    if (start < len && Character.isHighSurrogate(unicodeStr.charAt(start)))
      start--;

    StringBuilder sb = new StringBuilder();
    
    sb.append(unicodeStr, start, end);

    return encode(env, sb, encoding);
  }

  /**
   * Truncates the string.
   */
  public static StringValue mb_strimwidth(Env env,
                              final StringValue str,
                              int start,
                              int width,
                              @Optional() StringValue trimmarker,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);
    
    CharSequence unicodeStr = decode(env, str, encoding);

    int len = unicodeStr.length();
    int end = start + width;

    if (end > len)
      end = len;

    if (start < 0 || start > end)
      return str.EMPTY;

    StringBuilder sb = new StringBuilder();

    if (end < len && trimmarker.length() > 0) {
      sb.append(unicodeStr, start, end - 1);
      sb.append(decode(env, trimmarker, encoding));

      unicodeStr = sb;
    }
    else
      sb.append(unicodeStr, start, end);

    return encode(env, sb, encoding);
  }

  /**
   * Multibyte version of strlen.
   */
  public static LongValue mb_strlen(Env env,
                              StringValue str,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    str = str.convertToUnicode(env, encoding);

    return LongValue.create(str.length());
  }

  /**
   * Multibyte version of strpos.
   */
  public static Value mb_strpos(Env env,
                                StringValue haystack,
                                StringValue needle,
                                @Optional("0") int offset,
                                @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    haystack = haystack.convertToUnicode(env, encoding);
    needle = needle.convertToUnicode(env, encoding);

    return StringModule.strpos(env, haystack, needle, offset);
  }

  /**
   * Multibyte version of strrpos.
   */
  public static Value mb_strrpos(Env env,
                                 StringValue haystack,
                                 StringValue needle,
                                 @Optional Value offsetV,
                                 @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    haystack = haystack.convertToUnicode(env, encoding);
    needle = needle.convertToUnicode(env, encoding);

    return StringModule.strrpos(env, haystack, needle, offsetV);
  }

  /**
   * Converts all characters to lower-case.
   */
  public static StringValue mb_strtolower(Env env,
                                          StringValue str,
                                          @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    StringValue unicodeStr = str.convertToUnicode(env, encoding);
    unicodeStr = StringModule.strtolower(str);

    return str.create(env, unicodeStr, encoding);
  }

  /**
   * Converts all characters to upper-case.
   */
  public static StringValue mb_strtoupper(Env env,
                                          StringValue str,
                                          @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    StringValue unicodeStr = str.convertToUnicode(env, encoding);
    unicodeStr = StringModule.strtoupper(str);

    return str.create(env, unicodeStr, encoding);
  }

  /**
   * Returns the width of this multibyte string.
   */
  public static LongValue mb_strwidth(Env env,
                              StringValue str,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    str = str.convertToUnicode(env, encoding);

    return LongValue.create(str.length());

/*
    int width = 0;
    int len = string.length();

    // Per PHP manual
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      if (ch <= 0x19)
        continue;
      else if (ch <= 0x1fff)
        width += 1;
      else if (ch <= 0xff60)
        width += 2;
      else if (ch <= 0xff9f)
        width += 1;
      else
        width += 2;
    }

    return LongValue.create(width);
*/
  }

  /**
   * Sets the character to use when decoding/encoding fails on a character.
   */
  public static Value mb_substitute_character(Value substrchar)
  {
    throw new UnimplementedException("mb_substitute_character");
  }

  public static LongValue mb_substr_count(Env env,
                              StringValue haystack,
                              StringValue needle,
                              @Optional("") String encoding)
  {
    encoding = getEncoding(env, encoding);

    haystack = haystack.convertToUnicode(env, encoding);
    needle = needle.convertToUnicode(env, encoding);

    int count = 0;
    int sublen = needle.length();

    int i = haystack.indexOf(needle);

    while (i >= 0) {
      i = haystack.indexOf(needle, i + sublen);
      count++;
    }

    return LongValue.create(count);
  }

  /**
   * Multibyte version of substr.
   */
  public static StringValue mb_substr(Env env,
                                      StringValue str,
                                      int start,
                                      @Optional Value lengthV,
                                      @Optional String encoding)
  {
    encoding = getEncoding(env, encoding);

    StringValue unicodeStr = str.convertToUnicode(env, encoding);

    Value val = StringModule.substr(env, unicodeStr, start, lengthV);

    if (val == BooleanValue.FALSE)
      return str.EMPTY;
    
    return encode(env, val.toStringValue(), encoding);
  }


  // Private helper functions

  /**
   * Returns string with words capitalized and intermediate letters are
   * made lower-case.
   */
  private static CharSequence toUpperCaseTitle(Env env, CharSequence str)
  {
    StringBuilder sb = new StringBuilder();

    int strLen = str.length();
    boolean isWordStart = true;

    for (int i = 0; i < strLen; i++) {
      char ch = str.charAt(i);

      switch (ch) {
      case ' ': case '\t': case '\r': case '\n':
        isWordStart = true;
        sb.append(ch);
        break;
      default:
        if (isWordStart) {
          sb.append(Character.toUpperCase(ch));
          isWordStart = false;
        }
        else
          sb.append(Character.toLowerCase(ch));
        break;
      }
    }

    return sb;
  }
  
  private static CharSequence decode(Env env,
                                     StringValue str,
                                     String encoding)
  {
    if (str.isUnicode())
      return str;
    
    Decoder decoder = getDecoder(env, encoding);
    
    return decoder.decode(env, str);
  }
  
  private static Decoder getDecoder(Env env, String encoding)
  {
    Decoder decoder = Decoder.create(encoding);
    
    String ini = env.getIniString("mbstring.substitute_character");
    
    if (ini == null) {
      decoder.setReplacement("?");
    }
    else if (ini.equalsIgnoreCase("none")) {
      decoder.setIgnoreErrors(true);
    }
    else if (ini.equalsIgnoreCase("long")) {
      decoder.setReplaceUnicode(true);
    }
    else {
      int len = ini.length();
      
      int value = 0;
      
      for (int i = 0; i < len; i++) {
        char ch = ini.charAt(i);
        
        if ('0' <= ch && ch <= '9')
          value = value * 10 + ini.charAt(i) - '0';
        else
          break;
      }
      
      // XXX: surrogate pairs
      decoder.setReplacement("" + (char) value);
    }
    
    return decoder;
  }
  
  private static StringValue encode(Env env,
                                    CharSequence str,
                                    String encoding)
  {
    Encoder encoder = getEncoder(env, encoding);

    return encoder.encode(env, str);
  }
  
  private static Encoder getEncoder(Env env, String encoding)
  {
    Encoder encoder = Encoder.create(encoding);
    
    String ini = env.getIniString("mbstring.substitute_character");
    
    if (ini == null) {
      encoder.setReplacement("?");
    }
    else if (ini.equalsIgnoreCase("none")) {
      encoder.setIgnoreErrors(true);
    }
    else if (ini.equalsIgnoreCase("long")) {
      encoder.setReplaceUnicode(true);
    }
    else {
      int len = ini.length();
      
      int value = 0;
      
      for (int i = 0; i < len; i++) {
        char ch = ini.charAt(i);
        
        if ('0' <= ch && ch <= '9')
          value = value * 10 + ini.charAt(i) - '0';
        else
          break;
      }
      
      // XXX: surrogate pairs
      encoder.setReplacement("" + (char) value);
    }
    
    return encoder;
  }

  public static String getEncoding(Env env)
  {
    Value encoding = env.getIni("mbstring.internal_encoding");
    
    if (encoding.length() > 0)
      return encoding.toString();
    else
      return env.getRuntimeEncoding();
  }

  private static String getEncoding(Env env, String encoding)
  {
    if (encoding == null || encoding.length() == 0)
      return getEncoding(env);
    else
      return encoding;
  }

  private static void setEncoding(Env env, String encoding)
  {
    env.setIni("mbstring.internal_encoding", encoding);
  }
  
  private static String getOutputEncoding(Env env)
  {
    Value encoding = env.getIni("mbstring.http_output");
    
    if (encoding.length() != 0)
      return encoding.toString();
    else
      return env.getOutputEncoding();
  }
  
  private static ArrayList<String> getEncodingList(Env env, Value encodingV)
  {
    ArrayList<String> list = new ArrayList<String>();
    
    if (encodingV.isDefault()) {
      list.add(getEncoding(env));
    }
    else if (encodingV.isArray()) {
      Iterator<Value> iter = encodingV.getValueIterator(env);
      
      while (iter.hasNext()) {
        list.add(iter.next().toString());
      }
    }
    else {
      String encodings = encodingV.toString();
      
      if (encodings.equals("auto")) {
        list.add("ASCII");
        list.add("JIS");
        list.add("UTF-8");
        list.add("EUC-JP");
        list.add("SJIS");
      }
      else {
        int start = 0;
        int index;
        
        while ((index = encodings.indexOf(",", start)) >= 0) {
          String charset = encodings.substring(start, index).trim();
          
          start = index + 1;
          
          list.add(charset);
        }
        
        list.add(encodings.substring(start).trim());
      }
    }
    
    return list;
  }

  /**
   * Recursively decodes objects and arrays.
   */
  private static Value decodeAll(Env env,
                                 Value val,
                                 String encoding)
  {
    Decoder decoder = getDecoder(env, encoding);
    
    return decodeAll(env, val, decoder);
  }
  
  /**
   * Recursively decodes objects and arrays.
   */
  private static Value decodeAll(Env env,
                                 Value val,
                                 Decoder decoder)
  {
    decoder.reset();
    
    val = val.toValue();

    if (val.isString()) {
      return decoder.decodeUnicode(env, val.toStringValue());
    }

    else if (val.isArray()) {
      ArrayValue array = new ArrayValueImpl();

      for (Map.Entry<Value,Value> entry : ((ArrayValue)val).entrySet()) {
        array.put(entry.getKey(),
                  decodeAll(env, entry.getValue(), decoder));
      }

      return array;
    } else if (val.isObject()) {

      ObjectValue obj = (ObjectValue) val.toObject(env);

      for (Map.Entry<Value,Value> entry : obj.entrySet()) {
        obj.putThisField(env,
                         entry.getKey().toStringValue(),
                         decodeAll(env, entry.getValue(), decoder));
      }

      return obj;
    } else
      return val;
  }

  /**
   * Recursively encodes objects and arrays.
   */
  private static Value encodeAll(Env env,
                                 Value val,
                                 String encoding)
  {
    Encoder encoder = getEncoder(env, encoding);
    
    return encodeAll(env, val, encoder);
  }
  
  /**
   * Recursively encodes objects and arrays.
   */
  private static Value encodeAll(Env env,
                                 Value val,
                                 Encoder encoder)
  {
    val = val.toValue();

    if (val.isString()) {
      return encoder.encode(env, val.toStringValue(), true);
    }
    else if (val.isArray()) {
      ArrayValue array = new ArrayValueImpl();

      for (Map.Entry<Value,Value> entry : ((ArrayValue)val).entrySet()) {
        array.put(entry.getKey(),
                  encodeAll(env, entry.getValue(), encoder));
      }

      return array;
    } else if (val.isObject()) {

      ObjectValue obj = (ObjectValue)val;

      for (Map.Entry<Value,Value> entry : obj.entrySet()) {
        obj.putThisField(env,
                         entry.getKey().toStringValue(),
                         encodeAll(env, entry.getValue(), encoder));
      }

      return obj;
    } else
      return val;
  }

  /**
   * ereg state object (saves previous match and other info)
   *
   * XXX: option
   */
  static class EregSearch {
    private StringValue _string;
    private UnicodeEregi _ereg;
    private Value _option;
    private int _length;

    ArrayValue _lastMatch;
    int _position;
    boolean _isValidRegexp;

    EregSearch(Env env,
               StringValue string,
               UnicodeEregi ereg,
               Value option)
    {
      _string = string.convertToUnicode(env, getEncoding(env));
      _position = 0;
      _length = _string.length();
      
      _ereg = ereg;
      _isValidRegexp = ereg != null;
      
      _option = option;
    }
    
    void init(UnicodeEregi ereg, Value option)
    {
      _ereg = ereg;
      _isValidRegexp = ereg != null;
      
      _option = option;
    }

    StringValue getString(Env env)
    {
      if (_position == 0)
        return _string;
      else if (_position < _length)
        return _string.substring(_position);
      else
        return _string.EMPTY;
    }

    Value search(Env env, boolean isArrayReturn)
    {
      if (_position < 0)
        return BooleanValue.FALSE;
      
      StringValue string = getString(env);

      ArrayValue regs = new ArrayValueImpl();
      Value val = eregImpl(env, _ereg, string, regs);

      if (val == BooleanValue.FALSE)
        return BooleanValue.FALSE;

      StringValue match = regs.get(LongValue.ZERO).toStringValue();

      int matchIndex = _string.indexOf(match, _position);
      int matchLength = match.length();

      _position = matchIndex + matchLength;

      _lastMatch = regs;

      if (isArrayReturn) {
        ArrayValue array = new ArrayValueImpl();

        array.put(LongValue.create(matchIndex));
        array.put(LongValue.create(matchLength));

        return array;

      } else
        return BooleanValue.TRUE;
    }
  }

  static final IniDefinition INI_MBSTRING_HTTP_INPUT
    = _iniDefinitions.add("mbstring.http_input", "pass", PHP_INI_ALL);
  static final IniDefinition INI_MBSTRING_HTTP_OUTPUT
    = _iniDefinitions.add("mbstring.http_output", "pass", PHP_INI_ALL);
}
