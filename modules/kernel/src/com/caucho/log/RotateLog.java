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

package com.caucho.log;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Configuration for a rotating log
 */
public class RotateLog {
  private final static L10N L = new L10N(RotateLog.class);
  
  private static final long ROLLOVER_SIZE = Long.MAX_VALUE / 2;
  
  private Path _path;
  private String _pathFormat;
  private String _archiveFormat;
  
  private Period _rolloverPeriod;
  private Bytes _rolloverSize;
  private int _rolloverCount = -1;
  
  private RotateStream _rotateStream;
  
  private String _timestamp;

  /**
   * Gets the output path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the output path.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Gets the output path.
   */
  public String getPathFormat()
  {
    return _pathFormat;
  }

  /**
   * Sets the output path.
   */
  public void setPathFormat(String path)
  {
    _pathFormat = path;
  }

  /**
   * Sets the output path (backward compat).
   */
  public void setHref(Path path)
  {
    setPath(path);
  }

  /**
   * Sets the rollover period.
   */
  public void setRolloverPeriod(Period period)
  {
    _rolloverPeriod = period;
  }

  /**
   * Sets the rollover size.
   */
  public void setRolloverSize(Bytes size)
  {
    _rolloverSize = size;
  }

  /**
   * Sets the rollover count
   */
  public int getRolloverCount()
  {
    return _rolloverCount;
  }

  /**
   * Sets the rollover count.
   */
  public void setRolloverCount(int count)
  {
    _rolloverCount = count;
  }

  /**
   * Sets the timestamp
   */
  public String getTimestamp()
  {
    return _timestamp;
  }

  /**
   * Sets the timestamp.
   */
  /*
  public void setTimestamp(String timestamp)
  {
    _timestamp = timestamp;
  }
  */

  /**
   * Gets the archive format
   */
  public String getArchiveFormat()
  {
    return _archiveFormat;
  }

  /**
   * Sets the archive format.
   */
  public void setArchiveFormat(String format)
  {
    _archiveFormat = format;
  }

  /**
   * Returns the rotated stream.
   */
  public RotateStream getRotateStream()
  {
    return _rotateStream;
  }

  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "rotate-log";
  }

  /**
   * Initialize the log.
   */
  @PostConstruct
  public void init()
    throws ConfigException, IOException
  {
    if (_path != null)
      _rotateStream = RotateStream.create(_path);
    else if (_pathFormat != null)
      _rotateStream = RotateStream.create(_pathFormat);
    else
      throw new ConfigException(L.l("`path' is a required attribute of <{0}>.  Each <{0}> must configure the destination stream.", getTagName()));

    if (_path != null && _path.exists() && ! _path.canRead() &&
        (_rolloverPeriod != null ||
         _rolloverSize != null ||
         _archiveFormat != null)) {
      throw new ConfigException(L.l("log path '{0}' is not readable and therefore cannot be rotated.", _path.getURL()));
    }

    AbstractRolloverLog rolloverLog = _rotateStream.getRolloverLog();

    if (_rolloverPeriod != null)
      rolloverLog.setRolloverPeriod(_rolloverPeriod);

    if (_rolloverSize != null)
      rolloverLog.setRolloverSize(_rolloverSize);
    _rotateStream.setMaxRolloverCount(_rolloverCount);
    if (_archiveFormat != null)
      rolloverLog.setArchiveFormat(_archiveFormat);

    _rotateStream.init();
  }
}
