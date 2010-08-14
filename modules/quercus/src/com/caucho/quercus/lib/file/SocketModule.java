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

package com.caucho.quercus.lib.file;

import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempCharBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Information and actions for about sockets
 */
public class SocketModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(SocketModule.class);
  private static final Logger log
    = Logger.getLogger(SocketModule.class.getName());

  private static final int AF_UNIX = 1;
  private static final int AF_INET = 2;
  private static final int AF_INET6 = 10;
  private static final int SOCK_STREAM = 1;
  private static final int SOCK_DGRAM = 2;
  private static final int SOCK_RAW = 3;
  private static final int SOCK_SEQPACKET = 5;
  private static final int SOCK_RDM = 4;
  private static final int MSG_OOB = 1;
  private static final int MSG_WAITALL = 256;
  private static final int MSG_PEEK = 2;
  private static final int MSG_DONTROUTE = 4;
  private static final int SO_DEBUG = 1;
  private static final int SO_REUSEADDR = 2;
  private static final int SO_KEEPALIVE = 9;
  private static final int SO_DONTROUTE = 5;
  private static final int SO_LINGER = 13;
  private static final int SO_BROADCAST = 6;
  private static final int SO_OOBINLINE = 10;
  private static final int SO_SNDBUF = 7;
  private static final int SO_RCVBUF = 8;
  private static final int SO_SNDLOWAT = 19;
  private static final int SO_RCVLOWAT = 18;
  private static final int SO_SNDTIMEO = 21;
  private static final int SO_RCVTIMEO = 20;
  private static final int SO_TYPE = 3;
  private static final int SO_ERROR = 4;
  private static final int SOL_SOCKET = 1;
  private static final int SOMAXCONN = 128;
  private static final int PHP_NORMAL_READ = 1;
  private static final int PHP_BINARY_READ = 2;
  private static final int SOCKET_EPERM = 1;
  private static final int SOCKET_ENOENT = 2;
  private static final int SOCKET_EINTR = 4;
  private static final int SOCKET_EIO = 5;
  private static final int SOCKET_ENXIO = 6;
  private static final int SOCKET_E2BIG = 7;
  private static final int SOCKET_EBADF = 9;
  private static final int SOCKET_EAGAIN = 11;
  private static final int SOCKET_ENOMEM = 12;
  private static final int SOCKET_EACCES = 13;
  private static final int SOCKET_EFAULT = 14;
  private static final int SOCKET_ENOTBLK = 15;
  private static final int SOCKET_EBUSY = 16;
  private static final int SOCKET_EEXIST = 17;
  private static final int SOCKET_EXDEV = 18;
  private static final int SOCKET_ENODEV = 19;
  private static final int SOCKET_ENOTDIR = 20;
  private static final int SOCKET_EISDIR = 21;
  private static final int SOCKET_EINVAL = 22;
  private static final int SOCKET_ENFILE = 23;
  private static final int SOCKET_EMFILE = 24;
  private static final int SOCKET_ENOTTY = 25;
  private static final int SOCKET_ENOSPC = 28;
  private static final int SOCKET_ESPIPE = 29;
  private static final int SOCKET_EROFS = 30;
  private static final int SOCKET_EMLINK = 31;
  private static final int SOCKET_EPIPE = 32;
  private static final int SOCKET_ENAMETOOLONG = 36;
  private static final int SOCKET_ENOLCK = 37;
  private static final int SOCKET_ENOSYS = 38;
  private static final int SOCKET_ENOTEMPTY = 39;
  private static final int SOCKET_ELOOP = 40;
  private static final int SOCKET_EWOULDBLOCK = 11;
  private static final int SOCKET_ENOMSG = 42;
  private static final int SOCKET_EIDRM = 43;
  private static final int SOCKET_ECHRNG = 44;
  private static final int SOCKET_EL2NSYNC = 45;
  private static final int SOCKET_EL3HLT = 46;
  private static final int SOCKET_EL3RST = 47;
  private static final int SOCKET_ELNRNG = 48;
  private static final int SOCKET_EUNATCH = 49;
  private static final int SOCKET_ENOCSI = 50;
  private static final int SOCKET_EL2HLT = 51;
  private static final int SOCKET_EBADE = 52;
  private static final int SOCKET_EBADR = 53;
  private static final int SOCKET_EXFULL = 54;
  private static final int SOCKET_ENOANO = 55;
  private static final int SOCKET_EBADRQC = 56;
  private static final int SOCKET_EBADSLT = 57;
  private static final int SOCKET_ENOSTR = 60;
  private static final int SOCKET_ENODATA = 61;
  private static final int SOCKET_ETIME = 62;
  private static final int SOCKET_ENOSR = 63;
  private static final int SOCKET_ENONET = 64;
  private static final int SOCKET_EREMOTE = 66;
  private static final int SOCKET_ENOLINK = 67;
  private static final int SOCKET_EADV = 68;
  private static final int SOCKET_ESRMNT = 69;
  private static final int SOCKET_ECOMM = 70;
  private static final int SOCKET_EPROTO = 71;
  private static final int SOCKET_EMULTIHOP = 72;
  private static final int SOCKET_EBADMSG = 74;
  private static final int SOCKET_ENOTUNIQ = 76;
  private static final int SOCKET_EBADFD = 77;
  private static final int SOCKET_EREMCHG = 78;
  private static final int SOCKET_ERESTART = 85;
  private static final int SOCKET_ESTRPIPE = 86;
  private static final int SOCKET_EUSERS = 87;
  private static final int SOCKET_ENOTSOCK = 88;
  private static final int SOCKET_EDESTADDRREQ = 89;
  private static final int SOCKET_EMSGSIZE = 90;
  private static final int SOCKET_EPROTOTYPE = 91;
  private static final int SOCKET_ENOPROTOOPT = 92;
  private static final int SOCKET_EPROTONOSUPPORT = 93;
  private static final int SOCKET_ESOCKTNOSUPPORT = 94;
  private static final int SOCKET_EOPNOTSUPP = 95;
  private static final int SOCKET_EPFNOSUPPORT = 96;
  private static final int SOCKET_EAFNOSUPPORT = 97;
  private static final int SOCKET_EADDRINUSE = 98;
  private static final int SOCKET_EADDRNOTAVAIL = 99;
  private static final int SOCKET_ENETDOWN = 100;
  private static final int SOCKET_ENETUNREACH = 101;
  private static final int SOCKET_ENETRESET = 102;
  private static final int SOCKET_ECONNABORTED = 103;
  private static final int SOCKET_ECONNRESET = 104;
  private static final int SOCKET_ENOBUFS = 105;
  private static final int SOCKET_EISCONN = 106;
  private static final int SOCKET_ENOTCONN = 107;
  private static final int SOCKET_ESHUTDOWN = 108;
  private static final int SOCKET_ETOOMANYREFS = 109;
  private static final int SOCKET_ETIMEDOUT = 110;
  private static final int SOCKET_ECONNREFUSED = 111;
  private static final int SOCKET_EHOSTDOWN = 112;
  private static final int SOCKET_EHOSTUNREACH = 113;
  private static final int SOCKET_EALREADY = 114;
  private static final int SOCKET_EINPROGRESS = 115;
  private static final int SOCKET_EISNAM = 120;
  private static final int SOCKET_EREMOTEIO = 121;
  private static final int SOCKET_EDQUOT = 122;
  private static final int SOCKET_ENOMEDIUM = 123;
  private static final int SOCKET_EMEDIUMTYPE = 124;
  private static final int SOL_TCP = 6;
  private static final int SOL_UDP = 17;

  private static final HashMap<StringValue,Value> _constMap
    = new HashMap<StringValue,Value>();

  /**
   * Returns the constants defined by this module.
   */
  public Map<StringValue,Value> getConstMap()
  {
    return _constMap;
  }

  @ReturnNullAsFalse
  public static SocketInputOutput socket_create(Env env,
                                                int domain,
                                                int type,
                                                int protocol)
  {
    try {
      SocketInputOutput.Domain socketDomain = SocketInputOutput.Domain.AF_INET;

      switch (domain) {
        case AF_INET:
          socketDomain = SocketInputOutput.Domain.AF_INET;
          break;
        case AF_INET6:
          socketDomain = SocketInputOutput.Domain.AF_INET6;
          break;
        case AF_UNIX:
          env.warning(L.l("Unix sockets not supported"));
          return null;
        default:
          env.warning(L.l("Unknown domain: {0}", domain));
          return null;
      }

      switch (type) {
        case SOCK_STREAM:
          return new TcpInputOutput(env, new Socket(), socketDomain);
        case SOCK_DGRAM:
          return new UdpInputOutput(env, new DatagramSocket(), socketDomain);
        default:
          env.warning(L.l("socket stream not socked"));
          return null;
      }

    } catch (Exception e) {
      env.warning(e);
      return null;
    }
  }

  public static boolean socket_bind(Env env,
                                    @NotNull SocketInputOutput socket,
                                    StringValue address,
                                    @Optional("0") int port)
  {
    try {
      InetAddress []addresses = InetAddress.getAllByName(address.toString());

      if (addresses == null || addresses.length < 1) {
        //XXX: socket.setError();
        return false;
      }

      InetSocketAddress socketAddress =
        new InetSocketAddress(addresses[0], port);

      socket.bind(socketAddress);

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static void socket_close(Env env, @NotNull SocketInputOutput socket)
  {
    socket.close();
  }

  public static boolean socket_connect(Env env,
                                       @NotNull SocketInputOutput socket,
                                       StringValue address, @Optional int port)
  {
    try {
      InetAddress []addresses = InetAddress.getAllByName(address.toString());

      if (addresses == null || addresses.length < 1) {
        //XXX: socket.setError();
        return false;
      }

      InetSocketAddress socketAddress =
        new InetSocketAddress(addresses[0], port);

      socket.connect(socketAddress);

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static Value socket_get_status(Env env, BinaryStream stream)
  {
    return StreamModule.stream_get_meta_data(env, stream);
  }

  public static Value socket_read(Env env,
                                  @NotNull SocketInputOutput socket,
                                  int length, @Optional int type)
  {
    TempBuffer tempBuffer = null;
    TempCharBuffer tempCharBuffer = null;

    try {
      if (type == PHP_NORMAL_READ) {
        return socket.readLine(length);
      } else {
        tempBuffer = TempBuffer.allocate();

        if (length > tempBuffer.getCapacity())
          length = tempBuffer.getCapacity();

        byte []buffer = tempBuffer.getBuffer();

        length = socket.read(buffer, 0, length);

        if (length > 0) {
          StringValue sb = env.createBinaryBuilder(buffer, 0, length);
          return sb;
        } else
          return BooleanValue.FALSE;
      }
    } catch (IOException e) {
      env.warning(e);

      return BooleanValue.FALSE;
    } finally {
      if (tempCharBuffer != null)
        TempCharBuffer.free(tempCharBuffer);

      if (tempBuffer != null)
        TempBuffer.free(tempBuffer);
    }
  }

  public static boolean socket_set_timeout(Env env,
                                           @NotNull Value stream,
                                           int seconds,
                                           @Optional("-1") int milliseconds)
  {
    return StreamModule.stream_set_timeout(env, stream, seconds, milliseconds);
  }

  public static Value socket_write(Env env,
                                   @NotNull SocketInputOutput socket,
                                   @NotNull InputStream is,
                                   @Optional("-1") int length)
  {
    if (is == null)
      return BooleanValue.FALSE;

    // php/4800
    if (length < 0)
      length = Integer.MAX_VALUE;

    try {
      int result = socket.write(is, length);

      if (result < 0)
        return BooleanValue.FALSE;
      else
        return LongValue.create(result);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Closes a socket.
   *
   * @param how 0 = read, 1 = write, 2 = both
   */
  public boolean socket_shutdown(Env env,
                                 @NotNull SocketInputOutput file,
                                 int how)
  {
    if (file == null)
      return false;

    switch (how) {
    case 0:
      file.closeRead();
      return true;

    case 1:
      file.closeWrite();
      return true;

    case 2:
      file.close();
      return true;

    default:
      return false;
    }
  }

  static {
    addConstant(_constMap ,"AF_UNIX", AF_UNIX);
    addConstant(_constMap, "AF_INET", AF_INET);
    addConstant(_constMap, "AF_INET6", AF_INET6);

    addConstant(_constMap, "SOCK_STREAM", SOCK_STREAM);
    addConstant(_constMap, "SOCK_DGRAM", SOCK_DGRAM);
    addConstant(_constMap, "SOCK_RAW", SOCK_RAW);
    addConstant(_constMap, "SOCK_SEQPACKET", SOCK_SEQPACKET);
    addConstant(_constMap, "SOCK_RDM", SOCK_RDM);

    addConstant(_constMap, "MSG_OOB", MSG_OOB);
    addConstant(_constMap, "MSG_WAITALL", MSG_WAITALL);
    addConstant(_constMap, "MSG_PEEK", MSG_PEEK);
    addConstant(_constMap, "MSG_DONTROUTE", MSG_DONTROUTE);

    addConstant(_constMap, "SO_DEBUG", SO_DEBUG);
    addConstant(_constMap, "SO_REUSEADDR", SO_REUSEADDR);
    addConstant(_constMap, "SO_KEEPALIVE", SO_KEEPALIVE);
    addConstant(_constMap, "SO_DONTROUTE", SO_DONTROUTE);
    addConstant(_constMap, "SO_LINGER", SO_LINGER);
    addConstant(_constMap, "SO_BROADCAST", SO_BROADCAST);
    addConstant(_constMap, "SO_OOBINLINE", SO_OOBINLINE);
    addConstant(_constMap, "SO_SNDBUF", SO_SNDBUF);
    addConstant(_constMap, "SO_RCVBUF", SO_RCVBUF);
    addConstant(_constMap, "SO_SNDLOWAT", SO_SNDLOWAT);
    addConstant(_constMap, "SO_RCVLOWAT", SO_RCVLOWAT);
    addConstant(_constMap, "SO_SNDTIMEO", SO_SNDTIMEO);
    addConstant(_constMap, "SO_RCVTIMEO", SO_RCVTIMEO);
    addConstant(_constMap, "SO_TYPE", SO_TYPE);
    addConstant(_constMap, "SO_ERROR", SO_ERROR);

    addConstant(_constMap, "SOL_SOCKET", SOL_SOCKET);

    addConstant(_constMap, "SOMAXCONN", SOMAXCONN);

    addConstant(_constMap, "PHP_NORMAL_READ", PHP_NORMAL_READ);
    addConstant(_constMap, "PHP_BINARY_READ", PHP_BINARY_READ);

    addConstant(_constMap, "SOCKET_EPERM", SOCKET_EPERM);
    addConstant(_constMap, "SOCKET_ENOENT", SOCKET_ENOENT);
    addConstant(_constMap, "SOCKET_EINTR", SOCKET_EINTR);
    addConstant(_constMap, "SOCKET_EIO", SOCKET_EIO);
    addConstant(_constMap, "SOCKET_ENXIO", SOCKET_ENXIO);
    addConstant(_constMap, "SOCKET_E2BIG", SOCKET_E2BIG);
    addConstant(_constMap, "SOCKET_EBADF", SOCKET_EBADF);
    addConstant(_constMap, "SOCKET_EAGAIN", SOCKET_EAGAIN);
    addConstant(_constMap, "SOCKET_ENOMEM", SOCKET_ENOMEM);
    addConstant(_constMap, "SOCKET_EACCES", SOCKET_EACCES);
    addConstant(_constMap, "SOCKET_EFAULT", SOCKET_EFAULT);
    addConstant(_constMap, "SOCKET_ENOTBLK", SOCKET_ENOTBLK);
    addConstant(_constMap, "SOCKET_EBUSY", SOCKET_EBUSY);
    addConstant(_constMap, "SOCKET_EEXIST", SOCKET_EEXIST);
    addConstant(_constMap, "SOCKET_EXDEV", SOCKET_EXDEV);
    addConstant(_constMap, "SOCKET_ENODEV", SOCKET_ENODEV);
    addConstant(_constMap, "SOCKET_ENOTDIR", SOCKET_ENOTDIR);
    addConstant(_constMap, "SOCKET_EISDIR", SOCKET_EISDIR);
    addConstant(_constMap, "SOCKET_EINVAL", SOCKET_EINVAL);
    addConstant(_constMap, "SOCKET_ENFILE", SOCKET_ENFILE);
    addConstant(_constMap, "SOCKET_EMFILE", SOCKET_EMFILE);
    addConstant(_constMap, "SOCKET_ENOTTY", SOCKET_ENOTTY);
    addConstant(_constMap, "SOCKET_ENOSPC", SOCKET_ENOSPC);
    addConstant(_constMap, "SOCKET_ESPIPE", SOCKET_ESPIPE);
    addConstant(_constMap, "SOCKET_EROFS", SOCKET_EROFS);
    addConstant(_constMap, "SOCKET_EMLINK", SOCKET_EMLINK);
    addConstant(_constMap, "SOCKET_EPIPE", SOCKET_EPIPE);
    addConstant(_constMap, "SOCKET_ENAMETOOLONG", SOCKET_ENAMETOOLONG);
    addConstant(_constMap, "SOCKET_ENOLCK", SOCKET_ENOLCK);
    addConstant(_constMap, "SOCKET_ENOSYS", SOCKET_ENOSYS);
    addConstant(_constMap, "SOCKET_ENOTEMPTY", SOCKET_ENOTEMPTY);
    addConstant(_constMap, "SOCKET_ELOOP", SOCKET_ELOOP);
    addConstant(_constMap, "SOCKET_EWOULDBLOCK", SOCKET_EWOULDBLOCK);
    addConstant(_constMap, "SOCKET_ENOMSG", SOCKET_ENOMSG);
    addConstant(_constMap, "SOCKET_EIDRM", SOCKET_EIDRM);
    addConstant(_constMap, "SOCKET_ECHRNG", SOCKET_ECHRNG);
    addConstant(_constMap, "SOCKET_EL2NSYNC", SOCKET_EL2NSYNC);
    addConstant(_constMap, "SOCKET_EL3HLT", SOCKET_EL3HLT);
    addConstant(_constMap, "SOCKET_EL3RST", SOCKET_EL3RST);
    addConstant(_constMap, "SOCKET_ELNRNG", SOCKET_ELNRNG);
    addConstant(_constMap, "SOCKET_EUNATCH", SOCKET_EUNATCH);
    addConstant(_constMap, "SOCKET_ENOCSI", SOCKET_ENOCSI);
    addConstant(_constMap, "SOCKET_EL2HLT", SOCKET_EL2HLT);
    addConstant(_constMap, "SOCKET_EBADE", SOCKET_EBADE);
    addConstant(_constMap, "SOCKET_EBADR", SOCKET_EBADR);
    addConstant(_constMap, "SOCKET_EXFULL", SOCKET_EXFULL);
    addConstant(_constMap, "SOCKET_ENOANO", SOCKET_ENOANO);
    addConstant(_constMap, "SOCKET_EBADRQC", SOCKET_EBADRQC);
    addConstant(_constMap, "SOCKET_EBADSLT", SOCKET_EBADSLT);
    addConstant(_constMap, "SOCKET_ENOSTR", SOCKET_ENOSTR);
    addConstant(_constMap, "SOCKET_ENODATA", SOCKET_ENODATA);
    addConstant(_constMap, "SOCKET_ETIME", SOCKET_ETIME);
    addConstant(_constMap, "SOCKET_ENOSR", SOCKET_ENOSR);
    addConstant(_constMap, "SOCKET_ENONET", SOCKET_ENONET);
    addConstant(_constMap, "SOCKET_EREMOTE", SOCKET_EREMOTE);
    addConstant(_constMap, "SOCKET_ENOLINK", SOCKET_ENOLINK);
    addConstant(_constMap, "SOCKET_EADV", SOCKET_EADV);
    addConstant(_constMap, "SOCKET_ESRMNT", SOCKET_ESRMNT);
    addConstant(_constMap, "SOCKET_ECOMM", SOCKET_ECOMM);
    addConstant(_constMap, "SOCKET_EPROTO", SOCKET_EPROTO);
    addConstant(_constMap, "SOCKET_EMULTIHOP", SOCKET_EMULTIHOP);
    addConstant(_constMap, "SOCKET_EBADMSG", SOCKET_EBADMSG);
    addConstant(_constMap, "SOCKET_ENOTUNIQ", SOCKET_ENOTUNIQ);
    addConstant(_constMap, "SOCKET_EBADFD", SOCKET_EBADFD);
    addConstant(_constMap, "SOCKET_EREMCHG", SOCKET_EREMCHG);
    addConstant(_constMap, "SOCKET_ERESTART", SOCKET_ERESTART);
    addConstant(_constMap, "SOCKET_ESTRPIPE", SOCKET_ESTRPIPE);
    addConstant(_constMap, "SOCKET_EUSERS", SOCKET_EUSERS);
    addConstant(_constMap, "SOCKET_ENOTSOCK", SOCKET_ENOTSOCK);
    addConstant(_constMap, "SOCKET_EDESTADDRREQ", SOCKET_EDESTADDRREQ);
    addConstant(_constMap, "SOCKET_EMSGSIZE", SOCKET_EMSGSIZE);
    addConstant(_constMap, "SOCKET_EPROTOTYPE", SOCKET_EPROTOTYPE);
    addConstant(_constMap, "SOCKET_ENOPROTOOPT", SOCKET_ENOPROTOOPT);
    addConstant(_constMap, "SOCKET_EPROTONOSUPPORT",
        SOCKET_EPROTONOSUPPORT);
    addConstant(_constMap, "SOCKET_ESOCKTNOSUPPORT",
        SOCKET_ESOCKTNOSUPPORT);
    addConstant(_constMap, "SOCKET_EOPNOTSUPP", SOCKET_EOPNOTSUPP);
    addConstant(_constMap, "SOCKET_EPFNOSUPPORT", SOCKET_EPFNOSUPPORT);
    addConstant(_constMap, "SOCKET_EAFNOSUPPORT", SOCKET_EAFNOSUPPORT);
    addConstant(_constMap, "SOCKET_EADDRINUSE", SOCKET_EADDRINUSE);
    addConstant(_constMap, "SOCKET_EADDRNOTAVAIL",
        SOCKET_EADDRNOTAVAIL);
    addConstant(_constMap, "SOCKET_ENETDOWN", SOCKET_ENETDOWN);
    addConstant(_constMap, "SOCKET_ENETUNREACH", SOCKET_ENETUNREACH);
    addConstant(_constMap, "SOCKET_ENETRESET", SOCKET_ENETRESET);
    addConstant(_constMap, "SOCKET_ECONNABORTED", SOCKET_ECONNABORTED);
    addConstant(_constMap, "SOCKET_ECONNRESET", SOCKET_ECONNRESET);
    addConstant(_constMap, "SOCKET_ENOBUFS", SOCKET_ENOBUFS);
    addConstant(_constMap, "SOCKET_EISCONN", SOCKET_EISCONN);
    addConstant(_constMap, "SOCKET_ENOTCONN", SOCKET_ENOTCONN);
    addConstant(_constMap, "SOCKET_ESHUTDOWN", SOCKET_ESHUTDOWN);
    addConstant(_constMap, "SOCKET_ETOOMANYREFS", SOCKET_ETOOMANYREFS);
    addConstant(_constMap, "SOCKET_ETIMEDOUT", SOCKET_ETIMEDOUT);
    addConstant(_constMap, "SOCKET_ECONNREFUSED", SOCKET_ECONNREFUSED);
    addConstant(_constMap, "SOCKET_EHOSTDOWN", SOCKET_EHOSTDOWN);
    addConstant(_constMap, "SOCKET_EHOSTUNREACH", SOCKET_EHOSTUNREACH);
    addConstant(_constMap, "SOCKET_EALREADY", SOCKET_EALREADY);
    addConstant(_constMap, "SOCKET_EINPROGRESS", SOCKET_EINPROGRESS);
    addConstant(_constMap, "SOCKET_EISNAM", SOCKET_EISNAM);
    addConstant(_constMap, "SOCKET_EREMOTEIO", SOCKET_EREMOTEIO);
    addConstant(_constMap, "SOCKET_EDQUOT", SOCKET_EDQUOT);
    addConstant(_constMap, "SOCKET_ENOMEDIUM", SOCKET_ENOMEDIUM);
    addConstant(_constMap, "SOCKET_EMEDIUMTYPE", SOCKET_EMEDIUMTYPE);

    addConstant(_constMap, "SOL_TCP", SOL_TCP);
    addConstant(_constMap, "SOL_UDP", SOL_UDP);
  }
}

