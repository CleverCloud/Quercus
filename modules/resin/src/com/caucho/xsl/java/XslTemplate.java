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
 * Represents the xsl:template node.
 */
public class XslTemplate extends XslNode implements XslTopNode {
  private String _match;
  private String _name;
  private String _mode;
  private double _priority = 0.0/0.0;
  private String _as;

  private String _macroName;

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("match"))
      _match = value;
    else if (name.getName().equals("name"))
      _name = value;
    else if (name.getName().equals("mode"))
      _mode = value;
    else if (name.getName().equals("priority"))
      _priority = Double.parseDouble(value);
    else if (name.getName().equals("as"))
      _as = value;
    else
      super.addAttribute(name, value);
  }

  /**
   * Ends the attributes.
   */
  public void endAttributes()
    throws XslParseException
  {
    if (_match == null && _name == null)
      throw error(L.l("xsl:template needs a 'match' or a 'name' attribute."));

    if (_name != null) {
      _macroName = ("_xsl_macro_" + _gen.toJavaIdentifier(_name) + "__" +
                    _gen.uniqueId());
      
      _gen.addMacro(_name, _macroName);
    }
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    String fun = null;
    
    if (_match != null) {
      fun = _gen.createTemplatePattern(_name, parseMatch(_match),
                                       _mode, _priority);

      out.println();
      out.print("// '" + _match.replace('\n', ' ') + "'");
      
      if (_mode != null) {
        _gen.addMode(_mode);
        out.println(" mode '" + _mode + "'");
      }
      else
        out.println();
      
      out.printJavaString("// " + getFilename() + ":" + getStartLine());
      out.println();
      
      out.println("private void " + fun +
                  "(XslWriter out, Node inputNode, Env env)");
      out.println("  throws Exception");
      out.println("{");
      out.pushDepth();

      out.println("Object _xsl_tmp;");
      out.println("Node node = inputNode;");
      out.println("int _xsl_top = env.getTop();");

      boolean isRawText = _gen.getDisableOutputEscaping();
      if (isRawText)
        out.println("boolean oldEscaping = out.disableEscaping(true);");
      else
        out.println("boolean oldEscaping = out.disableEscaping(false);");

      String filename = getBaseURI();
      if (filename != null) {
        int pos = _gen.addStylesheet(filename);
        
        out.println("env.setStylesheetEnv(stylesheets[" + pos + "]);");
      }

      _gen.setSelectDepth(0);
      _gen.clearUnique();

      generateChildren(out);
      /*
      if (node.getLocalName().equals("template") ||
          node.getLocalName().equals("xsl:template"))
        generateChildren(node);
      else
        generateChild((QAbstractNode) node);
      */

      /*
      if (! _isCacheable)
        println("out.setNotCacheable();");
      */

      out.println("out.disableEscaping(oldEscaping);");
      out.println("env.popToTop(_xsl_top);");
      out.popDepth();
      out.println("}");
      out.println();
    }

    if (_name != null) {
      out.println("void " + _macroName +
              "(XslWriter out, Node inputNode, Env env)");
      out.println("  throws Exception");
      out.println("{");
      out.pushDepth();

      if (_match == null) {
        out.println("Object _xsl_tmp;");
        out.println("Node node = inputNode;");
        generateChildren(out);
      }
      else {
        out.println(fun + "(out, inputNode, env);");
      }
      
      out.popDepth();
      out.println("}");
    }
  }
}
