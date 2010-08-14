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
 * @author Emil Ong
 */

package com.caucho.bam;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The @QueryGet annotation marks a SimpleBamService method as handling
 * a GET RPC query.  GET calls must not modify any state in the service,
 * because SET calls are responsible for modifying state.  Queries
 * are matched to results using a long id, which is unique for each
 * query in the connection.  GET queries must send either a queryResult
 * or a queryError to the caller, because the client may be blocking
 * waiting for a reply.
 *
 * <code><pre>
 * @QueryGet
 * boolean queryGet(long id, String to, String from, MyGetQuery value)
 * </pre></code>
 *
 * A ping RPC query handler would look like:
 *
 * <code><pre>
 * @QueryGet
 * public boolean pingQuery(long id, String to, String from, PingQuery value)
 * {
 *   getBrokerStream().queryResult(id, from, to, value);
 *
 *   return true;
 * }
 * </pre></code>
 */
@Target({METHOD})
@Retention(RUNTIME)
@Documented  
public @interface QueryGet {
}
