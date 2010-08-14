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
 * @author Emil Ong
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.*;
import com.caucho.util.URLUtil;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Implements the built-in URL rewriter for passing session ids and other
 * variables.
 */
public class UrlRewriterCallback extends CallbackFunction {
  private StringBuilder _rewriterQuery = new StringBuilder();
  private ArrayList<String[]> _rewriterVars = new ArrayList<String[]>();

  public UrlRewriterCallback(Env env)
  {
    super(env, "URL-Rewriter");

    try {
      Method rewriterMethod = 
        UrlRewriterCallback.class.getMethod("_internal_url_rewriter",
                                            Env.class, Value.class);
      setFunction(new JavaMethod(env.getModuleContext(), rewriterMethod));
    } catch (NoSuchMethodException e) {
    } catch (SecurityException e) {
    }
  }

  /**
   * Returns the unique rewriter.
   */
  public static UrlRewriterCallback getInstance(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    for (; ob != null; ob = ob.getNext()) {
      Callable callback = ob.getCallback();

      if (callback instanceof UrlRewriterCallback)
        return (UrlRewriterCallback)callback;
    }

    return null;
  }

  /**
   * Adds a rewrite variable.  Intended for 
   * <code>output_add_rewrite_var()</code>.
   */
  public void addRewriterVar(String var, String value)
  {
    if (_rewriterQuery.length() > 0)
      _rewriterQuery.append("&");

    String encodedVar = URLUtil.encodeURL(var.replaceAll(" ", "+"));
    String encodedValue = URLUtil.encodeURL(value.replaceAll(" ", "+"));

    _rewriterQuery.append(encodedVar + "=" + encodedValue);
    _rewriterVars.add(new String[] {encodedVar, encodedValue});
  }

  /**
   * Resets (clears) all the rewrite variables.  Intended for 
   * <code>output_reset_rewrite_vars()</code>.
   */
  public void resetRewriterVars()
  {
    _rewriterQuery = new StringBuilder();
    _rewriterVars.clear();
  }
  
  /**
   * Callback function to rewrite URLs to include session information.
   * Note that this function should return BooleanValue.FALSE in the
   * case where data should be discarded.
   */
  public static Value _internal_url_rewriter(Env env, Value buffer)
  {
    Value result;
    UrlRewriterCallback rewriter = getInstance(env);

    // We should never have been called in this case, but 
    // return the buffer unmodified anyway.

    if (rewriter == null)
      result = buffer;
    else {
      // Return the buffer unmodified when no urls are rewritten
      // php/1k6x

      Parser parser = rewriter.new Parser(buffer.toString(), env);
      result = parser.parse();

      if (result.isNull())
        result = buffer;
    }

    return result;
  }

  private class Parser {
    private Env _env;

    private boolean _includeSessionInfo = false;
    private String _sessionName = null;
    private String _sessionId = null;
    private String _javaSessionName = null;
    private String _javaSessionId = null;

    private int _index;
    private String _value;
    private boolean _quoted;

    private String _input;
    private StringValue _output;

    public Parser(String input, Env env)
    {
      _input = input;
      _env = env;
      _index = 0;
      _output = env.createUnicodeBuilder();
    }

    public Value parse()
    {
      if (_env.getSession() != null && _env.getJavaSession() != null
          && _env.getIni("session.use_trans_sid").toBoolean()) {
        _includeSessionInfo = true;

        _sessionName = _env.getIni("session.name").toString();
        _sessionId = _env.getSession().getId();

        _javaSessionName = _env.getQuercus().getCookieName();

        _javaSessionId = _env.getJavaSession().getId();
      }

      if (_includeSessionInfo == false && _rewriterVars.isEmpty())
        return NullValue.NULL;

      String [] tagPairs = 
        _env.getIni("url_rewriter.tags").toString().split(",");
      HashMap<String,String> tags = new HashMap<String,String>();

      for (String tagPair : tagPairs) {
        String [] tagAttribute = tagPair.split("=");

        switch (tagAttribute.length) {
          case 1:
            tags.put(tagAttribute[0], null);
            break;

          case 2:
            tags.put(tagAttribute[0], tagAttribute[1]);
            break;

          default:
            break;
        }
      }

      for (String tag = getNextTag(); tag != null; tag = getNextTag()) {
        if (tags.containsKey(tag)) {
          String attribute = tags.get(tag);
          
          if (attribute == null) {
            consumeToEndOfTag();
            
            if (_includeSessionInfo) {
              String phpSessionInputTag = 
                "<input type=\"hidden\" name=\"" + _sessionName + "\""
                    + " value=\"" + _sessionId + "\" />";

              _output.append(phpSessionInputTag);
            }

            for (String[] entry : _rewriterVars) {
              String inputTag = 
                "<input type=\"hidden\" name=\"" + entry[0] + "\""
                    + " value=\"" + entry[1] + "\" />";
              _output.append(inputTag);
            }
          } else {
            int valueEnd = 0;

            for (valueEnd = getNextAttribute(attribute);
                 valueEnd == 0;
                 valueEnd = getNextAttribute(attribute))
            {
              // intentionally empty
              // TODO: thats a bad smell! refactor
            }

            if (valueEnd > 0) {
              _output.append(rewriteUrl(_value));

              if (_quoted)
                consumeOneCharacter();
            }
          }
        }
      }

      return _output;
    }

    /**
     * Finds the next tag in the string returns it.
     */
    private String getNextTag()
    {
      int tagStart = _input.indexOf('<', _index);

      if (tagStart < 0) {
        _output.append(_input.substring(_index));
        return null;
      }

      // consume everything upto the tag opening
      _output.append(_input.substring(_index, tagStart + 1));

      // skip the '<'
      _index = tagStart + 1;
      
      consumeNonWhiteSpace();

      return _input.substring(tagStart + 1, _index);
    }

    /**
     * Finds the next attribute matching the given name.
     *
     * @return -1 if no more valid attributes can be found, 0 if the next
     * attribute is not the one sought, and 1 if the attribute was found.  
     *
     * The _index pointer will refer to the end position for the value
     * in the _input in the final case, but only those characters up to 
     * the beginning of the value will have been copied to the output.
     */
    private int getNextAttribute(String attribute)
    {
      consumeWhiteSpace();

      int attributeStart = _index;

      while (_index < _input.length()
          && isValidAttributeCharacter(_input.charAt(_index)))
        consumeOneCharacter();
        
      // no valid attribute was found (we're probably at the end of the tag)
      if (_index == attributeStart)
        return -1;

      String foundAttribute = _input.substring(attributeStart, _index);

      consumeWhiteSpace();

      // Any attributes that we will affect are of the form attr=value
      if (_input.length() <= _index || _input.charAt(_index) != '=')
        return -1;

      consumeOneCharacter();

      consumeWhiteSpace();

      // check for quoting
      char quote = ' ';
      
      if (_input.charAt(_index) == '"' || _input.charAt(_index) == '\'') {
        _quoted = true;
        quote = _input.charAt(_index);

        consumeOneCharacter();
      }

      int valueEnd = _index;
      
      if (_quoted) {
        valueEnd = _input.indexOf(quote, _index);
      
        // try to account for unclosed quotes
        int tagEnd = _input.indexOf('>', _index);

        if (valueEnd < 0) {
          if (tagEnd > 0) 
            valueEnd = tagEnd;
          else
            valueEnd = _input.length();
        }
      } else {
        // skip to the end of the value
        for (valueEnd = _index; 
             valueEnd < _input.length()
                 && _input.charAt(valueEnd) != '/'
                 && _input.charAt(valueEnd) != '>'
                 && _input.charAt(valueEnd) != ' ';
             valueEnd++) {
          // intentionally left empty
        }
      }

      if (foundAttribute.equals(attribute)) {
        _value = _input.substring(_index, valueEnd);

        _index = valueEnd;

        return 1;
      } else {
        // make sure to skip the complete attribute if it's not the
        // one we're looking for.
        if (_quoted)
          valueEnd += 1;

        _output.append(_input.substring(_index, valueEnd));

        _index = valueEnd;

        return 0;
      }
    }

    private void consumeOneCharacter()
    {
      if (_index < _input.length()) {
        _output.append(_input.charAt(_index));
        _index += 1;
      }
    }

    private void consumeWhiteSpace()
    {
      while (_index < _input.length()
          && Character.isWhitespace(_input.charAt(_index)))
        consumeOneCharacter();
    }

    private void consumeNonWhiteSpace()
    {
      while (_index < _input.length()
          && !Character.isWhitespace(_input.charAt(_index)))
        consumeOneCharacter();
    }

    private void consumeToEndOfTag()
    {
      while (_input.charAt(_index) != '>')
        consumeOneCharacter();

      // consume the '>'
      consumeOneCharacter();
    }

    private boolean isValidAttributeCharacter(char ch)
    {
      return Character.isLetterOrDigit(ch)
          || (ch == '-') || (ch == '.') || (ch == '_') || (ch == ':');
    }

    private String rewriteUrl(String urlString)
    {
      // according to php documentation, it only adds tags to the
      // end of relative URLs, but according to RFC 2396, any
      // URI beginning with '/' (e.g. <a href="/foo">link</a>) is
      // absolute.  Nonetheless, php does add session ids to these
      // links.  Thus php must be defining "relative" as relative
      // to the host, not the hierarchy.  Thus we only check to make
      // sure that the scheme and authority are undefined, not that
      // the first character of the path begins with '/'.

      URI uri;

      try {
        uri = new URI(urlString);
      } catch (URISyntaxException e) {
        return urlString;
      }

      if ((uri.getScheme() != null) || (uri.getAuthority() != null)) {
        return urlString;
      }

      StringBuilder query = new StringBuilder();

      if (uri.getQuery() != null) {
        query.append("?");
        query.append(uri.getQuery());
        query.append("&");
      } 
      else
        query.append("?");

      if (_includeSessionInfo) {
        query.append(_sessionName);
        query.append("=");
        query.append(_sessionId);
      }

      if (_rewriterQuery.length() != 0) {
        if (_includeSessionInfo)
          query.append("&");

        query.append(_rewriterQuery);
      }

      StringBuilder newUri = new StringBuilder();

      if (uri.getPath() != null)
        newUri.append(uri.getPath());

      newUri.append(query);

      if (uri.getFragment() != null) {
        newUri.append("#");
        newUri.append(uri.getFragment());
      }

      return newUri.toString();
    }
  }
}
