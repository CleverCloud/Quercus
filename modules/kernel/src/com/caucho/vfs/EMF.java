package com.caucho.vfs;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public final class EMF
{
  private static final EntityManagerFactory _factory
    = Persistence.createEntityManagerFactory("transactions-optional");

  private EMF() {}

  public static EntityManagerFactory get()
  {
    return _factory;
  }
  
  public static EntityManager createEntityManager()
  {
    return _factory.createEntityManager();
  }
}