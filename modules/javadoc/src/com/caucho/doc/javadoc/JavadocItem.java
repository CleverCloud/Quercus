/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits redistribution, modification and use
 * of this file in source and binary form ("the Software") under the
 * Caucho Developer Source License ("the License").  The following
 * conditions must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Redistributions of the Software in source or binary form must include
 *    an unmodified copy of the License, normally in a plain ASCII text
 *
 * 3. The names "Resin" or "Caucho" are trademarks of Caucho Technology and
 *    may not be used to endorse products derived from this software.
 *    "Resin" or "Caucho" may not appear in the names of products derived
 *    from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.
 *
 * @author Sam 
 */

package com.caucho.doc.javadoc;

import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * A single javadoc item, a package, class, member, or method.
 */
public class JavadocItem {
  static protected final Logger log = Log.open(JavadocItem.class);
  static final L10N L = new L10N(JavadocItem.class);

  public final static int PACKAGE=0x01;
  public final static int CLASS=0x02;
  public final static int METHOD=0x04;
  public final static int VARIABLE=0x08;
  public final static int ANY=PACKAGE | CLASS | METHOD | VARIABLE;

  String _name;
  String _fullName;
  int _type;
  JavadocFile _file;
  String _anchor;
  String _description;

  boolean _exact = false;

  public JavadocItem(String name, String fullName, int type, String anchor, String description, JavadocFile file)
  {
    _name = name;
    _fullName = fullName;
    _type = type;
    _file = file;
    _anchor = anchor;
    _description = description;
  }

  public String getName()
  {
    return _name;
  }

  public String getFullName()
  {
    return _fullName;
  }

  public int getType()
  {
    return _type;
  }

  public String getTypeString()
  {
    switch (_type) {
      case PACKAGE:
        return "package";
      case CLASS:
        return "class";
      case METHOD:
        return "method";
      case VARIABLE:
        return "var";
    }
    return "unknown";
  }

  public JavadocFile getFile()
  {
    return _file;
  }

  public String getAnchor()
  {
    return _anchor;
  }

  public String getDescription()
  {
    return _description;
  }

  void setExact(boolean exact)
  {
    _exact = exact;
  }

  /**
   * If this JavadocItem is generated as a result of a query, it may be flagged
   * as exact to indicate that it would be appropriate for this item to be
   * displayed automatically.
   */
  public boolean getExact()
  {
    return _exact;
  }

  public String getHref()
  {
    CharBuffer cb = CharBuffer.allocate();
    cb.append(_file.getHref());
    if (_anchor != null) {
      cb.append('#');
      cb.append(_anchor);
    }
    return cb.close();
  }
}

