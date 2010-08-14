package example;

import javax.servlet.*;

public class CometState {
  private ServletRequest _request;

  private int _count;

  public CometState(ServletRequest request)
  {
    _request = request;
  }

  public boolean isClosed()
  {
    return _request == null;
  }

  public boolean wake()
  {
    if (_request == null || _request.getAttribute("comet.complete") != null)
      return false;

    _request.setAttribute("comet.count", ++_count);

    AsyncContext async = _request.getAsyncContext();

    if (async == null)
      return false;
    else if (async.getRequest() != null) {
      async.dispatch();

      return true;
    }

    _request = null;
    async.complete();

    return false;
  }
}
