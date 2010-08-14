/*
 * Copyright (c) 1999-2004 Caucho Technology.  All rights reserved.
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */


#ifndef COMMON_H
#define COMMON_H

#ifndef LOG
#ifdef DEBUG
#define LOG(x) cse_log x
#else
#define LOG(x)
#endif
#endif

char *get_java_home(char *resin_home, char *java_home);
char *get_resin_home(char *resin_home, char *path);
char *get_java_exe(char *java_home);
char *set_classpath(char *cp, char *resin_home, char *java_home, char *env_classpath);
char *rsprintf(char *buf, char *fmt, ...);
void set_window(HWND window);
void die(char *fmt, ...);
void log(char *fmt, ...);
extern "C" {
void cse_log(char *fmt, ...);
}
char *add_classpath(char *cp, char *path);
int start_service(char *name, char *full_name, char *class_name, int argc, char **argv);
char **get_server_args(char *name, char *full_name, char *main, int argc, char **argv);
int spawn_java(char *exe, char **args);
void stop_server();
void start_server();
void quit_server();
void add_path(char *buf, char *path);
void install_service(char *name, char *full_name, char *user, char *password, char **service_args);
void remove_service(char *name);

extern FILE *out;
extern FILE *err;

#endif
