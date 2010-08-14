/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.filters;

import com.caucho.util.FreeList;
import com.caucho.util.RuntimeExceptionWrapper;
import com.caucho.vfs.GzipStream;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Compresses the response output if the browser accepts compression.
 *
 * <p/>Browsers which support gzip compression will set the Accept-Encoding
 * header.  If GzipFilter detects the gzip compression, it will compress
 * the output.
 *
 * <p/>GzipFilter will always set the "Vary: Accept-Encoding" header because
 * the output depends on the request </p>
 *
 * @since Resin 2.0.6
 */
public class GzipFilter implements Filter {
  private final FreeList<GzipResponse> _freeList
    = new FreeList<GzipResponse>(16);
  
  private final FreeList<GzipPlainResponse> _plainFreeList
    = new FreeList<GzipPlainResponse>(16);

  private static final int NONE = 0;
  private static final int GZIP = 1;
  private static final int DEFLATE = 2;

  private static final AllowEntry ALLOW = new AllowEntry();
  private static final AllowEntry DENY = new AllowEntry();
  
  private ServletContext _app;
  private boolean _embedError;
  private boolean _useVary = true;
  private boolean _noCache = false;

  private HashMap<String,AllowEntry> _contentTypeMap;
  private boolean _hasDeny;

  /**
   * Set true if the vary support should be enabled.
   */
  public void setUseVary(boolean useVary)
  {
    _useVary = useVary;
  }

  /**
   * Set true if the output should not be cached.
   */
  public void setNoCache(boolean noCache)
  {
    _noCache = noCache;
  }

  /**
   * Set true if errors should be embedded in the output.
   */
  public void setEmbedErrorInOutput(boolean embedError)
  {
    _embedError = embedError;
  }

  /**
   * Adds an allowed content type.
   */
  public void addAllowContentType(String type)
  {
    if (_contentTypeMap == null)
      _contentTypeMap = new HashMap<String,AllowEntry>();

    _contentTypeMap.put(type, ALLOW);
  }

  /**
   * Adds a deny content type.
   */
  public void addDenyContentType(String type)
  {
    if (_contentTypeMap == null)
      _contentTypeMap = new HashMap<String,AllowEntry>();

    _hasDeny = true;
    _contentTypeMap.put(type, DENY);
  }
  
  public void init(FilterConfig config)
    throws ServletException
  {
    _app = config.getServletContext();
    _embedError = "true".equals(config.getInitParameter("embed-error-in-output"));
    String value = config.getInitParameter("use-vary");

    if (value == null) {
    }
    else if ("false".equals(value))
      _useVary = false;
    else if ("false".equals(value))
      _useVary = true;
    
    value = config.getInitParameter("no-cache");

    if (value == null) {
    }
    else if ("true".equals(value))
      _noCache = true;
    else if ("false".equals(value))
      _noCache = true;
  }
  
  /**
   * Creates a wrapper to compress the output.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    int encoding = allowGzip(req, res);
    
    if (encoding != NONE) {
      GzipResponse gzipResponse = _freeList.allocate();
      
      if (gzipResponse == null)
        gzipResponse = new GzipResponse();
      
      gzipResponse.setUseDeflate(encoding == DEFLATE);
      gzipResponse.init(res);

      try {
        nextFilter.doFilter(req, gzipResponse);
      }
      catch (Exception e) {
        handleError(e, gzipResponse);
      }

      gzipResponse.close();
      _freeList.free(gzipResponse);
    }
    else {
      GzipPlainResponse plainRes = _plainFreeList.allocate();

      if (plainRes == null)
        plainRes = new GzipPlainResponse();

      plainRes.init(res);
      // addVaryHeader(res);
    
      nextFilter.doFilter(req, plainRes);

      plainRes.close();

      _plainFreeList.free(plainRes);
    }
  }

  protected void addVaryHeader(HttpServletResponse response)
  {
    if (_noCache)
      response.setHeader("Cache-Control", "no-cache");
    else if (_useVary) {
      // #3043, server/183q
      if (! response.containsHeader("Vary"))
        response.addHeader("Vary", "Accept-Encoding");
    }
    else
      response.setHeader("Cache-Control", "private");
  }

  /**
   * Returns true if the GZip is allowed.
   */
  protected int allowGzip(HttpServletRequest req,
                          HttpServletResponse res)
  {
    String acceptEncoding = req.getHeader("Accept-Encoding");

    if (acceptEncoding == null)
      return NONE;
    else if (req.getHeader("Range") != null)
      return NONE;
    else if (acceptEncoding.indexOf("gzip") >= 0)
      return GZIP;
    else if (acceptEncoding.indexOf("deflate") >= 0)
      return DEFLATE;
    else
      return NONE;
  }
  
  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }
  
  private void handleError(Exception e, CauchoResponseWrapper res)
    throws ServletException, IOException
  {
    if (_embedError && res.isCommitted()) {
      _app.log(e.getMessage(), e);
      
      CharArrayWriter writer = new CharArrayWriter();
      PrintWriter pw = new PrintWriter(writer);
      e.printStackTrace(pw);
      pw.flush();
      
      res.getWriter().print(writer.toCharArray());
    }
    else if (e instanceof ServletException)
      throw (ServletException) e;
    else if (e instanceof IOException)
      throw (IOException) e;
    else
      throw RuntimeExceptionWrapper.create(e);
  }

  class GzipResponse extends CauchoResponseWrapper {
    private boolean _useVary = true;
    private boolean _allowGzip = true;
    private boolean _useDeflate = false;
    
    private final GzipStream _savedGzipStream = new GzipStream();
    private GzipStream _gzipStream;

    /**
     * Set true if the response should use deflate.
     */
    public void setUseDeflate(boolean useDeflate)
    {
      _useDeflate = useDeflate;
    }

    /**
     * Check for valid content type.
     */
    @Override
    public void setContentType(String value)
    {
      super.setContentType(value);

      if (_contentTypeMap == null) {
        return;
      }

      int p = value.indexOf(';');

      if (p > 0)
        value = value.substring(0, p);
      
      AllowEntry entry = _contentTypeMap.get(value);

      if (entry == ALLOW)
        _allowGzip = true;
      else if (entry == DENY) {
        _useVary = false;
        _allowGzip = false;
      }
      else if (! _hasDeny) {
        _useVary = false;
        _allowGzip = false;
      }
      else {
        _allowGzip = true;
      }
    }

    /**
     * Check for valid content type.
     */
    public void setHeader(String header, String value)
    {
      if (header.equalsIgnoreCase("Content-Type"))
        setContentType(value);
      else if (header.equalsIgnoreCase("Content-Encoding")) {
        _allowGzip = false;
        super.setHeader(header, value);
      }
      else
        super.setHeader(header, value);
    }

    /**
     * Check for valid content type.
     */
    public void addHeader(String header, String value)
    {
      if (header.equalsIgnoreCase("Content-Type"))
        setContentType(value);
      else if (header.equalsIgnoreCase("Content-Encoding")) {
        _allowGzip = false;
        super.addHeader(header, value);
      }
      else
        super.addHeader(header, value);
    }

    /**
     * This needs to be bypassed because the file's content
     * length has nothing to do with the returned length.
     */
    public void setContentLength(int length)
    {
    }

    /**
     * If the status changes, need to disable the response.
     */
    public void setStatus(int status, String message)
    {
      super.setStatus(status, message);

      if (_gzipStream != null) {
        _gzipStream.setEnable(false);
        _response.setHeader("Content-Encoding", "plain");
      }

      _allowGzip = false;
    }

    /**
     * If the status changes, need to disable the response.
     */
    public void setStatus(int status)
    {
      super.setStatus(status);

      if (status == 206 || status == 200)
        return;

      _allowGzip = false;
    }

    /**
     * Clears the output stream
     */
    public void reset()
    {
      super.reset();

      if (_gzipStream != null)
        _gzipStream.reset();
    }

    /**
     * Returns the underlying stream
     */
    public OutputStream getStream() throws IOException
    {
      if (_useVary)
        addVaryHeader(_response);
    
      if (! _allowGzip)
        return _response.getOutputStream();
      
      OutputStream os = _response.getOutputStream();

      if (_useDeflate)
        _response.setHeader("Content-Encoding", "deflate");
      else
        _response.setHeader("Content-Encoding", "gzip");
      
      _gzipStream = _savedGzipStream;
      _gzipStream.setGzip(! _useDeflate);
      _gzipStream.init(os);

      return _gzipStream;
    }

    public void close()
      throws IOException
    {
      try {
        super.close();
      } finally {
        _useVary = true;
        _allowGzip = true;
        _useDeflate = false;

        GzipStream gzipStream = _gzipStream;
        _gzipStream = null;
      
        if (gzipStream != null) {
          if (gzipStream.isData())
            gzipStream.close();
          else
            gzipStream.free();
        }
      }
    }
  }

  // handles a non-gzipped response because the client can't support
  class GzipPlainResponse extends CauchoResponseWrapper {
    private boolean _useVary = true;
    
    /**
     * Check for valid content type.
     */
    @Override
    public void setContentType(String value)
    {
      super.setContentType(value);

      if (_contentTypeMap == null) {
        return;
      }

      int p = value.indexOf(';');

      if (p > 0)
        value = value.substring(0, p);
      
      AllowEntry entry = _contentTypeMap.get(value);

      if (entry == ALLOW)
        _useVary = true;
      else if (entry == DENY)
        _useVary = false;
      else if (! _hasDeny)
        _useVary = false;
      else
        _useVary = true;
    }

    /**
     * Returns the underlying stream
     */
    public OutputStream getStream() throws IOException
    {
      if (_useVary)
        addVaryHeader(_response);
      
      return _response.getOutputStream();
    }

    public void close()
      throws IOException
    {
      try {
        super.close();
      } finally {
        _useVary = true;
      }
    }
  }

  static class AllowEntry {
  }
}
