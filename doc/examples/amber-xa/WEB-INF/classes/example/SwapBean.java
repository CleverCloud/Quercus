package example;

import javax.ejb.Stateless;

import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.SUPPORTS;

/**
 * Implementation of the Swap bean.
 */
@Stateless(name="swap")
public class SwapBean implements Swap {
  /**
   * Swaps the teacher inside a transaction.
   */
  @TransactionAttribute(REQUIRED)
  public void swap(Course a, Course b)
  {
    String teacher = a.getTeacher();
    a.setTeacher(b.getTeacher());
    b.setTeacher(teacher);
  }
  
  /**
   * Returns the teacher.
   */
  @TransactionAttribute(SUPPORTS)
  public String getTeacher(Course a)
  {
    return a.getTeacher();
  }
}
