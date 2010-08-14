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

package com.caucho.quercus.lib.xml;

import com.caucho.quercus.annotation.*;
import com.caucho.quercus.env.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;
import com.caucho.quercus.module.AbstractQuercusModule;

import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * XMLWriter
 */
public class XMLWriterModule extends AbstractQuercusModule {
  private static final Logger log
    = Logger.getLogger(XMLWriterModule.class.getName());
  private static final L10N L = new L10N(XMLWriterModule.class);

  public String []getLoadedExtensions()
  {
    return new String[] { "XMLWriter" };
  }

  /**
   * Flushes the output and returns the result.
   */
  public static Value xmlwriter_flush(@NotNull XMLWriter w)
  {
    if (w == null)
      return BooleanValue.FALSE;
    
    return w.flush();
  }

  /**
   * Opens the writer for a memory target
   */
  public static XMLWriter xmlwriter_open_memory(Env env)
  {
    XMLWriter w = new XMLWriter();

    w.openMemory(env);

    return w;
  }

  /**
   * Opens the writer for a uri target
   */
  public static XMLWriter xmlwriter_open_uri(Env env, Path path)
  {
    XMLWriter w = new XMLWriter();

    w.openURI(env, path);
    
    return w;
  }

  /**
   * Returns the memory result
   */
  public static Value xmlwriter_output_memory(@NotNull XMLWriter w)
  {
    if (w == null)
      return NullValue.NULL;
    
    return w.outputMemory();
  }

  /**
   * Ends an attribute
   */
  public static boolean xmlwriter_end_attribute(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;

    return w.endAttribute();
  }

  /**
   * Starts a CData section
   */
  public static boolean xmlwriter_end_cdata(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;

    return w.endCData();
  }

  /**
   * Starts a comment section
   */
  public static boolean xmlwriter_end_comment(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;

    return w.endComment();
  }

  /**
   * Ends a pi section
   */
  public static boolean xmlwriter_end_pi(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;

    return w.endPI();
  }

  /**
   * Ends the document
   */
  public static boolean xmlwriter_end_document(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;
    
    return w.endDocument();
  }

  /**
   * Ends a DTD attribute list
   */
  public static boolean xmlwriter_end_dtd_attlist(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;
    
    return w.endDTDAttlist();
  }

  /**
   * Ends a DTD element list
   */
  public static boolean xmlwriter_end_dtd_element(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;
    
    return w.endDTDElement();
  }

  /**
   * Ends a DTD entity
   */
  public static boolean xmlwriter_end_dtd_entity(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;
    
    return w.endDTDEntity();
  }

  /**
   * Ends a DTD
   */
  public static boolean xmlwriter_end_dtd(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;
    
    return w.endDTD();
  }

  /**
   * Ends an element
   */
  public static boolean xmlwriter_end_element(Env env,
                                              @NotNull XMLWriter w)
  {
    if (w == null)
      return false;

    return w.endElement(env);
  }

  /**
   * Ends an element
   */
  public static boolean xmlwriter_end_element_ns(Env env,
                                                 @NotNull XMLWriter w)
  {
    if (w == null)
      return false;
    
    return w.endElement(env);
  }

  /**
   * Ends an element
   */
  public static boolean xmlwriter_full_end_element(Env env,
                                                   @NotNull XMLWriter w)
  {
    if (w == null)
      return false;

    return w.fullEndElement(env);
  }

  /**
   * enables indentation
   */
  public static boolean xmlwriter_set_indent(@NotNull XMLWriter w,
                                             boolean isIndent)
  {
    if (w == null)
      return false;
    
    return w.setIndent(isIndent);
  }

  /**
   * sets the indentation string
   */
  public static boolean xmlwriter_set_indent_string(@NotNull XMLWriter w,
                                                    StringValue value)
  {
    if (w == null)
      return false;
    
    return w.setIndentString(value);
  }

  /**
   * Starts an attribute
   */
  public static boolean xmlwriter_start_attribute(Env env,
                                                  @NotNull XMLWriter w,
                                                  StringValue name)
  {
    if (w == null)
      return false;

    return w.startAttribute(env, name);
  }

  /**
   * Starts an attribute with a namespace
   */
  public static boolean xmlwriter_start_attribute_ns(Env env,
                                                     @NotNull XMLWriter w,
                                                     StringValue prefix,
                                                     StringValue name,
                                                     StringValue uri)
  {
    if (w == null)
      return false;

    return w.startAttributeNS(env, prefix, name, uri);
  }

  /**
   * Starts a CData section
   */
  public static boolean xmlwriter_start_cdata(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;

    return w.startCData();
  }

  /**
   * Starts a comment section
   */
  public static boolean xmlwriter_start_comment(@NotNull XMLWriter w)
  {
    if (w == null)
      return false;

    return w.startComment();
  }

  /**
   * Starts the document
   */
  public static boolean xmlwriter_start_document(
      Env env,
      @NotNull XMLWriter w,
      @Optional StringValue version,
      @Optional StringValue encoding,
      @Optional StringValue standalone) {
    if (w == null)
      return false;

    return w.startDocument(env, version, encoding, standalone);
  }

  /**
   * Starts a DTD attribute list
   */
  public static boolean xmlwriter_start_dtd_attlist(@NotNull XMLWriter w,
                                                    StringValue name)
  {
    if (w == null)
      return false;
    
    return w.startDTDAttlist(name);
  }

  /**
   * Starts a DTD element list
   */
  public static boolean xmlwriter_start_dtd_element(@NotNull XMLWriter w,
                                                    StringValue name)
  {
    if (w == null)
      return false;
    
    return w.startDTDElement(name);
  }

  /**
   * Starts a DTD entity
   */
  public static boolean xmlwriter_start_dtd_entity(@NotNull XMLWriter w,
                                                   StringValue name)
  {
    if (w == null)
      return false;
    
    return w.startDTDEntity(name);
  }

  /**
   * Starts a DTD
   */
  public static boolean xmlwriter_start_dtd(@NotNull XMLWriter w,
                                            StringValue name,
                                            @Optional StringValue publicId,
                                            @Optional StringValue systemId)
  {
    if (w == null)
      return false;
    
    return w.startDTD(name, publicId, systemId);
  }

  /**
   * Starts an element
   */
  public static boolean xmlwriter_start_element(Env env,
                                                @NotNull XMLWriter w,
                                                StringValue name)
  {
    if (w == null)
      return false;

    return w.startElement(env, name);
  }

  /**
   * Starts a namespaced element
   */
  public static boolean xmlwriter_start_element_ns(Env env,
                                                   @NotNull XMLWriter w,
                                                   StringValue prefix,
                                                   StringValue name,
                                                   StringValue uri)
  {
    if (w == null)
      return false;

    return w.startElementNS(env, prefix, name, uri);
  }

  /**
   * Starts a processing instruction section
   */
  public static boolean xmlwriter_start_pi(Env env,
                                           @NotNull XMLWriter w,
                                           StringValue target)
  {
    if (w == null)
      return false;

    return w.startPI(env, target);
  }

  /**
   * Writes text
   */
  public static boolean xmlwriter_text(Env env,
                                       @NotNull XMLWriter w,
                                       StringValue text)
  {
    if (w == null)
      return false;

    return w.text(env, text);
  }

  /**
   * Writes a complete attribute
   */
  public static boolean xmlwriter_write_attribute(Env env,
                                                  @NotNull XMLWriter w,
                                                  StringValue name,
                                                  StringValue value)
  {
    if (w == null)
      return false;

    return w.writeAttribute(env, name, value);
  }

  /**
   * Writes a complete attribute
   */
  public static boolean xmlwriter_write_attribute_ns(Env env,
                                                     @NotNull XMLWriter w,
                                                     StringValue prefix,
                                                     StringValue name,
                                                     StringValue uri,
                                                     StringValue value)
  {
    if (w == null)
      return false;

    return w.writeAttributeNS(env, prefix, name, uri, value);
  }

  /**
   * Writes a complete cdata
   */
  public static boolean xmlwriter_write_cdata(Env env,
                                              @NotNull XMLWriter w,
                                              StringValue value)
  {
    if (w == null)
      return false;

    return w.writeCData(env, value);
  }

  /**
   * Writes a complete comment
   */
  public static boolean xmlwriter_write_comment(Env env,
                                                @NotNull XMLWriter w,
                                                StringValue value)
  {
    if (w == null)
      return false;

    return w.writeComment(env, value);
  }

  /**
   * Writes a DTD attribute list
   */
  public static boolean xmlwriter_write_dtd_attlist(Env env,
                                                    @NotNull XMLWriter w,
                                                    StringValue name,
                                                    StringValue content)
  {
    if (w == null)
      return false;

    return w.writeDTDAttlist(env, name, content);
  }

  /**
   * Writes a DTD element
   */
  public static boolean xmlwriter_write_dtd_element(Env env,
                                                    @NotNull XMLWriter w,
                                                    StringValue name,
                                                    StringValue content)
  {
    if (w == null)
      return false;

    return w.writeDTDElement(env, name, content);
  }

  /**
   * Writes a DTD entity
   */
  public static boolean xmlwriter_write_dtd_entity(Env env,
                                                   @NotNull XMLWriter w,
                                                   StringValue name,
                                                   StringValue content)
  {
    if (w == null)
      return false;

    return w.writeDTDEntity(env, name, content);
  }

  /**
   * Writes a DTD
   */
  public static boolean xmlwriter_write_dtd(Env env,
                                            @NotNull XMLWriter w,
                                            StringValue name,
                                            @Optional StringValue publicId,
                                            @Optional StringValue systemId,
                                            @Optional StringValue subset)
  {
    if (w == null)
      return false;

    return w.writeDTD(env, name, publicId, systemId, subset);
  }

  /**
   * Writes a complete element
   */
  public static boolean xmlwriter_write_element(Env env,
                                                @NotNull XMLWriter w,
                                                StringValue name,
                                                @Optional StringValue content)
  {
    if (w == null)
      return false;

    return w.writeElement(env, name, content);
  }

  /**
   * Writes a complete element
   */
  public static boolean xmlwriter_write_element_ns(
      Env env,
      @NotNull XMLWriter w,
      StringValue prefix,
      StringValue name,
      StringValue uri,
      @Optional StringValue content) {
    if (w == null)
      return false;

    return w.writeElementNS(env, prefix, name, uri, content);
  }

  /**
   * Writes a pi
   */
  public static boolean xmlwriter_write_pi(Env env,
                                           @NotNull XMLWriter w,
                                           StringValue name,
                                           StringValue value)
  {
    if (w == null)
      return false;

    return w.writePI(env, name, value);
  }

  /**
   * Writes raw text
   */
  public static boolean xmlwriter_write_raw(Env env,
                                            @NotNull XMLWriter w,
                                            StringValue value)
  {
    if (w == null)
      return false;

    return w.writeRaw(env, value);
  }
}
