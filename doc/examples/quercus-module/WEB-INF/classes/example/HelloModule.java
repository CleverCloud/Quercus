package example;

import com.caucho.quercus.module.AbstractQuercusModule;
  
public class HelloModule extends AbstractQuercusModule {
  public static String hello_test(String message)
  {
    return "Hello, " + message;
  }
}
