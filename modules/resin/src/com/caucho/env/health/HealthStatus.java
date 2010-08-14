/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */
package com.caucho.env.health;

/**
 * The health thread checks the status of the server every 60s.
 */
public enum HealthStatus
{
  OK,
  WARNING,
  FAIL;
}
