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

import com.caucho.config.ConfigException;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Configuration for a filter.
 */
public class FilterMapping extends FilterConfigImpl {
  static L10N L = new L10N(FilterMapping.class);

  private String _urlPattern;
  private final LinkedHashSet<String> _urlPatterns
    = new LinkedHashSet<String>();
  
  private final ArrayList<String> _servletNames = new ArrayList<String>();
  
  // The match expressions
  private final ArrayList<Match> _matchList = new ArrayList<Match>();

  private HashSet<DispatcherType> _dispatcher;
  
  /**
   * Creates a new filter mapping object.
   */
  public FilterMapping()
  {
  }

  /**
   * Sets the url pattern
   */
  public URLPattern createUrlPattern()
    throws ServletException
  {
    return new URLPattern();
  }

  /**
   * Gets the url pattern
   */
  public String getURLPattern()
  {
    return _urlPattern;
  }

  public HashSet<String> getURLPatterns()
  {
    return _urlPatterns;
  }

  /**
   * Sets the url regexp
   */
  public void setURLRegexp(String pattern)
    throws ServletException
  {
    Pattern regexp;
    
    if (CauchoSystem.isCaseInsensitive())
      regexp = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    else
      regexp = Pattern.compile(pattern, 0);

    _matchList.add(Match.createInclude(regexp));
  }

  /**
   * Sets the servlet name
   */
  public void addServletName(String servletName)
  {
    if (servletName == null)
      throw new NullPointerException();
    
    _servletNames.add(servletName);
  }

  public ArrayList<String> getServletNames()
  {
    return _servletNames;
  }

  /**
   * Adds a dispatcher.
   */
  public void addDispatcher(String dispatcher)
    throws ConfigException
  {
    try {
      addDispatcher(DispatcherType.valueOf(dispatcher));
    }
    catch (IllegalArgumentException e) {
      throw new ConfigException(L.l("'{0}' is an unknown value for <dispatcher>  'REQUEST', 'FORWARD', 'INCLUDE', and 'ERROR' are the valid values.",
                                    dispatcher));
    }
  }

  public void addDispatcher(DispatcherType dispather)
  {
    if (_dispatcher == null)
      _dispatcher = new HashSet<DispatcherType>();

    _dispatcher.add(dispather);
  }

  /**
   * True if the dispatcher is for REQUEST.
   */
  public boolean isRequest()
  {
    return _dispatcher == null || _dispatcher.contains(DispatcherType.REQUEST);
  }

  /**
   * True if the dispatcher is for INCLUDE.
   */
  public boolean isInclude()
  {
    return _dispatcher != null && _dispatcher.contains(DispatcherType.INCLUDE);
  }

  /**
   * True if the dispatcher is for FORWARD.
   */
  public boolean isForward()
  {
    return _dispatcher != null && _dispatcher.contains(DispatcherType.FORWARD);
  }

  /**
   * True if the dispatcher is for ERROR.
   */
  public boolean isError()
  {
    return _dispatcher != null && _dispatcher.contains(DispatcherType.ERROR);
  }

  /**
   * Returns true if the filter map matches the invocation URL.
   *
   * @param servletName the servlet name to match
   */
  boolean isMatch(String servletName)
  {
    for (int i = 0; i < _servletNames.size(); i++) {
      String matchName = _servletNames.get(i);

      if (matchName.equals(servletName) || "*".equals(matchName))
        return true;
    }

    return false;
  }

  /**
   * Returns true if the filter map matches the invocation URL.
   *
   * @param invocation the request's invocation
   */
  boolean isMatch(ServletInvocation invocation)
  {
    return isMatch(invocation.getServletPath(), invocation.getPathInfo());
  }

  /**
   * Returns true if the filter map matches the servlet path and path info.
   * */
  public boolean isMatch(String servletPath, String pathInfo)
  {
    String uri;

    if (pathInfo == null)
      uri = servletPath;
    else if (servletPath == null)
      uri = pathInfo;
    else
      uri = servletPath + pathInfo;

    int size = _matchList.size();
    
    for (int i = 0; i < size; i++) {
      Match match = _matchList.get(i);

      int value = match.match(uri);

      switch (value) {
      case Match.INCLUDE:
        return true;
      case Match.EXCLUDE:
        return false;
      }
    }

    return false;
  }

  /**
   * Converts a url-pattern to a regular expression
   *
   * @param pattern the url-pattern to convert
   * @param flags the regexp flags, e.g. "i" for case insensitive
   *
   * @return the equivalent regular expression
   */
  private Pattern urlPatternToRegexp(String pattern, int flags)
    throws ServletException
  {
    if (pattern.length() == 0 ||
        pattern.length() == 1 && pattern.charAt(0) == '/') {
      try {
        return Pattern.compile("^/$", flags);
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }

    int length = pattern.length();
    boolean isExact = true;
      
    if (pattern.charAt(0) != '/' && pattern.charAt(0) != '*') {
      pattern = "/" + pattern;
      length++;
    }

    int prefixLength = -1;
    boolean isShort = false;
    CharBuffer cb = new CharBuffer();
    cb.append("^");
    for (int i = 0; i < length; i++) {
      char ch = pattern.charAt(i);
      
      if (ch == '*' && i + 1 == length && i > 0) {
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
        isExact = false;
        cb.append(".*");
        if (prefixLength < 0)
          prefixLength = i;
        
        if (i == 0)
          isShort = true;
      }
      else if (ch == '.' || ch == '[' || ch == '^' || ch == '$' ||
               ch == '{' || ch == '}' || ch == '|' ||
               ch == '(' || ch == ')' || ch == '?') {
        cb.append('\\');
        cb.append(ch);
      }
      else
        cb.append(ch);
    }

    if (isExact)
      cb.append('$');
    else
      cb.append("(?=/)|" + cb.toString() + "$");

    try {
      return Pattern.compile(cb.close(), flags);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Returns a printable representation of the filter config object.
   */
  public String toString()
  {
    return "FilterMapping[pattern=" + _urlPattern + ",name=" + getFilterName() + "]";
  }

  public class URLPattern {
    boolean _hasInclude = false;

    /**
     * Sets the singleton url-pattern.
     */
    public URLPattern addText(String pattern)
      throws ServletException
    {
      pattern = pattern.trim();
      
      _urlPattern = pattern;
      _urlPatterns.add(pattern);

      Pattern regexp;
      
      if (CauchoSystem.isCaseInsensitive())
        regexp = urlPatternToRegexp(pattern, Pattern.CASE_INSENSITIVE);
      else
        regexp = urlPatternToRegexp(pattern, 0);

      _hasInclude = true;
      
      _matchList.add(Match.createInclude(regexp));

      return this;
    }

    /**
     * Adds an include pattern.
     */
    public void addIncludePattern(String pattern)
      throws ServletException
    {
      pattern = pattern.trim();

      Pattern regexp;
      
      if (CauchoSystem.isCaseInsensitive())
        regexp = urlPatternToRegexp(pattern, Pattern.CASE_INSENSITIVE);
      else
        regexp = urlPatternToRegexp(pattern, 0);

      _hasInclude = true;
      
      _matchList.add(Match.createInclude(regexp));
    }

    /**
     * Adds an exclude pattern.
     */
    public void addExcludePattern(String pattern)
      throws ServletException
    {
      pattern = pattern.trim();
      
      Pattern regexp;
      
      if (CauchoSystem.isCaseInsensitive())
        regexp = urlPatternToRegexp(pattern, Pattern.CASE_INSENSITIVE);
      else
        regexp = urlPatternToRegexp(pattern, 0);

      _matchList.add(Match.createExclude(regexp));
    }

    /**
     * Adds an include regexp.
     */
    public void addIncludeRegexp(String pattern)
    {
      pattern = pattern.trim();

      Pattern regexp;
      
      if (CauchoSystem.isCaseInsensitive())
        regexp = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
      else
        regexp = Pattern.compile(pattern, 0);

      _hasInclude = true;
      
      _matchList.add(Match.createInclude(regexp));
    }

    /**
     * Adds an exclude regexp.
     */
    public void addExcludeRegexp(String pattern)
    {
      pattern = pattern.trim();

      Pattern regexp;
      
      if (CauchoSystem.isCaseInsensitive())
        regexp = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
      else
        regexp = Pattern.compile(pattern, 0);

      _matchList.add(Match.createExclude(regexp));
    }

    /**
     * Initialize, adding the all-match for exclude patterns.
     */
    @PostConstruct
    public void init()
      throws Exception
    {
      if (_matchList.size() > 0 && ! _hasInclude) {
        Pattern regexp = Pattern.compile("");
        _matchList.add(Match.createInclude(regexp));
      }
    }
  }
    
  static class Match {
    static final int INCLUDE = 1;
    static final int EXCLUDE = -1;
    static final int NO_MATCH = 0;
    
    private final Pattern _regexp;
    private final int _value;

    private Match(Pattern regexp, int value)
    {
      _regexp = regexp;
      _value = value;
    }

    /**
     * Creates an include pattern.
     */
    static Match createInclude(Pattern regexp)
    {
      return new Match(regexp, INCLUDE);
    }

    /**
     * Creates an exclude pattern.
     */
    static Match createExclude(Pattern regexp)
    {
      return new Match(regexp, EXCLUDE);
    }

    /**
     * Returns the match value.
     */
    int match(String uri)
    {
      if (_regexp.matcher(uri).find())
        return _value;
      else
        return NO_MATCH;
    }
  }
}
