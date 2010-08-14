package example;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Inject;

@Startup
public class MyStartupBean {
  private @Inject StartupResourceBean _startupResource;
  private @Inject MyService _service;

  @PostConstruct
  public void init()
  {
    _startupResource.setData(this + ": initial value");
    _service.setMessage(this + ": initial value");
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName();
  }
}
