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
 * @author Sam
 */

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.env.DefaultValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.lib.simplexml.SimpleXMLElement;
import com.caucho.quercus.module.AbstractQuercusModule;

public class QuercusDOMModule
  extends AbstractQuercusModule
{
  public static final int XML_ELEMENT_NODE = 1;
  public static final int XML_ATTRIBUTE_NODE = 2;
  public static final int XML_TEXT_NODE = 3;
  public static final int XML_CDATA_SECTION_NODE = 4;
  public static final int XML_ENTITY_REF_NODE = 5;
  public static final int XML_ENTITY_NODE = 6;
  public static final int XML_PI_NODE = 7;
  public static final int XML_COMMENT_NODE = 8;
  public static final int XML_DOCUMENT_NODE = 9;
  public static final int XML_DOCUMENT_TYPE_NODE = 10;
  public static final int XML_DOCUMENT_FRAG_NODE = 11;
  public static final int XML_NOTATION_NODE = 12;
  public static final int XML_HTML_DOCUMENT_NODE = 13;
  public static final int XML_DTD_NODE = 14;
  public static final int XML_ELEMENT_DECL_NODE = 15;
  public static final int XML_ATTRIBUTE_DECL_NODE = 16;
  public static final int XML_ENTITY_DECL_NODE = 17;
  public static final int XML_NAMESPACE_DECL_NODE = 18;

  public static final int XML_ATTRIBUTE_CDATA = 1;
  public static final int XML_ATTRIBUTE_ID = 2;
  public static final int XML_ATTRIBUTE_IDREF = 3;
  public static final int XML_ATTRIBUTE_IDREFS = 4;
  public static final int XML_ATTRIBUTE_ENTITY = 5;
  public static final int XML_ATTRIBUTE_NMTOKEN = 7;
  public static final int XML_ATTRIBUTE_NMTOKENS = 8;
  public static final int XML_ATTRIBUTE_ENUMERATION = 9;
  public static final int XML_ATTRIBUTE_NOTATION = 10;

  public static final int DOM_INDEX_SIZE_ERR = 1;
  public static final int DOMSTRING_SIZE_ERR = 2;
  public static final int DOM_HIERARCHY_REQUEST_ERR = 3;
  public static final int DOM_WRONG_DOCUMENT_ERR = 4;
  public static final int DOM_INVALID_CHARACTER_ERR = 5;
  public static final int DOM_NO_DATA_ALLOWED_ERR = 6;
  public static final int DOM_NO_MODIFICATION_ALLOWED_ERR = 7;
  public static final int DOM_NOT_FOUND_ERR = 8;
  public static final int DOM_NOT_SUPPORTED_ERR = 9;
  public static final int DOM_INUSE_ATTRIBUTE_ERR = 10;
  public static final int DOM_INVALID_STATE_ERR = 11;
  public static final int DOM_SYNTAX_ERR = 12;
  public static final int DOM_INVALID_MODIFICATION_ERR = 13;
  public static final int DOM_NAMESPACE_ERR = 14;
  public static final int DOM_INVALID_ACCESS_ERR = 15;
  public static final int DOM_VALIDATION_ERR = 16;

  public static DOMElement dom_import_simplexml(Env env, SimpleXMLElement node)
  {
    if (node == null)
      return null;
    
    DOMDocument document = DOMDocument.__construct(env, "1.0", null);
    
    StringValue xml = node.asXML(env);
    document.loadXML(env, xml, DefaultValue.DEFAULT);

    return document.getDocumentElement();
  }
}
