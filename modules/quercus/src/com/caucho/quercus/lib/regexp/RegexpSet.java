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

package com.caucho.quercus.lib.regexp;

import java.util.HashMap;

import com.caucho.util.*;

// XXX: non-ascii range not quite correct for unicode, and neither is
// PHP's /u unicode option
class RegexpSet {
  static final int BITSET_CHARS = 128;

  static RegexpSet SPACE = null;
  static RegexpSet WORD = null;
  static RegexpSet DIGIT = null;
  static RegexpSet DOT = null;
  
  // POSIX character classes
  static RegexpSet PALNUM = null;
  static RegexpSet PALPHA = null;
  static RegexpSet PASCII = null;
  static RegexpSet PBLANK = null;
  static RegexpSet PCNTRL = null;
  static RegexpSet PDIGIT = null;
  static RegexpSet PGRAPH = null;
  static RegexpSet PLOWER = null;
  static RegexpSet PPRINT = null;
  static RegexpSet PPUNCT = null;
  static RegexpSet PSPACE = null;
  static RegexpSet PUPPER = null;
  static RegexpSet PXDIGIT = null;
  
  static HashMap<String,RegexpSet> CLASS_MAP = null;
  
  boolean []_bitset = new boolean[BITSET_CHARS];
  IntSet _range;

  /**
   * Create a new RegexpSet
   */
  RegexpSet()
  {
    _range = new IntSet();
  }

  /**
   * Create a new RegexpSet
   */
  RegexpSet(RegexpSet old)
  {
    System.arraycopy(old._bitset, 0, _bitset, 0, _bitset.length);

    _range = (IntSet) old._range.clone();
  }
  
  /**
   * Ors two character sets.
   */
  void mergeOr(RegexpSet b)
  {
    for (int i = 0; i < BITSET_CHARS; i++)
      _bitset[i] = _bitset[i] || b._bitset[i];

    _range.union(b._range);
  }

  /**
   * Ors a set with the inverse of another.
   */
  void mergeOrInv(RegexpSet b)
  {
    for (int i = 0; i < BITSET_CHARS; i++)
      _bitset[i] = _bitset[i] || ! b._bitset[i];

    _range.unionNegate(b._range, 0, 0xfffff);
  }

  /**
   * Set a range of characters in a character set.
   */
  void setRange(int low, int high)
  {
    // php/4es0
    // http://bugs.caucho.com/view.php?id=3811
    if (low > high || low < 0)
      throw new RuntimeException(
          "Range out of range (" + low + ", " + high + ")");

    if (low < BITSET_CHARS) {
      for (int i = low; i < Math.min(high + 1, BITSET_CHARS); i++)
        _bitset[i] = true;

      if (high < BITSET_CHARS)
        return;

      low = BITSET_CHARS;
    }

    _range.union(low, high);
  }

  /**
   * Calculate the intersection of two sets.
   *
   * @return true if disjoint
   */
  boolean mergeOverlap(RegexpSet next)
  {
    boolean isDisjoint = true;

    for (int i = 0; i < BITSET_CHARS; i++) {
      _bitset[i] = _bitset[i] & next._bitset[i];
      
      if (_bitset[i])
        isDisjoint = false;
    }

    if (_range.intersection(next._range))
      isDisjoint = false;

    return isDisjoint;
  }

  /**
   * Calculate the difference of two sets.
   *
   * @return true if disjoint
   */
  void difference(RegexpSet next)
  {
    for (int i = 0; i < BITSET_CHARS; i++) {
      _bitset[i] = _bitset[i] & ! next._bitset[i];
    }

    _range.difference(next._range);
  }

  /*
   *   Returns true if the character is in the set.
   */
  boolean match(int ch)
  {
    if (ch < 0)
      return false;
    else if (ch < BITSET_CHARS)
      return _bitset[ch];
    else {
      return _range.contains(ch);
    }
  }

  RegexpNode createNode()
  {
    if (_range.size() == 0)
      return new RegexpNode.AsciiSet(_bitset);
    else
      return new RegexpNode.Set(_bitset, _range);
  }

  RegexpNode createNotNode()
  {
    if (_range.size() == 0)
      return new RegexpNode.AsciiNotSet(_bitset);
    else
      return new RegexpNode.NotSet(_bitset, _range);
  }
  
  int getSize()
  {
    return _range.size();
  }

  static {
    SPACE = new RegexpSet();
    SPACE.setRange(' ', ' ');
    SPACE.setRange(0x09, 0x0A); //tab to newline
    SPACE.setRange(0x0C, 0x0D); //form feed to carriage return

    DOT = new RegexpSet();
    DOT.setRange('\n', '\n');

    DIGIT = new RegexpSet();
    DIGIT.setRange('0', '9');

    WORD = new RegexpSet();
    WORD.setRange('a', 'z');
    WORD.setRange('A', 'Z');
    WORD.setRange('0', '9');
    WORD.setRange('_', '_');
    
    PASCII = new RegexpSet();
    PASCII.setRange(0, 0x7F);
    PASCII.setRange(0x81, 0x87);
    PASCII.setRange(0x89, 0x97);
    PASCII.setRange(0x9A, 0xFF);
    
    PBLANK = new RegexpSet();
    PBLANK.setRange(' ', ' ');
    PBLANK.setRange('\t', '\t');
    PBLANK.setRange(0xA0, 0xA0);
    
    PCNTRL = new RegexpSet();
    PCNTRL.setRange(0, 0x1F);
    PCNTRL.setRange(0x7F, 0x7F);
    PCNTRL.setRange(0x81, 0x81);
    PCNTRL.setRange(0x8D, 0x8D);
    PCNTRL.setRange(0x8F, 0x90);
    PCNTRL.setRange(0x9D, 0x9D);

    PDIGIT = new RegexpSet();
    PDIGIT.setRange('0', '9');
    PDIGIT.setRange(0xB2, 0xB3);
    PDIGIT.setRange(0xB9, 0xB9);
    
    PLOWER = new RegexpSet();
    PLOWER.setRange('a', 'z');
    PLOWER.setRange(0x83, 0x83);
    PLOWER.setRange(0x9A, 0x9A);
    PLOWER.setRange(0x9C, 0x9C);
    PLOWER.setRange(0x9E, 0x9E);
    PLOWER.setRange(0xAA, 0xAA);
    PLOWER.setRange(0xB5, 0xB5);
    PLOWER.setRange(0xBA, 0xBA);
    PLOWER.setRange(0xDF, 0xF6);
    PLOWER.setRange(0xF8, 0xFF);
    
    PSPACE = new RegexpSet();
    PSPACE.setRange(' ', ' ');
    PSPACE.setRange(0x09, 0x0D);
    PSPACE.setRange(0xA0, 0xA0);
    
    PUPPER = new RegexpSet();
    PUPPER.setRange('A', 'Z');
    PUPPER.setRange(0x8A, 0x8A);
    PUPPER.setRange(0x8C, 0x8C);
    PUPPER.setRange(0x8E, 0x8E);
    PUPPER.setRange(0x9F, 0x9F);
    PUPPER.setRange(0xC0, 0xD6);
    PUPPER.setRange(0xD8, 0xDE);
    
    PXDIGIT = new RegexpSet();
    PXDIGIT.setRange('0', '9');
    PXDIGIT.setRange('A', 'F');
    PXDIGIT.setRange('a', 'f');
    
    PALPHA = new RegexpSet();
    PALPHA.mergeOr(PLOWER);
    PALPHA.mergeOr(PUPPER);
    
    PALNUM = new RegexpSet();
    PALNUM.mergeOr(PALPHA);
    PALNUM.mergeOr(PDIGIT);
    
    PPUNCT = new RegexpSet();
    PPUNCT.setRange(0x21, 0x2F);
    PPUNCT.setRange(0x3A, 0x40);
    PPUNCT.setRange(0x5B, 0x60);
    PPUNCT.setRange(0x7B, 0x7E);
    PPUNCT.setRange(0x82, 0x82);
    PPUNCT.setRange(0x84, 0x87);
    PPUNCT.setRange(0x89, 0x89);
    PPUNCT.setRange(0x8B, 0x8B);
    PPUNCT.setRange(0x91, 0x97);
    PPUNCT.setRange(0x9B, 0x9B);
    PPUNCT.setRange(0xA1, 0xBF);
    PPUNCT.setRange(0xD7, 0xD7);
    PPUNCT.setRange(0xF7, 0xF7);
    
    PGRAPH = new RegexpSet();
    PGRAPH.mergeOr(PALNUM);
    PGRAPH.mergeOr(PPUNCT);
    
    PPRINT = new RegexpSet();
    PPRINT.mergeOr(PGRAPH);
    PPRINT.setRange(' ', ' ');
    PPRINT.setRange(0x09, 0x09);
    PPRINT.setRange(0xA0, 0xA0);
    
    CLASS_MAP = new HashMap<String,RegexpSet>();
    CLASS_MAP.put("alnum", PALNUM); //php/4ek0
    CLASS_MAP.put("alpha", PALPHA); //php/4ek1
    CLASS_MAP.put("ascii", PASCII); //php/4ek2
    CLASS_MAP.put("blank", PBLANK); //php/4ek3
    CLASS_MAP.put("cntrl", PCNTRL); //php/4ek4
    CLASS_MAP.put("digit", PDIGIT); //php/4ek5
    CLASS_MAP.put("graph", PGRAPH); //php/4ek6
    CLASS_MAP.put("lower", PLOWER); //php/4ek7
    CLASS_MAP.put("print", PPRINT); //php/4ek8
    CLASS_MAP.put("punct", PPUNCT); //php/4ek9
    CLASS_MAP.put("space", PSPACE); //php/4eka
    CLASS_MAP.put("upper", PUPPER); //php/4ekb
    CLASS_MAP.put("xdigit", PXDIGIT); //php/4ekc
  }
}
