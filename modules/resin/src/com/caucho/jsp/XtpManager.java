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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp;

import com.caucho.server.webapp.WebApp;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;

import javax.servlet.ServletConfig;
import javax.servlet.jsp.JspFactory;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Manages XTP templates.  The XtpManager allows for a template style of
 * XTP.  A servlet can use XTP for its output.
 *
 * <p>The template API lets servlets assign implicit script variables for
 * JavaScript.  Filling up a HashMap with the variable name will do the
 * trick.
 *
 * <p>An example servlet may look something like the following.  If
 * the stylesheet or the generated JSP file use JavaScript,
 * testObject will be assigned to the global variable "test".
 *
 * <p><pre><code>
 * void service(ServletRequest req, ServletResponse res)
 * {
 *   // do some processing here
 *
 *   // get the XTP template
 *   XtpManager manager =  XtpManager.getManager(getServletContext());
 *   Page page = manager.createPage("WEB-INF/xtp/test.xtp");
 *
 *   // fill in implicit variables (optional)
 *   HashMap vars = new HashMap();
 *   vars.put("test", testObject);
 *
 *   // execute the template
 *   page.service(req, res, vars);
 * }
 * </code></pre>
 *
 * @see Page
 */
public class XtpManager extends PageManager {
  private static final Logger log
    = Logger.getLogger(XtpManager.class.getName());

  private boolean _strictXml;
  private boolean _toLower = true;
  private boolean _entitiesAsText = true;
  private XslManager _xslManager;
  private JspManager _jspManager;
  private String _defaultStylesheet = "default.xsl";

  XtpManager()
  {
  }

  void initWebApp(WebApp context)
  {
    super.initWebApp(context);
    
    if (JspFactory.getDefaultFactory() == null)
      JspFactory.setDefaultFactory(new QJspFactory());
    
    _xslManager = new XslManager(context);
    _jspManager = new JspManager();
    _jspManager.initWebApp(context);
  }

  /**
   * Returns the default stylesheet.
   */
  public String getDefaultStylesheet()
  {
    return _defaultStylesheet;
  }

  /**
   * Sets the default stylesheet.
   */
  public void setDefaultStylesheet(String stylesheet)
  {
    _defaultStylesheet = stylesheet;
  }

  /**
   * Requires XTP documents conform to strict XML.  If false (the
   * default), XTP files are loose HTML.
   */
  public void setStrictXml(boolean strictXml)
  {
    _strictXml = strictXml;
  }

  /**
   * Requires XTL stylesheets to conform to strict XSL.  If false (the
   * default), XTP files follow the XSLT-lite syntax.
   */
  public void setStrictXsl(boolean strictXsl)
  {
    _xslManager.setStrictXsl(strictXsl);
  }

  /**
   * If true, HTML tags are normalized to lower-case.
   */
  public void setToLower(boolean toLower)
  {
    _toLower = toLower;
  }

  /**
   * If true, parse XML with entities as text
   */
  public void setEntitiesAsText(boolean entitiesAsText)
  {
    _entitiesAsText = entitiesAsText;
  }

  /**
   * Creates a new XTP page.  The stylesheet is given by 
   * &lt;?xml-stylesheet href='...'?>, or default.xsl if none is specified.
   *
   * <p>The stylesheet is parsed immediately, but it is only applied
   * when the request is processed.  See XtpPage for the actual
   * processing.
   *
   * @param path Path to the XTP file.
   * @param uri The uri for the XTP file.
   * @param uriPwd The parent of uri.
   *
   * @return the page or null for not found
   */
  Page createPage(Path path, String uri, String className,
                  ServletConfig config,
                  ArrayList<PersistentDependency> dependList)
    throws Exception
  {
    if (path == null || ! path.canRead() || path.isDirectory())
      return null;

    XtpPage xtpPage = new XtpPage(path, uri,
                                  className,
                                  _webApp, _xslManager, _strictXml);
    xtpPage.setManager(_jspManager);
    xtpPage.setHtmlToLower(_toLower);

    return xtpPage;
  }
}
