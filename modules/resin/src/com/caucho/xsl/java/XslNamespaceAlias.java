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

package com.caucho.xsl.java;

import com.caucho.java.JavaWriter;
import com.caucho.xml.QName;
import com.caucho.xsl.XslParseException;

/**
 * the xsl:namespace-alias tag
 */
public class XslNamespaceAlias extends XslNode implements XslTopNode {
  private String _stylesheetPrefix;
  private String _resultPrefix;
  
  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:namespace-alias";
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("stylesheet-prefix")) {
      _stylesheetPrefix = value;
    }
    else if (name.getName().equals("result-prefix")) {
      _resultPrefix = value;
    }
    else
      super.addAttribute(name, value);
  }
  
  /**
   * called at end of the attributes
   */
  public void endAttributes()
    throws XslParseException
  {
    if (_stylesheetPrefix == null)
      throw error(L.l("'stylesheet-prefix' is a required attribute of <xsl:namespace-alias>"));
    if (_resultPrefix == null)
      throw error(L.l("'result-prefix' is a required attribute of <xsl:namespace-alias>"));
  }

  /**
   * Called when the element ends.
   */
  public void endElement()
    throws Exception
  {
    String stylesheetNs = getNamespace(_stylesheetPrefix);
    
    if (_stylesheetPrefix.equals("#default"))
      stylesheetNs = "";
    else if (stylesheetNs.equals(""))
      throw error(L.l("'{0}' is not a valid namespace prefix",
                      _stylesheetPrefix));
    
    String resultNs = getNamespace(_resultPrefix);
    if (_resultPrefix.equals("#default")) {
      _resultPrefix = "";
      resultNs = "";
    }
    else if (resultNs == null)
      throw error(L.l("'{0}' is not a valid namespace prefix",
                      _resultPrefix));
    
    String result[] = new String[] { _resultPrefix, resultNs };

    _gen.addNamespaceAlias(stylesheetNs, result);
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
  }
}
