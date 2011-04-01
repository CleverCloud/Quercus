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
package com.caucho.config.gen;

import java.io.IOException;
import java.util.HashMap;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents the XA interception
 */
@Module
public class XaGenerator<X> extends AbstractAspectGenerator<X> {
  private TransactionAttributeType _transactionType;
  private boolean _isContainerManaged = true;
  private boolean _isSessionSynchronization;

  public XaGenerator(XaFactory<X> factory,
                     AnnotatedMethod<? super X> method,
                     AspectGenerator<X> next,
                     TransactionAttributeType xa,
                     boolean isBeanManaged)
  {
    super(factory, method, next);
    
    _transactionType = xa;

    _isContainerManaged = ! isBeanManaged;
  }

  /**
   * Returns the transaction type
   */
  public TransactionAttributeType getTransactionType()
  {
    return _transactionType;
  }

  //
  // bean prologue generation
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    if (map.get("caucho.ejb.xa") == null) {
      map.put("caucho.ejb.xa", "done");

      out.println();
      out.println("private static final com.caucho.ejb.util.XAManager _xa");
      out.println("  = new com.caucho.ejb.util.XAManager();");
    }

    super.generateMethodPrologue(out, map);
  }

  boolean isEjb()
  {
    return (getBeanType().isAnnotationPresent(Stateless.class)
            || getBeanType().isAnnotationPresent(Stateful.class)
            || getBeanType().isAnnotationPresent(Singleton.class));
  }
  //
  // method generation code
  //

  /**
   * Generates code before the "try" block <code><pre>
   * retType myMethod(...)
   * {
   *   [pre-try]
   *   try {
   *     ...
   * }
   * </pre></code>
   */
  @Override
  public void generatePreTry(JavaWriter out) throws IOException
  {
    out.println();
    out.println("boolean isXAValid = false;");

    if (!_isContainerManaged) {
      out.println();
      out.println("Transaction xa = null;");
    } else if (_transactionType != null) {
      switch (_transactionType) {
      case NOT_SUPPORTED:
      case REQUIRED:
      case REQUIRES_NEW: {
        out.println();
        out.println("Transaction xa = null;");
        break;
      }
      
      case MANDATORY: {
        out.println();
        out.println("_xa.beginMandatory();");
        break;
      }

      case NEVER: {
        out.println();
        out.println("_xa.beginNever();");
        break;
      }
      }
    }

    super.generatePreTry(out);
  }

  /**
   * Generates the interceptor code after the try-block and before the call.
   * 
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     [pre-call]
   *     retValue = super.myMethod(...);
   * }
   * </pre></code>
   */
  @Override
  public void generatePreCall(JavaWriter out) throws IOException
  {
    if (!_isContainerManaged) {
      out.println("xa = _xa.beginNotSupported();");
    } else if (_transactionType != null) {
      switch (_transactionType) {
      case NOT_SUPPORTED: {
        out.println();
        out.println("xa = _xa.beginNotSupported();");
        break;
      }

      case REQUIRED: {
        out.println();
        out.println("xa = _xa.beginRequired();");
        break;
      }

      case REQUIRES_NEW: {
        out.println();
        out.println("xa = _xa.beginRequiresNew();");
        break;
      }
      }
    }

    if (_isContainerManaged && _isSessionSynchronization) {
      out.print("_xa.registerSynchronization(");
      // XXX: getBusinessMethod().generateThis(out);
      out.println(");");
    }

    super.generatePreCall(out);
  }

  /**
   * Generates the interceptor code after invocation and before the call.
   * 
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     retValue = super.myMethod(...);
   *     [post-call]
   *     return retValue;
   *   } finally {
   *     ...
   *   }
   * }
   * </pre></code>
   */
  @Override
  public void generatePostCall(JavaWriter out) throws IOException
  {
    super.generatePostCall(out);

    out.println("isXAValid = true;");
  }

  /**
   * Generates aspect code for an application exception
   * 
   */
  @Override
  public void generateApplicationException(JavaWriter out, Class<?> exception)
      throws IOException
  {
    super.generateApplicationException(out, exception);

    out.println("isXAValid = true;");
    out.println("_xa.applicationException(e);");
  }

  @Override
  public void generateSystemException(JavaWriter out, Class<?> exn)
      throws IOException
  {
    out.println("isXAValid = true;");
    out.println("if (_xa.systemException(e)) {");
    out.pushDepth();
    
    if (_isContainerManaged) {
      /*
      out.println("if (_xa.getTransaction() != null) {");
      out.println("  _xa.markRollback(e);");
      //out.println("  isXAValid = true;");
      out.println("}");
      */
      
      if (isEjb()) {
        switch (_transactionType) {
        case SUPPORTS:
          out.println("  _xa.rethrowEjbException(e, _xa.getTransaction() != null);");
          break;
          
        case REQUIRES_NEW:
        case NOT_SUPPORTED:
        case NEVER:
          out.println("_xa.rethrowEjbException(e, false);");
          break;
          
        case MANDATORY:
          out.println("_xa.rethrowEjbException(e, true);");
          break;
          
        case REQUIRED:
          out.println("_xa.rethrowEjbException(e, xa != null);");
          break;

        default:
          out.println("_xa.rethrowEjbException(e, xa != null);");
          break;
        }
      }
    }
    else {
      if (isEjb()) {
        /*
        out.println("if (_xa.getTransaction() != null) {");
        out.println("  _xa.markRollback(e);");
        //out.println("  isXAValid = true;");
        out.println("}");
        */
        
        out.println("_xa.rethrowEjbException(e, false);");
      }
    }
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the aspect code in the finally block.
   * 
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     ...
   *   } finally {
   *     [finally]
   *   }
   * }
   * </pre></code>
   */
  @Override
  public void generateFinally(JavaWriter out) throws IOException
  {
    super.generateFinally(out);
    
    out.println("if (! isXAValid)");
    out.println("  _xa.markRollback();");
    
    if (! _isContainerManaged) {
      out.println("if (_xa.getTransaction() != null)");
      out.println("  _xa.commit();");
      
      out.println("if (xa != null)");
      out.println("  _xa.resume(xa);");
    } 
    else if (_transactionType != null) {
      switch (_transactionType) {
      case NOT_SUPPORTED: {
        out.println("if (xa != null)");
        out.pushDepth();
        out.println("  _xa.resume(xa);");
        out.popDepth();
        break;
      }

      case REQUIRED: {
        out.println("if (xa == null)");
        out.pushDepth();
        out.println("_xa.commit();");
        out.popDepth();
        break;
      }

      case REQUIRES_NEW: {
        out.println("_xa.endRequiresNew(xa);");
        break;
      }
      }
    }
  }
}