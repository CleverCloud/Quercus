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

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaCompiler;
import com.caucho.java.LineMap;
import com.caucho.jsp.cfg.JspConfig;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.jsp.cfg.ImplicitTld;
import com.caucho.jsp.java.JspTagSupport;
import com.caucho.jsp.java.TagTaglib;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.xml.Xml;
import com.caucho.jsf.cfg.JsfPropertyGroup;

import org.xml.sax.SAXException;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compilation interface for JSP pages.
 */
public class JspCompilerInstance {
  private static final L10N L = new L10N(JspCompilerInstance.class);
  
  private static final Logger log
    = Logger.getLogger(JspCompilerInstance.class.getName());

  // The underlying compiler
  private JspCompiler _jspCompiler;

  // The path to the JSP source
  private Path _jspPath;

  // The JSP uri (user-name)
  private String _uri;

  // The JSP class name
  private String _className;

  private JspPropertyGroup _jspPropertyGroup;

  private JsfPropertyGroup _jsfPropertyGroup;

  // The builder
  private JspBuilder _jspBuilder;

  // true for XML parsing
  private boolean _isXml;

  // true for prototype parsing.
  private boolean _isPrototype;

  // true for generated source (like XTP)
  private boolean _isGeneratedSource;

  // The parse state
  private ParseState _parseState;

  // The tag manager
  private ParseTagManager _tagManager;

  // The parser
  private JspParser _parser;

  // The compiled page
  private Page _page;

  // The generator
  private JspGenerator _generator;

  private ArrayList<String> _preludeList = new ArrayList<String>();
  private ArrayList<String> _codaList = new ArrayList<String>();

  private ArrayList<PersistentDependency> _dependList =
    new ArrayList<PersistentDependency>();

  /**
   * Creates a JSP compiler instance.
   */
  JspCompilerInstance(JspCompiler compiler)
  {
    _jspCompiler = compiler;

    _isXml = _jspCompiler.isXml();
  }

  /**
   * Sets the builder.
   */
  void setJspBuilder(JspBuilder builder)
  {
    _jspBuilder = builder;
  }

  /**
   * Sets the path.
   */
  void setJspPath(Path path)
  {
    _jspPath = path;
  }

  /**
   * Sets the uri
   */
  void setURI(String uri)
  {
    _uri = uri;
  }

  /**
   * Sets true for xml
   */
  void setXML(boolean isXml)
  {
    _isXml = isXml;
  }

  /*
   * Sets true for generated source
   */
  void setGeneratedSource(boolean isGeneratedSource)
  {
    _isGeneratedSource = isGeneratedSource;
  }

  /*
   * Sets true for generated source
   */
  public boolean isGeneratedSource()
  {
    return _isGeneratedSource;
  }

  /**
   * Sets the class name.
   */
  void setClassName(String className)
  {
    _className = className;
  }

  /**
   * Adds a dependency.
   */
  public void addDepend(PersistentDependency depend)
  {
    _dependList.add(depend);
  }

  /**
   * Adds a dependency.
   */
  public void addDependList(ArrayList<PersistentDependency> dependList)
  {
    if (dependList != null)
      _dependList.addAll(dependList);
  }

  /**
   * Returns the jsp configuration.
   */
  public JspPropertyGroup getJspPropertyGroup()
  {
    return _jspPropertyGroup;
  }

  /**
   * Returns true for prototype compilation.
   */
  public boolean isPrototype()
  {
    return _isPrototype;
  }

  /**
   * Set true for prototype compilation.
   */
  public void setPrototype(boolean prototype)
  {
    _isPrototype = prototype;
  }

  /**
   * Initialize the instance.
   */
  void init()
    throws Exception
  {
    _parseState = new ParseState();

    String uriPwd;
    if (_uri != null) {
      int p = _uri.lastIndexOf('/');
      uriPwd = p <= 0 ? "/" : _uri.substring(0, p + 1);
    }
    else {
      uriPwd = "/";
    }
    
    _parseState.setUriPwd(uriPwd);

    if (_className == null)
      _className = JavaCompiler.mangleName("jsp/" + _uri);

    // default to true if ends with x
    if (_uri.endsWith("x"))
      _parseState.setXml(true);

    WebApp app = _jspCompiler.getWebApp();
    Path appDir = _jspCompiler.getAppDir();
    if (appDir == null && app != null)
      appDir = app.getAppDir();

    if (app != null && app.hasPre23Config()
        && _parseState.getELIgnoredDefault() == null
        && ! _parseState.isXml()) { // jsp/100a
      _parseState.setELIgnoredDefault(true);
    }

    JspConfig jspConfig = null;

    if (jspConfig == null && app != null)
      jspConfig = (JspConfig) app.getExtension("jsp-config");

    ArrayList<JspPropertyGroup> jspList = new ArrayList<JspPropertyGroup>();
    _jspPropertyGroup = null;

    if (_jspPropertyGroup == null) {
      _jspPropertyGroup = _jspCompiler.getJspPropertyGroup();

      if (_jspPropertyGroup != null) {
        jspList.add(_jspPropertyGroup);
      }
    }

    if (_jspPropertyGroup == null && app != null) {
      _jspPropertyGroup = app.getJsp();
      
      if (_jspPropertyGroup != null)
        jspList.add(_jspPropertyGroup);
    }

    if (_jspPropertyGroup == null) {
      _jspPropertyGroup = _jspCompiler.getJspPropertyGroup();
      
      if (_jspPropertyGroup != null)
        jspList.add(_jspPropertyGroup);
    }

    if (jspConfig != null) {
      jspList.addAll(jspConfig.findJspPropertyGroupList(_uri));

      if (_parseState.getELIgnoredDefault() == null)
        _parseState.setELIgnoredDefault(false);
    }

    JspResourceManager resourceManager = _jspCompiler.getResourceManager();
    if (resourceManager != null) {
    }
    else if (app != null)
      resourceManager = new AppResourceManager(app);
    else {
      resourceManager = new AppDirResourceManager(appDir);
    }

    TagFileManager tagFileManager = _jspCompiler.getTagFileManager();

    TaglibManager taglibManager = _jspCompiler.getTaglibManager();

    JspPageConfig pageConfig = new JspPageConfig();

    for (JspPropertyGroup jspPropertyGroup : jspList) {
      ArrayList<String> preludeList = jspPropertyGroup.getIncludePreludeList();
      for (int i = 0; preludeList != null && i < preludeList.size(); i++) {
        String prelude = preludeList.get(i);
        _preludeList.add(prelude);
      }

      ArrayList<String> codaList = jspPropertyGroup.getIncludeCodaList();
      for (int i = 0; codaList != null && i < codaList.size(); i++) {
        String coda = codaList.get(i);
        _codaList.add(coda);
      }

      _parseState.setJspPropertyGroup(jspPropertyGroup);
      _parseState.setSession(jspPropertyGroup.isSession());
      _parseState.setScriptingInvalid(jspPropertyGroup.isScriptingInvalid());

      if (jspPropertyGroup.isELIgnored() != null) {
        _parseState.setELIgnored(Boolean.TRUE.equals(jspPropertyGroup.isELIgnored()));
      }
      
      _parseState.setVelocityEnabled(jspPropertyGroup.isVelocityEnabled());
      _parseState.setPageEncoding(jspPropertyGroup.getPageEncoding());
      
      if (Boolean.TRUE.equals(jspPropertyGroup.isXml()))
        _parseState.setXml(true);
      
      if (Boolean.FALSE.equals(jspPropertyGroup.isXml())) {
        _parseState.setXml(false);
        _parseState.setForbidXml(true);
      }

      if (jspPropertyGroup.getPageEncoding() != null) {
        try {
          _parseState.setPageEncoding(jspPropertyGroup.getPageEncoding());
        } catch (JspParseException e) {
        }
      }

      pageConfig.setStaticEncoding(jspPropertyGroup.isStaticEncoding());

      _parseState.setRecycleTags(jspPropertyGroup.isRecycleTags());
      
      _parseState.setTrimWhitespace(jspPropertyGroup.isTrimDirectiveWhitespaces());
      _parseState.setDeferredSyntaxAllowedAsLiteral(jspPropertyGroup.isDeferredSyntaxAllowedAsLiteral());

      if (jspPropertyGroup.getTldFileSet() != null)
        taglibManager.setTldFileSet(jspPropertyGroup.getTldFileSet());
    }

    if (_jsfPropertyGroup == null)
      _jsfPropertyGroup = _jspCompiler.getJsfPropertyGroup();

    if (_jsfPropertyGroup == null && app != null)
       _jsfPropertyGroup = app.getJsf();

    _parseState.setResourceManager(resourceManager);
    LineMap lineMap = null;

    _tagManager = new ParseTagManager(resourceManager,
                                      taglibManager,
                                      tagFileManager);

    _jspBuilder = new com.caucho.jsp.java.JavaJspBuilder();
    _jspBuilder.setParseState(_parseState);
    _jspBuilder.setJspCompiler(_jspCompiler);
    _jspBuilder.setJspPropertyGroup(_jspPropertyGroup);
    _jspBuilder.setJsfPropertyGroup(_jsfPropertyGroup);
    _jspBuilder.setTagManager(_tagManager);
    _jspBuilder.setPageConfig(pageConfig);
    _jspBuilder.setPrototype(_isPrototype);

    _parser = new JspParser();
    _parser.setJspBuilder(_jspBuilder);
    _parser.setParseState(_parseState);
    _parser.setTagManager(_tagManager);

    _jspBuilder.setJspParser(_parser);

    /*
    for (int i = 0; i < _preludeList.size(); i++)
      _parser.addPrelude(_preludeList.get(i));

    for (int i = 0; i < _codaList.size(); i++)
      _parser.addCoda(_codaList.get(i));
    */
  }

  public Page compile()
    throws Exception
  {
    LineMap lineMap = null;
    if (_page != null)
      throw new IllegalStateException("JspCompilerInstance cannot be reused");

    try {
      JspGenerator generator = generate();

      lineMap = generator.getLineMap();

      String encoding = _parseState.getCharEncoding();

      _jspCompiler.compilePending();

      boolean isAutoCompile = true;
      if (_jspPropertyGroup != null)
        isAutoCompile = _jspPropertyGroup.isAutoCompile();

      Page page;
      if (! generator.isStatic()) {
        compileJava(_jspPath, _className, lineMap, encoding);

        page = _jspCompiler.loadPage(_className, isAutoCompile);
      }
      else {
        page = _jspCompiler.loadStatic(_className,
                                       _parseState.isOptionalSession());
        page._caucho_addDepend(generator.getDependList());
        page._caucho_setContentType(_parseState.getContentType());
      }

      return page;
    } catch (JspParseException e) {
      e.setLineMap(lineMap);
      e.setErrorPage(_parseState.getErrorPage());
      throw e;
    } catch (SAXException e) {
      if (e.getCause() instanceof JspParseException) {
        JspParseException subE = (JspParseException) e.getCause();

        subE.setLineMap(lineMap);
        subE.setErrorPage(_parseState.getErrorPage());
        throw subE;
      }
      else {
        JspParseException exn = new JspParseException(e);
        exn.setErrorPage(_parseState.getErrorPage());
        exn.setLineMap(lineMap);

        throw exn;
      }
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setLineMap(lineMap);
      exn.setErrorPage(_parseState.getErrorPage());

      throw exn;
    } catch (Throwable e) {
      JspParseException exn = new JspParseException(e);
      exn.setLineMap(lineMap);
      exn.setErrorPage(_parseState.getErrorPage());

      throw exn;
    }
  }

  public JspGenerator generate()
    throws Exception
  {
    if (_page != null)
      throw new IllegalStateException("JspCompilerInstance cannot be reused");

    parse();

    try {
      JspGenerator generator = _jspBuilder.getGenerator();
      generator.setJspCompilerInstance(this);

      for (int i = 0; i < _dependList.size(); i++)
        generator.addDepend(_dependList.get(i));

      generator.validate();

      generator.generate(_jspPath, _className);

      return generator;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());

      throw exn;
    }
  }
  
  public void parse()
    throws Exception
  {
    boolean isXml = _parseState.isXml();
    boolean isForbidXml = _parseState.isForbidXml();

    ParseState parseState = _parser.getParseState();
    
    try {
      _parser.getParseState().setBuilder(_jspBuilder);

      for (String prelude : _preludeList) {
        parseState.setXml(false);
        parseState.setForbidXml(false);
        _parser.parse(parseState.getResourceManager().resolvePath(prelude),
                      prelude);
      }
      
      _parser.getParseState().setXml(isXml);
      _parser.getParseState().setForbidXml(isForbidXml);

      if (isXml) {
        if (_parseState.getELIgnoredDefault() == null)
          _parseState.setELIgnoredDefault(false);

        Xml xml = new Xml();
        _parseState.setXml(xml);
        xml.setContentHandler(new JspContentHandler(_jspBuilder));
        _jspPath.setUserPath(_uri);
        xml.setNamespaceAware(true);
        xml.setDtdValidating(true);
        xml.parse(_jspPath);
      }
      else {
        WebApp app = _jspCompiler.getWebApp();

        // jsp/0135
        if (app != null && app.hasPre23Config()) {
          if (_parseState.getELIgnoredDefault() == null)
            _parseState.setELIgnoredDefault(true);
        }

        _parser.parse(_jspPath, _uri);
      }

      for (String coda : _codaList) {
        parseState.setXml(false);
        parseState.setForbidXml(false);
        _parser.parse(parseState.getResourceManager().resolvePath(coda),
                      coda);
      }
    } catch (JspParseException e) {
      e.setErrorPage(_parseState.getErrorPage());
      throw e;
    } catch (SAXException e) {
      if (e.getCause() instanceof JspParseException) {
        JspParseException subE = (JspParseException) e.getCause();

        subE.setErrorPage(_parseState.getErrorPage());
        throw subE;
      }
      else {
        JspParseException exn = new JspParseException(e);
        exn.setErrorPage(_parseState.getErrorPage());

        throw exn;
      }
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());

      throw exn;
    }
  }

  public TagInfo compileTag(TagTaglib taglib)
    throws Exception
  {
    TagInfo preloadTag = preloadTag(taglib);

    if (preloadTag != null)
      return preloadTag;

    return generateTag(taglib);
  }

  private TagInfo preloadTag(TagLibraryInfo taglib)
  {
    try {
      JspTagSupport tag = (JspTagSupport) _jspCompiler.loadClass(_className, true);

      if (tag == null)
        return null;

      tag.init(_jspCompiler.getAppDir());

      if (tag._caucho_isModified())
        return null;

      return tag._caucho_getTagInfo(taglib);
    } catch (Throwable e) {
      return null;
    }
  }

  private TagInfo generateTag(TagTaglib taglib)
    throws Exception
  {
    LineMap lineMap = null;
    if (_page != null)
      throw new IllegalStateException("JspCompilerInstance cannot be reused");

    try {
      boolean isXml = _isXml;
      
      if (_jspPropertyGroup != null && ! isXml
          && _jspPropertyGroup.isXml() != null)
        isXml = Boolean.TRUE.equals(_jspPropertyGroup.isXml());

      if (_jspPropertyGroup != null
          && Boolean.FALSE.equals(_jspPropertyGroup.isXml()))
        _parseState.setForbidXml(true);

      _parseState.setXml(isXml);

      if (_jspCompiler.addTag(_className)) {
        _isPrototype = true;
        _jspBuilder.setPrototype(true);
      }

      _parseState.setTag(true);
      _isXml = isXml;

      Path implicitTld = _jspPath.lookup(_jspPath.getParent() + "/implicit.tld");

      // jsp/10h4
      taglib.setJspVersion("2.0");
      
      if (implicitTld.canRead()) {
        Config config = new Config();
        ImplicitTld tldTaglib = new ImplicitTld();

        config.configure(tldTaglib, implicitTld);

        if (tldTaglib.getJspVersion() != null
            && tldTaglib.getJspVersion().compareTo("2.0") < 0)
          throw new ConfigException(L.l("'{0}' must have a jsp-version 2.0 or greater",
                                        implicitTld));

        taglib.setJspVersion(tldTaglib.getJspVersion());
      }

      if (taglib.getRequiredVersion().compareTo("2.1") < 0) {
        _parseState.setJspVersion("2.0");
        _parseState.setDeferredSyntaxAllowedAsLiteral(true);
      }
      
      if (isXml) {
        _parseState.setELIgnoredDefault(false);

        Xml xml = new Xml();
        _parseState.setXml(xml);
        xml.setContentHandler(new JspContentHandler(_jspBuilder));
        _jspPath.setUserPath(_uri);
        xml.setNamespaceAware(true);
        xml.setDtdValidating(true);
        xml.parse(_jspPath);
      }
      else
        _parser.parseTag(_jspPath, _uri);

      _generator = _jspBuilder.getGenerator();

      if (_isPrototype) {
        return _generator.generateTagInfo(_className, taglib);
      }

      _generator.validate();
      
      _generator.generate(_jspPath, _className);

      if (_jspCompiler.hasRecursiveCompile()) {
        _jspCompiler.addPending(this);

        return _generator.generateTagInfo(_className, taglib);
      }

      return completeTag(taglib);
    } catch (JspParseException e) {
      e.setLineMap(lineMap);
      e.setErrorPage(_parseState.getErrorPage());
      throw e;
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());
      exn.setLineMap(lineMap);
      throw exn;
    }
  }

  TagInfo completeTag()
    throws Exception
  {
    return completeTag(new TagTaglib("x", "uri"));
  }

  TagInfo completeTag(TagLibraryInfo taglib)
    throws Exception
  {
    LineMap lineMap = null;

    try {
      lineMap = _generator.getLineMap();

      String encoding = _parseState.getCharEncoding();

      compileJava(_jspPath, _className, lineMap, encoding);

      JspTagSupport tag = (JspTagSupport) _jspCompiler.loadClass(_className, true);

      tag.init(_jspCompiler.getAppDir());

      return tag._caucho_getTagInfo(taglib);
      // Page page = _jspCompiler.loadClass(_className);
    } catch (JspParseException e) {
      e.setLineMap(lineMap);
      e.setErrorPage(_parseState.getErrorPage());
      throw e;
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());
      exn.setLineMap(lineMap);
      throw exn;
    } catch (Throwable e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());
      exn.setLineMap(lineMap);
      throw exn;
    }
  }

  private void compileJava(Path path, String className,
                           LineMap lineMap, String charEncoding)
    throws Exception
  {
    JavaCompiler compiler = JavaCompiler.create(null);
    compiler.setClassDir(_jspCompiler.getClassDir());
    // compiler.setEncoding(charEncoding);
    String fileName = className.replace('.', '/') + ".java";

    compiler.compile(fileName, lineMap);

    /*
    boolean remove = true;
    try {
      compiler.compile(fileName, lineMap);
      remove = false;
    } finally {
      if (remove)
        Vfs.lookup(fileName).remove();
    }
    */

    Path classDir = _jspCompiler.getClassDir();
    Path classPath = classDir.lookup(className.replace('.', '/') + ".class");
    Path smapPath =  classDir.lookup(fileName + ".smap");

    // jsp/18p1
    // compiler.mergeSmap(classPath, smapPath);
  }

  private void readSmap(ClassLoader loader, String className)
  {
    if (loader == null)
      return;

    String smapName = className.replace('.', '/') + ".java.smap";

    InputStream is = null;
    try {
      is = loader.getResourceAsStream(smapName);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
