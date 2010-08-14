/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

#ifdef WIN32
#ifndef _WINSOCKAPI_ 
#define _WINSOCKAPI_
#endif 
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <io.h>
#else
#include <sys/param.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/resource.h>
#include <dirent.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#ifdef EPOLL
#include <sys/epoll.h>
#endif
#ifdef POLL
#include <sys/poll.h>
#else
#include <sys/select.h>
#endif
#include <pwd.h>
#include <syslog.h>
#include <netdb.h>
#endif

#ifdef linux
#include <linux/version.h>
#endif

#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
/* probably system-dependent */
#include <jni.h>

#include "resin.h"

#define STACK_BUFFER_SIZE (16 * 1024)

void
cse_log(char *fmt, ...)
{
#ifdef DEBUG  
  va_list list;

  va_start(list, fmt);
  vfprintf(stderr, fmt, list);
  va_end(list);
#endif
}

static char *
q_strdup(char *str)
{
  size_t len = strlen(str);
  char *dup = cse_malloc(len + 1);

  strcpy(dup, str);

  return dup;
}

static int
set_byte_array_region(JNIEnv *env, jbyteArray j_buf, jint offset, jint sublen,
		      char *c_buf)
{
  (*env)->SetByteArrayRegion(env, j_buf, offset, sublen, (void*) c_buf);
  
  return 1;
}

static int
get_byte_array_region(JNIEnv *env, jbyteArray buf, jint offset, jint sublen,
		      char *buffer)
{
  (*env)->GetByteArrayRegion(env, buf, offset, sublen, (void*) buffer);
  
  return 1;
}

JNIEXPORT jlong JNICALL
Java_com_caucho_vfs_JniSocketImpl_nativeAllocate(JNIEnv *env,
						 jobject obj)
{
  connection_t *conn;

  conn = (connection_t *) malloc(sizeof(connection_t));
  
  memset(conn, 0, sizeof(connection_t));
  conn->fd = -1;
  conn->client_sin = (struct sockaddr *) conn->client_data;
  conn->server_sin = (struct sockaddr *) conn->server_data;

  conn->ops = &std_ops;

#ifdef WIN32
  // conn->event = WSACreateEvent();
#endif

  return (jlong) (PTR) conn;
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniSocketImpl_readNative(JNIEnv *env,
					     jobject obj,
					     jlong conn_fd,
					     jbyteArray buf,
					     jint offset,
					     jint length,
					     jlong timeout)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  int sublen;
  char buffer[STACK_BUFFER_SIZE];

  if (! conn || conn->fd < 0)
    return -1;

  conn->jni_env = env;

  if (length < STACK_BUFFER_SIZE)
    sublen = length;
  else
    sublen = STACK_BUFFER_SIZE;

  sublen = conn->ops->read(conn, buffer, sublen, (int) timeout);

  /* Should probably have a different response for EINTR */
  if (sublen < 0) {
    return sublen;
  }

  set_byte_array_region(env, buf, offset, sublen, buffer);

  return sublen;
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniStream_readNonBlockNative(JNIEnv *env,
						 jobject obj,
						 jlong conn_fd,
						 jbyteArray buf,
						 jint offset,
						 jint length)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  int sublen;
  char buffer[STACK_BUFFER_SIZE];

  if (! conn || conn->fd < 0)
    return -1;

  conn->jni_env = env;

  if (length < STACK_BUFFER_SIZE)
    sublen = length;
  else
    sublen = STACK_BUFFER_SIZE;

  sublen = conn->ops->read_nonblock(conn, buffer, sublen);

  /* Should probably have a different response for EINTR */
  if (sublen < 0)
    return sublen;

  set_byte_array_region(env, buf, offset, sublen, buffer);

  return sublen;
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniSocketImpl_writeNative(JNIEnv *env,
					      jobject obj,
					      jlong conn_fd,
					      jbyteArray buf,
					      jint offset,
					      jint length)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  char buffer[STACK_BUFFER_SIZE];
  int sublen;
  int write_length = 0;

  if (! conn || conn->fd < 0 || ! buf)
    return -1;
  
  conn->jni_env = env;

  while (length > 0) {
    int result;
    
    if (length < sizeof(buffer))
      sublen = length;
    else
      sublen = sizeof(buffer);

    get_byte_array_region(env, buf, offset, sublen, buffer);
    
    result = conn->ops->write(conn, buffer, sublen);
    
    if (result < 0) {
      return result;
    }

    length -= result;
    offset += result;
    write_length += result;
  }

  return write_length;
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniSocketImpl_writeNative2(JNIEnv *env,
					       jobject obj,
					       jlong conn_fd,
					       jbyteArray buf1,
					       jint off1,
					       jint len1,
					       jbyteArray buf2,
					       jint off2,
					       jint len2)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  char buffer[2 * STACK_BUFFER_SIZE];
  int sublen;
  int buffer_offset;
  int write_length = 0;

  buffer_offset = 0;

  if (! conn || conn->fd < 0 || ! buf1 || ! buf2)
    return -1;
  
  conn->jni_env = env;

  while (sizeof(buffer) < len1) {
    sublen = sizeof(buffer);
    
    get_byte_array_region(env, buf1, off1, sublen, buffer);
      
    sublen = conn->ops->write(conn, buffer, sublen);

    if (sublen < 0) {
      /* XXX: probably should throw exception */
      return sublen;
    }

    len1 -= sublen;
    off1 += sublen;
    write_length += sublen;
  }

  get_byte_array_region(env, buf1, off1, len1, buffer);
  buffer_offset = len1;

  while (buffer_offset + len2 > 0) {
    int result;
    
    if (len2 < sizeof(buffer) - buffer_offset)
      sublen = len2;
    else
      sublen = sizeof(buffer) - buffer_offset;

    get_byte_array_region(env, buf2, off2, sublen,
			       buffer + buffer_offset);
      
    result = conn->ops->write(conn, buffer, buffer_offset + sublen);

    if (result < 0) {
      /* XXX: probably should throw exception */
      return result;
    }

    len2 -= sublen;
    off2 += sublen;
    write_length += sublen + buffer_offset;
    buffer_offset = 0;
  }

  return write_length;
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniSocketImpl_flushNative(JNIEnv *env,
					      jobject obj,
					      jlong conn_fd)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;

  if (! conn)
    return -1;
  else
    return 0;

  /* return cse_flush_request(res); */
}

/**
 * Force an interrupt so listening threads will close
 */
JNIEXPORT void JNICALL
Java_com_caucho_vfs_JniSocketImpl_nativeCloseFd(JNIEnv *env,
						jobject obj,
						jlong conn_fd)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  int fd = -1;

  if (conn) {
    fd = conn->fd;
    conn->fd = -1;
  }

  if (fd >= 0) {
    closesocket(fd);
  }
}

JNIEXPORT void JNICALL
Java_com_caucho_vfs_JniSocketImpl_nativeClose(JNIEnv *env,
					      jobject obj,
					      jlong conn_fd)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;

  if (conn && conn->fd >= 0) {
    conn->jni_env = env;

    conn->ops->close(conn);
  }
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniSocketImpl_writeCloseNative(JNIEnv *env,
                                                   jobject obj,
                                                   jlong conn_fd,
                                                   jbyteArray buf,
                                                   jint offset,
                                                   jint length)
{
  int value;

  value = Java_com_caucho_vfs_JniSocketImpl_writeNative(env, obj, conn_fd,
                                                        buf, offset, length);

  Java_com_caucho_vfs_JniSocketImpl_nativeClose(env, obj, conn_fd);

  return value;
}

JNIEXPORT void JNICALL
Java_com_caucho_vfs_JniSocketImpl_nativeFree(JNIEnv *env,
					     jobject obj,
					     jlong conn_fd)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;

  if (conn) {
    if (conn->fd >= 0) {
      conn->jni_env = env;

      conn->ops->close(conn);
    }

#ifdef WIN32
	  /*
    if (conn->event)
      WSACloseEvent(conn->event);
	  */
#endif
    
    free(conn);
  }
}

JNIEXPORT jboolean JNICALL
Java_com_caucho_vfs_JniSocketImpl_isSecure(JNIEnv *env,
                                        jobject obj,
                                        jlong conn_fd)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;

  if (! conn)
    return 0;
  
  return conn->sock != 0 && conn->ssl_cipher != 0;
}

JNIEXPORT jstring JNICALL
Java_com_caucho_vfs_JniSocketImpl_getCipher(JNIEnv *env,
                                            jobject obj,
                                            jlong conn_fd)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;

  if (! conn || ! conn->sock || ! conn->ssl_cipher)
    return 0;
  
  return (*env)->NewStringUTF(env, conn->ssl_cipher);
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniSocketImpl_getCipherBits(JNIEnv *env,
					     jobject obj,
					     jlong conn_fd)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;

  if (! conn || ! conn->sock)
    return 0;
  else
    return conn->ssl_bits;
}

#ifdef POLL
JNIEXPORT jboolean JNICALL
Java_com_caucho_vfs_JniSocketImpl_nativeReadNonBlock(JNIEnv *env,
                                                  jobject obj,
                                                  jlong conn_fd,
						  jint ms)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  struct pollfd poll_item[1];
  int fd;

  if (! conn)
    return 0;

  fd = conn->fd;

  if (fd < 0)
    return 0;

  poll_item[0].fd = fd;
  poll_item[0].events = POLLIN|POLLPRI;
  poll_item[0].revents = 0;

  return (poll(poll_item, 1, ms) > 0);
}
#else /* SELECT */
JNIEXPORT jboolean JNICALL
Java_com_caucho_vfs_JniSocketImpl_nativeReadNonBlock(JNIEnv *env,
                                                  jobject obj,
                                                  jlong conn_fd,
						  jint ms)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  fd_set read_set;
  struct timeval timeout;
  int result;
  int fd;

  if (! conn)
    return 0;

  fd = conn->fd;

  if (fd < 0)
    return 0;

  FD_ZERO(&read_set);
  FD_SET((unsigned int) fd, &read_set);

  timeout.tv_sec = ms / 1000;
  timeout.tv_usec = (ms % 1000) * 1000;

  result = select(fd + 1, &read_set, 0, 0, &timeout);

  return result > 0;
}
#endif

#ifdef AI_NUMERICHOST

static struct sockaddr_in *
lookup_addr(JNIEnv *env, char *addr_name, int port,
	    char *buffer, int *p_family, int *p_protocol,
	    int *p_sin_length)
{
  struct addrinfo hints;
  struct addrinfo *addr;
  struct sockaddr_in *sin;
  int sin_length;
  char port_name[16];
  
  memset(&hints, 0, sizeof(hints));

  hints.ai_socktype = SOCK_STREAM;
  hints.ai_family = PF_UNSPEC;
  hints.ai_flags = AI_NUMERICHOST;

  sprintf(port_name, "%d", port);

  if (getaddrinfo(addr_name, port_name, &hints, &addr)) {
    resin_printf_exception(env, "java/net/SocketException", "can't find address %s", addr_name);
    return 0;
  }

  *p_family = addr->ai_family;
  *p_protocol = addr->ai_protocol;
  sin_length = addr->ai_addrlen;
  memcpy(buffer, addr->ai_addr, sin_length);
  sin = (struct sockaddr_in *) buffer;
  freeaddrinfo(addr);

  *p_sin_length = sin_length;

  return sin;
}

#else

static struct sockaddr_in *
lookup_addr(JNIEnv *env, char *addr_name, int port,
	    char *buffer, int *p_family, int *p_protocol, int *p_sin_length)
{
  struct sockaddr_in *sin = (struct sockaddr_in *) buffer;
  
  memset(sin, 0, sizeof(struct sockaddr_in));

  *p_sin_length = sizeof(struct sockaddr_in);
  
  sin->sin_family = AF_INET;
  *p_family = AF_INET;
  *p_protocol = 0;

  sin->sin_addr.s_addr = inet_addr(addr_name);
 
  sin->sin_port = htons((unsigned short) port);

  return sin;
}

#endif

static void
init_server_socket(JNIEnv *env, server_socket_t *ss)
{
  jclass jniServerSocketClass;
  
  jniServerSocketClass = (*env)->FindClass(env, "com/caucho/vfs/JniSocketImpl");

  if (jniServerSocketClass) {
    ss->_isSecure = (*env)->GetFieldID(env, jniServerSocketClass,
				       "_isSecure", "Z");
    if (! ss->_isSecure)
      resin_throw_exception(env, "com/caucho/config/ConfigException",
			    "can't load _isSecure field");
      
    /*
    ss->_localAddrBuffer = (*env)->GetFieldID(env, jniServerSocketClass,
					      "_localAddrBuffer", "[B");
    if (! ss->_localAddrBuffer)
      resin_throw_exception(env, "com/caucho/config/ConfigException",
			    "can't load _localAddrBuffer field");
    
    ss->_localAddrLength = (*env)->GetFieldID(env, jniServerSocketClass,
					      "_localAddrLength", "I");
    if (! ss->_localAddrLength)
      resin_throw_exception(env, "com/caucho/config/ConfigException",
			    "can't load _localAddrLength field");
    */
    
    ss->_localPort = (*env)->GetFieldID(env, jniServerSocketClass,
					"_localPort", "I");
    if (! ss->_localPort)
      resin_throw_exception(env, "com/caucho/config/ConfigException",
			    "can't load _localPort field");
      
    /*
    ss->_remoteAddrBuffer = (*env)->GetFieldID(env, jniServerSocketClass,
					       "_remoteAddrBuffer", "[B");
    if (! ss->_remoteAddrBuffer)
      resin_throw_exception(env, "com/caucho/config/ConfigException",
			    "can't load _remoteAddrBuffer field");
    
    ss->_remoteAddrLength = (*env)->GetFieldID(env, jniServerSocketClass,
					      "_remoteAddrLength", "I");
    if (! ss->_remoteAddrLength)
      resin_throw_exception(env, "com/caucho/config/ConfigException",
			    "can't load _remoteAddrLength field");
    */
    
    ss->_remotePort = (*env)->GetFieldID(env, jniServerSocketClass,
					 "_remotePort", "I");
    if (! ss->_remotePort)
      resin_throw_exception(env, "com/caucho/config/ConfigException",
			    "can't load _remotePort field");
      
  }
}

JNIEXPORT jlong JNICALL
Java_com_caucho_vfs_JniServerSocketImpl_bindPort(JNIEnv *env,
						 jobject obj,
						 jstring jaddr,
						 jint port)
{
  int val = 0;
  char addr_name[256];
  const char *temp_string = 0;
  int sock;
  int family = 0;
  int protocol = 0;
  server_socket_t *ss;
  char sin_data[256];
  struct sockaddr_in *sin = (struct sockaddr_in *) sin_data;
  int sin_length = sizeof(sin_data);

#ifdef WIN32
  {
	  WSADATA data;
	  WORD version = MAKEWORD(2,2);
	  WSAStartup(version, &data);
  }
#endif
  
  addr_name[0] = 0;
  memset(sin_data, 0, sizeof(sin_data));

  if (jaddr != 0) {
    temp_string = (*env)->GetStringUTFChars(env, jaddr, 0);
  
    if (temp_string) {
      strncpy(addr_name, temp_string, sizeof(addr_name));
      addr_name[sizeof(addr_name) - 1] = 0;
  
      (*env)->ReleaseStringUTFChars(env, jaddr, temp_string);
    }
    else {
      resin_throw_exception(env, "java/lang/NullPointerException", "missing addr");
      return 0;
    }

    sin = lookup_addr(env, addr_name, port, sin_data,
                      &family, &protocol, &sin_length);
  }
  else {
    sin = (struct sockaddr_in *) sin_data;
    sin->sin_family = AF_INET;
    sin->sin_port = htons(port);
    family = AF_INET;
    protocol = IPPROTO_TCP;
    sin_length = sizeof(struct sockaddr_in);
  }
  
  if (! sin)
    return 0;

  sock = socket(family, SOCK_STREAM, 0);
  if (sock < 0) {
    return 0;
  }
  
  val = 1;
  if (setsockopt(sock, SOL_SOCKET, SO_REUSEADDR,
		 (char *) &val, sizeof(int)) < 0) {
    closesocket(sock);
    return 0;
  }

  if (bind(sock, (struct sockaddr *) sin, sin_length) < 0) {
    int i = 5;
    int result = 0;
    
    /* somewhat of a hack to clear the old connection. */
    while (result == 0 && i-- >= 0) {
      int fd = socket(AF_INET, SOCK_STREAM, 0);
      result = connect(fd, (struct sockaddr *) &sin, sizeof(sin));
      closesocket(fd);
    }

    result = -1;
    for (i = 50; result < 0 && i >= 0; i--) {
      result = bind(sock, (struct sockaddr *) sin, sin_length);

      if (result < 0) {
	struct timeval tv;

	tv.tv_sec = 0;
	tv.tv_usec = 100000;

	select(0, 0, 0, 0, &tv);
      }
    }

    if (result < 0) {
      closesocket(sock);
      return 0;
    }
  }

  sin_length = sizeof(sin_data);
  getsockname(sock, (struct sockaddr *) sin, &sin_length);

  /* must be 0 if the poll is missing for accept */
#if 0 && defined(O_NONBLOCK)
  /*
   * sets nonblock to ensure the timeout work in the case of multiple threads.
   */
  {
    int flags;
    int result;
    
    flags = fcntl(sock, F_GETFL);
    result = fcntl(sock, F_SETFL, O_NONBLOCK|flags);
  }
#endif

  ss = (server_socket_t *) cse_malloc(sizeof(server_socket_t));
  memset(ss, 0, sizeof(server_socket_t));

  ss->fd = sock;
  ss->port = ntohs(sin->sin_port);

  ss->conn_socket_timeout = 65000;

  ss->accept = &std_accept;
  ss->close = &std_close_ss;

#ifdef WIN32
  ss->accept_lock = CreateMutex(0, 0, 0);
  ss->ssl_lock = CreateMutex(0, 0, 0);
#endif

  init_server_socket(env, ss);
  
  return (PTR) ss;
}

JNIEXPORT jlong JNICALL
Java_com_caucho_vfs_JniServerSocketImpl_nativeOpenPort(JNIEnv *env,
						       jobject obj,
						       jint sock,
						       jint port)
{
  server_socket_t *ss;

#ifdef WIN32
  {
	  WSADATA data;
	  WORD version = MAKEWORD(2,2);
	  WSAStartup(version, &data);
  }
#endif

  if (sock < 0)
    return 0;

  ss = (server_socket_t *) cse_malloc(sizeof(server_socket_t));

  if (ss == 0)
    return 0;
  
  memset(ss, 0, sizeof(server_socket_t));

  ss->fd = sock;
  ss->port = port;
  
  ss->conn_socket_timeout = 65000;

  ss->accept = &std_accept;
  ss->close = &std_close_ss;

#ifdef WIN32
  ss->accept_lock = CreateMutex(0, 0, 0);
  ss->ssl_lock = CreateMutex(0, 0, 0);
#endif
  
  init_server_socket(env, ss);
  
  return (PTR) ss;
}

JNIEXPORT void JNICALL
Java_com_caucho_vfs_JniServerSocketImpl_nativeSetConnectionSocketTimeout(JNIEnv *env,
						       jobject obj,
						       jlong ss_fd,
						       jint timeout)
{
  server_socket_t *ss = (server_socket_t *) (PTR) ss_fd;

  if (! ss)
    return;

  if (timeout < 0)
    timeout = 600 * 1000;
  else if (timeout < 500)
    timeout = 500;
  
  ss->conn_socket_timeout = timeout;
}

JNIEXPORT void JNICALL
Java_com_caucho_vfs_JniServerSocketImpl_nativeListen(JNIEnv *env,
						     jobject obj,
						     jlong ss_fd,
						     jint backlog)
{
  server_socket_t *ss = (server_socket_t *) (PTR) ss_fd;

  if (! ss || ss->fd < 0)
    return;

  if (backlog < 0)
    backlog = 0;

  if (backlog < 0)
    listen(ss->fd, 100);
  else
    listen(ss->fd, backlog);
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniServerSocketImpl_getLocalPort(JNIEnv *env,
                                                  jobject obj,
                                                  jlong ss)
{
  server_socket_t *socket = (server_socket_t *) (PTR) ss;

  if (socket) {
    return socket->port;
  }
  else
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniServerSocketImpl_nativeGetSystemFD(JNIEnv *env,
							  jobject obj,
							  jlong ss)
{
  server_socket_t *socket = (server_socket_t *) (PTR) ss;

  if (! socket)
    return -1;
  else
    return socket->fd;
}

JNIEXPORT jboolean JNICALL
Java_com_caucho_vfs_JniServerSocketImpl_nativeSetSaveOnExec(JNIEnv *env,
							    jobject obj,
							    jlong ss)
{
#ifdef WIN32
  return 0;
#else
  server_socket_t *socket = (server_socket_t *) (PTR) ss;

  if (! socket)
    return 0;
  else {
    int fd = socket->fd;
    int arg = 0;
    int result = 0;

    if (fd < 0)
      return 0;

    /* sets the close on exec flag */
    arg = fcntl(fd, F_GETFD, 0);
    arg &= ~FD_CLOEXEC;

    result = fcntl(fd, F_SETFD, arg);

    return result >= 0;
  }
#endif
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniServerSocketImpl_closeNative(JNIEnv *env,
                                                 jobject obj,
                                                 jlong ss)
{
  server_socket_t *socket = (server_socket_t *) (PTR) ss;

  if (! socket)
    return 0;

  socket->close(socket);

  cse_free(socket);

  return 0;
}

#if ! defined(AF_INET6)
static int
get_address(struct sockaddr *addr, char *dst, int length)
{
  struct sockaddr_in *sin = (struct sockaddr_in *) addr;

  if (! sin)
    return 0;
  
  memset(dst, 0, 10);
  dst[10] = 0xff;
  dst[11] = 0xff;
  memcpy(dst + 12, sin->sin_addr, 4);

  return 4;
}
#else

static int
get_address(struct sockaddr *addr, char *dst, int length)
{
  struct sockaddr_in *sin = (struct sockaddr_in *) addr;
  const char *result;
  
  if (sin->sin_family == AF_INET6) {
    struct sockaddr_in6 *sin6 = (struct sockaddr_in6 *) sin;
    struct in6_addr *sin6_addr = &sin6->sin6_addr;

    memcpy(dst, sin6_addr, 16);

    return 6;
  }
  else {
    memset(dst, 0, 10);
    dst[10] = 0xff;
    dst[11] = 0xff;
    memcpy(dst + 12, (char *) &sin->sin_addr, 4);

    return 4;
  }
}
#endif

static void
socket_fill_address(JNIEnv *env, jobject obj,
                    server_socket_t *ss,
                    connection_t *conn,
                    jbyteArray local_addr,
                    jbyteArray remote_addr)
{
  char temp_buf[1024];
  struct sockaddr_in *sin;

  if (ss->_isSecure) {
    jboolean is_secure = conn->sock != 0 && conn->ssl_cipher != 0;
    
    (*env)->SetBooleanField(env, obj, ss->_isSecure, is_secure);
  }

  if (local_addr) {
    /* the 16 must match JniSocketImpl 16 bytes ipv6 */
    get_address(conn->server_sin, temp_buf, 16);

    set_byte_array_region(env, local_addr, 0, 16, temp_buf);
  }

  if (ss->_localPort) {
    jint local_port;

    sin = (struct sockaddr_in *) conn->server_sin;
    local_port = ntohs(sin->sin_port);

    (*env)->SetIntField(env, obj, ss->_localPort, local_port);
  }

  if (remote_addr) {
    /* the 16 must match JniSocketImpl 16 bytes ipv6 */
    get_address(conn->client_sin, temp_buf, 16);

    set_byte_array_region(env, remote_addr, 0, 16, temp_buf);
  }

  if (ss->_remotePort) {
	jint remote_port;

    sin = (struct sockaddr_in *) conn->client_sin;
    remote_port = ntohs(sin->sin_port);

    (*env)->SetIntField(env, obj, ss->_remotePort, remote_port);
  }
}

JNIEXPORT jboolean JNICALL
Java_com_caucho_vfs_JniSocketImpl_nativeAccept(JNIEnv *env,
                                               jobject obj,
                                               jlong ss_fd,
                                               jlong conn_fd,
                                               jbyteArray local_addr,
                                               jbyteArray remote_addr)
{
  server_socket_t *ss = (server_socket_t *) (PTR) ss_fd;
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  jboolean value;

  if (! ss || ! conn || ! env || ! obj)
    return 0;

  if (conn->fd >= 0) {
    conn->jni_env = env;

    conn->ops->close(conn);
  }

  if (! ss->accept(ss, conn))
    return 0;

  socket_fill_address(env, obj, ss, conn, local_addr, remote_addr);

  return 1;
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniSocketImpl_getClientCertificate(JNIEnv *env,
                                                    jobject obj,
                                                    jlong conn_fd,
                                                    jbyteArray buf,
                                                    jint offset,
                                                    jint length)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;
  int sublen;
  char buffer[8192];

  if (! conn)
    return -1;

  if (length < 8192)
    sublen = length;
  else
    sublen = 8192;

  sublen = conn->ops->read_client_certificate(conn, buffer, sublen);

  /* Should probably have a different response for EINTR */
  if (sublen < 0 || length < sublen)
    return sublen;

  set_byte_array_region(env, buf, offset, sublen, buffer);

  return sublen;
}

JNIEXPORT jint JNICALL
Java_com_caucho_vfs_JniSocketImpl_getNativeFd(JNIEnv *env,
                                           jobject obj,
                                           jlong conn_fd)
{
  connection_t *conn = (connection_t *) (PTR) conn_fd;

  if (! conn)
    return -1;
  else
    return conn->fd;
}
