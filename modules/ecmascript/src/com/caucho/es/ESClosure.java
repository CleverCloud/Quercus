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
 * @author Scott Ferguson
 */

package com.caucho.es;

/**
 * Implementation class representing a JavaScript function.
 */
public class ESClosure extends ESObject {
  static ESString LENGTH = ESId.intern("length");
  static ESString CALLEE = ESId.intern("callee");
  static ESString ARGUMENTS = ESId.intern("arguments");
  static ESString OBJECT = ESId.intern("Object");
  static ESString PROTOTYPE = ESId.intern("prototype");
  static ESString CONSTRUCTOR = ESId.intern("constructor");

  public ESString name;
  private ESCallable esClass;
  public int n;

  ESId []formals;
  int nFormals;

  ESBase proto;

  ESGlobal global;
  int stackRequired; // how much argument stack space this function requires
  int scopeRequired; // how much scope space
  ESBase []scope;
  int scopeLength;

  boolean hasFields;

  public ESClosure(ESString name, ESCallable esClass, ESObject proto,
                   int n, ESId []formals, ESObject global)
  {
    this.className = "Function";
    this.prototype = Global.getGlobalProto().funProto;
    this.name = name;
    this.esClass = esClass;
    this.proto = proto;
    this.n = n;
    this.formals = formals;
    this.nFormals = formals.length;

    if (global instanceof ESGlobal)
      this.global = (ESGlobal) global;

    if (global != null) {
      this.scopeLength = 1;
      this.scope = new ESBase[1];
      this.scope[0] = global;
    }
  }

  protected ESClosure(ESBase []scope, int scopeLength)
  {
    super("Function", null);

    hasFields = true;
    this.scope = scope;
    this.scopeLength = scopeLength;
  }
  /**
   * Create a new object based on a prototype
   */
  protected ESClosure()  {}

  public void closure(Call env)
  {
    if (scope == null) {
      scope = new ESBase[16];
    }

    for (; scopeLength < env.scopeLength; scopeLength++)
      scope[scopeLength] = env.scope[scopeLength];

    if (scopeLength == 0)
      scope[scopeLength++] = env.global;

    global = (ESGlobal) scope[0];
  }

  /**
   * Create a new object based on a prototype
   */
  void setScope(ESBase []scope, int scopeLength)
  {
    this.scope = scope;
    this.scopeLength = scopeLength;
  }

  public ESBase hasProperty(ESString id)
    throws Throwable
  {
    if (id.equals(PROTOTYPE)) {
      if (proto == null) {
        ESObject obj = Global.getGlobalProto().createObject();
        proto = obj;
        obj.put(CONSTRUCTOR, this, DONT_ENUM);
      }

      return proto;
    } else if (id.equals(LENGTH)) {
      return ESNumber.create(nFormals);
    } else if (hasFields) {
      return super.hasProperty(id);
    } else if (prototype != null)
      return prototype.hasProperty(id);
    else
      return ESBase.esEmpty;
  }

  public ESBase getProperty(ESString id)
    throws Throwable
  {
    if (id.equals(PROTOTYPE)) {
      if (proto == null) {
        ESObject obj = Global.getGlobalProto().createObject();
        proto = obj;
        obj.put(CONSTRUCTOR, this, DONT_ENUM);
      }

      return proto;
    } else if (id.equals(LENGTH)) {
      return ESNumber.create(nFormals);
    } else if (hasFields) {
      return super.getProperty(id);
    } else
      return prototype.getProperty(id);
  }

  public boolean canPut(ESString id)
  {
    if (id.equals(PROTOTYPE)) {
      return true;
    } else if (id.equals(LENGTH)) {
      return false;
    } else if (hasFields) {
      return super.canPut(id);
    } else {
      return true;
    }
  }

  public void setProperty(ESString id, ESBase value)
    throws Throwable
  {
    if (id.equals(PROTOTYPE)) {
      proto = value;
    } else if (id.equals(LENGTH)) {
    } else if (hasFields) {
      super.setProperty(id, value);
    } else {
      init(className, prototype);
      hasFields = true;
      super.setProperty(id, value);
    }
  }

  public void put(ESString id, ESBase value, int flags)
  {
    if (id.equals(PROTOTYPE)) {
      proto = value;
    } else if (id.equals(LENGTH)) {
    } else if (hasFields) {
      super.put(id, value, flags);
    } else {
      init(className, prototype);
      hasFields = true;
      super.put(id, value, flags);
    }
  }

  public ESBase delete(ESString id)
    throws Throwable
  {
    if (id.equals(PROTOTYPE)) {
      proto = ESBase.esEmpty;
      return ESBoolean.TRUE;
    } else if (id.equals(LENGTH)) {
      return ESBoolean.FALSE;
    } else if (hasFields) {
      return super.delete(id);
    } else
      return ESBoolean.TRUE;
  }

  public ESString toStr()
  {
    return ESString.create(decompile());
  }

  String decompile()
  {
    StringBuffer sbuf = new StringBuffer();

    sbuf.append("function ");
    if (name != null && ! name.toString().startsWith("$lambda"))
        sbuf.append(name);
    sbuf.append("(");
    for (int i = 0; formals != null && i < nFormals; i++) {
      if (i != 0)
        sbuf.append(", ");
      sbuf.append(formals[i]);
    }

    sbuf.append(") ");

    sbuf.append("{ ");
    sbuf.append("[compiled code]");
    sbuf.append(" }");

    return sbuf.toString();
  }

  protected ESBase dispatch() throws ESException
  {
    throw new ESException("dispatch not specialized");
  }

  public ESBase call(Call call, int length) throws Throwable
  {
    if (global != null)
      call.global = global;

    if (esClass != null)
      return esClass.call(n, call, length);
    else
      return ESBase.esUndefined;
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    Global resin = Global.getGlobalProto();
    ESObject obj = Global.getGlobalProto().createObject();
    ESBase proto = this.proto;

    if (! (proto instanceof ESObject))
      proto = resin.object.getProperty(PROTOTYPE);

    if (proto instanceof ESObject)
      obj.prototype = proto;

    eval.setThis(obj);

    ESBase value = call(eval, length);

    return value instanceof ESObject ? (ESObject) value : obj;
  }

  public ESBase typeof() throws ESException
  {
    return ESString.create("function");
  }

  protected void copy(Object newObj)
  {
    ESClosure closure = (ESClosure) newObj;

    super.copy(newObj);

    closure.name = name;
    closure.esClass = esClass;
    closure.n = n;
    closure.formals = formals;
    closure.nFormals = nFormals;

    closure.stackRequired = stackRequired;
    closure.scopeRequired = scopeRequired;
    closure.scopeLength = scopeLength;
    closure.scope = new ESBase[scopeLength];
    closure.hasFields = hasFields;

    for (int i = 0; i < scopeLength; i++) {
      if (scope[i] != null) {
        closure.scope[i] = (ESBase) scope[i];
      }
    }

    closure.proto = proto; // XXX: should be copied?
  }

  public ESObject dup() { return new ESClosure(); }

  ESObject resinCopy()
  {
    ESObject obj = dup();

    copy(obj);

    return obj;
  }

  ESObject getClassPrototype()
    throws Throwable
  {
    ESBase proto = hasProperty(PROTOTYPE);

    return (ESObject) proto;
  }

  void setClassPrototype(ESObject proto)
    throws ESException
  {
    this.proto = proto;
    proto.put(CONSTRUCTOR, this, DONT_ENUM);
  }
}
