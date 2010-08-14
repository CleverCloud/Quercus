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

package com.caucho.xml;

import com.caucho.vfs.WriteStream;

import org.w3c.dom.Node;

import java.io.IOException;

/**
 * CauchoNode extends the DOM, providing namespace support and input file
 * support.
 *
 * <p>Application can print the filename and line number where the
 * error occurred.
 */
public interface CauchoNode extends Node {
  // DOM2 compatibility for old DOM
  public String getNamespaceURI();
  public String getPrefix();
  public String getLocalName();

  /**
   * Returns the node's canonical name.
   *
   * <p>e.g. for foo:bar:baz, the prefix name might be '{/caucho/1.0}baz'
   */
  public String getCanonicalName();

  /**
   * Returns the text value of the node
   */
  public String getTextValue();

  /**
   * Returns the source filename of this node.
   */
  public String getFilename();

  /**
   * Returns the source uri of this node.
   */
  public String getBaseURI();

  /**
   * Returns the source line of this node.
   */
  public int getLine();

  /**
   * Returns the source column of this node.
p   */
  public int getColumn();
  /**
   * Sets the location
   */
  public void setLocation(String systemId, String filename,
                          int line, int column);

  /**
   * Prints the node to a stream
   */
  public void print(WriteStream os) throws IOException;

  /**
   * Pretty-prints the node to a stream
   */
  public void printPretty(WriteStream os) throws IOException;

  /**
   * Prints the node as html to a stream
   */
  public void printHtml(WriteStream os) throws IOException;

  /**
   * For testing...
   */
  public boolean checkValid() throws Exception;
}
