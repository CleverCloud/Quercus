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

package com.caucho.relaxng;

import com.caucho.config.ConfigException;
import com.caucho.relaxng.pattern.*;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Builder for the relax.
 */
public class RelaxBuilder extends DefaultHandler {
  private static final String RELAX_S = "http://relaxng.org/ns/structure/1.0";

  private static final L10N L = new L10N(RelaxBuilder.class);

  private GrammarPattern _rootGrammar;
  private GrammarPattern _grammar;

  private Pattern _pattern;
  private String _ns = "";
  private HashMap<String,String> _nsMap = new HashMap<String,String>();

  private CharBuffer _cb = new CharBuffer();

  private ArrayList<Pattern> _patternStack = new ArrayList<Pattern>();
  private ArrayList<String> _nsStack = new ArrayList<String>();
  private ArrayList<HashMap<String,String>> _nsMapStack
  = new ArrayList<HashMap<String,String>>();

  RelaxBuilder()
  {
  }

  /**
   * Gets the root pattern.
   */
  public GrammarPattern getGrammar()
  {
    return _rootGrammar;
  }
  
  public void startPrefixMapping (String prefix, String uri)
    throws SAXException
  {
    _nsMapStack.add(_nsMap);
    _nsMap = new HashMap<String,String>(_nsMap);
    _nsMap.put(prefix, uri);
  }
  
  public void endPrefixMapping(String prefix)
    throws SAXException
  {
    _nsMapStack.add(_nsMap);
    _nsMap = _nsMapStack.remove(_nsMapStack.size() - 1);
  }
  
  /**
   * handles new element.
   */
  public void startElement(String uri, String localName,
                           String qName, Attributes attrs)
    throws SAXException
  {
    _cb.clear();
    
    try {
      if (RELAX_S.equals(uri)) {
        _patternStack.add(_pattern);
        _nsStack.add(_ns);

        String ns = attrs.getValue("ns");
        if (ns != null)
          _ns = ns;
      
        if (localName.equals("grammar")) {
          _grammar = new GrammarPattern();
          
          if (_rootGrammar == null)
            _rootGrammar = _grammar;

          _pattern = null;
        }
        else if (localName.equals("start")) {
          if (_pattern != null || _grammar == null)
            throw new RelaxException("<start> must be a direct child of <grammar>.");

          
          _pattern = new GroupPattern();
          _grammar.setStart(_pattern);
        }
        else if (localName.equals("define")) {
          if (_pattern != null || _grammar == null)
            throw new RelaxException("<define> must be a direct child of <grammar>.");

          String name = attrs.getValue("name");
          if (name == null || name.equals(""))
            throw new RelaxException(L.l("<define> requires a `name' attribute."));
          
          _pattern = new GroupPattern();
          _grammar.setDefinition(name, _pattern);
        }
        else if (localName.equals("name")) {
          if (_pattern == null)
            throw new RelaxException("<name> must be child of <element> or <attribute>.");

          _pattern = null;
        }
        else if (localName.equals("nsName")) {
          if (_pattern == null)
            throw new RelaxException("<nsName> must be child of <element> or <attribute>.");

          _pattern = new NsNamePattern();
        }
        else if (localName.equals("anyName")) {
          if (_pattern == null)
            throw new RelaxException("<anyName> must be child of <element> or <attribute>.");

          _pattern = new AnyNamePattern();
        }
        else if (localName.equals("except")) {
          if (_pattern == null)
            throw new RelaxException("<except> must be child of <nsName> or <anyName>.");

          _pattern = new ExcludeNamePattern();
        }
        else {
          if (_grammar == null) {
            _grammar = new GrammarPattern();
            _rootGrammar = _grammar;
            _pattern = new GroupPattern();
            _grammar.setStart(_pattern);
            
            _patternStack.set(_patternStack.size() - 1, _pattern);
          }
          
          Pattern child = null;

          if (localName.equals("element")) {
            String name = attrs.getValue("name");

            String defName = _grammar.generateId();
            
            child = new ElementPattern(defName);

            if (name != null) {
              QName eltQName = getName(name);
              child.addNameChild(new NamePattern(eltQName));
            }

            if (name != null) {
              QName attrQName = getName(name);
              child.addNameChild(new NamePattern(attrQName));
            }
            
            _grammar.setDefinition(defName, child);
          }
          else if (localName.equals("attribute")) {
            String name = attrs.getValue("name");

            child = new AttributePattern();
            
            if (name != null) {
              QName attrQName = new QName(name, _ns);
              child.addNameChild(new NamePattern(attrQName));
            }
          }
          else if (localName.equals("empty")) {
            child = new EmptyPattern();
          }
          else if (localName.equals("text")) {
            child = new TextPattern();
          }
          else if (localName.equals("data")) {
            String type = attrs.getValue("type");

            if (type == null)
              throw new RelaxException(L.l("<data> requires a `type' attribute."));
            
            child = new DataPattern(type);
          }
          else if (localName.equals("value")) {
            child = new DataPattern("string");
          }
          else if (localName.equals("choice")) {
            if (_pattern instanceof ElementPattern) {
              ElementPattern eltPattern = (ElementPattern) _pattern;

              if (eltPattern.getNameChild() == null) {
                child = new ChoiceNamePattern();
                eltPattern.addNameChild((NameClassPattern) child);
                _pattern = child;
                return;
              }
            }
            
            if (_pattern instanceof AttributePattern) {
              AttributePattern attrPattern = (AttributePattern) _pattern;

              if (attrPattern.getNameChild() == null) {
                child = new ChoiceNamePattern();
                attrPattern.addNameChild((NameClassPattern) child);
                _pattern = child;
                return;
              }
            }

            child = new ChoicePattern();
          }
          else if (localName.equals("group")) {
            child = new GroupPattern();
          }
          else if (localName.equals("interleave")) {
            child = new InterleavePattern();
          }
          else if (localName.equals("zeroOrMore")) {
            child = new ZeroOrMorePattern();
          }
          else if (localName.equals("oneOrMore")) {
            child = new ZeroOrMorePattern();
          }
          else if (localName.equals("optional")) {
            Pattern choice = new ChoicePattern();
            choice.addChild(new EmptyPattern());
            choice.setParent(_pattern);

            Pattern group = new GroupPattern();
            group.setParent(choice);
            choice.addChild(group);
            _pattern = group;
            return;
          }
          else if (localName.equals("ref")) {
            String name = attrs.getValue("name");
            if (name == null || name.equals(""))
              throw new RelaxException(L.l("<define> requires a `name' attribute."));
            child = new RefPattern(_grammar, name);
          }
          else
            throw new ConfigException(L.l("<{0}> is an unknown relax element.",
                                               localName));

          child.setParent(_pattern);
          if (child.getElementName() == null)
            child.setElementName(_pattern.getElementName());
        
          _pattern = child;
        }
      }
    } catch (Exception e) {
      throw new SAXException(getLocation() + e.getMessage(), e);
    }
  }
  
  public void characters (char ch[], int start, int length)
    throws SAXException
  {
    _cb.append(ch, start, length);
  }
  
  /**
   * handles the end of an element.
   */
  public void endElement(String uri, String localName, String qName)
    throws SAXException
  {
    try {
      if (RELAX_S.equals(uri)) {
        NameClassPattern nameChild = null;
        Pattern child = _pattern;
        String ns = _ns;
        
        _pattern = _patternStack.remove(_patternStack.size() - 1);
        _ns = _nsStack.remove(_nsStack.size() - 1);

        if (localName.equals("name")) {
          nameChild = new NamePattern(getName(_cb.toString()));

          if (_pattern != null)
            _pattern.addNameChild(nameChild);
        }
        else if (localName.equals("nsName")) {
          NsNamePattern nsName = (NsNamePattern) child;

          nsName.setNamespace(ns);

          if (_pattern != null)
            _pattern.addNameChild(nsName);
        }
        else if (localName.equals("anyName")) {
          nameChild = (AnyNamePattern) child;

          if (_pattern != null)
            _pattern.addNameChild(nameChild);
        }
        else if (localName.equals("except")) {
          ExcludeNamePattern exclude = (ExcludeNamePattern) child;

          if (_pattern instanceof NsNamePattern) {
            NsNamePattern nsPattern = (NsNamePattern) _pattern;

            nsPattern.setExcept(exclude.getNameChild());
          }

          if (_pattern instanceof AnyNamePattern) {
            AnyNamePattern anyNamePattern = (AnyNamePattern) _pattern;

            anyNamePattern.setExcept(exclude.getNameChild());
          }
        }
        else if (localName.equals("optional")) {
          if (_pattern != null)
            _pattern.addChild(child.getParent());
        }
        else if (localName.equals("oneOrMore")) {
          ZeroOrMorePattern starPattern = (ZeroOrMorePattern) child;
          Pattern subPattern = starPattern.getPatterns();
          Pattern group = new GroupPattern();
          group.addChild(subPattern);
          group.addChild(starPattern);
          
          if (_pattern != null)
            _pattern.addChild(group);
        }
        else if (child instanceof NameClassPattern) {
        }
        else {
          if (child != null)
            child.endElement();
      
          if (_pattern != null)
            _pattern.addChild(child);
        }
      }
    } catch (Exception e) {
      throw new SAXException(getLocation() + e.getMessage(), e);
    }
  }

  private QName getName(String name)
  {
    int p = name.lastIndexOf(':');

    if (p > 0) {
      String prefix = name.substring(0, p);
      String ns = _nsMap.get(prefix);

      if (ns != null)
        return new QName(name, ns);
      else
        return new QName(name, _ns);
    }
    else
      return new QName(name, _ns);
  }

  /**
   * Returns the location.
   */
  public String getLocation()
  {
    return "";
  }
}
