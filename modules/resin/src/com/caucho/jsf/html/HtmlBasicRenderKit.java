/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.html;

import java.io.*;
import java.util.*;

import javax.faces.context.*;
import javax.faces.render.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

public class HtmlBasicRenderKit extends RenderKit
{
  private static final L10N L = new L10N(HtmlBasicRenderKit.class);
  
  private ResponseStateManager _responseStateManager
    = new ResponseStateManagerImpl();
  
  private HashMap<Key,Renderer> _rendererMap
    = new HashMap<Key,Renderer>();

  private Key _key = new Key();

  public HtmlBasicRenderKit()
  {
    addRenderer("javax.faces.Command", "javax.faces.Button",
                HtmlCommandButtonRenderer.RENDERER);
    
    addRenderer("javax.faces.Command", "javax.faces.Link",
                HtmlCommandLinkRenderer.RENDERER);
    
    addRenderer("javax.faces.Data", "javax.faces.Table",
                HtmlDataTableRenderer.RENDERER);
    
    addRenderer("javax.faces.Form", "javax.faces.Form",
                HtmlFormRenderer.RENDERER);
    
    addRenderer("javax.faces.Graphic", "javax.faces.Image",
                HtmlGraphicImageRenderer.RENDERER);
    
    addRenderer("javax.faces.Input", "javax.faces.Secret",
                HtmlInputSecretRenderer.RENDERER);
    
    addRenderer("javax.faces.Input", "javax.faces.Hidden",
                HtmlInputHiddenRenderer.RENDERER);
    
    addRenderer("javax.faces.Input", "javax.faces.Text",
                HtmlInputTextRenderer.RENDERER);
    
    addRenderer("javax.faces.Input", "javax.faces.Textarea",
                HtmlInputTextareaRenderer.RENDERER);
    
    addRenderer("javax.faces.Message", "javax.faces.Message",
                HtmlMessageRenderer.RENDERER);
    
    addRenderer("javax.faces.Messages", "javax.faces.Messages",
                HtmlMessagesRenderer.RENDERER);
    
    addRenderer("javax.faces.Output", "javax.faces.Format",
                HtmlOutputFormatRenderer.RENDERER);
    
    addRenderer("javax.faces.Output", "javax.faces.Label",
                HtmlOutputLabelRenderer.RENDERER);
    
    addRenderer("javax.faces.Output", "javax.faces.Link",
                HtmlOutputLinkRenderer.RENDERER);
    
    addRenderer("javax.faces.Output", "javax.faces.Text",
                HtmlOutputTextRenderer.RENDERER);

    addRenderer("javax.faces.Output", "javax.faces.Format",
                HtmlOutputFormatRenderer.RENDERER);

    addRenderer("javax.faces.Panel", "javax.faces.Grid",
                HtmlPanelGridRenderer.RENDERER);
    
    addRenderer("javax.faces.Panel", "javax.faces.Group",
                HtmlPanelGroupRenderer.RENDERER);
    
    addRenderer("javax.faces.SelectBoolean", "javax.faces.Checkbox",
                HtmlBooleanCheckboxRenderer.RENDERER);
    
    addRenderer("javax.faces.SelectMany", "javax.faces.Checkbox",
                HtmlSelectManyCheckboxRenderer.RENDERER);
    
    addRenderer("javax.faces.SelectMany", "javax.faces.Listbox",
                HtmlSelectManyListboxRenderer.RENDERER);
    
    addRenderer("javax.faces.SelectMany", "javax.faces.Menu",
                HtmlSelectManyMenuRenderer.RENDERER);
    
    addRenderer("javax.faces.SelectOne", "javax.faces.Listbox",
                HtmlSelectOneListboxRenderer.RENDERER);
    
    addRenderer("javax.faces.SelectOne", "javax.faces.Menu",
                HtmlSelectOneMenuRenderer.RENDERER);
    
    addRenderer("javax.faces.SelectOne", "javax.faces.Radio",
                HtmlSelectOneRadioRenderer.RENDERER);
  }
  
  public void addRenderer(String family,
                          String rendererType,
                          Renderer renderer)
  {
    if (family == null)
      throw new NullPointerException("family argument is null");

    if (rendererType == null)
      throw new NullPointerException("rendererType argument is null");

    if (renderer == null)
      throw new NullPointerException("renderer argument is null");
    
    _rendererMap.put(new Key(family, rendererType), renderer);
  }
  
  public Renderer getRenderer(String family,
                              String rendererType)
  {
    if (family == null)
      throw new NullPointerException("family argument is null");

    if (rendererType == null)
      throw new NullPointerException("rendererType argument is null");

    _key.init(family, rendererType);
    
    Renderer renderer = _rendererMap.get(_key);
    
    return renderer;
  }

  public ResponseStateManager getResponseStateManager()
  {
    return _responseStateManager;
  }

  public ResponseWriter createResponseWriter(Writer writer,
                                             String contentTypeList,
                                             String characterEncoding)
  {
    String contentType = null;

    if (contentTypeList != null) {
      if (contentTypeList.indexOf("text/html") > -1) {
        contentType = "text/html";
      }
      else if (contentTypeList.indexOf("application/xhtml+xml") > -1 ||
               contentTypeList.indexOf("application/xml") > -1 ||
               contentTypeList.indexOf("text/xml") > -1) {
        contentType = "application/xhtml+xml";
      }
      else if (contentTypeList.indexOf("*/*") > -1) {
        contentType = "text/html";
      }
      else {
        throw new IllegalArgumentException(L.l(
          "'{0}' does not have a matching ResponseWriter.",
          contentTypeList));
      }
    }

    if (characterEncoding != null) {

        if (Encoding.getWriteEncoding(characterEncoding) == null)
          throw new IllegalArgumentException(L.l("'{0}' is an unknown character encoding for ResponseWriter.",
                                                 characterEncoding));
    }
    
    return new HtmlResponseWriter(writer, contentType, characterEncoding);
  }

  public ResponseStream createResponseStream(OutputStream out)
  {
    return new HtmlResponseStream(out);
  }

  public String toString()
  {
    return "HtmlBasicRenderKit[]";
  }

  static final class Key {
    private String _family;
    private String _type;

    Key()
    {
    }
      
    Key(String family, String type)
    {
      _family = family;
      _type = type;
    }

    public void init(String family, String type)
    {
      _family = family;
      _type = type;
    }

    @Override
    public int hashCode()
    {
      if (_type != null)
        return _family.hashCode() * 65521 + _type.hashCode();
      else
        return _family.hashCode();
    }

    public boolean equals(Object o)
    {
      Key key = (Key) o;

      if (_type != null)
        return _family.equals(key._family) && _type.equals(key._type);
      else
        return _family.equals(key._family) && key._type != null;
    }

    public String toString()
    {
      return "Key[" + _family + ", " + _type + "]";
    }
  }
}
