package test;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;

/**
 * Implements a resource which is a plain-old bean.
 *
 * The resource is configured in the resin.conf (or web.xml) using
 * bean-style configuration and saved in JNDI.
 *
 * <pre>
 * &lt;bean class="test.TestResource">
 *  &lt;init>
 *    &lt;value>sample configuration&lt;/value>
 *  &lt;/init>
 * &lt;/resource>
 * </pre>
 *
 * <p>Applications will use WebBeans to retrieve the resource:</p>
 *
 * <code><pre>
 * @In TestResource resource;
 * </pre></code>
 */
public class TestResource {
  private static final Logger log =
    Logger.getLogger(TestResource.class.getName());

  /**
   * Sample initialization param set by the <resource>
   */
  private String _value = "default";

  /**
   * Sets the value.
   */
  public void setValue(String value)
  {
    _value = value;
  }

  /**
   * init() is called at the end of the configuration.
   */
  @PostConstruct
  public void init()
  {
    log.config("TestResource[" + _value + "] init");
  }

  /**
   * Returns a printable version of the resource.
   */
  public String toString()
  {
    return "TestResource[" + _value + "]";
  }
}
