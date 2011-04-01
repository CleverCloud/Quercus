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

package com.caucho.xml;

import com.caucho.relaxng.*;
import com.caucho.relaxng.pattern.*;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.*;

class DtdRelaxGenerator {
  private static final Logger log
    = Logger.getLogger(DtdRelaxGenerator.class.getName());
  
  private QDocumentType _dtd;
  private GrammarPattern _grammar;

  DtdRelaxGenerator(QDocumentType dtd)
  {
    _dtd = dtd;
  }

  ContentHandler generate()
  {
    try {
      _grammar = new GrammarPattern();

      HashMap<String,QElementDef> elementMap = _dtd.getElementMap();

      for (QElementDef elt : elementMap.values()) {
        String name = elt.getName();

        Pattern pattern = null;

        pattern = parseContentParticle(elt.getContent());

        if (pattern != null) {
          ElementPattern eltPattern = new ElementPattern(name);
          eltPattern.addNameChild(new NamePattern(new QName(name)));
          eltPattern.addChild(pattern);
          eltPattern.endElement();

          _grammar.setDefinition(name, eltPattern);
        }
      }

      Pattern start = _grammar.getDefinition(_dtd.getName());

      if (start == null)
        return null;

      _grammar.setStart(start);

      SchemaImpl schema = new SchemaImpl(_grammar);

      VerifierHandlerImpl handler = new VerifierHandlerImpl(schema);

      return handler;
    } catch (Exception e) {
      e.printStackTrace();
      
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  private Pattern parseContentParticle(Object obj)
    throws Exception
  {
    if (obj instanceof QContentParticle) {
      QContentParticle cp = (QContentParticle) obj;

      Pattern pattern = null;
      boolean isText = false;

      if (cp.getSeparator() == ',') {
        pattern = new GroupPattern();
      }
      else if (cp.getSeparator() == '|') {
        pattern = new ChoicePattern();
      }
      else if (cp.getSeparator() == '&') {
        pattern = new InterleavePattern();
      }
      else
        pattern = new GroupPattern();

      for (int i = 0; i < cp.getChildSize(); i++) {
        Pattern child = parseContentParticle(cp.getChild(i));

        if (child instanceof TextPattern) {
          isText = true;
          continue;
        }

        if (child == null) {
          log.finer(this + " " + cp.getChild(i) + " is an unknown CP");

          return null;
        }

        pattern.addChild(child);
      }

      pattern.endElement();

      if (cp.getRepeat() == '*')
        pattern = new ZeroOrMorePattern(pattern);
      else if (cp.getRepeat() == '?') {
        Pattern group = new ChoicePattern();
        group.addChild(new EmptyPattern());
        group.addChild(pattern);
        group.endElement();

        pattern = group;
      }
      else if (cp.getRepeat() == '+') {
        Pattern group = new GroupPattern();
        group.addChild(pattern);
        group.addChild(new ZeroOrMorePattern(pattern));
        group.endElement();

        pattern = group;
      }

      if (isText) {
        Pattern group = new InterleavePattern();
        group.addChild(pattern);
        group.addChild(new TextPattern());
        group.endElement();
        pattern = group;
      }

      return pattern;
    }
    else if (obj instanceof String) {
      String s = (String) obj;

      if ("EMPTY".equals(s))
        return new EmptyPattern();
      else if (s.startsWith("#"))
        return new TextPattern();
      else
        return new RefPattern(_grammar, s);
    }
    else
      return null;
  }
}
