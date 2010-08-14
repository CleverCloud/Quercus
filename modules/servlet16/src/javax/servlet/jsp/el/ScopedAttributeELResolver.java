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

package javax.servlet.jsp.el;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;
import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Variable resolution for JSP variables
 */
public class ScopedAttributeELResolver extends ELResolver {
  @Override
  public Class<String> getCommonPropertyType(ELContext context,
                                             Object base)
  {
    if (base == null)
      return String.class;
    else
      return null;
  }

  @Override
  public Iterator<FeatureDescriptor>
    getFeatureDescriptors(ELContext context, Object base)
  {
    if (base != null)
      return null;

    PageContext pageContext
      = (PageContext) context.getContext(JspContext.class);

    context.setPropertyResolved(true);

    ArrayList<FeatureDescriptor> keys = new ArrayList<FeatureDescriptor>();

    Enumeration e = pageContext.getAttributeNamesInScope(PageContext.PAGE_SCOPE);
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      String name = (String) key;

      FeatureDescriptor desc = new FeatureDescriptor();
      desc.setName(name);
      desc.setDisplayName(name);
      desc.setShortDescription("");
      desc.setExpert(false);
      desc.setHidden(false);
      desc.setPreferred(true);

      if (key == null)
        desc.setValue(ELResolver.TYPE, null);
      else
        desc.setValue(ELResolver.TYPE, key.getClass());

      desc.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);

      keys.add(desc);
    }

    return keys.iterator();
  }

  @Override
  public Class<Object> getType(ELContext context,
                          Object base,
                          Object property)
  {
    if (base != null)
      return null;

    context.setPropertyResolved(true);
    return Object.class;
  }

  @Override
    public Object getValue(ELContext context,
                         Object base,
                         Object property)
  {
    if (base != null)
      return null;

    context.setPropertyResolved(true);
    PageContext pageContext
      = (PageContext) context.getContext(JspContext.class);

    return pageContext.getAttribute(String.valueOf(property));
  }

  @Override
    public boolean isReadOnly(ELContext context,
                         Object base,
                         Object property)
  {
    if (base != null)
      return true;

    context.setPropertyResolved(true);
    
    return false;
  }

  @Override
    public void setValue(ELContext context,
                         Object base,
                         Object property,
                         Object value)
  {
    if (base != null)
      return;

    context.setPropertyResolved(true);

    context.setPropertyResolved(true);
    PageContext pageContext
      = (PageContext) context.getContext(JspContext.class);

    pageContext.setAttribute(String.valueOf(property), value);
  }
}
