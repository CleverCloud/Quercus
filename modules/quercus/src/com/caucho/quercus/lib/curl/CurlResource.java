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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.BinaryOutput;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CurlResource
{
  private static final Logger log
    = Logger.getLogger(CurlResource.class.getName());
  private static final L10N L = new L10N(CurlResource.class);

  private String _requestMethod = "GET";
  private int _responseCode;

  private String _URL;
  private int _port = -1;

  private String _username;
  private String _password;

  private boolean _isProxying = false;
  private String _proxyUsername;
  private String _proxyPassword;
  private String _proxyURL;
  private String _proxyType = "HTTP";
  private int _proxyPort = -1;

  private boolean _isFollowingRedirects = true;
  private boolean _isReturningBody = true;
  private boolean _isReturningData = false;
  private boolean _isReturningHeader = false;

  private boolean _isVerifySSLPeer = true;
  private boolean _isVerifySSLCommonName = true;
  private boolean _isVerifySSLHostname = true;
  
  private boolean _ifModifiedSince = true;
  private String _modifiedTime;

  private int _errorCode = CurlModule.CURLE_OK;
  private String _error = "";
  private boolean _failOnError = false;
  private boolean _isVerbose = false;

  private int _readTimeout = -1;
  private int _connectTimeout = -1;

  private HashMap<String,String> _requestProperties
    = new HashMap<String, String>();

  private StringValue _header;
  private StringValue _body;
  private Value _postBody;

  private String _contentType;
  private int _contentLength;

  private String _cookie;
  private String _cookieFilename;

  private BinaryOutput _outputFile;
  private BinaryOutput _outputHeaderFile;
  private BinaryInput _uploadFile;
  private int _uploadFileSize;

  private Callable _headerCallback;
  private Callable _passwordCallback;
  private Callable _readCallback;
  private Callable _writeCallback;

  public CurlResource()
  {
  }

  /**
   * Returns body of last transfer.
   */
  public Value getBody()
  {
    return _body;
  }

  /**
   * Sets the body of the last request.
   */
  public void setBody(StringValue body)
  {
    _body = body;
  }

  /**
   * Returns the max time until timeout while establishing a connection.
   */
  public int getConnectTimeout()
  {
    return _connectTimeout;
  }

  /**
   * Sets the max time until timeout while establishing a connection.
   */
  public void setConnectTimeout(int timeout)
  {
    _connectTimeout = timeout;
  }

  /**
   * Returns the length of the body from the last request.
   */
  public int getContentLength()
  {
    return _contentLength;
  }

  /**
   * Sets the length of the body from the last request.
   */
  public void setContentLength(int length)
  {
    _contentLength = length;
  }

  /**
   * Returns the "Content-Type" header from the last request.
   */
  public String getContentType()
  {
    return _contentType;
  }

  /**
   * Sets the "Content-Type" from the last request.
   */
  public void setContentType(String type)
  {
    _contentType = type;
  }

  /**
   * Sets the "Set-Cookie" request property.
   */
  public void setCookie(String cookie)
  {
    _cookie = cookie;
  }

  /**
   * Sets the filename to save the cookies from the last request.
   */
  public void setCookieFilename(String filename)
  {
    _cookieFilename = filename;
  }

  /**
   * Returns the error string from the last request.
   */
  public String getError()
  {
    return _error;
  }

  /**
   * Sets the error string from the last request.
   */
  public void setError(String error)
  {
    _error = error;
  }

  /**
   * Sets the error code from the last request.
   */
  public int getErrorCode()
  {
    return _errorCode;
  }

  /**
   * Returns the error code from the last request.
   */
  public void setErrorCode(int code)
  {
    _errorCode = code;
  }

  /**
   * Set to true to fail on response codes >= 400.
   */
  public void setFailOnError(boolean failOnError)
  {
    _failOnError = failOnError;
  }

  /**
   * Returns the header from the last request.
   */
  public Value getHeader()
  {
    return _header;
  }

  /**
   * Saves the header that was returned by the server.
   */
  public void setHeader(StringValue header)
  {
    _header = header;
  }
  
  /*
   * Returns the header callback.
   */
  public Callable getHeaderCallback()
  {
    return _headerCallback;
  }

  /**
   * Sets the callback to read the header.
   */
  public void setHeaderCallback(Callable callback)
  {
    _headerCallback = callback;
  }

  /**
   * Set to true to set the If-Modified-Since property.
   * Time to use is set with setModifiedTime().
   */
  public void setIfModifiedSince(boolean option)
  {
    _ifModifiedSince = option;
  }

  /**
   * Returns true if automatically following redirects.
   */
  public boolean getIsFollowingRedirects()
  {
    return _isFollowingRedirects;
  }

  /**
   * Set to true to automatically follow redirects.
   */
  public void setIsFollowingRedirects(boolean followRedirects)
  {
    _isFollowingRedirects = followRedirects;
  }

  /**
   * Returns true if a proxy is to be used.
   */
  public boolean getIsProxying()
  {
    return _isProxying;
  }

  /**
   * Set to true to proxy request.
   */
  public void setIsProxying(boolean proxy)
  {
    _isProxying = proxy;
  }

  /**
   * Set to true to return body for this request.
   */
  public void setIsReturningBody(boolean returnBody)
  {
    _isReturningBody = returnBody;
  }

  /**
   * Set to true to return data instead of to stdout.
   */
  public void setIsReturningData(boolean returnData)
  {
    _isReturningData = returnData;
  }

  /**
   * Set to true to return the body from this request.
   */
  public void setIsReturningHeader(boolean returnHeader)
  {
    _isReturningHeader = returnHeader;
  }

  /**
   * Returns the verbosity of this library.
   */
  public boolean getIsVerbose()
  {
    return _isVerbose;
  }

  /**
   * Sets the verbosity of this library.
   */
  public void setIsVerbose(boolean verbose)
  {
    _isVerbose = verbose;
  }
  
  public boolean getIsVerifySSLPeer()
  {
    return _isVerifySSLPeer;
  }
  
  public void setIsVerifySSLPeer(boolean isVerify)
  {
    _isVerifySSLPeer = isVerify;
  }

  public boolean getIsVerifySSLCommonName()
  {
    return _isVerifySSLCommonName;
  }
  
  public void setIsVerifySSLCommonName(boolean isVerify)
  {
    _isVerifySSLCommonName = isVerify;
  }
  
  public boolean getIsVerifySSLHostname()
  {
    return _isVerifySSLHostname;
  }
  
  public void setIsVerifySSLHostname(boolean isVerify)
  {
    _isVerifySSLHostname = isVerify;
  }
  
  /**
   * Sets the modified time request property.
   */
  public void setModifiedTime(String time)
  {
    _modifiedTime = time;
  }

  /**
   * Sets the file to save the data to save from a request.
   */
  public void setOutputFile(BinaryOutput file)
  {
    _outputFile = file;
  }

  /**
   * Sets the file to save the header from a request.
   */
  public void setOutputHeaderFile(BinaryOutput file)
  {
    _outputHeaderFile = file;
  }

  /**
   * Returns the password to use for authentication.
   */
  public String getPassword()
  {
    return _password;
  }

  /**
   * Sets the password to use for authentication.
   */
  public void setPassword(String pwd)
  {
    _password = pwd;
  }

  /**
   *
   */
  public void setPasswordCallback(Callable callback)
  {
    _passwordCallback = callback;
  }

  /**
   * Returns the port to use for this request.
   */
  public int getPort()
  {
    return _port;
  }

  /**
   * Sets the port to use for this request.
   */
  public void setPort(int port)
  {
    _port = port;
  }

  /**
   * Gets the body to POST to the server.
   */
  public Value getPostBody()
  {
    return _postBody;
  }

  /**
   * Sets the body to POST to the server.
   */
  public void setPostBody(Value body)
  {
    _postBody = body;
  }
  
  /**
   * Returns the password to use for proxy authentication.
   */
  public String getProxyPassword()
  {
    return _proxyPassword;
  }

  /**
   * Sets the password to use for proxy authentication.
   */
  public void setProxyPassword(String pass)
  {
    _proxyPassword = pass;
  }

  /**
   * Returns the port to use for the proxy.
   */
  public int getProxyPort()
  {
    return _proxyPort;
  }

  /**
   * Sets the port to use for the proxy.
   */
  public void setProxyPort(int port)
  {
    _proxyPort = port;
  }

  /**
   * Returns of type of the proxy (Http or SOCKS).
   */
  public String getProxyType()
  {
    return _proxyType;
  }

  /**
   * Sets the type of the proxy (Http or SOCKS).
   */
  public void setProxyType(String type)
  {
    _proxyType = type;
  }

  /**
   * Returns the URL of the proxy.
   */
  public String getProxyURL()
  {
    return _proxyURL;
  }

  /**
   * Sets the URL of the proxy.
   */
  public void setProxyURL(String proxy)
  {
    _proxyURL = proxy;
  }

  /**
   * Returns the username to use for proxy authentication.
   */
  public String getProxyUsername()
  {
    return _proxyUsername;
  }

  /**
   * Sets the username to use for proxy authentication.
   */
  public void setProxyUsername(String user)
  {
    _proxyUsername = user;
  }

  /*
   * Returns the callback to read the body.
   */
  public Callable getReadCallback()
  {
    return _readCallback;
  }
  
  /**
   * Sets the callback to read the body.
   */
  public void setReadCallback(Callable callback)
  {
    _readCallback = callback;
  }

  /**
   * Returns the max time until timeout while reading body.
   */
  public int getReadTimeout()
  {
    return _readTimeout;
  }

  /**
   * Sets the max time until timeout while reading body.
   */
  public void setReadTimeout(int timeout)
  {
    _readTimeout = timeout;
  }

  /**
   * Returns the current request method.
   */
  public String getRequestMethod()
  {
    return _requestMethod;
  }

  /**
   * Sets the request method to use for this request.
   */
  public void setRequestMethod(String method)
  {
    _requestMethod = method;
  }

  /**
   * Returns a map of all the request properties.
   */
  public HashMap<String,String> getRequestPropertiesMap()
  {
    return _requestProperties;
  }

  /**
   * Returns all the request properties as a String.
   */
  public Value getRequestProperties(Env env)
  {
    StringValue bb = env.createBinaryBuilder();

    for (Map.Entry<String,String> entry : _requestProperties.entrySet()) {
      bb.append(entry.getKey());
      bb.append(": ");
      bb.append(entry.getValue());
      bb.append("\r\n");
    }

    bb.append("\r\n");

    return bb;
  }

  /**
   * Sets a request property to use for this request.
   */
  public void setRequestProperty(String key, String value)
  {
    _requestProperties.put(key, value);
  }

  /**
   * Returns the response code for the last request.
   */
  public int getResponseCode()
  {
    return _responseCode;
  }

  /**
   * Sets the response code for the last request.
   */
  public void setResponseCode(int code)
  {
    _responseCode = code;
  }

  /**
   * Returns handle of file to upload.
   */
  public BinaryInput getUploadFile()
  {
    return _uploadFile;
  }

  /**
   * Sets handle of file to upload.
   */
  public void setUploadFile(BinaryInput file)
  {
    _uploadFile = file;
  }

  /**
   * Returns size of file to upload.
   */
  public int getUploadFileSize()
  {
    return _uploadFileSize;
  }

  /**
   * Sets size of file to upload.
   */
  public void setUploadFileSize(int size)
  {
    _uploadFileSize = size;
  }

  /**
   * Gets the URL to use for this request.
   */
  public String getURL()
  {
    return _URL;
  }

  /**
   * Sets the URL to use for this request.
   */
  public void setURL(String url)
  {
    _URL = url;
  }

  /**
   * Gets the username to use for authentication.
   */
  public String getUsername()
  {
    return _username;
  }

  /**
   * Sets the username to use for authentication.
   */
  public void setUsername(String user)
  {
    _username = user;
  }

  /**
   *
   */
  public void setWriteCallback(Callable callback)
  {
    _writeCallback = callback;
  }

  /**
   * Remove a request property.
   */
  public void removeRequestProperty(String key)
  {
    _requestProperties.remove(key);
  }


  /**
   * Finalizes the request properties for this connection.
   */
  private void init()
  {
    _error = null;
    _errorCode = CurlModule.CURLE_OK;

    if (_modifiedTime != null) {
      if (_ifModifiedSince) {
        removeRequestProperty("If-Unmodified-Since");
        setRequestProperty("If-Modified-Since", _modifiedTime);
      }
      else {
        removeRequestProperty("If-Modified-Since");
        setRequestProperty("If-Unmodified-Since", _modifiedTime);
      }
    }

    if (_cookie != null)
      setRequestProperty("Cookie", _cookie);
    else
      removeRequestProperty("Cookie");
  }

  /**
   * Executes this request.
   */
  public Value execute(Env env)
  {
    init();

    HttpRequest httpRequest = HttpRequest.getRequest(this);

    env.addCleanup(httpRequest);

    if (! httpRequest.execute(env))
      return BooleanValue.FALSE;

    //if (hasError())
      //return BooleanValue.FALSE;

    if (_cookie != null && _cookieFilename != null)
      saveCookie(env);

    return getReturnValue(env);
  }

  /**
   * Returns headers and/or body of the last request.
   */
  private Value getReturnValue(Env env)
  {
    StringValue data;

    if (_responseCode == HttpURLConnection.HTTP_NOT_MODIFIED
        || _responseCode == HttpURLConnection.HTTP_PRECON_FAILED
        || (_failOnError && _responseCode >= 400)) {
      if (_isReturningHeader)
        data = _header;
      else
        return BooleanValue.TRUE;
    }
    else {
      StringValue bb = env.createBinaryBuilder();

      if (_isReturningHeader)
        bb.append(_header);

      if (_isReturningBody)
        bb.append(_body);

      data = bb;
    }

    if (_isReturningData)
      return data;

    if (_outputHeaderFile != null) {
      FileModule.fwrite(env,
                        _outputHeaderFile,
                        _header.toInputStream(),
                        Integer.MAX_VALUE);
    }

    if (_outputFile != null) {
      FileModule.fwrite(env,
                        _outputFile,
                        data.toInputStream(),
                        Integer.MAX_VALUE);

    }
    else {
      env.print(data);
    }

    return BooleanValue.TRUE;
  }

  /**
   * Save the cookies from the last request.
   */
  private void saveCookie(Env env)
  {
    WriteStream out = null;

    try {
      Path path = env.getPwd().lookup(_cookieFilename);

      out = path.openWrite();

      int len = _cookie.length();

      for (int i = 0; i < len; i++) {
        out.write((byte)_cookie.charAt(i));
      }
    }
    catch (IOException e) {
      throw new QuercusModuleException(e);
    }
    finally {
      try {
        if (out != null)
          out.close();
      }
      catch (IOException e) {
        // intentionally don't do anything
      }
    }
  }

  /**
   *
   */
  public void close()
  {
  }

  /**
   * Returns true if an error occuring during the last operation.
   */
  protected boolean hasError()
  {
    return _errorCode != CurlModule.CURLE_OK;
  }

  /**
   * Returns a copy of this resource.
   */
  public CurlResource clone()
  {
    CurlResource curl = new CurlResource();

    curl.setBody(_body);
    curl.setConnectTimeout(_connectTimeout);
    curl.setContentLength(_contentLength);
    curl.setContentType(_contentType);
    curl.setCookie(_cookie);
    curl.setCookieFilename(_cookieFilename);
    curl.setError(_error);
    curl.setErrorCode(_errorCode);
    curl.setFailOnError(_failOnError);
    curl.setHeaderCallback(_headerCallback);
    curl.setHeader(_header);
    curl.setIsFollowingRedirects(_isFollowingRedirects);
    curl.setIfModifiedSince(_ifModifiedSince);
    curl.setIsProxying(_isProxying);
    curl.setIsReturningBody(_isReturningBody);
    curl.setIsReturningData(_isReturningData);
    curl.setIsReturningHeader(_isReturningHeader);
    curl.setIsVerbose(_isVerbose);
    curl.setModifiedTime(_modifiedTime);
    curl.setOutputFile(_outputFile);
    curl.setOutputHeaderFile(_outputHeaderFile);
    curl.setPassword(_password);
    curl.setPasswordCallback(_passwordCallback);
    curl.setPort(_port);
    curl.setPostBody(_postBody);
    curl.setProxyPassword(_proxyPassword);
    curl.setProxyPort(_proxyPort);
    curl.setProxyType(_proxyType);
    curl.setProxyURL(_proxyURL);
    curl.setProxyUsername(_proxyUsername);
    curl.setReadCallback(_readCallback);
    curl.setReadTimeout(_readTimeout);
    curl.setRequestMethod(_requestMethod);

    for (Map.Entry<String,String> entry : _requestProperties.entrySet()) {
      curl.setRequestProperty(entry.getKey(), entry.getValue());
    }

    curl.setResponseCode(_responseCode);
    curl.setUploadFile(_uploadFile);
    curl.setUploadFileSize(_uploadFileSize);
    curl.setURL(_URL);
    curl.setUsername(_username);
    curl.setWriteCallback(_writeCallback);

    return curl;
  }

  public String toString()
  {
    return "CurlResource[" + _requestMethod + "]";
  }

}
