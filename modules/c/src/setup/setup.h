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

#ifndef SETUP_H
#define SETUP_H

void set_resin_home(char *resin_home);
int get_iis_script_dir(char *name);
char *configure_iis(HWND hDlg, char *resin_home, char *iis_script);
char *remove_iis(HWND hDlg);
char *stop_service(char *name);
char *start_service(char *name);
void log(char *fmt, ...);
char *get_apache_home();
char *configure_apache(HWND hDlg, char *resin_home, char *apache_home);
char *get_netscape_home();
char *configure_netscape(HWND hDlg, char *resin_home, char *netscape_home);
char *get_website_home();
char *configure_website(HWND hDlg, char *resin_home, char *netscape_home);
char *rsprintf(char *buf, char *fmt, ...);

HKEY reg_lookup(HKEY hkey, char *path);
void reg_set_string(char *path, char *name, char *value);

#endif
