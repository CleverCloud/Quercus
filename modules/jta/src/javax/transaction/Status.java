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

package javax.transaction;

/**
 * Status constants.
 * associated with a single thread.
 */
public interface Status {
  public final static int STATUS_ACTIVE = 0;
  public final static int STATUS_MARKED_ROLLBACK = 1;
  public final static int STATUS_PREPARED = 2;
  public final static int STATUS_COMMITTED = 3;
  public final static int STATUS_ROLLEDBACK = 4;
  public final static int STATUS_UNKNOWN = 5;
  public final static int STATUS_NO_TRANSACTION = 6;
  public final static int STATUS_PREPARING = 7;
  public final static int STATUS_COMMITTING = 8;
  public final static int STATUS_ROLLING_BACK = 9;
}
