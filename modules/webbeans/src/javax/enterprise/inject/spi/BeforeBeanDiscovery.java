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

/**
 * Extension callback before any bean discovery, so extensions can add
 * annotated types, stereotypes and binding types.
 *
 * <code><pre>
 * public class MyExtension implements Extension
 * {
 *  public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event)
 *  {
 *    ...
 *  }
 * }
 * </pre></code>
 */
public interface BeforeBeanDiscovery
{
  /**
   * Registers an annotated type with the BeanManager, used by extensions to
   * register configured beans.
   *
   * @param type the abstract introspected type for the new bean
   */
  public void addAnnotatedType(AnnotatedType<?> type);

  public void addInterceptorBinding(Class<? extends Annotation> bindingType,
                                    Annotation... bindingTypeDef);

  /**
   * Registers an annotation as a binding type, so applications can use
   * existing annotations for binding without modifying the annotation
   * source.
   *
   * @param bindingType the annotation to register as a binding type.
   */
  public void addQualifier(Class<? extends Annotation> qualifier);

  public void addScope(Class<? extends Annotation> scopeType,
                       boolean isNormal,
                       boolean isPassivating);

  public void addStereotype(Class<? extends Annotation> stereotype,
                            Annotation... stereotypeDef);
}
