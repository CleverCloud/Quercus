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

package com.caucho.xmpp;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.vfs.ReadStream;
import com.caucho.xmpp.im.ImMessage;
import com.caucho.xmpp.im.ImPresence;
import com.caucho.xmpp.im.ImSessionQuery;
import com.caucho.xmpp.im.Text;
import com.caucho.xmpp.im.XmlData;

/**
 * Protocol handler from the TCP/XMPP stream forwarding to the broker
 */
public class XmppReader
{
  private static final Logger log
    = Logger.getLogger(XmppReader.class.getName());

  private ActorStream _toReply;
  private ActorStream _handler;

  private ReadStream _is;
  private XmppStreamReader _in;

  private XmppContext _xmppContext;
  private XmppMarshalFactory _marshalFactory;

  private String _uid;
  private String _jid;

  private boolean _isFinest;

  XmppReader(XmppContext context,
             ReadStream is,
             XmppStreamReader in,
             ActorStream toReply,
             ActorStream handler)
  {
    _xmppContext = context;
    _marshalFactory = context.getMarshalFactory();
    
    _is = is;
    _in = in;

    _toReply = toReply;
    
    _handler = handler;

    _isFinest = log.isLoggable(Level.FINEST);
  }

  void setHandler(ActorStream handler)
  {
    _handler = handler;
  }

  void setUid(String uid)
  {
    _uid = uid;
  }

  void setJid(String jid)
  {
    _jid = jid;
  }
  
  boolean readNext()
    throws IOException
  {
    XmppStreamReader in = _in;
    
    if (in == null)
      return false;

    try {
      int tag;

      while ((tag = _in.next()) > 0) {
        if (_isFinest)
          debug(_in);

        if (tag == XMLStreamConstants.END_ELEMENT) {
          if ("stream".equals(_in.getLocalName())) {
            if (log.isLoggable(Level.FINE))
              log.fine(this + " end-stream");
          }
          else {
            log.warning(this + " " + _in.getLocalName());
          }

          return false;
        }

        if (tag == XMLStreamConstants.START_ELEMENT) {
          boolean valid = false;

          if ("iq".equals(_in.getLocalName()))
            valid = handleIq();
          else if ("presence".equals(_in.getLocalName()))
            valid = handlePresence();
          else if ("message".equals(_in.getLocalName()))
            valid = handleMessage();
          else {
            if (log.isLoggable(Level.FINE))
              log.fine(this + " " + _in.getLocalName() + " is an unknown tag");

            return false;
          }

          if (! valid)
            return false;

          if (_in.available() < 1)
            return true;
        }
      }

      if (_isFinest)
        log.finest(this + " end of stream");

      return false;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return false;
    }
  }

  /**
   * Processes a message
   *
   * <code><pre>
   * element message{xmlns="jabber:client"} {
   *   attribute from?
   *   &amp; attribute to?
   *   &amp; attribute id?
   *   &amp; attribute type?
   *
   *   &amp; subject*
   *   &amp; body*
   *   &amp; thread?
   *   &amp; other*
   * }
   *
   * element body {
   *   attribute xml:lang?
   *   &amp; string
   * }
   *
   * element subject {
   *   attribute xml:lang?
   *   &amp; string
   * } 
   *
   * element thread {
   *   &amp; string
   * }
   * </pre></code>
   */
  boolean handleMessage()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");

    if (type == null)
      type = "normal";

    int tag;

    ArrayList<Text> subjectList = null;
    ArrayList<Text> bodyList = null;
    ArrayList<Serializable> extraList = null;
    String thread = null;
    
    while ((tag = _in.next()) > 0
           && ! (tag == XMLStreamReader.END_ELEMENT
                 && "message".equals(_in.getLocalName()))) {
      if (_isFinest)
        debug(_in);
      
      if (tag != XMLStreamReader.START_ELEMENT)
        continue;

      if ("body".equals(_in.getLocalName())
          && "jabber:client".equals(_in.getNamespaceURI())) {
        String lang = null;

        if (_in.getAttributeCount() > 0
            && "lang".equals(_in.getAttributeLocalName(0))) {
          lang = _in.getAttributeValue(0);
        }

        tag = _in.next();
        if (_isFinest)
          debug(_in);

        String body = _in.getText();

        if (bodyList == null)
          bodyList = new ArrayList<Text>();

        bodyList.add(new Text(body, lang));

        expectEnd("body");
      }
      else if ("subject".equals(_in.getLocalName())
               && "jabber:client".equals(_in.getNamespaceURI())) {
        String lang = null;

        if (_in.getAttributeCount() > 0
            && "lang".equals(_in.getAttributeLocalName(0)))
          lang = _in.getAttributeValue(0);

        tag = _in.next();
        if (_isFinest)
          debug(_in);

        String text = _in.getText();

        if (subjectList == null)
          subjectList = new ArrayList<Text>();

        subjectList.add(new Text(text, lang));

        expectEnd("subject");
      }
      else if ("thread".equals(_in.getLocalName())
               && "jabber:client".equals(_in.getNamespaceURI())) {
        tag = _in.next();
        if (_isFinest)
          debug(_in);

        thread = _in.getText();

        expectEnd("thread");
      }
      else {
        String name = _in.getLocalName();
        QName qName = _in.getName();
        String uri = _in.getNamespaceURI();

        if (extraList == null)
          extraList = new ArrayList<Serializable>();

        XmppMarshal marshal = _marshalFactory.getUnserialize(qName);

        Serializable extra;

        if (marshal != null)
          extra = marshal.fromXml(_in);
        else
          extra = readAsXmlString(_in);

        // extraList.add(new XmlData(name, uri, data));
        extraList.add(extra);
      }
    }

    expectEnd("message", tag);

    Text []subjectArray = null;

    if (subjectList != null) {
      subjectArray = new Text[subjectList.size()];
      subjectList.toArray(subjectArray);
    }

    Text []bodyArray = null;

    if (bodyList != null) {
      bodyArray = new Text[bodyList.size()];
      bodyList.toArray(bodyArray);
    }

    Serializable []extraArray = null;
    
    if (extraList != null) {
      extraArray = new Serializable[extraList.size()];
      extraList.toArray(extraArray);
    }

    if (_jid != null)
      from = _jid;

    if (to == null)
      to = _uid;

    Serializable message;

    /*
    if (! "normal".equals(type)
        || subjectArray != null 
        || bodyArray != null 
        || thread != null
        || extraArray == null
        || extraArray.length > 1) {
    }
    else {
      message = extraArray[0];
    }
    */

    message = new ImMessage(to, from, type,
                            subjectArray, bodyArray, thread,
                            extraArray);

    if (_handler != null)
      _handler.message(to, from, message);
    
    return true;
  }

  /**
   * Processes a query
   */
  boolean handleIq()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");

    int tag = _in.nextTag();

    if (_isFinest)
      debug(_in);

    String localName = _in.getLocalName();
    String uri = _in.getNamespaceURI();
    
    QName name = _in.getName();

    Serializable query = null;

    XmppMarshal marshal = _marshalFactory.getUnserialize(name);

    if (marshal != null)
      query = marshal.fromXml(_in);
    else
      query = readAsXmlString(_in);

    ActorError error = null;
      
    skipToEnd("iq");

    if (_jid != null)
      from = _jid;

    if (to == null) {
      to = _uid;

      if (query instanceof ImSessionQuery && "set".equals(type)) {
        long bamId = _xmppContext.addId(id);

        _toReply.queryResult(bamId, from, to, query);

        return true;
      }
    }

    if ("get".equals(type)) {
      long bamId = _xmppContext.addId(id);

      if (_handler != null)
        _handler.queryGet(bamId, to, from, query);
    }
    else if ("set".equals(type)) {
      long bamId = _xmppContext.addId(id);

      if (_handler != null)
        _handler.querySet(bamId, to, from, query);
    }
    else if ("result".equals(type)) {
      long bamId = Long.parseLong(id);

      if (_handler != null)
        _handler.queryResult(bamId, to, from, query);
    }
    else if ("error".equals(type)) {
      long bamId = Long.parseLong(id);

      if (_handler != null)
        _handler.queryError(bamId, to, from, query, error);
    }
    else {
      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " <" + _in.getLocalName() + " xmlns="
                 + _in.getNamespaceURI() + "> unknown type");
      }
    }

    return true;
  }

  boolean handlePresence()
    throws IOException, XMLStreamException
  {
    String type = _in.getAttributeValue(null, "type");
    String id = _in.getAttributeValue(null, "id");
    String from = _in.getAttributeValue(null, "from");
    String to = _in.getAttributeValue(null, "to");
    String target = to;

    if (type == null)
      type = "";

    int tag;

    String show = null;
    Text status = null;
    int priority = 0;
    ArrayList<Serializable> extraList = new ArrayList<Serializable>();
    ActorError error = null;

    while ((tag = _in.nextTag()) > 0
           && ! ("presence".equals(_in.getLocalName())
                 && tag == XMLStreamReader.END_ELEMENT)) {
      if (_isFinest)
        debug(_in);
      
      if (tag != XMLStreamReader.START_ELEMENT)
        continue;

      if ("status".equals(_in.getLocalName())) {
        tag = _in.next();
    
        if (_isFinest)
          debug(_in);

        status = new Text(_in.getText());

        skipToEnd("status");
      }
      else if ("show".equals(_in.getLocalName())) {
        tag = _in.next();
    
        if (_isFinest)
          debug(_in);

        show = _in.getText();

        skipToEnd("show");
      }
      else if ("priority".equals(_in.getLocalName())) {
        tag = _in.next();
    
        if (_isFinest)
          debug(_in);

        priority = Integer.parseInt(_in.getText());

        skipToEnd("priority");
      }
      else {
        String name = _in.getLocalName();
        String uri = _in.getNamespaceURI();
        String data = _in.readAsXmlString();

        extraList.add(new XmlData(name, uri, data));
      }
    }

    if (_isFinest)
      debug(_in);

    expectEnd("presence", tag);

    if (_jid != null)
      from = _jid;

    if (target == null)
      target = _uid;

    // XXX: need different types
    ImPresence presence = new ImPresence(to, from,
                                         show, status, priority,
                                         extraList);

    if (_handler != null) {
      /*
      if ("".equals(type) || "presence".equals(type))
        _handler.presence(target, from, presence);
      else if ("probe".equals(type))
        _handler.presenceProbe(target, from, presence);
      else if ("unavailable".equals(type))
        _handler.presenceUnavailable(target, from, presence);
      else if ("subscribe".equals(type))
        _handler.presenceSubscribe(target, from, presence);
      else if ("subscribed".equals(type))
        _handler.presenceSubscribed(target, from, presence);
      else if ("unsubscribe".equals(type))
        _handler.presenceUnsubscribe(target, from, presence);
      else if ("unsubscribed".equals(type))
        _handler.presenceUnsubscribed(target, from, presence);
      else if ("error".equals(type))
        _handler.presenceError(target, from, presence, error);
      else
        log.warning(this + " " + type + " is an unknown presence type");
        */
    }

    return true;
  }

  protected void skipToEnd(String tagName)
    throws IOException, XMLStreamException
  {
    XMLStreamReader in = _in;
      
    if (in == null)
      return;

    int tag;
    while ((tag = in.next()) > 0) {
      if (_isFinest)
        debug(in);

      if (tag == XMLStreamReader.START_ELEMENT) {
      }
      else if (tag == XMLStreamReader.END_ELEMENT) {
        if (tagName.equals(in.getLocalName()))
          return;
      }
    }
  }

  private void expectEnd(String tagName)
    throws IOException, XMLStreamException
  {
    expectEnd(tagName, _in.nextTag());
  }

  private void expectEnd(String tagName, int tag)
    throws IOException, XMLStreamException
  {
    if (tag != XMLStreamReader.END_ELEMENT)
      throw new IllegalStateException("expected </" + tagName + "> at <" + _in.getLocalName() + ">");

    else if (! tagName.equals(_in.getLocalName()))
      throw new IllegalStateException("expected </" + tagName + "> at </" + _in.getLocalName() + ">");
  }

  private String readAsXmlString(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    StringBuilder sb = new StringBuilder();
    int depth = 0;

    while (true) {
      if (XMLStreamReader.START_ELEMENT == in.getEventType()) {
        depth++;

        String prefix = in.getPrefix();

        sb.append("<");

        if (! "".equals(prefix)) {
          sb.append(prefix);
          sb.append(":");
        }

        sb.append(in.getLocalName());

        if (in.getNamespaceURI() != null) {
          if ("".equals(prefix))
            sb.append(" xmlns");
          else
            sb.append(" xmlns:").append(prefix);

          sb.append("=\"");
          sb.append(in.getNamespaceURI()).append("\"");
        }

        for (int i = 0; i < in.getAttributeCount(); i++) {
          sb.append(" ");
          sb.append(in.getAttributeLocalName(i));
          sb.append("=\"");
          sb.append(in.getAttributeValue(i));
          sb.append("\"");
        }
        sb.append(">");

        log.finest(this + " " + sb);
      }
      else if (XMLStreamReader.END_ELEMENT == in.getEventType()) {
        depth--;

        sb.append("</");

        String prefix = in.getPrefix();
        if (! "".equals(prefix))
          sb.append(prefix).append(":");

        sb.append(in.getLocalName());
        sb.append(">");

        if (depth == 0)
          return sb.toString();
      }
      else if (XMLStreamReader.CHARACTERS == in.getEventType()) {
        sb.append(in.getText());
      }
      else {
        log.finer(this + " tag=" + in.getEventType());

        return sb.toString();
      }

      if (in.next() < 0) {
        log.finer(this + " unexpected end of file");

        return sb.toString();
      }
    }
  }

  private void debug(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    if (XMLStreamReader.START_ELEMENT == in.getEventType()) {
      StringBuilder sb = new StringBuilder();
      sb.append("<").append(in.getLocalName());

      if (in.getNamespaceURI() != null)
        sb.append("{").append(in.getNamespaceURI()).append("}");

      for (int i = 0; i < in.getAttributeCount(); i++) {
        sb.append(" ");
        sb.append(in.getAttributeLocalName(i));
        sb.append("='");
        sb.append(in.getAttributeValue(i));
        sb.append("'");
      }
      sb.append(">");

      log.finest(this + " " + sb);
    }
    else if (XMLStreamReader.END_ELEMENT == in.getEventType()) {
      log.finest(this + " </" + in.getLocalName() + ">");
    }
    else if (XMLStreamReader.CHARACTERS == in.getEventType()) {
      String text = in.getText().trim();

      if (! "".equals(text))
        log.finest(this + " text='" + text + "'");
    }
    else
      log.finest(this + " tag=" + in.getEventType());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
