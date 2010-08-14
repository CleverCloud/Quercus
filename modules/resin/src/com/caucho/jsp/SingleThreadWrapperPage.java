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

package com.caucho.jsp;

import javax.servlet.SingleThreadModel;
import javax.servlet.jsp.HttpJspPage;
import java.io.IOException;

/**
 * Wraps Java JSP files using 'extends' in a page.  Since a JSP file which
 * uses 'extends' does not subclass from Page, we need to wrap it with
 * a Page-compatible class.
 *
 * <p>Because it inherits from Page, the wrapped page still be recompiled
 * when the underlying page changes.
 */
class SingleThreadWrapperPage extends WrapperPage
  implements SingleThreadModel {
  SingleThreadWrapperPage(HttpJspPage child)
    throws IOException
  {
    super(child);
  }
}
