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
 */

package com.caucho.config.xml;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.caucho.config.Config;
import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.attribute.Attribute;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.NodeBuilderChildProgram;
import com.caucho.config.type.ConfigType;
import com.caucho.config.type.StringType;
import com.caucho.config.type.TypeFactory;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.inject.Module;
import com.caucho.util.CompileException;
import com.caucho.util.DisplayableException;
import com.caucho.util.L10N;
import com.caucho.util.LineCompileException;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.xml.QAbstractNode;
import com.caucho.xml.QAttributedNode;
import com.caucho.xml.QDocument;
import com.caucho.xml.QElement;
import com.caucho.xml.QName;
import com.caucho.xml.QNode;
import com.caucho.xml.XmlUtil;

/**
 * The ConfigContext contains the state of the current configuration.
 */
@Module
public class XmlConfigContext {
  private final static L10N L = new L10N(XmlConfigContext.class);
  private final static Logger log
    = Logger.getLogger(XmlConfigContext.class.getName());

  private final static QName TEXT = new QName("#text");
  private final static Object NULL = new Object();

  private static ThreadLocal<XmlConfigContext> _currentBuilder
    = new ThreadLocal<XmlConfigContext>();

  private Config _config;

  private InjectionPoint _ij;
  
  private CreationalContextImpl<?> _beanStack;

  private ArrayList<Dependency> _dependList;
  private Document _dependDocument;

  private String _baseUri;

  public XmlConfigContext()
  {
  }

  public XmlConfigContext(XmlConfigContext parent)
  {
    this();
  }

  public XmlConfigContext(Config config)
  {
    this();

    _config = config;
  }

  public static XmlConfigContext create()
  {
    XmlConfigContext env = _currentBuilder.get();

    if (env != null)
      return env;
    else
      return new XmlConfigContext();
  }

  public static XmlConfigContext createForProgram()
  {
    return new XmlConfigContext();
  }

  public static XmlConfigContext getCurrentBuilder()
  {
    return _currentBuilder.get();
  }

  public static XmlConfigContext getCurrent()
  {
    return _currentBuilder.get();
  }

  // s/b private?
  static void setCurrentBuilder(XmlConfigContext builder)
  {
    _currentBuilder.set(builder);
  }

  public InjectionPoint getInjectionPoint()
  {
    return _ij;
  }

  public void setInjectionPoint(InjectionPoint ij)
  {
    _ij = ij;
  }

  /**
   * Returns the file var
   */
  public String getBaseUri()
  {
    return Vfs.decode(_baseUri);
  }

  /**
   * Returns the component value for the dependent scope
   *
   * @param aThis
   * @return
   */
  public Object get(Bean<?> bean)
  {
    return CreationalContextImpl.find(_beanStack, bean);
  }
  
  public Object findByName(String name)
  {
    return CreationalContextImpl.findByName(_beanStack, name);
  }
  
  public CreationalContext<?> setCreationalContext(CreationalContext<?> cxt)
  {
    CreationalContext<?> oldCxt = _beanStack;
    
    _beanStack = (CreationalContextImpl<?>) cxt;
    
    return oldCxt;
  }
  
  public CreationalContext<?> getCreationalContext()
  {
    return _beanStack;
  }

  public Config getConfig()
  {
    return _config;
  }

  /**
   * Returns true if EL expressions are used.
   */
  private boolean isEL()
  {
    // server/26b6
    return _config == null || _config.isEL();
  }

  public boolean isIgnoreEnvironment()
  {
    return _config != null && _config.isIgnoreEnvironment();
  }

  /**
   * External call to configure a bean based on a top-level node.
   * The init() and replaceObject() are not called.
   *
   * @param bean the object to be configured.
   */
  public Object configure(Object bean, Node top)
    throws LineConfigException
  {
    if (bean == null)
      throw new NullPointerException(L.l("unexpected null bean at node '{0}'", top));

    XmlConfigContext oldBuilder = _currentBuilder.get();
    try {
      _currentBuilder.set(this);

      ConfigType<?> type = TypeFactory.getType(bean);

      configureBean(bean, top);

      type.init(bean);

      return type.replaceObject(bean);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, top);
    } finally {
      _currentBuilder.set(oldBuilder);
    }
  }

  /**
   * External call to configure a bean based on a top-level node, calling
   * init() and replaceObject() when done.
   *
   * @param bean the bean to be configured
   * @param top the top-level XML configuration node
   * @return the configured object, or the factory generated object
   */
  public void configureBean(Object bean, Node top)
    throws LineConfigException
  {
    XmlConfigContext oldBuilder = _currentBuilder.get();
    String oldFile = _baseUri;
    ArrayList<Dependency> oldDependList = _dependList;

    try {
      _currentBuilder.set(this);

      if (top instanceof QNode) {
        QNode qNode = (QNode) top;

        _baseUri = qNode.getBaseURI();
      }

      _dependList = getDependencyList(top);

      ConfigType<?> type = TypeFactory.getType(bean);

      configureNode(top, bean, type);
    } finally {
      _currentBuilder.set(oldBuilder);

      _dependList = oldDependList;
      _baseUri = oldFile;
    }
  }

  /**
   * External call to configure a bean's attribute.
   *
   * @param bean the bean to be configured
   * @param attribute the node representing the configured attribute
   * @throws LineConfigException
   */
  public void configureAttribute(Object bean, Node attribute)
    throws LineConfigException
  {
    String attrName = attribute.getNodeName();

    if (attrName.startsWith("xmlns"))
      return;

    String oldFile = _baseUri;
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    XmlConfigContext oldBuilder = getCurrentBuilder();
    try {
      setCurrentBuilder(this);

      _baseUri = attribute.getBaseURI();

      ConfigType<?> type = TypeFactory.getType(bean);

      QName qName = ((QAbstractNode) attribute).getQName();

      type.beforeConfigure(this, bean, attribute);

      configureChildNode(attribute, qName, bean, type, false);

      type.afterConfigure(this, bean);
    }
    catch (LineConfigException e) {
      throw e;
    }
    catch (Exception e) {
      throw error(e, attribute);
    } finally {
      _baseUri = oldFile;
      setCurrentBuilder(oldBuilder);
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Configures a bean, calling its init() and replaceObject() methods.
   *
   * @param typeStrategy the strategy for handling the bean's type
   * @param bean the bean instance
   * @param top the configuration top
   * @return the configured bean, possibly the replaced object
   * @throws LineConfigException
   */
  public Object configureNode(Node node,
                              Object bean,
                              ConfigType<?> type)
    throws LineConfigException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      type.beforeConfigure(this, bean, node);
      type.beforeConfigureBean(this, bean, node);

      if (log.isLoggable(Level.FINEST))
        log.finest("config begin " + type);

      configureNodeAttributes(node, bean, type);

      for (Node childNode = node.getFirstChild();
           childNode != null;
           childNode = childNode.getNextSibling()) {
        QName qName = ((QAbstractNode) childNode).getQName();

        configureChildNode(childNode, qName, bean, type, false);
      }

      if (log.isLoggable(Level.FINEST))
        log.finest("config end " + type);

      type.afterConfigure(this, bean);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, node);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return bean;
  }

  /**
   * Configures a bean, calling its init() and replaceObject() methods.
   *
   * @param typeStrategy the strategy for handling the bean's type
   * @param bean the bean instance
   * @param top the configuration top
   * @return the configured bean, possibly the replaced object
   * @throws LineConfigException
   */
  private void configureNodeAttributes(Node node,
                                       Object bean,
                                       ConfigType<?> type)
  {
    if (node instanceof QAttributedNode) {
      Node child = ((QAttributedNode) node).getFirstAttribute();

      for (; child != null; child = child.getNextSibling()) {
        Attr attr = (Attr) child;
        QName qName = ((QNode) attr).getQName();

        configureChildNode(attr, qName, bean, type, false);
      }
    }
    else {
      NamedNodeMap attrList = node.getAttributes();
      if (attrList != null) {
        int length = attrList.getLength();
        for (int i = 0; i < length; i++) {
          Attr attr = (Attr) attrList.item(i);
          QName qName = ((QNode) attr).getQName();

          configureChildNode(attr, qName, bean, type, false);
        }
      }
    }
  }

  private void configureChildNode(Node childNode,
                                  QName qName,
                                  Object bean,
                                  ConfigType<?> type,
                                  boolean allowParam)
  {
    if (qName.getName().startsWith("xmlns")) {
      return;
    }

    Attribute attrStrategy;

   try {
      attrStrategy = getAttribute(type, qName, childNode);
      
      if (attrStrategy == null) {
      }
      else if (attrStrategy.isProgram()) {
        attrStrategy.setValue(bean, qName,
                              buildProgram(attrStrategy, childNode));
      }
      else if (attrStrategy.isNode()) {
        attrStrategy.setValue(bean, qName, childNode);
      }
      else if (configureInlineText(bean, childNode, qName, attrStrategy)) {
      }
      else if (configureInlineBean(bean, childNode, attrStrategy)) {
      }
      else {
        configureBeanProperties(childNode, qName, bean,
                                type, attrStrategy,
                                allowParam);
      }
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, childNode);
    }
  }

  private Attribute getAttribute(ConfigType<?> type,
                                 QName qName,
                                 Node childNode)
  {
    Attribute attrStrategy;

    attrStrategy = type.getAttribute(qName);
    if (attrStrategy == null) {
      attrStrategy = type.getDefaultAttribute(qName);
    }

    if (attrStrategy != null)
      return attrStrategy;

    if (childNode instanceof Element || childNode instanceof Attr) {
      String localName = qName.getLocalName();

      if (localName.indexOf(':') >= 0) {
        // XXX: need ioc QA
        throw error(L.l("'{0}' does not have a defined namespace for 'xmlns:{1}'.  Tags with prefixes need defined namespaces.",
                        qName.getName(),
                        localName.substring(0, localName.indexOf(':'))),
                    childNode);
      }

      // ioc/23m2
      if ("new".equals(localName))
        return null;

      throw error(L.l("'{0}' is an unknown property of '{1}'.",
                      qName.getName(), type.getTypeName()),
                  childNode);
    }

    return null;
  }

  private void setText(Object bean,
                       QName qName,
                       String text,
                       Attribute attrStrategy,
                       boolean isTrim)
  {
    ConfigType<?> attrType = attrStrategy.getConfigType();

    if (isTrim && ! attrType.isNoTrim())
      text = text.trim();

    if (isEL() && attrType.isEL() && text.indexOf("${") >= 0) {
      Object value = attrType.valueOf(evalObject(text));

      attrStrategy.setValue(bean, qName, value);
    }
    else
      attrStrategy.setText(bean, qName, text);
  }

  private boolean configureInlineText(Object bean,
                                      Node childNode,
                                      QName qName,
                                      Attribute attrStrategy)
  {
    // ioc/2013
    if (! attrStrategy.isSetter())
      return false;

    boolean isTrim = ! attrStrategy.getConfigType().isNoTrim();
    String text = getTextValue(childNode, isTrim);

    if (text == null)
      return false;

    isTrim = isTrim(childNode);

    if (isEL() && attrStrategy.isEL()
        && (text.indexOf("#{") >= 0 || text.indexOf("${") >= 0)) {
      if (isTrim)
        text = text.trim();

      Object elValue = eval(attrStrategy.getConfigType(), text);

      // ioc/2410
      if (elValue != NULL)
        attrStrategy.setValue(bean, qName, elValue);
      else {
        try {
          attrStrategy.setValue(bean, qName, null);
        } catch (Exception e) {
          throw ConfigException.create(L.l("{0} value must not be null.\n  ", text), e);
        }
      }

      return true;
    }
    else if (attrStrategy.isAllowText()) {
      setText(bean, qName, text, attrStrategy, isTrim);

      return true;
    }
    else
      return false;
  }

  private boolean configureInlineBean(Object parent, Node node,
                                      Attribute attrStrategy)
  {
    /* server/0219
    if (! attrStrategy.isAllowInline()) {
      return false;
    }
    */

    Node childNode = getChildElement(node);

    if (childNode == null) {
      return false;
    }
    
    QName qName = ((QNode) childNode).getQName();

    ConfigType<?> type = TypeFactory.getFactory().getEnvironmentType(qName);

    if (type == null || ! attrStrategy.isInlineType(type)) {
      // server/6500
      return false;
    }

    // server/0219
    // Object childBean = attrStrategy.create(parent, qName, type);

    Element childNew = getChildNewElement(childNode);

    Object childBean;

    if (childNew != null)
      childBean = createNew(type, parent, childNew);
    else
      childBean = type.create(parent, qName);

    if (childBean == null)
      return false;

    // server/1af3
    ConfigType<?> childType = TypeFactory.getType(childBean);

    childBean = configureChildBean(childBean, childType,
                                   childNode, attrStrategy);

    attrStrategy.setValue(parent, qName, childBean);

    return true;
  }

  private void configureBeanProperties(Node childNode,
                                       QName qName,
                                       Object bean,
                                       ConfigType<?> type,
                                       Attribute attrStrategy,
                                       boolean allowParam)
  {
    Object childBean = attrStrategy.create(bean, qName);

    if (childBean == null) {
      throw unableToCreateError(attrStrategy, bean, qName, childNode);
    }

    ConfigType<?> childBeanType = TypeFactory.getType(childBean);

    childBean = configureChildBean(childBean, childBeanType,
                                   childNode, attrStrategy);

    attrStrategy.setValue(bean, qName, childBean);
  }

  private Object configureChildBean(Object childBean,
                                    ConfigType<?> childBeanType,
                                    Node childNode,
                                    Attribute attrStrategy)
  {
    if (childNode instanceof Element) {
      configureNode(childNode, childBean, childBeanType);
    }
    else
      configureChildNode(childNode, TEXT, childBean, childBeanType, false);

    childBeanType.init(childBean);

    Object newBean = attrStrategy.replaceObject(childBean);

    if (newBean == childBean)
      return childBeanType.replaceObject(childBean);
    else
      return newBean;
  }
  
  private ConfigException unableToCreateError(Attribute attr,
                                              Object bean,
                                              QName qName,
                                              Node node)
  {
    Element child = getUniqueChildElement(node);
    
    if (child == null) {
      throw error(L.l("unable to create inline attribute '{2}' for '{1}' because no unique child Element exists.  Attribute = {0}",
                      attr, bean, qName),
                      node);
    }
    

    QName childQName = ((QNode) child).getQName();
    String uri = childQName.getNamespaceURI();

    if (uri == null || ! uri.startsWith("urn:java:")) {
      throw error(L.l("unable to create inline attribute '{0}' for '{1}' because the child <{2}> uri must start with xmlns='urn:java:...' but uri='{3}' for {4}",
                      qName.getName(), bean, childQName.getName(), uri, attr),
                      node);
    }
    
    throw throwUnableToCreateError(attr, bean, qName, node);
  }
  
  private ConfigException throwUnableToCreateError(Attribute attr,
                                                   Object bean,
                                                   QName qName,
                                                   Node childNode)
  {
    throw error(L.l("unable to create attribute {0} for {1} and {2}",
                    attr, bean, qName),
                    childNode);
  }

  private Node getChildElement(Node node)
  {
    Element child = getUniqueChildElement(node);
    
    if (child == null)
      return null;

    QName qName = ((QNode) child).getQName();
    String uri = qName.getNamespaceURI();

    if (uri == null || ! uri.startsWith("urn:java:"))
      return null;

    return child;
  }
  
  private Element getUniqueChildElement(Node node)
  {
    if (! (node instanceof Element))
      return null;

    Element elt = (Element) node;

    Node child = elt.getFirstChild();

    if (child == null)
      return null;

    if (isEmptyText(child)) {
      child = child.getNextSibling();

      if (child == null)
        return null;
    }

    Node next = child.getNextSibling();

    if (next != null) {
      if (next.getNextSibling() != null || ! isEmptyText(next))
        return null;
    }
    
    if (child instanceof Element)
      return (Element) child;
    else
      return null;
  }

  private Object createNew(ConfigType<?> type,
                           Object parent,
                           Element newNode)
  {
    boolean isTrim = true;
    
    String text = getTextValue(newNode, isTrim);

    if (text != null) {
      text = text.trim();

      return type.valueOf(create(newNode, StringType.TYPE));
    }

    int count = countNewChildren(newNode);

    Constructor<?> ctor = type.getConstructor(count);

    Class<?> []paramTypes = ctor.getParameterTypes();

    Object []args = new Object[paramTypes.length];
    int i = 0;
    for (Node child = newNode.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (! (child instanceof Element))
        continue;

      ConfigType<?> childType = TypeFactory.getType(paramTypes[i]);

      args[i++] = create(child, childType);
    }

    try {
      return ctor.newInstance(args);
    } catch (InvocationTargetException e) {
      throw ConfigException.create(ctor.getName(), e.getCause());
    } catch (Exception e) {
      throw ConfigException.create(ctor.getName(), e);
    }
  }

  private int countNewChildren(Element newNode)
  {
    int count = 0;

    for (Node child = newNode.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (child instanceof Element)
        count++;
    }

    return count;
  }

  private Element getChildNewElement(Node node)
  {
    if (! (node instanceof Element))
      return null;

    Element elt = (Element) node;

    for (Node child = elt.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if ("new".equals(child.getLocalName()))
        return (Element) child;
    }

    return null;
  }

  private boolean isEmptyText(Node node)
  {
    if (! (node instanceof CharacterData))
      return false;

    CharacterData data = (CharacterData) node;

    return data.getData().trim().length() == 0;
  }

  private boolean isTrim(Node node)
  {
    if (node instanceof Attr)
      return false;
    else if (node instanceof Element) {
      Element elt = (Element) node;

      if (! "".equals(elt.getAttribute("xml:space")))
        return false;
    }

    return true;
  }

  private ConfigProgram buildProgram(Attribute attr, Node node)
  {
    return new NodeBuilderChildProgram(node);
  }

  //
  // Used for args
  //
  public Object create(Node childNode, ConfigType<?> type)
    throws ConfigException
  {
    try {
      Object childBean;
      String text;

      if ((text = getArgTextValue(childNode)) != null) {
        boolean isTrim = isTrim(childNode);

        if (isEL() && type.isEL()
            && (text.indexOf("#{") >= 0 || text.indexOf("${") >= 0)) {
          if (isTrim)
            text = text.trim();

          Object elValue = eval(type, text);

          // ioc/2410
          if (elValue != NULL)
            return elValue;
          else
            return null;
        }
        else {
          return type.valueOf(text);
        }
      }

      QName qName = ((QNode) childNode).getQName();

      ConfigType<?> childBeanType = type.createType(qName);

      if (childBeanType != null) {
        childBean = childBeanType.create(null, qName);

        if (childNode instanceof Element)
          configureNode(childNode, childBean, childBeanType);
        else
          configureChildNode(childNode, TEXT, childBean, childBeanType, false);

        childBeanType.init(childBean);

        return childBeanType.replaceObject(childBean);
      }
      else {
        String textValue;

        if (type.isNoTrim())
          textValue = textValueNoTrim(childNode);
        else
          textValue = textValue(childNode);

        if (isEL() && type.isEL() && textValue.indexOf("${") >= 0) {
          Object value = type.valueOf(evalObject(textValue));

          return value;
        }
        else
          return type.valueOf(textValue);
      }
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, childNode);
    }
  }

  Object configureValue(Node node)
  {
    String value = textValue(node);

    if (isEL() && value != null
        && value.startsWith("${") && value.endsWith("}")) {
      return evalObject(value);
    }
    else
      return value;
  }

  public ArrayList<Dependency> getDependencyList()
  {
    return _dependList;
  }

  ArrayList<Dependency> getDependencyList(Node node)
  {
    ArrayList<Dependency> dependList = null;

    if (node instanceof QElement) {
      QElement qelt = (QElement) node;

      QDocument doc = (QDocument) qelt.getOwnerDocument();

      if (doc == null)
        return null;
      else if (doc == _dependDocument)
        return _dependList;

      _dependDocument = doc;

      ArrayList<Path> pathList;
      pathList = doc.getDependList();

      if (pathList != null) {
        dependList = new ArrayList<Dependency>();

        for (int i = 0; i < pathList.size(); i++) {
          dependList.add(new Depend(pathList.get(i)));
        }
      }

      _dependList = dependList;
    }

    return dependList;
  }

  /**
   * Returns the variable resolver.
   */
  public static ConfigELContext getELContext()
  {
    return ConfigELContext.EL_CONTEXT;
  }

  /**
   * Returns the text value of the node.
   */
  private String getTextValue(Node node, boolean isTrim)
  {
    if (node instanceof Attr) {
      Attr attrNode = (Attr) node;
      String data = attrNode.getNodeValue();

      return data;
    }
    else if (node instanceof CharacterData) {
      CharacterData cData = (CharacterData) node;

      return cData.getData();
    }

    if (! (node instanceof Element))
      return null;

    QElement elt = (QElement) node;

    // ioc/2235
    for (Node attr = elt.getFirstAttribute();
         attr != null;
         attr = attr.getNextSibling()) {
      if (! "xml".equals(attr.getPrefix()))
        return null;
    }

    for (Node child = elt.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (child instanceof Element) {
        return null;
      }

      else if (child instanceof CharacterData) {
        String data = ((CharacterData) child).getData();

        if (child.getNextSibling() != null) {
          return null;
        }
        else if (! isTrim || ! XmlUtil.isWhitespace(data)) {
          return data;
        }
        else {
          return null;
        }
      }
    }

    return "";
  }

  /**
   * Returns the text value of the node.
   */
  private String getArgTextValue(Node node)
  {
    if (node instanceof Element && ! node.getLocalName().equals("value"))
      return null;

    boolean isTrim = true;
    return getTextValue(node, isTrim);
  }

  private Object eval(ConfigType<?> type, String data)
  {
    ELContext elContext = getELContext();

    ELParser parser = new ELParser(elContext, data);

    Expr expr = parser.parse();

    Object value = type.valueOf(elContext, expr);
    
    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Returns the text value of the node.
   */
  private static String textValue(Node node)
  {
    if (node instanceof Attr)
      return node.getNodeValue();
    else {
      String value = XmlUtil.textValue(node);

      if (value == null || value.equals(""))
        return "";
      else if (node instanceof Element) {
        String space = ((Element) node).getAttribute("xml:space");

        if (! space.equals(""))
          return value;
      }

      return value.trim();
    }
  }

  /**
   * Returns the text value of the node.
   */
  private static String textValueNoTrim(Node node)
  {
    if (node instanceof Attr)
      return node.getNodeValue();
    else {
      String value = XmlUtil.textValue(node);

      if (value == null)
        return "";

      return value;
    }
  }

  /**
   * Evaluate as an object
   */
  private Object evalObject(String exprString)
    throws ELException
  {
    if (exprString.indexOf("${") >= 0 && isEL()) {
      ELParser parser = new ELParser(getELContext(), exprString);
      parser.setCheckEscape(true);
      Expr expr = parser.parse();

      return expr.getValue(getELContext());
    }
    else
      return exprString;
  }

  private static RuntimeException error(String msg, Node node)
  {
    String systemId = null;
    String filename = null;
    int line = 0;

    if (node instanceof QAbstractNode) {
      QAbstractNode qnode = (QAbstractNode) node;

      systemId = qnode.getBaseURI();
      filename = qnode.getFilename();
      line = qnode.getLine();
    }

    if (systemId != null) {
      String sourceLines = getSourceLines(systemId, line);

      msg = msg + sourceLines;
    }

    if (filename != null)
      return new LineConfigException(filename, line, msg);
    else
      return new LineConfigException(msg);
  }

  private static RuntimeException error(Throwable e, Node node)
  {
    String systemId = null;
    String filename = null;
    int line = 0;

    if (e instanceof RuntimeException
        && e instanceof DisplayableException
        && ! ConfigException.class.equals(e.getClass())) {
      return (RuntimeException) e;
    }

    if (node instanceof QAbstractNode) {
      QAbstractNode qnode = (QAbstractNode) node;

      systemId = qnode.getBaseURI();
      filename = qnode.getFilename();
      line = qnode.getLine();
    }

    for (; e.getCause() != null; e = e.getCause()) {
      if (e instanceof LineCompileException)
        break;
      else if (e instanceof LineConfigException)
        break;
      else if (e instanceof CompileException)
        break;
    }

    if (e instanceof LineConfigException)
      return (LineConfigException) e;
    else if (e instanceof LineCompileException) {
      return new LineConfigException(e.getMessage(), e);
    }
    else if (e instanceof ConfigException
             && e.getMessage() != null
             && filename != null) {
      String sourceLines = getSourceLines(systemId, line);

      return new LineConfigException(filename, line,
                                     e.getMessage() + sourceLines,
                                     e);
    }
    else if (e instanceof CompileException && e.getMessage() != null) {
      return new LineConfigException(filename, line, e);
    }
    else {
      String sourceLines = getSourceLines(systemId, line);

      String msg = filename + ":" + line + ": " + e + sourceLines;

      if (e instanceof RuntimeException) {
        throw new LineConfigException(msg, e);
      }
      else if (e instanceof Error) {
        // server/1711
        throw new LineConfigException(msg, e);
        // throw (Error) e;
      }
      else
        return new LineConfigException(msg, e);
    }
  }

  private static String getSourceLines(String systemId, int errorLine)
  {
    if (systemId == null)
      return "";

    ReadStream is = null;
    try {
      is = Vfs.lookup().lookup(systemId).openRead();
      int line = 0;
      StringBuilder sb = new StringBuilder("\n\n");
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

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
