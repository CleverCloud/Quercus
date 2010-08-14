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

package javax.enterprise.inject.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * API for the Java Injection (JSR-299) BeanManager.
 *
 * Applications needing a programmatic interface to BeanManager can use
 * JNDI at "java:comp/BeanManager".  Bean registered with CanDI can use
 * injection to get the manager.
 *
 * <code><pre>
 * @Current BeanManager _manager;
 * </pre></code>
 */
public interface BeanManager
{
  //
  // enabled deployment types, scopes, and binding types
  //

  /**
   * Tests if an annotation is an enabled scope type
   */
  public boolean isScope(Class<? extends Annotation> annotationType);

  /**
   * Tests if an annotation is an enabled scope type
   */
  public boolean isPassivatingScope(Class<? extends Annotation> annotationType);

  /**
   * Tests if an annotation is an enabled scope type
   */
  public boolean isNormalScope(Class<? extends Annotation> annotationType);

  /**
   * Tests if an annotation is an enabled qualifier type
   */
  public boolean isQualifier(Class<? extends Annotation> annotationType);

  /**
   * Tests if an annotation is an enabled interceptor binding type
   */
  public boolean isInterceptorBinding(Class<? extends Annotation> annotationType);

  /**
   * Returns the bindings for an interceptor binding type
   */
  public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType);

  /**
   * Tests if an annotation is an enabled stereotype.
   */
  public boolean isStereotype(Class<? extends Annotation> annotationType);

  /**
   * Returns the annotations associated with a stereotype
   */
  public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype);


  //
  // bean registration and discovery
  //

  /**
   * Creates an introspected annotated type.
   */
  public <T> AnnotatedType<T> createAnnotatedType(Class<T> type);

  /**
   * Creates an injection target
   */
  public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type);

  //
  // Bean resolution
  //

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param qualifiers array of required @Qualifier annotations
   */
  public Set<Bean<?>> getBeans(Type beanType, Annotation... qualiviers);

  /**
   * Returns the bean definitions matching a given name
   *
   * @param name the name of the bean to match
   */
  public Set<Bean<?>> getBeans(String name);

  /**
   * Returns the bean with the highest precedence deployment type from a set.
   *
   * @param beans the set of beans to select from
   */
  public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans);

  /**
   * Returns the bean with the highest precedence deployment type from a set.
   *
   * @param beans the set of beans to select from
   */
  public <X> Bean<? extends X> getMostSpecializedBean(Bean<X> bean);

  /**
   * Returns the passivation-capable bean with the given id.  Used by
   * custom Contexts during deserialization to get the beans needed for
   * destruction.
   *
   * @param id the basic bean
   */
  public Bean<?> getPassivationCapableBean(String id);

  /**
   * Internal callback during creation to get a new injection instance.
   */
  public void validate(InjectionPoint injectionPoint);

  //
  // Bean instantiation
  //

  /**
   * Creates a new CreationalContext for instantiating a bean.  Normally
   * used for getReference by frameworks.
   */
  public <T> CreationalContext<T>
    createCreationalContext(Contextual<T> contextual);

  /**
   * Returns an instance for the given bean.  This method will obey
   * the scope of the bean, so a singleton will return the single bean.
   *
   * @param bean the metadata for the bean
   * @param beanType the expected type
   * @param env the creational context environment for the bean
   *
   * @return an instance of the bean obeying scope
   */
  public Object getReference(Bean<?> bean,
                             Type beanType,
                             CreationalContext<?> env);

  /**
   * Internal callback during creation to get a new injection instance.
   */
  public Object getInjectableReference(InjectionPoint ij,
                                       CreationalContext<?> ctx);

  //
  // contexts
  //

  /**
   * Returns the scope context for the given type
   */
  public Context getContext(Class<? extends Annotation> scopeType);

  //
  // EL integration
  //

  /**
   * Returns the BeanManager's EL resolver.
   */
  public ELResolver getELResolver();

  /**
   * Returns the BeanManager's EL resolver.
   */
  public ExpressionFactory
    wrapExpressionFactory(ExpressionFactory expressionFactory);

  //
  // Observer resolution
  //

  /**
   * Returns the observers listening for an event
   *
   * @param eventType event to resolve
   * @param qualifiers the qualifier set for the event
   */
  public <T> Set<ObserverMethod<? super T>>
    resolveObserverMethods(T event, Annotation... qualifiers);

  /**
   * Fires an event
   *
   * @param event the event to fire
   * @param qualifiers the event qualifiers
   */
  public void fireEvent(Object event, Annotation... qualifiers);

  //
  // interceptor support
  //

  /**
   * Resolves the interceptors for a given interceptor type
   *
   * @param type the main interception type
   * @param bindings qualifying bindings
   *
   * @return the matching interceptors
   */
  public List<Interceptor<?>> resolveInterceptors(InterceptionType type,
                                                  Annotation... bindings);


  //
  // decorator
  //

  /**
   * Resolves the decorators for a given set of types
   *
   * @param types the types to match for the decorator
   * @param qualifier qualifiers
   *
   * @return the matching interceptors
   */
  public List<Decorator<?>> resolveDecorators(Set<Type> types,
                                              Annotation... qualifiers);
}
