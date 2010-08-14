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

package javax.xml.stream;

public interface XMLStreamConstants {

  public static final int ATTRIBUTE=10;

  public static final int CDATA=12;

  public static final int CHARACTERS=4;

  public static final int COMMENT=5;

  public static final int DTD=11;

  public static final int END_DOCUMENT=8;

  public static final int END_ELEMENT=2;

  // XXX: online doc has  17 for ENTITY_DECLARATION
  // XXX: online doc has  15 for ENTITY_DECLARATION here:
  //      http://java.sun.com/javaee/5/docs/api/constant-values.html
  public static final int ENTITY_DECLARATION=15;

  public static final int ENTITY_REFERENCE=9;

  public static final int NAMESPACE=13;

  public static final int NOTATION_DECLARATION=14;

  public static final int PROCESSING_INSTRUCTION=3;

  public static final int SPACE=6;

  public static final int START_DOCUMENT=7;

  public static final int START_ELEMENT=1;
}

