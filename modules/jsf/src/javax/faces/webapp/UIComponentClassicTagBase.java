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
 * @author Scott Ferguson
 */

package javax.faces.webapp;

import java.io.*;

import java.util.*;
import java.util.logging.Logger;

import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.ServletRequest;

public abstract class UIComponentClassicTagBase
  extends UIComponentTagBase
  implements JspIdConsumer, BodyTag
{
  protected static final String UNIQUE_ID_PREFIX = "j_id_";
  
  protected static Logger log
    = Logger.getLogger(UIComponentClassicTagBase.class.getName());


  private String _id;
  private String _jspId;

  protected PageContext pageContext;
  protected BodyContent bodyContent;

  private FacesContext _facesContext;

  private UIComponentClassicTagBase _parentUIComponentTag;

  private Tag _parent;

  private UIComponent _component;
  private boolean _created;

  
  public UIComponentClassicTagBase()
  {
    _facesContext = FacesContext.getCurrentInstance();
  }

  protected String getFacetName()
  {
    return null;
  }

  public void setJspId(String id)
  {
    _jspId = id;
  }

  public String getJspId()
  {
    return _jspId;
  }

  public void setPageContext(PageContext pageContext)
  {
    this.pageContext = pageContext;
  }

  public Tag getParent()
  {
    return _parent;
  }

  public void setParent(Tag parent)
  {
    _parent = parent;
  }

  public void setBodyContent(BodyContent bodyContent)
  {
    this.bodyContent = bodyContent;
  }

  public BodyContent getBodyContent()
  {
    return this.bodyContent;
  }

  public JspWriter getPreviousOut()
  {
    if (bodyContent != null)
      return bodyContent.getEnclosingWriter();
    else
      return pageContext.getOut();
  }

  public void setId(String id)
  {
    if (id.startsWith(UNIQUE_ID_PREFIX))
      throw new IllegalArgumentException("id may not begin with " +
                                         UNIQUE_ID_PREFIX);

    _id = id;
  }

  protected String getFacesJspId(){
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  protected String getId()
  {
    return _id;
  }

  protected abstract boolean hasBinding();

  public UIComponent getComponentInstance()
  {
    return _component;
  }

  public boolean getCreated()
  {
    return _created;
  }

  public int doStartTag()
    throws JspException
  {
    PageContext pageContext = this.pageContext;

    this.bodyContent = null;
    _created = false;
    _component = null;

    _parentUIComponentTag
      = getParentUIComponentClassicTagBase(pageContext);

    ServletRequest request = pageContext.getRequest();

    Map tagMap = (Map) request.getAttribute("caucho.jsf.tag.map");

    if (tagMap == null) {
      tagMap = new HashMap();
      request.setAttribute("caucho.jsf.tag.map", tagMap);
    }

    Integer iterCounter = (Integer) tagMap.get(_jspId);

    iterCounter = (iterCounter == null
                   ? new Integer(0)
                   : new Integer(iterCounter.intValue() + 1));

    tagMap.put(_jspId, iterCounter);

    _component = findComponent(_facesContext);

    request.setAttribute("caucho.jsf.parent", this);

    return getDoStartValue();
  }

  /**
   * Returns the doStart value for the tag.  Defaults to EVAL_BODY_BUFFERED.
   */
  protected int getDoStartValue()
    throws JspException
  {
    return BodyTag.EVAL_BODY_BUFFERED;
  }

  public void doInitBody()
    throws JspException
  {
  }

  public int doAfterBody()
    throws JspException
  {
    UIComponent verbatim = createVerbatimComponentFromBodyContent();

    if (verbatim != null) {
      UIComponent component = getComponentInstance();

      if (component != null)
        component.getChildren().add(verbatim);
    }

    return getDoAfterBodyValue();
  }

  protected int getDoAfterBodyValue()
    throws JspException
  {
    return BodyTag.SKIP_PAGE;
  }

  public int doEndTag()
    throws JspException
  {
    pageContext.getRequest().setAttribute("caucho.jsf.parent",
                                          _parentUIComponentTag);

    return getDoEndValue();
  }

  protected int getDoEndValue()
    throws JspException
  {
    return Tag.EVAL_PAGE;
  }

  protected abstract UIComponent createComponent(FacesContext context,
                                                 String newId)
    throws JspException;

  protected abstract void setProperties(UIComponent component);

  protected UIComponent findComponent(FacesContext context)
    throws JspException
  {
    _created = false;

    if (_component != null)
      return _component;

    UIComponentClassicTagBase parentTag = _parentUIComponentTag;

    if (parentTag == null) {
      _component = context.getViewRoot();

      // XXX:
      if (_component.getChildCount() == 0){
        _created = true;

        setProperties(_component);
      }

      return _component;
    }

    UIComponent verbatim = parentTag.createVerbatimComponentFromBodyContent();

    UIComponent parent = parentTag.getComponentInstance();

    String id = getId();
    String facetName = null;

    if (id == null)
      id = UIViewRoot.UNIQUE_ID_PREFIX + getJspId();

    Map tagMap = (Map) pageContext.getRequest()
      .getAttribute("caucho.jsf.tag.map");

    Integer iterCounter = (Integer) tagMap.get(_jspId);

    if (iterCounter.intValue() > 0)
      id = id + "_" + iterCounter.intValue();

    if (_parent instanceof FacetTag) {
      facetName = ((FacetTag) _parent).getName();

      _component = parent.getFacet(facetName);

      if (_component != null)
        return _component;
    }
    else {
      _component = parent.findComponent(id);

      if (_component != null) {
        if (verbatim != null) {
          addVerbatimBeforeComponent(parentTag, verbatim, _component);
        }

        return _component;
      }

      if (verbatim != null) {
        parent.getChildren().add(verbatim);
      }
    }

    String componentType = getComponentType();

    if (hasBinding()) {
      // XXX: binding
    }

    _component = createComponent(context, id);

    _created = true;

    _component.setId(id);

    setProperties(_component);

    if (facetName != null)
      parent.getFacets().put(facetName, _component);
    else
      parent.getChildren().add(_component);

    return _component;
  }

  protected int getIndexOfNextChildTag()
  {
    throw new UnsupportedOperationException();
  }

  protected void addChild(UIComponent child)
  {
    getComponentInstance().getChildren().add(child);
  }

  protected void addFacet(String name)
  {
    throw new UnsupportedOperationException();
  }

  protected UIComponent createVerbatimComponentFromBodyContent()
  {
    BodyContent bodyContent = this.bodyContent;

    if (bodyContent == null)
      return null;

    String text = bodyContent.getString();
    bodyContent.clearBody();
    boolean isWhitespace = true;

    for (int i = text.length() - 1; i >= 0; i--) {
      char ch = text.charAt(i);

      if (!Character.isWhitespace(ch)) {
        // check for comment
        if (ch == '>' && text.indexOf("-->") + 2 == i) {
          int head = text.indexOf("<!--");

          if (head >= 0) {
            for (int j = 0; j < head; j++) {
              if (!Character.isWhitespace(text.charAt(j))) {
                isWhitespace = false;
                break;
              }
            }

            if (isWhitespace)
              return null;
          }
        }

        isWhitespace = false;
        break;
      }
    }

    if (isWhitespace)
      return null;

    UIOutput verbatim = createVerbatimComponent();

    verbatim.setValue(text);

    return verbatim;
  }

  protected UIOutput createVerbatimComponent()
  {
    Application app = _facesContext.getApplication();
    UIOutput output
      = (UIOutput) app.createComponent(HtmlOutputText.COMPONENT_TYPE);

    output.setId(_facesContext.getViewRoot().createUniqueId());
    output.setTransient(true);
    if (output instanceof HtmlOutputText)
      ((HtmlOutputText) output).setEscape(false);

    return output;
  }

  protected void addVerbatimBeforeComponent(UIComponentClassicTagBase parentTag,
                                            UIComponent verbatim,
                                            UIComponent component)
  {
    UIComponent parent = parentTag.getComponentInstance();

    int size = parent.getChildCount();

    if (size > 0) {
      List<UIComponent> children = parent.getChildren();

      for (int i = 0; i < size; i++) {
        if (children.get(i) == component)
          children.add(i, verbatim);
      }
    }
  }

  protected void addVerbatimAfterComponent(UIComponentClassicTagBase parentTag,
                                           UIComponent verbatim,
                                           UIComponent component)
  {
    UIComponent parent = parentTag.getComponentInstance();

    int size = parent.getChildCount();

    if (size > 0) {
      List<UIComponent> children = parent.getChildren();

      for (int i = 0; i < size; i++) {
        if (children.get(i) == component)
          children.add(i + 1, verbatim);
      }
    }
  }

  protected List<String> getCreatedComponents()
  {
    throw new UnsupportedOperationException();
  }

  protected FacesContext getFacesContext()
  {
    return _facesContext;
  }

  /**
   * @deprecated
   */
  protected void setupResponseWriter()
  {
  }

  /**
   * @deprecated
   */
  protected void encodeBegin()
    throws IOException
  {
    UIComponent component = getComponentInstance();
    FacesContext context = getFacesContext();

    if (component != null && context != null)
      component.encodeBegin(context);
  }

  /**
   * @deprecated
   */
  protected void encodeChildren()
    throws IOException
  {
    UIComponent component = getComponentInstance();
    FacesContext context = getFacesContext();

    if (component != null && context != null)
      component.encodeChildren(context);
  }

  /**
   * @deprecated
   */
  protected void encodeEnd()
    throws IOException
  {
    UIComponent component = getComponentInstance();
    FacesContext context = getFacesContext();

    if (component != null && context != null)
      component.encodeEnd(context);
  }

  public void release()
  {
  }

  public static UIComponentClassicTagBase
  getParentUIComponentClassicTagBase(PageContext pageContext)
  {
    return (UIComponentClassicTagBase)
      pageContext.getRequest().getAttribute("caucho.jsf.parent");
  }
}
