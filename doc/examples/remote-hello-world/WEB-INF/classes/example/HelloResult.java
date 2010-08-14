package example;

import java.io.Serializable;

public class HelloResult implements Serializable {
  public String value = "hello, world";

  public String toString()
  {
    return value;
  }
}
