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
 * @author Sam
 */


package com.caucho.netbeans.ide;

import com.caucho.netbeans.util.JspNameUtil;

import org.netbeans.modules.j2ee.deployment.plugins.spi.FindJSPServlet;

import javax.enterprise.deploy.spi.DeploymentManager;
import java.io.File;

public final class ResinFindJSPServlet
  implements FindJSPServlet
{

  private static final String WEB_INF_TAGS = "WEB-INF/tags/";     // NOI18N
  private static final String META_INF_TAGS = "META-INF/tags/";   // NOI18N

  public ResinFindJSPServlet(DeploymentManager manager)
  {
  }


  public File getServletTempDirectory(String moduleContextPath)
  {
    return new File("unimplemented"); // XXX: unimplemented
  }


  public String getServletResourcePath(String moduleContextPath,
                                       String jspResourcePath)
  {
    //String path = module.getWebURL();

    //we expect .tag file; in other case, we expect .jsp file
    String path = getTagHandlerClassName(jspResourcePath);
    if (path != null) { // .tag
      path = path.replace('.', '/') + ".java";
    }
    else { // .jsp
      path = getServletPackageName(jspResourcePath).replace('.', '/') +
             '/' +
             getServletClassName(jspResourcePath) +
             ".java"; // NOI18N
    }
    return path;

    //int lastDot = jspResourcePath.lastIndexOf('.');
    //return jspResourcePath.substring(0, lastDot) + "$jsp.java"; // NOI18N
  }

  // copied from org.apache.jasper.JspCompilationContext
  private String getServletPackageName(String jspUri)
  {
    String dPackageName = getDerivedPackageName(jspUri);
    if (dPackageName.length() == 0) {
      return JspNameUtil.JSP_PACKAGE_NAME;
    }
    return JspNameUtil.JSP_PACKAGE_NAME + '.' + getDerivedPackageName(jspUri);
  }

  // copied from org.apache.jasper.JspCompilationContext
  private String getDerivedPackageName(String jspUri)
  {
    int iSep = jspUri.lastIndexOf('/');
    return (iSep > 0)
           ? JspNameUtil.makeJavaPackage(jspUri.substring(0, iSep))
           : ""; // NOI18N
  }

  // copied from org.apache.jasper.JspCompilationContext
  private String getServletClassName(String jspUri)
  {
    int iSep = jspUri.lastIndexOf('/') + 1;
    return JspNameUtil.makeJavaIdentifier(jspUri.substring(iSep));
  }

  public String getServletEncoding(String moduleContextPath,
                                   String jspResourcePath)
  {
    return "UTF8"; // NOI18N
  }

  /**
   * Copied (and slightly modified) from org.apache.jasper.compiler.JspUtil
   * <p/>
   * Gets the fully-qualified class name of the tag handler corresponding to the
   * given tag file path.
   *
   * @param path Tag file path
   *
   * @return Fully-qualified class name of the tag handler corresponding to the
   *         given tag file path
   */
  private String getTagHandlerClassName(String path)
  {

    String className = null;
    int begin = 0;
    int index;

    index = path.lastIndexOf(".tag");
    if (index == -1) {
      return null;
    }

    index = path.indexOf(WEB_INF_TAGS);
    if (index != -1) {
      className = "org.apache.jsp.tag.web.";      // NIO18N
      begin = index + WEB_INF_TAGS.length();
    }
    else {
      index = path.indexOf(META_INF_TAGS);
      if (index != -1) {
        className = "org.apache.jsp.tag.meta.";  // NIO18N
        begin = index + META_INF_TAGS.length();
      }
      else {
        return null;
      }
    }

    className += JspNameUtil.makeJavaPackage(path.substring(begin));

    return className;
  }

}
