package example;

import javax.jws.WebMethod;
import javax.jws.WebService;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@WebService(endpointInterface="example.HelloService")
@XmlRootElement
public class HelloServiceImpl implements HelloService {
  @XmlElement(name="hello")
  private String _hello;

  /**
   * Returns "hello, world".
   */
  @WebMethod
  public String hello()
  {
    return _hello;
  }
}
