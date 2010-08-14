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

package com.caucho.xmpp.data;

import com.caucho.vfs.*;
import com.caucho.xmpp.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.stream.*;

/**
 * DataForm
 *
 * XEP-0004: http://www.xmpp.org/extensions/xep-0004.html
 *
 * <code><pre>
 * namespace = jabber:x:data
 *
 * element x {
 *   attribute type,
 *
 *   instructions*,
 *   title?,
 *   field*,
 *   reported?,
 *   item*
 * }
 *
 * element field {
 *    attribute label?,
 *    attribute type?,
 *    attribute var?,
 *
 *    desc?,
 *    required?,
 *    value*,
 *    option*,
 * }
 *
 * element item {
 *   field+
 * }
 *
 * element option {
 *   attribute label?,
 *
 *   value*
 * }
 *
 * element reported {
 *   field+
 * }
 *
 * element value {
 *   string
 * }
 * </pre></code>
 */
public class XmppDataFormMarshal extends AbstractXmppMarshal {
  private static final Logger log
    = Logger.getLogger(XmppDataFormMarshal.class.getName());
  private static final boolean _isFinest = log.isLoggable(Level.FINEST);

  /**
   * Returns the namespace uri for the XMPP stanza value
   */
  public String getNamespaceURI()
  {
    return "jabber:x:data";
  }

  /**
   * Returns the local name for the XMPP stanza value
   */
  public String getLocalName()
  {
    return "x";
  }

  /**
   * Returns the java classname of the object
   */
  public String getClassName()
  {
    return DataForm.class.getName();
  }
  
  /**
   * Serializes the object to XML
   */
  public void toXml(XmppStreamWriter out, Serializable object)
    throws IOException, XMLStreamException
  {
    DataForm form = (DataForm) object;

    out.writeStartElement("", getLocalName(), getNamespaceURI());
    out.writeNamespace("", getNamespaceURI());

    if (form.getType() != null)
      out.writeAttribute("type", form.getType());

    if (form.getTitle() != null) {
      out.writeStartElement("title");
      out.writeCharacters(form.getTitle());
      out.writeEndElement(); // </title>
    }

    DataInstructions []instructions = form.getInstructions();
    if (instructions != null) {
      for (DataInstructions instruction : instructions) {
        toXml(out, instruction);
      }
    }

    DataField []fields = form.getField();
    if (fields != null) {
      for (DataField field : fields) {
        toXml(out, field);
      }
    }

    if (form.getReported() != null)
      toXml(out, form.getReported());

    DataItem []items = form.getItem();
    if (items != null) {
      for (DataItem item : items) {
        toXml(out, item);
      }
    }
    
    out.writeEndElement(); // </form>
  }

  private void toXml(XmppStreamWriter out, DataField field)
    throws IOException, XMLStreamException
  {
    out.writeStartElement("field");

    if (field.getLabel() != null)
      out.writeAttribute("label", field.getLabel());

    if (field.getType() != null)
      out.writeAttribute("type", field.getType());

    if (field.getVar() != null)
      out.writeAttribute("var", field.getVar());

    if (field.getDesc() != null) {
      out.writeStartElement("desc");
      out.writeCharacters(field.getDesc());
      out.writeEndElement(); // </desc>
    }

    if (field.isRequired()) {
      out.writeStartElement("required");
      out.writeEndElement(); // </required>
    }

    DataValue []values = field.getValue();
    if (values != null) {
      for (int i = 0; i < values.length; i++) {
        DataValue value = values[i];

        out.writeStartElement("value");
        out.writeCharacters(value.getValue());
        out.writeEndElement(); // </value>
      }
    }

    DataOption []options = field.getOption();
    if (options != null) {
      for (int i = 0; i < options.length; i++) {
        toXml(out, options[i]);
      }
    }

    out.writeEndElement(); // </field>
  }

  private void toXml(XmppStreamWriter out, DataOption option)
    throws IOException, XMLStreamException
  {
    out.writeStartElement("option");

    if (option.getLabel() != null)
      out.writeAttribute("label", option.getLabel());

    DataValue []values = option.getValue();
    if (values != null) {
      for (int i = 0; i < values.length; i++) {
        DataValue value = values[i];

        out.writeStartElement("value");
        out.writeCharacters(value.getValue());
        out.writeEndElement(); // </value>
      }
    }

    out.writeEndElement(); // </option>
  }

  private void toXml(XmppStreamWriter out, DataItem item)
    throws IOException, XMLStreamException
  {
    out.writeStartElement("item");

    DataField []fields = item.getField();
    if (fields != null) {
      for (int i = 0; i < fields.length; i++) {
        toXml(out, fields[i]);
      }
    }

    out.writeEndElement(); // </item>
  }

  private void toXml(XmppStreamWriter out, DataReported reported)
    throws IOException, XMLStreamException
  {
    out.writeStartElement("reported");

    DataField []fields = reported.getField();
    if (fields != null) {
      for (int i = 0; i < fields.length; i++) {
        toXml(out, fields[i]);
      }
    }

    out.writeEndElement(); // </reported>
  }

  private void toXml(XmppStreamWriter out, DataInstructions instructions)
    throws IOException, XMLStreamException
  {
    out.writeStartElement("instructions");

    if (instructions.getValue() != null)
      out.writeCharacters(instructions.getValue());
    
    out.writeEndElement(); // </instructions>
  }
  
  /**
   * Deserializes the object from XML
   */
  public Serializable fromXml(XmppStreamReader in)
    throws IOException, XMLStreamException
  {
    boolean isFinest = log.isLoggable(Level.FINEST);

    String type = in.getAttributeValue(null, "type");

    DataForm form = new DataForm(type);
    
    ArrayList<DataField> fieldList = new ArrayList<DataField>();
    ArrayList<DataItem> itemList = new ArrayList<DataItem>();
    ArrayList<DataInstructions> instructionsList
      = new ArrayList<DataInstructions>();
    
    int tag = in.nextTag();
    while (tag > 0) {
      if (isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        form.setFieldList(fieldList);
        form.setItemList(itemList);
        form.setInstructionsList(instructionsList);

        return form;
      }

      if (XMLStreamReader.START_ELEMENT == tag
          && "field".equals(in.getLocalName())) {
        fieldList.add(parseField(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "item".equals(in.getLocalName())) {
        itemList.add(parseItem(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "reported".equals(in.getLocalName())) {
        form.setReported(parseReported(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "title".equals(in.getLocalName())) {
        String title = in.getElementText();

        form.setTitle(title);

        skipToEnd(in, "title");
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "instructions".equals(in.getLocalName())) {
        String value = in.getElementText();

        instructionsList.add(new DataInstructions(value));

        skipToEnd(in, "instructions");
      }
      else if (XMLStreamReader.START_ELEMENT == tag) {
        log.finer(this + " <" + in.getLocalName() + "> is an unknown tag");

        skipToEnd(in, in.getLocalName());
      }

      tag = in.nextTag();
    }

    return null;
  }
  
  /**
   * Deserializes the object from XML
   */
  public DataField parseField(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String label = in.getAttributeValue(null, "label");
    String type = in.getAttributeValue(null, "type");
    String var = in.getAttributeValue(null, "var");
    
    DataField field = new DataField(type, var, label);

    ArrayList<DataValue> valueList = new ArrayList<DataValue>();
    ArrayList<DataOption> optionList = new ArrayList<DataOption>();
    
    int tag = in.nextTag();
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        field.setValueList(valueList);
        field.setOptionList(optionList);

        return field;
      }
    
      if (XMLStreamReader.START_ELEMENT == tag
          && "desc".equals(in.getLocalName())) {
        String desc = in.getElementText();

        field.setDesc(desc);

        skipToEnd(in, "desc");
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "option".equals(in.getLocalName())) {
        optionList.add(parseOption(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "required".equals(in.getLocalName())) {
        field.setRequired(true);

        skipToEnd(in, "required");
      }
      else if (XMLStreamReader.START_ELEMENT == tag
               && "value".equals(in.getLocalName())) {
        String value = in.getElementText();

        valueList.add(new DataValue(value));

        skipToEnd(in, "value");
      }
      else if (XMLStreamReader.START_ELEMENT == tag) {
        log.finer(this + " <" + in.getLocalName() + "> is an unknown tag");

        skipToEnd(in, in.getLocalName());
      }

      tag = in.nextTag();
    }

    skipToEnd(in, "field");

    return field;
  }
  
  /**
   * Deserializes the object from XML
   */
  public DataItem parseItem(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    DataItem item = new DataItem();

    ArrayList<DataField> fieldList = new ArrayList<DataField>();
    
    int tag = in.nextTag();
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        item.setFieldList(fieldList);

        return item;
      }
    
      if (XMLStreamReader.START_ELEMENT == tag
          && "field".equals(in.getLocalName())) {
        fieldList.add(parseField(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag) {
        log.finer(this + " <" + in.getLocalName() + "> is an unknown tag");

        skipToEnd(in, in.getLocalName());
      }

      tag = in.nextTag();
    }

    skipToEnd(in, "item");

    return item;
  }
  
  /**
   * Deserializes the object from XML
   */
  public DataReported parseReported(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    DataReported reported = new DataReported();

    ArrayList<DataField> fieldList = new ArrayList<DataField>();
    
    int tag = in.nextTag();
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        reported.setFieldList(fieldList);

        return reported;
      }
    
      if (XMLStreamReader.START_ELEMENT == tag
          && "field".equals(in.getLocalName())) {
        fieldList.add(parseField(in));
      }
      else if (XMLStreamReader.START_ELEMENT == tag) {
        log.finer(this + " <" + in.getLocalName() + "> is an unknown tag");

        skipToEnd(in, in.getLocalName());
      }

      tag = in.nextTag();
    }

    skipToEnd(in, "reported");

    return reported;
  }
  
  /**
   * Deserializes the object from XML
   */
  public DataOption parseOption(XMLStreamReader in)
    throws IOException, XMLStreamException
  {
    String label = in.getAttributeValue(null, "label");
    
    DataOption option = new DataOption(label);

    ArrayList<DataValue> valueList = new ArrayList<DataValue>();
    
    int tag = in.nextTag();
    while (tag > 0) {
      if (_isFinest)
        debug(in);

      if (XMLStreamReader.END_ELEMENT == tag) {
        option.setValueList(valueList);

        return option;
      }
    
      if (XMLStreamReader.START_ELEMENT == tag
               && "value".equals(in.getLocalName())) {
        String value = in.getElementText();

        valueList.add(new DataValue(value));

        skipToEnd(in, "value");
      }
      else if (XMLStreamReader.START_ELEMENT == tag) {
        log.finer(this + " <" + in.getLocalName() + "> is an unknown tag");

        skipToEnd(in, in.getLocalName());
      }

      tag = in.nextTag();
    }

    skipToEnd(in, "option");

    return option;
  }
}
