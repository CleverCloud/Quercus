/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

package com.caucho.jsp.java;

import com.caucho.vfs.WriteStream;
import com.caucho.xml.QName;
import com.caucho.jsp.JspParseException;

import javax.servlet.jsp.JspException;
import javax.faces.event.PhaseListener;
import java.io.IOException;

public class JsfPhaseListener
  extends JsfNode
{
  private static final QName TYPE = new QName("type");
  private static final QName BINDING = new QName("binding");

  private String _type;
  private String _binding;

  public String getType()
  {
    return _type;
  }

  public String getBinding()
  {
    return _binding;
  }

  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws JspParseException
  {
    if (TYPE.equals(name))
      _type = value;
    else if (BINDING.equals(name)) {
      _binding = value;
    }
    else
      super.addAttribute(name, value);
  }

  public void printXml(WriteStream os)
    throws IOException
  {
    //
  }

  public void generate(JspJavaWriter out)
    throws Exception
  {
    JspNode parent = getParent();

    if (!(parent instanceof JsfViewRoot))
      throw new JspException(L.l(
        "f:phaseListener must be nested inside a f:view tag."));

    JsfViewRoot viewRoot = (JsfViewRoot) parent;


    out.println("if (" + viewRoot.getJsfVar() + ".getChildCount() == 0) {");

    if (_binding != null) {
      String exprVar = "_caucho_value_expr_" +
                       _gen.addValueExpr(_binding,
                                         PhaseListener.class.getName());

      String varName = "_jsp_phase_listener" + _gen.uniqueId();

      out.println("javax.faces.event.PhaseListener " +
                  varName + " = (javax.faces.event.PhaseListener)" + exprVar +
                  ".getValue(_jsp_faces_context.getELContext());");

      if (_type != null) {
        out.println("if (" + varName + " == null) {");

        out.println(varName +
                    " = (javax.faces.event.PhaseListener)" +
                    _type +
                    ".class.newInstance();");

        out.println(exprVar +
                    ".setValue(_jsp_faces_context.getELContext(), " +
                    varName +
                    ");");

        out.println("}");
      }

      out.println("" + viewRoot.getJsfVar() + ".addPhaseListener(" +
                  varName + ");");
    }
    else if (_type != null) {

      out.println("\t" + viewRoot.getJsfVar() +
                  ".addPhaseListener((javax.faces.event.PhaseListener)" +
                  _type +
                  ".class.newInstance());");
    }
    else {
    }
    out.println("}");
  }
}
