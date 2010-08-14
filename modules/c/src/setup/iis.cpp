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

#define UNICODE
#define INITGUID
#include <windows.h>
#include <cguid.h>
#include <stdio.h>
#include "iadmw.h"
#include "iiscnfg.h" 
#include "atlBase.h" 
#include "setup.h"

#define IIS_SERVICE "W3SVC"
#define IIS_ROOTS "System\\CurrentControlSet\\Services\\W3SVC\\Parameters\\Virtual Roots"
#define IIS_PARAM "System\\CurrentControlSet\\Services\\W3SVC\\Parameters"

static int
get_string(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, int userType, int code, char *data)
{
	HRESULT hRes = 0; 
	DWORD indx = 0; 
	DWORD len = 0;
	METADATA_RECORD MyData;
	TCHAR Data[64 * 1024];

	MyData.dwMDIdentifier = code;
	MyData.dwMDAttributes = METADATA_INHERIT;
	MyData.dwMDUserType = userType;
	MyData.dwMDDataType = STRING_METADATA;
	MyData.dwMDDataLen = sizeof(Data);
	MyData.pbMDData = (unsigned char *) Data;
	len = sizeof(Data);

	hRes = pIMeta->GetData(MyHandle, path, &MyData, &len); 

	if (FAILED(hRes)) {
		return -1;
	}

	wcstombs(data, Data, sizeof(Data));

	return MyData.dwMDDataLen;
}

static int
add_multi_string(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, int userType, int code, char *data)
{
	HRESULT hRes = 0; 
	DWORD indx = 0; 
	DWORD len = 0;
	METADATA_RECORD MyData;
	TCHAR Data[64 * 1024];
	TCHAR newData[1024];

	int newLen = (strlen(data) + 1) * sizeof(TCHAR);
	mbstowcs(newData, data, 256);

	MyData.dwMDIdentifier = code;
	MyData.dwMDAttributes = METADATA_INHERIT;
	MyData.dwMDUserType = userType;
	MyData.dwMDDataType = MULTISZ_METADATA;
	MyData.dwMDDataLen = sizeof(Data);
	MyData.pbMDData = (unsigned char *) Data;
	len = sizeof(Data);

	hRes = pIMeta->GetData(MyHandle, path, &MyData, &len); 

	if (FAILED(hRes)) {
		return -1;
	}

	TCHAR *ptr;

	for (ptr = Data; *ptr; ptr += wcslen(ptr) + 1) {
		if (! memcmp(ptr, newData, newLen)) {
			return -1;
		}
	}

	memcpy(ptr, newData, newLen);
	ptr[newLen + 1] = 0;

	MyData.dwMDDataLen += newLen;

	hRes = pIMeta->SetData(MyHandle, path, &MyData); 

	return MyData.dwMDDataLen;
}

static int
get_iis_script_dir_from_metabase(char *dir)
{
	HRESULT hRes = 0; 
	DWORD indx = 0; 
	DWORD len = 0;
	METADATA_HANDLE MyHandle; 
	CComPtr <IMSAdminBase> pIMeta;  

	*dir = 0;

	CoInitialize(0);
	hRes = CoCreateInstance(CLSID_MSAdminBase, NULL, CLSCTX_ALL,
							IID_IMSAdminBase, (void **) &pIMeta);
	
	if (FAILED(hRes)) {
		return 0;
	}

	//get a handle to the local machine 
	hRes = pIMeta->OpenKey(METADATA_MASTER_ROOT_HANDLE, TEXT("/LM/W3SVC/1/ROOT/SCRIPTS"),
						   METADATA_PERMISSION_READ, 20, &MyHandle); 

	int result = -1;
	if (! FAILED(hRes)) {
		result = get_string(pIMeta, MyHandle, TEXT("/"), IIS_MD_UT_FILE, MD_VR_PATH, dir);
	}

	pIMeta->CloseKey(MyHandle);

	if (result > 0)
	  return result;
	
	//get a handle to the local machine 
	hRes = pIMeta->OpenKey(METADATA_MASTER_ROOT_HANDLE, TEXT("/LM/W3SVC/1/ROOT"),
						   METADATA_PERMISSION_READ, 20, &MyHandle); 

	if (! FAILED(hRes)) {
	  char *tail;
	  
		result = get_string(pIMeta, MyHandle, TEXT("/"), IIS_MD_UT_FILE, MD_VR_PATH, dir);
		tail = strrchr(dir, '\\');
		if (tail)
		  *tail = 0;
		else {
		  tail = strrchr(dir, '/');
		  if (tail)
		    *tail = 0;
		}

		strcat(dir, "\\scripts");
	}

	pIMeta->CloseKey(MyHandle);

	return result > 0;
}

static HKEY
reg_lookup(HKEY hkey, TCHAR *path)
{
	HKEY newKey;
	DWORD rc;

	rc = RegOpenKeyEx(hkey, path, 0, KEY_QUERY_VALUE, &newKey);
	if (rc != ERROR_SUCCESS)
		return 0;

	return newKey;
}

static char *
reg_query_string(HKEY key, TCHAR *subkey, char *value)
{
	TCHAR buf[1024];
	DWORD len = sizeof buf;
	DWORD type;

	int rc = RegQueryValueEx(key, subkey, 0, &type, (LPBYTE) buf, &len);

	if (rc != ERROR_SUCCESS || type != REG_SZ)
		return 0;

	wcstombs(value, buf, 1024);

	return value;
}

int
get_iis_script_dir(char *dir)
{
	*dir = 0;

	if (get_iis_script_dir_from_metabase(dir)) {
		return 1;
	}

	HKEY hKeyRoot = reg_lookup(HKEY_LOCAL_MACHINE, TEXT(IIS_ROOTS));
	if (! hKeyRoot) {
		strcpy(dir, "");
		return 0;
	}

	if (reg_query_string(hKeyRoot, TEXT("/SCRIPTS"), dir))
		return 1;

	if (reg_query_string(hKeyRoot, TEXT("/"), dir))
		return 1;

	strcpy(dir, "unknown");

	return 1;
}

static int
set_dword_data(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, int code, int type, DWORD data)
{
	HRESULT hRes = 0; 
	METADATA_RECORD MyData;

	MyData.dwMDIdentifier = code;
	MyData.dwMDAttributes = METADATA_INHERIT;
	MyData.dwMDUserType = type;
	MyData.dwMDDataType = DWORD_METADATA;
	MyData.dwMDDataLen = sizeof(data);
	MyData.pbMDData = (unsigned char *) &data;
	hRes = pIMeta->SetData(MyHandle, path, &MyData); 

	return ! FAILED(hRes);
}

static int
set_boolean_data(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, int code, int type, DWORD data)
{
	HRESULT hRes = 0; 
	METADATA_RECORD MyData;

	MyData.dwMDIdentifier = code;
	MyData.dwMDAttributes = METADATA_INHERIT;
	MyData.dwMDUserType = type;
	MyData.dwMDDataType = DWORD_METADATA;
	MyData.dwMDDataLen = sizeof(data);
	MyData.pbMDData = (unsigned char *) &data;
	hRes = pIMeta->SetData(MyHandle, path, &MyData); 

	return ! FAILED(hRes);
}
static int
set_dword(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, int code, DWORD data)
{
	return set_dword_data(pIMeta, MyHandle, path, code, IIS_MD_UT_FILE, data);
}

static int
set_data_len(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, int code, int type, 
		char *data, int len)
{
	HRESULT hRes = 0; 
	METADATA_RECORD MyData;
	TCHAR buf[1024];

	mbstowcs(buf, data, 256);

	memset(&MyData, 0, sizeof(MyData));

	MyData.dwMDIdentifier = code;
	MyData.dwMDAttributes = METADATA_INHERIT;
	MyData.dwMDUserType = type;
	MyData.dwMDDataType = STRING_METADATA;
	MyData.dwMDDataLen = len * sizeof(WCHAR);
	MyData.pbMDData = (unsigned char *) buf;

	hRes = pIMeta->SetData(MyHandle, path, &MyData);

	return ! FAILED(hRes);
}
static int
set_data(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, int code, int type, 
		char *data)
{
	return set_data_len(pIMeta, MyHandle, path, code, type, data, strlen(data) + 1);
}

static int
set_string(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, int code, char *data)
{
	return set_data_len(pIMeta, MyHandle, path, code, IIS_MD_UT_FILE, data, strlen(data) + 1);
}

static int
set_type(CComPtr<IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle, TCHAR *path, char *data)
{
	HRESULT hRes = 0; 
	METADATA_RECORD MyData;
	TCHAR buf[1024];

	mbstowcs(buf, data, 256);

	memset(&MyData, 0, sizeof(MyData));
	MyData.dwMDIdentifier = MD_KEY_TYPE;
	MyData.dwMDAttributes = METADATA_NO_ATTRIBUTES;
	MyData.dwMDUserType = IIS_MD_UT_SERVER;
	MyData.dwMDDataType = STRING_METADATA;
	MyData.dwMDDataLen = (wcslen(buf) + 1) * sizeof(WCHAR);
	MyData.pbMDData = (unsigned char *) buf;

	hRes = pIMeta->SetData(MyHandle, path, &MyData);
	return ! FAILED(hRes);
}

static int
add_resin_script_permission(char *script_dir)
{
	METADATA_HANDLE MyHandle; 
	CComPtr <IMSAdminBase> pIMeta;
	char data[1024];

	*data = 0;

	CoInitialize(0);
	HRESULT hRes;
	hRes = CoCreateInstance(CLSID_MSAdminBase, NULL, CLSCTX_ALL,
							IID_IMSAdminBase, (void **) &pIMeta);
	
	if (FAILED(hRes)) {
		return 0;
	}

	//get a handle to the local machine 
	hRes = pIMeta->OpenKey(METADATA_MASTER_ROOT_HANDLE, TEXT("/LM/W3SVC"),
						   METADATA_PERMISSION_READ|METADATA_PERMISSION_WRITE, 20, &MyHandle); 

	if (FAILED(hRes)) {
		return 0;
	}

	sprintf(data, "1,%s\\isapi_srun.dll,1,,Resin", script_dir);

	add_multi_string(pIMeta, MyHandle, TEXT("/"), IIS_MD_UT_SERVER, MD_WEB_SVC_EXT_RESTRICTION_LIST, data);

	pIMeta->CloseKey(MyHandle);

	return 1;
}

static char *
add_resin_script(CComPtr <IMSAdminBase> pIMeta, TCHAR *name, char *script_path)
{
	HRESULT hRes = 0; 
	METADATA_HANDLE MyHandle;
	TCHAR *scripts = TEXT("/scripts");
	char data[256];

	hRes = pIMeta->OpenKey(METADATA_MASTER_ROOT_HANDLE, name,
			       METADATA_PERMISSION_READ|METADATA_PERMISSION_WRITE, 20, &MyHandle);
	if (FAILED(hRes)) {
		return "Can't open script";
	}

	data[0] = 0;
	if (get_string(pIMeta, MyHandle, scripts, IIS_MD_UT_SERVER, MD_KEY_TYPE, data) < 0) {
		pIMeta->AddKey(MyHandle, scripts);
	}
	set_type(pIMeta, MyHandle, scripts, "IIsWebVirtualDir");
	set_data(pIMeta, MyHandle, scripts, MD_APP_ROOT, IIS_MD_UT_FILE, "/LM/W3SVC/1/Root/scripts");
	set_data(pIMeta, MyHandle, scripts, MD_VR_PATH, IIS_MD_UT_FILE, script_path);
	set_data(pIMeta, MyHandle, scripts, MD_APP_FRIENDLY_NAME, IIS_MD_UT_WAM, "scripts");
	set_dword(pIMeta, MyHandle, scripts, MD_ACCESS_PERM, MD_ACCESS_EXECUTE);
	set_dword_data(pIMeta, MyHandle, scripts, MD_APP_ISOLATED, IIS_MD_UT_WAM, 2);

	pIMeta->CloseKey(MyHandle);

	return 0;
}

static char *
add_resin_filter(CComPtr <IMSAdminBase> pIMeta, TCHAR *name, char *filter_path)
{
	HRESULT hRes = 0; 
	METADATA_HANDLE MyHandle; 
	char data[256];

	hRes = pIMeta->OpenKey(METADATA_MASTER_ROOT_HANDLE, name,
					       METADATA_PERMISSION_READ|METADATA_PERMISSION_WRITE, 20, &MyHandle);
	if (FAILED(hRes)) {
		return "Can't open filter";
	}

	if (get_string(pIMeta, MyHandle, TEXT("/Resin"), IIS_MD_UT_SERVER, MD_KEY_TYPE, data) < 0) {
		pIMeta->AddKey(MyHandle, TEXT("/Resin"));
	}

	set_type(pIMeta, MyHandle, TEXT("/Resin"), "IIsFilter");
	set_boolean_data(pIMeta, MyHandle, TEXT("/Resin"), MD_FILTER_ENABLED, IIS_MD_UT_SERVER, 1);
	set_data(pIMeta, MyHandle, TEXT("/Resin"), MD_FILTER_IMAGE_PATH, IIS_MD_UT_SERVER, filter_path);
	char filterLoad[1024];
	char newFilter[1024];
	if (get_string(pIMeta, MyHandle, TEXT("/"), IIS_MD_UT_SERVER, MD_FILTER_LOAD_ORDER, filterLoad) < 0)
		filterLoad[0] = 0;

	if (! strstr(filterLoad, "Resin")) {
		if (! filterLoad[0])
			strcpy(newFilter, "Resin");
		else {
			strcpy(newFilter, "Resin,");
			strcat(newFilter, filterLoad);
		}

		if (! set_data(pIMeta, MyHandle, TEXT("/"), MD_FILTER_LOAD_ORDER, IIS_MD_UT_SERVER, newFilter)) {
			return "Can't set IIS filter order";
		}
	}

	pIMeta->CloseKey(MyHandle);

	return 0;
}

static char *
add_resin_filter_metabase(HWND hDlg, char *filter_path)
{
	HRESULT hRes = 0; 
	DWORD indx = 0;
	DWORD len = 0;
	CComPtr <IMSAdminBase> pIMeta;  

	CoInitialize(0);
	hRes = CoCreateInstance(CLSID_MSAdminBase, NULL, CLSCTX_ALL,
							IID_IMSAdminBase, (void **) &pIMeta);
	
	if (FAILED(hRes))
		return "Can't open metabase";

	char *result = add_resin_filter(pIMeta, TEXT("/LM/W3SVC/Filters"), filter_path);
	if (! result)
		return 0;

	// Try again?
	result = add_resin_filter(pIMeta, TEXT("/LM/W3SVC/Filters"), filter_path);

	return result;
}

static char *
add_resin_script_metabase(HWND hDlg, char *script_path)
{
	HRESULT hRes = 0; 
	DWORD indx = 0;
	DWORD len = 0;
	CComPtr <IMSAdminBase> pIMeta;  

	CoInitialize(0);
	hRes = CoCreateInstance(CLSID_MSAdminBase, NULL, CLSCTX_ALL,
							IID_IMSAdminBase, (void **) &pIMeta);
	
	if (FAILED(hRes))
		return "Can't open metabase";

	char *result = add_resin_script(pIMeta, TEXT("/LM/W3SVC/1/ROOT"),
					script_path);

	return result;
}

static char *
add_resin_filter_registry(HWND hDlg, char *filter_path)
{
	char buf[1024];
	HKEY hKeyParams;
	
	buf[0] = 0;
	if (! (hKeyParams = reg_lookup(HKEY_LOCAL_MACHINE, IIS_PARAM)))
		return "Can't open IIS Parameters";

	reg_query_string(hKeyParams, TEXT("Filter DLLs"), buf);

	RegCloseKey(hKeyParams);

	if (strstr("isapi_srun.dll", buf))
		return 0;

	if (buf[0])
		strcat(buf, ",");

	strcat(buf, "isapi_srun.dll");

	reg_set_string(IIS_PARAM, "Filter DLLs", buf);


	return 0;
}

static char *
remove_resin_filter(CComPtr <IMSAdminBase> pIMeta, METADATA_HANDLE MyHandle)
{
	set_dword(pIMeta, MyHandle, TEXT("/Resin"), MD_FILTER_ENABLED, 0);
	pIMeta->DeleteKey(MyHandle, TEXT("/Resin"));

	char filterLoad[1024];
	if (! get_string(pIMeta, MyHandle, TEXT("/"), IIS_MD_UT_SERVER, MD_FILTER_LOAD_ORDER, filterLoad))
		return 0;

	char *resin = strstr(filterLoad, "Resin");
	if (! resin)
		return 0;

	char newFilter[1024];
	memset(newFilter, 0, sizeof(newFilter));
	if (filterLoad != resin) {
		int i = -1;
		for (; resin - filterLoad + i >= 0 && (resin[i] == ' ' || resin[i] == ','); i--) {
		}
		strncpy(newFilter, filterLoad, resin - filterLoad + i + 1);
		newFilter[resin - filterLoad + i + 1] = 0;
		strcat(newFilter, resin + 5);
	}
	else {
		int i;
		for (i = 5; resin[i] == ' ' || resin[i] == ','; i++) {
		}
		strcpy(newFilter, resin + i);
	}

	if (! set_data(pIMeta, MyHandle, TEXT("/"), MD_FILTER_LOAD_ORDER, IIS_MD_UT_SERVER, newFilter))
		return "Can't set IIS filter order";

	return 0;
}

static char *
remove_resin_filter_metabase(HWND hDlg)
{
	HRESULT hRes = 0; 
	DWORD indx = 0;
	DWORD len = 0;
	METADATA_HANDLE MyHandle; 
	CComPtr <IMSAdminBase> pIMeta;  

	CoInitialize(0);
	hRes = CoCreateInstance(CLSID_MSAdminBase, NULL, CLSCTX_ALL,
							IID_IMSAdminBase, (void **) &pIMeta);
	
	if (FAILED(hRes))
		return "Can't open IIS metabase";

	TCHAR *filter;
	//get a handle to the local machine 
	filter = TEXT("/LM/W3SVC/Filters");
	hRes = pIMeta->OpenKey(METADATA_MASTER_ROOT_HANDLE, filter,
					       METADATA_PERMISSION_READ|METADATA_PERMISSION_WRITE, 20, &MyHandle);
	int hasFilter = 0;
	char *result = 0;
	if (! FAILED(hRes)) {
		hasFilter = 1;
		result = remove_resin_filter(pIMeta, MyHandle);
		pIMeta->CloseKey(MyHandle);
	}

	filter = TEXT("/LM/W3SVC/1/Filters");
	hRes = pIMeta->OpenKey(METADATA_MASTER_ROOT_HANDLE, filter,
					       METADATA_PERMISSION_READ|METADATA_PERMISSION_WRITE, 20, &MyHandle); 
	if (! FAILED(hRes)) {
		hasFilter = 1;
		result = remove_resin_filter(pIMeta, MyHandle);
		pIMeta->CloseKey(MyHandle);
	}

	return 0;
}

char *
remove_iis(HWND hDlg)
{
	char *msg = remove_resin_filter_metabase(hDlg);
	if (msg)
		return msg;

	int is_nt = (GetVersion() & 0x80000000) == 0; 
	if (is_nt) {
		if (MessageBox(hDlg, TEXT("Do you want to restart IIS?"), TEXT("Restart IIS?"), MB_OKCANCEL) == IDCANCEL)
			return 0;
		
		msg = stop_service(IIS_SERVICE);

		if (! msg)
			start_service(IIS_SERVICE);
	}
	else {
		MessageBox(hDlg, TEXT("You will need to restart IIS to remove Resin.  You may need to reboot to see the changes."), TEXT("Restart"), MB_OK);
	}

	return msg;
}

static char *
copy_filter(HWND hDlg, char *resin_home, char *isapi_script, int *iis_restart,
			int is_nt)
{
	char src_name[1024];
	char dst_name[1024];
	FILE *src_file;
	FILE *dst_file;
	char buf[1024];
	int len;

	*iis_restart = 0;

	sprintf(src_name, "%s/win32/isapi_srun.dll", resin_home);
	sprintf(dst_name, "%s\\isapi_srun.dll", isapi_script);

	dst_file = fopen(dst_name, "w+b");
	if (! dst_file) {
		if (! is_nt)
			MessageBox(hDlg, TEXT("You must stop PWS for setup to copy the new iis_srun filter.  You may need to press 'Remove', reboot, and then install Resin."), TEXT("Stop PWS"), MB_OK);
		else if (MessageBox(hDlg, TEXT("Setup needs to stop IIS to copy the ISAPI filter."), TEXT("Stop IIS?"), MB_OKCANCEL) == IDCANCEL)
			return 0;
		
		if (is_nt) {
			char *msg = stop_service(IIS_SERVICE);
			if (msg)
				return msg;
			*iis_restart = 1;
		}
		dst_file = fopen(dst_name, "w+b");	
	}

	if (! dst_file)
		return "Can't create isapi_srun.dll in IIS script directory";

	src_file = fopen(src_name, "rb");
	if (! src_file) {
		fclose(dst_file);
		return "Can't open isapi_srun.dll in RESIN_HOME";
	}

	while ((len = fread(buf, 1, sizeof(buf), src_file)) > 0) {
		fwrite(buf, 1, len, dst_file);
	}

	fclose(src_file);
	fclose(dst_file);

	return 0;
}

char *
configure_iis(HWND hDlg, char *resin_home, char *isapi_script)
{
	char filter[1024];
	int is_nt = (GetVersion() & 0x80000000) == 0; 
        
	sprintf(filter, "%s\\isapi_srun.dll", isapi_script);
	char *metabase_msg = 0;

	add_resin_script_permission(isapi_script);

	char *script = add_resin_script_metabase(hDlg, isapi_script);
	metabase_msg = add_resin_filter_metabase(hDlg, filter);
	if (metabase_msg)
		add_resin_filter_registry(hDlg, isapi_script);

	int iis_restart = 0;
	char *msg = copy_filter(hDlg, resin_home, isapi_script, &iis_restart, is_nt);

	if (! is_nt) {
	}
	else if (iis_restart)
		start_service(IIS_SERVICE);
	else if (! msg) {
		if (MessageBox(hDlg, TEXT("Do you want to restart IIS to use the new filter?"), TEXT("Restart IIS?"), MB_OKCANCEL) == IDCANCEL)
			return 0;
		
		char *msg = stop_service(IIS_SERVICE);

		if (! msg)
			start_service(IIS_SERVICE);
	}

	if (! msg)
		return metabase_msg;
	else
		return msg;
}
