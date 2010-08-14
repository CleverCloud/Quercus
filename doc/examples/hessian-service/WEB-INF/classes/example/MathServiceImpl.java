package example;

import javax.jws.*;

@WebService
public class MathServiceImpl implements MathService {
  /**
   * Adds two integers.
   */
  public int add(int a, int b)
  {
    return a + b;
  }
  
  /**
   * Subtracts two integers.
   */
  public int sub(int a, int b)
  {
    return a - b;
  }
  
  /**
   * Multiplies two integers.
   */
  public int mul(int a, int b)
  {
    return a * b;
  }
  
  /**
   * Divides two integers.
   */
  public int div(int a, int b)
  {
    return a / b;
  }
}
