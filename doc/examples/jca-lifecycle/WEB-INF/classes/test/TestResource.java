package test;

import java.util.logging.Logger;

import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.BootstrapContext;

import com.caucho.jca.AbstractResourceAdapter;

/**
 * Implements a resource which uses the ResourceAdapter lifecycle.
 */
public class TestResource extends AbstractResourceAdapter {
  private static final Logger log =
    Logger.getLogger(ResourceAdapter.class.getName());

  /**
   * Sample initialization param set by the <resource>
   */
  private String _value = "default";

  private String _status = "new";

  /**
   * Sets the value.
   */
  public void setValue(String value)
  {
    _value = value;
  }
  
  /**
   * The start method is called when the resource adapter starts, i.e.
   * when the web-app or host initializes.
   */
  public void start(BootstrapContext ctx)
    throws ResourceAdapterInternalException
  {
    log.info("Resource[value=" + _value + "] starting");

    _status = "started";
  }
  
  /**
   * Called when the resource adapter is stopped, i.e. when the
   * web-app or host closes down.
   */
  public void stop()
    throws ResourceAdapterInternalException
  {
    log.info("Resource[value=" + _value + "] stopping");
    
    _status = "stopped";
  }

  /**
   * Returns a printable version of the resource.
   */
  public String toString()
  {
    return "TestResource[" + _status + ", " + _value + "]";
  }
}
