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
 * @author Emil Ong
 */

package com.caucho.jaxb;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.ws.Holder;

import static java.lang.Character.*;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.caucho.util.L10N;

/**
 * JAXB utilities.
 */
public class JAXBUtil {
  private static final L10N L = new L10N(JAXBUtil.class);
  private static final Logger log = Logger.getLogger(JAXBUtil.class.getName());

  private static final Map<Class,QName> _datatypeMap
    = new HashMap<Class,QName>();
  private static final Map<QName,Class> _classMap
    = new HashMap<QName,Class>();

  private static DatatypeFactory _datatypeFactory;

  public static final XMLEventFactory EVENT_FACTORY 
    = XMLEventFactory.newInstance();

  public static final String XML_SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";
  public static final String XML_SCHEMA_PREFIX = "xsd";

  public static DatatypeFactory getDatatypeFactory()
    throws JAXBException
  {
    if (_datatypeFactory == null) {
      try {
        _datatypeFactory = DatatypeFactory.newInstance();
      }
      catch (DatatypeConfigurationException e) {
        throw new JAXBException(e);
      }
    }

    return _datatypeFactory;
  }

  public static QName qnameFromNode(Node node)
  {
    if (node == null)
      return null;

    int colon = node.getNodeName().indexOf(':');

    if (colon > 0) { 
      String prefix = node.getNodeName().substring(0, colon);
      String localName = node.getNodeName().substring(colon + 1);

      return new QName(node.getNamespaceURI(), localName, prefix);
    } 
    else if (node.getNamespaceURI() != null)
      return new QName(node.getNamespaceURI(), node.getNodeName());
    else
      return new QName(node.getNodeName());
  }

  public static Element elementFromQName(QName name, Node node)
  {
    Document doc = node.getOwnerDocument();

    if (name.getPrefix() != null)
      return doc.createElementNS(name.getNamespaceURI(),
                                 name.getPrefix() + ':' + name.getLocalPart());
    else if (name.getNamespaceURI() != null)
      return doc.createElementNS(name.getNamespaceURI(), name.getLocalPart());
    else
      return doc.createElement(name.getLocalPart());
  }

  // skip all the whitespace and comments
  public static Node skipIgnorableNodes(Node node) 
  {
    while (node != null) {
      if (node.getNodeType() == Node.TEXT_NODE) {
        String text = node.getTextContent();

        boolean whitespace = true;

        for (int i = 0; i < text.length(); i++) {
          if (! Character.isWhitespace(text.charAt(i))) {
            whitespace = false;
            break;
          }
        }

        if (! whitespace)
          break;
      }
      else if (node.getNodeType() != Node.COMMENT_NODE) 
        break;

      node = node.getNextSibling();
    }

    return node;
  }

  /**
   * Gets the type of a parameter.  If the type is something like Holder<T>,
   * it return T, otherwise, it returns the passed type.
   *
   **/
  public static Type getActualParameterType(Type type)
    throws JAXBException
  {
    if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType) type;

      if (ptype.getRawType().equals(Holder.class)) {
        Type[] arguments = ptype.getActualTypeArguments();

        if (arguments.length != 1)
          throw new JAXBException("Holder has incorrect number of arguments");

        return arguments[0];
      }
    }

    return type;
  }

  public static void introspectClass(Class cl, Collection<Class> jaxbClasses)
    throws JAXBException
  {
    log.finest("Introspecting class " + cl.getName());

    Method[] methods = cl.getMethods();

    for (Method method : methods)
      introspectMethod(method, jaxbClasses);
  }

  /**
   * Finds all the classes mentioned in a method signature (return type and
   * parameters) and adds them to the passed in classList.  Pass in a set if
   * you expect multiple references.
   */
  public static void introspectMethod(Method method, 
                                      Collection<Class> jaxbClasses)
    throws JAXBException
  {
    log.finest("Introspecting method " + method.getName());

    introspectType(method.getReturnType(), jaxbClasses);

    Type[] params = method.getGenericParameterTypes();

    for (Type param : params) {
      if (param.equals(Holder.class))
        continue;

      introspectType(getActualParameterType(param), jaxbClasses);
    }

    // XXX: Check for @WebFault annotation

    /*
    Type[] exceptions = method.getGenericExceptionTypes();

    for (Type exception : exceptions) {
      if (! exception.toString().endsWith("_Exception"))
        introspectType(exception, jaxbClasses);
    }*/
  }

  /**
   * Add all classes referenced by type to jaxbClasses.
   */
  private static void introspectType(Type type, Collection<Class> jaxbClasses)
  {
    log.finest("  Introspecting type " + type);

    if (type instanceof Class) {
      Class cl = (Class) type;

      if (! cl.isInterface())
        jaxbClasses.add((Class) type);
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      introspectType(pType.getRawType(), jaxbClasses);
      introspectType(pType.getOwnerType(), jaxbClasses);

      Type[] arguments = pType.getActualTypeArguments();

      for (Type argument : arguments)
        introspectType(argument, jaxbClasses);
    }
    else if (type instanceof GenericArrayType) {
      Type component = ((GenericArrayType) type).getGenericComponentType();
      introspectType(component, jaxbClasses);
    }
    else if (type != null) {
      // Type variables must be instantiated
      throw new UnsupportedOperationException(L.l("Method arguments cannot have uninstantiated type variables or wildcards ({0} of type {1})", type, type.getClass().getName()));
    }
  }

  public static String primitiveToWrapperName(Class cl)
  {
    if (cl.getName().equals("int"))
      return "Integer";
    else if (cl.getName().equals("char"))
      return "Character";
    else
      return toUpperCase(cl.getName().charAt(0)) + cl.getName().substring(1);
  }

  /**
   * Tests for punctuation according to JAXB page 334.
   */
  private static boolean isPunctuation(char ch)
  {
    return "-.:\u00B7\u0387\u06DD\u06dd\u06de_".indexOf((int) ch) >= 0;
  }

  /**
   * Tests for "uncased" characters.
   */
  private static boolean isUncased(char ch)
  {
    return (! isLowerCase(ch)) && (! isUpperCase(ch));
  }

  /**
   * Splits a string into XML "words" as defined by the JAXB standard.
   * (see page 162 and appendix D)
   *
   */
  private static List<StringBuilder> splitIdentifier(String identifier)
  {
    List<StringBuilder> words = new ArrayList<StringBuilder>();
    StringBuilder word = new StringBuilder();
    char lastCh = 0;

    for (int i = 0; i < identifier.length(); i++) {
      char ch = identifier.charAt(i);

      // punctuation shouldn't be common for the java -> xml direction
      if (word.length() > 0 && isPunctuation(ch)) {
        words.add(word);
        word = new StringBuilder();
      } 
      else if (isDigit(ch)) {
        if (word.length() > 0 && ! isDigit(lastCh)) {
          words.add(word);
          word = new StringBuilder();
        }

        word.append(ch);
      }
      else if (i > 0) { // all of the following need lastCh
        if (isLowerCase(lastCh) && isUpperCase(ch)) {
          words.add(word);
          word = new StringBuilder();
          word.append(ch);
        }
        else if (isUpperCase(lastCh) && isLowerCase(ch)) {
          // need to steal the last character from the current word 
          // for the next word (e.g. FOOBar -> { "FOO", "Bar" })

          if (word.length() > 1) {
            word.deleteCharAt(word.length() - 1);
            words.add(word);
          }

          word = new StringBuilder();
          word.append(lastCh);
          word.append(ch);
        }
        else if (isLetter(lastCh) != isLetter(ch)) {
          words.add(word);
          word = new StringBuilder();
          word.append(ch);
        }
        else if (isUncased(lastCh) != isUncased(ch)) {
          words.add(word);
          word = new StringBuilder();
          word.append(ch);
        }
        else
          word.append(ch);
      }
      else
        word.append(ch);

      lastCh = ch;
    }

    if (word.length() > 0)
      words.add(word);

    return words;
  }

  public static String identifierToXmlName(Class cl)
  {
    List<StringBuilder> words = splitIdentifier(cl.getSimpleName());
    StringBuilder xmlName = new StringBuilder();

    xmlName.append(toLowerCase(words.get(0).charAt(0)));
    xmlName.append(words.get(0).substring(1));

    for (int i = 1; i < words.size(); i++) {
      if (words.get(i).length() > 0) {
        xmlName.append(toUpperCase(words.get(i).charAt(0)));
        xmlName.append(words.get(i).substring(1));
      }
    }

    return xmlName.toString();
  }

  public static String xmlNameToClassName(String name)
  {
    // XXX FIXME

    if (name.length() > 1)
      return toUpperCase(name.charAt(0)) + name.substring(1);
    else
      return toUpperCase(name.charAt(0)) + "";
  }

  /// XXX this is sooooooooo wrong.  It belongs in JAXBContextImpl
  public static QName getXmlSchemaDatatype(Class cl, JAXBContextImpl context)
  {
    if (_datatypeMap.containsKey(cl))
      return _datatypeMap.get(cl);

    String name = null;
    String namespace = context.getTargetNamespace();

    Package pkg = cl.getPackage();

    // look at package defaults first...
    XmlSchema schema = (XmlSchema) pkg.getAnnotation(XmlSchema.class);

    if (schema != null && ! "".equals(schema.namespace()))
      namespace = schema.namespace();

    if (cl.isAnnotationPresent(XmlType.class)) {
      XmlType xmlType = (XmlType) cl.getAnnotation(XmlType.class);

      if (! "##default".equals(xmlType.name()))
        name = xmlType.name();

      if (! "##default".equals(xmlType.namespace()))
        namespace = xmlType.namespace();
    }

    if (name == null)
      name = identifierToXmlName(cl);

    QName qname = null;
    
    if (namespace == null)
      qname = new QName(name);
    else
      qname = new QName(namespace, name);

    _datatypeMap.put(cl, qname);

    return qname;
  }

  public static Class getClassForDatatype(QName qname)
  {
    return _classMap.get(qname);
  }

  public static String qNameToString(QName qName)
  {
    if (qName.getPrefix() == null || "".equals(qName.getPrefix()))
      return qName.getLocalPart();
    else
      return qName.getPrefix() + ':' + qName.getLocalPart();
  }

  public static boolean isJAXBAnnotated(AnnotatedElement element)
  {
    return element.isAnnotationPresent(XmlAnyAttribute.class) ||
           element.isAnnotationPresent(XmlAnyElement.class) ||
           element.isAnnotationPresent(XmlAttribute.class) ||
           element.isAnnotationPresent(XmlElement.class) ||
           element.isAnnotationPresent(XmlElements.class) ||
           element.isAnnotationPresent(XmlElementRef.class) ||
           element.isAnnotationPresent(XmlID.class) ||
           element.isAnnotationPresent(XmlValue.class);
  }

  static {
    QName stringQName = new QName(XML_SCHEMA_NS, "string", XML_SCHEMA_PREFIX);
    _datatypeMap.put(String.class, stringQName);
    _classMap.put(stringQName, String.class);

    QName bigDecimalQName = 
      new QName(XML_SCHEMA_NS, "decimal", XML_SCHEMA_PREFIX);
    _datatypeMap.put(BigDecimal.class, bigDecimalQName);
    _classMap.put(bigDecimalQName, BigDecimal.class);

    QName bigIntegerQName = 
      new QName(XML_SCHEMA_NS, "integer", XML_SCHEMA_PREFIX);
    _datatypeMap.put(BigInteger.class, bigIntegerQName);
    _classMap.put(bigIntegerQName, BigInteger.class);

    QName booleanQName = new QName(XML_SCHEMA_NS, "boolean", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Boolean.class, booleanQName);
    _datatypeMap.put(boolean.class, booleanQName);
    _classMap.put(booleanQName, boolean.class);

    // XXX hexBinary
    QName base64BinaryQName = 
      new QName(XML_SCHEMA_NS, "base64Binary", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Byte[].class, base64BinaryQName); 
    _datatypeMap.put(byte[].class, base64BinaryQName);
    _classMap.put(base64BinaryQName, byte[].class);

    QName byteQName = new QName(XML_SCHEMA_NS, "byte", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Byte.class, byteQName);
    _datatypeMap.put(byte.class, byteQName);
    _classMap.put(byteQName, byte.class);

    QName charQName = 
      new QName(XML_SCHEMA_NS, "unsignedShort", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Character.class, charQName);
    _datatypeMap.put(char.class, charQName);
    _classMap.put(charQName, char.class);

    // XXX time
    QName calendarQName = new QName(XML_SCHEMA_NS, "date", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Calendar.class, calendarQName);
    _classMap.put(calendarQName, Calendar.class);

    QName dateTimeQName = 
      new QName(XML_SCHEMA_NS, "dateTime", XML_SCHEMA_PREFIX);
    _datatypeMap.put(XMLGregorianCalendar.class, dateTimeQName);
    _classMap.put(dateTimeQName, XMLGregorianCalendar.class);

    QName doubleQName = new QName(XML_SCHEMA_NS, "double", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Double.class, doubleQName);
    _datatypeMap.put(double.class, doubleQName);
    _classMap.put(doubleQName, double.class);

    QName floatQName = new QName(XML_SCHEMA_NS, "float", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Float.class, floatQName); 
    _datatypeMap.put(float.class, floatQName);
    _classMap.put(floatQName, float.class);

    QName intQName = new QName(XML_SCHEMA_NS, "int", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Integer.class, intQName);
    _datatypeMap.put(int.class, intQName);
    _classMap.put(intQName, int.class);

    QName longQName = new QName(XML_SCHEMA_NS, "long", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Long.class, longQName);
    _datatypeMap.put(long.class, longQName);
    _classMap.put(longQName, long.class);

    QName shortQName = new QName(XML_SCHEMA_NS, "short", XML_SCHEMA_PREFIX);
    _datatypeMap.put(Short.class, shortQName);
    _datatypeMap.put(short.class, shortQName);
    _classMap.put(shortQName, short.class);
  }
}
