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

import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.render.*;

public class HtmlResponseWriter extends ResponseWriter
{
  private String _contentType;
  private String _encoding;
  
  private Writer _out;
  private boolean _inElement;

  HtmlResponseWriter(Writer out, String contentType, String encoding)
  {
    _out = out;
    _contentType = contentType;
    _encoding = encoding;
  }

  public void write(char []buffer, int offset, int length)
    throws IOException
  {
    if (_inElement)
      closeElement();
    
    _out.write(buffer, offset, length);
  }

  public void write(char ch)
    throws IOException
  {
    if (_inElement)
      closeElement();
    
    _out.write(ch);
  }

  public void write(String v)
    throws IOException
  {
    if (_inElement)
      closeElement();

    _out.write(v);
  }
  
  public String getContentType()
  {
    return _contentType;
  }

  public String getCharacterEncoding()
  {
    return _encoding;
  }

  public void flush()
    throws IOException
  {
    _out.flush();
  }

  public void startDocument()
    throws IOException
  {
  }

  public void endDocument()
    throws IOException
  {
  }

  public void startElement(String name, UIComponent component)
    throws IOException
  {
    if (_inElement)
      closeElement();
    
    _out.write("<");
    _out.write(name);
    _inElement = true;
  }

  public void endElement(String name)
    throws IOException
  {
    if (_inElement)
      closeElement();
    
    _out.write("</");
    _out.write(name);
    _out.write(">");
  }

  public void writeAttribute(String name,
                             Object value,
                             String property)
    throws IOException
  {
    /*
    if (name.equals(value)) {
      _out.write(' ');
      _out.write(name);
    }
    else {
    */
      _out.write(' ');
      _out.write(name);
      _out.write("=\"");
      _out.write(String.valueOf(value));
      _out.write("\"");
      //}
  }

  public void writeURIAttribute(String name,
                                Object value,
                                String property)
    throws IOException
  {
    writeAttribute(name, value, property);
  }

  public void writeComment(Object comment)
    throws IOException
  {
    if (_inElement)
      closeElement();
    
    _out.write("<!--");
    _out.write(String.valueOf(comment));
    _out.write("-->");
  }

  public void writeText(Object text,
                        String property)
    throws IOException
  {
    if (_inElement)
      closeElement();
    
    _out.write(String.valueOf(text));
  }

  public void writeText(char []text, int offset, int length)
    throws IOException
  {
    if (_inElement)
      closeElement();
    
    _out.write(text, offset, length);
  }

  public ResponseWriter cloneWithWriter(Writer out)
  {
    return new HtmlResponseWriter(out, _contentType, _encoding);
  }

  private void closeElement()
    throws IOException
  {
    _out.write(">");
    _inElement = false;
  }

  public void close()
    throws IOException
  {
    // jsf/2006
    flush();
    _out.close();
  }

  public String toString()
  {
    return "HtmlResponseWriter[]";
  }
}
