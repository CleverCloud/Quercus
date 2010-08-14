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

package com.caucho.config.inject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.ejb.Stateful;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.interceptor.InterceptorBinding;
import javax.naming.InitialContext;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import com.caucho.config.CauchoDeployment;
import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.config.Configured;
import com.caucho.config.ContextDependent;
import com.caucho.config.LineConfigException;
import com.caucho.config.ModulePrivate;
import com.caucho.config.ModulePrivateLiteral;
import com.caucho.config.bytecode.ScopeAdapter;
import com.caucho.config.el.CandiElResolver;
import com.caucho.config.el.CandiExpressionFactory;
import com.caucho.config.event.EventBeanImpl;
import com.caucho.config.event.EventManager;
import com.caucho.config.extension.ExtensionManager;
import com.caucho.config.j2ee.EjbHandler;
import com.caucho.config.j2ee.PersistenceContextHandler;
import com.caucho.config.j2ee.PersistenceUnitHandler;
import com.caucho.config.j2ee.ResourceHandler;
import com.caucho.config.program.ResourceProgramManager;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.reflect.BaseType;
import com.caucho.config.reflect.BaseTypeFactory;
import com.caucho.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.config.scope.ApplicationContext;
import com.caucho.config.scope.DependentContext;
import com.caucho.config.scope.ErrorContext;
import com.caucho.config.scope.SingletonScope;
import com.caucho.config.xml.XmlCookie;
import com.caucho.config.xml.XmlCookieLiteral;
import com.caucho.config.xml.XmlStandardPlugin;
import com.caucho.inject.Module;
import com.caucho.inject.RequestContext;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentApply;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.Alarm;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

/**
 * The web beans container for a given environment.
 */
@ModulePrivate
@SuppressWarnings("serial")
public final class InjectManager
  implements BeanManager, EnvironmentListener,
             java.io.Serializable, HandleAware
{
  private static final L10N L = new L10N(InjectManager.class);
  private static final Logger log
    = Logger.getLogger(InjectManager.class.getName());

  private static final EnvironmentLocal<InjectManager> _localContainer
    = new EnvironmentLocal<InjectManager>();

  private static final int DEFAULT_PRIORITY = 1;

  private static final Annotation []DEFAULT_ANN
    = DefaultLiteral.DEFAULT_ANN_LIST;

  private static final String []FORBIDDEN_ANNOTATIONS = {
    "javax.persistence.Entity",
    /*
    "javax.ejb.Stateful",
    "javax.ejb.Stateless",
    "javax.ejb.Singleton",
    "javax.ejb.MessageDriven"
    */
  };

  private static final String []FORBIDDEN_CLASSES = {
    "javax.servlet.Servlet",
    "javax.servlet.Filter",
    "javax.servlet.ServletContextListener",
    "javax.servlet.http.HttpSessionListener",
    "javax.servlet.ServletRequestListener",
    "javax.ejb.EnterpriseBean",
    "javax.faces.component.UIComponent",
    "javax.enterprise.inject.spi.Extension",
  };

  private static final Class<? extends Annotation> []_forbiddenAnnotations;
  private static final Class<?> []_forbiddenClasses;
  
  private static final ClassLoader _systemClassLoader;

  private String _id;

  private InjectManager _parent;

  private EnvironmentClassLoader _classLoader;
  private ClassLoader _jndiClassLoader;
  
  private final InjectScanManager _scanManager;
  private final ExtensionManager _extensionManager
    = new ExtensionManager(this);
  private EventManager _eventManager = new EventManager(this);
  
  private AtomicLong _version = new AtomicLong();

  private HashMap<Class<?>,Class<?>> _specializedMap
    = new HashMap<Class<?>,Class<?>>();

  private HashMap<Class<?>,Integer> _deploymentMap
    = new HashMap<Class<?>,Integer>();

  private BaseTypeFactory _baseTypeFactory = new BaseTypeFactory();

  private HashMap<Class<?>,InjectionPointHandler> _injectionMap
    = new HashMap<Class<?>,InjectionPointHandler>();
  
  private ResourceProgramManager _resourceManager
    = new ResourceProgramManager();

  //
  // self configuration
  //

  private HashMap<Class<?>,ArrayList<TypedBean>> _selfBeanMap
    = new HashMap<Class<?>,ArrayList<TypedBean>>();

  private HashMap<String,ArrayList<Bean<?>>> _selfNamedBeanMap
    = new HashMap<String,ArrayList<Bean<?>>>();

  private HashMap<String,Bean<?>> _selfPassivationBeanMap
    = new HashMap<String,Bean<?>>();

  //
  // combined visibility configuration
  //

  private HashMap<Class<?>,WebComponent> _beanMap
    = new HashMap<Class<?>,WebComponent>();

  private HashMap<String,ArrayList<Bean<?>>> _namedBeanMap
    = new HashMap<String,ArrayList<Bean<?>>>();

  private HashMap<Type,Bean<?>> _newBeanMap
    = new HashMap<Type,Bean<?>>();
  
  private HashSet<Class<? extends Annotation>> _qualifierSet
    = new HashSet<Class<? extends Annotation>>();
  
  private HashSet<Class<? extends Annotation>> _scopeTypeSet
    = new HashSet<Class<? extends Annotation>>();
  
  private HashSet<Class<? extends Annotation>> _normalScopeSet
    = new HashSet<Class<? extends Annotation>>();
  
  private HashSet<Class<? extends Annotation>> _passivatingScopeSet
    = new HashSet<Class<? extends Annotation>>();
  
  private HashMap<Class<? extends Annotation>, Set<Annotation>> _stereotypeMap
    = new HashMap<Class<? extends Annotation>, Set<Annotation>>();

  private HashMap<Class<?>,Context> _contextMap
    = new HashMap<Class<?>,Context>();

  private ArrayList<Class<?>> _interceptorClassList
    = new ArrayList<Class<?>>();

  private ArrayList<InterceptorEntry<?>> _interceptorList
    = new ArrayList<InterceptorEntry<?>>();

  private ArrayList<Class<?>> _decoratorClassList
    = new ArrayList<Class<?>>();

  private ArrayList<DecoratorEntry<?>> _decoratorList
    = new ArrayList<DecoratorEntry<?>>();

  private HashSet<Bean<?>> _beanSet = new HashSet<Bean<?>>();

  private boolean _isUpdateNeeded = true;
  private boolean _isAfterValidationNeeded = true;

  private ArrayList<Path> _pendingPathList
    = new ArrayList<Path>();

  private ArrayList<AnnotatedType<?>> _pendingAnnotatedTypes
    = new ArrayList<AnnotatedType<?>>();

  private ArrayList<AbstractBean<?>> _pendingBindList
    = new ArrayList<AbstractBean<?>>();
  
  private ArrayList<Bean<?>> _pendingValidationBeans
    = new ArrayList<Bean<?>>();

  private ArrayList<Bean<?>> _pendingServiceList
    = new ArrayList<Bean<?>>();
  
  private ConcurrentHashMap<Bean<?>,ReferenceFactory<?>> _refFactoryMap
    = new ConcurrentHashMap<Bean<?>,ReferenceFactory<?>>();
  
  private ConcurrentHashMap<String,ReferenceFactory<?>> _namedRefFactoryMap
    = new ConcurrentHashMap<String,ReferenceFactory<?>>();

  private ThreadLocal<CreationalContextImpl<?>> _proxyThreadLocal
    = new ThreadLocal<CreationalContextImpl<?>>();
  
  private ConcurrentHashMap<Long,InjectionTarget<?>> _xmlTargetMap
    = new ConcurrentHashMap<Long,InjectionTarget<?>>();

  private boolean _isBeforeBeanDiscoveryComplete;
  private boolean _isAfterBeanDiscoveryComplete;
  
  private final AtomicLong _xmlCookieSequence
    = new AtomicLong(Alarm.getCurrentTime());

  // XXX: needs to be a local resolver
  private ELResolver _elResolver = new CandiElResolver(this);

  private DependentContext _dependentContext = new DependentContext();
  private SingletonScope _singletonScope;
  private ApplicationContext _applicationScope;
  private XmlStandardPlugin _xmlExtension;

  private RuntimeException _configException;

  private Object _serializationHandle;

  private InjectManager(String id,
                        InjectManager parent,
                        EnvironmentClassLoader loader,
                        boolean isSetLocal)
  {
    _id = id;
    
    _classLoader = loader;
    
    _parent = parent;
    
    _scanManager = new InjectScanManager(this);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      if (isSetLocal) {
        _localContainer.set(this, _classLoader);

        if (_parent == null) {
          _localContainer.setGlobal(this);
        }
      }

      if (_classLoader != null)
        _classLoader.getNewTempClassLoader();
      else
        new DynamicClassLoader(null);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void init(boolean isSetLocal)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      try {
        InitialContext ic = new InitialContext();
        ic.rebind("java:comp/BeanManager", new WebBeansJndiProxy());
      } catch (Throwable e) {
        log.log(Level.FINEST, e.toString(), e);
      }

      _singletonScope = new SingletonScope();
      _applicationScope = new ApplicationContext();
      
      addContext(new RequestContext());
      addContext("com.caucho.server.webbeans.SessionScope");
      addContext("com.caucho.server.webbeans.ConversationContext");
      addContext("com.caucho.server.webbeans.TransactionScope");
      addContext(_applicationScope);
      addContext(_singletonScope);
      addContext(_dependentContext);

      _injectionMap.put(PersistenceContext.class,
                        new PersistenceContextHandler(this));
      _injectionMap.put(PersistenceUnit.class,
                        new PersistenceUnitHandler(this));
      _injectionMap.put(Resource.class,
                        new ResourceHandler(this));
      _injectionMap.put(EJB.class,
                        new EjbHandler(this));
      _injectionMap.put(EJBs.class,
                        new EjbHandler(this));

      _deploymentMap.put(CauchoDeployment.class, 0);
      // DEFAULT_PRIORITY
      _deploymentMap.put(Configured.class, 2);

      BeanBuilder<InjectManager> factory = createBeanFactory(InjectManager.class);
      // factory.deployment(Standard.class);
      factory.type(InjectManager.class);
      factory.type(BeanManager.class);
      factory.annotation(ModulePrivateLiteral.create());
      addBean(factory.singleton(this));
      
      // ioc/0162
      addBean(new InjectionPointStandardBean());

      _xmlExtension = new XmlStandardPlugin(this);
      addExtension(_xmlExtension);
      _extensionManager.createExtension("com.caucho.server.webbeans.ResinStandardPlugin");

      if (_classLoader != null && isSetLocal) {
        // _classLoader.addScanListener(this);
        _classLoader.addScanListener(_scanManager);
      }

      Environment.addEnvironmentListener(this, _classLoader);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns the modification version.
   */
  public long getVersion()
  {
    return _version.get();
  }
  
  public InjectScanManager getScanManager()
  {
    return _scanManager;
  }
  
  public void setIsCustomExtension(boolean isCustom)
  {
    getScanManager().setIsCustomExtension(isCustom);
  }
  
  @Module
  public EventManager getEventManager()
  {
    return _eventManager;
  }
  
  @Module
  public ExtensionManager getExtensionManager()
  {
    return _extensionManager;
  }

  private void addContext(String contextClassName)
  {
    try {
      Class<?> cl = Class.forName(contextClassName);
      Context context = (Context) cl.newInstance();

      addContext(context);
    } catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the local container.
   */
  public static InjectManager getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current environment container.
   */
  public static InjectManager getCurrent(ClassLoader loader)
  {
    return _localContainer.get(loader);
  }

  /**
   * Returns the current active container.
   */
  public static InjectManager create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current active container.
   */
  public static InjectManager create(ClassLoader loader)
  {
    if (loader == null)
      loader = _systemClassLoader;
    
    InjectManager manager = null;

    manager = _localContainer.getLevel(loader);
    
    if (manager != null)
      return manager;
      
    EnvironmentClassLoader envLoader
      = Environment.getEnvironmentClassLoader(loader);

    // ejb doesn't create a new InjectManager even though it's a new
    // environment
    // XXX: yes it does, because of the SessionContext
    // ejb/2016 vs ejb/12h0
    /*
    if (envLoader != null
        && Boolean.FALSE.equals(envLoader.getAttribute("caucho.inject"))) {
      manager = create(envLoader.getParent());
      
      if (manager != null)
        return manager;
    }
    */

    String id;

    if (envLoader != null)
      id = envLoader.getId();
    else
      id = "";

    InjectManager parent = null;

    if (envLoader != null && envLoader != _systemClassLoader)
      parent = create(envLoader.getParent());

    synchronized (_localContainer) {
      manager = _localContainer.getLevel(envLoader);
        
      if (manager != null)
        return manager;
        
      manager = new InjectManager(id, parent, envLoader, true);
    }
      
    manager.init(true);
    
    return manager;
  }

  /**
   * Returns the current active container.
   */
  public InjectManager createParent(String prefix)
  {
    _parent = new InjectManager(prefix + _id,
                                _parent,
                                _classLoader,
                                false);
    _parent.init(false);

    return _parent;
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }
  
  public ClassLoader getJndiClassLoader()
  {
    return _jndiClassLoader;
  }
  
  public void setJndiClassLoader(ClassLoader loader)
  {
    _jndiClassLoader = loader;
  }

  public InjectManager getParent()
  {
    return _parent;
  }

  public ApplicationContext getApplicationScope()
  {
    return _applicationScope;
  }

  public void setParent(InjectManager parent)
  {
    _parent = parent;
  }

  public void addPath(Path path)
  {
    _pendingPathList.add(path);
  }

  public void setDeploymentTypes(ArrayList<Class<?>> deploymentList)
  {
    _deploymentMap.clear();

    _deploymentMap.put(CauchoDeployment.class, 0);
    // DEFAULT_PRIORITY

    int priority = DEFAULT_PRIORITY + 1;

    if (! deploymentList.contains(Configured.class)) {
      _deploymentMap.put(Configured.class, priority);
    }

    for (int i = deploymentList.size() - 1; i >= 0; i--) {
      _deploymentMap.put(deploymentList.get(i), priority);
    }
  }

  /**
   * Adds the bean to the named bean map.
   */
  private void addBeanByName(String name, Bean<?> bean)
  {
    ArrayList<Bean<?>> beanList = _selfNamedBeanMap.get(name);

    if (beanList == null) {
      beanList = new ArrayList<Bean<?>>();
      _selfNamedBeanMap.put(name, beanList);
    }
    else if (bean.isAlternative()) {
    }
    else if (_specializedMap.get(bean.getBeanClass()) != null) {
    }
    else {
      // ioc/0n18 vs ioc/0g30
      for (Bean<?> testBean : beanList) {
        if (testBean.isAlternative()) {
        }
        else if (bean.isAlternative()) {
        }
        else if (bean.getBeanClass().isAnnotationPresent(Specializes.class)
                 && testBean.getBeanClass().isAssignableFrom(bean.getBeanClass())) {
        }
        else if (testBean.getBeanClass().isAnnotationPresent(Specializes.class)
                  && bean.getBeanClass().isAssignableFrom(testBean.getBeanClass())) {
        }
        else if ((bean instanceof AbstractIntrospectedBean<?>)
                 && ((AbstractIntrospectedBean<?>) bean).getAnnotated().isAnnotationPresent(Specializes.class)) {
          // ioc/07a2
        }
        else if ((testBean instanceof AbstractIntrospectedBean<?>)
                 && ((AbstractIntrospectedBean<?>) testBean).getAnnotated().isAnnotationPresent(Specializes.class)) {
        }
        else {
          throw new ConfigException(L.l("@Named('{0}') is a duplicate name for\n  {1}\n  {2}",
                                        name, bean, testBean));
        }
      }
    }

    beanList.add(bean);

    _namedBeanMap.remove(name);
    
    // ioc/0g31
    int p = name.indexOf('.');
    if (p > 0) {
      addBeanByName(name.substring(0, p), bean);
    }
  }

  /**
   * Adds a bean by the interface type
   *
   * @param type the interface type to expose the component
   * @param bean the component to register
   */
  private void addBeanByType(Type type,
                             Annotated annotated,
                             Bean<?> bean)
  {
    if (type == null)
      return;
    
    BaseType baseType = createSourceBaseType(type);
    
    addBeanByType(baseType, annotated, bean);
  }

  private void addBeanByType(BaseType type,
                             Annotated annotated,
                             Bean<?> bean)
  {
    if (type == null)
      return;
    
    if (isSpecialized(bean.getBeanClass())) {
      return;
    }

    if (log.isLoggable(Level.FINEST))
      log.finest(bean + "(" + type + ") added to " + this);

    Class<?> rawType = type.getRawClass();

    ArrayList<TypedBean> beanSet = _selfBeanMap.get(rawType);

    if (beanSet == null) {
      beanSet = new ArrayList<TypedBean>();
      _selfBeanMap.put(rawType, beanSet);
    }
    _beanMap.remove(rawType);

    TypedBean typedBean = new TypedBean(type, annotated, bean);
    
    if (! beanSet.contains(typedBean)) {
      beanSet.add(typedBean);
    }
  }

  /**
   * Finds a component by its component name.
   */
  protected ArrayList<Bean<?>> findByName(String name)
  {
    // #3334 - shutdown timing issues
    HashMap<String,ArrayList<Bean<?>>> namedBeanMap = _namedBeanMap;

    if (namedBeanMap == null)
      return null;

    ArrayList<Bean<?>> beanList = _namedBeanMap.get(name);

    if (beanList == null) {
      beanList = new ArrayList<Bean<?>>();

      if (_classLoader != null)
        _classLoader.applyVisibleModules(new FillByName(name, beanList));

      // ioc/0680 
      /*
      for (int i = beanList.size() - 1; i >= 0; i--) {
        if (getDeploymentPriority(beanList.get(i)) < 0) {
          beanList.remove(i);
        }
      }
      */

      _namedBeanMap.put(name, beanList);
    }

    return beanList;
  }

  private void fillByName(String name, ArrayList<Bean<?>> beanList)
  {
    ArrayList<Bean<?>> localBeans = _selfNamedBeanMap.get(name);

    if (localBeans != null) {
      for (Bean<?> bean : localBeans) {
        /*
        if (getDeploymentPriority(bean) < 0)
          continue;
          */
        
        // ioc/0g20
        if (bean.isAlternative() && ! isEnabled(bean))
          continue;
        
        if (_specializedMap.containsKey(bean.getBeanClass()))
          continue;
        
        if (! beanList.contains(bean))
          beanList.add(bean);
      }
    }
  }

  //
  // javax.webbeans.Container
  //

  public Conversation createConversation()
  {
    return (Conversation) _contextMap.get(ConversationScoped.class);
  }

  /**
   * Creates an object, but does not register the
   * component with webbeans.
   */
  public <T> T createTransientObject(Class<T> type)
  {
    ManagedBeanImpl<T> bean = createManagedBean(type);

    validate(bean);
    
    // server/10gn
    //return factory.create(new ConfigContext());
    InjectionTarget<T> injectionTarget = bean.getInjectionTarget();

    CreationalContext<T> env = new OwnerCreationalContext<T>(bean);

    T instance = injectionTarget.produce(env);
    injectionTarget.inject(instance, env);

    return instance;
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> BeanBuilder<T> createBeanFactory(ManagedBeanImpl<T> managedBean)
  {
    return new BeanBuilder<T>(managedBean);
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with webbeans.
   */
  public <T> BeanBuilder<T> createBeanFactory(Class<T> type)
  {
    ManagedBeanImpl<T> managedBean = createManagedBean(type);
    
    if (managedBean != null)
      return createBeanFactory(managedBean);
    else
      return null;
  }

  /**
   * Returns a new instance for a class, but does not register the
   * component with CDI.
   */
  public <T> BeanBuilder<T> createBeanFactory(AnnotatedType<T> type)
  {
    return createBeanFactory(createManagedBean(type));
  }

  //
  // enabled deployment types, scopes, and qualifiers
  //

  @Module
  public void addScope(Class<? extends Annotation> scopeType,
                       boolean isNormal,
                       boolean isPassivating)
  {
    if (isPassivating && ! isNormal)
      throw new ConfigException(L.l("@{0} must be 'normal' because it's using 'passivating'",
                                    scopeType.getName()));

    _scopeTypeSet.add(scopeType);
    
    if (isNormal)
      _normalScopeSet.add(scopeType);
    
    if (isPassivating)
      _passivatingScopeSet.add(scopeType);
    
    if (isNormal) {
      // TCK - force validation of all methods
      _scanManager.setIsCustomExtension(true);
    }
  }

  /**
   * Tests if an annotation is an enabled scope type
   */
  @Override
  public boolean isScope(Class<? extends Annotation> annotationType)
  {
    return (annotationType.isAnnotationPresent(Scope.class)
            || annotationType.isAnnotationPresent(NormalScope.class)
            || _scopeTypeSet.contains(annotationType));
  }

  /**
   * Tests if an annotation is an enabled scope type
   */
  @Override
  public boolean isNormalScope(Class<? extends Annotation> annotationType)
  {
    return (annotationType.isAnnotationPresent(NormalScope.class)
            || _normalScopeSet.contains(annotationType));
  }

  /**
   * Tests if an annotation is an enabled scope type
   */
  @Override
  public boolean isPassivatingScope(Class<? extends Annotation> annotationType)
  {
    NormalScope scope = annotationType.getAnnotation(NormalScope.class);

    if (scope != null)
      return scope.passivating();
    
    return _passivatingScopeSet.contains(annotationType);
  }
  
  @Module
  public void addQualifier(Class<? extends Annotation> qualifier)
  {
    _qualifierSet.add(qualifier);
  }

  /**
   * Tests if an annotation is an enabled binding type
   */
  @Override
  public boolean isQualifier(Class<? extends Annotation> annotationType)
  {
    return (annotationType.isAnnotationPresent(Qualifier.class)
            || _qualifierSet.contains(annotationType));
  }
  
  /**
   * Tests if an annotation is an enabled interceptor binding type
   */
  @Override
  public boolean isInterceptorBinding(Class<? extends Annotation> annotationType)
  {
    return annotationType.isAnnotationPresent(InterceptorBinding.class);
  }

  /**
   * Returns the bindings for an interceptor binding type
   */
  @Override
  public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType)
  {
    LinkedHashSet<Annotation> annSet = new LinkedHashSet<Annotation>();
    
    for (Annotation ann : bindingType.getAnnotations()) {
      annSet.add(ann);
    }
    
    return annSet;
  }

  @Module
  public void addStereotype(Class<? extends Annotation> annotationType,
                            Annotation []annotations)
  {
    LinkedHashSet<Annotation> annSet = new LinkedHashSet<Annotation>();
    
    for (Annotation ann : annotations)
      annSet.add(ann);
    
    _stereotypeMap.put(annotationType, annSet);
  }
  
  /**
   * Tests if an annotation is an enabled stereotype.
   */
  @Override
  public boolean isStereotype(Class<? extends Annotation> annotationType)
  {
    return (annotationType.isAnnotationPresent(Stereotype.class)
            || _stereotypeMap.get(annotationType) != null);
  }

  /**
   * Returns the annotations associated with a stereotype
   */
  @Override
  public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype)
  {
    Set<Annotation> mapAnnSet = _stereotypeMap.get(stereotype);
    
    if (mapAnnSet != null)
      return mapAnnSet;
    
    if (! stereotype.isAnnotationPresent(Stereotype.class))
      return null;
    
    LinkedHashMap<Class<?>, Annotation> annMap
      = new LinkedHashMap<Class<?>, Annotation>();
    
    addStereotypeDefinitions(annMap, stereotype);
    
    mapAnnSet = new LinkedHashSet<Annotation>(annMap.values());
    
    _stereotypeMap.put(stereotype, mapAnnSet);
    
    return mapAnnSet;
  }
  
  private void addStereotypeDefinitions(Map<Class<?>,Annotation> annMap, 
                                        Class<? extends Annotation> stereotype)
  {
    for (Annotation ann : stereotype.getAnnotations()) {
      if (annMap.get(ann.annotationType()) == null)
        annMap.put(ann.annotationType(), ann);
    }
    
    for (Annotation ann : stereotype.getAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();
      
      if (annType.isAnnotationPresent(Stereotype.class)) {
        addStereotypeDefinitions(annMap, annType);
      }
    }
  }

  //
  // bean resolution and instantiation
  //

  /**
   * Creates a BaseType from a Type used as a target, for example 
   * an injection point.
   */
  public BaseType createTargetBaseType(Type type)
  {
    return _baseTypeFactory.createForTarget(type);
  }

  /**
   * Creates a BaseType from a Type used as a source, for example a Bean.
   */
  public BaseType createSourceBaseType(Type type)
  {
    return _baseTypeFactory.createForSource(type);
  }

  /**
   * Creates an annotated type.
   */
  @Override
  public <T> AnnotatedType<T> createAnnotatedType(Class<T> cl)
  {
    AnnotatedType<T> annType = ReflectionAnnotatedFactory.introspectType(cl);
    
    // TCK:
    // return getExtensionManager().processAnnotatedType(annType);
    
    return annType;
  }

  /**
   * Creates an injection target
   */
  @Override
  public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type)
  {
    InjectionTarget<T> target = new InjectionTargetBuilder<T>(this, type);

    return getExtensionManager().processInjectionTarget(target, type);
  }

  /**
   * Creates a managed bean.
   */
  public <T> InjectionTarget<T> createInjectionTarget(Class<T> type)
  {
    try {
      AnnotatedType<T> annType = ReflectionAnnotatedFactory.introspectType(type);
      
      AnnotatedType<T> enhAnnType
        = getExtensionManager().processAnnotatedType(annType);
      
      if (enhAnnType != null)
        return createInjectionTarget(enhAnnType);
      else {
        // special call from servlet, etc.
        return createInjectionTarget(annType);
      }
    } catch (Exception e) {
      throw ConfigException.createConfig(e);
    }
  }

  public <T,X> void addObserver(ObserverMethod<T> observer,
                                AnnotatedMethod<X> method)
  {
    _extensionManager.processObserver(observer, method);
    
    getEventManager().addObserver(observer);
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> createManagedBean(AnnotatedType<T> type)
  {
    if (type == null)
      throw new NullPointerException();
    
    ManagedBeanImpl<T> bean
      = new ManagedBeanImpl<T>(this, type, false);
    bean.introspect();

    return bean;
  }

  /**
   * Creates a managed bean.
   */
  public <T> ManagedBeanImpl<T> createManagedBean(Class<T> cl)
  {
    AnnotatedType<T> type = createAnnotatedType(cl);
    
    AnnotatedType<T> extType = getExtensionManager().processAnnotatedType(type);
    
    if (extType != null)
      return createManagedBean(extType);
    else
      return createManagedBean(type);
  }

  /**
   * Processes the discovered bean
   */
  public <T> void addBean(Bean<T> bean)
  {
    if (bean == null)
      throw new NullPointerException(L.l("null bean passed to addBean"));
    
    addBean(bean, (Annotated) null);
  }
  
  /**
   * Processes the discovered bean
   */
  @Module
  public <T> void addBean(Bean<T> bean, Annotated ann)
  {
    if (ann == null && bean instanceof AbstractBean<?>)
      ann = ((AbstractBean<T>) bean).getAnnotatedType();
    
    if (bean instanceof ManagedBeanImpl<?>) {
      ManagedBeanImpl<T> managedBean = (ManagedBeanImpl<T>) bean;
      
      bean = getExtensionManager().processManagedBean(managedBean, ann);
    }
    else if (bean instanceof ProducesMethodBean<?,?>) {
      ProducesMethodBean<?,T> methodBean = (ProducesMethodBean<?,T>) bean;
      
      bean = getExtensionManager().processProducerMethod(methodBean);
    }
    else if (bean instanceof ProducesFieldBean<?,?>) {
      ProducesFieldBean<?,T> fieldBean = (ProducesFieldBean<?,T>) bean;
      
      bean = getExtensionManager().processProducerField(fieldBean);
    }
    else
      bean = getExtensionManager().processBean(bean, ann);
    
    addBeanImpl(bean, ann);
  }
  
  /**
   * Adds a new bean definition to the manager
   */
  public <T> void addBean(Bean<T> bean, ProcessBean<T> process)
  {
    bean = getExtensionManager().processBean(bean, process);

    if (bean != null)
      addBeanImpl(bean, process.getAnnotated());
  }
  /**
   * Adds a new bean definition to the manager
   */
  public <T> void addBeanImpl(Bean<T> bean, Annotated ann)
  {
    if (bean == null)
      return;

    if (log.isLoggable(Level.FINER))
      log.finer(this + " add bean " + bean);
    
    _isAfterValidationNeeded = true;

    _version.incrementAndGet();
    
    if (bean instanceof Interceptor<?>) {
      addInterceptor((Interceptor<?>) bean);
      return;
    }
    else if (bean instanceof Decorator<?>) {
      addDecorator((Decorator<?>) bean);
      return;
    }

    // bean = new InjectBean<T>(bean, this);

    _beanSet.add(bean);

    for (Type type : bean.getTypes()) {
      addBeanByType(type, ann, bean);
    }

    if (bean.getName() != null) {
      addBeanByName(bean.getName(), bean);
    }
    
    // XXX: required for TCK, although we use lazily
    boolean isNullable = bean.isNullable();

    if (bean instanceof PassivationCapable) {
      PassivationCapable pass = (PassivationCapable) bean;

      if (pass.getId() != null)
        _selfPassivationBeanMap.put(pass.getId(), bean);
    }

    registerJmx(bean);
  }
  
  public ResourceProgramManager getResourceManager()
  {
    return _resourceManager;
  }

  private void registerJmx(Bean<?> bean)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_classLoader);

      /*
      WebBeanAdmin admin = new WebBeanAdmin(bean, _beanId);

      admin.register();
      */
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns the bean definitions matching a given name
   *
   * @param name the name of the bean to match
   */
  public Set<Bean<?>> getBeans(String name)
  {
    ArrayList<Bean<?>> beanList = findByName(name);

    if (beanList != null)
      return new LinkedHashSet<Bean<?>>(beanList);
    else
      return new LinkedHashSet<Bean<?>>();
  }
  
  @Module
  public ReferenceFactory<?> getReferenceFactory(String name)
  {
    ReferenceFactory<?> refFactory = _namedRefFactoryMap.get(name);
    
    if (refFactory == null) {
      Set<Bean<?>> beanSet = getBeans(name);
      
      if (beanSet != null && beanSet.size() > 0) {
        Bean<?> bean = resolve(beanSet);
        refFactory = getReferenceFactory(bean);
        
        // ioc/0301
        if (refFactory instanceof DependentReferenceFactoryImpl<?>)
          refFactory = new DependentElReferenceFactoryImpl((ManagedBeanImpl<?>) bean);
      }
      else {
        refFactory = new UnresolvedReferenceFactory();
      }
      
      _namedRefFactoryMap.put(name, refFactory);
    }
    
    return refFactory;
  }

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param qualifiers required @Qualifier annotations
   */
  @Override
  public Set<Bean<?>> getBeans(Type type,
                               Annotation... qualifiers)
  {
    if (qualifiers != null) {
      for (int i = 0; i < qualifiers.length; i++) {
        for (int j = i + 1; j < qualifiers.length; j++) {
          if (qualifiers[i].annotationType() == qualifiers[j].annotationType())
            throw new IllegalArgumentException(L.l("getBeans may not have a duplicate qualifier '{0}'",
                                          qualifiers[i]));
        }
      }
    }
    
    Set<Bean<?>> set = resolve(type, qualifiers);

    if (set != null)
      return (Set<Bean<?>>) set;
    else
      return new HashSet<Bean<?>>();
  }

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param qualifiers required @Qualifier annotations
   */
  private Set<Bean<?>> getBeans(Type type,
                                Set<Annotation> qualifierSet)
  {
    Annotation []qualifiers = new Annotation[qualifierSet.size()];
    qualifierSet.toArray(qualifiers);
    
    return getBeans(type, qualifiers);
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  private Set<Bean<?>> resolve(Type type, Annotation []bindings)
  {
    if (type == null)
      throw new NullPointerException();
    
    if (bindings == null || bindings.length == 0) {
      if (Object.class.equals(type))
        return resolveAllBeans();

      bindings = DEFAULT_ANN;
    }

    BaseType baseType = createTargetBaseType(type);
    
    // ioc/024n
    /*
    if (baseType.isGeneric())
      throw new IllegalArgumentException(L.l("'{0}' is an invalid getBeans type because it's generic.",
                                    baseType));
                                    */
    
    // ioc/02b1
    if (baseType.isVariable())
      throw new IllegalArgumentException(L.l("'{0}' is an invalid getBeans type because it's a type variable.",
                                             baseType));

    return resolveRec(baseType, bindings);
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  private Set<Bean<?>> resolveRec(BaseType baseType,
                                  Annotation []qualifiers)
  {
    WebComponent component = getWebComponent(baseType);
    
    if (component != null) {
      Set<Bean<?>> beans = component.resolve(baseType, qualifiers);

      if (beans != null && beans.size() > 0) {
        if (log.isLoggable(Level.FINEST))
          log.finest(this + " bind(" + baseType.getSimpleName()
                     + "," + toList(qualifiers) + ") -> " + beans);

        return beans;
      }
    }
    
    if (New.class.equals(qualifiers[0].annotationType())) {
      // ioc/0721
      New newQualifier = (New) qualifiers[0];
      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      Class<?> newClass = newQualifier.value();
      
      if (newClass == null || newClass.equals(void.class))
        newClass = baseType.getRawClass();
      
      AnnotatedType<?> ann = ReflectionAnnotatedFactory.introspectType(newClass);
      NewBean<?> newBean = new NewBean(this, baseType.getRawClass(), ann);
      newBean.introspect();
      
      if (component != null) {
        component.addComponent(baseType, null, newBean);
      }

      set.add(newBean);

      return set;
    }

    Class<?> rawType = baseType.getRawClass();

    if (Instance.class.equals(rawType)) {
      BaseType []param = baseType.getParameters();

      Type beanType;
      if (param.length > 0)
        beanType = param[0].getRawClass();
      else
        beanType = Object.class;

      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      set.add(new InstanceBeanImpl(this, beanType, qualifiers));
      return set;
    }
    else if (Event.class.equals(rawType)) {
      if (baseType.isGenericRaw())
        throw new InjectionException(L.l("Event must have parameters because a non-parameterized Event would observe no events."));
                                      
      BaseType []param = baseType.getParameters();

      Type beanType;
      if (param.length > 0)
        beanType = param[0].getRawClass();
      else
        beanType = Object.class;
      
      HashSet<Annotation> qualifierSet = new LinkedHashSet<Annotation>();
      
      for (Annotation ann : qualifiers) {
        qualifierSet.add(ann);
      }
      
      qualifierSet.add(AnyLiteral.ANY);

      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      set.add(new EventBeanImpl(this, beanType, qualifierSet));
      return set;
    }

    if (_parent != null) {
      return _parent.resolveRec(baseType, qualifiers);
    }

    for (Annotation ann : qualifiers) {
      if (! ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        throw new IllegalArgumentException(L.l("'{0}' is an invalid binding annotation because it does not have a @Qualifier meta-annotation",
                                               ann));
      }
    }

    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " bind(" + baseType.getSimpleName()
                + "," + toList(qualifiers) + ") -> none");
    }

    return null;
  }


  /**
   * Returns the web beans component with a given binding list.
   */
  public Set<Bean<?>> resolveAllByType(Class<?> type)
  {
    Annotation []bindings = new Annotation[0];

    WebComponent component = getWebComponent(createTargetBaseType(type));

    if (component != null) {
      Set<Bean<?>> beans = component.resolve(type, bindings);

      if (log.isLoggable(Level.FINEST))
        log.finest(this + " bind(" + getSimpleName(type)
                  + "," + toList(bindings) + ") -> " + beans);

      if (beans != null && beans.size() > 0)
        return beans;
    }

    if (_parent != null) {
      return _parent.resolveAllByType(type);
    }

    return null;
  }

  private WebComponent getWebComponent(BaseType baseType)
  {
    if (_beanMap == null)
      return null;

    WebComponent beanSet = _beanMap.get(baseType.getRawClass());

    if (beanSet == null) {
      HashSet<TypedBean> typedBeans = new HashSet<TypedBean>();

      if (_classLoader != null) {
        FillByType fillByType = new FillByType(baseType, typedBeans, this);

        _classLoader.applyVisibleModules(fillByType);
      }
      
      Class<?> rawClass = baseType.getRawClass();
      
      beanSet = new WebComponent(this, rawClass);
      _beanMap.put(rawClass, beanSet);
      
      for (TypedBean typedBean : typedBeans) {
        if (getDeploymentPriority(typedBean.getBean()) < 0) {
          continue;
        }
        
        _pendingValidationBeans.add(typedBean.getBean());
        
        beanSet.addComponent(typedBean.getType(),
                             typedBean.getAnnotated(),
                             typedBean.getBean());
      }
    }

    return beanSet;
  }

  private void fillByType(BaseType baseType,
                          HashSet<TypedBean> beanSet,
                          InjectManager beanManager)
  {
    Class<?> rawClass = baseType.getRawClass();
    
    InjectScanClass scanClass
      = _scanManager.getScanClass(rawClass.getName());
    
    if (scanClass != null && ! scanClass.isRegistered()) {
      discoverScanClass(scanClass);
      processPendingAnnotatedTypes();
    }
      
    ArrayList<TypedBean> localBeans = _selfBeanMap.get(rawClass);
    
    if (localBeans != null) {
      // ioc/0k00, ioc/0400 - XXX: not exactly right.  want local beans to have
      // priority if type and binding match
      /*
      if (this == beanManager)
        beanSet.clear();
      else if (beanSet.size() > 0) {
        return;
      }
      */

      for (TypedBean bean : localBeans) {
        if (getDeploymentPriority(bean.getBean()) < 0)
          continue;

        if (bean.isModulePrivate() && this != beanManager)
          continue;

        beanSet.add(bean);
      }
    }
  }

  @Override
  public <X> Bean<X> getMostSpecializedBean(Bean<X> bean)
  {
    throw new UnsupportedOperationException();
    /*
    Bean<?> special = _specializedMap.get(bean.getBeanClass());
    
    if (special != null)
      return (Bean<X>) special;
    else
      return bean;
      */
  }
  
  @Module
  public boolean isSpecialized(Class<?> beanClass)
  {
    return _specializedMap.get(beanClass) != null;
  }

  public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans)
  {
    Bean<? extends X> bestBean = null;
    Bean<? extends X> secondBean = null;
    
    int bestPriority = -1;
    boolean isSpecializes = false;

    for (Bean<? extends X> bean : beans) {
      if (_specializedMap.get(bean.getBeanClass()) != null)
        continue;
      
      if ((bean instanceof AbstractIntrospectedBean<?>)
          && ((AbstractIntrospectedBean<?>) bean).getAnnotated().isAnnotationPresent(Specializes.class)) {
        if (! isSpecializes) {
          // ioc/07a3
          
          bestPriority = -1;
          bestBean = null;
          secondBean = null;
          isSpecializes = true;
        }
      }
      else if (isSpecializes)
        continue;
      
      int priority = getDeploymentPriority(bean);

      if (bestPriority < priority) {
        bestBean = bean;
        secondBean = null;
        
        bestPriority = priority;
      }
      else if (bestPriority == priority) {
        secondBean = bean;

        // TCK: ProducerFieldDefinitionTest
        boolean isFirstProduces = (bestBean instanceof ProducesMethodBean<?,?>
                                   || bestBean instanceof ProducesFieldBean<?,?>);
        boolean isSecondProduces = (secondBean instanceof ProducesMethodBean<?,?>
                                    || secondBean instanceof ProducesFieldBean<?,?>);
        
        // ioc/02b0
        if (isFirstProduces && ! isSecondProduces) {
          secondBean = null;
        }
        else if (isSecondProduces && ! isFirstProduces) {
          bestBean = bean;
          secondBean = null;
        }
      }
    }

    if (secondBean == null)
      return bestBean;
    else {
      throw ambiguousException(beans, bestPriority);
    }
  }
  
  private void validate(Type type)
  {
    BaseType baseType = createTargetBaseType(type);
    
    WebComponent comp = getWebComponent(baseType);
  }

  private void validate(Bean<?> bean)
  {
    if (bean.isAlternative() && ! isEnabled(bean))
      return;
    
    boolean isPassivating = isPassivatingScope(bean.getScope());
    
    if (bean instanceof InjectEnvironmentBean) {
      InjectEnvironmentBean envBean = (InjectEnvironmentBean) bean;
      
      if (envBean.getCdiManager() != this) {
        envBean.getCdiManager().validate(bean);
        return;
      }
    }
    
    if (bean instanceof CdiStatefulBean)
      isPassivating = true;

    for (InjectionPoint ip : bean.getInjectionPoints()) {
      ReferenceFactory<?> factory = validateInjectionPoint(ip);
      
      if (ip.isDelegate() && ! (bean instanceof Decorator))
        throw new ConfigException(L.l("'{0}' is an invalid delegate because {1} is not a Decorator.",
                                      ip.getMember().getName(),
                                      bean));
      
      RuntimeException exn = validatePassivation(ip);
      
      if (exn != null && ! factory.isProducer())
        throw exn;
    }
    
    if (isNormalScope(bean.getScope())) {
      validateNormal(bean);
    }
  }
  
  private RuntimeException validatePassivation(InjectionPoint ip)
  {
    Bean<?> bean = ip.getBean();
    
    if (bean == null)
      return null;
    
    boolean isPassivating = isPassivatingScope(bean.getScope());
    
    if (bean instanceof CdiStatefulBean
        || bean.getBeanClass().isAnnotationPresent(Stateful.class)) {
      isPassivating = true;
    }
    
    if (isPassivating && ! ip.isTransient()) {
      Class<?> cl = getRawClass(ip.getType());
      
      Bean<?> prodBean = resolve(getBeans(ip.getType(), ip.getQualifiers()));
    
      // TCK conflict
      if (! cl.isInterface()
          && ! Serializable.class.isAssignableFrom(cl)
          && ! isNormalScope(prodBean.getScope())) {
        RuntimeException exn;
        
        if (isProduct(prodBean))
          exn = new IllegalProductException(L.l("'{0}' is an invalid injection point of type {1} because it's not serializable for {2}",
                                                ip.getMember().getName(),
                                                ip.getType(),
                                                bean));
        else
          exn = new ConfigException(L.l("'{0}' is an invalid injection point of type {1} because it's not serializable for {2}",
                                        ip.getMember().getName(),
                                        ip.getType(),
                                        bean));
        
        return exn;
      }
    }
    
    return null;
  }
  
  private boolean isProduct(Bean<?> bean)
  {
    return ((bean instanceof ProducesFieldBean<?,?>)
            || (bean instanceof ProducesMethodBean<?,?>));
  }
  
  private Class<?> getRawClass(Type type)
  {
    if (type instanceof Class<?>)
      return (Class<?>) type;
    
    BaseType baseType = createSourceBaseType(type);
    
    return baseType.getRawClass();
  }
  
  private void validateNormal(Bean<?> bean)
  {
    Annotated ann = null;
    
    if (bean instanceof AbstractBean) {
      AbstractBean absBean = (AbstractBean) bean;
      
      ann = absBean.getAnnotated();
    }
    
    if (ann == null)
      return;
    
    Type baseType = ann.getBaseType();
    
    Class<?> cl = createTargetBaseType(baseType).getRawClass();
    
    if (cl.isInterface())
      return;
    
    int modifiers = cl.getModifiers();
    
    if (Modifier.isFinal(modifiers)) {
      throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because it's a final class, for {2}.",
                                    cl.getSimpleName(), bean.getScope().getSimpleName(),
                                    bean));
    }
    
    Constructor<?> ctor = null;
    
    for (Constructor<?> ctorPtr : cl.getDeclaredConstructors()) {
      if (ctorPtr.getParameterTypes().length > 0)
        continue;
      
      if (Modifier.isPrivate(ctorPtr.getModifiers())) {
        throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because its constructor is private for {2}.",
                                      cl.getSimpleName(), bean.getScope().getSimpleName(),
                                      bean));

      }
      
      ctor = ctorPtr;
    }
    
    if (ctor == null) {
      throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because it doesn't have a zero-arg constructor for {2}.",
                                    cl.getName(), bean.getScope().getSimpleName(),
                                    bean));

    }
    
    
    for (Method method : cl.getMethods()) {
      if (method.getDeclaringClass() == Object.class)
        continue;
      
      if (Modifier.isFinal(method.getModifiers())) {
        throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because {2} is a final method for {3}.",
                                      cl.getSimpleName(), bean.getScope().getSimpleName(),
                                      method.getName(),
                                      bean));
      
      }
    }
    
    for (Field field : cl.getFields()) {
      if (Modifier.isStatic(field.getModifiers()))
        continue;
      
      if (Modifier.isPublic(field.getModifiers())) {
        throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because {2} is a public field for {3}.",
                                      cl.getSimpleName(), bean.getScope().getSimpleName(),
                                      field.getName(),
                                      bean));
      }
    }
    
    for (InjectionPoint ip : bean.getInjectionPoints()) {
      if (ip.getType().equals(InjectionPoint.class))
        throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because '{2}' injects an InjectionPoint for {3}.",
                                      cl.getSimpleName(), bean.getScope().getSimpleName(),
                                      ip.getMember().getName(),
                                      bean));
      
    }
  }
  
  @Override
  public void validate(InjectionPoint ij)
  {
    validateInjectionPoint(ij);
  }
  
  public ReferenceFactory<?> validateInjectionPoint(InjectionPoint ij)
  {
    try {
      if (ij.isDelegate()) {
        if (! (ij.getBean() instanceof Decorator<?>))
          throw new ConfigException(L.l("'{0}' is an invalid @Delegate because {1} is not a decorator",
                                        ij.getMember().getName(), ij.getBean()));
      }
      else {
        return getReferenceFactory(ij);
      }
    } catch (AmbiguousResolutionException e) {
      throw new AmbiguousResolutionException(L.l("{0}.{1}: {2}",
                                       ij.getMember().getDeclaringClass().getName(),
                                       ij.getMember().getName(),
                                       e.getMessage()),
                                   e);
    } catch (UnsatisfiedResolutionException e) {
      throw new UnsatisfiedResolutionException(L.l("{0}.{1}: {2}",
                                                   ij.getMember().getDeclaringClass().getName(),
                                                   ij.getMember().getName(),
                                                   e.getMessage()),
                                   e);
    } catch (IllegalProductException e) {
      throw new IllegalProductException(L.l("{0}.{1}: {2}",
                                            ij.getMember().getDeclaringClass().getName(),
                                            ij.getMember().getName(),
                                            e.getMessage()),
                                   e);
    } catch (Exception e) {
      throw new InjectionException(L.l("{0}.{1}: {2}",
                                       ij.getMember().getDeclaringClass().getName(),
                                       ij.getMember().getName(),
                                       e.getMessage()),
                                   e);
    }
    
    return null;
  }

  public int getDeploymentPriority(Bean<?> bean)
  {
    int priority = DEFAULT_PRIORITY;

    Set<Class<? extends Annotation>> stereotypes = bean.getStereotypes();
    
    if (bean.isAlternative()) {
      priority = -1;
      
      Integer value = _deploymentMap.get(bean.getBeanClass());
      
      if (value != null)
        priority = value;
    }

    if (stereotypes != null) {
      for (Class<? extends Annotation> annType : stereotypes) {
        Integer value = _deploymentMap.get(annType);
                                           
        if (value != null) {
          if (priority < value)
            priority = value;
        }
        else if (annType.isAnnotationPresent(Alternative.class)
                 && priority == DEFAULT_PRIORITY)
          priority = -1;
      }
    }

    if (priority < 0)
      return priority;
    else if (bean instanceof AbstractBean<?>) {
      // ioc/0213
      AbstractBean<?> absBean = (AbstractBean<?>) bean;

      if (absBean.getBeanManager() == this)
        priority += 1000000;
    }
    else
      priority += 1000000;

    return priority;
  }

  private Set<Bean<?>> resolveAllBeans()
  {
    synchronized (_beanMap) {
      LinkedHashSet<Bean<?>> beans = new LinkedHashSet<Bean<?>>();

      for (ArrayList<TypedBean> comp : _selfBeanMap.values()) {
        for (TypedBean typedBean : comp) {
          beans.add(typedBean.getBean());
        }
      }

      return beans;
    }
  }

  @Override
  public <T> CreationalContext<T> createCreationalContext(Contextual<T> bean)
  {
    return new OwnerCreationalContext<T>(bean);
  }

  /**
   * Convenience-class for Resin.
   */
  public <T> T getReference(Class<T> type, Annotation... bindings)
  {
    Set<Bean<?>> beans = getBeans(type);
    Bean<T> bean = (Bean<T>) resolve(beans);

    if (bean == null)
      return null;

    return getReference(bean);
  }

  /**
   * Convenience for Resin.
   */
  public <T> T getReference(Bean<T> bean)
  {
    ReferenceFactory<T> factory = getReferenceFactory(bean);
    
    if (factory != null)
      return factory.create(null, null, null);
    else
      return null;
  }

  /**
   * Convenience for Resin.
   */
  public <T> T getReference(Bean<T> bean, CreationalContextImpl<?> parentEnv)
  {
    ReferenceFactory<T> factory = getReferenceFactory(bean);
    
    return factory.create(null, parentEnv, null);
  }

  /**
   * Convenience-class for Resin.
   */
  public <T> T getReference(String name)
  {
    Set<Bean<?>> beans = getBeans(name);
    Bean<T> bean = (Bean<T>) resolve(beans);

    if (bean == null)
      return null;

    ReferenceFactory<T> factory = getReferenceFactory(bean);
    
    return factory.create(null, null, null);
  }

  /**
   * Convenience-class for Resin.
   */
  public <T> T getReference(String name, CreationalContextImpl parentEnv)
  {
    Set<Bean<?>> beans = getBeans(name);
    Bean<T> bean = (Bean<T>) resolve(beans);

    if (bean == null)
      return null;

    ReferenceFactory<T> factory = getReferenceFactory(bean);

    return factory.create(null, parentEnv, null);
  }

  /**
   * Returns an instance for the given bean.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param bean the metadata for the bean
   *
   * @return an instance of the bean obeying scope
   */
  @Override
  public Object getReference(Bean<?> bean,
                             Type type,
                             CreationalContext<?> createContext)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(getClassLoader());
      
      ReferenceFactory factory = getReferenceFactory(bean);
      
      if (createContext instanceof CreationalContextImpl<?>)
        return factory.create((CreationalContextImpl) createContext, null, null);
      else
        return factory.create(null, null, null);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Used by ScopeProxy
   */
  private <T> T getInstanceForProxy(Bean<T> bean)
  {
    CreationalContextImpl<?> oldEnv = _proxyThreadLocal.get();
  
    T value;
    
    if (oldEnv != null) {
      value = oldEnv.get(bean);
      
      if (value != null)
        return value;
    }
    
    try {
      CreationalContextImpl<T> env = new OwnerCreationalContext(bean, oldEnv);
      
      _proxyThreadLocal.set(env);

      value = bean.create(env);
      
      return value;
    } finally {
      _proxyThreadLocal.set(oldEnv);
    }
  }

  public <T> ReferenceFactory<T> getReferenceFactory(Bean<T> bean)
  {
    if (bean == null)
      return null;
    
    ReferenceFactory<T> factory = (ReferenceFactory<T>) _refFactoryMap.get(bean);
    
    if (factory == null) {
      factory = createReferenceFactory(bean);
      _refFactoryMap.put(bean, factory);
    }
    
    return factory;
  }
  
  private <T> ReferenceFactory<T> createReferenceFactory(Bean<T> bean)
  {
    Class<? extends Annotation> scopeType = bean.getScope();
    
    if (InjectionPoint.class.equals(bean.getBeanClass()))
      return (ReferenceFactory) new InjectionPointReferenceFactory();

    if (Dependent.class == scopeType) {
      if (bean instanceof ManagedBeanImpl<?>)
        return new DependentReferenceFactoryImpl<T>((ManagedBeanImpl<T>) bean);
      else
        return new DependentReferenceFactory<T>(bean);
    }

    if (scopeType == null) {
      throw new IllegalStateException("Unknown scope for " + bean);
    }

    InjectManager ownerManager;

    if (bean instanceof AbstractBean<?>)
      ownerManager = ((AbstractBean<?>) bean).getBeanManager();
    else
      ownerManager = this;

    Context context = ownerManager.getContextImpl(scopeType);

    /*
    if (context == null)
      return null;
      */
    if (context == null)
      throw new InjectionException(L.l("Bean has an unknown scope '{0}' for bean {1}",
                                       scopeType, bean));
    
    if (isNormalScope(scopeType) && bean instanceof ScopeAdapterBean<?>) {
      ScopeAdapterBean<T> scopeAdapterBean = (ScopeAdapterBean<T>) bean;
      
      return new NormalContextReferenceFactory<T>(bean, scopeAdapterBean, context);
    }
    else
      return new ContextReferenceFactory<T>(bean, context);
  }
  
  public <T> ReferenceFactory<T> createNormalInstanceFactory(Bean<T> bean)
  {
    Class<? extends Annotation> scopeType = bean.getScope();

    if (! isNormalScope(scopeType)) {
      throw new IllegalStateException(L.l("{0} is an invalid normal scope for {1}",
                                          scopeType, bean));
    }

    InjectManager ownerManager;

    if (bean instanceof AbstractBean<?>)
      ownerManager = ((AbstractBean<?>) bean).getBeanManager();
    else
      ownerManager = this;

    Context context = ownerManager.getContextImpl(scopeType);

    if (context == null)
      throw new InjectionException(L.l("Bean has an unknown scope '{0}' for bean {1}",
                                       scopeType, bean));

    return new NormalInstanceReferenceFactory<T>(bean, context);
  }

  public RuntimeException unsatisfiedException(Type type,
                                               Annotation []qualifiers)
  {
    WebComponent component = getWebComponent(createTargetBaseType(type));

    if (component == null) {
      throw new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans implementing that class have been registered with the injection manager {1}.",
                                                   type, this));
    }
    else {
      ArrayList<Bean<?>> enabledList = component.getEnabledBeanList();

      if (enabledList.size() == 0) {
        throw new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans implementing that class have been registered with the injection manager {1}.",
                                                     type, this));
      }
      else {
        return new UnsatisfiedResolutionException(L.l("Can't find a bean for '{0}' because no beans match the type and qualifiers {1}.\nBeans:{2}",
                                                      type,
                                                      toList(qualifiers),
                                                      listToLines(enabledList)));
      }
    }
  }

  private String listToLines(List<?> list)
  {
    StringBuilder sb = new StringBuilder();

    ArrayList<String> lines = new ArrayList<String>();

    for (int i = 0; i < list.size(); i++) {
      lines.add(list.get(i).toString());
    }

    Collections.sort(lines);

    for (String line : lines) {
      sb.append("\n    ").append(line);
    }

    return sb.toString();
  }

  /**
   * Convert an annotation array to a list for debugging purposes
   */
  private ArrayList<Annotation> toList(Annotation []annList)
  {
    ArrayList<Annotation> list = new ArrayList<Annotation>();

    if (annList != null) {
      for (Annotation ann : annList) {
        list.add(ann);
      }
    }

    return list;
  }

  InjectionPointHandler getInjectionPointHandler(AnnotatedField<?> field)
  {
    // InjectIntrospector.introspect(_injectProgramList, field);

    for (Annotation ann : field.getAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();

      InjectionPointHandler handler = _injectionMap.get(annType);

      if (handler != null) {
        return handler;
      }
    }

    return null;
  }

  InjectionPointHandler getInjectionPointHandler(AnnotatedMethod<?> method)
  {
    // InjectIntrospector.introspect(_injectProgramList, field);

    for (Annotation ann : method.getAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();

      InjectionPointHandler handler = _injectionMap.get(annType);

      if (handler != null) {
        return handler;
      }
    }

    return null;
  }
  
  InjectionPointHandler 
  getInjectionPointHandler(Class<? extends Annotation> annType)
  {
    return _injectionMap.get(annType);
  }

  /**
   * Internal callback during creation to get a new injection instance.
   */
  @Override
  public Object getInjectableReference(InjectionPoint ij,
                                       CreationalContext<?> parentCxt)
  {
    CreationalContextImpl<?> parentEnv = null;
    
    if (parentCxt instanceof CreationalContextImpl<?>)
      parentEnv = (CreationalContextImpl<?>) parentCxt;
    
    if (InjectionPoint.class.equals(ij.getType())) {
      if (parentEnv != null) {
        return parentEnv.findInjectionPoint();
      }
    }
    
    ReferenceFactory<?> factory = getReferenceFactory(ij);
    
    return factory.create(null, parentEnv, ij);
  }

  public ReferenceFactory<?> getReferenceFactory(InjectionPoint ij)
  {
    if (ij.isDelegate())
      return new DelegateReferenceFactory();
    else if (ij.getType().equals(InjectionPoint.class))
      return new InjectionPointReferenceFactory();
    
    Type type = ij.getType();
    Set<Annotation> qualifiers = ij.getQualifiers();

    ReferenceFactory factory = getReferenceFactory(type, qualifiers, ij);
    
    RuntimeException exn = validatePassivation(ij);
    
    if (exn != null) {
      if (factory.isProducer())
        return new ErrorReferenceFactory(exn);
      else
        throw exn;
    }
    
    return factory;
  }

  public ReferenceFactory<?> getReferenceFactory(Type type,
                                                 Set<Annotation> qualifiers,
                                                 InjectionPoint ij)
  {
    if (ij != null && ij.isDelegate())
      return new DelegateReferenceFactory();
    
    Bean<?> bean = resolveByInjectionPoint(type, qualifiers, ij);
    
    return getReferenceFactory(bean);
  }

  private Bean<?> resolveByInjectionPoint(Type type,
                                          Set<Annotation> qualifierSet,
                                          InjectionPoint ij)
  {
    Annotation []qualifiers;

    if (qualifierSet != null && qualifierSet.size() > 0) {
      qualifiers = new Annotation[qualifierSet.size()];
      qualifierSet.toArray(qualifiers);

      if (qualifiers.length == 1
          && qualifiers[0].annotationType().equals(New.class)) {
        New newQualifier = (New) qualifiers[0];
        
        return createNewBean(type, newQualifier);
      }
    }
    else
      qualifiers = new Annotation[] { DefaultLiteral.DEFAULT };
    
    BaseType baseType = createTargetBaseType(type);
    
    /*
    if (baseType.isGeneric())
      throw new InjectionException(L.l("'{0}' is an invalid type for injection because it's generic. {1}",
                                       baseType, ij));
                                       */
    if (baseType.isGenericVariable())
      throw new InjectionException(L.l("'{0}' is an invalid type for injection because it's a variable . {1}",
                                       baseType, ij));

    Set<Bean<?>> set = resolveRec(baseType, qualifiers);

    if (set == null || set.size() == 0) {
      if (InjectionPoint.class.equals(type))
        return new InjectionPointBean(this, ij);
      
      throw unsatisfiedException(type, qualifiers);
    }

    Bean<?> bean = resolve(set);

    if (bean != null 
        && type instanceof Class<?>
        && ((Class<?>) type).isPrimitive()
        && bean.isNullable()) {
      throw new InjectionException(L.l("'{0}' cannot be injected because it's a primitive with {1}",
                                       type, bean));                               
    }
    
    return bean;

    /*
    else if (set.size() == 1) {
      Iterator iter = set.iterator();

      if (iter.hasNext()) {
        Bean bean = (Bean) iter.next();

        return bean;
      }
    }
    else {
      throw new AmbiguousResolutionException(L.l("'{0}' with binding {1} matches too many configured beans{2}",
                                                 BaseType.create(type, null),
                                                 bindingSet,
                                                 toLineList(set)));
    }

    return null;
*/
  }

  private <T> Bean<?> createNewBean(Type type, New newQualifier)
  {
    Class<?> newClass = newQualifier.value();
    
    if (newClass == null 
        || void.class.equals(newClass)
        || New.class.equals(newClass)) {
      BaseType baseType = createTargetBaseType(type);
      newClass = (Class<T>) baseType.getRawClass();
    }
      
    Bean<?> bean = _newBeanMap.get(newClass);

    if (bean == null) {
      AnnotatedType<T> annType = (AnnotatedType<T>) ReflectionAnnotatedFactory.introspectType(newClass);
      
      BaseType newType = createSourceBaseType(type);

      NewBean<T> newBean = new NewBean<T>(this, newType.getRawClass(), annType);
      newBean.introspect();

      _newBeanMap.put(type, bean);
      bean = newBean;
    }

    return bean;
  }

  private <X> AmbiguousResolutionException
    ambiguousException(Set<Bean<? extends X>> beanSet, int bestPriority)
  {
    ArrayList<Bean<?>> matchBeans = new ArrayList<Bean<?>>();

    for (Bean<?> bean : beanSet) {
      int priority = getDeploymentPriority(bean);

      if (priority == bestPriority)
        matchBeans.add(bean);
    }

    return new AmbiguousResolutionException(L.l("Too many beans match, because they all have equal precedence.  See the @Stereotype and <enable> tags to choose a precedence.  Beans:{0}\nfor {1}",
                                                listToLines(matchBeans), this));
  }

  @Override
  public ELResolver getELResolver()
  {
    return _elResolver;
  }

  @Override
  public ExpressionFactory
    wrapExpressionFactory(ExpressionFactory expressionFactory)
  {
    return new CandiExpressionFactory(expressionFactory);
  }

  //
  // scopes
  //

  /**
   * Adds a new scope context
   */
  public void addContext(Context context)
  {
    Class<? extends Annotation> scopeType = context.getScope();
    
    Context oldContext = _contextMap.get(scopeType);
    
    if (oldContext == null) {
      _contextMap.put(context.getScope(), context);
    }
    else {
      // ioc/0p41 - CDI TCK
      
      RuntimeException exn
        = new IllegalStateException(L.l("{0} is an invalid new context because @{1} is already registered as a scope",
                                        context, scopeType.getName()));
                                        
      _contextMap.put(context.getScope(), new ErrorContext(exn, context));
    }
  }
  
  public void replaceContext(Context context)
  {
    _contextMap.put(context.getScope(), context);
  }

  /**
   * Returns the scope context for the given type
   */
  @Override
  public Context getContext(Class<? extends Annotation> scopeType)
  {
    Context context = _contextMap.get(scopeType);

    if (context != null && context.isActive()) {
      return context;
    }
    
    if (context instanceof ErrorContext) {
      ErrorContext cxt = (ErrorContext) context;
      
      throw cxt.getException();
    }
    
    /*
    if (! isScope(scopeType)) {
      throw new IllegalStateException(L.l("'@{0}' is not a valid scope because it does not have a @Scope annotation",
                                          scopeType));
    }
    */
    
    throw new ContextNotActiveException(L.l("'@{0}' is not an active Java Injection context.",
                                            scopeType.getName()));
  }

  /**
   * Required for TCK. Returns the scope context for the given type.
   */
  public Context getContextImpl(Class<? extends Annotation> scopeType)
  {
    return _contextMap.get(scopeType);
  }

  /**
   * Returns the bean for the given passivation id.
   */
  public Bean<?> getPassivationCapableBean(String id)
  {
    return _selfPassivationBeanMap.get(id);
  }

  public Annotation []getQualifiers(Set<Annotation> annotations)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        bindingList.add(ann);
    }

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
  }

  public Annotation []getQualifiers(Annotation []annotations)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        bindingList.add(ann);
    }

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
  }

  /**
   * Sends the specified event to any observer instances in the scope
   */
  @Override
  public void fireEvent(Object event, Annotation... qualifiers)
  {
    if (log.isLoggable(Level.FINEST))
      log.finest(this + " fireEvent " + event);

    getEventManager().fireEvent(event, qualifiers);
  }

  /**
   * Returns the observers listening for an event
   *
   * @param eventType event to resolve
   * @param bindings the binding set for the event
   */
  @Override
  public <T> Set<ObserverMethod<? super T>>
  resolveObserverMethods(T event, Annotation... qualifiers)
  {
    return getEventManager().resolveObserverMethods(event, qualifiers);
  }

  //
  // interceptor support
  //

  /**
   * Adds a new decorator class
   */
  public <X> BeanManager addInterceptorClass(Class<?> interceptorClass)
  {
    _interceptorClassList.add(interceptorClass);

    return this;
  }

  /**
   * Adds a new interceptor to the manager
   */
  public <X> InterceptorEntry<X> addInterceptor(Interceptor<X> interceptor)
  {
    InterceptorEntry<X> entry = new InterceptorEntry<X>(interceptor);
    
    _interceptorList.add(entry);
    
    return entry;
  }

  /**
   * Resolves the interceptors for a given interceptor type
   *
   * @param type the main interception type
   * @param qualifiers qualifying bindings
   *
   * @return the matching interceptors
   */
  @Override
  public List<Interceptor<?>> resolveInterceptors(InterceptionType type,
                                                  Annotation... qualifiers)
  {
    if (qualifiers == null || qualifiers.length == 0)
      throw new IllegalArgumentException(L.l("resolveInterceptors requires at least one @InterceptorBinding"));
    
    for (int i = 0; i < qualifiers.length; i++) {
      Class<? extends Annotation> annType = qualifiers[i].annotationType();
      
      if (! annType.isAnnotationPresent(InterceptorBinding.class))
        throw new IllegalArgumentException(L.l("Annotation must be an @InterceptorBinding at '{0}' in resolveInterceptors",
                                               qualifiers[i]));
        
      for (int j = i + 1; j < qualifiers.length; j++) {
        if (annType.equals(qualifiers[j].annotationType()))
          throw new IllegalArgumentException(L.l("Duplicate binding '{0}' is not allowed in resolveInterceptors",
                                                 qualifiers[i]));
      }
    }

    ArrayList<Interceptor<?>> interceptorList
      = new ArrayList<Interceptor<?>>();

    for (InterceptorEntry<?> entry : _interceptorList) {
      Interceptor<?> interceptor = entry.getInterceptor();

      if (! interceptor.intercepts(type)) {
        continue;
      }

      if (entry.isMatch(qualifiers)) {
        interceptorList.add(interceptor);
      }
    }

    return interceptorList;
  }

  //
  // decorator
  //

  /**
   * Adds a new decorator
   */
  private <X> DecoratorEntry<X> addDecorator(Decorator<X> decorator)
  {
    BaseType baseType = createTargetBaseType(decorator.getDelegateType());

    DecoratorEntry<X> entry = new DecoratorEntry<X>(this, decorator, baseType);
    
    _decoratorList.add(entry);

    return entry;
  }

  /**
   * Adds a new decorator class
   */
  public <X> BeanManager addDecoratorClass(Class<?> decoratorClass)
  {
    _decoratorClassList.add(decoratorClass);

    return this;
  }
  
  /**
   * Called by the generated code.
   */
  public List<Decorator<?>> resolveDecorators(Class<?> type)
  {
    HashSet<Type> types = new HashSet<Type>();                                  
    types.add(type);                                                            

    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();            

    boolean isQualifier = false;                                                

    for (Annotation ann : type.getAnnotations()) {
      if (isQualifier(ann.annotationType())) {
        bindingList.add(ann);                                                   

        if (! Named.class.equals(ann.annotationType())) {
          isQualifier = true;                                                   
        }
      }
    }

    if (! isQualifier)
      bindingList.add(DefaultLiteral.DEFAULT);                                  
    bindingList.add(AnyLiteral.ANY);                                            

    Annotation []bindings = new Annotation[bindingList.size()];                 
    bindingList.toArray(bindings);                                              

    List<Decorator<?>> decorators = resolveDecorators(types, bindings);
    
    // XXX: 4.0.7
    // log.info("DECORATORS: " + decorators + " " + types + " " + this);
    
    return decorators;
  }

  /**
   * Resolves the decorators for a given set of types
   *
   * @param types the types to match for the decorator
   * @param qualifiers qualifying bindings
   *
   * @return the matching interceptors
   */
  @Override
  public List<Decorator<?>> resolveDecorators(Set<Type> types,
                                              Annotation... qualifiers)
  {
    if (types.size() == 0)
      throw new IllegalArgumentException(L.l("type set must contain at least one type"));
    
    if (qualifiers != null) {
      for (int i = 0; i < qualifiers.length; i++) {
        for (int j = i + 1; j < qualifiers.length; j++) {
          if (qualifiers[i].annotationType() == qualifiers[j].annotationType())
            throw new IllegalArgumentException(L.l("resolveDecorators may not have a duplicate qualifier '{0}'",
                                          qualifiers[i]));
        }
      }
    }

    ArrayList<Decorator<?>> decorators = new ArrayList<Decorator<?>>();

    if (qualifiers == null || qualifiers.length == 0)
      qualifiers = DEFAULT_ANN;

    if (_decoratorList == null)
      return decorators;
    
    for (Annotation ann : qualifiers) {
      if (! isQualifier(ann.annotationType()))
        throw new IllegalArgumentException(L.l("@{0} must be a qualifier", ann.annotationType()));
    }
    
    ArrayList<BaseType> targetTypes = new ArrayList<BaseType>();
    
    for (Type type : types) {
      targetTypes.add(createSourceBaseType(type));
    }

    for (DecoratorEntry<?> entry : _decoratorList) {
      Decorator<?> decorator = entry.getDecorator();
      
      // XXX: delegateTypes
      if (isDelegateAssignableFrom(entry.getDelegateType(), targetTypes)
          && entry.isMatch(qualifiers)) {
        decorators.add(decorator);
      }
    }

    return decorators;
  }

  private boolean isDelegateAssignableFrom(BaseType delegateType,
                                           ArrayList<BaseType> sourceTypes)
  {
    for (BaseType sourceType : sourceTypes) {
      if (delegateType.isAssignableFrom(sourceType)) {
        return true;
      }
    }

    return false;
  }

  public void addConfiguredClass(String className)
  {
    _xmlExtension.addConfiguredBean(className);
//    _configuredClasses.add(className);
  }
  
  public XmlCookie generateXmlCookie()
  {
    return new XmlCookieLiteral(_xmlCookieSequence.incrementAndGet());
  }

  public void addLoader()
  {
    _isUpdateNeeded = true;
  }

  public void update()
  {
    if (! _isUpdateNeeded 
        && ! _scanManager.isPending()
        && _pendingAnnotatedTypes.size() == 0) {
      return;
    }

    _isUpdateNeeded = false;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      _extensionManager.updateExtensions();

      ArrayList<ScanRootContext> rootContextList
        = _scanManager.getPendingScanRootList();

      for (ScanRootContext context : rootContextList) {
        _xmlExtension.addRoot(context.getRoot());
      }

      _isBeforeBeanDiscoveryComplete = true;
      getExtensionManager().fireBeforeBeanDiscovery();
      
      /*
      // ioc/0061
      if (rootContextList.size() == 0)
        return;

      for (ScanRootContext context : rootContextList) {
        for (String className : context.getClassNameList()) {
          if (! _configuredClasses.contains(className)) {
            discoverBean(className);
          }
        }
      }
      */

      processPendingAnnotatedTypes();
    } catch (ConfigException e) {
      if (_configException == null)
        _configException = e;

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public void processPendingAnnotatedTypes()
  {
    _scanManager.discover();
    
    ArrayList<AnnotatedType<?>> types = new ArrayList<AnnotatedType<?>>(_pendingAnnotatedTypes);
    _pendingAnnotatedTypes.clear();
    
    for (AnnotatedType<?> type : types) {
      discoverBeanImpl(type);
    }
  }

  void discoverScanClass(InjectScanClass scanClass)
  {
    scanClass.register();
    
    // processPendingAnnotatedTypes();
  }
  
  void discoverBean(InjectScanClass scanClass)
  {
    AnnotatedType<?> type = createDiscoveredType(scanClass.getClassName());
    
    if (type != null)
      discoverBean(type);
  }
  
  void discoverBean(String className)
  {
    AnnotatedType<?> type = createDiscoveredType(className);
    
    if (type != null)
      discoverBean(type);
  }
  
  private AnnotatedType<?> createDiscoveredType(String className)
  {
    try {
      Class<?> cl;

      cl = Class.forName(className, false, _classLoader);

      /*
      if (! isValidSimpleBean(cl))
        return;
        */

      if (cl.getDeclaringClass() != null
          && ! Modifier.isStatic(cl.getModifiers()))
        return null;

      for (Class<? extends Annotation> forbiddenAnnotation : _forbiddenAnnotations) {
        if (cl.isAnnotationPresent(forbiddenAnnotation))
          return null;
      }

      for (Class<?> forbiddenClass : _forbiddenClasses) {
        if (forbiddenClass.isAssignableFrom(cl))
          return null;
      }

      // ioc/0619
      /*
      if (isDisabled(cl))
        return;
        */
      
      if (cl.isInterface()) {
        if (Annotation.class.isAssignableFrom(cl)
            && cl.isAnnotationPresent(Qualifier.class)) {
          // validateQualifier(cl);
          QualifierBinding.validateQualifier(cl, null);
        }
      }

      return createAnnotatedType(cl);
    } catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return null;
  }
  
  public <X> void discoverBean(AnnotatedType<X> beanType)
  {
    Class<X> cl;
    
    // ioc/07fb
    cl = beanType.getJavaClass();
    
    if (cl.isAnnotationPresent(Specializes.class)) {
      Class<?> parent = cl.getSuperclass();

      if (parent != null) {
        addSpecialize(cl, parent);
      }
    }
    
    AnnotatedType<X> type = getExtensionManager().processAnnotatedType(beanType);
    if (type == null)
      return;

    _pendingAnnotatedTypes.add(type);
  }
  
  private void addSpecialize(Class<?> specializedType, Class<?> parentType)
  {
    Class<?> oldSpecialized = _specializedMap.get(parentType);
    
    if (oldSpecialized != null)
      throw new ConfigException(L.l("@Specialized on '{0}' is invalid because it conflicts with an older specialized '{1}'",
                                    specializedType.getName(),
                                    oldSpecialized.getName()));
    
    if (! isValidSimpleBean(parentType))
      throw new ConfigException(L.l("@Specialized on '{0}' is invalid because its parent '{1}' is not a managed bean.",
                                    specializedType.getName(),
                                    parentType.getName()));
    
    _specializedMap.put(parentType, specializedType);
  }

  boolean isEnabled(Bean<?> bean)
  {
    if (! bean.isAlternative())
      return true;
    
    if (_deploymentMap.containsKey(bean.getBeanClass()))
      return true;
    
    for (Class<?> stereotype : bean.getStereotypes()) {
      if (_deploymentMap.containsKey(stereotype))
        return true;
    }
    
    return false;
  }

  boolean isIntrospectObservers(AnnotatedType<?> type)
  {
    if (type.isAnnotationPresent(Specializes.class))
      return true;
    
    String javaClassName = type.getJavaClass().getName();
    
    InjectScanClass scanClass = getScanManager().getScanClass(javaClassName);
    
    if (scanClass == null)
      return true;
    else
      return scanClass.isObserves();
  }
  
  private boolean isValidSimpleBean(Class<?> type)
  {
    if (type.isInterface())
      return false;
    else if (type.isAnonymousClass())
      return false;
    /*
    else if (type.isMemberClass())
      return false;
      */
    
    if (Modifier.isAbstract(type.getModifiers()))
      return false;

    /* XXX: ioc/024d */
    // ioc/070c, ioc/0j0g
    /*
    if (type.getTypeParameters() != null
        && type.getTypeParameters().length > 0) {
      return false;
    }
    */
    
    if (! isValidConstructor(type))
      return false;

    return true;
  }

  private boolean isValidSimpleBean(AnnotatedType<?> type)
  {
    if (type.isAnnotationPresent(XmlCookie.class)) {
      // ioc/04d0
      return true;
    }
      
    return isValidSimpleBean(type.getJavaClass());
  }

  private boolean isValidConstructor(Class<?> type)
  {
    for (Constructor<?> ctor : type.getDeclaredConstructors()) {
      if (ctor.getParameterTypes().length == 0)
        return true;

      if (ctor.isAnnotationPresent(Inject.class))
        return true;
    }

    return false;
  }

  private <T> void discoverBeanImpl(AnnotatedType<T> type)
  {
    // ioc/0n18
    /*
    if (_specializedMap.get(type.getJavaClass()) != null)
      return;
      */
    
    // XXX: not sure this is correct.
    if (Throwable.class.isAssignableFrom(type.getJavaClass()))
      return;
    
    if (! isValidSimpleBean(type)) {
      return;
    }
    
    ManagedBeanImpl<T> bean = new ManagedBeanImpl<T>(this, type, false);
    
    InjectionTarget<T> target = bean.getInjectionTarget(); //createInjectionTarget(type);

    if (target instanceof InjectionTargetBuilder<?>) {
      InjectionTargetBuilder<?> targetImpl = (InjectionTargetBuilder<?>) target;

      targetImpl.setGenerateInterception(true);
    }

    target = processInjectionTarget(target, type);

    if (target == null)
      return;

    if (target instanceof InjectionTargetBuilder<?>) {
      InjectionTargetBuilder<T> targetImpl = (InjectionTargetBuilder<T>) target;

      targetImpl.setBean(bean);
    }
    
    bean.setInjectionTarget(target);

    bean.introspect();

    AnnotatedType<T> annType = bean.getAnnotatedType();

    // ioc/0i04
    if (annType.isAnnotationPresent(javax.decorator.Decorator.class)) {
      if (annType.isAnnotationPresent(javax.interceptor.Interceptor.class))
        throw new ConfigException(L.l("'{0}' bean may not have both a @Decorator and @Interceptor annotation.",
                                      annType.getJavaClass()));
      // ioc/0c92
      DecoratorBean decoratorBean = new DecoratorBean(this, annType.getJavaClass());
      
      // addBean(decoratorBean);
    
      return;
    }
    // ioc/0c1a
    if (annType.isAnnotationPresent(javax.interceptor.Interceptor.class)) {
      InterceptorBean interceptorBean = new InterceptorBean(this, annType.getJavaClass());
      
      addBean(interceptorBean);
      return;
    }
    
    addDiscoveredBean(bean);
    
    fillProducerBeans(bean);

    // beans.addScannedClass(cl);
  }
  
  public <T> InjectionTarget<T> processInjectionTarget(InjectionTarget<T> target,
                                                       AnnotatedType<T> ann)
  {
    return getExtensionManager().processInjectionTarget(target, ann);
  }
  
  private void fillProducerBeans(ManagedBeanImpl<?> bean)
  {
  }

  private <X> void addDiscoveredBean(ManagedBeanImpl<X> managedBean)
  {
    /*
     // ioc/04d0
    if (! isValidSimpleBean(managedBean.getBeanClass()))
      return;
      */
    
    // ioc/0680
    if (! managedBean.isAlternative() || isEnabled(managedBean)) {
      // ioc/0680
      addBean(managedBean);

      // ioc/0b0f
      if (! _specializedMap.containsKey(managedBean.getBeanClass()))
        managedBean.introspectObservers();
      
      /*
      for (ObserverMethodImpl<X,?> observer : managedBean.getObserverMethods()) {
        // observer = processObserver(observer);

        if (observer != null) {
          Set<Annotation> annSet = observer.getObservedQualifiers();

          Annotation []bindings = new Annotation[annSet.size()];
          annSet.toArray(bindings);

          BaseType baseType = createSourceBaseType(observer.getObservedType());

          _eventManager.addObserver(observer, baseType, bindings);
        }
      }
      */
    }

    // ioc/07d2
    if (! _specializedMap.containsKey(managedBean.getBeanClass())
        && isEnabled(managedBean)) {
      managedBean.introspectProduces();
    }
  }
  
  public <X> void addProduces(Bean<X> bean, AnnotatedType<X> beanType)
  {
    ProducesBuilder builder = new ProducesBuilder(this);
    
    builder.introspectProduces(bean, beanType);
  }
  
  public <X> void addManagedProduces(Bean<X> bean, AnnotatedType<X> beanType)
  {
    ProducesBuilder builder = new ManagedProducesBuilder(this);
    
    builder.introspectProduces(bean, beanType);
  }
  
  public <X,T> void addProducesBean(ProducesMethodBean<X,T> bean)
  {
    AnnotatedMethod<X> producesMethod
    = (AnnotatedMethod<X>) bean.getProducesMethod();
    
    Producer<T> producer = bean.getProducer();
    
    producer = getExtensionManager().processProducer(producesMethod, producer);
    
    bean.setProducer(producer);
    
    addBean(bean, producesMethod);
  }
  
  public <X,T> void addProducesFieldBean(ProducesFieldBean<X,T> bean)
  {
    AnnotatedField<X> producesField
      = (AnnotatedField<X>) bean.getField();
    
    Producer<T> producer = bean.getProducer();
    
    producer = getExtensionManager().processProducer(producesField, producer);
    
    bean.setProducer(producer);
    
    addBean(bean, producesField);
  }

  public <X> void addManagedBean(ManagedBeanImpl<X> managedBean)
  {
    addBean(managedBean);
    
    managedBean.introspectProduces();
  }

  public <T> ArrayList<T> loadServices(Class<T> serviceClass)
  {
    return loadServices(serviceClass, new HashSet<URL>(), false);
  }

  public <T> ArrayList<T> loadLocalServices(Class<T> serviceClass)
  {
    return loadServices(serviceClass, new HashSet<URL>(), true);
  }

  private <T> ArrayList<T> loadServices(Class<T> serviceApiClass,
                                        HashSet<URL> serviceSet,
                                        boolean isLocal)
  {
    ArrayList<T> services = new ArrayList<T>();

    try {
      DynamicClassLoader loader = _classLoader;

      if (loader == null)
        return services;
      
      String serviceName = "META-INF/services/" + serviceApiClass.getName();
      
      Enumeration<URL> e;
      
      if (isLocal)
        e = loader.findResources(serviceName);
      else
        e = loader.getResources(serviceName);

      while (e.hasMoreElements()) {
        URL url = e.nextElement();

        if (serviceSet.contains(url))
          continue;

        serviceSet.add(url);

        InputStream is = null;
        try {
          is = url.openStream();
          ReadStream in = Vfs.openRead(is);

          String line;

          while ((line = in.readLine()) != null) {
            int p = line.indexOf('#');
            if (p >= 0)
              line = line.substring(0, p);
            line = line.trim();

            if (line.length() > 0) {
              Class<T> cl = loadServiceClass(serviceApiClass, line);

              if (cl != null)
                services.add(createTransientObject(cl));
            }
          }

          in.close();
        } catch (IOException e1) {
          log.log(Level.WARNING, e1.toString(), e1);
        } finally {
          IoUtil.close(is);
        }
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return services;
  }

  private <T> Class<T> loadServiceClass(Class<T> serviceApi,
                                        String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> serviceClass = Class.forName(className, false, loader);

      if (! serviceApi.isAssignableFrom(serviceClass))
        throw new InjectionException(L.l("'{0}' is not a valid servicebecause it does not implement {1}",
                                         serviceClass, serviceApi.getName()));

      return (Class<T>) serviceClass;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  public void addExtension(Extension extension)
  {
    _extensionManager.addExtension(extension);
  }

  /**
   * Starts the bind phase
   */
  public void bind()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    boolean isBind = false;

    try {
      thread.setContextClassLoader(_classLoader);
      
      processPendingAnnotatedTypes();
      
      if (_pendingBindList != null) {
        ArrayList<AbstractBean<?>> bindList
          = new ArrayList<AbstractBean<?>>(_pendingBindList);

        _pendingBindList.clear();

        if (bindList.size() > 0)
          isBind = true;
      }
      
      if (! _isAfterBeanDiscoveryComplete)
        isBind = true;

      if (isBind) {
        _isAfterBeanDiscoveryComplete = true;

        getExtensionManager().fireAfterBeanDiscovery();
      }

      if (_configException != null)
        throw _configException;

      /*
      for (AbstractBean comp : bindList) {
        if (_deploymentMap.get(comp.getDeploymentType()) != null)
          comp.bind();
      }
      */
      
      addDecorators();
      addInterceptors();

      validate();
      
      /*
      if (isBind) {
        getExtensionManager().fireAfterDeploymentValidation();
      }
      */
    } catch (RuntimeException e) {
      if (_configException == null)
        _configException = e;
      else {
        log.log(Level.WARNING, e.toString(), e);
      }

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private void addInterceptors()
  {
    if (_interceptorClassList.size() == 0)
      return;
    
    ArrayList<Class<?>> interceptorClassList
      = new ArrayList<Class<?>>(_interceptorClassList);
    _interceptorClassList.clear();
    
    for (Class<?> interceptorClass : interceptorClassList) {
      for (InterceptorEntry<?> entry : _interceptorList) {
        if (entry.getInterceptor().getBeanClass().equals(interceptorClass)) {
          entry.setEnabled(true);
          return;
        }
      }
      
      if (! interceptorClass.isAnnotationPresent(javax.interceptor.Interceptor.class))
        throw new ConfigException(L.l("'{0}' is an invalid interceptor because it does not have an @Interceptor.",
                                      interceptorClass.getName()));
        
      
      InterceptorBean<?> bean = new InterceptorBean(this, interceptorClass);
      
      InterceptorEntry<?> entry = addInterceptor(bean);
      entry.setEnabled(true);
    }
  }
  
  private void addDecorators()
  {
    if (_decoratorClassList.size() == 0)
      return;
    
    ArrayList<Class<?>> decoratorClassList
      = new ArrayList<Class<?>>(_decoratorClassList);
    _decoratorClassList.clear();
    
    for (Class<?> decoratorClass : decoratorClassList) {
      for (DecoratorEntry<?> entry : _decoratorList) {
        if (entry.getDecorator().getBeanClass().equals(decoratorClass)) {
          entry.setEnabled(true);
          return;
        }
      }
      
      DecoratorBean<?> bean = new DecoratorBean(this, decoratorClass);
      
      DecoratorEntry<?> entry = addDecorator(bean);
      entry.setEnabled(true);
    }
    
    // ioc/0i57 - validation must be early
    for (DecoratorEntry<?> entry : _decoratorList) {
      if (entry.isEnabled()) {
        for (Type type : entry.getDelegateType().getTypeClosure(this)) {
          validate(type);
        }
      }
    }
  }
  
  private void validate()
  {
    ArrayList<ArrayList<TypedBean>> typeValues
      = new ArrayList<ArrayList<TypedBean>>(_selfBeanMap.values());
    
    for (int i = typeValues.size() - 1; i >= 0; i--) {
      ArrayList<TypedBean> beans = typeValues.get(i);
      
      validateSpecializes(beans);
    }
    
    for (int i = typeValues.size() - 1; i >= 0; i--) {
      ArrayList<TypedBean> beans = typeValues.get(i);
      
      if (beans == null)
        continue;

      for (int j = beans.size() - 1; j >= 0; j--) {
        TypedBean typedBean = beans.get(j);
        
        typedBean.validate();
      }
    }
  }
  
  private void validateSpecializes(ArrayList<TypedBean> beans)
  {
    if (beans == null)
      return;
    
    for (int i = beans.size() - 1; i >= 0; i--) {
      TypedBean bean = beans.get(i);
      
      Annotated ann = bean.getAnnotated();
      
      if (ann == null || ! ann.isAnnotationPresent(Specializes.class))
        continue;

      for (int j = beans.size() - 1; j >= 0; j--) {
        if (i == j)
          continue;
        
        TypedBean bean2 = beans.get(j);
        
        // XXX:
        
        Annotated ann2 = bean.getAnnotated();
        
        if (ann2 == null)
          continue;
        
        if (isSpecializes(ann, ann2) && isMatchInject(bean, bean2)) {
          beans.remove(j);
          i = 0;
        }
      }
    }
  }
  
  private boolean isMatchInject(TypedBean typedBeanA, TypedBean typedBeanB)
  {
    Bean<?> beanA = typedBeanA.getBean();
    Bean<?> beanB = typedBeanB.getBean();
    
    return (beanA.getTypes().equals(beanB.getTypes())
            && beanA.getQualifiers().equals(beanB.getQualifiers()));
  }
  
  private boolean isSpecializes(Annotated childAnn, Annotated parentAnn)
  {
    if (childAnn instanceof AnnotatedMethod<?>
        && parentAnn instanceof AnnotatedMethod<?>) {
      Method childMethod = ((AnnotatedMethod<?>) childAnn).getJavaMember();
      Method parentMethod = ((AnnotatedMethod<?>) parentAnn).getJavaMember();
      
      if (! AnnotatedTypeUtil.isMatch(childMethod, parentMethod)) {
        return false;
      }
      
      Class<?> childClass = childMethod.getDeclaringClass();
      Class<?> parentClass = parentMethod.getDeclaringClass();
       
      if (parentClass.isAssignableFrom(childClass))
        return true;
    }
    
    return false;
  }

  /**
   * Handles the case the environment config phase
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
    initialize();
  }

  /**
   * Handles the case the environment config phase
   */
  @Override
  public void environmentBind(EnvironmentClassLoader loader)
  {
    initialize();
    bind();
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  @Override
  public void environmentStart(EnvironmentClassLoader loader)
  {
    start();
  }

  public void initialize()
  {
    update();

    /*
    if (_lifecycle.toInit()) {
      fireEvent(this, new AnnotationLiteral<Initialized>() {});
    }
    */
  }

  public void start()
  {
    initialize();

    bind();

    startServices();

    if (_configException != null) {
      // ioc/0p91
      throw _configException;
    }
    
    notifyStart();
  }

  public void notifyStart()
  {

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      update();
      
      // cloud/0300
      if (_isAfterValidationNeeded) {
        _isAfterValidationNeeded = false;
        getExtensionManager().fireAfterDeploymentValidation();
      }
    } catch (ConfigException e) {
      if (_configException == null)
        _configException = e;

      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public void addDefinitionError(Throwable t)
  {
    if (_configException != null) {
      log.log(Level.WARNING, t.toString(), t);
    }
    else if (t instanceof RuntimeException) {
      _configException = (RuntimeException) t;
    }
    else {
      _configException = ConfigException.create(t);
    }
  }

  public void addConfiguredBean(String className)
  {
    _xmlExtension.addConfiguredBean(className);
  }

  public void addXmlInjectionTarget(long cookie, InjectionTarget<?> target)
  {
    _xmlTargetMap.put(cookie, target);
  }
  
  public InjectionTarget<?> getXmlInjectionTarget(long cookie)
  {
    return _xmlTargetMap.get(cookie);
  }
  
  void addService(Bean<?> bean)
  {
    _pendingServiceList.add(bean);
  }

  /**
   * Initialize all the services
   */
  private void startServices()
  {
    ArrayList<Bean<?>> services;
    // ArrayList<ManagedBean> registerServices;

    synchronized (_pendingServiceList) {
      services = new ArrayList<Bean<?>>(_pendingServiceList);
      _pendingServiceList.clear();
    }

    for (Bean<?> bean : services) {
      CreationalContext<?> env = createCreationalContext(bean);

      getReference(bean, bean.getBeanClass(), env);
    }

    /*
    for (ManagedBean bean : registerServices) {
      startRegistration(bean);
    }
    */
  }

  /**
   * Handles the case where the environment is stopping
   */
  @Override
  public void environmentStop(EnvironmentClassLoader loader)
  {
    destroy();
  }

  public void destroy()
  {
    _singletonScope.closeContext();
    
    _parent = null;
    _classLoader = null;
    _deploymentMap = null;

    _selfBeanMap = null;
    _selfNamedBeanMap = null;
    _beanMap = null;
    _namedBeanMap = null;
    _contextMap = null;

    _interceptorList = null;
    _decoratorList = null;
    _pendingBindList = null;
    _pendingServiceList = null;
    
    _eventManager = null;
  }

  public static ConfigException injectError(AccessibleObject prop, String msg)
  {
    String location = "";

    if (prop instanceof Field) {
      Field field = (Field) prop;
      String className = field.getDeclaringClass().getName();

      location = className + "." + field.getName() + ": ";
    }
    else if (prop instanceof Method) {
      Method method = (Method) prop;
      String className = method.getDeclaringClass().getName();

      location = className + "." + method.getName() + ": ";
    }

    return new ConfigException(location + msg);
  }

  public static String location(Field field)
  {
    return field.getDeclaringClass().getName() + "." + field.getName() + ": ";
  }

  public static String location(Method method)
  {
    return LineConfigException.loc(method);
  }

  public static ConfigException error(Method method, String msg)
  {
    return new ConfigException(location(method) + msg);
  }

  public void setSerializationHandle(Object handle)
  {
    _serializationHandle = handle;
  }

  /**
   * Serialization rewriting
   */
  public Object writeReplace()
  {
    return _serializationHandle;
  }

  public void checkActive()
  {
  }

  public String toString()
  {
    if (_classLoader != null)
      return getClass().getSimpleName() + "[" + _classLoader.getId() + "]";
    else
      return getClass().getSimpleName() + "[" + _id + "]";
  }

  static String getSimpleName(Type type)
  {
    if (type instanceof Class<?>)
      return ((Class<?>) type).getSimpleName();
    else
      return String.valueOf(type);
  }

  class TypedBean {
    private final BaseType _type;
    private final Annotated _annotated;
    private final Bean<?> _bean;
    private final boolean _isModulePrivate;
    private boolean _isValidated;

    TypedBean(BaseType type, Annotated annotated, Bean<?> bean)
    {
      _type = type;
      _annotated = annotated;
      _bean = bean;
      
      _isModulePrivate = isModulePrivate(bean) || bean.isAlternative();
    }
    
    public Annotated getAnnotated()
    {
      return _annotated;
    }

    /**
     * 
     */
    public void validate()
    {
      if (! _isValidated) {
        _isValidated = true;
    
        InjectManager.this.validate(_bean);
        /*
        for (InjectionPoint ip : _bean.getInjectionPoints()) {
          InjectManager.this.validate(ip);
        }
        */
      }
    }

    boolean isModulePrivate()
    {
      return _isModulePrivate;
    }

    BaseType getType()
    {
      return _type;
    }

    Bean<?> getBean()
    {
      return _bean;
    }

    boolean isModulePrivate(Bean<?> bean)
    {
      if (! (bean instanceof AnnotatedBean))
        return false;

      Annotated annotated = ((AnnotatedBean) bean).getAnnotated();

      if (annotated == null)
        return false;

      for (Annotation ann : annotated.getAnnotations()) {
        Class<?> annType = ann.annotationType();

        if (annType.equals(ModulePrivate.class)
            || annType.isAnnotationPresent(ModulePrivate.class)
            || annType.equals(Module.class)
            || annType.isAnnotationPresent(Module.class)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public int hashCode()
    {
      return 65521 * _type.hashCode() + _bean.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
      if (! (o instanceof TypedBean))
        return false;

      TypedBean bean = (TypedBean) o;

      return _type.equals(bean._type) && _bean.equals(bean._bean);
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _type + "," + _bean + "]";
    }
  }

  static class FillByName implements EnvironmentApply
  {
    private String _name;
    private ArrayList<Bean<?>> _beanList;

    FillByName(String name, ArrayList<Bean<?>> beanList)
    {
      _name = name;
      _beanList = beanList;
    }

    public void apply(EnvironmentClassLoader loader)
    {
      InjectManager beanManager = InjectManager.getCurrent(loader);

      beanManager.fillByName(_name, _beanList);
    }
  }

  static class FillByType implements EnvironmentApply
  {
    private BaseType _baseType;
    private HashSet<TypedBean> _beanSet;
    private InjectManager _manager;

    FillByType(BaseType baseType,
               HashSet<TypedBean> beanSet,
               InjectManager manager)
    {
      _baseType = baseType;
      _beanSet = beanSet;
      _manager = manager;
    }

    @Override
    public void apply(EnvironmentClassLoader loader)
    {
      InjectManager beanManager = InjectManager.getCurrent(loader);

      beanManager.fillByType(_baseType, _beanSet, _manager);
    }
  }

  static class FactoryBinding {
    private static final Annotation []NULL = new Annotation[0];

    private final Type _type;
    private final Annotation []_ann;

    FactoryBinding(Type type, Annotation []ann)
    {
      _type = type;

      if (ann != null)
        _ann = ann;
      else
        _ann = NULL;
    }

    @Override
    public int hashCode()
    {
      int hash = _type.hashCode();

      for (Annotation ann : _ann)
        hash = 65521 * hash + ann.hashCode();

      return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (! (obj instanceof FactoryBinding))
        return false;

      FactoryBinding binding = (FactoryBinding) obj;

      if (_type != binding._type)
        return false;

      if (_ann.length != binding._ann.length)
        return false;

      for (int i = 0; i < _ann.length; i++) {
        if (! _ann[i].equals(binding._ann[i]))
          return false;
      }

      return true;
    }
  }

  static class InjectBean<X> extends BeanWrapper<X>
    implements PassivationCapable, ScopeAdapterBean<X>
  {
    private ClassLoader _loader;

    InjectBean(Bean<X> bean, InjectManager beanManager)
    {
      super(beanManager, bean);

      _loader = Thread.currentThread().getContextClassLoader();

      if (bean instanceof AbstractBean) {
        AbstractBean<X> absBean = (AbstractBean<X>) bean;
        Annotated annotated = absBean.getAnnotated();

        if (annotated != null
            && annotated.isAnnotationPresent(ContextDependent.class)) {
          // ioc/0e17
          _loader = null;
        }
      }
    }

    public String getId()
    {
      Bean<?> bean = getBean();

      if (bean instanceof PassivationCapable)
        return ((PassivationCapable) bean).getId();
      else
        return null;
    }

    public X getScopeAdapter(Bean<?> topBean, CreationalContextImpl<X> cxt)
    {
      Bean<?> bean = getBean();

      if (bean instanceof ScopeAdapterBean<?>)
        return (X) ((ScopeAdapterBean) bean).getScopeAdapter(topBean, cxt);
      else
        return null;
    }

    @Override
    public X create(CreationalContext<X> env)
    {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        if (_loader != null) {
          // ioc/0e17
          thread.setContextClassLoader(_loader);
        }

        X value = getBean().create(env);
        
        return value;
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }

    @Override
    public int hashCode()
    {
      return getBean().hashCode();
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof InjectBean<?>))
        return false;

      InjectBean<?> bean = (InjectBean<?>) o;

      return getBean().equals(bean.getBean());
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + getBean() + "]";
    }
  }
  
  abstract public class ReferenceFactory<T> {
    public Bean<T> getBean()
    {
      return null;
    }
    
    public final T create()
    {
      return create(null, null, null);
    }
    
    public boolean isResolved()
    {
      return true;
    }
    
    public boolean isProducer()
    {
      Bean<T> bean = getBean();
      
      return ((bean instanceof ProducesMethodBean<?,?>)
              || (bean instanceof ProducesFieldBean<?,?>));
    }
    
    abstract public T create(CreationalContextImpl<T> env,
                             CreationalContextImpl<?> parentEnv,
                             InjectionPoint ip);
  }
  
  public class DependentReferenceFactory<T> extends ReferenceFactory<T> {
    private Bean<T> _bean;
    
    DependentReferenceFactory(Bean<T> bean)
    {
      _bean = bean;
    }
    
    @Override
    public Bean<T> getBean()
    {
      return _bean;
    }
   
    @Override
    public T create(CreationalContextImpl<T> env,
                    CreationalContextImpl<?> parentEnv,
                    InjectionPoint ip)
    {
      Bean<T> bean = _bean;
      
      T instance = CreationalContextImpl.find(parentEnv, bean);
      
      if (instance != null)
        return instance;
      
      if (env == null) {
        if (parentEnv != null)
          env = new DependentCreationalContext<T>(bean, parentEnv, ip);
        else
          env = new OwnerCreationalContext<T>(bean);
      }
      
      instance = bean.create(env);
      
      env.push(instance);
      
      /*
      if (env.isTop() && ! (bean instanceof CdiStatefulBean)) {
        bean.destroy(instance, env);
      }
      */
      
      return instance;
    }
  }
  
  public class DependentReferenceFactoryImpl<T> extends ReferenceFactory<T> {
    private ManagedBeanImpl<T> _bean;
    
    DependentReferenceFactoryImpl(ManagedBeanImpl<T> bean)
    {
      _bean = bean;
    }
    
    @Override
    public Bean<T> getBean()
    {
      return _bean;
    }
   
    @Override
    public T create(CreationalContextImpl<T> env,
                    CreationalContextImpl<?> parentEnv,
                    InjectionPoint ip)
    {
      ManagedBeanImpl<T> bean = _bean;
      
      T instance = CreationalContextImpl.find(parentEnv, bean);
      
      if (instance != null)
        return instance;
      
      if (env == null) {
        if (parentEnv != null)
          env = new DependentCreationalContext<T>(bean, parentEnv, ip);
        else
          env = new OwnerCreationalContext<T>(bean);
      }
      
      instance = bean.createDependent(env);

      // ioc/0k13
      /*
      if (env.isTop() && ! (bean instanceof CdiStatefulBean)) {
        bean.destroy(instance, env);
      }
      */
      
      return instance;
    }
  }
  
  public class DependentElReferenceFactoryImpl<T> extends ReferenceFactory<T> {
    private ManagedBeanImpl<T> _bean;
    
    DependentElReferenceFactoryImpl(ManagedBeanImpl<T> bean)
    {
      _bean = bean;
    }
    
    @Override
    public Bean<T> getBean()
    {
      return _bean;
    }
   
    @Override
    public T create(CreationalContextImpl<T> env,
                    CreationalContextImpl<?> parentEnv,
                    InjectionPoint ip)
    {
      ManagedBeanImpl<T> bean = _bean;
      
      T instance = CreationalContextImpl.findAny(parentEnv, bean);
      
      if (instance != null)
        return instance;
      
      if (env == null) {
        if (parentEnv != null)
          env = new DependentCreationalContext<T>(bean, parentEnv, ip);
        else
          env = new OwnerCreationalContext<T>(bean);
      }
      
      instance = bean.createDependent(env);

      if (env.isTop() && ! (bean instanceof CdiStatefulBean)) {
        bean.destroy(instance, env);
      }
      
      return instance;
    }
  }
  
  public class ContextReferenceFactory<T> extends ReferenceFactory<T> {
    private Bean<T> _bean;
    private Context _context;
    
    ContextReferenceFactory(Bean<T> bean,
                            Context context)
    {
      _bean = bean;
      _context = context;
    }
    
    @Override
    public Bean<T> getBean()
    {
      return _bean;
    }
   
    @Override
    public T create(CreationalContextImpl<T> env,
                    CreationalContextImpl<?> parentEnv,
                    InjectionPoint ip)
    {
      Bean<T> bean = _bean;
      
      T instance = CreationalContextImpl.find(parentEnv, bean);
      
      if (instance != null)
        return instance;
      
      if (env == null)
        env = new OwnerCreationalContext<T>(bean, parentEnv);
      
      instance = _context.get(bean, env);
        
      if (instance == null)
        throw new NullPointerException(L.l("null instance returned by '{0}' for bean '{1}'",
                                           _context, bean));
        
      return instance;
    }
  }
  
  public class NormalInstanceReferenceFactory<T> extends ReferenceFactory<T> {
    private ThreadLocal<CreationalContextImpl<T>> _threadLocal
      = new ThreadLocal<CreationalContextImpl<T>>();
    
    private Bean<T> _bean;
    private Context _context;
    
    NormalInstanceReferenceFactory(Bean<T> bean,
                                   Context context)
    {
      _bean = bean;
      _context = context;
    }
    
    @Override
    public Bean<T> getBean()
    {
      return _bean;
    }
   
    @Override
    public T create(CreationalContextImpl<T> env,
                    CreationalContextImpl<?> parentEnv,
                    InjectionPoint ip)
    {
      Bean<T> bean = _bean;
      
      // ioc/0155
      // XXX: possibly restrict to NormalScope adapter
      CreationalContextImpl<T> oldEnv = _threadLocal.get();
      
      try {
        T instance = CreationalContextImpl.find(oldEnv, bean);
        
        if (instance != null) {
          return instance;
        }

        env = new OwnerCreationalContext<T>(bean, oldEnv);
          
        _threadLocal.set(env);
      
        instance = _context.get(bean, env);
        
        if (instance == null)
          throw new NullPointerException(L.l("null instance returned by '{0}' for bean '{1}'",
                                             _context, bean));
        
        return instance;
      } finally {
        _threadLocal.set(oldEnv);
      }
    }
  }

  public class NormalContextReferenceFactory<T> extends ReferenceFactory<T> {
    private Bean<T> _bean;
    private ScopeAdapterBean<T> _scopeAdapterBean;
    private Context _context;
    private T _scopeAdapter;
    
    NormalContextReferenceFactory(Bean<T> bean,
                                  ScopeAdapterBean<T> scopeAdapterBean,
                                  Context context)
    {
      _bean = bean;
      _scopeAdapterBean = scopeAdapterBean;
      
      _context = context;
      
      ScopeAdapter scopeAdapter = ScopeAdapter.create(bean);
      _scopeAdapter = scopeAdapter.wrap(createNormalInstanceFactory(bean));
    }
    
    @Override
    public Bean<T> getBean()
    {
      return _bean;
    }
   
    @Override
    public T create(CreationalContextImpl<T> env,
                    CreationalContextImpl<?> parentEnv,
                    InjectionPoint ip)
    {
      return _scopeAdapter;
    }
  }
  
  public class DelegateReferenceFactory<T> extends ReferenceFactory<T> {
    DelegateReferenceFactory()
    {
    }
   
    @Override
    public T create(CreationalContextImpl<T> env,
                    CreationalContextImpl<?> parentEnv,
                    InjectionPoint ip)
    {
      return (T) parentEnv.getDelegate();
    }
  }
  
  public class ErrorReferenceFactory<T> extends ReferenceFactory<T> {
    private RuntimeException _exn;
    
    ErrorReferenceFactory(RuntimeException e)
    {
      _exn = e;
    }
    
    @Override
    public boolean isProducer()
    {
      return true;
    }
   
    @Override
    public T create(CreationalContextImpl<T> env,
                    CreationalContextImpl<?> parentEnv,
                    InjectionPoint ip)
    {
      throw _exn;
    }
  }
  
  public class InjectionPointReferenceFactory 
    extends ReferenceFactory<InjectionPoint> {
    InjectionPointReferenceFactory()
    {
    }
   
    @Override
    public InjectionPoint create(CreationalContextImpl<InjectionPoint> env,
                                 CreationalContextImpl<?> parentEnv,
                                 InjectionPoint ip)
    {
      InjectionPoint ip2 =  parentEnv.findInjectionPoint();
      
      if (ip2 != null)
        return ip2;
      
      throw new InjectionException(L.l("no injection point available in this context {0}",
                                       ip));
    }
  }
  
  public class UnresolvedReferenceFactory extends ReferenceFactory<Object> {
    private InjectionException _exn;
    
    UnresolvedReferenceFactory()
    {
      _exn = new InjectionException("unresolved injection");
    }
    
    @Override
    public boolean isResolved()
    {
      return false;
    }
   
    @Override
    public Object create(CreationalContextImpl<Object> env,
                         CreationalContextImpl<?> parentEnv,
                         InjectionPoint ip)
    {
      throw _exn;
    }
  }

  static {
    ArrayList<Class<?>> forbiddenAnnotations = new ArrayList<Class<?>>();
    ArrayList<Class<?>> forbiddenClasses = new ArrayList<Class<?>>();

    for (String className : FORBIDDEN_ANNOTATIONS) {
      try {
        Class<?> cl = Class.forName(className);

        if (cl != null)
          forbiddenAnnotations.add(cl);
      } catch (Throwable e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }

    for (String className : FORBIDDEN_CLASSES) {
      try {
        Class<?> cl = Class.forName(className);

        if (cl != null)
          forbiddenClasses.add(cl);
      } catch (Throwable e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }

    _forbiddenAnnotations = new Class[forbiddenAnnotations.size()];
    forbiddenAnnotations.toArray(_forbiddenAnnotations);

    _forbiddenClasses = new Class[forbiddenClasses.size()];
    forbiddenClasses.toArray(_forbiddenClasses);

    ClassLoader systemClassLoader = null;

    try {
      systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (Throwable e) {
      // a security manager may not allow this call

      log.log(Level.FINEST, e.toString(), e);
    }

    _systemClassLoader = systemClassLoader;
  }
}
