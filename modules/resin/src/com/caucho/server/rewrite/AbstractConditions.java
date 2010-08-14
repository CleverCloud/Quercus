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
 * @author Sam
 */

package com.caucho.server.rewrite;

import com.caucho.config.Config;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

abstract public class AbstractConditions
  extends AbstractCondition
{
  private final RewriteDispatch _rewriteDispatch;
  private ArrayList<Condition> _conditionsList = new ArrayList<Condition>();
  private Condition[] _conditions;

  public AbstractConditions(RewriteDispatch rewriteDispatch)
  {
    _rewriteDispatch = rewriteDispatch;
  }

  public <T extends Condition> T add(T condition)
  {
    _conditionsList.add(condition);
    return condition;
  }

  public void addWhen(ConditionConfig condition)
  {
    add(condition.getCondition());
  }

  public void addUnless(ConditionConfig condition)
  {
    NotConditions not = new NotConditions();
    not.add(condition.getCondition());
    Config.init(not);

    add(not);
  }

  public AndConditions createAnd()
  {
    return new AndConditions(_rewriteDispatch);
  }

  public void addAnd(AndConditions and)
  {
    add(and);
  }

  public NotConditions createNot()
  {
    return new NotConditions(_rewriteDispatch);
  }

  public void addNot(NotConditions not)
  {
    add(not);
  }

  public OrConditions createOr()
  {
    return new OrConditions(_rewriteDispatch);
  }

  public void addOr(OrConditions or)
  {
    add(or);
  }

  @PostConstruct
  public void init()
  {
    _conditions = _conditionsList.toArray(new Condition[_conditionsList.size()]);
    _conditionsList = null;
  }

  protected Condition[] getConditions()
  {
    return _conditions;
  }
}
