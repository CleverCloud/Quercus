package example;

import javax.annotation.Resource;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.SUPPORTS;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Implementation of the Hello bean.
 */
@Stateless
public class HelloBean implements Hello {
  @Inject @Named("greeting1")
  private String _greeting1;

  @Inject @Named("greeting2")
  private String _greeting2;

  /**
   * Returns the first greeting
   */
  @TransactionAttribute(SUPPORTS)
  public String greeting1()
  {
    return _greeting1;
  }

  /**
   * Returns the second greeting.
   */
  @TransactionAttribute(SUPPORTS)
  public String greeting2()
  {
    return _greeting2;
  }
}
