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

package com.caucho.env.repository;

import com.caucho.env.git.GitService;
import com.caucho.env.service.AbstractResinService;
import com.caucho.env.service.ResinSystem;
import com.caucho.util.L10N;

public class LocalRepositoryService
  extends AbstractResinService
{
  public static final int START_PRIORITY = GitService.START_PRIORITY + 1;
  
  private static final L10N L = new L10N(LocalRepositoryService.class);

  private FileRepository _fileRepository;
  
  public LocalRepositoryService()
  {
    GitService git = GitService.create();
    
    if (git == null) {
      throw new IllegalStateException(L.l("{0} is required for {1}",
                                          GitService.class.getSimpleName(),
                                          getClass().getSimpleName()));
    }
    
    _fileRepository = new FileRepository(git);
  }

  public static LocalRepositoryService getCurrent()
  {
    return ResinSystem.getCurrentService(LocalRepositoryService.class);
  }
  
  public Repository getRepository()
  {
    return _fileRepository;
  }
  
  @Override
  public void start()
  {
    _fileRepository.start();
  }
}