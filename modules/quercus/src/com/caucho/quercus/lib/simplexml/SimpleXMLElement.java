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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.simplexml;

import com.caucho.quercus.annotation.Hide;
import com.caucho.quercus.annotation.JsonEncode;
import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.annotation.EntrySet;
import com.caucho.quercus.env.*;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.logging.*;

/**
 * SimpleXMLElement object oriented API facade.
 * Also acts as the DOM document.
 */
public class SimpleXMLElement implements Map.Entry<String,Object>
{
  private static final Logger log
    = Logger.getLogger(SimpleXMLElement.class.getName());
  private static final L10N L = new L10N(SimpleXMLElement.class);
  
  protected SimpleXMLElement _parent;
  
  protected String _name;
  
  // mixed content is all combined
  protected StringValue _text;
  
  protected ArrayList<SimpleXMLElement> _children;
  protected ArrayList<SimpleXMLElement> _attributes;
  
  protected String _namespace;
  protected String _prefix;
  
  protected LinkedHashMap<String, String> _namespaceMap;
  
  protected Env _env;
  protected QuercusClass _cls;
  
  protected SimpleXMLElement(Env env,
                             QuercusClass cls)
  {
    _env = env;
    _cls = cls;
  }

  protected SimpleXMLElement(Env env, QuercusClass cls,
                             SimpleXMLElement parent, String name)
  {
    _env = env;
    _cls = cls;

    _parent = parent;
    _name = name;
  }

  protected SimpleXMLElement(Env env,
                             QuercusClass cls,
                             SimpleXMLElement parent,
                             String name,
                             String namespace)
  {
    _env = env;
    _cls = cls;

    _parent = parent;

    int p = name.indexOf(':');

    if (p > 0) {
      _name = name.substring(p + 1);
      _prefix = name.substring(0, p);
    }
    else
      _name = name;

    if ("".equals(_name))
      throw new IllegalArgumentException(L.l("name can't be empty"));

    _namespace = namespace;

    if (namespace != null) {
      if (_prefix == null)
        _prefix = "";

      if (! hasNamespace(_prefix, namespace)) {
        String ns;

        if ("".equals(_prefix))
          ns = "xmlns";
        else
          ns = "xmlns:" + _prefix;

        addNamespaceAttribute(env, ns, namespace);
      }
    }
  }
  
  protected static Value create(Env env,
                                QuercusClass cls,
                                Value data,
                                int options,
                                boolean dataIsUrl,
                                Value namespaceV,
                                boolean isPrefix)
  {
    if (data.length() == 0) {
      env.warning(L.l("xml data must have length greater than 0"));
      return BooleanValue.FALSE;
    }
    
    try {
      String namespace = null;

      if (! namespaceV.isNull())
        namespace = namespaceV.toString();
      
      Node node = parse(env, data, options, dataIsUrl, namespace, isPrefix);
      
      if (node == null) {
        return BooleanValue.FALSE;
      }

      SimpleXMLElement elt
        = buildNode(env, cls, null, node, namespace, isPrefix);
      
      return wrapJava(env, cls, elt);
      
    } catch (IOException e) {
      env.warning(e);
      
      return BooleanValue.FALSE;
    }
    catch (ParserConfigurationException e) {
      env.warning(e);
      
      return BooleanValue.FALSE;
    }
    catch (SAXException e) {
      env.warning(e);
      
      return BooleanValue.FALSE;
    }
  }
  
  protected static Value wrapJava(Env env,
                                  QuercusClass cls,
                                  SimpleXMLElement element)
  {
    if (! "SimpleXMLElement".equals(cls.getName()))
      return new ObjectExtJavaValue(cls, element, cls.getJavaClassDef());
    else
      return new JavaValue(env, element, cls.getJavaClassDef());
  }
  
  protected QuercusClass getQuercusClass()
  {
    return _cls;
  }
  
  protected void setQuercusClass(QuercusClass cls)
  {
    _cls = cls;
  }

  protected void addNamespace(String prefix, String namespace)
  {
    if (prefix == null)
      prefix = "";
    
    if (hasNamespace(prefix, namespace))
      return;

    if (_namespaceMap == null)
      _namespaceMap = new LinkedHashMap<String,String>();
      
    _namespaceMap.put(prefix, namespace);
  }

  protected boolean hasNamespace(String prefix, String namespace)
  {
    String uri = getNamespace(prefix);

    return uri != null && uri.equals(namespace);
  }

  protected String getNamespace(String prefix)
  {
    if (prefix == null)
      prefix = "";

    if (_namespaceMap != null) {
      String uri = _namespaceMap.get(prefix);

      if (uri != null)
        return uri;
    }

    if (_parent != null)
      return _parent.getNamespace(prefix);
    else
      return null;
  }
  
  /**
   * Returns a new instance based on the xml from 'data'.
   * 
   * @param env
   * @param data xml data
   * @param options
   * @param dataIsUrl
   * @param namespaceV
   * @param isPrefix
   */
  public static Value __construct(Env env,
                                  Value data,
                                  @Optional int options,
                                  @Optional boolean dataIsUrl,
                                  @Optional Value namespaceV,
                                  @Optional boolean isPrefix)
  { 
    QuercusClass cls = env.getCallingClass();
    
    if (cls == null)
      cls = env.getClass("SimpleXMLElement");
    
    return create(env, cls,
                  data, options, dataIsUrl, namespaceV, isPrefix);
  }

  protected String getName()
  {
    return _name;
  }

  protected String getNamespace()
  {
    return _namespace != null ? _namespace : "";
  }

  protected SimpleXMLElement getOwner()
  {
    return this;
  }

  protected boolean isElement()
  {
    return true;
  }
  
  protected boolean isNamespaceAttribute()
  {
    return false;
  }

  protected void setText(StringValue text)
  {
    _text = text.createStringBuilder().append(text);
  }

  protected void addText(StringValue text)
  {
    if (_text == null)
      _text = text.createStringBuilder();
    
    _text = _text.append(text);
  }
  
  protected boolean isSameNamespace(String namespace)
  {
    if (namespace == null || namespace.length() == 0)
      return true;
    else if (_namespace != null && _namespace.length() > 0)
      return namespace.equals(_namespace);
    else if (_parent != null)
      return _parent.isSameNamespace(namespace);
    else
      return false;
  }
  
  protected boolean isSamePrefix(String prefix)
  {
    if (prefix == null || prefix.length() == 0)
      return true;

    return prefix.equals(_prefix);
  }

  protected SimpleXMLElement getAttribute(String name)
  {
    if (_attributes == null)
      return null;

    int size = _attributes.size();
    for (int i = 0; i < size; i++) {
      SimpleXMLElement attr = _attributes.get(i);

      if (attr.getName().equals(name))
        return attr;
    }

    return null;
  }

  private SimpleXMLElement getElement(String name)
  {
    if (_children == null)
      return null;

    int size = _children.size();
    for (int i = 0; i < size; i++) {
      SimpleXMLElement elt = _children.get(i);

      if (elt.getName().equals(name))
        return elt;
    }
    
    return null;
  }

  //
  // Map.Entry api for iterator
  //

  @Hide
  public String getKey()
  {
    return _name;
  }

  @Hide
  public Object getValue()
  {
    if (_children == null)
      return _text;
    else
      return wrapJava(_env, _cls, this);
  }

  @Hide
  public Object setValue(Object value)
  {
    return wrapJava(_env, _cls, this);
  }

  /**
   * Adds an attribute to this node.
   */
  public void addAttribute(Env env,
                           String name,
                           StringValue value,
                           @Optional String namespace)
  {
    if (namespace != null && namespace.length() > 0) {
      int colonIndex = name.indexOf(":");
      
      // php/1x42
      if (colonIndex <= 0 || colonIndex >= name.length()) {
        env.warning(
            L.l("Adding attributes with namespaces requires "
                + "attribute name with a prefix"));
        return;
      }
    }
    
    if (_attributes == null)
      _attributes = new ArrayList<SimpleXMLElement>();

    SimpleXMLAttribute attr
      = new SimpleXMLAttribute(env, _cls, this, name, namespace, value);
    
    _attributes.add(attr);
  }

  /**
   * Adds a namespace attribute to this node.
   */
  protected void addNamespaceAttribute(Env env, String name,
                                       String namespace)
  {
    if (namespace == null || "".equals(namespace))
      return;
    
    if (_attributes == null)
      _attributes = new ArrayList<SimpleXMLElement>();

    SimpleXMLAttribute attr
      = new SimpleXMLNamespaceAttribute(env, _cls,
                                        this, name, "",
                                        env.createString(namespace));

    int p = name.indexOf(':');
    if (p > 0) {
      String prefix = name.substring(p + 1);
      addNamespace(prefix, namespace);
    }
    else {
      if (_namespace == null)
        _namespace = namespace;
      
      addNamespace("", namespace);
    }

    for (int i = _attributes.size() - 1; i >= 0; i--) {
      SimpleXMLElement oldAttr = _attributes.get(i);

      if (oldAttr.getName().equals(name)
          && oldAttr.getNamespace().equals(namespace)) {
        _attributes.set(i, attr);
        return;
      }
    }
    
    _attributes.add(attr);
  }

  /**
   * Adds an attribute to this node.
   */
  protected void addAttribute(SimpleXMLElement attr)
  {
    if (_attributes == null)
      _attributes = new ArrayList<SimpleXMLElement>();
    
    _attributes.add(attr);
  }

  /**
   * Adds a child to this node.
   * 
   * @param env
   * @param name of the child node
   * @param value of the text node of the child
   * @param namespace
   * @return
   */
  public Value addChild(Env env,
                        String name,
                        String value,
                        @Optional Value namespaceV)
  {
    String namespace;

    /*
    if (! namespaceV.isNull())
      namespace = namespaceV.toString();
    else
      namespace = _namespace;
    */
    
    namespace = namespaceV.toString();
    
    SimpleXMLElement child
      = new SimpleXMLElement(env, _cls, this, name, namespace);

    child.setText(env.createString(value));

    addChild(child);
    return wrapJava(env, _cls, child);
  }

  private void addChild(SimpleXMLElement child)
  {
    if (_children == null)
      _children = new ArrayList<SimpleXMLElement>();

    _children.add(child);
  }
  
  /**
   * Returns the attributes of this node.
   * 
   * @param env
   * @param namespaceV
   * @param isPrefix
   */
  public Value attributes(Env env,
                          @Optional Value namespaceV,
                          @Optional boolean isPrefix)
  {
    String namespace = null;
    if (! namespaceV.isNull())
      namespace = namespaceV.toString();

    SimpleXMLElement attrList
      = new SimpleXMLAttributeList(env, _cls, this,
                                   "#attrlist", namespace, null);

    if (_attributes != null) {
      for (SimpleXMLElement attr : _attributes) {
        if (attr.isSameNamespace(namespace)
            && ! attr.isNamespaceAttribute())
          attrList.addAttribute(attr);
      }
    }

    return wrapJava(env, _cls, attrList);
  }
  
  /**
   * Returns all the children of this node, including the attributes of
   * this node.
   * 
   * @param env
   * @param namespaceV
   * @param isPrefix
   */
  public Value children(Env env,
                        @Optional Value namespaceV,
                        @Optional boolean isPrefix)
  {
    String namespace = null;
    if (! namespaceV.isNull())
      namespace = namespaceV.toString();
    
    SimpleXMLElement result
      = new SimpleXMLChildren(env, _cls, this, getName());

    if (_attributes != null) {
      for (SimpleXMLElement attr : _attributes) {
        if (attr.isSameNamespace(namespace))
          result.addAttribute(attr);
      }
    }

    if (_children != null) {
      for (SimpleXMLElement child : _children) {
        if (isPrefix) {
          if (child.isSamePrefix(namespace)) {
            result.addChild(child);
          }
        }
        else {
          if (child.isSameNamespace(namespace)) {
            result.addChild(child);
          }
        }
      }
    }

    return wrapJava(env, _cls, result);
  }

  //
  // XML parsing and generation
  //
  
  private static Node parse(Env env,
                            Value data,
                            int options,
                            boolean dataIsUrl,
                            String namespace,
                            boolean isPrefix)
    throws IOException,
           ParserConfigurationException,
           SAXException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();

    Document document = null;

    if (dataIsUrl) {
      Path path = env.lookup(data.toStringValue());

      // PHP throws an Exception instead
      if (path == null) {
        log.log(Level.FINE, L.l("Cannot read file/URL '{0}'", data));
        env.warning(L.l("Cannot read file/URL '{0}'", data));

        return null;
      }

      ReadStream is = path.openRead();

      try {
        document = builder.parse(is);
      } finally {
        is.close();
      }
    }
    else {
      StringReader reader = new java.io.StringReader(data.toString());

      document = builder.parse(new InputSource(reader));
    }

    NodeList childList = document.getChildNodes();

    // php/1x70
    for (int i = 0; i < childList.getLength(); i++) {
      if (childList.item(i).getNodeType() == Node.ELEMENT_NODE)
        return childList.item(i);
    }
   
    return childList.item(0);
  }
  
  private static SimpleXMLElement buildNode(Env env,
                                            QuercusClass cls,
                                            SimpleXMLElement parent,
                                            Node node,
                                            String namespace,
                                            boolean isPrefix)
  {
    if (node.getNodeType() == Node.TEXT_NODE) {
      String value = node.getNodeValue();
      
      if (parent != null) {
        parent.addChild(new SimpleXMLText(env, cls,
                                          env.createString(value)));

        if (! isWhitespace(value))
          parent.addText(env.createString(value));
      }
      
      return parent;
    }
    
    /*
    NamedNodeMap attrMap = node.getAttributes();
    Node namespaceAttr = attrMap.getNamedItem("xmlns");

    if (namespaceAttr != null)
      namespace = namespaceAttr.getNodeValue();
    */
    
    SimpleXMLElement elt = new SimpleXMLElement(env, cls,
                                                parent,
                                                node.getNodeName(),
                                                namespace);

    if (parent != null)
      parent.addChild(elt);

    NamedNodeMap attrs = node.getAttributes();
    
    if (attrs != null) {
      int length = attrs.getLength();
      
      for (int i = 0; i < length; i++) {
        Attr attr = (Attr)attrs.item(i);

        if (attr.getName().startsWith("xmlns")) {
          elt.addNamespaceAttribute(env, attr.getName(), attr.getValue());
        }
        else {
              elt.addAttribute(env,
                               attr.getName(),
                               env.createString(attr.getValue()),
                               namespace);
        }
      }
    }

    for (Node child = node.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      buildNode(env, cls, elt, child, namespace, isPrefix);
    }
    
    return elt;
  }
  
  /**
   * Converts node tree to a valid xml string.
   * 
   * @return xml string
   */
  @ReturnNullAsFalse
  public StringValue asXML(Env env)
  {
    if (_parent == null) {
      StringValue sb = env.createBinaryBuilder();

      sb.append("<?xml version=\"1.0\"?>\n");
      toXMLImpl(sb);
      sb.append("\n");
      
      return sb;
    }
    else
      return toXML(env);
  }
  
  public StringValue toXML(Env env)
  {
    StringValue sb = env.createBinaryBuilder();
    
    toXMLImpl(sb);
    
    return sb;
  }
  
  protected void toXMLImpl(StringValue sb)
  {
    sb.append("<");

    boolean hasPrefix = false;
    
    if (_prefix != null && ! "".equals(_prefix)
        && getNamespace(_prefix) != null)
      hasPrefix = true;

    if (hasPrefix) {
      sb.append(_prefix);
      sb.append(":");
    }

    sb.append(_name);

    /*
    if (_namespaceMap != null) {
      for (Map.Entry<String,String> entry : _namespaceMap.entrySet()) {
        if (! "".equals(entry.getKey())) {
          sb.append(" xmlns:");
          sb.append(entry.getKey());
        }
        else
          sb.append(" xmlns");
        sb.append("=\"");
        sb.append(entry.getValue());
        sb.append("\"");
      }
    }
    */
    
    // add attributes, if any
    if (_attributes != null) {
      int size = _attributes.size();

      for (int i = 0; i < size; i++) {
        SimpleXMLElement attr = _attributes.get(i);

        attr.toXMLImpl(sb);
      }
    }
    
    // add children, if any
    if (_children != null) {
      sb.append(">");
      
      int size = _children.size();

      for (int i = 0; i < size; i++) {
        SimpleXMLElement child = _children.get(i);

        child.toXMLImpl(sb);
      }
    }
    else if (_text == null || _text.length() == 0) {
      sb.append("/>");
      return;
    }
    else {
      sb.append(">");
      
      sb.append(_text);
    }

    // add closing tag
    sb.append("</");

    if (hasPrefix) {
      sb.append(_prefix);
      sb.append(":");
    }
    
    sb.append(_name);
    
    sb.append(">");
  }

  /**
   * Returns the name of the node.
   * 
   * @return name of the node
   */
  @Name("getName")
  public String simplexml_getName()
  {
    return _name;
  }

  /**
   * Alias of getNamespaces().
   */
  public Value getDocNamespaces(Env env, @Optional boolean isRecursive)
  {
    return getNamespaces(env, isRecursive);
  }
  
  /**
   * Returns the namespaces used in this document.
   */
  public Value getNamespaces(Env env, @Optional boolean isRecursive)
  {
    ArrayValue array = new ArrayValueImpl();

    if (isRecursive)
      getNamespacesRec(env, array);
    else
      getNamespaces(env, array);

    return array;
  }
  
  private void getNamespacesRec(Env env, ArrayValue array)
  {
    getNamespaces(env, array);

    if (_children != null) {
      for (SimpleXMLElement child : _children) {
        child.getNamespacesRec(env, array);
      }
    }
  }
  
  private void getNamespaces(Env env, ArrayValue array)
  {
    if (_namespaceMap != null) {
      for (Map.Entry<String,String> entry : _namespaceMap.entrySet()) {
        StringValue name = env.createString(entry.getKey());
        StringValue uri = env.createString(entry.getValue());

        SimpleXMLAttribute attr
          = new SimpleXMLAttribute(env, _cls, this, entry.getKey());
        attr.setText(uri);
      
        array.append(name, env.wrapJava(attr));
      }
    }
  }
  
  /**
   * Runs an XPath expression on this node.
   * 
   * @param env
   * @param expression
   * @return array of results
   * @throws XPathExpressionException
   */
  public Value xpath(Env env, String expression)
  {
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();

      InputSource is = new InputSource(asXML(env).toInputStream());
      NodeList nodes = (NodeList) xpath.evaluate(expression, is,
                                                 XPathConstants.NODESET);

      int nodeLength = nodes.getLength();

      if (nodeLength == 0)
        return NullValue.NULL;

      // There are matching nodes
      ArrayValue result = new ArrayValueImpl();
      for (int i = 0; i < nodeLength; i++) {
        Node node = nodes.item(i);
        
        boolean isPrefix = node.getPrefix() != null;
        
        SimpleXMLElement xml
          = buildNode(env, _cls, null, nodes.item(i),
                      node.getNamespaceURI(), isPrefix);
        
        result.put(wrapJava(env, _cls, xml));
      }

      return result;
    }
    catch (XPathExpressionException e) {
      env.warning(e);
      log.log(Level.FINE, e.getMessage());
      
      return NullValue.NULL;
    }
  }
  
  /**
   * Implementation for getting the indices of this class.
   * i.e. <code>$a->foo[0]</code>
   */
  public Value __get(Env env, Value indexV)
  {
    if (indexV.isString()) {
      String name = indexV.toString();
      
      SimpleXMLElement attr = getAttribute(name);
      
      if (attr == null)
        return NullValue.NULL;
      else
        return wrapJava(env, _cls, attr);
    }
    else if (indexV.isLongConvertible()) {
      int i = indexV.toInt();

      if (i == 0)
        return wrapJava(env, _cls, getOwner());
      else if (_parent == null)
        return NullValue.NULL;

      ArrayList<SimpleXMLElement> children = _parent._children;
      
      if (children != null) {
        int size = children.size();

        for (int j = 0; j < size; j++) {
          SimpleXMLElement child = children.get(j);

          if (child.getName().equals(getName()) && i-- == 0)
            return wrapJava(env, _cls, child);
        }
      }

      return NullValue.NULL;
    }
    else
      return NullValue.NULL;
  }
  
  /**
   * Implementation for setting the indices of this class.
   * i.e. <code>$a->foo[0] = "hello"</code>
   */
  public void __set(String name, StringValue value)
  {
    addAttribute(_env, name, value, null);
  }
  
  /**
   * Implementation for getting the fields of this class.
   * i.e. <code>$a->foo</code>
   */
  public Value __getField(String name)
  {
    SimpleXMLElement elt = getElement(name);

    if (elt != null)
      return wrapJava(_env, _cls,
                      new SelectedXMLElement(_env, _cls, elt));
    else
      return NullValue.NULL;
  }
  
  /**
   * Implementation for setting the fields of this class.
   * i.e. <code>$a->foo = "hello"</code>
   */
  public void __setField(String name, Value value)
  {
    SimpleXMLElement child = getElement(name);
    
    if (child == null) {
      child = new SimpleXMLElement(_env, _cls, this, name);
      child.setText(value.toStringValue());
      addChild(child);
    }
    else {
      child._children = null;
    
      child.setText(value.toStringValue());
    }
  }
  
  /**
   * Required for 'foreach'. When only values are specified in
   * the loop <code>foreach($a as $b)</code>, this method
   * should return an iterator that contains Java objects
   * that will be wrapped in a Value.
   *
   * When a 'foreach' loop with name/value pairs
   * i.e. <code>foreach($a as $b=>$c)</code>
   * invokes this method, it expects an iterator that
   * contains objects that implement Map.Entry.
   */
  public Iterator iterator()
  {
    // php/1x05
   
    if (_children != null)
      return new ElementIterator(_children);
    else
      return null;
  }

  @EntrySet
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    LinkedHashMap<Value,Value> map
      = new LinkedHashMap<Value,Value>();

    if (_attributes != null) {
      ArrayValue array = new ArrayValueImpl();
      
      for (SimpleXMLElement attr : _attributes) {
        StringValue value = attr._text;

        array.put(_env.createString(attr._name), value);
      }

      map.put(_env.createString("@attributes"), array);
    }

    boolean hasElement = false;
    if (_children != null) {
      for (SimpleXMLElement child : _children) {
        if (! child.isElement())
          continue;

        hasElement = true;
        
        StringValue name = _env.createString(child.getName());
        Value oldChild = map.get(name);
        Value childValue;

        if (child._text != null)
          childValue = child._text;
        else
          childValue = wrapJava(_env, _cls, child);

        if (oldChild == null) {
          map.put(name, childValue);
        }
        else if (oldChild.isArray()) {
          ArrayValue array = (ArrayValue) oldChild;

          array.append(childValue);
        }
        else {
          ArrayValue array = new ArrayValueImpl();
          array.append(oldChild);
          array.append(childValue);

          map.put(name, array);
        }
      }
    }
    
    if (! hasElement && _text != null) {
      map.put(LongValue.ZERO, _text);
    }

    return map.entrySet();
  }
    
  /**
   * var_dump() implementation
   */
  public void varDumpImpl(Env env,
                          Value obj,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    String name = "SimpleXMLElement";
    
    if (obj != null)
      name = obj.getClassName();
    
    // php/1x33
    if (_text != null && _children == null && _attributes == null) {
      if (depth > 0) {
        _text.varDump(env, out, depth, valueSet);
        return;
      }
      
      out.println("object(" + name + ") (1) {");
      printDepth(out, 2 * (depth + 1));
      out.println("[0]=>");
      
      printDepth(out, 2 * (depth + 1));
      _text.varDump(env, out, depth, valueSet);
      out.println();
      
      printDepth(out, 2 * depth);
      out.print("}");

      return;
    }
    
    Set<Map.Entry<Value,Value>> entrySet = entrySet();
    out.println("object(" + name + ") (" + entrySet.size() + ") {");

    for (Map.Entry<Value,Value> entry : entrySet) {
      printDepth(out, 2 * (depth + 1));
      out.print("[");
      
      if (entry.getKey().isString())
        out.print("\"" + entry.getKey() + "\"");
      else
        out.print(entry.getKey());
      
      out.println("]=>");

      printDepth(out, 2 * (depth + 1));
      entry.getValue().varDump(env, out, depth + 1, valueSet);
      out.println();
    }
    
    printDepth(out, 2 * depth);
    out.print('}');
  }
  
  @JsonEncode
  public void jsonEncode(Env env, StringValue sb)
  {
    sb.append('{');
    
    jsonEncodeImpl(env, sb, true);
    
    sb.append('}');
  }
  
  protected void jsonEncodeImpl(Env env, StringValue sb, boolean isTop)
  {  
    if (! isTop) {
      sb.append('"');
      sb.append(getName());
      sb.append('"');
      
      sb.append(':');
    }

    if (_attributes == null
        && _children != null
        && _children.size() == 1
        && ! _children.get(0).isElement())
    {
      _children.get(0).jsonEncodeImpl(env, sb, false);
    }
    else {
      int length = 0;
      
      boolean hasChildren = _attributes != null && _children != null;
      
      if (hasChildren)
        sb.append('{');
      
      if (_attributes != null) {
        length++;
        
        sb.append("\"@attributes\"");
        sb.append(':');
        sb.append('{');
        
        for (SimpleXMLElement attribute : _attributes) {
          attribute.jsonEncodeImpl(env, sb, false);
        }
        
        sb.append('}');
      }
      
      if (_children != null) {
        for (SimpleXMLElement child : _children) {
          if (! child.isElement())
            continue;
          
          if (length++ > 0)
            sb.append(',');
          
          child.jsonEncodeImpl(env, sb, false);
        }
      }

      if (hasChildren)
        sb.append('}');
    }
  }

  protected void printDepth(WriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < depth; i++)
      out.print(' ');
  }
  
  public StringValue __toString(Env env)
  {
    if (_text != null)
      return _text;
    else
      return env.getEmptyString();
  }
  
  private static boolean isWhitespace(String text)
  {
    for (int i = text.length() - 1; i >= 0; i--) {
      if (! isWhitespace(text.charAt(i)))
        return false;
    }

    return true;
  }

  private static boolean isWhitespace(int ch)
  {
    return ch <= 0x20 && (ch == 0x20 || ch == 0x9 || ch == 0xa || ch == 0xd);
  }

  class ElementIterator implements Iterator {
    private ArrayList<SimpleXMLElement> _children;
    private int _index;
    private int _size;

    ElementIterator(ArrayList<SimpleXMLElement> children)
    {
      _children = children;
      _size = children.size();
    }

    public boolean hasNext()
    {
      for (; _index < _size; _index++) {
        SimpleXMLElement elt = _children.get(_index);

        if (elt.isElement())
          return true;
      }

      return false;
    }

    public Object next()
    {
      while (_index < _size) {
        SimpleXMLElement elt = _children.get(_index++);

        if (elt.isElement())
          return elt;
      }

      return null;
    }

    public void remove()
    {
    }
  }
}
