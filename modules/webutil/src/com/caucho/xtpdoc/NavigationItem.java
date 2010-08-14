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

import javax.annotation.PostConstruct;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

public class NavigationItem {
  private static final Logger log 
    = Logger.getLogger(NavigationItem.class.getName());

  private Navigation _navigation;
  private NavigationItem _parent;

  private String _uri;
  private String _refUri;

  private int _maxDepth = 3;
  private int _depth;
  private boolean _isRelative;
  private String _link;
  private String _ref;
  private String _title;
  private String _product;
  private Document _document;
  private String _description;
  private ContentItem _fullDescription;
  private boolean _atocDescend;
  private Navigation _child;
  private ArrayList<NavigationItem> _items = new ArrayList<NavigationItem>();

  public NavigationItem(Navigation navigation,
                        NavigationItem parent,
                        int depth)
  {
    _navigation = navigation;
    _parent = parent;
    
    _document = navigation.getDocument();
    _depth = depth;
  }

  public Navigation getNavigation()
  {
    return _navigation;
  }

  NavigationItem getParent()
  {
    return _parent;
  }

  void setParent(NavigationItem parent)
  {
    _parent = parent;
  }

  NavigationItem getPrevious()
  {
    NavigationItem parent = getParent();

    if (parent != null) {
      int p = parent._items.indexOf(this);

      if (p > 0) {
        NavigationItem ptr = _parent._items.get(p - 1);

        while (ptr != null && ptr._items.size() > 0) {
          ptr = ptr._items.get(ptr._items.size() - 1);
        }

        return ptr;
      }
      else
        return parent;
    }

    return null;
  }

  NavigationItem getNext()
  {
    NavigationItem ptr = this;
    NavigationItem child = null;

    while (ptr != null) {
      int p = ptr._items.indexOf(child);

      if (p < 0 && ptr._items.size() > 0)
        return ptr._items.get(0);
      else if (p + 1 < ptr._items.size())
        return ptr._items.get(p + 1);

      child = ptr;
      ptr = ptr.getParent();
    }

    return null;
  }

  ArrayList<NavigationItem> getChildren()
  {
    return _items;
  }

  void addChildren(ArrayList<NavigationItem> children)
  {
    _items.addAll(children);
  }

  String getUri()
  {
    return _uri;
  }

  protected void initSummary()
  {
    if (_child != null || _fullDescription != null)
      return;

    Path rootPath = _document.getDocumentPath().getParent();

    if (_uri != null) {
      Path linkPath = _document.getRealPath(_uri);

      if (linkPath.exists() && linkPath.getPath().endsWith(".xtp")) {
        Config config = new Config();
        config.setEL(false);

        try {
          config.configure(_document, linkPath);

          if (_document.getHeader() != null)
            _fullDescription = _document.getHeader().getDescription();
          else
            _fullDescription = new Description(_document);
        } catch (NullPointerException e) {
          log.info("error configuring " + linkPath + ": " + e);
        } catch (Exception e) {
          log.info("error configuring " + linkPath + ": " + e);
        }

        if (_atocDescend) {
          Path linkRoot = linkPath.getParent();

          if (linkRoot.equals(_navigation.getRootPath().getParent()))
            return;

          Path subToc = linkPath.getParent().lookup("toc.xml");

          if (subToc.exists()) {
            _child = new Navigation(_navigation,
                                    _uri,
                                    linkRoot,
                                    _depth + 1);

            try {
              config.configure(_child, subToc);
            } catch (Exception e) {
              log.info("Failed to configure " + subToc + ": " + e);
            }
          } else {
            log.info(subToc + " does not exist!");
          }
        }
      }
    }
  }

  public void setATOCDescend(boolean atocDescend)
  {
    _atocDescend = atocDescend;
  }

  public void setLink(String link)
  {
    _link = link;

    _isRelative  = (_link.indexOf(':') < 0);
  }

  public String getLink()
  {
    return _link;
  }

  public void setReference(String ref)
  {
    _ref = ref;
  }

  public String getReference()
  {
    return _ref;
  }

  public String getReferenceUri()
  {
    return _refUri;
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public String getTitle()
  {
    return _title;
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public void setProduct(String product)
  {
    _product = product;
  }

  public NavigationItem createItem()
  {
    NavigationItem item = new NavigationItem(_navigation, this, _depth + 1);
    _items.add(item);
    return item;
  }

  @PostConstruct
  public void init()
  {
    if (_isRelative) {
      _uri = _navigation.getUri() + _link;

      if (_ref != null) 
        _refUri = _navigation.getUri() + _ref;
    }
    else {
      _uri = _link;

      if (_ref != null) 
        _refUri = _ref;
    }

    _navigation.putItem(_uri, this);
  }

  public void writeHtml(XMLStreamWriter out, String path)
    throws XMLStreamException
  {
    initSummary();

    out.writeStartElement("dl");
    out.writeAttribute("class", "atoc-top");
    
    for (NavigationItem item : _items)
      item.writeHtmlImpl(out, path, 0, 0, 3);
    
    out.writeEndElement(); // dl
  }

  public void writeHtml(XMLStreamWriter out, String path,
                        int depth, int styleDepth, int maxDepth)
    throws XMLStreamException
  {
    if (depth + styleDepth <= 1)
      initSummary();

    for (NavigationItem item : _items)
      item.writeHtmlImpl(out, path, depth, styleDepth, maxDepth);
  }

  protected void writeHtmlImpl(XMLStreamWriter out, String path,
                               int depth, int styleDepth, int maxDepth)
    throws XMLStreamException
  {
    if (maxDepth <= depth)
      return;

    if (depth + styleDepth <= 1)
      initSummary();

    if (_child != null && depth + 1 < maxDepth)
      _child.initSummary();

    if (depth <= 1) {
      out.writeStartElement("h2");
      out.writeAttribute("class", "section");

      if (_link != null) {
        out.writeStartElement("a");

        if (_isRelative)
          out.writeAttribute("href", path + _link);
        else
          out.writeAttribute("href", _link);

        out.writeCharacters(_title);
        out.writeEndElement(); // a
      }
      else
        out.writeCharacters(_title);

      out.writeEndElement(); // h2
    }
    else {
      out.writeStartElement("dt");

      out.writeStartElement("b");

      if (_link != null) {
        out.writeStartElement("a");

        if (_isRelative)
          out.writeAttribute("href", path + _link);
        else
          out.writeAttribute("href", _link);

        out.writeCharacters(_title);
        out.writeEndElement(); // a

        if (_ref != null) {
          out.writeStartElement("span");
          out.writeAttribute("class", "ref");
          out.writeCharacters(" ");
          out.writeEntityRef("laquo");
          out.writeStartElement("a");

          if (_isRelative)
            out.writeAttribute("href", path + _ref);
          else
            out.writeAttribute("href", _ref);

          out.writeCharacters("ref");
          out.writeEndElement(); // a
          out.writeEntityRef("raquo");
          out.writeEndElement(); // span
        }
      }

      out.writeEndElement(); // b

      if (_fullDescription != null && depth + styleDepth <= 1) {
      }
      else if (_description != null && depth > 1) {
        out.writeCharacters(" - ");
        out.writeCharacters(_description);
      }

      out.writeEndElement(); // dt
    }

    out.writeStartElement("dd");

    // XXX: brief/paragraph/none
    if (_fullDescription != null && depth + styleDepth <= 1) {
      out.writeStartElement("p");
      _fullDescription.writeHtml(out);
      out.writeEndElement(); // p
    }

    if (_link != null) {
      int p = _link.lastIndexOf('/');
      String tail;
      if (p >= 0)
        tail = path + _link.substring(0, p + 1);
      else
        tail = path;

      String depthString = (depth == 0) ? "top" : ("" + depth);
      boolean hasDL = false;

      if (_child != null || _items.size() > 0) {
        out.writeStartElement("dl");
        out.writeAttribute("class", "atoc-" + (depth + 1));

        if (_child != null)
          _child.writeHtml(out, tail, depth + 1, styleDepth, maxDepth);
        else {
          for (NavigationItem item : _items)
            item.writeHtmlImpl(out, tail, depth + 1, styleDepth, maxDepth);
        }
        out.writeEndElement();
      }
    }

    out.writeEndElement(); // dd
  }

  public void writeLeftNav(XMLStreamWriter out)
    throws XMLStreamException
  {
    if (_parent != null)
      _parent.writeLeftNavPre(out, this, 1);

    writeLeftNavItem(out, this);
      
    if (_items.size() > 0) {
      out.writeStartElement("ul");
      out.writeAttribute("class", "leftnav");

      for (NavigationItem item : _items)
        item.writeLeftNavNoParent(out, this);

      out.writeEndElement(); // ul
    }

    if (_parent != null)
      _parent.writeLeftNavPost(out, this);
  }

  public int writeLeftNavPre(XMLStreamWriter out, 
                             NavigationItem caller, 
                             int padding)
    throws XMLStreamException
  {
    if (_parent != null)
      padding = _parent.writeLeftNavPre(out, this, padding);

    writeLeftNavItem(out, caller);

    if (_items.size() > 0) {
      out.writeStartElement("ul");
      out.writeAttribute("class", "leftnav");

      for (NavigationItem item : _items) {
        if (item.getUri().equals(caller.getUri()))
          return padding + 1;

        item.writeLeftNavItem(out, caller);
      }

      return padding + 1;
    }
    else {
      return padding;
    }
  }

  public void writeLeftNavPost(XMLStreamWriter out, NavigationItem caller)
    throws XMLStreamException
  {
    if (_items.size() > 0) {
      boolean found = false;

      for (NavigationItem item : _items) {
        if (found) 
          item.writeLeftNavItem(out, caller);

        if (item.getUri().equals(caller.getUri()))
          found = true;
      }

      out.writeEndElement(); // ul
    }

    if (_parent != null) {
      _parent.writeLeftNavPost(out, this);
    }
  }

  public void writeLeftNavNoParent(XMLStreamWriter out, NavigationItem caller)
    throws XMLStreamException
  {
    writeLeftNavItem(out, caller);

    if (_items.size() > 0) {
      out.writeStartElement("ul");
      out.writeAttribute("class", "leftnav");

      for (NavigationItem item : _items)
        item.writeLeftNavNoParent(out, this);

      out.writeEndElement(); // ul
    }
  }

  private void writeLeftNavItem(XMLStreamWriter out, NavigationItem caller)
    throws XMLStreamException
  {
    out.writeStartElement("li");

    if (caller != null && _uri.equals(caller.getUri())) {
      out.writeAttribute("class", "selected");
    }

    out.writeStartElement("a");
    out.writeAttribute("href", _uri);
    out.writeAttribute("class", "leftnav");
    out.writeCharacters(_title.toLowerCase());
    out.writeEndElement(); // a

    if (_refUri != null) {
      out.writeStartElement("span");
      out.writeAttribute("class", "ref");
      out.writeCharacters(" ");
      out.writeEntityRef("laquo");
      out.writeStartElement("a");
      out.writeAttribute("href", _refUri);
      out.writeAttribute("class", "leftnav");
      out.writeCharacters("ref");
      out.writeEndElement(); // a
      out.writeEntityRef("raquo");
      out.writeEndElement(); // span
    }

    out.writeEndElement(); // li
  }

  public void writeLink(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("a");
    out.writeAttribute("href", _uri);

    out.writeCharacters(_title.toLowerCase());
    out.writeEndElement(); // a
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
  }
}
