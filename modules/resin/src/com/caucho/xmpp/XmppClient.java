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
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.caucho.bam.AbstractActorStream;
import com.caucho.bam.ActorStream;
import com.caucho.env.thread.ThreadPool;
import com.caucho.util.Base64;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SocketStream;
import com.caucho.vfs.WriteStream;
import com.caucho.xmpp.im.ImBindQuery;

/**
 * XMPP protocol
 */
public class XmppClient {
  private static final L10N L = new L10N(XmppClient.class);
  private static final Logger log
    = Logger.getLogger(XmppClient.class.getName());

  private static final String STREAMS_NS = "http://etherx.jabber.org/streams";

  private static final String AUTH
    = "auth{http://jabber.org/features/iq-auth}";
  private static final String REGISTER
    = "register{http://jabber.org/features/iq-register}";

  private InetAddress _address;
  private int _port;
  
  private String _to;

  private Socket _s;
  private ReadStream _is;
  private WriteStream _os;

  private String _id;
  private String _from;
  
  private XmppStreamReader _in;
  private XmppReader _reader;
  
  private boolean _isFinest;

  private int _mId;

  private HashSet<String> _authMechanisms = new HashSet<String>();
  private HashSet<String> _features = new HashSet<String>();

  private BlockingQueue<Stanza> _stanzaQueue
    = new LinkedBlockingQueue<Stanza>();

  private XmppContext _xmppContext = new XmppContext();
  private XmppClientBrokerStream _toBroker;
  private BindCallback _bindCallback;

  private String _jid;
  
  private ActorStream _callback;

  public XmppClient(InetAddress address, int port)
  {
    _address = address;
    _port = port;

    _to = _address.getHostAddress();

    _isFinest = log.isLoggable(Level.FINEST);
  }

  public XmppClient(String address, int port)
  {
    this(getByName(address), port);
    
    _to = address;
  }

  private static InetAddress getByName(String address)
  {
    try {
      return InetAddress.getByName(address);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void connect(String name, String password)
    throws IOException
  {
    connect();

    login(name, password);
  }

  public void connect()
    throws IOException
  {
    try {
      if (_s != null)
        throw new IllegalStateException(L.l("{0} is already connected", this));

      _s = new Socket(_address, _port);

      SocketStream ss = new SocketStream(_s);
    
      _os = new WriteStream(ss);
      _is = new ReadStream(ss);

      _os.print("<?xml version='1.0' encoding='UTF-8' ?>\n");
      _os.setEncoding("utf-8");
      
      startStream();

      _os.flush();
      
      XMLInputFactory factory = XMLInputFactory.newInstance();

      XmppMarshalFactory marshalFactory = new XmppMarshalFactory();
      
      XmppStreamWriterImpl out;
      out = new XmppStreamWriterImpl(_os, marshalFactory);

      XmppWriterImpl writer = new XmppWriterImpl(_xmppContext, out);
      
      _toBroker = new XmppClientBrokerStream(this, writer);
    
      _in = new XmppStreamReaderImpl(_is, marshalFactory);

      _bindCallback = new BindCallback();
      _reader = new XmppReader(_xmppContext, _is, _in, _toBroker,
                               _bindCallback);

      String tag = readStartTag();

      if (! tag.equals("stream")
          || ! STREAMS_NS.equals(_in.getNamespaceURI())) {
        throw new IOExceptionWrapper(L.l("<{0}> with ns={1} is an unexpected server response",
                                  tag, _in.getNamespaceURI()));
      }

      readStreamFeatures();
    } catch (XMLStreamException e) {
      throw new IOExceptionWrapper(e);
    }
  }
  
  public void login(String name, String password)
    throws IOException
  {
    String base64 = Base64.encode("" + (char) 0 + name + (char) 0 + password);

    if (log.isLoggable(Level.FINER))
      log.finer(this + " authenticating " + name);

    _os.print("<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>");
    _os.print(base64);
    _os.print("</auth>");

    startStream();
    _os.flush();

    try {
      if (! readSuccess())
        throw new RuntimeException("expected success");
      
      if (! readStream())
        throw new RuntimeException("expected stream");

      StringBuilder sb = new StringBuilder();
      Base64.encode(sb, RandomUtil.getRandomLong());
      
      _os.print("<iq type='set' id='" + _mId++ + "'>");
      _os.print("<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>");
      _os.print("<resource>" + getClass().getSimpleName() + "</resource>");
      _os.print("</bind>");
      _os.print("</iq>");
      _os.flush();

      _reader.readNext();

      if (_jid == null)
        throw new RuntimeException("expected bind");
      
      _os.print("<iq type='set' id='" + _mId++ + "'>");
      _os.print("<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>");
      _os.print("</iq>");
      _os.flush();

      _reader.readNext();

      /*
      Stanza stanza = _stanzaQueue.poll(2, TimeUnit.SECONDS);

      if (! (stanza instanceof SessionStanza)
          && ! (stanza instanceof EmptyStanza))
        throw new RuntimeException("expected session");
      */

      if (log.isLoggable(Level.FINER))
        log.finer(this + " authentication successful for " + name);

      _reader.setHandler(_callback);

      ThreadPool.getThreadPool().start(new Listener());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void setCallback(ActorStream callback)
  {
    _callback = callback;

    if (_reader != null && _jid != null)
      _reader.setHandler(callback);
  }
  
  public void send(String type, String to, String body)
    throws IOException
  {
    send(type, to, body, null);
  }
  
  public void send(String type, String to, String body, String subject)
    throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " send to=" + to + " body=" + body);

    try {
      _os.print("<message ");
      _os.print(" type='" + type + "'");
      
      if (to != null)
        _os.print(" to='" + to + "'");
      if (_from != null)
        _os.print(" from='" + _from + "'");
      _os.print(">");
      
      if (subject != null)
        _os.print("<subject>" + subject + "</subject>");
      if (body != null)
        _os.print("<body>" + body + "</body>");
      _os.print("</message>");
      _os.flush();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void startStream()
    throws IOException
  {
    _os.print("<stream:stream");
    _os.print(" to='" + _to + "'");
    _os.print(" xmlns='jabber:client'");
    _os.print(" xmlns:stream='http://etherx.jabber.org/streams'");
    _os.print(" version='1.0'");
    _os.print(">");
    _os.flush();

    if (log.isLoggable(Level.FINER))
      log.finer(this + " <stream:stream xmlns='jabber:client' to='" + _to + "'>");
  }

  private void readStreamFeatures()
    throws IOException, XMLStreamException
  {
    String startTag = readStartTag();

    if (! "features".equals(startTag))
      throw unexpected();

    int tag = 0;
    
    while ((tag = _in.next()) > 0
           && ! (tag == XMLStreamReader.END_ELEMENT
                 && "features".equals(_in.getLocalName()))) {
      if (_isFinest)
        debug(_in);
      
      if (tag == XMLStreamReader.START_ELEMENT) {
        String localName = _in.getLocalName();

        if ("mechanisms".equals(localName)) {
        }
        else if ("mechanism".equals(localName)) {
          tag = _in.next();

          String mechanism = _in.getText();

          _authMechanisms.add(mechanism);
        }
        else {
          String feature = localName + "{" + _in.getNamespaceURI() + "}";

          if (log.isLoggable(Level.FINER))
            log.finer(this + " feature " + feature);

          _features.add(feature);
        }
      }
      else if (tag == XMLStreamReader.END_ELEMENT) {
        String localName = _in.getLocalName();
      }
    }
  }

  private String readStartTag()
    throws IOException, XMLStreamException
  {
    int tag = 0;
    
    while ((tag = _in.next()) > 0 && tag != XMLStreamReader.START_ELEMENT) {
      if (_isFinest)
        debug(_in);
    }
    
    if (_isFinest)
      debug(_in);

    return _in.getLocalName();    
  }

  private IOException unexpected()
    throws IOException, XMLStreamException
  {
    if ("error".equals(_in.getLocalName())) {
      int tag;
      
      while ((tag = _in.next()) > 0
             && ! (tag == XMLStreamReader.END_ELEMENT
                   && "error".equals(_in.getLocalName()))) {
        if (tag == XMLStreamReader.START_ELEMENT) {
          System.out.println("<" + _in.getLocalName() + ">");
        }
        else if (tag == XMLStreamReader.END_ELEMENT) {
          System.out.println("</" + _in.getLocalName() + ">");
        }
      }
      
      return new IOException(L.l("<error> is unexpected", _in.getLocalName()));
    }
    else
      return new IOException(L.l("<{0}> is unexpected", _in.getLocalName()));
  }

  public boolean isClosed()
  {
    return _s == null;
  }

  public void close()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close");
    
    try {
      Socket s;
      ReadStream is;
      WriteStream os;
      
      synchronized (this) {
        s = _s;
        _s = null;

        is = _is;
        _is = null;

        os = _os;
        _os = null;
      }

      if (os != null) {
        try { os.close(); } catch (IOException e) {}
      }

      if (is != null) {
        is.close();
      }

      if (s != null) {
        s.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  private void debug(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    if (XMLStreamReader.START_ELEMENT == in.getEventType()) {
      StringBuilder sb = new StringBuilder();
      sb.append("<");
      if (in.getPrefix() != null && ! "".equals(in.getPrefix()))
        sb.append(in.getPrefix()).append(":");
      sb.append(in.getLocalName());

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
    else if (XMLStreamReader.CHARACTERS == in.getEventType())
      log.finest(this + " text='" + in.getText() + "'");
    else
      log.finest(this + " tag=" + in.getEventType());
  }

  private boolean readStream()
    throws IOException, XMLStreamException
  {
    int tag;

    XMLStreamReader in = _in;

    if (in == null)
      return false;

    while ((tag = in.next()) > 0) {
      if (_isFinest)
        debug(in);

      if (tag == XMLStreamReader.START_ELEMENT) {
        String localName = in.getLocalName();

        if ("stream".equals(localName)) {
          readStreamFeatures();

          return true;
        }
        else {
          log.fine(XmppClient.this + " expected stream at tag <" + _in.getLocalName() + ">");
          close();
          return false;
        }
      }
      else if (tag == XMLStreamReader.END_ELEMENT) {
        log.fine(XmppClient.this + " unexpected end </" + _in.getLocalName() + ">");
        close();
        return false;
      }
    }

    if (tag < 0) {
      close();
    }

    return false;
  }

  private boolean readSuccess()
    throws IOException, XMLStreamException
  {
    int tag;

    XMLStreamReader in = _in;

    if (in == null)
      return false;

    while ((tag = in.next()) > 0) {
      if (_isFinest)
        debug(in);

      if (tag == XMLStreamReader.START_ELEMENT) {
        String localName = in.getLocalName();

        if ("success".equals(localName)) {
          _reader.skipToEnd("success");

          return true;
        }
        else {
          log.fine(XmppClient.this + " expected success at tag <" + _in.getLocalName() + ">");
          close();
          return false;
        }
      }
      else if (tag == XMLStreamReader.END_ELEMENT) {
        log.fine(XmppClient.this + " unexpected end </" + _in.getLocalName() + ">");
        close();
        return false;
      }
    }

    if (tag < 0) {
      close();
    }

    return false;
  }

  public ActorStream getBrokerStream()
  {
    return _toBroker;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "," + _port + "]";
  }

  @Override
  protected void finalize() throws Throwable
  {
    super.finalize();
    
    close();
  }

  class BindCallback extends AbstractActorStream {
    public String getJid()
    {
      throw new UnsupportedOperationException();
    }
    
    @Override
    public ActorStream getLinkStream()
    {
      throw new UnsupportedOperationException();
    }
    
    @Override
    public void queryResult(long id, String to, String from,
                            Serializable value)
    {
      if (value instanceof ImBindQuery) {
        ImBindQuery bind = (ImBindQuery) value;

        _jid = bind.getJid();
      }
    }
  }

  class Listener implements Runnable {
    private boolean _isFinest;
    
    public void run()
    {
      _isFinest = log.isLoggable(Level.FINEST);

      try {
        while (! isClosed()) {
          readPacket();
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        close();
      }
    }

    private void readPacket()
      throws IOException, XMLStreamException
    {
      int tag;

      XMLStreamReader in = _in;

      if (in == null)
        return;

      while ((tag = in.next()) > 0) {
        if (_isFinest)
          debug(in);

        if (tag == XMLStreamReader.START_ELEMENT) {
          String localName = in.getLocalName();

          if ("success".equals(localName)) {
            skipToEnd("success");

            _stanzaQueue.add(new SuccessStanza(in));
          }
          else if ("stream".equals(localName)) {
            readStreamFeatures();

            _stanzaQueue.add(new StreamStanza(in));
          }
          else if ("iq".equals(localName)) {
            _reader.handleIq();
          }
          else if ("message".equals(localName)) {
            _reader.handleMessage();
          }
          else if ("presence".equals(localName)) {
            _reader.handlePresence();
          }
          else {
            log.fine(XmppClient.this + " unknown tag <" + _in.getLocalName() + ">");
            close();
            return;
          }
        }
        else if (tag == XMLStreamReader.END_ELEMENT) {
          log.fine(XmppClient.this + " unexpected end </" + _in.getLocalName() + ">");
          close();
          return;
        }
      }

      if (tag < 0) {
        close();
      }
    }

    private Stanza readIq(XMLStreamReader in)
      throws IOException, XMLStreamException
    {
      String type = in.getAttributeValue(null, "type");

      if ("error".equals(type)) {
        skipToEnd("iq");

        return new IqErrorStanza(in);
      }
      else if ("result".equals(type)) {
        String id = in.getAttributeValue(null, "id");

        int tag = in.nextTag();

        if (_isFinest)
          debug(in);

        if (tag == XMLStreamReader.END_ELEMENT
            && "iq".equals(in.getLocalName())) {
          return new EmptyStanza();
        }

        if (tag != XMLStreamReader.START_ELEMENT)
          throw new IllegalStateException("expected start");

        String name = in.getLocalName();

        if ("bind".equals(name)) {
          return readBind(in, id);
        }
        else if ("session".equals(name)) {
          skipToEnd("iq");

          return new SessionStanza();
        }
        else {
          skipToEnd("iq");

          return new IqErrorStanza();
        }
      }
      else {
        throw new UnsupportedOperationException(type);
      }
    }

    private Stanza readBind(XMLStreamReader in, String id)
      throws IOException, XMLStreamException
    {
      BindStanza bind = new BindStanza();
      bind.setId(id);

      skipToEnd("bind");
      skipToEnd("iq");

      return bind;
    }

    private void skipToEnd(String tagName)
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
  }
}
