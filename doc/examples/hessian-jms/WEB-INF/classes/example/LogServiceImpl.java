package example;

public class LogServiceImpl implements LogService {
  private int _sequenceNumber = 1;
  private StringBuilder _log = new StringBuilder();

  public void log(String message)
  {
    _log.append(_sequenceNumber + ": " + message + "\n");
    _sequenceNumber++;
  }

  public String getLog()
  {
    return _log.toString();
  }
}
