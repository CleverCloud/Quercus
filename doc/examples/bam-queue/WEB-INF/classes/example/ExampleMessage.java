package example;

import java.io.Serializable;
import java.util.Date;

public class ExampleMessage implements Serializable
{
  private String _body;
  
  // zero-arg required for hessian serialization
  private void ExampleMessage()
  {
  }

  public ExampleMessage(String body)
  {
    _body = body;
  }

  public String getBody()
  {
    return _body;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _body + "]";
  }
}