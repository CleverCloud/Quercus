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

import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.jsp.java.JspNode;
import com.caucho.vfs.Path;
import com.caucho.xml.QName;
import com.caucho.jsf.cfg.JsfPropertyGroup;

/**
 * Generates the nodes for JSP code.
 */
abstract public class JspBuilder {
  // The current source
  protected Path _sourcePath;
  
  // The current filename
  protected String _filename;
  
  // The current line
  protected int _line;

  // The parse state
  protected ParseState _parseState;
  
  // The parser
  protected JspParser _jspParser;
  
  // The compiler configuration
  protected JspCompiler _jspCompiler;
  
  // The jsp property
  private JspPropertyGroup _jspPropertyGroup;

  // The jsf property
  private JsfPropertyGroup _jsfPropertyGroup;

  // The tag manager
  protected ParseTagManager _tagManager;

  /**
   * Returns the generator.
   */
  abstract public JspGenerator getGenerator();

  /**
   * Returns the top node.
   */
  abstract public JspNode getRootNode();

  /**
   * Sets the parse state.
   */
  public void setParseState(ParseState parseState)
  {
    _parseState = parseState;
  }

  /**
   * Returns the parse state.
   */
  public ParseState getParseState()
  {
    return _parseState;
  }

  /**
   * Sets the page config?
   */
  public void setPageConfig(JspPageConfig pageConfig)
  {
  }

  /**
   * Sets the compiler
   */
  public void setJspCompiler(JspCompiler compiler)
  {
    _jspCompiler = compiler;
  }

  /**
   * Returns the parse state.
   */
  public JspCompiler getJspCompiler()
  {
    return _jspCompiler;
  }

  /**
   * Sets the parser
   */
  public void setJspParser(JspParser parser)
  {
    _jspParser = parser;
  }

  /**
   * Returns the parse state.
   */
  public JspParser getJspParser()
  {
    return _jspParser;
  }
  
  /**
   * Sets the tag manager
   */
  public void setTagManager(ParseTagManager manager)
  {
    _tagManager = manager;
  }

  /**
   * Returns the tag manager
   */
  public ParseTagManager getTagManager()
  {
    return _tagManager;
  }
  
  /**
   * Sets the jsp-property-group
   */
  public void setJspPropertyGroup(JspPropertyGroup jsp)
  {
    _jspPropertyGroup = jsp;
  }
  
  /**
   * Gets the jsp-property-group
   */
  public JspPropertyGroup getJspPropertyGroup()
  {
    return _jspPropertyGroup;
  }

  /**
   * Returns true if fast-jstl is enabled.
   */
  public boolean isFastJstl()
  {
    JspPropertyGroup jsp = getJspPropertyGroup();

    if (jsp == null)
      return true;
    else
      return jsp.isFastJstl();
  }

  public JsfPropertyGroup getJsfPropertyGroup()
  {
    return _jsfPropertyGroup;
  }

  public void setJsfPropertyGroup(JsfPropertyGroup jsfPropertyGroup)
  {
    _jsfPropertyGroup = jsfPropertyGroup;
  }

  /**
   * Returns true if fast-jsf is enabled.
   */
  public boolean isFastJsf()
  {
    JsfPropertyGroup jsf = getJsfPropertyGroup();

    if (jsf == null)
      return false;
    else
      return jsf.isFastJsf();
  }

  /**
   * Returns true if require source is enabled.
   */
  public boolean getRequireSource()
  {
    JspPropertyGroup jsp = getJspPropertyGroup();

    if (jsp == null)
      return false;
    else
      return jsp.getRequireSource();
  }

  /**
   * Set for prototype builder.
   */
  public void setPrototype(boolean isPrototype)
  {
  }

  /**
   * Sets the source line number.
   */
  public void setLocation(Path sourcePath, String filename, int line)
  {
    _sourcePath = sourcePath;
    _filename = filename;
    _line = line;
  }
  
  /**
   * Starts the document
   */
  abstract public void startDocument()
    throws JspParseException;
  
  /**
   * Starts the document
   */
  abstract public void endDocument()
    throws JspParseException;

  /**
   * Starts an element.
   *
   * @param qname the name of the element to start
   */
  abstract public void startElement(QName qname)
    throws JspParseException;

  /**
   * Starts a prefix mapping.
   *
   * @param prefix the xml prefix
   * @param uri the namespace uri
   */
  abstract public void startPrefixMapping(String prefix, String uri)
    throws JspParseException;
  
  /**
   * Adds an attribute to the element.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  abstract public void attribute(QName name, String value)
    throws JspParseException;

  /**
   * Called after the attributes end.
   */
  abstract public void endAttributes()
    throws JspParseException;
  
  /**
   * Ends an element.
   *
   * @param name the name of the element to end
   */
  abstract public void endElement(String name)
    throws JspParseException;
  
  /**
   * Adds text.
   *
   * @param text the text to add
   */
  abstract public void text(String text)
    throws JspParseException;
  
  /**
   * Adds text.
   *
   * @param text the text to add
   */
  public void text(String text, String filename, int startLine, int endLine)
    throws JspParseException
  {
    text(text);
  }

  public void addNamespace(String prefix, String uri)
  {
    // getParseState().pushNamespace(prefix, uri);
    getCurrentNode().addNamespace(prefix, uri);
  }

  /**
   * Returns the current node.
   */
  abstract public JspNode getCurrentNode();

  public void startTagDependent()
  {
  }

  public boolean isTagDependent()
  {
    return false;
  }
}
