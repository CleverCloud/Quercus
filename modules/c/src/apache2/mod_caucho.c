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

#include <sys/types.h>
#include <errno.h>
#ifndef WIN32
#include <unistd.h>
#endif
#include "ap_config.h"
#include "apr_strings.h"
#include "httpd.h"
#include "http_config.h"
#include "http_core.h"
#include "http_protocol.h"
#include "http_connection.h"

APR_DECLARE_OPTIONAL_FN(char *, ssl_var_lookup,
						(apr_pool_t *, server_rec *,
						conn_rec *, request_rec *, char*));

#ifdef DEBUG
#include <time.h>
#endif

#include "cse.h"
#include "version.h"

#if ! APR_HAS_THREADS
#define apr_thread_mutex_t int
#define apr_thread_mutex_create(a,b,c)
#define apr_thread_mutex_lock(a)
#define apr_thread_mutex_unlock(a)
#endif

static APR_OPTIONAL_FN_TYPE(ssl_var_lookup) *g_ssl_lookup = NULL;

/*
 * Apache magic module declaration.
 */
module AP_MODULE_DECLARE_DATA caucho_module;

static time_t g_start_time;

#define DEFAULT_PORT 6802

void
cse_log(char *fmt, ...)
{
#ifdef DEBUG
  va_list args;
  char timestamp[32];
  char buffer[8192];
  time_t t;
  int pid;
  FILE *file;

  file = fopen("/tmp/mod_caucho.log", "a");

  pid = (int) getpid();

  time(&t);
  strftime(timestamp, sizeof(timestamp), "[%m/%b/%Y:%H:%M:%S %z]",
	   localtime(&t));

#ifdef WIN32
#if APR_HAS_THREADS
  _snprintf(buffer, sizeof(buffer), "%s %d_%ld: ",
	   timestamp, pid, (long int) apr_os_thread_current());
#else   
  _snprintf(buffer, sizeof(buffer), "%s %d: ", timestamp, pid);
#endif
#else
#if APR_HAS_THREADS
  snprintf(buffer, sizeof(buffer), "%s %d_%ld: ",
	   timestamp, pid, (long int) apr_os_thread_current());
#else   
  snprintf(buffer, sizeof(buffer), "%s %d: ", timestamp, pid);
#endif
#endif  
   va_start(args, fmt);
   vsprintf(buffer + strlen(buffer), fmt, args);
   va_end(args);

   if (file) {
     fputs(buffer, file);
     fclose(file);
   }
   else {
     fputs(buffer, stderr);

     fflush(stderr);
   }
#endif
}

void *
cse_create_lock(config_t *config)
{
  apr_thread_mutex_t *lock = 0;

  apr_thread_mutex_create(&lock, APR_THREAD_MUTEX_DEFAULT, config->web_pool);

  return lock;
}

void
cse_free_lock(config_t *config, void *vlock)
{
  apr_thread_mutex_destroy(vlock);
}

int
cse_lock(void *vlock)
{
  apr_thread_mutex_t *lock = vlock;

  if (lock)
    apr_thread_mutex_lock(lock);
  
  return 1;
}

void
cse_unlock(void *vlock)
{
  apr_thread_mutex_t *lock = vlock;

  if (lock)
    apr_thread_mutex_unlock(lock);
}

void
cse_error(config_t *config, char *format, ...)
{
  char buf[BUF_LENGTH];
  va_list args;

  va_start(args, format);
  vsprintf(buf, format, args);
  va_end(args);

  LOG(("ERROR: %s\n", buf));

  if (config->error && *config->error)
    free(config->error);
  
  config->error = strdup(buf);
}

void
cse_set_socket_cleanup(int socket, void *pool)
{
  /* XXX:
  LOG(("set cleanup %d\n", socket));

  if (socket > 0)
    apr_note_cleanups_for_socket(pool, socket);
  */
}

void
cse_kill_socket_cleanup(int socket, void *pool)
{
  /* XXX:
  LOG(("kill cleanup %d\n", socket));

  if (socket > 0)
    apr_kill_cleanups_for_socket(pool, socket);
  */
}

void *
cse_malloc(int size)
{
  return malloc(size);
}

void cse_free(config_t *config, void *data) {}

static void *
cse_init_server_config(apr_pool_t *p, char *dummy)
{
  ap_add_version_component(p, VERSION);
  
  return 0;
}

static void *
cse_create_server_config(apr_pool_t *p, server_rec *s)
{
  config_t *config = (config_t *) apr_pcalloc(p, sizeof(config_t));

  memset(config, 0, sizeof(config_t));

  config->web_pool = p;
  config->start_time = g_start_time;
  cse_init_config(config);

  return (void *) config;
}

static void *
cse_merge_server_config(apr_pool_t *p, void *basev, void *overridesv)
{
  config_t *base = (config_t *) basev;
  config_t *overrides = (config_t *) overridesv;

  if (! overrides || ! overrides->has_config)
    return base;
  else
    return overrides;
}

static void *
cse_create_dir_config(apr_pool_t *p, char *path)
{
  config_t *config = (config_t *) apr_pcalloc(p, sizeof(config_t));
  
  memset(config, 0, sizeof(config_t));

  config->web_pool = p;
  cse_init_config(config);

  return (void *) config;
}

static void *
cse_merge_dir_config(apr_pool_t *p, void *basev, void *overridesv)
{
  config_t *base = (config_t *) basev;
  config_t *overrides = (config_t *) overridesv;

  if (! overrides || ! overrides->has_config)
    return base;
  else
    return overrides;
}

/**
 * Retrieves the caucho configuration from Apache
 */
static config_t *
cse_get_module_config(request_rec *r)
{
  config_t *config = 0;

  if (r->per_dir_config) {
    config = (config_t *) ap_get_module_config(r->per_dir_config,
					       &caucho_module);
  }

  if (config && config->has_config)
    return config;

  if (r->server->module_config)
    config = (config_t *) ap_get_module_config(r->server->module_config,
                                               &caucho_module);

  return config;
}

/**
 * Retrieves the caucho configuration from Apache
 */
static config_t *
cse_get_server_config(server_rec *s)
{
  config_t *config;

  config = (config_t *) ap_get_module_config(s->module_config,
                                             &caucho_module);

  return config;
}

/**
 * Parse the CauchoHosts configuration in the apache config file.
 */
static const char *
resin_config_server_command(cmd_parms *cmd, void *pconfig,
			    const char *host_arg, const char *port_arg)
{
  config_t *config = pconfig; /* cse_get_server_config(cmd->server); */
  int port = port_arg ? atoi(port_arg) : DEFAULT_PORT;
  
  if (! config)
    return 0;

  config->has_config = 1;

  /*
  cse_add_host(&config->config_cluster, host_arg, port);
  */
  if (! strcmp(host_arg, "current")) {
    char hostname[256];
    
    gethostname(hostname, sizeof(hostname));
    host_arg = strdup(host_arg);
  }
  
  cse_add_config_server(config->p, config, host_arg, port);

  return 0;
}

/**
 * Parse the CauchoStatus configuration in the apache config file.
 */
static const char *
caucho_status_command(cmd_parms *cmd, void *pconfig, const char *value)
{
  config_t *config = pconfig; /* = cse_get_server_config(cmd->server); */
  
  if (! config)
    return 0;  

  config->has_config = 1;
  
  if (value == 0 || ! strcmp(value, "true") || ! strcmp(value, "yes"))
    config->enable_caucho_status = 1;
  else
    config->enable_caucho_status = 0;

  return 0;
}

/**
 * Parse the CauchoConfigCacheDirectory configuration in the apache config file.
 */
static const char *
resin_config_cache_command(cmd_parms *cmd, void *pconfig,
			   const char *cache_dir)
{
  config_t *config = pconfig; /* cse_get_server_config(cmd->server); */
  
  if (! config || ! cache_dir)
    return 0;

  config->has_config = 1;

  strcpy(config->work_dir, cache_dir);

  return 0;
}

/**
 * Parse the CauchoConfigHost configuration in the apache config file.
 */
static const char *
cse_config_file_command(cmd_parms *cmd, void *pconfig, char *value)
{
  return "CauchoConfigFile has been replaced by ResinConfigServer.\n";
}

/**
 * Parse the CauchoHosts configuration in the apache config file.
 */
static const char *
cse_host_command(cmd_parms *cmd, void *pconfig,
		 const char *host_arg, const char *port_arg)
{
  config_t *config = pconfig; /* = cse_get_server_config(cmd->server); */

  int port = port_arg ? atoi(port_arg) : DEFAULT_PORT;
  resin_host_t *host;

  if (! config)
    return 0;

  config->has_config = 1;
  
  host = config->manual_host;
  if (! host) {
    host = cse_alloc(config->p, sizeof(resin_host_t));
    memset(host, 0, sizeof(resin_host_t));
    
    host->name = "manual";
    host->canonical = host;

    host->config = config;
    
    host->cluster.config = config;
    host->cluster.round_robin_index = -1;
    
    config->manual_host = host;
  }

  cse_add_host(config->p, &host->cluster, host_arg, port);

  return 0;
}

/**
 * Parse the CauchoBackup configuration in the apache config file.
 */
static const char *
cse_backup_command(cmd_parms *cmd, void *pconfig,
		   const char *host_arg, const char *port_arg)
{
  config_t *config = pconfig; /* = cse_get_server_config(cmd->server); */
  int port = port_arg ? atoi(port_arg) : DEFAULT_PORT;
  resin_host_t *host;

  if (! config)
    return 0;

  config->has_config = 1;
  
  host = config->manual_host;
  if (! host) {
    host = cse_alloc(config->p, sizeof(resin_host_t));
    memset(host, 0, sizeof(resin_host_t));
    
    host->name = "manual";
    host->canonical = host;

    host->cluster.config = config;
    host->cluster.round_robin_index = -1;
    
    config->manual_host = host;
  }

  cse_add_backup(config->p, &host->cluster, host_arg, port);

  return 0;
}

/**
 * Set the default session cookie used by mod_caucho.
 */
static const char *
resin_session_cookie_command(cmd_parms *cmd, void *pconfig,
			     const char *cookie_arg)
{
  config_t *config = pconfig; /* = cse_get_server_config(cmd->server); */

  if (! config)
    return 0;

  config->has_config = 1;

  strcpy(config->session_cookie, cookie_arg);

  return 0;
}

/**
 * Set the default session cookie used by mod_caucho.
 */
static const char *
resin_session_sticky_command(cmd_parms *cmd, void *pconfig,
			     const char *cookie_arg)
{
  config_t *config = pconfig; /* = cse_get_server_config(cmd->server); */

  if (! config)
    return 0;

  config->has_config = 1;

  if (! strcmp(cookie_arg, "false")) {
    config->disable_sticky_sessions = 1;
  }

  return 0;
}

/**
 * Set the default session url used by mod_caucho.
 */
static const char *
resin_session_url_prefix_command(cmd_parms *cmd, void *pconfig,
				 const char *cookie_arg)
{
  config_t *config = pconfig; /* = cse_get_server_config(cmd->server); */

  if (! config)
    return 0;

  config->has_config = 1;

  strcpy(config->session_url_prefix, cookie_arg);

  return 0;
}

/**
 * Gets the session index from the request
 *
 * Cookies have priority over the query
 *
 * @return -1 if no session
 */
static int
get_session_index(config_t *config, request_rec *r, int *backup)
{
  const apr_array_header_t *header = apr_table_elts(r->headers_in);
  const apr_table_entry_t *headers = (const apr_table_entry_t *) header->elts;
  int i;
  int session;

  for (i = 0; i < header->nelts; ++i) {
    if (! headers[i].key || ! headers[i].val)
      continue;

    if (strcasecmp(headers[i].key, "Cookie"))
      continue;

    session = cse_session_from_string(headers[i].val,
                                      config->session_cookie,
                                      backup);
    if (session >= 0)
      return session;
  }

  return cse_session_from_string(r->uri, config->session_url_prefix, backup);
}

/**
 * Writes request parameters to srun.
 */
static void
write_env(stream_t *s, request_rec *r)
{
  char buf[4096];
  int ch;
  int i, j;
  
  conn_rec *c = r->connection;
  const char *host;
  const char *uri;
  int port;
  int is_sub_request = 1; /* for mod_rewrite */

  /*
   * is_sub_request is always true, since we can't detect mod_rewrite
   * and mod_rewrite doesn't change the unparsed_uri.
   */
  if (is_sub_request)
    uri = r->uri;
  else
    uri = r->unparsed_uri; /* #937 */

  j = 0;
  for (i = 0; (ch = uri[i]) && ch != '?' && j + 2 < sizeof(buf); i++) {
    if (ch == '%') { /* #1661 */
      buf[j++] = '%';
      buf[j++] = '2';
      buf[j++] = '5';
    }
    else
      buf[j++] = ch;
  }
  buf[j] = 0;

  cse_write_string(s, HMUX_URL, buf);

  cse_write_string(s, HMUX_METHOD, r->method);
  
  cse_write_string(s, CSE_PROTOCOL, r->protocol);

  if (r->args)
    cse_write_string(s, CSE_QUERY_STRING, r->args);

  /* Gets the server name */
  host = ap_get_server_name(r);
  port = ap_get_server_port(r);

  cse_write_string(s, HMUX_SERVER_NAME, host);
  sprintf(buf, "%u", port);
  cse_write_string(s, CSE_SERVER_PORT, buf);

  if (c->remote_host)
    cse_write_string(s, CSE_REMOTE_HOST, c->remote_host);
  else
    cse_write_string(s, CSE_REMOTE_HOST, c->remote_ip);

  cse_write_string(s, CSE_REMOTE_ADDR, c->remote_ip);
  sprintf(buf, "%u", ntohs(c->remote_addr->port));
  cse_write_string(s, CSE_REMOTE_PORT, buf);

  if (r->user)
    cse_write_string(s, CSE_REMOTE_USER, r->user);
  if (r->ap_auth_type)
    cse_write_string(s, CSE_AUTH_TYPE, r->ap_auth_type);

  /* mod_ssl */
  if (g_ssl_lookup) {
    static char *vars[] = { "SSL_CLIENT_S_DN",
                            "SSL_CIPHER",
                            "SSL_CIPHER_EXPORT",
                            "SSL_PROTOCOL",
                            "SSL_CIPHER_USEKEYSIZE",
                            "SSL_CIPHER_ALGKEYSIZE",
                            0};
    char *var;
    int i;
    
    if ((var = g_ssl_lookup(r->pool, r->server, r->connection, r,
			    "SSL_CLIENT_CERT"))) {
      cse_write_string(s, CSE_CLIENT_CERT, var);
    }

    for (i = 0; vars[i]; i++) {
      if ((var = g_ssl_lookup(r->pool, r->server, r->connection, r,
			      vars[i]))) {
        cse_write_string(s, HMUX_HEADER, vars[i]);
        cse_write_string(s, HMUX_STRING, var);
      }
    }
  }
}

/**
 * Writes headers to srun.
 */
static void
write_headers(stream_t *s, request_rec *r)
{
  const apr_array_header_t *header = apr_table_elts(r->headers_in);
  apr_table_entry_t *headers = (apr_table_entry_t *) header->elts;
  int i;

  for (i = 0; i < header->nelts; ++i) {
    if (! headers[i].key || ! headers[i].val)
      continue;

    /*
     * Content-type and Content-Length are special cased for a little
     * added efficiency.
     */

    if (! strcasecmp(headers[i].key, "Content-type"))
      cse_write_string(s, CSE_CONTENT_TYPE, headers[i].val);
    else if (! strcasecmp(headers[i].key, "Content-length"))
      cse_write_string(s, CSE_CONTENT_LENGTH, headers[i].val);
    else if (! strcasecmp(headers[i].key, "Expect")) {
      /* expect=continue-100 shouldn't be passed to backend */
    }
    else {
      cse_write_string(s, HMUX_HEADER, headers[i].key);
      cse_write_string(s, HMUX_STRING, headers[i].val);
    }
  }
}

static void
write_added_headers(stream_t *s, request_rec *r)
{
  const apr_array_header_t *header = apr_table_elts(r->subprocess_env);
  apr_table_entry_t *headers = (apr_table_entry_t *) header->elts;
  int i;

  for (i = 0; i < header->nelts; ++i) {
    if (! headers[i].key || ! headers[i].val)
      continue;

    if (! strcmp(headers[i].key, "HTTPS") &&
	! strcmp(headers[i].val, "on")) {
      cse_write_string(s, CSE_IS_SECURE, "");
    }
    else if (! r->user && ! strcmp(headers[i].key, "SSL_CLIENT_DN"))
      cse_write_string(s, CSE_REMOTE_USER, headers[i].val);
      

    cse_write_string(s, HMUX_HEADER, headers[i].key);
    cse_write_string(s, HMUX_STRING, headers[i].val);
  }

  if (r->prev) {
    if (r->prev->args) {
      cse_write_string(s, HMUX_HEADER, "REDIRECT_QUERY_STRING");
      cse_write_string(s, HMUX_STRING, r->prev->args);
    }
    
    if (r->prev->uri) {
      cse_write_string(s, HMUX_HEADER, "REDIRECT_URL");
      cse_write_string(s, HMUX_STRING, r->prev->uri);
    }
  }
}

/**
 * Writes a response from srun to the client
 */
static int
cse_write_response(stream_t *s, int len, request_rec *r)
{
  while (len > 0) {
    int sublen;
    int writelen;
    int sentlen;

    if (s->read_length <= s->read_offset && cse_fill_buffer(s) < 0)
      return -1;

    sublen = s->read_length - s->read_offset;
    if (len < sublen)
      sublen = len;

    writelen = sublen;
    while (writelen > 0) {
      sentlen = ap_rwrite(s->read_buf + s->read_offset, writelen, r);
      /*
       * RSN-420.  If the client fails, should still read data from the
       * server and complete that side of the socket.
       */

      if (sentlen > 0)
	writelen -= sentlen;
      else
	writelen = 0;
    }
    
    s->read_offset += sublen;
    len -= sublen;
  }
  
  return 1;
}

/**
 * Copy data from the JVM to the browser.
 */
static int
send_data(stream_t *s, request_rec *r)
{
  int code = -1;
  char buf[8193];
  char key[8193];
  char value[8193];
  int i;
  int channel;
    
  if (cse_fill_buffer(s) < 0) {
    return -1;
  }
    
  while (1) {
    int len;

    code = cse_read_byte(s);

    LOG(("%s:%d:send_data(): r-code %c\n", __FILE__, __LINE__, code));
    
    switch (code) {
    case HMUX_CHANNEL:
      channel = hmux_read_len(s);
      LOG(("%s:%d:send_data(): r-channel %d\n", __FILE__, __LINE__, channel));
      break;
      
    case HMUX_ACK:
      channel = hmux_read_len(s);
      LOG(("%s:%d:send_data(): r-ack %d\n", __FILE__, __LINE__, channel));
      return code;
      
    case HMUX_STATUS:
      len = hmux_read_len(s);
      cse_read_limit(s, buf, sizeof(buf), len);

      for (i = 0; buf[i] && buf[i] != ' '; i++) {
      }
      i++;
      r->status = atoi(buf);
      r->status_line = apr_pstrdup(r->pool, buf);
      break;

    case HMUX_HEADER:
      len = hmux_read_len(s);
      cse_read_limit(s, key, sizeof(key), len);
      cse_read_string(s, value, sizeof(value));
      
      if (! strcasecmp(key, "content-type")) {
	r->content_type = apr_pstrdup(r->pool, value);
	apr_table_set(r->headers_out, key, value);
      }
      else
	apr_table_add(r->headers_out, key, value);
      break;
      
    case HMUX_META_HEADER:
      len = hmux_read_len(s);
      cse_read_limit(s, key, sizeof(key), len);
      cse_read_string(s, value, sizeof(value));
      break;

    case HMUX_DATA:
      len = hmux_read_len(s);
      if (cse_write_response(s, len, r) < 0)
	return HMUX_EXIT;
      break;

    case HMUX_FLUSH:
      len = hmux_read_len(s);
      ap_rflush(r);
      break;

    case CSE_SEND_HEADER:
      len = hmux_read_len(s);
      break;

    case HMUX_QUIT:
    case HMUX_EXIT:
      return code;

    default:
      if (code < 0) {
	return code;
      }
      
      len = hmux_read_len(s);
      cse_skip(s, len);
      break;
    }
  }
}

/**
 * handles a client request
 */
static int
write_request(stream_t *s, request_rec *r, config_t *config,
	      int session_index, int backup_index)
{
  int len;
  int code = -1;

  write_env(s, r);
  write_headers(s, r);
  write_added_headers(s, r);

  /* read post data */
  if (ap_should_client_block(r)) {
    char buf[BUF_LENGTH];
    int ack_size = s->cluster_srun->srun->send_buffer_size;
    int send_length = 0;

    while ((len = ap_get_client_block(r, buf, BUF_LENGTH)) > 0) {
      LOG(("%s:%d:write-request(): w-D %d\n", __FILE__, __LINE__, len));
      
      if (ack_size <= send_length + len && send_length > 0) {
        LOG(("%s:%d:write-request(): w-Y send_length=%d ack_size=%d\n",
             __FILE__, __LINE__, send_length, ack_size));
        
	send_length = 0;
	cse_write_byte(s, HMUX_YIELD);
        code = send_data(s, r);
        if (code != HMUX_ACK)
          break;
      }

      cse_write_packet(s, HMUX_DATA, buf, len);

      send_length += len;
    }
  }

  LOG(("%s:%d:write-request(): w-Q\n", __FILE__, __LINE__));

  cse_write_byte(s, HMUX_QUIT);
  code = send_data(s, r);

  LOG(("%s:%d:write_request(): return code %c\n", __FILE__, __LINE__, code));

  return code;
}

/**
 * Handle a request.
 */
static int
caucho_request(request_rec *r, config_t *config, resin_host_t *host,
	       unsigned int now)
{
  stream_t s;
  int retval;
  int code = -1;
  int session_index;
  int backup_index = 0;
  char *ip;
  srun_t *srun;

  if ((retval = ap_setup_client_block(r, REQUEST_CHUNKED_DECHUNK)))
    return retval;

  session_index = get_session_index(config, r, &backup_index);
  ip = r->connection->remote_ip;

  if (host) {
  }
  else if (config->manual_host) {
    host = config->manual_host;
  }
  else {
    host = cse_match_host(config,
			  ap_get_server_name(r),
			  ap_get_server_port(r),
			  now);
  }
    
  LOG(("%s:%d:caucho_request(): session index: %d\n",
       __FILE__, __LINE__, session_index));
  
  if (! host) {
    ERR(("%s:%d:caucho_request(): no host: %p\n",
	 __FILE__, __LINE__, host));
    
    return HTTP_SERVICE_UNAVAILABLE;
  }
  else if (! cse_open_connection(&s, &host->cluster,
				 session_index, backup_index,
				 now, r->pool)) {
    ERR(("%s:%d:caucho_request(): no connection: cluster(%p)\n",
	 __FILE__, __LINE__, &host->cluster));
    
    return HTTP_SERVICE_UNAVAILABLE;
  }

  srun = s.cluster_srun->srun;

  apr_thread_mutex_lock(srun->lock);
  srun->active_sockets++;
  apr_thread_mutex_unlock(srun->lock);
  
  code = write_request(&s, r, config, session_index, backup_index);

  apr_thread_mutex_lock(srun->lock);
  srun->active_sockets--;
  apr_thread_mutex_unlock(srun->lock);
  
  /* on failure, do not failover but simply fail */
  if (code == HMUX_QUIT)
    cse_free_idle(&s, now);
  else
    cse_close(&s, "no reuse");

  if (code != HMUX_QUIT && code != HMUX_EXIT) {
    ERR(("%s:%d:caucho_request(): protocol failure code:%d\n",
	 __FILE__, __LINE__, code));

    return HTTP_SERVICE_UNAVAILABLE;
  }
  else if (r->status == HTTP_SERVICE_UNAVAILABLE) {
    return HTTP_SERVICE_UNAVAILABLE;
  }
  else {
    /*
     * See pages like jms/index.xtp
    int status = r->status;
    r->status = HTTP_OK;

    return status;
    */
    return OK;
  }
}

/**
 * Print the statistics for each JVM.
 */
static void
jvm_status(cluster_t *cluster, request_rec *r)
{
  int i;
  stream_t s;

  ap_rputs("<center><table border=2 width='80%' style='size:small;'>\n", r);
  ap_rputs("<tr><th width=\"30%\">Host</th>\n", r);
  ap_rputs("    <th>Active</th>\n", r);
  ap_rputs("    <th>Pooled</th>\n", r);
  ap_rputs("    <th>Connect<br>Timeout</th>\n", r);
  ap_rputs("    <th>Idle<br>Time</th>\n", r);
  ap_rputs("    <th>Recover<br>Time</th>\n", r);
  ap_rputs("    <th>Socket<br>Timeout</th>\n", r);
  ap_rputs("</tr>\n", r);

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

      ap_rputs("<tr>", r);

      if (! cse_open(&s, cluster, cluster_srun, r->pool, 0)) {
	ap_rprintf(r, "<td bgcolor='#ff6666'>%d. %s:%d%s (down)</td>",
		   cluster_srun->index + 1,
		   srun->hostname ? srun->hostname : "localhost",
		   port, cluster_srun->is_backup ? "*" : "");
      }
      else {
	ap_rprintf(r, "<td bgcolor='#66ff66'>%d. %s:%d%s (ok)</td>",
		   cluster_srun->index + 1,
		   srun->hostname ? srun->hostname : "localhost",
		   port, cluster_srun->is_backup ? "*" : "");
      }

      /* This needs to be close, because cse_open doesn't use recycle. */
      cse_close(&s, "caucho-status");
      LOG(("%s:%d:jvm_status(): close\n", __FILE__, __LINE__));

      ap_rprintf(r, "<td align=right>%d</td><td align=right>%d</td>",
		 srun->active_sockets, pool_count);
      ap_rprintf(r, "<td align=right>%d</td><td align=right>%d</td><td align=right>%d</td><td align=right>%d</td>",
		 srun->connect_timeout,
		 srun->idle_timeout,
		 srun->fail_recover_timeout,
		 srun->read_timeout);
      ap_rputs("</tr>\n", r);
    }
  }
  ap_rputs("</table></center>\n", r);
}

static void
caucho_host_status(request_rec *r, config_t *config, resin_host_t *host)
{
  web_app_t *app;
  location_t *loc;
  unsigned int now = time(0);
  
  /* check updates as appropriate */
  cse_match_host(config, host->name, host->port, now);

  if (host->canonical == host)
    ap_rprintf(r, "<h2>");
  else
    ap_rprintf(r, "<h3>");

  if (! host->has_data)
    ap_rprintf(r, "Unconfigured ");
  
  if (host->canonical != host)
    ap_rprintf(r, "Alias ");
  
  if (! *host->name)
    ap_rprintf(r, "Default Virtual Host");
  else if (host->port)
    ap_rprintf(r, "Virtual Host: %s:%d", host->name, host->port);
  else
    ap_rprintf(r, "Virtual Host: %s", host->name);

  if (host->canonical == host) {
  }
  else if (! host->canonical)
    ap_rprintf(r, " -> <font color='red'>null</font>");
  else if (host->canonical->port)
    ap_rprintf(r, " -> %s:%d", host->canonical->name, host->canonical->port);
  else if (! host->canonical->name[0])
    ap_rprintf(r, " -> default");
  else
    ap_rprintf(r, " -> %s", host->canonical->name);
  
  if (host->canonical == host)
    ap_rprintf(r, "</h2>");
  else
    ap_rprintf(r, "</h3>");

  if (host->error_message[0])
    ap_rprintf(r, "<h3 color='red'>Error: %s</h3>\n", host->error_message);

  ap_rprintf(r, "<p style='margin-left:2em'>");

  if (host->config_source[0]) {
    ap_rprintf(r, "<b>Source:</b> %s<br />\n",
	       host->config_source);
  }
  
  ap_rprintf(r, "<b>Last-Update:</b> %s</p><br />\n",
	     ctime(&host->last_update_time));
  ap_rprintf(r, "</p>\n");

  if (host->canonical == host) {
    jvm_status(&host->cluster, r);

    ap_rputs("<p><center><table border=2 cellspacing=0 cellpadding=2 width='80%'>\n", r);
    ap_rputs("<tr><th width=\"50%\">web-app\n", r);
    ap_rputs("    <th>url-pattern\n", r);

    app = host->applications;
    
    for (; app; app = app->next) {
      if (! app->has_data) {
	ap_rprintf(r, "<tr bgcolor='#ffcc66'><td>%s<td>unconfigured</tr>\n", 
		   *app->context_path ? app->context_path : "/");
      }
      
      for (loc = app->locations; loc; loc = loc->next) {
	if (! strcasecmp(loc->prefix, "/META-INF") ||
	    ! strcasecmp(loc->prefix, "/WEB-INF"))
	  continue;
	
	ap_rprintf(r, "<tr bgcolor='#ffcc66'><td>%s<td>%s%s%s%s%s</tr>\n", 
		   *app->context_path ? app->context_path : "/",
		   loc->prefix,
		   ! loc->is_exact && ! loc->suffix ? "/*" : 
		   loc->suffix && loc->prefix[0] ? "/" : "",
		   loc->suffix ? "*" : "",
		   loc->suffix ? loc->suffix : "",
		   loc->ignore ? " (ignore)" : "");
      }
    }
    ap_rputs("</table></center>\n", r);
  }
}

/**
 * Print a summary of the configuration so users can understand what's
 * going on.  Ping the server to check that it's up.
 */
static int
caucho_status(request_rec *r)
{
  config_t *config;
  resin_host_t *host;
  time_t now = time(0);
 
  if (! r->handler || strcmp(r->handler, "caucho-status"))
    return DECLINED;

  config = cse_get_module_config(r);

  if (! config)
    return DECLINED;
  
  r->content_type = "text/html";
  if (r->header_only)
    return OK;

  ap_rputs("<html><title>Status : Caucho Servlet Engine</title>\n", r);
  ap_rputs("<body bgcolor=white>\n", r);
  ap_rputs("<h1>Status : Caucho Servlet Engine</h1>\n", r);

  if (! config)
    return OK;

  if (config->error)
    ap_rprintf(r, "<h2 color='red'>Error : %s</h2>\n", config->error);

  ap_rprintf(r, "<table border='0'>");
  ap_rprintf(r, "<tr><td><b>Start Time</b></td><td>%s</td></tr>\n",
	     ctime(&config->start_time));
  ap_rprintf(r, "<tr><td><b>Now</b></td><td>%s</td></tr>\n",
	     ctime(&now));
  ap_rprintf(r, "<tr><td><b>Session Cookie</b></td><td>'%s'</td></tr>\n",
	     config->session_cookie);
  ap_rprintf(r, "<tr><td><b>Session URL</b></td><td>'%s'</td></tr>\n",
	     config->session_url_prefix);
  ap_rprintf(r, "<tr><td><b>Config Check Interval</b></b></td><td>%ds</td></tr>\n",
	     config->update_timeout);
  if (config->config_path && config->config_path[0]) {
    ap_rprintf(r, "<tr><td><b>Config Cache File</b></td><td>%s</td></tr>\n",
	       config->config_path);
  }
  
  ap_rprintf(r, "</table>");
  
  ap_rprintf(r, "<h2>Configuration Cluster</h2>\n");
  jvm_status(&config->config_cluster, r);
  
  host = config ? config->hosts : 0;
  for (; host; host = host->next) {
    if (host != host->canonical) {
      continue;
    }

    caucho_host_status(r, config, host);
  }

  if (config->manual_host)
    caucho_host_status(r, config, config->manual_host);

  ap_rputs("<hr>", r);
  ap_rprintf(r, "<em>%s<em>", VERSION);
  ap_rputs("</body></html>\n", r);

  return OK;
}

/**
 * Strip the ;jsessionid
 */
static int
cse_strip(request_rec *r)
{
  config_t *config = cse_get_module_config(r);
  const char *uri = r->uri;
  
  if (config == NULL || ! uri)
    return DECLINED;

  if (config->session_url_prefix) {
    char buffer[8192];
    char *new_uri;
    
    new_uri = strstr(uri, config->session_url_prefix);
    
    if (new_uri) {
      *new_uri = 0;
  
      /* Strip session encoding from static files. */
      if (r->filename) {
	char *url_rewrite = strstr(r->filename, config->session_url_prefix);
    
	if (url_rewrite) {
	  *url_rewrite = 0;

	  /*
	    if (stat(r->filename, &r->finfo) < 0)
	    r->finfo.st_mode = 0;
	  */
	}
      }

      if (r->args) {
	sprintf(buffer, "%s?%s", r->uri, r->args);
	
	apr_table_setn(r->headers_out, "Location",
		       ap_construct_url(r->pool, buffer, r));
      }
      else {
	apr_table_setn(r->headers_out, "Location",
		       ap_construct_url(r->pool, r->uri, r));
      }
      
      return HTTP_MOVED_PERMANENTLY;
    }
  }
  
  return DECLINED;
}

/**
 * Look at the request to see if Resin should handle it.
 */
static int
cse_dispatch(request_rec *r)
{
  config_t *config = cse_get_module_config(r);
  const char *host_name = ap_get_server_name(r);
  int port = ap_get_server_port(r);
  const char *uri = r->uri;
  resin_host_t *host;
  unsigned int now;
  int len;

  if (config == NULL || ! uri)
    return DECLINED;

  now = time(0);
 
  LOG(("%s:%d:cse_dispatch(): [%d] host %s\n",
       __FILE__, __LINE__, getpid(), host_name ? host_name : "null"));

  len = strlen(uri);

  /* move back below host */
  if (config->enable_caucho_status &&
      len >= sizeof("/caucho-status") - 1 &&
      ! strcmp(uri + len - sizeof("/caucho-status") + 1, "/caucho-status")) {
    r->handler = "caucho-status";
    return caucho_status(r);
  }
  
  /* Check for exact virtual host match */
  host = cse_match_request(config, host_name, port, uri, 0, now);
  
  if (host || (r->handler && ! strcmp(r->handler, "caucho-request"))) {
    LOG(("%s:%d:cse_dispatch(): [%d] match %s:%s\n",
	 __FILE__, __LINE__, getpid(), host_name ? host_name : "null", uri));

    return caucho_request(r, config, host, now);
  }
  else if (r->handler && ! strcmp(r->handler, "caucho-status")) {
    return caucho_status(r);
  }
  
  if (config->session_url_prefix) {
    return cse_strip(r);
  }

  return DECLINED;
}

/*
 * Only needed configuration is pointer to resin.conf
 */
static command_rec caucho_commands[] = {
    AP_INIT_TAKE12("ResinConfigServer", resin_config_server_command,
		   NULL, RSRC_CONF|ACCESS_CONF,
		   "Adds a configuration server."),
    AP_INIT_TAKE12("ResinHost", cse_host_command,
		   NULL, RSRC_CONF|ACCESS_CONF,
		   "Configures a cluster host for manual configuration."),
    AP_INIT_TAKE12("ResinBackup", cse_backup_command,
		   NULL, RSRC_CONF|ACCESS_CONF,
		   "Configures a cluster host for manual configuration."),
    AP_INIT_TAKE1("ResinConfigCacheDirectory", resin_config_cache_command,
		  NULL, RSRC_CONF|ACCESS_CONF,
		  "Configures the saved configuration file."),
    AP_INIT_TAKE1("ResinSessionCookie", resin_session_cookie_command,
		  NULL, RSRC_CONF|ACCESS_CONF, 
		  "Configures the session cookie."),
    AP_INIT_TAKE1("ResinSessionSticky", resin_session_sticky_command,
		  NULL, RSRC_CONF|ACCESS_CONF, 
		  "Configures the session sticky."),
    AP_INIT_TAKE1("ResinSessionUrlPrefix", resin_session_url_prefix_command,
		  NULL, RSRC_CONF|ACCESS_CONF,
		  "Configures the session url."),
    
    AP_INIT_TAKE1("CauchoStatus", caucho_status_command,
		  NULL, RSRC_CONF|ACCESS_CONF, 
		  "Adds a configuration server."),
    
    AP_INIT_TAKE12("CauchoHost", cse_host_command,
		   NULL, RSRC_CONF|ACCESS_CONF, 
		   "Configures a cluster host for manual configuration."),
    AP_INIT_TAKE12("CauchoBackup", cse_backup_command,
		   NULL, RSRC_CONF|ACCESS_CONF, 
		   "Configures a cluster host for manual configuration."),
    AP_INIT_TAKE1("CauchoConfigCacheDirectory", resin_config_cache_command,
		  NULL, RSRC_CONF|ACCESS_CONF,
		  "Configures the saved configuration file."),
    {NULL}
};

static int
prefork_post_config(apr_pool_t *p, apr_pool_t *plog,
		    apr_pool_t *dummy, server_rec *ptemp)
{
  g_start_time = time(0);
  
  ap_add_version_component(p, VERSION);

  g_ssl_lookup = APR_RETRIEVE_OPTIONAL_FN(ssl_var_lookup);

  return OK;
}

static void caucho_register_hooks(apr_pool_t *p)
{
  ap_hook_post_config(prefork_post_config, NULL, NULL, APR_HOOK_MIDDLE);
  
  ap_hook_handler(cse_dispatch, NULL, NULL, APR_HOOK_FIRST);
}

/* Dispatch list for API hooks */
module AP_MODULE_DECLARE_DATA caucho_module = {
    STANDARD20_MODULE_STUFF, 
    cse_create_dir_config,    /* create per-dir    config structures */
    cse_merge_dir_config,     /* merge  per-dir    config structures */
    cse_create_server_config, /* create per-server config structures */
    cse_merge_server_config,  /* merge  per-server config structures */
    caucho_commands,          /* table of config file commands       */
    caucho_register_hooks     /* register hooks                      */
};

