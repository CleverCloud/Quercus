/*
 * Copyright (c) 1999-2008 Caucho Technology.  All rights reserved.
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

/*
 * SSL client certificate contributed by Ahn Le
 */

/*
 * Anh: We have to define _WIN32_WINNT as 0x0400 to have access to
 * the Crypto API.  By definning as this way, the dll can only work
 * with Windows 95 OEM Service Release 2 or above.
 */ 
#define _WIN32_WINNT 0x0400

#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <windows.h>
#include "httpext.h"
#include <errno.h>
#include <ctype.h>
#include <string.h>

#define ISAPI_SCRIPT "/scripts/isapi_srun.dll"

extern "C" {
#include "../common/cse.h"
#include "../common/version.h"
}

extern "C" {

void
cse_error(config_t *config, char *fmt, ...)
{
	va_list arg;
	char buf[1024];

	va_start(arg, fmt);
	if (fmt)
		vsprintf(buf, fmt, arg);
	else
		buf[0] = 0;

	va_end(arg);

	if (buf[0]) {
		config->error = strdup(buf);
                config->enable_caucho_status = 1;
        }
#ifdef DEBUG
	{
		FILE *file;
	    file = fopen("/temp/isapi.log", "a+b");
		fprintf(file, "%s\n", buf);
		fclose(file);
	}
#endif
}

void *
cse_malloc(int size)
{
  return malloc(size);
}

void cse_free(config_t *config, void *value) {}

void
cse_set_socket_cleanup(int socket, void *pool)
{
}

void
cse_kill_socket_cleanup(int socket, void *pool)
{
}

}

void
cse_log(char *fmt, ...)
{
#ifdef DEBUG
	va_list arg;
	FILE *file;
	file = fopen("/temp/isapi.log", "a+");
	va_start(arg, fmt);
	if (file)
		vfprintf(file, fmt, arg);
	va_end(arg);
	fclose(file);
#endif
}

void *
cse_create_lock(config_t *config)
{
	return CreateMutex(0, false, 0);
}

void
cse_free_lock(config_t *config, void *lock)
{
}


int
cse_lock(void *lock)
{
	if (lock) {
		WaitForSingleObject(lock, INFINITE);
	}

	return 1;
}

void
cse_unlock(void *lock)
{
	if (lock) {
		ReleaseMutex(lock);
	}
}

static void
cse_printf(EXTENSION_CONTROL_BLOCK *r, char *fmt, ...)
{
  va_list args;
  char buf[4096];
  unsigned long len;

  va_start(args, fmt);
  vsprintf(buf, fmt, args);

  va_end(args);

  len = strlen(buf);

  r->WriteClient(r->ConnID, buf, &len, 0);
}

static void
cse_pad(EXTENSION_CONTROL_BLOCK *r)
{
  cse_printf(r, "\n\n\n\n");
  cse_printf(r, "<!--\n");
  cse_printf(r, "   - Unfortunately, Microsoft has added a clever new\n");
  cse_printf(r, "   - \"feature\" to Internet Explorer.  If the text in\n");
  cse_printf(r, "   - an error's message is \"too small\", specifically\n");
  cse_printf(r, "   - less than 512 bytes, Internet Explorer returns\n");
  cse_printf(r, "   - its own error message.  Yes, you can turn that\n");
  cse_printf(r, "   - off, but *surprise* it's pretty tricky to find\n");
  cse_printf(r, "   - buried as a switch called \"smart error\n");
  cse_printf(r, "   - messages\"  That means, of course, that many of\n");
  cse_printf(r, "   - Resin's error messages are censored by default.\n");
  cse_printf(r, "   - And, of course, you'll be shocked to learn that\n");
  cse_printf(r, "   - IIS always returns error messages that are long\n");
  cse_printf(r, "   - enough to make Internet Explorer happy.  The\n");
  cse_printf(r, "   - workaround is pretty simple: pad the error\n");
  cse_printf(r, "   - message with a big comment to push it over the\n");
  cse_printf(r, "   - five hundred and twelve byte minimum.  Of course,\n");
  cse_printf(r, "   - that's exactly what you're reading right now.\n");
  cse_printf(r, "   -->\n");
}


static int
connection_error(config_t *config, EXTENSION_CONTROL_BLOCK *r)
{
  // r->content_type = "text/html";
  // ap_send_http_header(r);
  char *hostname = 0;
  int port = 0;

  if (config->error_page && config->error_page[0]) {
	  DWORD size = strlen(config->error_page);
	  DWORD type = 0;

          cse_printf(r, "HTTP/1.0 302 Redirect\r\n");
          cse_printf(r, "Location: %s\r\n", config->error_page);
          cse_printf(r, "\r\n");
          
	  return 1;
  }

  cse_printf(r, "HTTP/1.0 503 Busy\n\n");
  cse_printf(r, "<html><body bgcolor='white'>\n");
  cse_printf(r, "<h1>Server is currently unavailable or down for maintenance\n");
  cse_printf(r, "</h1>\n");
  cse_printf(r, "</body></html>\n");
  
  cse_pad(r);

   return 1;
}

static int
cse_error(config_t *config, EXTENSION_CONTROL_BLOCK *r)
{
  // r->content_type = "text/html";
  // ap_send_http_header(r);

  if (config->error_page) {
	  DWORD size = strlen(config->error_page);
	  DWORD type = 0;

		cse_printf(r, "HTTP/1.0 302 Redirect\n");
		cse_printf(r, "Location: %s\n", config->error_page);
		cse_printf(r, "\n");

		return 1;
  }

  cse_printf(r, "HTTP/1.0 500 Server Error\n\n");
  cse_printf(r, "<html><body bgcolor='white'>\n");
  cse_printf(r, "<h1>Can't access URL</h1>\n");
  if (config->error)
	  cse_printf(r, "<pre>%s</pre>", config->error);
  cse_printf(r, "</body></html>\n");
  
  cse_pad(r);

   return 1;
}

static void
write_var(stream_t *s, EXTENSION_CONTROL_BLOCK *r, char *name, int code)
{
	char buf[BUF_LENGTH];
        char *ptr;
	unsigned long size = sizeof(buf);

	buf[0] = 0;
	if (r->GetServerVariable(r->ConnID, name, buf, &size) && size > 0 && buf[0]) {
		buf[size] = 0;
                
                for (ptr = buf; isspace(*ptr); ptr++) {
                }
                
		cse_write_string(s, code, ptr);
	}
}

static void
write_header(stream_t *s, EXTENSION_CONTROL_BLOCK *r, char *name)
{
  char buf[BUF_LENGTH];
  unsigned long size = sizeof(buf);
  char *ptr = buf;

  buf[0] = 0;
	if (r->GetServerVariable(r->ConnID, name, buf, &size) && size > 0 && buf[0]) {
                for (; size > 0 && isspace(buf[size - 1]); size--) {
                }
		buf[size] = 0;
		cse_write_string(s, HMUX_HEADER, name);
                for (ptr = buf; isspace(*ptr); ptr++) {
                }
                
		cse_write_string(s, HMUX_STRING, ptr);
 	}
}

/**
 * Writes SSL data, including client certificates.
 */
static void
write_ssl(stream_t *s, EXTENSION_CONTROL_BLOCK *r)
{
  char buf[BUF_LENGTH];
  unsigned long size = sizeof(buf);

	if (! r->GetServerVariable(r->ConnID, "SERVER_PORT_SECURE", buf, &size) ||
            size <= 0 || buf[0] != '1')
          return;

	cse_write_string(s, CSE_IS_SECURE, "");

	// Anh : Add SSL connection informations
	cse_write_string(s, HMUX_HEADER, "HTTPS");
	cse_write_string(s, HMUX_STRING, "on");
	write_header(s, r, "HTTPS_KEYSIZE");
	write_header(s, r, "HTTPS_SECRETKEYSIZE");
			
	// Anh : Check client certificate existence
	size = sizeof(buf);
	buf[0] = 0;

	if (! r->GetServerVariable(r->ConnID, "CERT_FLAGS", buf, &size) || 
		size <= 0 || buf[0] != '1')
		return;

	// There is a client certificate
    char cert_buf[BUF_LENGTH]={0};
    CERT_CONTEXT_EX cert;
    cert.cbAllocated = sizeof(cert_buf);
    cert.CertContext.pbCertEncoded = (BYTE*) cert_buf;
    cert.CertContext.cbCertEncoded = 0;
    DWORD dwSize = sizeof(cert);

    if (r->ServerSupportFunction(r->ConnID,
                                 (DWORD)HSE_REQ_GET_CERT_INFO_EX,                               
                                 (LPVOID)&cert, &dwSize,NULL) != FALSE)
    {
      // cert now contains valid client certificate information	
      LOG(("\ndwCertEncodingType = %d (%d) %ld\n",
                    cert.CertContext.dwCertEncodingType & X509_ASN_ENCODING ,
                    cert.CertContext.cbCertEncoded,
                    cert.dwCertificateFlags));
               
      cse_write_packet(s, CSE_CLIENT_CERT, 
                       (char *)cert.CertContext.pbCertEncoded,
                       cert.CertContext.cbCertEncoded);
               
      write_header(s, r, "CERT_ISSUER");
      write_header(s, r, "CERT_SERIALNUMBER");
      write_header(s, r, "CERT_SUBJECT");
      write_header(s, r, "CERT_SERVER_ISSUER");
      write_header(s, r, "CERT_SERVER_SUBJECT");
	}
}

static int
hexify(char *uri, int offset, int ch)
{
  int d1 = (ch >> 4) & 0xf;
  int d2 = ch & 0xf;
  uri[offset++] = '%';
  uri[offset++] = (d1 < 10) ? (d1 + '0') : (d1 - 10 + 'A');
  uri[offset++] = (d2 < 10) ? (d2 + '0') : (d2 - 10 + 'A');

  return offset;
}

static int 
write_env(stream_t *s, EXTENSION_CONTROL_BLOCK *r)
{
	int isHttp11 = 0;

	char protocol[BUF_LENGTH];
	char path_info_buffer[BUF_LENGTH];
	char *path_info = path_info_buffer;
	char uri_buffer[BUF_LENGTH];
	char *uri = uri_buffer;
	unsigned long size = sizeof(protocol);

	if (r->GetServerVariable(r->ConnID, "SERVER_PROTOCOL", protocol, &size) && size > 0) {
		protocol[size] = 0;
		isHttp11 = ! strcmp(protocol, "HTTP/1.1");
	}
	size = sizeof(path_info_buffer);
	if (r->GetServerVariable(r->ConnID, "PATH_INFO", path_info, &size) && size > 0) {
	        int i;
	  
		path_info[size] = 0;
		if (! strncmp(path_info, ISAPI_SCRIPT, sizeof(ISAPI_SCRIPT) - 1))
			path_info += sizeof(ISAPI_SCRIPT) - 1;

		i = 0;
		while (i < BUF_LENGTH - 6) {
		  int ch = *path_info++ & 0xff;

		  if (ch == 0)
		    break;
		  else if (' ' <= ch && ch < 0x80 && ch != '%') {
		    uri[i++] = ch;
		  }
		  else if (ch < 0x80) {
		    i = hexify(uri, i, ch);
		  }
		  else {
		    i = hexify(uri, i, 0xc0 | ((ch >> 6) & 0x1f));
		    i = hexify(uri, i, 0x80 | (ch & 0x3f));
		  }
		}
		uri[i] = 0;
	}
	else
		uri[size] = 0;
	hmux_start_channel(s, 1);
	cse_write_string(s, HMUX_URL, uri);
	write_var(s, r, "REQUEST_METHOD", HMUX_METHOD);
	write_var(s, r, "SERVER_PROTOCOL", CSE_PROTOCOL);
	//	write_var(s, r, "PATH_TRANSLATED", CSE_PATH_TRANSLATED);
	write_var(s, r, "QUERY_STRING", CSE_QUERY_STRING);
	write_var(s, r, "SERVER_NAME", HMUX_SERVER_NAME);
	write_var(s, r, "SERVER_PORT", CSE_SERVER_PORT);
	write_var(s, r, "REMOTE_HOST", CSE_REMOTE_HOST);
	write_var(s, r, "REMOTE_ADDR", CSE_REMOTE_ADDR);
	write_var(s, r, "REMOTE_USER", CSE_REMOTE_USER);
	write_var(s, r, "AUTH_TYPE", CSE_AUTH_TYPE);

	// write_var(s, r, "CONTENT_TYPE", CSE_CONTENT_TYPE);
	// write_var(s, r, "CONTENT_LENGTH", CSE_CONTENT_LENGTH);
  // cse_write_string(s, CSE_DOCROOT, ap_document_root(r));
  //	sprintf(buf, "%d", s->srun->session);
	cse_write_string(s, CSE_SERVER_TYPE, "ISAPI");

	write_ssl(s, r);

	return isHttp11;
}

static void
write_headers(stream_t *s, EXTENSION_CONTROL_BLOCK *r)
{
	char buf[16384];
	unsigned long i = 0;
	unsigned long len = sizeof(buf);

	if (! r->GetServerVariable(r->ConnID, "ALL_RAW", buf, &len))
		return;

	while (i < len) {
	  int ch;
	  int j;
	  
		for (; i < len && isspace(buf[i]); i++) {
		}

		int head = i;
		int tail;

		for (; i < len && (ch = buf[i]) != ':'; i++) {
		  if (isspace(ch))
		    buf[i] = 0;
		}
		buf[i] = 0;

		for (i++;
		     (i < len && ((ch = buf[i]) == ' ' || ch == '\t') && ch != '\n');
		      i++) {
		}

		tail = i;
		for (; i < len && (buf[i] != '\n' || isspace(buf[i+1])); i++) {
			if (isspace(buf[i]))
				buf[i] = ' ';
		}
		for (j = i - 1; tail <= j && isspace(buf[j]); j--) {
		  buf[j] = 0;
		}
		buf[i++] = 0;
		if (buf[head]) {
			cse_write_string(s, HMUX_HEADER, buf + head);
			cse_write_string(s, HMUX_STRING, buf + tail);
		}
	}
}

static int
write_client_buffer(EXTENSION_CONTROL_BLOCK *r, void *v_buf, int len)
{
	char *buffer = (char *) v_buf;

	while (len > 0) {
		unsigned long sentlen = len;

		if (! r->WriteClient(r->ConnID, buffer, &sentlen, HSE_IO_SYNC) || 
			sentlen <= 0) {
			return -1;
		}

		len -= sentlen;
		buffer += sentlen;
	}

	return 0;
}

static int
cse_write_response(stream_t *s, unsigned long len, EXTENSION_CONTROL_BLOCK *r)
{
	while (len > 0) {
		unsigned long sublen;

		if (s->read_offset >= s->read_length) {
		  if (cse_fill_buffer(s) < 0) {
		    connection_error(s->config, r);
		    return -1;
		  }
		}

		sublen = s->read_length - s->read_offset;
		if (len < sublen)
			sublen = len;

		s->read_buf[s->read_length] = 0;
		if (write_client_buffer(r, s->read_buf + s->read_offset,
					sublen) < 0)
		  return -1;

		len -= sublen;
		s->read_offset += sublen;
	}

	return 1;
}

static char *
fill_chunk(char *buf, int size)
{
	int i;

	for (i = 12; i >= 0; i -= 4) {
		int digit = (size >> i) & 0xf;

		if (digit > 9)
			*buf++ = 'a' + digit - 10;
		else
			*buf++ = '0' + digit;
	}
	*buf++ = '\r';
	*buf++ = '\n';
	*buf = 0;

	return buf;
}

static int
send_data(stream_t *s, EXTENSION_CONTROL_BLOCK *r, config_t *config, 
	  int ack, int *p_http11, int *p_is_first)
{
	char headers[32 * 1024];
	char status[BUF_LENGTH];
	char chunk[16];
	char *status_ptr = status;
    char *header_ptr = headers;
    char *header_end = header_ptr + sizeof(headers) - 256;
	int code;
	int http11 = *p_http11;

	chunk[0] = '\r';
	chunk[1] = '\n';

	*header_ptr = 0;
	do {
		int read_len;
		unsigned long size;

		code = cse_read_byte(s);
		if (code < 0 || s->socket < 0) {
		  if (status == status_ptr)
		    connection_error(s->config, r);
		  
			return -1;
		}

		LOG(("code %c(%d)\n", code, code));

		switch (code) {
		case HMUX_YIELD:
		  break;
		  
		case HMUX_CHANNEL:
			read_len = hmux_read_len(s);
			break;
			
		case HMUX_ACK:
			read_len = hmux_read_len(s);
			break;

		case HMUX_DATA:
			read_len = hmux_read_len(s);
			if (http11) {
				char *tail = fill_chunk(chunk + 2, read_len);
				if (*p_is_first)
					write_client_buffer(r, chunk + 2, tail - chunk - 2);
				else
					write_client_buffer(r, chunk, tail - chunk);
				*p_is_first = 0;
			}
			if (cse_write_response(s, read_len, r) < 0)
				code = -1;
			break;

		case HMUX_STATUS:
			read_len = hmux_read_len(s);
            cse_read_limit(s, status, sizeof(status), read_len);

			if (status[0] != '2') {
				http11 = 0;
				*p_http11 = 0;
			}

			status_ptr = status + read_len;
			break;

		case HMUX_META_HEADER:
			read_len = hmux_read_len(s);
			cse_skip(s, read_len);
			code = cse_read_byte(s);
			read_len = hmux_read_len(s);
			cse_skip(s, read_len);
			break;

		case HMUX_HEADER:
		  {
		    char *ptr = header_ptr;
		    
			read_len = hmux_read_len(s);
			header_ptr += cse_read_limit(s, header_ptr,
                                                     header_end - header_ptr,
                                                     read_len);
            *header_ptr++ = ':';
            *header_ptr++ = ' ';
            code = cse_read_byte(s);
            read_len = hmux_read_len(s);
	        if (read_len < 0 || s->socket < 0)
				return -1;
                        
			header_ptr += cse_read_limit(s, header_ptr,
                                                     header_end - header_ptr,
                                                     read_len);
                        *header_ptr++ = '\r';
                        *header_ptr++ = '\n';

			/* content-length must be ignored for chunking */
			if (http11 && ! strncmp(ptr, "Content-Length:",
						sizeof("Content-Length:") - 1)) {
			  header_ptr = ptr;
			}
			
			break;
		  }

		case CSE_SEND_HEADER:
			read_len = hmux_read_len(s);
			cse_skip(s, read_len);
			if (http11) {
				char chunked[] = "Transfer-Encoding: chunked\r\n";
				strcpy(header_ptr, chunked);
				header_ptr += sizeof(chunked) - 1;
			}
            *header_ptr++ = '\r';
            *header_ptr++ = '\n';
            *header_ptr++ = 0;
			size = header_ptr - headers;
			{
				HSE_SEND_HEADER_EX_INFO info;
				unsigned long info_size = sizeof(info);
				int statusCode = atoi(status_ptr);

				memset(&info, 0, sizeof(info));
				info.cchStatus = status_ptr - status;
				info.cchHeader = header_ptr - headers;
				info.pszHeader = headers;
				info.pszStatus = status;
				/* #1802, #2150 */
				info.fKeepConn = http11;
				/*
			r->dwHttpStatusCode = atoi(status_ptr);
			r->ServerSupportFunction(r->ConnID, HSE_REQ_SEND_RESPONSE_HEADER, status, &size, (unsigned long *) headers);
			*/
				r->dwHttpStatusCode = statusCode;
				r->ServerSupportFunction(r->ConnID, HSE_REQ_SEND_RESPONSE_HEADER_EX, &info, &info_size, 0);
			}
			break;

		case HMUX_QUIT:
		case HMUX_EXIT:
			if (http11) {
				write_client_buffer(r, "\r\n0\r\n\r\n", 7);
			}
		  return code;

		default:
			if (code < 0) {
				code = -1;
				connection_error(config, r);
				break;
			}
			read_len = hmux_read_len(s);
			if (read_len < 0 || read_len > BUF_LENGTH) {
				code = -1;
				break;
			}

			cse_skip(s, read_len);
			break;
		}
	} while (code > 0 && code != HMUX_QUIT && code != HMUX_EXIT && code != ack);

	return code;
}

static void
cse_get_ip(EXTENSION_CONTROL_BLOCK *r, char *ip, unsigned long length)
{
	ip[0] = 0;
	if (r->GetServerVariable(r->ConnID, "REMOTE_ADDR", ip, &length) && length >= 0)
		ip[length] = 0;
	else
		ip[0] = 0;
}

static int
get_session_index(EXTENSION_CONTROL_BLOCK *r, int *backup)
{
	char buf[16384];
	unsigned long i = 0;
	unsigned long len = sizeof(buf);
	int session;

	if (r->GetServerVariable(r->ConnID, "HTTP_COOKIE", buf, &len)) {
	    session = cse_session_from_string(buf, "JSESSIONID=", backup);
		LOG(("session %d %s\n", session, buf));
      	if (session >= 0)
			return session;
	}

	len = sizeof(buf);

	buf[0] = 0;
	if (! r->GetServerVariable(r->ConnID, "PATH_INFO", buf, &len))
		return -1;
	
	buf[len] = 0;
	return cse_session_from_string(buf, "jsessionid=", backup);
}

static int
get_session_from_raw(EXTENSION_CONTROL_BLOCK *r, int *backup)
{
	char buf[16384];
	unsigned long i = 0;
	unsigned long len = sizeof(buf);
	int session;

	if (! r->GetServerVariable(r->ConnID, "ALL_RAW", buf, &len))
		return -1;

	while (i < len) {
		int head = i + 5;
		int tail;
		i = head;
		for (; i < len && buf[i] != ':'; i++) {
		}
		tail = i;

		for (i++; i < len && buf[i] != '\r' && buf[i] != '\n'; i++) {
		}

		if (head >= tail)
			continue;

		buf[tail] = 0;
		buf[i] = 0;

		if (stricmp(buf + head, "cookie"))
			continue;

	    session = cse_session_from_string(buf + tail + 1, "JSESSIONID=", backup);
		if (session >= 0)
			return session;
	}

	return -1;
}

static int
write_request(stream_t *s, EXTENSION_CONTROL_BLOCK *r, config_t *config, char *host_name, int port)
{
	int backup_index = 0;
	int session_index = get_session_index(r, &backup_index);
	srun_t *srun;
	resin_host_t *host;

	DWORD now = GetTickCount() / 1000;

	host = cse_match_host(config, host_name, port, now);

	if (! host || ! cse_open_connection(s, &host->cluster, session_index, backup_index, now, 0))
		return connection_error(config, r);

	srun = s->cluster_srun->srun;
	cse_lock(srun->lock);
	srun->active_sockets++;
	cse_unlock(srun->lock);

	int isHttp11 = write_env(s, r);
	write_headers(s, r);

	// read post data
	char buf[BUF_LENGTH + 8];
	int ack_size = s->cluster_srun->srun->send_buffer_size;
	int send_length = 0;
	unsigned long totalLen = 0;
		
	int code = HMUX_ACK;
	int is_first = 1;

	//while (totalLen < r->cbTotalBytes) {
	while (true) {
		unsigned long len = BUF_LENGTH;

		if (r->cbAvailable > 0) {
			if (len > r->cbAvailable)
				len = r->cbAvailable;
			memcpy(buf, r->lpbData + totalLen, len);
			r->cbAvailable -= len;
		}
		else if (r->cbTotalBytes <= 0)
			break;
		else if (r->cbTotalBytes <= totalLen)
			break;
		else if (! r->ReadClient(r->ConnID, buf, &len) || len <= 0)
			break;

		totalLen += len;
		send_length += len;
		LOG(("send-post %d\n", len));
		cse_write_packet(s, CSE_DATA, buf, len);

		if (ack_size <= send_length) {
		  send_length = 0;
		  cse_write_byte(s, HMUX_YIELD);

			code = send_data(s, r, config, CSE_ACK, &isHttp11, &is_first);

			if (code < 0 || code == HMUX_QUIT || code == HMUX_EXIT)
				break;
		}
	}	

	LOG(("quit\n"));
	cse_write_byte(s, HMUX_QUIT);

	code = send_data(s, r, config, HMUX_QUIT, &isHttp11, &is_first);

	if (code == HMUX_QUIT)
		cse_free_idle(s, now);
	else
		cse_close(s, "write");

	cse_lock(srun->lock);
	srun->active_sockets--;
	cse_unlock(srun->lock);

	return code == HMUX_QUIT || code == HMUX_EXIT;
}

static int g_count;

static void
jvm_status(cluster_t *cluster, EXTENSION_CONTROL_BLOCK *r)
{
  int i;
  stream_t s;

  cse_printf(r, "<p><center><table border='2' width=\"90%%\">\n");
  cse_printf(r, "<tr><th>Host</th>\n");
  cse_printf(r, "    <th>Active</th>\n");
  cse_printf(r, "    <th>Pooled</th>\n");
  cse_printf(r, "    <th>Connect<br>Timeout</th>\n");
  cse_printf(r, "    <th>Live<br>Time</th>\n");
  cse_printf(r, "    <th>Dead<br>Time</th>\n");
  cse_printf(r, "    <th>Read<br>Timeout</th>\n");
  cse_printf(r, "</tr>\n");

  for (; cluster; cluster = cluster->next) {
    for (i = 0; i < cluster->srun_capacity; i++) {
      cluster_srun_t *cluster_srun = cluster->srun_list + i;
      srun_t *srun = cluster_srun->srun;
      int port;
      int pool_count;

      if (! srun)
	continue;
    
      port = srun->port;
      pool_count = ((srun->conn_head - srun->conn_tail + CONN_POOL_SIZE) %
		    CONN_POOL_SIZE);

      cse_printf(r, "<tr>");

      if (! cse_open(&s, cluster, cluster_srun, 0, 0)) {
	cse_printf(r, "<td bgcolor='#ff6666'>%d. %s:%d%s (down)</td>",
		   cluster_srun->index + 1,
		   srun->hostname ? srun->hostname : "localhost",
		   port, cluster_srun->is_backup ? "*" : "");
      }
      else {
	cse_printf(r, "<td bgcolor='#66ff66'>%d. %s:%d%s (ok)</td>",
		   cluster_srun->index + 1,
		   srun->hostname ? srun->hostname : "localhost",
		   port, cluster_srun->is_backup ? "*" : "");
      }

      /* This needs to be close, because cse_open doesn't use recycle. */
      cse_close(&s, "caucho-status");
      LOG(("close\n"));

      cse_printf(r, "<td align=right>%d</td><td align=right>%d</td>",
		 srun->active_sockets, pool_count);
      cse_printf(r, "<td align=right>%d</td><td align=right>%d</td><td align=right>%d</td>",
		 srun->connect_timeout, srun->idle_timeout, 
		 srun->fail_recover_timeout);
	  cse_printf(r, "<td align=right>%d</td>", srun->read_timeout);
      cse_printf(r, "</tr>\n");
    }
  }
  cse_printf(r, "</table></center>\n");
}

static void
cse_caucho_status(config_t *config, EXTENSION_CONTROL_BLOCK *r)
{
	resin_host_t *host;
	web_app_t *app;
  location_t *loc;
  char *headers = "200 OK\r\nContent-Type: text/html";
  unsigned long size = strlen(headers);

	DWORD now = GetTickCount() / 1000;

  if (! config->enable_caucho_status)
	  return;

  r->ServerSupportFunction(r->ConnID, HSE_REQ_SEND_RESPONSE_HEADER, headers, &size, 0);
 
  cse_printf(r, "<html><title>Status : Resin ISAPI Plugin</title>\n");
  cse_printf(r, "<body bgcolor=white>\n");
  cse_printf(r, "<h1>Status : Resin ISAPI Plugin</h1>\n");
 
  if (config->error) {
	  cse_printf(r, "<b><font color=\"red\">%s</font></b><br>",
			     config->error);
  }
  
  cse_printf(r, "<h2>Configuration Cluster</h2>\n");
  jvm_status(&config->config_cluster, r);
  
  host = config ? config->hosts : 0;
  for (; host; host = host->next) {
    if (host != host->canonical)
      continue;

    /* check updates as appropriate */
    cse_match_host(config, host->name, host->port, now);

    if (! *host->name)
      cse_printf(r, "<h2>Default Virtual Host</h2>\n");
    else if (host->port)
      cse_printf(r, "<h2>Virtual Host: %s:%d</h2>\n", host->name, host->port);
    else
      cse_printf(r, "<h2>Virtual Host: %s</h2>\n", host->name);
    
    jvm_status(&host->cluster, r);

    cse_printf(r, "<p><center><table border='2' cellspacing='0' cellpadding='2' width='90%%'>\n");
    cse_printf(r, "<tr><th>web-app\n");
    cse_printf(r, "    <th>url-pattern\n");

    app = host->applications;
    
    for (; app; app = app->next) {
      for (loc = app->locations; loc; loc = loc->next) {
	cse_printf(r, "<tr bgcolor='#ffcc66'><td>%s<td>%s%s%s%s%s</tr>\n", 
		   *app->context_path ? app->context_path : "/",
		   loc->prefix,
		   ! loc->is_exact && ! loc->suffix ? "/*" : 
		   loc->suffix && loc->prefix[0] ? "/" : "",
		   loc->suffix ? "*" : "",
		   loc->suffix ? loc->suffix : "",
		   loc->ignore ? " (ignore)" : "");
      }
    }
    cse_printf(r, "</table></center>\n");
  }

  cse_printf(r, "<hr>");
  cse_printf(r, "<em>%s<em>", VERSION);
  cse_printf(r, "</body></html>\n");
}

int
cse_handle_request(config_t *config, EXTENSION_CONTROL_BLOCK *r)
{
	char host[1024];
	char port_buf[80];
	int port = 0;
	char url[BUF_LENGTH];
	char *ptr = url;
	unsigned long size;

	size = sizeof(host);
	host[0] = 0;
	r->GetServerVariable(r->ConnID, "SERVER_NAME", host, &size);

	size = sizeof(port_buf);
	if (r->GetServerVariable(r->ConnID, "SERVER_PORT", port_buf, &size) && size > 0) {
		port = atoi(port_buf);
	}

	size = sizeof(url);

	url[0] = 0;
	if (r->GetServerVariable(r->ConnID, "PATH_INFO", url, &size) && size > 0 && url[0]) {
		DWORD now = GetTickCount() / 1000;
		url[size] = 0;

		if (! strcmp(url, "/caucho-status")) {
			cse_caucho_status(config, r);
			return 1;
		}
		else if (! cse_match_request(config, host, port, ptr, 0, now))
			return cse_error(config, r);
	}
	else
		return cse_error(config, r);

	stream_t s;

	return write_request(&s, r, config, host, port);
}
