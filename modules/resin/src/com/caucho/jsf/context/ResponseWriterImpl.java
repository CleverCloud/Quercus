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

package com.caucho.jsf.context;

import java.io.*;

import javax.faces.component.*;
import javax.faces.context.*;

import javax.servlet.http.*;

public class ResponseWriterImpl extends ResponseWriter
{
  private HttpServletResponse _response;
  private Writer _out;
  private boolean _inElement;

  ResponseWriterImpl(HttpServletResponse response, Writer out)
  {
    _response = response;
    _out = out;
  }

  public void write(char []buffer, int offset, int length)
    throws IOException
  {
    _out.write(buffer, offset, length);
  }

  public void write(char ch)
    throws IOException
  {
    if (_inElement)
      closeElement();
    
    _out.write(ch);
  }
  
  public String getContentType()
  {
    return _response.getContentType();
  }

  public String getCharacterEncoding()
  {
    return _response.getCharacterEncoding();
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
    _out.write(' ');
    _out.write(name);
    _out.write("=\"");
    _out.write(String.valueOf(value));
    _out.write("\"");
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
    return new ResponseWriterImpl(_response, out);
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
  }
}
