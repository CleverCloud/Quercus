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

package com.caucho.xsl;

import com.caucho.xml.XmlUtil;

import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;

/**
   * Handler for xml-stylesheet the resin.conf file.
   */
public class XmlStylesheetReader extends DefaultHandler {
  private ArrayList _stylesheets = new ArrayList();

  public XmlStylesheetReader()
  {
  }

  public void processingInstruction(String name, String value)
  {
    if (! name.equals("xml-stylesheet"))
      return;

    try {
      HashMap values = XmlUtil.splitNameList(value);

      String href = (String) values.get("href");
      String media = (String) values.get("media");
      String title = (String) values.get("title");
      String charset = (String) values.get("charset");

      if (href == null)
        return;

      _stylesheets.add(new XmlStylesheet(href, media, title, charset));
    } catch (Exception e) {
    }
  }

  public String getAssociatedStylesheet(String media,
                                        String title,
                                        String charset)
  {
    String best = null;
    int bestCost = -1;

    for (int i = 0; i < _stylesheets.size(); i++) {
      XmlStylesheet ss = (XmlStylesheet) _stylesheets.get(i);

      int cost = ss.match(media, title, charset);

      if (cost > bestCost) {
        bestCost = cost;
        best = ss.getSystemId();
      }
    }
    
    return best;
  }

  static class XmlStylesheet {
    private String _systemId;
    private String _media;
    private String _title;
    private String _charset;

    XmlStylesheet(String systemId, String media, String title, String charset)
    {
      _systemId = systemId;
      _media = media;
      _title = title;
      _charset = charset;
    }

    public String getSystemId()
    {
      return _systemId;
    }

    public int match(String media, String title, String charset)
    {
      int cost = 1;

      if (_media != null && _media.equals(media))
        cost++;
      else if (_media != null && ! _media.equals(media))
        return -1;
      else if (_media == null && media == null)
        cost++;

      if (_title != null && _title.equals(title))
        cost++;
      else if (_title != null && ! _title.equals(title))
        return -1;
      else if (_title == null && title == null)
        cost++;

      if (_charset != null && _charset.equals(charset))
        cost++;
      else if (_charset != null && ! _charset.equals(charset))
        return -1;
      else if (_charset == null && charset == null)
        cost++;

      return cost;
    }
  }
}
