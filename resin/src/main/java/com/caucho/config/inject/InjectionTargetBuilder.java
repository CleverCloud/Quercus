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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.decorator.Delegate;
import javax.ejb.Stateful;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.caucho.config.ConfigException;
import com.caucho.config.SerializeHandle;
import com.caucho.config.bytecode.SerializationAdapter;
import com.caucho.config.gen.CandiBeanGenerator;
import com.caucho.config.inject.InjectManager.ReferenceFactory;
import com.caucho.config.j2ee.PostConstructProgram;
import com.caucho.config.j2ee.PreDestroyInject;
import com.caucho.config.program.Arg;
import com.caucho.config.program.BeanArg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ResourceProgramManager;
import com.caucho.config.reflect.AnnotatedConstructorImpl;
import com.caucho.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
@Module
public class InjectionTargetBuilder<X> implements InjectionTarget<X>
{
  private static final L10N L = new L10N(InjectionTargetBuilder.class);
  private static final Logger log
    = Logger.getLogger(InjectionTargetBuilder.class.getName());

  private InjectManager _cdiManager;

  private Class<X> _instanceClass;
  
  private Bean<X> _bean;

  private final AnnotatedType<X> _annotatedType;

  private AnnotatedConstructor<X> _beanCtor;
  
  private CandiProducer<X> _producer;
  
  private Constructor<X> _javaCtor;
  
  private boolean _isGenerateInterception = true;

  private ConfigProgram []_newArgs;

  private Set<InjectionPoint> _injectionPointSet;
  
  public InjectionTargetBuilder(InjectManager cdiManager,
                                AnnotatedType<X> beanType,
                                Bean<X> bean)
  {
    _cdiManager = cdiManager;

    _annotatedType = beanType;
    _bean = bean;
  }
  
  public InjectionTargetBuilder(InjectManager cdiManager,
                                AnnotatedType<X> beanType)
  {
    this(cdiManager, beanType, null);
  }

  protected InjectManager getBeanManager()
  {
    return _cdiManager;
  }
  
  public AnnotatedType<X> getAnnotatedType()
  {
    return _annotatedType;
  }
  
  void setBean(Bean<X> bean)
  {
    _bean = bean;
  }
  
  Bean<X> getBean()
  {
    return _bean;
  }

  /**
   * Returns the injection points.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    synchronized (this) {
      if (_producer == null) {
        _producer = build();
        validate(getBean());
      }
    }
    
    return _producer.getInjectionPoints();
  }

  public void setGenerateInterception(boolean isEnable)
  {
    _isGenerateInterception = isEnable;
  }

  @Override
  public X produce(CreationalContext<X> env)
  {
    if (_producer == null)
      getInjectionPoints();
    
    return _producer.produce(env);
  }

  @Override
  public void inject(X instance, CreationalContext<X> env)
  {
    if (_producer == null)
      getInjectionPoints();
    
    _producer.inject(instance, env);
  }

  @Override
  public void postConstruct(X instance)
  {

    if (_producer == null)
      getInjectionPoints();
    
    _producer.postConstruct(instance);
  }

  /**
   * Call pre-destroy
   */
  @Override
  public void preDestroy(X instance)
  {
    if (_producer == null)
      getInjectionPoints();
    
    _producer.preDestroy(instance);
  }

  @Override
  public void dispose(X instance)
  {
    if (_producer == null)
      getInjectionPoints();

    _producer.dispose(instance);
  }

  protected Object getHandle()
  {
    return new SingletonHandle(null);
  }
  
  public String getPassivationId()
  {
    return null;
  }
  /**
   * Binds parameters
   */
  private CandiProducer<X> build()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
      
    try {
      thread.setContextClassLoader(getBeanManager().getClassLoader());

      introspect();
      
      Class<X> cl = (Class<X>) _annotatedType.getBaseType();

      if (_beanCtor == null) {
        // XXX:
        AnnotatedType<X> beanType = _annotatedType;
          
        if (beanType == null)
          beanType = ReflectionAnnotatedFactory.introspectType(cl);

        introspectConstructor(beanType);
      }

      Class<X> instanceClass = null;

      if (_isGenerateInterception) {
        if (! _annotatedType.isAnnotationPresent(javax.interceptor.Interceptor.class)
            && ! _annotatedType.isAnnotationPresent(javax.decorator.Decorator.class)) {
          CandiBeanGenerator<X> bean = new CandiBeanGenerator<X>(getBeanManager(), _annotatedType);
          bean.introspect();

          instanceClass = (Class<X>) bean.generateClass();
        }

        if (instanceClass == cl && isSerializeHandle()) {
            instanceClass = SerializationAdapter.gen(instanceClass);
        }
      }

      if (instanceClass != null && instanceClass != _instanceClass) {
        try {
          if (_javaCtor != null) {
            _javaCtor = (Constructor<X>) getConstructor(instanceClass, _javaCtor.getParameterTypes());
            _javaCtor.setAccessible(true);
          }
        } catch (Exception e) {
          // server/2423
          log.log(Level.FINE, e.toString(), e);
          // throw ConfigException.create(e);
        }
      }

      ConfigProgram []injectProgram = introspectInject(_annotatedType);
      ConfigProgram []initProgram = introspectPostConstruct(_annotatedType);

      ArrayList<ConfigProgram> destroyList = new ArrayList<ConfigProgram>();
      introspectDestroy(destroyList, _annotatedType);
      ConfigProgram []destroyProgram = new ConfigProgram[destroyList.size()];
      destroyList.toArray(destroyProgram);
      
      Arg []args = null;
      
      if (_beanCtor != null)
        args = introspectArguments(_beanCtor, _beanCtor.getParameters());

      CandiProducer<X> producer
        = new CandiProducer<X>(_bean,
                               instanceClass,
                               _javaCtor,
                               args,
                               injectProgram,
                               initProgram,
                               destroyProgram,
                               _injectionPointSet);
      
      return producer;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private static Constructor<?> getConstructor(Class<?> cl, Class<?> []paramTypes)
  {
    for (Constructor<?> ctor : cl.getDeclaredConstructors()) {
      if (isMatch(ctor.getParameterTypes(), paramTypes))
        return ctor;
    }
    
    throw new IllegalStateException("No matching constructor found for " + cl);
  }
  
  private static boolean isMatch(Class<?> []paramTypesA, Class<?> []paramTypesB)
  {
    if (paramTypesA.length != paramTypesB.length)
      return false;
    
    for (int i = paramTypesA.length - 1; i >= 0; i--) {
      if (! paramTypesA[i].equals(paramTypesB[i]))
        return false;
    }
    
    return true;
  }
  
  private ConfigProgram []introspectPostConstruct(AnnotatedType<X> annType)
  {
    if (annType.isAnnotationPresent(Interceptor.class)) {
      return new ConfigProgram[0];
    }
    
    ArrayList<ConfigProgram> initList = new ArrayList<ConfigProgram>();
    introspectInit(initList, annType);
    ConfigProgram []initProgram = new ConfigProgram[initList.size()];
    initList.toArray(initProgram);
    
    Arrays.sort(initProgram);
    
    return initProgram;
  }

  public static void
    introspectInit(ArrayList<ConfigProgram> initList,
                   AnnotatedType<?> type)
    throws ConfigException
  {
    for (AnnotatedMethod<?> annMethod : type.getMethods()) {
      Method method = annMethod.getJavaMember();
      
      if (! annMethod.isAnnotationPresent(PostConstruct.class)) {
        // && ! isAnnotationPresent(annList, Inject.class)) {
        continue;
      }

      if (method.getParameterTypes().length == 1
          && InvocationContext.class.equals(method.getParameterTypes()[0]))
        continue;

      if (method.isAnnotationPresent(PostConstruct.class)
          && method.getParameterTypes().length != 0) {
          throw new ConfigException(location(method)
                                    + L.l("{0}: @PostConstruct is requires zero arguments"));
      }

      PostConstructProgram initProgram
        = new PostConstructProgram(method);

      if (! initList.contains(initProgram))
        initList.add(initProgram);
    }
  }

  private void
    introspectDestroy(ArrayList<ConfigProgram> destroyList, 
                      AnnotatedType<?> type)
    throws ConfigException
  {
    if (type == null || type.equals(Object.class))
      return;
    
    if (type.isAnnotationPresent(Interceptor.class)) {
      return;
    }

    for (AnnotatedMethod<?> method : type.getMethods()) {
      if (method.isAnnotationPresent(PreDestroy.class)) {
        Method javaMethod = method.getJavaMember();
        
        Class<?> []types = javaMethod.getParameterTypes();

        if (types.length == 0) {
        }
        else if (types.length == 1 && types[0].equals(InvocationContext.class)) {
          // XXX:
          continue;
        }
        else
          throw new ConfigException(location(javaMethod)
                                    + L.l("@PreDestroy is requires zero arguments"));

        PreDestroyInject destroyProgram
          = new PreDestroyInject(javaMethod);

        if (! destroyList.contains(destroyProgram))
          destroyList.add(destroyProgram);
      }
    }
  }

  //
  // introspection
  //

  private void introspect()
  {
    introspect(_annotatedType);
  }

  /**
   * Called for implicit introspection.
   */
  private void introspect(AnnotatedType<X> beanType)
  {
    Class<X> cl = (Class<X>) beanType.getBaseType();
    
    introspectConstructor(beanType);
  }

  /**
   * Introspects the constructor
   */
  private void introspectConstructor(AnnotatedType<X> beanType)
  {
    if (_beanCtor != null)
      return;

    // XXX: may need to modify BeanFactory
    if (beanType.getJavaClass().isInterface())
      return;

    try {
      /*
      Class cl = getInstanceClass();

      if (cl == null)
        cl = getTargetClass();
      */

      AnnotatedConstructor<X> best = null;
      AnnotatedConstructor<X> second = null;

      for (AnnotatedConstructor<X> ctor : beanType.getConstructors()) {
        if (_newArgs != null
            && ctor.getParameters().size() != _newArgs.length) {
          continue;
        }
        else if (best == null) {
          best = ctor;
        }
        else if (ctor.isAnnotationPresent(Inject.class)) {
          if (best != null && best.isAnnotationPresent(Inject.class))
            throw new ConfigException(L.l("'{0}' can't have two constructors marked by @Inject or by a @Qualifier, because the Java Injection BeanManager can't tell which one to use.",
                                          beanType.getJavaClass().getName()));
          best = ctor;
          second = null;
        }
        else if (best.isAnnotationPresent(Inject.class)) {
        }
        else if (ctor.getParameters().size() == 0) {
          best = ctor;
        }
        else if (best.getParameters().size() == 0) {
        }
        else if (ctor.getParameters().size() == 1
                 && ctor.getParameters().get(0).equals(String.class)) {
          second = best;
          best = ctor;
        }
      }

      if (best == null) {
        // ioc/0q00
        best = new AnnotatedConstructorImpl(beanType, beanType.getJavaClass().getConstructor(new Class[0]));
      }

      if (best == null) {
        throw new ConfigException(L.l("{0}: no constructor found while introspecting bean for Java Injection",
                                      beanType.getJavaClass().getName()));
      }

      if (second == null) {
      }
      else if (beanType.getJavaClass().getName().startsWith("java.lang")
               && best.getParameters().size() == 1
               && best.getParameters().get(0).equals(String.class)) {
        log.fine(L.l("{0}: WebBean does not have a unique constructor, choosing String-arg constructor",
                     beanType.getJavaClass().getName()));
      }
      else
        throw new ConfigException(L.l("{0}: Bean does not have a unique constructor.  One constructor must be marked with @Inject or have a qualifier annotation.",
                                      beanType.getJavaClass().getName()));

      _beanCtor = best;
      _javaCtor = _beanCtor.getJavaMember();
      _javaCtor.setAccessible(true);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @SuppressWarnings("unchecked")
  private Arg<X> []introspectArguments(Annotated ann, List<AnnotatedParameter<X>> params)
  {
    Arg<X> []args = new Arg[params.size()];

    for (int i = 0; i < args.length; i++) {
      AnnotatedParameter<X> param = params.get(i);
      
      args[i] = introspectArg(ann, param);
    }

    return args;
  }
  
  private Arg<X> introspectArg(Annotated ann, AnnotatedParameter<X> param)
  {
    Annotation []qualifiers = getQualifiers(param);
 
    InjectionPoint ip = new InjectionPointImpl<X>(getBeanManager(),
                                                  this,
                                                  param);
    
    if (ann.isAnnotationPresent(Inject.class)) {
      // ioc/022k
      _injectionPointSet.add(ip);
    }
    
    if (param.isAnnotationPresent(Disposes.class)) {
      throw new ConfigException(L.l("{0} is an invalid managed bean because its constructor has a @Disposes parameter",
                                    getAnnotatedType().getJavaClass().getName()));
    }
    
    if (param.isAnnotationPresent(Observes.class)) {
      throw new ConfigException(L.l("{0} is an invalid managed bean because its constructor has an @Observes parameter",
                                    getAnnotatedType().getJavaClass().getName()));
    }

    return new BeanArg<X>(getBeanManager(),
                          param.getBaseType(), 
                          qualifiers,
                          ip);
  }

  private Annotation []getQualifiers(Annotated annotated)
  {
    ArrayList<Annotation> qualifierList = new ArrayList<Annotation>();

    for (Annotation ann : annotated.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifierList.add(ann);
      }
    }

    if (qualifierList.size() == 0)
      qualifierList.add(CurrentLiteral.CURRENT);

    Annotation []qualifiers = new Annotation[qualifierList.size()];
    qualifierList.toArray(qualifiers);

    return qualifiers;
  }

  private ConfigProgram []introspectInject(AnnotatedType<X> type)
  {
    ArrayList<ConfigProgram> injectProgramList = new ArrayList<ConfigProgram>();
    
    _injectionPointSet = new HashSet<InjectionPoint>();
    
    introspectInject(type, injectProgramList);
    
    ConfigProgram []injectProgram = new ConfigProgram[injectProgramList.size()];
    injectProgramList.toArray(injectProgram);
    
    Arrays.sort(injectProgram);
    
    return injectProgram;
  }
  
  private void introspectInject(AnnotatedType<X> type,
                                ArrayList<ConfigProgram> injectProgramList)
  {
    Class<?> rawType = (Class<?>) type.getBaseType();

    if (rawType == null || Object.class.equals(rawType))
      return;
    
    // Class<?> parentClass = rawType.getSuperclass();
    
    // configureClassResources(injectList, type);

    introspectInjectClass(type, injectProgramList);
    introspectInjectField(type, injectProgramList);
    introspectInjectMethod(type, injectProgramList);
    
    ResourceProgramManager resourceManager = _cdiManager.getResourceManager();
    
    resourceManager.buildInject(rawType, injectProgramList);
  }
  
  private void introspectInjectClass(AnnotatedType<X> type,
                                     ArrayList<ConfigProgram> injectProgramList)
  {
    InjectManager cdiManager = getBeanManager();
    
    for (Annotation ann : type.getAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();
      
      InjectionPointHandler handler 
        = cdiManager.getInjectionPointHandler(annType);
      
      if (handler != null) {
        injectProgramList.add(new ClassHandlerProgram(ann, handler));
      }
    }
    
    // ioc/123i
    for (Class<?> parentClass = type.getJavaClass().getSuperclass();
         parentClass != null;
         parentClass = parentClass.getSuperclass()) {
      for (Annotation ann : parentClass.getAnnotations()) {
        Class<? extends Annotation> annType = ann.annotationType();
      
        InjectionPointHandler handler 
          = cdiManager.getInjectionPointHandler(annType);
      
        if (handler != null) {
          injectProgramList.add(new ClassHandlerProgram(ann, handler));
        }
      }
    }
  }
  
  private void introspectInjectField(AnnotatedType<X> type,
                                     ArrayList<ConfigProgram> injectProgramList)
  {
    for (AnnotatedField<?> field : type.getFields()) {
      if (field.getAnnotations().size() == 0)
        continue;

      if (field.isAnnotationPresent(Inject.class)) {
        // boolean isOptional = isQualifierOptional(field);

        InjectionPoint ij = new InjectionPointImpl(getBeanManager(), this, field);

        _injectionPointSet.add(ij);

        if (field.isAnnotationPresent(Delegate.class)) {
          // ioc/0i60
          /*
        if (! type.isAnnotationPresent(javax.decorator.Decorator.class)) {
          throw new IllegalStateException(L.l("'{0}' may not inject with @Delegate because it is not a @Decorator",
                                              type.getJavaClass()));
        }
           */
        }
        else {
          injectProgramList.add(new FieldInjectProgram(field.getJavaMember(), ij));
        }
      }
      else {
        InjectionPointHandler handler
        = getBeanManager().getInjectionPointHandler(field);

        if (handler != null) {
          ConfigProgram program = new FieldHandlerProgram(field, handler);

          injectProgramList.add(program);
        }
      }
    }
  }
  
  private void introspectInjectMethod(AnnotatedType<X> type,
                                      ArrayList<ConfigProgram> injectProgramList)
  {

    for (AnnotatedMethod method : type.getMethods()) {
      if (method.getAnnotations().size() == 0)
        continue;

      if (method.isAnnotationPresent(Inject.class)) {
        // boolean isOptional = isQualifierOptional(field);

        List<AnnotatedParameter<?>> params = method.getParameters();

        InjectionPoint []args = new InjectionPoint[params.size()];

        for (int i = 0; i < args.length; i++) {
          InjectionPoint ij
            = new InjectionPointImpl(getBeanManager(), this, params.get(i));

          _injectionPointSet.add(ij);

          args[i] = ij;
        }

        injectProgramList.add(new MethodInjectProgram(method.getJavaMember(),
                                                      args));
      }
      else {
        InjectionPointHandler handler
          = getBeanManager().getInjectionPointHandler(method);
        
        if (handler != null) {
          ConfigProgram program = new MethodHandlerProgram(method, handler);
          
          injectProgramList.add(program);
        }
      }
    }
  }
  
  private void validate(Bean<?> bean)
  {
    if (bean == null)
      return;
    
    Class<? extends Annotation> scopeType = bean.getScope();
    
    if (getBeanManager().isPassivatingScope(scopeType)) {
      //validateNormal(bean);
      validatePassivating(bean);
    }
    else if (getBeanManager().isNormalScope(scopeType)) {
      //validateNormal(bean);
    }
  }
  
  private void validatePassivating(Bean<?> bean)
  {
    Type baseType = _annotatedType.getBaseType();
    
    Class<?> cl = getBeanManager().createTargetBaseType(baseType).getRawClass();
    boolean isStateful = _annotatedType.isAnnotationPresent(Stateful.class);
    
    if (! Serializable.class.isAssignableFrom(cl) && ! isStateful) {
      throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because it's not serializable for {2}.",
                                    cl.getSimpleName(), bean.getScope().getSimpleName(),
                                    bean));
    }
    
    for (InjectionPoint ip : bean.getInjectionPoints()) {
      if (ip.isTransient())
        continue;
      
      Type type = ip.getType();
      
      if (ip.getBean() instanceof CdiStatefulBean)
        continue;
      
      if (type instanceof Class<?>) {
        Class<?> ipClass = (Class<?>) type;

        if (! ipClass.isInterface()
            && ! Serializable.class.isAssignableFrom(ipClass)
            && ! getBeanManager().isNormalScope(ip.getBean().getScope())) {
          throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because '{2}' value {3} is not serializable for {4}.",
                                        cl.getSimpleName(), bean.getScope().getSimpleName(),
                                        ip.getType(),
                                        ip.getMember().getName(),
                                        bean));
        }
      }
    }
  }

  /**
   * Checks for validity for classpath scanning.
   */
  public static boolean isValid(Class<?> type)
  {
    if (type.isInterface())
      return false;

    if (type.getTypeParameters() != null
        && type.getTypeParameters().length > 0) {
      return false;
    }

    if (! isValidConstructor(type))
      return false;

    return true;
  }

  public static boolean isValidConstructor(Class<?> type)
  {
    for (Constructor<?> ctor : type.getDeclaredConstructors()) {
      if (ctor.getParameterTypes().length == 0)
        return true;

      if (ctor.isAnnotationPresent(Inject.class))
        return true;
    }

    return false;
  }

  private static String location(Method method)
  {
    String className = method.getDeclaringClass().getName();

    return className + "." + method.getName() + ": ";
  }

  private boolean isSerializeHandle()
  {
    return getAnnotatedType().isAnnotationPresent(SerializeHandle.class);
  }

  private static boolean hasQualifierAnnotation(AnnotatedConstructor<?> ctor)
  {
    return ctor.isAnnotationPresent(Inject.class);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _annotatedType + "]";
  }

  class FieldInjectProgram extends ConfigProgram {
    private final Field _field;
    private final InjectionPoint _ip;
    private final InjectManager.ReferenceFactory<?> _fieldFactory;

    FieldInjectProgram(Field field, InjectionPoint ip)
    {
      _field = field;
      _field.setAccessible(true);
      _ip = ip;
      
      InjectManager beanManager = getBeanManager();

      try {
        _fieldFactory = beanManager.getReferenceFactory(_ip);
      } catch (AmbiguousResolutionException e) {
        String loc = getLocation(field);
        
        throw new AmbiguousResolutionException(loc + e.getMessage(), e);
      } catch (UnsatisfiedResolutionException e) {
        String loc = getLocation(field);
        
        throw new UnsatisfiedResolutionException(loc + e.getMessage(), e);
      } catch (IllegalProductException e) {
        String loc = getLocation(field);
        
        throw new IllegalProductException(loc + e.getMessage(), e);
      } catch (InjectionException e) {
        String loc = getLocation(field);
      
        throw new InjectionException(loc + e.getMessage(), e);
      }
    }
    
    @Override
    public Class<?> getDeclaringClass()
    {
      return _field.getDeclaringClass();
    }
    
    @Override
    public String getName()
    {
      return _field.getName();
    }
    
    private String getLocation(Field field)
    {
      return _field.getDeclaringClass().getName() + "." + _field.getName() + ": ";
      
    }

    @Override
    public <T> void inject(T instance, CreationalContext<T> cxt)
    {
      try {
        CreationalContextImpl<?> env;
        
        if (cxt instanceof CreationalContextImpl<?>)
          env = (CreationalContextImpl<?>) cxt;
        else
          env = null;
        
        // server/30i1 vs ioc/0155
        Object value = _fieldFactory.create(null, env, _ip);
        
        _field.set(instance, value);
      } catch (AmbiguousResolutionException e) {
        throw new AmbiguousResolutionException(getFieldName(_field) + e.getMessage(), e);
      } catch (IllegalProductException e) {
        throw new IllegalProductException(getFieldName(_field) + e.getMessage(), e);
      } catch (InjectionException e) {
        throw new InjectionException(getFieldName(_field) + e.getMessage(), e);
      } catch (Exception e) {
        throw ConfigException.create(_field, e);
      }
    }
    
    private String getFieldName(Field field)
    {
      return field.getDeclaringClass().getSimpleName() + "." + field.getName() + ": ";
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _field + "]";
    }
  }

  class MethodInjectProgram extends ConfigProgram {
    private final Method _method;
    private final InjectionPoint []_args;
    private ReferenceFactory<?> []_factoryArgs;

    MethodInjectProgram(Method method, 
                        InjectionPoint []args)
    {
      _method = method;
      _method.setAccessible(true);
      _args = args;
      
      _factoryArgs = new ReferenceFactory[args.length];
    }

    /**
     * Sorting priority: fields are second
     */
    @Override
    public int getPriority()
    {
      return 1;
    }
    
    @Override
    public Class<?> getDeclaringClass()
    {
      return _method.getDeclaringClass();
    }
    
    @Override
    public String getName()
    {
      return _method.getName();
    }
    
    @Override
    public <T> void inject(T instance, CreationalContext<T> cxt)
    {
      try {
        CreationalContextImpl<T> env;
        
        if (cxt instanceof CreationalContextImpl<?>)
          env = (CreationalContextImpl<T>) cxt;
        else
          env = null;
        
        Object []args = new Object[_args.length];

        for (int i = 0; i < _args.length; i++) {
          if (_factoryArgs[i] == null)
            _factoryArgs[i] = getBeanManager().getReferenceFactory(_args[i]);

          args[i] = _factoryArgs[i].create(null, env, _args[i]);
        }

        _method.invoke(instance, args);
      } catch (Exception e) {
        throw ConfigException.create(_method, e);
      }
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _method + "]";
    }
  }
  
  class FieldHandlerProgram extends ConfigProgram {
    private final AnnotatedField<?> _field;
    private final InjectionPointHandler _handler;
    private ConfigProgram _boundProgram;
    
    FieldHandlerProgram(AnnotatedField<?> field, InjectionPointHandler handler)
    {
      _field = field;
      _handler = handler;
    }

    @Override
    public <T> void inject(T instance, CreationalContext<T> env)
    {
      if (_boundProgram == null)
        bind();
      
      _boundProgram.inject(instance, env);
    }
    
    @Override
    public void bind()
    {
      _boundProgram = _handler.introspectField(_field);
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _field + "]";
    }
  }
  
  class ClassHandlerProgram extends ConfigProgram {
    private final Annotation _ann;
    private final InjectionPointHandler _handler;
    private ConfigProgram _boundProgram;
    
    ClassHandlerProgram(Annotation ann, InjectionPointHandler handler)
    {
      _ann = ann;
      _handler = handler;
    }

    @Override
    public <T> void inject(T instance, CreationalContext<T> env)
    {
      if (_boundProgram == null)
        bind();
      
      _boundProgram.inject(instance, env);
    }
    
    @Override
    public void bind()
    {
      _boundProgram = _handler.introspectType(_annotatedType);
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _annotatedType + "]";
    }
  }
  
  class MethodHandlerProgram extends ConfigProgram {
    private final AnnotatedMethod<?> _method;
    private final InjectionPointHandler _handler;
    private ConfigProgram _boundProgram;
    
    MethodHandlerProgram(AnnotatedMethod<?> method,
                         InjectionPointHandler handler)
    {
      _method = method;
      _handler = handler;
    }

    @Override
    public int getPriority()
    {
      return 1;
    }
    
    @Override
    public <T> void inject(T instance, CreationalContext<T> env)
    {
      if (_boundProgram == null)
        bind();
      
      _boundProgram.inject(instance, env);
    }
    
    @Override
    public void bind()
    {
      _boundProgram = _handler.introspectMethod(_method);
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _method + "]";
    }
  }
}
