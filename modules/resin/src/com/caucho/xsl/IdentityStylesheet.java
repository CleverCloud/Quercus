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

import com.caucho.xml.QElement;
import com.caucho.xml.XMLWriter;
//import com.caucho.xml.saaj.SOAPElementImpl;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * A compiled XSL stylesheet.  Stylesheets use 'transform' to transform
 * an XML tree to an XML Document.
 *
 * <p>The resulting document can be printed, or it can be added to another
 * XML tree.
 */
public class IdentityStylesheet extends StylesheetImpl {
  /**
   * Transforms the XML node to a new XML document based on this stylesheet.
   *
   * <p>Since Documents are DocumentFragments, calling functions can insert
   * the contents using appendChild.
   *
   * @param xml source xml to convert
   * @param out source xml to convert
   * @return the converted document
   */
  public void transform(Node xml,
                        XMLWriter writer,
                        TransformerImpl transformer)
    throws SAXException, IOException, TransformerException
  {
    XslWriter out = new XslWriter(null, this, transformer);
    out.init(writer);
    
    applyNode(out, xml);

    out.close();
  }
  
  protected void applyNode(XslWriter out, Node node)
    throws SAXException, IOException, TransformerException
  {
    if (node == null)
      return;

    switch (node.getNodeType()) {
    case Node.DOCUMENT_NODE:
    case Node.DOCUMENT_FRAGMENT_NODE:
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        applyNode(out, child);
      }
      break;
      
    case Node.ELEMENT_NODE:
      out.pushCopy(node);

      if (node instanceof QElement) {
        for (Node child = ((QElement) node).getFirstAttribute();
             child != null;
             child = child.getNextSibling()) {
          applyNode(out, child);
        }
      }
      /*
      else if (node instanceof SOAPElementImpl) {
        for (Node child = ((SOAPElementImpl) node).getFirstAttribute();
             child != null;
             child = child.getNextSibling()) {
          applyNode(out, child);
        }
      }
      */
      else {
        NamedNodeMap attributes = ((Element) node).getAttributes();

        for (int i = 0; i < attributes.getLength(); i++) {
          Node child = attributes.item(i);
          applyNode(out, child);
        }
      }

      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        applyNode(out, child);
      }
      out.popCopy(node);
      break;

    case Node.TEXT_NODE:
    case Node.CDATA_SECTION_NODE:
      String value = node.getNodeValue();
      out.print(value);
      return;
      
    case Node.ATTRIBUTE_NODE:
    case Node.ENTITY_REFERENCE_NODE:
      out.pushCopy(node);
      out.popCopy(node);
      break;
    }
  }

  public OutputFormat getOutputFormat()
  {
    return new OutputFormat();
  }
}
    
