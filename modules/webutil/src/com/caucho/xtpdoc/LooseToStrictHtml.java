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

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.xml.LooseHtml;
import com.caucho.xml.QElement;
import com.caucho.xml.QName;
import com.caucho.xml.XmlPrinter;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;

public class LooseToStrictHtml {
  private static void renameSections(org.w3c.dom.Node node, int level)
  {
    org.w3c.dom.NodeList nodeList = node.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++) {
      int thisLevel = level;

      org.w3c.dom.Node child = nodeList.item(i);

      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        org.w3c.dom.Element subElement = (org.w3c.dom.Element) child;

        if (subElement.getTagName().equals("section")) {
          ((QElement) subElement).setName(new QName("s" + level));

          thisLevel = level + 1;
        } 
        else if (subElement.getTagName().equals("defun")) {
          ((QElement) subElement).setName(new QName("s" + level));
          subElement.setAttribute("type", "defun");

          thisLevel = level + 1;
        }
        else if (subElement.getTagName().equals("faq")) {
          ((QElement) subElement).setName(new QName("s" + level));
          subElement.setAttribute("type", "faq");

          thisLevel = level + 1;
        }
      } 

      renameSections(child, thisLevel);
    }
  }

  private 
  static org.w3c.dom.Element getXTPDocumentElement(org.w3c.dom.Node node)
  {
    org.w3c.dom.NodeList nodeList = node.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++) {
      org.w3c.dom.Node child = nodeList.item(i);

      if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
        org.w3c.dom.Element subElement = (org.w3c.dom.Element) child;

        if (subElement.getTagName().equals("document"))
          return subElement;
      } 

      org.w3c.dom.Element subdocument = getXTPDocumentElement(child);
      
      if (subdocument != null)
        return subdocument;
    }

    return null;
  }

  public static org.w3c.dom.Element looseToStrictHtml(Path path)
    throws IOException,SAXException
  {
    if (! path.exists())
      throw new IOException(path + " does not exist");

    org.w3c.dom.Document document = new LooseHtml().parseDocument(path);

    renameSections(document.getDocumentElement(), 1);

    return getXTPDocumentElement(document.getDocumentElement());
  }

  public static void main(String []args)
    throws Exception
  {
    if (args.length == 0) {
      System.err.println("usage: " + LooseToStrictHtml.class.getName() + 
                         " <xtp file>");
      System.exit(1);
    }

    Path path = Vfs.lookup(args[0]);

    org.w3c.dom.Element xtpDocument = looseToStrictHtml(path);
    
    OutputStream fileOut = path.openWrite();

    XmlPrinter printer = new XmlPrinter(fileOut);

    printer.printXml(xtpDocument);

    fileOut.close();
  }
}
