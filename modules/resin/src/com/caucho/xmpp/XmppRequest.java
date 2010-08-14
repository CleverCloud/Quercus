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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.caucho.bam.Broker;
import com.caucho.env.thread.ThreadPool;
import com.caucho.hemp.broker.HempBrokerManager;
import com.caucho.network.listen.AbstractProtocolConnection;
import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.util.Base64;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * XMPP protocol
 */
public class XmppRequest extends AbstractProtocolConnection {
  private static final L10N L = new L10N(XmppRequest.class);
  private static final Logger log
    = Logger.getLogger(XmppRequest.class.getName());

  private static final String STREAMS_NS = "http://etherx.jabber.org/streams";
  private static final String STARTTLS_NS = "urn:ietf:params:xml:ns:xmpp-tls";
  private static final String AUTH_NS = "urn:ietf:params:xml:ns:xmpp-sasl";

  private XmppProtocol _protocol;

  private HempBrokerManager _brokerManager;
  private Broker _broker;
  
  private TcpSocketLink _conn;

  private ReadStream _is;
  private WriteStream _os;

  private volatile int _requestId;

  private String _id;

  private String _host; // hostname given in stream
  private String _clientTo;
  private String _uid;
  
  private String _streamFrom;
  private String _clientBind;

  private String _name;
  
  private XmppStreamReader _in;

  private boolean _isAllowTls = false;
  private boolean _isRequireSession = true;

  private boolean _isPresent;
  private boolean _isThread;

  private final ThreadPool _threadPool;
  private final BlockingQueue<Stanza> _outboundQueue
    = new ArrayBlockingQueue<Stanza>(1024);

  private State _state;
  private boolean _isFinest;

  XmppRequest(XmppProtocol protocol, TcpSocketLink conn)
  {
    _protocol = protocol;
    _brokerManager = protocol.getBrokerManager();
    _conn = conn;
    _threadPool = ThreadPool.getThreadPool();
  }

  int getRequestId()
  {
    return _requestId;
  }

  String getUid()
  {
    return _uid;
  }

  /**
   * Returns the tcp connection
   */
  public TcpSocketLink getConnection()
  {
    return _conn;
  }

  XmppProtocol getProtocol()
  {
    return _protocol;
  }
  
  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init()
  {
  }

  /**
   * Return true if the connection should wait for a read before
   * handling the request.
   */
  public boolean isWaitForRead()
  {
    return true;
  }

  /**
   * Called when the connection starts.
   */
  public void startConnection()
  {
    _host = null;
    _broker = null;
  }
  
  /**
   * Handles a new connection.  The controlling TcpServer may call
   * handleConnection again after the connection completes, so 
   * the implementation must initialize any variables for each connection.
   */
  public boolean handleRequest()
    throws IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_protocol.getClassLoader());

      _isFinest = log.isLoggable(Level.FINEST);
      
      if (_state == null) {
        return handleInit();
      }

      SocketLinkDuplexListener handler
        = new XmppBrokerStream(this, _broker, _is, _in, _os);
      
      SocketLinkDuplexController controller = _conn.startDuplex(handler);

      return true;
    } catch (XMLStreamException e) {
      e.printStackTrace();
      throw new IOExceptionWrapper(e);
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private boolean handleInit()
    throws IOException, XMLStreamException
  {
    _state = State.INIT;

    StringBuilder sb = new StringBuilder();
    Base64.encode(sb, RandomUtil.getRandomLong());
    while (sb.charAt(sb.length() - 1) == '=')
      sb.setLength(sb.length() - 1);
    
    _id = sb.toString();
    
    int ch;
      
    _is = _conn.getReadStream();
    _os = _conn.getWriteStream();
      
    _in = new XmppStreamReaderImpl(_is, _protocol.getMarshalFactory());

    if (! readStreamHeader())
      return false;

    writeStreamHeader(_host);

    readStreamInit();

    return true;
  }

  private boolean readStreamHeader()
    throws IOException, XMLStreamException
  {
    int tag;
    while ((tag = _in.next()) > 0
           && tag != XMLStreamConstants.START_ELEMENT) {
      if (_isFinest)
        debug(_in);
    }
    
    if (_isFinest)
      debug(_in);

    String name = _in.getLocalName();
      
    if (! "stream".equals(name)) {
      _os.print("<error><invalid-xml/></error>");
      
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("{0}: '{1}' is an unknown tag from {2}",
                     this, name, _conn.getRemoteAddress()));
      return false;
    }
    else if (! STREAMS_NS.equals(_in.getNamespaceURI())) {
      _os.print("<error><bad-namespace-prefix/></error>");
      
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("{0}: xmlns='{1}' is an unknown namespace from {2}",
                     this, name, _conn.getRemoteAddress()));
      
      return false;
    }
    /*
    else if (! "jabber:client".equals(_in.getNamespaceURI(""))) {
      _os.print("<error><bad-namespace-prefix/></error>");
      
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("{0}: xmlns='{1}' is an unknown namespace for '' from {2}",
                     this, name, _conn.getRemoteAddress()));
      
      return false;
    }
    */
    else if (! "1.0".equals(_in.getAttributeValue(null, "version"))) {
      _os.print("<error><unsupported-version/></error>");
      
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("{0}: version='{1}' is an unknown version from {2}",
                     this, _in.getAttributeValue(null, "version"),
                     _conn.getRemoteAddress()));
      
      return false;
    }

    _host = _in.getAttributeValue(null, "to");

    String from = _host;

    if (from == null)
      from = _conn.getLocalAddress().getHostAddress();

    _broker = _brokerManager.findBroker(_host);

    if (_broker == null) {
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("{0}: host='{1}' is an unknown host",
                     this, _host));

      _os.print("<error><unknown-host/></error>");
      
      return false;
    }
      
    _streamFrom = from;
    _clientTo = from + "/" + _id;

    return true;
  }

  private boolean skipToStartElement()
    throws IOException, XMLStreamException
  {
    int tag;
    
    while ((tag = _in.next()) > 0
           && tag != XMLStreamConstants.START_ELEMENT) {
      if (_isFinest)
        debug(_in);
    }
    
    if (tag >= 0 && _isFinest)
      debug(_in);

    return tag >= 0;
  }
  
  private boolean readStreamInit()
    throws IOException, XMLStreamException
  {
    if (! skipToStartElement())
      return false;

    if ("starttls".equals(_in.getLocalName())
        && STARTTLS_NS.equals(_in.getNamespaceURI())) {
      if (! startTls())
        return false;
    }

    if ("auth".equals(_in.getLocalName())
        && AUTH_NS.equals(_in.getNamespaceURI())) {
      if (! handleAuth())
        return false;

      if (! skipToStartElement())
        return false;
    }

    if ("stream".equals(_in.getLocalName())
        && STREAMS_NS.equals(_in.getNamespaceURI())) {
      if (! handleStream())
        return false;
    }

    return true;
  }

  private boolean startTls()
    throws IOException, XMLStreamException
  {
    skipToEnd("starttls");

    _os.print("<proceed xmlns='" + STARTTLS_NS + "'/>");
    _os.flush();

    // _conn.upgradeTLS();
      
    System.out.println("STARTTLS");

    return true;
  }

  private void writeStreamHeader(String host)
    throws IOException
  {
    _os.print("<stream:stream xmlns='jabber:client'");
    _os.print(" xmlns:stream='http://etherx.jabber.org/streams'");
    _os.print(" id='" + _id + "'");
    _os.print(" from='" + host + "'");
    _os.print(" version='1.0'>");
      
    // + "   <mechanism>DIGEST-MD5</mechanism>\n"
    _os.print("<stream:features>");
    
    _os.print("<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>");
    _os.print("<mechanism>PLAIN</mechanism>");
    _os.print("</mechanisms>");

    if (_isAllowTls)
      _os.print("<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'></starttls>");
    
    _os.print("<auth xmlns='http://jabber.org/features/iq-auth'></auth>");
    //_os.print("<register xmlns='http://jabber.org/features/iq-register'></register>");
    _os.print("</stream:features>\n");
    _os.flush();
  }

  private boolean handleStream()
    throws IOException, XMLStreamException
  {
    String name = _in.getLocalName();

    String to = null;
      
    for (int i = _in.getAttributeCount() - 1; i >= 0; i--) {
      String localName = _in.getAttributeLocalName(i);
      String value = _in.getAttributeValue(i);

      if ("to".equals(localName))
        to = value;
    }

    String from = _host;

    if (from == null)
      from = to;

    if (from == null)
      from = _conn.getLocalAddress().getHostAddress();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " stream open(from=" + from + " id=" + _id + ")");
      
    _os.print("<stream:stream xmlns='jabber:client'");
    _os.print(" xmlns:stream='http://etherx.jabber.org/streams'");
    _os.print(" id='" + _id + "'");
    _os.print(" from='" + from + "'");
    _os.print(" version='1.0'>");
      
    _os.print("<stream:features>");
    _os.print("<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/>");

    if (_isRequireSession)
      _os.print("<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>");
    
    _os.print("</stream:features>");
    _os.flush();

    return true;
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

  private boolean handleAuth()
    throws IOException, XMLStreamException
  {
    String mechanism = _in.getAttributeValue(null, "mechanism");

    if ("PLAIN".equals(mechanism))
      return handleAuthPlain();
    else
      throw new IllegalStateException("Unknown mechanism: " + mechanism);
  }

  private boolean handleAuthPlain()
    throws IOException, XMLStreamException
  {
    String value = null;

    int tag;
    while ((tag = _in.next()) > 0
           && tag != XMLStreamConstants.START_ELEMENT
           && tag != XMLStreamConstants.END_ELEMENT) {
      if (_isFinest)
        debug(_in);
      
      if (tag == XMLStreamConstants.CHARACTERS) {
        char []buffer = _in.getTextCharacters();
        int start = _in.getTextStart();
        int len = _in.getTextLength();

        value = new String(_in.getTextCharacters(), start, len);
      }
    }

    if (value == null)
      return false;
    
    if (_isFinest)
      debug(_in);

    String decoded = Base64.decode(value);

    int p = decoded.indexOf(0, 1);

    if (p < 0)
      return false;

    String name = decoded.substring(1, p);
    String password = decoded.substring(p + 1);

    boolean isAuth = true;

    if (isAuth) {
      _name = name;

      _uid = _name + "@" + _host;

      if (log.isLoggable(Level.FINE))
        log.fine(this + " auth-plain success for " + name);
      
      _os.print("<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'></success>");
      _os.flush();
      
      return true;
    }

    return false;
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
  
  /**
   * Resumes processing after a wait.
   */
  public boolean handleResume()
    throws IOException
  {
    return false;
  }

  /**
   * Handles a close event when the connection is closed.
   */
  public void onCloseConnection()
  {
    _requestId++;
    
    _state = null;
    _isPresent = false;
  }

  @Override
  public String toString()
  {
    if (_conn != null)
      return getClass().getSimpleName() + "[" + _conn.getId() + "]";
    else
      return getClass().getSimpleName() + "[]";
  }

  enum State {
    INIT
  };
}
