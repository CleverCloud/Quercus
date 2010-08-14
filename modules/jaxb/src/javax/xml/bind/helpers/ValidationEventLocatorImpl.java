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
*
*   Free Software Foundation, Inc.
*   59 Temple Place, Suite 330
*   Boston, MA 02111-1307  USA
*
* @author Scott Ferguson
*/

package javax.xml.bind.helpers;
import org.w3c.dom.Node;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.bind.ValidationEventLocator;
import java.net.URL;

public class ValidationEventLocatorImpl implements ValidationEventLocator {

  private URL _url;
  private LocatorImpl _locator;
  private Node _node;
  private Object _object;
  private int _offset = -1;
  private SAXParseException _exception;

  public ValidationEventLocatorImpl()
  {
    initLocator();
  }

  public ValidationEventLocatorImpl(Locator locator)
  {
    _locator = new LocatorImpl(locator);
  }

  public ValidationEventLocatorImpl(Node node)
  {
    _node = node;
    initLocator();
  }

  public ValidationEventLocatorImpl(Object object)
  {
    _object = object;
    initLocator();
  }

  public ValidationEventLocatorImpl(SAXParseException e)
  {
    _exception = e;
    _locator = new LocatorImpl();
    _locator.setColumnNumber(e.getColumnNumber());
    _locator.setLineNumber(e.getLineNumber());
  }

  private void initLocator()
  {
    _locator = new LocatorImpl();
    _locator.setColumnNumber(-1);
    _locator.setLineNumber(-1);
  }

  public int getColumnNumber()
  {
    return _locator.getColumnNumber();
  }

  public int getLineNumber()
  {
    return _locator.getLineNumber();
  }

  public Node getNode()
  {
    return _node;
  }

  public Object getObject()
  {
    return _object;
  }

  public int getOffset()
  {
    return _offset;
  }

  public URL getURL()
  {
    return _url;
  }

  public void setColumnNumber(int columnNumber)
  {
    _locator.setColumnNumber(columnNumber);
  }

  public void setLineNumber(int lineNumber)
  {
    _locator.setLineNumber(lineNumber);
  }

  public void setNode(Node node)
  {
    _node = node;
  }

  public void setObject(Object object)
  {
    _object = object;
  }

  public void setOffset(int offset)
  {
    _offset = offset;
  }

  public void setURL(URL url)
  {
    _url = url;
  }
}

