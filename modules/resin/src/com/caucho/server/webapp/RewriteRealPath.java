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

package com.caucho.server.webapp;

import com.caucho.config.ConfigException;
import com.caucho.config.types.PathBuilder;
import com.caucho.server.dispatch.UrlMap;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration for a rewrite-real-path
 */
public class RewriteRealPath {
  static final L10N L = new L10N(RewriteRealPath.class);
  static final Logger log = Logger.getLogger(RewriteRealPath.class.getName());

  private Path _appDir;

  private final ArrayList<Program> _programList = new ArrayList<Program>();
  
  // path mapping (old path-mapping)
  private UrlMap<String> _pathMapping;

  public RewriteRealPath(Path appDir)
  {
    _appDir = appDir;
  }

  /**
   * Adds a rewrite
   */
  public void addRewrite(Rewrite rewrite)
  {
    _programList.add(rewrite);
  }

  /**
   * Adds a real-path.
   */
  public void addRealPath(RealPath realPath)
  {
    _programList.add(realPath);
  }
  
  /**
   * Adds a path pattern.
   */
  public void addPathPattern(String urlPattern, String realPath)
  {
    if (_pathMapping == null)
      _pathMapping = new UrlMap<String>();
    
    _pathMapping.addMap(urlPattern, realPath);
  }

  /**
   * Adds a path pattern.
   */
  public void addPathRegexp(String urlRegexp, String realPath)
  {
    if (_pathMapping == null)
      _pathMapping = new UrlMap<String>();
    
    _pathMapping.addRegexp(urlRegexp, realPath);
  }

  /**
   * Maps the path.
   */
  public String mapToRealPath(String uri)
  {
    for (int i = 0; i < _programList.size(); i++) {
      Program program = _programList.get(i);

      uri = program.rewrite(uri);
      
      String realPath = program.toRealPath(uri);

      if (realPath != null)
        return _appDir.lookup(realPath).getNativePath();
    }

    return pathMappingToRealPath(uri);
  }

  /**
   * Compatibility mapping to real path.
   */
  private String pathMappingToRealPath(String uri)
  {
    if (_pathMapping == null)
      return _appDir.lookup("./" + uri).getNativePath();

    ArrayList<String> regexpVars = new ArrayList<String>();
    
    String map = _pathMapping.map(uri, regexpVars);

    Path path;
    if (map == null)
      path = _appDir.lookup("./" + uri);
    else {
      try {
        path = PathBuilder.lookupPath(map, regexpVars);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
        
        path = _appDir.lookup(map);
      }
    
      String match = (String) regexpVars.get(0);
      String tail = uri.substring(match.length());

      // hacks to get the trailing '/' correct
      if (uri.endsWith("/") && ! tail.endsWith("/"))
        tail = tail + '/';

      if (tail.startsWith("/"))
        tail = '.' + tail;

      if (! tail.equals(""))
        path = path.lookup(tail);
    }

    String nativePath = path.getNativePath();

    // server/108j expects case insensitive
    /*
    if (CaseInsensitive.isCaseInsensitive())
      return nativePath.toLowerCase();
    else
      return nativePath;
    */
    
    return nativePath;
  }

  static class Program {
    public String rewrite(String uri)
    {
      return uri;
    }
    
    public String toRealPath(String uri)
    {
      return null;
    }
  }

  public static class Rewrite extends Program {
    private Pattern _regexp;
    private String _replacement;

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    /**
     * Sets the target.
     */
    public void setReplacement(String replacement)
    {
      _replacement = replacement;
    }

    /**
     * Init
     */
    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_regexp == null)
        throw new ConfigException(L.l("rewrite needs 'regexp' attribute."));
      if (_replacement == null)
        throw new ConfigException(L.l("rewrite needs 'replacement' attribute."));
    }
    
    public String rewrite(String uri)
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
        matcher.reset();
        return matcher.replaceAll(_replacement);
      }
      else
        return uri;
    }
  }

  public static class RealPath extends Program {
    private Pattern _regexp;
    private String _target;

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    /**
     * Sets the target.
     */
    public void setTarget(String target)
    {
      StringBuilder sb = new StringBuilder();

      int length = target.length();
      for (int i = 0; i < length; i++) {
        char ch = target.charAt(i);
        char ch1;

        if (ch == '\\' && i + 1 < length
            && ! ('0' <= (ch1 = target.charAt(i + 1)) && ch1 <= '9')) {
          sb.append("\\\\");
        }
        else
          sb.append(ch);
      }
      
      _target = sb.toString();
    }

    /**
     * Init
     */
    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_regexp == null)
        throw new ConfigException(L.l("real-path needs 'regexp' attribute."));
      if (_target == null)
        throw new ConfigException(L.l("real-path needs 'target' attribute."));
    }
    
    public String toRealPath(String uri)
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
        matcher.reset();

        return matcher.replaceAll(_target);
      }
      else
        return null;
    }
  }
}
