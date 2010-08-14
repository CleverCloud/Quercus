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

public abstract class AttachmentPart {
  private static final String CONTENT_ID = "Content-ID";
  private static final String CONTENT_LOCATION = "Content-Location";
  private static final String CONTENT_TYPE = "Content-Type";

  public AttachmentPart()
  {
  }

  public String getContentId()
  {
    String[] contentIds = getMimeHeader(CONTENT_ID);

    if (contentIds != null && contentIds.length > 0)
      return contentIds[0];

    return null;
  }

  public String getContentLocation()
  {
    String[] contentLocations = getMimeHeader(CONTENT_LOCATION);

    if (contentLocations != null && contentLocations.length > 0)
      return contentLocations[0];

    return null;
  }

  public String getContentType()
  {
    String[] contentTypes = getMimeHeader(CONTENT_TYPE);

    if (contentTypes != null && contentTypes.length > 0)
      return contentTypes[0];

    return null;
  }

  public void setContentId(String contentId)
  {
    setMimeHeader(CONTENT_ID, contentId);
  }

  public void setContentLocation(String contentLocation)
  {
    setMimeHeader(CONTENT_LOCATION, contentLocation);
  }

  public void setContentType(String contentType)
  {
    if (contentType == null)
      throw new IllegalArgumentException("content type may not be null");

    setMimeHeader(CONTENT_TYPE, contentType);
  }

  public abstract void addMimeHeader(String name, String value);
  public abstract void clearContent();
  public abstract Iterator getAllMimeHeaders();
  public abstract InputStream getBase64Content() throws SOAPException;
  public abstract Object getContent() throws SOAPException;
  public abstract DataHandler getDataHandler() throws SOAPException;
  public abstract Iterator getMatchingMimeHeaders(String[] names);
  public abstract String[] getMimeHeader(String name);
  public abstract Iterator getNonMatchingMimeHeaders(String[] names);
  public abstract InputStream getRawContent() throws SOAPException;
  public abstract byte[] getRawContentBytes() throws SOAPException;
  public abstract int getSize() throws SOAPException;
  public abstract void removeAllMimeHeaders();
  public abstract void removeMimeHeader(String header);
  public abstract void setBase64Content(InputStream content, String contentType) throws SOAPException;
  public abstract void setContent(Object object, String contentType);
  public abstract void setDataHandler(DataHandler dataHandler);
  public abstract void setMimeHeader(String name, String value);
  public abstract void setRawContent(InputStream content, String contentType) throws SOAPException;
  public abstract void setRawContentBytes(byte[] content, int offset, int len, String contentType) throws SOAPException;
}

