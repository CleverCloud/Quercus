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

package com.caucho.quercus.lib;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.SocketInputOutput;
import com.caucho.quercus.lib.file.TcpInputOutput;
import com.caucho.quercus.lib.file.UdpInputOutput;
import com.caucho.quercus.lib.file.SocketInputOutput.Domain;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Information about PHP network
 */
public class NetworkModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(NetworkModule.class);
  private static final Logger log
    = Logger.getLogger(NetworkModule.class.getName());

  private static final LinkedHashMap<String, LongValue> _protoToNum
    = new LinkedHashMap<String, LongValue>();
  private static final LinkedHashMap<String, ServiceNode> _servToNum
    = new LinkedHashMap<String, ServiceNode>();

  public static final int LOG_EMERG = 0;
  public static final int LOG_ALERT = 1;
  public static final int LOG_CRIT = 2;
  public static final int LOG_ERR = 3;
  public static final int LOG_WARNING = 4;
  public static final int LOG_NOTICE = 5;
  public static final int LOG_INFO = 6;
  public static final int LOG_DEBUG = 7;

  public static final int LOG_PID = 1;
  public static final int LOG_CONS = 2;
  public static final int LOG_NDELAY = 8;
  public static final int LOG_NOWAIT = 16;
  public static final int LOG_ODELAY = 4;
  public static final int LOG_PERROR = 32;

  public static final int LOG_AUTH = 32;
  public static final int LOG_AUTHPRIV = 80;
  public static final int LOG_CRON = 72;
  public static final int LOG_DAEMON = 24;
  public static final int LOG_KERN = 0;
  public static final int LOG_LOCAL0 = 128;
  public static final int LOG_LOCAL1 = 136;
  public static final int LOG_LOCAL2 = 144;
  public static final int LOG_LOCAL3 = 152;
  public static final int LOG_LOCAL4 = 160;
  public static final int LOG_LOCAL5 = 168;
  public static final int LOG_LOCAL6 = 176;
  public static final int LOG_LOCAL7 = 184;
  public static final int LOG_LPR = 48;
  public static final int LOG_MAIL = 16;
  public static final int LOG_NEWS = 56;
  public static final int LOG_SYSLOG = 40;
  public static final int LOG_USER = 8;
  public static final int LOG_UUCP = 64;

  public static final int DNS_A = 1;
  public static final int DNS_CNAME = 16;
  public static final int DNS_HINFO = 4096;
  public static final int DNS_MX = 16384;
  public static final int DNS_NS = 2;
  public static final int DNS_PTR = 2048;
  public static final int DNS_SOA = 32;
  public static final int DNS_TXT = 32768;
  public static final int DNS_AAAA = 134217728;
  public static final int DNS_SRV = 33554432;
  public static final int DNS_NAPTR = 67108864;
  public static final int DNS_A6 = 16777216;
  public static final int DNS_ALL = 251713587;
  public static final int DNS_ANY = 268435456;

  /**
   * Opens a socket
   */
  public static SocketInputOutput fsockopen(Env env,
                                            String host,
                                            @Optional int port,
                                            @Optional @Reference Value errno,
                                            @Optional @Reference Value errstr,
                                            @Optional double timeout)
  {
    try {
      if (host == null)
        return null;
      
      String protocol = null;
      int p = host.indexOf("://");
      if (p > 0) {
        protocol = host.substring(0, p);
        host = host.substring(p + 3);
      }

      p = host.lastIndexOf(':');
      int q = host.lastIndexOf(']');
      
      if (p > 0 && q < p) {
        String portStr = host.substring(p + 1);
        host = host.substring(0, p);

        if (port == 0)
          port = Integer.parseInt(portStr);
      }

      if (port == 0)
        port = 80;
      
      SocketInputOutput stream;
      
      if ("udp".equals(protocol))
        stream = new UdpInputOutput(env, host, port, Domain.AF_INET);
      else {
        boolean isSecure = "ssl".equals(protocol);
        
        stream = new TcpInputOutput(env, host, port, isSecure, Domain.AF_INET);
      }
      
      if (timeout > 0)
        stream.setTimeout((int) (timeout * 1000));
      else
        stream.setTimeout(120000);

      stream.init();

      return stream;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      if (errstr != null)
        errstr.set(env.createString(e.toString()));

      return null;
    }
  }

  /**
   * Converts string to long
   */
  public static Value ip2long(String ip)
  {
    // php/1m00
    
    if (ip == null)
      return LongValue.MINUS_ONE;
    
    long v = 0;

    int p = 0;
    int len = ip.length();
    for (int i = 0; i < 4; i++) {
      int digit = 0;
      char ch = 0;

      for (; p < len && '0' <= (ch = ip.charAt(p)) && ch <= '9'; p++) {
        digit = 10 * digit + ch - '0';
      }

      if (p < len && ch != '.')
        return BooleanValue.FALSE;
      else if (p == len && i < 3)
        return BooleanValue.FALSE;

      p++;

      v = 256 * v + digit;
    }

    return new LongValue(v);
  }
  
  /*
   * Converts internet address in integer format to dotted format.
   */
  public static StringValue long2ip(Env env, long address)
  {
    if (address < 0L || address >= 0xFFFFFFFFL)
      return env.createString("255.255.255.255");
    
    StringValue sb = env.createStringBuilder();
    
    sb.append((address & 0xFF000000L) >> 24);
    sb.append('.');
    
    sb.append((address & 0xFF0000L) >> 16);
    sb.append('.');
    
    sb.append((address & 0xFF00L) >> 8);
    sb.append('.');
    
    sb.append(address & 0xFFL);
    
    return sb;
  }

  /**
   * Returns the IP address of the given host name.  If the IP address cannot
   * be obtained, then the provided host name is returned instead.
   *
   * @param hostname  the host name who's IP to search for
   *
   * @return the IP for the given host name or, if the IP cannot be obtained,
   *         the provided host name
   */
  public static String gethostbyname(String hostname)
  {
    // php/1m01

    if (hostname == null)
      return "";
    
    InetAddress ip = null;

    try {
      ip = InetAddress.getByName(hostname);
    }
    catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return hostname;
    }

    return ip.getHostAddress();
  }

  /**
   * Returns the IP addresses of the given host name.  If the IP addresses
   * cannot be obtained, then the provided host name is returned instead.
   *
   * @param hostname  the host name who's IP to search for
   *
   * @return the IPs for the given host name or, if the IPs cannot be obtained,
   *         the provided host name
   */
  public static Value gethostbynamel(Env env, String hostname)
  {
    // php/1m02

    InetAddress []ip = null;

    try {
      ip = InetAddress.getAllByName(hostname);
    }
    catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return BooleanValue.FALSE;
    }

    ArrayValue ipArray = new ArrayValueImpl();

    for (int k = 0; k < ip.length; k++) {
      String currentIPString = ip[k].getHostAddress();

      StringValue currentIP = env.createString((currentIPString));

      ipArray.append(currentIP);
    }

    return ipArray;
  }

  /**
   * Returns the IP address of the given host name.  If the IP address cannot
   * be obtained, then the provided host name is returned instead.
   *
   * @return the IP for the given host name or, if the IP cannot be obtained,
   *         the provided host name
   */
  @ReturnNullAsFalse
  public static String gethostbyaddr(Env env, String ip)
  {
    // php/1m03
    
    if (ip == null) {
      env.warning("Address must not be null.");      

      return null;
    }

    String formIPv4 = "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
        + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
        + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
        + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

    CharSequence ipToCS = ip.subSequence(0, ip.length());

    if (! (Pattern.matches(formIPv4, ipToCS))) {
      env.warning("Address is not in a.b.c.d form");

      return null;
    }

    String []splitIP = null;

    try {
      splitIP = ip.split("\\.");
    }
    catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      env.warning("Regex expression invalid");

      return ip;
    }

    byte []addr = new byte[splitIP.length];

    for (int k = 0; k < splitIP.length; k++) {
      Integer intForm = new Integer(splitIP[k]);

      addr[k] = intForm.byteValue();
    }

    InetAddress host = null;

    try {
      host = InetAddress.getByAddress(addr);
    }
    catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return ip;
    }

    return host.getHostName();
  }

  /**
   * Returns the protocol number associated with the given protocol name.
   *
   * @param protoName  the name of the protocol
   *
   * @return the number associated with the given protocol name
   */
  public static Value getprotobyname(String protoName)
  {
    // php/1m04

    if (! (_protoToNum.containsKey(protoName)))
      return BooleanValue.FALSE;

    return LongValue.create((_protoToNum.get(protoName).toLong()));
  }

  /**
   * Returns the protocol name associated with the given protocol number.
   */
  @ReturnNullAsFalse
  public static String getprotobynumber(int protoNumber)
  {
    // php/1m05

    for (Map.Entry<String, LongValue> entry : _protoToNum.entrySet())
      if (entry.getValue().toLong() == protoNumber)
        return entry.getKey();

    return null;
  }

  /**
   * Returns the port number associated with the given protocol and service
   * name.
   *
   * @param service  the service name
   * @param protocol  the protocol, either udp or tcp
   *
   * @return the number associated with the given protocol and service name
   */
  public static Value getservbyname(String service, String protocol)
  {
    // php/1m06

    if (! (_servToNum.containsKey(service)))
      return BooleanValue.FALSE;

    ServiceNode node = _servToNum.get(service);

    if (! (node.protocolCheck(protocol)))
      return BooleanValue.FALSE;

    return node.getPort();
  }

  /**
   * Returns the service name associated it the given protocol name and
   * service port.
   *
   * @param port  the service port number
   * @param protocol  the protocol, either udp or tcp
   *
   * @return the service name
   */
  @ReturnNullAsFalse
  public static String getservbyport(int port, String protocol)
  {
    // php/1m07

    for (Map.Entry<String, ServiceNode> entry : _servToNum.entrySet()) {
      ServiceNode node = entry.getValue();

      if (node.getPort().toLong() == port
          && node.protocolCheck(protocol))
              return entry.getKey();
    }

    return null;
  }

  public static boolean getmxrr(Env env,
                                @NotNull String hostname,
                                @Reference Value mxhosts,
                                @Optional @Reference Value weight)
  {
    return dns_get(env, hostname, "MX", mxhosts, weight);
  }

  private static boolean dns_get(Env env,
                                 String hostname,
                                 String type,
                                 Value hostsRef,
                                 Value weightRef)
  {
    try {
      // php/1m08

      if (hostname == null || type == null)
        return false;

      DirContext ictx = new InitialDirContext();
      Attributes attributes;

      if (type.equals("ANY") || type.equals("ALL"))
        attributes = ictx.getAttributes("dns:/" + hostname);
      else
        attributes = ictx.getAttributes(
            "dns:/" + hostname, new String[] { type });


      ArrayValue hosts =  new ArrayValueImpl();

      ArrayValue weights =  new ArrayValueImpl();

      NamingEnumeration list = attributes.getAll();

      if (! (list.hasMore()))
        return false;

      while (list.hasMore()) {
        Attribute record = (Attribute) list.next();

        String id = record.getID();

        NamingEnumeration attrList = record.getAll();

        while (attrList.hasMore()) {
          String target = String.valueOf(attrList.next());

          if (target.endsWith("."))
            target = target.substring(0, target.length() - 1);

          int weight = 0;

          if ("MX".equals(id)) {
            int space = target.indexOf(" ");
            if (space > -1) {
              String priorityPart = target.substring(0, space);
              String hostPart = target.substring(space + 1);

              target = hostPart;
              
              try {
                weight = Integer.valueOf(priorityPart);
              }
              catch (NumberFormatException ex) {
                log.log(Level.FINE, ex.toString(), ex);
              }
            }
          }

          weights.append(LongValue.create(weight));
          hosts.append(StringValue.create(target));
        }
      }

      ArrayModule.array_multisort(env, new Value[] { weights, hosts });

      if (hostsRef != null)
        hostsRef.set(hosts);

      if (weightRef != null)
        weightRef.set(weights);

      return true;
    } catch (NameNotFoundException e) {
      return false;
    } catch (NamingException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Finds the mx hosts for the given hostname, placing them in mxhosts and
   * their corresponding weights in weight, if provided.  Returns true if any
   * hosts were found.  False otherwise.
   *
   * @param hostname  the hostname to find records for
   * @param mxhosts  an array to add the mx hosts to
   * @param weight  an array to add the weights to
   *
   * @return true if records are found, false otherwise
   */
  public static boolean dns_get_mx(Env env,
                                   @NotNull String hostname,
                                   @Reference Value mxhosts,
                                   @Optional @Reference Value weight)
  {
    return dns_get(env, hostname, "MX", mxhosts, weight);

  }

  public static boolean checkdnsrr(Env env,
                                   @NotNull String hostname,
                                   @Optional("MX") String type)
  {
    return dns_get(env, hostname, type, null, null);
  }

  /**
   * Finds the mx hosts for the given hostname, placing them in mxhosts and
   * their corresponding weights in weight, if provided.  Returns true if any
   * hosts were found.  False otherwise.
   *
   * @param hostname  the hostname to find records for
   *
   * @return true if records are found, false otherwise
   */
  public static boolean dns_check_record(Env env,
                                         @NotNull String hostname,
                                         @Optional("MX") String type)
  {
    return dns_get(env, hostname, type, null, null);
  }

  public ArrayValue dns_get_record(Env env,
                                   @NotNull String hostname,
                                   @Optional("-1") int type,
                                   @Optional @Reference Value authnsRef,
                                   @Optional @Reference Value addtlRef)
  {
    ArrayValue result =  new ArrayValueImpl();

    ArrayValueImpl authns = null;

    if (authnsRef != null && !authnsRef.isNull()) {
      authns = new ArrayValueImpl();
      authnsRef.set(authns);
      env.stub("authns unimplemented");
    }

    ArrayValueImpl addtl = null;

    if (addtlRef != null && !addtlRef.isNull()) {
      addtl = new ArrayValueImpl();
      addtlRef.set(addtl);
      env.stub("addtl unimplemented");
    }

    if (hostname == null)
      return result;

    if (type == -1)
     type = DNS_ANY;

    String typeName;

    switch (type) {
      case DNS_A: typeName = "A"; break;
      case DNS_CNAME: typeName = "CNAME"; break;
      case DNS_HINFO: typeName = "HINFO"; break;
      case DNS_MX: typeName = "MX"; break;
      case DNS_NS: typeName = "NS"; break;
      case DNS_PTR: typeName = "PTR"; break;
      case DNS_SOA: typeName = "SOA"; break;
      case DNS_TXT: typeName = "TXT"; break;
      case DNS_AAAA: typeName = "AAAA"; break;
      case DNS_SRV: typeName = "SRV"; break;
      case DNS_NAPTR: typeName = "NAPTR"; break;
      case DNS_A6: typeName = "A6"; break;
      default:
        typeName = null;
    }

    try {
      DirContext ictx = new InitialDirContext();
      Attributes attributes;

      if (typeName == null)
        attributes = ictx.getAttributes("dns:/" + hostname);
      else
        attributes = ictx.getAttributes(
            "dns:/" + hostname, new String[] { typeName });

      NamingEnumeration list = attributes.getAll();

      while (list.hasMore()) {
        Attribute record = (Attribute) list.next();

        String id = record.getID();

        NamingEnumeration attrList = record.getAll();

        while (attrList.hasMore()) {
          String attr = String.valueOf(attrList.next());

          String target = attr;

          if (target.endsWith("."))
            target = target.substring(0, target.length() - 1);

          ArrayValueImpl recordValue = new ArrayValueImpl();
          recordValue.put("host", hostname);
          recordValue.put("type", id);

          if ("MX".equals(id)) {
            int space = target.indexOf(" ");
            if (space > -1) {
              String priorityPart = target.substring(0, space);
              String hostPart = target.substring(space + 1);

              try {
                recordValue.put("pri", Integer.valueOf(priorityPart));
                target = hostPart;
              }
              catch (NumberFormatException ex) {
                log.log(Level.FINE, ex.toString(), ex);
              }
            }
          }
          else if ("A".equals(id)) {
            try {
              recordValue.put("ip", target);

              target = null;
            }
            catch (Exception e) {
              log.log(Level.FINE, e.toString(), e);
            }
          }

          if (target != null)
            recordValue.put("target", target);

          result.put(recordValue);
        }
      }
    }
    catch (NameNotFoundException ex) {
      log.log(Level.FINER, ex.toString(), ex);
    }
    catch (NamingException ex) {
      throw new QuercusModuleException(ex);
    }

    return result;
  }

  /**
   * Initialization of syslog.
   */
  public static Value define_syslog_variables(Env env)
  {
    env.stub("unimplemented");
    return NullValue.NULL;
  }

  /**
   * Opens syslog.
   *
   * XXX: stubbed for now
   */
  public static boolean openlog(Env env, String ident, int option, int facility)
  {
    env.stub("unimplemented");
    return true;
  }

  /**
   * Closes syslog.
   */
  public static boolean closelog()
  {
    return true;
  }

  /**
   * syslog
   */
  public static boolean syslog(Env env, int priority, String message)
  {
    Level level = Level.OFF;

    switch (priority) {
      case LOG_EMERG:
      case LOG_ALERT:
      case LOG_CRIT:
        level = Level.SEVERE;
        break;
      case LOG_ERR:
      case LOG_WARNING:
        level = Level.WARNING;
        break;
      case LOG_NOTICE:
        level = Level.CONFIG;
        break;
      case LOG_INFO:
        level = Level.INFO;
        break;
      case LOG_DEBUG:
        level = Level.FINE;
        break;
    }

    env.getLogger().log(level, message);

    return true;
  }

  private static class ServiceNode {
    private LongValue _port;

    private boolean _isTCP;
    private boolean _isUDP;

    ServiceNode(int port, boolean tcp, boolean udp)
    {
      _port = LongValue.create(port);
      _isTCP = tcp;
      _isUDP = udp;
    }

    public LongValue getPort()
    {
      return _port;
    }

    public boolean protocolCheck(String protocol)
    {
      if (protocol.equals("tcp"))
              return _isTCP;
      else if (protocol.equals("udp"))
              return _isUDP;
      else
              return false;
    }

    public boolean isTCP()
    {
      return _isTCP;
    }

    public boolean isUDP()
    {
      return _isUDP;
    }
  }

  static {
    _protoToNum.put("ip", LongValue.create(0));
    _protoToNum.put("icmp", LongValue.create(1));
    _protoToNum.put("ggp", LongValue.create(3));
    _protoToNum.put("tcp", LongValue.create(6));
    _protoToNum.put("egp", LongValue.create(8));
    _protoToNum.put("pup", LongValue.create(12));
    _protoToNum.put("udp", LongValue.create(17));
    _protoToNum.put("hmp", LongValue.create(12));
    _protoToNum.put("xns-idp", LongValue.create(22));
    _protoToNum.put("rdp", LongValue.create(27));
    _protoToNum.put("rvd", LongValue.create(66));
    _servToNum.put("echo", new ServiceNode(7, true, true));
    _servToNum.put("discard", new ServiceNode(9, true, true));
    _servToNum.put("systat", new ServiceNode(11, true, true));
    _servToNum.put("daytime", new ServiceNode(13, true, true));
    _servToNum.put("qotd", new ServiceNode(17, true, true));
    _servToNum.put("chargen", new ServiceNode(19, true, true));
    _servToNum.put("ftp-data", new ServiceNode(20, true, false));
    _servToNum.put("ftp", new ServiceNode(21, true, false));
    _servToNum.put("telnet", new ServiceNode(23, true, false));
    _servToNum.put("smtp", new ServiceNode(25, true, false));
    _servToNum.put("time", new ServiceNode(37, true, true));
    _servToNum.put("rlp", new ServiceNode(39, false, true));
    _servToNum.put("nameserver", new ServiceNode(42, true, true));
    _servToNum.put("nicname", new ServiceNode(43, true, false));
    _servToNum.put("domain", new ServiceNode(53, true, true));
    _servToNum.put("bootps", new ServiceNode(67, false, true));
    _servToNum.put("bootpc", new ServiceNode(68, false, true));
    _servToNum.put("tftp", new ServiceNode(69, false, true));
    _servToNum.put("gopher", new ServiceNode(70, true, false));
    _servToNum.put("finger", new ServiceNode(79, true, false));
    _servToNum.put("http", new ServiceNode(80, true, false));
    _servToNum.put("kerberos", new ServiceNode(88, true, true));
    _servToNum.put("hostname", new ServiceNode(101, true, false));
    _servToNum.put("iso-tsap", new ServiceNode(102, true, false));
    _servToNum.put("rtelnet", new ServiceNode(107, true, false));
    _servToNum.put("pop2", new ServiceNode(109, true, false));
    _servToNum.put("pop3", new ServiceNode(110, true, false));
    _servToNum.put("sunrpc", new ServiceNode(111, true, true));
    _servToNum.put("auth", new ServiceNode(113, true, false));
    _servToNum.put("uucp-path", new ServiceNode(117, true, false));
    _servToNum.put("nntp", new ServiceNode(119, true, false));
    _servToNum.put("ntp", new ServiceNode(123, false, true));
    _servToNum.put("epmap", new ServiceNode(135, true, true));
    _servToNum.put("netbios-ns", new ServiceNode(137, true, true));
    _servToNum.put("netbios-dgm", new ServiceNode(138, false, true));
    _servToNum.put("netbios-ssn", new ServiceNode(139, true, false));
    _servToNum.put("imap", new ServiceNode(143, true, false));
    _servToNum.put("pcmail-srv", new ServiceNode(158, true, false));
    _servToNum.put("snmp", new ServiceNode(161, false, true));
    _servToNum.put("snmptrap", new ServiceNode(162, false, true));
    _servToNum.put("print-srv", new ServiceNode(170, true, false));
    _servToNum.put("bgp", new ServiceNode(179, true, false));
    _servToNum.put("irc", new ServiceNode(194, true, false));
    _servToNum.put("ipx", new ServiceNode(213, false, true));
    _servToNum.put("ldap", new ServiceNode(389, true, false));
    _servToNum.put("https", new ServiceNode(443, true, true));
    _servToNum.put("microsoft-ds", new ServiceNode(445, true, true));
    _servToNum.put("kpasswd", new ServiceNode(464, true, true));
    _servToNum.put("isakmp", new ServiceNode(500, false, true));
    _servToNum.put("exec", new ServiceNode(512, true, false));
    _servToNum.put("biff", new ServiceNode(512, false, true));
    _servToNum.put("login", new ServiceNode(513, true, false));
    _servToNum.put("who", new ServiceNode(513, false, true));
    _servToNum.put("cmd", new ServiceNode(514, true, false));
    _servToNum.put("syslog", new ServiceNode(514, false, true));
    _servToNum.put("printer", new ServiceNode(515, true, false));
    _servToNum.put("talk", new ServiceNode(517, false, true));
    _servToNum.put("ntalk", new ServiceNode(518, false, true));
    _servToNum.put("efs", new ServiceNode(520, true, false));
    _servToNum.put("router", new ServiceNode(520, false, true));
    _servToNum.put("timed", new ServiceNode(525, false, true));
    _servToNum.put("tempo", new ServiceNode(526, true, false));
    _servToNum.put("courier", new ServiceNode(530, true, false));
    _servToNum.put("conference", new ServiceNode(531, true, false));
    _servToNum.put("netnews", new ServiceNode(532, true, false));
    _servToNum.put("netwall", new ServiceNode(533, false, true));
    _servToNum.put("uucp", new ServiceNode(540, true, false));
    _servToNum.put("klogin", new ServiceNode(543, true, false));
    _servToNum.put("kshell", new ServiceNode(544, true, false));
    _servToNum.put("new-rwho", new ServiceNode(550, false, true));
    _servToNum.put("remotefs", new ServiceNode(556, true, false));
    _servToNum.put("rmonitor", new ServiceNode(560, false, true));
    _servToNum.put("monitor", new ServiceNode(561, false, true));
    _servToNum.put("ldaps", new ServiceNode(636, true, false));
    _servToNum.put("doom", new ServiceNode(666, true, true));
    _servToNum.put("kerberos-adm", new ServiceNode(749, true, true));
    _servToNum.put("kerberos-iv", new ServiceNode(750, false, true));
    _servToNum.put("kpop", new ServiceNode(1109, true, false));
    _servToNum.put("phone", new ServiceNode(1167, false, true));
    _servToNum.put("ms-sql-s", new ServiceNode(1433, true, true));
    _servToNum.put("ms-sql-m", new ServiceNode(1434, true, true));
    _servToNum.put("wins", new ServiceNode(1512, true, true));
    _servToNum.put("ingreslock", new ServiceNode(1524, true, false));
    _servToNum.put("12tp", new ServiceNode(1701, false, true));
    _servToNum.put("pptp", new ServiceNode(1723, true, false));
    _servToNum.put("radius", new ServiceNode(1812, false, true));
    _servToNum.put("radacct", new ServiceNode(1813, false, true));
    _servToNum.put("nfsd", new ServiceNode(2049, false, true));
    _servToNum.put("knetd", new ServiceNode(2053, true, false));
    _servToNum.put("man", new ServiceNode(9535, true, false));
  }
}

