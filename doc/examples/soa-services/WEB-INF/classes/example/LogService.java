package example;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class LogService {
  private int _sequenceNumber = 1;
  private StringBuilder _log = new StringBuilder();

  @WebMethod
  public int log(String message)
  {
    _log.append(_sequenceNumber + ": " + message + "\n");
    return _sequenceNumber++;
  }

  @WebMethod(exclude=true)
  public String getLog()
  {
    return _log.toString();
  }
}
