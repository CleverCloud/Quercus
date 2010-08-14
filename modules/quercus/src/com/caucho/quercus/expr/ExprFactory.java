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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.*;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.*;
import com.caucho.quercus.statement.*;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating PHP expressions and statements
 */
public class ExprFactory {
  private static final L10N L = new L10N(ExprFactory.class);
  private static final Logger log
    = Logger.getLogger(ExprFactory.class.getName());

  private static boolean _isPro = true;

  public ExprFactory()
  {
  }

  public static ExprFactory create()
  {
    if (! _isPro)
      return new ExprFactory();

    try {
      Class cl = Class.forName("com.caucho.quercus.expr.ExprFactoryPro");

      return (ExprFactory) cl.newInstance();
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);

      _isPro = false;

      return new ExprFactory();
    }
  }

  /**
   * Creates a null literal expression.
   */
  public Expr createNull()
  {
    return LiteralNullExpr.NULL;
  }

  /**
   * Creates a string (php5) literal expression.
   */
  public Expr createString(String lexeme)
  {
    return new LiteralStringExpr(lexeme);
  }

  /**
   * Creates a string literal expression.
   */
  public Expr createUnicode(String lexeme)
  {
    return new LiteralUnicodeExpr(lexeme);
  }

  /**
   * Creates a binary literal expression.
   */
  public Expr createBinary(byte []bytes)
  {
    return new LiteralBinaryStringExpr(bytes);
  }

  /**
   * Creates a long literal expression.
   */
  public Expr createLong(long value)
  {
    return new LiteralLongExpr(value);
  }

  /**
   * Creates a string literal expression.
   */
  public Expr createLiteral(Value literal)
  {
    return new LiteralExpr(literal);
  }

  /**
   * Creates a var expression.
   */
  public VarExpr createVar(VarInfo var)
  {
    return new VarExpr(var);
  }

  /**
   * Creates a var expression.
   */
  public VarVarExpr createVarVar(Expr var)
  {
    return new VarVarExpr(var);
  }

  //
  // constants
  //

  /**
   * Creates a __FILE__ expression.
   */
  public ConstFileExpr createFileNameExpr(String fileName)
  {
    return new ConstFileExpr(fileName);
  }

  /**
   * Creates a __DIR__ expression.
   */
  public ConstDirExpr createDirExpr(String dirName)
  {
    return new ConstDirExpr(dirName);
  }

  /**
   * Creates a const expression.
   */
  public ConstExpr createConst(String name)
  {
    return new ConstExpr(name);
  }

  //
  // array deref
  //

  /**
   * Creates an array get 'a[0]' expression.
   * @param location
   */
  public ArrayGetExpr createArrayGet(Location location,
                                     Expr base,
                                     Expr index)
  {
    /* XXX:
    if (base instanceof ArrayGetExpr
        || base instanceof ObjectFieldExpr
        || base instanceof ObjectFieldVarExpr)
      return new ArrayGetGetExpr(location, base, index);
    else
      return new ArrayGetExpr(location, base, index);
      */
    return new ArrayGetExpr(location, base, index);
  }

  /**
   * Creates an array tail 'a[]' expression.
   * @param location TODO
   */
  public ArrayTailExpr createArrayTail(Location location, Expr base)
  {
    return new ArrayTailExpr(location, base);
  }

  //
  // field deref
  //

  /**
   * Creates an object get '$a->b' expression.
   */
  public Expr createFieldGet(Expr base,
                             StringValue name)
  {
    return new ObjectFieldExpr(base, name);
  }

  /**
   * Creates an object get '$a->$b' expression.
   */
  public Expr createFieldVarGet(Expr base,
                                Expr name)
  {
    return new ObjectFieldVarExpr(base, name);
  }
  
  //
  // $this expressions
  //

  /**
   * Creates a this expression.
   */
  public ThisExpr createThis(InterpretedClassDef cl)
  {
    return new ThisExpr(cl);
  }

  /**
   * Creates a "$this->foo" expression.
   */
  public ThisFieldExpr createThisField(ThisExpr qThis, 
                                       StringValue name)
  {
    return new ThisFieldExpr(qThis, name);
  }

  /**
   * Creates a "$this->$foo" expression.
   */
  public ThisFieldVarExpr createThisField(ThisExpr qThis, Expr name)
  {
    return new ThisFieldVarExpr(qThis, name);
  }

  /**
   * Creates a $this method call $this->foo(...).
   */
  public Expr createThisMethod(Location loc,
                               ThisExpr qThis,
                               String methodName,
                               ArrayList<Expr> args)
  {
    return new ThisMethodExpr(loc, qThis, methodName, args);
  }

  /**
   * Creates a $this method call $this->foo(...).
   */
  public Expr createThisMethod(Location loc,
                               ThisExpr qThis,
                               Expr methodName,
                               ArrayList<Expr> args)
  {
    return new ThisMethodVarExpr(loc, qThis, methodName, args);
  }

  //
  // class scope foo::bar
  //

  /**
   * Creates a class const expression.
   */
  public ClassConstExpr createClassConst(String className, String name)
  {
    return new ClassConstExpr(className, name);
  }

  /**
   * Creates an expression class const expression ($class::FOO).
   */
  public Expr createClassConst(Expr className, String name)
  {
    return new ClassVarConstExpr(className, name);
  }

  /**
   * Creates a class const expression (static::FOO).
   */
  public ClassVirtualConstExpr createClassVirtualConst(String name)
  {
    return new ClassVirtualConstExpr(name);
  }

  //
  // class fields
  //

  /**
   * Creates an class static field 'a::$b' expression.
   */
  public Expr createClassField(String className,
                               String name)
  {
    return new ClassFieldExpr(className, name);
  }

  /**
   * Creates an class static field '$a::$b' expression.
   */
  public Expr createClassField(Expr className,
                               String name)
  {
    return new ClassVarFieldExpr(className, name);
  }

  /**
   * Creates a class static field 'static::$b' expression.
   */
  public Expr createClassVirtualField(String name)
  {
    return new ClassVirtualFieldExpr(name);
  }

  /**
   * Creates an class static field 'a::${b}' expression.
   */
  public Expr createClassField(String className,
                               Expr name)
  {
    return new ClassFieldVarExpr(className, name);
  }

  /**
   * Creates an class static field '$class::$b' expression.
   */
  public Expr createClassField(Expr className,
                               Expr name)
  {
    return new ClassVarFieldVarExpr(className, name);
  }

  /**
   * Creates a class static field 'static::${b}' expression.
   */
  public Expr createClassVirtualField(Expr name)
  {
    return new ClassVirtualFieldVarExpr(name);
  }

  //
  // unary expressions
  //

  /**
   * Creates an unset '$a' expression.
   */
  public Expr createUnsetVar(AbstractVarExpr var)
  {
    return new VarUnsetExpr(var);
  }

  /**
   * Creates a char at 'a{0}' expression.
   */
  public BinaryCharAtExpr createCharAt(Expr base, Expr index)
  {
    return new BinaryCharAtExpr(base, index);
  }

  /**
   * Creates a post increment 'a++' expression.
   */
  public UnaryPostIncrementExpr createPostIncrement(Expr expr, int incr)
  {
    return new UnaryPostIncrementExpr(expr, incr);
  }

  /**
   * Creates a pre increment '++a' expression.
   */
  public UnaryPreIncrementExpr createPreIncrement(Expr expr, int incr)
  {
    return new UnaryPreIncrementExpr(expr, incr);
  }

  /**
   * Creates a unary minus '-a' expression.
   */
  public Expr createMinus(Expr expr)
  {
    return new UnaryMinusExpr(expr);
  }

  /**
   * Creates a unary plus '+a' expression.
   */
  public Expr createPlus(Expr expr)
  {
    return new UnaryPlusExpr(expr);
  }

  /**
   * Creates a unary not '!a' expression.
   */
  public Expr createNot(Expr expr)
  {
    return new UnaryNotExpr(expr);
  }

  /**
   * Creates a unary inversion '~a' expression.
   */
  public Expr createBitNot(Expr expr)
  {
    return new UnaryBitNotExpr(expr);
  }

  /**
   * Creates a clone 'clone a' expression.
   */
  public Expr createClone(Expr expr)
  {
    return new FunCloneExpr(expr);
  }

  /**
   * Creates a clone 'clone a' expression.
   */
  public Expr createCopy(Expr expr)
  {
    return new UnaryCopyExpr(expr);
  }

  /**
   * Creates an error suppression '@a' expression.
   */
  public Expr createSuppress(Expr expr)
  {
    return new UnarySuppressErrorExpr(expr);
  }

  /**
   * Creates a boolean cast
   */
  public Expr createToBoolean(Expr expr)
  {
    return new ToBooleanExpr(expr);
  }

  /**
   * Creates a long cast
   */
  public Expr createToLong(Expr expr)
  {
    return new ToLongExpr(expr);
  }

  /**
   * Creates a double cast
   */
  public Expr createToDouble(Expr expr)
  {
    return new ToDoubleExpr(expr);
  }

  /**
   * Creates a string cast
   */
  public Expr createToString(Expr expr)
  {
    return new ToStringExpr(expr);
  }

  /**
   * Creates a unicode cast
   */
  public Expr createToUnicode(Expr expr)
  {
    return new ToUnicodeExpr(expr);
  }

  /**
   * Creates a binary string cast
   */
  public Expr createToBinary(Expr expr)
  {
    return new ToBinaryExpr(expr);
  }

  /**
   * Creates an object cast
   */
  public Expr createToObject(Expr expr)
  {
    return new ToObjectExpr(expr);
  }

  /**
   * Creates an array cast
   */
  public Expr createToArray(Expr expr)
  {
    return new ToArrayExpr(expr);
  }

  /**
   * Creates a die 'die("msg")' expression.
   */
  public Expr createDie(Expr expr)
  {
    return new FunDieExpr(expr);
  }

  /**
   * Creates an exit 'exit("msg")' expression.
   */
  public Expr createExit(Expr expr)
  {
    return new FunExitExpr(expr);
  }

  /**
   * Creates a required
   */
  public Expr createRequired()
  {
    return ParamRequiredExpr.REQUIRED;
  }

  /**
   * Creates a default
   */
  public Expr createDefault()
  {
    return new ParamDefaultExpr();
  }

  /**
   * Creates an addition expression.
   */
  public Expr createAdd(Expr left, Expr right)
  {
    return new BinaryAddExpr(left, right);
  }

  /**
   * Creates a subtraction expression.
   */
  public Expr createSub(Expr left, Expr right)
  {
    return new BinarySubExpr(left, right);
  }

  /**
   * Creates a multiplication expression.
   */
  public Expr createMul(Expr left, Expr right)
  {
    return new BinaryMulExpr(left, right);
  }

  /**
   * Creates a division expression.
   */
  public Expr createDiv(Expr left, Expr right)
  {
    return new BinaryDivExpr(left, right);
  }

  /**
   * Creates a modulo expression.
   */
  public Expr createMod(Expr left, Expr right)
  {
    return new BinaryModExpr(left, right);
  }

  /**
   * Creates a left-shift expression.
   */
  public Expr createLeftShift(Expr left, Expr right)
  {
    return new BinaryLeftShiftExpr(left, right);
  }

  /**
   * Creates a right-shift expression.
   */
  public Expr createRightShift(Expr left, Expr right)
  {
    return new BinaryRightShiftExpr(left, right);
  }

  /**
   * Creates a bit-and expression.
   */
  public Expr createBitAnd(Expr left, Expr right)
  {
    return new BinaryBitAndExpr(left, right);
  }

  /**
   * Creates a bit-or expression.
   */
  public Expr createBitOr(Expr left, Expr right)
  {
    return new BinaryBitOrExpr(left, right);
  }

  /**
   * Creates a bit-xor expression.
   */
  public Expr createBitXor(Expr left, Expr right)
  {
    return new BinaryBitXorExpr(left, right);
  }

  /**
   * Creates an append expression
   */
  public final Expr createAppend(Expr left, Expr right)
  {
    BinaryAppendExpr leftAppend;

    // XXX: i18n binary vs unicode issues
    /*
    if (left instanceof ToStringExpr)
      left = ((ToStringExpr) left).getExpr();

    if (left instanceof StringLiteralExpr) {
      StringLiteralExpr string = (StringLiteralExpr) left;

      if (string.evalConstant().length() == 0)
        return ToStringExpr.create(right);
    }
    */

    if (left instanceof BinaryAppendExpr)
      leftAppend = (BinaryAppendExpr) left;
    else
      leftAppend = createAppendImpl(left, null);

    BinaryAppendExpr next;

    /*
    if (right instanceof ToStringExpr)
      right = ((ToStringExpr) right).getExpr();

    if (right instanceof StringLiteralExpr) {
      StringLiteralExpr string = (StringLiteralExpr) right;

      if (string.evalConstant().length() == 0)
        return ToStringExpr.create(left);
    }
    */

    if (right instanceof BinaryAppendExpr)
      next = (BinaryAppendExpr) right;
    else
      next = createAppendImpl(right, null);

    BinaryAppendExpr result = append(leftAppend, next);

    if (result.getNext() != null)
      return result;
    else
      return result.getValue();
  }

  /**
   * Appends the tail to the current expression, combining
   * constant literals.
   */
  private BinaryAppendExpr append(BinaryAppendExpr left, BinaryAppendExpr tail)
  {
    if (left == null)
      return tail;

    tail = append(left.getNext(), tail);

    if (left.getValue() instanceof LiteralBinaryStringExpr
        && tail.getValue() instanceof LiteralBinaryStringExpr) {
      LiteralBinaryStringExpr leftString
        = (LiteralBinaryStringExpr) left.getValue();
      LiteralBinaryStringExpr rightString
        = (LiteralBinaryStringExpr) tail.getValue();

      try {
        byte []bytes = (leftString.evalConstant().toString()
            + rightString.evalConstant().toString()).getBytes("ISO-8859-1");

        Expr value = createBinary(bytes);

        return createAppendImpl(value, tail.getNext());
      } catch (java.io.UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    else if (left.getValue() instanceof LiteralBinaryStringExpr
             || tail.getValue() instanceof LiteralBinaryStringExpr) {
      left.setNext(tail);

      return left;
    }
    else if (left.getValue() instanceof LiteralStringExpr
             && tail.getValue() instanceof LiteralStringExpr) {
      LiteralStringExpr leftString = (LiteralStringExpr) left.getValue();
      LiteralStringExpr rightString = (LiteralStringExpr) tail.getValue();

      Expr value = createString(leftString.evalConstant().toString()
                                + rightString.evalConstant().toString());

      return createAppendImpl(value, tail.getNext());
    }
    else if (left.getValue() instanceof LiteralUnicodeExpr
             && tail.getValue() instanceof LiteralUnicodeExpr) {
      LiteralUnicodeExpr leftString = (LiteralUnicodeExpr) left.getValue();
      LiteralUnicodeExpr rightString = (LiteralUnicodeExpr) tail.getValue();

      Expr value = createUnicode(leftString.evalConstant().toString()
                                 + rightString.evalConstant().toString());

      return createAppendImpl(value, tail.getNext());
    }
    else {
      left.setNext(tail);

      return left;
    }
  }

  protected BinaryAppendExpr createAppendImpl(Expr left, BinaryAppendExpr right)
  {
    return new BinaryAppendExpr(left, right);
  }

  /**
   * Creates a lt expression.
   */
  public Expr createLt(Expr left, Expr right)
  {
    return new BinaryLtExpr(left, right);
  }

  /**
   * Creates a leq expression.
   */
  public Expr createLeq(Expr left, Expr right)
  {
    return new BinaryLeqExpr(left, right);
  }

  /**
   * Creates a gt expression.
   */
  public Expr createGt(Expr left, Expr right)
  {
    return new BinaryGtExpr(left, right);
  }

  /**
   * Creates a geq expression.
   */
  public Expr createGeq(Expr left, Expr right)
  {
    return new BinaryGeqExpr(left, right);
  }

  /**
   * Creates an eq expression.
   */
  public Expr createEq(Expr left, Expr right)
  {
    return new BinaryEqExpr(left, right);
  }

  /**
   * Creates a neq expression.
   */
  public Expr createNeq(Expr left, Expr right)
  {
    return new BinaryNeqExpr(left, right);
  }

  /**
   * Creates an equals expression.
   */
  public Expr createEquals(Expr left, Expr right)
  {
    return new BinaryEqualsExpr(left, right);
  }

  /**
   * Creates an assignment expression.
   */
  public Expr createAssign(AbstractVarExpr left, Expr right)
  {
    return new BinaryAssignExpr(left, right);
  }

  /**
   * Creates an assignment expression.
   */
  public Expr createAssignRef(AbstractVarExpr left, Expr right)
  {
    return new BinaryAssignRefExpr(left, right);
  }

  /**
   * Creates a ref '&$a' expression.
   */
  public UnaryRefExpr createRef(Expr base)
  {
    return new UnaryRefExpr(base);
  }

  /**
   * Creates an and expression.
   */
  public Expr createAnd(Expr left, Expr right)
  {
    return new BinaryAndExpr(left, right);
  }

  /**
   * Creates an or expression.
   */
  public Expr createOr(Expr left, Expr right)
  {
    return new BinaryOrExpr(left, right);
  }

  /**
   * Creates an xor expression.
   */
  public Expr createXor(Expr left, Expr right)
  {
    return new BinaryXorExpr(left, right);
  }

  /**
   * Creates a comma expression.
   */
  public Expr createComma(Expr left, Expr right)
  {
    return new BinaryCommaExpr(left, right);
  }

  /**
   * Creates an instanceof expression.
   */
  public Expr createInstanceOf(Expr expr, String name)
  {
    return new BinaryInstanceOfExpr(expr, name);
  }

  /**
   * Creates an instanceof expression.
   */
  public Expr createInstanceOfVar(Expr expr, Expr name)
  {
    return new BinaryInstanceOfVarExpr(expr, name);
  }

  /**
   * Creates an each expression.
   */
  public Expr createEach(Expr expr)
  {
    return new FunEachExpr(expr);
  }

  /**
   * Creates a list expression.
   */
  public final Expr createList(QuercusParser parser,
                               ListHeadExpr head, Expr value)
  {
    boolean isSuppress = value instanceof UnarySuppressErrorExpr;

    if (isSuppress) {
      UnarySuppressErrorExpr suppressExpr = (UnarySuppressErrorExpr) value;

      value = suppressExpr.getExpr();
    }

    Expr expr;

    if (value instanceof FunEachExpr) {
      Expr arg = ((FunEachExpr) value).getExpr();

      expr = createListEach(head, arg);
    }
    else
      expr = createList(head, value);

    if (isSuppress)
      return createSuppress(expr);
    else
      return expr;
  }

  /**
   * Creates a list expression.
   */
  public ListHeadExpr createListHead(ArrayList<Expr> keys)
  {
    return new ListHeadExpr(keys);
  }

  /**
   * Creates a list expression.
   */
  public Expr createList(ListHeadExpr head, Expr value)
  {
    return new BinaryAssignListExpr(head, value);
  }

  /**
   * Creates a list expression.
   */
  public Expr createListEach(ListHeadExpr head, Expr value)
  {
    return new BinaryAssignListEachExpr(head, value);
  }

  /**
   * Creates an conditional expression.
   */
  public Expr createConditional(Expr test, Expr left, Expr right)
  {
    return new ConditionalExpr(test, left, right);
  }

  /**
   * Creates an conditional expression.
   */
  public Expr createShortConditional(Expr test, Expr right)
  {
    return new ConditionalShortExpr(test, right);
  }

  /**
   * Creates a array() expression.
   */
  public Expr createArrayFun(ArrayList<Expr> keys, ArrayList<Expr> values)
  {
    return new FunArrayExpr(keys, values);
  }

  /**
   * Creates a new function call.
   */
  public Expr createCall(QuercusParser parser,
                         String name,
                         ArrayList<Expr> args)
  {
    Location loc = parser.getLocation();
    
    if ("isset".equals(name) && args.size() == 1)
      return new FunIssetExpr(args.get(0));
    else if ("get_called_class".equals(name) && args.size() == 0)
      return new FunGetCalledClassExpr(loc);
    else if ("get_class".equals(name) && args.size() == 0)
      return new FunGetClassExpr(parser);
    else if ("each".equals(name) && args.size() == 1) {
      Expr arg = args.get(0);
      
      if (! arg.isVar()) {
        parser.error(L.l("each() argument must be a variable at '{0}'", arg));
      }

      return new FunEachExpr(arg);
    }
    else
      return new CallExpr(loc, name, args);
  }

  /**
   * Creates a new var function call.
   */
  public CallVarExpr createVarFunction(Location loc,
                                           Expr name,
                                           ArrayList<Expr> args)
  {
    return new CallVarExpr(loc, name, args);
  }

  /**
   * Creates a new closure.
   */
  public ClosureExpr createClosure(Location loc,
                                   Function fun,
                                   ArrayList<VarExpr> useArgs)
  {
    return new ClosureExpr(loc, fun);
  }

  //
  // methods
  //

  /**
   * Creates a method call $a->foo(...).
   */
  public Expr createMethodCall(Location loc,
                               Expr objExpr,
                               String methodName,
                               ArrayList<Expr> args)
  {
    return new ObjectMethodExpr(loc, objExpr, methodName, args);
  }

  /**
   * Creates a variable method call $a->${"foo"}(...).
   */
  public Expr createMethodCall(Location loc,
                               Expr objExpr,
                               Expr methodName,
                               ArrayList<Expr> args)
  {
    return new ObjectMethodVarExpr(loc, objExpr, methodName, args);
  }

  /**
   * Creates a class method call A::foo(...)
   */
  public Expr createClassMethodCall(Location loc,
                                    String className,
                                    String methodName,
                                    ArrayList<Expr> args)
  {
    return new ClassMethodExpr(loc, className, methodName, args);
  }

  /**
   * Creates a class method call ${class}::foo(...)
   */
  public Expr createClassMethodCall(Location loc,
                                    Expr className,
                                    String methodName,
                                    ArrayList<Expr> args)
  {
    return new ClassVarMethodExpr(loc, className, methodName, args);
  }

  /**
   * Creates a new function call based on the class context.
   */
  public Expr createClassVirtualMethodCall(Location loc,
                                           String methodName,
                                           ArrayList<Expr> args)
  {
    return new ClassVirtualMethodExpr(loc, methodName, args);
  }

  /**
   * Creates a new method A::$f()
   */
  public Expr createClassMethodCall(Location loc,
                                    String className,
                                    Expr methodName,
                                    ArrayList<Expr> args)
  {
    return new ClassMethodVarExpr(loc, className, methodName, args);
  }

  /**
   * Creates a new method ${class}::$f()
   */
  public Expr createClassMethodCall(Location loc,
                                    Expr className,
                                    Expr methodName,
                                    ArrayList<Expr> args)
  {
    return new ClassVarMethodVarExpr(loc, className, methodName, args);
  }

  /**
   * Creates a new method static::$f()
   */
  public Expr createClassVirtualMethodCall(Location loc,
                                           Expr var,
                                           ArrayList<Expr> args)
  {
    return new ClassVirtualMethodVarExpr(loc, var, args);
  }

  /**
   * Creates a class method call A::foo(...)
   */
  public Expr createClassConstructor(Location loc,
                                     String className,
                                     String methodName,
                                     ArrayList<Expr> args)
  {
    return new ClassConstructorExpr(loc, className, methodName, args);
  }

  /**
   * Creates a parent method call parent::foo(...)
   *
   * XXX: isn't this lexical?
   */
  /*
  public Expr createParentClassMethod(Location loc,
                                      String parentName,
                                      String name,
                                      ArrayList<Expr> args)
  {
    return new ParentMethodExpr(loc, parentName, name, args);
  }
  */

  /**
   * Creates a static function call.
   */
  /*
  public Expr createStaticMethod(Location loc,
                                 String className,
                                 String name,
                                 ArrayList<Expr> args)
  {
    return new StaticMethodExpr(loc, className, name, args);
  }
  */

  /**
   * Creates a static function call based on the calling class.
   */
  /*
  public Expr createLateStaticBindingStaticMethod(Location loc,
                                                  String name,
                                                  ArrayList<Expr> args)
  {
    return new LateStaticBindingStaticMethodExpr(loc, name, args);
  }
  */

  /**
   * Creates a new function call new foo(...).
   */
  public ObjectNewExpr createNew(Location loc,
                           String name,
                           ArrayList<Expr> args)
  {
    return new ObjectNewExpr(loc, name, args);
  }

  /**
   * Creates a new function call.
   */
  public ObjectNewVarExpr createVarNew(Location loc,
                                 Expr name,
                                 ArrayList<Expr> args)
  {
    return new ObjectNewVarExpr(loc, name, args);
  }

  /**
   * Creates an include expr
   */
  public Expr createInclude(Location loc,
                            Path source,
                            Expr expr)
  {
    return new FunIncludeExpr(loc, source, expr, false);
  }

  /**
   * Creates an include expr
   */
  public Expr createRequire(Location loc,
                            Path source,
                            Expr expr)
  {
    return new FunIncludeExpr(loc, source, expr, true);
  }

  /**
   * Creates an include expr
   */
  public Expr createIncludeOnce(Location loc,
                                Path source,
                                Expr expr)
  {
    return new FunIncludeOnceExpr(loc, source, expr, false);
  }

  /**
   * Creates an include expr
   */
  public Expr createRequireOnce(Location loc,
                                Path source,
                                Expr expr)
  {
    return new FunIncludeOnceExpr(loc, source, expr, true);
  }

  /**
   * Creates a Quercus class import.
   */
  public Expr createImport(Location loc,
                           String name,
                           boolean isWildcard)
  {
    return new ImportExpr(loc, name, isWildcard);
  }

  //
  // statements
  //

  /**
   * Creates a null literal expression.
   */
  public Statement createNullStatement()
  {
    return NullStatement.NULL;
  }

  /**
   * Creates an echo statement
   */
  public Statement createEcho(Location loc, Expr expr)
  {
    return new EchoStatement(loc, expr);
  }

  /**
   * Creates an expr statement
   */
  public Statement createExpr(Location loc, Expr expr)
  {
    return new ExprStatement(loc, expr);
  }

  public final Statement createBlock(Location loc,
                                     ArrayList<Statement> statementList)
  {
    if (statementList.size() == 1)
      return statementList.get(0);

    Statement []statements = new Statement[statementList.size()];

    statementList.toArray(statements);

    return createBlockImpl(loc, statements);
  }

  public final Statement createBlock(Location loc, Statement []statementList)
  {
    if (statementList.length == 1)
      return statementList[0];

    Statement []statements = new Statement[statementList.length];

    System.arraycopy(statementList, 0, statements, 0, statementList.length);

    return createBlockImpl(loc, statements);
  }

  /**
   * Creates an expr statement
   */
  public final BlockStatement createBlockImpl(Location loc,
                                              ArrayList<Statement> statementList
  )
  {
    Statement []statements = new Statement[statementList.size()];

    statementList.toArray(statements);

    return createBlockImpl(loc, statements);
  }

  /**
   * Creates an expr statement
   */
  public BlockStatement createBlockImpl(Location loc, Statement []statements)
  {
    return new BlockStatement(loc, statements);
  }

  /**
   * Creates a text statement
   */
  public Statement createText(Location loc, String text)
  {
    return new TextStatement(loc, text);
  }

  /**
   * Creates an if statement
   */
  public Statement createIf(Location loc,
                            Expr test,
                            Statement trueBlock,
                            Statement falseBlock)
  {
    return new IfStatement(loc, test, trueBlock, falseBlock);
  }

  /**
   * Creates a switch statement
   */
  public Statement createSwitch(Location loc,
                                Expr value,
                                ArrayList<Expr[]> caseList,
                                ArrayList<BlockStatement> blockList,
                                Statement defaultBlock,
                                String label)
  {
    return new SwitchStatement(loc, value, caseList, blockList,
                               defaultBlock, label);
  }

  /**
   * Creates a for statement
   */
  public Statement createFor(Location loc,
                             Expr init,
                             Expr test,
                             Expr incr,
                             Statement block,
                             String label)
  {
    return new ForStatement(loc, init, test, incr, block, label);
  }

  /**
   * Creates a foreach statement
   */
  public Statement createForeach(Location loc,
                                 Expr objExpr,
                                 AbstractVarExpr key,
                                 AbstractVarExpr value,
                                 boolean isRef,
                                 Statement block,
                                 String label)
  {
    return new ForeachStatement(loc, objExpr, key, value, isRef,
                                block, label);
  }

  /**
   * Creates a while statement
   */
  public Statement createWhile(Location loc,
                               Expr test,
                               Statement block,
                               String label)
  {
    return new WhileStatement(loc, test, block, label);
  }

  /**
   * Creates a do-while statement
   */
  public Statement createDo(Location loc,
                            Expr test,
                            Statement block,
                            String label)
  {
    return new DoStatement(loc, test, block, label);
  }

  /**
   * Creates a break statement
   */
  public BreakStatement createBreak(Location location,
                                    Expr target,
                                    ArrayList<String> loopLabelList)
  {
    return new BreakStatement(location, target, loopLabelList);
  }

  /**
   * Creates a continue statement
   */
  public ContinueStatement createContinue(Location location,
                                          Expr target,
                                          ArrayList<String> loopLabelList)
  {
    return new ContinueStatement(location, target, loopLabelList);
  }

  /**
   * Creates a global statement
   */
  public Statement createGlobal(Location loc,
                                VarExpr var)
  {
    return new GlobalStatement(loc, var);
  }

  /**
   * Creates a global var statement
   */
  public Statement createVarGlobal(Location loc,
                                   VarVarExpr var)
  {
    return new VarGlobalStatement(loc, var);
  }

  /**
   * Creates a static statement inside a class
   */
  public Statement createClassStatic(Location loc,
                                     String className,
                                     VarExpr var,
                                     Expr value)
  {
    return new ClassStaticStatement(loc, className, var, value);
  }

  /**
   * Creates a static statement
   */
  public Statement createStatic(Location loc,
                                VarExpr var,
                                Expr value)
  {
    return new StaticStatement(loc, var, value);
  }

  /**
   * Creates a throw statement
   */
  public Statement createThrow(Location loc,
                               Expr value)
  {
    return new ThrowStatement(loc, value);
  }

  /**
   * Creates a try statement
   */
  public TryStatement createTry(Location loc,
                                Statement block)
  {
    return new TryStatement(loc, block);
  }

  /**
   * Creates a return statement
   */
  public Statement createReturn(Location loc,
                                Expr value)
  {
    return new ReturnStatement(loc, value);
  }

  /**
   * Creates a return ref statement
   */
  public Statement createReturnRef(Location loc,
                                   Expr value)
  {
    return new ReturnRefStatement(loc, value);
  }

  /**
   * Creates a new function definition def.
   */
  public Statement createFunctionDef(Location loc,
                                     Function fun)
  {
    return new FunctionDefStatement(loc, fun);
  }

  /**
   * Creates a new function def statement
   */
  public Statement createClassDef(Location loc,
                                  InterpretedClassDef cl)
  {
    return new ClassDefStatement(loc, cl);
  }

  //
  // functions
  //

  /**
   * Creates a new FunctionInfo
   */
  public FunctionInfo createFunctionInfo(QuercusContext quercus, 
                                         ClassDef classDef,
                                         String name)
  {
    return new FunctionInfo(quercus, classDef, name);
  }

  /**
   * Creates a new function definition.
   */
  public Function createFunction(Location loc,
                                 String name,
                                 FunctionInfo info,
                                 Arg []argList,
                                 Statement []statementList)
  {
    return new Function(this, loc, name, info, argList, statementList);
  }

  /**
   * Creates a new object method definition.
   */
  public Function createObjectMethod(Location loc,
                                     InterpretedClassDef cl,
                                     String name,
                                     FunctionInfo info,
                                     Arg []argList,
                                     Statement []statementList)
  {
    return new ObjectMethod(this, loc, cl, name, info, argList, statementList);
  }

  /**
   * Creates a new object method definition.
   */
  public Function createMethodDeclaration(Location loc,
                                          InterpretedClassDef cl,
                                          String name,
                                          FunctionInfo info,
                                          Arg []argList)
  {
    return new MethodDeclaration(this, loc, cl, name, info, argList);
  }

  public InterpretedClassDef createClassDef(Location location,
                                            String name,
                                            String parentName,
                                            String[] ifaceList,
                                            int index)
  {
    return new InterpretedClassDef(location,
                                   name, parentName, ifaceList,
                                   index);
  }
}

