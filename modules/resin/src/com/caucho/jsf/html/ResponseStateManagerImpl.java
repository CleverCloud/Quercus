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

import com.caucho.util.Base64;
import com.caucho.util.L10N;

import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.render.*;

public class ResponseStateManagerImpl extends ResponseStateManager
{
  private static final L10N L = new L10N(ResponseStateManagerImpl.class);
  private static final Logger log
    = Logger.getLogger(ResponseStateManagerImpl.class.getName());

  public static final String COMPONENTS_STATE = "com.caucho.jsf.ComponentsState";


  public void writeState(FacesContext context,
                         Object state)
    throws IOException
  {
    if (! Object [].class.isAssignableFrom(state.getClass()))
      throw new IllegalArgumentException();

    Object [] values = (Object []) state;

    if (values.length != 2)
      throw new IllegalArgumentException();

    String value = encode(values [0]);

    ResponseWriter rw = context.getResponseWriter();

    rw.startElement("input", null);

    rw.writeAttribute("type", "hidden", null);
    rw.writeAttribute("name", VIEW_STATE_PARAM, null);
    rw.writeAttribute("value", value, null);

    rw.endElement("input");

    rw.write("\n");

    if (values [1] != null) {
      value = encode(values [1]);
      rw = context.getResponseWriter();

      rw.startElement("input", null);

      rw.writeAttribute("type", "hidden", null);
      rw.writeAttribute("name", COMPONENTS_STATE, null);
      rw.writeAttribute("value", value, null);

      rw.endElement("input");

      rw.write("\n");
    }
  }

  @Deprecated
  public void writeState(FacesContext context,
                         StateManager.SerializedView state)
    throws IOException
  {
  }

  /**
   * @Since 1.2
   */
  public Object getState(FacesContext context,
                         String viewId)
  {
    ExternalContext extContext = context.getExternalContext();
    
    String data = extContext.getRequestParameterMap().get(VIEW_STATE_PARAM);

    return new Object[]{decode(data), null};
  }


  @Deprecated
  public Object getTreeStructureToRestore(FacesContext context,
                                          String viewId)
  {
    ExternalContext extContext = context.getExternalContext();

    String data = extContext.getRequestParameterMap().get(VIEW_STATE_PARAM);

    return decode(data);
  }

  @Deprecated
  public Object getComponentStateToRestore(FacesContext context)
  {
    ExternalContext extContext = context.getExternalContext();

    String data = extContext.getRequestParameterMap().get(COMPONENTS_STATE);

    if (data == null)
      return null;

    return decode(data);
  }

  /**
   * Since 1.2
   */
  public boolean isPostback(FacesContext context)
  {
    ExternalContext extContext = context.getExternalContext();
    
    return extContext.getRequestParameterMap().containsKey(VIEW_STATE_PARAM);
  }

  private String encode(Object obj) throws IOException {
    if (byte [].class.isAssignableFrom(obj.getClass())) {
      
      return Base64.encodeFromByteArray((byte []) obj);
    }
    else if (obj instanceof String) {

      return "!"+obj.toString();
    } else if (obj instanceof Serializable || obj instanceof Externalizable) {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      ObjectOutputStream oos = new ObjectOutputStream(buffer);

      oos.writeObject(obj);
      
      oos.flush();
      oos.close();

      buffer.flush();
      buffer.close();

      return "~" + Base64.encodeFromByteArray(buffer.toByteArray());
    }

    else {
      throw new IllegalArgumentException();
    }
  }

  private Object decode(String data)
  {
    if (data.charAt(0) == '!') {
      return data.substring(1);
    }
    else if (data.charAt(0) == '~') {
      try {
        byte [] bytes = Base64.decodeToByteArray(data.substring(1));

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
          bytes));
        Object state = ois.readObject();
        ois.close();

        return state;
      }
      catch (Exception e) {
        log.log(Level.SEVERE, e.toString(), e);
        throw new RuntimeException(e);
      }
    }
    else {
      return Base64.decodeToByteArray(data);
    }

  }
}
