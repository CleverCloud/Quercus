/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class PreFormattedText extends FormattedText {
  public PreFormattedText(Document document)
  {
    super(document);
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("pre");

    super.writeHtml(out);

    out.writeEndElement(); // pre
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    super.writeLaTeX(new PrintWriter(new PreFormatFilterWriter(out)));
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    super.writeLaTeXEnclosed(new PrintWriter(new PreFormatFilterWriter(out)));
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }

  private static class PreFormatFilterWriter extends FilterWriter {
    public PreFormatFilterWriter(Writer out)
    {
      super(out);
    }

    public void write(char[] cbuf, int off, int len)
      throws IOException
    {
      for (int i = off; i < len; i++)
        filterChar(cbuf[i]);
    }

    public void write(int c)
      throws IOException
    {
      filterChar(c);
    }

    public void write(String str, int off, int len)
      throws IOException
    {
      for (int i = off; i < len; i++)
        filterChar(str.charAt(i));
    }

    private void filterChar(int ch)
      throws IOException
    {
      switch (ch) {
        case ' ':
        case '\t':
          super.write('\\');
          super.write(' ');
          break;
        case '\n':
          super.write("\\hspace*{\\fill} \\\\");
          break;
        default:
          super.write(ch);
          break;
      }
    }
  }
}
