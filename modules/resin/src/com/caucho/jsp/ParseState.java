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

package com.caucho.jsp;

import com.caucho.java.LineMap;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.CharScanner;
import com.caucho.util.L10N;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.TempCharBuffer;
import com.caucho.vfs.Path;
import com.caucho.xml.QName;
import com.caucho.xml.Xml;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents the current state of the parser.
 */
public class ParseState {
  private static final L10N L = new L10N(ParseState.class);
  private static final Logger log
    = Logger.getLogger(ParseState.class.getName());

  private WebApp _application;

  private JspPropertyGroup _jspPropertyGroup = new JspPropertyGroup();

  private Boolean _isELIgnored;
  private Boolean _isELIgnoredDefault;
  private boolean _isELIgnoredPageSpecified = false;
  
  private boolean _isScriptingInvalid = false;
  private boolean _isLocalScriptingInvalid = false;
  
  private boolean _isVelocityEnabled;

  private boolean _isSession = true;
  private boolean _isOptionalSession = false;
  private boolean _isSessionSet = false;
  
  private boolean _isErrorPage = false;
  private boolean _isErrorPageSet = false;
  
  private boolean _isAutoFlush = true;
  private boolean _isAutoFlushSet = false;

  private boolean _isThreadSafe = true;
  private boolean _isThreadSafeSet = false;

  private boolean _isTag = false;
  private boolean _isXml = false;
  private boolean _isForbidXml = false;

  private int _buffer = TempCharBuffer.SIZE;
  private boolean _isBufferSet = false;

  private String _info;
  private String _errorPage;
  private String _contentType;
  private String _charEncoding;

  private int _bom = -1;

  private String _pageEncoding;
  private String _xmlPageEncoding;
  private Class _extends;

  private boolean _recycleTags = true;
  private boolean _isTrimWhitespace;
  private boolean _isDeferredSyntaxAllowedAsLiteral;

  private JspResourceManager _resourceManager;

  private JspBuilder _jspBuilder;

  private ArrayList<String> _importList = new ArrayList<String>();

  private String _uriPwd;
  
  private ArrayList<PersistentDependency> _depends
    = new ArrayList<PersistentDependency>();
  private LineMap _lineMap;
  
  private boolean _isPrototype;
  
  private Xml _xml;
  private Namespace _namespaces;

  private String _jspVersion = "2.1";

  /**
   * Create a new parse state instance.
   */
  public ParseState()
  {
  }

  /**
   * Sets the JSP property group.
   */
  public void setJspPropertyGroup(JspPropertyGroup group)
  {
    _jspPropertyGroup = group;
  }

  /**
   * Gets the JSP property group.
   */
  public JspPropertyGroup getJspPropertyGroup()
  {
    return _jspPropertyGroup;
  }

  /**
   * Returns true if JSP EL is ignored.
   */
  public boolean isELIgnored()
  {
    if (_isELIgnored != null)
      return _isELIgnored;
    else if (_isELIgnoredDefault != null)
      return _isELIgnoredDefault;
    else
      return false;
  }

  /**
   * Set if JSP EL is ignored.
   */
  public void setELIgnored(boolean isELIgnored)
  {
    _isELIgnored = isELIgnored;
  }

  /**
   * Set if JSP EL is ignored.
   */
  public void setELIgnoredDefault(boolean isELIgnored)
  {
    _isELIgnoredDefault = isELIgnored;
  }

  /**
   * Set if JSP EL is ignored.
   */
  public Boolean getELIgnoredDefault()
  {
    return _isELIgnoredDefault;
  }

  public boolean isELIgnoredPageSpecified()
  {
    return _isELIgnoredPageSpecified;
  }

  public void setELIgnoredPageSpecified(boolean ELIgnoredPageSpecified)
  {
    _isELIgnoredPageSpecified = ELIgnoredPageSpecified;
  }

  /**
   * Returns true if JSP scripting is invalid.
   */
  public boolean isScriptingInvalid()
  {
    return _isScriptingInvalid;
  }

  /**
   * Set if JSP scripting is ignored.
   */
  public void setScriptingInvalid(boolean isScriptingInvalid)
  {
    _isScriptingInvalid = isScriptingInvalid;
  }

  public boolean isLocalScriptingInvalid()
  {
    return _isLocalScriptingInvalid;
  }

  public void setLocalScriptingInvalid(boolean isLocalScriptingInvalid)
  {
    _isLocalScriptingInvalid = isLocalScriptingInvalid;
  }

  /**
   * Set if velocity statements are enabled.
   */
  public void setVelocityEnabled(boolean isVelocity)
  {
    _isVelocityEnabled = isVelocity;
  }

  /**
   * Returns true if Velocity statements are enabled.
   */
  public boolean isVelocityEnabled()
  {
    return _isVelocityEnabled;
  }

  /**
   * Returns true if the session is enabled.
   */
  public boolean isSession()
  {
    return _isSession;
  }

  /**
   * Returns true if the optional session is enabled.
   */
  public boolean isOptionalSession()
  {
    return _isOptionalSession;
  }
  
  /**
   * Set if the session is enabled.
   */
  public boolean setSession(boolean session)
  {
    boolean isSession = _isSession;
    
    _isSession = session;
    _isOptionalSession = session;

    return (session == isSession || ! _isSessionSet);
  }

  /**
   * Mark the thread safe attribute as set.
   */
  public void markSessionSet()
  {
    _isSessionSet = true;
  }

  /**
   * Returns true if the autoFlush is enabled.
   */
  public boolean isAutoFlush()
  {
    return _isAutoFlush;
  }
  
  /**
   * Set if the autoFlush is enabled.
   */
  public boolean setAutoFlush(boolean autoFlush)
  {
    boolean isAutoFlush = _isAutoFlush;
    
    _isAutoFlush = autoFlush;

    return (autoFlush == isAutoFlush || ! _isAutoFlushSet);
  }

  /**
   * Mark the thread safe attribute as set.
   */
  public void markAutoFlushSet()
  {
    _isAutoFlushSet = true;
  }

  /**
   * Returns true if the threadSafe is enabled.
   */
  public boolean isThreadSafe()
  {
    return _isThreadSafe;
  }

  /**
   * Set if the threadSafe is enabled.
   */
  public boolean setThreadSafe(boolean threadSafe)
  {
    boolean isThreadSafe = _isThreadSafe;
    
    _isThreadSafe = threadSafe;

    return (threadSafe == isThreadSafe || ! _isThreadSafeSet);
  }

  /**
   * Mark the thread safe attribute as set.
   */
  public void markThreadSafeSet()
  {
    _isThreadSafeSet = true;
  }

  /**
   * Set if the errorPage is enabled.
   */
  public boolean setErrorPage(boolean errorPage)
  {
    boolean isErrorPage = _isErrorPage;
    
    _isErrorPage = errorPage;

    return (errorPage == isErrorPage || ! _isErrorPageSet);
  }

  /**
   * Returns true if the errorPage is enabled.
   */
  public boolean isErrorPage()
  {
    return _isErrorPage;
  }

  /**
   * Mark the error page attribute as set.
   */
  public void markErrorPage()
  {
    _isErrorPageSet = true;
  }

  /**
   * Returns the buffer size in bytes.
   */
  public int getBuffer()
  {
    return _buffer;
  }
  
  /**
   * Set the buffer size.
   */
  public boolean setBuffer(int buffer)
  {
    int oldBuffer = _buffer;
    
    _buffer = buffer;

    return (buffer == oldBuffer || ! _isBufferSet);
  }

  /**
   * Mark the buffer attribute as set.
   */
  public void markBufferSet()
  {
    _isBufferSet = true;
  }

  /**
   * Sets the JSP's error page
   */
  public void setErrorPage(String errorPage)
  {
    _errorPage = errorPage;
  }

  /**
   * Gets the JSP's error page
   */
  public String getErrorPage()
  {
    return _errorPage;
  }

  /**
   * Sets the JSP's content type
   */
  public void setContentType(String contentType)
  {
    _contentType = contentType;
  }

  /**
   * Gets the JSP's content type
   */
  public String getContentType()
  {
    return _contentType;
  }

  /**
   * Sets the XML parser
   */
  public void setXml(Xml xml)
  {
    _xml = xml;
  }

  public Xml getXml()
  {
    return _xml;
  }

  /**
   * Sets the JSP's character encoding
   */
  public void setCharEncoding(String charEncoding)
    throws JspParseException
  {
    if ("UTF-16".equalsIgnoreCase(charEncoding))
      charEncoding = "UTF-16LE";
    
    /*
    if (_charEncoding != null &&
        ! _charEncoding.equalsIgnoreCase(charEncoding))
      throw new JspParseException(L.l("Cannot change character encoding to '{0}' (old value '{1}').  The character encoding may only be set once.",
                                      charEncoding, _charEncoding));
    */
    
    _charEncoding = charEncoding;
  }

  /**
   * Gets the JSP's character encoding
   */
  public String getCharEncoding()
  {
    return _charEncoding;
  }

  public void setXmlPageEncoding(String pageEncoding)
    throws JspParseException
  {
    _xmlPageEncoding = pageEncoding;
    setPageEncoding(pageEncoding);
  }

  /**
   * Sets the JSP's page encoding
   */
  public void setPageEncoding(String pageEncoding)
    throws JspParseException
  {
    if (pageEncoding == null)
      return;

    if (_xml != null && _pageEncoding == null)
      _pageEncoding = _xml.getEncoding();

    if ("UTF-16".equalsIgnoreCase(pageEncoding))
      pageEncoding = "UTF-16LE";

    if (_pageEncoding == null
        || _pageEncoding.equalsIgnoreCase(pageEncoding)) {
      _pageEncoding = pageEncoding;
    }
    else if (("UTF-16".equalsIgnoreCase(_pageEncoding)
              || "UTF-16LE".equalsIgnoreCase(_pageEncoding))
             && ("UTF-16LE".equalsIgnoreCase(pageEncoding)
                 || "UTF-16BE".equalsIgnoreCase(pageEncoding))) {
      _pageEncoding = pageEncoding;
    }
    else if ("UTF-16LE".equalsIgnoreCase(pageEncoding)
             && ("UTF-16LE".equalsIgnoreCase(_pageEncoding)
                 || "UTF-16BE".equalsIgnoreCase(_pageEncoding))) {
    }
    else {
      String oldPageEncoding = _pageEncoding;
      
      //_pageEncoding = pageEncoding;

      if (_bom == -1)
        throw new JspParseException(L.l(
          "Cannot change page encoding to '{0}' (old value '{1}').  The page encoding may only be set once.",
          pageEncoding,
          oldPageEncoding));
      else
        throw new JspParseException(L.l(
          "Cannot change page encoding to '{0}' (old value is specified by BOM '{1}' -> '{2}').  The page encoding may only be set once.",
          pageEncoding,
          Integer.toHexString(_bom),
          oldPageEncoding));
    }
  }

  public void setBom(int bom) {
    _bom = bom;
  }

  /**
   * Gets the JSP's character encoding
   */
  public String getPageEncoding()
  {
    if (_pageEncoding != null)
      return _pageEncoding;
    else if (_xmlPageEncoding != null)
      return "utf-8";
    else
      return null;
  }

  /**
   * Gets the JSP's character encoding
   */
  public String getXmlPageEncoding()
  {
    return _xmlPageEncoding;
  }

  /**
   * Returns the JSP's info string.
   */
  public String getInfo()
  {
    return _info;
  }

  /**
   * Sets the JSP's info string
   */
  public void setInfo(String info)
  {
    _info = info;
  }

  /**
   * Returns the JSP's extends
   */
  public Class getExtends()
  {
    return _extends;
  }

  /**
   * Sets the JSP's extends
   */
  public void setExtends(Class extendsValue)
  {
    _extends = extendsValue;
  }

  /**
   * Returns true if parsing is a tag
   */
  public boolean isTag()
  {
    return _isTag;
  }

  /**
   * Set if parsing a tag
   */
  public void setTag(boolean isTag)
  {
    _isTag = isTag;
  }

  /**
   * Returns true if parsing is XML
   */
  public boolean isXml()
  {
    return _isXml;
  }

  /**
   * Set if parsing is xml
   */
  public void setXml(boolean isXml)
  {
    _isXml = isXml;
  }

  /**
   * Returns true if parsing forbids XML
   */
  public boolean isForbidXml()
  {
    return _isForbidXml;
  }

  /**
   * Set if parsing forbids xml
   */
  public void setForbidXml(boolean isForbidXml)
  {
    _isForbidXml = isForbidXml;
  }

  /**
   * Returns true if the print-null-as-blank is enabled.
   */
  public boolean isPrintNullAsBlank()
  {
    return _jspPropertyGroup.isPrintNullAsBlank();
  }

  /**
   * Returns true if JSP whitespace is trimmed.
   */
  public boolean isTrimWhitespace()
  {
    return _isTrimWhitespace;
  }

  /**
   * Set true if JSP whitespace is trimmed.
   */
  public void setTrimWhitespace(boolean trim)
  {
    _isTrimWhitespace = trim;
  }

  /**
   * Returns true if JSP whitespace is trimmed.
   */
  public boolean isDeferredSyntaxAllowedAsLiteral()
  {
    return _isDeferredSyntaxAllowedAsLiteral;
  }

  /**
   * Set true if JSP whitespace is trimmed.
   */
  public void setDeferredSyntaxAllowedAsLiteral(boolean trim)
  {
    _isDeferredSyntaxAllowedAsLiteral = trim;
  }

  /**
   * Set the version
   */
  public void setJspVersion(String version)
  {
    _jspVersion = version;
  }

  /**
   * Set the version
   */
  public String getJspVersion()
  {
    return _jspVersion;
  }
  
  /**
   * Gets the resource manager.
   */
  public JspResourceManager getResourceManager()
  {
    return _resourceManager;
  }
  
  /**
   * Sets the resource manager.
   */
  public void setResourceManager(JspResourceManager resourceManager)
  {
    _resourceManager = resourceManager;
  }
  
  /**
   * Gets the builder
   */
  public JspBuilder getBuilder()
  {
    return _jspBuilder;
  }
  
  /**
   * Sets the builder
   */
  public void setBuilder(JspBuilder jspBuilder)
  {
    _jspBuilder = jspBuilder;
  }

  private static CharScanner COMMA_DELIM_SCANNER = new CharScanner(" \t\n\r,");
  
  /**
   * Adds an import string.
   */
  public void addImport(String importString)
    throws JspParseException
  {
    String []imports = importString.split("[ \t\n\r,]+");

    for (int i = 0; i < imports.length; i++) {
      String value = imports[i];

      if (value.equals(""))
        continue;
      
      if (value.equals("static") && i + 1 < imports.length) {
        value = "static " + imports[i + 1];
        i++;
      }

      if (! _importList.contains(value))
        _importList.add(value);
    }
  }

  /**
   * Returns the import list.
   */
  public ArrayList<String> getImportList()
  {
    return _importList;
  }

  /**
   * Sets the URI pwd
   */
  public void setUriPwd(String uriPwd)
  {
    _uriPwd = uriPwd;
  }

  /**
   * Gets the URI pwd
   */
  public String getUriPwd()
  {
    return _uriPwd;
  }

  /**
   * Returns the line map.
   */
  public LineMap getLineMap()
  {
    return _lineMap;
  }

  /**
   * Add a dependency.
   */
  public void addDepend(Path path)
  {
    PersistentDependency depend = path.createDepend();
    if (! _depends.contains(depend))
      _depends.add(depend);
  }

  /**
   * Returns the dependencies
   */
  public ArrayList<PersistentDependency> getDependList()
  {
    return _depends;
  }

  /**
   * Resolves a path.
   *
   * @param uri the uri for the path
   *
   * @return the Path
   */
  public Path resolvePath(String uri)
  {
    return getResourceManager().resolvePath(uri);
  }

  /**
   * Set if recycle-tags is enabled.
   */
  public void setRecycleTags(boolean recycleTags)
  {
    _recycleTags = recycleTags;
  }

  /**
   * Returns true if recycle-tags is enabled.
   */
  public boolean isRecycleTags()
  {
    return _recycleTags;
  }

  /**
   * Returns the QName for the given name.
   */
  public QName getQName(String name)
  {
    int p = name.indexOf(':');

    if (p < 0)
      return new QName(name);
    else {
      String prefix = name.substring(0, p);
      String uri = Namespace.find(_namespaces, prefix);

      if (uri != null)
        return new QName(name, uri);
      else
        return new QName(name);
    }
  }

  public Namespace getNamespaces()
  {
    return _namespaces;
  }

  /**
   * Pushes a namespace.
   */
  public void pushNamespace(String prefix, String uri)
  {
    _namespaces = new Namespace(_namespaces, prefix, uri);
  }

  /**
   * Pops a namespace.
   */
  public void popNamespace(String prefix)
  {
    if (_namespaces._prefix.equals(prefix))
      _namespaces = _namespaces.getNext();
    else
      throw new IllegalStateException();
  }

  public String findPrefix(String uri)
  {
    return Namespace.findPrefix(_namespaces, uri);
  }

  public boolean isPrototype()
  {
    return _isPrototype;
  }

  public void setPrototype(boolean isPrototype)
  {
    _isPrototype = isPrototype;
  }
}

