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
 * @author Emil Ong, Adam Megacz
 */

package com.caucho.jaxb;

import com.caucho.jaxb.skeleton.*;
import com.caucho.jaxb.property.*;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.xml.QNode;

import org.w3c.dom.Node;

import javax.activation.DataHandler;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.datatype.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;

/**
 * Entry point to API
 */
public class JAXBContextImpl extends JAXBContext {
  public static final String XML_SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";

  public static final String TARGET_NAMESPACE = 
    "com.caucho.jaxb.targetNamespace";

  static final ValidationEventHandler DEFAULT_VALIDATION_EVENT_HANDLER
    = new DefaultValidationEventHandler();

  private static final L10N L = new L10N(JAXBContextImpl.class);

  private static final HashSet<Class> _specialClasses = new HashSet<Class>();

  static {
    _specialClasses.add(Object.class);
    _specialClasses.add(Class.class);
    _specialClasses.add(String.class);
    _specialClasses.add(Double.class);
    _specialClasses.add(Float.class);
    _specialClasses.add(Integer.class);
    _specialClasses.add(Long.class);
    _specialClasses.add(Boolean.class);
    _specialClasses.add(Character.class);
    _specialClasses.add(Short.class);
    _specialClasses.add(Byte.class);
    _specialClasses.add(BigDecimal.class);
    _specialClasses.add(BigInteger.class);
    _specialClasses.add(QName.class);
    _specialClasses.add(Date.class);
    _specialClasses.add(Calendar.class);
    _specialClasses.add(XMLGregorianCalendar.class);
    _specialClasses.add(UUID.class);
    _specialClasses.add(DataHandler.class);
  }

  private String _targetNamespace = null;

  private String[] _packages;
  private JAXBIntrospector _jaxbIntrospector;

  private XMLInputFactory _staxInputFactory;
  private XMLOutputFactory _staxOutputFactory;

  private final ArrayList<ObjectFactorySkeleton> _objectFactories 
    = new ArrayList<ObjectFactorySkeleton>();

  private final HashMap<String,Object> _properties 
    = new HashMap<String,Object>();

  private final LinkedHashMap<Class,ClassSkeleton> _classSkeletons 
    = new LinkedHashMap<Class,ClassSkeleton>();

  private final LinkedHashMap<Class,JAXBElementSkeleton> _jaxbElementSkeletons 
    = new LinkedHashMap<Class,JAXBElementSkeleton>();

  private final HashMap<QName,ClassSkeleton> _roots 
    = new HashMap<QName,ClassSkeleton>();
  private final HashMap<QName,ClassSkeleton> _types 
    = new HashMap<QName,ClassSkeleton>();

  private final ArrayList<EnumProperty> _enums = new ArrayList<EnumProperty>();

  private HashSet<Class> _pendingSkeletons = new HashSet<Class>();

  private DynamicJAXBElementSkeleton _dynamicSkeleton;
  private Property _laxAnyTypeProperty = null;

  private boolean _isDiscoveryFinished = false;

  public static JAXBContext createContext(String contextPath,
                                          ClassLoader classLoader,
                                          Map<String,?> properties)
    throws JAXBException
  {
    return new JAXBContextImpl(contextPath, classLoader, properties);
  }

  public JAXBContextImpl(String contextPath,
                         ClassLoader classLoader,
                         Map<String,?> properties)
    throws JAXBException
  {
    _jaxbIntrospector = new JAXBIntrospectorImpl(this);
    StringTokenizer st = new StringTokenizer(contextPath, ":");

    if (properties != null)
      _targetNamespace = (String) properties.get(TARGET_NAMESPACE);

    do {
      String packageName = st.nextToken(); 
      loadPackage(packageName, classLoader);
    } 
    while (st.hasMoreTokens());

    init(properties);
  }

  public static JAXBContext createContext(Class []classes, 
                                          Map<String,?> properties)
    throws JAXBException
  {
    return new JAXBContextImpl(classes, properties);
  }

  public JAXBContextImpl(Class[] classes, Map<String,?> properties)
    throws JAXBException
  {
    _jaxbIntrospector = new JAXBIntrospectorImpl(this);
    _packages = new String[0];

    if (properties != null)
      _targetNamespace = (String) properties.get(TARGET_NAMESPACE);

    for(Class c : classes) {
      if (c.getName().endsWith(".ObjectFactory"))
        introspectObjectFactory(c);
      else if (! c.isPrimitive() && // XXX pull out to JAX-WS?
          ! c.isArray() && 
          ! _specialClasses.contains(c))
        createSkeleton(c);
    }

    init(properties);
  }

  private void init(Map<String,?> properties)
    throws JAXBException
  {
    if (properties != null) {
      for(Map.Entry<String,?> e : properties.entrySet())
        setProperty(e.getKey(), e.getValue());
    }

    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());

    _dynamicSkeleton = new DynamicJAXBElementSkeleton(this);

    _isDiscoveryFinished = true;

    for (ClassSkeleton skeleton : _classSkeletons.values())
      skeleton.postProcess();
  }

  public boolean isDiscoveryFinished()
  {
    return _isDiscoveryFinished;
  }

  public Marshaller createMarshaller()
    throws JAXBException
  {
    return new MarshallerImpl(this);
  }

  public Unmarshaller createUnmarshaller()
    throws JAXBException
  {
    return new UnmarshallerImpl(this);
  }

  public Validator createValidator()
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  XMLStreamReader getXMLStreamReader(InputStream is)
    throws XMLStreamException
  {
    if (_staxInputFactory == null)
      _staxInputFactory = XMLInputFactory.newInstance();

    return _staxInputFactory.createXMLStreamReader(is);
  }

  public XMLInputFactory getXMLInputFactory()
  {
    if (_staxInputFactory == null)
      _staxInputFactory = XMLInputFactory.newInstance();

    return _staxInputFactory;
  }

  public XMLOutputFactory getXMLOutputFactory()
  {
    if (_staxOutputFactory == null) {
      _staxOutputFactory = XMLOutputFactory.newInstance();
      _staxOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                                     Boolean.TRUE);
    }

    return _staxOutputFactory;
  }

  public String getTargetNamespace()
  {
    return _targetNamespace;
  }

  public String toString() 
  {
    StringBuilder sb = new StringBuilder();
    sb.append("JAXBContext[");

    for (Class c : _classSkeletons.keySet())
      sb.append(c.getName() + ":");

    for (int i = 0; i < _packages.length; i++) {
      String p = _packages[i];
      sb.append(p + (i < _packages.length - 1 ? ":" : ""));
    }

    sb.append("]");
    return sb.toString();
  }

  private void setProperty(String key, Object val)
  {
    _properties.put(key, val);
  }

  public Binder<Node> createBinder()
  {
    return (Binder<Node>) new BinderImpl(this);
  }

  public <T> Binder<T> createBinder(Class<T> domType)
  {
    if (! domType.equals(QNode.class))
      throw new UnsupportedOperationException("Unsupported implementation: " + 
                                              domType);

    return (Binder) new BinderImpl(this);
  }
  
  public JAXBIntrospector createJAXBIntrospector()
  {
    return _jaxbIntrospector;
  }

  public void generateSchema(SchemaOutputResolver outputResolver)
    throws IOException
  {
    Result result = outputResolver.createOutput("", "schema1.xsd");

    XMLStreamWriter out = null;
    
    try {
      XMLOutputFactory factory = getXMLOutputFactory();
      out = factory.createXMLStreamWriter(result);

      out.writeStartDocument("UTF-8", "1.0");

      out.writeStartElement("xsd", "schema", XML_SCHEMA_NS);
      out.writeAttribute("version", "1.0");

      generateSchemaWithoutHeader(out);

      out.writeEndElement(); // schema
    }
    catch (Exception e) {
      IOException ioException = new IOException();

      ioException.initCause(e);

      throw ioException;
    }
    finally {
      try {
        out.close();
      }
      catch (XMLStreamException e) {
        throw new IOException(e.toString());
      }
    }
  }

  public void generateSchemaWithoutHeader(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    for (ClassSkeleton skeleton : _classSkeletons.values())
      skeleton.generateSchema(out);

    for (int i = 0; i < _enums.size(); i++)
      ((EnumProperty) _enums.get(i)).generateSchema(out);
  }

  public ClassSkeleton createSkeleton(Class c)
    throws JAXBException
  {
    ClassSkeleton skeleton = _classSkeletons.get(c);

    if (skeleton != null)
      return skeleton;

    if (Object.class.equals(c)) {
      skeleton = new AnyTypeSkeleton(this);
      _classSkeletons.put(c, skeleton);
    }
    else {
      // XXX
      if (c.isEnum() || c.isInterface()) {
        return null;
        /*
        throw new IllegalStateException(L.l("{0}: Can't create skeleton for an interface or enum",
                                            c.getName()));
        */
      }

      skeleton = new ClassSkeleton(this, c);

      // Breadcrumb to prevent problems with recursion
      _classSkeletons.put(c, skeleton); 

      _pendingSkeletons.add(c);

      skeleton.init();

      _pendingSkeletons.remove(c);
    }

    return skeleton;
  }

  public ClassSkeleton getSkeleton(Class c)
    throws JAXBException
  {
    return createSkeleton(c);
  }

  public boolean hasSkeleton(Class c)
  {
    return _classSkeletons.containsKey(c);
  }

  public ClassSkeleton findSkeletonForClass(Class cl)
    throws JAXBException
  {
    return findSkeletonForClass(cl, _jaxbElementSkeletons);
  }

  public ClassSkeleton 
    findSkeletonForClass(Class cl, Map<Class,? extends ClassSkeleton> map)
  {
    Class givenClass = cl;

    while (! cl.equals(Object.class)) {
      ClassSkeleton skeleton = map.get(cl);

      if (skeleton != null)
        return skeleton;

      cl = cl.getSuperclass();
    }

    return null;
  }

  public ClassSkeleton findSkeletonForObject(Object obj)
  {
    if (obj instanceof JAXBElement) {
      JAXBElement element = (JAXBElement) obj;

      obj = element.getValue();

      ClassSkeleton skeleton = 
        findSkeletonForClass(obj.getClass(), _jaxbElementSkeletons);

      if (skeleton == null)
        skeleton = _dynamicSkeleton;
      
      return skeleton;
    }
    else
      return findSkeletonForClass(obj.getClass(), _classSkeletons);
  }

  /**
   * Finds all ClassSkeletons that are subclasses of the given class and
   * are root elements.
   **/
  public List<ClassSkeleton> getRootElements(Class cl)
  {
    ArrayList<ClassSkeleton> list = new ArrayList<ClassSkeleton>();

    for (Map.Entry<Class,ClassSkeleton> entry : _classSkeletons.entrySet()) {
      if (cl.isAssignableFrom(entry.getKey()) && 
          entry.getValue().isRootElement())
        list.add(entry.getValue());
    }

    return list;
  }

  public Property getLaxAnyTypeProperty()
    throws JAXBException
  {
    if (_laxAnyTypeProperty == null)
      _laxAnyTypeProperty = new LaxAnyTypeProperty(this);

    return _laxAnyTypeProperty;
  }

  // XXX clean up all this argument tiering

  public Property createProperty(Type type)
    throws JAXBException
  {
    return createProperty(type, false);
  }

  public Property createProperty(Type type, boolean anyType)
    throws JAXBException
  {
    return createProperty(type, anyType, null);
  }

  public Property createProperty(Type type, boolean anyType, String mimeType)
    throws JAXBException
  {
    return createProperty(type, anyType, mimeType, false);
  }

  public Property createProperty(Type type, boolean anyType, String mimeType, 
                                 boolean xmlList)
    throws JAXBException
  {
    return createProperty(type, anyType, mimeType, xmlList, false);
  }

  public Property createProperty(Type type, boolean anyType, String mimeType, 
                                 boolean xmlList, boolean xmlValue)
    throws JAXBException
  {
    if (type instanceof Class) {
      if (anyType && Object.class.equals(type))
        return getLaxAnyTypeProperty();

      Property simpleTypeProperty = 
        getSimpleTypeProperty((Class) type, mimeType);

      if (simpleTypeProperty != null) {
        // jaxb/12gb
        if (simpleTypeProperty == ByteArrayProperty.PROPERTY && 
            xmlList && ! xmlValue)
          throw new JAXBException(L.l("@XmlList applied to byte[] valued fields or properties"));

        return simpleTypeProperty;
      }

      Class cl = (Class) type;

      if (cl.isArray()) {
        Property componentProperty = 
          createProperty(cl.getComponentType(), anyType);

        if (xmlList) {
          if (! (componentProperty instanceof CDataProperty))
            throw new JAXBException(L.l("Elements annotated with @XmlList or @XmlValue must be simple XML types"));

          Class componentType = cl.getComponentType();
          CDataProperty cdataProperty = (CDataProperty) componentProperty;

          return XmlListArrayProperty.createXmlListArrayProperty(cdataProperty,
                                                                 componentType);
        }
        else 
          return ArrayProperty.createArrayProperty(componentProperty,
                                                   cl.getComponentType());
      }

      if (cl.isEnum()) {
        EnumProperty enumProperty = new EnumProperty(cl, this);
        _enums.add(enumProperty);

        return enumProperty;
      }

      // XXX Map

      if (List.class.isAssignableFrom(cl)) {
        Property property = new SkeletonProperty(getSkeleton(Object.class));

        if (xmlList) {
          throw new JAXBException(L.l("Elements annotated with @XmlList or @XmlValue must be simple XML types"));
        }
        else
          return new ListProperty(property);
      }

      if (Collection.class.isAssignableFrom(cl)) {
        Property property = new SkeletonProperty(getSkeleton(Object.class));

        if (xmlList) {
          throw new JAXBException(L.l("Elements annotated with @XmlList or @XmlValue must be simple XML types"));
        }
        else
          return new CollectionProperty(property);
      }

      ClassSkeleton skel = getSkeleton(cl);

      // XXX: interfaces
      if (skel != null)
        return new SkeletonProperty(skel);
      else
        return null;
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType) type;
      Type rawType = ptype.getRawType();

      if (rawType instanceof Class) {
        Class rawClass = (Class) rawType;

        if (Map.class.isAssignableFrom(rawClass)) {
          Type[] args = ptype.getActualTypeArguments();

          if (args.length != 2)
            throw new JAXBException(L.l("unexpected number of generic arguments for Map<>: {0}", args.length));

          Property keyProperty = createProperty(args[0], anyType);
          Property valueProperty = createProperty(args[1], anyType);

          return new MapProperty(rawClass, keyProperty, valueProperty);
        }

        if (Collection.class.isAssignableFrom(rawClass)) {
          Type[] args = ptype.getActualTypeArguments();

          if (args.length != 1)
            throw new JAXBException(L.l("unexpected number of generic arguments for List<>: {0}", args.length));

          Property componentProperty = createProperty(args[0], anyType);

          if (xmlList) {
            if (! (componentProperty instanceof CDataProperty))
              throw new JAXBException(L.l("Elements annotated with @XmlList or @XmlValue must be simple XML types"));

            CDataProperty cdataProperty = (CDataProperty) componentProperty;

            return new XmlListCollectionProperty(cdataProperty, rawClass);
          }
          else if (List.class.isAssignableFrom(rawClass))
            return new ListProperty(componentProperty);
          else
            return new CollectionProperty(componentProperty);
        }
      }
    }
    else if (type instanceof GenericArrayType) {
      Type component = ((GenericArrayType) type).getGenericComponentType();

      if (byte.class.equals(component))
        return ByteArrayProperty.PROPERTY;

      // XXX other component types?
    }

    throw new JAXBException(L.l("Unrecognized type: {0}", type.toString()));
  }

  public Property getSimpleTypeProperty(Class type)
    throws JAXBException
  {
    return getSimpleTypeProperty(type, null);
  }

  public Property getSimpleTypeProperty(Class type, String mimeType)
    throws JAXBException
  {
    if (String.class.equals(type))
      return StringProperty.PROPERTY;

    if (URI.class.equals(type))
      return URIProperty.PROPERTY;

    if (UUID.class.equals(type))
      return UUIDProperty.PROPERTY;

    if (Double.class.equals(type))
      return DoubleProperty.OBJECT_PROPERTY;

    if (Double.TYPE.equals(type))
      return DoubleProperty.PRIMITIVE_PROPERTY;

    if (Float.class.equals(type))
      return FloatProperty.OBJECT_PROPERTY;

    if (Float.TYPE.equals(type))
      return FloatProperty.PRIMITIVE_PROPERTY;

    if (Integer.class.equals(type))
      return IntProperty.OBJECT_PROPERTY;

    if (Integer.TYPE.equals(type))
      return IntProperty.PRIMITIVE_PROPERTY;

    if (Long.class.equals(type))
      return LongProperty.OBJECT_PROPERTY;

    if (Long.TYPE.equals(type))
      return LongProperty.PRIMITIVE_PROPERTY;

    if (Boolean.class.equals(type))
      return BooleanProperty.OBJECT_PROPERTY;

    if (Boolean.TYPE.equals(type))
      return BooleanProperty.PRIMITIVE_PROPERTY;

    if (Character.class.equals(type))
      return CharacterProperty.OBJECT_PROPERTY;

    if (Character.TYPE.equals(type))
      return CharacterProperty.PRIMITIVE_PROPERTY;

    if (Short.class.equals(type))
      return ShortProperty.OBJECT_PROPERTY;

    if (Short.TYPE.equals(type))
      return ShortProperty.PRIMITIVE_PROPERTY;

    if (Byte.class.equals(type))
      return ByteProperty.OBJECT_PROPERTY;

    if (Byte.TYPE.equals(type))
      return ByteProperty.PRIMITIVE_PROPERTY;

    if (BigDecimal.class.equals(type))
      return BigDecimalProperty.PROPERTY;

    if (BigInteger.class.equals(type))
      return BigIntegerProperty.PROPERTY;

    if (QName.class.equals(type))
      return QNameProperty.PROPERTY;

    if (Date.class.equals(type))
      return DateTimeProperty.PROPERTY;

    if (Calendar.class.equals(type))
      return CalendarProperty.PROPERTY;

    if (Duration.class.equals(type))
      return DurationProperty.PROPERTY;

    if (XMLGregorianCalendar.class.equals(type))
      return XMLGregorianCalendarProperty.PROPERTY;

    if (Image.class.equals(type))
      return ImageProperty.getImageProperty(mimeType);

    if (DataHandler.class.equals(type))
      return DataHandlerProperty.PROPERTY;

    if (Source.class.equals(type))
      return SourceProperty.PROPERTY;

    if (byte[].class.equals(type))
      return ByteArrayProperty.PROPERTY;

    return null;
  }

  public void addXmlType(QName typeName, ClassSkeleton skeleton)
    throws JAXBException
  {
    if (_types.containsKey(typeName)) {
      ClassSkeleton existing = _types.get(typeName);

      if (! _pendingSkeletons.contains(existing.getType())) {
        throw new JAXBException(L.l("Duplicate type name {0} for types {1} and {2}",
                                    typeName,
                                    skeleton.getType(),
                                    existing.getType()));
      }
    }

    _types.put(typeName, skeleton);
  }

  public void addRootElement(ClassSkeleton s) 
    throws JAXBException
  {
    ClassSkeleton old = _roots.get(s.getElementName());

    // Use != here to check duplicate puts; equals() isn't necessary
    if (old != null && old != s && ! _pendingSkeletons.contains(s.getType())) {
      throw new JAXBException(L.l("Duplicate name {0} for classes {1} and {2}",
                                  s.getElementName(),
                                  s.getType(),
                                  _roots.get(s.getElementName()).getType()));
    }

    _roots.put(s.getElementName(), s);
  }

  public boolean hasXmlType(QName typeName)
  {
    return _types.containsKey(typeName);
  }

  public boolean hasRootElement(QName elementName)
  {
    return _roots.containsKey(elementName);
  }

  public ClassSkeleton getRootElement(QName q)
  {
    return _roots.get(q);
  }

  private void loadPackage(String packageName, ClassLoader classLoader) 
    throws JAXBException
  {
    boolean success = false;

    try {
      Class cl = Class.forName(packageName + ".ObjectFactory");
      introspectObjectFactory(cl);

      success = true;
    }
    catch (ClassNotFoundException e) {
      // we can still try for jaxb.index
    }

    String resourceName = packageName.replace('.', '/') + "/jaxb.index";

    // For some reason, this approach works when running resin...
    InputStream is = this.getClass().getResourceAsStream('/' + resourceName);

    // ...and this approach works in QA
    if (is == null)
      is = classLoader.getResourceAsStream(resourceName);

    if (is == null) {
      if (success)
        return;

      throw new JAXBException(L.l("Unable to open jaxb.index for package {0}",
                                  packageName));
    }

    try {
      InputStreamReader isr = new InputStreamReader(is, "utf-8");
      LineNumberReader in = new LineNumberReader(isr);

      for (String line = in.readLine();
           line != null;
           line = in.readLine()) {
        String[] parts = line.split("#", 2);
        String className = parts[0].trim();

        if (! "".equals(className)) {
          Class cl = classLoader.loadClass(packageName + "." + className);

          createSkeleton(cl);
        }
      }
    }
    catch (IOException e) {
      throw new JAXBException(L.l("Error while reading jaxb.index for package {0}", packageName), e);
    }
    catch (ClassNotFoundException e) {
      throw new JAXBException(e);
    }
  }

  private void introspectObjectFactory(Class factoryClass)
    throws JAXBException
  {
    Object objectFactory = null;

    try {
      objectFactory = factoryClass.newInstance();
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }

    String namespace = null;
    Package pkg = factoryClass.getPackage();

    if (pkg.isAnnotationPresent(XmlSchema.class)) {
      XmlSchema schema = (XmlSchema) pkg.getAnnotation(XmlSchema.class);

      if (! "".equals(schema.namespace()))
        namespace = schema.namespace();
    }

    Method[] methods = factoryClass.getMethods();

    for (Method method : methods) {
      if (method.getName().startsWith("create")) {
        XmlElementDecl decl = method.getAnnotation(XmlElementDecl.class);
        Class cl = method.getReturnType();

        ClassSkeleton skeleton = null;
        QName root = null;

        if (cl.equals(JAXBElement.class)) {
          ParameterizedType type = 
            (ParameterizedType) method.getGenericReturnType();
          cl = (Class) type.getActualTypeArguments()[0];

          skeleton = new JAXBElementSkeleton(this, cl, method, objectFactory);
          _jaxbElementSkeletons.put(cl, (JAXBElementSkeleton) skeleton);
        }
        else {
          skeleton = getSkeleton(cl);

          if (skeleton == null) 
            skeleton = createSkeleton(cl);

          skeleton.setCreateMethod(method, objectFactory);

          root = skeleton.getElementName();
        }

        if (decl != null) {
          String localName = decl.name();

          if (! "##default".equals(decl.namespace()))
            namespace = decl.namespace();

          if (namespace == null)
            root = new QName(localName);
          else
            root = new QName(namespace, localName);
        }

        skeleton.setElementName(root);
        addRootElement(skeleton);
      }
      else if (method.getName().equals("newInstance")) {
        // XXX
      }
      else if (method.getName().equals("getProperty")) {
        // XXX
      }
      else if (method.getName().equals("setProperty")) {
        // XXX
      }
    }
  }

  static class DefaultValidationEventHandler implements ValidationEventHandler {
    public boolean handleEvent(ValidationEvent event)
    {
      if (event == null)
        throw new IllegalArgumentException("Event may not be null");

      return event.getSeverity() != ValidationEvent.FATAL_ERROR;
    }
  }
}

