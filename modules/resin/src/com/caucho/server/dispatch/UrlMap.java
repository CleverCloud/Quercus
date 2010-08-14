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

package com.caucho.server.dispatch;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.*;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Maps uris to objects, using the syntax in the servlet2.2 deployment
 * descriptors:
 *
 * /foo/bar   -- exact match
 * /foo/bar/* -- matches anything with the /foo/bar prefix
 * *.jsp      -- matches anything with the .jsp suffix
 */
public class UrlMap<E> {
  private static final L10N L = new L10N(UrlMap.class);

  // List of matching regular expressions
  private ArrayList<RegexpEntry<E>> _regexps;
  
  /**
   * Create a new map
   */
  public UrlMap()
  {
    _regexps = new ArrayList<RegexpEntry<E>>();
  }

  /**
   * Create a new map preferring a short match.
   *
   * @param bestShort if true, use the shortest match
   */
  public UrlMap(boolean bestShort)
  {
    _regexps = new ArrayList<RegexpEntry<E>>();
  }

  /**
   * If set to true, this map uses the shortest match instead of the
   * longest.
   *
   * @param bestShort if true, use the shortest match
   */
  void setBestShort(boolean bestShort)
  {
  }

  boolean contains(Filter<E> filter)
  {
    for (int i = _regexps.size() - 1; i >= 0; i--) {
      RegexpEntry<E> regexp = _regexps.get(i);

      if (filter.isMatch(regexp.getValue())) {
        return true;
      }
    }

    return false;
  }

  int size()
  {
    return _regexps.size();
  }

  public void addMap(String pattern, E value, boolean isIgnore)
    throws PatternSyntaxException
  {
    addMap(pattern, null, value, isIgnore, false);
  }

  public void addMap(String pattern, E value)
    throws PatternSyntaxException
  {
    addMap(pattern, null, value, false, false);
  }

  public void addMap(String pattern, 
                     E value,
                     boolean isIgnore,
                     boolean ifAbsent)
    throws PatternSyntaxException
  {
    addMap(pattern, null, value, isIgnore, ifAbsent);
  }

  /**
   * Adds a new url-pattern and its corresponding value to the map
   *
   * @param pattern servlet2.2 url-pattern
   * @param value object stored as the value
   */
  public void addMap(String pattern,
                     String flags, 
                     E value,
                     boolean isIgnore,
                     boolean ifAbsent)
    throws PatternSyntaxException
  {
    boolean startsWithSlash = pattern.charAt(0) == '/';

    if (pattern.length() == 0
        || pattern.length() == 1 && startsWithSlash) {
      addRegexp(-1, "", flags, value, true, isIgnore, ifAbsent);
      return;
    }

    else if (pattern.equals("/*")) {
      addRegexp(1, "/*", flags, value, true, isIgnore, ifAbsent);
      return;
    }

    int length = pattern.length();
    boolean isExact = true;

    if (! startsWithSlash && pattern.charAt(0) != '*') {
      pattern = "/" + pattern;
      length++;
    }

    int prefixLength = -1;
    boolean isShort = false;
    boolean hasWildcard = false;
    CharBuffer cb = new CharBuffer();
    cb.append("^");
    for (int i = 0; i < length; i++) {
      char ch = pattern.charAt(i);

      if (ch == '*' && i + 1 == length && i > 0) {
        hasWildcard = true;
        isExact = false;

        if (pattern.charAt(i - 1) == '/') {
          cb.setLength(cb.length() - 1);

          if (prefixLength < 0)
            prefixLength = i - 1;

        }
        else if (prefixLength < 0)
          prefixLength = i;

        if (prefixLength == 0)
          prefixLength = 1;
      }
      else if (ch == '*') {
        hasWildcard = true;
        isExact = false;
        cb.append(".*");
        if (prefixLength < 0)
          prefixLength = i;

        if (i == 0)
          isShort = true;
      }
      else if (ch == '.' || ch == '[' || ch == '^' || ch == '$'
               || ch == '{' || ch == '}' || ch == '|'
               || ch == '(' || ch == ')' || ch == '?') {
        cb.append('\\');
        cb.append(ch);
      }
      else
        cb.append(ch);
    }

    if (isExact)
      cb.append("$");
    else {
      cb.append("(?=/)|" + cb.toString() + "\\z");
    }

    if (prefixLength < 0)
      prefixLength = pattern.length();
    else if (prefixLength < pattern.length()
             && pattern.charAt(prefixLength) == '/')
      prefixLength--;

    if (cb.length() > 0 && cb.charAt(0) == '/')
      cb.insert(0, '^');

    addRegexp(prefixLength, pattern, cb.close(), flags, value,
              isShort, isIgnore, ifAbsent, ! hasWildcard);
  }

  public static String urlPatternToRegexpPattern(String pattern)
  {
    if (pattern.length() == 0
        || pattern.length() == 1 && pattern.charAt(0) == '/') {
      return "^.*$";
    }

    else if (pattern.equals("/*"))
      return "^.*$";

    int length = pattern.length();

    if (pattern.charAt(0) != '/' && pattern.charAt(0) != '*') {
      pattern = "/" + pattern;
      length++;
    }

    boolean isExact = true;
    CharBuffer cb = new CharBuffer();
    cb.append("^");
    for (int i = 0; i < length; i++) {
      char ch = pattern.charAt(i);

      if (ch == '*' && i + 1 == length && i > 0) {
        isExact = false;

        if (pattern.charAt(i - 1) == '/') {
          cb.setLength(cb.length() - 1);
        }
      }
      else if (ch == '*') {
        isExact = false;
        cb.append(".*");
      }
      else if (ch == '.' || ch == '[' || ch == '^' || ch == '$'
               || ch == '{' || ch == '}' || ch == '|'
               || ch == '(' || ch == ')' || ch == '?') {
        cb.append('\\');
        cb.append(ch);
      }
      else
        cb.append(ch);
    }

    if (isExact)
      cb.append("\\z");
    else
      cb.append("(?=/)|" + cb.toString() + "\\z");

    if (cb.length() > 0 && cb.charAt(0) == '/')
      cb.insert(0, '^');

    return cb.close();
  }

  /**
   * Adds a new url-pattern and its corresponding value to the map
   *
   * @param pattern servlet2.2 url-pattern
   * @param value object stored as the value
   */
  public void addStrictMap(String pattern, String flags,
                           E value)
    throws PatternSyntaxException, ServletException
  {
    boolean ifAbsent = false;
    
    if (pattern.length() == 0
        || pattern.length() == 1 && pattern.charAt(0) == '/') {
      addRegexp(-1, "^.*$", flags, value, true, false, ifAbsent);
      return;
    }

    int length = pattern.length();
    
    if (pattern.charAt(0) != '/' && pattern.charAt(0) != '*') {
      pattern = "/" + pattern;
      length++;
    }

    if (pattern.indexOf('*') < pattern.lastIndexOf('*'))
      throw new ServletException("at most one '*' is allowed");

    int prefixLength = -1;
    boolean isShort = false;
    CharBuffer cb = new CharBuffer();
    cb.append('^');

    for (int i = 0; i < length; i++) {
      char ch = pattern.charAt(i);

      switch (ch) {
      case '*':
        if (i > 0 && i + 1 == length && pattern.charAt(i - 1) == '/') {
          cb.append(".*");
        }
        else if (i == 0 && length > 1 && pattern.charAt(1) == '.'
                 && pattern.lastIndexOf('/') < 0) {
          cb.append(".*");
        }
        else
          throw new ServletException(L.l("illegal url-pattern '{0}'",
                                         pattern));
        break;

      case '.': case '[': case '^': case '$':
      case '{': case '}': case '|': case '(': case '?':
        cb.append('\\');
        cb.append(ch);
        break;

      default:
        cb.append(ch);
      }
    }

    cb.append("$");

    addRegexp(prefixLength, pattern, cb.close(), flags, value,
              isShort, false, ifAbsent, false);
  }

  public void addRegexp(String regexp, String flags, E value)
    throws PatternSyntaxException
  {
    addRegexp(0, regexp, flags, value, false, false, false);
  }

  public void addRegexp(String regexp, E value)
    throws PatternSyntaxException
  {
    addRegexp(0, regexp, null, value, false, false, false);
  }

  public void addRegexpIfAbsent(String regexp, E value)
    throws PatternSyntaxException
  {
    addRegexp(0, regexp, null, value, false, false, true);
  }

  /**
   * Adds a regular expression to the map.
   *
   * @param prefixLength the length of the pattern's mandatory prefix
   * @param regexp the regexp pattern to add
   * @param flags regexp flags, like "i" for case insensitive
   * @param value the value for matching the pattern
   * @param isShort if true, this regexp expects to be shorter than others
   */
  public void addRegexp(int prefixLength, 
                        String regexp,
                        String flags,
                        E value,
                        boolean isShort, 
                        boolean isIgnore,
                        boolean ifAbsent)
    throws PatternSyntaxException
  {
    RegexpEntry<E> entry
      = new RegexpEntry<E>(prefixLength, regexp, flags, value);

    for (int i = 0; i < _regexps.size(); i++) {
      RegexpEntry<E> re = _regexps.get(i);

      /*
      if (re.equals(entry)) {
        _regexps.remove(i);
        break;
      }
      */
      if (re.equals(entry)) {
        if (ifAbsent) {
          // server/1p1b - registration does not overwrite
          return;
        }
        else {
          _regexps.remove(i);
          break;
        }
      }
    }

    if (isShort)
      entry.setShortMatch();
    
    if (isIgnore)
      entry.setIgnore(true);

    _regexps.add(entry);
  }

  /**
   * Adds a regular expression to the map.
   *
   * @param prefixLength the length of the pattern's mandatory prefix
   * @param pattern the regexp pattern to add
   * @param regexp the regexp pattern to add
   * @param flags regexp flags, like "i" for case insensitive
   * @param value the value for matching the pattern
   * @param isShort if true, this regexp expects to be shorter than others
   */
  public void addRegexp(int prefixLength, String pattern,
                        String regexp, String flags,
                        E value, boolean isShort,
                        boolean isIgnore,
                        boolean ifAbsent,
                        boolean isSimple)
    throws PatternSyntaxException
  {
    RegexpEntry<E> entry
      = new RegexpEntry<E>(prefixLength, pattern, regexp, flags, value,
                           isIgnore, isSimple);

    for (int i = _regexps.size() - 1; i >= 0; i--) {
      RegexpEntry<E> re = _regexps.get(i);

      if (re.equals(entry)) {
        if (ifAbsent) {
          return;
        }
        else {
          _regexps.remove(i);
        }
      }
    }

    if (isShort)
      entry.setShortMatch();

    _regexps.add(entry);
  }

  /**
   * Finds the best match for the uri.  In the case of a servlet dispatch,
   * match is servletPath and replacement is pathInfo.
   *
   * @param uri uri to match
   *
   * @return matching object
   */
  public E map(String uri)
  {
    return map(uri, null);
  }

  public E map(String uri, ArrayList<String> vars)
  {
    return map(uri, vars, false);
  }
  /**
   * Finds the best match for the uri.  In the case of a servlet dispatch,
   * match is servletPath and replacement is pathInfo.
   *
   * @param uri uri to match
   * @param vars a list of the regexp variables.
   *
   * @return matching object
   */
  public E map(String uri, ArrayList<String> vars, boolean isWelcome)
  {
    E best = null;

    if (vars != null)
      vars.add(uri);

    int bestPrefixLength = -2;
    int bestMinLength = -2;
    
    for (int i = 0; i < _regexps.size(); i++) {
      RegexpEntry<E> entry = _regexps.get(i);

      if (isWelcome && ! entry.isSimple())
        continue;

      if (entry.isIgnore()) // plugin-match and plugin-ignore
        continue;
      if (entry._prefixLength < bestPrefixLength)
        continue;

      Matcher matcher = entry._regexp.matcher(uri);

      if (! matcher.find())
        continue;

      int begin = matcher.start();
      int end = matcher.end();

      int length = end - begin;

      // Earlier matches override later ones
      if (bestPrefixLength < entry._prefixLength || bestMinLength < length) {
        if (vars != null) {
          vars.clear();

          if ("/*".equals(entry.getPattern()))
            vars.add("");
          else
            vars.add(uri.substring(0, end));

          for (int j = 1; j <= matcher.groupCount(); j++)
            vars.add(matcher.group(j));
        }

        best = entry._value;
        bestPrefixLength = entry._prefixLength;
        if (! entry.isShortMatch())
          bestMinLength = length;

        if (bestMinLength < entry._prefixLength)
          bestMinLength = entry._prefixLength;
      }
    }

    return best;
  }

  /**
   * Return the matching url patterns.
   */
  public ArrayList<String> getURLPatterns()
  {
    ArrayList<String> patterns = new ArrayList<String>();

    for (int i = 0; i < _regexps.size(); i++) {
      RegexpEntry<E> entry = _regexps.get(i);

      String urlPattern = entry.getURLPattern();

      if (urlPattern != null)
        patterns.add(urlPattern);
    }

    return patterns;
  }

  static class RegexpEntry<E> {
    String _urlPattern;
    String _pattern;
    int _flags;
    Pattern _regexp;
    E _value;
    int _prefixLength;
    boolean _shortMatch;
    boolean _isIgnore; // plugin_match or plugin-ignore
    boolean _isSimple; //simple when does not start with a / and contains no *

    RegexpEntry(int prefixLength, String pattern, String flags, E value)
      throws PatternSyntaxException
    {
      this(prefixLength, pattern, pattern, flags, value, false, false);
    }

    RegexpEntry(int prefixLength, String urlPattern,
                String pattern, String flags, E value,
                boolean isIgnore, boolean isSimple)
      throws PatternSyntaxException
    {
      _urlPattern = urlPattern;
      _prefixLength = prefixLength;
      _pattern = pattern;

      if (flags == null && CauchoSystem.isCaseInsensitive())
        _flags = Pattern.CASE_INSENSITIVE;
      else if (flags != null && flags.equals("i"))
        _flags = Pattern.CASE_INSENSITIVE;

      _regexp = Pattern.compile(pattern, _flags);
      _value = value;
      _isIgnore = isIgnore;
      _isSimple = isSimple;
    }
    
    void setIgnore(boolean isIgnore)
    {
      _isIgnore = isIgnore;
    }

    boolean isIgnore()
    {
      return _isIgnore;
    }

    void setShortMatch()
    {
      _shortMatch = true;
    }

    boolean isShortMatch()
    {
      return _shortMatch;
    }

    String getURLPattern()
    {
      return _urlPattern;
    }

    String getPattern()
    {
      return _pattern;
    }

    E getValue()
    {
      return _value;
    }

    boolean isSimple()
    {
      return _isSimple;
    }

    public int hashCode()
    {
      if (_urlPattern != null)
        return _urlPattern.hashCode();
      else if (_pattern != null)
        return _pattern.hashCode();
      else
        return 17;
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof RegexpEntry<?>))
        return false;

      RegexpEntry<?> re = (RegexpEntry<?>) o;

      if (_urlPattern != null)
        return _urlPattern.equals(re._urlPattern);
      else if (_pattern != null)
        return _pattern.equals(re._pattern);
      else
        return false;
    }

    public String toString()
    {
      if (_urlPattern != null)
        return "RegexpEntry[" + _urlPattern + "]";
      else if (_pattern != null)
        return "RegexpEntry[" + _pattern + "]";
      else
        return super.toString();
    }
  }

  public interface Filter<X> {
    public boolean isMatch(X item);
  }
}
