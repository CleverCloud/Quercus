package javax.jws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface WebParam {

    boolean header() default false;
    WebParam.Mode mode() default WebParam.Mode.IN;
    String name() default "";
    String partName() default "";
    String targetNamespace() default "";

    public static enum Mode {
        IN,
        OUT,
        INOUT
    }

}

