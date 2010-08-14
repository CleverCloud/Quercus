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
 *
 * $Id: BodyContent.java,v 1.2 2004/09/29 00:12:48 cvs Exp $
 */

package javax.servlet.jsp.tagext;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * BodyContent subclasses JspWriter to accumulate the contents of a
 * BodyTag.  The JSP engine is responsible for creating BodyContent objects;
 * tags just use them.  A BodyTag will generally grab the contents in
 * its doAfterBody:
 *
 * <code><pre>
 * void doAfterBody() throws JspException
 * {
 *   BodyContent body = getBodyContent();
 *   JspWriter out = body.getEnclosingWriter();
 *   try {
 *     body.writeOut(out);
 *   } catch (IOException e) {
 *     throw new JspException(String.valueOf(e));
 *   }
 * }
 * </pre></code>
 */
abstract public class BodyContent extends JspWriter {
  private JspWriter prevOut;

  /**
   * Creates a new BodyContent with prevOut as its enclosing writer.
   *
   * @param prevOut the enclosing writer.
   */
  protected BodyContent(JspWriter prevOut)
  {
    super(UNBOUNDED_BUFFER, false);

    this.prevOut = prevOut;
  }

  /**
   * Returns the encloding writer.  For top-level tags, this will be the
   * JSP page's out.  For child tags, this will be the parent's bodyContent.
   */
  public JspWriter getEnclosingWriter()
  {
    return prevOut;
  }

  /**
   * Flush does nothing for a bodyContent.
   */
  public void flush() throws IOException
  {
  }

  /**
   * Clears the contents of a body tag.
   */
  public void clearBody()
  {
  }
  
  /**
   * Returns a Reader for accessing the contents of a body tag.
   */
  abstract public Reader getReader();
  /**
   * Returns a String representing the contents of a body tag.
   */
  abstract public String getString();
  /**
   * Writes the contents to the writer.
   *
   * @param out the destination writer .
   */
  abstract public void writeOut(Writer out) throws IOException;
}





