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
package javax.jcr.util;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

public abstract class TraversingItemVisitor implements ItemVisitor {
  protected final boolean breadthFirst;
  protected final int maxLevel;
  
  public TraversingItemVisitor()
  {
    this(false, -1);
  }
  
  public TraversingItemVisitor(boolean breadthFirst)
  {
    this(breadthFirst, -1);
  }

  public TraversingItemVisitor(boolean breadthFirst, int maxLevel)
  {
    throw new UnsupportedOperationException();
  }
  
  protected abstract void entering(Property property, int level)
    throws RepositoryException;
  
  protected abstract void entering(Node node, int level)
    throws RepositoryException;
  
  protected abstract void leaving(Property property, int level)
    throws RepositoryException;
  
  protected abstract void leaving(Node node, int level)
    throws RepositoryException;
  
  public void visit(Property property)
    throws RepositoryException
  {
    // entering(property, currentLevel);
    // leaving(property, currentLevel);
  }
  
  public void visit(Node node)
    throws RepositoryException
  {
    throw new UnsupportedOperationException();
  }

  public static class Default extends TraversingItemVisitor {
    public Default() {
    }
      
    public Default(boolean breadthFirst)
    {
      super(breadthFirst);
    }
      
    public Default(boolean breadthFirst, int maxLevel)
    {
      super(breadthFirst, maxLevel);
    }
      
    protected void entering(Node node, int level)
      throws RepositoryException
    {
    }

    protected void entering(Property property, int level)
      throws RepositoryException
    {
    }

    protected void leaving(Node node, int level)
      throws RepositoryException
    {
    }

    protected void leaving(Property property, int level)
      throws RepositoryException
    {
    }
  }
}
