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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.cfg;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.Period;
import com.caucho.jsp.JspServlet;
import com.caucho.jsp.JspXmlServlet;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.dispatch.UrlMap;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Configuration for the jsp-property-group.
 */
public class JspPropertyGroup implements JspPropertyGroupDescriptor {
  private static final L10N L = new L10N(JspPropertyGroup.class);

  private static int _gId;

  private WebApp _webApp;
  
  private String _id;
  private ArrayList<String> _urlPatterns = new ArrayList<String>();
  private String _pageEncoding;
  private Boolean _isELIgnored;
  private Boolean _isScriptingInvalid = null;
  private Boolean _isXml = null;
  private ArrayList<String> _includePrelude = new ArrayList<String>();
  private ArrayList<String> _includeCoda = new ArrayList<String>();
  private String _characterEncoding;

  // Resin config
  private boolean _precompile = true;
  private boolean _fastJstl = true;
  private boolean _fastJsf = false;
  private boolean _ideHack = false;
  private boolean _velocity = false;
  private boolean _session = true;
  private boolean _staticEncoding = true;
  private boolean _recycleTags = true;
  private boolean _autoCompile = true;
  private boolean _requireSource = false;
  private boolean _ignoreELException = true;
  private boolean _isValidateTaglibSchema = true;
  private boolean _isPrintNullAsBlank = false;

  private int _jspMax = 0;
  private boolean _disableLog = true;
  private long _dependencyCheckInterval = Long.MIN_VALUE;
  private boolean _staticPageGeneratesClass = true;
  private boolean _loadTldOnInit = false;

  private boolean _recompileOnError = false;
  private FileSetType _tldFileSet;

  private Boolean _isTrimWhitespace = null;
  private Boolean _isDeferredSyntaxAllowedAsLiteral = null;
  private boolean _isELIgnoredForOldWebApp = false;

  // servlet 3.0
  private String _defaultContentType;
  private String _buffer;
  private Boolean _isErrorOnUndeclaredNamespace;

  public JspPropertyGroup()
  {
  }

  public JspPropertyGroup(WebApp webApp)
  {
    _webApp = webApp;
  }
  
  /**
   * Returns the group's identifier.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Sets the group's identifier.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Sets the group's description
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the group's display name
   */
  public void setDisplayName(String displayName)
  {
  }

  /**
   * Adds a URL pattern.
   */
  public void addURLPattern(String urlPattern)
  {
    _urlPatterns.add(urlPattern);
  }

  @Override
  public Collection<String> getUrlPatterns()
  {
    return _urlPatterns;
  }

  /**
   * Sets the default page encoding.
   */
  public void setPageEncoding(String pageEncoding)
  {
    _pageEncoding = pageEncoding;
  }

  /**
   * Returns the default page encoding.
   */
  public String getPageEncoding()
  {
    return _pageEncoding;
  }

  /**
   * Set true if EL expressions are ignored for the JSP page.
   */
  public void setELIgnored(boolean isELIgnored)
  {
    _isELIgnored = isELIgnored ? Boolean.TRUE : Boolean.FALSE;;
  }

  /**
   * Return true if EL expressions are ignored for the JSP page.
   */
  public Boolean isELIgnored()
  {
    return _isELIgnored;
  }

  @Override
  public String getElIgnored()
  {
    return _isELIgnored == null ? null : _isELIgnored.toString();
  }

  /**
   * Set true when JSP pages should load tld files.
   */
  public void setLoadTldOnInit(boolean isPreload)
  {
    _loadTldOnInit = isPreload;
  }

  /**
   * Set true when JSP pages should load tld files.
   */
  public boolean isLoadTldOnInit()
  {
    return _loadTldOnInit;
  }

  /**
   * Sets the dependency check interval.
   */
  public void setDependencyCheckInterval(Period period)
  {
    _dependencyCheckInterval = period.getPeriod();
  }

  /**
   * Gets the dependency check interval.
   */
  public long getDependencyCheckInterval()
  {
    return _dependencyCheckInterval;
  }

  /**
   * Set true if scripting is invalid for the JSP page.
   */
  public void setScriptingInvalid(boolean isScriptingInvalid)
  {
    _isScriptingInvalid = isScriptingInvalid;
  }

  /**
   * Return true if scripting expressions are invalid for the JSP page.
   */
  public boolean isScriptingInvalid()
  {
    if (_isScriptingInvalid == null)
      return false;

    return _isScriptingInvalid;
  }

  @Override
  public String getScriptingInvalid()
  {
    return _isScriptingInvalid == null ? null : _isScriptingInvalid.toString();
  }

  /**
   * Return true if the JSP uses XML format.
   */
  public void setIsXml(boolean isXml)
  {
    _isXml = isXml ? Boolean.TRUE : Boolean.FALSE;
  }

  /**
   * Return true if the JSP uses XML format.
   */
  public Boolean isXml()
  {
    return _isXml;
  }

  @Override
  public String getIsXml()
  {
    return _isXml == null ? null : _isXml.toString(); 
  }

  /**
   * Adds a new prelude inclusion.
   */
  public void addIncludePrelude(String url)
  {
    _includePrelude.add(url);
  }

  /**
   * Returns the prelude inclusion.
   */
  public ArrayList<String> getIncludePreludeList()
  {
    return _includePrelude;
  }

  @Override
  public Collection<String> getIncludePreludes()
  {
    return _includePrelude;
  }

  /**
   * Adds a new coda inclusion.
   */
  public void addIncludeCoda(String url)
  {
    _includeCoda.add(url);
  }

  /**
   * Returns the coda inclusion.
   */
  public ArrayList<String> getIncludeCodaList()
  {
    return _includeCoda;
  }

  @Override
  public Collection<String> getIncludeCodas()
  {
    return _includeCoda;
  }

  public String getCharacterEncoding()
  {
    return _characterEncoding;
  }

  public void setCharacterEncoding(String characterEncoding)
  {
    _characterEncoding = characterEncoding;
  }

  // Resin config

  /**
   * Set if precompilation is allowed.
   */
  public void setPrecompile(boolean precompile)
  {
    _precompile = precompile;
  }

  /**
   * Return true if precompilation is allowed.
   */
  public boolean getPrecompile()
  {
    return _precompile;
  }

  /**
   * Return true if auto-compilation is allowed.
   */
  public boolean isAutoCompile()
  {
    return _autoCompile;
  }

  /**
   * Set if auto-compile is allowed.
   */
  public void setAutoCompile(boolean compile)
  {
    _autoCompile = compile;
  }

  /**
   * Set if the *.jsp source is required.
   */
  public void setRequireSource(boolean requireSource)
  {
    _requireSource = requireSource;
  }

  /**
   * Set if the *.jsp source is required.
   */
  public boolean getRequireSource()
  {
    return _requireSource;
  }

  /**
   * Set if nulls are printed as space
   */
  public void setPrintNullAsBlank(boolean enable)
  {
    _isPrintNullAsBlank = enable;
  }

  /**
   * Set if nulls are printed as space
   */
  public boolean isPrintNullAsBlank()
  {
    return _isPrintNullAsBlank;
  }

  /**
   * Set true if EL exceptions should be ignored.
   */
  public boolean isIgnoreELException()
  {
    return _ignoreELException;
  }

  /**
   * Set true if EL exceptions should be ignored.
   */
  public void setIgnoreELException(boolean ignore)
  {
    _ignoreELException = ignore;
  }

  /**
   * Set if fast jstl is allowed.
   */
  public void setFastJstl(boolean fastJstl)
  {
    _fastJstl = fastJstl;
  }

  /**
   * Return true if fast jstl is allowed.
   */
  public boolean isFastJstl()
  {
    return _fastJstl;
  }

  /**
   * Set if fast jsf is allowed.
   */
  public void setFastJsf(boolean fastJsf)
  {
    _fastJsf = fastJsf;
  }

  /**
   * Return true if fast jsf is allowed.
   */
  public boolean isFastJsf()
  {
    return _fastJsf;
  }

  /**
   * Set if velocity-style syntax is allowed.
   */
  public void setVelocityEnabled(boolean velocity)
  {
    _velocity = velocity;
  }

  /**
   * Return true if velocity is allowed.
   */
  public boolean isVelocityEnabled()
  {
    return _velocity;
  }

  /**
   * Set if sessions are enabled by default
   */
  public void setSession(boolean session)
  {
    _session = session;
  }

  /**
   * Return true if sessions are enabled by default.
   */
  public boolean isSession()
  {
    return _session;
  }

  /**
   * Set if static encoding is enabled.
   */
  public void setStaticEncoding(boolean staticEncoding)
  {
    _staticEncoding = staticEncoding;
  }

  /**
   * Return true if static encoding is enabled
   */
  public boolean isStaticEncoding()
  {
    return _staticEncoding;
  }

  /**
   * Set if recycle tags is enabled.
   */
  public void setRecycleTags(boolean recycleTags)
  {
    _recycleTags = recycleTags;
  }

  /**
   * Return true if recycle tags is enabled
   */
  public boolean isRecycleTags()
  {
    return _recycleTags;
  }

  /**
   * Sets true for the ide-hack
   */
  public void setIdeHack(boolean ideHack)
  {
    _ideHack = ideHack;
  }

  /**
   * Gets the value of the ide-hack
   */
  public boolean getIdeHack()
  {
    return _ideHack;
  }

  /**
   * Sets the jsp-max
   */
  public void setJspMax(int max)
    throws ConfigException
  {
    if (max <= 0)
      throw new ConfigException(L.l("`{0}' is too small a value for jsp-max",
                                    max));

    _jspMax = max;
  }

  /**
   * Gets the value of the jsp-max
   */
  public int getJspMax()
  {
    return _jspMax;
  }

  /**
   * Returns true if JSP logging should be disabled.
   */
  public boolean isDisableLog()
  {
    return _disableLog;
  }

  /**
   * Sets the deferred-syntax.
   */
  public void setDeferredSyntaxAllowedAsLiteral(boolean isAllowed)
  {
    _isDeferredSyntaxAllowedAsLiteral = isAllowed;
  }

  /**
   * Sets the deferred-syntax.
   */
  public boolean isDeferredSyntaxAllowedAsLiteral()
  {
    return _isDeferredSyntaxAllowedAsLiteral == null ?
      false :
      _isDeferredSyntaxAllowedAsLiteral;
  }

  @Override
  public String getDeferredSyntaxAllowedAsLiteral()
  {
    return _isDeferredSyntaxAllowedAsLiteral == null ?
      null :
      _isDeferredSyntaxAllowedAsLiteral.toString();
  }

  public String getDefaultContentType()
  {
    return _defaultContentType;
  }

  public void setDefaultContentType(String defaultContentType)
  {
    _defaultContentType = defaultContentType;
  }

  public String getBuffer()
  {
    return _buffer;
  }

  public void setBuffer(String buffer)
  {
    _buffer = buffer;
  }

  public boolean isErrorOnUndeclaredNamespace()
  {
    return _isErrorOnUndeclaredNamespace == null ?
      true :
      _isErrorOnUndeclaredNamespace.booleanValue();
  }

  @Override
  public String getErrorOnUndeclaredNamespace()
  {
    return _isErrorOnUndeclaredNamespace == null ?
      null :
      _isErrorOnUndeclaredNamespace.toString();
  }

  public void setErrorOnUndeclaredNamespace(Boolean errorOnUndeclaredNamespace)
  {
    _isErrorOnUndeclaredNamespace = errorOnUndeclaredNamespace;
  }

  /**
   * Sets the recompile-on-errrkReturns true if JSP logging should be disabled.
   */
  public void setRecompileOnError(boolean recompile)
  {
    _recompileOnError = recompile;
  }

  /**
   * Sets the recompile-on-errrkReturns true if JSP logging should be disabled.
   */
  public boolean isRecompileOnError()
  {
    return _recompileOnError;
  }

  /**
   * Set true if the taglibs should have the schema validated
   */
  public void setValidateTaglibSchema(boolean isValidate)
  {
    _isValidateTaglibSchema = isValidate;
  }

  /**
   * Set true if the taglibs should have the schema validated
   */
  public boolean isValidateTaglibSchema()
  {
    return _isValidateTaglibSchema;
  }

  /**
   * Sets the tld fileset.
   */
  public void setTldFileSet(FileSetType fileSet)
  {
    _tldFileSet = fileSet;
  }

  /**
   * Gets the tld fileset.
   */
  public FileSetType getTldFileSet()
  {
    return _tldFileSet;
  }

  /**
   * Sets a restriction of the tld dir.
   */
  public void setTldDir(Path tldDir)
  {
    _tldFileSet = new FileSetType();
    _tldFileSet.setDir(tldDir);
  }

  /**
   * Set true if static pages should generate a class
   */
  public void setStaticPageGeneratesClass(boolean generate)
  {
    _staticPageGeneratesClass = generate;
  }

  /**
   * Set true if static pages should generate a class
   */
  public boolean getStaticPageGeneratesClass()
  {
    return _staticPageGeneratesClass;
  }

  /**
   * True if whitespace is trimmed.
   */
  public boolean isTrimDirectiveWhitespaces()
  {
    return _isTrimWhitespace == null ? false : _isTrimWhitespace.booleanValue();
  }

  /**
   * Set if whitespace is trimmed.
   */
  public void setTrimDirectiveWhitespaces(boolean isTrim)
  {
    _isTrimWhitespace = isTrim;
  }

  @Override
  public String getTrimDirectiveWhitespaces()
  {
    return _isTrimWhitespace == null ? null : _isTrimWhitespace.toString();
  }

  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_webApp != null) {
      ServletMapping mapping = _webApp.createServletMapping();
      mapping.setServletName("jsp-property-group-" + _gId++);
      
      if (Boolean.TRUE.equals(_isXml))
        mapping.setServletClass(JspXmlServlet.class.getName());
      else
        mapping.setServletClass(JspServlet.class.getName());

      for (int i = 0; i < _urlPatterns.size(); i++) {
        mapping.addURLPattern(_urlPatterns.get(i));
      }

      _webApp.addServletMapping(mapping);
    }
  }

  /**
   * Returns true if the property group matches the URL.
   */
  public boolean match(String url)
  {
    if (_urlPatterns.size() == 0)
      return true;

    for (int i = 0; i < _urlPatterns.size(); i++) {
      String urlPattern = _urlPatterns.get(i);
      String regexpPattern = UrlMap.urlPatternToRegexpPattern(urlPattern);

      if (Pattern.compile(regexpPattern).matcher(url).find()) {
        return true;
      }
    }

    return false;
  }
}
