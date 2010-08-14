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
 * @author Alex Rojkov
 */
package javax.servlet;

import javax.servlet.annotation.MultipartConfig;

/**
 * @since Servlet 3.0
 */
public class MultipartConfigElement {

  private String _location = "";
  private long _maxFileSize;
  private long _maxRequestSize;
  private int _fileSizeThreshold;

  public MultipartConfigElement(String location)
  {
    _location = location;
  }

  public MultipartConfigElement(String location, long maxFileSize,
                                long maxRequestSize, int fileSizeThreshold)
  {
    _location = location;
    _maxFileSize = maxFileSize;
    _maxRequestSize = maxRequestSize;
    _fileSizeThreshold = fileSizeThreshold;
  }

  public MultipartConfigElement(MultipartConfig config)
  {
    _location = config.location();
    _maxFileSize = config.maxFileSize();
    _maxRequestSize = config.maxRequestSize();
    _fileSizeThreshold = config.fileSizeThreshold();
  }

  public String getLocation()
  {
    return _location;
  }

  public long getMaxFileSize()
  {
    return _maxFileSize;
  }

  public long getMaxRequestSize()
  {
    return _maxRequestSize;
  }

  public int getFileSizeThreshold()
  {
    return _fileSizeThreshold;
  }
}
