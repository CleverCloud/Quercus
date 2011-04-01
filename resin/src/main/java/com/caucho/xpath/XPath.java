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

package com.caucho.xpath;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.LruCache;
import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xpath.pattern.FromContext;

import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Public facade for selecting nodes and creating match patterns.
 *
 * <p>Applications can select nodes directly from the XPath facade.  
 * For example,
 * <code><pre>
 * Node verse = XPath.find("chapter/verse", node);
 * </pre></code>
 *
 * <p>For greater efficiency, applications can also precompile the 
 * match patterns.
 * <code><pre>
 * Pattern pattern = XPath.parseSelect("chapter/verse");
 * Node verse = pattern.find(node);
 * </pre></code>
 *
 * <p>XPath can also return values based on XPath expressions, following
 * the XPath expression syntax.  Applications can use the expressions for
 * the equivalent of xsl:value-of
 * <code><pre>
 * Expr expr = XPath.parseExpr("chapter/verse/@id + 1");
 * double value = expr.evalNumber(node);
 * </pre></code>
 *
 * <p>To support the XPath pattern variables, XPath uses an environment
 * object.  Most applications will not need to use it. 
 */
public class XPath {
  private static final Logger log
    = Logger.getLogger(XPath.class.getName());

  private static EnvironmentLocal<LruCache<String,Pattern>> _matchCache
    = new EnvironmentLocal<LruCache<String,Pattern>>();
  
  private static EnvironmentLocal<LruCache<String,Pattern>> _selectCache
    = new EnvironmentLocal<LruCache<String,Pattern>>();
  
  private static EnvironmentLocal<LruCache<String,Expr>> _exprCache
    = new EnvironmentLocal<LruCache<String,Expr>>();

  private XPath()
  {
  }

  /**
   * Finds a node based on an XPath pattern.  The pattern is relative
   * to the node so <code>XPath.find("child", node)</code> will find children,
   * not grandchildren.
   *
   * @param query XPath select pattern.
   * @param node XML node to start searching from.
   * @return The first matching node in document order.
   */
  public static Node find(String query, Node node)
    throws XPathException
  {
    Pattern pattern = parseSelect(query);

    return (Node) pattern.find(node);
  }

  /**
   * Selects all node matching an XPath pattern
   *
   * @param query XPath select pattern.
   * @param node XML node to start searching from.
   * @return An iterator of nodes matching the pattern.
   */
  public static Iterator select(String query, Node node)
    throws XPathException
  {
    Pattern pattern = parseSelect(query);

    return pattern.select(node);
  }

  /**
   * Create a node selection pattern.  The pattern matches relative
   * to the current node.
   *
   * @param query XPath select pattern.
   * @return a pattern that can later select nodes.
   */
  public static Pattern parseSelect(String query)
    throws XPathParseException
  {
    LruCache<String,Pattern> cache = _selectCache.get();
    if (cache == null)
      cache = new LruCache<String,Pattern>(128);
    
    Pattern pattern = cache.get(query);

    if (pattern == null) {
      pattern = parseSelect(query, null);
      cache.put(query, pattern);
    }

    return pattern;
  }

  /**
   * Create a node selection pattern.  The pattern matches relative
   * to the current node.
   *
   * <p>XSLT uses this version of parseSelect for proper namespace
   * matching.
   *
   * @param query XPath select pattern.
   * @param namespace the appropriate namespace mappings
   *
   * @return a pattern that can later select nodes.
   */
  public static Pattern parseSelect(String query, NamespaceContext namespace)
    throws XPathParseException
  {
    XPathParser parser = new XPathParser(query, namespace);

    AbstractPattern pattern = parser.parseSelect();

    if (log.isLoggable(Level.FINER))
      log.finest("select: " + pattern);

    return new Pattern(pattern);
  }

  /**
   * Create a node match pattern.  Match patterns are intended to test
   * if a node matches the pattern.  They do not work well for finding or
   * selecting patterns.  Essentially, a match pattern of 'foo[@bar]' is
   * equivalent to a select pattern of '//foo[@bar]', but with less overhead.
   *
   * @param query XPath match pattern.
   * @return a pattern that can later be used for isMatch.
   */
  public static Pattern parseMatch(String query)
    throws XPathParseException
  {
    LruCache<String,Pattern> cache = _matchCache.get();
    if (cache == null)
      cache = new LruCache<String,Pattern>(128);
    
    Pattern pattern = cache.get(query);

    if (pattern == null) {
      pattern = parseMatch(query, null);
      cache.put(query, pattern);
    }

    return pattern;
  }

  /**
   * Create a node match pattern.  Match patterns are intended to test
   * if a node matches the pattern.  They do not work well for finding or
   * selecting patterns.  Essentially, a match pattern of 'foo[@bar]' is
   * equivalent to a select pattern of '//foo[@bar]', but with less overhead.
   *
   * @param query XPath match pattern.
   * @param namespace the appropriate namespace mappings.
   *
   * @return a pattern that can later be used for isMatch.
   */
  public static Pattern parseMatch(String query, NamespaceContext namespace)
    throws XPathParseException
  {
    XPathParser parser = new XPathParser(query, namespace);

    AbstractPattern pattern = parser.parseMatch();

    if (log.isLoggable(Level.FINER))
      log.finest("match: " + pattern);

    return new Pattern(pattern);
  }

  /**
   * Evaluates an XPath expression, returning a string.  evalString works
   * like the XSL <code>value-of</code> element.
   *
   * <p>For example, to get the value of an attribute use:
   *
   * <code><pre>
   * String value = XPath.evalString("@id", node);
   * </pre></code>
   *
   * @param query XPath expression
   * @param node the node context
   *
   * @return the string result of the expression.
   */
  public static String evalString(String query, Node node)
    throws XPathException
  {
    Expr expr = parseExpr(query);

    return expr.evalString(node);
  }

  /**
   * Evaluates an XPath expression, returning a double.
   *
   * @param query XPath expression
   * @param node the node context
   *
   * @return the number result of the expression.
   */
  public static double evalNumber(String query, Node node)
    throws XPathException
  {
    Expr expr = parseExpr(query);

    return expr.evalNumber(node);
  }

  /**
   * Evaluates an XPath expression, returning a boolean.
   *
   * @param query XPath expression
   * @param node the node context
   *
   * @return the boolean result of the expression.
   */
  public static boolean evalBoolean(String query, Node node)
    throws XPathException
  {
    Expr expr = parseExpr(query);

    return expr.evalBoolean(node);
  }

  /**
   * Evaluates an XPath expression, returning an object
   *
   * @param query XPath expression
   * @param node the node context
   *
   * @return the result of the expression.
   */
  public static Object evalObject(String query, Node node)
    throws XPathException
  {
    Expr expr = parseExpr(query);

    return expr.evalObject(node);
  }

  /**
   * Parses an XPath expression for later evaluation.
   *
   * @param query XPath expression
   * @return the result of the expression.
   */
  public static Expr parseExpr(String query)
    throws XPathParseException
  {
    LruCache<String,Expr> cache = _exprCache.get();
    if (cache == null) {
      cache = new LruCache<String,Expr>(128);
      _exprCache.set(cache);
    }
    
    Expr expr = cache.get(query);

    if (expr == null) {
      expr = parseExpr(query, null);
      cache.put(query, expr);
    }

    return expr;
  }

  /**
   * Parses an XPath expression for later evaluation.
   *
   * @param query XPath expression
   * @param namespace namespace context
   *
   * @return the compiled expression
   */
  public static Expr parseExpr(String query, NamespaceContext namespace)
    throws XPathParseException
  {
    XPathParser parser = new XPathParser(query, namespace);

    Expr expr = parser.parseExpr();

    if (log.isLoggable(Level.FINER))
      log.finest("expr: " + expr);

    return expr;
  }

  /**
   * Parses an XPath expression for later evaluation.
   *
   * @param query XPath expression
   * @param namespace namespace context
   * @param nodeList containing nodeList pattern
   *
   * @return the compiled expression
   */
  public static Expr parseExpr(String query, NamespaceContext namespace,
                               AbstractPattern nodeList)
    throws XPathParseException
  {
    XPathParser parser = new XPathParser(query, namespace);

    Expr expr = parser.parseExpr(new FromContext(), nodeList);

    if (expr != null)
      expr.setListContext(nodeList);

    if (log.isLoggable(Level.FINER))
      log.finest("expr: " + expr);

    return expr;
  }

  /**
   * Creates a new variable environment.
   */
  public static Env createEnv()
  {
    return Env.create();
  }

  /**
   * Creates a new variable environment based on an old environment.
   *
   * <p>This lets environments share globals even through function calls.
   */
  public static Env createEnv(Env global)
  {
    Env env = Env.create();

    env.init(global);

    return env;
  }

  /**
   * Creates a new variable environment based on an old environment.
   *
   * <p>This lets environments share globals even through function calls.
   */
  public static Env createCall(Env parent)
  {
    Env env = Env.create();

    env.initMacro(parent);

    return env;
  }

  /**
   * Free an environment.
   */
  public static void freeEnv(Env env)
  {
    // env.free();
  }
}
