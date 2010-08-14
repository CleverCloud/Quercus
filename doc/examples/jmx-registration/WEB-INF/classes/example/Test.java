package example;

import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.MBeanRegistration;

/**
 * Implements a resource which is a plain-old bean, which exposes
 * the <code>getData()</code> method as a JMX-managed attribute.
 */
public class Test implements TestMBean, MBeanRegistration {
  /**
   * The bean's name.
   */
  private ObjectName _name;

  /**
   * Gets the name.
   */
  public ObjectName getObjectName()
  {
    return _name;
  }
  
  /**
   * Called before the registration.
   *
   * @param server the mbean server to be registered
   * @param name the client's name to be registered
   *
   * @return the name the object wans the be registered as
   */
  public ObjectName preRegister(MBeanServer server, ObjectName name)
    throws Exception
  {
    _name = name;

    return name;
  }
  
  /**
   * Called after the registration.
   *
   * @param registrationDone true if the registration was successful.
   */
  public void postRegister(Boolean registrationDone)
  {
  }
  
  /**
   * Called before deregistration.
   */
  public void preDeregister()
    throws Exception
  {
  }
  
  /**
   * Called after the deregistration.
   */
  public void postDeregister()
  {
  }

  /**
   * Returns a printable version of the resource.
   */
  public String toString()
  {
    return "Test[" + _name + "]";
  }
}
