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

package javax.xml.soap;
import java.io.*;
import javax.activation.*;
import java.util.*;

public abstract class SOAPMessage {
  public static final String CHARACTER_SET_ENCODING =
      "javax.xml.soap.character-set-encoding";
  public static final String WRITE_XML_DECLARATION =
      "javax.xml.soap.write-xml-declaration";

  public SOAPMessage()
  {
  }

  public abstract void addAttachmentPart(AttachmentPart attachmentPart);
  public abstract int countAttachments();
  public abstract AttachmentPart createAttachmentPart();

  public AttachmentPart createAttachmentPart(DataHandler dataHandler)
  {
    throw new UnsupportedOperationException();
  }

  public AttachmentPart createAttachmentPart(Object content, String contentType)
  {
    throw new UnsupportedOperationException();
  }

  public abstract AttachmentPart getAttachment(SOAPElement element)
      throws SOAPException;

  public abstract Iterator getAttachments();
  public abstract Iterator getAttachments(MimeHeaders headers);
  public abstract String getContentDescription();
  public abstract MimeHeaders getMimeHeaders();

  public Object getProperty(String property) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public SOAPBody getSOAPBody() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public SOAPHeader getSOAPHeader() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public abstract SOAPPart getSOAPPart();
  public abstract void removeAllAttachments();
  public abstract void removeAttachments(MimeHeaders headers);
  public abstract void saveChanges() throws SOAPException;
  public abstract boolean saveRequired();
  public abstract void setContentDescription(String description);

  public void setProperty(String property, Object value) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public abstract void writeTo(OutputStream out)
      throws SOAPException, IOException;
}

