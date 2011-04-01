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

package com.caucho.relaxng;

import com.caucho.relaxng.program.EmptyItem;
import com.caucho.relaxng.program.Item;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.xml.QName;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JARV verifier implementation
 */
public class VerifierHandlerImpl extends DefaultHandler
  implements VerifierHandler
{
  private static final L10N L = new L10N(VerifierHandlerImpl.class);
  protected static final Logger log
    = Logger.getLogger(VerifierHandlerImpl.class.getName());

  // very verbose logging
  private static final boolean _isDebug = false;

  private SchemaImpl _schema;
  private VerifierImpl _verifier;
  private boolean _hasProgram;

  private boolean _isValid = true;

  private LruCache<Object,Item> _programCache;

  private QName _name;
  private ArrayList<QName> _nameStack = new ArrayList<QName>();
  private String _eltLocation;
  private ArrayList<String> _eltLocationStack = new ArrayList<String>();

  private Item _item;
  private ArrayList<Item> _itemStack = new ArrayList<Item>();

  private Locator _locator;
  
  private boolean _isLogFinest;

  private CharBuffer _text = new CharBuffer();
  private boolean _hasText;
  
  private StartKey _startKey = new StartKey();
  private EndElementKey _endElementKey = new EndElementKey();

  /**
   * Creates the Verifier Handler.
   */
  public VerifierHandlerImpl(SchemaImpl schema, VerifierImpl verifier)
  {
    _schema = schema;
    _programCache = _schema.getProgramCache();
    _verifier = verifier;
  }

  /**
   * Creates the Verifier Handler.
   */
  public VerifierHandlerImpl(SchemaImpl schema)
  {
    this(schema, (VerifierImpl) schema.newVerifier());
  }

  /**
   * Sets the locator.
   */
  public void setDocumentLocator(Locator locator)
  {
    _locator = locator;
  }
  
  /**
   * Sets the error handler
   */
  public void setErrorHandler(ErrorHandler errorHandler)
    throws SAXException
  {
    _verifier.setErrorHandler(errorHandler);
  }

  private String getFileName()
  {
    if (_locator != null)
      return _locator.getSystemId();
    else
      return null;
  }

  private int getLine()
  {
    if (_locator != null)
      return _locator.getLineNumber();
    else
      return -1;
  }

  /**
   * Called when the document starts.
   */
  public void startDocument()
    throws SAXException
  {
    try {
      _nameStack.clear();
      _itemStack.clear();
      _eltLocationStack.clear();

      _name = new QName("#top", "");
      _item = _schema.getStartItem();

      _itemStack.add(_item);

      _eltLocation = getLocation();

      _isLogFinest = _isDebug && log.isLoggable(Level.FINEST);
      _hasText = false;
      _text.clear();
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * Called when an element starts.
   */
  public void startElement(String uri, String localName,
                           String qName, Attributes attrs)
    throws SAXException
  {
    if (! _isValid)
      return;

    if (_hasText)
      sendText();
    
    if (_isLogFinest)
      log.finest("element start: " + qName);

    try {
      QName parent = _name;
      _nameStack.add(parent);

      String parentLocation = _eltLocation;
      _eltLocationStack.add(parentLocation);
      
      QName name = new QName(qName, uri);
      _name = name;
      _eltLocation = getLocation();

      Item newItem = getStartElement(_item, name);

      if (newItem == null) {
        Item parentItem = _itemStack.get(_itemStack.size() - 1);

        if (parent.getName().equals("#top"))
          throw new RelaxException(L.l("<{0}> is an unexpected top-level tag.{1}",
                                       errorNodeName(name, _item, parentItem),
                                       errorMessageDetail(_item, parentItem, null, name)));
        else
          throw new RelaxException(L.l("<{0}> is an unexpected tag (parent <{1}> starts at {2}).{3}",
                                       errorNodeName(name, _item, parentItem),
                                       parent.getName(), parentLocation,
                                       errorMessageDetail(_item, parentItem, parent.getName(), name)));
      }

      _item = newItem;
      _itemStack.add(newItem);

      Item parentItem = newItem;

      int len = attrs.getLength();
      for (int i = 0; i < len; i++) {
        String attrUri = attrs.getURI(i);
        String attrQName = attrs.getQName(i);
        String value = attrs.getValue(i);
        
        if (_isLogFinest)
          log.finest("attribute: " + attrQName + "=\"" + value + "\"");
        
        name = new QName(attrQName, attrUri);

        if (attrQName.startsWith("xml:")) {
        }
        else if (! _item.allowAttribute(name, value)) {
          throw new RelaxException(L.l("{0}=\"{1}\" is an unexpected attribute in <{2}>.{3}",
                                       attrQName, value, qName,
                                       attributeMessageDetail(_item,
                                                              parentItem,
                                                              qName, null)));
        }
        else
          _item = _item.setAttribute(name, value);

        if (_item == null)
          _item = EmptyItem.create();
      }

      newItem = _item.attributeEnd();
      if (newItem == null) {
        throw new RelaxException(L.l("<{0}> expects more attributes.{1}",
                                     qName, 
                                     attributeMessageDetail(_item,
                                                            parentItem,
                                                            qName, null)));
      }
      
      _item = newItem;
    } catch (Exception e) {
      error(e);
    }
  }

  private Item getStartElement(Item item, QName name)
    throws RelaxException
  {
    _startKey.init(item, name);

    Item newItem = null;//_programCache.get(_startKey);

    if (newItem != null) {
      return newItem;
    }
    
    newItem = _item.startElement(name);

    /*
    if (newItem != null)
      _programCache.put(new StartKey(item, name), newItem);
    */

    return newItem;
  }
  
  public void characters(char ch[], int start, int length)
    throws SAXException
  {
    _hasText = true;
    _text.append(ch, start, length);
  }

  public void sendText()
    throws SAXException
  {
    if (! _hasText)
      return;

    _hasText = false;

    try {
      Item newItem = _item.text(_text);
      
      if (newItem == null) {
        String string = _text.toString();

        Item parentItem = _itemStack.get(_itemStack.size() - 1);

        throw new RelaxException(L.l("The following text is not allowed in this context.\n{0}\n{1}", string,
                                     errorMessageDetail(_item, parentItem,
                                                        _name.getName(), null)));
      }

      _text.clear();
      _item = newItem;                  
    } catch (Exception e) {
      _text.clear();
      error(e);
    }
  }

  /**
   * Called when an element ends.
   */
  public void endElement(String uri, String localName, String qName)
    throws SAXException
  {
    if (_hasText)
      sendText();
    
    if (! _isValid)
      return;

    if (_isLogFinest)
      log.finest("element end: " + qName);

    QName name = _name;
    QName parent = _nameStack.remove(_nameStack.size() - 1);
    _name = parent;

    Item parentItem = _itemStack.remove(_itemStack.size() - 1);

    String eltOpen = _eltLocation;
    _eltLocation = _eltLocationStack.remove(_eltLocationStack.size() - 1);
    
    try {
      Item nextItem = getEndElement(_item);
      
      if (nextItem == null)
        throw new RelaxException(L.l("<{0}> closed while expecting more elements (open at {1}).{2}",
                                     qName, eltOpen,
                                     requiredMessageDetail(_item, parentItem,
                                                        qName, null)));
      
      _item = nextItem;
    } catch (Exception e) {
      error(e);
    }
  }

  private Item getEndElement(Item item)
    throws RelaxException
  {
    _endElementKey.init(item);

    Item newItem = null;//_programCache.get(_endElementKey);

    if (newItem != null) {
      return newItem;
    }
    
    newItem = _item.endElement();

    /*
    if (newItem != null)
      _programCache.put(new EndElementKey(item), newItem);
    */

    return newItem;
  }

  /**
   * Called for errors.
   */
  private void error(SAXException e)
    throws SAXException
  {
    _isValid = false;

    _verifier.error(new SAXParseException(e.getMessage(), _locator));
  }

  /**
   * Called for errors.
   */
  private void error(Exception e)
    throws SAXException
  {
    if (e instanceof RuntimeException)
      throw (RuntimeException) e;
    else if (e instanceof SAXException)
      error((SAXException) e);
    else
      error(new SAXException(e.getMessage(), e));
  }

  /**
   * Returns a string containing the allowed values.
   */
  private String errorNodeName(QName name, Item item, Item parentItem)
  {
    Item currentItem = item;
    
    if (currentItem == null)
      currentItem = parentItem;

    if (currentItem == null)
      return name.toString();

    HashSet<QName> values = new LinkedHashSet<QName>();
    currentItem.firstSet(values);

    for (QName value : values) {
      if (! name.getLocalName().equals(value.getLocalName())) {
      }
      else if (name.getPrefix() == null || name.getPrefix().equals("")) {
        return name.getName() + " xmlns=\"" + name.getNamespaceURI() + "\"";
      }
      else {
        return name.getName() + " xmlns:" + name.getPrefix() + "=\"" + name.getNamespaceURI() + "\"";
      }
    }

    return name.getName();
  }

  /**
   * Returns a string containing the allowed values.
   */
  private String errorMessageDetail(Item item, Item parentItem,
                                    String parentName, QName qName)
  {
    Item currentItem = item;
    
    if (currentItem == null)
      currentItem = parentItem;

    HashSet<QName> values = new LinkedHashSet<QName>();
    currentItem.firstSet(values);

    String expected = null;
    if (values.size() <= 5)
      expected = namesToString(values, parentName, qName,
                               currentItem.allowEmpty());
    
    return (getLineContext(getFileName(), getLine())
            + syntaxMessage(item, parentItem, parentName, qName, expected));
  }

  /**
   * Returns a string containing the allowed values.
   */
  private String requiredMessageDetail(Item item, Item parentItem,
                                       String parentName, QName qName)
  {
    Item currentItem = item;

    if (currentItem == null)
      currentItem = parentItem;

    HashSet<QName> values = new LinkedHashSet<QName>();
    currentItem.requiredFirstSet(values);
      
    String expected = null;
    
    if (values.size() <= 5) {
      expected = namesToString(values, parentName, qName,
                               currentItem.allowEmpty());
    }
    
    return (getLineContext(getFileName(), getLine())
            + syntaxMessage(item, parentItem, parentName, qName, expected));
  }

  /**
   * Returns a string containing the allowed values.
   */
  private String attributeMessageDetail(Item item, Item parentItem,
                                        String parentName, QName qName)
  {
    Item currentItem = item;

    if (currentItem == null)
      currentItem = parentItem;
    
    String allowed = allowedAttributes(currentItem, qName);
    
    return (getLineContext(getFileName(), getLine())
            + syntaxMessage(item, parentItem, parentName, qName, allowed));
  }

  /**
   * Returns a string containing the allowed values.
   */
  private String syntaxMessage(Item item, Item parentItem,
                               String parentName, QName qName,
                               String expected)
  {
    String syntaxPrefix;

    if (parentName == null || parentName.equals("#top"))
      syntaxPrefix = "Syntax: ";
    else
      syntaxPrefix = "<" + parentName + "> syntax: ";
    
    String msg = "";

    Item topParent = null;
    for (Item parent = item;
         parent != null;
         parent = null) { // parent.getParent()) {
      if (qName != null && parent.allowsElement(qName)) {
        msg = "\n Check for duplicate and out-of-order tags.";

        if (expected != null)
          msg += expected + "\n";

        msg += "\n";

        String prefix = "Syntax: ";
        if (parent == parentItem)
          prefix = syntaxPrefix;

        msg += prefix + parent.toSyntaxDescription(prefix.length());
        break;
      }
      
      // topParent = parent;
    }

    if (topParent == null || topParent instanceof EmptyItem) {
      topParent = parentItem;

      if (qName != null && topParent.allowsElement(qName)) {
        msg = "\n Check for duplicate and out-of-order tags.";

        if (expected != null)
          msg += expected + "\n";

        msg += "\n";

        String prefix = syntaxPrefix;
        msg += prefix + topParent.toSyntaxDescription(prefix.length());
      }
    }

    if (msg.equals("")) {
      msg = "";

      if (expected != null)
        msg += expected + "\n";
      
      msg += "\n";

      String prefix = syntaxPrefix;
      msg += prefix + topParent.toSyntaxDescription(prefix.length());
    }

    return msg;
  }

  /**
   * Returns a string containing the allowed values.
   */
  private String requiredValues(Item item, String parentName, QName qName)
  {
    if (item == null)
      return "";

    HashSet<QName> values = new LinkedHashSet<QName>();
    item.requiredFirstSet(values);

    return namesToString(values, parentName, qName, item.allowEmpty());
  }

  private String namesToString(HashSet<QName> values,
                               String parentName,
                               QName qName,
                               boolean allowEmpty)
  {
    CharBuffer cb = new CharBuffer();
    if (values.size() > 0) {
      ArrayList<QName> sortedValues = new ArrayList<QName>(values);
      Collections.sort(sortedValues);
      
      for (int i = 0; i < sortedValues.size(); i++) {
        QName name = sortedValues.get(i);

        if (i == 0)
          cb.append("\n\n");
        else if (i == sortedValues.size() - 1)
          cb.append(" or\n");
        else
          cb.append(",\n");

        if (name.getName().equals("#text")) {
          cb.append("text");
        }
        else if (name.getNamespaceURI() == null || qName == null)
          cb.append("<" + name.getName() + ">");
        else if (qName.getNamespaceURI() != name.getNamespaceURI()) {
          if (name.getPrefix() != null)
            cb.append("<" + name.getName() + " xmlns:" + name.getPrefix() + "=\"" + name.getNamespaceURI() + "\">");
          else
            cb.append("<" + name.getName() + " xmlns=\"" + name.getNamespaceURI() + "\">");
        }
        else
          cb.append("<" + name.getName() + ">");
      }

      if (values.size() == 1)
        cb.append(" is expected");
      else
        cb.append(" are expected");

      if (allowEmpty) {
        if (parentName == null || parentName.equals("#top"))
          cb.append(",\nor the document may end.");
        else
          cb.append(",\nor </" + parentName + "> may close.");
      }
      else
        cb.append(".");
    }
    else if (allowEmpty) {
      if (parentName == null || parentName.equals("#top"))
        cb.append("\n\nThe document is expected to end.");
      else
        cb.append("\n\n</" + parentName + "> is expected to close.");
    }

    return cb.toString();
  }      

  /**
   * Returns a string containing the allowed values.
   */
  private String allowedAttributes(Item item, QName qName)
  {
    if (item == null)
      return "";

    HashSet<QName> values = new LinkedHashSet<QName>();
    item.attributeSet(values);

    CharBuffer cb = new CharBuffer();
    if (values.size() > 0) {
      ArrayList<QName> sortedValues = new ArrayList<QName>(values);
      Collections.sort(sortedValues);
      
      for (int i = 0; i < sortedValues.size(); i++) {
        QName name = sortedValues.get(i);

        if (i == 0)
          cb.append("\n\n");
        else if (i == sortedValues.size() - 1)
          cb.append(" or ");
        else
          cb.append(", ");

        String uri = name.getNamespaceURI();
        if (uri == null || uri.equals(""))
          cb.append("'" + name.getName() + "'");
        else if (qName == null || qName.getName().equals(name.getName()))
          cb.append("'" + name.getCanonicalName() + "'");
        else
          cb.append("'" + name.getName() + "'");
      }

      if (values.size() == 1)
        cb.append(" is expected.");
      else
        cb.append(" are expected.");
    }

    return cb.toString();
  }      

  /**
   * Returns the current location.
   */
  private String getLocation()
  {
    if (_locator == null)
      return "";
    else
      return "" + _locator.getLineNumber();
  }
  
  /**
   * Checks if the document was valid.
   * 
   * <p>
   * This method can be only called after this handler receives
   * the <code>endDocument</code> event.
   * 
   * @return
   *                <b>true</b> if the document was valid,
   *                <b>false</b> if not.
   * 
   * @exception IllegalStateException
   *                If this method is called before the endDocument event is dispatched.
   */
  public boolean isValid() throws IllegalStateException
  {
    return _isValid;
  }

  private String getLineContext(String filename, int errorLine)
  {
    if (filename == null || errorLine <= 0)
      return "";
    
    ReadStream is = null;
    try {
      Path path = Vfs.lookup().lookup(filename);

      StringBuilder sb = new StringBuilder("\n\n");

      is = path.openRead();
      int line = 0;
      String text;
      while ((text = is.readLine()) != null) {
        line++;

        if (errorLine - 2 <= line && line <= errorLine + 2) {
          sb.append(line);
          sb.append(": ");
          sb.append(text);
          sb.append("\n");
        }
      }

      return sb.toString();
    } catch (IOException e) {
      log.log(Level.FINEST, e.toString(), e);

      return "";
    } finally {
      if (is != null)
        is.close();
    }
  }

  static class StartKey {
    private Item _item;
    private QName _name;
    
    public StartKey(Item item, QName name)
    {
      _item = item;
      _name = name;
    }
    
    public StartKey()
    {
    }

    public void init(Item item, QName name)
    {
      _item = item;
      _name = name;
    }

    public int hashCode()
    {
      return _name.hashCode() + 137 * System.identityHashCode(_item);
    }

    public boolean equals(Object o)
    {
      if (o == this)
        return true;

      if (o.getClass() != StartKey.class)
        return false;

      StartKey key = (StartKey) o;

      return _name.equals(key._name) && _item == key._item;
    }
  }

  static class EndElementKey {
    private Item _item;
    
    public EndElementKey(Item item)
    {
      _item = item;
    }
    
    public EndElementKey()
    {
    }

    public void init(Item item)
    {
      _item = item;
    }

    public int hashCode()
    {
      return 137 + _item.hashCode();
    }

    public boolean equals(Object o)
    {
      if (o == this)
        return true;

      if (o.getClass() != EndElementKey.class)
        return false;

      EndElementKey key = (EndElementKey) o;

      return _item.equals(key._item);
    }
  }
}
