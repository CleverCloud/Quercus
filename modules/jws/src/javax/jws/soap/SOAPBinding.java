package javax.jws.soap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface SOAPBinding {

  SOAPBinding.ParameterStyle parameterStyle() default ParameterStyle.WRAPPED;
  SOAPBinding.Style style() default Style.DOCUMENT;
  SOAPBinding.Use use() default Use.LITERAL;

  public static enum Use {
    ENCODED, LITERAL
  }

  public static enum Style {
    DOCUMENT, RPC
  }

  public static enum ParameterStyle {
    BARE, WRAPPED
  }

}

