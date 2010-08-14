/**
 * Java Dependency Injection annotations and exceptions.
 *
 * For programmatic access see {@link javax.enterprise.inject.spi}.
 *
 * <h2>Example: injecting a servlet using a custom binding type</h2>
 *
 * <code><pre>
 * package example;
 *
 * import example.MyBinding;
 * import javax.servlet.*;
 * import java.io.*;
 *
 * public class MyServlet extends GenericServlet {
 *   {@literal @MyBinding} MyBean _bean;
 *
 *   public void service(ServletRequest req, ServletResponse res)
 *     throws IOException
 *   {
 *     PrintWriter out = res.getWriter();
 *
 *     out.println("my-bean: " + _bean);
 *   }
 * }
 * </pre></code>
 *
 * <h2>Example: creating a custom binding type</h2>
 *
 * <code><pre>
 * package example;
 *
 * import static java.lang.annotation.ElementType.*;
 * import static java.lang.annotation.RetentionPolicy.Runtime;
 * import java.lang.annotation.*;
 *
 * import javax.enterprise.inject.BindingType;
 *
 * {@literal @BindingType}
 * {@literal @Documented}
 * Target({TYPE, METHOD, FIELD, PARAMETER})
 * Retention(RUNTIME)
 * public {@literal @interface} MyBinding {
 * }
 * </pre></code>
 *
 * <h2>Example: configuring using a custom binding type</h2>
 *
 * META-INF/beans.xml
 *
 * <code><pre>
 * &lt;Beans xmlns="urn:java:ee" xmlns:example="urn:java:example">
 *
 *   &lt;example:MyBean>
 *     &lt;example:MyBinding/>
 *   &lt;/example:MyBean>
 *
 * &lt;/Beans>
 * </pre></code>
 */
package javax.enterprise.inject;
