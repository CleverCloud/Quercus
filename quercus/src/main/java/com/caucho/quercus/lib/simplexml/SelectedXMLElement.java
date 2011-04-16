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
 * @author Charles Reich
 */
package com.caucho.quercus.lib.simplexml;

import com.caucho.quercus.env.*;

import java.util.*;

/**
 * SimpleXMLElement object oriented API facade.
 * Also acts as the DOM document.
 */
public class SelectedXMLElement extends SimpleXMLElement {

   private SimpleXMLElement _owner;

   protected SelectedXMLElement(Env env, QuercusClass cls,
           SimpleXMLElement owner) {
      super(env, cls, owner._parent, owner._name);

      _owner = owner;
      //_parent = owner._parent;
      //_name = owner._name;

      _text = owner._text;
      _children = owner._children;
      _attributes = owner._attributes;
   }

   @Override
   protected SimpleXMLElement getOwner() {
      return _owner;
   }

   @Override
   public String toString() {
      if (_text != null) {
         return _text.toString();
      } else {
         return "";
      }
   }

   /**
    * Required for 'foreach'. When only values are specified in
    * the loop <code>foreach($a as $b)</code>, this method
    * should return an iterator that contains Java objects
    * that will be wrapped in a Value.
    *
    * When a 'foreach' loop with name/value pairs
    * i.e. <code>foreach($a as $b=>$c)</code>
    * invokes this method, it expects an iterator that
    * contains objects that implement Map.Entry.
    */
   @Override
   public Iterator iterator() {
      // php/1x05

      ArrayList<SimpleXMLElement> children = _parent._children;

      if (children == null) {
         return null;
      }

      ArrayList<SimpleXMLElement> values = new ArrayList<SimpleXMLElement>();

      int size = children.size();
      for (int i = 0; i < size; i++) {
         SimpleXMLElement elt = children.get(i);

         if (getName().equals(elt.getName())) {
            values.add(elt);
         }
      }

      return values.iterator();
   }
}
