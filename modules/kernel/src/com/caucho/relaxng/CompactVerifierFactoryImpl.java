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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.relaxng;

import com.caucho.config.ConfigException;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;

/**
 * JARV Verifier factory.
 */
public class CompactVerifierFactoryImpl implements VerifierFactory {
  private static HashMap<Path,SoftReference<Schema>> _schemaMap =
    new HashMap<Path,SoftReference<Schema>>();

  /**
   * Reads the schema from the classpath.
   */
  public static Schema compileFromResource(String schemaName)
    throws SAXException, IOException
  {
    CompactVerifierFactoryImpl factory = new CompactVerifierFactoryImpl();

    MergePath mp = new MergePath();
    mp.addClassPath();

    return factory.compileSchema(mp.lookup(schemaName));
  }
  
  /**
   * Compile a schema.
   */
  public static Schema compileFromPath(Path path)
    throws SAXException, IOException
  {
    return new CompactVerifierFactoryImpl().compileSchema(path);
  }
  
  /**
   * Compile a schema.
   */
  public Schema compileSchema(Path path)
    throws SAXException, IOException
  {
    String nativePath = path.getNativePath();
    
    SoftReference<Schema> schemaRef = _schemaMap.get(path);
    Schema schema = null;

    if (schemaRef != null && (schema = schemaRef.get()) != null) {
      // XXX: probably eventually add an isModified
      return schema;
    }
    
    ReadStream is = path.openRead();

    try {
      InputSource source = new InputSource(is);

      source.setSystemId(path.getUserPath());

      schema = compileSchema(source);

      if (schema != null)
        _schemaMap.put(path, new SoftReference<Schema>(schema));
    } finally {
      is.close();
    }

    return schema;
  }
  /**
   * Compile a schema.
   */
  public Schema compileSchema(InputSource is)
    throws SAXException, IOException
  {
    try {
      CompactParser parser = new CompactParser();

      parser.parse(is);
      
      SchemaImpl schema = new SchemaImpl(parser.getGrammar());

      schema.setFilename(is.getSystemId());

      return schema;
    } catch (SAXException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
