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

package com.caucho.quercus.lib.bam;

/**
 * BAM event types
 */
public enum BamEventType
{
  /*
  Enum type             prefix                        hasId  hasError
  */
  MESSAGE               ("bam_message",               false, false),
  MESSAGE_ERROR         ("bam_message_error",         false, true),

  QUERY_GET             ("bam_query_get",             true,  false),
  QUERY_SET             ("bam_query_set",             true,  false),
  QUERY_RESULT          ("bam_query_result",          true,  false),
  QUERY_ERROR           ("bam_query_error",           true,  true),

  PRESENCE              ("bam_presence",              false, false),
  PRESENCE_UNAVAILABLE  ("bam_presence_unavailable",  false, false),
  PRESENCE_PROBE        ("bam_presence_probe",        false, false),
  PRESENCE_SUBSCRIBE    ("bam_presence_subscribe",    false, false),
  PRESENCE_SUBSCRIBED   ("bam_presence_subscribed",   false, false),
  PRESENCE_UNSUBSCRIBE  ("bam_presence_unsubscribe",  false, false),
  PRESENCE_UNSUBSCRIBED ("bam_presence_unsubscribed", false, false),
  PRESENCE_ERROR        ("bam_presence_error",        false, true),

  GET_DISCO_FEATURES    ("bam_get_disco_features",    false, false);

  private final String _prefix;
  private final boolean _hasId;
  private final boolean _hasError;

  BamEventType(String prefix, boolean hasId, boolean hasError)
  {
    _prefix = prefix;
    _hasId = hasId;
    _hasError = hasError;
  }

  public String getPrefix()
  {
    return _prefix;
  }

  public boolean hasId()
  {
    return _hasId;
  }

  public boolean hasError()
  {
    return _hasError;
  }
}
