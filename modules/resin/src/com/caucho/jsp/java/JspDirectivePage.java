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

package com.caucho.jsp.java;

import com.caucho.jsp.JspParseException;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;

import javax.servlet.jsp.HttpJspPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.*;

public class JspDirectivePage extends JspNode {
  private static final Logger log
    = Logger.getLogger(JspDirectivePage.class.getName());
  static L10N L = new L10N(JspDirectivePage.class);
  
  private static final QName IS_EL_IGNORED = new QName("isELIgnored");
  private static final QName IS_VELOCITY_ENABLED =
    new QName("isVelocityEnabled");
  private static final QName INFO = new QName("info");
  private static final QName CONTENT_TYPE = new QName("contentType");
  private static final QName PAGE_ENCODING = new QName("pageEncoding");
  private static final QName LANGUAGE = new QName("language");
  private static final QName IMPORT = new QName("import");
  private static final QName SESSION = new QName("session");
  private static final QName BUFFER = new QName("buffer");
  private static final QName ERROR_PAGE = new QName("errorPage");
  private static final QName IS_ERROR_PAGE = new QName("isErrorPage");
  private static final QName AUTO_FLUSH = new QName("autoFlush");
  private static final QName IS_THREAD_SAFE = new QName("isThreadSafe");
  private static final QName EXTENDS = new QName("extends");
  private static final QName TRIM_WS = new QName("trimDirectiveWhitespaces");
  private static final QName DEFER = new QName("deferredSyntaxAllowedAsLiteral");

  
  /**
   * Adds an attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (IS_EL_IGNORED.equals(name)) {
      boolean isIgnored = value.equals("true");

      if (_parseState.isELIgnoredPageSpecified() &&
          isIgnored != _parseState.isELIgnored())
        throw error(L.l("isELIgnored values conflict"));

      _parseState.setELIgnored(isIgnored);
      _parseState.setELIgnoredPageSpecified(true);
    }
    
    /*
    else if (name.equals("isScriptingInvalid"))
      _parseState.setScriptingInvalid(value.equals("true"));
    */
    else if (IS_VELOCITY_ENABLED.equals(name))
      _parseState.setVelocityEnabled(value.equals("true"));
    else if (INFO.equals(name)) {
      String oldInfo = _parseState.getInfo();
      
      if (oldInfo != null && ! value.equals(oldInfo))
        throw error(L.l("info '{0}' conflicts with previous value of info '{1}'.  Check the .jsp and any included .jsp files for conflicts.", value, oldInfo));
      
      _parseState.setInfo(value);
    }
    else if (CONTENT_TYPE.equals(name)) {
      String oldContentType = _parseState.getContentType();
      
      if (oldContentType != null && ! value.equals(oldContentType))
        throw error(L.l("contentType '{0}' conflicts with previous value of contentType '{1}'.  Check the .jsp and any included .jsp files for conflicts.", value, oldContentType));
      
      _parseState.setContentType(value);
      String charEncoding = parseCharEncoding(value);
      if (charEncoding != null)
        _parseState.setCharEncoding(charEncoding);
    }
    else if (PAGE_ENCODING.equals(name)) {
      String oldEncoding = _parseState.getPageEncoding();

      /*
      // jsp/01f1
      if (oldEncoding != null) {
        String oldCanonical = Encoding.getMimeName(oldEncoding);
        String newCanonical = Encoding.getMimeName(value);

        if (! newCanonical.equals(oldCanonical))
          throw error(L.l("pageEncoding '{0}' conflicts with previous value of pageEncoding '{1}'.  Check the .jsp and any included .jsp files for conflicts.", value, oldEncoding));
      }
      */

      try {
        _parseState.setPageEncoding(value);
        // _parseState.setCharEncoding(value);
      } catch (JspParseException e) {
        log.log(Level.FINER, e.toString(), e);

        throw error(e.getMessage());
      }
    }
    else if (LANGUAGE.equals(name)) {
      if (! value.equals("java"))
        throw error(L.l("'{0}' is not supported as a JSP scripting language.",
                        value));
    }
    else if (IMPORT.equals(name)) {
      _parseState.addImport(value);
    }
    else if (SESSION.equals(name)) {
      boolean isValid = false;
      
      if (value.equals("true"))
        isValid = _parseState.setSession(true);
      else if (value.equals("false"))
        isValid = _parseState.setSession(false);
      else
        throw error(L.l("session expects 'true' or 'false' at '{0}'",
                        value));

      _parseState.markSessionSet();

      if (! isValid)
        throw error(L.l("session is assigned different values."));
    }
    else if (BUFFER.equals(name)) {
      boolean isValid = _parseState.setBuffer(processBufferSize(value));

      _parseState.markBufferSet();
      
      if (! isValid)
        throw error(L.l("buffer is assigned different values."));

      if (_parseState.getBuffer() == 0 && ! _parseState.isAutoFlush())
        throw error(L.l("buffer must be non-zero when autoFlush is false."));
    }
    else if (ERROR_PAGE.equals(name)) {
      String errorPage = _parseState.getErrorPage();
      
      String newErrorPage = getRelativeUrl(value);

      _parseState.setErrorPage(newErrorPage);

      if (errorPage != null && ! errorPage.equals(newErrorPage)) {
        _parseState.setErrorPage(null);
        throw error(L.l("errorPage is assigned different value '{0}'.",
                        newErrorPage));
      }
    }
    else if (IS_ERROR_PAGE.equals(name)) {
      boolean isValid = false;
      
      if (value.equals("true"))
        isValid = _parseState.setErrorPage(true);
      else if (value.equals("false"))
        isValid = _parseState.setErrorPage(false);
      else
        throw error(L.l("isErrorPage expects 'true' or 'false' at '{0}'",
                        value));

      _parseState.markErrorPage();

      if (! isValid)
        throw error(L.l("isErrorPage is assigned different values."));
    }
    else if (AUTO_FLUSH.equals(name)) {
      boolean isValid = false;
      
      if (value.equals("true"))
        isValid = _parseState.setAutoFlush(true);
      else if (value.equals("false"))
        isValid = _parseState.setAutoFlush(false);
      else
        throw error(L.l("autoFlush expects 'true' or 'false' at '{0}'",
                        value));

      if (! isValid)
        throw error(L.l("autoFlush is assigned different values."));
      
      if (_parseState.getBuffer() == 0 && ! _parseState.isAutoFlush())
        throw error(L.l("buffer must be non-zero when autoFlush is false."));

      _parseState.markAutoFlushSet();
    }
    else if (IS_THREAD_SAFE.equals(name)) {
      boolean isValid = false;
      
      if (value.equals("true"))
        isValid = _parseState.setThreadSafe(true);
      else if (value.equals("false"))
        isValid = _parseState.setThreadSafe(false);
      else
        throw error(L.l("isThreadSafe expects 'true' or 'false' at '{0}'",
                        value));

      _parseState.markThreadSafeSet();

      if (! isValid)
        throw error(L.l("isThreadSafe is assigned different values."));
    }
    else if (EXTENDS.equals(name)) {
      Class cl = null;

      try {
        cl = CauchoSystem.loadClass(value);
      } catch (Exception e) {
        throw error(e);
      }
        
      if (! HttpJspPage.class.isAssignableFrom(cl))
        throw error(L.l("'{0}' must implement HttpJspPage.  The class named by jsp:directive.page extends='...' must implement HttpJspPage.", value));
      
      Class oldExtends = _parseState.getExtends();
      
      if (oldExtends != null && ! cl.equals(oldExtends))
        throw error(L.l("extends '{0}' conflicts with previous value of extends '{1}'.  Check the .jsp and any included .jsp files for conflicts.", value, oldExtends.getName()));
      
      _parseState.setExtends(cl);
    }
    else if (TRIM_WS.equals(name)) {
      if (value.equals("true"))
        _parseState.setTrimWhitespace(true);
      else if (value.equals("false"))
        _parseState.setTrimWhitespace(false);
      else
        throw error(L.l("trimDirectiveWhitespaces expects 'true' or 'false' at '{0}'",
                        value));
    }
    else if (DEFER.equals(name)) {
      if (value.equals("true"))
        _parseState.setDeferredSyntaxAllowedAsLiteral(true);
      else if (value.equals("false"))
        _parseState.setDeferredSyntaxAllowedAsLiteral(false);
      else
        throw error(L.l("deferredSyntaxAllowedAsLiteral expects 'true' or 'false' at '{0}'",
                        value));
    }
    else {
      throw error(L.l("'{0}' is an unknown JSP page directive attribute.  See the JSP documentation for a complete list of page directive attributes.",
                      name.getName()));
    }
  }

  /**
   * Parses the buffer size directive, grabbing the size out from the units.
   *
   * @param value buffer size string.
   * @return the size of the buffer in kb.
   */
  private int processBufferSize(String value) throws JspParseException
  {
    if (value.equals("none"))
      return 0;

    int i = 0;
    int kb = 0;
    for (; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (ch >= '0' && ch <= '9')
        kb = 10 * kb + ch - '0';
      else
        break;
    }

    if (! value.substring(i).equals("kb"))
      throw error(L.l("Expected buffer size at '{0}'.  Buffer sizes must end in 'kb'", value));

    return 1024 * kb;
  }
  
  protected String getRelativeUrl(String value)
  {
    if (value.length() > 0 && value.charAt(0) == '/')
      return value;
    else
      return _parseState.getUriPwd() + value;
  }

  /**
   * Charset can be specific as follows:
   *  test/html; z=9; charset=utf8; w=12
   */
  static String parseCharEncoding(String type)
    throws JspParseException
  {
    type = type.toLowerCase();
    int i;
    char ch;
    while ((i = type.indexOf(';')) >= 0 && i < type.length()) {
      i++;
      while (i < type.length() && ((ch = type.charAt(i)) == ' ' || ch == '\t'))
        i++;

      if (i >= type.length())
        return null;

      type = type.substring(i);
      i = type.indexOf('=');
      if (i >= 0) {
        if (! type.startsWith("charset"))
          continue;

        for (i++;
             i < type.length() && ((ch = type.charAt(i)) == ' ' || ch == '\t');
             i++) {
        }

        type = type.substring(i);
      }
      
      for (i = 0;
           i < type.length() && (ch = type.charAt(i)) != ';' && ch != ' ';
           i++) {
      }

      return type.substring(0, i);
    }

    return null;
  }
  
  /**
   * Called when the tag ends.
   */
  public void endAttributes()
    throws JspParseException
  {
    if (_gen.isTag())
      throw error(L.l("page directives are forbidden in tags."));
  }
  
  /**
   * Return true if the node only has static text.
   */
  public boolean isStatic()
  {
    return true;
  }

  /**
   * Generates the XML text representation for the tag validation.
   *
   * @param os write stream to the generated XML.
   */
  public void printXml(WriteStream os)
    throws IOException
  {
    os.print("<jsp:directive.page");
    printJspId(os);
    if (! _parseState.isELIgnored())
      os.print(" el-ignored='false'");
    /*
    if (! _parseState.isScriptingEnabled())
      os.print(" scripting-enabled='false'");
    */
    if (_parseState.getContentType() != null)
      os.print(" content-type='" + _parseState.getContentType() + "'");

    ArrayList<String> imports = _parseState.getImportList();

    if (imports != null && imports.size() != 0) {
      os.print(" import='");
      for (int i = 0; i < imports.size(); i++) {
        if (i != 0)
          os.print(',');
        os.print(imports.get(i));
      }
      os.print("'");
    }
    
    
    os.print("/>");
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JspJavaWriter out)
    throws Exception
  {
  }
}
