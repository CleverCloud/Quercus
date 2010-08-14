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

package com.caucho.j2ee.deployserver;

import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.xml.XmlPrinter;

import org.w3c.dom.Node;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Plan for the deployment.
 */
public class DeploymentPlan {
  private static final L10N L = new L10N(DeploymentPlan.class);

  private String _archiveType;
  private String _name;
  private String _metaInf;

  private ArrayList<PlanFile> _fileList = new ArrayList<PlanFile>();

  /**
   * Sets the deployment type.
   */
  public void setArchiveType(String type)
    throws ConfigException
  {
    if (type.equals("war")) {
      _metaInf = "WEB-INF/";
    }
    else if (type.equals("ear")) {
      _metaInf = "META-INF/";
    }
    else if (type.equals("rar")) {
      _metaInf = "META-INF/";
    }
    else
      throw new ConfigException(L.l("'{0}' is an unknown archive type.", type));

    _archiveType = type;
  }

  /**
   * Gets the deployment type.
   */
  public String getArchiveType()
  {
    return _archiveType;
  }

  /**
   * Sets the name
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the name
   */
  public String getName()
  {
    return _name;
  }

  @PostConstruct
  public void init()
  {
    if (_archiveType == null)
      throw new ConfigException(L.l("`{0}' is required", "archive-type"));

    if (_name == null)
      throw new ConfigException(L.l("`{0}' is required", "name"));
  }

  /**
   * An ExtFile is an Xml file that is written into the META-INF
   * (or WEB-INF for a war) * directory.
   */
  public ExtFile createExtFile()
  {
    return new ExtFile();
  }

  public void addExtFile(ExtFile extFile)
  {
    _fileList.add(extFile);
  }


  /**
   * A RawFile is any format file that is written into a file
   * specified by the path, relative to the root of the deployed archive.
   */
  public RawFile createRawFile()
  {
    return new RawFile();
  }

  public void addRawFile(RawFile rawFile)
  {
    _fileList.add(rawFile);
  }

  /**
   * Returns the list of ext and raw files.
   */
  public ArrayList<PlanFile> getFileList()
  {
    return _fileList;
  }

  abstract public class PlanFile {
    abstract public String getPath();
    abstract public void writeToStream(OutputStream os)
      throws IOException;
    public String toString()
    {
      return "DeploymentPlan$" + getClass().getSimpleName() + "[" + getPath() + "]";
    }

  }

  public class ExtFile
    extends PlanFile
  {
    private String _name;
    private Node _data;

    /**
     * Sets the file name.
     */
    public void setName(String name)
    {
      if (name.startsWith("/"))
        throw new ConfigException(L.l("name `{0}' cannot start with /", name));

      _name = name;
    }

    public void setData(Node data)
    {
      _data = data.getFirstChild();
    }

    @PostConstruct
    public void init()
    {
      if (_name == null)
        throw new ConfigException(L.l("`{0}' is required", "name"));

      if (_data == null)
        throw new ConfigException(L.l("`{0}' is required", "data"));
    }

    public String getPath()
    {
      return _metaInf + _name;
    }

    public void writeToStream(OutputStream os)
      throws IOException
    {
      XmlPrinter xmlPrinter = new XmlPrinter(os);
      xmlPrinter.setPretty(true);
      xmlPrinter.printXml(_data);
    }
  }

  public class RawFile
    extends PlanFile
  {
    private String _path;
    private String _data;

    /**
     * Sets the file name.
     */
    public void setPath(String path)
    {
      if (path.startsWith("/"))
        throw new ConfigException(L.l("path `{0}' cannot start with /", path));

      _path = path;
    }

    public void setData(RawString data)
    {
      _data = data.getValue();
    }

    @PostConstruct
    public void init()
    {
      if (_path == null)
        throw new ConfigException(L.l("`{0}' is required", "path"));

      if (_data == null)
        throw new ConfigException(L.l("`{0}' is required", "data"));
    }

    public String getPath()
    {
      return  _path;
    }

    public void writeToStream(OutputStream os)
      throws IOException
    {
      WriteStream writeStream = Vfs.openWrite(os);

      try {
        writeStream.print(_data);
      }
      finally {
        writeStream.close();
      }
    }
  }
}

