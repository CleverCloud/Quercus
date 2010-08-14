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

package com.caucho.xsl;

import com.caucho.java.LineMap;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntArray;
import com.caucho.vfs.Path;
import com.caucho.xml.CauchoNode;
import com.caucho.xml.QAbstractNode;
import com.caucho.xml.QElement;
import com.caucho.xml.XMLWriter;
import com.caucho.xml.XmlChar;
import com.caucho.xml.XmlUtil;
import com.caucho.xpath.Env;
import com.caucho.xpath.Expr;
import com.caucho.xpath.StylesheetEnv;
import com.caucho.xpath.XPath;
import com.caucho.xpath.XPathException;
import com.caucho.xpath.XPathFun;
import com.caucho.xpath.pattern.AbstractPattern;
import com.caucho.xpath.pattern.NodeIterator;
import com.caucho.xsl.fun.DocumentFun;
import com.caucho.xsl.fun.ExtensionElementFun;
import com.caucho.xsl.fun.ExtensionFunctionFun;
import com.caucho.xsl.fun.SystemPropertyFun;
import com.caucho.xsl.fun.UnparsedEntityFun;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Implementation base class for stylesheets.  It is made public only
 * because generated Java and JavaScript classes need to access these
 * routines.
 */
public class StylesheetImpl extends AbstractStylesheet  {
  private static final Logger log
    = Logger.getLogger(StylesheetImpl.class.getName());

  public char []text; // static buffer of the text nodes

  protected HashMap templates;
  HashMap<String,XPathFun> _funs = new HashMap<String,XPathFun>();

  private HashMap<String,String> _preserve;
  private HashMap<String,String> _strip;
  private HashMap<String,String> _preservePrefix;
  private HashMap<String,String> _stripPrefix;
  private HashMap<String,Object> _properties = new HashMap<String,Object>();

  boolean isCacheable = true;
  protected boolean _defaultDisableEscaping;
  Path cachePath;
  long lastModified;

  boolean _generateLocation;
  
  LineMap lineMap;

  protected void copy(AbstractStylesheet stylesheet)
  {
    super.copy(stylesheet);

    StylesheetImpl stylesheetImpl = (StylesheetImpl) stylesheet;
    
    stylesheetImpl.text = text;
    stylesheetImpl.templates = templates;
    stylesheetImpl._preserve = _preserve;
    stylesheetImpl._strip = _strip;
    stylesheetImpl._preservePrefix = _preservePrefix;
    stylesheetImpl._stripPrefix = _stripPrefix;
    stylesheetImpl.lineMap = lineMap;
    stylesheetImpl._properties = _properties;
    stylesheetImpl._defaultDisableEscaping = _defaultDisableEscaping;
  }

  public OutputFormat getOutputFormat()
  {
    return new OutputFormat();
  }

  public void setOutputFormat(OutputFormat output)
  {
  }

  protected void setSpaces(HashMap<String,String> preserve,
                           HashMap<String,String> preservePrefix,
                           HashMap<String,String> strip,
                           HashMap<String,String> stripPrefix)
  {
    _preserve = preserve;
    _strip = strip;
    _preservePrefix = preservePrefix;
    _stripPrefix = stripPrefix;
  }

  public void setProperty(String name, Object value)
  {
    _properties.put(name, value);
  }

  public void setGenerateLocation(boolean generateLocation)
  {
    _generateLocation = generateLocation;
  }

  public boolean getGenerateLocation()
  {
    return _generateLocation;
  }

  public Object getProperty(String name)
  {
    Object value = _properties.get(name);
    if (value != null)
      return value;

    return super.getProperty(name);
  }

  protected void addFunction(String name, XPathFun fun)
  {
    _funs.put(name, fun);
  }

  public void init(Path path)
    throws Exception
  {
    super.init(path);

    addFunction("system-property",  new SystemPropertyFun());
    addFunction("element-available", new ExtensionElementFun());
    addFunction("function-available", new ExtensionFunctionFun());
    addFunction("unparsed-entity-uri", new UnparsedEntityFun());
  }

  /**
   * Transforms the input node to the output writer
   *
   * @param xml the input node to be transformed
   * @param writer output writer receiving the output
   * @param transformer the transformer to be used
   */
  public void transform(Node xml,
                        XMLWriter writer,
                        TransformerImpl transformer)
    throws SAXException, IOException, TransformerException
  {
    if (xml == null)
      throw new NullPointerException("can't transform null node");
    
    XslWriter out = new XslWriter(null, this, transformer);
    out.init(writer);

    if (_funs == null)
      _funs = (HashMap) ((StylesheetImpl) _stylesheet)._funs.clone();
    else
      _funs.putAll((HashMap) ((StylesheetImpl) _stylesheet)._funs);

    addFunction("document", new DocumentFun(transformer));
    DocumentFun docFun = new DocumentFun(transformer);
    docFun.setHtml(true);
    addFunction("html_document", docFun);

    Env env = XPath.createEnv();
    env.setFunctions(_funs);
    StylesheetEnv ssEnv = new StylesheetEnv();
    ssEnv.setPath(getPath());
    env.setStylesheetEnv(ssEnv);

    out.disableEscaping(_defaultDisableEscaping);

    if (_strip != null && ! _strip.isEmpty()) {
      stripSpaces(xml);
    }
    
    try {
      _xsl_init(out, xml, env);

      applyNode(out, xml, env, 0, Integer.MAX_VALUE);
    } catch (TransformerException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (SAXException e) {
      throw e;
    } catch (Exception e) {
      throw new XslException(e, lineMap);
    }

    out.close();

    XPath.freeEnv(env);

    // funs = null;
  }

  protected void _xsl_init(XslWriter out, Node context, Env env)
    throws Exception
  {
  }

  protected Document ownerDocument(Node node)
  {
    Document owner = node.getOwnerDocument();
    
    if (owner != null)
      return owner;
    else
      return (Document) node;
  }

  public void applyNode(XslWriter out, Node node, Env env)
    throws Exception
  {
    applyNode(out, node, env, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }
  
  protected void applyNode(XslWriter out, Node node, Env env, int min, int max)
    throws Exception
  {
    if (node == null)
      return;

    switch (node.getNodeType()) {
    case Node.DOCUMENT_NODE:
    case Node.DOCUMENT_FRAGMENT_NODE:
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        applyNode(out, child, env, 0, 2147483647);
      }
      break;
      
    case Node.ELEMENT_NODE:
      out.pushCopy(node);
      if (node instanceof QElement) {
        for (Node child = ((QElement) node).getFirstAttribute();
             child != null;
             child = child.getNextSibling()) {
          applyNode(out, child, env, 0, 2147483647);
        }
      } else {
        NamedNodeMap attributeMap = ((Element) node).getAttributes();
        int size = attributeMap.getLength();
        for (int i = 0; i < size; i++) {
          Node child = attributeMap.item(i);
          
          applyNode(out, child, env, 0, 2147483647);
        }
      }
      
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        applyNode(out, child, env, 0, 2147483647);
      }
      out.popCopy(node);
      break;

    case Node.TEXT_NODE:
    case Node.CDATA_SECTION_NODE:
      String value = node.getNodeValue();
      out.print(value);
      return;
      
    case Node.ATTRIBUTE_NODE:
      out.pushCopy(node);
      out.popCopy(node);
      break;
      
    case Node.ENTITY_REFERENCE_NODE:
      out.pushCopy(node);
      out.popCopy(node);
      break;
    }
  }
  
  /**
   * Gets a template.
   *
   * Only those templates with importance between min and max are considered.
   * For apply-templates, min = 0, and max = Infinity, 
   *
   * @param min minimum allowed importance
   * @param max maximum allowed importance
   */
  protected Template getTemplate(HashMap templates,
                                 Node node, Env env, int min, int max)
    throws XPathException
  {
    Template template = null;

    Template []templateList = (Template []) templates.get(node.getNodeName());
    if (templateList == null)
      templateList = (Template []) templates.get("*");
    
    for (int i = 0; templateList != null && i < templateList.length; i++) {
      Template subtemplate = templateList[i];

      if (min <= subtemplate.maxImportance &&
          subtemplate.maxImportance <= max &&
          subtemplate.pattern.match(node, env)) {
        return subtemplate;
      }
    }

    return null;
  }

  /**
   * The default rule when no templates match.  By default, it
   * calls apply-template on element children and copies text.  All
   * other nodes are stripped.
   *
   * @param out the current writer.
   * @param node the current node.
   * @param env the xpath environment.
   */
  protected void applyNodeDefault(XslWriter out, Node node, Env env)
    throws Exception
  {
    switch (node.getNodeType()) {
    case Node.TEXT_NODE:
    case Node.CDATA_SECTION_NODE:
      if (_generateLocation && node instanceof QAbstractNode)
        out.setLocation(((QAbstractNode) node).getBaseURI(),
                        ((QAbstractNode) node).getFilename(),
                        ((QAbstractNode) node).getLine());
      String value = node.getNodeValue();
      out.print(value);
      return;
      
    case Node.ATTRIBUTE_NODE:
    case Node.ENTITY_REFERENCE_NODE:
      out.print(node.getNodeValue());
      break;

    case Node.ELEMENT_NODE:
    case Node.DOCUMENT_NODE:
      throw new RuntimeException();
    }
  }

  public void printValue(XslWriter out, Node node) throws IOException
  {
    if (node != null)
      out.print(getNodeValue(node));
  }

  public String getNodeValue(Node node)
  {
    CharBuffer cb = new CharBuffer();

    nodeValue(cb, node);

    return cb.toString();
  }

  private void nodeValue(CharBuffer cb, Node node)
  {
    if (node == null)
      return;

    switch (node.getNodeType()) {
    case Node.ELEMENT_NODE:
      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling()) {
        switch (child.getNodeType()) {
        case Node.ELEMENT_NODE:
        case Node.TEXT_NODE:
        case Node.CDATA_SECTION_NODE:
        case Node.ENTITY_REFERENCE_NODE:
          nodeValue(cb, child);
          break;
        }
      }
      break;

    case Node.ENTITY_REFERENCE_NODE:
      cb.append('&');
      cb.append(node.getNodeName());
      cb.append(';');
      break;

    case Node.DOCUMENT_NODE:
      Document doc = (Document) node;
      nodeValue(cb, doc.getDocumentElement());
      break;

    case Node.TEXT_NODE:
    case Node.CDATA_SECTION_NODE:
      String value = node.getNodeValue();
      cb.append(value);
      break;

    default:
      cb.append(node.getNodeValue());
      break;
    }
  }

  protected ArrayList xslSort(Node node, Env env, AbstractPattern pattern,
                              Sort []sortList)
    throws Exception
  {
    ArrayList<Node> sortKeys = new ArrayList<Node>();

    Iterator<Node> sortIter;
    NodeIterator iter = pattern.select(node, env);

    while (iter.hasNext()) {
      Node child = iter.next();
      sortKeys.add(child);
    }

    int []map = new int[sortKeys.size()];
    for (int i = map.length - 1; i >= 0; i--)
      map[i] = i;

    int []workMap = new int[map.length];
    
    Object []values = new Object[map.length * sortList.length];
    
    int size = map.length;
    for (int i = 0; i < size; i++) {
      Node child = (Node) sortKeys.get(i);
      
      env.setPosition(i + 1);
      // XXX: set last() as well

      for (int j = 0; j < sortList.length; j++) {
        Sort sort = sortList[j];
        Object value = sort.sortValue(child, env);

        values[i * sortList.length + j] = value;
      }
    }
    
    boolean []ascendingList = new boolean[sortList.length];

    for (int i = 0; i < ascendingList.length; i++) {
      Expr isAscending = sortList[i].getAscending();
      if (isAscending == null || isAscending.evalBoolean(node, env))
        ascendingList[i] = true;
    }

    Comparator []comparatorList = new Comparator[sortList.length];

    for (int i = 0; i < comparatorList.length; i++) {
      Expr langExpr = sortList[i].getLang();
      String lang = null;

      if (langExpr != null) {
        lang = langExpr.evalString(node, env);
      }

      if (lang != null)
        comparatorList[i] = getComparator(lang);
    }

    int []caseOrderList = new int[sortList.length];

    for (int i = 0; i < caseOrderList.length; i++) {
      Expr caseOrder = sortList[i].getCaseOrder();
      if (caseOrder == null)
        caseOrderList[i] = Sort.NO_CASE_ORDER;
      else if (caseOrder.evalBoolean(node, env))
        caseOrderList[i] = Sort.UPPER_FIRST;
      else
        caseOrderList[i] = Sort.LOWER_FIRST;
    }

    sort(values, sortList, comparatorList, ascendingList, caseOrderList,
         0, map.length, map, workMap);

    ArrayList sortedKeys = new ArrayList();

    for (int i = 0; i < map.length; i++)
      sortedKeys.add(sortKeys.get(map[i]));

    return sortedKeys;
  }

  /**
   * Returns the comparator for the language.
   */
  private Comparator getComparator(String lang)
  {
    Locale locale = getLocale(lang);

    return java.text.Collator.getInstance(locale);
  }

  /**
   * Returns the locale for the language.
   */
  private Locale getLocale(String lang)
  {
    int p = lang.indexOf('-');
    Locale locale = null;

    if (p < 0) {
      locale = new Locale(lang, "");
    }
    else {
      String language = lang.substring(0, p);
          
      int q = lang.indexOf(p + 1, '-');

      if (q < 0) {
        String country = lang.substring(p + 1);

        locale = new Locale(language, country);
      }
      else {
        String country = lang.substring(p + 1, q);
        String variant = lang.substring(q);

        locale = new Locale(language, country, variant);
      }
    }

    return locale;
  }

  /**
   * Sorts a subsequence.
   *
   * @param head the start of the subsequence
   * @param tail the tail of the subsequence
   */
  private void sort(Object []values, Sort []sortList,
                    Comparator []comparatorList,
                    boolean []ascendingList,
                    int []caseOrder,
                    int head, int tail,
                    int map[], int []workMap)
  {
    int length = tail - head;
    if (length <= 1)
      return;

    // shortcut when only have two items
    if (length == 2) {
      int a = map[head];
      int b = map[head + 1];

      if (lessThan(values, sortList, comparatorList,
                   ascendingList, caseOrder, b, a)) {
        map[head] = b;
        map[head + 1] = a;
      }
      return;
    }
    // shortcut when only have three items
    else if (length == 3) {
      int a = map[head];
      int b = map[head + 1];
      int c = map[head + 2];
      
      if (lessThan(values, sortList, comparatorList,
                   ascendingList, caseOrder, b, a)) {
        map[head] = b;
        map[head + 1] = a;
        a = map[head];
        b = map[head + 1];
      }

      if (! lessThan(values, sortList, comparatorList,
                     ascendingList, caseOrder, c, b)) {
      }
      else if (lessThan(values, sortList, comparatorList,
                        ascendingList, caseOrder, c, a)) {
        map[head] = c;
        map[head + 1] = a;
        map[head + 2] = b;
      }
      else {
        map[head + 1] = c;
        map[head + 2] = b;
      }

      return;
    }

    int pivotIndex = (head + tail) / 2;
    int pivot = map[pivotIndex];
    int top = tail;

    // values greater than the pivot value are put in the work map
    for (int i = tail - 1; i >= head; i--) {
      if (lessThan(values, sortList, comparatorList,
                   ascendingList, caseOrder, pivot, map[i])) {
        workMap[--top] = map[i];
        map[i] = -1;
      }
    }

    // if the pivot is the max, need to shift equals
    if (top == tail) {
      // values greater than the pivot value are put in the work map
      for (int i = tail - 1; i >= head; i--) {
        if (! lessThan(values, sortList, comparatorList,
                       ascendingList, caseOrder, map[i], pivot)) {
          workMap[--top] = map[i];
          map[i] = -1;
        }
      }

      // If all entries are equal to the pivot, we're done
      if (top == head) {
        for (int i = head; i < tail; i++)
          map[i] = workMap[i];
        return;
      }
    }
    
    // shift down the values less than the pivot
    int center = head;
    for (int i = head; i < tail; i++) {
      if (map[i] >= 0)
        map[center++] = map[i];
    }

    for (int i = center; i < tail; i++)
      map[i] = workMap[i];

    sort(values, sortList, comparatorList, ascendingList, caseOrder,
         head, center, map, workMap);
    sort(values, sortList, comparatorList, ascendingList, caseOrder,
         center, tail, map, workMap);
  }

  /**
   * Swaps two items in the map.
   */
  private void swap(int []map, int a, int b)
  {
    int ka = map[a];
    int kb = map[b];
    
    map[b] = ka;
    map[a] = kb;
  }

  /**
   * Returns true if the first value is strictly less than the second.
   */
  private boolean lessThan(Object []values,
                           Sort []sortList,
                           Comparator []comparatorList,
                           boolean []ascendingList,
                           int []caseOrder,
                           int ai, int bi)
  {
    int len = sortList.length;

    for (int i = 0; i < len; i++) {
      Object a = values[len * ai + i];
      Object b = values[len * bi + i];

      int cmp = sortList[i].cmp(a, b, comparatorList[i],
                                ascendingList[i], caseOrder[i]);
      if (cmp < 0)
        return true;
      else if (cmp > 0)
        return false;
    }

    return false;
  }

  public void singleNumber(XslWriter out, Node node, Env env,
                           AbstractPattern countPattern,
                           AbstractPattern fromPattern,
                           XslNumberFormat format)
    throws Exception
  {
    if (countPattern == null)
      countPattern = XPath.parseMatch(node.getNodeName()).getPattern();

    IntArray numbers = new IntArray();
    for (; node != null; node = node.getParentNode()) {
      if (countPattern.match(node, env)) {
        numbers.add(countPreviousSiblings(node, env, countPattern));
        break;
      }
      if (fromPattern != null && fromPattern.match(node, env))
        break;
    }
    if (fromPattern != null && ! findFromAncestor(node, env, fromPattern))  
      numbers.clear();

    format.format(out, numbers);
  }

  public void multiNumber(XslWriter out, Node node, Env env,
                          AbstractPattern countPattern,
                          AbstractPattern fromPattern,
                          XslNumberFormat format)
    throws Exception
  {
    if (countPattern == null)
      countPattern = XPath.parseMatch(node.getNodeName()).getPattern();

    IntArray numbers = new IntArray();
    for (; node != null; node = node.getParentNode()) {
      if (countPattern.match(node, env))
        numbers.add(countPreviousSiblings(node, env, countPattern));

      if (fromPattern != null && fromPattern.match(node, env))
        break;
    }
    if (fromPattern != null && ! findFromAncestor(node, env, fromPattern))  
      numbers.clear();

    format.format(out, numbers);
  }

  public void anyNumber(XslWriter out, Node node, Env env,
                        AbstractPattern countPattern,
                        AbstractPattern fromPattern,
                        XslNumberFormat format)
    throws Exception
  {
    if (countPattern == null)
      countPattern = XPath.parseMatch(node.getNodeName()).getPattern();

    IntArray numbers = new IntArray();
    int count = 0;
    for (; node != null; node = XmlUtil.getPrevious(node)) {
      if (countPattern.match(node, env))
        count++;

      if (fromPattern != null && fromPattern.match(node, env))
        break;
    }
    numbers.add(count);
    if (fromPattern != null && ! findFromAncestor(node, env, fromPattern))
      numbers.clear();

    format.format(out, numbers);
  }

  public void exprNumber(XslWriter out, Node node, Env env, Expr expr,
                         XslNumberFormat format)
    throws Exception
  {
    IntArray numbers = new IntArray();
    numbers.add((int) expr.evalNumber(node, env));

    format.format(out, numbers);
  }

  private int countPreviousSiblings(Node node, Env env, String name)
  {
    int count = 1;
    for (node = node.getPreviousSibling();
         node != null;
         node = node.getPreviousSibling()) {
      if (node.getNodeType() == node.ELEMENT_NODE &&
          node.getNodeName().equals(name))
        count++;
    }

    return count;
  }

  private int countPreviousSiblings(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    int count = 1;
    for (node = node.getPreviousSibling();
         node != null;
         node = node.getPreviousSibling()) {
      if (pattern.match(node, env))
        count++;
    }

    return count;
  }

  private boolean findFromAncestor(Node node, Env env, AbstractPattern pattern)
    throws XPathException
  {
    for (; node != null; node = node.getParentNode())
      if (pattern.match(node, env))
        return true;

    return false;
  }

  /**
   * Strips the spaces from a tree.
   */
  void stripSpaces(Node node)
  {
    Node child = node.getFirstChild();

    while (child != null) {
      Node next = child.getNextSibling();
      
      if (child instanceof Element) {
        stripSpaces(child);
      }
      else if (child instanceof Text) {
        String data = ((Text) child).getData();

        boolean hasContent = false;
        for (int i = data.length() - 1; i >= 0; i--) {
          char ch = data.charAt(i);

          if (! XmlChar.isWhitespace(ch)) {
            hasContent = true;
            break;
          }
        }

        if (! hasContent && isStripSpaces(node)) {
          node.removeChild(child);
        }
      }

      child = next;
    }
  }
  /**
   * Returns true if the node is a pure whitespace text node.
   */
  boolean isStripSpaces(Node node)
  {
    if (_strip == null)
      return false;
    
    for (Node ptr = node; ptr != null; ptr = ptr.getParentNode()) {
      if (ptr instanceof Element) {
        Element elt = (Element) ptr;
        String space = elt.getAttribute("xml:space");
        if (space != null && space.equals("preserve"))
          return false;
        else if (space != null)
          break;
      }
    }
    String name = node.getNodeName();
    if (_preserve.get(node.getNodeName()) != null)
      return false;
    else if (_strip.get(node.getNodeName()) != null)
      return true;

    CauchoNode cnode = (CauchoNode) node;
    String nsStar = cnode.getPrefix();
    if (_preservePrefix.get(nsStar) != null)
      return false;
    else if (_stripPrefix.get(nsStar) != null)
      return true;

    return _strip.get("*") != null;
  }

  /**
   * Merges two template arrays into the final one.
   */
  protected static Template []mergeTemplates(Template []star,
                                             Template []templates)
  {
    Template []merged = new Template[star.length + templates.length];

    int i = 0;
    int j = 0;
    int k = 0;

    while (i < star.length && j < templates.length) {
      if (star[i].compareTo(templates[j]) > 0)
        merged[k++] = star[i++];
      else
        merged[k++] = templates[j++];
    }

    for (; i < star.length; i++)
      merged[k++] = star[i];

    for (; j < templates.length; j++)
      merged[k++] = templates[j];

    return merged;
  }
}
