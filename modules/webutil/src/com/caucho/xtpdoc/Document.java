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
 * @author Emil Ong
 */

package com.caucho.xtpdoc;

import com.caucho.config.Config;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.servlet.ServletContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Document {
  private static Logger log = Logger.getLogger(Document.class.getName());

  private ServletContext _webApp;
  private Header _header;
  private Body _body;
  private Path _documentPath;
  private String _contextPath;
  private String _uri;
  private int _level;
  private Navigation _navigation;
  private NavigationItem _navItem;
  private String _encoding;
  private boolean _hasChildren;
  private boolean _isArticle;
  private boolean _isDisableAction;
  private boolean _isJavascriptEnabled = true;

  private String _redirect;

  private ReferenceDocument _referenceDocument;

  Document()
  {
    this(null, null, null, null, "utf-8");
  }

  public Document(Path documentPath, String contextPath)
  {
    this(null, documentPath, contextPath, null, "utf-8");
  }

  public Document(ServletContext webApp,
                  Path documentPath,
                  String contextPath,
                  String uri,
                  String encoding)
  {
    _webApp = webApp;
    _documentPath = documentPath;
    _contextPath = contextPath;
    _uri = uri;
    _encoding = encoding;
  }

  public boolean isArticle()
  {
    return _isArticle;
  }

  public void setArticle(boolean isArticle)
  {
    _isArticle = isArticle;
  }

  public void setDisableAction(boolean isDisable)
  {
    _isDisableAction = isDisable;
  }

  public boolean isDisableAction()
  {
    return _isDisableAction;
  }

  public boolean isJavascriptEnabled()
  {
    return _isJavascriptEnabled;
  }

  public void setJavascriptEnabled(boolean isJavascriptEnabled)
  {
    _isJavascriptEnabled = isJavascriptEnabled;
  }

  public Path getRealPath(String uri)
  {
    if (_webApp != null) {
      String contextPath = _webApp.getContextPath();

      if (uri.startsWith(contextPath))
        uri = uri.substring(contextPath.length());

      return Vfs.lookup(_webApp.getRealPath(uri));
    }
    else
      return Vfs.lookup("./" + uri);
  }

  NavigationItem getNavigation()
  {
    if (_navItem != null)
      return _navItem;

    ArrayList<Navigation> navList = new ArrayList();

    String uri = _uri;

    int p = uri.lastIndexOf('/');
    if (p > 0)
      uri = uri.substring(0, p + 1);

    ServletContext rootWebApp = _webApp.getContext("/");

    NavigationItem child = null;

    while (! uri.equals("") && rootWebApp != null) {
      String realPath = rootWebApp.getRealPath(uri);

      Path path = Vfs.lookup(realPath);

      Path toc = path.lookup("toc.xml");

      if (toc.canRead()) {
        Config config = new Config();
        config.setEL(false);

        Navigation navigation = new Navigation(this, uri, path, 0);

        navigation.setChild(child);

        try {
          config.configure(navigation, toc);

          navList.add(navigation);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);

          navigation = null;
        }

        if (navigation != null)
          child = navigation.getRootItem();
        else
          child = null;
      }

      p = uri.lastIndexOf('/', uri.length() - 2);
      if (p >= 0)
        uri = uri.substring(0, p + 1);
      else
        break;
    }

    if (navList.size() > 0) {
      Navigation nav = navList.get(0);

      _navItem = nav.getItem(_uri);
    }

    return _navItem;
  }

  void fillChildNavigation()
  {
    getNavigation();

    if (! _hasChildren) {
      _hasChildren = true;
      fillChildNavigation(_navItem);
    }
  }

  void fillChildNavigation(NavigationItem navItem)
  {
    if (navItem == null)
      return;

    for (NavigationItem child : navItem.getChildren()) {
      fillChildNavigation(child);
    }

    try {
      String link = navItem.getLink();

      if (link.indexOf('/') > 0) {
        ServletContext rootWebApp = _webApp.getContext("/");
        String uri = navItem.getUri();
        String realPath = rootWebApp.getRealPath(uri);

        Path path = Vfs.lookup(realPath);

        Path pwd = path.getParent();
        Path toc = pwd.lookup("toc.xml");

        if (toc.canRead()) {
          Config config = new Config();
          config.setEL(false);

          int p = uri.lastIndexOf('/');
          if (p > 0)
            uri = uri.substring(0, p + 1);

          Navigation navigation = new Navigation(this, uri, pwd, 0);

          navigation.setChild(navItem);

          config.configure(navigation, toc);

          if (navigation.getRootItem() != null)
            navItem.addChildren(navigation.getRootItem().getChildren());
        }
      }
    } 
    catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  ReferenceDocument getReferenceDocument()
  {
    if (_referenceDocument != null)
      return _referenceDocument;

    String uri = _uri;

    int p = uri.lastIndexOf('/');
    if (p > 0)
      uri = uri.substring(0, p + 1);

    ServletContext rootWebApp = _webApp.getContext("/");

    if (! uri.equals("") && rootWebApp != null) {
      String realPath = rootWebApp.getRealPath(uri);

      Path path = Vfs.lookup(realPath);

      Path ref = path.lookup("reference.xtp");

      if (ref.canRead()) {
        Config config = new Config();
        config.setEL(false);

        try {
          _referenceDocument = 
            new ReferenceDocument(_webApp, ref, _contextPath, 
                                  uri + "reference.xtp", _encoding);

          config.configure(_referenceDocument, ref);
        }
        catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);

          _referenceDocument = null;
        }
      }
    }

    return _referenceDocument;
  }

  public Path getDocumentPath()
  {
    return _documentPath;
  }

  public String getContextPath()
  {
    return _contextPath;
  }

  public String getURI()
  {
    return _uri;
  }

  public Header getHeader()
  {
    return _header;
  }

  public String getName()
  {
    // XXX
    return "";
  }

  public Header createHeader()
  {
    _header = new Header(this);
    return _header;
  }

  public Body createBody()
  {
    return new Body(this);
  }

  public void setBody(Body body)
  {
    _body = body;
  }

  public Body getBody()
  {
    return _body;
  }

  public void setRedirect(String href)
  {
    _redirect = href;
  }

  public String getRedirect()
  {
    return _redirect;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartDocument(_encoding, "1.0");
    out.writeDTD("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                 "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");

    out.writeStartElement("html");
    out.writeDefaultNamespace("http://www.w3.org/1999/xhtml");

    if (_header != null)
      _header.writeHtml(out);

    if (_body != null)
      _body.writeHtml(out);

    out.writeEndElement(); // html
  }

  public void writeLeftNav(XMLStreamWriter out)
    throws XMLStreamException
  {
    NavigationItem item = getNavigation();

    if (item != null) {
      item.writeLeftNav(out);
    }
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    out.println("\\documentclass{article}");

    _header.writeLaTeXTop(out);
    _body.writeLaTeXTop(out);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    _header.writeLaTeX(out);
    _body.writeLaTeX(out);
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    _header.writeLaTeXEnclosed(out);
    _body.writeLaTeXEnclosed(out);
  }

  public void writeLaTeXArticle(PrintWriter out)
    throws IOException
  {
    setArticle(true);
    
    _header.writeLaTeXArticle(out);
    _body.writeLaTeX(out);
  }

  public String toString()
  {
    return "Document[" + _documentPath + "]";
  }
}
