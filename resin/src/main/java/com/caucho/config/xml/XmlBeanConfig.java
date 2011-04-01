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

package com.caucho.config.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.interceptor.InterceptorBinding;

import org.w3c.dom.Node;

import com.caucho.config.ConfigException;
import com.caucho.config.ConfiguredLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.program.Arg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.NodeBuilderChildProgram;
import com.caucho.config.reflect.AnnotatedElementImpl;
import com.caucho.config.reflect.AnnotatedMethodImpl;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.config.type.ConfigType;
import com.caucho.config.type.TypeFactory;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

/**
 * Custom bean configured by namespace
 */
public class XmlBeanConfig<T> {
  private static final L10N L = new L10N(XmlBeanConfig.class);

  private static final String RESIN_NS
    = "http://caucho.com/ns/resin";

  private InjectManager _cdiManager;

  private Class<T> _class;
  private AnnotatedTypeImpl<T> _annotatedType;
  private Bean<T> _bean;
  private ConfigType<T> _configType;

  private ArrayList<ConfigProgram> _args;

  private QName _name;

  private String _filename;
  private int _line;

  private ContainerProgram _init;
  private boolean _hasBindings;
  private boolean _hasInterceptorBindings;
  private boolean _isInlineBean;

  public XmlBeanConfig(QName name, Class<T> cl)
  {
    _name = name;

    _class = cl;

    _cdiManager = InjectManager.create();
    
    // XXX:
    // _component = new SimpleBean(cl);
    // _component.setScopeClass(Dependent.class);
    AnnotatedType<T> annType = ReflectionAnnotatedFactory.introspectType(cl); 
    _annotatedType = new AnnotatedTypeImpl<T>(annType);
      
    _cdiManager.addConfiguredBean(cl.getName());
    
    _configType = TypeFactory.getCustomBeanType(cl);

    // defaults to @Configured
    // clearAnnotations(_annotatedType, DeploymentType.class);
    _annotatedType.addAnnotation(ConfiguredLiteral.create());

    for (AnnotatedMethod<?> method : _annotatedType.getMethods()) {
      // ioc/0614

      AnnotatedMethodImpl<?> methodImpl = (AnnotatedMethodImpl<?>) method;

      if (methodImpl.isAnnotationPresent(Produces.class))
        methodImpl.addAnnotation(ConfiguredLiteral.create());
    }
  }

  public ConfigType<T> getConfigType()
  {
    return _configType;
  }

  public Class<T> getClassType()
  {
    return _class;
  }

  public void setConfigLocation(String filename, int line)
  {
    _filename = filename;
    _line = line;
  }

  public String getFilename()
  {
    return _filename;
  }

  public int getLine()
  {
    return _line;
  }

  public void setInlineBean(boolean isInline)
  {
    _isInlineBean = isInline;
  }
  
  public void addArg(ConfigProgram arg)
  {
    if (_args == null)
      _args = new ArrayList<ConfigProgram>();

    _args.add(arg);
  }

  public void addArgs(ArrayList<ConfigProgram> args)
  {
    _args = new ArrayList<ConfigProgram>(args);
  }

  /*
  public void setNew(ConfigProgram []args)
  {
    if (_args == null)
      _args = new ArrayList<ConfigProgram>();

    for (ConfigProgram arg : args) {
      _args.add(arg);
    }
  }
  */

  public void addAdd(ConfigProgram add)
  {
    addInitProgram(add);
  }

  public void addInitProgram(ConfigProgram program)
  {
    if (_init == null) {
      _init = new ContainerProgram();

      /*
      if (_component != null)
        _component.setInit(_init);
      */
    }

    _init.addProgram(program);
  }

  public void addBuilderProgram(ConfigProgram program)
  {
    QName name = program.getQName();

    if (name == null) {
      addInitProgram(program);

      return;
    }

    Class<?> cl = createClass(name);

    if (cl == null) {
    }
    else if (Annotation.class.isAssignableFrom(cl)) {
      ConfigType<?> type = TypeFactory.getType(cl);

      Object bean = type.create(null, name);

      Node node = getProgramNode(program);

      if (node != null)
        XmlConfigContext.getCurrent().configureNode(node, bean, type);

      Annotation ann = (Annotation) type.replaceObject(bean);

      addAnnotation(ann);

      return;
    }

    if (name.getNamespaceURI().equals(_name.getNamespaceURI())) {
      if (_configType.getAttribute(name) != null)
        addInitProgram(program);
      else {
        throw new ConfigException(L.l("'{0}' is an unknown field for '{1}'",
                                      name.getLocalName(), _class.getName()));
      }
    }

    else
      throw new ConfigException(L.l("'{0}' is an unknown field name.  Fields must belong to the same namespace as the class",
                                    name.getCanonicalName()));
  }

  private Node getProgramNode(ConfigProgram program)
  {
    if (program instanceof NodeBuilderChildProgram)
      return ((NodeBuilderChildProgram) program).getNode();
    return null;
  }

  public void addAnnotation(Annotation ann)
  {
    // XXX: some annotations also remove other annotations

    if (ann.annotationType().isAnnotationPresent(Qualifier.class)
        && ! _hasBindings) {
      _hasBindings = true;
      clearBindings(_annotatedType);
    }

    if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)
        && ! _hasInterceptorBindings) {
      _hasInterceptorBindings = true;
      clearAnnotations(_annotatedType, InterceptorBinding.class);
    }

    if (ann.annotationType().isAnnotationPresent(Scope.class)
        || ann.annotationType().isAnnotationPresent(NormalScope.class)) {
      clearAnnotations(_annotatedType, Scope.class);
      clearAnnotations(_annotatedType, NormalScope.class);
    }

    _annotatedType.addAnnotation(ann);
  }

  public void addMethod(XmlBeanMethodConfig methodConfig)
  {
    Method method = methodConfig.getMethod();
    Annotation []annList = methodConfig.getAnnotations();

    AnnotatedMethod<?> annMethod = _annotatedType.createMethod(method);

    if (annMethod instanceof AnnotatedMethodImpl<?>) {
      AnnotatedMethodImpl<?> methodImpl = (AnnotatedMethodImpl<?>) annMethod;

      // ioc/0c64
      methodImpl.clearAnnotations();

      addAnnotations(methodImpl, annList);
    }

    //_component.addMethod(new SimpleBeanMethod(method, annList));
  }

  private void addAnnotations(AnnotatedElementImpl annotated,
                              Annotation []annList)
  {
    for (Annotation ann : annList) {
      annotated.addAnnotation(ann);
    }
  }

  public void addField(XmlBeanFieldConfig fieldConfig)
  {
    // Field field = fieldConfig.getField();
    // Annotation []annList = fieldConfig.getAnnotations();

    //_component.addField(new SimpleBeanField(field, annList));
  }

  private void clearBindings(AnnotatedTypeImpl<?> beanType)
  {
    HashSet<Annotation> annSet
      = new HashSet<Annotation>(beanType.getAnnotations());

    for (Annotation ann : annSet) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        beanType.removeAnnotation(ann);
    }
  }

  private void clearAnnotations(AnnotatedTypeImpl<?> beanType,
                                Class<? extends Annotation> annType)
  {
    HashSet<Annotation> annSet
      = new HashSet<Annotation>(beanType.getAnnotations());

    for (Annotation ann : annSet) {
      if (ann.annotationType().isAnnotationPresent(annType))
        beanType.removeAnnotation(ann);
    }
  }

  private Class<?> createClass(QName name)
  {
    String uri = name.getNamespaceURI();
    String localName = name.getLocalName();

    if (uri.equals(RESIN_NS)) {
      return createResinClass(localName);
    }

    if (! uri.startsWith("urn:java:"))
      return null;

    String pkg = uri.substring("urn:java:".length());

    return TypeFactory.loadClass(pkg, name.getLocalName());
  }

  private Class<?> createResinClass(String name)
  {
    Class<?> cl = TypeFactory.loadClass("ee", name);

    if (cl != null)
      return cl;

    cl = TypeFactory.loadClass("com.caucho.config", name);

    if (cl != null)
      return cl;

    return null;
  }

  @PostConstruct
  public void init()
  {
    if (_annotatedType != null) {
      initComponent();
    }
  }

  public void initComponent()
  {
    /* XXX: constructor

    if (_args != null)
      _component.setNewArgs(_args);
    */

    Arg<?> []newProgram = null;
    Constructor<?> javaCtor = null;

    if (_args != null) {
      AnnotatedConstructor<T> ctor = null;

      for (AnnotatedConstructor<T> testCtor
             : _annotatedType.getConstructors()) {
        if (testCtor.getParameters().size() == _args.size())
          ctor = testCtor;
      }

      if (ctor == null) {
        throw new ConfigException(L.l("No matching constructor found for '{0}' with {1} arguments.",
                                      _annotatedType, _args.size()));
      }

      javaCtor = ctor.getJavaMember();
      ArrayList<ConfigProgram> newList = _args;

      newProgram = new Arg[newList.size()];

      Type []genericParam = javaCtor.getGenericParameterTypes();
      List<AnnotatedParameter<T>> parameters
        = (List<AnnotatedParameter<T>>) ctor.getParameters();
      String loc = null;

      for (int i = 0; i < _args.size(); i++) {
        ConfigProgram argProgram = _args.get(i);
        ConfigType<?> type = TypeFactory.getType(genericParam[i]);

        if (argProgram != null)
          newProgram[i] = new ProgramArg(type, argProgram);
        else
          newProgram[i] = new BeanArg(loc, genericParam[i], parameters.get(i).getAnnotations());
      }
    }
    else
      newProgram = new Arg[0];

    ConfigProgram []injectProgram;

    if (_init != null) {
      ArrayList<ConfigProgram> programList = _init.getProgramList();

      injectProgram = new ConfigProgram[programList.size()];
      programList.toArray(injectProgram);
    }
    else
      injectProgram = new ConfigProgram[0];

    XmlCookie xmlCookie = _cdiManager.generateXmlCookie();
    
    _annotatedType.addAnnotation(xmlCookie);
    
    ManagedBeanImpl<T> managedBean
      = new ManagedBeanImpl<T>(_cdiManager,_annotatedType, false);
    
    managedBean.introspect();
    
    XmlInjectionTarget<T> injectionTarget
      = new XmlInjectionTarget(managedBean, javaCtor, newProgram, injectProgram);
    
    _bean = new XmlBean<T>(managedBean, injectionTarget);
    
    _cdiManager.addXmlInjectionTarget(xmlCookie.value(), injectionTarget);
    
    if (! _isInlineBean) {
      _cdiManager.discoverBean(_annotatedType);
      // ioc/23n3
      _cdiManager.processPendingAnnotatedTypes();
    }
    
    //beanManager.addBean(_bean);

    //managedBean.introspectProduces();
    /*
    for (Bean producesBean : managedBean.getProducerBeans()) {
      beanManager.addBean(producesBean);
    }
    */
  }

  protected Bean bindParameter(String loc,
                               Type type,
                               Set<Annotation> bindingSet)
  {
    Annotation []bindings = new Annotation[bindingSet.size()];
    bindingSet.toArray(bindings);

    Set<Bean<?>> set = _cdiManager.getBeans(type, bindings);

    if (set == null || set.size() == 0)
      return null;

    return _cdiManager.resolve(set);
  }

  public Object toObject()
  {
    if (_bean == null)
      init();
    
    InjectManager beanManager = InjectManager.create();

    CreationalContext<?> env = beanManager.createCreationalContext(_bean);
    Class<?> type = _bean.getBeanClass();

    return InjectManager.create().getReference(_bean, type, env);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _class.getSimpleName() + "]";
  }

  class BeanArg extends Arg<T> {
    private String _loc;
    private Constructor<T> _ctor;
    private Type _type;
    private Set<Annotation> _bindings;
    private Bean<T> _bean;

    BeanArg(String loc, Type type, Set<Annotation> bindings)
    {
      _loc = loc;
      _type = type;
      _bindings = bindings;
      bind();
    }

    public void bind()
    {
      if (_bean == null) {
        _bean = bindParameter(_loc, _type, _bindings);

        if (_bean == null)
          throw new ConfigException(L.l("{0}: {1} does not have valid arguments",
                                        _loc, _ctor));
      }
    }

    public Object eval(CreationalContext<T> env)
    {
      if (_bean == null)
        bind();

      // XXX: getInstance for injection?
      Type type = null;
      return _cdiManager.getReference(_bean, type, env);
    }
  }

  static class ProgramArg<T> extends Arg<T> {
    private ConfigType<T> _type;
    private ConfigProgram _program;

    ProgramArg(ConfigType<T> type, ConfigProgram program)
    {
      _type = type;
      _program = program;
    }

    public Object eval(CreationalContext<T> creationalContext)
    {
      // ConfigContext env = ConfigContext.create();
      
      // env.setCreationalContext(creationalContext);
      
      return _program.create(_type, creationalContext);
    }
  }
}
