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

package com.caucho.jsf.context;

import com.caucho.util.L10N;

import java.util.*;
import java.util.logging.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.event.PhaseId;
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.component.*;
import javax.faces.render.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class ServletFacesContextImpl extends FacesContext
{
  private static final Logger log
    = Logger.getLogger(ServletFacesContextImpl.class.getName());

  private static final L10N L = new L10N(ServletFacesContextImpl.class);


  private static final Iterator<FacesMessage> NO_MESSAGES
    = new NoMessagesIterator();

  private static final Iterator<String> NO_IDS
    = new NoIdsIterator();

  private FacesContextFactoryImpl _factory;

  private ServletContext _webApp;
  private HttpServletRequest _request;
  private HttpServletResponse _response;

  private ExternalContext _externalContext;
  private FacesELContext _elContext;
  
  private boolean _isResponseComplete;  
  private boolean _isRenderResponse;
  
  private UIViewRoot _uiViewRoot;

  private Object []_messages;
  private final Object _messagesLock = new Object();
  private int _messageModCount = 0;

  private ResponseWriter _responseWriter;
  private ResponseStream _responseStream;

  private boolean _isClosed;

  private HashMap<Object, Object> _attributes;

  protected ServletFacesContextImpl(FacesContextFactoryImpl factory,
                                    ServletContext webApp,
                                    HttpServletRequest request,
                                    HttpServletResponse response)
  {
    _factory = factory;

    _webApp = webApp;
    _request = request;
    _response = response;

    setCurrentInstance(this);
  }
  
  public Application getApplication()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    return _factory.getApplication();
  }

  public ExternalContext getExternalContext()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    if (_externalContext == null) {
      _externalContext
        = new ServletExternalContext(_webApp, _request, _response);
    }

    return _externalContext;
  }

  public RenderKit getRenderKit()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    UIViewRoot viewRoot = getViewRoot();

    if (viewRoot == null)
      return null;

    String renderKitId = viewRoot.getRenderKitId();
    if (renderKitId == null)
      return null;

    RenderKitFactory factory = (RenderKitFactory)
      FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);

    return factory.getRenderKit(this, renderKitId);
  }

  public ResponseStream getResponseStream()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    return _responseStream;
  }

  public void setResponseStream(ResponseStream responseStream)
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
     _responseStream = responseStream;
  }

  public ResponseWriter getResponseWriter()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");

    return _responseWriter;
  }

  public void setResponseWriter(ResponseWriter writer)
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");

    if (writer == null)
      throw new NullPointerException(L.l("ResponseWriter object can not be null"));

    _responseWriter = writer;
  }

  /**
   * Returns the root of the UI component tree.
   */
  public UIViewRoot getViewRoot()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");

    return _uiViewRoot;
    /*
    if (_uiViewRoot == null) {
      _uiViewRoot = getApplication().getViewHandler().createView(this,
                                                                 null);
    }
    
    return _uiViewRoot;
    */
  }

  /**
   * Sets the root of the UI component tree.
   */
  public void setViewRoot(UIViewRoot root)
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    if (root == null)
      throw new NullPointerException();
    
    _uiViewRoot = root;
  }

  /**
   * If true the facelet will skip to the render phase.
   */
  @Override
  public boolean getRenderResponse()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    return _isRenderResponse;
  }

  /**
   * Ask the lifecycle to skip to the render phase.
   */
  @Override
  public void renderResponse()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");

    _isRenderResponse = true;
  }

  /**
   * Return true if the lifecycle should skip the response phase.
   */
  @Override
  public boolean getResponseComplete()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    return _isResponseComplete;
  }

  /**
   * Ask the lifecycle to skip the response phase.
   */
  @Override
  public void responseComplete()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    _isResponseComplete = true;
  }

  public void addMessage(String clientId,
                         FacesMessage message)
  {
    if (_isClosed)
      throw new IllegalStateException("FacesContext is closed");
    
    if (message == null)
      throw new NullPointerException();

    if (log.isLoggable(Level.FINE))
      log.fine("FacesContext.addMessage " + clientId + " " + message);

    synchronized (_messagesLock) {
      if (_messages == null) {
        _messages = new Object[]{clientId, message};
      }
      else {
        Object []newMessages = new Object[_messages.length + 2];

        System.arraycopy(_messages, 0, newMessages, 0, _messages.length);

        newMessages[newMessages.length - 2] = clientId;
        newMessages[newMessages.length - 1] = message;

        _messages = newMessages;
      }

      _messageModCount++;
    }
  }

  public Iterator<String> getClientIdsWithMessages()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");

    synchronized (_messagesLock) {
      if (_messages == null)
        return NO_IDS;

      LinkedHashSet<String> ids = new LinkedHashSet<String>();

      for (int i = 0; i < _messages.length / 2; i++) {
        Object id = _messages[i * 2];

        ids.add((String) id);
      }

      return ids.iterator();
    }
  }

  public FacesMessage.Severity getMaximumSeverity()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");


    synchronized (_messagesLock) {
      if (_messages == null)
        return null;

      FacesMessage.Severity result = null;

      for (int i = 0; i < _messages.length / 2; i++) {
        FacesMessage msg = (FacesMessage) _messages[i * 2 + 1];

        if (result == null || msg.getSeverity().compareTo(result) > 0)
          result = msg.getSeverity();
      }

      return result;
    }
  }

  public Iterator<FacesMessage> getMessages()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");

    synchronized (_messagesLock) {
      if (_messages == null)
        return NO_MESSAGES;

      return new Iterator<FacesMessage>() {
        private int _cursor = 1;
        private int _expectedModCount = _messageModCount;

        public boolean hasNext()
        {
          synchronized (_messagesLock) {
            return (_cursor < _messages.length);
          }
        }

        public FacesMessage next()
        {
          synchronized (_messagesLock) {
            if (_expectedModCount != _messageModCount)
              throw new ConcurrentModificationException();

            int idx = _cursor;
            _cursor = _cursor + 2;

            try {
              return (FacesMessage) _messages[idx];
            }
            catch (ArrayIndexOutOfBoundsException e) {
              throw new NoSuchElementException();
            }
          }
        }

        public void remove()
        {
          synchronized (_messagesLock) {
            if (_expectedModCount != _messageModCount)
              throw new ConcurrentModificationException();

            Object []newMessages = new Object[_messages.length - 2];
            System.arraycopy(_messages, 0, newMessages, 0, _cursor - 3);

            System.arraycopy(_messages,
                             _cursor - 1,
                             newMessages,
                             _cursor - 3,
                             _messages.length - _cursor + 1);

            _messages = newMessages;

            _cursor = _cursor - 2;
            
            _expectedModCount++;
            _messageModCount = _expectedModCount;
          }
        }
      };
    }
  }

  public Iterator<FacesMessage> getMessages(final String clientId)
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");

    synchronized (_messagesLock) {
      if (_messages == null)
        return NO_MESSAGES;

      return new Iterator<FacesMessage>() {
        private int _expectedModCount = _messageModCount;
        private int _cursor = 0;

        public boolean hasNext()
        {
          for (int i = _cursor; i < _messages.length / 2; i++) {
            int idx = i * 2;

            if ((clientId == null && _messages[idx] == null) ||
                (clientId != null && clientId.equals(_messages[idx])))
              return true;

          }

          return false;
        }

        public FacesMessage next()
        {
          synchronized (_messagesLock) {
            if (_expectedModCount != _messageModCount)
              throw new ConcurrentModificationException();

            for (int i = _cursor; i < _messages.length / 2; i++) {
              int idx = i * 2;

              if ((clientId == null && _messages[idx] == null) ||
                  (clientId != null && clientId.equals(_messages[idx]))) {
                _cursor = i + 1;

                FacesMessage result = (FacesMessage) _messages[idx + 1];

                return result;
              }
            }

            throw new NoSuchElementException();
          }
        }

        public void remove()
        {
          synchronized (_messagesLock) {
            if (_expectedModCount != _messageModCount)
              throw new ConcurrentModificationException();

            Object []newMessages = new Object[_messages.length - 2];
            System.arraycopy(_messages, 0, newMessages, 0, (_cursor - 1) * 2);

            System.arraycopy(_messages,
                             _cursor * 2,
                             newMessages,
                             (_cursor - 1) * 2,
                             _messages.length - _cursor * 2);

            _messages = newMessages;

            _cursor = _cursor - 1;

            _expectedModCount++;
            _messageModCount = _expectedModCount;
          }
        }
      };
    }
  }

  /**
   * @Since 1.2
   */
  @Override
  public ELContext getELContext()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    if (_elContext == null) {
      _elContext = new FacesELContext(this, getApplication().getELResolver());
      _elContext.putContext(FacesContext.class, this);
    }

    return _elContext;
  }

  public void release()
  {
    _isClosed = true;
    
    if (_attributes != null) {
      _attributes.clear();
      _attributes = null;
    }

    _messages = null;

    FacesContext.setCurrentInstance(null);
  }

  public Map<Object, Object> getAttributes()
  {
    if (_isClosed)
      throw new IllegalStateException();

    if (_attributes == null)
      _attributes = new HashMap<Object, Object>();

    return _attributes;
  }

  public String toString()
  {
    return "ServletFacesContextImpl[]";
  }

  private static class NoIdsIterator
    implements Iterator<String> {
    public boolean hasNext()
    {
      return false;
    }

    public String next()
    {
      throw new NoSuchElementException();
    }

    public void remove()
    {
      throw new UnsupportedOperationException("unimplemented");
    }
  }

  private static class NoMessagesIterator
    implements Iterator<FacesMessage> {
    public boolean hasNext()
    {
      return false;
    }

    public FacesMessage next()
    {
      throw new NoSuchElementException();
    }

    public void remove()
    {
      throw new UnsupportedOperationException("unimplemented");
    }
  }
}
