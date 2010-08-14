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

package com.caucho.es.parser;

import com.caucho.es.ESBase;
import com.caucho.es.ESException;
import com.caucho.es.ESId;
import com.caucho.es.ESParseException;
import com.caucho.es.Script;
import com.caucho.java.JavaCompiler;
import com.caucho.java.LineMap;
import com.caucho.loader.SimpleLoader;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntArray;
import com.caucho.util.L10N;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Parser is a factory for generating compiled Script objects.
 *
 * <p>Most applications will use the <code>parse(String)</code> interface
 * to parse JavaScript.  That method will try to load a precompiled
 * script from the work directory before trying to parse it.
 *
 * <p>Applications will often set the script path a directory for
 * script and include the classpath in the path.  Applications will
 * often override the work dir for a more appropriate work directory.
 *
 * <code><pre>
 * package com.caucho.vfs.*;
 * package com.caucho.es.*;
 *
 * ...
 *
 * com.caucho.es.parser.Parser parser;
 * parser = new com.caucho.es.parser.Parser();
 *
 * // configure the path to search for *.js files
 * MergePath scriptPath = new MergePath();
 * scriptPath.addMergePath(Vfs.lookup("/home/ferg/js"));
 * ClassLoader loader = Thread.currentThread().contextClassLoader();
 * scriptPath.addClassPath(loader);
 * parser.setScriptPath(scriptPath);
 *
 * // configure the directory storing compiled scripts
 * Path workPath = Vfs.lookup("/tmp/caucho/work");
 * parser.setWorkDir(workPath);
 * 
 * Script script = parser.parse("test.js");
 * </pre></code>
 */
public class Parser {
  private static final Logger log
    = Logger.getLogger(Parser.class.getName());
  private static final L10N L = new L10N(Parser.class);
  private static final Object LOCK = new Object();
  
  static ESId CLINIT = ESId.intern("__clinit__");
  static ESId PROTOTYPE = ESId.intern("prototype");
  static ESId FINALLY = ESId.intern("finally");
  static ESId ANONYMOUS = ESId.intern("anonymous");
  static ESId OBJECT = ESId.intern("Object");
  static ESId REGEXP = ESId.intern("RegExp");
  static ESId ARRAY = ESId.intern("Array");
  static ESId LENGTH = ESId.intern("length");
  static ESId PACKAGES = ESId.intern("Packages");
  static ESId JAVA = ESId.intern("java");
  static ESId COM = ESId.intern("com");
  static ESId CAUCHO = ESId.intern("caucho");

  static final int PREC_DOT = 1;
  static final int PREC_POST = PREC_DOT;
  static final int PREC_FUN = PREC_POST + 1;
  static final int PREC_UMINUS = PREC_FUN + 1;
  static final int PREC_TIMES = PREC_UMINUS + 1;
  static final int PREC_PLUS = PREC_TIMES + 1;
  static final int PREC_SHIFT = PREC_PLUS + 1;
  static final int PREC_CMP = PREC_SHIFT + 1;
  static final int PREC_EQ = PREC_CMP + 1;
  static final int PREC_BITAND = PREC_EQ + 1;
  static final int PREC_BITXOR = PREC_BITAND + 1;
  static final int PREC_BITOR = PREC_BITXOR + 1;
  static final int PREC_AND = PREC_BITOR + 1;
  static final int PREC_OR = PREC_AND + 1;
  static final int PREC_COND = PREC_OR + 1;
  static final int PREC_ASSIGN = PREC_COND + 1;
  static final int PREC_COMMA = PREC_ASSIGN + 1;
  static final int PREC_MAX = PREC_COMMA + 1;

  ClassLoader parentLoader;
  ClassLoader loader;
  
  Path scriptPath;
  boolean isEval;
  // Name of the generated class
  String className;
  
  Lexer lexer;
  IntArray hashes = new IntArray();

  ArrayList importList = new ArrayList();

  ParseClass parseClass;
  Function globalFunction;
  Function staticFunction;
  Function function; // the current prototype, i.e. class
  Block block;

  LineMap lineMap;
  Path workPath;
  //JavaCompiler compiler;
  boolean isFast;

  public Parser()
  {
    workPath = CauchoSystem.getWorkPath();
    //compiler.setEncoding("utf8");
    addImport("java.lang.*");
    addImport("com.caucho.jslib.*");
  }

  /**
   * Sets the parent class loader.  If unspecified, defaults to the context
   * classloader.  Most applications will just use the default.
   *
   * @param parentLoader the classloader to be used for the script's parent.
   */
  public void setParentLoader(ClassLoader parentLoader)
  {
    this.parentLoader = parentLoader;
  }

  /**
   * Internal method to set the actual class loader.
   * Normally, this should only be called from com.caucho.es functions.
   */
  public void setClassLoader(ClassLoader loader)
  {
    this.loader = loader;
  }

  /**
   * Returns the current class loader.  If null, creates from the parent
   * loader and the work-dir.
   */
  ClassLoader getClassLoader()
  {
    if (loader != null)
      return loader;

    if (parentLoader != null)
      loader = SimpleLoader.create(parentLoader, workPath, null);
    else
      loader = SimpleLoader.create(null, workPath, null);

    return loader;
  }

  /**
   * Sets the path to search for imported javascript source files.
   * Normally, ScriptPath will be a MergePath.  If the MergePath
   * adds the classpath, then JavaScript files can be put in the
   * normal Java classpath.
   *
   * <p>If the ScriptPath is not specified, it will use the
   * current directory and the classpath from the context class loader.
   *
   * <code><pre>
   * MergePath scriptPath = new MergePath();
   * scriptPath.addMergePath(Vfs.lookup("/home/ferg/js"));
   *
   * ClassLoader loader = Thread.currentThread().contextClassLoader();
   * scriptPath.addClassPath(loader);
   *
   * parser.setScriptPath(scriptPath);
   * </pre></code>
   *
   * @param scriptPath path to search for imported scripts.
   */
  public void setScriptPath(Path scriptPath)
  {
    this.scriptPath = scriptPath;
  }

  /**
   * Returns the path to search for imported javascript.  Normally, scriptPath
   * will be a MergePath.
   *
   * @param scriptPath path to search for imported scripts.
   */
  public Path getScriptPath()
  {
    return scriptPath;
  }

  /**
   * Adds a package/script to be automatically imported by the script.
   * Each import is the equivalent of adding the following javascript:
   *
   * <code><pre>
   * package <em>name</em>;
   * </pre></code>
   *
   * @param name package or script name to be automatically imported.
   */
  public void addImport(String name)
  {
    if (! importList.contains(name))
      importList.add(name);
  }

  /**
   * Sets "fast" mode, i.e. JavaScript 2.0.  Fast mode lets the compiler
   * make assumptions about types and classes, e.g. that class methods
   * won't change dynamically.  This lets the compiler generate more code
   * that directly translates to Java calls.
   */
  public void setFast(boolean isFast)
  {
    this.isFast = isFast;
  }

  /**
   * Sets a line number map.  For generated files like JSP or XTP,
   * the error messages need an extra translation to get to the original
   * line numbers.
   */
  public void setLineMap(LineMap lineMap)
  {
    this.lineMap = lineMap;
  }

  /**
   * Sets the name of the generated java class.  If unset, the parser will
   * mangle the input name.
   */
  public void setClassName(String name)
  {
    this.className = name;
  }

  /**
   * Sets the directory for generated *.java and *.class files.
   * The parser will check this directory for any precompiled javascript
   * classes.  The default work-dir is /tmp/caucho on unix and
   * c:\temp\caucho on windows.
   *
   * @param path the work directory.
   */
  public void setWorkDir(Path path)
  {
    workPath = path;
  }

  /**
   * Returns the directory for generated *.java and *.class files.
   */
  public Path getWorkDir()
  {
    return workPath;
  }

  /**
   * Main application parsing method.  The parser will try to load
   * the compiled script.  If the compiled script exists and the
   * source file has not changed, parse will return the old script.
   * Otherwise, it will parse and compile the javascript.
   *
   * @param name the name of the javascript source.
   *
   * @return the parsed script
   */
  public Script parse(String name) throws ESException, IOException
  {
    Path path;

    try {
      String className;

      if (this.className != null)
        className = this.className;
      else
        className = "_js." + JavaCompiler.mangleName(name);
      
      if (scriptPath == null) {
        MergePath mergePath = new MergePath();
        mergePath.addMergePath(Vfs.lookup());
        ClassLoader parentLoader = this.parentLoader;
        if (parentLoader == null)
          parentLoader = Thread.currentThread().getContextClassLoader();
      
        mergePath.addClassPath(parentLoader);
        scriptPath = mergePath;
      }
      
      Script script = loadScript(className);

      if (! script.isModified()) {
        script.setScriptPath(getScriptPath());
        script.setClassDir(workPath);
        return script;
      }
    } catch (Throwable e) {
    }
  
    path = getScriptPath().lookup(name);

    ReadStream is = path.openRead();
    try {
      return parse(is, name, 1);
    } finally {
      is.close();
    }
  }

  /**
   * Alternative parsing method when the application only has an open
   * stream to the file.  Since this method will always compile a new script,
   * it can be significantly slower than the <code>parse(String)</code>
   * method.
   *
   * @param is a read stream to the javascript source.
   *
   * @return the parsed script.
   */
  public Script parse(ReadStream is) throws ESException, IOException
  {
    return parse(is, null, 1);
  }

  /**
   * An alternative parsing method given an open stream, a filename and
   * a line number.
   *
   * @param is a stream to the javascript source.
   * @param name filename to use for error messages.
   * @param line initial line number.
   *
   * @return the compiled script
   */
  public Script parse(ReadStream is, String name, int line)
    throws ESException, IOException
  {
    if (name == null)
      name = is.getUserPath();

    if (line <= 0)
      line = 1;
    
    return parse(is, name, line, false);
  }

  /**
   * Parses a script for the JavaScript "eval" expression.  The
   * semantics for "eval" are slightly different from standard
   * scripts.
   *
   * @param is stream to the eval source.
   * @param name filename to use for error messages
   * @param line initial line number to use for error messages.
   *
   * @return the compiled script.
   */
  public Script parseEval(ReadStream is, String name, int line)
    throws ESException, IOException
  {
    if (name == null)
      name = "eval";

    if (line <= 0)
      line = 1;

    return parse(is, name, line, true);
  }

  /**
   * The main parser method.
   *
   * @param is stream to read the script.
   * @param name the filename to use for error messages.
   * @param line the line number to use for error messages.
   * @param isEval if true, parse for an eval expression.
   *
   * @return the compiled javascript
   */
  private Script parse(ReadStream is, String name,
                       int line, boolean isEval)
    throws ESException, IOException
  {
    if (name == null)
      name = "anonymous";

    if (line <= 0)
      line = 1;
    
    lexer = new Lexer(is, name, line);
    if (lineMap != null)
      lexer.setLineMap(lineMap);
    
    if (className == null)
      className = "_js." + JavaCompiler.mangleName(name);
      
    if (scriptPath == null) {
      MergePath mergePath = new MergePath();
      if (is.getPath() != null)
        mergePath.addMergePath(is.getPath().getParent());
      else
        mergePath.addMergePath(Vfs.lookup());
      ClassLoader parentLoader = this.parentLoader;
      if (parentLoader == null)
        parentLoader = Thread.currentThread().getContextClassLoader();
      
      mergePath.addClassPath(parentLoader);
      scriptPath = mergePath;
    }
    
    block = null;

    JavaCompiler compiler = JavaCompiler.create(getClassLoader());
    compiler.setClassDir(workPath);

    parseClass = new ParseClass(name, className);
    parseClass.setParser(this);
    if (is.getPath() != null && is.getPath().getLastModified() > 0)
      parseClass.setSourcePath(is.getPath());

    globalFunction = parseClass.newFunction(null, ESId.intern("global"), false);
    globalFunction.setFast(isFast);
    staticFunction = parseClass.newFunction(null, ESId.intern("__es_static"), false);
    parseClass.setGlobal(globalFunction);

    if (isEval) {
      block = Block.create(this, globalFunction);
      block.finish();
      function = parseClass.newFunction(globalFunction, ESId.intern("eval"), false);
      function.setEval();
    }
    else
      function = globalFunction;
    
    block = Block.create(this, function);
    parseBlock(true);
    block.finish();

    if (lexer.peek() != Lexer.EOF)
      throw expect(L.l("end of file"));

    block = Block.create(this, staticFunction);
    block.finish();

    synchronized (LOCK) {
      Path path = workPath.lookup(className.replace('.', '/') + ".java");
      path.getParent().mkdirs();
    
      WriteStream os = path.openWrite();
      os.setEncoding("JAVA");
      parseClass.writeCode(os);
      os.close();

      Script script;
      try {
        compiler.compile(className.replace('.', '/') + ".java", null);
        script = loadScript(className);
      } catch (Exception e) {
        throw new ESParseException(e);
      }

      script.setScriptPath(getScriptPath());
      script.setClassDir(workPath);
      
      return script;
    }
  }

  /**
   * Tries to load an already compiled script.
   *
   * @param className mangled classname of the script.
   */
  private Script loadScript(String className)
    throws Exception
  {
    ClassLoader loader = getClassLoader();
    Class cl = CauchoSystem.loadClass(className, false, loader);
      
    Script script = (Script) cl.newInstance();
    script.setScriptPath(getScriptPath());
    script.setClassDir(workPath);

    return script;
  }

  /**
   * Parse a function
   */
  private Function parseFunction() throws ESException
  {
    function.setNeedsScope();

    ESId id = null;
    if (lexer.peek() == Lexer.IDENTIFIER) {
      lexer.next();
      id = lexer.getId();
    }

    if (lexer.next() != '(')
      throw expect("`('");

    if (id != null) {
      function.addVariable(block, id, null);
      block.newVar(id).getVar().setType(Expr.TYPE_ES);
    }

    Block oldBlock = block;
    Function oldFun = function;
    function = parseClass.newFunction(oldFun, id, false);
    
    oldFun.addFunction(function);
    
    block = Block.create(this, function);

    boolean isFirst = true;
    while (lexer.peek() != ')') {
      if (! isFirst && lexer.next() != ',')
        throw expect("`,'");
      isFirst = false;

      if (lexer.next() != Lexer.IDENTIFIER)
        throw expect(L.l("formal argument"));

      ESId argId = lexer.getId();
      
      Expr type = null;
      if (lexer.peek() == ':') {
        lexer.next();
        type = parseType();
      }

      function.addFormal(block, argId, type);
    }
    lexer.next();

    if (lexer.peek() == ':') {
      lexer.next();
      Expr type = parseType();

      function.setReturnType(type);
    }

    if (lexer.next() != '{')
        throw expect("`{'");

    parseBlock(false);

    if (lexer.next() != '}') {
        throw expect("`}'");
    }

    block.finish();

    Function newFun = function;

    function = oldFun;
    block = oldBlock;

    return newFun;
  }

  /**
   * Parses a list of statements, returning a block representing the
   * statements.
   *
   * @param parent the containing block
   * @param isTop true if this is a top level block
   */
  private void parseBlock(boolean isTop) 
    throws ESException
  {
    loop:
    while (true) {
      switch (lexer.peek()) {
      case Lexer.UNARY_OP:
      case Lexer.BANDU_OP:
      case Lexer.NEW:
      case Lexer.DELETE:
      case Lexer.VOID:
      case Lexer.TYPEOF:
      case Lexer.POSTFIX:
      case Lexer.IDENTIFIER:
      case Lexer.THIS:
      case Lexer.EVAL:
      case Lexer.LITERAL:
      case Lexer.REGEXP:
      case '(':
      case '[':
      case Lexer.HASH_REF:
      case Lexer.HASH_DEF:
      case Lexer.FUNCTION:
        parseStatement();
        break;

      case Lexer.IF:
      case Lexer.FOR:
      case Lexer.WHILE:
      case Lexer.DO:
      case Lexer.VAR:
      case Lexer.BREAK:
      case Lexer.WITH:
      case Lexer.SYNCHRONIZED:
      case Lexer.CONTINUE:
      case Lexer.RETURN:
      case Lexer.SWITCH:
      case Lexer.TRY:
      case Lexer.THROW:
      case ';':
        parseStatement();
        break;
        
      case '{':
        block = block.startBlock();
        parseStatement();
        block = block.finishBlock();
        break;

      case Lexer.CATCH:
        block.doTry();
        parseCatch();
        break;

      case Lexer.FINALLY:
        block.doTry();
        parseFinally();
        break;

      case Lexer.CLASS:
        parseClass();
        break;

      case Lexer.IMPORT:
        parseImport();
        break;

      case Lexer.STATIC:
        if (true) throw new ESException("nostatus");
        //parseStatic();
        break;

      default:
        break loop;
      }
    }
  }

  /**
   * Parses a statement.
   */
  private void parseStatement() throws ESException
  {
    int lexeme = lexer.peek();
    hashes.clear();
    int line = lexer.getLine();
    Expr expr = null;

    block.setLine(line);

    if (block.isDead) {
      switch (lexeme) {
      case ';':
      case Lexer.VAR:
      case Lexer.FUNCTION:
      case Lexer.CATCH:
      case Lexer.FINALLY:
        break;

      default:
        throw error(L.l("can't reach statement"));
      }
    }
    
    switch (lexeme) {
    case Lexer.IDENTIFIER:
      parseIdentifierStatement();
      break;

    case Lexer.UNARY_OP:
    case Lexer.BANDU_OP:
    case Lexer.NEW:
    case Lexer.THIS:
    case Lexer.EVAL:
    case Lexer.LITERAL:
    case Lexer.REGEXP:
    case Lexer.POSTFIX:
    case Lexer.DELETE:
    case Lexer.VOID:
    case Lexer.TYPEOF:
    case '(':
    case '[':
    case Lexer.HASH_REF:
    case Lexer.HASH_DEF:
      block.addExpr(parseExpression(PREC_MAX, true));
      parseStatementEnd();
      break;

    case Lexer.FUNCTION:
      lexer.next();
      Function newFun = parseFunction();
      break;

    case Lexer.VAR:
      parseVar(false);
      parseStatementEnd();
      break;

    case Lexer.BREAK:
      lexer.next();
      if (lexer.peek() == Lexer.IDENTIFIER && ! lexer.seenLineFeed()) {
        block.doBreak(lexer.getId());
        lexer.next();
      } else
        block.doBreak();
      parseStatementEnd();
      break;

    case Lexer.CONTINUE:
      lexer.next();
      if (lexer.peek() == Lexer.IDENTIFIER && ! lexer.seenLineFeed()) {
        block.doContinue(lexer.getId());
        lexer.next();
      } else
        block.doContinue();
      
      parseStatementEnd();
      break;

    case Lexer.RETURN:
      lexer.next();

      if (lexer.peek() == ';' || lexer.peek() == '}' || 
          lexer.seenLineFeed()) {
        block.doReturn();
      } else {
        block.doReturn(parseExpression(PREC_MAX, true));
      }
      
      parseStatementEnd();
      break;

    case Lexer.IF:
      parseIf();
      break;

    case Lexer.SWITCH:
      parseSwitch();
      break;

    case Lexer.WHILE:
      parseWhile(null);
      break;

    case Lexer.DO:
      parseDo(null);
      break;

    case Lexer.FOR:
      parseFor(null);
      break;

    case Lexer.WITH:
      parseWith();
      break;

    case Lexer.SYNCHRONIZED:
      parseSynchronized();
      break;

    case Lexer.TRY:
      parseTry();
      break;

    case Lexer.THROW:
      lexer.next();
      block.doThrow(parseExpression(PREC_MAX));
      break;

    case ';':
      lexer.next();
      break;

    case '{':
      lexer.next();
      parseBlock(false);
      if (lexer.next() != '}')
        throw expect("`}'");
      break;

    default:
      throw expect(L.l("statement"));
    }
  }

  private void parseStatementEnd() throws ESException
  {
    if (lexer.peek() == ';')
      lexer.next();
    else if (lexer.peek() == '}' ||
             lexer.peek() == Lexer.EOF || lexer.seenLineFeed()) {
    } else {
      throw expect("`;'");
    }
  }

  private void parseIdentifierStatement()
    throws ESException
  {
    ESId id = lexer.getId();
    int line = lexer.getLine();

    lexer.next();
    if (lexer.peek() != ':') {
      Expr var = getVar(id);
      Expr expr = parseExprRec(parseTermTail(var, false, true),
                               PREC_MAX, false, true);
      block.addExpr(expr);
      parseStatementEnd();
      return;
    }

    lexer.next();

    /*
    if (findLoop(null, id) != null)
      throw error("duplicate label `" + id + "'");
    */

    switch (lexer.peek()) {
    case Lexer.WHILE:
      parseWhile(id);
      break;

    case Lexer.DO:
      parseDo(id);
      break;

    case Lexer.FOR:
      parseFor(id);
      break;

    default:
      block = block.startBlock(id);
      parseStatement();
      block = block.finishBlock();
      break;
    }
  }

  private Expr parseType() throws ESException
  {
    if (lexer.next() != Lexer.IDENTIFIER)
      throw expect(L.l("identifier"));

    Expr type = block.newType(lexer.getId());

    while (lexer.peek() == '.') {
      lexer.next();
      if (lexer.next() != Lexer.IDENTIFIER)
        throw expect(L.l("identifier"));

      type = type.fieldReference(lexer.getId());
    }

    return type;
  }

  /**
   * if ::= IF '(' expr ')' stmt 
   */
  private void parseIf() throws ESException
  {
    boolean isFirst = true;
    boolean isDead = true;

    block = block.create();

    while (lexer.peek() == Lexer.IF) {
      lexer.next();

      if (lexer.next() != '(')
        throw expect("`('");

      block.startIf(parseBooleanExpression(PREC_MAX), ! isFirst);
      isFirst = false;

      if (lexer.next() != ')')
        throw expect("`)'");

      parseStatement();

      block.endBlock();
      if (! block.isDead)
        isDead = false;
      block.isDead = false;

      if (lexer.peek() != Lexer.ELSE) {
        block = block.pop();
        return;
      }

      lexer.next();
    }

    block.startElse();
    parseStatement();
    block.endBlock();
    if (! block.isDead)
      isDead = false;
    block = block.pop();
    block.isDead = isDead;
  }

  /**
   * switch ::= SWITCH '(' expr ')' '{' ((CASE expr:|DEFAULT)+ stmt*)* '}'
   */
  private void parseSwitch() throws ESException
  {
    lexer.next();
    if (lexer.next() != '(')
      throw expect("`)'");

    Expr test = parseExpression(PREC_MAX);

    if (lexer.next() != ')')
      throw expect("`)'");

    if (lexer.next() != '{')
      throw expect("`{'");

    ArrayList exprs = new ArrayList();

    block = block.startSwitch(test);
      
    int ch;
    while ((ch = lexer.peek()) != -1 && ch != '}') {
      switch (ch) {
      case Lexer.CASE:
        lexer.next();
        block.doCase(exprs.size());
        exprs.add(parseExpression(PREC_MAX));

        if (lexer.next() != ':')
          throw expect("`:'");
        break;

      case Lexer.DEFAULT:
        lexer.next();
        if (lexer.next() != ':')
          throw expect("`:'");

        block.doDefault();
        break;

      default:
        parseStatement();
      } 
    }

    if (lexer.next() != '}')
      throw expect("`}'");

    block = block.fillSwitch(exprs);
  }

  /**
   * while ::= WHILE '(' expr ')' stmt 
   */
  private void parseWhile(ESId id) throws ESException
  {
    lexer.next();
    if (lexer.next() != '(')
      throw expect("`('");

    Expr expr = parseBooleanExpression(PREC_MAX);
    if (expr instanceof LiteralExpr &&
        ! ((LiteralExpr) expr).getLiteral().toBoolean())
      throw error(L.l("while (false) is never executed."));
    block = block.startWhile(id, expr);

    if (lexer.next() != ')')
      throw expect("`)'");

    parseStatement();
    block = block.endLoop();
  }

  /**
   * for ::= FOR '(' expr ')' stmt 
   */
  private void parseFor(ESId id) throws ESException
  {
    lexer.next();
    if (lexer.next() != '(')
      throw expect("`('");
    
    boolean hasValue = false;
    Expr lhs = null;
    if (lexer.peek() == Lexer.VAR) {
      lhs = parseVar(true);
    }
    else if (lexer.peek() != ';') {
      lhs = parseExpression(PREC_MAX);
    } else if (lexer.peek() == Lexer.IN)
      throw expect(L.l("expression"));

    if (lexer.peek() == Lexer.IN) {
      parseForIn(id, lhs);
      return;
    }
    
    if (lhs != null)
      lhs.exprStatement(block.function);

    if (lexer.next() != ';')
      throw expect("`;'");

    Expr test = null;
    if (lexer.peek() != ';')
      test = parseExpression(PREC_MAX);

    if (lexer.next() != ';')
      throw expect("`;'");

    Expr incr = null;
    if (lexer.peek() != ')') {
      incr = parseExpression(PREC_MAX);
      incr.killValue();
    }
    
    if (lexer.next() != ')')
      throw expect("`)'");

    if (test instanceof LiteralExpr &&
        ! ((LiteralExpr) test).getLiteral().toBoolean())
      throw error(L.l("for (;false;) is never executed."));
    block = block.startFor(id, test, incr);
    parseStatement();
    block = block.endLoop();
  }

  private void parseForIn(ESId id, Expr lhs) 
    throws ESException
  {
    lexer.next();

    String var = block.newIterator(id, parseExpression(PREC_MAX));

    if (lexer.next() != ')')
      throw expect("`)'");

    block = block.startWhile(id, block.hasNext(var));
    
    block.addExpr(lhs.next(var, lhs));
    
    parseStatement();
    block = block.endLoop();
  }

  /**
   * do ::= DO stmt WHILE '(' expr ')'
   */
  private void parseDo(ESId id) throws ESException
  {
    lexer.next();

    block = block.startDo(id);
    parseStatement();

    if (lexer.next() != Lexer.WHILE)
      throw expect("`while'");

    if (lexer.next() != '(')
      throw expect("`('");

    block = block.endDo(parseBooleanExpression(PREC_MAX));
    
    if (lexer.next() != ')')
      throw expect("`)'");
    
    parseStatementEnd();
  }

  /**
   * with ::= WITH '(' expr ')' stmt 
   */
  private void parseWith() throws ESException
  {
    lexer.next();
    if (lexer.next() != '(')
      throw expect("`('");

    block = block.startWith(parseExpression(PREC_MAX));
    
    if (lexer.next() != ')')
      throw expect("`)'");
    
    parseStatement();

    block = block.endWith();
  }

  /**
   * var ::= VAR id (= expr)? (, id (= expr)?)*
   */
  private Expr parseVar(boolean keepValue) throws ESException
  {
    boolean isFirst = true;
    Expr retVar = null;
    do {
      lexer.next();
    
      if (lexer.next() != Lexer.IDENTIFIER)
        throw expect(L.l("identifier"));

      ESId name = lexer.getId();

      Expr type = null;

      if (lexer.peek() == ':') {
        lexer.next();
        
        type = parseType();
      }
      
      block.defVar(name, type);
      
      if (lexer.peek() == '=') {
        lexer.next();

        Expr var = block.newVar(name, type);
        Expr value = parseExpression(Parser.PREC_ASSIGN + 1, true);
        block.evalExpr();
        var.assign(value).exprStatement(block.function);
      } else if (keepValue)
        retVar = block.newVar(name, type);

      isFirst = false;
    } while (lexer.peek() == ',');

    return retVar;
  }

  /**
   * synchronized ::= SYNCHRONIZED '(' expr ')' stmt 
   */
  private void parseSynchronized() throws ESException
  {
    lexer.next();
    if (lexer.next() != '(')
      throw expect("`('");
    
    block = block.startSynchronized(parseExpression(PREC_MAX));

    if (lexer.next() != ')')
      throw expect("`)'");
    
    parseStatement();

    block = block.endSynchronized();
  }
  
  /**
   * try ::= TRY stmt (CATCH | FINALLY)
   */
  private void parseTry() throws ESException
  {
    lexer.next();

    block = block.startTry();
    parseStatement();
    block = block.endTry();
 
    if (lexer.peek() == Lexer.CATCH) {
      parseCatch();
    }
    else if (lexer.peek() == Lexer.FINALLY)
      parseFinally();
    else
      throw error(L.l("expected `catch' or `finally' at {0}", getToken()));
  }

  /**
   * catch ::= CATCH '(' (expr lhs?)? ')' stmt 
   */
  private void parseCatch() throws ESException
  {
    block.function.disallowJavaLocal();
    // XXX: don't forget catch w/o try

    boolean oldDead = block.isDead;
    boolean hasCatchall = false;

    while (lexer.peek() == Lexer.CATCH) {
      block.isDead = false;
    
      if (hasCatchall)
        throw error(L.l("catch () must be last catch clause"));

      lexer.next();
      if (lexer.next() != '(')
        throw expect("`('");

      String exceptionClass = "";
      while (lexer.peek() == Lexer.IDENTIFIER) {
        lexer.next();
        exceptionClass = exceptionClass + lexer.getText();

        if (lexer.peek() != '.')
          break;

        lexer.next();
        exceptionClass = exceptionClass + ".";
        if (lexer.peek() != Lexer.IDENTIFIER)
          throw expect(L.l("identifier"));
      }

      ESId name = null;
      if (lexer.peek() == Lexer.IDENTIFIER) {
        name = lexer.getId();
        lexer.next();
      }

      if (lexer.next() != ')') {
        if (exceptionClass.equals(""))
          throw expect(L.l("identifier"));
        else
          throw expect("`)'");
      }

      if (name == null) {
        name = ESId.intern(exceptionClass);
        exceptionClass = "java.lang.Exception";
      }

      Expr var = null;
      if (name != null)
        var = block.newVar(name);
      
      block = block.startCatch(exceptionClass, var);
      parseStatement();
      if (! block.isDead)
        oldDead = false;
      block = block.endCatch();
    }

    block.isDead = oldDead;
    // Don't forget to throw
    if (lexer.peek() == Lexer.FINALLY)
      parseFinally();
  }

  /**
   * finally ::= FINALLY stmt
   */
  private void parseFinally() throws ESException
  {
    boolean oldDead = block.isDead;
    block.isDead = false;
    lexer.next();

    block = block.startFinally();
    
    parseStatement();

    block = block.endFinally();
    block.isDead = oldDead;
  }

  static ESId BOGUS = ESId.intern("return ");

  /**
   * Parse a class
   */
  private void parseClass() throws ESException
  {
    if (function.getParent() != null)
      throw error(L.l("`class' only allowed at top level"));
    
    lexer.next();

    if (lexer.next() != Lexer.IDENTIFIER)
      throw expect("class name");

    ESId id = lexer.getId();

    ESId proto = parseExtends();

    if (lexer.next() != '{')
      throw expect("`{'");

    ParseClass oldClass = parseClass;
    Function oldGlobal = globalFunction;
    Function oldStatic = staticFunction;
    Function oldFunction = function;
    Block oldBlock = block;

    parseClass = oldClass.newClass(id);
    parseClass.setProto(proto);
    
    globalFunction = parseClass.newFunction(null, ESId.intern("global"), true);
    staticFunction = parseClass.newFunction(null, ESId.intern("__es_static"), true);
    parseClass.setGlobal(globalFunction);

    function = globalFunction;
    block = Block.create(this, function);
      
    parseBlock(true);
    block.finish();
    
    block = Block.create(this, staticFunction);
    block.finish();

    if (parseClass.getFunction(id) == null) {
      function = parseClass.newFunction(null, id, false);
      block = Block.create(this, function);
      block.finish();
    }

    block = oldBlock;
    function = oldFunction;
    globalFunction = oldGlobal;
    staticFunction = oldStatic;
    parseClass = oldClass;
    
    if (lexer.next() != '}')
      throw expect("`}'");
  }

  private ESId parseExtends()
    throws ESException
  {
    if (lexer.peek() != Lexer.EXTENDS)
      return null;

    lexer.next();
    if (lexer.next() != Lexer.IDENTIFIER)
      throw expect(L.l("parent class name"));

    return lexer.getId();
  }

  private void parseImport()
    throws ESException
  {
    CharBuffer path = new CharBuffer();

    lexer.next();

    while (true) {
      if (lexer.peek() == Lexer.BIN_OP && lexer.getOp() == '*') {
        lexer.next();
        path.append('*');
        importList.add(path.close());
        return;
      }

      if (lexer.peek() != Lexer.IDENTIFIER)
        throw expect(L.l("identifier"));

      path.append(lexer.getText());

      lexer.next();

      if (lexer.peek() != '.')
        break;

      lexer.next();
      path.append('.');
    }

    String className = path.close();
    String pathName = className.replace('.', '/') + ".js";

    Path importPath = getScriptPath().lookup(pathName);

    if (importPath.exists()) {
      function.cl.addImport(pathName);
      return;
    }

    try {
      CauchoSystem.loadClass(className, false, getClassLoader());

      importList.add(className);
    } catch (ClassNotFoundException e) {
      throw error(L.l("can't open import `{0}'", pathName));
    }
  }
  
/*
  private void parseStatic() throws ESException
  {
    if (function.rest != null || staticCode == null)
      throw error("illegal static statement");

    lexer.next();

    int oldVar = stmtVar;
    ParseFun oldFun = function;
    try {
      stmtVar = -1;
      function = staticFunction;
      parseStatement(staticCode);
    } finally {
      function = oldFun;
      stmtVar = oldVar;
    }
    } */

  private Expr parseExpression(int prevPrec, boolean isTop)
    throws ESException
  {
    Expr result = parseExprRec(parseTerm(isTop), prevPrec, false, isTop);
    result.getType();
    return result;
  }

  private Expr parseBooleanExpression(int prevPrec)
       throws ESException
  {
    Expr result = parseExprRec(parseTerm(false), prevPrec, true, false);
    result.getType();
    return result;
  }

  private Expr parseExpression(int prevPrec)
       throws ESException
  {
    Expr result = parseExprRec(parseTerm(false), prevPrec, false, false);
    result.getType();
    return result;
  }

  private Expr parseExpression()
       throws ESException
  {
    Expr result = parseExprRec(parseTerm(false), PREC_MAX, false, false);
    result.getType();
    return result;
  }

  /*
   * parseExpression ()
   *
   * Grammar:
   *   expr ::= obj (op obj)*
   */
  private Expr parseExprRec(Expr lexpr, int prevPrec, 
                            boolean isBool, boolean isTop)
       throws ESException
  {
    Expr rexpr = null;
    int op = 0;
    int lex = 0;
    int prec = 0;

    while (true) {
      boolean doLookahead = false;
      boolean doPostfix = false;
      boolean isRightAssoc = false;
      int nextPrec = 0;
      int nextOp = 0;
      int nextLex = 0;

      switch (lexer.peek()) {
      case '=':
        if (op != 0 && lex != ',')
          throw error(L.l("illegal left hand side of assignment"));
        if (isBool)
          throw error(L.l("assignment used as boolean needs parentheses"));

      case Lexer.BIN_OP:
        // careful with and/or for unassigned local variables
        if (lexer.getOp() == Lexer.AND || lexer.getOp() == Lexer.OR)
          function.setVars();
        // fall through
      case ',':
      case Lexer.BANDU_OP:
      case '?': 
        nextLex = lexer.peek();
        nextOp = lexer.getOp();
        nextPrec = lexer.getPrecedence();
        isRightAssoc = lexer.isRightAssoc();
        doLookahead = true;
        break;

      default:
        return op != 0 ? lexpr.binaryOp(lex, op, rexpr) : lexpr;
      }

      if (nextPrec >= prevPrec) {
        return op != 0 ? lexpr.binaryOp(lex, op, rexpr) : lexpr;
      }
      else if (prec == 0) {
      }
      else if (nextPrec < prec) {
        rexpr = parseExprRec(rexpr, prec, isBool, isTop);
        continue;
      }
      else {
        lexpr = op != 0 ? lexpr.binaryOp(lex, op, rexpr) : lexpr;
      }

      prec = nextPrec;
      lex = nextLex;
      op = nextOp;
      
      if (doLookahead)
        lexer.next();

      if (isRightAssoc) {
        /* XXX: is this the right thing to do? + 1 */
        rexpr = parseExpression(prec + 1, isTop);
      }
      else if (op == '?') {
        function.setVars();
        Expr mexpr = parseExpression(Parser.PREC_ASSIGN + 1);
        if (lexer.peek() != ':')
          throw expect("`:'");
        lexer.next();
        rexpr = parseExpression(Parser.PREC_ASSIGN + 1, isTop);

        lexpr = lexpr.conditional(mexpr, rexpr);
        op = 0;
      }
      else
        rexpr = parseTerm(isTop);
    }
  }

  /*
   * parseTerm
   *
   * term ::= BANDU_OP term
   *      ::= UNARY_OP term
   *      ::= IDENTIFIER termTail
   *      ::= LITERAL termTail
   *      ::= objLiteral termTail
   *      ::= '(' expr ')' termTail
   */
  private Expr parseTerm(boolean isTop) throws ESException
  {
    int op;

    switch (lexer.peek()) {
    case Lexer.BANDU_OP:
    case Lexer.UNARY_OP:
      lexer.next();
      op = lexer.getOp();
      return parseTerm(isTop).unaryOp(op);

    case Lexer.VOID:
      lexer.next();
      return parseTerm(isTop).doVoid();

    case Lexer.TYPEOF:
      lexer.next();
      return parseTerm(isTop).typeof();

    case Lexer.DELETE:
      lexer.next();
      return parseTerm(isTop).delete();

    case Lexer.POSTFIX:
      lexer.next();
      op = lexer.getOp();
      return parseTerm(isTop).prefix(op);

    case Lexer.LITERAL:
    case Lexer.REGEXP:
    case Lexer.IDENTIFIER:
    case Lexer.THIS:
    case Lexer.EVAL:
    case Lexer.NEW:
    case '(':
    case '{':
    case '[':
    case Lexer.HASH_REF:
    case Lexer.HASH_DEF:
    case Lexer.FUNCTION:
      return parseLhs(false, isTop);

    default:
      throw expect(L.l("expression"));
    }
  }

  /**
   * Parses the left-hand side of an expression
   *
   * @param hasNew true if this follows a new
   * @return the new expression
   */
  private Expr parseLhs(boolean hasNew, boolean isTop) 
    throws ESException
  {
    String name;
    int op;
    Expr expr = null;

    switch (lexer.next()) {
    case Lexer.NEW:
      return parseTermTail(parseLhs(true, isTop), hasNew, isTop);

    case Lexer.LITERAL:
      return parseTermTail(block.newLiteral(lexer.getLiteral()),
                           hasNew, isTop);

    case Lexer.REGEXP:
      /*
      return parseTermTail(block.newRegexp(lexer.getLiteral(),
                                          lexer.getFlags()),
                           hasNew, isTop);
      */
      throw new UnsupportedOperationException();
    
    case Lexer.IDENTIFIER:
      return parseTermTail(getVar(lexer.getId()),
                           hasNew, isTop);

    case Lexer.THIS:
      return parseTermTail(block.newThis(), hasNew, isTop);

    case Lexer.EVAL:
      if (lexer.peek() != '(')
        throw expect("`('");
      function.setArguments();
      return parseTermTail(block.newVar(ESId.intern("eval")), hasNew, false);

    case '(':
      expr = parseExpression(PREC_MAX);
      if (lexer.next() != ')')
        throw expect("`)'");
      return parseTermTail(expr, hasNew, isTop);

    case '{':
      expr = parseObjectLiteral(-1);
      if (lexer.next() != '}')
        throw expect("`}'");
      return parseTermTail(expr, hasNew, isTop);

    case '[':
      expr = parseArrayLiteral(-1);
      if (lexer.next() != ']')
        throw expect("`]'");
      return parseTermTail(expr, hasNew, isTop);

    case Lexer.FUNCTION:
      Function newFun = parseFunction();

      //function.addFunction(newFun);
      function.addVariable(block, newFun.id, null);
      block.newVar(newFun.id).getVar().setType(Expr.TYPE_ES);
      expr = block.newVar(newFun.id);
      return parseTermTail(expr, hasNew, isTop);

    case Lexer.HASH_DEF:
      switch (lexer.peek()) {
      case '{':
        lexer.next();
        expr = parseObjectLiteral(lexer.intValue);
        if (lexer.next() != '}')
          throw expect("`}'");
        return parseTermTail(expr, hasNew, isTop);

      case '[':
        lexer.next();
        expr = parseArrayLiteral(lexer.intValue);
        if (lexer.next() != ']')
          throw expect("`]'");
        return parseTermTail(expr, hasNew, isTop);

      default:
        /* XXX:
        expr = parseLhs(hasNew, isTop);
        int var = code.newVar();
        code.store(var);
        hashes.add(lexer.intValue, var);
        code.load(var);
        */
        return expr;
      }
      /*
    case Lexer.HASH_REF:
      if (hashes.size() <= lexer.intValue ||
          hashes.get(lexer.intValue) <= 0)
        throw error("bad sharp reference at " + getToken());

      return parseTermTail(code.load(hashes.get(lexer.intValue)), 
                           hasNew, isTop);
      */
    default:
      throw expect(L.l("term"));
    }
  }

  /*
   * parseTermTail
   *
   * termTail ::=
   *          ::= '.' Id termTail
   *          ::= '(' exprList ')' termTail
   *          ::= '[' expr ']' termTail
   *          ::= '++' termTail
   */
  private Expr parseTermTail(Expr term, boolean hasNew, boolean isTop) 
    throws ESException
  {
    int op;

    while (true) {
      switch (lexer.peek()) {
      case '.':
        lexer.next();
        if (lexer.next() != Lexer.IDENTIFIER)
          throw expect(L.l("property name"));

        term = term.fieldReference(lexer.getId());
        break;

      case '(':
        if (isTop && lexer.seenLineFeed())
          return term;

        lexer.next();

        int n = 0;
        CallExpr call;
        if (hasNew)
          call = term.startNew();
        else
          call = term.startCall();
        
        while (lexer.peek() != ')') {
          if (n != 0 && lexer.peek() != ',')
            throw expect("`,'");
          else if (n != 0)
            lexer.next();

          call.addCallParam(parseExpression(PREC_COMMA));
          n++;
        }
        lexer.next();

        if (hasNew)
          return call;
        else
          term = call;
        break;

      case '[':
        if (isTop && lexer.seenLineFeed())
          return term;

        lexer.next();
        term = term.fieldReference(parseExpression(PREC_MAX));

        if (lexer.next() != ']')
          throw expect("`]'");
        break;

      case Lexer.POSTFIX:
        if (hasNew)
          return term.startNew();

        if (lexer.seenLineFeed())
          return term;

        term = term.postfix(lexer.getOp());

        lexer.next();
        break;

      case '@':
        lexer.next();

        term = term.cast(parseType());
        break;

      default:
        if (hasNew)
          return term.startNew();
        else
          return term;
      }
    }
  }

  private Expr parseObjectLiteral(int hash) throws ESException
  {
    Expr expr = block.newVar(ESId.intern("Object"));
    CallExpr call = expr.startCall();

    /*
    if (hash >= 0) {
      if (hashes.size() <= hash)
        hashes.setLength(hash + 1);
      hashes.add(hash, var);
    }
    */

    if (lexer.peek() == ',') {
      lexer.next();

      return call;
    }

    while (lexer.peek() == Lexer.LITERAL ||
           lexer.peek() == Lexer.IDENTIFIER) {
      ESId id;

      if (lexer.next() == Lexer.LITERAL)
        id = ESId.intern(lexer.literal.toString());
      else
        id = lexer.getId();

      if (lexer.next() != ':')
        throw expect("`:'");

      call.addCallParam(block.newLiteral(id));
      call.addCallParam(parseExpression(PREC_COMMA));

      if (lexer.peek() != ',')
        break;
      lexer.next();
    }

    return call;
  }

  private Expr parseArrayLiteral(int hash)
    throws ESException
  {
    Expr expr = block.newVar(ESId.intern("Array"));
    
    CallExpr call = expr.startCall();

    boolean isFirst = true;
    while (lexer.peek() != ']') {
      if (lexer.peek() == ',') {
        lexer.next();
        call.addCallParam(block.newLiteral(ESBase.esUndefined));
        isFirst = false;
        continue;
      }

      Expr value = parseExpression(PREC_COMMA);
      
      if (isFirst && lexer.peek() == ']')
        return block.newArray(value);

      if (lexer.peek() != ',') {
        call.addCallParam(value);
        break;
      }
      
      lexer.next();
      
      if (isFirst && lexer.peek() == ']')
        return block.newArray(value);
      
      isFirst = false;
      call.addCallParam(value);
    }

    return call;
  }

  /**
   * Gets a variable instance.
   */
  private Expr getVar(ESId name)
    throws ESException
  {
    if (name == PACKAGES)
      return new PackageExpr(block);
    else if (name == JAVA)
      return new PackageExpr(block).fieldReference(JAVA);
    else if (name == CAUCHO)
      return new PackageExpr(block).fieldReference(COM).fieldReference(CAUCHO);
    else if (block.hasVar(name))
      return block.newVar(name);
    else {
      for (int i = 0; i < importList.size(); i++) {
        String className = (String) importList.get(i);

        if (className.endsWith(".*"))
          className = className.substring(0, className.length() - 1) + name;

        try {
          Class cl = CauchoSystem.loadClass(className, false,
                                            getClassLoader());

          return new JavaClassExpr(block, cl);
        } catch (Throwable e) {
        }
      }
      
      return block.newVar(name);
    }
  }

  /**
   * Returns the current filename being parsed.
   */
  public String getFilename()
  {
    return lexer.getFilename();
  }

  /**
   * Creates an error message with the given text.
   */
  ESException error(String text)
  {
    return lexer.error(text);
  }

  /**
   * Returns the current token for an error message.
   */
  private String getToken()
  {
    if (lexer.isEof())
      return "end of file";
    else
      return "`" + lexer.getToken() + "'";
  }

  /**
   * Returns a parse exception when expecting a result.
   */
  private ESException expect(String expect)
  {
    try {
      return lexer.error(L.l("expected {0} at {1}", expect, getToken()));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
