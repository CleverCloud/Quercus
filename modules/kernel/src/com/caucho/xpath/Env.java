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

import com.caucho.util.FreeList;
import com.caucho.xml.XmlUtil;
import com.caucho.xpath.expr.ObjectVar;
import com.caucho.xpath.expr.Var;
import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xpath.pattern.NodeIterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Global and local variable environment.  The April XSLT draft introduces
 * global and local variables.  The Env class contains those bindings.
 *
 * <p>Because this class exists only to support XSL, it makes a number
 * of assumptions that would be invalid for a typical API.  Specifically,
 * the variable names <font color='red'>must</font> be interned strings, i.e.
 * variable matching uses '==', not equals.
 *
 * <p>Local variables are handled like a stack.  They are pushed and
 * popped as necessary.  The top variables shadow bottom variables.
 *
 * <p>In other words, although the API somewhat resembles a HashMap, 
 * it can't be used as a generic hash map.
 */
public class Env implements ExprEnvironment {
  static FreeList<Env> _freeList = new FreeList<Env>(32);
  
  HashMap _ids;
  HashMap _idCache;
  Element _lastElement;
  HashMap<String,Var> _globals; // = new HashMap();
  HashMap _functions;
  HashMap _cache;

  String []_varKeys;
  Var []_varValues;
  int _varSize;

  private Node _currentNode;
  private Node _contextNode;
  
  private int _positionIndex;
  private boolean _hasMorePositions;
  private int _useCount;
  
  private Env _parent;
  private Env _root;

  private ExprEnvironment _exprEnv;
  private AbstractPattern _select;

  private int _position;
  private int _size;

  private StylesheetEnv _stylesheetEnv;
  private VarEnv _varEnv;

  static Env create()
  {
    Env env = null; // (Env) freeList.allocate();
    if (env == null)
      env = new Env();

    env._root = env;
    
    return env;
  }

  public void setStylesheetEnv(StylesheetEnv stylesheetEnv)
  {
    _stylesheetEnv = stylesheetEnv;
  }

  public StylesheetEnv getStylesheetEnv()
  {
    for (Env env = this; env != null; env = env._parent)
      if (env._stylesheetEnv != null)
        return env._stylesheetEnv;

    return null;
  }

  /**
   * Sets the variable environment.
   */
  public void setVarEnv(VarEnv varEnv)
  {
    _varEnv = varEnv;
  }

  /**
   * Returns the variable environment.
   */
  public VarEnv getVarEnv()
  {
    return _varEnv;
  }

  /**
   * Initialize the XPath environment with values from the parent.
   */
  void init(Env parent)
  {
    _parent = parent;
    _root = parent._root;
  }

  /**
   * Initialize the XPath environment with a context and a select node.
   */
  void init(Env parent, AbstractPattern select, Node currentNode)
  {
    _parent = parent;
    _root = parent._root;
    _select = select;
    _currentNode = currentNode;
  }

  /**
   * Initialize the XPath environment with values from the parent.
   */
  void initMacro(Env parent)
  {
    _parent = parent;
    _root = parent._root;
    
    _select = parent._select;
    _stylesheetEnv = parent._stylesheetEnv;
    _exprEnv = parent._exprEnv;
    _currentNode = parent._currentNode;
    _contextNode = parent._contextNode;
    
    _position = parent._position;
    _size = parent._size;
  
    _positionIndex = 0;
    _hasMorePositions = false;
    _useCount = 0;
  }

  /**
   * Clears all values in the local environment.
   */
  public void clear()
  {
    if (true)
      return;
    
    _useCount++;
    if (_ids != null) {
      _ids.clear();
      _idCache.clear();
    }

    while (_varSize-- > 0) {
      _varKeys[_varSize] = null;
      _varValues[_varSize] = null;
    }
    _varSize = 0;

    _lastElement = null;
    _globals.clear();
    _functions = null;
    _cache = null;
    _currentNode = null;
    _contextNode = null;
    _parent = null;
    _select = null;

    _size = 0;
    _position = 0;
    _positionIndex = 0;
    _hasMorePositions = false;
  }

  /**
   * Returns the parent envivonment.
   */
  Env getParent()
  {
    return _parent;
  }

  /**
   * Returns the current number of local variables.
   */
  public int getVarSize()
  {
    return _varSize;
  }

  /**
   * Sets the current number of local variables (popping, them).
   */
  public void setVarSize(int size)
  {
    if (_varKeys == null)
      return;

    for (; _varSize > size; _varSize--) {
      _varSize--;
      _varKeys[_varSize] = null;
      _varValues[_varSize] = null;
    }
  }

  /**
   * Returns the value associated with name.  
   *
   * <p><em>name must be interned</em>
   */
  public Var getVar(String name)
  {
    for (int i = _varSize - 1; i >= 0; i--) {
      if (_varKeys[i] == name)
        return _varValues[i];
    }

    if (_root._globals != null) {
      Var var = _root._globals.get(name);

      if (var != null)
        return var;
    }

    if (_root._varEnv != null)
      return _root._varEnv.getVar(name);
    else
      return null;
  }

  /**
   * Adds the value associated with name.
   *
   * <p><em>name must be interned</em>
   */
  public int addVar(String name, Object value)
  {
    _useCount++;
    if (value instanceof Iterator)
      value = iteratorToList((Iterator) value);

    if (! (value instanceof Var))
      value = new ObjectVar(value);
    
    return addVar(name, (Var) value);
  }
  
  /**
   * Sets the value associated with name.
   *
   * <p><em>name must be interned</em>
   */
  public void setVar(String name, Object value)
  {
    _useCount++;
    if (value instanceof Iterator)
      value = iteratorToList((Iterator) value);

    if (! (value instanceof Var))
      value = new ObjectVar(value);

    for (int i = _varSize - 1; i >= 0; i--) {
      if (_varKeys[i] == name) {
        _varValues[i] = (Var) value;
        return;
      }
    }

    addVar(name, (Var) value);
  }

  /**
   * Adds the value associated with name.
   *
   * <p><em>name must be interned</em>
   */
  public int addVar(String name, Var value)
  {
    _useCount++;

    if (_varKeys == null) {
      _varKeys = new String[16];
      _varValues = new Var[16];
    }
    else if (_varSize == _varKeys.length) {
      String []newKeys = new String[2 * _varKeys.length];
      Var []newValues = new Var[2 * _varKeys.length];

      System.arraycopy(_varKeys, 0, newKeys, 0, _varSize);
      System.arraycopy(_varValues, 0, newValues, 0, _varSize);

      _varKeys = newKeys;
      _varValues = newValues;
    }

    _varKeys[_varSize] = name;
    _varValues[_varSize] = value;
    _varSize++;

    return _varSize - 1;
  }

  /**
   * Pops the last count vars from the local stack.
   */
  public void popVars(int count)
  {
    _useCount++;
    if (_varKeys == null)
      return;

    for (; count > 0 && _varSize > 0; count--) {
      _varSize--;
      _varKeys[_varSize] = null;
      _varValues[_varSize].free();
      _varValues[_varSize] = null;
    }
  }

  /**
   * Returns the top of the stack.
   */
  public int getTop()
  {
    return _varSize;
  }
  
  /**
   * Pops the last count vars from the local stack.
   */
  public void popToTop(int top)
  {
    _useCount++;
    if (_varKeys == null)
      return;

    while (top < _varSize) {
      _varSize--;
      _varKeys[_varSize] = null;
      _varValues[_varSize].free();
      _varValues[_varSize] = null;
    }
  }

  /**
   * Sets a global variable.
   */
  public void setGlobal(String name, Object value)
  {
    _useCount++;
    
    Var var = null;
    
    if (value instanceof Iterator)
      value = iteratorToList((Iterator) value);

    if (value instanceof Var)
      var = (Var) value;
    else
      var = new ObjectVar(value);

    if (_root._globals == null)
      _root._globals = new HashMap<String,Var>();
    
    _root._globals.put(name, var);
  }

  /**
   * Converts an iterator to an array list
   */
  private ArrayList iteratorToList(Iterator iter)
  {
    ArrayList list = new ArrayList();
    while (iter.hasNext())
      list.add(iter.next());

    return list;
  }

  /**
   * Sets the extension function library
   *
   * @param function new function library
   * @return old function library
   */
  public HashMap setFunctions(HashMap functions)
  {
    HashMap old = _functions;

    _functions = functions;

    return old;
  }
  
  /**
   * Adds and extension function
   *
   * @param function new function library
   * @return old function library
   */
  public void addFunction(String name, Object fun)
  {
    if (_functions == null)
      _functions = new HashMap();

    _functions.put(name, fun);
  }

  /**
   * Returns the named function.
   */
  public XPathFun getFunction(String name)
  {
    if (_root._functions == null)
      return null;
    else
      return (XPathFun) _root._functions.get(name);
  }

  /**
   * Returns true if there are more positions() needed to iterate through.
   */
  public boolean hasMorePositions()
  {
    return _hasMorePositions;
  }
  /**
   * Set true if there are more positions() needed to iterate through.
   *
   * @param more if true, there are more positions to iterate through.
   *
   * @return the old more-position value.
   */
  public boolean setMorePositions(boolean more)
  {
    boolean old = _hasMorePositions;

    _hasMorePositions = more;

    return old;
  }
  /*
   * The position index is used for patterns which have multiple position()s
   * for the same node.  See FilterPattern for a more detailed description.
   *
   * @param index the new position index.
   *
   * @return the old position index.
   */
  public int setPositionIndex(int index)
  {
    int old = _positionIndex;

    _positionIndex = index;

    return old;
  }
  
  /*
   * Returns the position index is used for patterns which have
   * multiple position()s for the same node.  See FilterPattern for a
   * more detailed description.
   */
  public int getPositionIndex()
  {
    return _positionIndex;
  }

  /**
   * Gets the current node.
   */
  public Node getCurrentNode()
  {
    return _currentNode;
  }

  /**
   * Sets the current node.
   */
  public void setCurrentNode(Node node)
  {
    _currentNode = node;
  }

  /**
   * Sets the selection context
   */
  public AbstractPattern setSelect(Node node, AbstractPattern select)
  {
    AbstractPattern oldSelect = _select;
    
    _contextNode = node;
    _select = select;

    _position = 0;

    return oldSelect;
  }

  public AbstractPattern getSelect()
  {
    return _select;
  }

  /**
   * Sets the selection context
   */
  public ExprEnvironment setExprEnv(ExprEnvironment exprEnv)
  {
    ExprEnvironment oldExprEnv = _exprEnv;
    
    _exprEnv = exprEnv;

    return oldExprEnv;
  }

  public ExprEnvironment getExprEnv()
  {
    return _exprEnv;
  }

  /**
   * Gets the context node.
   */
  public Node getContextNode()
  {
    return _contextNode;
  }

  /**
   * Sets the context node.
   */
  public Node setContextNode(Node contextNode)
  {
    Node oldNode = _contextNode;
    _contextNode = contextNode;
    return oldNode;
  }

  /**
   * Returns the position of the context node.
   */
  public int getContextPosition()
  {
    if (_exprEnv != null)
      return _exprEnv.getContextPosition();
    
    if (_position > 0)
      return _position;

    if (_contextNode == null || _currentNode == null)
      return 0;

    if (_select != null) {
      try {
        NodeIterator iter = _select.select(_contextNode, this);
        Node child;
      
        while ((child = iter.nextNode()) != null && child != _currentNode) {
        }

        return iter.getContextPosition();
      } catch (Exception e) {
      }
    }

    Node child = _contextNode.getFirstChild();
    int pos = 1;
    for (;
         child != null && child != _currentNode;
         child = child.getNextSibling()) {
      pos++;
    }
    
    return pos;
  }

  /**
   * Returns the number of nodes in the context list.
   */
  public int getContextSize()
  {
    if (_exprEnv != null)
      return _exprEnv.getContextSize();
    
    if (_size > 0)
      return _size;
    
    if (_contextNode == null || _currentNode == null)
      return 0;

    if (_select != null) {
      try {
        NodeIterator iter = _select.select(_contextNode, this);
        Node child;
      
        while ((child = iter.nextNode()) != null && child != _currentNode) {
        }

        return iter.getContextSize();
      } catch (Exception e) {
      }
    }

    Node child = _contextNode.getFirstChild();
    int pos = 0;
    for (;
         child != null;
         child = child.getNextSibling())
      pos++;
    
    return pos;
  }

  /**
   * Returns a document for creating nodes.
   */
  public Document getOwnerDocument()
  {
    return null;
  }

  /**
   * Returns the given system property.
   */
  public Object systemProperty(String namespaceURI, String localName)
  {
    return null;
  }

  /**
   * Returns the string-value of the node.
   */
  public String stringValue(Node node)
  {
    return XmlUtil.textValue(node);
  }
  
  /*
   * Returns the position() value.  Note, this is not the same as
   * positionIndex.
   */
  public void setPosition(int position)
  {
    _position = position;
  }
  
  public int setContextPosition(int position)
  {
    int oldPosition = _position;
    _position = position;
    return oldPosition;
  }

  /**
   * Sets the context size to a know value.
   */
  public int setContextSize(int size)
  {
    int oldSize = _size;
    _size = size;
    return oldSize;
  }

  public Object getCache(Object key)
  {
    if (_root._cache == null)
      return null;
    else
      return _root._cache.get(key);
  }

  public void setCache(Object key, Object value)
  {
    if (_root._cache == null)
      _root._cache = new HashMap();

    _root._cache.put(key, value);
  }

  public int getUseCount()
  {
    return _useCount;
  }

  public void free()
  {
    _root = null;
    _parent = null;
    _select = null;

    _exprEnv = null;
    _stylesheetEnv = null;
    
    if (_ids != null) {
      _ids.clear();
      _idCache.clear();
    }

    while (_varSize-- > 0) {
      _varKeys[_varSize] = null;
      _varValues[_varSize] = null;
    }
    _varSize = 0;

    _lastElement = null;
    if (_globals != null)
      _globals.clear();
    _functions = null;
    _cache = null;
    _currentNode = null;
    _contextNode = null;

    _size = 0;
    _position = 0;
    _positionIndex = 0;
    _hasMorePositions = false;

    _freeList.free(this);
  }
}
