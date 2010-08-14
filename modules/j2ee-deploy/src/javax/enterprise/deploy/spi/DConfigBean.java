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

package javax.enterprise.deploy.spi;

import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.XpathEvent;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;
import java.beans.PropertyChangeListener;

/**
 * Interface for a configuration bean.
 */
public interface DConfigBean {
  /**
   * Returns the deployment descriptor.
   */
  public DDBean getDDBean();

  /**
   * Returns the XPaths for the deployment descriptor.
   */
  public String []getXpaths();

  /**
   * Returns the XPaths for the deployment descriptor.
   */
  public DConfigBean getDConfigBean(DDBean bean)
    throws ConfigurationException;

  /**
   * Removes a DConfigBean.
   */
  public void removeDConfigBean(DConfigBean bean)
    throws BeanNotFoundException;

  /**
   * Notifies a change.
   */
  public void notifyDDChange(XpathEvent event);

  /**
   * Adds a bean listener.
   */
  public void addPropertyChangeListener(PropertyChangeListener pcl);

  /**
   * Removes a bean listener.
   */
  public void removePropertyChangeListener(PropertyChangeListener pcl);
}

